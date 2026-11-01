# MusicFlow — Playback Speed Runtime Acceptance v1

**Ngày:** 2026-11-01. **HEAD nền:** `3a59bc7` (Lyrics Import UI E2E v2 đã commit). **Kết quả:** `BUILD SUCCESSFUL`, **105/105 JVM PASS**, **2/2 Playback Speed Android + 8/8 Playback/Queue regression = 10/10 Android API 30 PASS**.

## Mục tiêu và những gì đã chạy thật

Kiểm tra tốc độ phát nhạc **offline** trên Media3 ExoPlayer/MediaSessionService với hai file WAV 30 giây tạo riêng trong cache emulator. Không yêu cầu mạng, tài khoản, Supabase hay giấy phép streaming dành cho danh mục nhạc online của MusicFlow.

- **Now Playing + MediaSession:** Test mở `MainActivity`, đưa hàng đợi gồm **Speed QA Track Alpha/Beta** vào MediaSession thật, mở Now Playing từ Mini Player. Lần lượt kích hoạt lựa chọn `0.75x`, `1.0x`, `1.25x`, `1.5x`, `2.0x`. Mỗi lần phải thấy **MediaController.playbackParameters.speed** khớp giá trị mong đợi và FilterChip được đánh dấu `selected`.
- **Pause/resume và Next:** thao tác **Tạm dừng → Phát → Tiếp** từ Now Playing bằng Compose UI test, xác minh `isPlaying` thay đổi đúng, tốc độ **2.0x** được giữ, MediaItem chuyển từ Alpha sang Beta, số lượng/thứ tự hai bài giữ nguyên. Sau đó chọn **1.0x** để trở về mặc định.
- **Đầu vào ngoài giới hạn:** tạo một `PlaybackController` thật khác cùng kết nối MediaSession. Các giá trị `-1`, `0`, `0.49`, `2.01`, `NaN` và `+Infinity` bị từ chối, tốc độ đang phát vẫn giữ **1.25x**. Hai biên `0.5x` và `2.0x` của controller được chấp nhận mà không làm mất hàng đợi.
- **Cleanup:** lưu và khôi phục queue, media index/position, trạng thái phát, shuffle, repeat và **tốc độ gốc**; release controller và xóa hai WAV thử.

## Thay đổi mã và phát hiện QA

1. Thêm `app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackSpeedRuntimeTest.kt` gồm **2 Android tests**; mã chạy qua callback production và MediaSession thật, không giả lập trạng thái player.
2. Thêm `Modifier.testTag("playback-speed-${speed}x")` vào các FilterChip của `NowPlayingScreen.kt` để kiểm thử/khả năng nhận diện đúng từng chip; **không thay đổi chức năng, tốc độ được phép hay thiết kế hiển thị**.
3. Sửa **lỗi biên dịch có sẵn ở commit trước**: `LyricsImportUiE2eTest.kt` dùng `Context.MODE_PRIVATE` nhưng thiếu import `android.content.Context`; bổ sung import, không thay đổi nội dung logic Lyrics.
4. **Giới hạn quan trọng:** `performClick()` là sự kiện chạm dựa trên tọa độ trong Compose test **không kích hoạt ổn định** các FilterChip nằm trong `horizontalScroll` lồng trong màn `verticalScroll` trên emulator này, ngay cả khi tìm đúng chip bằng tag. Qua log theo từng tầng đã xác minh **callback thực của NowPlaying → MainActivity → PlaybackController → MediaSession** hoạt động khi kích hoạt `SemanticsActions.OnClick`. Bài test chính thức dùng `performSemanticsAction(SemanticsActions.OnClick)` cho các chip tốc độ, nhưng vẫn dùng **performClick thật** cho MiniPlayer, Tạm dừng, Phát và Tiếp. Vì vậy **chưa được tuyên bố kiểm thử chạm vật lý vào FilterChip PASS**; cần mốc kiểm chứng riêng trên thiết bị thực/UiAutomator hoặc sửa khả năng nhận chạm nếu tái hiện.
5. Không thay đổi production `PlaybackController.setPlaybackSpeed`, `PlaybackService`, quyền truy cập tài liệu, nội dung nhạc, bản quyền online hoặc Supabase.

## Bằng chứng kiểm thử

- Script: `scripts/run-playback-speed-runtime-v1-tests.ps1`, log: `qa-logs/playback-speed-runtime-v1.log`.
- Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`: **BUILD SUCCESSFUL**, JUnit XML **105 total / 0 FAIL / 0 SKIP**.
- `PlaybackSpeedRuntimeTest`: **2/2 Android PASS** với **7 marker**: `offlineMediaSessionReady`, `invalidSpeedsRejected`, `speedLimitsAndQueueStable`, `fiveUiSpeedsReachMediaSession`, `uiPauseResumeKeepsSpeed`, `nextTrackKeepsSpeedAndQueue`, `uiSpeedResetToNormal`.
- Hồi quy ở instrumentation khác: `PlaybackServiceIntegrationTest` **3/3 PASS** và `PlaybackQueuePersistenceTest` **5/5 PASS**. Tổng **10/10 Android API 30 PASS**, `GIT_DIFF_CHECK_EXIT=0`, `FINAL_RESULT=PASS`.
- Script bắt buộc dòng `OK (N tests)` và không có `FAILURES`; không tin rằng exit code 0 của `am instrument` nghĩa là PASS.

## Giới hạn và mốc tiếp theo

Chưa đo tỷ lệ thời gian thực/âm thanh đầu ra ở các mức tốc độ; chứng cứ hiện tại là MediaSession đọc lại đúng `playbackParameters.speed` và phát file offline không ngắt. Chưa kiểm thử tốc độ sau force-stop/process restart, đa API 29/33/36, điện thoại thực hoặc toàn bộ Android regression suite sau thay đổi này.

**Mốc tiếp theo (nhỏ): Playback Speed Touch Interaction QA v2** — xác minh chạm thực tế vào chip tốc độ trong giao diện cuộn ngang bằng UiAutomator/thiết bị; tìm và xử lý nếu đó là lỗi UX thật. Sau đó mới chuyển sang Crossfade/Gapless Runtime Acceptance v1. Google Login/email và Cloud Sync E2E hai máy vẫn để gần cuối; streaming thật chờ nhạc được cấp phép.
