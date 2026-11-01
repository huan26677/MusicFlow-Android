param([string]$DeviceId = "emulator-5554")

# Full, unfiltered Android instrumentation gate on one API 30 emulator.
# Phase-oriented SAF/process-restart tests have a deliberately separate
# multi-process harness; never treat their unfiltered skips as passes.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\full-product-regression-v2-part1.log"
$jvmResults = Join-Path $root "app\build\test-results\testDebugUnitTest"
$androidResults = Join-Path $root "app\build\outputs\androidTest-results\connected\debug"
$gateMutex = [System.Threading.Mutex]::new(
    $false, "Local\MusicFlow_FullRegressionGate_API30"
)
$ownsGateMutex = $false

function Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function RunGradle([string]$name, [string[]]$arguments) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & ".\gradlew.bat" @arguments 2>&1 |
            ForEach-Object { Add-Content -LiteralPath $log -Value ([string]$_) -Encoding UTF8 }
        $exit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $old }
    Log "${name}_EXIT=$exit"
    return $exit
}

function CollectCases([string]$folder, [string]$kind) {
    $reports = @(Get-ChildItem -LiteralPath $folder -File -Filter "TEST-*.xml" -ErrorAction SilentlyContinue)
    if ($reports.Count -eq 0) { throw "NO_${kind}_XML_REPORTS" }
    $result = @(
        foreach ($report in $reports) {
            [xml]$xml = Get-Content -LiteralPath $report.FullName -Raw -Encoding UTF8
            foreach ($case in $xml.GetElementsByTagName("testcase")) {
                $status = if ($case.GetElementsByTagName("failure").Count -gt 0 -or
                    $case.GetElementsByTagName("error").Count -gt 0) { "FAIL" }
                elseif ($case.GetElementsByTagName("skipped").Count -gt 0) { "SKIP" }
                else { "PASS" }
                $errorText = ""
                if ($status -eq "FAIL") {
                    $errors = @($case.GetElementsByTagName("failure")) +
                        @($case.GetElementsByTagName("error"))
                    $message = ($errors | ForEach-Object { $_.GetAttribute("message") } |
                        Where-Object { $_ }) -join " | "
                    $errorText = $message.Replace([char]10, " ").Replace([char]13, " ")
                    if ($errorText.Length -gt 320) { $errorText = $errorText.Substring(0, 320) }
                }
                [pscustomobject]@{
                    Status = $status
                    Suite = [string]$case.classname
                    Name = [string]$case.name
                    Message = $errorText
                }
            }
        }
    )
    if ($result.Count -eq 0) { throw "NO_${kind}_TESTCASES" }
    foreach ($status in @("PASS","FAIL","SKIP")) {
        Log "${kind}_${status}=$(@($result | Where-Object { $_.Status -eq $status }).Count)"
    }
    Log "${kind}_TOTAL=$($result.Count)"
    foreach ($case in $result) {
        if ($case.Status -ne "PASS") {
            Log ("CASE=" + $case.Status + " " + $kind + " " + $case.Suite + "#" + $case.Name)
            if ($case.Message) { Log ("CASE_ERROR=" + $case.Message) }
        }
    }
    return $result
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Full Product Regression Gate v2 - Part 1" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    $ownsGateMutex = $gateMutex.WaitOne(0)
    if (!$ownsGateMutex) { throw "ANOTHER_FULL_REGRESSION_GATE_IS_RUNNING" }
    $otherGradleTests = @(Get-CimInstance Win32_Process -Filter "name='java.exe'" |
        Where-Object { $_.CommandLine -match ':app:connectedDebugAndroidTest' })
    Log "OTHER_ANDROID_GRADLE_RUNS=$($otherGradleTests.Count)"
    if ($otherGradleTests.Count -gt 0) {
        throw "ANOTHER_CONNECTED_ANDROID_TEST_IS_RUNNING"
    }
    # 'adb devices -l' may append product/model/transport metadata after
    # the 'device' state. Match state as a separate token, not end-of-line.
    $devices = @(& adb devices -l | Where-Object {
        $_ -match '^emulator-\d+\s+device(?:\s|$)'
    })
    if ($devices.Count -ne 1 -or !$devices[0].StartsWith("$DeviceId ")) {
        throw "REQUIRES_ONE_MATCHING_EMULATOR"
    }
    $api = (& adb -s $DeviceId shell getprop ro.build.version.sdk | Out-String).Trim()
    $boot = (& adb -s $DeviceId shell getprop sys.boot_completed | Out-String).Trim()
    Log "DEVICE=$DeviceId API=$api BOOT=$boot"
    if ($api -ne "30" -or $boot -ne "1") { throw "EXPECTED_BOOTED_ANDROID_API_30" }

    $build = RunGradle "GRADLE_BUILD_JVM" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    if ($build -ne 0) { throw "BUILD_OR_JVM_FAILED" }
    $jvm = @(CollectCases $jvmResults "JVM")
    if (@($jvm | Where-Object { $_.Status -ne "PASS" }).Count -gt 0) {
        throw "JVM_HAS_FAILED_OR_SKIPPED_CASES"
    }

    # Prevent stale XML from an earlier test attempt from appearing fresh.
    if (Test-Path $androidResults) {
        Get-ChildItem -LiteralPath $androidResults -Filter "TEST-*.xml" -File |
            Remove-Item -Force
    }
    $androidExit = RunGradle "GRADLE_ANDROID" @(":app:connectedDebugAndroidTest","--console=plain")
    if ($androidExit -ne 0 -and
        @(Get-ChildItem -LiteralPath $androidResults -File -Filter "TEST-*.xml" -ErrorAction SilentlyContinue).Count -eq 0) {
        throw "CONNECTED_ANDROID_TEST_FAILED_BEFORE_JUNIT_XML"
    }
    $android = @(CollectCases $androidResults "ANDROID")
    $allowedSkippedTests = @(
        "cleanupSafDocument",
        "prepareSafDocument",
        "verifySafAfterForceStop"
    )
    $actualSkipped = @($android | Where-Object { $_.Status -eq "SKIP" })
    $unexpectedSkip = @($actualSkipped | Where-Object {
        $_.Suite -notlike "*SafPermissionRestartTest" -or
        $_.Name -notin $allowedSkippedTests
    })
    Log "ANDROID_PHASED_SAF_RESTART_SKIPS=$($actualSkipped.Count)"
    Log "ANDROID_UNEXPECTED_SKIP=$($unexpectedSkip.Count)"
    Log "PHASED_SAF_RESTART_EVIDENCE=docs/Android_Compatibility_Matrix_v2.md"
    Log "ANDROID_RUN_FRESH_XML=TRUE"
    $diffOld = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & git diff --check 2>&1 | ForEach-Object { Log ([string]$_) }
        $diffExit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $diffOld }
    Log "GIT_DIFF_CHECK_EXIT=$diffExit"
    if ($androidExit -ne 0 -or $diffExit -ne 0 -or
        $unexpectedSkip.Count -gt 0 -or
        @($android | Where-Object { $_.Status -eq "FAIL" }).Count -gt 0) {
        throw "FULL_REGRESSION_BLOCKED"
    }
    Log "FINAL_RESULT=PASS"
} catch {
    Log ("FAIL_REASON=" + $_.Exception.Message)
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally {
    if ($ownsGateMutex) { $gateMutex.ReleaseMutex() }
    $gateMutex.Dispose()
    Pop-Location
}
