param(
    [string]$DeviceId = "emulator-5554",
    [switch]$PackageOnly
)

# MusicFlow RC QA v1: release compilation, debug-key QA signing, APK provenance,
# and (unless PackageOnly) fresh-install + upgrade smoke on a dedicated emulator.
# NEVER produces a production-signed release and NEVER uninstalls a pre-existing app.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\release-candidate-packaging-v1.log"
$work = Join-Path $root "app\build\rc-qa"
$package = "com.example.musicflow"
$markerPath = "/data/local/tmp/musicflow-rc-qa-marker"
$installedByQa = $false
$markerPushedByQa = $false
$passed = $false

New-Item -ItemType Directory -Force -Path (Split-Path $log) | Out-Null
Set-Content -LiteralPath $log -Value "MusicFlow RC Packaging v1 - INTERNAL QA ONLY" -Encoding UTF8

function Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function Native([string]$step, [string]$command, [string[]]$arguments, [switch]$AllowFailure) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = @(& $command @arguments 2>&1 | ForEach-Object { [string]$_ })
        $exitCode = [int]$LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
    foreach ($line in $output) { Log "$step $line" }
    Log "${step}_EXIT=$exitCode"
    if (!$AllowFailure -and $exitCode -ne 0) { throw "${step}_FAILED_EXIT_$exitCode" }
    return $output
}

function SignerDigest([string]$signer, [string]$apk, [string]$phase) {
    $lines = @(Native $phase $signer @("verify", "--print-certs", $apk))
    $matchLine = $lines | Where-Object { $_ -match 'certificate SHA-256 digest: ([0-9a-f]+)' } |
        Select-Object -First 1
    if (!$matchLine -or $matchLine -notmatch 'certificate SHA-256 digest: ([0-9a-f]+)') {
        throw "NO_${phase}_SIGNER_DIGEST"
    }
    return $Matches[1].ToLowerInvariant()
}

function AppPresent([string]$adb) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $installed = @(& $adb -s $DeviceId shell pm path $package 2>&1)
        $code = [int]$LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
    return ($code -eq 0 -and @($installed | Where-Object {
        [string]$_ -match '^package:'
    }).Count -gt 0)
}

function LaunchApp([string]$adb, [string]$phase) {
    $result = @(Native "${phase}_LAUNCH" $adb @(
        "-s", $DeviceId, "shell", "am", "start", "-W",
        "-n", "$package/.MainActivity"
    ))
    if (!($result -match 'Status: ok')) { throw "${phase}_ACTIVITY_NOT_LAUNCHED" }
    $pidLines = @(Native "${phase}_PID" $adb @(
        "-s", $DeviceId, "shell", "pidof", $package
    ))
    if (!($pidLines -match '^\d+')) { throw "${phase}_PROCESS_NOT_RUNNING" }
}

