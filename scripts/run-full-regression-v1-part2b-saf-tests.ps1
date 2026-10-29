param([string]$DeviceId = "emulator-5554")

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\full-product-regression-v1-part2b-saf.log"

function Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function Native([string]$exe, [string[]]$arguments) {
    $before = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = @(& $exe @arguments 2>&1)
        $code = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $before }
    $message = $output -join [char]10
    Log $message
    return [pscustomobject]@{ Code = $code; Output = $message }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Full Product Regression v1 Part 2B - Real SAF Picker" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Log "DEVICE=$DeviceId"
    Log "NO_ONLINE_RIGHTS_REQUIRED_FOR_PERSONAL_OFFLINE_FILES=TRUE"
    $api = Native "adb" @("-s", $DeviceId, "shell", "getprop", "ro.build.version.sdk")
    if ($api.Code -ne 0 -or $api.Output.Trim() -ne "30") {
        throw "EXPECTED_ANDROID_API_30"
    }
    $build = Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Log "BUILD_JVM_EXIT=$($build.Code)"
    if ($build.Code -ne 0 -or $build.Output -notmatch "BUILD SUCCESSFUL") {
        throw "GRADLE_OR_JVM_FAILED"
    }
    foreach ($apk in @(
        "app\build\outputs\apk\debug\app-debug.apk",
        "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    )) {
        $installed = Native "adb" @("-s", $DeviceId, "install", "-r", (Join-Path $root $apk))
        if ($installed.Code -ne 0 -or !$installed.Output.Contains("Success")) {
            throw "APK_INSTALL_FAILED"
        }
    }

    $null = Native "adb" @("-s", $DeviceId, "logcat", "-c")
    $tests = Native "adb" @(
        "-s", $DeviceId, "shell", "am", "instrument", "-w",
        "-e", "class", "com.example.musicflow.core.playback.RealPickerSafTest",
        "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"
    )
    Log "INSTRUMENTATION_EXIT=$($tests.Code)"
    if ($tests.Code -ne 0 -or !$tests.Output.Contains("OK (2 tests)") -or
        $tests.Output -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "REAL_PICKER_TESTS_FAILED"
    }
    Log "REAL_PICKER_TESTS=2"

    $resultLog = Native "adb" @(
        "-s", $DeviceId, "logcat", "-d", "-v", "brief",
        "-s", "MusicFlowRealPickerQA:I", "*:S"
    )
    foreach ($marker in @(
        "PASS sourcePreparedWithoutPersistedRead",
        "PASS productionPickerLaunched",
        "PASS realPickerPlayback",
        "PASS realPickerPersistedRead",
        "PASS multiSourcesPrepared",
        "PASS multiQueue",
        "PASS multiPersistedRead",
        "PASS multiNext",
        "PASS multiNextWrittenHistory"
    )) {
        if (!$resultLog.Output.Contains($marker)) {
            throw "MISSING_$marker"
        }
    }
    if ($resultLog.Output -notmatch "AUTO_PICKER_RETURNED tapped=1" -or
        $resultLog.Output -notmatch "AUTO_PICKER_RETURNED tapped=2") {
        throw "MISSING_REAL_DOCUMENTS_UI_RETURN"
    }
    Log "REAL_PICKER_MARKERS=9"
    Log "DOCUMENTS_UI_RETURNED_ONE_AND_TWO=TRUE"

    $diff = Native "git" @("diff", "--check")
    Log "GIT_DIFF_CHECK_EXIT=$($diff.Code)"
    if ($diff.Code -ne 0) { throw "DIFF_CHECK_FAILED" }
    Log "FINAL_RESULT=PASS"
} catch {
    Log ("FAIL_REASON=" + $_.Exception.Message)
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
