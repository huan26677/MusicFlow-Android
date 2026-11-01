# MusicFlow — Playback Speed Touch Interaction QA v2

**Ngày:** 2026-11-01. **HEAD nền:** `70704d3` (Playback Speed Runtime Acceptance v1 đã commit). **Kết quả cuối:** **Build/JVM 105/105 PASS; 12/12 Android instrumentation API 30 PASS**. Đây là nghiệm thu tương tác tốc độ trên emulator, chưa phải nghiệm thu MusicFlow 1.0 hoặc chứng nhận thiết bị thật.

## Nguyên nhân tồn đọng từ v1

Trong `NowPlayingScreen`, các nút `0.75x / 1.0x / 1.25x / 1.5x / 2.0x` nằm trong `Row.horizontalScroll`, được lồng trong màn Player `Column.verticalScroll`. Test v1 gọi `performScrollTo()` trên **chính chip**, nên Compose tìm container có thể scroll gần nhất (ngang) mà **không đưa hàng nút vào vùng hiển thị theo chiều dọc**. Do đó `performClick`/tương tác theo tọa độ không kích hoạt chip; trong khi `SemanticsActions.OnClick` có thể kích hoạt callback kể cả khi phần tử chưa hiển thị. Điều này **không chứng minh ứng dụng bị lỗi touch**.

## Kiểm thử chạm và cuộn ngang trên Android thật của emulator

- Thêm `physicalTouchOnSpeedChipsWorksAcrossHorizontalScroll` vào `PlaybackSpeedRuntimeTest`. Mở `MainActivity` → Mini Player → Now Playing qua các thao tác UI, rồi **cuộn tới tiêu đề `Hẹn giờ tắt nhạc`** (phần tử cùng container dọc, ngay dưới hàng nút tốc độ). Test xác nhận nút hiển thị bằng `assertIsDisplayed`, lấy tọa độ `SemanticsNode.boundsInWindow`, chuyển đổi sang tọa độ màn hình thực và gọi `UiDevice.click(x,y)`. Không sử dụng `SemanticsActions.OnClick` hoặc đặt tốc độ trực tiếp để thay thế sự kiện chạm trong bài này.
- **Chạm đủ năm nút:** `0.75x, 1.0x, 1.25x, 1.5x, 2.0x`; sau mỗi thao tác, một MediaController độc lập đọc tốc độ `playbackParameters.speed` từ `PlaybackService`/MediaSession thật và xác nhận đúng. Hàng đợi là hai WAV offline thử nghiệm; test khôi phục queue, position, trạng thái phát và speed cũ trong cleanup.
- **Cuộn ngang bằng input Android thật:** dùng `UiDevice.swipe` ở trục y của hàng nút trước khi chạm `1.5x/2.0x`. Ở cấu hình bình thường 1080x2340, cả năm chip vừa màn hình nên swipe gần như không thay đổi vị trí; đây không phải bằng chứng kiểm thử overflow.
- **Cỡ chữ 150%:** script QA tạm đổi `settings system font_scale` thành `1.5` trên emulator rồi chạy riêng bài physical touch. Trước khi swipe, `2.0x` có bounds **rỗng** (`Rect.fromLTRB(0,0,0,0)`) vì ngoài vùng hiển thị; sau swipe, chip có bounds **hiển thị** và nhận được chạm thật. Có marker bắt buộc `PASS horizontalOverflowRevealedByPhysicalSwipe`. Script **khôi phục font_scale gốc bằng `finally`** dù kiểm thử FAIL (lần chạy chính thức khôi phục `1.0`).
- Khi hệ thống vừa đổi font scale/cấu hình, đầu vào chạm đầu tiên có thể bị mất do chuyển trạng thái focus/layout; helper có thể gửi lại **tối đa ba sự kiện chạm Android thật**, chờ MediaSession xác nhận sau từng lần. **Không có đường fallback dùng lệnh semantics hoặc setPlaybackSpeed**. Trong lượt QA cuối, cả năm nút ở cỡ chữ thường và 150% đều xác nhận từ **lần chạm đầu tiên** (attempt=1).
- **Không sửa logic phát nhạc, bộ xử lý tốc độ, layout/UX production, quyền streaming online, Supabase hoặc Auth/Cloud.** Tất cả thay đổi mã Kotlin của mốc này chỉ nằm trong `androidTest`.

## Kết quả có bằng chứng

Script `scripts/run-playback-speed-touch-v2-tests.ps1`, log `qa-logs/playback-speed-touch-v2.log`:

| Nhóm kiểm thử | Kết quả |
|---|---|
| Gradle Debug App + AndroidTest + JVM | **BUILD SUCCESSFUL** |
| JVM từ JUnit XML | **105 PASS / 0 FAIL / 0 SKIP** |
| `PlaybackSpeedRuntimeTest` (bao gồm test physical touch mới) | **3/3 Android PASS** |
| `PlaybackServiceIntegrationTest` | **3/3 Android PASS** |
| `PlaybackQueuePersistenceTest` | **5/5 Android PASS** |
| Physical Touch riêng tại font scale 150% | **1/1 Android PASS**, horizontal overflow được xác nhận |
| Tổng instrumentation API 30 | **12 PASS / 0 FAIL** |
| Git diff check | **exit 0** |
| Tổng kết | **FINAL_RESULT=PASS** |

Script không xem `adb am instrument` exit code 0 là PASS nếu thiếu `OK (N tests)` hoặc có `FAILURES`. Lượt chạy tổng hợp đầu tiên đã **FAIL tại lần chạm đầu sau đổi font scale**, script khôi phục font cũ và báo FAIL. Đã tăng độ ổn định với chờ UI idle, gửi lại sự kiện chạm thật khi cần; chạy lại toàn bộ script **PASS**, dữ liệu bên trên lấy từ lượt chạy thành công cuối.

## Giới hạn

Chưa thử điện thoại vật lý, kích thước màn hình/thao tác bằng ngón tay thật, Android API 29/33/36, TalkBack riêng hoặc full `:app:connectedDebugAndroidTest` sau thay đổi này. Kiểm thử xác nhận lệnh chạm Android và tốc độ MediaSession, **không đo độ dài âm thanh thực tế ở từng speed**. Không đánh đồng kết quả 12 targeted Android tests với toàn bộ MusicFlow 1.0.

**Mốc tiếp theo:** **Crossfade/Gapless Runtime Acceptance v1** — kiểm chứng chuyển hai bài WAV offline qua Media3, thời lượng crossfade cấu hình được (mặc định tắt), chuyển bài khi đang phát, fallback và hàng đợi; chỉ kết luận hỗ trợ gapless thật nếu có bằng chứng đo đủ. Google/email login thật và cloud hai thiết bị vẫn để giai đoạn cuối; licensed streaming chỉ chạy khi có audio được cấp phép.
