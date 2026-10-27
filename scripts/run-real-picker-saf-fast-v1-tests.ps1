param([switch]$CompileOnly)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$logFile = Join-Path $root 'qa-logs\real-picker-saf-fast-v1.log'
New-Item -ItemType Directory -Force -Path (Split-Path $logFile) | Out-Null
[System.IO.File]::WriteAllText($logFile, "MusicFlow Real Picker SAF v1 - FAST (isolated)`r`n", (New-Object System.Text.UTF8Encoding($false)))

function Write-Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $logFile -Value $line -Encoding UTF8
}
function Run-Gradle([string]$phase, [string[]]$arguments) {
    & .\gradlew.bat @arguments 2>&1 |
        ForEach-Object { Write-Log $_.ToString() }
    $exit = $LASTEXITCODE
    Write-Log "RESULT $phase exitCode=$exit"
    return ($exit -eq 0)
}

Push-Location $root
$success = $false
try {
    Write-Log "DATE=$(Get-Date -Format o)"
    Write-Log 'MODE=FAST_ISOLATED'
    $compileOk = Run-Gradle 'KOTLIN_COMPILE' @(':app:compileDebugAndroidTestKotlin', '--console=plain')
    if (!$compileOk) { throw 'KOTLIN_COMPILE_FAILED' }

    if ($CompileOnly) {
        Write-Log 'SKIPPED_ANDROID_RUN=COMPILE_ONLY'
        $success = $true
    } else {
        $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
            elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
            else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
        $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
        $adb = if ($adbCmd) { $adbCmd.Source }
            else { Join-Path $sdk 'platform-tools\adb.exe' }
        if (!(Test-Path -LiteralPath $adb)) { throw 'ADB_NOT_FOUND' }
        $devices = @(& $adb devices | Where-Object { $_ -match '^emulator-\d+\s+device\s*$' } |
            ForEach-Object { ($_ -split '\s+')[0] })
        if ($devices.Count -eq 0) {
            $emulator = Join-Path $sdk 'emulator\emulator.exe'
            if (!(Test-Path -LiteralPath $emulator)) { throw 'EMULATOR_NOT_FOUND' }
            if (@(& $emulator -list-avds) -notcontains 'Pixel_5_API_30') { throw 'PIXEL_5_API_30_AVD_NOT_FOUND' }
            Start-Process -FilePath $emulator -ArgumentList @('-avd', 'Pixel_5_API_30')
        }
        $serial = $null
        for ($i = 0; $i -lt 120; $i++) {
            $devices = @(& $adb devices | Where-Object { $_ -match '^emulator-\d+\s+device\s*$' } |
                ForEach-Object { ($_ -split '\s+')[0] })
            if ($devices.Count -gt 1) { throw 'MULTIPLE_EMULATORS' }
            if ($devices.Count -eq 1) {
                $boot = (& $adb -s $devices[0] shell getprop sys.boot_completed | Out-String).Trim()
                if ($boot -eq '1') { $serial = $devices[0]; break }
            }
            Start-Sleep -Seconds 2
        }
        if (!$serial) { throw 'EMULATOR_NOT_READY' }
        $api = (& $adb -s $serial shell getprop ro.build.version.sdk | Out-String).Trim()
        Write-Log "ANDROID_API_LEVEL=$api"
        if ($api -ne '30') { throw 'REQUIRES_ANDROID_API_30' }

        & $adb -s $serial logcat -c 2>$null
        Write-Log 'INTERACTION_MODE=MANUAL_ASSISTED'
        Write-Log 'Lúc DocumentsUI mở: chọn file musicflow_real_picker_*.wav mới tạo trong Downloads rồi xác nhận Open/Select.'
        Write-Log 'Bài test chỉ chờ thao tác này; không chạy lại 50 regression.'
        $filter = '-Pandroid.testInstrumentationRunnerArguments.class=com.example.musicflow.core.playback.RealPickerSafTest#realOpenMultipleDocumentsGrantsPersistedReadAndPlays'
        $androidOk = Run-Gradle 'REAL_PICKER_ONLY' @(':app:connectedDebugAndroidTest', $filter, '--console=plain')
        $lines = @(& $adb -s $serial logcat -d -v brief -s 'MusicFlowRealPickerQA:I' 'AndroidRuntime:E' '*:S' 2>&1)
        foreach ($line in $lines) { Write-Log $line.ToString() }
        $output = Get-Content -LiteralPath $logFile -Raw
        $required = @('PASS sourcePreparedWithoutPersistedRead', 'DOCUMENT_VISIBLE_READY_FOR_SELECTION',
            'PICKER_RETURNED_TO_APP', 'PASS realPickerPlayback', 'PASS realPickerPersistedRead')
        $markersOk = $true
        foreach ($marker in $required) {
            $found = @($lines | Where-Object { $_.ToString().Contains($marker) }).Count -gt 0
            Write-Log "MARKER $marker=$found"
            if (!$found) { $markersOk = $false }
        }
        $oneTestStarted = $output -match 'Starting\s+1\s+tests?\s+on'
        $oneTestFinished = $output -match 'Finished\s+1\s+tests?\s+on'
        Write-Log "ONE_TEST_STARTED=$oneTestStarted ONE_TEST_FINISHED=$oneTestFinished"
        $success = $androidOk -and $markersOk -and $oneTestStarted -and $oneTestFinished
    }
} catch {
    Write-Log "ERROR=$($_.Exception.Message)"
} finally {
    $result = if (!$success) { 'FAIL' } elseif ($CompileOnly) { 'COMPILE_PASS' } else { 'PASS' }
    Write-Log "FINAL_RESULT=$result"
    Pop-Location
}
if (!$success) { exit 1 }
exit 0
