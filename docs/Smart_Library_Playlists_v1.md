# MusicFlow — Smart Library / Smart Playlist v1

**Ngày:** 2026-11-07, dự án APK sideload phục vụ báo cáo. **Nền:** `main` commit `1d7088b` (Offline Reliability & Backup v1). Không thay đổi quyền Supabase/Owner, Media3 queue persistence, schema Room, thanh toán demo, bản quyền nhạc hoặc Google Play.

## 1. Tính năng

Vào **Trang chủ → Playlist thông minh · Tự cập nhật** hoặc **Thư viện → Thông minh**. MusicFlow xây dựng **5 bộ sưu tập tính toán tại thời điểm xem** dựa trên các bản ghi nhạc offline mà Android hiện cho phép quét từ MediaStore:

| Bộ sưu tập | Quy tắc deterministic |
|---|---|
| Mix của bạn | Bài vừa nghe trước, tiếp đến yêu thích, mới thêm và bài dài; nếu các nguồn trùng nhiều thì lấp chỗ từ thư viện đã quét; bỏ trùng theo URI; tối đa 30 bài |
| Yêu thích | Các bài được đánh dấu tim **và còn hiện trong MediaStore đã quét**, sắp xếp theo tiêu đề |
| Nghe gần đây | Thứ tự từ Room RecentHistory, nhưng chỉ bài còn có trong thư viện quét mới được phát |
| Mới thêm | Theo MediaStore `DATE_ADDED` giảm dần; bỏ timestamp không xác định (`<= 0`) |
| Nghe thật lâu | Các bài dài từ **240 giây** trở lên, sắp xếp theo tên |

Người dùng có thể chọn bộ sưu tập, xem số bài, **Phát N bài** hoặc bấm từng bài. Queue nhận **danh sách và chỉ số từ đúng bộ sưu tập đang hiển thị**, không làm thay đổi playlist thủ công. Các nút tim, thêm vào playlist thủ công, thêm vào queue (khi có quyền) giữ callback thật. Sau khi yêu thích thay đổi hoặc lịch sử phát nhạc cập nhật, danh sách sẽ phản ánh snapshot mới. Ngay cả khi MediaStore chưa có nhạc, tab Thông minh vẫn hiển thị đủ các bộ sưu tập với hướng dẫn trạng thái trống. **Không cần tài khoản hay Internet**.

Các bộ sưu tập **không lưu playlist giả vào Room**, không đăng ký đồng bộ cloud, không gán quyền Premium, không thêm metadata streaming. URI không phải `content://` hoặc không nằm trong thư viện quét không thể được đưa vào Smart playback. Các tệp SAF chọn riêng mà không xuất hiện trong MediaStore **có thể không được liệt kê**; sử dụng playlist thủ công/Quick Add nếu muốn giữ các tệp đó. Kiểm tra MediaStore không bảo đảm tệp còn đọc được ở mọi provider; Playback Auto-Skip và tính năng **Kiểm tra tệp** trong playlist thủ công vẫn xử lý lỗi riêng.

## 2. Mã nguồn

- `core/smart/SmartLibraryRules.kt`: model metadata thuần Kotlin và thuật toán tính danh sách xác định, loại streaming, mất thư viện, bỏ trùng, giới hạn 30; không IO/network.
- `feature/library/SmartLibraryContent.kt`: bộ chọn 5 playlist, trạng thái trống rõ ràng, nút play all và SongRow gốc với yêu thích/Thêm/Queue.
- `feature/library/LibraryScreen.kt`: thêm tab **Thông minh** trong Thư viện, quan sát `RoomRecentHistoryRepository.observeRecent` và `LocalFavoritesStore` hiện có, chuyển nguồn vào Smart.
- `feature/home/HomeScreen.kt`: nút trang chủ `home-smart-library-action`.
- `navigation/MusicFlowApp.kt`: route `SmartLibraryPage` điều hướng từ Home, trả về chính xác, giữ callback player, favorites, queue và quyền hiện có.
- `app/src/test/.../SmartLibraryRulesTest.kt`: 6 ca pure-JVM kiểm tra dữ liệu thiếu, thứ tự, ràng buộc 30, cập nhật yêu thích, timestamp, duration.
- `app/src/androidTest/.../SmartLibraryUiTest.kt`: 5 ca UI Compose (mix đúng queue+index, tim tự cập nhật, empty/165% font, nút Home và tab Smart hiện cả khi MediaStore trống).
- `scripts/run-smart-library-v1-qa.ps1`: kiểm thử một đợt JVM/Debug/AndroidTest, Web Admin, Android UI/Room/playback và DocumentsUI thật trên emulator QA riêng.

