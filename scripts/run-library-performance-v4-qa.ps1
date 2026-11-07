# QA on an isolated emulator. Never target a device without an explicit serial.
# Runs the established full regression, then v4 UX + real WAV scanning + picker.
param([Parameter(Mandatory=$true)][string]$Serial)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$runner = 'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner'
Push-Location $root
try {
    & .\scripts\run-smart-library-v1-qa.ps1 -Serial $Serial
    if ($LASTEXITCODE -ne 0) { throw 'V4_BASE_REGRESSION_FAILED' }
    Write-Output 'V4_BASE_REGRESSION=PASS'

    function CheckInstrument([string]$testClass, [int]$expected) {
        $raw = @(& $adb -s $Serial shell am instrument -w -r -e class $testClass $runner)
        $raw | Select-Object -Last 35
        if ($LASTEXITCODE -ne 0 -or !($raw -match "OK \($expected tests?\)")) {
            throw "V4_ANDROID_FAILED_CLASS=$testClass"
        }
        Write-Output "V4_ANDROID_PASS_CLASS=$testClass COUNT=$expected"
    }
    CheckInstrument 'com.example.musicflow.ui.LibraryPerformanceV4UiTest' 7
    CheckInstrument 'com.example.musicflow.core.media.OfflineDiscoveryMediaStoreTest' 2
    CheckInstrument 'com.example.musicflow.core.playback.RealPickerSafTest#realOpenMultipleDocumentsGrantsPersistedReadAndPlays' 1
    Write-Output 'LIBRARY_PERFORMANCE_V4_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
