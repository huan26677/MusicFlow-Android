param(
    [Parameter(Mandatory = $true)][string]$DeviceId,
    [Parameter(Mandatory = $true)][ValidateSet(29,30,33,36)][int]$ExpectedApi,
    [switch]$SkipSaf,
    [switch]$SkipRestart
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root ("qa-logs\android-compat-matrix-v2-api-" + $ExpectedApi + ".log")
$runner = "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"
$package = "com.example.musicflow"

function Log([string]$message) {
    Write-Host $message
    Add-Content -LiteralPath $log -Value $message -Encoding UTF8
}

function Invoke-DeviceAdb([string[]]$arguments) {
    $before = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = @(& adb -s $DeviceId @arguments 2>&1)
        $status = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $before }
    return [pscustomobject]@{
        Code = $status
        Text = ($output -join [char]10)
    }
}

function Instrument([string]$name,[string]$selector,[int]$count,[string[]]$extra) {
    $args = @("shell","am","instrument","-w","-e","class",$selector) +
        $extra + @($runner)
    $r = Invoke-DeviceAdb $args
    Log $r.Text
    Log ($name + "_EXIT=" + $r.Code)
    $unit = if ($count -eq 1) { "test" } else { "tests" }
    if ($r.Code -ne 0 -or
        !$r.Text.Contains("OK ($count $unit)") -or
        $r.Text -match "FAILURES|INSTRUMENTATION_FAILED|Error in") {
        throw ("FAILED_" + $name)
    }
    Log ($name + "_PASS=" + $count)
}

function ForceStop() {
    $result = Invoke-DeviceAdb @("shell","am","force-stop",$package)
    if ($result.Code -ne 0) { throw "FORCE_STOP_FAILED" }
    Start-Sleep -Milliseconds 450
    $runningPid = Invoke-DeviceAdb @("shell","pidof","-s",$package)
    if ($runningPid.Code -eq 0 -and $runningPid.Text.Trim()) {
        throw "PROCESS_STILL_RUNNING"
    }
}

function Phase([string]$name,[string]$class,[string]$method,[string]$flag,[string]$phase) {
    Instrument $name ($class + "#" + $method) 1 @("-e",$flag,$phase)
}

