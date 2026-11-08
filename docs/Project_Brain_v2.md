# MusicFlow — Project Brain v2 (Evidence-Driven Learning)

**Tình trạng 2026-11-08:** Brain v2 đã tích hợp vào repository. Dự án Android hiện là bản demo **v7**, phiên bản bộ não **`v7.0-BrainV2-r3`**, engine **`2.0`**. Transcript QA v7 đã commit cho API30/33/36 và signed v6→v7 upgrade API33/36; TalkBack thủ công/full API33/36 regression và benchmark thiết bị thật còn chờ.

## Một bộ não chính duy nhất

`docs/Cross_Session_Project_Brain.json` vẫn là tài liệu nạp đầu tiên cho AI; không tạo bộ nhớ JSON thứ hai. `AGENTS.md` chỉ dẫn và `scripts/project_brain_v2.py` kiểm tra tính nhất quán. Không huấn luyện lại trọng số LLM, không hoạt động liên tục khi không có phiên AI hoặc tác vụ chạy.

## 5 bộ phận triển khai

1. **Evidence Engine:** Mỗi mục `brain_v2.evidence_ledger` có ID, phạm vi và tham chiếu. Bằng chứng `repo` phải tìm được marker trong file tại commit Git được chỉ định; `repo` có `claim_type=test_log` được phép làm chứng cứ QA runtime **chỉ nếu là bản transcript `qa-logs/*.log` đã commit, kèm marker PASS kiểm chứng**, hỗ trợ UTF-8/UTF-16 PowerShell cả ở Git và local. Bằng chứng `local_log` chỉ có giá trị ở máy hiện tại; `artifact` xác minh SHA-256. Log/ APK không commit: AI phiên sau chỉ đánh dấu `unavailable`, không tự gọi PASS. Việc chứng minh cấu hình thiết bị/độc lập tái lập vẫn phải thực hiện bằng chạy QA thực.
2. **Decision Engine:** Mỗi task có `risk, user_value, evidence_gap, urgency, effort` từ 1–5; `score = 3*risk + 2*user_value + 2*evidence_gap + urgency - effort`. P2 ≥24, P3 ≥16, P4 ≥10, P5 dưới 10. P1 chỉ nếu `blocker=true` + score ≥24 + evidence đã xác minh. `sync` tạo lại chỉ số P1–P5, không để AI tự thay nhãn theo cảm tính.
3. **Learning Engine:** `review` chỉ ghi kinh nghiệm sau khi chứng cứ tương ứng thực sự được kiểm tra. Ghi `outcome` là success/failure/partial; gắn scope `repo` hoặc `local_only`, từ chối sự kiện trùng và tự tăng `learning_revision` cùng hậu tố phiên bản `-rN`. Chỉ tối đa 8 kết quả gần nhất; kết quả partial được giữ ở assumptions, thành công/thất bại ở `experience_learned` và lịch sử Git.
4. **Entropy Engine:** Ngân sách JSON 24 KB, loại các mục trùng *chính xác* khi `sync`, không tự xóa các bài học độc nhất hoặc chứng cứ cần thiết. Lịch sử chi tiết vẫn ở Git/tài liệu QA.
5. **Consistency Engine:** `check` so sánh scores và priority_index, kiểm tra marker Git/QA/artefact, schema, những mục lặp, cấu trúc lessons, phát hiện khả năng rò token và báo khi source thay đổi sau commit nền.

## Lệnh sử dụng

```powershell
py -3 scripts/project_brain_v2.py check
py -3 scripts/project_brain_v2.py sync
py -3 -m unittest discover -s scripts -p test_project_brain_v2.py -v
```

Để ghi bài học **sau khi đã bổ sung mục `evidence_ledger` hợp lệ và chạy QA**, dùng:

```powershell
py -3 scripts/project_brain_v2.py review --decision accessibility-v7 --outcome partial --evidence qa-api30 --lesson "Compose 200% đã thử; TalkBack thủ công vẫn cần kiểm chứng"
```

Sau mỗi lần phát triển: đọc JSON → `check` → kiểm tra mã nguồn → triển khai → QA → cập nhật evidence/decision/giả định → `review` → `sync` → `check` → commit/push. Không bắt buộc gọi `review` khi chưa có chứng cứ; không lấy score thay cho phân tích độc lập.

## Kiểm chứng CI và giới hạn

GitHub Actions `.github/workflows/brain-v2-check.yml` tự chạy unit tests và `check` khi có push/PR liên quan tới bộ não. CI có mã nguồn Git đầy đủ và transcript QA v7 được commit; nhưng **không có APK và log QA không commit**. CI có thể xác minh nội dung transcript/source, còn kiểm thử Android thật **chỉ thực sự chạy trên AVD QA khi sử dụng các script chuyên biệt**. GitHub Action PASS không phải tái chạy Android tests.

Các mục quan trọng chưa xác minh: TalkBack thủ công, WCAG thực, hiệu năng nhạc thật, full Android regression API33/36, signed upgrade dữ liệu trên API33/36, thiết bị thật và production.
