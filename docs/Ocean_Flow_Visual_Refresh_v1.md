# MusicFlow — Ocean Flow Visual Identity & UI Refresh v1

**Ngày:** 2026-11-07 (Asia/Ho_Chi_Minh). **Mục tiêu:** nâng chất lượng hình ảnh của APK báo cáo, giữ các chức năng đã có và không mở rộng sang production. **Lượt này chỉ thay phần trình bày Android Compose**; không sửa schema, Supabase RLS/RPC, logic Media3/Room, bản quyền nhạc online, đăng nhập thật hoặc thanh toán.

## 1. Nhận diện Ocean Flow

| Thuộc tính | Giá trị/thiết kế |
|---|---|
| Dark Mode mặc định cho cài mới | Nền xanh đêm `#101827`, bề mặt `#162334`, điểm nhấn mint `#5EEAD4`, xanh biển `#0F766E` |
| Light Mode | Trắng sương `#F5FAF9`, nền card trắng/xanh nhạt, chữ đậm, màu xanh biển đồng nhất |
| Người dùng đã tùy chỉnh giao diện | Giữ nguyên lựa chọn đã lưu (`dark`, `light`, `system`); chỉ **mặc định** của lần cài mới chuyển từ system sang dark |
| Thanh trạng thái và thanh điều hướng Android | Đồng bộ màu nền xanh đêm/trắng sương và màu biểu tượng theo Dark/Light, khi người dùng đổi giao diện cập nhật ngay; API35+ còn tùy cơ chế edge-to-edge của hệ điều hành |
| Shape & typography | Card tròn 24dp, hero 28dp, khu vực artwork 32dp, heading đậm hơn, khoảng cách thoáng |
| Logo/họa tiết | Họa tiết ba sóng âm Canvas thuần Compose, không cần tải hình hoặc font bên ngoài |
| Hành vi | Chuyển đổi giao diện không làm thay đổi playlist, queue, Favorites, tài khoản, biên nhận demo hay phân quyền |

## 2. Các màn hình được làm mới

- **Trang chủ:** tiêu đề Ocean Flow gradient với slogan, thẻ tiếp tục nghe có ảnh album, ba thao tác chính mở Thư viện/Tìm kiếm/Playlist, khối hướng dẫn mới, lịch sử và Khám phá thêm sử dụng cùng hình thức card.
- **Player:** nền chuyển sắc nhạt, artwork stage có họa tiết sóng âm, artwork bo 26dp, Play là nút nhấn chính; thao tác trước/sau, queue, shuffle/repeat/seek, tùy chỉnh tốc độ/Equalizer/Lyrics vẫn giữ callback cũ.
- **Mini Player & điều hướng:** Mini Player nổi bo tròn và có viền mint, ảnh album lớn, nút phát/tạm dừng có nền nổi; NavigationBar có chỉ báo chủ đề Ocean Flow, giữ nguyên bốn tab và test tags.
- **Thư viện / Tìm kiếm:** tiêu đề theo bộ nhận diện, danh sách bài có nền màu dịu và bo góc, nút lấy file và quét thư viện không đổi, tìm tiếng Việt không dấu và các nhóm kết quả giữ nguyên.
- **Playlist:** tiêu đề và chi tiết playlist dùng Ocean Flow hero, từng playlist có nền cùng màu và góc tròn, tạo/đổi tên/phát theo thứ tự/Quick Add SAF giữ nguyên.
- **Tài khoản / Demo Center / Premium billing mô phỏng:** tiêu đề thương hiệu, phần giới thiệu hai luồng demo đặt chung thẻ rõ ràng; màn 9 actor và thanh toán demo dùng cùng hero, giữ cảnh báo **MÔ PHỎNG**.
- **Trạng thái chung:** empty/loading/error/permission/info dùng nền card, viền nhất quán, sóng âm trang trí; mô tả và chức năng Retry/Cho phép vẫn hoạt động.

**Đã giới hạn chủ động:** Bản v1 sử dụng nền chuyển sắc nhận diện cố định cho artwork, **chưa trích xuất màu chủ đạo từ bitmap** vì MediaStore có thể bị mất quyền hoặc thiếu artwork. Chưa bổ sung animation liên tục để tránh ảnh hưởng pin và bộ kiểm thử. Các thành phần này có thể làm ở mốc mở rộng sau, không bắt buộc nộp báo cáo.

## 3. Mã nguồn thay đổi

- `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`: palette hai chế độ, contrast, typography, shapes.
- `ui/components/OceanFlowVisuals.kt` (mới): `OceanFlowWaveMark`, `OceanFlowHeader`, `OceanFlowCard`, `OceanFlowSectionTitle`, `OceanFlowArtworkStage`, không mạng/IO.
- `ui/components/MusicFlowStatusPanel.kt`: đồng bộ card/viền/trang trí.
- `feature/home/HomeScreen.kt`, `feature/player/NowPlayingScreen.kt`, `feature/player/MiniPlayer.kt`, `navigation/MusicFlowApp.kt`: home, player, mini player, navigation.
- `feature/library/LibraryScreen.kt`, `feature/library/SongRow.kt`, `feature/search/SearchScreen.kt`, `feature/playlist/PlaylistScreens.kt`: thư viện, tìm, playlist.
- `feature/account/AccountScreen.kt`, `feature/account/DemoCenterScreen.kt`, `feature/account/DemoBillingScreen.kt`: tài khoản và demo.
- `MainActivity.kt`: Dark Mode mặc định cài mới, bảo toàn lựa chọn theme trước đó, cập nhật màu System Bars theo giao diện.
- `app/src/androidTest/java/com/example/musicflow/ui/OceanFlowVisualUiTest.kt`: **6 Android UI cases mới**, gồm Light/Dark, artwork placeholder, Home chữ lớn, Mini Player, row tách action, Search có ô tìm.
- `scripts/run-ocean-flow-v1-qa.ps1`: một lượt full JVM/Build + Web Admin + Android UI/playback regressions + DocumentsUI SAF thật trên emulator QA riêng.

