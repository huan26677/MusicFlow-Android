param([string]$DeviceId = "emulator-5554")

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\lyrics-runtime-v1.log"
$runner = "com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner"

function Record([string]$message) {
    Write-Host $message
    Add-Content -LiteralPath $log -Value $message -Encoding UTF8
}

function Native([string]$exe, [string[]]$arguments) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $lines = @(& $exe @arguments 2>&1)
        $exit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $old }
    $text = $lines -join [char]10
    Record $text
    return [pscustomobject]@{ Exit = $exit; Output = $text }
}

function Instrument([string]$name, [string]$classes, [int]$count) {
    $run = Native "adb" @(
        "-s", $DeviceId, "shell", "am", "instrument", "-w",
        "-e", "class", $classes, $runner
    )
    Record "${name}_EXIT=$($run.Exit)"
    if ($run.Exit -ne 0 -or
        !$run.Output.Contains("OK ($count tests)") -or
        $run.Output -match "FAILURES|Error in|INSTRUMENTATION_FAILED") {
        throw "${name}_FAILED"
    }
    Record "${name}_TESTS=$count"
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Lyrics Runtime Acceptance v1" -Encoding UTF8
    Record "DATE=$(Get-Date -Format o)"
    Record "HEAD_BASE=$(git rev-parse --short HEAD)"
    Record "DEVICE=$DeviceId"
    Record "OFFLINE_MUSIC_WITHOUT_ONLINE_RIGHTS=TRUE"
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
    $jvmReports = @(Get-ChildItem "app\build\test-results\testDebugUnitTest" -Filter "TEST-*.xml")
    $totalJvm = 0
    $jvmFailed = 0
    foreach ($report in $jvmReports) {
        [xml]$xml = Get-Content $report.FullName -Raw -Encoding UTF8
        $cases = $xml.GetElementsByTagName("testcase")
        $totalJvm += $cases.Count
        foreach ($case in $cases) {
            if ($case.GetElementsByTagName("failure").Count -gt 0 -or
                $case.GetElementsByTagName("error").Count -gt 0) {
                $jvmFailed++
            }
        }
    }
    $lyricsJvm = $jvmReports | Where-Object {
        $_.Name -like "*LrcParserTest*"
    }
    if ($lyricsJvm.Count -eq 0) { throw "MISSING_PARSER_JVM_XML" }
    [xml]$parserReport = Get-Content $lyricsJvm[0].FullName -Raw -Encoding UTF8
    $parserCount = $parserReport.GetElementsByTagName("testcase").Count
    Record "JVM_TOTAL=$totalJvm"
    Record "JVM_FAIL=$jvmFailed"
    Record "LRC_PARSER_JVM_TESTS=$parserCount"
    if ($jvmFailed -ne 0 -or $parserCount -ne 6) {
        throw "JVM_PARSER_ASSERTIONS_FAILED"
    }

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

    Instrument "LYRICS_RUNTIME" (
        "com.example.musicflow.core.lyrics.LyricsSafImportTest," +
        "com.example.musicflow.core.lyrics.LyricsScreenSyncTest"
    ) 2
    $diagnostics = Native "adb" @(
        "-s", $DeviceId, "logcat", "-d", "-v", "brief",
        "-s", "MusicFlowLyricsQA:I", "*:S"
    )
    foreach ($marker in @(
        "PASS realSafImportAndPersistedRead",
        "PASS invalidLrcNotLinked",
        "PASS realPlaybackSeekLyricsHighlight"
    )) {
        $found = $diagnostics.Output.Contains($marker)
        Record "MARKER_$marker=$found"
        if (!$found) { throw "MISSING_LYRICS_MARKER_$marker" }
    }
    Record "LYRICS_RUNTIME_MARKERS=3"

    Instrument "PLAYBACK_REGRESSION" (
        "com.example.musicflow.core.playback.PlaybackServiceIntegrationTest," +
        "com.example.musicflow.core.playback.PlaybackQueuePersistenceTest"
    ) 8

    $diff = Native "git" @("diff", "--check")
    Record "GIT_DIFF_CHECK_EXIT=$($diff.Exit)"
    if ($diff.Exit -ne 0) { throw "GIT_DIFF_CHECK_FAILED" }
    Record "TOTAL_ANDROID_TESTS=10"
    Record "FINAL_RESULT=PASS"
} catch {
    Record "FAIL_REASON=$($_.Exception.Message)"
    Record "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
