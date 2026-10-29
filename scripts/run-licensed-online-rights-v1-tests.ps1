param([string]$DeviceId = "emulator-5554")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\licensed-online-rights-v1.log"

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
    } finally { $ErrorActionPreference = $previous }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Licensed Online Rights v1" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Log "DEVICE=$DeviceId"
    $gradle = Run-Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Log "BUILD_AND_JVM_EXIT=$gradle"
    if ($gradle -ne 0) { throw "GRADLE_OR_JVM_FAILED" }

    $report = Join-Path $root "app\build\test-results\testDebugUnitTest\TEST-com.example.musicflow.core.online.OnlineRightsPolicyTest.xml"
    if (!(Test-Path -LiteralPath $report)) { throw "MISSING_RIGHTS_JVM_REPORT" }
    [xml]$xml = Get-Content -LiteralPath $report -Raw
    $suite = $xml.testsuite
    Log "ONLINE_RIGHTS_JVM_TESTS=$($suite.tests)"
    if ([int]$suite.tests -lt 10 -or [int]$suite.failures -ne 0 -or
        [int]$suite.errors -ne 0 -or [int]$suite.skipped -ne 0) {
        throw "RIGHTS_JVM_FAILED"
    }

    if ((Run-Native "adb" @("-s", $DeviceId, "get-state")) -ne 0) {
        throw "DEVICE_NOT_READY"
    }
    $app = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    $androidTest = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $app)) -ne 0) {
        throw "APP_INSTALL_FAILED"
    }
    if ((Run-Native "adb" @("-s", $DeviceId, "install", "-r", $androidTest)) -ne 0) {
        throw "ANDROID_TEST_INSTALL_FAILED"
    }
    $classes = "com.example.musicflow.core.online.OnlineRightsJsonTest," +
        "com.example.musicflow.core.online.OnlineRightsScreenTest," +
        "com.example.musicflow.core.media.TrackMediaItemsTest," +
        "com.example.musicflow.core.playback.PlaybackQueuePersistenceTest," +
        "com.example.musicflow.core.playback.QueueManagerIntegrationTest"
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $results = @(& adb -s $DeviceId shell am instrument -w -e class $classes com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner 2>&1)
        $exit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $previous }
    $results | ForEach-Object { Log ([string]$_) }
    Log "INSTRUMENTATION_EXIT=$exit"
    $output = $results -join [char]10
    if ($exit -ne 0 -or $output -notmatch "OK \(21 tests\)" -or
        $output -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "RIGHTS_ANDROID_INSTRUMENTATION_FAILED"
    }
    Log "ONLINE_RIGHTS_ANDROID_TESTS=21"
    Log "FINAL_RESULT=PASS"
} catch {
    Log "FAIL_REASON=$($_.Exception.Message)"
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
