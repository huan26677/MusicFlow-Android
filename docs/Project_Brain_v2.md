# MusicFlow — Project Brain v2 (Evidence-Driven Learning)

**Tình trạng 2026-11-08:** Brain v2 đã tích hợp vào repository. Bản demo Android mới nhất là **v8 Keyboard & Search Index**; phiên bản bộ não được kiểm tra từ trường `meta.evolution_version` ở JSON gốc, engine **`2.0`**. QA v8: 137 Android UI/regression API30, 40 targeted mỗi API33/36, signed v7→v8 giữ Room/favorites/SAF trên API33 PASS. **TalkBack thủ công, full 98 regression API33/36, signed upgrade v7→v8 API36 và benchmark thiết bị thật vẫn chưa có nghiệm thu.**

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

## Modular Testing & Impact-driven Regression (bắt buộc)

Brain v2 giữ **chính sách** trong `brain_v2.test_architecture`. Bản đồ 19 module, quan hệ phụ thuộc, nguồn mã và nhóm smoke nằm ở `qa/impact_modules.json`; **105 lớp / 457 phương thức @Test** được tự đọc từ code và liệt kê ở `docs/Modular_Test_Catalog.md`. Đây là **danh mục QA**, không phải file não thứ hai. Khi thêm test, cập nhật mapping rồi tạo lại catalog.

**Trước sửa:** chọn module đổi trực tiếp và module phụ thuộc gián tiếp với:
```powershell
py -3 scripts/impact_regression.py validate
py -3 scripts/impact_regression.py plan --files app/src/main/java/com/example/musicflow/feature/search/SearchViewModel.kt
py -3 scripts/impact_regression.py plan --git-diff --json
```

Chạy toàn bộ test thuộc module sửa và smoke/contract test của downstream, không quét toàn dự án nếu phân tích tác động chứng minh các vùng khác không phụ thuộc. Với quyền truy cập, tài khoản Owner/Admin, Room/SAF, cấu hình Android, file chưa phân loại, APK bàn giao, lỗi lặp hoặc phạm vi lớn, **phải mở rộng full regression, signed upgrade/acceptance khi phù hợp**. Không dùng selective để bỏ qua chốt an toàn. Mỗi báo cáo ghi các test ID được chọn, lệnh/SDK, PASS-FAIL-SKIP, log/chứng cứ và lý do những phần khác không chạy. `SignedApkUpgradeV6Test` cần seed → cài đè APK cùng chữ ký → verify, không chạy hai method riêng lẻ. Xem thêm `docs/Test_Impact_Policy.md`.

CI chạy `scripts/test_impact_regression.py`, `impact_regression.py validate` và đối chiếu catalog mới sinh với bản commit, **không tự khẳng định Android UI đã chạy trên CI**.

GitHub Actions `.github/workflows/brain-v2-check.yml` tự chạy unit tests và `check` khi có push/PR liên quan tới bộ não. CI có mã nguồn Git đầy đủ và transcript QA v7 được commit; nhưng **không có APK và log QA không commit**. CI có thể xác minh nội dung transcript/source, còn kiểm thử Android thật **chỉ thực sự chạy trên AVD QA khi sử dụng các script chuyên biệt**. GitHub Action PASS không phải tái chạy Android tests.

Các mục quan trọng chưa xác minh: TalkBack thủ công, WCAG trên ảnh phủ thực, hiệu năng nhạc thật, full Android regression API33/36, signed upgrade v7→v8 trên API36, thiết bị thật và production.
