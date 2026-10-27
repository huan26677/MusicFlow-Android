$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$testPath = Join-Path $root 'app\src\androidTest\java\com\example\musicflow\core\playback\RealPickerSafTest.kt'
$docPath = Join-Path $root 'docs\Ke_Hoach_Du_An.md'
if (-not (Test-Path -LiteralPath $testPath)) { throw 'REAL_PICKER_TEST_NOT_FOUND' }

$raw = [System.IO.File]::ReadAllBytes($testPath)
$hasBom = $raw.Length -ge 3 -and $raw[0] -eq 239 -and $raw[1] -eq 187 -and $raw[2] -eq 191
$encoding = [System.Text.UTF8Encoding]::new($hasBom)
$original = [System.IO.File]::ReadAllText($testPath)
$eol = if ($original.Contains("`r`n")) { "`r`n" } else { "`n" }
$text = $original.Replace("`r`n", "`n")
$pickLabel = 'Ch' + [char]0x1ECD + 'n MP3'
$old = @'
            Log.i("MusicFlowRealPickerQA", "PASS composeUiReady")
            composeRule.onNodeWithText("__PICK_LABEL__").performClick()
'@
$new = @'
            Log.i("MusicFlowRealPickerQA", "PASS composeUiReady")

            // LibraryRoute asks for media permission when first opened.
            // Deny it so this test continues to exercise the SAF-only path.
            val dialogDeadline = SystemClock.uptimeMillis() + 12_000L
            var libraryStableSince = 0L
            var permissionDialogSeen = false
            var permissionDialogDenied = false
            var previousPackage = ""
            while (SystemClock.uptimeMillis() < dialogDeadline) {
                val root = instrumentation.uiAutomation.rootInActiveWindow
                val foregroundPackage = root?.packageName?.toString().orEmpty()
                if (foregroundPackage != previousPackage) {
                    Log.i("MusicFlowRealPickerQA", "LIBRARY_FOREGROUND=$foregroundPackage")
                    previousPackage = foregroundPackage
                }
                if (foregroundPackage.contains("permissioncontroller", ignoreCase = true)) {
                    libraryStableSince = 0L
                    permissionDialogSeen = true
                    val denyButton = findNode(root) { label ->
                        val normalized = label.trim().lowercase()
                        normalized == "deny" ||
                            (normalized.contains("allow") && normalized.contains("don")) ||
                            (normalized.contains("cho ph") && normalized.startsWith("kh"))
                    }
                    if (clickNode(denyButton)) {
                        permissionDialogDenied = true
                        Log.i("MusicFlowRealPickerQA", "MEDIA_PERMISSION_DENIED_FOR_SAF_TEST")
                    }
                } else if (foregroundPackage == context.packageName) {
                    if (libraryStableSince == 0L) {
                        libraryStableSince = SystemClock.uptimeMillis()
                    }
                    if (SystemClock.uptimeMillis() - libraryStableSince >= 1400L) {
                        break
                    }
                } else {
                    libraryStableSince = 0L
                }
                SystemClock.sleep(200)
            }
            val readyPackage = instrumentation.uiAutomation.rootInActiveWindow
                ?.packageName?.toString()
            Log.i(
                "MusicFlowRealPickerQA",
                "LIBRARY_PERMISSION_CHECK seen=$permissionDialogSeen " +
                    "denied=$permissionDialogDenied current=$readyPackage"
            )
            assertEquals(
                "The Android permission dialog or another window still covers Library",
                context.packageName,
                readyPackage
            )
            composeRule.waitForIdle()
            composeRule.onNodeWithText("__PICK_LABEL__").performClick()
            Log.i("MusicFlowRealPickerQA", "PASS libraryPickerButtonClicked")
'@
$old = $old.Replace("__PICK_LABEL__", $pickLabel).Replace("`r`n", "`n").TrimEnd()
$new = $new.Replace("__PICK_LABEL__", $pickLabel).Replace("`r`n", "`n").TrimEnd()
$count = [regex]::Matches($text, [regex]::Escape($old)).Count
if ($count -ne 1) { throw "UNEXPECTED_COMPOSE_PICKER_BLOCK_COUNT=$count" }
if ($text.Contains('LIBRARY_PERMISSION_CHECK')) { throw 'PATCH_ALREADY_APPLIED' }
if (-not $text.Contains('createAndroidComposeRule<MainActivity>()')) { throw 'COMPOSE_RULE_FIX_MISSING' }
if (-not $text.Contains('PASS realPickerPlayback') -or -not $text.Contains('PASS realPickerPersistedRead')) {
    throw 'SAF_VALIDATION_MARKERS_MISSING'
}
$backup = Join-Path ([System.IO.Path]::GetTempPath()) ('MusicFlow_RealPicker_before_permission_fix_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.kt')
[System.IO.File]::Copy($testPath, $backup, $true)
$updated = $text.Replace($old, $new).Replace("`n", $eol)
[System.IO.File]::WriteAllText($testPath, $updated, $encoding)
Write-Host 'PASS: isolated SAF test now diagnoses/denies runtime media permission dialog.'
Write-Host "BACKUP=$backup"

if (Test-Path -LiteralPath $docPath) {
    $doc = [System.IO.File]::ReadAllText($docPath)
    $noteMarker = '### 2026-10-28 - Real Picker SAF library permission dialog diagnosis'
    if (-not $doc.Contains($noteMarker)) {
        $heading = [regex]::Match($doc, '(?m)^## 8\.[^\r\n]*')
        if (-not $heading.Success) { throw 'QA_HISTORY_HEADING_NOT_FOUND' }
        $note = @'

### 2026-10-28 - Real Picker SAF library permission dialog diagnosis

- GitHub main reviewed: dfd8e452084eef3da967ff294ae86fb22fb82e7f.
- Fast QA: Kotlin compile PASS, exactly 1 test executed, final FAIL.
- Actual failure: No compose hierarchies found at second UI action (Choose MP3).
- PASS composeUiReady was logged immediately after Home->Library click.
- LibraryRoute can request READ_EXTERNAL_STORAGE on Android 11.
- Added diagnosis for foreground package and denial of permission prompt to preserve SAF-only testing.
- Fast QA remains the only required run until it PASS; full regression later.
- Playback + persisted READ not yet verified. No MVP/release acceptance.
'@
        $doc = $doc.Insert($heading.Index + $heading.Length, $note)
        $docRaw = [System.IO.File]::ReadAllBytes($docPath)
        $docBom = $docRaw.Length -ge 3 -and $docRaw[0] -eq 239 -and $docRaw[1] -eq 187 -and $docRaw[2] -eq 191
        $docEncoding = [System.Text.UTF8Encoding]::new($docBom)
        [System.IO.File]::WriteAllText($docPath, $doc, $docEncoding)
        Write-Host 'PASS: cross-chat QA handoff appended.'
    } else {
        Write-Host 'SKIP: QA handoff already recorded.'
    }
}
Write-Host 'NEXT: run compile-only FAST QA; then real-picker FAST QA (one test).'
