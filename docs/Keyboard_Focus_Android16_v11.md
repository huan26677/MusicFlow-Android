# MusicFlow — Android 16 Keyboard Focus & Accessibility v11

**Ngày:** 2026-11-08. **Phạm vi:** bản Demo Android 10+, không thay đổi dữ liệu Room/SAF, quyền tài khoản, phát nhạc hoặc thanh toán.

## Vấn đề được xác minh

Lượt UX v10 trên AVD Android API36.1 ghi 41/43 PASS. Hai trường hợp `LibraryKeyboardAccessibilityV8Test#dpadMovesFocusBetweenCollectionHeaders` và `#keyboardEnterCanExpandAndCollapseArtistWithoutTouch` FAIL, trong khi bài Enter của album PASS. Bằng chứng baseline: `qa-logs/musicflow-clear-journeys-v10-targeted-api36-single-avd.log` tại commit `89fc462`.

## Cách cải thiện

- `MusicCollectionsContent.kt`: xử lý rõ `Enter`, `NumPadEnter`, `Space`, `D-pad Center` để mở/thu gọn album/nghệ sĩ/thể loại bằng bàn phím; giữ `clickable` cho thao tác chạm, không tự phát nhạc.
- Đối với nhóm đang thu gọn, `D-pad Up/Down` chuyển focus sang tiêu đề liền kề. Khi tiêu đề kế tiếp nằm ngoài vùng ảo hóa của `LazyColumn`, cuộn chính xác đến item, chờ composition và request focus. Khi nhóm đã mở, giữ điều hướng tự nhiên đến nút **Phát toàn bộ** và các bài hát bên trong.
- Trạng thái trợ năng `Đang mở/Đang thu gọn` và focus ring hiện có được giữ nguyên. Có thêm UI test điều hướng 12 nhóm ở tỷ lệ chữ 200%, kiểm tra nhóm cuối nằm ngoài màn hình.

## Modular Testing & Impact-driven Regression

- **Trực tiếp:** `LIBRARY` (UI danh sách nhóm) và `UI_ACCESS` (tương tác bàn phím + UI fixtures).
- **Downstream consumer:** `AUTH`, `BILLING_DEMO`, `CLOUD_SYNC`, `CREATOR`, `DEMO_CENTER`, `FAVORITES`, `HISTORY`, `HOME_NAV`, `LYRICS`, `ONLINE_RIGHTS`, `PLAYBACK`, `PLAYLIST`, `RELEASE_BACKUP`, `ROLES_ADMIN`, `SEARCH`, `SMART` theo `qa/impact_modules.json`.
- Planner trước sửa: `selective`, 78 phương thức (đã tăng thành **79** sau khi thêm test), gồm Android instrumentation + JVM. Direct chạy toàn bộ bài thuộc module; downstream chạy smoke/contract, không coi kế hoạch là bằng chứng PASS. Catalog cập nhật **461 phương thức @Test** từ nguồn có thật, 19 module / 105 lớp test.
- Sau QA fix, actual diff chứa `LIBRARY`, `UI_ACCESS`, `PLAYBACK`, `SEARCH` và các transcript mới; planner đúng chính sách đã nâng lên **`full_required` / 461 methods** (thay đổi ít nhất bốn module). **Chưa chạy toàn bộ 461**; cổng release/full acceptance tiếp tục mở.
- Cổng full/acceptance demo vẫn còn mở theo quy định bàn giao, kể cả khi targeted PASS.

## Kiểm thử thực tế

- Gradle offline `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`: **PASS**.
- Android 16.1 AVD QA mới `MusicFlow_KeyFocusV11_QA_API36`: **4/4 keyboard PASS**, gồm hai bài từng FAIL và bài mới D-pad vượt 12 nhóm ở 320 dp/chữ 200%. Log `qa-logs/musicflow-keyboard-v11-targeted-api36.log`.
- **43/43 bài UX v10 PASS trên API36.1** sau thay đổi. Thử thêm bài nhập JSON trong cùng một lượt gồm 44 test: 43 PASS, riêng `PlaylistBackupRealPickerUiTest#actualJsonDocumentPickerImportsOnlyAfterConfirmation` FAIL ở bước DocumentsUI không trả về, *chưa kết luận lỗi import của app*. Log `qa-logs/musicflow-keyboard-v11-ux-plus-json-api36.log`.
- Bài nhập JSON chạy độc lập **1/1 PASS**; đặt lại trạng thái DocumentsUI **trên AVD riêng** rồi chạy lại **1/1 PASS**, xác minh nhập thực sự sau khi người dùng xác nhận. Bằng chứng portable của lượt sau: `qa-logs/musicflow-keyboard-v11-json-picker-api36.log`. Đây là **test flaky khi chạy nối tiếp**, chưa được coi là full-suite green.
- **Impact-driven regression (mở rộng):** planner chọn **79 cases** (71 Android instrumentation, 8 JVM). Chạy 71 Android trên AVD API36: **65 PASS / 6 FAIL** (`qa-logs/musicflow-keyboard-v11-impact-api36.log`); không công bố full regression PASS. Hai test MediaStore và một test tìm kiếm thất bại do QA dùng `READ_EXTERNAL_STORAGE` trên Android 13+; một test nút cấp quyền kiểm tra nhãn cũ. Sau sửa QA fixtures, **3/6 bài lỗi ban đầu PASS** trong lượt retest, còn **3/6 FAIL** (`qa-logs/musicflow-keyboard-v11-impact-retest-api36.log`). Hai test tài khoản và chủ đề, một test nhóm Album/Nghệ sĩ được thử lại nhưng chưa có lần PASS đáng tin cậy; không coi đó là lỗi đã sửa hoặc là kết quả ứng dụng production.
- Các trường hợp chưa đạt cần cô lập bằng kiểm thử mục tiêu: `AccountAuthUiTest#signupTabInlineErrorsGoogleDisabledAndGuestNavigation` (inline validation không thấy trong UI real activity), `ThemeBatchTest#themePersistsAfterRestart` (không quan sát thấy thay đổi theme qua test), `AdvancedLibraryRuntimeTest#mediaStoreAlbumsArtistsAndGenresOpenGroupsAndPlay` (không tìm thấy bước mở nhóm/nút Phát toàn bộ sau khi chuyển tab). Test mới ở đầu lượt không thay đổi đăng nhập hoặc bộ lưu theme; kết quả chỉ chứng minh còn khoảng trống hồi quy cần phân tích, không xác định được nguyên nhân gốc.
- **Không tăng số bài PASS bằng cách bỏ assertion, tắt quyền, tự cấp vai trò hay xóa bài test lỗi.** Những thử nghiệm chỉnh riêng các test nói trên mà không mang lại PASS đã được loại khỏi bản mã bàn giao.

**Giới hạn:** emulator/Compose test không thay thế người thật sử dụng TalkBack, bàn phím Bluetooth hoặc điều khiển TV; cần nghiệm thu riêng. Không có thao tác lên máy/playlist người dùng.
