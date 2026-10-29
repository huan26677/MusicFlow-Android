# MusicFlow - Full Product Regression Gate v1 - Part 2A: Now Playing controls

**Ngày:** 2026-10-29. **Git HEAD nền:** `a82fea3` (Part 1 đã commit). **Kết quả phần 2A:** BUILD/JVM PASS + **8/8 Android API 30 PASS**. Full Product Regression Gate tổng thể vẫn **CHƯA ĐẠT**, vì 2 bài RealPickerSafTest và 3 bài phase-specific SKIP từ baseline chưa được xử lý bằng lượt full-suite mới.

## Phạm vi nhỏ của phần này

Chỉ chẩn đoán và khắc phục bài `ExampleInstrumentedTest.mvpLibrarySearchToRealPlayer` thất bại ở lần full-suite trước. Không sửa production `PlaybackController`, `PlaybackService`, Media3/MediaSession, Supabase hoặc phần quyền bản quyền online; không sửa SAF (để Part 2B).

## Nguyên nhân được xác minh

- Trên Now Playing, danh sách dọc có cuộn, các nút `Trước`, `Tiếp`, `Tạm dừng`, `Phát` và `‹ Trở về` nằm ở những vị trí cuộn khác nhau.
- Bài test cũ dùng `performClick()` trực tiếp. Trong lần đầy đủ, test timeout sau `Trước`; khi kiểm thử có log và `performScrollTo().performClick()`, callback `previous` thật được gọi (count=1), `PlayerUiState` chuyển từ **MVP Beta (index 2)** sang **MVP Alpha (index 1)**. Test sau đó mới timeout ở `Tiếp` do cùng kiểu chạm ngoài viewport.
- Sửa test dùng `performScrollTo().performClick()` cho **năm** thao tác có thể nằm ngoài viewport; giữ mọi `waitUntil` xác nhận phát thật từ MediaSession và thêm `assertEquals(1, previousClicks)`, không giả lập callback để làm test pass.

## Bằng chứng QA

- Script: `scripts/run-full-regression-v1-part2a-playback-tests.ps1`.
- Log: `qa-logs/full-product-regression-v1-part2a-playback.log`, có `BUILD_JVM_EXIT=0`, `INSTRUMENTATION_EXIT=0`, `OK (8 tests)`, `MVP_PLAYBACK_MARKERS=5`, `FINAL_RESULT=PASS`.
- Android emulator: `emulator-5554`, API 30. Các lớp chạy: `ExampleInstrumentedTest` (**3/3**), `PlaybackServiceIntegrationTest` (**3/3**), `PlaybackSkipRecoveryTest` (**2/2**).
- Đúng năm marker: `libraryUiToMediaSession`, `nextPreviousFromUi`, `pauseResumeFromUi`, `searchUiToMediaSession`, `uiPlaybackWritesHistory`. Các hồi quy thực tế gồm quét MediaStore, Playlist/History UI và picker giả lập trong bộ Service Integration.
- Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL**, JVM task PASS; `git diff --check` PASS.

## Điều chưa nghiệm thu và phần tiếp theo

**KHÔNG** suy ra full-suite Android 89 tests đã PASS từ kết quả 8 bài kiểm thử. Kết quả tổng thể mới nhất (Part 1) là **83/89 PASS, 3 FAIL, 3 SKIP** trước sửa; Part 2A đã sửa và xác minh lại riêng lỗi MVP Previous/Next. Còn 2 lỗi `RealPickerSafTest` vì bài test `OpenMultipleDocuments` vẫn đợi người chọn tệp thủ công; 3 SKIP là phase-specific SAF cần dùng runner tương ứng, từng được kiểm tra riêng ở Android Compatibility Matrix v2.

**Bước tiếp theo là Part 2B: chỉ sửa tự động hóa OpenMultipleDocuments** (chọn một/hai bài thật, xác nhận URI + quyền đọc + phát qua MediaSession, giữ cleanup), sau đó mới chạy lại toàn bộ Regression Gate.

Nhạc offline người dùng tự tải tiếp tục phát như bình thường, không cần xác minh quyền phát online của MusicFlow. Google Login/email thật/cloud hai thiết bị E2E để gần cuối.