## 3. Hướng dẫn trình diễn

1. Cài bản APK mới lên Android 10+, mở Thư viện và cấp quyền quét nhạc, chọn ít nhất một file âm thanh được Android phân loại là nhạc.
2. Vào **Thông minh**, chọn **Mới thêm**, **Nghe thật lâu** hoặc **Mix của bạn**. Bấm Phát tất cả hoặc một bài.
3. Bấm tim trên bài từ bất kỳ màn nhạc, vào **Yêu thích** để thấy danh sách tự cập nhật mà không cần bấm Lưu.
4. Phát một bài, quay lại **Nghe gần đây** để xem lịch sử theo thứ tự và không đưa bài mất khỏi MediaStore vào queue.
5. Về Trang chủ → **Playlist thông minh · Tự cập nhật**; so sánh với playlist thủ công, nơi bài được giữ cố định và có thể sao lưu JSON.

## 4. QA và bàn giao — kết quả thực tế

| Nghiệm thu | Kết quả |
|---|---|
| Toàn bộ JVM unit tests | **199/199 PASS** (6 `SmartLibraryRulesTest` mới; 0 lỗi) |
| Android UI/Room/Media3/Account regressions | **90/90 PASS**, gồm **5 `SmartLibraryUiTest` mới** (Mix queue/index, tim động, font 165%/empty, Home shortcut, Smart tab khi MediaStore trống) |
| Android SAF chọn audio WAV thật, persisted read + Room | **1/1 PASS** |
| Android chọn JSON backup thật bằng DocumentsUI | **1/1 PASS** |
| Web Admin permissions / fail-closed | **4/4 PASS** |
| Debug, AndroidTest, Release build + zipalign/apksigner/aapt/hash | **PASS** |
| Chính APK Release QA ký và cài/cold launch Android API30 | **PASS** (`Status: ok`, `LaunchState: COLD`, PID tồn tại) |
| Trên APK Final: Home → Playlist thông minh → 5 danh sách khi thư viện trống | **PASS**, đã chụp ảnh từ chính APK |

### APK mới nhất nên dùng cho báo cáo

- `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-SmartLibraryV1-Final-Android.apk`
- **31.459.005 bytes**; SHA-256 **`2647528DED4F0483A1FBA918A918C16C7E2D1665A0EFDD4CBA222456BAC56C3C`**.
- Package `com.example.musicflow`, versionName `1.0`, versionCode `1`, minSdk29/targetSdk36, `debuggable=false`, Release-mode **ký bằng debug QA key** (không phải APK production/Google Play).
- APK trước `SmartLibraryV1-Android.apk` là bản trung gian **trước sửa màn hình Smart khi MediaStore rỗng**; nên nộp bản có chữ **Final**.

**Bằng chứng QA:** `qa-logs/musicflow-smart-library-v1-qa.log` (`SMART_LIBRARY_V1_FINAL_RESULT=PASS`), `qa-logs/demo-apk-smart-library-v1-final-package.log`, ảnh trực tiếp `qa-logs/screenshots/musicflow-smart-library-v1-final-empty-api30.png`. Script `scripts/run-smart-library-v1-qa.ps1` chạy trên AVD API30 riêng `MusicFlow_SmartLibraryV1_QA_API30` (`emulator-5580`).

**Sự cố đã khắc phục trong đợt:** bài JVM `eachListIsCappedAtThirtyTracks` ban đầu phát hiện Mix chưa bù đủ bài khi các nguồn trùng nhiều. Đã sửa thuật toán thêm các bài từ thư viện vào các ô trống, rerun **199 JVM PASS**. Smoke thử APK đầu phát hiện Smart tab đang báo trạng thái trống chung khi MediaStore rỗng; đã cập nhật màn Smart hiển thị đủ năm bộ sưu tập và thêm test UI, rerun **90 Android PASS**. Không xóa lịch sử lỗi đầu đợt; APK Final được ký sau cả hai sửa đổi.

**Giới hạn:** Trong smoke QA thủ công, hai tệp WAV vừa chép vào emulator không xuất hiện trong thư viện trả về cho app (MediaStore của emulator trả metadata chưa đầy đủ), nên **chỉ có ảnh APK Final khi không có bài**, không tuyên bố đã chụp được album/bài nhạc thật trên APK Release. Android tests đã kiểm chứng Smart khi có `DeviceSong` thật dạng data model và các test playback Media3/MediaStore hoạt động ở môi trường instrumented riêng. Chưa thử đầy đủ API29/33/36, TalkBack thủ công, thiết bị vật lý hay mọi SAF provider. Smart list không chuyển grant SAF từ thiết bị khác hoặc phát audio online không có bản quyền.
