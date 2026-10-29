param([string]$DeviceId = "emulator-5554")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\queue-manager-v1.log"

function Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function Run-Native([string]$exe, [string[]]$arguments) {
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & $exe @arguments 2>&1 | ForEach-Object { Log ([string]$_) }
        return [int]$LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Queue Manager v1" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Log "DEVICE=$DeviceId"

    $gradleCode = Run-Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest", ":app:testDebugUnitTest",
        "--console=plain"
    )
    Log "BUILD_AND_JVM_EXIT=$gradleCode"
    if ($gradleCode -ne 0) { throw "ANDROID_BUILD_OR_UNIT_FAILED" }

    $total = 0
    foreach ($class in @("QueueEditRulesTest", "QueueInsertRulesTest")) {
        $report = Join-Path $root "app\build\test-results\testDebugUnitTest\TEST-com.example.musicflow.core.playback.$class.xml"
        if (!(Test-Path -LiteralPath $report)) { throw "MISSING_JVM_REPORT_$class" }
        [xml]$xml = Get-Content -LiteralPath $report -Raw
        $suite = $xml.testsuite
        Log "$class tests=$($suite.tests) failures=$($suite.failures) errors=$($suite.errors)"
        if ([int]$suite.failures -ne 0 -or [int]$suite.errors -ne 0 -or
            [int]$suite.skipped -ne 0) { throw "JVM_QUEUE_FAILED_$class" }
        $total += [int]$suite.tests
    }
    Log "QUEUE_JVM_TESTS=$total"
    if ($total -lt 16) { throw "QUEUE_TEST_COUNT_BELOW_EXPECTATION" }

    if ((Run-Native "adb" @("-s", $DeviceId, "get-state")) -ne 0) {
        throw "ANDROID_DEVICE_NOT_AVAILABLE"
    }
    $appApk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    $testApk = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $appApk)) -ne 0) {
        throw "APP_INSTALL_FAILED"
    }
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $testApk)) -ne 0) {
        throw "TEST_INSTALL_FAILED"
    }

    $testClasses = "com.example.musicflow.core.playback.QueueManagerIntegrationTest," +
        "com.example.musicflow.core.playback.QueueManagerScreenTest"
    $result = @()
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $result = @(& adb -s $DeviceId shell am instrument -w -e class $testClasses com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner 2>&1)
        $instrumentExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    $result | ForEach-Object { Log ([string]$_) }
    Log "INSTRUMENTATION_EXIT=$instrumentExit"
    if ($instrumentExit -ne 0 -or !(($result -join [char]10) -match "OK \(3 tests\)") -or
        (($result -join [char]10) -match "FAILURES|INSTRUMENTATION_FAILED")) {
        throw "QUEUE_ANDROID_INSTRUMENTATION_FAILED"
    }
    Log "QUEUE_ANDROID_TESTS=3"
    Log "FINAL_RESULT=PASS"
} catch {
    Log "FAIL_REASON=$($_.Exception.Message)"
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally {
    Pop-Location
}
