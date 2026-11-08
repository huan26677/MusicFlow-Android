# Isolated emulator only; API30 can run whole legacy regression once.
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
if ($Serial -notmatch '^emulator-\d+$') { throw 'V7_ONLY_DISPOSABLE_EMULATORS' }
Push-Location $root
try {
    if (-not (@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
        throw 'V7_QA_EMULATOR_NOT_CONNECTED'
    }
    & .\scripts\run-accessibility-polish-v6-qa.ps1 -Serial $Serial -FullRegression:$FullRegression
    if ($LASTEXITCODE -ne 0) { throw 'V7_PREVIOUS_REGRESSION_FAILED' }
    $target = 'com.example.musicflow.feature.library.LibraryAccessibilityV7Test'
    $raw = @(& $adb -s $Serial shell am instrument -w -r -e class $target $runner)
    $rc = $LASTEXITCODE
    $raw | Select-Object -Last 80
    if ($rc -ne 0 -or !($raw -match 'OK \(6 tests\)')) {
        throw 'V7_LIBRARY_ACCESSIBILITY_TESTS_FAILED'
    }
    Write-Output 'LIBRARY_ACCESSIBILITY_V7_ANDROID=6_PASS'
    Write-Output 'LIBRARY_ACCESSIBILITY_V7_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
