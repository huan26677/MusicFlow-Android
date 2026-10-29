# MusicFlow — History Sync v1

Ngày xác minh: 2026-10-29. HEAD nền: `8f66af4`.

## Đã triển khai

- Supabase: `public.cloud_recent_history` và RPC `public.musicflow_merge_recent_history`. Bảng bật RLS; dữ liệu được gắn với `auth.uid()`.
- Android: `CloudHistorySync.kt`, `CloudHistoryRules.kt`, `RoomRecentHistoryRepository.mergeCloudRecent`, và nút **Đồng bộ lịch sử nghe** trong tab Tài khoản.
- Đồng bộ **thủ công** cho tài khoản đăng nhập, không chạy trong chế độ Khách.
- Chỉ đưa lên cloud **tên bài, nghệ sĩ và thời điểm nghe**; không truyền URI `content://`, đường dẫn file hay nội dung bài hát.
- Khi tải lịch sử từ cloud, chỉ liên kết với file nhạc nếu một bài duy nhất trên thiết bị khớp tên + nghệ sĩ. Không phát được bài chưa có file; không tạo URI giả.
- Giữ quy tắc local tối đa 20 bài gần nhất; không thay thế bản ghi mới hơn bằng bản cũ.
- Free đọc lịch sử cloud trong vòng 30 ngày; Premium đọc các bản ghi còn được lưu, theo chính sách RLS máy chủ và entitlement còn hiệu lực.

## Bằng chứng kiểm chứng

- Chạy lệnh `gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain`: **BUILD SUCCESSFUL in 31s**, exit code `0`.
- `CloudHistoryRulesTest`: **4 bài PASS, 0 lỗi, 0 bỏ qua** trong báo cáo Gradle XML.
- Supabase: `anon` không có SELECT bảng, không có EXECUTE RPC; `authenticated` có SELECT và EXECUTE, không có DELETE.
- Bảng lịch sử hiện trống. Không tạo tài khoản thử, lịch sử nghe giả hay gửi dữ liệu riêng tư vào Supabase.
- Security Advisor chỉ có thông tin `rls_enabled_no_policy` của bảng quản trị `public.music_uploads` (được cố ý khóa, không liên quan History Sync).
- Log build: `qa-logs/history-sync-v1.log`.

## Giới hạn

- Chưa chạy kiểm thử Auth, Sync E2E với người dùng thật, hai thiết bị, offline và lỗi mạng.
- Chưa thực hiện kiểm thử instrumentation trên thiết bị: `assembleDebugAndroidTest` chỉ **biên dịch APK test**.
- Danh sách đọc cloud tối đa 200 bản ghi cho mỗi lượt, không đồng bộ xóa lịch sử cloud khi người dùng xóa local.
- Khi Free đăng ký lại hoặc phát một bài sau thời gian dài, cần kiểm tra thêm chính sách xung đột bản ghi cũ với RLS 30 ngày.
- Google Login, Background Sync, Premium Billing và Full Regression là các milestone riêng.

## Bước tiếp theo

**Google Login Foundation v1** và chuẩn bị kiểm thử Auth/Cloud Sync E2E trên hai thiết bị thật; không đánh dấu E2E PASS trước khi thực sự chạy.
