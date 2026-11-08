# MusicFlow — Final User Experience Polish v13

**Ngày:** 2026-11-08. **Loại thay đổi:** chỉ tinh chỉnh UX phục vụ báo cáo đồ án, **không thêm use case**, không đổi backend, entitlement, Android permission, Room, persisted SAF, hợp đồng playback hay nguồn nhạc.

## 1. Vấn đề xác nhận từ mã nguồn v12 và cải thiện

| Khu vực | Vấn đề trước sửa | Chỉnh sửa v13 |
|---|---|---|
| PLAYBACK | Trước/Tạm dừng/Tiếp nằm sau ảnh album, yêu thích, hàng đợi và thanh tua; phải cuộn dài trên màn hình nhỏ | Đưa nút điều khiển phát ngay dưới tên bài/nghệ sĩ, trước thao tác phụ. Giảm khoảng trống dọc phía trên. Giữ callback và test tag. |
| LIBRARY | Sáu chip danh mục cuộn ngang; không có dấu hiệu còn danh mục khuất | Thêm dấu `›` ở cuối hàng chip trên màn hẹp, có nhãn hỗ trợ đọc “Vuốt ngang để xem thêm danh mục nhạc”; không chiếm thêm một dòng dọc, không thay cách lọc hoặc quyền. |
| PLAYLIST | Khi người dùng chưa có danh sách phát, hộp chọn playlist chỉ nói quay lại Home, không chỉ vị trí nút bị thu gọn | Hướng dẫn tuyến đường Trang chủ → Playlist và tùy chọn khác → Danh sách phát của tôi → Tạo playlist mới. Không tự tạo playlist, không thao tác Room. |
| AUTH/UI | Lựa chọn sáng/tối/hệ thống nằm sau công cụ trình diễn học tập | Đưa thiết lập Giao diện lên trước khu vực trình diễn giả lập; giữ theme preference, test tag và lựa chọn. |
| ROLES_ADMIN | Trên màn hình nhỏ, danh sách workspace và nội dung từng có vùng cuộn riêng nhưng nhãn chưa phân biệt | Đánh số hai khu vực, làm rõ đang xem workspace nào và có hai vùng cuộn. Không thay RPC, điều kiện cấp quyền, xác nhận hay thao tác dữ liệu. |

**Giới hạn bằng chứng:** các vấn đề là nhận định trực tiếp từ mã nguồn và UI layout, không phải thống kê nghiên cứu người dùng thật. Ảnh UI minh họa nếu có không thay thế bản Android đã cài.

## 2. Modular Testing & Impact-driven Regression

- **Trực tiếp:** AUTH, LIBRARY, PLAYBACK, PLAYLIST, ROLES_ADMIN.
- **Tiêu chí:** `scripts/impact_regression.py plan --git-diff` → `full_required` vì thay đổi ≥4 module; registry có **462** phương thức Kotlin @Test sau khi thêm một case thư viện lớn chữ (199 JVM là một tập riêng). Việc chạy một số bài targeted không tương đương full 462.
- **Android UI regression:** `MusicFlowUiPolishTest` có kiểm tra thứ tự bố cục điều khiển phát trước thanh tua, chủ đề trước công cụ Demo, chỉ dẫn danh mục cuộn ngang ở font 180%, ba nút playback hoạt động; thêm `ThemeBatchTest` khởi động lại để xác minh theme và `AccountAuthUiTest` không ép người dùng đăng nhập.
- **Nguyên tắc kiểm thử:** xét phần tử trong viewport khi so sánh `boundsInRoot`, vì Compose semantics của phần tử ngoài viewport có thể bị clip thành hình rỗng. Bài test **không được** xóa assertion chỉ để vượt qua lỗi.
- **Bắt buộc phân biệt:** PASS/FAIL theo transcript thật; TalkBack vật lý, UAT người dùng, signed APK upgrade API33/36 và full 462 là các cổng riêng, chưa thể kết luận từ những bài UI trên emulator.

## 3. Trạng thái nghiệm thu và điểm dừng phạm vi

Mục tiêu là bố cục rõ ràng hơn trước khi bàn giao đồ án, không mở rộng phần mềm. Các lựa chọn Owner/Admin thật chỉ hoạt động với quyền RPC được máy chủ xác minh; tài khoản Demo/Premium giả lập vẫn chỉ để trình diễn. Không coi UI fixture là xác nhận vai trò trên production.

Trước nộp báo cáo cần cài và nghiệm thu APK ký Demo v13 trên thiết bị dự kiến trình diễn, chạy nghiệm thu người thật theo `docs/UX_Journey_Acceptance_v10.md`, và ghi mọi test chưa chạy là **NOT RUN**.

## 4. Kết quả QA thực tế — Android 16.1 / API36

