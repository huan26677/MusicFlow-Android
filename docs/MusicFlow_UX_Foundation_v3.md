# MusicFlow — UX Foundation v3 (APK demo/báo cáo)

**Ngày:** 2026-11-05 · **HEAD nền:** `407939c` (`main = origin/main`, working tree sạch đầu lượt).  
**Kết quả:** hoàn thành chỉnh sửa 3 khu vực UI trọng tâm, **3/3 Android UI test PASS**, APK demo mới build/ký/cài đặt/khởi động thành công.

## Phạm vi đã cập nhật

### 1. Điều hướng nhất quán bằng tiếng Việt

- Thanh điều hướng đổi `Home` → **Trang chủ**, `Search` → **Tìm kiếm**, `Library` → **Thư viện**, `Account` → **Tài khoản**.
- Thay các ký hiệu văn bản rời rạc bằng bộ biểu tượng Material nhất quán: Home, Search, List và Person; nhãn chữ vẫn hiển thị để không phụ thuộc vào biểu tượng.
- Thêm UI tag `nav-home`, `nav-search`, `nav-library`, `nav-account` cho kiểm thử. Bổ sung thư viện `material-icons-core` theo Compose BOM; không thêm dịch vụ online.

### 2. Trang chủ dễ bắt đầu nghe

- Khi đã chọn bài, thẻ **Tiếp tục nghe** xuất hiện trước các nhóm chức năng, đưa người dùng vào màn phát nhạc bằng nút **Mở trình phát nhạc**.
- Nhóm **Bắt đầu nghe** chỉ ưu tiên 3 tác vụ chính: **Mở thư viện thiết bị**, **Tìm kiếm bài hát**, **Danh sách phát của tôi**; giữ nguyên callback cũ.
- Nhóm **Nghe gần đây** vẫn hiện trạng thái loading/empty/error, lịch sử, nút xóa có xác nhận.
- Đưa album yêu thích, bài hát yêu thích, danh mục nhạc online sang thẻ **Khám phá thêm** dưới lịch sử — chức năng không bị xóa và vẫn có thể truy cập.
- Thêm hướng dẫn **Mới dùng MusicFlow?** ba bước trên lần mở ứng dụng đầu, nút **Đã hiểu**. Lưu lựa chọn đã xem vào `musicflow_home_guide_v3` (SharedPreferences) để không hiển thị lại sau khi khởi động lại. Không đòi đăng nhập hoặc quyền truy cập mới.

### 3. Thư viện: ngôn ngữ không gây nhầm lẫn

- Nút `Chọn MP3` đổi thành **Chọn tệp nhạc**, do MusicFlow đã có đường phát WAV/MP3/FLAC.
- Hướng dẫn không có quyền hoặc thư viện trống được sửa để nói đến **tệp âm thanh**, không ám chỉ chỉ hỗ trợ MP3.
- Tương tự, nút ở cuối Now Playing đổi thành **Chọn tệp nhạc từ thiết bị**.
- Giữ SAF/MediaStore, các callback chọn tệp và quyền hiện tại; không sửa engine phát nhạc hay bộ lọc bản quyền streaming.

## Kiểm thử đúng phạm vi

Chỉ kiểm thử chức năng UI đổi, **không chạy full regression**:

| Kiểm tra | Kết quả |
|---|---|
| `:app:assembleDebug :app:assembleDebugAndroidTest` | BUILD SUCCESSFUL |
| `MusicFlowUxFoundationV3Test` — 2 test: nhóm nút/guide và nút chọn tệp | **2/2 PASS** |
| `AccountAuthUiTest` — điều hướng qua Tài khoản, tab Auth, Google disabled, Guest về Home | **1/1 PASS** |
| UI dump trên AVD API30 mới, 4 nhãn điều hướng và nút Mở thư viện | **PASS** |
| Bấm Đã hiểu → hướng dẫn biến mất → force-stop/cold launch không xuất hiện lại | **PASS** |
| Chính APK demo mới: `adb install -r`, cold launch và 5 marker UI | **PASS** |
| Kiểm tra SHA-256 + `apksigner verify` và badging release | **PASS** |

**Emulator QA:** `MusicFlow_UX_V3_QA_API30` / `emulator-5560`, tạo riêng, xác minh MusicFlow chưa cài trước khi install; không sử dụng hay gỡ ứng dụng trên emulator hiện có của người dùng.
Sau QA, AVD này đã được **xóa sạch (ini + dữ liệu)**; `emulator-5556` đang có từ trước được giữ nguyên.

**Không kiểm thử lại:** đăng ký/đăng nhập Supabase E2E, playback full suite, online license, Google OAuth, máy thật. Bằng chứng các mốc trước được bảo lưu và không bị khẳng định lại trong v3.

## APK demo mới

```text
E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-UXFoundationV3-Android.apk
```

- Kích thước **30.754.493 byte**.
- SHA-256: `467B5D4D4664BD35DE5A8FF07E56D5682D316C23A6A5F05AFB76C3E8DB5F3AAF`.
- Android minSdk 29 / targetSdk 36; `com.example.musicflow`, versionCode 1, versionName 1.0.
- APK release-mode không debuggable, **ký bằng Android Debug keystore để dùng cho QA/demo**, không phải chữ ký phát hành production. `dist` được Git ignore; file không tự upload GitHub.
- APK ở các mốc cũ (bao gồm `MusicFlow-1.0-Demo-AuthUX-Android.apk`) không bị ghi đè.
- Có checksum sidecar `.apk.sha256`; build lại: `scripts/build-musicflow-demo-apk-v1.ps1 -OutputName MusicFlow-1.0-Demo-UXFoundationV3-Android.apk` (không ghi đè tên trùng nhưng hash khác).

## Giới hạn, hướng tiếp theo

Ứng dụng hiện được cải thiện về tính dễ sử dụng trên emulator. Chưa có đánh giá định lượng người dùng thực tế, TalkBack thủ công, thiết bị thật hoặc thử toàn bộ font scale. Giữ nguyên phạm vi **dự án báo cáo / APK sideload**.

Nếu có yêu cầu phát triển tiếp, hướng phù hợp là **Player & Library Polish v3**: nhóm tính năng phát nâng cao, bổ sung sắp xếp thư viện, tinh gọn thao tác playlist — không cần thay đổi Supabase Auth hoặc phát hành lên Play.

**Log QA:** `qa-logs/musicflow-ux-foundation-v3.log`; **log đóng gói:** `qa-logs/demo-apk-ux-foundation-v3-package.log`.
