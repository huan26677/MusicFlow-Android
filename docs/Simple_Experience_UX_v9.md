# MusicFlow v9 — Simple Experience P1 (trải nghiệm theo từng vai trò)

**Ngày:** 2026-11-08. **Phạm vi:** đồ án Android offline. Đây là đợt đơn giản hóa UX, không thay đổi API Supabase, RLS, giao dịch thật, bảng Room, SAF persisted URI, quyền cấp vai trò hay bản quyền nghe nhạc.

## Vấn đề kiểm tra từ mã nguồn v8

- Trang chủ có bốn hành động nghe nhạc ngang hàng; người mới dễ phân tán giữa Thư viện, Search, Playlist và Smart Mix.
- Màn Tài khoản đặt khu vực trình diễn 9 vai trò và thanh toán giả lập **trước** nút nghe nhạc mà không cần đăng nhập. Người dùng Free dễ nhầm demo với tính năng của tài khoản.
- Nhãn `Supabase` trong luồng đăng nhập là tên hạ tầng kỹ thuật, không nói người dùng đang làm gì.
- Hộp công cụ vai trò hiện dùng hàng tab cuộn ngang cố định, kể cả khi chữ phóng lớn; thiếu nhãn nhiệm vụ trực quan cho Owner/Admin/Creator.

Đây là **đánh giá theo mã nguồn**, không phải kết luận nghiên cứu người dùng thật.

## Phạm vi v9 đã lập trình

1. **HOME_NAV:** nút chính `Chọn nhạc để nghe` mở Library hiện có (chọn tệp SAF không cần quyền hoặc quét cả máy khi được cấp quyền). Nút thứ hai `Tìm bài hát đã có`. Playlist và Smart Mix nằm dưới mục `Playlist và tùy chọn khác`, mở rộng theo yêu cầu và dùng `rememberSaveable`. Hướng dẫn lần đầu trỏ đúng đường đi mới.
2. **AUTH:** đặt `Nghe nhạc ngay, không cần đăng nhập` ngay sau lời chào, nổi bật bằng Button 48dp. Luồng đăng nhập có nhãn `Tài khoản trực tuyến` và `Tài khoản thử trên máy`, giữ nguyên callback và tag QA. Kịch bản 9 vai trò và thanh toán Premium giả lập được đặt **phía sau** thông tin tài khoản và mặc định thu gọn. Nút bật `account-demo-tools-toggle`, giữ hai đường dẫn demo độc lập, luôn kèm lời nhắc không cấp quyền hoặc thu tiền thật.
3. **ROLES_ADMIN:** khu vực chọn công việc Owner/Admin/Moderator/Creator/Developer có nhãn cụ thể, trạng thái đang xem rõ ràng; màn nhỏ hoặc font 150% trở lên dùng danh sách cuộn dọc giới hạn chiều cao thay vì bắt cuộn ngang, cho phép chuyển nhanh giữa các khu vực. Không thay bất kỳ RPC, điều kiện `MusicFlowAccess`, bước xác nhận cấp quyền, hay quyền Supabase nào.

## Modular Testing & Impact-driven Regression

- **Trực tiếp:** HOME_NAV, AUTH, ROLES_ADMIN.
- **Lan truyền:** BILLING_DEMO, DEMO_CENTER, CREATOR, CLOUD_SYNC, ONLINE_RIGHTS, PLAYBACK, PLAYLIST, HISTORY, LYRICS, RELEASE_BACKUP, SMART theo bản đồ `qa/impact_modules.json`.
- Kế hoạch source-only trước sửa là **134 test cases** (direct all + downstream smoke), nhưng diff còn sửa test Kotlin ở nhiều module để cập nhật hành trình UI: planner `--git-diff` yêu cầu **full regression**. Chạy QA trên AVD mới, lưu log riêng; mọi test **chưa chạy** không được ghi PASS.
- Các test đã cập nhật theo thao tác UI mới: `MusicFlowUxFoundationV3Test`, `OceanFlowVisualUiTest`, `SmartLibraryUiTest`, `DemoCenterV1UiTest`, `DemoBillingUiTest`, `AccountHomeV2UiTest` và `AccountTaskUxV3UiTest`. Thêm hai test chứng minh tính ẩn/hiện tự chọn ở Trang chủ và Tài khoản.
- Nếu hồi quy UI fail trên Android mới, phải phân biệt lỗi thật với thao tác test che khuất, không chỉ sửa test để che lỗi chức năng.

