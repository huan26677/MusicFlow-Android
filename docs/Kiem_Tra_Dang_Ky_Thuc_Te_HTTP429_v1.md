# MusicFlow — Kiểm tra đăng ký thực tế trên giao diện: lỗi Supabase 429

**Ngày:** 2026-11-05 (Asia/Ho_Chi_Minh)  
**HEAD nền:** `89dbfda`, Git sạch đầu lượt  
**Tình trạng:** **ĐÃ TÁI HIỆN LỖI; ĐÃ CẢI THIỆN THÔNG BÁO; ĐĂNG KÝ TÀI KHOẢN THẬT VẪN BỊ CHẶN BỞI CẤU HÌNH EMAIL SUPABASE.**

## 1. Bằng chứng chạy UI thực tế

- Trên AVD QA sạch `MusicFlow_SignUpQA_API30` Android API 30, cài **chính bản MusicFlow demo** từ `dist/MusicFlow-1.0-Demo-Android.apk`, mở tab Account.
- Nhập **địa chỉ thử nghiệm thuộc miền `example.invalid` không gửi email đến người thật** và mật khẩu QA ngẫu nhiên có độ dài hợp lệ (không lưu bí mật trong Git/log).
- Bấm **Đăng ký Free** trên chính giao diện với cả hai ô đã điền. Kết quả xuất hiện panel lỗi:
  - `Thao tác tài khoản không thành công`.
  - `Thao tác quá nhanh. Hãy thử lại sau.`
- Tra log Supabase Auth cùng thời điểm: **HTTP 429 — `email rate limit exceeded`**. Cơ sở dữ liệu `auth.users` trước/sau vẫn **0 người dùng**, tức signup chưa tạo account.
- Log Auth của các lần đăng ký trước cũng có **HTTP 400 — email bị Supabase từ chối**. Không ghi lại địa chỉ người dùng trong báo cáo.

**Kết luận:** Request từ UI thực sự đã tới Supabase; lỗi không nằm ở thiếu nút đăng ký, quyền Android hay mật khẩu chưa nhập. Supabase hosted đang chặn do **hạn mức email xác nhận**, và có thể từ chối email không thuộc nhóm địa chỉ được phép gửi trong dịch vụ email mặc định. Android cũ chỉ dựa vào HTTP status nên che mất nguyên nhân cụ thể.

## 2. Thay đổi đã triển khai

- `app/src/main/java/com/example/musicflow/core/account/AccountApi.kt` đọc **mã lỗi JSON từ phản hồi lỗi của Supabase Auth** qua `errorStream`, giới hạn 4096 ký tự; không hiển thị nguyên văn thông điệp chứa email/người dùng.
- `core/account/AccountAuthErrorLabels.kt` dịch các mã `over_email_send_rate_limit`, `email_address_not_authorized`, `email_address_invalid`, `email_not_confirmed`, `invalid_credentials`, `weak_password` và một số trường hợp khác sang tiếng Việt có chỉ dẫn rõ.
- `core/account/AccountAuthErrorLabelsTest.kt` có **7/7 JVM PASS** cho thông báo mới. Chạy lại toàn bộ `:app:testDebugUnitTest`: **112 PASS / 0 FAIL / 0 SKIP**.
- Tạo **APK demo thứ hai, không ghi đè APK bàn giao cũ**, dùng script `scripts/build-musicflow-demo-apk-v1.ps1` với `-OutputName MusicFlow-1.0-Demo-AuthFeedback-Android.apk`. Build/ký/verify PASS, cài **nâng cấp cùng chữ ký** lên AVD QA PASS, mở màn Tài khoản trên APK mới PASS.
- **Không thử gửi thêm email/signup vào backend sau khi sửa** vì Supabase vẫn đang bị giới hạn; logic phân loại mã lỗi được kiểm tra bằng unit tests. Không khẳng định đăng ký thành công.

APK mới cục bộ:

```text
E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-AuthFeedback-Android.apk
```

Kích thước: **30.246.481 byte**. SHA-256:

```text
27FCF8E9B55F26ACB4DCF3B6DC816FC87EF81F8D3CBD69003DFECF434B293F04
```

APK cũ `MusicFlow-1.0-Demo-Android.apk` được giữ nguyên (SHA-256 `ACE9AD765F75C7939C513210E2BBC1555F95E6504636303B423FB70598C2E750`). Cả hai tệp trong `dist/` đều bị Git ignore; chỉ mã nguồn, script, log và tài liệu được commit. Bản mới vẫn dùng **debug keystore QA chỉ dành cho demo**, không phát hành Google Play.

## 3. Việc cần làm nếu muốn đăng ký được

Theo tài liệu Supabase hiện hành `https://supabase.com/docs/guides/auth/testing-auth-flows`:

1. **Muốn dùng tài khoản email thật:** tại Supabase Dashboard, cấu hình **custom SMTP** hoặc Send Email Hook, giữ xác nhận email bật, và kiểm tra **Authentication → Rate Limits**. Dịch vụ email mặc định trên hosted project chỉ dành cho thử nghiệm và hạn chế địa chỉ người nhận; khi chạm hạn mức server trả `429 over_email_send_rate_limit`. Chủ dự án cần cấu hình/duyệt dịch vụ gửi thư và kiểm chứng hộp thư; không thể sửa phía Android để vượt giới hạn đó.
2. **Muốn demo không dùng email thật:** ưu tiên hệ thống **tài khoản demo offline được ghi nhãn rõ** hoặc **Supabase local + Mailpit** để thử signup và confirmation trên máy phát triển. Không trộn account demo vào Supabase production, không tuyên bố đã xác minh đăng ký thật.
3. **Không tự tắt xác minh email trên project hosted đang có:** việc này cho phép đăng ký địa chỉ không thuộc quyền sở hữu, không phù hợp để âm thầm chỉnh cấu hình trên server.

Đã giữ nguyên Auth providers, schema, RLS, cơ sở dữ liệu và app MusicFlow chạy trên emulator của người dùng. Chỉ tạo/dọn AVD QA do lượt kiểm thử này sở hữu. **Phạm vi demo/báo cáo vẫn là sideload APK, chưa phát hành thật**.

**Log ngắn:** `qa-logs/auth-signup-http429-v1.log`; log đóng gói APK mới `qa-logs/demo-apk-auth-feedback-package-v1.log`.
