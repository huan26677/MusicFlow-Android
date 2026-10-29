$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\account-google-hardening-v2.log"

function Write-Log([string]$message) {
    Write-Host $message
    Add-Content -LiteralPath $log -Value $message -Encoding UTF8
}

function Invoke-Recorded([string]$label, [string]$program, [string[]]$arguments) {
    Write-Log "BEGIN_$label"
    # Windows PowerShell 5.1 treats benign native stderr as terminating
    # when ErrorActionPreference=Stop (Python unittest writes results to stderr).
    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & $program @arguments 2>&1 | ForEach-Object { Write-Log ([string]$_) }
        $exit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $oldPreference
    }
    Write-Log "${label}_EXIT=$exit"
    if ($exit -ne 0) { throw "${label}_FAILED" }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Account & Google Security Hardening v2" -Encoding UTF8
    Write-Log "DATE=$(Get-Date -Format o)"
    Write-Log "SOURCE_HEAD=$(git rev-parse --short HEAD)"

    Invoke-Recorded "BACKUP_POLICY_TEST" "python" @("scripts/test_account_backup_rules.py")
    Invoke-Recorded "ANDROID_BUILD_AND_JVM" ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )

    $results = Join-Path $root "app\build\test-results\testDebugUnitTest\TEST-com.example.musicflow.core.account.GoogleOAuthFlowTest.xml"
    if (!(Test-Path -LiteralPath $results)) { throw "GOOGLE_TEST_REPORT_NOT_FOUND" }
    [xml]$xml = Get-Content -LiteralPath $results -Raw
    $suite = $xml.testsuite
    Write-Log "GOOGLE_OAUTH_TESTS=$($suite.tests)"
    Write-Log "GOOGLE_OAUTH_FAILURES=$($suite.failures)"
    Write-Log "GOOGLE_OAUTH_ERRORS=$($suite.errors)"
    if ([int]$suite.tests -lt 12 -or [int]$suite.failures -ne 0 -or
        [int]$suite.errors -ne 0 -or [int]$suite.skipped -ne 0) {
        throw "GOOGLE_OAUTH_REGRESSION_FAILED"
    }

    Write-Log "FINAL_RESULT=BUILD_AND_UNIT_PASS"
} catch {
    Write-Log "FAIL_REASON=$($_.Exception.Message)"
    Write-Log "FINAL_RESULT=FAIL"
    exit 1
} finally {
    Pop-Location
}