## 4. Tiêu chí nghiệm thu

Phải qua full JVM, Android Compose UI + actor/playlist/playback regressions, Web Admin fail-closed, SAF thực trên emulator API30; tiếp theo đóng gói Release-mode APK ký QA, kiểm tra checksum/chữ ký, cài và cold launch **chính APK đã ký**. Kiểm tra thực tế Dark Mode, Trang chủ, Player/Thư viện và Tài khoản, lưu ảnh báo cáo. Không được gọi API33 smoke là full Android test.

**Giới hạn còn nguyên:** JWT E2E 8 tài khoản thật, thanh toán/đồng bộ cloud thực, streaming bản quyền online, TalkBack thủ công, thiết bị vật lý, Play/production. Tất cả demo actor và thanh toán vẫn **giả lập cục bộ**.

## 5. Thực hành trình diễn

1. Cài APK có tên `OceanFlowUIV1`, mở app lần đầu (Dark Mode), ngắm Trang chủ mới và vào Thư viện.
2. Chọn file âm thanh hợp lệ bằng SAF hoặc MediaStore → mở Player để xem artwork stage/seek/shuffle/repeat, về Trang chủ xem Mini Player nổi.
3. Tìm không dấu, tạo Playlist, thêm bài qua Quick Add và phát theo thứ tự.
4. Vào Tài khoản → đổi sang **Light Mode** và về Trang chủ, xác nhận thiết kế rõ ràng. Có thể quay về Dark Mode.
5. Trình diễn Demo Center 9 actor và Premium billing mô phỏng; giới thiệu rõ dữ liệu không phải thật.

## 6. Kết quả QA thực tế và APK bàn giao

| Cổng nghiệm thu | Kết quả đã xác minh |
|---|---|
| Toàn bộ JVM tests | **190/190 PASS**, 33 suites, 0 lỗi |
| Android instrumentation trên QA emulator API30 | **77/77 PASS**, gồm 6 ca OceanFlowVisualUiTest mới và 71 hồi quy account/role/playlist/player/offline |
| OpenDocument SAF thực trên Android API30 | **1/1 PASS**, file WAV QA đọc/lưu Room với persisted READ grant |
| Web Admin role refresh / revoke | **4/4 PASS** |
| Gradle Debug/AndroidTest/Release | **PASS** sau thay đổi cuối cùng |
| `zipalign`, `apksigner`, `aapt`, SHA-256 | **PASS** cho APK Final ký QA |
| Chính APK Final cài và cold launch API30 | **PASS**, `Status: ok`, `LaunchState: COLD`, PID tồn tại |
| Kiểm tra ảnh thực tế trên APK Final | **PASS** cho Trang chủ, Tài khoản, Demo Center, thanh status/nav cùng tông Ocean Flow |

Sau lượt QA đầu đã đạt **190 JVM + 77 Android + 1 SAF + 4 Web**, ảnh thực tế phát hiện status bar Android vẫn xám. Đã sửa `MainActivity` để đồng bộ system bars Dark/Light, **chạy lại toàn bộ bộ QA với mã sau cùng**, kết quả lần hai vẫn PASS. Bản APK đầu `OceanFlowUIV1-Android.apk` được script bảo vệ khỏi ghi đè bởi checksum khác; **không dùng bản đầu khi nộp**. Bản Final có tên riêng, tạo mới và verify lại.

**APK mới nhất (trên máy phát triển):**

- `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-OceanFlowUIV1-Final-Android.apk`
- **31.344.317 bytes** (~31,3 MB)
- **SHA256:** `2FA0D0A5C79E2DA9768429CE18FCE62BD80CE4F52B38BF3B72BAA576579B9400`
- `com.example.musicflow`, `versionName=1.0`, `versionCode=1`, minSdk29, targetSdk36, `debuggable=false`.
- APK Release-mode được **ký bằng Android debug QA keystore** dùng cho sideload/báo cáo, **không phải ký production hay APK Google Play**.
- Log: `qa-logs/musicflow-ocean-flow-ui-v1-qa.log`, `qa-logs/demo-apk-ocean-flow-ui-v1-final-package.log`.
- Ảnh từ **chính APK Final** trên emulator API30: `qa-logs/screenshots/musicflow-ocean-flow-home-final-dark-api30.png`, `qa-logs/screenshots/musicflow-ocean-flow-account-dark-api30.png`, `qa-logs/screenshots/musicflow-ocean-flow-demo-center-dark-api30.png`.

**Giới hạn minh bạch:** Nhóm Android 77+1 chạy có chọn lọc, không phải toàn bộ instrumentation của repository; chưa có full QA API33/36 cho theme mới, chưa TalkBack thủ công, chưa thử trên thiết bị thật. Bài playback tích hợp Media3/queue đã PASS, nhưng một thử nghiệm thủ công tạo ảnh Player từ tệp WAV sao chép qua ADB bị DocumentsUI từ chối chọn tệp; không có ảnh Player với nhạc thật trong đợt này. Đây không phải lỗi của bài SAF thật, bài SAF tự động đã PASS riêng.

AVD QA riêng `MusicFlow_OceanFlowUIV1_QA_API30` (`emulator-5580`) chỉ dùng trong lượt này, cần xóa sau kiểm thử; giữ AVD và emulator của người dùng nguyên trạng. Không tự commit/push hoặc công khai repo khi chỉ nâng cấp giao diện.
