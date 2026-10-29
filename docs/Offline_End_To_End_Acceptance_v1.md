# MusicFlow — Offline End-to-End Acceptance v1

**Ngày:** 2026-10-29. **Nền Git:** `e6cd180`. **Trạng thái:** Android API 30, build/JVM và 22 bài Android integration/regression **PASS**, chưa nghiệm thu toàn ứng dụng.

## Quy tắc sản phẩm được giữ nguyên

**File người dùng tự lưu hoặc tải về máy** qua MediaStore, SAF hoặc đường dẫn file cục bộ luôn được nghe offline mà **không yêu cầu tài khoản, Internet, RLS Supabase hay kiểm tra giấy phép phát trực tuyến từ MusicFlow**. Chỉ các stream nhạc online do catalog MusicFlow phân phối phải có quyền phát online được xác minh. Không triển khai/bật Google Login hay thử email thật trong milestone này.

## Bài E2E mới

`app/src/androidTest/java/com/example/musicflow/core/playback/OfflineAcceptanceFlowTest.kt`

- Tạo hai tệp WAV ngắn hợp lệ bằng PCM mẫu kiểm thử rồi chèn vào MediaStore `Music/MusicFlowQA/`. Chỉ cấp quyền đọc nhạc của ứng dụng trên emulator khi cần; cleanup xóa từng URI QA.
- Quét thư viện bằng `DeviceMusicScanner.scan`; khẳng định đúng hai URI, tiêu đề và thứ tự.
- Dựng Media3 items với `TrackMediaItems.fromDevice`, tạo playlist Room thực và lưu hai bài; kiểm tra danh sách đã lưu.
- Phát hàng đợi qua `PlaybackService`/MediaSession/`MediaController`, xác nhận đang phát bài đầu rồi bài tiếp theo.
- Xác nhận `RoomRecentHistoryRepository` lưu cả hai bản nhạc theo thứ tự nghe.
- Tạm dừng, seek, xác nhận `PlaybackQueueStore` đã lưu snapshot gồm ID, vị trí bài hiện tại và thời gian phát.
- Dựng **ExoPlayer mới** và restore snapshot, xác nhận thứ tự bài, chỉ số, URI cục bộ, vị trí phát và trạng thái **không tự phát**. Kiểm tra playlist Room vẫn nguyên thứ tự.
- Cleanup tệp thử, bản ghi history/playlist mới và khôi phục snapshot SharedPreferences trước khi kiểm thử. Không tạo bài online hoặc upload Supabase.

**9 marker bắt buộc:** `mediaStoreScanner`, `localPlaylistPersisted`, `offlineMediaSessionPlaying`, `offlineHistoryFirst`, `offlineNextTrack`, `offlineHistoryOrder`, `offlineQueueSnapshot`, `offlineQueueRestoredPaused`, `offlinePlaylistUnchanged`.

## Kiểm thử Android / JVM

Chạy script `scripts/run-offline-e2e-acceptance-v1-tests.ps1` qua PowerShell, lưu log `qa-logs/offline-e2e-acceptance-v1.log`.

| Nhóm | Số bài / trạng thái |
|---|---|
| `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` | **BUILD SUCCESSFUL + JVM PASS** |
| `OfflineAcceptanceFlowTest` | **1/1 PASS**; xác nhận 9 marker |
| `PlaylistPlaybackOrderTest` | **3/3 PASS** |
| `QueueManagerIntegrationTest` | **1/1 PASS** |
| `PlaybackQueuePersistenceTest` | **5/5 PASS** |
| `PlaybackServiceIntegrationTest` | **3/3 PASS** (bao gồm UI Playlist → Now Playing → Home/History → replay, picker, background) |
| `TrackMediaItemsTest` | **9/9 PASS** (nguồn local, SAF, legacy-online-id-có-URI-local, URL online hạn chế quyền) |
| **Tổng Android API 30** | **22/22 PASS**, `FINAL_RESULT=PASS` |
| `git diff --check` | **PASS** |

### Phát hiện và xử lý lỗi kiểm thử

- Kotlin instrumentation method lần đầu suy luận kiểu trả về không phải `Unit`; đã chỉ định `runBlocking<Unit>`, biên dịch chạy lại thành công.
- `PlaybackServiceIntegrationTest.mvpHomePlaylistNowPlayingThroughMediaSession` ban đầu FAIL sau khi chạm nút `Tiếp` ở cuối trang: `‹ Trở về` nằm ngoài viewport, click không điều hướng. Sửa test thành `performScrollTo().performClick()`, chờ thanh điều hướng Home xuất hiện trước khi click, sau đó **3/3 PASS** cả nhóm. Không sửa logic Player, Navigation hay dữ liệu để che lỗi.
- Script cũ `run-mvp-e2e-v1-tests.ps1` dùng tổng số test và marker tĩnh theo baseline cũ nên không được xem là bằng chứng chính cho milestone này. Script mới chia ba phiên instrumentation độc lập, kiểm tra count và 9 marker bắt buộc.

## Những điều milestone này chưa chứng minh

- **Chưa khởi động lại tiến trình Android thực sự trong bài E2E mới**: restore vào ExoPlayer mới và snapshot là một phép thử persistence, **không thay thế** `PlaybackProcessRestartTest` hay app force-stop/restart QA. Các bài restart trước đây có kết quả riêng, cần xác nhận lại trong ma trận tương thích.
- Chưa chạy SAF real picker / persistable grant sau reboot ở mốc này; các bài `RealPickerSafTest` và `SafPermissionRestartTest` thuộc ma trận SAF riêng.
- Chưa chạy hết API 29/33/36, thiết bị thật, Bluetooth/lockscreen, font scale toàn bộ app.
- Chưa có nội dung streaming thật đã chứng minh quyền; không áp dụng bộ bản quyền online vào nhạc offline tự tải.

**Bước tiếp theo:** `Android Compatibility Matrix v2` trên API 29/30/33/36, ưu tiên SAF/permission/restart regression và các nhóm test nền; sau đó MVP full acceptance/release candidate, Google/email thật E2E về cuối theo quyết định chủ dự án.
