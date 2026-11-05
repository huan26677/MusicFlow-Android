# MusicFlow Creator Studio v1 — Nháp tác phẩm và xét duyệt metadata

**Ngày:** 2026-11-05. **Phạm vi:** giai đoạn A3 Creator Studio, tiếp nối Roles MVP. **Không triển khai bản phát hành thương mại.**

## Phạm vi đã lập trình

- Nghệ sĩ/Tác giả **đã được Owner/PlatformAdmin duyệt vai trò qua Supabase** mới có quyền lưu nháp.
- Lưu dữ liệu riêng trên Supabase: tiêu đề, nghệ sĩ chính, album tùy chọn, chủ bản ghi âm (master), mã giấy phép dự kiến, khai báo có quyền streaming, mô tả/mã bằng chứng quyền và **1–12 tín chỉ đóng góp** (biểu diễn, sáng tác, lời, sản xuất, chủ master).
- Mỗi Creator chỉ xem/sửa nháp **của chính mình**. Chỉ sửa được trạng thái `draft` hoặc `needs_changes`; khi `submitted` sẽ khóa cho đến khi được xét duyệt.
- Gửi xét duyệt yêu cầu **có giấy phép được chọn, bằng chứng quyền tối thiểu 8 ký tự và chủ động xác nhận quyền streaming**. Server kiểm tra lại mọi quy tắc, không tin điều kiện nút Android.
- Owner/PlatformAdmin có `review_creators` xem danh sách chờ, duyệt **metadata** hoặc trả lại `needs_changes` với lý do; không thể tự xét duyệt tác phẩm của chính mình.
- Nhật ký tại bảng riêng `musicflow_private.role_audit`, không tạo/đổi file nhạc, không chạm bảng catalog online khi duyệt hồ sơ.

## Quy trình

1. Người dùng đăng nhập Supabase → xin vai trò **Nghệ sĩ / Tác giả** → quản trị viên có quyền duyệt.
2. Mở **Tài khoản → Vai trò & công cụ → Creator Studio** → **Tạo nháp**; thêm ít nhất một tên người tham gia và vai trò. Lưu nháp có thể để giấy phép `UNVERIFIED` để hoàn thiện sau.
3. Khi có bằng chứng hợp lệ, chọn giấy phép, khai báo quyền streaming và lưu nháp lần nữa → **Gửi bản nháp đã lưu để xét duyệt**.
4. Quản trị viên mở **Duyệt bản nháp tác phẩm** trên Android Role Panel hoặc **admin-web** (chạy nội bộ) → xem tín chỉ/bằng chứng khai báo → ghi lý do và duyệt/trả lại.
5. Trạng thái `metadata_approved` chỉ xác nhận hồ sơ. **Không xuất bản track, không tạo URL stream, không gán quyền phát trực tuyến.** Bước upload/đối soát giấy phép thật là kế hoạch kế tiếp.

## Backend và mã nguồn

- Script triển khai độc lập, idempotent: `docs/sql/musicflow_creator_drafts_v1.sql`. Bảng `musicflow_private.creator_drafts` bật RLS, không cấp quyền đọc/ghi trực tiếp cho anon/authenticated; server RPC xác minh `auth.uid()` và role trong `musicflow_private.user_roles` trên mỗi lần gọi.
- RPC: `musicflow_creator_draft_save`, `musicflow_creator_my_drafts`, `musicflow_creator_submit_draft`, `musicflow_pending_drafts`, `musicflow_review_draft`. Public RPC cho authenticated, **từ chối anon**. Thao tác viết có server-side authorization và giới hạn dữ liệu.
- Kotlin: `core/account/CreatorDraftRules.kt`, `feature/account/CreatorDraftStudio.kt`, danh sách RPC trong `AccountApi.kt`; mở từ `AccountRolePanel.kt`.
- Web: `admin-web/index.html`, thẻ **Creator Studio — Duyệt bản nháp tác phẩm**.
- QA: `CreatorDraftRulesTest` (JVM), `CreatorDraftStudioUiTest` (Android debug), truy vấn RLS/grant, kiểm tra web JavaScript.

## Những việc chưa làm

- **Chưa có tải bản ghi âm, ảnh bìa, tệp chứng cứ lên Supabase Storage**; trường bằng chứng hiện chỉ nhận mã hoặc mô tả do người gửi khai báo. Không đủ để khẳng định giấy phép đã được kiểm chứng.
- Chưa tạo luồng phát hành thực tế từ bản nháp đã duyệt; không bỏ qua pipeline bản quyền `musicflow_publish_licensed_track_v2`. Không công khai nhạc mà chưa đủ chứng cứ được kiểm tra.
- Chưa có tài khoản Owner được xác minh/khởi tạo; vì vậy **không được tuyên bố kiểm thử E2E giữa Creator với quản trị viên thật đã hoàn tất**.
- Không thay đổi việc **tệp offline do người dùng tự lưu vẫn phát bình thường**.

## Trạng thái nghiệm thu

- [x] SQL đã triển khai lên project Supabase đang sử dụng.
- [x] Bảng riêng RLS bật, không có quyền SELECT trực tiếp cho anon/authenticated; tất cả 5 RPC không cho anon. Người nghe không có role bị từ chối khi tạo nháp/xem hàng chờ/duyệt; Owner vẫn chưa tự tạo.
- [x] Gradle debug và AndroidTest build PASS; **3/3 JVM**, **2/2 Creator UI**, **4/4 Role UI hồi quy** PASS. Không chạy full regression.
- [x] Admin web script JavaScript syntax PASS, không thêm service key hay mật khẩu cố định.
- [x] QA log và kế hoạch đã cập nhật. Bản APK mới ký demo: `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-CreatorStudioV1-Android.apk`, **30.983.869 byte**, SHA-256 `7CB9F67DBF4CC93F0E6246CE493306B2AA51C8010D2BA07C3C1FF905AB4D22E7`. Cài/cold start/màn Account guest PASS; AVD QA riêng đã xóa, giữ emulator có sẵn.

**Hạn chế QA:** không có Owner đã xác minh nên **không kiểm thử E2E đăng nhập Owner → cấp quyền Artist → nháp → quản trị viên duyệt** bằng các tài khoản thật. UI dùng RPC giả trong test Android; kiểm tra từ chối quyền thực hiện trên Supabase. Trường bằng chứng hiện chỉ là mô tả/mã, không phải giấy phép đã thẩm định.

**Supabase Security Advisors:** còn cảnh báo về 18 RPC `SECURITY DEFINER` callable bởi authenticated (các RPC mới cố ý có kiểm tra `auth.uid()`/role trong thân hàm), và **Leaked Password Protection đang tắt**. Cần rà soát/siết lại và bật bảo vệ mật khẩu trước khi phát hành thương mại; RLS không có policy ở bảng riêng là chủ ý từ chối mọi truy cập trực tiếp. Xem `qa-logs/musicflow-creator-studio-v1.log` và `qa-logs/demo-apk-creator-studio-v1-package.log`.
