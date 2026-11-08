# Only for newly created, disposable MusicFlow V10 QA emulators.
# Signed QA APK v9 -> v10, Room/Favorites/persisted SAF real WAV check.
param([Parameter(Mandatory=$true)][string]$Serial)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$runner = 'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner'
if ($Serial -notmatch '^emulator-\d+$') { throw 'V10_QA_EMULATOR_ONLY' }
Push-Location $root
try {
    $avd = @(& $adb -s $Serial emu avd name)
    if (-not ($avd -match 'MusicFlow_ClearJourneysV10_QA_')) {
        throw 'V10_REFUSE_OTHER_DEVICE_OR_EXISTING_AVD'
    }
    $oldApk = 'dist/MusicFlow-1.0-Demo-SimpleExperienceV9-Android.apk'
    $newApk = 'dist/MusicFlow-1.0-Demo-ClearJourneysV10-Android.apk'
    if (!(Test-Path $oldApk) -or !(Test-Path $newApk)) {
        throw 'V10_QA_SIGNED_APKS_MISSING'
    }
    function InstallApk([string]$apk) {
        & $adb -s $Serial install -r $apk
        if ($LASTEXITCODE -ne 0) { throw "V10_APK_INSTALL_FAILED=$apk" }
    }
    function TestPhase([string]$name) {
        $case = "com.example.musicflow.ui.SignedApkUpgradeV6Test#$name"
        $raw = @(& $adb -s $Serial shell am instrument -w -r -e class $case $runner)
        $rc = $LASTEXITCODE
        $raw | Select-Object -Last 40
        if ($rc -ne 0 -or !($raw -match 'OK \(1 test\)')) {
            throw "V10_SIGNED_UPGRADE_FAIL=$name"
        }
    }
    InstallApk $oldApk
    InstallApk 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    TestPhase 'seedSignedV5PlaylistFavoriteAndRealSafGrant'
    InstallApk $newApk
    TestPhase 'verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly'
    Write-Output 'V10_SIGNED_UPGRADE_V9_TO_V10_ROOM_FAVORITES_SAF=PASS'
} finally {
    Pop-Location
}
