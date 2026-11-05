# MusicFlow — Kiểm tra tài khoản, đăng ký, đăng nhập và admin v1

**Ngày:** 2026-11-05 (Asia/Ho_Chi_Minh). **HEAD nền:** `8095a22` (`main = origin/main`). **Phạm vi:** APK demo trên emulator API 30, mã nguồn Android, Supabase Auth/DB read-only, JVM unit; không dùng email thật hoặc tạo tài khoản trên backend.

## Kết luận ngắn

| Mục | Kết quả |
|---|---|
| Có UI đăng ký email và mật khẩu | **CÓ — đã mở được UI**: `Đăng ký Free` |
| Có UI đăng nhập email và mật khẩu | **CÓ — đã mở được UI**: `Đăng nhập` |
| Signup với form trống | **PASS xác minh dữ liệu tại client**: hiện lỗi yêu cầu email hợp lệ/mật khẩu từ 6 ký tự |
| Signin với mật khẩu trống | **PASS xác minh dữ liệu tại client**: hiện cùng lỗi |
| Supabase Auth email provider | **BẬT**, endpoint `/auth/v1/settings` trả OK |
| Supabase Email Confirmation | **ĐANG YÊU CẦU** (`mailer_autoconfirm=false`) |
| Supabase Google provider | **TẮT**; nút Google trong UI bị khóa và hiển thị giải thích |
| Đăng ký tài khoản mới thành công + xác nhận email | **CHƯA KIỂM THỬ E2E** — không dùng email thật, không tạo user |
| Đăng nhập thành công, restore/logout phiên thật | **CHƯA KIỂM THỬ E2E** — backend hiện chưa có tài khoản QA |
| Có bảng hồ sơ và quyền Free/Premium | **CÓ** (`account_profiles` và `account_entitlements` bật RLS) |
| Hiện tại có bao nhiêu người dùng | **0** trong `auth.users`, `account_profiles` và `account_entitlements` khi kiểm tra read-only |
| Có giao diện Admin Android hoặc dashboard Web do MusicFlow viết | **CHƯA CÓ** — không có trang/điều hướng Admin trong mã nguồn hiện tại |
| Có chức năng quản trị kỹ thuật | **CÓ, CHỈ Ở MỨC CLI/BACKEND**: `scripts/musicflow_licensed_upload.py` hỗ trợ `stage` / `publish` với secret chỉ trên máy quản trị, không có GUI |
| Test JVM liên quan tài khoản | **17/17 PASS** — `AccountTierTest` 5/5 và `GoogleOAuthFlowTest` 12/12; không phải E2E login |

## Bằng chứng

1. Tạo AVD QA sạch `MusicFlow_AuthQA_API30` (không dùng AVD gốc); cài APK demo từ `dist/MusicFlow-1.0-Demo-Android.apk`: `adb install` trả `Success`, `am start -W` trả `Status: ok`. Vào tab Account bằng chạm UI; các nút `Đăng nhập`, `Đăng ký Free`, `Tiếp tục với Google` và hai ô `Email`/`Mật khẩu` hiển thị.
2. Nhấn `Đăng ký Free` với dữ liệu trống: panel `Thao tác tài khoản không thành công` và nội dung `Nhập email hợp lệ và mật khẩu từ 6 ký tự.`. Nhấn `Đăng nhập` trong trạng thái mật khẩu chưa nhập, cũng xuất hiện thông báo xác minh. **Không gửi signup hợp lệ đến backend, không tạo tài khoản thử ảo.**
3. Supabase settings GET trực tiếp: `external.email=true`, `external.google=false`, `mailer_autoconfirm=false`. Sau khi tải cấu hình, UI hiển thị thông báo Google Login chưa được bật trong Supabase.
4. Truy vấn SQL read-only dự án Supabase `vzvxpbwrftlnyvuhfoqn`: bảng `account_profiles`, `account_entitlements` tồn tại với RLS; số hàng `auth.users` = **0**, `account_profiles` = **0**, `account_entitlements` = **0**. Không tìm thấy bảng `public` quản lý role/admin; không suy ra từ đó rằng Dashboard Supabase không có người quản trị dự án.
5. Kiểm tra `AccountApi.kt`: các endpoint `POST /auth/v1/signup`, `POST /auth/v1/token?grant_type=password`, refresh, logout và `GET /auth/v1/user` đã được viết. `AccountViewModel` yêu cầu email có `@` và mật khẩu tối thiểu 6 ký tự, xử lý trường hợp xác nhận email khi signup không có session. Có source **không đồng nghĩa E2E thành công**.
6. Kiểm tra màn `AccountScreen.kt` và điều hướng `MusicFlowApp.kt`: UI Guest, đăng ký, đăng nhập, Free/Premium và quản lý profile; không có Admin route/screen. `scripts/musicflow_licensed_upload.py --help` chạy được với hai lệnh quản trị backend `stage` và `publish`; **không phải giao diện admin bằng web/app**.
7. Gradle `:app:testDebugUnitTest --tests AccountTierTest --tests GoogleOAuthFlowTest` BUILD SUCCESSFUL, 5/5 + 12/12 JVM test, 0 FAIL, 0 SKIP.

## Lưu ý phạm vi và bước sau nếu được yêu cầu

- Theo mục tiêu MusicFlow **APK demo/báo cáo**, không dùng email thật, không bắt buộc login thật. Vì email confirmation đang bật, việc đánh giá luồng đăng ký rồi đăng nhập thành công **cần một hộp thư kiểm thử do chủ dự án cung cấp/quản lý hoặc giải pháp Auth mock trong môi trường riêng**; không làm việc này khi chưa có yêu cầu đổi phạm vi.
- **Hiện chưa có tài khoản Admin và chưa có giao diện Admin MusicFlow** được thiết kế trong Android/web; Supabase Dashboard là công cụ quản trị nền tảng, không phải dashboard sản phẩm. Công cụ quản lý nhạc có bản quyền ở CLI không được đưa secret vào ứng dụng.
- Trạng thái bản demo APK không thay đổi; không sửa production Kotlin, không đổi Auth provider, không tạo tài khoản/hồ sơ trên Supabase và không tự mở giai đoạn phát hành.

Log tóm tắt: `qa-logs/account-auth-admin-audit-v1.log`.
