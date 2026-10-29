param([string]$DeviceId = "emulator-5554")

# Full, unfiltered Android instrumentation gate on one API 30 emulator.
# Unlike phased process-restart/SAF tests this run does not claim to reboot
# the app; those scenarios are covered by separate multi-process QA scripts.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\full-product-regression-v1-part3.log"
$jvmResults = Join-Path $root "app\build\test-results\testDebugUnitTest"
$androidResults = Join-Path $root "app\build\outputs\androidTest-results\connected\debug"

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
                [pscustomobject]@{
                    Status = $status
                    Suite = [string]$case.classname
                    Name = [string]$case.name
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
        }
    }
    return $result
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Full Product Regression v1 - Part 3" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
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

    # Prevent stale XML from an earlier test attempt from appearing fresh.
    if (Test-Path $androidResults) {
        Get-ChildItem -LiteralPath $androidResults -Filter "TEST-*.xml" -File |
            Remove-Item -Force
    }
    $androidExit = RunGradle "GRADLE_ANDROID" @(":app:connectedDebugAndroidTest","--console=plain")
    $android = @(CollectCases $androidResults "ANDROID")
    Log "PHASED_SAF_RESTART_SKIPS=SEE_ANDROID_COMPAT_MATRIX_V2"
    Log "ANDROID_RUN_FRESH_XML=TRUE"
    $diffOld = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & git diff --check 2>&1 | ForEach-Object { Log ([string]$_) }
        $diffExit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $diffOld }
    Log "GIT_DIFF_CHECK_EXIT=$diffExit"
    if ($androidExit -ne 0 -or $diffExit -ne 0 -or
        @($jvm | Where-Object { $_.Status -eq "FAIL" }).Count -gt 0 -or
        @($android | Where-Object { $_.Status -eq "FAIL" }).Count -gt 0) {
        throw "FULL_REGRESSION_BLOCKED"
    }
    Log "FINAL_RESULT=PASS"
} catch {
    Log ("FAIL_REASON=" + $_.Exception.Message)
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
