# MusicFlow — Equalizer Runtime Acceptance v1

**Ngày:** 2026-10-29. **HEAD nền:** `38af246`. **Trạng thái:** **BUILD/JVM PASS; 10/10 Android API 30 PASS** (Equalizer runtime 2/2, Playback/Queue regression 8/8). Không đồng nghĩa Full Product 1.0 đã nghiệm thu.

## Phạm vi

Kiểm tra Equalizer **thật** của `android.media.audiofx.Equalizer` gắn với audio session của **Media3 ExoPlayer trong `PlaybackService`**. Phát tệp WAV 30 giây được tạo trong cache emulator, không dùng quyền Internet, không truy vấn Supabase và không kiểm tra quyền streaming online với tệp offline do người dùng tự lưu.

**UI và tính năng người dùng không đổi:** Equalizer tùy thuộc từng thiết bị; app chỉ bật và điều chỉnh các band nếu Android cung cấp audio effect. Khi effect không được hỗ trợ, UI nhận `supported=false` và không được tự quảng cáo EQ đã hoạt động. Các thiết bị có thể có số band, tần số giữa và gain range khác nhau.

## Đường kiểm thử và các xác nhận

`app/src/androidTest/java/com/example/musicflow/core/playback/EqualizerRuntimeTest.kt`:

1. **`validationAndPlaybackContinuity` PASS:** phát WAV thật bằng MediaController; xác nhận Eq state `supported=true`, session ID thực dương, có band/frequency/gain range hợp lệ; từ chối operation không tồn tại, band index ngoài phạm vi và gain trên max, không làm thay đổi state EQ hoặc ngắt nhạc offline.
2. **`enableBandPersistAndControllerReconnect` PASS:** bật/tắt EQ qua SessionCommand, đọc lại `enabled`, xác nhận lưu `enabled` trong SharedPreferences; đổi gain của band đầu và đọc lại `levelsMb`, `band_0` đã lưu; kết nối **MediaController thứ hai với cùng service** và xác nhận thấy setting mới. MediaSession vẫn đang phát WAV sau thay đổi.
3. **Cleanup:** test lưu snapshot queue và trạng thái EQ ban đầu, trả lại gain/enable qua command, khôi phục đúng các key SharedPreferences liên quan, khôi phục danh sách phát và xóa file WAV QA. Không chỉnh dữ liệu thật của người dùng hoặc kho online.

**6 marker chính:** `liveAudioEffectSession`, `invalidSettingsDoNotStopPlayback`, `enableTogglePersisted`, `bandGainPersisted`, `controllerReconnectKeepsState`, `equalizerDoesNotStopOfflineAudio`.

## Mã nguồn thay đổi

- `EqualizerContract.KEY_AUDIO_SESSION_ID` và `EqualizerEngine.stateBundle()` xuất ID session hiện tại cho controller nội bộ để kiểm chứng effect gắn đúng playback audio session. Không thêm quyền ngoài, không thay đổi giao diện, giá trị gain, cơ chế lưu preset hay quy tắc bản quyền.
- Mọi lệnh `MediaController` được gọi trên **main thread** theo quy định Media3; kiểm tra kết quả `SessionResult` và `Bundle` trả về thật, không giả lập thành công.

## Bằng chứng QA

- `scripts/run-equalizer-runtime-v1-tests.ps1`, `qa-logs/equalizer-runtime-v1.log`: `FINAL_RESULT=PASS`.
- Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`: **BUILD SUCCESSFUL / JVM PASS**.
- Android emulator `emulator-5554` / API 30: **2/2 EqualizerRuntimeTest PASS**, 6/6 marker.
- Hồi quy ở phiên instrumentation riêng: `PlaybackServiceIntegrationTest` **3/3**, `PlaybackQueuePersistenceTest` **5/5**. Tổng **10/10 Android PASS**.
- `git diff --check` **exit 0** (cảnh báo CRLF của Git không phải lỗi).

## Giới hạn chưa nghiệm thu

- Chưa đo tín hiệu tần số/âm lượng thực qua audio loopback hoặc thiết bị nghe, chỉ xác nhận API audio effect thực chấp nhận lệnh và trả trạng thái đúng.
- Chưa kiểm thử *tạo lại PlaybackService/process restart thật* để chứng minh engine tải preset sau process death; chỉ xác nhận SharedPreferences cập nhật và một MediaController **reconnect cùng service** nhận đúng thông tin.
- Chưa thử giao diện Compose Equalizer end-to-end bằng thao tác người dùng, máy thật, các bản Android 29/33/36 hoặc thiết bị không hỗ trợ effect. Không đánh đồng 10 bài targeted với toàn bộ 90+ Android tests.
- Chưa thực hiện streaming nhạc được cấp phép thật, Google Login/email thật hoặc cloud hai thiết bị (theo kế hoạch Full Product trước).

**Bước tiếp theo:** **Runtime Feature Acceptance — Lyrics v1**: kiểm tra nhập LRC qua SAF, parser, đồng bộ dòng theo timestamp, khôi phục liên kết LRC theo audio URI và màn hiển thị; mốc nhỏ riêng biệt, rồi mới QA Release Candidate sau các phần còn thiếu.
