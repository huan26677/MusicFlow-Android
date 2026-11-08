# One complete Android QA pass after the v6 feature batch.
# Use ONLY a newly created disposable QA emulator (API30 full; API33/36 targeted).
param(
    [Parameter(Mandatory=$true)][string]$Serial,
    [switch]$FullRegression
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$runner = 'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner'

Push-Location $root
try {
    if ($Serial -notmatch '^emulator-\d+$') { throw 'V6_QA_MUST_USE_EMULATOR' }
    if (-not (@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
        throw 'V6_QA_EMULATOR_NOT_CONNECTED'
    }
    if ($FullRegression) {
        & .\scripts\run-accessibility-offline-search-v5-qa.ps1 -Serial $Serial -FullRegression
        if ($LASTEXITCODE -ne 0) { throw 'V6_FULL_V5_REGRESSION_FAILED' }
        Write-Output 'V6_FULL_V5_REGRESSION=PASS'
    } else {
        & .\scripts\run-accessibility-offline-search-v5-qa.ps1 -Serial $Serial
        if ($LASTEXITCODE -ne 0) { throw 'V6_TARGETED_V5_REGRESSION_FAILED' }
        Write-Output 'V6_TARGETED_V5_REGRESSION=PASS'
    }
    $test = 'com.example.musicflow.ui.AccessibilityPolishV6UiTest'
    $raw = @(& $adb -s $Serial shell am instrument -w -r -e class $test $runner)
    $rc = $LASTEXITCODE
    $raw | Select-Object -Last 80
    if ($rc -ne 0 -or !($raw -match 'OK \(6 tests\)')) {
        throw 'V6_ACCESSIBILITY_200_PERCENT_TESTS_FAILED'
    }
    Write-Output 'V6_ACCESSIBILITY_200_PERCENT_ANDROID=6_PASS'
    Write-Output 'ACCESSIBILITY_POLISH_V6_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
