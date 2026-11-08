# Runs v7 regression + v8 keyboard/search index tests on an ISOLATED emulator.
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
if ($Serial -notmatch '^emulator-\d+$') { throw 'V8_ONLY_DISPOSABLE_EMULATORS' }
Push-Location $root
try {
    if (-not (@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
        throw 'V8_QA_EMULATOR_NOT_CONNECTED'
    }
    & .\scripts\run-library-accessibility-v7-qa.ps1 -Serial $Serial -FullRegression:$FullRegression
    if ($LASTEXITCODE -ne 0) { throw 'V8_V7_REGRESSION_FAILED' }
    foreach ($item in @(
        @('com.example.musicflow.feature.library.LibraryKeyboardAccessibilityV8Test',3),
        @('com.example.musicflow.feature.search.AdvancedSearchIndexV8Test',5)
    )) {
        $test = [string]$item[0]
        $count = [int]$item[1]
        $raw = @(& $adb -s $Serial shell am instrument -w -r -e class $test $runner)
        $rc = $LASTEXITCODE
        $raw | Select-Object -Last 48
        if ($rc -ne 0 -or !($raw -match "OK \($count tests\)")) {
            throw "V8_ANDROID_TESTS_FAILED=$test"
        }
        Write-Output "V8_ANDROID_PASS_CLASS=$test COUNT=$count"
    }
    Write-Output 'KEYBOARD_SEARCHINDEX_V8_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
