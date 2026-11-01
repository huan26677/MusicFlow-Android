# MusicFlow — Release Candidate QA / Packaging v1, Part 3

**Ngày kiểm thử:** 2026-11-01 (Asia/Ho_Chi_Minh)  
**HEAD nền:** `b5724143699173c97e700169e3c3cb4f9a00527b` — Part 2 đã commit trên `main = origin/main` trước khi bắt đầu.  
**Kết luận:** **PASS trong phạm vi Release Playback Smoke trên Android API 29, 33, 36**, tiếp nối API 30 đã PASS ở Part 2. APK dùng để thử nghiệm được ký bằng Android Debug keystore **chỉ phục vụ QA**. **Chưa được nghiệm thu phát hành production.**

## 1. Ma trận kết quả trên APK release-mode

| Môi trường | Hai WAV MediaStore | Library → Play / Pause / Resume / Next | Phát nền / MediaStyle | Cleanup | Kết quả |
|---|---|---|---|---|---|
| Android 10, API 29 | PASS | PASS | PASS | PASS | **PASS** |
| Android 11, API 30 (Part 2) | PASS | PASS | PASS | PASS | **PASS (mốc trước)** |
| Android 13, API 33 | PASS | PASS | PASS | PASS | **PASS** |
| Android 16, API 36 | PASS | PASS | PASS | PASS | **PASS sau sửa QA parser** |

Ở mỗi API mới, script dùng bản ứng dụng **release-mode không debug**; tạo hai WAV PCM 40 giây trong thư mục QA riêng, công bố chính xác hai row MediaStore, mở ứng dụng và chạm Home → Library → tên bài A. Xác minh MediaSession PLAYING với đúng metadata của A, PAUSED, RESUME, NEXT sang B, giữ PLAYING khi nhấn Home và có thông báo MediaStyle. Cuối mỗi lượt xác minh `QA_CLEANUP=PASS` và `FINAL_RESULT=PASS`.

**Cả ba lượt API 29/33/36 có cùng APK SHA-256** (bản release-mode được zipalign và ký bằng debug keystore QA):

```text
5D28135ABA17EC333E30FB3064AFCDFF3EB317FAA73EF7F25CF7F254D1A85B6C
```

APK QA nằm tại `app/build/rc-qa/release-playback/MusicFlow-1.0-RC2-QA.apk`; đây là build output không đưa lên GitHub.

## 2. Sự cố đã phân loại và xử lý

**API 29 — bảo vệ ứng dụng có sẵn:** AVD Android 10 ban đầu có sẵn `com.example.musicflow`. Script trả `PREEXISTING_MUSICFLOW_DO_NOT_UNINSTALL` **trước mọi bước cài đặt**, nên không làm thay đổi ứng dụng đang có; lưu `qa-logs/release-playback-smoke-api29-initial-blocked.log`. Sau đó tạo AVD QA riêng với cùng system image, chạy trọn bộ và **PASS**.

**API 36 — khác định dạng dumpsys:** Lượt đầu trả `MEDIASESSION_NOT_EXPECTED_STATE_3`, dù `dumpsys media_session` tại thời điểm FAIL hiển thị **`state=PLAYING(3)`, vị trí phát tăng, metadata bài A đúng và `error=null`**. Trên API 29/30/33, cùng trường này có định dạng số `state=3`. Đây là **lỗi biểu thức nhận dạng trạng thái của script QA**, không phải lỗi sản phẩm. Cập nhật `AssertSession()` trong `scripts/run-release-playback-smoke-v1.ps1` chấp nhận cả `PLAYING(3)`/`3` và `PAUSED(2)`/`2`. Chạy lại đầy đủ API 36: tất cả marker **PASS**. Bằng chứng lượt đầu: `qa-logs/release-playback-smoke-api36-initial-failure.log` và trích xuất chỉ các dòng liên quan ở `qa-logs/release-playback-smoke-api36-diagnostics-v1.log`.

**Không sửa mã Kotlin production**, logic Media3, Auth/Cloud, Supabase hoặc kiểm soát quyền nhạc online. File offline người dùng tự lưu/tải vẫn được phát bình thường, không cần xác minh giấy phép như nhạc online trong danh mục do MusicFlow cung cấp.

## 3. Bằng chứng

- `qa-logs/release-playback-smoke-api29-v1.log` — `FINAL_RESULT=PASS`.
- `qa-logs/release-playback-smoke-api33-v1.log` — `FINAL_RESULT=PASS`.
- `qa-logs/release-playback-smoke-api36-v1.log` — `FINAL_RESULT=PASS`.
- `qa-logs/release-playback-smoke-v1.log` — API 30 PASS từ Part 2.
- Script tái kiểm thử: `scripts/run-release-playback-smoke-v1.ps1 -DeviceId <emulator> -ExpectedApi <29|30|33|36>`. Với PowerShell 5.1 phải giữ encoding UTF-8 BOM cho nhãn UI tiếng Việt; mỗi API ghi log riêng.
- Tạo AVD QA riêng `MusicFlow_RC_QA_API29`, `MusicFlow_RC_QA_API33`, `MusicFlow_RC_QA_API36` từ system images đã cài. Sau kiểm thử đã gỡ các AVD QA **do mốc này tạo**; bốn AVD của người dùng `Pixel_5_API_29`, `Pixel_5_API_30`, `Pixel_5_API_33`, `Pixel_7_API_36` giữ nguyên.

## 4. Build Android App Bundle (AAB) và tình trạng phát hành

Đã chạy `:app:bundleRelease`: **BUILD SUCCESSFUL**, tạo `app/build/outputs/bundle/release/app-release.aab`, **10.934.255 bytes**, chứa `base/manifest/AndroidManifest.xml` và `base/dex/classes.dex`. `jarsigner -verify` báo **jar is unsigned**; **AAB chưa ký nên không thể coi là artifact phát hành hoàn chỉnh**.

SHA-256 của AAB chưa ký:

```text
CAACCB7D022B1D5B90273B75726BEA45DDDCA1AF0DE845CC50B3D8FE35E44738
```

Release manifest hiện tại: `applicationId=com.example.musicflow`, `versionCode=1`, `versionName=1.0`, `minSdk=29`, `targetSdk=36`, `optimization.enable=false`. Chưa có signingConfig release chính thức và chưa chốt applicationId/version strategy, đường phân phối và Play App Signing. Không tự tạo/upload khóa ký thật hoặc công bố artifact.

## 5. Giới hạn và bước tiếp theo

- **Smoke ≠ Full Regression:** kiểm thử release-mode 2 WAV/MediaSession/notification trên bốn API, nhưng **chưa chạy toàn bộ 115 Android test case trên APK release** (các bài instrumentation thuộc debug ở Full Regression Gate v2), chưa test codec MP3/FLAC với tag thật và chưa điện thoại vật lý.
- Chưa có kiểm thử Bluetooth/tai nghe/âm lượng/interruptions thật, pin & ANR, hiệu năng thư viện lớn, accessibility TalkBack thực tế, khóa production, AAB đã ký, Play Console, Google Login/email thật, Cloud Sync hai thiết bị hay licensed streaming nội dung thật.
- **Bước tiếp theo: RC QA / Packaging — Part 4 — Release Readiness**: lập checklist thiết bị vật lý + codec thực tế, kiểm tra versioning/applicationId và quy trình ký/phân phối phù hợp, kế hoạch R8/size/performance, xác nhận artifact production khi có quyết định/keystore từ chủ dự án. Tiếp tục giữ kiểm thử email/Google/cloud hai thiết bị ở giai đoạn cuối.
