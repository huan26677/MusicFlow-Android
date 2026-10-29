# MusicFlow — Cloud Playlist & Favorites Sync v1

Ngày: 2026-10-29. Supabase: `vzvxpbwrftlnyvuhfoqn`.

## Phạm vi và nguyên tắc

- **Khách**: chỉ dùng dữ liệu local; không tạo tài khoản hay tự gửi dữ liệu lên Supabase.
- **Free và Premium**: nút **Đồng bộ thư viện** trên tab Tài khoản. Đây là thao tác **thủ công, do người dùng chủ động**; không tự gửi dữ liệu khi mở ứng dụng.
- Giữ nguyên Room Playlist và SharedPreferences Favorites. Đồng bộ là **hợp nhất cộng dồn**, không xóa cloud hoặc local khi mất mạng, đăng xuất hoặc một thiết bị không có bài.
- **Chỉ đồng bộ metadata**: tên Playlist, tên bài, tên nghệ sĩ và metadata thời gian. Không gửi file nhạc, URI `content://`, đường dẫn riêng tư, quyền SAF, lịch sử nghe hay bản ghi LRC.
- Ở thiết bị mới, chỉ tự liên kết bài nhạc khi MediaStore tìm thấy **duy nhất một bài** có cùng tên và nghệ sĩ. Bài không khớp hoặc trùng tên sẽ được bỏ qua để tránh phát nhầm. Danh sách Playlist được phục hồi nhưng bài chưa khớp không thể phát cho đến khi có cơ chế relink riêng.
- Nếu ứng dụng từng đồng bộ local với tài khoản A, đồng bộ với tài khoản B trên cùng bộ dữ liệu local bị chặn để tránh lộ thư viện của tài khoản A. Không có nút chuyển chủ sở hữu trong v1.
- Đồng bộ không có cơ chế xóa từ xa; xóa playlist trên thiết bị chưa có nghĩa là xóa bản sao cloud. Tính năng xóa đồng bộ, tombstone, xử lý xung đột nâng cao thuộc v2.

## Cấu trúc Supabase

Migration `musicflow_cloud_playlists_favorites_v1`:

- `public.cloud_playlists`: `owner_id`, `playlist_id`, tên, `created_at_ms`, `updated_at_ms`, `tracks` JSONB. Giới hạn tối đa 500 metadata bài/playlist và 128 KiB JSONB.
- `public.cloud_favorites`: `owner_id`, `favorite_key` (SHA-256 của tên+nghệ sĩ chuẩn hóa), tên bài và nghệ sĩ.
- Cả hai bảng bật RLS; `anon` không được đọc. `authenticated` chỉ đọc/ghi bản ghi có `owner_id = auth.uid()`. Không cho xóa bằng Android.
- RPC `musicflow_merge_cloud_playlist` (`SECURITY INVOKER`) dùng `auth.uid()`, chỉ cấp `EXECUTE` cho authenticated. Upsert và gộp tracks nguyên tử trong một giao dịch PostgreSQL; giữ tên mới hơn theo `updated_at_ms`, không để thao tác sync của một máy tự xóa bài của máy khác.
- Free 20 Playlist/2 thiết bị và Premium 5 thiết bị là **quyền lợi dự kiến**, CHƯA có quota máy chủ hoặc tính năng kiểm đếm thiết bị ở mốc này. Không xem giới hạn phía client là bảo mật.

## Android

- `CloudLibrarySync.kt` lấy metadata từ Room Playlist và LocalFavoritesStore, gửi qua Supabase Data API với JWT của tài khoản đã đăng nhập; khi tải về chỉ merge và relink bài được tìm thấy duy nhất bằng MediaStore.
- `CloudSyncRules.kt`: chuẩn hóa tên, kiểm tra match duy nhất, không nhận URI từ cloud. Có JVM unit test để tránh ghép nhầm khi metadata bị trùng.
- `RoomPlaylistRepository.mergeCloudPlaylist` và `LocalFavoritesStore.addSongIfAbsent` không xóa dữ liệu đang có.
- `AccountViewModel` cung cấp trạng thái chạy, số lượng đã đưa lên/lấy về và số bài chưa relink; tài khoản Guest không thấy nút đồng bộ.
- `AccountApi` dùng lại phiên Supabase Auth; refresh token khi gặp 401, không gửi service_role hoặc secret key trong Android.

## Kiểm tra và giới hạn

- Supabase: kiểm tra RLS/grants, anon không gọi RPC; thử cập nhật hai lần trên cùng playlist với role authenticated, xác minh hai bài được giữ và ROLLBACK để không tạo dữ liệu giả.
- Android: build APK ứng dụng, APK AndroidTest và chạy JVM unit tests; log `qa-logs/cloud-sync-v1.log`.
- **Chưa test E2E bằng hai tài khoản/thiết bị thật**, chưa test mất mạng giữa hai request, chưa có realtime background sync, chưa có xóa đồng bộ hay conflict UX.
- Nếu cấp quyền MediaStore thiếu thì thao tác sync có thể thất bại; luồng local vẫn hoạt động.
- Chỉ xử lý tối đa 100 Playlist và 500 Favorites trong mỗi lượt tải. Số bài/playlist tải lên giới hạn 500 metadata. Cần phân trang và quota backend ở mốc tiếp.

## Bước phát triển tiếp theo

**Cloud Sync Hardening v2 / Track Relink UX**: UI cho bài chưa liên kết, cấp quyền và chọn file tương ứng, xóa đồng bộ có tombstone, phân trang lớn, xử lý xung đột nhiều thiết bị, quota Free/Premium được thực thi phía máy chủ.
