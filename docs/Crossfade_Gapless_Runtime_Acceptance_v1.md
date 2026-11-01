# MusicFlow — Crossfade / Gapless Runtime Acceptance v1

**Ngày:** 2026-11-01. **Git HEAD nền:** `36c55cc` (Playback Speed Touch Interaction QA v2 đã commit). **Kết quả:** Build + **105/105 JVM PASS**, Android API 30 **4/4 Crossfade Runtime + 8/8 Playback/Queue = 12/12 PASS**, có đủ marker `MIX_STARTED` và `HANDOFF_DONE` cho từng thời lượng **1s/2s/3s**. **Chưa chứng minh chất lượng âm thanh gapless/crossfade qua phép đo sóng.**

## Phạm vi và hành vi sản phẩm

MusicFlow có một `PlaybackCrossfade` thử nghiệm, **mặc định tắt** (`durationMs=0`). Khi tắt, hai bài được chuyển theo Media3 thông thường. Khi bật 1/2/3 giây ở điều kiện phù hợp, dịch vụ tạm preload bài kế tiếp trên ExoPlayer thứ hai, đặt volume của hai player theo tiến trình chuyển tiếp, rồi seek MediaSession chính sang bài mới. Hàng đợi và lịch sử vẫn do MediaSession chính quản lý.

Theo thiết kế hiện có, tính năng **không cố Crossfade** khi phát tốc độ khác 1x, shuffle/repeat không phù hợp, bài quá ngắn hoặc seek/pause/queue thay đổi. Nếu không đủ điều kiện, player chuyển bài theo cơ chế Media3. Tệp nhạc offline của người dùng tự lưu **không cần Internet, đăng nhập hay kiểm tra bản quyền streaming online**.

## Bài kiểm thử thực tế

Thêm `app/src/androidTest/java/com/example/musicflow/core/playback/CrossfadeRuntimeTest.kt` (AndroidJUnit4), dùng MediaController/PlaybackService thực với **hai WAV 12 giây, PCM yên lặng** tạo trong cache emulator (không tải nhạc không có quyền). Bốn bài:

1. `transitionCommandValidatesAndPersistsSupportedDurations` — **PASS**: command GET/SET của MediaSession nhận 0/1000/2000/3000 ms, từ chối -1/500/1500/4000 ms và operation không tồn tại; SharedPreferences lưu đúng lựa chọn.
2. `zeroDurationUsesNaturalMedia3ForwardTransition` — **PASS**: Crossfade=0, từ bài WAV Alpha chuyển sang Beta do Media3 khi bài đầu tới cuối; index và hai phần tử queue còn chính xác. Điều này là **functional transition**, không chứng nhận gapless không có khoảng lặng.
3. `enabledCrossfadeMixesAndHandsOffAtOneTwoThreeSeconds` — **PASS**: lần lượt SET 1/2/3 giây, phát bài Alpha, seek tới trước cửa sổ preload theo thời gian của engine. Với mỗi thời lượng, MediaSession chuyển sang Beta, không mất bài/queue, tốc độ 1x không đổi; log **`MIX_STARTED durationMs=...`** và **`HANDOFF_DONE durationMs=... index=1`** có mặt cho **cả ba** mốc.
4. `variableSpeedFallsBackToOrdinaryForwardTransition` — **PASS**: dù config Crossfade=3 giây, phát ở 1.25x vẫn tự chuyển Alpha→Beta theo Media3, giữ tốc độ và queue. Đây là fallback có kiểm thử, không phải đo mức volume overlap.

Trong cleanup, bài test khôi phục queue, vị trí, speed, shuffle/repeat, trạng thái phát và giá trị Crossfade đã tồn tại trước QA (qua command thật); xóa WAV test. Không đụng thư viện riêng, Supabase hay giấy phép nhạc online.

## Bằng chứng QA chính thức

- `scripts/run-crossfade-runtime-v1-tests.ps1`: xác nhận chính xác một emulator API 30 đã boot, build app/AndroidTest/JVM, đếm JVM từ JUnit XML, cài APK, chạy riêng `CrossfadeRuntimeTest` 4 bài, bắt buộc **13 marker** (gồm sáu marker engine MIX/HANDOFF tương ứng 1/2/3s), rồi chạy `PlaybackServiceIntegrationTest` 3/3 + `PlaybackQueuePersistenceTest` 5/5 và `git diff --check`. Instrumentation phải có `OK (N tests)`, không được có `FAILURES`, tránh `adb am instrument` exit code 0 giả.
- `qa-logs/crossfade-runtime-v1.log`: **`BUILD_JVM_EXIT=0`**, `JVM_TOTAL=105`, `JVM_FAIL=0`, `JVM_SKIP=0`; `CROSSFADE_RUNTIME_TESTS=4`; `CROSSFADE_RUNTIME_MARKERS=13`; `PLAYBACK_QUEUE_REGRESSION_TESTS=8`; `TOTAL_ANDROID_TESTS=12`; `GIT_DIFF_CHECK_EXIT=0`; **`FINAL_RESULT=PASS`**.
- Trong lượt debug đầu đã có `MIX_STARTED durationMs=1000` nhưng test bị lỗi đọc `MediaController.playbackParameters` ngoài main thread. Sửa đúng main thread, chờ engine hoàn thành handoff trước khi reset; lượt sau **4/4 PASS** với log hoàn tất **cả 3 thời lượng**. Script QA lượt đầu có tên marker cũ do sao chép từ Playback Speed, nên **FAIL kiểm tra marker dù 4/4 Android đã PASS**; sửa marker, chạy lại toàn bộ script và chỉ chốt lượt **FINAL_RESULT=PASS**.
- Mã production chỉ bổ sung thông tin `durationMs` vào **hai log vốn đã có** của `PlaybackCrossfade`; không thay đổi thuật toán, cơ chế volume, quyền truy cập hay mặc định.

## Giới hạn cần giữ trung thực

WAV kiểm thử dùng PCM yên lặng: log engine xác nhận quy trình trộn và bàn giao, nhưng **không chứng minh người nghe nghe được bản mix mượt, không đo khoảng lặng gapless, clipping, loudness hoặc chất lượng đầu ra**. Chưa chạy loopback/thu âm tần số, tai nghe Bluetooth/thiết bị vật lý, API 29/33/36, toàn bộ Android regression suite sau thay đổi này, hay xử lý từng trường hợp ngắt giữa chừng (pause/seek/queue change) qua một bài QA riêng. Chưa chạy end-to-end chạm nút Crossfade trên Now Playing; mốc hiện tại kiểm tra MediaSession command thật.

**Bước tiếp theo (mốc nhỏ): Crossfade UI & Interruption QA v2** — nhấn 0/1/2/3s trên Player, kiểm tra trạng thái chọn, dừng/tua giữa chuyển tiếp và fallback an toàn, hàng đợi không hỏng; không tuyên bố gapless tuyệt đối nếu chưa đo tín hiệu.