### Kết quả QA trên AVD Android 11 / API30

- **Focused UX:** 31/31 Android UI test PASS cho AccountHome, AccountTask, HomeFoundation, DemoCenter và DemoBilling (gồm hai test mới về mở/đóng tính năng nâng cao). Log `qa-logs/musicflow-simple-experience-v9-focused-api30.log`.
- **Baseline regression:** `scripts/run-library-accessibility-v7-qa.ps1 -FullRegression` PASS: **129/129 Android UI/hồi quy** (98 regression nền tảng + 7 Library v4 + 6 Search v5 + 4 Search UX + 2 Search runtime + 6 accessibility v6 + 6 v7), **199 JVM**, các luồng MediaStore/SAF WAV thật, JSON picker và **4 Web Admin** theo transcript. Log `qa-logs/musicflow-simple-experience-v9-baseline-api30.log`.
- **V8 contract:** 8/8 Android targeted PASS (3 bàn phím/D-pad + 5 Indexed Search/refresh quyền). Log cục bộ `qa-logs/musicflow-simple-experience-v9-v8-safety-api30.log`.
- Trùng lặp giữa các lần chạy tập trung và hồi quy không được cộng thành số test **duy nhất**. Planner yêu cầu full regression cho toàn bộ diff, nhưng **459 phương thức Kotlin trong registry không được thực thi đầy đủ trên mọi API**; coverage gap vẫn được ghi rõ.
- Chưa QA Android 13/16 cho mã v9, TalkBack/Bluetooth keyboard, user study thật hoặc benchmark nhạc thật; không suy diễn từ v8.
- **Cài đè ký QA v8→v9 trên API30: PASS**. Hai bài seed/verify chạy ở hai lần cài APK `adb install -r`, xác minh Room playlist, Favorites và quyền đọc WAV qua SAF persisted READ còn nguyên, chỉ dọn fixture QA của mình. Log `qa-logs/musicflow-simple-experience-v9-signed-upgrade-api30.log`. Không đảm bảo nâng cấp khi khác chữ ký hoặc nhiều SAF provider/thiết bị thật.
- **APK ký v9 cold launch API30 PASS**: cài đè giữ dữ liệu, force-stop rồi `am start -W` trả về `Status: ok`, `LaunchState: COLD`, tiến trình MusicFlow đang chạy. Log smoke tại máy QA `qa-logs/musicflow-simple-experience-v9-signed-smoke-api30.log`; AVD `MusicFlow_SimpleExperienceV9_QA_API30` đã được xóa sau kiểm thử, không chạm AVD/máy người dùng.

## Nghiệm thu người thật còn mở

Thử 5 hành trình: (a) khách tìm và phát file WAV/MP3 không đăng nhập, (b) Free tạo playlist và thêm bài, (c) Premium thấy rõ demo và quyền Premium thật, (d) Creator biết đơn đang chờ, (e) Owner tìm user, xem quyền, cấp/thu hồi đúng xác nhận, sau đó Admin chỉ thấy công việc được giao. Ghi thời gian thao tác, số lần bấm, lỗi nhầm, cảm nhận chữ 200%, TalkBack, D-pad/Bluetooth và thiết bị vật lý. **Không suy diễn UX score hoặc tỷ lệ hoàn thành từ Compose fixture.**

## APK demo QA v9

`E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-SimpleExperienceV9-Android.apk`

- Kích thước: **31.606.461 bytes**.
- SHA256: `E6AE4F5EAA61B5D0262F63195A600E2757E5C551256D0C1262A1AA4CB5A066C9`.
- Ký bằng debug/QA key sẵn có, `zipalign`, `apksigner --verify` và `aapt` đều PASS. Package `com.example.musicflow`, versionCode 1, minSdk29, targetSdk36. **Không phải chữ ký bản production/Play Store**.
- Bản APK ở thư mục `dist/` được giữ cục bộ, Git-ignore; không có link APK trực tiếp trên GitHub.
