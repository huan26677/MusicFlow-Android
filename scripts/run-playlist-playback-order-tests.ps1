$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "qa-logs"
$logFile = Join-Path $logDir "playlist-playback-order-v1.log"

New-Item -ItemType Directory -Force -Path $logDir |
    Out-Null

Set-Content `
    -Path $logFile `
    -Value "MusicFlow Playlist Playback Order v1" `
    -Encoding utf8

function Write-Log([string]$message) {
    Write-Host $message

    $message | Out-File `
        -FilePath $logFile `
        -Encoding utf8 `
        -Append
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
    Write-Log "COMMIT=$(& git rev-parse --short HEAD)"

    $adbCommand =
        Get-Command adb -ErrorAction SilentlyContinue

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

    $unitPassed = Run-Gradle "JVM_UNIT_TESTS" @(
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

    Write-Log "=== MEDIA3_ORDER_DIAGNOSTICS ==="

    $diagnostics = @()

    if ($adbPath) {
        $diagnostics = @(
            & $adbPath logcat -d -v brief -s `
                "MusicFlowOrderTest:I" `
                "*:S" 2>&1 |
                ForEach-Object {
                    $_.ToString()
                }
        )

        foreach ($line in $diagnostics) {
            Write-Log $line
        }
    }

    $testPassed = @(
        $diagnostics | Where-Object {
            $_ -match (
                "PASS " +
                "playlistReorderControlsMedia3Playback"
            )
        }
    ).Count -gt 0

    Write-Log "ORDER_TEST_PASS_MARKER=$testPassed"

    if (
        $unitPassed -and
        $androidPassed -and
        $testPassed
    ) {
        Write-Log "FINAL_RESULT=PASS"
        $exitCode = 0
    } else {
        Write-Log "FINAL_RESULT=FAIL"
        $exitCode = 1
    }
}
finally {
    Pop-Location
}

exit $exitCode
