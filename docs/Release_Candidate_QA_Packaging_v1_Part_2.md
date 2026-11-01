# MusicFlow — Release Candidate QA / Packaging v1, Part 2

**Ngày:** 2026-11-01 (Asia/Ho_Chi_Minh). **HEAD nền:** 00f2d41c36bceb422caadcff34354390c7c6c2f5 (Part 1 đã commit). **Kết quả cuối:** PASS — kiểm thử playback thực tế trên APK release-mode ký QA, Android 11 / API 30. **Chưa phải nghiệm thu release production hoặc toàn bộ MusicFlow 1.0.**

## Mục tiêu

Part 1 chỉ xác nhận build, ký QA, cài mới/nâng cấp và giữ internal marker. Part 2 xác minh trực tiếp cấu hình **release, không debug**, với file nhạc offline qua MediaStore, Library Compose, Media3 và MediaSession trên emulator, không dùng instrumentation APK debug để thay thế ứng dụng thực tế.

## Kết quả có bằng chứng

| Tiêu chí | Kết quả / bằng chứng |
|---|---|
| Build release và ký bằng debug-key QA | PASS — BUILD_RELEASE=PASS, QA_APK_SIGNATURE=PASS, chữ ký v3 |
| Tệp nhạc thực qua MediaStore | PASS — 2/2 WAV dài 40 giây được quét, metadata được công bố, hiển thị trong Library |
| Chọn bài từ giao diện người dùng | PASS — chạm dòng bài A thật trên Library; MediaSession phát bài A, state=3 |
| Tạm dừng bằng Android media key | PASS — state=2, vẫn đúng bài A |
| Phát tiếp bằng Android media key | PASS — state=3, vẫn đúng bài A |
| Bài tiếp theo bằng Android media key | PASS — MediaSession state=3, metadata chuyển đúng sang bài B |
| Phát nền | PASS — nhấn Home; MediaSession vẫn state=3 ở bài B |
| Notification MediaStyle | PASS — notification thuộc MusicFlow, Android MediaStyle |
| Dọn dữ liệu QA | PASS — xóa đúng tệp WAV, MediaStore row, ứng dụng cài bởi QA và UI dump, không còn bài QA/ứng dụng cài |

Log cuối cùng: **qa-logs/release-playback-smoke-v1.log**, có các marker RELEASE_OFFLINE_PLAY=PASS, RELEASE_PAUSE=PASS, RELEASE_RESUME=PASS, RELEASE_NEXT=PASS, RELEASE_BACKGROUND=PASS, RELEASE_MEDIA_NOTIFICATION=PASS, QA_CLEANUP=PASS và FINAL_RESULT=PASS.

## Script và artifact

Script: **scripts/run-release-playback-smoke-v1.ps1**, mặc định chạy trên emulator-5554 API 30. Script có tham số DeviceId và ExpectedApi; các API khác **chưa được xác nhận PASS trong đợt này**.

Quy trình: build lại assembleRelease từ source; zipalign và ký APK **chỉ với Android Debug keystore cục bộ**; xác minh APK Signature v3; tạo hai WAV 40 giây bằng PowerShell, đẩy vào thư mục QA riêng có tên duy nhất và quét MediaStore; riêng trên emulator, tệp đẩy bằng ADB có thể bị MediaStore đánh dấu IS_PENDING=1 nên chỉ cập nhật các row chính xác do QA mới tạo thành IS_PENDING=0 / IS_MUSIC=1 / duration=40000. Không tác động vào nhạc người dùng. Sau đó cài bản release-mode, cấp permission audio theo API, chạm UI thực bằng Android input, kiểm tra MediaSession qua dumpsys và notification qua dumpsys. Cuối cùng dọn đúng các đối tượng QA.

APK kiểm thử: **app/build/rc-qa/release-playback/MusicFlow-1.0-RC2-QA.apk** (nằm trong thư mục build, không commit Git).

SHA-256 của APK đã kiểm thử:

    7A6E18284BB2923DF97A1C29B74CB721CEB2164F4A601367794D8E0E2DB13BDD

Lần đầu script FAIL trước khi chọn Library do PowerShell 5.1 diễn giải text tiếng Việt sai khi file chưa có UTF-8 BOM; không phải lỗi của MusicFlow. Đã lưu script bằng UTF-8 BOM, chạy lại và toàn bộ marker PASS. Một lượt thứ hai sau thay đổi cleanup UI dump cũng kết thúc FINAL_RESULT=PASS. Không sửa Kotlin production, Supabase, metadata bản quyền hay cloud.

## Giới hạn và bước kế tiếp

- Đã thực hiện **smoke trên bản release-mode QA tại API 30**, không phải full regression 115 Android testcase trên APK release. Full regression 115 case trước đó chạy debug instrumentation và đã PASS theo báo cáo v2 Part 1.
- Chưa chạy release playback trên API 29/33/36 hoặc điện thoại thật, Bluetooth thật, lockscreen, đo chất lượng âm thanh, latency/gapless, pin, ANR và kho nhạc lớn.
- APK vẫn ký bằng debug keystore để phục vụ QA, không phải artifact phân phối. Application ID hiện là com.example.musicflow, versionCode 1, versionName 1.0, chưa chốt production signing, App Bundle hoặc chiến lược version.
- File nhạc offline người dùng tự lưu/tải vẫn không phải xác minh giấy phép online. Licensed Streaming Smoke cần nguồn có quyền thật; Google Login/email thật và Cloud Sync hai thiết bị được giữ ở giai đoạn cuối theo quyết định phạm vi.

**Bước tiếp theo:** RC QA / Packaging — Part 3: thực hiện release-mode playback smoke trên Android 29/33/36 (có AVD nhưng chưa được kiểm thử trong bước này), sau đó lập danh sách cổng phát hành cần thiết cho artifact ký production và thiết bị thật. Không đánh dấu MusicFlow 1.0 đã nghiệm thu khi những cổng này còn mở.
