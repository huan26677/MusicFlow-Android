# MusicFlow v10 — Clear Journeys & Account Accessibility

**Ngày:** 2026-11-08. **Phạm vi:** trải nghiệm học tập/demo, Android 10+; không chỉnh quyền Supabase/RLS/Owner, cloud schema, nguồn nhạc hay dữ liệu SAF/Room.

## Mục tiêu

V9 ưu tiên nghe nhạc trước đăng nhập và thu gọn chức năng nâng cao. V10 tiếp tục xử lý các nhầm lẫn còn lại trong luồng người dùng:

- Khi cấu hình đăng nhập trực tuyến không có trên bản cài, tránh chỉ hiển thị tên dịch vụ kỹ thuật không có hành động tiếp theo.
- Khi Trang chủ/Tài khoản thu gọn danh sách nâng cao, TalkBack cần có trạng thái rõ **Đã mở** / **Đã thu gọn**, thay vì chỉ đọc tên nút.
- Owner/Admin/Creator có nhiều khu vực, đặc biệt với chữ phóng lớn; người dùng cần biết danh sách công việc còn lựa chọn phía dưới.
- Nút làm mới quyền không cần lộ tên nhà cung cấp backend; vẫn phải **fail-closed** và tải quyền thật mới qua RPC.

## Các thay đổi đã triển khai

1. **Tài khoản:** khi `configured=false` và không ở Demo, hiện thông báo thân thiện và nút `Dùng tài khoản thử trên máy` (tag `account-online-unavailable-demo`). Nút chỉ gọi cơ chế `onDemoModeChange(true)` hiện có, tuyệt đối không tạo phiên mạng, không tự đăng ký, không cấp quyền Premium/Admin. Vẫn có nút nghe nhạc dưới dạng khách.
2. **Trang chủ & Tài khoản:** các nút mở rộng tính năng bổ sung `Modifier.semantics { stateDescription = ... }`, nhờ đó dịch vụ trợ năng có thể đọc được trạng thái. Các nút sẵn có giữ nguyên tag và callback.
3. **Khu vực vai trò:** với màn nhỏ/chữ lớn và từ bốn khu vực trở lên, hiển thị gợi ý cuộn danh sách công việc. Nhãn làm mới quyền đổi thành `Làm mới quyền truy cập của tôi`. Điều kiện `MusicFlowAccess`, quyền thực tế và đường gọi RPC không thay đổi.
4. **Test cases:** cập nhật Compose UI fixtures kiểm tra thông báo offline, nút chuyển qua tài khoản thử, ô email bật theo trạng thái, và semantics mở/thu gọn; không sử dụng tài khoản thật.

## Impact-driven regression

Thay đổi trực tiếp: `AUTH`, `HOME_NAV`, `ROLES_ADMIN`; consumer phụ thuộc: `BILLING_DEMO`, `CLOUD_SYNC`, `CREATOR`, `DEMO_CENTER`, `HISTORY`, `LYRICS`, `ONLINE_RIGHTS`, `PLAYBACK`, `PLAYLIST`, `RELEASE_BACKUP`, `SMART`. Nguồn đã phân loại cho phép selective; thay đổi script đóng gói release và nhiều module test cần cổng full regression theo policy `docs/Test_Impact_Policy.md`. Mọi test chưa chạy phải ghi NOT RUN.

## Kết quả & giới hạn

Các con số QA phải dựa vào transcript thực tế, xem `qa-logs/musicflow-clear-journeys-v10-*.log` được rà soát hoặc hồ sơ local. `app:assembleDebug`, `app:assembleDebugAndroidTest`, `app:testDebugUnitTest` và QA-signed APK build/zipalign/apksigner/aapt đã chạy thành công.

