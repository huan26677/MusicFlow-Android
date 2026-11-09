# MusicFlow — UX Clarity Polish v15.1 (khóa phạm vi chức năng)

**Ngày:** 2026-11-09. **Mục tiêu:** chỉ tinh chỉnh câu chữ và thao tác của các chức năng sẵn có, theo kết quả rà soát UX sau v15. Không thêm Actor, Use Case, nguồn nhạc, quyền, thanh toán hoặc bảng dữ liệu.

## Thay đổi

| Nhóm | Trước | Sau |
|---|---|---|
| Playlist / sao lưu | Lỗi tệp JSON, SAF và kiểm tra tệp còn đọc được có thể lộ nội dung exception kỹ thuật | Thông báo có hướng xử lý: chọn lại tệp, cấp quyền, đúng định dạng .json hoặc giới hạn 2 MB/100 playlist/500 bài; không xuất chi tiết backend/tệp |
| Đồng bộ thư viện, lịch sử, danh sách phát/yêu thích trên đám mây | Một số lỗi là raw exception, các câu thành công pha trộn “cloud”, “local” | Nhắc đăng nhập lại/kiểm tra Internet/quyền; thống nhất “trên đám mây” và “trên thiết bị”, làm rõ bản trên máy vẫn giữ nguyên |
| Owner/Admin | “Khu vực làm việc” và mô tả lặp lại hai lần trong cùng vùng | Nêu tên khu vực đang thao tác ngay ở tiêu đề phần nội dung, giữ nguyên bộ chọn workspace, quyền và các test tag |
| Community | “Sửa” bình luận chỉ đẩy thanh cuộn về đầu nhưng không đặt con trỏ vào ô nhập | Dùng FocusRequester đưa tiêu điểm vào đúng ô nhập đang có; không thêm editor, không thay create/edit/delete, không đổi repository/RPC |

## Bất biến nghiệp vụ

- Mọi thông báo là lớp trình bày; không thay đổi Room/SAF, bản sao sao lưu, quy trình nhập trước xác nhận, API/RPC, JWT hoặc phân quyền Owner.
- Các lỗi dữ liệu được chuyển thành thông báo định dạng/giới hạn rõ ràng; lỗi chưa biết **không** hiển thị nội dung exception hoặc địa chỉ tài nguyên riêng tư.
- Chỉ nhạc online đã xác nhận quyền phát; offline nghe bình thường không cần tài khoản; tài khoản Demo và thanh toán Demo giữ nguyên.

## QA — Modular Testing & Impact-driven Regression

**Thay đổi trực tiếp:** `PLAYLIST`, `AUTH`, `ROLES_ADMIN`, `COMMUNITY_COMMENTS`, `UI_ACCESS`. **Phụ thuộc cần kiểm tra:** `MEDIA_SAF`, `CLOUD_SYNC`, `HISTORY`, `FAVORITES`, `RELEASE_BACKUP`, đường vào Community và vai trò Demo. Policy `full_required` vì chạm UI/role + tệp SAF và đóng gói APK.

Unit tests mới `MusicFlowActionFeedbackTest` đảm bảo thông báo vẫn có giới hạn tệp hữu ích, không làm lộ chuỗi kỹ thuật, có hướng kiểm tra kết nối/đăng nhập. Android UI mới `CommunityDemoUiTest#editExistingCommentMovesFocusToExistingComposer` kiểm tra tiêu điểm thực, `AccountTaskUxV3UiTest#actorNavigationIsAvailableAtLargeFontScale` kiểm tra khu vực Owner.

**Build/JVM:** `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest` **PASS**; **223/223 JVM test PASS**. Impact engine Python **19/19 PASS**, Project Brain Python **15/15 PASS**. QA registry sau sửa **22 module, 113 lớp, 498 phương thức @Test** (`full_required`, **full 498 NOT RUN**).

**Android QA:** dùng AVD sạch `MusicFlow_UxPolishV15_1_QA_API36` (Android API36), không động tới thiết bị người dùng khác. **15/15 bài UI chọn lọc PASS**: `CommunityDemoUiTest` (bao gồm sửa bình luận chuyển focus), `AccountTaskUxV3UiTest` (Owner/Admin ở chữ lớn), `OfflineReliabilityUiTest` (sao lưu/khôi phục thu gọn và chọn lại tệp). Kiểm tra bổ sung trình chọn tệp Android `PlaylistBackupRealPickerUiTest#actualJsonDocumentPickerImportsOnlyAfterConfirmation`: **1/1 PASS**, tạo và xóa dữ liệu QA sở hữu, xác nhận chỉ nhập playlist sau hộp thoại. Log thô cục bộ `qa-logs/musicflow-v15_1-ux-android-api36.local.log` và `qa-logs/musicflow-v15_1-ux-json-picker-api36.local.log`.

**APK Demo QA:** `dist/MusicFlow-1.0-Demo-UXPolishV15_1-Android.apk`, **31.786.685 bytes**, SHA-256 `2B2DDCF1795F93FA3DCEA0711F37F7E4222B3FD85FDCA5E2BA646E16F540B56C`. Build Release/zipalign/apksigner/hash đều PASS. Chỉ ký Debug QA để phục vụ đồ án, `debuggable=false`, **không phải bản phát hành sản xuất/Google Play**; APK thuộc `dist/` Git-ignored.

**APK ký thực trên API36:** xác minh lại SHA-256/chứng thư/zipalign **PASS**; `adb install -r` lên AVD QA **PASS**, cold launch `MainActivity` **PASS**, tiến trình ứng dụng đang chạy và không phát hiện `FATAL EXCEPTION` trong lượt kiểm tra log mới. `SignedCommunityNavigationUiTest` chạy trực tiếp trên **APK ký QA v15.1**: **1/1 PASS** từ Home tới Community Demo. 15/15 UI phía trên chạy Debug, **1/1 signed** là lượt riêng; không gộp thành full regression.

**Bằng chứng Git đã lọc:** `qa-logs/musicflow-v15_1-ux-android-api36.log`, `qa-logs/musicflow-v15_1-signed-package.log`, `qa-logs/musicflow-v15_1-signed-smoke-api36.log`. Log instrumentation/đường dẫn workstation gốc chỉ giữ cục bộ, không push lên Git.

**Giới hạn:** Chưa chạy full 498, JWT multi-role HTTP, máy thật/TalkBack/UAT và v15→v15.1 signed upgrade có fixture Room/SAF. Không coi số test trong catalog là các test đã thực chạy và không suy diễn test bản Debug sang bản ký QA.
