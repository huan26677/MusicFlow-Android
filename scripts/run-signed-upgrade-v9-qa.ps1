# QA-only signed v8 -> v9 upgrade on an isolated disposable Android emulator.
# No app-data reset during upgrade; all fixtures are owned by this test.
param([Parameter(Mandatory=$true)][string]$Serial)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$runner = 'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner'
$oldApk = 'dist/MusicFlow-1.0-Demo-KeyboardSearchIndexV8-Android.apk'
$newApk = 'dist/MusicFlow-1.0-Demo-SimpleExperienceV9-Android.apk'
if ($Serial -notmatch '^emulator-\d+$') { throw 'V9_ONLY_DISPOSABLE_EMULATORS' }
Push-Location $root
try {
    if (-not (Test-Path $oldApk) -or -not (Test-Path $newApk)) {
        throw 'V9_SIGNED_APKS_MISSING'
    }
    if (-not (@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
        throw 'V9_QA_EMULATOR_NOT_CONNECTED'
    }
    $avdIdentity = @(& $adb -s $Serial emu avd name)
    if (-not ($avdIdentity -match 'MusicFlow_SimpleExperienceV9_QA_')) {
        throw 'V9_REFUSE_NONDEDICATED_AVD'
    }
    function InstallApk([string]$apk) {
        & $adb -s $Serial install -r $apk
        if ($LASTEXITCODE -ne 0) { throw "V9_INSTALL_FAILED=$apk" }
    }
    function TestPhase([string]$name) {
        $test = "com.example.musicflow.ui.SignedApkUpgradeV6Test#$name"
        $raw = @(& $adb -s $Serial shell am instrument -w -r -e class $test $runner)
        $rc = $LASTEXITCODE
        $raw | Select-Object -Last 48
        if ($rc -ne 0 -or !($raw -match 'OK \(1 test\)')) {
            throw "V9_UPGRADE_FAILED_STAGE=$name"
        }
        Write-Output "V9_SIGNED_UPGRADE_PHASE_PASS=$name"
    }
    InstallApk $oldApk
    InstallApk 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
    TestPhase 'seedSignedV5PlaylistFavoriteAndRealSafGrant'
    InstallApk $newApk
    TestPhase 'verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly'
    Write-Output 'V9_SIGNED_UPGRADE_V8_TO_V9_ROOM_FAVORITES_SAF=PASS'
} finally {
    Pop-Location
}
