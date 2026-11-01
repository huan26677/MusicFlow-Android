param([string]$DeviceId = "emulator-5554")

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\playback-speed-touch-v2.log"
$runner = "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"

function Record([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function Native([string]$exe, [string[]]$arguments) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $lines = @(& $exe @arguments 2>&1)
        $exit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $old }
    $output = $lines -join [char]10
    Record $output
    return [pscustomobject]@{ Exit = $exit; Output = $output }
}

function Instrument([string]$label, [string]$classes, [int]$count) {
    $run = Native "adb" @(
        "-s", $DeviceId, "shell", "am", "instrument", "-w",
        "-e", "class", $classes, $runner
    )
    Record "${label}_EXIT=$($run.Exit)"
    $noun = if ($count -eq 1) { "test" } else { "tests" }
    if ($run.Exit -ne 0 -or
        !$run.Output.Contains("OK ($count $noun)") -or
        $run.Output -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "${label}_FAILED"
    }
    Record "${label}_TESTS=$count"
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Playback Speed Touch Interaction QA v2" -Encoding UTF8
    Record "DATE=$(Get-Date -Format o)"
    Record "HEAD_BASE=$(git rev-parse --short HEAD)"
    Record "DEVICE=$DeviceId"
    Record "OFFLINE_USER_AUDIO_REQUIRES_ONLINE_RIGHTS_CHECK=FALSE"
    $api = Native "adb" @("-s", $DeviceId, "shell", "getprop", "ro.build.version.sdk")
    $boot = Native "adb" @("-s", $DeviceId, "shell", "getprop", "sys.boot_completed")
    if ($api.Exit -ne 0 -or $api.Output.Trim() -ne "30" -or
        $boot.Exit -ne 0 -or $boot.Output.Trim() -ne "1") {
        throw "EXPECTED_BOOTED_API_30_EMULATOR"
    }

    $build = Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Record "BUILD_JVM_EXIT=$($build.Exit)"
    if ($build.Exit -ne 0 -or !$build.Output.Contains("BUILD SUCCESSFUL")) {
        throw "GRADLE_BUILD_OR_JVM_FAILED"
    }
    $reports = @(Get-ChildItem "app\build\test-results\testDebugUnitTest" -Filter "TEST-*.xml")
    if ($reports.Count -eq 0) { throw "MISSING_JVM_REPORTS" }
    $jvmTotal = 0
    $jvmFail = 0
    $jvmSkip = 0
    foreach ($report in $reports) {
        [xml]$xml = Get-Content $report.FullName -Raw -Encoding UTF8
        foreach ($case in $xml.GetElementsByTagName("testcase")) {
            $jvmTotal++
            if ($case.GetElementsByTagName("failure").Count -gt 0 -or
                $case.GetElementsByTagName("error").Count -gt 0) { $jvmFail++ }
            elseif ($case.GetElementsByTagName("skipped").Count -gt 0) { $jvmSkip++ }
        }
    }
    Record "JVM_TOTAL=$jvmTotal"
    Record "JVM_FAIL=$jvmFail"
    Record "JVM_SKIP=$jvmSkip"
    if ($jvmFail -ne 0 -or $jvmSkip -ne 0) { throw "JVM_REGRESSION_FAILED" }

    foreach ($apk in @(
        "app\build\outputs\apk\debug\app-debug.apk",
        "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    )) {
        $installed = Native "adb" @("-s", $DeviceId, "install", "-r", (Join-Path $root $apk))
        if ($installed.Exit -ne 0 -or !$installed.Output.Contains("Success")) {
            throw "APK_INSTALL_FAILED"
        }
    }
    $null = Native "adb" @("-s", $DeviceId, "logcat", "-c")

    Instrument "SPEED_RUNTIME" (
        "com.example.musicflow.core.playback.PlaybackSpeedRuntimeTest"
    ) 3
    $traceLines = @(& adb -s $DeviceId logcat -d -v brief 2>&1 |
        Select-String "MusicFlowSpeedQA|MusicFlowSpeedTouchQA")
    $trace = $traceLines -join [char]10
    Record $trace
    $markers = @(
        "PASS offlineMediaSessionReady",
        "PASS invalidSpeedsRejected",
        "PASS speedLimitsAndQueueStable",
        "PASS fiveUiSpeedsReachMediaSession",
        "PASS uiPauseResumeKeepsSpeed",
        "PASS nextTrackKeepsSpeedAndQueue",
        "PASS uiSpeedResetToNormal",
        "PASS physicalTapFirstSpeed",
        "PASS physicalTapVisibleMiddleSpeeds",
        "PASS physicalSwipeAndTapLastSpeed",
        "PASS fivePhysicalSpeedTouches"
    )
    foreach ($marker in $markers) {
        if (!$trace.Contains($marker)) { throw "MISSING_SPEED_MARKER_$marker" }
    }
    Record "SPEED_RUNTIME_MARKERS=11"

    # Independent instrumentation session to detect side effects against
    # the real playback service and persisted queue implementation.
    Instrument "PLAYBACK_QUEUE_REGRESSION" (
        "com.example.musicflow.core.playback.PlaybackServiceIntegrationTest," +
        "com.example.musicflow.core.playback.PlaybackQueuePersistenceTest"
    ) 8

    # With font_scale=1.5, the final speed chip genuinely starts beyond
    # the horizontal viewport. Verify it becomes visible after a real swipe.
    # Always restore the original emulator setting, including unset/null.
    $priorFontScale = Native "adb" @(
        "-s", $DeviceId, "shell", "settings", "get", "system", "font_scale"
    )
    if ($priorFontScale.Exit -ne 0) { throw "READ_FONT_SCALE_FAILED" }
    $prior = $priorFontScale.Output.Trim()
    Record "ORIGINAL_FONT_SCALE=$prior"
    try {
        $setScale = Native "adb" @(
            "-s", $DeviceId, "shell", "settings", "put", "system",
            "font_scale", "1.5"
        )
        if ($setScale.Exit -ne 0) { throw "SET_LARGE_FONT_FAILED" }
        Start-Sleep -Seconds 2
        $null = Native "adb" @("-s", $DeviceId, "logcat", "-c")
        Instrument "LARGE_FONT_PHYSICAL_TOUCH" (
            "com.example.musicflow.core.playback.PlaybackSpeedRuntimeTest#" +
            "physicalTouchOnSpeedChipsWorksAcrossHorizontalScroll"
        ) 1
        $largeLines = @(& adb -s $DeviceId logcat -d -v brief 2>&1 |
            Select-String "MusicFlowSpeedTouchQA")
        $largeTrace = $largeLines -join [char]10
        Record $largeTrace
        foreach ($marker in @(
            "PASS fivePhysicalSpeedTouches",
            "PASS horizontalOverflowRevealedByPhysicalSwipe"
        )) {
            if (!$largeTrace.Contains($marker)) {
                throw "LARGE_FONT_MISSING_$marker"
            }
        }
        Record "LARGE_FONT_HORIZONTAL_OVERFLOW_REVEALED=TRUE"
    } finally {
        $restoreArgs = if ($prior -eq "null" -or $prior -eq "") {
            @("-s", $DeviceId, "shell", "settings", "delete", "system", "font_scale")
        } else {
            @("-s", $DeviceId, "shell", "settings", "put", "system", "font_scale", $prior)
        }
        $restored = Native "adb" $restoreArgs
        Record "FONT_SCALE_RESTORE_EXIT=$($restored.Exit)"
        if ($restored.Exit -ne 0) { throw "FONT_SCALE_RESTORE_FAILED" }
    }

    $diff = Native "git" @("diff", "--check")
    Record "GIT_DIFF_CHECK_EXIT=$($diff.Exit)"
    if ($diff.Exit -ne 0) { throw "GIT_DIFF_CHECK_FAILED" }
    Record "TOTAL_ANDROID_TESTS=12"
    Record "FINAL_RESULT=PASS"
} catch {
    Record "FAIL_REASON=$($_.Exception.Message)"
    Record "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