function LogMarkers([string]$tag,[string[]]$markers) {
    $response = Invoke-DeviceAdb @("logcat","-d","-v","brief","-s",($tag + ":I"),"*:S")
    foreach ($marker in $markers) {
        $ok = $response.Text.Contains($marker)
        Log ("MARKER_" + $marker + "=" + $ok)
        if (!$ok) { throw ("MISSING_MARKER_" + $marker) }
    }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Android Compatibility Matrix v2" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Log "DEVICE=$DeviceId EXPECTED_API=$ExpectedApi"
    $level = (Invoke-DeviceAdb @("shell","getprop","ro.build.version.sdk")).Text.Trim()
    $boot = (Invoke-DeviceAdb @("shell","getprop","sys.boot_completed")).Text.Trim()
    Log "ACTUAL_API=$level BOOT_COMPLETE=$boot"
    if ($level -ne [string]$ExpectedApi -or $boot -ne "1") {
        throw "DEVICE_OR_API_NOT_READY"
    }
    foreach ($apk in @(
        "app\build\outputs\apk\debug\app-debug.apk",
        "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    )) {
        $path = Join-Path $root $apk
        if (!(Test-Path $path)) { throw ("MISSING_APK_" + $apk) }
        $installed = Invoke-DeviceAdb @("install","-r",$path)
        Log ("INSTALL_" + $apk + "=" + $installed.Text)
        if ($installed.Code -ne 0 -or !$installed.Text.Contains("Success")) {
            throw ("INSTALL_FAILED_" + $apk)
        }
    }
    $null = Invoke-DeviceAdb @("logcat","-c")

    $core = "com.example.musicflow.core.playback.OfflineAcceptanceFlowTest," +
        "com.example.musicflow.core.media.TrackMediaItemsTest," +
        "com.example.musicflow.core.playback.PlaybackQueuePersistenceTest," +
        "com.example.musicflow.core.playlist.PlaylistPlaybackOrderTest"
    Instrument "OFFLINE_CORE" $core 18 @()
    LogMarkers "MusicFlowOfflineE2E" @(
        "PASS mediaStoreScanner",
        "PASS localPlaylistPersisted",
        "PASS offlineMediaSessionPlaying",
        "PASS offlineHistoryFirst",
        "PASS offlineNextTrack",
        "PASS offlineHistoryOrder",
        "PASS offlineQueueSnapshot",
        "PASS offlineQueueRestoredPaused",
        "PASS offlinePlaylistUnchanged"
    )

    if (!$SkipRestart) {
        $name = "com.example.musicflow.core.playback.PlaybackProcessRestartTest"
        $flag = "musicflow_restart_phase"
        ForceStop
        Phase "RESTART_INITIAL_CLEANUP" $name "cleanupAfterRestart" $flag "cleanup"
        ForceStop
        $prepared = $false
        try {
            Phase "RESTART_PREPARE" $name "prepareBeforeRestart" $flag "prepare"
            $prepared = $true
            $before = Invoke-DeviceAdb @("logcat","-d","-v","brief","-s","MusicFlowRestartIT:I","*:S")
            $firstPid = [regex]::Match($before.Text,"PREPARE_PROCESS_PID=(\d+)").Groups[1].Value
            ForceStop
            Phase "RESTART_VERIFY" $name "verifyAfterRestart" $flag "verify"
            $after = Invoke-DeviceAdb @("logcat","-d","-v","brief","-s","MusicFlowRestartIT:I","*:S")
            $newPid = [regex]::Match($after.Text,"VERIFY_PROCESS_PID=(\d+)").Groups[1].Value
            if (!$firstPid -or !$newPid -or $firstPid -eq $newPid) {
                throw "PROCESS_RESTART_NOT_VERIFIED"
            }
            Log "PROCESS_RESTART_NEW_PID=TRUE"
            LogMarkers "MusicFlowRestartIT" @(
                "PHASE_PREPARE_PASS","PHASE_VERIFY_PAUSED_PASS",
                "PHASE_VERIFY_PASS","PASS historyPersistedAfterRestart"
            )
        } finally {
            ForceStop
            Phase "RESTART_CLEANUP" $name "cleanupAfterRestart" $flag "cleanup"
        }
        Log "RESTART_PHASES=4"
    } else { Log "RESTART_PHASES=SKIPPED_BY_REQUEST" }

    if (!$SkipSaf) {
        $name = "com.example.musicflow.core.playback.SafPermissionRestartTest"
        $flag = "musicflow_saf_phase"
        ForceStop
        Phase "SAF_INITIAL_CLEANUP" $name "cleanupSafDocument" $flag "cleanup"
        ForceStop
        try {
            Phase "SAF_PREPARE" $name "prepareSafDocument" $flag "prepare"
            $before = Invoke-DeviceAdb @("logcat","-d","-v","brief","-s","MusicFlowSafQA:I","*:S")
            $firstPid = [regex]::Match($before.Text,"SAF_PREPARE_PID=(\d+)").Groups[1].Value
            ForceStop
            Phase "SAF_VERIFY" $name "verifySafAfterForceStop" $flag "verify"
            $after = Invoke-DeviceAdb @("logcat","-d","-v","brief","-s","MusicFlowSafQA:I","*:S")
            $newPid = [regex]::Match($after.Text,"SAF_VERIFY_PID=(\d+)").Groups[1].Value
            if (!$firstPid -or !$newPid -or $firstPid -eq $newPid) {
                throw "SAF_PROCESS_RESTART_NOT_VERIFIED"
            }
            Log "SAF_PROCESS_RESTART_NEW_PID=TRUE"
            LogMarkers "MusicFlowSafQA" @(
                "SAF_PREPARE_PASS","SAF_PERSISTED_READ_PASS",
                "SAF_FAVORITE_PLAYBACK_PASS"
            )
        } finally {
            ForceStop
            Phase "SAF_CLEANUP" $name "cleanupSafDocument" $flag "cleanup"
        }
        Log "SAF_PHASES=4"
    } else { Log "SAF_PHASES=SKIPPED_BY_REQUEST" }
    Log "FINAL_RESULT=PASS"
} catch {
    Log ("FAIL_REASON=" + $_.Exception.Message)
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally {
    Pop-Location
}
