# MusicFlow — Release Candidate QA / Packaging v1 — Part 1

**Ngày thử nghiệm:** 2026-11-01, Asia/Ho_Chi_Minh. **HEAD nền:** `818be079c29312afd5b627267eef5023a2cec11f` (Full Product Regression Gate v2 Part 1 đã commit/push). **Kết quả:** **PASS cho RC QA build, chữ ký thử nghiệm, cài mới và nâng cấp trên Android API 30**; **CHƯA ĐẠT điều kiện phát hành chính thức**.

## Phạm vi và quyết định về chữ ký

- `:app:assembleRelease` **BUILD SUCCESSFUL**, bao gồm `lintVitalRelease`. `app/build/outputs/apk/release/app-release-unsigned.apk` là APK **chưa ký**, không thể dùng để phân phối/cài đặt trực tiếp. `apksigner verify` trả `DOES NOT VERIFY`, đúng theo cấu hình hiện tại.
- Không có `signingConfig` chính thức cho release, không dùng hoặc tạo khóa ký production. Dùng khóa **Android Debug chỉ dành cho QA cục bộ** để ký bản build **release-mode** bằng `zipalign` + `apksigner`, tạo `app/build/rc-qa/MusicFlow-1.0-qa-signed.apk`. Cả keystore và APK QA nằm ngoài Git (APK/keystore được ignore).
- Xác minh APK ký QA: `apksigner verify --verbose --print-certs` **PASS** với APK Signature Scheme **v3**; chứng thư khớp APK debug baseline để kiểm thử nâng cấp trên emulator. Bản QA **không phải artifact có thể công bố chính thức**.
- Manifest từ `aapt dump badging`: application ID `com.example.musicflow`, versionCode `1`, versionName `1.0`, minSdk `29`, targetSdk `36`. Cấu hình release hiện để `optimization.enable = false`, chưa có quyết định tối ưu hóa/minify cuối.

## Kết quả đã kiểm thử

| Phép thử | Kết quả | Bằng chứng |
|---|---|---|
| Gradle release + debug build và lint vital | PASS | `GRADLE_RELEASE_AND_DEBUG_PASS=TRUE` |
| Xác nhận APK release gốc chưa ký | PASS | `UNSIGNED_RELEASE_CONFIRMED=TRUE` |
| zipalign + ký APK bằng debug keystore | PASS | `QA_SIGN_EXIT=0` |
| Xác minh chữ ký QA và đúng cert debug baseline | PASS | `QA_VERIFY_EXIT=0`, `QA_AND_DEBUG_SIGNATURE_MATCH=TRUE` |
| Khởi động APK debug baseline trên emulator API 30 | PASS | `BASELINE_LAUNCH Status: ok` |
| Nâng cấp tại chỗ debug → release-mode QA ký cùng cert | PASS | `UPGRADE_INSTALL Success`, `UPGRADED_RELEASE_LAUNCH Status: ok`, PID tồn tại |
| Tệp marker QA trong internal app storage sống qua nâng cấp | PASS | `UPGRADE_INTERNAL_DATA_MARKER_PERSISTED=PASS` (kiểm tra sau khi đưa debug baseline trở lại, không xóa data) |
| Cài mới và khởi động release-mode QA APK | PASS | `CLEAN_RELEASE_INSTALL Success`, `CLEAN_RELEASE_LAUNCH Status: ok`, PID tồn tại |
| Dọn các bản cài và marker do QA tạo ra | PASS | `EMULATOR_PREEXISTING_APP_STATE_RESTORED=PASS` |

Thiết bị kiểm thử: `emulator-5554`, Pixel_5 Android 11 / API 30. Trước lúc thử cài đặt không có gói `com.example.musicflow` trên emulator; **script chỉ cho phép uninstall khi gói được chính lượt QA cài**. Nếu có ứng dụng cài sẵn, script không chạy bước cài đặt và đề nghị dùng `-PackageOnly`, không xóa dữ liệu người dùng.

**SHA-256 của APK ký QA**:

```text
0A5F7ED7660EAFB4D111C4489F34B631A2D248C6C790FD50A30E59858171FB36
```

Kích thước APK ký QA: **30.230.097 bytes**. Đường dẫn tại workspace Windows: `E:\Du_An\MusicFlow\app\build\rc-qa\MusicFlow-1.0-qa-signed.apk`. Artifact này được tạo cục bộ và không commit/push.

## Cách tái hiện

Từ root `E:\Du_An\MusicFlow`, chạy `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-candidate-packaging-v1.ps1` trên **emulator API 30 dùng riêng cho QA**, khi chưa có MusicFlow cài sẵn. Chỉ kiểm tra đóng gói mà không động đến ứng dụng trên emulator: thêm `-PackageOnly`.

Script cố ý **không dùng khóa production, không upload APK, không đổi package ID, không phát nhạc online và không truy cập dữ liệu riêng tư**. Log tự động: `qa-logs/release-candidate-packaging-v1.log`, mốc `FINAL_RESULT=PASS`. Bằng chứng của đợt full regression v2 trước đó vẫn nằm ở `docs/Full_Product_Regression_Gate_v2_Part_1.md`.

## Giới hạn trước khi phát hành

**Đây chỉ là RC QA Part 1**. Chưa chạy test phát nhạc/queue/MediaSession toàn phần **trên bản release QA** (chỉ xác nhận cài/nâng cấp/khởi động và marker data), chưa test nâng cấp từ các versionCode trước đó, chưa kiểm tra Android 29/33/36 trên release APK, điện thoại thật, performance/battery, Bluetooth, Play App Signing, AAB, tối ưu hóa R8, hoặc release ký bằng keystore sản phẩm.

Các quyết định phát hành còn mở: application ID cuối (hiện vẫn là `com.example.musicflow`), chiến lược versionCode/versionName, khóa ký sản phẩm và nơi phân phối. Chưa bật billing/quảng cáo hoặc đánh dấu bản QA là production. Quy tắc bản quyền không thay đổi: offline người dùng tự lưu/tải phát bình thường; chỉ danh mục online do MusicFlow cung cấp cần quyền streaming.

**Bước tiếp theo:** **RC QA / Packaging — Part 2**: chọn tập kiểm thử offline/playback trên bản QA release-mode, ma trận Android/thiết bị thật, xem xét tối ưu hóa và chuẩn bị cấu hình ký production **sau khi chốt định danh phát hành**. Licensed Streaming có quyền thật, Google Login/email thật và Cloud Sync hai thiết bị giữ ở giai đoạn cuối.
