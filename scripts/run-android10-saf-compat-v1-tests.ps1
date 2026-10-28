$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
$logFile = Join-Path $root "qa-logs\android10-saf-compat-v1.log"
New-Item -ItemType Directory -Force -Path (Split-Path $logFile) | Out-Null
Set-Content $logFile "MusicFlow Android 10 SAF Compatibility v1" -Encoding UTF8

function Write-Log([string]$message) {
    Write-Host $message
    Add-Content $logFile $message -Encoding UTF8
}

function Run-Gradle([string]$name, [string[]]$argsList) {
    & .\gradlew.bat @argsList 2>&1 | ForEach-Object { Write-Log $_.ToString() }
    $code = $LASTEXITCODE
    Write-Log "RESULT $name exitCode=$code"
    return ($code -eq 0)
}

$package = "com.example.musicflow"
$runner = "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"
$testClass = "com.example.musicflow.core.playback.SafPermissionRestartTest"

function Stop-App {
    & $adb -s $serial shell am force-stop $package 2>&1 |
        ForEach-Object { Write-Log $_.ToString() }
    Start-Sleep -Milliseconds 400
    $pidValue = (& $adb -s $serial shell pidof -s $package 2>$null | Out-String).Trim()
    return [string]::IsNullOrWhiteSpace($pidValue)
}

function Run-Phase([string]$name, [string]$method, [string]$phase) {
    Write-Log "=== PHASE_$name ==="
    $lines = @(
        & $adb -s $serial shell am instrument -w `
            -e class "$testClass#$method" `
            -e musicflow_saf_phase $phase $runner 2>&1 |
            ForEach-Object { $_.ToString() }
    )
    $code = $LASTEXITCODE
    foreach ($line in $lines) { Write-Log $line }
    $value = $lines -join "`n"
    $ok = $code -eq 0 -and $value -match 'OK \(1 test\)' -and
        $value -notmatch 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_STATUS: Error='
    Write-Log "PHASE_${name}_RESULT=$ok"
    return $ok
}

