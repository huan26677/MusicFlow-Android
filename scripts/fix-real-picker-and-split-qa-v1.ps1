$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$testPath = Join-Path $root 'app\src\androidTest\java\com\example\musicflow\core\playback\RealPickerSafTest.kt'
$docPath = Join-Path $root 'docs\Ke_Hoach_Du_An.md'

if (-not (Test-Path -LiteralPath $testPath)) {
    throw "TEST_FILE_NOT_FOUND: $testPath"
}

$text = [System.IO.File]::ReadAllText($testPath)
$startToken = '    private fun selectRealDocument(title: String): Boolean {'
$start = $text.IndexOf($startToken, [System.StringComparison]::Ordinal)
if ($start -lt 0) { throw 'SELECT_REAL_DOCUMENT_START_NOT_FOUND' }
$duplicate = $text.IndexOf($startToken, $start + $startToken.Length, [System.StringComparison]::Ordinal)
if ($duplicate -ge 0) { throw 'DUPLICATE_SELECT_REAL_DOCUMENT' }
$end = $text.IndexOf('    @Test', $start + $startToken.Length, [System.StringComparison]::Ordinal)
if ($end -lt 0) { throw 'NEXT_TEST_MARKER_NOT_FOUND' }

$broken = $text.Substring($start, $end - $start)
if (-not $broken.Contains('MANUAL_SELECTION_REQUIRED')) {
    throw 'UNEXPECTED_TEST_VERSION: manual-assisted function not found'
}
$remaining = $text.Substring($end)
if (-not $remaining.Contains('fun realOpenMultipleDocumentsGrantsPersistedReadAndPlays()')) {
    throw 'REAL_PICKER_TEST_NOT_FOUND_AFTER_REPAIR_REGION'
}
if (-not $remaining.Contains('PASS realPickerPlayback') -or -not $remaining.Contains('PASS realPickerPersistedRead')) {
    throw 'PLAYBACK_OR_PERMISSION_ASSERTIONS_MISSING'
}

$nl = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$replacement = @'
    private fun selectRealDocument(title: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 90_000L
        var documentVisible = false

        Log.i("MusicFlowRealPickerQA", "MANUAL_SELECTION_REQUIRED file=$title")

        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            val currentPackage = root?.packageName?.toString()

            if (currentPackage == "com.google.android.documentsui") {
                if (!documentVisible && findDocumentTitleNode(root, title) != null) {
                    documentVisible = true
                    Log.i(
                        "MusicFlowRealPickerQA",
                        "DOCUMENT_VISIBLE_READY_FOR_SELECTION"
                    )
                }
            }

            if (currentPackage == context.packageName) {
                Log.i("MusicFlowRealPickerQA", "PICKER_RETURNED_TO_APP")
                return documentVisible
            }

            if (currentPackage != null &&
                currentPackage != "com.google.android.documentsui"
            ) {
                Log.e(
                    "MusicFlowRealPickerQA",
                    "UNEXPECTED_PICKER_PACKAGE=$currentPackage"
                )
                return false
            }

            SystemClock.sleep(250)
        }

        Log.e("MusicFlowRealPickerQA", "MANUAL_SELECTION_TIMEOUT")
        return false
    }
'@
$replacement = $replacement.Replace("`r`n", "`n").Replace("`n", $nl).TrimEnd()
$updated = $text.Substring(0, $start) + $replacement + $nl + $nl + $remaining

if ([regex]::Matches($updated, 'private fun selectRealDocument\(').Count -ne 1) {
    throw 'REPAIR_VALIDATION_FAILED: duplicate selectRealDocument'
}
if (-not $updated.Contains('    @Test' + $nl + '    fun realOpenMultipleDocumentsGrantsPersistedReadAndPlays')) {
    throw 'REPAIR_VALIDATION_FAILED: adjacent test marker missing'
}
if ($updated.Contains('PICKER_NOT_RETURNED_TO_APP')) {
    throw 'REPAIR_VALIDATION_FAILED: leftover old function code'
}

# Save a copy outside the repository so "git add ." does not stage a backup.
$backup = Join-Path ([System.IO.Path]::GetTempPath()) ('MusicFlow_RealPickerSafTest_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.kt')
[System.IO.File]::Copy($testPath, $backup, $true)

# Preserve a BOM if one exists; otherwise write UTF-8 without BOM.
$raw = [System.IO.File]::ReadAllBytes($testPath)
$hasBom = $raw.Length -ge 3 -and $raw[0] -eq 239 -and $raw[1] -eq 187 -and $raw[2] -eq 191
$encoding = [System.Text.UTF8Encoding]::new($hasBom)
[System.IO.File]::WriteAllText($testPath, $updated, $encoding)
Write-Host 'PASS: Replaced broken selectRealDocument region exactly once.'
Write-Host "BACKUP=$backup"

if (Test-Path -LiteralPath $docPath) {
    $doc = [System.IO.File]::ReadAllText($docPath)
    $marker = '### 2026-10-28 - Real Picker SAF isolated QA repair'
    if (-not $doc.Contains($marker)) {
        $headingMatch = [regex]::Match($doc, '(?m)^## 8\.[^\r\n]*')
        if (-not $headingMatch.Success) { throw 'DOC_HISTORY_SECTION_8_NOT_FOUND' }
        $entry = @'

### 2026-10-28 - Real Picker SAF isolated QA repair

- Reviewed GitHub main d5ed525c: broken Kotlin source includes leftover code below selectRealDocument().
- Replaced one complete function region and kept playback / persisted READ assertions unchanged.
- Compile only: scripts/run-real-picker-saf-fast-v1-tests.ps1 -CompileOnly.
- Isolated device QA: scripts/run-real-picker-saf-fast-v1-tests.ps1; log qa-logs/real-picker-saf-fast-v1.log.
- Full regression: scripts/run-real-picker-saf-v1-tests.ps1 after fast PASS.
- Current Real Picker SAF status: PENDING; do not claim PASS before a successful log.
- Prior Android API 29/30/33/36 PASS results remain historical baselines, not substitute for release regression.
'@
        $insertAt = $headingMatch.Index + $headingMatch.Length
        $docNl = if ($doc.Contains("`r`n")) { "`r`n" } else { "`n" }
        $doc = $doc.Insert($insertAt, $entry.Replace("`r`n", "`n").Replace("`n", $docNl))
        $docRaw = [System.IO.File]::ReadAllBytes($docPath)
        $docBom = $docRaw.Length -ge 3 -and $docRaw[0] -eq 239 -and $docRaw[1] -eq 187 -and $docRaw[2] -eq 191
        [System.IO.File]::WriteAllText($docPath, $doc, ([System.Text.UTF8Encoding]::new($docBom)))
        Write-Host 'PASS: Appended QA handoff note to docs/Ke_Hoach_Du_An.md.'
    } else {
        Write-Host 'SKIP: QA handoff note already exists.'
    }
}

Write-Host 'NEXT: run compile-only Fast QA before any device test.'
