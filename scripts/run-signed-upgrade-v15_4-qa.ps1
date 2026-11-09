# Check QA-signed APK v15.3 -> v15.4 with actual Room/favorite/SAF WAV data.
# ONLY an independently created MusicFlow_SignedUXUpgradeV15_4_QA_API36 AVD.
# Do not use personal emulators or real phones; never clear/uninstall app data.
param([Parameter(Mandatory = $true)][string]$Serial)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$signer = Join-Path $sdk 'build-tools\36.0.0\apksigner.bat'
$runner = 'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner'
$oldApk = 'dist/MusicFlow-1.0-Demo-UATReadyV15_3-Android.apk'
$newApk = 'dist/MusicFlow-1.0-Demo-FinalUXBatchV15_4-Android.apk'
$testApk = 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
$oldHash = '92B176675344964A3130D9FBABD084C8E1D77CBE42742A88285B97B2F62C2BE0'
if ($Serial -notmatch '^emulator-\d+$') { throw 'REFUSE_NON_EMULATOR_SERIAL' }
Push-Location $root
try {
    foreach ($path in @($adb,$signer,$oldApk,$newApk,$testApk,($newApk + '.sha256'))) {
        if (!(Test-Path $path -PathType Leaf)) { throw "MISSING_QA_FILE=$path" }
    }
    $avd = @(& $adb -s $Serial emu avd name)
    if ($LASTEXITCODE -ne 0 -or !($avd -match '^MusicFlow_SignedUXUpgradeV15_4_QA_API36$')) {
        throw 'REFUSE_UNOWNED_OR_PERSONAL_EMULATOR'
    }
    $boot = (@(& $adb -s $Serial shell getprop sys.boot_completed) -join '').Trim()
    $api = (@(& $adb -s $Serial shell getprop ro.build.version.sdk) -join '').Trim()
    if ($boot -ne '1' -or $api -ne '36') { throw 'QA_EMULATOR_NOT_BOOTED_API36' }
    $installed = @(& $adb -s $Serial shell pm path com.example.musicflow)
    if ($installed -match 'package:') { throw 'QA_UPGRADE_REQUIRES_FRESH_UNINSTALLED_AVD' }
    if ((Get-FileHash $oldApk -Algorithm SHA256).Hash -ne $oldHash) {
        throw 'OLD_SIGNED_APK_HASH_MISMATCH'
    }
    $newHash = (Get-FileHash $newApk -Algorithm SHA256).Hash
    $hashFile = (Get-Content ($newApk+'.sha256') -Raw -Encoding ASCII).Trim()
    if (!$hashFile.StartsWith($newHash+'  ')) { throw 'NEW_SIGNED_APK_HASH_MISMATCH' }
    $certs = @()
    foreach ($apk in @($oldApk,$newApk)) {
        $result = @(& $signer verify --print-certs $apk)
        if ($LASTEXITCODE -ne 0) { throw "SIGNATURE_INVALID=$apk" }
        $row = $result | Where-Object {
            $_ -match '^Signer #1 certificate SHA-256 digest:'
        } | Select-Object -First 1
        if (!$row) { throw "NO_SIGNER=$apk" }
        $certs += (($row -split ':\s+',2)[1].Trim())
    }
    if ($certs[0] -ne $certs[1]) { throw 'QA_APKS_HAVE_DIFFERENT_SIGNERS' }
    Write-Output 'SIGNED_APK_HASH_AND_SAME_SIGNER=PASS'
    function InstallQa([string]$apk) {
        $out = @(& $adb -s $Serial install -r $apk)
        if ($LASTEXITCODE -ne 0 -or !($out -match '^Success$')) {
            throw "QA_APK_INSTALL_FAILED=$apk"
        }
    }
    function TestQa([string]$method) {
        $case = "com.example.musicflow.ui.SignedApkUpgradeV6Test#$method"
        $out = @(& $adb -s $Serial shell am instrument -w -r -e class $case $runner)
        if ($LASTEXITCODE -ne 0 -or !($out -match '^OK \(1 test\)$')) {
            $out | Select-Object -Last 50
            throw "QA_UPGRADE_PHASE_FAILED=$method"
        }
        Write-Output "QA_PHASE_$method=PASS"
    }
    InstallQa $oldApk
    InstallQa $testApk
    TestQa 'seedSignedV5PlaylistFavoriteAndRealSafGrant'
    # The sole state-preservation step: do not uninstall, clear or wipe app data.
    InstallQa $newApk
    TestQa 'verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly'
    Write-Output 'SIGNED_UPGRADE_V15_3_TO_V15_4_ROOM_FAVORITES_SAF_API36=PASS'
    Write-Output 'QA_ONLY_FIXTURES_CLEANED=PASS'
} finally {
    Pop-Location
}