function Ensure-Installed {
    $appApk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    $testDir = Join-Path $root "app\build\outputs\apk\androidTest"
    $testApk = Get-ChildItem -Path $testDir -Filter "*.apk" -File -Recurse `
        -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (!(Test-Path $appApk) -or !$testApk) {
        Write-Log "ERROR=APK_NOT_FOUND"
        return $false
    }
    & $adb -s $serial install -r -t $appApk 2>&1 | ForEach-Object { Write-Log $_.ToString() }
    if ($LASTEXITCODE -ne 0) { return $false }
    & $adb -s $serial install -r -t $testApk.FullName 2>&1 |
        ForEach-Object { Write-Log $_.ToString() }
    if ($LASTEXITCODE -ne 0) { return $false }
    $list = (& $adb -s $serial shell pm list instrumentation 2>&1 | Out-String)
    return $list.Contains($runner)
}

function Find-SafPid([string]$marker) {
    $entries = @(& $adb -s $serial logcat -d -v brief -s "MusicFlowSafQA:I" "*:S")
    $line = $entries | Where-Object { $_ -match "$marker=(\d+)" } | Select-Object -Last 1
    if ($line -match "$marker=(\d+)") { return $matches[1] }
    return ""
}

Push-Location $root
$success = $false
try {
    Write-Log "DATE=$(Get-Date -Format o)"
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
        elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
        else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $adb = if ($adbCommand) { $adbCommand.Source }
        else { Join-Path $sdk "platform-tools\adb.exe" }
    if (!(Test-Path $adb)) { throw "ADB_NOT_FOUND" }

    $devices = @(& $adb devices | Where-Object { $_ -match '^emulator-\d+\s+device\s*$' } |
        ForEach-Object { ($_ -split '\s+')[0] })
    if ($devices.Count -eq 0) {
        $emulator = Join-Path $sdk "emulator\emulator.exe"
        if (!(Test-Path $emulator)) { throw "EMULATOR_NOT_FOUND" }
        $avds = @(& $emulator -list-avds)
        if ($avds -notcontains "Pixel_5_API_29") { throw "PIXEL_7_API_36_AVD_NOT_FOUND" }
        Start-Process $emulator -ArgumentList @("-avd", "Pixel_5_API_29")
    }

    $serial = $null
    for ($i = 0; $i -lt 120; $i++) {
        $devices = @(& $adb devices | Where-Object { $_ -match '^emulator-\d+\s+device\s*$' } |
            ForEach-Object { ($_ -split '\s+')[0] })
        if ($devices.Count -gt 1) { throw "MULTIPLE_EMULATORS" }
        if ($devices.Count -eq 1) {
            $boot = (& $adb -s $devices[0] shell getprop sys.boot_completed | Out-String).Trim()
            if ($boot -eq '1') { $serial = $devices[0]; break }
        }
        Start-Sleep -Seconds 2
    }
    if (!$serial) { throw "EMULATOR_NOT_READY" }
    Write-Log "EMULATOR=$serial"
    $apiLevel = (
        & $adb -s $serial shell getprop ro.build.version.sdk |
            Out-String
    ).Trim()
    Write-Log "ANDROID_API_LEVEL=$apiLevel"
    if ($apiLevel -ne "29") { throw "REQUIRES_ANDROID_API_29" }
    & $adb -s $serial logcat -c 2>$null

    $jvmOk = Run-Gradle "JVM_TESTS" @(':app:testDebugUnitTest', '--console=plain')
    $androidOk = $false
    if ($jvmOk) {
        $androidOk = Run-Gradle "ANDROID_TESTS" @(':app:connectedDebugAndroidTest', '--console=plain')
    }
    if (!$jvmOk -or !$androidOk) {
    Write-Log "=== ANDROID10_FAILURE_DIAGNOSTICS ==="

    $diagnostics = @(
        & $adb -s $serial logcat -d -v brief -s `
            'MusicFlowServiceIT:I' `
            'ActivityTaskManager:I' `
            'AndroidRuntime:E' `
            '*:S' 2>&1
    )

    foreach ($line in $diagnostics) {
        Write-Log $line.ToString()
    }

    throw "REGRESSION_FAILED"
}

    # The 3 phase-specific tests are skipped by JUnit Assume during the general suite.
    # Their real assertions are executed below in separate instrumented processes.
    $regressionText = Get-Content $logFile -Raw

    $startMatch = [regex]::Match(
        $regressionText,
        'Starting\s+(\d+)\s+tests\s+on'
    )

    $finishMatch = [regex]::Match(
        $regressionText,
        'Finished\s+(\d+)\s+tests\s+on'
    )

    $startCount = if ($startMatch.Success) {
        [int]$startMatch.Groups[1].Value
    } else {
        -1
    }

    $finishCount = if ($finishMatch.Success) {
        [int]$finishMatch.Groups[1].Value
    } else {
        -1
    }

    Write-Log "REGRESSION_START_COUNT=$startCount"
    Write-Log "REGRESSION_FINISHED_COUNT=$finishCount"

    if ($startCount -ne 55 -or $finishCount -ne 58) {
        throw "UNEXPECTED_REGRESSION_TEST_COUNT"
    }

    $phaseTests = @(
        'cleanupSafDocument',
        'prepareSafDocument',
        'verifySafAfterForceStop'
    )

    foreach ($method in $phaseTests) {
        if (
            $regressionText -notmatch
            "SafPermissionRestartTest > $method.*SKIPPED"
        ) {
            throw "EXPECTED_PHASE_SKIP_MISSING_$method"
        }
    }

    Write-Log "SAF_PHASE_TESTS_SKIPPED=3"
    if (!(Ensure-Installed)) { throw "INSTRUMENTATION_INSTALL_FAILED" }
    if (!(Stop-App)) { throw "INITIAL_STOP_FAILED" }
    if (!(Run-Phase 'INITIAL_CLEANUP' 'cleanupSafDocument' 'cleanup')) {
        throw "INITIAL_CLEANUP_FAILED"
    }
    if (!(Stop-App)) { throw "PREPARE_STOP_FAILED" }

    $prepared = $false
    $verified = $false
    $cleaned = $false
    $newProcess = $false
    try {
        $prepared = Run-Phase 'PREPARE' 'prepareSafDocument' 'prepare'
        $oldPid = Find-SafPid 'SAF_PREPARE_PID'
        Write-Log "OLD_PID=$oldPid"
        if (!(Stop-App)) { throw "FORCE_STOP_FAILED" }
        if ($prepared) {
            $verified = Run-Phase 'VERIFY' 'verifySafAfterForceStop' 'verify'
        }
        $newPid = Find-SafPid 'SAF_VERIFY_PID'
        Write-Log "NEW_PID=$newPid"
        $newProcess = $verified -and $oldPid -and $newPid -and ($oldPid -ne $newPid)
        Write-Log "PROCESS_RESTARTED=$newProcess"
    } finally {
        if (Stop-App) { $cleaned = Run-Phase 'CLEANUP' 'cleanupSafDocument' 'cleanup' }
    }

    $diagnostics = @(
        & $adb -s $serial logcat -d -v brief -s `
            'MusicFlowMvpE2E:I' 'MusicFlowFavoritesQA:I' 'MusicFlowBackgroundQA:I' `
            'MusicFlowSystemV2QA:I' 'MusicFlowSafQA:I' 'MusicFlowRealPickerQA:I' 'MusicFlowPlaylistUIQA:I' 'AndroidRuntime:E' '*:S' 2>&1
    )
    foreach ($line in $diagnostics) { Write-Log $line.ToString() }

    $markers = @(
        'PASS libraryUiToMediaSession', 'PASS nextPreviousFromUi',
        'PASS pauseResumeFromUi', 'PASS searchUiToMediaSession',
        'PASS uiPlaybackWritesHistory', 'PASS realMediaStoreScan',
        'PASS realMediaStoreUiPlayback', 'PASS pickerIntentIntercepted',
        'PASS pickerResultPlaysThroughService', 'PASS favoritesPersistence',
        'PASS favoriteLibraryAndAlbum', 'PASS safFavoriteWithoutMediaStorePermission',
        'PASS homeFavoritesShortcuts', 'PASS themePersistsAfterRestart',
        'PASS backgroundSurvivesActivityStop', 'PASS mediaNotificationVisible',
        'PASS survivesActivityDestroy', 'PASS controllerControlsWithoutUi',
        'PASS notificationButtonCommands', 'PASS transientAudioFocusLoss',
'PASS sourcePreparedWithoutPersistedRead',
'PASS realPickerPlayback',
'PASS realPickerPersistedRead',
'PASS multiSourcesPrepared',
'PASS multiQueue',
'PASS multiPersistedRead',
'PASS multiNext',
'PASS multiNextWrittenHistory',
'PASS playlistUiReorderAndPlayAll',
'PASS playlistUiSelectedTrack',
'PASS playlistUiCreate',
'PASS playlistUiRename',
'PASS playlistUiDelete',
'PASS mvpPlaylistNavigation',
'PASS mvpPlaylistPlayback',
'PASS mvpPlaylistNext',
'PASS mvpPlaylistFirstHistory',
'PASS mvpPlaylistHistoryOrder',
'PASS mvpPlaylistHistoryUi',
'PASS mvpHistoryReplay',
        'SAF_PREPARE_PASS', 'SAF_PERSISTED_READ_PASS',
        'SAF_FAVORITE_PLAYBACK_PASS', 'SAF_CLEANUP_PASS'
    )
    $markersOk = $true
    foreach ($marker in $markers) {
        $found = @($diagnostics | Where-Object { $_.Contains($marker) }).Count -gt 0
        Write-Log "MARKER $marker=$found"
        if (!$found) { $markersOk = $false }
    }
    $success = $prepared -and $verified -and $cleaned -and $newProcess -and $markersOk
    Write-Log "PHASE_PREPARE=$prepared PHASE_VERIFY=$verified PHASE_CLEANUP=$cleaned"
} catch {
    Write-Log "ERROR=$($_.Exception.Message)"
} finally {
    Write-Log "FINAL_RESULT=$(if ($success) { 'PASS' } else { 'FAIL' })"
    Pop-Location
}
if (!$success) { exit 1 }
exit 0