Push-Location $root
try {
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse HEAD)"
    Log "QA_SIGNATURE=LOCAL_ANDROID_DEBUG_KEY_NOT_FOR_DISTRIBUTION"
    Log "RELEASE_SIGNING_CONFIG=NOT_CONFIGURED"

    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
        $env:ANDROID_SDK_ROOT
    } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $buildTools = Get-ChildItem (Join-Path $sdk "build-tools") -Directory |
        Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
    if (!$buildTools) { throw "NO_ANDROID_BUILD_TOOLS" }
    $signer = Join-Path $buildTools.FullName "apksigner.bat"
    $zipalign = Join-Path $buildTools.FullName "zipalign.exe"
    $aapt = Join-Path $buildTools.FullName "aapt.exe"
    $adb = Join-Path $sdk "platform-tools\adb.exe"
    $debugKey = Join-Path $env:USERPROFILE ".android\debug.keystore"
    foreach ($path in @($signer, $zipalign, $aapt, $debugKey, $adb)) {
        if (!(Test-Path -LiteralPath $path)) { throw "MISSING_REQUIRED_TOOL_OR_DEBUG_KEY" }
    }

    $devices = @(Native "DEVICES" $adb @("devices", "-l"))
    $connected = @($devices | Where-Object { $_ -match "^$([regex]::Escape($DeviceId))\s+device(?:\s|$)" })
    if ($connected.Count -ne 1) { throw "EXPECTED_QA_EMULATOR" }
    $apiLines = @(Native "DEVICE_API" $adb @("-s", $DeviceId, "shell", "getprop", "ro.build.version.sdk"))
    if (!($apiLines -contains "30")) { throw "EXPECTED_ANDROID_API_30" }
    $bootLines = @(Native "DEVICE_BOOT" $adb @("-s", $DeviceId, "shell", "getprop", "sys.boot_completed"))
    if (!($bootLines -contains "1")) { throw "EMULATOR_NOT_BOOTED" }

    $build = @(Native "GRADLE_BUILD" ".\gradlew.bat" @(
        ":app:assembleRelease", ":app:assembleDebug", "--console=plain"
    ))
    Log "GRADLE_RELEASE_AND_DEBUG_PASS=TRUE"
    $unsigned = Join-Path $root "app\build\outputs\apk\release\app-release-unsigned.apk"
    $debug = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    if (!(Test-Path -LiteralPath $unsigned) -or !(Test-Path -LiteralPath $debug)) {
        throw "EXPECTED_RELEASE_AND_DEBUG_APKS"
    }
    New-Item -ItemType Directory -Force -Path $work | Out-Null

    $aaptLines = @(Native "BADGING" $aapt @("dump", "badging", $unsigned))
    $badge = $aaptLines | Where-Object { $_ -match "package: name='com.example.musicflow' versionCode='1' versionName='1.0'" } |
        Select-Object -First 1
    if (!$badge -or !($aaptLines -match "sdkVersion:'29'") -or
        !($aaptLines -match "targetSdkVersion:'36'")) { throw "UNEXPECTED_RELEASE_MANIFEST" }
    if ($aaptLines -match '^application-debuggable') { throw "RELEASE_MODE_MUST_NOT_BE_DEBUGGABLE" }
    Log "PACKAGE_VERSION_AND_SDK=PASS"
    Log "RELEASE_DEBUGGABLE=FALSE"

    # Verify absence of distribution signing; the raw release APK is not installable.
    $unsignedVerify = @(Native "UNSIGNED_CHECK" $signer @(
        "verify", $unsigned
    ) -AllowFailure)
    if ($unsignedVerify -match '^Verifies$') { throw "UNEXPECTED_SIGNED_RELEASE_ARTIFACT" }
    Log "UNSIGNED_RELEASE_CONFIRMED=TRUE"

    $aligned = Join-Path $work "app-release-aligned.apk"
    $qa = Join-Path $work "MusicFlow-1.0-qa-signed.apk"
    Native "ZIPALIGN" $zipalign @("-f", "-p", "4", $unsigned, $aligned) | Out-Null
    Native "QA_SIGN" $signer @(
        "sign", "--ks", $debugKey, "--ks-key-alias", "androiddebugkey",
        "--ks-pass", "pass:android", "--key-pass", "pass:android",
        "--out", $qa, $aligned
    ) | Out-Null
    $verify = @(Native "QA_VERIFY" $signer @("verify", "--verbose", "--print-certs", $qa))
    if (!($verify -match '^Verifies$') -or
        !($verify -match 'Verified using v3 scheme .*true')) { throw "QA_SIGNATURE_INVALID" }

    $qaSigner = SignerDigest $signer $qa "QA_CERT"
    $debugSigner = SignerDigest $signer $debug "DEBUG_CERT"
    if ($qaSigner -ne $debugSigner) { throw "QA_SIGNER_DOES_NOT_MATCH_DEBUG_BASELINE" }
    Log "QA_AND_DEBUG_SIGNATURE_MATCH=TRUE"
    $sha = (Get-FileHash -LiteralPath $qa -Algorithm SHA256).Hash
    Log "QA_APK_SHA256=$sha"
    Log "QA_APK_SIZE=$((Get-Item -LiteralPath $qa).Length)"
    Log "QA_APK_PATH=$qa"
    Log "PRODUCTION_RELEASE_APPROVED=FALSE"

    if ($PackageOnly) {
        Log "INSTALL_SMOKE=SKIPPED_PACKAGE_ONLY"
    } else {
        $parallelQa = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
            Where-Object {
                $_.CommandLine -and
                $_.CommandLine -match 'connectedDebugAndroidTest|run-full-regression-v2-part1'
            })
        if ($parallelQa.Count -gt 0) { throw "PARALLEL_INSTRUMENTATION_PRESENT" }
        # Never remove/replace an app already present when this script starts.
        # The only packages uninstalled below were installed by this QA run.
        if (AppPresent $adb) { throw "PREEXISTING_APP_PRESENT_USE_PACKAGE_ONLY" }

        # First: clean-install a debuggable baseline and put a tiny fixture in
        # internal app storage. It contains no user data or credentials.
        Native "BASELINE_INSTALL" $adb @("-s", $DeviceId, "install", $debug) | Out-Null
        $installedByQa = $true
        LaunchApp $adb "BASELINE"
        $marker = "MUSICFLOW_RC_QA_$($sha.Substring(0,16))"
        $localMarker = Join-Path $work "qa-marker.txt"
        [System.IO.File]::WriteAllText($localMarker, $marker, [System.Text.Encoding]::ASCII)
        Native "MARKER_PUSH" $adb @("-s", $DeviceId, "push", $localMarker, $markerPath) | Out-Null
        $markerPushedByQa = $true
        Native "MARKER_DIR" $adb @(
            "-s", $DeviceId, "shell", "run-as", $package, "mkdir", "-p", "files"
        ) | Out-Null
        Native "MARKER_CREATE" $adb @(
            "-s", $DeviceId, "shell", "run-as", $package, "cp",
            $markerPath, "files/rc-qa-marker"
        ) | Out-Null
        $before = @(Native "MARKER_BEFORE" $adb @(
            "-s", $DeviceId, "shell", "run-as", $package, "cat", "files/rc-qa-marker"
        ))
        if (!(($before -join "").Contains($marker))) { throw "BASELINE_MARKER_NOT_STORED" }

        # Upgrade in place to the exact release-mode APK signed for QA.
        Native "UPGRADE_INSTALL" $adb @("-s", $DeviceId, "install", "-r", $qa) | Out-Null
        Native "UPGRADE_STOP" $adb @(
            "-s", $DeviceId, "shell", "am", "force-stop", $package
        ) | Out-Null
        LaunchApp $adb "UPGRADED_RELEASE"
        # Restore the debuggable baseline *without clearing data* so run-as
        # can verify the same internal marker survived the release upgrade.
        Native "BASELINE_RESTORE" $adb @("-s", $DeviceId, "install", "-r", $debug) | Out-Null
        $after = @(Native "MARKER_AFTER" $adb @(
            "-s", $DeviceId, "shell", "run-as", $package, "cat", "files/rc-qa-marker"
        ))
        if (!(($after -join "").Contains($marker))) { throw "DATA_LOST_DURING_UPGRADE" }
        Log "UPGRADE_INTERNAL_DATA_MARKER_PERSISTED=PASS"

        # A separate clean install of the release-mode QA APK. Only the QA
        # baseline from above is removed; the initial state had no installed app.
        Native "BASELINE_UNINSTALL" $adb @("-s", $DeviceId, "uninstall", $package) | Out-Null
        $installedByQa = $false
        Native "CLEAN_RELEASE_INSTALL" $adb @("-s", $DeviceId, "install", $qa) | Out-Null
        $installedByQa = $true
        LaunchApp $adb "CLEAN_RELEASE"
        Log "CLEAN_INSTALL_AND_LAUNCH=PASS"
        Native "CLEAN_RELEASE_UNINSTALL" $adb @("-s", $DeviceId, "uninstall", $package) | Out-Null
        $installedByQa = $false
        if (AppPresent $adb) { throw "APP_LEFT_INSTALLED_AFTER_QA" }
        Log "EMULATOR_PREEXISTING_APP_STATE_RESTORED=PASS"
    }
    $passed = $true
} catch {
    Log ("FAIL_REASON=" + $_.Exception.Message)
} finally {
    if ($installedByQa) {
        try {
            # Safe because this app was absent at start and installed by QA.
            & $adb -s $DeviceId uninstall $package 2>&1 | Out-Null
            Log "QA_INSTALLED_APP_CLEANUP_ATTEMPTED=TRUE"
        } catch { Log "QA_INSTALLED_APP_CLEANUP_FAILED" }
    }
    try {
        if ($markerPushedByQa -and $adb -and (Test-Path -LiteralPath $adb)) {
            & $adb -s $DeviceId shell rm -f $markerPath 2>&1 | Out-Null
        }
    } catch { Log "QA_TEMP_MARKER_CLEANUP_FAILED" }
    Pop-Location
}
Log "FINAL_RESULT=$(if ($passed) { 'PASS' } else { 'FAIL' })"
if (!$passed) { exit 1 }
exit 0
