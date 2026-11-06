# MusicFlow Owner Demo Readiness v2 — Báo cáo / APK demo

**Ngày:** 2026-11-06. **Phạm vi:** Nâng cấp bảng tổng quan Owner sau khi tạo đủ tài khoản Supabase QA. Đây là bảng **chỉ đọc**, không phải quy trình gán quyền hay xác nhận phát hành nội dung online.

## Chức năng vừa triển khai

- Supabase migration `musicflow_owner_demo_readiness_v2`, nguồn `docs/sql/musicflow_owner_demo_readiness_v2.sql`, nâng cấp RPC `musicflow_owner_demo_summary()` với ba bộ đếm bổ sung:
  - `active_premium_count`: số entitlement Premium còn hạn;
  - `non_premium_count`: số tài khoản không có Premium còn hạn, **bao gồm Owner nếu không có Premium** — không đồng nghĩa chính xác số người dùng Free thông thường;
  - `delegated_admin_count`: số PlatformAdmin có đủ ba quyền `manage_users`, `review_creators`, `moderate_catalog`.
- Giữ nguyên các trường v1, chỉ thêm trường tổng hợp. RPC kiểm tra Owner qua `auth.uid()` + bảng `musicflow_private.user_roles`, không xuất danh tính/email/UUID/token và không thay đổi vai trò của bất kỳ tài khoản nào. `anon` không được EXECUTE.
- Android `OwnerDemoSnapshot` kiểm tra đủ mọi bộ đếm, không chấp nhận số âm, có trạng thái riêng cho nhóm vai trò chính, Free/Premium và yêu cầu Creator đang chờ. `OwnerDemoDashboard` hiển thị các chỉ số và gợi ý demo; khi RPC lỗi, không hiển thị thống kê cũ.
- Trang nội bộ `admin-web/index.html` hiển thị ba metric mới, đối chiếu riêng Developer, Artist và Author, và kiểm tra PlatformAdmin đã được ủy quyền đầy đủ. Không sử dụng service_role hay mật khẩu từ file.
- Có script `scripts/run-owner-demo-readiness-v2-tests.ps1` để lặp lại kiểm thử Gradle, JS và tùy chọn UI trên **device QA được chỉ định rõ bằng `-Serial`**. Script không tự bật/đụng emulator của chủ dự án.

## Trạng thái thật trên Supabase lúc triển khai

| Chỉ số | Giá trị |
|---|---:|
| Tổng tài khoản Auth | 11 |
| Owner | 1 |
| PlatformAdmin / được ủy quyền đủ 3 quyền | 1 / 1 |
| Developer / Moderator | 1 / 1 |
| Artist / Author | 1 / 1 |
| Premium còn hạn / không có Premium còn hạn | 1 / 10 |
| Yêu cầu Creator chờ duyệt | 1 |
| Nháp tác phẩm chờ duyệt / file đã tải | 0 / 0 |

Những con số này đến từ RPC thực tế trên Supabase, không phải dữ liệu dựng trong Android. Riêng persona xác nhận bởi role/entitlement không chứng minh đã chạy E2E ứng dụng.

## Kiểm thử và giới hạn

- **DB PASS:** RPC Owner trả các bộ đếm trên; SQL xác nhận `anon` không có EXECUTE; thử với claims UID của Free và PlatformAdmin bị từ chối; `musicflow_my_access` khi giả lập claims DB trả đúng permissions PlatformAdmin, Developer không có admin permissions, Creator pending không tự nhận Creator role. Đây là **kiểm thử quyền nội bộ bằng SQL giả lập UID**, không phải JWT/đăng nhập HTTP E2E.
- **JVM:** 5 bài `OwnerDemoSnapshotTest` kiểm tra readiness, điều kiện đủ 3 quyền, tier/pending độc lập; test JSON trực tiếp được đặt trong Android instrumentation vì `org.json` của Android không chạy trong JVM unit test mặc định.
- **Android UI:** 3 bài Owner Dashboard (có số liệu, 403/fail-closed, bộ đếm lỗi/fail-closed) và 5 bài Role UI hồi quy (Developer, Premium, Moderator, Creator, PlatformAdmin). Chạy trên AVD Android API30 **riêng**, không sử dụng tài khoản thật.
- **Web JS:** `node --check` với JavaScript trích từ HTML.
- **APK demo:** `dist/MusicFlow-1.0-Demo-OwnerReadinessV2-Android.apk` (31.049.405 bytes, SHA256 `495BF4C7A3F45F205A66EF0A98C458B2799110A3E05890A711F5E27847AB9C63`). Build release-mode, dùng debug keystore QA để ký, zipalign/apksigner/aapt verify PASS, không debuggable, minSdk 29/targetSdk 36, **không phải phát hành thương mại**.
- **Kết quả cuối trên AVD QA Android API30:** script tái sử dụng **PASS** (`5/5 JVM`, `3/3 Owner UI` + `5/5 Roles UI` = **8/8 Android**, JS PASS). **Chính APK release-mode đã cài nâng cấp `Success` và cold launch `Status: ok`, `LaunchState: COLD`**, ứng dụng có tiến trình đang chạy. QA AVD `MusicFlow_OwnerReadinessV2_QA_API30` đã tắt/xóa; emulator có sẵn `emulator-5554` không bị thay đổi.
- **Lần chạy đầu có một JVM failure do gọi `org.json.JSONObject` stub trên JVM**; đã chuyển kiểm thử JSON sai định dạng sang Android instrumentation; chạy lại toàn bộ nhóm targeted PASS. Không che giấu lần FAIL trước.
- Log: `qa-logs/musicflow-owner-demo-readiness-v2-gradle.log`, `qa-logs/musicflow-owner-demo-readiness-v2-qa.log`, `qa-logs/demo-apk-owner-readiness-v2-package.log`.
- **Chưa kiểm thử:** Owner đăng nhập bằng tài khoản cá nhân thực trên Android/web để xem dashboard; Creator upload file thật và Moderator duyệt qua JWT HTTP từng vai trò. Tài khoản QA `example.com` không có mailbox khôi phục mật khẩu. **Không tự đẩy repo lên GitHub.**
- **Security Advisor sau migration:** vẫn báo `rls_enabled_no_policy` (8 bảng khóa mặc định), `authenticated_security_definer_function_executable` (25 RPC — phải audit trước production) và `auth_leaked_password_protection` chưa bật. Đây không phải một bản xác nhận production security. Tham khảo [SECURITY DEFINER linter](https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable) và [Supabase password security](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection).

## Chạy lại trên máy phát triển (PowerShell)

```powershell
.\scripts\run-owner-demo-readiness-v2-tests.ps1
```

Nếu muốn kiểm thử UI, bật một emulator QA riêng rồi chạy với `-Serial` để không tác động emulator khác. Đóng gói lại bằng:

```powershell
.\scripts\build-musicflow-demo-apk-v1.ps1 -OutputName 'MusicFlow-1.0-Demo-OwnerReadinessV2-Android.apk'
```

APK cùng tên đã có trong `dist/` sẽ **không bị ghi đè** nếu SHA-256 khác, đúng quy tắc giữ các bản demo cũ.
