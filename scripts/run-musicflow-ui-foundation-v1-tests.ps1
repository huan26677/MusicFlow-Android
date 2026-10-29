param([string]$DeviceId = "emulator-5554")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\musicflow-ui-foundation-v1.log"

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
    } finally {
        $ErrorActionPreference = $before
    }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow UI/UX Foundation v1" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Log "DEVICE=$DeviceId"

    $build = Run-Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Log "BUILD_AND_JVM_EXIT=$build"
    if ($build -ne 0) { throw "BUILD_OR_JVM_FAILED" }

    if ((Run-Native "adb" @("-s", $DeviceId, "get-state")) -ne 0) {
        throw "DEVICE_NOT_AVAILABLE"
    }
    $appApk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    $testApk = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $appApk)) -ne 0) {
        throw "APP_INSTALL_FAILED"
    }
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $testApk)) -ne 0) {
        throw "TEST_INSTALL_FAILED"
    }

    $classes = "com.example.musicflow.ui.MusicFlowUiFoundationTest," +
        "com.example.musicflow.core.playback.QueueManagerScreenTest," +
        "com.example.musicflow.ThemeBatchTest"
    $before = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $result = @(& adb -s $DeviceId shell am instrument -w -e class $classes com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner 2>&1)
        $testExit = [int]$LASTEXITCODE
    } finally {
        $ErrorActionPreference = $before
    }
    $result | ForEach-Object { Log ([string]$_) }
    Log "INSTRUMENTATION_EXIT=$testExit"
    $output = $result -join [char]10
    if ($testExit -ne 0 -or $output -notmatch "OK \(13 tests\)" -or
        $output -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "ANDROID_UI_TESTS_FAILED"
    }

    Log "UI_FOUNDATION_ANDROID_TESTS=10"
    Log "QUEUE_SCREEN_REGRESSION_TESTS=2"
    Log "THEME_PERSISTENCE_REGRESSION_TESTS=1"
    Log "FINAL_RESULT=PASS"
} catch {
    Log "FAIL_REASON=$($_.Exception.Message)"
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally {
    Pop-Location
}
