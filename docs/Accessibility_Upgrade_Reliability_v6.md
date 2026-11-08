# MusicFlow — Accessibility & Upgrade Reliability v6

**Ngày:** 2026-11-08. **Phạm vi:** Android APK demo cho báo cáo; không thay đổi quyền backend, thanh toán thật hoặc nguồn nhạc online.

## Thay đổi mã nguồn

- Ocean Flow: tiêu đề màn hình, mục và bảng trạng thái có semantic *heading* cho TalkBack; chữ 200% không cắt tiêu đề/phụ đề trong header; thu nhỏ phần ảnh bìa Player ở font lớn.
- SongRow: nút phát công bố tên bài + nghệ sĩ; trạng thái yêu thích có `stateDescription`; menu của màn hẹp tiếp tục tách độc lập với nút phát.
- Player: tên bài/lời có heading; nút phát hoặc tạm dừng công bố tên bài; Slider có mô tả vị trí phát và tổng thời lượng; nút điều khiển có testTag, hành động tùy chỉnh có mô tả đóng/mở.
- Account: hai lựa chọn đăng nhập Supabase/Demo offline và Đăng nhập/Tạo tài khoản chuyển thành nút lựa chọn toàn hàng trên màn hẹp/chữ lớn 175–200%, vẫn giữ trạng thái selected và testTag cũ.
- Home/Search/Library sử dụng các thành phần chung nên nhận heading semantics; không thay đổi callback account, playback, queue, playlist hoặc cloud.
- Bổ sung **6 bài UI Android v6** kiểm tra Heading, dark/light, trạng thái trống, phát/yêu thích ở 200%, Player 200%, tài khoản 200% và Home 200%.
- Bổ sung **kiểm thử APK nâng cấp có dữ liệu thật hai giai đoạn**: cài APK ký QA v5, tạo WAV qua DocumentsUI, lưu SAF persisted READ, tạo Room playlist và favorite, cài đè APK ký QA v6 bằng `adb install -r`, kiểm tra tất cả dữ liệu/quyền/file WAV đọc được rồi chỉ xóa fixture đã tạo bởi test.

## Kiểm thử

**Android 11 / API30 — ĐẠT trong phạm vi demo, sau khi hoàn thiện v6:**

- `gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease` **BUILD PASS**, **199/199 JVM**, không lỗi.
- **98/98** Android regression + **7/7** Library UX v4 + **6/6** Accessibility/Offline Search v5 + **4/4** Search UX v3 + **2/2** Advanced Search runtime + **6/6** Accessibility UX v6 = **123 Android UI/regression PASS**.
- **2/2** MediaStore WAV thực, **1/1** MainActivity DocumentsUI WAV persisted READ thực, **1/1** SAF Quick Add, **1/1** JSON picker, **4/4** Web Admin fail-closed PASS. Log: `qa-logs/musicflow-accessibility-upgrade-v6-final-regression-api30.log`.
- **Nâng cấp hai signed APK thật (2/2 bước):** bản v5 ký QA cài lên AVD; DocumentsUI tạo một WAV có persisted READ, app ghi shortcut LocalPickedMusicStore, Room playlist/track và favorite; `adb install -r` bản v6 **không clear/uninstall**; tiến trình instrumentation mới sau nâng cấp đọc lại đúng Room/SharedPreferences/SAF, mở WAV xác nhận header RIFF và chỉ dọn fixture QA. Kết quả **PASS**; log `qa-logs/musicflow-accessibility-upgrade-v6-signed-upgrade-api30.log`.
- Chính APK v6 ký QA đã chạy cold start API30: `Status: ok`, `LaunchState: COLD`. Log `qa-logs/musicflow-accessibility-upgrade-v6-signed-smoke-api30.log`.

**APK đề xuất nộp báo cáo:** `dist/MusicFlow-1.0-Demo-AccessibilityUpgradeV6-Android.apk` (**31.573.693 bytes**, SHA-256 `5BECC22142FCB5457A8E4D898AA536ED4805064CCD63B0816F8173BBCF3E9F0D`). Zipalign, chữ ký QA, aapt và hash đều kiểm chứng. Package `com.example.musicflow`, versionCode 1, versionName 1.0, min SDK 29, target SDK 36; không debuggable nhưng **ký QA/debug**, không phải production.

**Android 13 / API33 — ĐẠT kiểm thử mục tiêu:** **26/26 Android tests PASS**, gồm 6 v5, 4 Search UX v3, 7 Library v4, 2 MediaStore WAV thật, 1 MainActivity SAF DocumentsUI persisted READ thật và 6 accessibility UI v6. **Chính APK signed v6** cài và cold-launch API33 PASS. Log `qa-logs/musicflow-accessibility-upgrade-v6-targeted-api33.log`, `qa-logs/musicflow-accessibility-upgrade-v6-signed-smoke-api33.log`. Đây **không phải** 98 bài hồi quy nền tảng ở API33.

**Android 16 / API36 — ĐẠT kiểm thử mục tiêu:** **26/26 Android tests PASS**, gồm 6 v5, 4 Search UX v3, 7 Library v4, 2 MediaStore WAV thật, 1 MainActivity DocumentsUI SAF persisted READ thật và 6 UX v6 ở chữ 200%. **Chính APK ký QA v6 cài và cold-launch API36 PASS** (`Status: ok`, `LaunchState: COLD`); AVD QA đã xóa. Log `qa-logs/musicflow-accessibility-upgrade-v6-targeted-api36.log`, `qa-logs/musicflow-accessibility-upgrade-v6-signed-smoke-api36.log`. Đây không phải toàn bộ 98 bài hồi quy trên API36.

## Giới hạn

- APK ký QA/demo cho báo cáo, không production/Google Play; `dist/` bị Git ignore, chép APK riêng khi nộp.
- Không kiểm thử dữ liệu của người dùng; script nâng cấp **chỉ** chạy trên AVD QA tách biệt, không dùng điện thoại/AVD cá nhân.
- Test nâng cấp xác nhận playlist/favorites/SAF fixture thực, không đủ bảo đảm mọi phiên bản Android/provider, máy vật lý, hoặc tài khoản cloud.
- Tương phản dark/light chỉ được xác nhận bằng màu Material Theme và kiểm thử thành phần, **chưa** có kiểm toán WCAG trên mọi thành phần và chưa kiểm thử TalkBack/keyboard thực trên máy vật lý.
