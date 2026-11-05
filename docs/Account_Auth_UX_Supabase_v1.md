# MusicFlow — Account UX / Supabase Email Auth v1

**Kiểm thử:** 2026-11-05, dự án báo cáo / sideload APK, không phát hành Play.  
**HEAD nền:** `3854034`, thay đổi local chưa commit.

## Kết quả

- Supabase hosted `vzvxpbwrftlnyvuhfoqn`: GET `/auth/v1/settings` xác nhận **Email provider ON**, **`mailer_autoconfirm=true` (không cần xác nhận email)** và **Google provider OFF**. Việc tắt Confirm email được chủ dự án thực hiện trong Dashboard; **không chỉnh server settings hoặc RLS từ Android**.
- Giao diện Account được bố trí lại: phần đăng nhập đứng trước tùy chọn theme; hai tab **Đăng nhập / Tạo tài khoản**; đúng một nút hành động chính; nhập lại mật khẩu chỉ trong tab đăng ký; nút **Hiện/Ẩn** riêng từng ô mật khẩu; kiểm tra trường email, mật khẩu và xác nhận ngay dưới ô; gợi ý ≥6 ký tự; xác nhận trạng thái email từ Supabase; lỗi server được phân biệt; nút Google **vẫn hiển thị nhưng disabled** và ghi chú chưa hoạt động; nút **Tiếp tục nghe nhạc không đăng nhập** điều hướng tới Home.
- **Mặc định đăng ký/đăng nhập Supabase**; phiên Demo offline trước đây không tự đăng nhập lại hay giả danh tài khoản online, dữ liệu local cũ không bị xóa. Guest vẫn nghe nhạc offline.
- Tầng Auth giữ nguyên `AccountApi.signUp/signIn` (REST Supabase), token được lưu bảo vệ; phần profile `account_profiles` lưu trên Supabase; Free/Premium lấy từ entitlement server, không tự cấp Premium.

## QA thực tế

**Emulator QA riêng:** `MusicFlow_AuthUX_QA_API30`, Android API 30, `emulator-5560`, ứng dụng không tồn tại trước khi cài.

1. **121/121 JVM test PASS**, 0 FAIL, 0 SKIP; bổ sung 5 test `AccountFormValidationTest`: ô trống, sai định dạng, mật khẩu ngắn, xác nhận sai, đăng nhập không cần nhập lại.
2. **1/1 Android instrumented UI PASS** (`AccountAuthUiTest`, output `OK (1 test)`): chuyển tab, hiển thị xác nhận mật khẩu, lỗi nhập liệu ngay dưới ô, Google disabled, nút khách đưa về Home. Không tạo user trong test UI tự động.
3. **E2E đăng ký qua UI thật PASS**: dùng email QA ngẫu nhiên ở miền `example.com`, mật khẩu QA và nhập lại, bấm `Tạo tài khoản`; UI chuyển sang profile **Free**. SQL read-only xác minh Auth user đã xuất hiện, `email_confirmed_at` có giá trị và hồ sơ `account_profiles` được tạo. Không cần email xác nhận.
4. **E2E đăng xuất → đăng nhập lại PASS** bằng tài khoản QA vừa tạo: UI trở lại profile Free. **Đóng ứng dụng → mở lại PASS** sau khi chờ refresh token: profile được khôi phục.
5. **APK release-mode demo đã ký QA, không debug:** `dist/MusicFlow-1.0-Demo-AuthUX-Android.apk`; `assembleRelease`, `zipalign`, `apksigner verify` và `aapt badging` PASS. Cài `adb install -r` lên QA emulator với chữ ký tương thích PASS; profile đang đăng nhập vẫn được giữ. Sau khi xóa tài khoản QA từ backend, cold launch bản đóng gói hiển thị lại màn Đăng nhập/Tạo tài khoản cùng nút Google disabled.
6. **Dọn dữ liệu thử nghiệm:** chỉ xóa **1 tài khoản QA được tạo trong lượt này**, khớp email QA ngẫu nhiên chính xác. Sau cleanup, Supabase còn **1 tài khoản/1 hồ sơ của một QA khác**, không đụng dữ liệu đó. Không chạm dữ liệu thật của người dùng hoặc emulator khác.

## APK demo bàn giao

```text
E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-AuthUX-Android.apk
```

- SHA-256: `C1B081CDD7AFE5BCB2C3FFC2E1B231CFFFD1A188503B18D0C06A0F1229A0AA04`.
- Kích thước: **30.279.249 byte**.
- Package `com.example.musicflow`, `versionCode=1`, `versionName=1.0`, minSdk 29, targetSdk 36.
- QA demo signing (debug keystore), không phải production release. APK giữ trong thư mục `dist/` bị Git ignore; không tự upload/push lên GitHub. SHA sidecar `.apk.sha256` có sẵn.
- Bản APK demo cũ vẫn được bảo toàn; bản này được đóng gói tên riêng, **không ghi đè** các artifact trước.

## Lưu ý

Tắt xác nhận email cho phép bất kỳ ai đăng ký một địa chỉ chưa chứng minh quyền sở hữu. Phù hợp demo báo cáo, **không khuyến nghị khi phát hành công khai**. Google có nút UI nhưng chưa hoạt động theo yêu cầu. Chưa kiểm thử email thật, máy vật lý, Premium thanh toán thật hay phát hành thật.

Mã nguồn: `feature/account/AccountScreen.kt`, `AccountViewModel.kt`, `AccountFormValidation.kt`, điều hướng `MusicFlowApp.kt`; tests JVM + instrumented. Log `qa-logs/account-auth-ux-supabase-v1.log` và log đóng gói `qa-logs/demo-apk-auth-ux-package-v1.log`.
