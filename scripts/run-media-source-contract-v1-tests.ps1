param([string]$DeviceId = "emulator-5554")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\media-source-contract-v1.log"

function Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function Run-Native([string]$exe, [string[]]$arguments) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & $exe @arguments 2>&1 | ForEach-Object { Log ([string]$_) }
        return [int]$LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Media Source Contract v1" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Log "DEVICE=$DeviceId"

    $gradle = Run-Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Log "ANDROID_BUILD_AND_JVM_EXIT=$gradle"
    if ($gradle -ne 0) { throw "ANDROID_BUILD_OR_JVM_FAILED" }

    $report = Join-Path $root "app\build\test-results\testDebugUnitTest\TEST-com.example.musicflow.core.media.MediaSourceContractTest.xml"
    if (!(Test-Path -LiteralPath $report)) { throw "MISSING_SOURCE_IDENTITY_REPORT" }
    [xml]$xml = Get-Content -LiteralPath $report -Raw
    $suite = $xml.testsuite
    Log "SOURCE_IDENTITY_JVM_TESTS=$($suite.tests)"
    Log "SOURCE_IDENTITY_JVM_FAILURES=$($suite.failures)"
    Log "SOURCE_IDENTITY_JVM_ERRORS=$($suite.errors)"
    if ([int]$suite.tests -lt 11 -or [int]$suite.failures -ne 0 -or
        [int]$suite.errors -ne 0 -or [int]$suite.skipped -ne 0) {
        throw "SOURCE_IDENTITY_REGRESSION_FAILED"
    }

    if ((Run-Native "adb" @("-s", $DeviceId, "get-state")) -ne 0) {
        throw "DEVICE_NOT_AVAILABLE"
    }
    $apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    $testApk = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $apk)) -ne 0) {
        throw "APP_INSTALL_FAILED"
    }
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $testApk)) -ne 0) {
        throw "TEST_INSTALL_FAILED"
    }

    $classes = "com.example.musicflow.core.media.TrackMediaItemsTest," +
        "com.example.musicflow.core.playback.QueueManagerIntegrationTest," +
        "com.example.musicflow.core.playback.PlaybackQueuePersistenceTest"
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $result = @(& adb -s $DeviceId shell am instrument -w -e class $classes com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner 2>&1)
        $instrumentCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
    $result | ForEach-Object { Log ([string]$_) }
    Log "ANDROID_INSTRUMENTATION_EXIT=$instrumentCode"
    if ($instrumentCode -ne 0 -or !(($result -join [char]10) -match "OK \(9 tests\)") -or
        (($result -join [char]10) -match "FAILURES|INSTRUMENTATION_FAILED")) {
        throw "SOURCE_ANDROID_TEST_FAILED"
    }
    Log "SOURCE_AND_REGRESSION_ANDROID_TESTS=9"
    Log "FINAL_RESULT=PASS"
} catch {
    Log "FAIL_REASON=$($_.Exception.Message)"
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally {
    Pop-Location
}
