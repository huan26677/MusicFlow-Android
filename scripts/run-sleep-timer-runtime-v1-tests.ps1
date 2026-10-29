param([string]$DeviceId = "emulator-5554")

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\sleep-timer-runtime-v1.log"
$runner = "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"

function Record([string]$message) {
    Write-Host $message
    Add-Content -LiteralPath $log -Value $message -Encoding UTF8
}

function Native([string]$exe, [string[]]$arguments) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $lines = @(& $exe @arguments 2>&1)
        $exit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $old }
    $value = $lines -join [char]10
    Record $value
    return [pscustomobject]@{ Exit = $exit; Output = $value }
}

function Instrument([string]$label, [string]$classes, [int]$count) {
    $r = Native "adb" @(
        "-s",$DeviceId,"shell","am","instrument","-w",
        "-e","class",$classes,$runner
    )
    Record "$($label)_EXIT=$($r.Exit)"
    if ($r.Exit -ne 0 -or !$r.Output.Contains("OK ($count tests)") -or
        $r.Output -match "FAILURES|INSTRUMENTATION_FAILED|Error in") {
        throw "FAILED_$label"
    }
    Record "$($label)_PASS=$count"
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Sleep Timer Runtime Acceptance v1" -Encoding UTF8
    Record "DATE=$(Get-Date -Format o)"
    Record "HEAD_BASE=$(git rev-parse --short HEAD)"
    Record "DEVICE=$DeviceId"
    Record "NO_REAL_ONLINE_MUSIC_OR_AUTH=TRUE"
    $sdk = Native "adb" @("-s",$DeviceId,"shell","getprop","ro.build.version.sdk")
    $boot = Native "adb" @("-s",$DeviceId,"shell","getprop","sys.boot_completed")
    if ($sdk.Exit -ne 0 -or $sdk.Output.Trim() -ne "30" -or
        $boot.Output.Trim() -ne "1") { throw "EMULATOR_API_30_NOT_READY" }

    $build = Native ".\gradlew.bat" @(
        ":app:assembleDebug",":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest","--console=plain"
    )
    Record "BUILD_JVM_EXIT=$($build.Exit)"
    if ($build.Exit -ne 0 -or !$build.Output.Contains("BUILD SUCCESSFUL")) {
        throw "GRADLE_BUILD_JVM_FAILED"
    }
    foreach ($apk in @(
        "app\build\outputs\apk\debug\app-debug.apk",
        "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    )) {
        $installed = Native "adb" @(
            "-s",$DeviceId,"install","-r",(Join-Path $root $apk)
        )
        if ($installed.Exit -ne 0 -or !$installed.Output.Contains("Success")) {
            throw "APK_INSTALL_FAILED"
        }
    }
    $null = Native "adb" @("-s",$DeviceId,"logcat","-c")
    Instrument "SLEEP_RUNTIME" (
        "com.example.musicflow.core.playback.SleepTimerRuntimeTest"
    ) 3
    $diagnostics = Native "adb" @(
        "-s",$DeviceId,"logcat","-d","-v","brief",
        "-s","MusicFlowSleepQA:I","MusicFlowSleepTimer:I","*:S"
    )
    $markers = @(
        "PASS invalidValuesRejected",
        "PASS timerSetAndQueried",
        "PASS timerReplaced",
        "PASS timerCancelled",
        "PASS offlinePlaybackStarted",
        "TIMER_FINISHED",
        "PASS expiryPausesAndPreservesQueue",
        "PASS cancelPreventsExpiryPause"
    )
    foreach ($marker in $markers) {
        $found = $diagnostics.Output.Contains($marker)
        Record "MARKER_$marker=$found"
        if (!$found) { throw "MISSING_SLEEP_TIMER_MARKER_$marker" }
    }
    Record "SLEEP_RUNTIME_MARKERS=8"

    # Independent session so the main app/player integration assertions can
    # detect any timer side effects without coupling to our test's MediaSession.
    Instrument "PLAYBACK_REGRESSION" (
        "com.example.musicflow.core.playback.PlaybackServiceIntegrationTest," +
        "com.example.musicflow.core.playback.PlaybackQueuePersistenceTest"
    ) 8

    $diff = Native "git" @("diff","--check")
    Record "GIT_DIFF_CHECK_EXIT=$($diff.Exit)"
    if ($diff.Exit -ne 0) { throw "GIT_DIFF_CHECK_FAILED" }
    Record "TOTAL_ANDROID_TESTS=11"
    Record "FINAL_RESULT=PASS"
} catch {
    Record "FAIL_REASON=$($_.Exception.Message)"
    Record "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
