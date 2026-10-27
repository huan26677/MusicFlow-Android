$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$testPath = Join-Path $root 'app\src\androidTest\java\com\example\musicflow\core\playback\RealPickerSafTest.kt'
$docPath = Join-Path $root 'docs\Ke_Hoach_Du_An.md'

if (!(Test-Path -LiteralPath $testPath)) { throw "TEST_FILE_NOT_FOUND: $testPath" }
$text = [System.IO.File]::ReadAllText($testPath)
$startToken = '    private fun selectRealDocument(title: String): Boolean {'
$start = $text.IndexOf($startToken, [System.StringComparison]::Ordinal)
if ($start -lt 0) { throw 'SELECT_REAL_DOCUMENT_START_NOT_FOUND' }
$secondStart = $text.IndexOf($startToken, $start + $startToken.Length, [System.StringComparison]::Ordinal)
if ($secondStart -ge 0) { throw 'DUPLICATE_SELECT_REAL_DOCUMENT' }
$end = $text.IndexOf('    @Test', $start + $startToken.Length, [System.StringComparison]::Ordinal)
if ($end -lt 0) { throw 'NEXT_TEST_MARKER_NOT_FOUND' }
$broken = $text.Substring($start, $end - $start)
if (!$broken.Contains('MANUAL_SELECTION_REQUIRED')) { throw 'UNEXPECTED_TEST_VERSION' }
if (!$text.Substring($end, [Math]::Min(130, $text.Length - $end)).Contains('realOpenMultipleDocumentsGrantsPersistedReadAndPlays')) {
    throw 'NEXT_TEST_NOT_THE_REAL_PICKER_TEST'
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
$replacement = $replacement.Replace("`n", $nl)
$updated = $text.Substring(0, $start) + $replacement + $text.Substring($end)
if ($updated.Contains('assertTrue(') -eq $false) { throw 'ASSERTIONS_MISSING_IN_RESULT' }
[System.IO.File]::WriteAllText($testPath, $updated, (New-Object System.Text.UTF8Encoding($false)))
Write-Host 'PASS: Dọn sạch đoạn selectRealDocument cũ bị sót.'

if (Test-Path -LiteralPath $docPath) {
    $doc = [System.IO.File]::ReadAllText($docPath)
    $doc = [regex]::Replace($doc, '(?m)^\| Commit HEAD tại thời điểm rà soát \|[^\r\n]*', '| Commit HEAD tại thời điểm rà soát | `d5ed525c` (trước khi tách QA; xem commit mới nhất trên GitHub sau khi push) |')
    $doc = [regex]::Replace($doc, '(?m)^\| Bước đang phát triển tiếp \|[^\r\n]*', '| Bước đang phát triển tiếp | **Real Picker SAF v1 — chạy QA nhanh riêng; chưa PASS** |')
    $section = '## 8. Nhật ký thay đổi — giữ lịch sử'
    $entry = @'
### 2026-10-27 — Real Picker SAF v1: tách QA để sửa nhanh

- HEAD được đối chiếu trước khi sửa: `d5ed525c3b856f46090c6374e749f9b7181512df`.
- Log lỗi: `qa-logs/real-picker-saf-v1.log`; Kotlin instrumentation chưa biên dịch do phần mã cũ sót sau `selectRealDocument()`.
- Đã sửa hàm `selectRealDocument()` thành một khối duy nhất, giữ kiểm tra playback và persisted READ.
- Chạy **nhanh**: `scripts/run-real-picker-saf-fast-v1-tests.ps1`, chỉ một bài RealPickerSafTest; log `qa-logs/real-picker-saf-fast-v1.log`.
- Chạy **đầy đủ**: `scripts/run-real-picker-saf-v1-tests.ps1`, sau khi vòng nhanh PASS; log riêng như trước.
- Không đánh dấu PASS khi Kotlin compile fail, picker không trả về MusicFlow hoặc thiếu marker playback/persisted READ.
- Các mốc Android API 29/30/33/36 có PASS lịch sử, nhưng cần hồi quy đầy đủ sau khi thay đổi liên quan.
- Trạng thái toàn dự án: CHƯA NGHIỆM THU MVP.
'@
    if (!$doc.Contains('Real Picker SAF v1: tách QA để sửa nhanh')) {
        if (!$doc.Contains($section)) { throw 'DOC_HISTORY_SECTION_NOT_FOUND' }
        $doc = $doc.Replace($section, $section + "`r`n`r`n" + $entry)
    }
    [System.IO.File]::WriteAllText($docPath, $doc, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host 'PASS: Đã cập nhật docs/Ke_Hoach_Du_An.md.'
}
Write-Host 'Hoàn tất. Bước kế tiếp: chạy script FAST, chỉ một bài Real Picker.'
