param([string]$DeviceId = "emulator-5554")

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\full-product-regression-v1-part2a-playback.log"

function Record([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function Native([string]$executable, [string[]]$arguments) {
    $before = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = @(& $executable @arguments 2>&1)
        $exit = [int]$LASTEXITCODE
    } finally {
        $ErrorActionPreference = $before
    }
    $message = $output -join [char]10
    Record $message
    return [pscustomobject]@{ Exit = $exit; Output = $message }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Full Regression v1 - Part 2A Previous/Next" -Encoding UTF8
    Record "DATE=$(Get-Date -Format o)"
    Record "HEAD_BASE=$(git rev-parse --short HEAD)"
    Record "DEVICE=$DeviceId"

    $api = Native "adb" @("-s", $DeviceId, "shell", "getprop", "ro.build.version.sdk")
    if ($api.Exit -ne 0 -or $api.Output.Trim() -ne "30") {
        throw "EXPECTED_ANDROID_API_30"
    }
    $build = Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Record "BUILD_JVM_EXIT=$($build.Exit)"
    if ($build.Exit -ne 0 -or $build.Output -notmatch "BUILD SUCCESSFUL") {
        throw "ANDROID_BUILD_OR_JVM_FAILED"
    }
    foreach ($apk in @(
        "app\build\outputs\apk\debug\app-debug.apk",
        "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    )) {
        $installed = Native "adb" @("-s", $DeviceId, "install", "-r", (Join-Path $root $apk))
        if ($installed.Exit -ne 0 -or $installed.Output -notmatch "Success") {
            throw "APK_INSTALL_FAILED"
        }
    }
    $null = Native "adb" @("-s", $DeviceId, "logcat", "-c")

    # Regression suite covers MVP screen touch, MediaSession integration,
    # and playback skip recovery; no external account or online audio needed.
    $classes = "com.example.musicflow.ExampleInstrumentedTest," +
        "com.example.musicflow.core.playback.PlaybackServiceIntegrationTest," +
        "com.example.musicflow.core.playback.PlaybackSkipRecoveryTest"
    $results = Native "adb" @(
        "-s", $DeviceId, "shell", "am", "instrument", "-w",
        "-e", "class", $classes,
        "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"
    )
    Record "INSTRUMENTATION_EXIT=$($results.Exit)"
    if ($results.Exit -ne 0 -or $results.Output -notmatch "OK \(8 tests\)" -or
        $results.Output -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "PLAYBACK_REGRESSION_FAILED"
    }
    $logcat = Native "adb" @(
        "-s", $DeviceId, "logcat", "-d", "-v", "brief",
        "-s", "MusicFlowMvpE2E:I", "MusicFlowSkip:I", "*:S"
    )
    $markers = @(
        "PASS libraryUiToMediaSession",
        "PASS nextPreviousFromUi",
        "PASS pauseResumeFromUi",
        "PASS searchUiToMediaSession",
        "PASS uiPlaybackWritesHistory"
    )
    foreach ($marker in $markers) {
        if (!$logcat.Output.Contains($marker)) {
            throw "MISSING_MARKER_$marker"
        }
    }
    Record "MVP_PLAYBACK_MARKERS=5"
    Record "ANDROID_TESTS=8"

    $diff = Native "git" @("diff", "--check")
    if ($diff.Exit -ne 0) { throw "GIT_DIFF_CHECK_FAILED" }
    Record "FINAL_RESULT=PASS"
} catch {
    Record ("FAIL_REASON=" + $_.Exception.Message)
    Record "FINAL_RESULT=FAIL"
    exit 1
} finally {
    Pop-Location
}
