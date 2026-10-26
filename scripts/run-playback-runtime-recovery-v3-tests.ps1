$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root 'qa-logs'
$logFile = Join-Path $logDir 'playback-runtime-recovery-v3.log'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
Set-Content -Path $logFile -Value 'MusicFlow Runtime Recovery Race Hardening v3' -Encoding utf8

function Write-Log([string]$message) {
    Write-Host $message
    $message | Out-File -FilePath $logFile -Encoding utf8 -Append
}

function Run-Gradle([string]$name, [string[]]$gradleArgs) {
    Write-Log "=== $name ==="
    & .\gradlew.bat @gradleArgs 2>&1 | ForEach-Object { Write-Log $_.ToString() }
    $code = $LASTEXITCODE
    Write-Log "RESULT $name exitCode=$code"
    return ($code -eq 0)
}

$exitCode = 1
$totalTests = -1
$head = 'UNAVAILABLE'
Push-Location $root
try {
    Write-Log "DATE=$(Get-Date -Format o)"
    # SHA ở đầu log có thể là commit trước khi kết quả QA được push.
    $head = (& git rev-parse --short HEAD 2>$null)
    Write-Log "COMMIT=$head"

    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($adb) { $adbPath = $adb.Source }
    else {
        $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
            elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
            else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
        $adbPath = Join-Path $sdk 'platform-tools\adb.exe'
    }

    if (!(Test-Path $adbPath)) {
        Write-Log 'ERROR=ADB_NOT_FOUND'
    } else {
$devices = @(
    & $adbPath devices |
        Where-Object {
            $_ -match '^emulator-\d+\s+device\s*$'
        } |
        ForEach-Object { ($_ -split '\s+')[0] }
)

$canWait = $true

if ($devices.Count -eq 0) {
    $sdkRoot = if ($env:ANDROID_HOME) {
        $env:ANDROID_HOME
    } elseif ($env:ANDROID_SDK_ROOT) {
        $env:ANDROID_SDK_ROOT
    } else {
        Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    }

    $emulatorExe = Join-Path $sdkRoot 'emulator\emulator.exe'

    if (!(Test-Path $emulatorExe)) {
        Write-Log "ERROR=EMULATOR_EXE_NOT_FOUND path=$emulatorExe"
        $canWait = $false
    } else {
        $avdNames = @(
            & $emulatorExe -list-avds 2>$null |
                ForEach-Object { $_.Trim() }
        )

        if ($avdNames -notcontains 'Pixel_5') {
            Write-Log "ERROR=PIXEL_5_AVD_NOT_FOUND available=$($avdNames -join ',')"
            $canWait = $false
        } else {
            Write-Log 'STARTING_AVD=Pixel_5'
            Start-Process -FilePath $emulatorExe `
                -ArgumentList @('-avd', 'Pixel_5') |
                Out-Null
        }
    }
}

$serial = $null

if ($canWait) {
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        $devices = @(
            & $adbPath devices |
                Where-Object {
                    $_ -match '^emulator-\d+\s+device\s*$'
                } |
                ForEach-Object { ($_ -split '\s+')[0] }
        )

        if ($devices.Count -gt 1) {
            Write-Log "ERROR=MULTIPLE_EMULATORS count=$($devices.Count)"
            break
        }

        if ($devices.Count -eq 1) {
            $bootResult = @(
                & $adbPath -s $devices[0] shell getprop sys.boot_completed 2>$null
            )

            if (($bootResult -join '').Trim() -eq '1') {
                $serial = $devices[0]
                break
            }
        }

        Start-Sleep -Seconds 2
    }
}

if ($null -eq $serial) {
    Write-Log 'ERROR=EMULATOR_NOT_READY'
} else {
    Write-Log "EMULATOR=$serial"
            & $adbPath -s $serial logcat -c 2>$null

            $jvmOk = Run-Gradle 'JVM_TESTS' @(':app:testDebugUnitTest', '--console=plain')
            $androidOk = $false
            if ($jvmOk) {
                $androidOk = Run-Gradle 'ANDROID_TESTS' @(':app:connectedDebugAndroidTest', '--console=plain')
            }

            Write-Log '=== RUNTIME_V3_DIAGNOSTICS ==='
            $diagnostics = @(
                & $adbPath -s $serial logcat -d -v brief -s `
                    'MusicFlowRaceV3:I' 'MusicFlowAutoSkipTest:I' `
                    'MusicFlowRuntimeV2:I' 'MusicFlowAutoSkip:I' `
                    'MusicFlowAutoSkip:W' '*:S' 2>&1 |
                    ForEach-Object { $_.ToString() }
            )
            foreach ($line in $diagnostics) { Write-Log $line }

            $newMarkers = @(
                'PASS pauseResumeCancelsStaleRecovery',
                'PASS identicalQueueCancelsStaleRecovery',
                'PASS sameIndexSeekCancelsStaleRecovery',
                'PASS disposeCancelsStaleRecovery',
                'PASS uninterruptedRecoveryStillSkips'
            )
            $oldMarkers = @(
                'PASS singleMissingAutoSkipped',
                'PASS consecutiveMissingAutoSkipped',
                'PASS allMissingStopsWithoutLoop',
                'PASS pausedPlayerDoesNotAutoSkip',
                'PASS injectedRecoveryBranches',
                'PASS realServiceExoPlayerErrorObserved',
                'PASS pendingPauseCancelsAutoSkip',
                'PASS replacedQueueCancelsAutoSkip'
            )
            $allMarkers = $true
            foreach ($marker in ($newMarkers + $oldMarkers)) {
                $found = @($diagnostics | Where-Object { $_.Contains($marker) }).Count -gt 0
                Write-Log "MARKER $marker=$found"
                if (!$found) { $allMarkers = $false }
            }

            $errorCount = @($diagnostics | Where-Object { $_ -match 'SERVICE_PLAYER_ERROR code=' }).Count
            $forwardedCount = @($diagnostics | Where-Object { $_ -match 'SERVICE_FORWARDED_ERROR code=' }).Count
            $skipCount = @($diagnostics | Where-Object { $_ -match 'AUTO_SKIP_BRANCH_VERIFIED from=' }).Count
            $exhaustedCount = @($diagnostics | Where-Object { $_ -match 'QUEUE_EXHAUSTED_BRANCH_VERIFIED index=' }).Count
            Write-Log "SERVICE_ERROR_COUNT=$errorCount"
            Write-Log "SERVICE_FORWARDED_COUNT=$forwardedCount"
            Write-Log "SKIP_BRANCH_COUNT=$skipCount"
            Write-Log "EXHAUSTED_BRANCH_COUNT=$exhaustedCount"
            Write-Log "ALL_MARKERS_FOUND=$allMarkers"
            Write-Log 'EXPECTED_ANDROID_TESTS=36'

            # V2 có 31 bài; thêm đúng 5 bài v3, không bỏ bài hồi quy.
            $logText = Get-Content -Path $logFile -Raw
            $testCountMatch = [regex]::Match($logText, 'Finished\s+(\d+)\s+tests\s+on')
            $totalTests = if ($testCountMatch.Success) { [int]$testCountMatch.Groups[1].Value } else { -1 }
            Write-Log "ANDROID_TEST_COUNT=$totalTests"
            if ($jvmOk -and $androidOk -and $totalTests -eq 36 -and $allMarkers -and `
                $errorCount -ge 1 -and $errorCount -eq $forwardedCount -and `
                $skipCount -eq 1 -and $exhaustedCount -eq 1) {
                $exitCode = 0
            }
        }
    }
    if ($exitCode -eq 0) { Write-Log 'FINAL_RESULT=PASS' }
    else { Write-Log 'FINAL_RESULT=FAIL' }
} catch {
    Write-Log "UNHANDLED_ERROR=$($_.Exception.Message)"
    Write-Log 'FINAL_RESULT=FAIL'
    $exitCode = 1
} finally {
    Pop-Location
}

# Sau MỖI vòng QA (kể cả FAIL) ghi trạng thái vào kế hoạch, không xóa PASS cũ.
try {
    & (Join-Path $PSScriptRoot 'update-runtime-recovery-v3-plan.ps1') `
        -Result $(if ($exitCode -eq 0) { 'PASS' } else { 'FAIL' }) `
        -AndroidTestCount $totalTests `
        -TestRunHead $head
} catch {
    Write-Log "PLAN_UPDATE_ERROR=$($_.Exception.Message)"
    $exitCode = 1
}
exit $exitCode
