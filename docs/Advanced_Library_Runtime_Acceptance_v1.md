# MusicFlow — Advanced Library Runtime Acceptance v1

**Ngày:** 2026-11-01. **Git HEAD nền:** `a4b045c` (Playback Queue Actions Runtime Acceptance v1 đã commit). **Kết quả cuối:** Gradle Build + **105/105 JVM PASS**, Android API 30 **2/2 Advanced Library + 12/12 Playback/Queue/Offline Regression = 14/14 PASS**. Chỉ nghiệm thu phạm vi thử nghiệm nêu dưới đây, **không phải** toàn bộ ứng dụng MusicFlow 1.0.

## Phạm vi thực thi và nguồn dữ liệu

Thêm `app/src/androidTest/java/com/example/musicflow/core/playback/AdvancedLibraryRuntimeTest.kt`. Trên emulator Android 11, test xác nhận quyền `READ_EXTERNAL_STORAGE` được Android cấp (dùng lệnh `pm grant` khi chưa có), không giả lập quyền hoặc bỏ qua MediaStore. **Chưa tự động thao tác hộp thoại xin quyền lần đầu.**

Mỗi bài test tạo **3 WAV PCM 28 giây** bằng byte dữ liệu tổng hợp, dùng `ContentResolver.insert` vào **`MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`** với `IS_PENDING=1`, ghi WAV thật rồi publish `IS_PENDING=0`. Hai bài Alpha/Beta trong thư mục `Music/MusicFlowLibraryQA`, bài Gamma trong `Music/MusicFlowLibraryQASecond`. Test dùng `MediaStoreLocalMusicRepository.getSongs` qua `DeviceMusicScanner.scan` thật, chờ thấy chính xác 3 URI/ID thực, kiểm tra `durationMs > 0`.

**Điểm phát hiện quan trọng:** Android MediaProvider tự dùng **tên hai thư mục** làm Album, vì vậy có **hai albumId khác nhau** và Album UI tách đúng hai nhóm. Tuy đã truyền `ARTIST`/`ALBUM` trong `ContentValues`, Android scanner trả `artist = Không rõ nghệ sĩ`, `album = MusicFlowLibraryQA`/ `MusicFlowLibraryQASecond`. Vì vậy test **chỉ xác nhận fallback artist/genre** (tên không biết), không kiểm thử metadata nghệ sĩ hay thể loại có tag trong tệp nhạc thật. Đây là cách xử lý hiện tại của provider, không bị giấu bằng dữ liệu giả.

## Bài kiểm thử mới

1. **`realMediaStoreLibraryRouteScansAndStartsPlayback` — PASS.** Đưa `LibraryRoute` production lên Compose UI (có `LibraryViewModel`, `MediaStoreLocalMusicRepository`, lifecycle refresh thật), chờ đủ tên ba bài WAV vừa publish. Nhấn bài Alpha từ danh sách và xác nhận MediaSession/ExoPlayer phát từ **`content://media/...`** thực với mediaId đúng. Marker `realLibraryRouteDisplaysMediaStoreSongs`, `realLibraryRouteToMediaSession`.
2. **`mediaStoreAlbumsArtistsAndGenresOpenGroupsAndPlay` — PASS.** Với chính 3 `DeviceSong` đã quét bằng MediaStore, đưa `LibraryScreen` production lên Compose và chuyển tab **Album**; kiểm tra **`2 album`** và mở một album, nhấn **Phát toàn bộ album** rồi xác nhận số bài và bài đầu của nhóm trong MediaSession thật. Chuyển tab **Nghệ sĩ**, mở nhóm hiện có và nhấn **Phát toàn bộ**, xác nhận đúng số bài/bài đang phát. Chuyển sang **Thể loại**, mở nhóm fallback và thấy lệnh **Phát toàn bộ**. Marker `distinctRealAlbumGroups`, `realAlbumGroupPlayback`, `realArtistGroupPlayback`, `realGenreGroupDisplay`.

Test khôi phục queue, media index/position, trạng thái phát, shuffle/repeat, tốc độ và queue snapshot trước QA; release controller, xóa chính xác các URI MediaStore QA. **Không xóa thư viện nhạc cá nhân.** Các file offline của người dùng vẫn phát bình thường không cần quyền streaming của danh mục online MusicFlow.

## Kết quả QA tái hiện được

- Script PowerShell: `scripts/run-advanced-library-runtime-v1-tests.ps1`.
- Bằng chứng: `qa-logs/advanced-library-runtime-v1.log` (log có `SCANNED title/album/albumId/artist/artistId/duration` từ Android).
- `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`: **BUILD SUCCESSFUL**; **JVM 105 PASS / 0 FAIL / 0 SKIP** từ XML.
- `AdvancedLibraryRuntimeTest`: **2/2 Android PASS**, **8/8 marker** gồm đọc quyền, quét MediaStore thật, LibraryRoute hiển thị/phát, phân biệt hai album, phát nhóm Album/Nghệ sĩ và hiển thị nhóm Thể loại.
- Hồi quy riêng: `PlaybackServiceIntegrationTest` **3/3**, `PlaybackQueuePersistenceTest` **5/5**, `QueueManagerIntegrationTest` **1/1**, `QueueManagerScreenTest` **2/2**, `OfflineAcceptanceFlowTest` **1/1** (MediaStore→Playlist/History/Queue offline) = **12/12 PASS**.
- Tổng **14/14 Android API 30 PASS**. Script buộc `am instrument` có `OK (N tests)`, không có FAILURES, kiểm tra marker và `git diff --check`; log kết thúc **`FINAL_RESULT=PASS`**.
- Không thay đổi Kotlin production, kiến trúc scanner, UI hay chính sách bản quyền; toàn bộ code Kotlin mới chỉ ở `androidTest`.

## Giới hạn và mốc tiếp theo

- Chỉ emulator **API 30**; chưa có Android 29/33/36, điện thoại thật, thử quyền bị từ chối qua hộp thoại, danh mục nhạc lớn hoặc tệp MP3/FLAC có **artist/album/genre tags thật**. Cần một bộ metadata fixtures có tag để chứng minh nhóm Artist/Genre khi dữ liệu không phải `unknown`.
- Bài UI của nhóm Album/Nghệ sĩ/Thể loại nhận `DeviceSong` thật từ scanner qua state `LibraryScreen`, nhưng chưa đi qua navigation của `MainActivity`; bài `LibraryRoute` riêng đã chạy repository/lifecycle và phát bài từ danh sách.
- Chưa chạy toàn bộ `:app:connectedDebugAndroidTest` unfiltered sau mốc này. Không suy diễn 14 targeted PASS là toàn bộ sản phẩm đã nghiệm thu.

**Bước tiếp theo (nhỏ): Advanced Search Runtime Acceptance v1** — kiểm thử `SearchRoute`/SearchViewModel sử dụng MediaStore WAV thật, tìm theo tiêu đề và không dấu, các bộ lọc Album/Nghệ sĩ/Thể loại, thao tác phát và Queue qua MediaSession; ghi rõ giới hạn metadata chưa gắn tag. Login Google/email thật và cloud sync E2E hai thiết bị vẫn dời về cuối; Licensed Streaming Smoke chỉ khi có nhạc được cấp phép.
