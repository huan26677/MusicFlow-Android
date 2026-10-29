param([string]$DeviceId = "emulator-5554")

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\lyrics-import-ui-e2e-v2.log"
$runner = "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"

function Record([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function Native([string]$exe, [string[]]$arguments) {
    $before = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $lines = @(& $exe @arguments 2>&1)
        $exit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $before }
    $output = $lines -join [char]10
    Record $output
    return [pscustomobject]@{ Exit = $exit; Output = $output }
}

function Instrument([string]$name, [string]$classes, [int]$count) {
    $run = Native "adb" @(
        "-s", $DeviceId, "shell", "am", "instrument", "-w",
        "-e", "class", $classes, $runner
    )
    Record "${name}_EXIT=$($run.Exit)"
    # Android's am instrument sometimes exits 0 even on JUnit failures.
    $noun = if ($count -eq 1) { "test" } else { "tests" }
    if ($run.Exit -ne 0 -or
        !$run.Output.Contains("OK ($count $noun)") -or
        $run.Output -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "${name}_FAILED"
    }
    Record "${name}_TESTS=$count"
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Lyrics Import UI E2E v2" -Encoding UTF8
    Record "DATE=$(Get-Date -Format o)"
    Record "HEAD_BASE=$(git rev-parse --short HEAD)"
    Record "DEVICE=$DeviceId"
    Record "PERSONAL_OFFLINE_AUDIO_NO_STREAMING_RIGHTS_CHECK=TRUE"
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
    if ($reports.Count -eq 0) { throw "MISSING_JVM_TEST_RESULTS" }
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
    if ($jvmFail -ne 0 -or $jvmSkip -ne 0) { throw "JVM_TESTS_FAILED_OR_SKIPPED" }

    foreach ($apk in @(
        "app\build\outputs\apk\debug\app-debug.apk",
        "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
    )) {
        $install = Native "adb" @("-s", $DeviceId, "install", "-r", (Join-Path $root $apk))
        if ($install.Exit -ne 0 -or !$install.Output.Contains("Success")) {
            throw "APK_INSTALL_FAILED"
        }
    }

    $null = Native "adb" @("-s", $DeviceId, "logcat", "-c")
    Instrument "LYRICS_UI_E2E" (
        "com.example.musicflow.core.lyrics.LyricsImportUiE2eTest"
    ) 1
    # Use Android's complete buffer and filter by tag after capture: some
    # host shells handle logcat's '-s *:S' quoting differently.
    $logs = @(& adb -s $DeviceId logcat -d -v brief 2>&1 |
        Select-String "MusicFlowLyricsUiQA")
    $trace = $logs -join [char]10
    Record $trace
    $markers = @(
        "PASS realLrcCreated",
        "PASS realNowPlayingImportButton",
        "PICKER_TOUCHED_REAL_DOCUMENT",
        "PICKER_SELECT_CLICKED",
        "PICKER_RETURNED_TO_MUSICFLOW",
        "PASS productionCallbackPersistedAndRendered",
        "PASS realPlayerSeekUpdatesImportedLyrics"
    )
    foreach ($marker in $markers) {
        if (!$trace.Contains($marker)) {
            throw "MISSING_E2E_MARKER_$marker"
        }
    }
    Record "LYRICS_UI_E2E_MARKERS=7"

    Instrument "LYRICS_V1_REGRESSION" (
        "com.example.musicflow.core.lyrics.LyricsSafImportTest," +
        "com.example.musicflow.core.lyrics.LyricsScreenSyncTest"
    ) 2
    Instrument "PLAYBACK_QUEUE_REGRESSION" (
        "com.example.musicflow.core.playback.PlaybackServiceIntegrationTest," +
        "com.example.musicflow.core.playback.PlaybackQueuePersistenceTest"
    ) 8

    $diff = Native "git" @("diff", "--check")
    Record "GIT_DIFF_CHECK_EXIT=$($diff.Exit)"
    if ($diff.Exit -ne 0) { throw "GIT_DIFF_CHECK_FAILED" }
    Record "TOTAL_ANDROID_TESTS=11"
    Record "FINAL_RESULT=PASS"
} catch {
    Record "FAIL_REASON=$($_.Exception.Message)"
    Record "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
