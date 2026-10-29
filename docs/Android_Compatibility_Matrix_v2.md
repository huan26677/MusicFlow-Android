# MusicFlow — Android Compatibility Matrix v2

**Ngày kiểm thử:** 2026-10-29. **Git HEAD nền:** `d199bbb`. **Trạng thái:** representative offline/SAF/process-restart matrix trên 4 Android emulators **PASS**. Không đồng nghĩa Full Product 1.0 hoặc toàn bộ Android instrumentation suite đã nghiệm thu.

## Phạm vi

Nhạc người dùng tự lưu/tải về thiết bị **được phát bình thường không cần đăng nhập, Supabase, Internet hay xác minh bản quyền online**. Chỉ streaming từ online catalog do MusicFlow cung cấp mới qua kiểm tra quyền phát. Các bài nhạc thử của matrix là WAV tự tạo trên emulator, không có bài nhạc thật/được tải trái phép.

Tái sử dụng `OfflineAcceptanceFlowTest`, `TrackMediaItemsTest`, `PlaybackQueuePersistenceTest`, `PlaylistPlaybackOrderTest`, `PlaybackProcessRestartTest` và `SafPermissionRestartTest`. Script mới `scripts/run-android-compat-matrix-v2-device.ps1` nhận `DeviceId` và `ExpectedApi`, **kiểm tra API thực trước khi chạy**, cài APK/app-test, chạy 18 bài offline cùng 9 marker logcat, rồi thực hiện 4 pha restart và 4 pha SAF (bao gồm cleanup trước/sau), so sánh PID của hai tiến trình trước/sau force-stop và kiểm tra markers; mỗi API có log riêng.

## Kết quả thực tế

| API | AVD | Offline core | Process restart phases | SAF phases | Kết luận |
|---|---|---:|---:|---:|---|
| 29 / Android 10 | Pixel_5_API_29 | 18/18 | 4/4 + PID đổi | 4/4 + PID đổi | PASS |
| 30 / Android 11 | Pixel_5_API_30 | 18/18 | 4/4 + PID đổi | 4/4 + PID đổi | PASS |
| 33 / Android 13 | Pixel_5_API_33 | 18/18 | 4/4 + PID đổi | 4/4 + PID đổi | PASS |
| 36 / Android 16 | Pixel_7_API_36 | 18/18 | 4/4 + PID đổi | 4/4 + PID đổi | PASS |
| **Tổng** | **4 emulators** | **72/72** | **16/16** | **16/16** | **104/104 PASS** |

Mỗi nhóm 18 bài gồm: **1 E2E offline**, **9 nguồn nhạc Media3**, **5 snapshot/queue persistence**, **3 Playlist playback order/UI**. SAF kiểm tra `ACTION_CREATE_DOCUMENT` thật, persisted URI grant sau force-stop, phát lại bài yêu thích SAF, và cleanup; restart test kiểm tra playlist/history/queue/paused state trên **tiến trình có PID khác**. 9 marker E2E từ MediaStore đến Queue Restore cũng được xác minh trên cả bốn API.

**Bằng chứng có thể kiểm tra lại:**

- `qa-logs/android-compat-matrix-v2-api-29.log` — `FINAL_RESULT=PASS`.
- `qa-logs/android-compat-matrix-v2-api-30.log` — `FINAL_RESULT=PASS`.
- `qa-logs/android-compat-matrix-v2-api-33.log` — `FINAL_RESULT=PASS`.
- `qa-logs/android-compat-matrix-v2-api-36.log` — `FINAL_RESULT=PASS` từ **vòng chạy lại**.

## Phát hiện trên Android 16 và cách xử lý

Vòng SAF đầu trên API 36 **FAIL ở pha prepare** với lỗi `DocumentsUI Save action was not found`. Trích xuất cây accessibility xác nhận nguyên nhân là hộp thoại hệ thống **`System UI isn't responding`** che DocumentsUI, không phải lỗi quyền phát hoặc từ chối bản quyền offline của MusicFlow.

Sửa **chỉ trong QA instrumentation** `SafPermissionRestartTest.kt`:

- Khi UI hiện nút `android:id/aerr_wait`, bài test chọn **Wait** và tiếp tục chờ picker hoạt động; tuyệt đối không tính trường hợp này là thành công nếu chưa nhận URI và persisted read permission.
- Hỗ trợ nhận nút Save qua text/description hoặc resource ID, bổ sung log accessibility có giới hạn 80 node nếu không tìm thấy.
- Tăng riêng timeout trên API 36 để emulator cold boot có thời gian phản hồi; không thay đổi trình phát hoặc quyền nhạc offline trong production.

Sau khi sửa, pha prepare API 36 đã tạo tài liệu thật và **PASS**; chạy lại toàn bộ API 36, bao gồm kiểm tra quyền được giữ sau force-stop và phát bài SAF qua MediaSession, kết quả **26/26 PASS**. 3 API còn lại đã PASS trước thay đổi QA helper; mã production không thay đổi.

## Cách chạy lại

Build APK và test APK trước:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain
```

Khởi động AVD tương ứng, ghi nhận serial qua `adb devices`; với serial ví dụ `emulator-5556`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-android-compat-matrix-v2-device.ps1 -DeviceId emulator-5556 -ExpectedApi 29
```

Đổi `ExpectedApi` cho 30, 33, 36 sau khi khởi động đúng AVD; **không gán tên API giả**, script sẽ từ chối serial có API khác. Có tùy chọn `-SkipSaf`/`-SkipRestart` cho kiểm thử giới hạn; khi bỏ qua thì **không được tuyên bố full 26/26**.

## Giới hạn chưa nghiệm thu

- Đây là **bộ hồi quy tập trung 26 lần instrumented mỗi emulator**, không phải **toàn bộ 55+ bài Android QA** được dùng trong các mốc cũ; chưa chạy connectedDebugAndroidTest toàn bộ suite trên cả 4 API trong lần này.
- Chưa chạy real picker `OpenMultipleDocuments` trên bốn API trong lượt matrix; chỉ kiểm thử hệ thống SAF CreateDocument + URI grant và bài offline MediaStore/playlist.
- Chưa kiểm thử 4 điện thoại thật, Bluetooth/lockscreen end-to-end, audio focus bằng thiết bị vật lý, bộ nhớ lớn/performance, Play Store release-signing, nhiều cỡ màn hình.
- API 36 emulator từng có SystemUI ANR trong cold boot; vẫn nên chạy lại trên thiết bị thật trước khi release, không quy lỗi SystemUI cho MusicFlow nếu chưa có bằng chứng.
- Không thử Google OAuth/email thật/cloud 2 thiết bị và chưa phát nhạc online thật có quyền; các mốc này còn pending theo quyết định dự án.

**Bước tiếp theo:** **Full Product Regression Gate v1** — chạy toàn bộ nhóm test Android/JVM (thay thế script cũ có count hard-coded), ghi nhận fail/skip và xử lý; sau đó Release Candidate QA / packaging. Google Login/email thật và cloud 2 thiết bị vẫn ở giai đoạn cuối.
