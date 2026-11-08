# Real signed APK replacement: Room playlist, favorite, persisted SAF READ.
# Use ONLY an isolated disposable QA AVD, never a personal emulator or phone.
param([Parameter(Mandatory=$true)][string]$Serial)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$runner = 'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner'
$oldApk = 'dist/MusicFlow-1.0-Demo-AccessibilitySearchV5-Android.apk'
$newApk = 'dist/MusicFlow-1.0-Demo-AccessibilityUpgradeV6-Android.apk'
if ($Serial -notmatch '^emulator-\d+$') { throw 'V6_REQUIRES_ISOLATED_QA_EMULATOR' }

Push-Location $root
try {
    if (-not (Test-Path $oldApk) -or -not (Test-Path $newApk)) {
        throw 'V6_BOTH_SIGNED_APKS_REQUIRED'
    }
    if (-not (@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
        throw 'V6_QA_EMULATOR_NOT_CONNECTED'
    }
    function RequireInstall([string]$apk) {
        & $adb -s $Serial install -r $apk
        if ($LASTEXITCODE -ne 0) { throw "V6_APK_INSTALL_FAILED=$apk" }
    }
    function RequireTest([string]$method) {
        $target = "com.example.musicflow.ui.SignedApkUpgradeV6Test#$method"
        $out = @(& $adb -s $Serial shell am instrument -w -r -e class $target $runner)
        $status = $LASTEXITCODE
        $out | Select-Object -Last 45
        if ($status -ne 0 -or !($out -match 'OK \(1 test\)')) {
            throw "V6_UPGRADE_TEST_FAILED=$method"
        }
        Write-Output "V6_UPGRADE_STEP_PASS=$method"
    }
    RequireInstall $oldApk
    RequireInstall 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    Write-Output 'V6_SIGNED_V5_INSTALLED=PASS'
    RequireTest 'seedSignedV5PlaylistFavoriteAndRealSafGrant'
    RequireInstall $newApk
    Write-Output 'V6_SIGNED_V6_INSTALLED_OVER_V5_WITHOUT_CLEAR=PASS'
    RequireTest 'verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly'
    Write-Output 'V6_SIGNED_UPGRADE_ROOM_FAVORITES_PERSISTED_SAF=PASS'
} finally {
    Pop-Location
}
