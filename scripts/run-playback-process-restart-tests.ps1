param([switch]$PhasesOnly)

$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "qa-logs"
$logFile = Join-Path $logDir "playback-process-restart-v1.log"

New-Item -ItemType Directory -Force `
    -Path $logDir | Out-Null

Set-Content -Path $logFile `
    -Value "MusicFlow Process Restart Recovery v1" `
    -Encoding utf8

function Write-Log([string]$message) {
    Write-Host $message
    $message | Out-File `
        -FilePath $logFile `
        -Encoding utf8 -Append
}

function Run-Gradle(
    [string]$name,
    [string[]]$arguments
) {
    Write-Log "=== $name ==="

    & .\gradlew.bat @arguments 2>&1 |
        ForEach-Object {
            Write-Log $_.ToString()
        }

    $code = $LASTEXITCODE
    Write-Log "RESULT $name exitCode=$code"

    return ($code -eq 0)
}

Push-Location $root

$exitCode = 1
$adbPath = $null
$serial = $null

$package = "com.example.musicflow"
$runner = (
    "com.example.musicflow.test/" +
    "androidx.test.runner.AndroidJUnitRunner"
)

$testClass = (
    "com.example.musicflow.core.playback." +
    "PlaybackProcessRestartTest"
)

function Get-AppPid {
    $value = & $adbPath -s $serial `
        shell pidof -s $package 2>$null

    return ($value | Out-String).Trim()
}

function Stop-App {
    Write-Log "=== FORCE_STOP ==="

    & $adbPath -s $serial `
        shell am force-stop $package 2>&1 |
        ForEach-Object { Write-Log $_.ToString() }

    Start-Sleep -Milliseconds 400

    $remaining = Get-AppPid
    $stopped = [string]::IsNullOrWhiteSpace(
        $remaining
    )

    Write-Log "PROCESS_STOPPED=$stopped"
    return $stopped
}

function Run-Phase(
    [string]$name,
    [string]$method,
    [string]$phase
) {
    Write-Log "=== PHASE_$name ==="

    $output = @(
        & $adbPath -s $serial shell am instrument `
            -w `
            -e class "$testClass#$method" `
            -e musicflow_restart_phase $phase `
            $runner 2>&1 |
            ForEach-Object {
                $_.ToString()
            }
    )

    $code = $LASTEXITCODE

    foreach ($line in $output) {
        Write-Log $line
    }

    $content = $output -join "`n"

    $passed = (
        $code -eq 0 -and
        $content -match 'OK \(1 test\)' -and
        $content -notmatch (
            'FAILURES!!!|' +
            'INSTRUMENTATION_FAILED|' +
            'INSTRUMENTATION_STATUS: Error='
        )
    )

    Write-Log "PHASE_${name}_EXIT_CODE=$code"

    Write-Log "PHASE_${name}_RESULT=$passed"
    return $passed
}

