$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$testPath = Join-Path $root 'app\src\androidTest\java\com\example\musicflow\core\playback\RealPickerSafTest.kt'
$docPath = Join-Path $root 'docs\Ke_Hoach_Du_An.md'

if (-not (Test-Path -LiteralPath $testPath)) { throw 'REAL_PICKER_TEST_NOT_FOUND' }

$bytes = [System.IO.File]::ReadAllBytes($testPath)
$hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191
$encoding = [System.Text.UTF8Encoding]::new($hasBom)
$text = [System.IO.File]::ReadAllText($testPath)
$eol = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$normalized = $text.Replace("`r`n", "`n")

if ($normalized.Contains('LIBRARY_PICKER_ACCESSIBILITY_CLICKED=')) {
    Write-Host 'ALREADY_APPLIED: Accessibility picker click exists. No file changed.'
    exit 0
}
if (-not $normalized.Contains('createAndroidComposeRule<MainActivity>()')) {
    throw 'EXPECTED_COMPOSE_ACTIVITY_RULE_NOT_FOUND'
}
if (-not $normalized.Contains('private fun clickNamedAction(')) {
    throw 'CLICK_NAMED_ACTION_HELPER_NOT_FOUND'
}
if (-not $normalized.Contains('PASS realPickerPlayback') -or -not $normalized.Contains('PASS realPickerPersistedRead')) {
    throw 'SAF_ASSERTIONS_MISSING'
}

$pattern = '(?m)^            composeRule\.waitForIdle\(\)\n            composeRule\.onNodeWithText\("(?<label>[^"\n]+)"\)\.performClick\(\)\n            Log\.i\("MusicFlowRealPickerQA", "PASS libraryPickerButtonClicked"\)'
$actionCandidates = [regex]::Matches($normalized, $pattern)
if ($actionCandidates.Count -ne 1) { throw "UNEXPECTED_PICKER_ACTION_COUNT=$($actionCandidates.Count)" }
$match = $actionCandidates[0]
$label = $match.Groups['label'].Value

$replacement = @'
            // DocumentsUI/permission transitions may detach Compose semantics briefly.
            // Use Android accessibility for this one second-stage UI action.
            val pickerClicked = clickNamedAction(20_000L) { actionLabel ->
                actionLabel.trim().equals("__LABEL__", ignoreCase = true)
            }
            val pickerForeground = instrumentation.uiAutomation
                .rootInActiveWindow?.packageName?.toString()
            Log.i(
                "MusicFlowRealPickerQA",
                "LIBRARY_PICKER_ACCESSIBILITY_CLICKED=$pickerClicked " +
                    "package=$pickerForeground"
            )
            if (!pickerClicked) {
                logPickerUi("LIBRARY_PICKER_CLICK_FAILED")
            }
            assertTrue(
                "Library picker button not accessible/clickable",
                pickerClicked
            )
            Log.i("MusicFlowRealPickerQA", "PASS libraryPickerButtonClicked")
'@
$replacement = $replacement.Replace('__LABEL__', $label).Replace("`r`n", "`n").TrimEnd()
$updated = $normalized.Substring(0, $match.Index) + $replacement +
    $normalized.Substring($match.Index + $match.Length)

$actionPattern = '            val isActionLabel =\n                text\.lowercase\(\) in actionLabels'
$actionMatches = [regex]::Matches($updated, $actionPattern)
if ($actionMatches.Count -ne 1) { throw "UNEXPECTED_ACTION_LOG_FILTER_COUNT=$($actionMatches.Count)" }
$newAction = '            val isActionLabel =' + "`n" +
    '                text.lowercase() in actionLabels ||' + "`n" +
    '                text.equals("__LABEL__", ignoreCase = true)'
$newAction = $newAction.Replace('__LABEL__', $label)
$updated = [regex]::Replace($updated, $actionPattern, $newAction)

# Prepare Markdown update before touching source, so missing headings fail safely.
$docUpdated = $null
$docEncoding = $null
$docEol = "`n"
if (Test-Path -LiteralPath $docPath) {
    $docBytes = [System.IO.File]::ReadAllBytes($docPath)
    $docBom = $docBytes.Length -ge 3 -and $docBytes[0] -eq 239 -and $docBytes[1] -eq 187 -and $docBytes[2] -eq 191
    $docEncoding = [System.Text.UTF8Encoding]::new($docBom)
    $docRaw = [System.IO.File]::ReadAllText($docPath)
    $docEol = if ($docRaw.Contains("`r`n")) { "`r`n" } else { "`n" }
    $docUpdated = $docRaw.Replace("`r`n", "`n")
    $marker = '### 2026-10-28 - Real Picker SAF accessibility click diagnosis'
    if (-not $docUpdated.Contains($marker)) {
        $h = [regex]::Match($docUpdated, '(?m)^## 8\.[^\n]*')
        if (-not $h.Success) { throw 'QA_HISTORY_SECTION_8_NOT_FOUND' }
        $entry = @'

### 2026-10-28 - Real Picker SAF accessibility click diagnosis

- Reviewed main 9231b73a and qa-logs/real-picker-saf-fast-v1.log.
- Compile: PASS. Fast QA: one instrumentation test executed, FAIL.
- Source WAV preparation: PASS; initial Home->Library click: PASS.
- Permissioncontroller not observed; foreground was com.example.musicflow.
- Failure at second Compose UI click (Choose MP3): No compose hierarchies found.
- Test-only change: use clickNamedAction for the Library picker button and log accessibility foreground if it fails.
- Next step: run isolated Fast QA only, assisted with manual DocumentsUI selection when it opens.
- Playback and persisted READ are NOT YET verified. Previous Android compatibility PASS baselines unchanged.
'@
        $docUpdated = $docUpdated.Insert($h.Index + $h.Length, $entry.Replace("`r`n", "`n"))
    }
    $headPattern = '(?m)^(\| Commit HEAD[^|]*\|)[^\n]*$'
    if ([regex]::IsMatch($docUpdated, $headPattern)) {
        $docUpdated = [regex]::Replace($docUpdated, $headPattern,
            '${1} `9231b73a` (Fast QA: second Compose picker action FAIL; accessibility fix pending) |')
    }
}

$backup = Join-Path ([System.IO.Path]::GetTempPath()) ('MusicFlow_RealPicker_before_accessibility_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.kt')
[System.IO.File]::Copy($testPath, $backup, $true)
[System.IO.File]::WriteAllText($testPath, $updated.Replace("`n", $eol), $encoding)
Write-Host 'PASS: Replaced ONLY the second Compose click with accessibility UI action.'
Write-Host "BACKUP=$backup"

if ($null -ne $docUpdated) {
    [System.IO.File]::WriteAllText($docPath, $docUpdated.Replace("`n", $docEol), $docEncoding)
    Write-Host 'PASS: Updated QA handoff and latest reviewed HEAD in docs/Ke_Hoach_Du_An.md.'
}
Write-Host 'NEXT: run compile-only Fast QA, then isolated Real Picker Fast QA.'
