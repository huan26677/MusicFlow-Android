# MusicFlow — Playback Queue Actions Runtime Acceptance v1

**Ngày:** 2026-11-01. **Git HEAD nền:** `8c39415` (Crossfade UI & Interruption QA v2 đã commit). **Kết quả:** Android Build và JVM **105/105 PASS**; Android API 30 **2/2 Queue Actions Runtime + 11/11 Playback/Queue Manager Regression = 13/13 PASS**. Đây là QA chức năng theo phạm vi cụ thể, không đồng nghĩa nghiệm thu toàn MusicFlow 1.0.

## Phạm vi thực thi

Chạy **giao diện `LibraryScreen` và `SearchScreen` thật** bằng Compose instrumentation, trên dữ liệu `DeviceSong` QA được cung cấp có chủ đích (WAV PCM 60 giây trong cache). Nút ba chấm của `SongRow`, hai mục **Phát tiếp** và **Thêm vào hàng đợi**, callback `PlayerViewModel.queueDeviceSong`, `TrackMediaItems.fromDevice`, `PlaybackController.enqueue` và `PlaybackService`/MediaSession đều là **code production thật**.

Không gọi `LibraryRoute` / `SearchRoute` để quét MediaStore/điều hướng `MainActivity` trong bài E2E này; nguồn hàng trong UI là `DeviceSong` kiểm thử, nhưng mỗi file WAV và MediaSession phát đều là thật. **Nhạc offline người dùng tự tải/tự lưu vẫn nghe bình thường, không cần xét giấy phép streaming online của MusicFlow**; không sử dụng nhạc có bản quyền hoặc kết nối Supabase.

## Những tình huống đã kiểm thử

Thêm `app/src/androidTest/java/com/example/musicflow/core/playback/QueueActionsRuntimeTest.kt`, 2 bài Android:

1. **`libraryPlayNextThenSearchAppendPreservesPlaybackAndSnapshot` — PASS.** Bắt đầu playback thực ở vị trí 5 giây với hai WAV `Queue Alpha`, `Queue Omega`. Nhấn menu hàng đợi trên `LibraryScreen` của `Queue Next` → **Phát tiếp**; xác minh thứ tự trở thành **Alpha → Next → Omega** và Alpha vẫn đang phát, không reset vị trí. Chuyển sang `SearchScreen` với `Queue End`, nhấn menu → **Thêm vào hàng đợi**; thứ tự thành **Alpha → Next → Omega → End**. Đọc `musicflow_playback_queue_v1.snapshot` do service lưu thật, xác minh bốn media ID đúng thứ tự/index ban đầu. Sau đó gọi `PlayerViewModel.next()` và xác minh **Next thực sự phát đúng bài mới chèn**, không chỉ đổi nhãn giao diện.
2. **`nextRejectsShuffleRepeatOneAndEmptyQueueWithoutInterruptingAudio` — PASS.** Trên controller có MediaSession đang phát, bật shuffle và đợi **PlaybackController khác** nhận trạng thái mới rồi thử Phát tiếp: bị từ chối, hàng đợi không đổi. Làm tương tự với repeat-one, MediaItem thiếu URI: đều từ chối. Khi queue rỗng, cả NEXT/END không tự khởi chạy hoặc tạo queue. Bài test khôi phục trạng thái phát.

**Phát hiện và cách xử lý:** Lượt kiểm thử đầu FAIL ở tình huống bật shuffle vì hai `MediaController` cập nhật trạng thái bất đồng bộ, bài test gọi enqueue **trước khi controller nhận được sự kiện shuffle**. Sửa **đồng bộ kỳ vọng trong bài test**: chờ `PlaybackController.uiState` phản ánh shuffle/repeat-one/empty timeline trước khi xác minh chính sách từ chối. Chạy lại **2/2 PASS**. Không sửa hoặc nới lỏng `QueueInsertRules`, quyền truy cập nguồn offline, Media3 hay các callback production.

## Bằng chứng và khả năng tái hiện

- Runner: `scripts/run-playback-queue-actions-v1-tests.ps1`, log: `qa-logs/playback-queue-actions-v1.log`.
- `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`: **BUILD SUCCESSFUL**; XML **105 JVM PASS, 0 FAIL/0 SKIP**.
- `QueueActionsRuntimeTest` **2/2 Android PASS**; 7 marker: `offlineQueueReady`, `libraryPlayNextOrder`, `searchAppendAtTail`, `queuePersistedInCorrectOrder`, `insertedNextActuallyPlays`, `shuffleRepeatAndInvalidRejected`, `emptyQueueDoesNotAutoPlay`.
- Hồi quy trên phiên instrumentation riêng: `PlaybackServiceIntegrationTest` **3/3**, `PlaybackQueuePersistenceTest` **5/5**, `QueueManagerIntegrationTest` **1/1**, `QueueManagerScreenTest` **2/2**; tổng **11/11 regression PASS**.
- **Tổng 13/13 Android PASS** trên emulator **API 30**; `GIT_DIFF_CHECK_EXIT=0`, `FINAL_RESULT=PASS`. Script đòi `OK (N tests)` và không có `FAILURES` vì `adb am instrument` đôi khi trả exit code 0 khi test lỗi.
- Cleanup: snapshot queue/position, playWhenReady, shuffle/repeat, speed, SharedPreferences của queue, xóa WAV test và release controllers. Mã production không thay đổi ở mốc này.

## Giới hạn / việc kế tiếp

Chưa kiểm thử vòng E2E từ **MediaStore scanner / MainActivity / LibraryRoute / SearchRoute** với cấp quyền đọc media và kết quả tìm kiếm thật; bài test này kiểm thử màn `LibraryScreen`/`SearchScreen` dùng state có kiểm soát và các callback production nối tới MediaSession thật. Chưa thử đa API 29/33/36, điện thoại thật, việc enqueue trong lúc đang crossfade, độ trễ của MediaStore thư viện lớn, full unfiltered connectedAndroidTest sau mốc này. Không suy ra ứng dụng MusicFlow 1.0 đã nghiệm thu.

**Bước kế tiếp (mốc nhỏ): Advanced Library Runtime Acceptance v1** — kiểm thử MediaStore thực với bài WAV QA, quyền đọc nhạc, màn Library hiển thị và nhóm Album/Artist/Genre phù hợp. Sau đó thực hiện **Advanced Search Runtime Acceptance v1** riêng, trước Release Candidate packaging. Google Login/email thật và cloud hai thiết bị E2E vẫn ở gần cuối; Licensed Streaming Smoke phụ thuộc nhạc được cấp quyền.
