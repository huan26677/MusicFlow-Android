# MusicFlow — Google Login Foundation v1

Ngày triển khai: 2026-10-29. Supabase project: `vzvxpbwrftlnyvuhfoqn`.

## Trạng thái thật

- Android đã được nối với Supabase Auth Google OAuth bằng PKCE: authorization code + SHA-256 challenge, verifier và flow nonce ngẫu nhiên, xác nhận callback rồi đổi code lấy phiên.
- File phiên mới dùng `AccountTokenStore` hiện có (AES-GCM/Android Keystore). Mật khẩu Google, Google Client Secret và service_role **không được lưu trong APK/Git**.
- Callback: `musicflow://auth/callback?flow=<nonce>&code=<code>`.
- Tab Account tự được mở lại sau callback; cùng Supabase session được sử dụng để đọc hồ sơ Free/Premium và Cloud Sync, không tạo hệ thống phân quyền mới.
- Ứng dụng vẫn sử dụng được như Khách, đăng ký/đăng nhập email tiếp tục hoạt động.
- **Chưa có Google Login E2E PASS.** Kiểm tra endpoint `/auth/v1/settings` ngày 2026-10-29: `external.google=false`, `external.email=true`. Nút Google được khóa và có giải thích khi provider chưa bật.

## Cấu hình bắt buộc của chủ dự án (không nhập bí mật vào chat)

1. Tạo hoặc chọn Google Cloud project. Thiết lập OAuth consent screen, tên ứng dụng và danh sách test users nếu ứng dụng đang ở chế độ thử nghiệm.
2. Tạo **OAuth 2.0 Client ID loại Web application** trong Google Cloud Credentials. Thêm Google **Authorized redirect URI**:

   `https://vzvxpbwrftlnyvuhfoqn.supabase.co/auth/v1/callback`

3. Trong **Supabase Dashboard → Authentication → Sign In / Providers → Google**, bật Google, điền Client ID và Client Secret ngay tại Dashboard, lưu lại. Không đưa Client Secret vào mã Android, `secrets.properties`, GitHub hay cuộc trò chuyện.
4. Trong **Supabase Dashboard → Authentication → URL Configuration → Redirect URLs**, cho phép deep link Android:

   `musicflow://auth/callback**`

   Đuôi `**` là cần thiết để chứa query `flow` và mã trả về.
5. Trên điện thoại/emulator, mở MusicFlow → Account → **Tiếp tục với Google**. Chọn Google account trên trình duyệt hệ thống, rồi kiểm tra callback trở lại app.
6. Xác minh user ID ở Supabase Auth, `account_profiles`, `account_entitlements`, logout, app restart và Cloud Sync; nếu email đã có tài khoản, cần kiểm tra chính sách hợp nhất identity của Supabase.

## Luồng ứng dụng

1. `AccountViewModel` đọc trạng thái `external.google` từ Supabase settings và chỉ mở nút khi server bật.
2. `GoogleOAuthFlow` tạo verifier/nonce bằng `SecureRandom`, challenge SHA-256, URL `/auth/v1/authorize?provider=google`.
3. `AccountTokenStore` mã hóa verifier, nonce, timestamp để callback vẫn xử lý được khi ứng dụng bị khởi tạo lại. Thời hạn callback ở client là 10 phút.
4. Trình duyệt hệ thống mở URL xác thực (không đăng nhập Google qua WebView).
5. Android Manifest nhận `musicflow://auth/callback` qua `MainActivity`; `AccountOAuthRedirects` chuyển callback tới tab Account.
6. Client kiểm tra scheme/host/path, nonce, thời hạn, code duy nhất; xóa trạng thái chờ một lần rồi gọi `POST /auth/v1/token?grant_type=pkce` với `auth_code` và `code_verifier`.
7. Sau thành công, dùng `currentAccount()` của Supabase Auth để tải hồ sơ và gói. Quyền Premium **vẫn chỉ được xác định từ RLS-entitlement phía server**, không từ tài khoản Google.

## Kiểm chứng & giới hạn

- Build APK app + APK AndroidTest (biên dịch, không chạy trên máy), JVM unit tests.
- `GoogleOAuthFlowTest`: **7/7 PASS**, 0 failure/error; kiểm tra PKCE known vector, authorize URL, callback hợp lệ, nonce sai, quá hạn, origin lạ, mã bị lặp.
- Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain`: **BUILD SUCCESSFUL**, exit code `0`.
- Custom URI scheme có nguy cơ bị app khác đăng ký trùng. PKCE + nonce giảm nguy cơ chiếm phiên; khi có tên miền thuộc quyền sở hữu, nên chuyển sang **verified HTTPS Android App Links** để tăng độ an toàn.
- Chưa có Google OAuth client credentials và provider còn tắt, nên chưa kiểm thử đăng nhập Google thật, hồi phục phiên, hoặc linking identity bằng hai thiết bị.
- Log: `qa-logs/google-login-foundation-v1.log`.

## Bước kế tiếp

Sau khi Google provider được bật: **Google Login / Account E2E**, kiểm thử trên emulator/thiết bị thật, khởi động lại app, đăng xuất, identity linking, rồi đồng bộ cloud với hai thiết bị. Tiếp tục milestone Full Product còn thiếu, Full Regression cuối giai đoạn.
