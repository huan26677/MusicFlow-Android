$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
$logFile = Join-Path $root "qa-logs\\background-controls-v2.log"

Set-Content $logFile "MusicFlow Background Controls v2" -Encoding UTF8

function Write-Log([string]$message) {
    Write-Host $message
    Add-Content $logFile $message -Encoding UTF8
}

function Run-Gradle([string]$name, [string[]]$argsList) {
    & .\\gradlew.bat @argsList 2>&1 |
        ForEach-Object { Write-Log $_.ToString() }

    $code = $LASTEXITCODE
    Write-Log "RESULT $name exitCode=$code"
    return ($code -eq 0)
}

Push-Location $root
$passed = $false

try {
    Write-Log "DATE=$(Get-Date -Format o)"

    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
        elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
        else { Join-Path $env:LOCALAPPDATA "Android\\Sdk" }

    $adb = if ($adbCommand) { $adbCommand.Source }
        else { Join-Path $sdk "platform-tools\\adb.exe" }

    if (!(Test-Path $adb)) {
        throw "ADB_NOT_FOUND"
    }

    $devices = @(
        & $adb devices |
            Where-Object { $_ -match '^emulator-\d+\s+device\s*$' } |
            ForEach-Object { ($_ -split '\s+')[0] }
    )

    if ($devices.Count -eq 0) {
        $emulator = Join-Path $sdk "emulator\\emulator.exe"
        if (!(Test-Path $emulator)) {
            throw "EMULATOR_NOT_FOUND"
        }

        $avds = @(& $emulator -list-avds)
        if ($avds -notcontains "Pixel_5") {
            throw "PIXEL_5_AVD_NOT_FOUND"
        }

        Start-Process $emulator -ArgumentList @("-avd", "Pixel_5")
    }

    $serial = $null

    for ($i = 0; $i -lt 120; $i++) {
        $devices = @(
            & $adb devices |
                Where-Object { $_ -match '^emulator-\d+\s+device\s*$' } |
                ForEach-Object { ($_ -split '\s+')[0] }
        )

        if ($devices.Count -gt 1) {
            throw "MULTIPLE_EMULATORS"
        }

        if ($devices.Count -eq 1) {
            $boot = @(
                & $adb -s $devices[0] shell getprop sys.boot_completed
            )

            if (($boot -join "").Trim() -eq "1") {
                $serial = $devices[0]
                break
            }
        }

        Start-Sleep -Seconds 2
    }

    if (!$serial) { throw "EMULATOR_NOT_READY" }

    Write-Log "EMULATOR=$serial"
    & $adb -s $serial logcat -c 2>$null

    $jvmOk = Run-Gradle "JVM_TESTS" @(
        ":app:testDebugUnitTest", "--console=plain"
    )

    $androidOk = $false

    if ($jvmOk) {
        $androidOk = Run-Gradle "ANDROID_TESTS" @(
            ":app:connectedDebugAndroidTest", "--console=plain"
        )
    }

    $diagnostics = @(
        & $adb -s $serial logcat -d -v brief -s `
            "MusicFlowMvpE2E:I" `
            "MusicFlowFavoritesQA:I" `
            "MusicFlowBackgroundQA:I" `
            "MusicFlowSystemV2QA:I" `
            "AndroidRuntime:E" "*:S" 2>&1
    )

    $diagnostics | ForEach-Object {
        Write-Log $_.ToString()
    }

    $markers = @(
        "PASS libraryUiToMediaSession",
        "PASS nextPreviousFromUi",
        "PASS pauseResumeFromUi",
        "PASS searchUiToMediaSession",
        "PASS uiPlaybackWritesHistory",
        "PASS realMediaStoreScan",
        "PASS realMediaStoreUiPlayback",
        "PASS pickerIntentIntercepted",
        "PASS pickerResultPlaysThroughService",
        "PASS favoritesPersistence",
        "PASS favoriteLibraryAndAlbum",
        "PASS safFavoriteWithoutMediaStorePermission",
        "PASS homeFavoritesShortcuts",
        "PASS themePersistsAfterRestart",
        "PASS backgroundSurvivesActivityStop",
        "PASS mediaNotificationVisible",
        "PASS survivesActivityDestroy",
        "PASS controllerControlsWithoutUi",
        "PASS notificationButtonCommands",
        "PASS transientAudioFocusLoss"
    )

    $markersOk = $true

    foreach ($marker in $markers) {
        $found = @(
            $diagnostics |
                Where-Object { $_.Contains($marker) }
        ).Count -gt 0

        Write-Log "MARKER $marker=$found"

        if (!$found) { $markersOk = $false }
    }

    $logText = Get-Content $logFile -Raw
    $match = [regex]::Match(
        $logText, 'Finished\\s+(\\d+)\\s+tests\\s+on'
    )

    $count = if ($match.Success) {
        [int]$match.Groups[1].Value
    } else {
        -1
    }

    Write-Log "ANDROID_TEST_COUNT=$count"

    $passed = (
        $jvmOk -and
        $androidOk -and
        $count -eq 47 -and
        $markersOk
    )
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