param([string]$DeviceId = "emulator-5554")

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\equalizer-runtime-v1.log"
$runner = "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"

function Record([string]$message) {
    Write-Host $message
    Add-Content -LiteralPath $log -Value $message -Encoding UTF8
}

function Native([string]$exe, [string[]]$arguments) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $lines = @(& $exe @arguments 2>&1)
        $exit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $previous }
    $result = $lines -join [char]10
    Record $result
    return [pscustomobject]@{ Exit = $exit; Output = $result }
}

function Instrument([string]$name, [string]$classes, [int]$count) {
    $result = Native "adb" @(
        "-s", $DeviceId, "shell", "am", "instrument", "-w",
        "-e", "class", $classes, $runner
    )
    Record "${name}_EXIT=$($result.Exit)"
    if ($result.Exit -ne 0 -or
        !$result.Output.Contains("OK ($count tests)") -or
        $result.Output -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "${name}_FAILED"
    }
    Record "${name}_TESTS=$count"
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Equalizer Runtime Acceptance v1" -Encoding UTF8
    Record "DATE=$(Get-Date -Format o)"
    Record "HEAD_BASE=$(git rev-parse --short HEAD)"
    Record "DEVICE=$DeviceId"
    Record "NO_ONLINE_MUSIC_OR_ACCOUNT_REQUIRED=TRUE"
    Record "REAL_EQ_EFFECT_ON_DEVICE=REQUIRED_FOR_PASS"

    $api = Native "adb" @("-s", $DeviceId, "shell", "getprop", "ro.build.version.sdk")
    $boot = Native "adb" @("-s", $DeviceId, "shell", "getprop", "sys.boot_completed")
    if ($api.Exit -ne 0 -or $api.Output.Trim() -ne "30" -or
        $boot.Exit -ne 0 -or $boot.Output.Trim() -ne "1") {
        throw "EXPECTED_BOOTED_ANDROID_API_30"
    }

    $build = Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Record "BUILD_JVM_EXIT=$($build.Exit)"
    if ($build.Exit -ne 0 -or !$build.Output.Contains("BUILD SUCCESSFUL")) {
        throw "BUILD_OR_JVM_FAILED"
    }

    foreach ($apk in @(
        "app\build\outputs\apk\debug\app-debug.apk",
        "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    )) {
        $installed = Native "adb" @("-s", $DeviceId, "install", "-r", (Join-Path $root $apk))
        if ($installed.Exit -ne 0 -or !$installed.Output.Contains("Success")) {
            throw "APK_INSTALL_FAILED"
        }
    }
    $null = Native "adb" @("-s", $DeviceId, "logcat", "-c")

    Instrument "EQUALIZER_RUNTIME" (
        "com.example.musicflow.core.playback.EqualizerRuntimeTest"
    ) 2
    $diagnostics = Native "adb" @(
        "-s", $DeviceId, "logcat", "-d", "-v", "brief",
        "-s", "MusicFlowEqualizerQA:I", "MusicFlowEqualizer:W", "*:S"
    )
    $markers = @(
        "PASS liveAudioEffectSession",
        "PASS invalidSettingsDoNotStopPlayback",
        "PASS enableTogglePersisted",
        "PASS bandGainPersisted",
        "PASS controllerReconnectKeepsState",
        "PASS equalizerDoesNotStopOfflineAudio"
    )
    foreach ($marker in $markers) {
        $found = $diagnostics.Output.Contains($marker)
        Record "MARKER_$marker=$found"
        if (!$found) { throw "MISSING_EQUALIZER_MARKER_$marker" }
    }
    Record "EQUALIZER_RUNTIME_MARKERS=6"

    # Run in another instrumentation session to catch unintended interactions
    # with the real playback service, history, queue and media session.
    Instrument "PLAYBACK_REGRESSION" (
        "com.example.musicflow.core.playback.PlaybackServiceIntegrationTest," +
        "com.example.musicflow.core.playback.PlaybackQueuePersistenceTest"
    ) 8

    $check = Native "git" @("diff", "--check")
    Record "GIT_DIFF_CHECK_EXIT=$($check.Exit)"
    if ($check.Exit -ne 0) { throw "GIT_DIFF_CHECK_FAILED" }
    Record "TOTAL_ANDROID_TESTS=10"
    Record "FINAL_RESULT=PASS"
} catch {
    Record "FAIL_REASON=$($_.Exception.Message)"
    Record "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