- **Bản cuối sau chỉnh dấu cuộn ngang, AVD QA vừa làm sạch:** **14/14 Android selected cases PASS**, chạy theo năm lượt instrumentation tách biệt để không trộn trạng thái cửa sổ: 6/6 `MusicFlowUiPolishTest` + Theme + Auth; 1/1 Album 600 bài/200% chữ; 2/2 Library search + queue; 4/4 UiAutomator keyboard/D-pad; 1/1 Lyrics DocumentsUI `CreateDocument` thực. Log lần lượt `qa-logs/musicflow-v13-focused-api36.log`, `musicflow-v13-album-api36.log`, `musicflow-v13-search-api36.log`, `musicflow-v13-keyboard-api36.log`, `musicflow-v13-lyrics-api36.log`. 14 case phân biệt, không cộng trùng.
- **Lượt tích hợp mở rộng trên bản v13 ban đầu, trước khi đổi chỉ dẫn thành mũi tên:** **63/67 PASS, 4 FAIL**: Lyrics DocumentsUI Save không tương tác được (1), Album virtualized 600 bài bị cuộn sai khi có thêm dòng chỉ dẫn (1), hai bài tìm kiếm dùng Espresso `closeSoftKeyboard` không lấy được window focus (2). Kết quả này **không bị xóa** bởi các lượt sau; bằng chứng `qa-logs/musicflow-v13-impact67-api36.log`. Khi thử lại trên AVD vẫn chứa trạng thái DocumentsUI bị treo, các test riêng tiếp tục thất bại, kể cả keyboard; sau khi làm sạch AVD, các trường hợp tương ứng chạy runner riêng đều PASS trên bản sửa cuối. Không suy diễn mọi thất bại ban đầu đều cùng nguyên nhân.
- **Build và kiểm thử khác:** `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest` PASS; 199/199 JVM tests từ các XML; 18/18 impact-engine Python; **15/15 Brain v2 Python** sau bổ sung kiểm thử ngân sách 24 KB; 17/17 licensed-upload Python; 4/4 Web Admin role-refresh JS. Registry `validate` PASS: 19 module, 105 lớp, 462 phương thức @Test, 533 module/case links.
- **Không được tuyên bố:** full 462 regressions (NOT RUN), Android API33 và máy thật cho bản v13 (NOT RUN), TalkBack thực, UAT sáu vai trò, multi-SAF-provider, nâng cấp APK có ký **kèm kiểm chứng dữ liệu Room/SAF** (NOT RUN). Các bài instrumented 14/14 chạy trên bản Debug; APK ký Demo có kiểm tra cài và mở riêng, không suy diễn thành 14/14 trên Release.

**Bài học kỹ thuật:** dấu hiệu cuộn ngang cần được thiết kế không làm hẹp vùng danh sách nội dung ở font 200%; kiểm thử cuộn `LazyColumn` 600 bài là regression có giá trị. Lượt chạy nhiều Activity/Document picker trên API36 có thể để lại trạng thái focus làm các test kế tiếp sai; lưu cả FAIL, sau đó kiểm chứng trên AVD QA sạch với runner tách biệt và không nới lỏng assertion.

## 5. APK Demo ký QA v13 — đóng gói sau khi tinh chỉnh UX

- **Tệp:** `dist/MusicFlow-1.0-Demo-FinalUXV13-Android.apk` — **31.622.845 bytes**.
- **SHA-256:** `D74CD7F4FEEE8819FBE6528554AC8EBC60885EBE7182FBFC7EBC77F1762F9D3F` (có tệp `.sha256` kèm trong `dist/`).
- **Đóng gói:** `scripts/build-musicflow-demo-apk-v1.ps1 -OutputName MusicFlow-1.0-Demo-FinalUXV13-Android.apk` → `GRADLE_RELEASE_BUILD=PASS`, `ZIPALIGN_EXIT=0`, `DEMO_SIGNATURE_VERIFIED=PASS`, `DIST_APK_HASH_VERIFIED=PASS`, `FINAL_RESULT=PASS` trong `qa-logs/demo-apk-final-ux-v13-package.log` (lưu cục bộ, không đưa keystore lên Git).
- **Android 16.1 AVD QA chuyên dụng:** `adb install -r` APK ký QA lên package Debug cùng chữ ký **PASS**, `am start -W -n com.example.musicflow/.MainActivity` **COLD / Status: ok**, tiến trình sống, không có `FATAL EXCEPTION` trong 400 dòng error log gần nhất. Bằng chứng cục bộ `qa-logs/musicflow-v13-signed-apk-install-smoke.local.log`. Đây chỉ là cài đè/smoke, **chưa phải** kiểm thử toàn vẹn dữ liệu sau nâng cấp có fixture Room/SAF.
- **Bằng chứng Git đã lọc thông tin:** `qa-logs/musicflow-v13-signed-package-api36.log` và `qa-logs/musicflow-v13-signed-smoke-api36.log` giữ các marker từ log gốc, không chứa đường dẫn workstation, keystore hay tài khoản.
- APK **chỉ dùng keystore Android Debug cho Demo lớp học** (Release không bật debuggable), không có signing key production, không phải Google Play release, thanh toán/quyền giả lập vẫn không phải production. Artifact cục bộ bị `.gitignore` loại khỏi Git; không công khai keystore hoặc bản log nhạy cảm.
