# Use ONLY a dedicated QA AVD. Run full legacy regression on API30, then
# targeted v5 coverage; on API33/36 use targeted coverage without old API30-only tests.
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
    if (-not (@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
        throw 'V5_QA_DEVICE_NOT_CONNECTED'
    }
    if ($FullRegression) {
        & .\scripts\run-library-performance-v4-qa.ps1 -Serial $Serial
        if ($LASTEXITCODE -ne 0) { throw 'V5_FULL_BASELINE_FAILED' }
        Write-Output 'V5_EXISTING_FULL_REGRESSION=PASS'
    } else {
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'V5_DEBUG_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'V5_TEST_INSTALL_FAILED' }
    }
    function CheckInstrument([string]$testClass, [int]$expected) {
        $raw = @(& $adb -s $Serial shell am instrument -w -r -e class $testClass $runner)
        $exitCode = $LASTEXITCODE
        $raw | Select-Object -Last 45
        if ($exitCode -ne 0 -or !($raw -match "OK \($expected tests?\)")) {
            throw "V5_ANDROID_FAILED_CLASS=$testClass"
        }
        Write-Output "V5_ANDROID_PASS_CLASS=$testClass COUNT=$expected"
    }
    CheckInstrument 'com.example.musicflow.ui.AccessibilityOfflineSearchV5Test' 6
    CheckInstrument 'com.example.musicflow.ui.MusicFlowSearchUxV3Test' 4
    if (-not $FullRegression) {
        CheckInstrument 'com.example.musicflow.ui.LibraryPerformanceV4UiTest' 7
        CheckInstrument 'com.example.musicflow.core.media.OfflineDiscoveryMediaStoreTest' 2
        CheckInstrument 'com.example.musicflow.core.playback.RealPickerSafTest#realOpenMultipleDocumentsGrantsPersistedReadAndPlays' 1
    } else {
        CheckInstrument 'com.example.musicflow.core.playback.AdvancedSearchRuntimeTest' 2
    }
    Write-Output 'ACCESSIBILITY_OFFLINE_SEARCH_V5_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
