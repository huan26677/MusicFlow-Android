# MusicFlow — Lyrics Import UI End-to-End v2

**Ngày:** 2026-10-29. **HEAD nền:** `6cf6d21` (Lyrics Runtime Acceptance v1 đã được commit). **Trạng thái:** PASS cho E2E nhập LRC qua MainActivity trên emulator Android 11 (API 30), cùng các nhóm hồi quy. Đây không phải nghiệm thu toàn bộ MusicFlow 1.0.

## Phạm vi và chuỗi thao tác thực

Thêm `app/src/androidTest/java/com/example/musicflow/core/lyrics/LyricsImportUiE2eTest.kt` với **một bài E2E duy nhất** chạy trên `MainActivity` và `PlaybackService`/MediaSession thực:

1. Dùng `ActivityResultContracts.CreateDocument` mở **DocumentsUI thật** để tạo file LRC UTF-8 trong bộ lưu trữ thử; lấy tên hiển thị thực từ `OpenableColumns.DISPLAY_NAME`. Không giả lập SAF URI hoặc ActivityResult.
2. Sinh WAV offline 30 giây riêng, đặt làm media item trên MediaController thật; chờ MiniPlayer hiển thị bài **Lyrics UI E2E track**. Nhấn đúng MiniPlayer bằng `contentDescription` để chuyển tới **Now Playing**.
3. Cuộn tới nút **Nhập file .lrc** và nhấn bằng Compose UI test. Đây là callback production `MainActivity.onPickLyrics` mở `ActivityResultContracts.OpenDocument`, không trực tiếp gọi repository từ test.
4. UIAutomator tìm đúng tên tệp LRC trong Google DocumentsUI, long-click thẻ document, bấm **SELECT** của Android, chờ quay về MusicFlow. Bài test **không tính việc quay về ứng dụng là đủ**: còn phải thấy lời thực trên Now Playing và liên kết SAF trong SharedPreferences.
5. Xác minh `LocalLyricsRepository` được chính callback production nhập: URI document đúng với liên kết audio URI riêng của bài, persisted READ grant thật, đọc lại 3 dòng lyric và hiển thị **UI first line** trên Now Playing.
6. Tạm dừng và tua MediaController thật tới **1.4 / 3.4 / 6.4 / 1.6 giây**; cứ mỗi mốc, `NowPlayingScreen` phải có đúng một `lyrics-active-line` chứa lời tương ứng, bao gồm tua ngược.
7. **Cleanup:** khôi phục queue, vị trí, shuffle/repeat và trạng thái phát trước bài test; trả lại khóa liên kết lời đã lưu, xóa document QA và thu hồi persisted READ, xóa WAV tạm. Không sửa thư viện nhạc cá nhân.

## Bằng chứng kết quả

- Script `scripts/run-lyrics-import-ui-e2e-v2-tests.ps1` nhận `-DeviceId`, xác nhận API 30/boot, Build APK + test APK + JVM, cài APK, chạy E2E rồi lần lượt chạy 2 bài Lyrics v1 và 8 bài Playback/Queue trên các phiên instrumentation riêng, kiểm tra `git diff --check`. Script **không tin exit code 0 đơn lẻ của instrumentation** mà buộc có `OK (N test/tests)` và không có FAILURES.
- Log `qa-logs/lyrics-import-ui-e2e-v2.log`: **`FINAL_RESULT=PASS`**, `BUILD_JVM_EXIT=0`, `JVM_TOTAL=105`, `JVM_FAIL=0`, `JVM_SKIP=0`.
- Android API 30: **1/1 E2E PASS**, 7/7 markers, **2/2 Lyrics Runtime v1 PASS**, **8/8 PlaybackServiceIntegration + PlaybackQueuePersistence PASS**; tổng **11/11 targeted Android PASS**, `GIT_DIFF_CHECK_EXIT=0`.
- 7 E2E markers bắt buộc: `realLrcCreated`, `realNowPlayingImportButton`, `PICKER_TOUCHED_REAL_DOCUMENT`, `PICKER_SELECT_CLICKED`, `PICKER_RETURNED_TO_MUSICFLOW`, `productionCallbackPersistedAndRendered`, `realPlayerSeekUpdatesImportedLyrics`.
- Lượt thử đầu tiên FAIL trong test automation vì tên bài xuất hiện ở MiniPlayer lẫn Home/History; sửa selector dùng **contentDescription của MiniPlayer**. Lượt đầu của script QA báo FAIL giả dù instrumentation `OK (1 test)`: script dùng chuỗi `OK (1 tests)`; đã phân biệt số ít/số nhiều và chạy lại script **PASS đầy đủ**. Không có sửa production code.

## Giới hạn nghiệm thu

- Chỉ chạy trên **emulator API 30**; chưa thử OpenDocument trên API 29/33/36 hoặc nhà cung cấp document khác, điện thoại thật, TalkBack, file LRC cực lớn.
- Không xác minh sau khi Android giết tiến trình/reboot; repository load sau restart và process death cần bài riêng.
- Không chạy lại toàn bộ `connectedDebugAndroidTest` sau mốc này, chỉ 11 bài targeted Android + 105 JVM.
- **Chỉ kiểm thử nhạc offline** với tệp WAV QA, không phát hay tải nội dung online chưa có quyền. Nhạc offline người dùng tự lưu/tải không bị MusicFlow xác minh giấy phép streaming.

## Mốc tiếp theo (nhỏ)

**Playback Speed Runtime Acceptance v1:** kiểm tra thay tốc độ phát 0.75x / 1x / 1.25x / 1.5x / 2x bằng MediaSession thật, không làm hỏng queue, pause/resume hoặc trạng thái trên Player. Sau đó chọn các mốc runtime build-only khác. Google/email thật và Cloud hai thiết bị E2E vẫn để gần cuối, Licensed Streaming Smoke phụ thuộc nhạc thật có quyền phát.
