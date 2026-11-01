# MusicFlow — Advanced Search Runtime Acceptance v1

**Ngày:** 2026-11-01. **Git HEAD nền:** `a3055de` (Advanced Library Runtime Acceptance v1 đã được commit). **Kết quả cuối:** Gradle Build + **105/105 JVM PASS**, Android API 30 **2/2 Advanced Search + 12/12 Playback/Queue/Offline Regression = 14/14 PASS**. Không coi đây là nghiệm thu MusicFlow 1.0.

## Phạm vi và dữ liệu thực

Thêm `app/src/androidTest/java/com/example/musicflow/core/playback/AdvancedSearchRuntimeTest.kt`. Hai bài test dùng **SearchRoute production thật** và `SearchViewModel`/ `MediaStoreLocalMusicRepository` thật, không mock scanner hoặc kết quả tìm kiếm. Test xác minh quyền đọc nhạc `READ_EXTERNAL_STORAGE` API 30 (dùng `pm grant` nếu chưa cấp; chưa tự động bấm hộp thoại quyền).

Mỗi bài tạo ba WAV PCM 35 giây và **publish vào `MediaStore.Audio.Media`** qua `ContentResolver` + `IS_PENDING=0`:

- `MFlow Tình Yêu <id>` và `MFlow Đường Về <id>` trong cùng thư mục Album A.
- `MFlow Khúc Nhạc <id>` ở thư mục Album B.

Scanner trả đúng ba `content://media/...` URI, tiêu đề tiếng Việt **có dấu thực** và hai `albumId` riêng. Test xóa chỉ các URI tạo ra và khôi phục queue/snapshot, tốc độ, repeat/shuffle, trạng thái phát trước khi kết thúc. Nhạc offline của người dùng vẫn **không cần giấy phép streaming dành cho kho online MusicFlow**.

## Hai bài kiểm thử Android

1. **`searchRouteAccentInsensitivePlaybackAndQueueActions` — PASS.** Đặt `SearchRoute.initialQuery="tinh yeu"` và chờ bài **Tình Yêu** do MediaStore quét; từ màn Search nhấn bài và xác minh MediaSession/ExoPlayer thực phát. Đổi nội dung ô tìm kiếm thành **`duong ve`**, mở menu hàng đợi của bài `Đường Về`, nhấn **Thêm vào hàng đợi** và xác minh bài đầu vẫn phát, bài mới nằm cuối. Tiếp tục tìm **`khuc nhac`**, nhấn **Phát tiếp**, xác nhận thứ tự **Tình Yêu → Khúc Nhạc → Đường Về**, snapshot lưu đúng thứ tự, sau đó bấm Next và xác minh MediaSession thực phát `Khúc Nhạc`.
2. **`searchRouteAlbumArtistAndGenreFilterTabsUseRealScanner` — PASS.** Đặt truy vấn tên Album A thực từ MediaStore, chọn tab **Album**, mở nhóm và nhấn **Phát toàn bộ**; kiểm tra MediaSession phát các bài của đúng nhóm. Đổi truy vấn thành `khong ro nghe si`, mở tab **Nghệ sĩ** và nhóm fallback `Không rõ nghệ sĩ`; tương tự `khong ro the loai` với tab **Thể loại** và nhóm fallback. Trở về tab **Bài hát** với một truy vấn chắc chắn không có kết quả, xác minh trạng thái `Không tìm thấy bài hát phù hợp`.

### Phát hiện và sửa test

- Lượt build đầu tiên FAIL do import không tồn tại `androidx.compose.ui.test.onNode`; đã bỏ import, **không sửa ứng dụng production**.
- Lượt Android đầu **1/2 PASS**, bài Album FAIL vì chuỗi tên Album khớp **hai node**: nội dung ô nhập và tên nhóm. Sửa selector thành `onAllNodesWithText(album).onLast()` để chạm đúng dòng kết quả, rồi chạy lại **2/2 PASS**. Không thay đổi thuật toán tìm kiếm.

## Bằng chứng QA

- Script: `scripts/run-advanced-search-runtime-v1-tests.ps1`.
- Log: `qa-logs/advanced-search-runtime-v1.log` với `FINAL_RESULT=PASS`.
- Build: `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL**; JVM XML **105 PASS / 0 FAIL / 0 SKIP**.
- Advanced Search Android: **2/2 PASS**, **13/13 marker** gồm MediaStore/Unicode/title, phát thực, queue append/next/persistence, Album playback, Artist/Genre fallback và empty-result.
- Hồi quy riêng: `PlaybackServiceIntegrationTest` **3/3**, `PlaybackQueuePersistenceTest` **5/5**, `QueueManagerIntegrationTest` **1/1**, `QueueManagerScreenTest` **2/2**, `OfflineAcceptanceFlowTest` **1/1** = **12/12 PASS**. Tổng **14/14 Android API 30 PASS**, `GIT_DIFF_CHECK_EXIT=0`.
- Runner đòi Android instrumentation có `OK (N tests)` và không có `FAILURES`, không dựa vào exit code adb đơn thuần. Test run có MediaSession và file WAV offline thật, nhưng không có nhạc thương mại.

## Giới hạn và đề xuất bước sau

- Android MediaProvider của emulator gán album theo thư mục, còn artist là `Không rõ nghệ sĩ` và genre list rỗng với WAV không gắn tag. **Chưa chứng minh tìm Artist/Genre với metadata nhúng trong MP3/FLAC thật**; chỉ xác nhận nhóm fallback. Cần bộ fixture âm thanh có tag thích hợp ở mốc QA mở rộng.
- `SearchRoute` production, scanner/ViewModel, SearchScreen, SongRow và callbacks `PlayerViewModel` được kiểm thử thật, nhưng route được dựng trực tiếp bằng Compose instrumentation, **chưa đi qua thanh tab và Navigation của MainActivity**. Bài kiểm thử quyền cấp qua shell thay vì thao tác hộp thoại xin quyền; chưa thử từ chối quyền.
- Chỉ emulator API 30, chưa chạy ma trận mới API 29/33/36, điện thoại thật hoặc toàn bộ `:app:connectedDebugAndroidTest` không lọc sau các mốc runtime mới.
- **Mốc tiếp theo (nhỏ): Full Product Regression Gate v2 — Part 1**, chạy lại toàn bộ JVM và Android instrumentation không lọc sau khi bổ sung Sleep Timer, Lyrics, EQ, Crossfade, Library/Search và Queue Actions; xử lý lỗi hồi quy từng phần trước Release Candidate Packaging. Google/email thật và Cloud E2E hai thiết bị vẫn gần cuối; Licensed Streaming Smoke chờ nhạc có quyền sử dụng.
