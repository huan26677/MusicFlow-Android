$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "qa-logs"
$logFile = Join-Path $logDir "playback-queue-hardening-v1.log"

New-Item -ItemType Directory -Force `
    -Path $logDir | Out-Null

Set-Content -Path $logFile `
    -Value "MusicFlow Playback Queue Hardening v1" `
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

    $adbCommand = Get-Command adb `
        -ErrorAction SilentlyContinue

    $adbPath = if ($adbCommand) {
        $adbCommand.Source
    } else {
        $null
    }

    if (!$adbPath) {
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

    if ($adbPath) {
        & $adbPath logcat -c 2>$null
    } else {
        Write-Log "ADB_NOT_FOUND"
    }

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

    Write-Log "=== HARDENING_DIAGNOSTICS ==="

    $diagnostics = @()

    if ($adbPath) {
        $diagnostics = @(
            & $adbPath logcat -d -v brief -s `
                "MusicFlowQueueHardeningTest:I" `
                "MusicFlowQueueRecovery:I" `
                "MusicFlowQueue:W" `
                "*:S" 2>&1 |
                ForEach-Object {
                    $_.ToString()
                }
        )

        foreach ($line in $diagnostics) {
            Write-Log $line
        }
    }

    $markers = @(
        "PASS deletedSelectedFilePruned",
        "PASS validSelectionPositionKept",
        "PASS permissionDeniedClearsQueue",
        "PASS malformedSnapshotCleared"
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

    Write-Log "ALL_MARKERS_FOUND=$allMarkersFound"

    if (
        $unitPassed -and
        $androidPassed -and
        $allMarkersFound
    ) {
        Write-Log "FINAL_RESULT=PASS"
        $exitCode = 0
    } else {
        Write-Log "FINAL_RESULT=FAIL"
    }
}
finally {
    Pop-Location
}

exit $exitCode

