$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\playback-queue-actions-v1.log"

function Write-Log([string]$message) {
    Write-Host $message
    Add-Content -LiteralPath $log -Value $message -Encoding UTF8
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Playback Queue Actions v1" -Encoding UTF8
    Write-Log "DATE=$(Get-Date -Format o)"
    Write-Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Write-Log "BEGIN_ANDROID_BUILD_AND_JVM"
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain 2>&1 |
            ForEach-Object { Write-Log ([string]$_) }
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $old
    }
    Write-Log "BUILD_AND_JVM_EXIT=$code"
    if ($code -ne 0) { throw "GRADLE_FAILED" }

    $report = Join-Path $root "app\build\test-results\testDebugUnitTest\TEST-com.example.musicflow.core.playback.QueueInsertRulesTest.xml"
    if (!(Test-Path -LiteralPath $report)) { throw "QUEUE_TEST_REPORT_MISSING" }
    [xml]$xml = Get-Content -LiteralPath $report -Raw
    $suite = $xml.testsuite
    Write-Log "QUEUE_RULE_TESTS=$($suite.tests)"
    Write-Log "QUEUE_RULE_FAILURES=$($suite.failures)"
    Write-Log "QUEUE_RULE_ERRORS=$($suite.errors)"
    if ([int]$suite.tests -lt 8 -or [int]$suite.failures -ne 0 -or
        [int]$suite.errors -ne 0 -or [int]$suite.skipped -ne 0) {
        throw "QUEUE_RULE_REGRESSION_FAILED"
    }
    Write-Log "FINAL_RESULT=BUILD_AND_UNIT_PASS"
} catch {
    Write-Log "FAIL_REASON=$($_.Exception.Message)"
    Write-Log "FINAL_RESULT=FAIL"
    exit 1
} finally {
    Pop-Location
}
