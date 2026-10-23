$ErrorActionPreference = "Continue"

$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $root "qa-logs"
$logFile = Join-Path $logDir "playback-skip-v1.log"

New-Item -ItemType Directory -Force -Path $logDir |
    Out-Null

function Write-Log([string]$message) {
    $message | Out-File `
        -FilePath $logFile `
        -Encoding utf8 `
        -Append

    Write-Host $message
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

Set-Content -Path $logFile `
    -Value "MusicFlow Playback Skip v1" `
    -Encoding utf8

Push-Location $root

$success = $true

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

    $success = Run-Gradle "JVM_UNIT_TESTS" @(
        ":app:testDebugUnitTest",
        "--console=plain"
    )

    if ($success) {
        $success = Run-Gradle "ANDROID_TESTS" @(
            ":app:connectedDebugAndroidTest",
            "--console=plain"
        )
    }

    Write-Log "=== MEDIA3_DIAGNOSTICS ==="

    if ($adbPath) {
        & $adbPath logcat -d -v brief -s `
            "MusicFlowSkip:I" `
            "MusicFlowTest:I" `
            "*:S" 2>&1 |
            ForEach-Object {
                Write-Log $_.ToString()
            }
    } else {
        Write-Log "ADB_NOT_FOUND"
        $success = $false
    }

    if ($success) {
        Write-Log "FINAL_RESULT=PASS"
    } else {
        Write-Log "FINAL_RESULT=FAIL"
    }
}
finally {
    Pop-Location
}

if ($success) {
    exit 0
} else {
    exit 1
}
