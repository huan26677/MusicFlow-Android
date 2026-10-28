$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
$logFile = Join-Path $root "qa-logs\real-picker-saf-v1.log"
New-Item -ItemType Directory -Force -Path (Split-Path $logFile) | Out-Null
Set-Content $logFile "MusicFlow Real OpenMultipleDocuments SAF v1" -Encoding UTF8

function Write-Log([string]$message) {
    Write-Host $message
    Add-Content $logFile $message -Encoding UTF8
}

function Run-Gradle([string]$name, [string[]]$argsList) {
    & .\gradlew.bat @argsList 2>&1 |
        ForEach-Object { Write-Log $_.ToString() }
    $code = $LASTEXITCODE
    Write-Log "RESULT $name exitCode=$code"
    return ($code -eq 0)
}

Push-Location $root
$passed = $false
try {
    Write-Log "DATE=$(Get-Date -Format o)"
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
        elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
        else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    $adb = if ($adbCmd) { $adbCmd.Source }
        else { Join-Path $sdk "platform-tools\adb.exe" }
    if (!(Test-Path $adb)) { throw "ADB_NOT_FOUND" }

    $devices = @(
        & $adb devices |
            Where-Object { $_ -match '^emulator-\d+\s+device\s*$' } |
            ForEach-Object { ($_ -split '\s+')[0] }
    )
    if ($devices.Count -eq 0) {
        $emulator = Join-Path $sdk "emulator\emulator.exe"
        if (!(Test-Path $emulator)) { throw "EMULATOR_NOT_FOUND" }
        $avds = @(& $emulator -list-avds)
        if ($avds -notcontains "Pixel_5_API_30") { throw "PIXEL_5_API_30_AVD_NOT_FOUND" }
        Start-Process $emulator -ArgumentList @("-avd", "Pixel_5_API_30")
    }

    $serial = $null
    for ($i = 0; $i -lt 120; $i++) {
        $devices = @(
            & $adb devices |
                Where-Object { $_ -match '^emulator-\d+\s+device\s*$' } |
                ForEach-Object { ($_ -split '\s+')[0] }
        )
        if ($devices.Count -gt 1) { throw "MULTIPLE_EMULATORS" }
        if ($devices.Count -eq 1) {
            $boot = (& $adb -s $devices[0] shell getprop sys.boot_completed | Out-String).Trim()
            if ($boot -eq "1") { $serial = $devices[0]; break }
        }
        Start-Sleep -Seconds 2
    }
    if (!$serial) { throw "EMULATOR_NOT_READY" }
    Write-Log "EMULATOR=$serial"
    $api = (& $adb -s $serial shell getprop ro.build.version.sdk | Out-String).Trim()
    Write-Log "ANDROID_API_LEVEL=$api"
    if ($api -ne "30") { throw "REQUIRES_ANDROID_API_30" }

    & $adb -s $serial logcat -c 2>$null
    $jvmOk = Run-Gradle "JVM_TESTS" @(':app:testDebugUnitTest', '--console=plain')
    $androidOk = $false
    if ($jvmOk) {
        Write-Log "INTERACTION_MODE=MANUAL_ASSISTED"
    $androidOk = Run-Gradle "ANDROID_TESTS" @(':app:connectedDebugAndroidTest', '--console=plain')
    }

    $logs = @(
        & $adb -s $serial logcat -d -v brief -s `
            'MusicFlowMvpE2E:I' 'MusicFlowFavoritesQA:I' 'MusicFlowBackgroundQA:I' `
            'MusicFlowSystemV2QA:I' 'MusicFlowRealPickerQA:I' `
            'AndroidRuntime:E' '*:S' 2>&1
    )
    $logs | ForEach-Object { Write-Log $_.ToString() }

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
        'PASS realPickerPlayback', 'PASS realPickerPersistedRead',
        'PASS multiSourcesPrepared', 'PASS multiQueue',
        'PASS multiPersistedRead', 'PASS multiNext',
        'PASS multiNextWrittenHistory'
    )
    $markersOk = $true
    foreach ($marker in $markers) {
        $seen = @($logs | Where-Object { $_.Contains($marker) }).Count -gt 0
        Write-Log "MARKER $marker=$seen"
        if (!$seen) { $markersOk = $false }
    }

    $text = Get-Content $logFile -Raw
    $start = [regex]::Match($text, 'Starting\s+(\d+)\s+tests\s+on')
    $finish = [regex]::Match($text, 'Finished\s+(\d+)\s+tests\s+on')
    $startCount = if ($start.Success) { [int]$start.Groups[1].Value } else { -1 }
    $finishCount = if ($finish.Success) { [int]$finish.Groups[1].Value } else { -1 }
    Write-Log "REGRESSION_START_COUNT=$startCount"
    Write-Log "REGRESSION_FINISHED_COUNT=$finishCount"

    $phaseTests = @('cleanupSafDocument', 'prepareSafDocument', 'verifySafAfterForceStop')
    $skipsOk = $true
    foreach ($method in $phaseTests) {
        $skipped = $text -match "SafPermissionRestartTest > $method.*SKIPPED"
        Write-Log "PHASE_TEST_SKIP_${method}=$skipped"
        if (!$skipped) { $skipsOk = $false }
    }
    $passed = $jvmOk -and $androidOk -and $startCount -eq 52 -and
        $finishCount -eq 55 -and $skipsOk -and $markersOk
}
catch {
    Write-Log "ERROR=$($_.Exception.Message)"
}
finally {
    Write-Log "FINAL_RESULT=$(if ($passed) { 'PASS' } else { 'FAIL' })"
    Pop-Location
}
if (!$passed) { exit 1 }
exit 0
