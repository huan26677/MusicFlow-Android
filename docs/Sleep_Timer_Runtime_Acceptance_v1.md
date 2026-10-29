# MusicFlow — Sleep Timer Runtime Acceptance v1

**Ngày:** 2026-10-29. **Git HEAD nền:** `5350ce5`. **Kết quả:** Build/JVM + **11/11 Android API 30 PASS** (Sleep Timer 3/3, MediaSession/Queue regressions 8/8). Đây là nghiệm thu runtime riêng Sleep Timer, không phải nghiệm thu toàn MusicFlow 1.0.

## Phạm vi người dùng

Sleep Timer trên Now Playing vẫn chỉ có nút **15 / 30 / 45 / 60 phút** và **Hủy hẹn giờ**. Khi đến hạn, `PlaybackService` tạm dừng MediaSession nhưng giữ nguyên bài hiện tại, vị trí và hàng đợi. Người dùng có thể tiếp tục phát thủ công sau đó. **Nhạc offline tự tải không cần Internet, tài khoản hoặc xác minh quyền streaming online của MusicFlow.**

## Kiểm thử đã thực hiện

`SleepTimerRuntimeTest` dùng MediaController/MediaSessionService thật và tệp WAV cục bộ tạo trong cache emulator; gửi cùng `SleepTimerContract.ACTION` mà giao diện sử dụng, không giả mạo callback hay audio. Kết quả:

1. **`validatesSetQueryReplaceAndCancel` PASS**: từ chối thời lượng 5 phút không hỗ trợ, từ chối debug duration sai; đặt 15 phút, đọc remaining; thay bằng 30 phút và thấy deadline tăng; hủy, đọc remaining = 0.
2. **`expiryPausesRealMediaSessionWithoutClearingOfflineQueue` PASS**: nạp hai WAV vào MediaSession thật, xác nhận đang phát; đặt timer có thời lượng debug 2 giây, thấy timer kết thúc, kiểm tra **isPlaying=false, playWhenReady=false, current media ID/index không đổi, queue vẫn đủ 2 bài** và remaining = 0.
3. **`cancellingActiveShortTimerDoesNotPauseOfflineMusic` PASS**: đặt timer debug 1 giây rồi hủy, đợi qua deadline cũ, xác nhận **nhạc vẫn đang phát**, current media ID không đổi.

**8 marker log bắt buộc:** `invalidValuesRejected`, `timerSetAndQueried`, `timerReplaced`, `timerCancelled`, `offlinePlaybackStarted`, `TIMER_FINISHED`, `expiryPausesAndPreservesQueue`, `cancelPreventsExpiryPause`.

### Thời gian kiểm thử rút ngắn — chỉ trong debug

Vì timer UI ngắn nhất là 15 phút, thêm `SleepTimerContract.KEY_DEBUG_DURATION_MS` để kiểm chứng **đường hẹn giờ production thật** bằng thời gian ngắn. Lệnh debug chỉ được nhận nếu:

- `BuildConfig.DEBUG == true` **và** `controller.packageName == packageName` (chính ứng dụng).
- `minutes > 0` thuộc bộ giá trị có sẵn `15/30/45/60`.
- `debugDurationMs` nằm trong **200..15000 ms**. Không chấp nhận override trên lệnh query/hủy.

**Release không chấp nhận override debug** theo guard trong mã nguồn; chưa chạy kiểm thử riêng trên APK release-signed. Thời lượng người dùng vẫn dùng chính xác `minutes * 60000` qua `SystemClock.elapsedRealtime()`; timer chỉ còn trong vòng đời tiến trình dịch vụ. Việc dừng service/reboot có thể làm mất hẹn giờ, **chưa có yêu cầu hoặc kiểm thử persistence qua process restart cho timer**.

## QA và bằng chứng

- Script `scripts/run-sleep-timer-runtime-v1-tests.ps1`; log `qa-logs/sleep-timer-runtime-v1.log` có `FINAL_RESULT=PASS`.
- `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`: **BUILD SUCCESSFUL/JVM PASS** (99/99 trong suite hiện tại).
- `SleepTimerRuntimeTest`: **3/3 Android PASS** trên `emulator-5554` API 30 và 8/8 markers.
- Hồi quy độc lập `PlaybackServiceIntegrationTest` (3) + `PlaybackQueuePersistenceTest` (5): **8/8 Android PASS**.
- Tổng **11/11 Android PASS**; `git diff --check` PASS. Bài kiểm thử giữ snapshot queue cũ, cancel timer QA trong cleanup và xóa WAV thử. Không động tới nhạc người dùng trên thiết bị thật.

## Giới hạn và bước tiếp theo

- Không chờ thật 15–60 phút, chưa test Sleep Timer trên API 29/33/36 hoặc điện thoại thật, chưa test service bị hệ thống kill khi timer đang chạy; chỉ xác minh service đang sống.
- Test không bao gồm Sleep Timer Compose UI end-to-end với nút bấm thật; kiểm tra MediaSession API và logic runtime với debug clock thực.
- Chưa đổi bất kỳ hệ thống quyền phát online, Supabase hoặc cloud/Auth nào.

**Bước kế tiếp (mốc nhỏ): Runtime Feature Acceptance — Equalizer v1.** Kiểm tra command/session ID/enable/band state và phát WAV offline trên emulator, rồi tiếp tục Lyrics v1 độc lập. Google Login/email thật và cloud sync hai máy vẫn ở cuối.
