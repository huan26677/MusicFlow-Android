param([string]$DeviceId = "emulator-5554")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\musicflow-ui-polish-v2.log"

function Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function Run-Native([string]$exe, [string[]]$arguments) {
    $before = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & $exe @arguments 2>&1 | ForEach-Object { Log ([string]$_) }
        return [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $before }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow UI/UX Polish v2" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Log "DEVICE=$DeviceId"
    $build = Run-Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Log "BUILD_AND_JVM_EXIT=$build"
    if ($build -ne 0) { throw "GRADLE_BUILD_OR_JVM_FAILED" }
    if ((Run-Native "adb" @("-s", $DeviceId, "get-state")) -ne 0) {
        throw "DEVICE_NOT_READY"
    }
    $apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    $testApk = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $apk)) -ne 0) {
        throw "APP_INSTALL_FAILED"
    }
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $testApk)) -ne 0) {
        throw "TEST_INSTALL_FAILED"
    }
    $classes = "com.example.musicflow.ui.MusicFlowUiPolishTest," +
        "com.example.musicflow.core.playlist.PlaylistPlaybackOrderTest," +
        "com.example.musicflow.ui.MusicFlowUiFoundationTest," +
        "com.example.musicflow.core.playback.QueueManagerScreenTest," +
        "com.example.musicflow.ThemeBatchTest"
    $before = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = @(& adb -s $DeviceId shell am instrument -w -e class $classes com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner 2>&1)
        $instrumentExit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $before }
    $output | ForEach-Object { Log ([string]$_) }
    Log "INSTRUMENTATION_EXIT=$instrumentExit"
    $summary = $output -join [char]10
    if ($instrumentExit -ne 0 -or $summary -notmatch "OK \(19 tests\)" -or
        $summary -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "ANDROID_UI_REGRESSION_FAILED"
    }
    Log "UI_POLISH_TESTS=3"
    Log "PLAYLIST_REGRESSION_TESTS=3"
    Log "FOUNDATION_REGRESSION_TESTS=10"
    Log "QUEUE_REGRESSION_TESTS=2"
    Log "THEME_PERSISTENCE_TESTS=1"
    Log "ANDROID_TESTS=19"
    Log "FINAL_RESULT=PASS"
} catch {
    Log "FAIL_REASON=$($_.Exception.Message)"
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
