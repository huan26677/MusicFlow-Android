# MusicFlow Creator Studio v2 — Upload riêng tư bản thu & chứng cứ

**Ngày:** 2026-11-05. **Phạm vi:** APK Android demo và Supabase, nối tiếp Creator Studio v1.

## Nghiệp vụ đã thiết kế

- Tài khoản Artist/Author **được phê duyệt** lưu nháp tác phẩm trước, rồi chọn một bản thu âm mẫu và một tài liệu quyền phát (PDF/JPG/PNG).
- Giới hạn **6 MB/tệp** cho demo; bản thu hỗ trợ MP3/M4A/OGG/FLAC/WAV. File lớn cần cơ chế resumable ở giai đoạn sau.
- Bucket `musicflow-creator-submissions` là **PRIVATE**; không có URL công khai, file chỉ upload một lần. Đường dẫn do máy chủ tự tạo: User ID / Draft ID / File ID.
- Luồng: đặt chỗ bằng RPC → Storage POST với JWT của người đăng nhập → RPC xác minh object tồn tại, đúng chủ, MIME và kích thước → xác nhận upload.
- Sau khi tải cả hai tệp và hoàn tất khai báo bản quyền v1 mới gửi xét duyệt; Owner/PlatformAdmin có quyền `review_creators` tải chứng cứ để xem khi nháp đang chờ duyệt.
- Các API nháp kiểm tra vai trò tại Supabase. Không có cổng upload public, không dùng khóa service_role trong APK.
- Android dùng bộ chọn tệp hệ thống (không cần xin quyền đọc toàn bộ bộ nhớ); tải xuống vào vị trí người dùng tự chọn. Web console nội bộ tải theo JWT, chỉ tạo blob URL tạm.
- **Duyệt metadata không xuất bản bản thu và không chứng nhận quyền streaming**. Nhạc offline của người nghe vẫn hoạt động bình thường.

## Tệp triển khai

- SQL: `docs/sql/musicflow_creator_private_uploads_v2.sql`; bảng `musicflow_private.creator_upload_slots` bật RLS, không cấp SELECT trực tiếp cho user; Storage policies cho quyền INSERT/SELECT riêng cho file hợp lệ.
- RPC: `musicflow_creator_upload_reserve`, `musicflow_creator_upload_confirm`, `musicflow_creator_upload_list`, hai helper Storage policy; `musicflow_creator_submit_draft` yêu cầu cả hai file được xác nhận.
- Android: `CreatorPrivateFileRules.kt`, `CreatorPrivateStorage.kt`, `CreatorPrivateAttachmentPanel.kt` và phần kết nối Account/Creator Studio.
- Web: `admin-web/index.html` bổ sung tải chứng cứ từ private bucket.

## Còn lại

- Chưa có Owner được xác minh nên chưa thể kết luận E2E Creator thật → Admin thật.
- Chưa hỗ trợ tệp audio trên 6 MB, thay thế/xóa bản đã upload, quét virus/bằng chứng tự động, hồ sơ khiếu nại và pipeline phát hành thật.
- Không được lấy việc người dùng khai báo có bản quyền làm bằng chứng hợp pháp. Production cần Supabase security hardening trước khi public console.

## Checklist QA

- [x] Đã triển khai Supabase SQL. Bucket PRIVATE, hạn mức 6 MB, bảng slot RLS bật/không SELECT trực tiếp cho anon/authenticated; 5 RPC không cho anon.
- [x] Người dùng thường bị từ chối đặt slot và đọc file; HTTP thật không đăng nhập gọi reserve nhận 401. Hàm RLS không cấp quyền upload/download khi không có slot.
- [x] Debug và AndroidTest build PASS; 2/2 JVM file rules, 3/3 JVM draft rules, 2/2 Attachment Android UI, 2/2 Creator Studio UI hồi quy. JavaScript admin-web cú pháp PASS.
- [x] APK demo build, ký QA và verify PASS: `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-CreatorPrivateFilesV2-Android.apk`, **31.033.021 byte**, SHA-256 `0A00A111FC824CC1A1A948FB75F713A65693913096817079D8A6C462EABDD309`. Cài, cold launch, mở Tài khoản Guest trên QA AVD PASS; đã xóa QA AVD, giữ emulator cũ.
- [x] Cập nhật kế hoạch, tài liệu và `qa-logs/musicflow-creator-private-files-v2.log`. Không commit/push tự động.

**Chưa kiểm thử E2E upload audio/PDF giữa tài khoản Creator và Owner thật** vì Owner chưa được chỉ định, và không tự cấp role cho user bất kỳ. Tài liệu mật mã/cấp phép thực tế vẫn cần kiểm tra bởi người có trách nhiệm; file PDF do user tải lên không đồng nghĩa được xác minh hợp đồng.
