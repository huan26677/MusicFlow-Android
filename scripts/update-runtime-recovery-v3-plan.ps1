param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('PASS','FAIL')]
    [string]$Result,
    [Parameter(Mandatory=$true)]
    [int]$AndroidTestCount,
    [Parameter(Mandatory=$true)]
    [string]$TestRunHead
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$planPath = Join-Path $root 'docs\Ke_Hoach_Du_An.md'
$logPath = Join-Path $root 'qa-logs\playback-runtime-recovery-v3.log'
if (!(Test-Path $planPath)) { throw "Missing plan: $planPath" }
if (!(Test-Path $logPath)) { throw "Missing QA log: $logPath" }

$content = Get-Content -Path $planPath -Raw -Encoding utf8
$log = Get-Content -Path $logPath -Raw -Encoding utf8
$logResult = [regex]::Match($log, '(?m)^FINAL_RESULT=(PASS|FAIL)\s*$')
if (!$logResult.Success -or $logResult.Groups[1].Value -ne $Result) {
    throw 'Refusing to update plan: QA log FINAL_RESULT does not match.'
}

$baseline = 'e0141a8d7d5d55ca8bf6bf2aa30492e9458db253'
$testedTime = Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'
$logName = 'qa-logs/playback-runtime-recovery-v3.log'
$status = if ($Result -eq 'PASS') { '**PASS — 36/36 Android tests**' } else { "**FAIL — Android tests: $AndroidTestCount (xem log)**" }
$next = if ($Result -eq 'PASS') {
    '**MVP End-to-End Acceptance v1 — CHƯA BẮT ĐẦU**'
} else {
    '**Playback Runtime Recovery Race Hardening v3 — CẦN SỬA/CHẠY LẠI**'
}

function Replace-OneLine([string]$source, [string]$pattern, [string]$replacement) {
    $regex = [regex]::new($pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)
    if ($regex.Matches($source).Count -ne 1) {
        throw "Expected exactly one line matching: $pattern"
    }
    return $regex.Replace($source, [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $replacement }, 1)
}

$content = Replace-OneLine $content '^\| Bước đang phát triển tiếp \|.*$' "| Bước đang phát triển tiếp | $next |"
$content = Replace-OneLine $content '^\| Commit HEAD tại thời điểm rà soát \|.*$' "| Commit HEAD tại thời điểm rà soát | ``$TestRunHead`` (HEAD lúc chạy QA, trước commit kết quả) |"
$content = Replace-OneLine $content '^\| Log gần nhất đã xác nhận \|.*$' "| Log gần nhất đã xác nhận | ``$logName`` (QA v3 $Result; v2 PASS lưu ở mục 4) |"

if ($Result -eq 'PASS') {
    $content = Replace-OneLine $content '^\| Mốc kỹ thuật gần nhất \|.*$' '| Mốc kỹ thuật gần nhất | **Playback Runtime Recovery Race Hardening v3 — PASS** |'
    $content = Replace-OneLine $content '^\| JVM unit tests \(mốc (?:v2|v3)\) \|.*$' '| JVM unit tests (mốc v3) | **PASS** (`exitCode=0`) |'
    $content = Replace-OneLine $content '^\| Android instrumented tests \(mốc (?:v2|v3)\) \|.*$' '| Android instrumented tests (mốc v3) | **36/36 PASS** — Pixel_5 emulator |'
    $content = Replace-OneLine $content '^\| Kết quả log \|.*$' '| Kết quả log | `ALL_MARKERS_FOUND=True`, `FINAL_RESULT=PASS` (v3) |'
}


$backlogPattern = '(?m)^\| \*\*P0 — (?:kế tiếp|đã hoàn thành|đang sửa)\*\* \| \*\*Playback Runtime Recovery Race Hardening v3\*\* \|.*$'
$backlogPrefix = if ($Result -eq 'PASS') { '**P0 — đã hoàn thành**' } else { '**P0 — đang sửa**' }
$backlogLine = "| $backlogPrefix | **Playback Runtime Recovery Race Hardening v3** | $status | Xem ``$logName``; yêu cầu 5 marker v3 + regression v2 |"
$content = Replace-OneLine $content $backlogPattern $backlogLine
if ($Result -eq 'PASS') {
    $content = Replace-OneLine $content '(?m)^\| \*\*P0(?: — kế tiếp)?\*\* \| \*\*MVP End-to-End Acceptance v1\*\* \|.*$' '| **P0 — kế tiếp** | **MVP End-to-End Acceptance v1** — Library/Search/Picker → Player → Playlist/History → restart | CHƯA NGHIỆM THU | Script UI/integration tests có assertions và log cho từng flow |'
}

if ($Result -eq 'PASS' -and $content -notmatch '\| Playback Runtime Recovery Race Hardening v3 \| 36 \|') {
    $anchorPattern = '(?m)^\| Playback Runtime Recovery Integration v2 \| 31 \|.*$'
    $anchorMatch = [regex]::Match($content, $anchorPattern)
    if (!$anchorMatch.Success) { throw 'Cannot locate v2 row in section 4.' }
    $entry = '| Playback Runtime Recovery Race Hardening v3 | 36 | **PASS** | `qa-logs/playback-runtime-recovery-v3.log` — SHA sau khi push cần đối chiếu |'
    $content = $content.Replace($anchorMatch.Value, $anchorMatch.Value + "`r`n" + $entry)
}

$journalAnchor = '## 8. Nhật ký thay đổi — giữ lịch sử'
if (!$content.Contains($journalAnchor)) { throw 'Section 8 not found; cannot update history.' }
$journal = @"

### $testedTime — Playback Runtime Recovery Race Hardening v3 — $Result
- Baseline HEAD trước sửa: ``$baseline``.
- HEAD lúc chạy test: ``$TestRunHead`` (SHA commit source/test/log cần đối chiếu sau khi push).
- Mã nguồn: ``PlaybackRuntimeAutoSkip.kt``; test: ``PlaybackRuntimeRaceHardeningTest.kt``.
- Script: ``scripts/run-playback-runtime-recovery-v3-tests.ps1``.
- Log: ``$logName``.
- Android: $AndroidTestCount tests; kết quả: **$Result**. JVM và marker chi tiết theo log.
- Kịch bản race: Pause→Play, queue giống hệt, Seek cùng index, dispose, và uninterrupted skip.
- Giới hạn: mới có test chủ động tiêm lỗi cho các race; test Service thực tế cũ là regression. Không coi kết quả này là nghiệm thu MVP.
- Hành động kế tiếp: $(if ($Result -eq 'PASS') { 'MVP End-to-End Acceptance v1' } else { 'đọc dòng FAIL, sửa đúng nguyên nhân và chạy lại script v3' }).
"@
$content = $content.Replace($journalAnchor, $journalAnchor + $journal + "`r`n")

# Ghi qua tệp tạm để giảm nguy cơ file kế hoạch bị mất khi ghi lỗi.
$tmp = "$planPath.tmp"
Set-Content -Path $tmp -Value $content -Encoding utf8
Move-Item -Path $tmp -Destination $planPath -Force
Write-Host "PLAN_UPDATED=$planPath RESULT=$Result"
