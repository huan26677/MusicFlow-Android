# MusicFlow — Android 16.1 Regression Stability v12

**Ngày:** 2026-11-08. **Mục tiêu:** khắc phục các bài QA còn FAIL từ v11 và xác minh nhập playlist JSON khi chạy nối tiếp. Chỉ thay đổi Android instrumentation tests, **không đổi logic** xác thực, phân quyền, playback, Room/SAF hay thanh toán Demo.

## Ba bài kiểm thử được cải thiện

| Module | Test case | Điều chỉnh |
|---|---|---|
| AUTH | AccountAuthUiTest#signupTabInlineErrorsGoogleDisabledAndGuestNavigation | Chọn tài khoản Demo offline khi Auth online chưa cấu hình; cuộn nút tạo tài khoản trước khi bấm; kiểm tra lỗi email/mật khẩu trống và lối tiếp tục nghe nhạc không đăng nhập; xác nhận không còn nút Google vô dụng |
| UI_ACCESS | ThemeBatchTest#themePersistsAfterRestart | Cuộn trang bằng thao tác chạm thực để hiện chip ở dưới màn hình, đổi Sáng → Tối, kiểm tra lưu tùy chọn và khôi phục sau khi mở lại Activity |
| LIBRARY, PLAYBACK | AdvancedLibraryRuntimeTest#mediaStoreAlbumsArtistsAndGenresOpenGroupsAndPlay | Chạm tab qua test tag và cuộn ngang; cuộn danh sách ảo hóa theo key header/play; xác minh phát WAV thực qua Media3 |

**Bằng chứng riêng:** Bài Album/Artist/Genre PASS trong qa-logs/musicflow-v12-three-blockers-first-api36.local.log và hai bài Account/Theme PASS 2/2 trong qa-logs/musicflow-v12-account-theme-retest-api36.local.log. Thiết bị là AVD QA API36.1 tách biệt; hai log chỉ có trên máy QA. Toàn bộ ba bài sau đó PASS trong bộ 45 UX tích hợp lưu portable ở qa-logs/musicflow-v12-ux-json-api36.log; không coi là full regression.

## JSON DocumentsUI

Bài PlaylistBackupRealPickerUiTest#actualJsonDocumentPickerImportsOnlyAfterConfirmation tạo JSON trong Downloads của AVD, chọn tệp qua DocumentsUI thật, kiểm tra chưa nhập trước khi xác nhận, thêm bản sao vào Room và xóa chỉ fixture QA. Ở v11, bài này PASS độc lập nhưng FAIL ở cuối bộ 44 bài. v12 dùng UiObject2.click và cho phép tối đa ba lần thử cách ít nhất sáu giây nếu picker vẫn chưa trả về MusicFlow; không giảm assertion về bước xác nhận của người dùng. Cần kiểm tra bằng transcript chạy tích hợp.

## Phát hiện kiểm thử nối tiếp

Lượt tích hợp đầu v12 trên API36.1 gồm 48 test: **45 PASS, 3 FAIL** (ba bài UiAutomator D-pad/Enter), còn **JSON picker PASS** khi nằm cuối suite. Ngay sau đó, trên **cùng AVD**, chạy riêng lớp LibraryKeyboardAccessibilityV8Test: **4/4 PASS** mà không đổi mã ứng dụng. Điều này phản ánh xung đột trạng thái cửa sổ nhận phím/touch-mode giữa nhiều Android instrumented Activity; kết luận là phải tách runner keyboard khỏi runner UX/MediaStore, chứ không thay test phím hệ thống bằng lời gọi giả lập Compose để che FAIL. Bằng chứng portable: qa-logs/musicflow-v12-integrated-flake-api36.log và qa-logs/musicflow-v12-keyboard-standalone-api36.log.

Lượt kiểm tra tiếp theo đã chạy lại nhóm UX, Auth, Theme, MediaStore và JSON DocumentsUI trong runner khác với keyboard: **45/45 PASS API36.1**, JSON DocumentsUI cũng PASS ở bài cuối. Bằng chứng portable: qa-logs/musicflow-v12-ux-json-api36.log. Kết quả tách theo runner là **4/4 hardware keyboard + 45/45 UX/integration**; hai nhóm có thể có bài giao nhau với những suite khác, không cộng dồn thành full 461.

## Modular Testing & Impact-driven Regression

- **Module trực tiếp:** AUTH, LIBRARY, PLAYBACK, PLAYLIST, RELEASE_BACKUP và UI_ACCESS; downstream tính theo registry thực tế.
- **Ngưỡng QA:** planner yêu cầu full 461 phương thức thuộc 19 module vì nhiều hơn bốn module trực tiếp. Lượt targeted không được gọi là full PASS.
- **Baseline v11:** 71 Android impact cases: 65 PASS, 6 FAIL; ba bài đã sửa với quyền đọc MediaStore Android 13+/nhãn UI, ba bài trên được xử lý tiếp ở v12.
- **Retest v12 trên API36.1:** 67/67 Android impact cases (loại 4 hardware-key case để cách ly window state) **PASS** trong qa-logs/musicflow-v12-impact67-api36.log. Cộng với 4/4 bài bàn phím thực trong runner độc lập: **71/71 Android selected cases PASS**, khác với baseline 65/71. Không đếm lặp 45 case UX đã chạy riêng, và không suy diễn thành full registry 461.
- **JVM & QA scripts:** Gradle --rerun-tasks :app:testDebugUnitTest **199/199 PASS** (35 test suites XML, 0 fail/error/skip); 18/18 impact-engine, 14/14 Project Brain v2, 17/17 licensed-upload Python và 4/4 Web Admin role-refresh PASS. Build Debug/AndroidTest PASS. Bộ 461 case vẫn cần chạy đầy đủ theo chính sách trước nghiệm thu demo chính thức.
- **Nghiệm thu còn mở:** TalkBack thực, bàn phím Bluetooth/TV, nhiều SAF provider, signed upgrade API36 và full 461 tests trên API33/36, UAT người dùng.
