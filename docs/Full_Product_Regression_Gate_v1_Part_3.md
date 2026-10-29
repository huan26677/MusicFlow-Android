# MusicFlow — Full Product Regression Gate v1 / Part 3

**Ngày thực hiện:** 2026-10-29. **HEAD nền:** `84fa5d2`. **Kết quả cuối:** **PASS trong phạm vi Build/JVM + toàn bộ Android instrumentation trên emulator API 30**. Đừng suy ra toàn bộ MusicFlow 1.0 đã nghiệm thu.

## Phạm vi

- Build `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`.
- Chạy **không lọc** `:app:connectedDebugAndroidTest` trên `Pixel_5_API_30` / Android 11 / API 30.
- Đếm từng JUnit testcase từ XML mới, ghi rõ PASS/FAIL/SKIP; không lấy log lịch sử hoặc tự đổi SKIP thành PASS.
- Các pha SAF/process restart thực sự đã chạy riêng trong `docs/Android_Compatibility_Matrix_v2.md` trên API 29/30/33/36. Lượt generic instrumentation **không chứng minh force-stop** cho các bài cần cờ phase.
- Quy tắc sản phẩm không đổi: nhạc offline người dùng tự tải/lưu cục bộ vẫn nghe bình thường, không qua hệ thống kiểm tra quyền streaming online.

## Diễn biến và nguyên nhân lỗi cuối cùng

| Vòng kiểm thử | JVM | Android trên API 30 | Nhận định |
|---|---|---|---|
| Baseline Part 1 trước các bản sửa | 99/99 PASS | **83 PASS / 3 FAIL / 3 SKIP** (89 total) | Các lỗi Previous và 2 SAF real-picker |
| Part 3 — lượt unfiltered đầu tiên | 99/99 PASS | **85 PASS / 1 FAIL / 3 SKIP** | Previous, 2 SAF đã qua. `PlaylistPlaybackOrderTest.playlistUiReorderPassesCorrectQueue` không tìm thấy nút đưa Track 3 lên |
| Part 3 — lượt đầy đủ sau sửa | **99 PASS / 0 FAIL / 0 SKIP** | **86 PASS / 0 FAIL / 3 SKIP** (89 total) | **GRADLE_ANDROID_EXIT=0, FINAL_RESULT=PASS** |

**Lỗi Playlist Reorder:** test `playlistUiReorderPassesCorrectQueue` click ngay sau `setContent`, trong khi danh sách bài hát Room được tải bất đồng bộ. Ngoài ra bài thứ ba có thể nằm ngoài phần `LazyColumn` đã compose. Vì vậy nút có `contentDescription` đúng nhưng chưa nằm trong cây semantics lúc click. Đây là một lỗi độ ổn định của UI automation, không có bằng chứng Playlist Media3 phát sai.

**Cách sửa:**

- `feature/playlist/PlaylistScreens.kt`: thêm `Modifier.testTag("playlist-detail-tracks")` vào `LazyColumn` để truy xuất đúng danh sách khi kiểm thử; không thay đổi callback, reorder logic, schema hay giao diện hiển thị.
- `PlaylistPlaybackOrderTest.kt`: chờ `QA UI Track 1` xuất hiện sau tải Room, dùng `performScrollToIndex(2)` trước khi di chuyển Track 3 lần một, `performScrollToIndex(1)` trước lần hai, và cuộn tới index 2 trước khi bấm Track 2; mọi assert thứ tự playlist/callback vẫn giữ nguyên.
- Chạy lại riêng bài từng lỗi trên API 30: **1/1 PASS** với AndroidTest APK mới, rồi chạy full-suite như bảng trên.

## Bằng chứng có thể tái hiện

- Script `scripts/run-full-regression-v1-part3.ps1`, log `qa-logs/full-product-regression-v1-part3.log`.
- Script kiểm tra **đúng một emulator API 30**, xóa các Android JUnit XML cũ trước khi chạy, đọc các testcase trực tiếp từ XML; lỗi build/test trả kết quả nonzero.
- Kết quả lần cuối: `GRADLE_BUILD_JVM_EXIT=0`, `JVM_TOTAL=99`, `JVM_FAIL=0`, `JVM_SKIP=0`; `GRADLE_ANDROID_EXIT=0`, `ANDROID_TOTAL=89`, `ANDROID_FAIL=0`, `ANDROID_SKIP=3`; `GIT_DIFF_CHECK_EXIT=0`, `FINAL_RESULT=PASS`.
- Ba testcase SKIP có chủ đích trong unfiltered suite:
  `SafPermissionRestartTest.cleanupSafDocument`,
  `SafPermissionRestartTest.prepareSafDocument`,
  `SafPermissionRestartTest.verifySafAfterForceStop`.
  Các pha tương ứng từng PASS trên 4 API qua script chuyên dụng `run-android-compat-matrix-v2-device.ps1`.
- Kết quả JUnit XML: `app/build/test-results/testDebugUnitTest/TEST-*.xml`, `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml` (thư mục build local, không commit).

## Giới hạn & việc kế tiếp

Full Android/JVM **gate trên API 30** đạt; **chưa phải Full Product Acceptance**. Ma trận Android 29/30/33/36 trước đó đã thử một bộ regression đại diện, nhưng lần này **không** chạy lại toàn bộ 89 bài trên mỗi API. Chưa có thiết bị thật, Bluetooth/lockscreen/performance với kho nhạc lớn, kiểm thử toàn bộ Lyrics/EQ/Sleep Timer/Crossfade thực tế, release signing, các luồng Auth/email/Google thật, cloud E2E hai thiết bị hoặc online streaming bằng nhạc có giấy phép thật.

**Bước tiếp theo (nhỏ): Runtime Feature Acceptance — Sleep Timer v1**, tập trung một tính năng build-only còn thiếu QA, rồi xử lý Equalizer/Lyrics bằng những mốc riêng trước Release Candidate. Google/email thật và cloud hai thiết bị vẫn để gần cuối như đã chốt.
