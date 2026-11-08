# MusicFlow — Chính sách Modular Testing & Impact-driven Regression

**Vai trò:** Software QA Lead / Test Automation Architect. Có hiệu lực trước mọi bug fix, refactor và tính năng mới của MusicFlow. Phạm vi vẫn là ứng dụng Android + Web Admin phục vụ bài báo cáo/demo.

## 1. Một catalog, một bộ não

- **Bộ não duy nhất:** `docs/Cross_Session_Project_Brain.json` → `brain_v2.test_architecture`. Không tạo brain thứ hai.
- **Registry kiểm thử:** `qa/impact_modules.json` chứa 19 module, đường dẫn nguồn, dependencies `requires`, lớp test hiện có và smoke case phải chạy cho từng consumer bị ảnh hưởng.
- **Danh sách từng test case:** `docs/Modular_Test_Catalog.md` tự sinh từ **phương thức Kotlin `@Test` có thật**. ID ổn định là `MODULE/ClassName#methodName`. Một test E2E có thể phục vụ nhiều module, nhưng khi chọn chạy sẽ gộp, không chạy lặp.
- **Công cụ kế hoạch:** `scripts/impact_regression.py` là planner **chỉ đọc**, không tự chạy emulator, Gradle, thay đổi mã, tệp nhạc hay quyền.

## 2. Quy trình trước khi sửa code

1. **Xác định thay đổi trực tiếp**: tên module bị sửa, file nguồn và hành vi thay đổi. Đưa toàn bộ file có khả năng sửa vào planner, hoặc `--module MODULE_ID`.
2. **Xác định tác động lây lan**: duyệt ngược `requires`. Ví dụ Search là consumer của Library + Media, Home là consumer của Search. Sửa Search → trực tiếp `SEARCH`, gián tiếp `HOME_NAV`; không suy diễn rằng Playlist/Billing bị ảnh hưởng.
3. **Chốt Selective Regression tối thiểu theo module**: chạy tất cả test đã đăng ký của module sửa và smoke/contract test ở mọi downstream consumer; trình bày **danh sách case ID, nguyên nhân, môi trường** trước khi thực thi.
4. **Quy tắc mở rộng**: nếu có source không được phân loại, rủi ro quyền truy cập, roles Owner/Admin, security, Room migration, SAF persisted grants, Gradle/Manifest, **hợp đồng lõi dùng chung chưa thể khoanh hết tác động**, release APK, thay đổi ≥4 module hoặc lỗi không được giải thích, bắt buộc yêu cầu full regression và acceptance/integration liên quan. Riêng component UI dùng chung đã có bản đồ consumers có thể chạy selective + smoke màn hình phụ thuộc. Không lấy phép lọc làm lý do giảm an toàn.
5. **Sau sửa**: chạy `plan --git-diff` theo thay đổi thực; kiểm tra coverage map mới, chạy test, báo pass/fail/skip kèm log. Nếu test FAIL phải sửa hoặc công bố blocker, **không ép test PASS**. Nếu không có AVD/SDK/máy thật thì đánh dấu **NOT RUN**.

## 3. Lệnh dùng được ngay

```powershell
py -3 scripts/impact_regression.py validate
py -3 scripts/impact_regression.py catalog
py -3 scripts/impact_regression.py plan --module SEARCH
py -3 scripts/impact_regression.py plan --files app/src/main/java/com/example/musicflow/feature/search/SearchViewModel.kt
py -3 scripts/impact_regression.py plan --git-diff --json
py -3 -m unittest discover -s scripts -p test_impact_regression.py -v
```

`--files` nhận đường dẫn tương đối repository. `--git-diff` đọc file đã sửa và file mới chưa được track. `--full` mở rộng toàn bộ suite. Kết quả JSON có `direct_modules`, `affected_modules`, `testcases`, `risk_reasons`, `mode` và `commands`. Các lệnh được **đề xuất**, không tự thực thi.

## 4. Ví dụ impact analysis

| Yêu cầu sửa | Module trực tiếp | Module ảnh hưởng | Selective / mở rộng |
|---|---|---|---|
| Sửa debounce của SearchViewModel | SEARCH | HOME_NAV | Tất cả SEARCH + smoke HOME_NAV |
| Sửa UI thanh toán mô phỏng | BILLING_DEMO | DEMO_CENTER | Tất cả BILLING_DEMO + smoke DEMO_CENTER; không cần playback |
| Sửa nhãn lỗi đăng nhập AccountAuthErrorLabels | AUTH | ROLES_ADMIN, CREATOR, BILLING_DEMO, CLOUD_SYNC, ONLINE_RIGHTS, DEMO_CENTER và downstream | AUTH + smoke từng consumer |
| Sửa GoogleOAuthFlow/token hoặc Web Admin | AUTH / ROLES_ADMIN | Có thể ảnh hưởng xác thực và các quyền | **Full required**, E2E RBAC fail-closed |
| Sửa persisted SAF grant | MEDIA_SAF | LIBRARY, SEARCH, PLAYBACK, PLAYLIST, SMART, v.v. | **Full required**; SAF thật, permission revoke, signed upgrade |
| Sửa Owner/PlatformAdmin quyền truy cập | ROLES_ADMIN | CREATOR, BILLING_DEMO, DEMO_CENTER | **Full required** + test RBAC fail-closed |
| Đổi Compose theme/Manifest/Gradle | UI_ACCESS/shared | Có thể toàn ứng dụng | **Full required** |
| Chỉ sửa tài liệu, catalog QA, Brain | QA/docs | Không đổi app runtime | Chỉ QA engine + Brain audit; không tự chạy Android |

## 5. QA evidence/report template

Mỗi lần làm việc ghi một bản tóm tắt:

```text
Change: ...
Direct module(s): ...
Affected module(s) + dependency rationale: ...
Risk mode: selective / full_required / qa_docs_only
Selected case IDs: ...
Execution: Android API / JVM / JS / manual, commands, logs
Results: PASS ... / FAIL ... / SKIP ... / NOT RUN ...
Why other modules excluded: Not in transitive dependency closure, no shared contract changed
Escalations / uncovered edges: ...
Brain evidence & lesson: ...
```

**Điều kiện nghiệm thu không thể thay bằng kiểm thử tự động:** thao tác TalkBack nghe/đọc thật, bàn phím Bluetooth/TV D-pad thực, nhạc MP3/FLAC/WAV trên máy thật, hiệu năng RAM/scroll/scan thực, quyền với nhiều SAF provider, thanh toán/Supabase production và giấy phép nhạc online. Chứng cứ fixture/emulator chỉ đủ cho phạm vi ghi trong log.

**Lưu ý đặc biệt:** `SignedApkUpgradeV6Test` có cặp test seed/verify liên tiếp qua `adb install -r`; phải dùng `scripts/run-signed-upgrade-v8-qa.ps1` trên **AVD QA mới**, tuyệt đối không chạy method verify đơn lẻ rồi tuyên bố upgrade PASS.
