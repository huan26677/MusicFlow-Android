# MusicFlow — Core Media Source Contract v1

**Mục tiêu:** một nơi tạo Media3 `MediaItem` cho MediaStore, tệp chọn SAF, online HTTPS, dữ liệu playlist/history/queue đã lưu. Không thay đổi Room schema, queue snapshot version, hoặc định danh bài của người dùng.

## Đã triển khai

| Luồng | Định danh được giữ nguyên | Adapter |
|---|---|---|
| MediaStore | `song.mediaId` nếu đã có; còn lại `device-<id>` | `TrackMediaItems.fromDevice` |
| SAF picker | URI gốc `content://...` (giữ cả `file://` cũ từ fixture) | `TrackMediaItems.fromPicked` |
| Online đã được cấp phép bởi backend | `online:<track.id>` | `TrackMediaItems.fromOnlineOrNull` |
| Playlist / history / queue snapshot | `mediaId` và `audioUri` nguyên bản | `TrackMediaItems.fromStored` |

- `MediaSourceContract.kt` là lớp Kotlin thuần với `MediaSourceKind`, `MediaSourceRef`, luật phân loại ID, không phụ thuộc Android, Compose hay Supabase.
- `TrackMediaItems.kt` là Android/Media3 adapter thống nhất metadata/URI, không chứa business logic của giao diện.
- `PlayerViewModel`, `PlaylistMediaItems`, `PlaybackQueueStore` gọi chung adapter. Các snapshot v1 đã có vẫn đọc được vì trường `mediaId`, `uri`, `title`, `artist` không đổi.
- Online chỉ đưa vào queue khi URI là HTTPS có host, không chứa userinfo hay fragment; query của signed URL được phép. Lựa chọn bài dựa trên **chỉ số dòng gốc**, không dựa trên ID duy nhất (bảo vệ khi catalog có ID trùng/bài không phát được).
- Phân loại `LICENSED_ONLINE` chỉ là tên nguồn; **không tự chứng minh bài nhạc có giấy phép**. Backend phải enforce rights/entitlement, kiểm tra hợp đồng bản quyền và expiry URL trước E2E thực.

## Tương thích và giới hạn

- Không re-key `device-123`, `content://...`, `online:...` hoặc ID tự đặt đã lưu. Nếu cần namespace online nhiều nhà cung cấp, phải thiết kế migration riêng kèm relink; không thay các ID hiện tại âm thầm.
- Không tự động làm mới signed streaming URL. Snapshot cũ có thể chứa signed URL đã hết hạn; cần phát triển resolver qua backend trước khi coi online restore là hoàn chỉnh.
- `fromStored` giữ dữ liệu legacy, không biến dữ liệu tự khai thành nguồn nhạc có bản quyền. Android URI permission và backend là ranh giới tin cậy tiếp theo.
- Chưa tách thành Gradle `:core:model` module vì các ranh giới cần được kiểm chứng ở source trước khi di chuyển package/module.
- Không đụng Google Login hay email thật theo phạm vi 1.0 đã chốt.

## Kiểm thử

- JVM: `MediaSourceContractTest` kiểm tra identity fallback, legacy ID, SAF, online, phân loại và từ chối URL từ picker sai.
- Android: `TrackMediaItemsTest` kiểm tra Media3 item metadata, URI, signed HTTPS query, URL không an toàn, lựa chọn đúng index.
- Regression Android: `QueueManagerIntegrationTest`, `PlaybackQueuePersistenceTest` bảo đảm sử dụng Media3 và snapshot sau thay đổi.
- Script `scripts/run-media-source-contract-v1-tests.ps1` sinh log `qa-logs/media-source-contract-v1.log`. Kết quả PASS chỉ áp dụng khi script hoàn tất với `FINAL_RESULT=PASS`.

**Bước kế tiếp:** UI/UX 1.0 hardening, accessibility, trạng thái lỗi/rỗng, ưu tiên không cần thông tin đăng nhập; kế tiếp là kiểm tra licensed streaming smoke sau khi có nhạc hợp lệ.
