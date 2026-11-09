# MusicFlow — Community v14: đánh giá sao và bình luận

**Ngày:** 2026-11-09. **Phạm vi:** module độc lập cho bài hát đã xuất bản và xác minh quyền trên Supabase; phần Demo mẫu không sinh dữ liệu công khai. Không thay trình phát/SAF/Room/playlist/quyền streaming/entitlement.

## 1. Trải nghiệm và use case

| Mã | Actor | Hành trình | Ràng buộc |
|---|---|---|---|
| UC-C01 | Guest/Free/Premium | Khám phá online → Đánh giá & bình luận → đọc sao trung bình/bình luận | Guest chỉ đọc; bài phải có UUID trên `music_tracks`, `is_published=true`, `rights_confirmed=true` |
| UC-C02 | Người dùng Supabase đã đăng nhập | Chọn 1–5 sao, chọn lại để thay điểm | Duy nhất một bản ghi trên cặp `track_id,user_id`, không cộng trọng số Premium |
| UC-C03 | Người dùng Supabase đã đăng nhập | Đăng bình luận; sửa/xóa của mình | 1–500 ký tự; tải theo trang 20; mỗi lần đăng cách nhau tối thiểu 10 giây phía server |
| UC-C04 | Người dùng Supabase đã đăng nhập | Báo cáo bình luận người khác | Lý do 5–300 ký tự; mỗi tài khoản báo cáo một lần trên một bình luận |
| UC-C05 | Owner / PlatformAdmin được cấp `moderate_catalog` / Moderator được Owner cấp | Tải báo cáo → ẩn bình luận hoặc bác báo cáo → nhập lý do → xác nhận | Phía server xác minh quyền và ghi `musicflow_private.role_audit`; Moderator chỉ có role chưa đủ quyền |
| UC-C06 | Owner | Chọn tài khoản có role Moderator → cấp/thu hồi quyền kiểm duyệt cộng đồng có lý do | Không cấp role mới; RPC kiểm tra cả role Moderator lẫn Owner |
| UC-C07 | Mọi người dùng Demo | Khám phá online → Xem thử đánh giá/bình luận (Demo) → tương tác với bình luận mẫu | `DemoCommunityStore` chỉ giữ RAM, không gọi Supabase, không cấp quyền thật, khi đóng giao diện có thể reset |

**Giới hạn nguồn nhạc:** tệp offline MediaStore/SAF chỉ tồn tại trên thiết bị, không nhận ID public và không đẩy đường dẫn tệp lên server. Danh mục online Supabase lúc rà soát có **0 bài**; test với bảng public chưa có bài cần dùng Demo riêng. Dữ liệu nguồn catalog bên thứ ba không được viết vào Supabase.

## 2. Kiến trúc và API

- `core/community/CommunityModels.kt`: validate sao, nội dung, lý do, ID UUID; model snapshot/comment.
- `core/community/DemoCommunityStore.kt`: fixture độc lập không có JWT.
- `core/community/SupabaseCommunityRepository.kt`: sử dụng `AccountApi.restore()`, JWT thật và allow-list RPC có sẵn; đọc Guest qua RPC public HTTPS giới hạn response 128 KiB; chỉ chấp nhận UUID cloud; mọi ghi luôn kiểm tra phiên.
- `feature/community/CommunityRoute.kt`: giao diện Compose dialog theo bài, nút sao trợ năng, phân trang, sửa/xóa có xác nhận, báo cáo có lý do, trạng thái lỗi/mất kết nối.
- `feature/community/CommunityModerationPanel.kt`: hàng đợi báo cáo trong không gian quản trị; xác nhận với lý do.
- `feature/online/OnlineCatalogScreen.kt`: chỉ bài thuộc catalog do Supabase quản lý mới có nút cộng đồng thật; Demo có nhãn ở luồng rỗng/chưa kết nối.
- `feature/account/AccountRolePanel.kt`: mục xử lý bình luận và thao tác Owner cấp/thu hồi quyền Moderator có xác nhận.

