$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$testPath = Join-Path $root 'app\src\androidTest\java\com\example\musicflow\core\playback\RealPickerSafTest.kt'
$docPath = Join-Path $root 'docs\Ke_Hoach_Du_An.md'
if (-not (Test-Path -LiteralPath $testPath)) { throw 'REAL_PICKER_TEST_NOT_FOUND' }

# All script text is ASCII-safe for Windows PowerShell 5.1.
# Read/modify/write UTF-8 Kotlin and Markdown without damaging non-ASCII content.
$raw = [System.IO.File]::ReadAllBytes($testPath)
$hasBom = $raw.Length -ge 3 -and $raw[0] -eq 239 -and $raw[1] -eq 187 -and $raw[2] -eq 191
$enc = [System.Text.UTF8Encoding]::new($hasBom)
$text = [System.IO.File]::ReadAllText($testPath)
$eol = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$text = $text.Replace("`r`n", "`n")

function Replace-One([string]$body, [string]$old, [string]$new, [string]$label) {
    $count = [regex]::Matches($body, [regex]::Escape($old)).Count
    if ($count -ne 1) { throw "UNEXPECTED_$($label)_COUNT=$count" }
    return $body.Replace($old, $new)
}

$text = Replace-One $text `
    'import androidx.compose.ui.test.junit4.createEmptyComposeRule' `
    'import androidx.compose.ui.test.junit4.createAndroidComposeRule' 'COMPOSE_RULE_IMPORT'

$text = Replace-One $text `
    'import androidx.test.core.app.ActivityScenario' '' 'ACTIVITY_SCENARIO_IMPORT'

$text = Replace-One $text `
    '    @get:Rule val composeRule = createEmptyComposeRule()' `
    '    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()' 'COMPOSE_RULE_FIELD'

$text = Replace-One $text `
    '        var scenario: ActivityScenario<MainActivity>? = null' '' 'SCENARIO_FIELD'

$oldLaunch = @'
            val launched = ActivityScenario.launch(MainActivity::class.java)
            scenario = launched
            launched.onActivity { activity ->
'@
$newLaunch = @'
            val activity = composeRule.activity
            instrumentation.runOnMainSync {
'@
$text = Replace-One $text $oldLaunch.TrimEnd() $newLaunch.TrimEnd() 'LAUNCH_ACTIVITY'

$text = Replace-One $text `
    '            scenario?.close()' '' 'SCENARIO_CLOSE'

# Match the original multi-line Compose action without embedding Unicode in this PS1.
$uiPattern = '(?m)^            composeRule\.onNodeWithText\([^\r\n]+\)\n                \.performClick\(\)'
$uiMatches = [regex]::Matches($text, $uiPattern)
if ($uiMatches.Count -ne 1) { throw "UNEXPECTED_COMPOSE_UI_ACTION_COUNT=$($uiMatches.Count)" }
$uiMatch = $uiMatches[0]
$newUi = "            composeRule.waitForIdle()`n" + $uiMatch.Value + "`n" +
    '            Log.i("MusicFlowRealPickerQA", "PASS composeUiReady")'
$text = $text.Substring(0, $uiMatch.Index) + $newUi +
    $text.Substring($uiMatch.Index + $uiMatch.Length)

if ($text.Contains('createEmptyComposeRule()') -or $text.Contains('ActivityScenario.launch(')) {
    throw 'LEFTOVER_OLD_RULE_OR_ACTIVITY_LAUNCH'
}
if (-not $text.Contains('PASS realPickerPlayback') -or -not $text.Contains('PASS realPickerPersistedRead')) {
    throw 'REQUIRED_SAF_ASSERTIONS_MISSING'
}
if ([regex]::Matches($text, 'private fun selectRealDocument\(').Count -ne 1) {
    throw 'SELECT_REAL_DOCUMENT_COUNT_INVALID'
}

$backup = Join-Path ([System.IO.Path]::GetTempPath()) ('MusicFlow_RealPicker_before_compose_rule_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.kt')
[System.IO.File]::Copy($testPath, $backup, $true)
[System.IO.File]::WriteAllText($testPath, $text.Replace("`n", $eol), $enc)
Write-Host 'PASS: createAndroidComposeRule configured; old ActivityScenario launch removed.'
Write-Host "BACKUP=$backup"

if (Test-Path -LiteralPath $docPath) {
    $doc = [System.IO.File]::ReadAllText($docPath)
    $marker = '### 2026-10-28 - Real Picker SAF Compose rule fix'
    if (-not $doc.Contains($marker)) {
        $h = [regex]::Match($doc, '(?m)^## 8\.[^\r\n]*')
        if (-not $h.Success) { throw 'QA_HISTORY_HEADING_NOT_FOUND' }
        $note = @'

### 2026-10-28 - Real Picker SAF Compose rule fix

- Reviewed main 19e39627 and qa-logs/real-picker-saf-fast-v1.log.
- FAST QA compile: PASS; 1 Android instrumentation test started.
- FAIL at RealPickerSafTest.kt:387: No compose hierarchies found.
- Prepared change: createAndroidComposeRule<MainActivity>(), remove manual ActivityScenario.launch, synchronize Compose before opening picker.
- Do not change PlaybackService or previous PASS baselines.
- Next run: scripts/run-real-picker-saf-fast-v1-tests.ps1 -CompileOnly; then scripts/run-real-picker-saf-fast-v1-tests.ps1.
- Real Picker SAF is NOT yet PASS; perform full regression only after isolated Fast QA PASS.
'@
        $doc = $doc.Insert($h.Index + $h.Length, $note)
        $doc = [regex]::Replace($doc, '(?m)^(\| Commit HEAD[^|]*\|)[^\r\n]*$', '${1} `19e39627` (Fast QA: Compose hierarchy FAIL; next fix pending) |')
        $docRaw = [System.IO.File]::ReadAllBytes($docPath)
        $docBom = $docRaw.Length -ge 3 -and $docRaw[0] -eq 239 -and $docRaw[1] -eq 187 -and $docRaw[2] -eq 191
        $docEncoding = [System.Text.UTF8Encoding]::new($docBom)
        [System.IO.File]::WriteAllText($docPath, $doc, $docEncoding)
        Write-Host 'PASS: QA handoff note added to docs/Ke_Hoach_Du_An.md.'
    } else {
        Write-Host 'SKIP: QA handoff note already recorded.'
    }
}
Write-Host 'NEXT: run isolated Kotlin compile, then one-test Fast QA.'
