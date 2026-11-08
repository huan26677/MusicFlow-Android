# Actual signed v6 -> v7 app replacement. Run ONLY on fresh isolated QA AVD.
# Reuses two tested SAF/Room upgrade stages from SignedApkUpgradeV6Test;
# class method names mention v5/v6 for backward compatibility, but APK
# installation order here explicitly uses v6 as the baseline and v7 as target.
param([Parameter(Mandatory=$true)][string]$Serial)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$runner = 'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner'
$oldApk = 'dist/MusicFlow-1.0-Demo-AccessibilityUpgradeV6-Android.apk'
$newApk = 'dist/MusicFlow-1.0-Demo-LibraryAccessibilityV7-Android.apk'
if ($Serial -notmatch '^emulator-\d+$') { throw 'V7_ONLY_DISPOSABLE_EMULATORS' }
Push-Location $root
try {
    if (-not (Test-Path $oldApk) -or -not (Test-Path $newApk)) {
        throw 'V7_SIGNED_APKS_MISSING'
    }
    if (-not (@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
        throw 'V7_QA_EMULATOR_NOT_CONNECTED'
    }
    function InstallApk([string]$apk) {
        & $adb -s $Serial install -r $apk
        if ($LASTEXITCODE -ne 0) { throw "V7_INSTALL_FAILED=$apk" }
    }
    function TestPhase([string]$name) {
        $test = "com.example.musicflow.ui.SignedApkUpgradeV6Test#$name"
        $raw = @(& $adb -s $Serial shell am instrument -w -r -e class $test $runner)
        $rc = $LASTEXITCODE
        $raw | Select-Object -Last 48
        if ($rc -ne 0 -or !($raw -match 'OK \(1 test\)')) {
            throw "V7_UPGRADE_FAILED_STAGE=$name"
        }
        Write-Output "V7_SIGNED_UPGRADE_PHASE_PASS=$name"
    }
    InstallApk $oldApk
    InstallApk 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    TestPhase 'seedSignedV5PlaylistFavoriteAndRealSafGrant'
    InstallApk $newApk
    TestPhase 'verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly'
    Write-Output 'V7_SIGNED_UPGRADE_V6_TO_V7_ROOM_FAVORITES_SAF=PASS'
} finally {
    Pop-Location
}