SQL versioned: `docs/sql/musicflow_community_v1.sql` (Supabase migration `musicflow_community_v1`, applied 2026-11-09). Bảng `public.music_track_ratings`/ `music_track_comments`/ `music_comment_reports`, bảng grant `musicflow_private.community_moderator_grants`. Tất cả bật RLS và từ chối SELECT/INSERT/UPDATE raw với `anon`, `authenticated`; thao tác chỉ qua RPC với xác thực trong body máy chủ. Public RPC `musicflow_community_read(uuid,int)` chỉ trả aggregate, danh sách bình luận visible và tên hiển thị, không trả user ID. Chức năng ẩn bình luận giữ nội dung nhưng không hiển thị công khai; xóa từ chủ bài thì trống nội dung.

RPC:
`musicflow_community_read`, `musicflow_community_rate`, `musicflow_community_comment_create`, `musicflow_community_comment_edit`, `musicflow_community_comment_delete`, `musicflow_community_report`, `musicflow_community_reports`, `musicflow_community_moderate`, `musicflow_community_grant_moderator`. `musicflow_community_can_moderate` chỉ dùng nội bộ, không GRANT trực tiếp.

**Đối chiếu Supabase live:** migration có phản hồi `success=true`. Truy vấn metadata xác nhận ba bảng public mới `rowsecurity=true`, không `anon SELECT`, không `authenticated INSERT/UPDATE`; `anon` chỉ có EXECUTE RPC read và không được gọi RPC ghi. Advisor thông báo RLS không policy (INFO, **cố ý deny raw access**) và `SECURITY DEFINER` exposed (WARN). Các RPC có explicit `auth.uid()` / role checks, revoke mặc định và kiểm tra bài đã xuất bản. **Vẫn cần kiểm thử xâm nhập/phân quyền E2E bằng tài khoản riêng**, và định kỳ rà soát advisor: https://supabase.com/docs/guides/database/database-linter?lint=0028_anon_security_definer_function_executable.

## 3. Impact-driven Regression

- **Module mới:** `COMMUNITY_RATING`, `COMMUNITY_COMMENTS`, `COMMUNITY_MODERATION` với test JVM và Compose riêng trong `qa/impact_modules.json`.
- **Module có thay đổi:** `AUTH` (RPC allow-list), `ONLINE_RIGHTS` (catalog detail entry), `ROLES_ADMIN` (workspace), `UI_ACCESS` (trợ năng).
- **Gián tiếp:** Guest/free/premium/Creator/Moderator/Owner/Developer journeys, và Home/Playback qua điều hướng; audio pipeline, local URI, quyền phát không được thay.
- **QA policy:** vì SQL/auth/role và >=4 module, `full_required`. Map v14 có 22 module, **478 phương thức @Test đăng ký** (không đồng nghĩa 478 đã chạy). JVM **210/210 PASS**, Android Community **5/5 PASS**, Android Auth/Owner/Online/v13-UX impact **14/14 PASS** trên máy ảo API36 chuyên dụng. Supabase SQL rollback test: bài chưa xuất bản và Guest ghi sao đều bị từ chối, không để lại dữ liệu. Tuy nhiên chưa chạy trọn 478 phương thức, chưa E2E đăng bình luận thật trên tài khoản thử / nhiều thiết bị hoặc manual TalkBack.

## 4. Phạm vi bàn giao

Community v14 là tiện ích demo và cơ sở dữ liệu thật, **không có nội dung đã được cấp quyền trên catalog**. Không tự tạo bài hát giả có cờ quyền phát ở public database, không phát hay tải file không có license. Quyền Owner/PlatformAdmin/Moderator được server xác minh; chỉ chọn persona trong Demo Center không thay đổi auth.role/RLS. Không có hệ thống trả lời lồng nhau, media attachments, notification, tính điểm Premium, realtime hay tự động kiểm duyệt AI.
