$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "qa-logs"
$logFile = Join-Path $logDir "playback-runtime-auto-skip-v1.log"

New-Item -ItemType Directory -Force `
    -Path $logDir | Out-Null

Set-Content -Path $logFile `
    -Value "MusicFlow Runtime Auto-Skip v1" `
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

try {
    Write-Log "DATE=$(Get-Date -Format o)"

    $command = Get-Command adb `
        -ErrorAction SilentlyContinue

    $adbPath = if ($command) {
        $command.Source
    } else {
        $sdk = if ($env:ANDROID_HOME) {
            $env:ANDROID_HOME
        } elseif ($env:ANDROID_SDK_ROOT) {
            $env:ANDROID_SDK_ROOT
        } else {
            Join-Path $env:LOCALAPPDATA "Android\Sdk"
        }

        Join-Path $sdk "platform-tools\adb.exe"
    }

    if (!(Test-Path $adbPath)) {
        Write-Log "ERROR=ADB_NOT_FOUND"
        Write-Log "FINAL_RESULT=FAIL"
    } else {
        $devices = @(
            & $adbPath devices |
                Where-Object {
                    $_ -match '^emulator-\d+\s+device\s*$'
                } |
                ForEach-Object {
                    ($_ -split '\s+')[0]
                }
        )

        if ($devices.Count -ne 1) {
            Write-Log "ERROR=REQUIRES_ONE_EMULATOR"
            Write-Log "FINAL_RESULT=FAIL"
        } else {
            $serial = $devices[0]
            Write-Log "EMULATOR=$serial"

            & $adbPath -s $serial logcat -c 2>$null

            $unitPassed = Run-Gradle "JVM_TESTS" @(
                ":app:testDebugUnitTest",
                "--console=plain"
            )

            $androidPassed = $false

            if ($unitPassed) {
                $androidPassed = Run-Gradle "ANDROID_TESTS" @(
                    ":app:connectedDebugAndroidTest",
                    "--console=plain"
                )
            }

            Write-Log "=== AUTO_SKIP_DIAGNOSTICS ==="

            $diagnostics = @(
                & $adbPath -s $serial logcat `
                    -d -v brief -s `
                    "MusicFlowAutoSkipTest:I" `
                    "MusicFlowAutoSkip:I" `
                    "MusicFlowAutoSkip:W" `
                    "*:S" 2>&1 |
                    ForEach-Object {
                        $_.ToString()
                    }
            )

            foreach ($line in $diagnostics) {
                Write-Log $line
            }

            $markers = @(
                "PASS singleMissingAutoSkipped",
                "PASS consecutiveMissingAutoSkipped",
                "PASS allMissingStopsWithoutLoop",
                "PASS pausedPlayerDoesNotAutoSkip",
                "PASS injectedRecoveryBranches"
            )

            $allMarkersFound = $true

            foreach ($marker in $markers) {
                $found = @(
                    $diagnostics | Where-Object {
                        $_.Contains($marker)
                    }
                ).Count -gt 0

                Write-Log "MARKER $marker=$found"

                if (!$found) {
                    $allMarkersFound = $false
                }
            }

            $skipCount = @(
                $diagnostics | Where-Object {
                    $_ -match 'SKIPPED_ERROR from='
                }
            ).Count

            $exhaustedCount = @(
                $diagnostics | Where-Object {
                    $_ -match 'QUEUE_EXHAUSTED index='
                }
            ).Count

            Write-Log "AUTO_SKIP_COUNT=$skipCount"
            Write-Log "QUEUE_EXHAUSTED_COUNT=$exhaustedCount"
            Write-Log "ALL_MARKERS_FOUND=$allMarkersFound"

            # Các test integration xác nhận hành vi phát.
            # Test injectedRecoveryBranches xác nhận
            # hai nhánh xử lý của PlaybackRuntimeAutoSkip.
            # Không giả định số lần Media3 gọi onPlayerError.
            if (
                $unitPassed -and
                $androidPassed -and
                $allMarkersFound -and
                $skipCount -ge 1 -and
                $exhaustedCount -ge 1
            ) {
                Write-Log "FINAL_RESULT=PASS"
                $exitCode = 0
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