**Android 13 (API33): 43/43 bài UI/targeted PASS** trên AVD `MusicFlow_ClearJourneysV10_QA_API33`. Bao gồm AccountHome (guest/Free/Premium/Owner/Moderator/Developer và offline fallback), AccountTask, AccountWorkspaces, HomeFoundation, DemoCenter, DemoBilling và 8 bài Keyboard/SearchIndex v8. Log `qa-logs/musicflow-clear-journeys-v10-targeted-api33.log`. Đây là **test tự động bằng fixture**, không phải thao tác tài khoản Owner thực hay TalkBack nghe thật.

**QA bổ sung 2026-11-08:** `scripts/impact_regression.py validate` PASS (19 module, 460 test methods); **18/18** unit tests cho impact engine, 14/14 cho Brain v2, 17/17 test offline licensed-upload và 4/4 kiểm tra web-admin PASS; `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest` PASS. Sửa lệnh chạy `test_musicflow_licensed_upload.py` của impact map để chạy được từ gốc repo; kiểm tra lại bằng command thực. Impact plan yêu cầu full/acceptance vì thay đổi nhiều module cùng script đóng gói/nâng cấp APK. Đây chưa phải kết quả toàn bộ 460 phương thức test.

- **Android 16 (API36) — BLOCKED:** 27 bài test kết thúc PASS, bài thứ 28 bắt đầu thì AVD `MusicFlow_ClearJourneysV10_QA_API36` mất kết nối (`adb` exit 255). Không có kết quả hoàn tất cho 16 bài còn lại. Lưu transcript chưa hoàn tất `qa-logs/musicflow-clear-journeys-v10-targeted-api36.log`, **không ghi 43/43 PASS**.
- **Signed upgrade API33 v9→v10 — PASS sau khi chạy lại:** sau một lượt đầu bị gián đoạn do runner/AVD không ổn định, QA chạy lại theo script hai giai đoạn trên AVD riêng đã cho **2/2 PASS**: tạo playlist/favorites/persisted SAF WAV với APK ký QA v9, cài đè `-r` APK v10, rồi xác minh giữ cả ba và dọn dữ liệu test. Transcript mới `qa-logs/musicflow-clear-journeys-v10-signed-upgrade-api33.log` có **hai `OK (1 test)`** và marker `V10_SIGNED_UPGRADE_V9_TO_V10_ROOM_FAVORITES_SAF=PASS`. Đây là bằng chứng cho AVD API33, không bảo đảm thiết bị thật hay API36.
- **JSON DocumentsUI API33 — NOT VERIFIED:** baseline và lần thử riêng từng thất bại tại bước chọn tệp JSON. Test harness `PlaylistBackupRealPickerUiTest` đã điều chỉnh thời gian chờ/không nhấn lặp, biên dịch thành công nhưng chưa có lần PASS mới sau khi thay đổi. Không khẳng định import JSON lỗi ở sản phẩm chỉ từ lỗi automation này.
- Cần chạy lại từng AVD **tuần tự** trên môi trường ổn định, dùng QA AVD riêng; không thao tác lên thiết bị hoặc dữ liệu thật của người dùng. Cần hoàn tất full regression / UAT trước khi kết luận sẵn sàng bàn giao.

**Không suy diễn từ Compose semantics sang kiểm thử TalkBack thực**. Vẫn cần người thật xác nhận bằng TalkBack trên máy Android, bàn phím Bluetooth, thao tác Owner/Admin thật trên backend được cấp phép, đối chiếu WCAG trên ảnh thực, và thử nhiều SAF provider. Test trên AVD dùng các tài khoản giả, không có giao dịch/cấp quyền thật.

## Bản APK cho báo cáo

`dist/MusicFlow-1.0-Demo-ClearJourneysV10-Android.apk` (nằm cục bộ, không commit lên Git), **31.606.461 bytes**, SHA-256:

`C8136BE71BE7BEEB15CB2C37C9DAFDFE416257E60E4A855B9D7E580FED908097`

Ký bằng khóa Android **debug QA** (không phải production). Package `com.example.musicflow`, versionCode `1`, versionName `1.0`; muốn cài đè phải có cùng package và chữ ký QA. Không gỡ cài đặt nếu cần giữ playlist/favorites/SAF.