function Get-PhasePid([string]$marker) {
    $lines = @(
        & $adbPath -s $serial logcat `
            -d -v brief -s `
            "MusicFlowRestartIT:I" `
            "*:S" 2>$null |
            ForEach-Object { $_.ToString() }
    )

    $matchedLine = $lines |
        Where-Object { $_ -match "$marker=" } |
        Select-Object -Last 1

    if ($matchedLine -match "$marker=(\d+)") {
        return $matches[1]
    }

    return ""
}

function Ensure-TestRunner {
    Write-Log "=== INSTALL_TEST_RUNNER ==="
    # connectedDebugAndroidTest có thể gỡ APK ứng dụng
    # sau khi hoàn thành. Cần cài lại ứng dụng trước.
    $appApk = Join-Path `
        $root "app\build\outputs\apk\debug\app-debug.apk"

    if (!(Test-Path $appApk)) {
        Write-Log "ERROR=MAIN_APP_APK_NOT_FOUND"
        return $false
    }

    Write-Log "MAIN_APP_APK=$appApk"

    $appOutput = @(
        & $adbPath -s $serial install -r -t `
            $appApk 2>&1 |
            ForEach-Object {
                $_.ToString()
            }
    )

    $appInstallCode = $LASTEXITCODE

    foreach ($line in $appOutput) {
        Write-Log $line
    }

    if ($appInstallCode -ne 0) {
        Write-Log "TARGET_APP_INSTALLED=False"
        return $false
    }

    $targetPackages = @(
        & $adbPath -s $serial `
            shell pm path $package 2>&1 |
            ForEach-Object {
                $_.ToString()
            }
    )

    $targetReady = @(
        $targetPackages | Where-Object {
            $_ -match '^package:'
        }
    ).Count -gt 0

    Write-Log "TARGET_APP_INSTALLED=$targetReady"

    if (!$targetReady) {
        return $false
    }


    $apkRoot = Join-Path `
        $root "app\build\outputs\apk\androidTest"

    $apk = Get-ChildItem `
        -Path $apkRoot `
        -Filter "*.apk" `
        -File `
        -Recurse `
        -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (!$apk) {
        Write-Log "ERROR=ANDROID_TEST_APK_NOT_FOUND"
        return $false
    }

    Write-Log "TEST_APK=$($apk.FullName)"

    $output = @(
        & $adbPath -s $serial `
            install -r -t $apk.FullName 2>&1 |
            ForEach-Object { $_.ToString() }
    )

    $installCode = $LASTEXITCODE

    foreach ($line in $output) {
        Write-Log $line
    }

    if ($installCode -ne 0) {
        Write-Log "TEST_APK_INSTALL=False"
        return $false
    }

    $available = @(
        & $adbPath -s $serial `
            shell pm list instrumentation 2>&1 |
            ForEach-Object { $_.ToString() }
    )

    foreach ($line in $available) {
        Write-Log $line
    }

    $found = @(
        $available | Where-Object {
            $_.Contains($runner)
        }
    ).Count -gt 0

    Write-Log "TEST_RUNNER_AVAILABLE=$found"

    return $found
}

try {
    Write-Log "DATE=$(Get-Date -Format o)"
    Write-Log "COMMIT=$(& git rev-parse --short HEAD)"

    $command = Get-Command adb `
        -ErrorAction SilentlyContinue

    if ($command) {
        $adbPath = $command.Source
    } else {
        $sdk = if ($env:ANDROID_HOME) {
            $env:ANDROID_HOME
        } elseif ($env:ANDROID_SDK_ROOT) {
            $env:ANDROID_SDK_ROOT
        } else {
            Join-Path $env:LOCALAPPDATA "Android\Sdk"
        }

        $candidate = Join-Path `
            $sdk "platform-tools\adb.exe"

        if (Test-Path $candidate) {
            $adbPath = $candidate
        }
    }

    if (!$adbPath) {
        Write-Log "ERROR=ADB_NOT_FOUND"
    } else {
        $devices = @(
            & $adbPath devices |
                Where-Object {
                    $_ -match '^(\S+)\s+device\s*$'
                } |
                ForEach-Object {
                    if ($_ -match '^(\S+)\s+device\s*$') {
                        $matches[1]
                    }
                }
        )

        if (
            $devices.Count -ne 1 -or
            $devices[0] -notmatch '^emulator-\d+$'
        ) {
            Write-Log (
                "ERROR=REQUIRES_EXACTLY_ONE_EMULATOR"
            )
        } else {
            $serial = $devices[0]
            Write-Log "EMULATOR=$serial"

            & $adbPath -s $serial logcat -c 2>$null

            $unitPassed = if ($PhasesOnly) {
                Run-Gradle "BUILD_TEST_APKS" @(
                    ":app:assembleDebug",
                    ":app:assembleDebugAndroidTest",
                    "--console=plain"
                )
            } else {
                Run-Gradle "JVM_TESTS" @(
                    ":app:testDebugUnitTest",
                    "--console=plain"
                )
            }

            $androidPassed = $PhasesOnly

            if ($unitPassed -and -not $PhasesOnly) {
                $androidPassed = Run-Gradle "ANDROID_TESTS" @(
                    ":app:connectedDebugAndroidTest",
                    "--console=plain"
                )
            }

            $runnerReady = $false

            if ($unitPassed -and $androidPassed) {
                $runnerReady = Ensure-TestRunner
            }

            if (
                $unitPassed -and
                $androidPassed -and
                $runnerReady
            ) {
                $initialStop = Stop-App

                $initialCleanup = $false

                if ($initialStop) {
                    $initialCleanup = Run-Phase `
                        "INITIAL_CLEANUP" `
                        "cleanupAfterRestart" `
                        "cleanup"
                }

                $beforePrepareStop = Stop-App

                $prepared = $false
                $verified = $false
                $processStopped = $false
                $newProcess = $false
                $cleaned = $false

                try {
                    if (
                        $initialCleanup -and
                        $beforePrepareStop
                    ) {
                        $prepared = Run-Phase `
                            "PREPARE" `
                            "prepareBeforeRestart" `
                            "prepare"
                    }

                    $oldPid = Get-PhasePid "PREPARE_PROCESS_PID"
                    Write-Log "OLD_PID=$oldPid"

                    $processStopped = Stop-App

                    if (
                        $prepared -and
                        $processStopped -and
                        ![string]::IsNullOrWhiteSpace(
                            $oldPid
                        )
                    ) {
                        $verified = Run-Phase `
                            "VERIFY" `
                            "verifyAfterRestart" `
                            "verify"
                    }

                    $newPid = Get-PhasePid "VERIFY_PROCESS_PID"
                    Write-Log "NEW_PID=$newPid"

                    $newProcess = (
                        $verified -and
                        ![string]::IsNullOrWhiteSpace(
                            $oldPid
                        ) -and
                        ![string]::IsNullOrWhiteSpace(
                            $newPid
                        ) -and
                        $oldPid -ne $newPid
                    )

                    Write-Log "NEW_PROCESS=$newProcess"
                }
                finally {
                    $finalStop = Stop-App

                    if ($finalStop) {
                        $cleaned = Run-Phase `
                            "CLEANUP" `
                            "cleanupAfterRestart" `
                            "cleanup"
                    }
                }


                Write-Log "=== RESTART_DIAGNOSTICS ==="

                $diagnostics = @(
                    & $adbPath -s $serial logcat `
                        -d -v brief -s `
                        "MusicFlowRestartIT:I" `
                        "MusicFlowQueue:I" `
                        "MusicFlowQueue:W" `
                        "*:S" 2>&1 |
                        ForEach-Object {
                            $_.ToString()
                        }
                )

                foreach ($line in $diagnostics) {
                    Write-Log $line
                }

                $preparedMarker = @(
                    $diagnostics | Where-Object {
                        $_ -match "PHASE_PREPARE_PASS"
                    }
                ).Count -gt 0

                $pausedMarker = @(
                    $diagnostics | Where-Object {
                        $_ -match "PHASE_VERIFY_PAUSED_PASS"
                    }
                ).Count -gt 0

                $verifiedMarker = @(
                    $diagnostics | Where-Object {
                        $_ -match "PHASE_VERIFY_PASS"
                    }
                ).Count -gt 0

                $cleanupMarker = @(
                    $diagnostics | Where-Object {
                        $_ -match "PHASE_CLEANUP_PASS"
                    }
                ).Count -gt 0

                $historyPrepared = @(
                    $diagnostics | Where-Object {
                        $_ -match "PASS historyPreparedBeforeRestart"
                    }
                ).Count -gt 0

                $historyRestored = @(
                    $diagnostics | Where-Object {
                        $_ -match "PASS historyPersistedAfterRestart"
                    }
                ).Count -gt 0

                $historyCleaned = @(
                    $diagnostics | Where-Object {
                        $_ -match "PASS historyCleanedAfterRestart"
                    }
                ).Count -gt 0

                Write-Log "HISTORY_PREPARED=$historyPrepared"
                Write-Log "HISTORY_RESTORED=$historyRestored"
                Write-Log "HISTORY_CLEANED=$historyCleaned"

                $historyUiOk = @(
                    $diagnostics | Where-Object {
                        $_ -match "PASS historyUiAfterRestart"
                    }
                ).Count -gt 0

                $playlistUiOk = @(
                    $diagnostics | Where-Object {
                        $_ -match "PASS playlistUiAfterRestart"
                    }
                ).Count -gt 0

                $pausedAfterUiOk = @(
                    $diagnostics | Where-Object {
                        $_ -match "PASS remainsPausedAfterUiNavigation"
                    }
                ).Count -gt 0

                Write-Log "HISTORY_UI_RESTORED=$historyUiOk"
                Write-Log "PLAYLIST_UI_RESTORED=$playlistUiOk"
                Write-Log "PAUSED_AFTER_UI=$pausedAfterUiOk"

                Write-Log "PREPARED=$prepared"
                Write-Log "PROCESS_RESTARTED=$newProcess"
                Write-Log "RECOVERED_PAUSED=$pausedMarker"
                Write-Log "PLAYBACK_RESUMED=$verifiedMarker"
                Write-Log "CLEANUP=$cleaned"

                if (
                    $prepared -and
                    $verified -and
                    $newProcess -and
                    $cleaned -and
                    $preparedMarker -and
                    $pausedMarker -and
                    $verifiedMarker -and
                    $cleanupMarker -and
                    $historyPrepared -and
                    $historyRestored -and
                    $historyCleaned -and
                    $historyUiOk -and
                    $playlistUiOk -and
                    $pausedAfterUiOk
                ) {
                    Write-Log "FINAL_RESULT=PASS"
                    $exitCode = 0
                } else {
                    Write-Log "FINAL_RESULT=FAIL"
                }
            } else {
                Write-Log "FINAL_RESULT=FAIL"
            }
        }
    }
}
finally {
    Pop-Location
}

exit $exitCode

