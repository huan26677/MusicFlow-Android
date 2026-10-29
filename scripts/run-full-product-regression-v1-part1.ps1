param(
    [string]$DeviceId = "emulator-5554",
    [switch]$AnalyzeOnly
)

# Part 1 is intentionally small: APK/JVM + the unfiltered Android baseline
# on ONE emulator. SAF and process-restart phase tests require their dedicated
# multi-process harness; a default JUnit pass alone does not prove those flows.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\full-product-regression-v1-part1.log"
$jvmFolder = Join-Path $root "app\build\test-results\testDebugUnitTest"
$androidFolder = Join-Path $root "app\build\outputs\androidTest-results\connected\debug"

function Record([string]$message) {
    Write-Host $message
    Add-Content -LiteralPath $log -Value $message -Encoding UTF8
}

function Gradle([string]$phase, [string[]]$arguments) {
    Record "PHASE_$phase=START"
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & ".\gradlew.bat" @arguments 2>&1 |
            ForEach-Object { Add-Content -LiteralPath $log -Value ([string]$_) -Encoding UTF8 }
        $code = [int]$LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
    Record "PHASE_${phase}_EXIT=$code"
    return $code
}

function Get-TestCases([string]$folder, [string]$kind) {
    $reports = @(Get-ChildItem -LiteralPath $folder -Filter "TEST-*.xml" -File -ErrorAction SilentlyContinue)
    if ($reports.Count -eq 0) {
        throw "MISSING_${kind}_XML_RESULTS"
    }
    $records = @(
        foreach ($report in $reports) {
            [xml]$doc = Get-Content -LiteralPath $report.FullName -Raw -Encoding UTF8
            foreach ($node in $doc.GetElementsByTagName("testcase")) {
                $status = "PASS"
                if ($node.GetElementsByTagName("failure").Count -gt 0 -or
                    $node.GetElementsByTagName("error").Count -gt 0) {
                    $status = "FAIL"
                } elseif ($node.GetElementsByTagName("skipped").Count -gt 0) {
                    $status = "SKIP"
                }
                [pscustomobject]@{
                    Kind = $kind
                    Suite = [string]$node.classname
                    Name = [string]$node.name
                    Status = $status
                    Report = $report.Name
                }
            }
        }
    )
    if ($records.Count -eq 0) {
        throw "EMPTY_${kind}_TESTCASE_REPORTS"
    }
    return $records
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Full Product Regression Gate v1 - Part 1" -Encoding UTF8
    Record "DATE=$(Get-Date -Format o)"
    Record "HEAD_BASE=$(git rev-parse --short HEAD)"
    Record "ANALYZE_ONLY=$AnalyzeOnly"

    $gradleJvm = 0
    $gradleAndroid = 0
    if (!$AnalyzeOnly) {
        $devices = @(& adb devices | Where-Object { $_ -match '^emulator-\d+\s+device\s*$' })
        if ($devices.Count -ne 1 -or !$devices[0].StartsWith("$DeviceId ")) {
            throw "REQUIRES_EXACTLY_ONE_MATCHING_EMULATOR"
        }
        $api = (& adb -s $DeviceId shell getprop ro.build.version.sdk | Out-String).Trim()
        $boot = (& adb -s $DeviceId shell getprop sys.boot_completed | Out-String).Trim()
        Record "ANDROID_DEVICE=$DeviceId API=$api BOOT=$boot"
        if ($boot -ne "1") { throw "DEVICE_NOT_READY" }
        $gradleJvm = Gradle "JVM_BUILD" @(
            ":app:assembleDebug", ":app:assembleDebugAndroidTest",
            ":app:testDebugUnitTest", "--console=plain"
        )
        if ($gradleJvm -ne 0) {
            Record "BUILD_OR_JVM_FAILED=TRUE"
        } else {
            $gradleAndroid = Gradle "ANDROID_ALL" @(
                ":app:connectedDebugAndroidTest", "--console=plain"
            )
        }
    } else {
        Record "RESULTS_PROVENANCE=EXISTING_GRADLE_XML_VERIFY_TIMESTAMP_EXTERNALLY"
    }

    $jvm = @(Get-TestCases $jvmFolder "JVM")
    $android = @(Get-TestCases $androidFolder "ANDROID")
    foreach ($kind in @("JVM", "ANDROID")) {
        $cases = if ($kind -eq "JVM") { $jvm } else { $android }
        $total = @($cases).Count
        $passCount = @($cases | Where-Object { $_.Status -eq "PASS" }).Count
        $failCount = @($cases | Where-Object { $_.Status -eq "FAIL" }).Count
        $skipCount = @($cases | Where-Object { $_.Status -eq "SKIP" }).Count
        Record "${kind}_TOTAL=$total"
        Record "${kind}_PASS=$passCount"
        Record "${kind}_FAIL=$failCount"
        Record "${kind}_SKIP=$skipCount"
        foreach ($case in $cases) {
            if ($case.Status -ne "PASS") {
                Record ("CASE=" + $case.Status + " " + $kind + " " +
                    $case.Suite + "#" + $case.Name)
            }
        }
    }
    Record "PHASED_PROCESS_RESTART_DEFAULT_RUN=NOT_A_VALID_RESTART_CHECK"
    Record "PHASED_SAF_DEFAULT_RUN=NOT_A_VALID_PERSISTED_GRANT_CHECK"
    Record "PHASED_RESTART_AND_SAF=SEE_ANDROID_COMPAT_MATRIX_V2_LOGS"
    if ($gradleJvm -ne 0 -or $gradleAndroid -ne 0 -or
        @($jvm | Where-Object Status -eq "FAIL").Count -gt 0 -or
        @($android | Where-Object Status -eq "FAIL").Count -gt 0) {
        throw "REGRESSION_GATE_HAS_FAILURES"
    }
    Record "FINAL_RESULT=PASS"
} catch {
    Record ("FAIL_REASON=" + $_.Exception.Message)
    Record "FINAL_RESULT=FAIL"
    exit 1
} finally {
    Pop-Location
}
