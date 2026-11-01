# MusicFlow — Release Candidate QA / Packaging v1, Part 4 — Release Readiness

**Ngày:** 2026-11-01 (Asia/Ho_Chi_Minh)  
**HEAD nền:** `1b8491d19e76b72f541e277bb95cc7506a5c9939` (Part 3 đã commit/push; `main = origin/main` lúc bắt đầu)  
**Kết quả:** **PASS — emulator-based MP3/FLAC release smoke + release artifact/JVM audit**. **`PRODUCTION_RELEASE_READY=FALSE`**.

## 1. Quyết định phạm vi

Theo quyết định mới của chủ dự án, **không yêu cầu chạy kiểm thử trên thiết bị vật lý** trong chuỗi nghiệm thu MusicFlow hiện tại. Đánh giá được thực hiện trên emulator, build/JVM và artifact; những điều không đo được trên emulator (Bluetooth/tai nghe thật, độ ổn định trên phần cứng, chất lượng DAC/âm thanh, pin ngoài đời thực) **ghi là chưa kiểm chứng** nhưng **không đưa thành cổng bắt buộc chặn phát triển**. Không được đánh dấu chúng là PASS khi chưa thử.

Quy tắc nội dung giữ nguyên: nhạc offline do người dùng có sẵn/tự tải được phát bình thường, không áp cơ chế duyệt quyền nhạc online; chỉ nhạc online do MusicFlow cung cấp cần quyền streaming hợp lệ. Auth email thật, Google Login E2E và Cloud Sync hai thiết bị tiếp tục ở giai đoạn cuối theo phạm vi đã chốt.

## 2. Release-mode MP3/FLAC smoke — PASS

Tạo hai tệp âm thanh **nguyên bản, tổng hợp từ sóng sine, không sử dụng bản nhạc thương mại**, mỗi tệp 40 giây: MP3 bằng `libmp3lame` (440 Hz), FLAC lossless (554 Hz). Mỗi file chứa thẻ `title` khác nhau, `artist=MusicFlow QA Artist`, `album=MusicFlow QA Album`, `genre=Electronic`. Generator: `scripts/generate-release-codec-fixtures.py`; file đầu ra trong `app/build/rc-qa/release-playback`, thuộc build output bị Git ignore.

Chạy kiểm thử trên **emulator Android 11/API 30**, dùng **APK release-mode không debug**, căn chỉnh và ký **chỉ bằng Android Debug keystore cục bộ phục vụ QA**. Script tái sử dụng `scripts/run-release-playback-smoke-v1.ps1` với tùy chọn `-FixtureMode codec`, không thay thế bài WAV mặc định và không ghi đè log các API cũ.

| Tiêu chí | Kết quả |
|---|---|
| `assembleRelease` và kiểm tra chữ ký APK QA | PASS |
| Tạo MP3 và FLAC 40 giây bằng FFmpeg | PASS |
| MediaStore nhận diện hai định dạng, đọc **duration thật** và thẻ title/artist/album | PASS |
| Chọn MP3 từ màn Library, MediaSession PLAYING và metadata chính xác | PASS |
| Pause và Resume qua Android media key | PASS |
| Next từ MP3 sang FLAC, FLAC thật phát và đúng metadata | PASS |
| Phát nền sau Home + thông báo MediaStyle | PASS |
| Chỉ dọn ứng dụng, hai tệp và MediaStore row do QA tạo; không còn file QA | PASS |

**Log:** `qa-logs/release-codec-smoke-api30-v1.log`, `FINAL_RESULT=PASS`.  
**APK ký QA SHA-256:** `ACE9AD765F75C7939C513210E2BBC1555F95E6504636303B423FB70598C2E750`.  
Đây là **smoke codec trên API 30**, không suy diễn các codec đã được kiểm tra trên API 29/33/36. Bài WAV release-mode trước đó PASS các API 29/30/33/36 ở Part 2–3.

**Kiểm tra tương thích ngược WAV sau khi mở rộng script:** chạy lại cùng luồng release-mode với `-FixtureMode wav`, kết quả **FINAL_RESULT=PASS** bao gồm MediaSession, Background Playback, MediaStyle và cleanup; log mới `qa-logs/release-playback-smoke-wav-backcompat-v1.log`. Log WAV API 30 cũ của Part 2 đã được khôi phục nguyên trạng, không bị ghi đè bởi lượt này.

