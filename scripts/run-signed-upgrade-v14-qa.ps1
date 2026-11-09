# MusicFlow signed QA v13 -> v14, Room/favorites/persisted SAF continuity.
# SAFE: only a fresh, disposable dedicated MusicFlow_CommunityV14_QA_API33/36 AVD.
# Never uninstall, clear app data or wipe an existing device. The instrumented
# fixture owns and cleans up only its own playlist, favorite and SAF WAV document.
param([Parameter(Mandatory = $true)][string]$Serial)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$signer = Join-Path $sdk 'build-tools\36.0.0\apksigner.bat'
$runner = 'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner'
$oldApk = 'dist/MusicFlow-1.0-Demo-FinalUXV13-Android.apk'
$newApk = 'dist/MusicFlow-1.0-Demo-CommunityV14-Android.apk'
$instrumentation = 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
$hashV13 = 'D74CD7F4FEEE8819FBE6528554AC8EBC60885EBE7182FBFC7EBC77F1762F9D3F'
$hashV14 = '3C2FFD95231A1AECBE9A76E21EE34A4BA56FF320CD18F14FA6B3E6904B6DD0BD'

if ($Serial -notmatch '^emulator-\d+$') { throw 'V14_UPGRADE_ONLY_ANDROID_QA_EMULATOR' }
Push-Location $root
try {
    if (!(Test-Path $adb) -or !(Test-Path $signer)) { throw 'V14_QA_ANDROID_SDK_MISSING' }
    foreach ($apk in @($oldApk,$newApk,$instrumentation)) {
        if (!(Test-Path $apk)) { throw "V14_QA_APK_MISSING=$apk" }
    }
    $devices = @(& $adb devices)
    if (-not ($devices -match ('^' + [regex]::Escape($Serial) + '\s+device$'))) {
        throw 'V14_UPGRADE_QA_AVD_NOT_CONNECTED'
    }
    $identity = @(& $adb -s $Serial emu avd name)
    $expectedApi = if ($identity -match '^MusicFlow_CommunityV14_QA_API33$') { '33' }
        elseif ($identity -match '^MusicFlow_CommunityV14_QA_API36$') { '36' }
        else { throw 'V14_REFUSE_ANY_OTHER_OR_PERSONAL_AVD' }
    $boot = (@(& $adb -s $Serial shell getprop sys.boot_completed) -join '').Trim()
    $api = (@(& $adb -s $Serial shell getprop ro.build.version.sdk) -join '').Trim()
    if ($boot -ne '1' -or $api -ne $expectedApi) {
        throw "V14_WRONG_QA_EMULATOR_API_OR_UNBOOTED=$api/$expectedApi"
    }
    $installed = @(& $adb -s $Serial shell pm path com.example.musicflow)
    if ($installed -match 'package:') {
        throw 'V14_UPGRADE_REQUIRES_FRESH_AVD_NO_MUSICFLOW_APP'
    }

    $actualOld = (Get-FileHash $oldApk -Algorithm SHA256).Hash
    $actualNew = (Get-FileHash $newApk -Algorithm SHA256).Hash
    if ($actualOld -ne $hashV13 -or $actualNew -ne $hashV14) {
        throw 'V14_QA_APK_HASH_MISMATCH'
    }
    foreach ($apk in @($oldApk,$newApk)) {
        $cert = @(& $signer verify --print-certs $apk)
        if ($LASTEXITCODE -ne 0 -or -not ($cert -match '^Signer #1 certificate SHA-256 digest:')) {
            throw "V14_QA_APK_SIGNATURE_NOT_VALID=$apk"
        }
        $digest = (($cert | Where-Object {
            $_ -match '^Signer #1 certificate SHA-256 digest:'
        } | Select-Object -First 1) -split ':\s+',2)[1].Trim()
        if (!$digest) { throw "V14_QA_APK_CERT_MISSING=$apk" }
        if ($apk -eq $oldApk) { $oldCert = $digest } else { $newCert = $digest }
    }
    if ($oldCert -ne $newCert) { throw 'V14_QA_APK_DIFFERENT_SIGNERS' }
    Write-Output "QA_AVD_NAME=$($identity[0])"
    Write-Output "QA_ANDROID_API=$expectedApi"
    Write-Output 'APK_V13_V14_HASH_AND_SIGNER=PASS'

    function InstallQa([string]$apk) {
        $output = @(& $adb -s $Serial install -r $apk)
        if ($LASTEXITCODE -ne 0 -or !($output -match '^Success$')) {
            throw "V14_QA_INSTALL_FAILED=$apk"
        }
    }
    function TestQa([string]$stage) {
        $case = "com.example.musicflow.ui.SignedApkUpgradeV6Test#$stage"
        $output = @(& $adb -s $Serial shell am instrument -w -r -e class $case $runner)
        $exitCode = $LASTEXITCODE
        if ($exitCode -ne 0 -or -not ($output -match '^OK \(1 test\)$')) {
            $output | Select-Object -Last 45
            throw "V14_UPGRADE_FAILED_STAGE=$stage"
        }
        Write-Output "QA_PHASE_$stage=PASS"
    }

    InstallQa $oldApk
    InstallQa $instrumentation
    TestQa 'seedSignedV5PlaylistFavoriteAndRealSafGrant'
    # No pm clear, no uninstall, no wipe. This is a real signed APK replacement.
    InstallQa $newApk
    TestQa 'verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly'
    Write-Output "V14_SIGNED_UPGRADE_V13_TO_V14_ROOM_FAVORITES_SAF_API$expectedApi=PASS"
    Write-Output 'QA_TEST_OWNED_FIXTURES_CLEANED=PASS'
} finally {
    Pop-Location
}
