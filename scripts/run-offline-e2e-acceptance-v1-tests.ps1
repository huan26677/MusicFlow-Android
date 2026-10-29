param([string]$DeviceId = "emulator-5554")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\offline-e2e-acceptance-v1.log"

function Log([string]$message) {
    Write-Host $message
    Add-Content -LiteralPath $log -Value $message -Encoding UTF8
}

function Native([string]$exe, [string[]]$arguments) {
    $before = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = @(& $exe @arguments 2>&1)
        $exit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $before }
    foreach ($line in $output) { Log ([string]$line) }
    return [pscustomobject]@{
        Exit = $exit
        Output = ($output -join [char]10)
    }
}

function Check-Instrumentation(
    [string]$label,
    [string]$classes,
    [int]$expected
) {
    $result = Native "adb" @(
        "-s", $DeviceId, "shell", "am", "instrument",
        "-w", "-e", "class", $classes,
        "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"
    )
    Log ($label + "_EXIT=" + $result.Exit)
    if ($result.Exit -ne 0 -or
        $result.Output -notmatch [regex]::Escape("OK ($expected tests)") -or
        $result.Output -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "FAILED_$label"
    }
    Log ($label + "_TESTS=" + $expected)
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Offline E2E Acceptance v1" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Log "DEVICE=$DeviceId"
    Log "GUEST_OFFLINE_NO_SUPABASE_OR_RIGHTS_CHECK=TRUE"

    $build = Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Log "BUILD_JVM_EXIT=$($build.Exit)"
    if ($build.Exit -ne 0 -or $build.Output -notmatch "BUILD SUCCESSFUL") {
        throw "GRADLE_OR_JVM_FAILED"
    }
    if ((Native "adb" @("-s", $DeviceId, "get-state")).Exit -ne 0) {
        throw "ANDROID_DEVICE_NOT_AVAILABLE"
    }
    $apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    $testApk = Join-Path $root "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    if ((Native "adb" @("-s", $DeviceId, "install", "-r", $apk)).Exit -ne 0) {
        throw "APP_INSTALL_FAILED"
    }
    if ((Native "adb" @("-s", $DeviceId, "install", "-r", $testApk)).Exit -ne 0) {
        throw "TEST_INSTALL_FAILED"
    }

    $null = Native "adb" @("-s", $DeviceId, "logcat", "-c")

    Check-Instrumentation "OFFLINE_CORE" (
        "com.example.musicflow.core.playback.OfflineAcceptanceFlowTest," +
        "com.example.musicflow.core.playlist.PlaylistPlaybackOrderTest," +
        "com.example.musicflow.core.playback.QueueManagerIntegrationTest," +
        "com.example.musicflow.core.playback.PlaybackQueuePersistenceTest"
    ) 10

    $diagnostics = Native "adb" @(
        "-s", $DeviceId, "logcat", "-d", "-v", "brief",
        "-s", "MusicFlowOfflineE2E:I", "*:S"
    )
    $markers = @(
        "mediaStoreScanner",
        "localPlaylistPersisted",
        "offlineMediaSessionPlaying",
        "offlineHistoryFirst",
        "offlineNextTrack",
        "offlineHistoryOrder",
        "offlineQueueSnapshot",
        "offlineQueueRestoredPaused",
        "offlinePlaylistUnchanged"
    )
    foreach ($marker in $markers) {
        $found = $diagnostics.Output.Contains("PASS $marker")
        Log "MARKER_$marker=$found"
        if (-not $found) { throw "MISSING_OFFLINE_MARKER_$marker" }
    }
    Log "OFFLINE_E2E_MARKERS=9"

    # Separate instrumentation session to isolate UI navigation regressions.
    Check-Instrumentation "SERVICE_UI" (
        "com.example.musicflow.core.playback.PlaybackServiceIntegrationTest"
    ) 3

    Check-Instrumentation "MEDIA_COMPAT" (
        "com.example.musicflow.core.media.TrackMediaItemsTest"
    ) 9

    $diff = Native "git" @("diff", "--check")
    Log "GIT_DIFF_CHECK_EXIT=$($diff.Exit)"
    if ($diff.Exit -ne 0) { throw "GIT_DIFF_CHECK_FAILED" }
    Log "TOTAL_ANDROID_TESTS=22"
    Log "FINAL_RESULT=PASS"
} catch {
    Log "FAIL_REASON=$($_.Exception.Message)"
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally {
    Pop-Location
}
