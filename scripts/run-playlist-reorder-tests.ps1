$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "qa-logs"
$logFile = Join-Path $logDir "playlist-reorder-v1.log"

New-Item -ItemType Directory -Force -Path $logDir |
    Out-Null

Set-Content -Path $logFile `
    -Value "MusicFlow Playlist Reorder v1" `
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

try {
    Write-Log "DATE=$(Get-Date -Format o)"
    Write-Log "COMMIT=$(& git rev-parse --short HEAD)"

    $adb = Get-Command adb -ErrorAction SilentlyContinue
    $adbPath = if ($adb) { $adb.Source } else { $null }

    if (!$adbPath) {
        $sdk = if ($env:ANDROID_HOME) {
            $env:ANDROID_HOME
        } elseif ($env:ANDROID_SDK_ROOT) {
            $env:ANDROID_SDK_ROOT
        } else {
            Join-Path $env:LOCALAPPDATA "Android\Sdk"
        }

        $candidate = Join-Path $sdk "platform-tools\adb.exe"

        if (Test-Path $candidate) {
            $adbPath = $candidate
        }
    }

    if ($adbPath) {
        & $adbPath logcat -c 2>$null
    }

    $unitPassed = Run-Gradle "JVM_UNIT_TESTS" @(
        ":app:testDebugUnitTest",
        "--console=plain"
    )

    $androidPassed = Run-Gradle "ANDROID_TESTS" @(
        ":app:connectedDebugAndroidTest",
        "--console=plain"
    )

    Write-Log "=== REORDER_DIAGNOSTICS ==="

    if ($adbPath) {
        & $adbPath logcat -d -v brief -s `
            "MusicFlowReorderTest:I" `
            "*:S" 2>&1 |
            ForEach-Object {
                Write-Log $_.ToString()
            }
    } else {
        Write-Log "ADB_NOT_FOUND: Logcat unavailable"
    }

    if ($unitPassed -and $androidPassed) {
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