### Tái kiểm thử

Từ thư mục gốc của dự án:

```powershell
python -m pip install --target .\app\build\rc-qa\python-deps imageio-ffmpeg
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-release-playback-smoke-v1.ps1 -DeviceId emulator-5554 -ExpectedApi 30 -FixtureMode codec
```

Nếu FFmpeg đã nằm trong PATH, bước cài `imageio-ffmpeg` không cần thiết. Cần một emulator API 30 **không có MusicFlow đang cài**. Script cố ý dừng nếu phát hiện ứng dụng có sẵn, không gỡ hoặc ghi đè dữ liệu người dùng. Không có phụ thuộc Python hoặc FFmpeg được thêm vào APK sản phẩm.

## 3. Release artifact + JVM readiness audit — PASS trong phạm vi QA

Script `scripts/run-release-readiness-audit-v1.ps1` chạy `:app:assembleRelease`, `:app:bundleRelease`, `:app:testDebugUnitTest`, đọc XML JUnit, xác minh badging bằng `aapt` và kiểm tra thành phần file AAB. Kết quả:

| Tiêu chí | Kết quả |
|---|---|
| APK release build + AAB build | **PASS** |
| JVM unit tests | **105 PASS / 0 FAIL / 0 SKIP** |
| Package ID, versionCode, versionName, minSdk/targetSdk | **PASS** so với cấu hình hiện tại |
| APK release không bật `debuggable` | **PASS** |
| AAB có manifest và classes.dex | **PASS** |
| APK/AAB SHA-256, byte-size | **Đã ghi log** |

**Log:** `qa-logs/release-readiness-v1.log`, `FINAL_RESULT=PASS` nghĩa là *audit thành công*, **không có nghĩa artifact production đã đủ điều kiện phát hành**.

| Mục phát hành chính thức còn mở | Trạng thái |
|---|---|
| AAB release được ký production | **CHƯA** — AAB hiện chưa ký |
| Signing config/khóa production/Play App Signing | **CHƯA** |
| Định danh gói cuối cùng | **CHƯA CHỐT** — hiện `com.example.musicflow` |
| Quy tắc nâng `versionCode`/`versionName` | **CHƯA CHỐT** — hiện 1/1.0 |
| R8 tối ưu hóa bản release | **CHƯA BẬT** — `optimization.enable=false` |
| Chọn kênh phân phối (APK riêng / Play) | **CHƯA CHỐT** |

**AAB chưa ký:** `app/build/outputs/bundle/release/app-release.aab`, **10.934.255 byte**, SHA-256 **`3E3DCE6793CF08A6653C7C1EF9C8E22B2E2265CBA797F37F7E947034D832E88C`**.

**APK release chưa ký:** `app/build/outputs/apk/release/app-release-unsigned.apk`, **30.201.442 byte**, SHA-256 **`8D64D7A1526A7F3F9E9AB987DFBF317625300E669F36E0B1A0F16BA476E2FC7F`**.

Hai file đầu ra nằm ngoài Git và không được upload hoặc tự động ký bằng khóa thật. Không sửa cấu hình build production hay Supabase trong mốc Part 4.

## 4. Giới hạn và mốc kế tiếp

- Không yêu cầu thiết bị thật, theo quyết định của chủ dự án. Không được coi những bài test phụ thuộc phần cứng là PASS.
- Bài codec mới chạy **release-mode trên emulator API 30**, chưa kiểm thử full 115 Android instrumentation case trên APK release (bộ 115 case thuộc debug instrumentation).
- Chưa kiểm thử tăng tải thư viện lớn, ANR/memory/performance, R8 bật thật, AAB ký thật, cài lên Play, license streaming thật, Google/email thật và Cloud Sync E2E hai thiết bị.
- **Bước tiếp theo (Part 5):** chuẩn bị cổng tối ưu hóa R8 và benchmark kích thước/khởi chạy trên emulator, rà soát upgrade/versioning và checklist phát hành ký thật **sau khi chủ dự án chốt định danh gói cùng kênh phân phối**; không thêm điều kiện máy thật.
