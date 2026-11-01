# MusicFlow — Crossfade UI & Interruption QA v2

**Ngày:** 2026-11-01. **Git HEAD nền:** `6aa0549` (Crossfade/Gapless Runtime v1 đã commit). **Kết quả:** Gradle Build, JVM **105/105 PASS**, Android API 30 **17/17 PASS** (8 Crossfade runtime, 1 UI Now Playing, 8 Playback/Queue regression). Đây chỉ là QA chức năng và không khẳng định đầu ra âm thanh gapless tuyệt đối.

## Mục tiêu

Trên nhạc offline WAV do QA tự tạo, kiểm thử **đường giao diện thật**: chọn Crossfade **Tắt / 1s / 2s / 3s / Tắt** ở Now Playing, xác nhận callback MainActivity → ViewModel → SessionCommand → `PlaybackService` cập nhật trạng thái. Kiểm thử khi **Mixing thật đang hoạt động**, người dùng pause, seek ngược, sửa queue hoặc tắt Crossfade: không còn secondary player tranh chấp, primary volume trở lại 1, không mất/trùng bài.

## Thay đổi trong source

- `NowPlayingScreen.kt`: mỗi FilterChip crossfade có `Modifier.testTag("crossfade-duration-${duration}ms")` để kiểm thử định vị chính xác, **không đổi giao diện hay logic callback**.
- `PlaybackCrossfade.kt`: bổ sung log lifecycle khi secondary được release, có `MIX_CANCELLED stage=MIXING` cho việc hủy thật, `MIX_CLEANUP stage=HANDOFF` khi bàn giao thành công; không đánh đồng cleanup bình thường là lỗi; **không thay thuật toán trộn hoặc volume**, vẫn mặc định tắt.
- `CrossfadeRuntimeTest.kt`: giữ bốn bài v1 và thêm bốn bài interrupt trên MediaSession/ExoPlayer thật. Vào cửa sổ trộn của bài đầu trong playlist hai WAV 12 giây bằng seek từ ngoài cửa sổ preload; sau đó hủy ở khoảng giữa mix, kiểm tra queue, item index/ID, primary volume và trạng thái phát:
  - `pauseDuringMixingRestoresPrimaryAndKeepsPosition`: pause, chờ engine cancel, queue/volume/position giữ đúng; tắt crossfade rồi resume.
  - `seekingBackwardDuringMixingCancelsSecondary`: seek về 1 giây, player tiếp tục, index 0/volume và queue giữ đúng.
  - `editingQueueDuringMixingCancelsSecondaryWithoutGhostItem`: xóa bài thứ hai, quan sát queue chỉ còn một bài, thêm lại bài cũ mà không nhân đôi; tắt Crossfade rồi chuyển sang bài thứ hai bằng Media3 thông thường.
  - `disablingCrossfadeDuringMixingRestoresNormalPlayback`: đặt duration=0 giữa mix, volume trở về 1 và chuyển bài tiếp theo theo Media3 thông thường.
- `CrossfadeUiE2eTest.kt`: chạy `MainActivity`, WAV 30 giây và MediaController thật. Mở Now Playing từ MiniPlayer, cuộn theo tiêu đề `Equalizer` để đưa hàng FilterChip vào viewport dọc, thực hiện **`performClick` trên từng chip**; xác nhận `SessionCommand(OP_GET)` trả đúng duration và Compose chip selected, cuối cùng MediaSession vẫn phát bài QA. Cleanup khôi phục queue, tốc độ, repeat/shuffle, trạng thái phát và thời lượng Crossfade ban đầu.

## Chứng cứ QA

Script tái hiện: `scripts/run-crossfade-ui-interruption-v2-tests.ps1`; log: `qa-logs/crossfade-ui-interruption-v2.log`.

| Nhóm | Kết quả |
|---|---|
| `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` | **BUILD SUCCESSFUL** |
| JVM JUnit XML mới nhất | **105 PASS / 0 FAIL / 0 SKIP** |
| `CrossfadeRuntimeTest` | **8/8 PASS** |
| `CrossfadeUiE2eTest` | **1/1 PASS** |
| `PlaybackServiceIntegrationTest` | **3/3 PASS** |
| `PlaybackQueuePersistenceTest` | **5/5 PASS** |
| **Tổng Android API 30** | **17/17 PASS** |
| Git diff check | **exit 0** |
| Log | **`FINAL_RESULT=PASS`** |

Bằng chứng engine: `ACTIVE_MIX_CANCELS=4`, nghĩa là mỗi trường hợp interrupt đều có **`MIX_CANCELLED stage=MIXING`** thật, không chỉ gọi pause/seek khi chưa bắt đầu trộn. Các bài runtime v1 vẫn có `MIX_STARTED` và `HANDOFF_DONE` ở **1s/2s/3s**; `CROSSFADE_RUNTIME_MARKERS=17`. Bài UI có 5 dấu mốc bắt buộc (`CROSSFADE_UI_E2E_MARKERS=5`). Mỗi instrumentation buộc phải có `OK (N tests)`, không có FAILURES; riêng exit code `adb am instrument = 0` không đủ để đánh dấu đạt.

## Giới hạn và rủi ro chưa khép

- WAV thử nghiệm là PCM **yên lặng**; test xác nhận Media3/MediaSession state và log lifecycle, **không đo sóng âm**, khoảng lặng gapless, clipping, loudness hay chồng âm phát ra loa/tai nghe.
- Chỉ emulator API 30; chưa kiểm thử máy thật, Bluetooth, API 29/33/36, thay đổi speed/repeat/shuffle *giữa* mix hoặc tiến trình bị kill giữa handoff.
- Test UI nhấn bằng `Compose.performClick` sau khi cuộn đúng viewport, **không phải UiDevice raw-touch với màn hình thật**. Các chip nằm trong hàng cuộn ngang; cần real-device UX QA.
- Chưa chạy lại toàn bộ `:app:connectedDebugAndroidTest` sau thay đổi; chỉ 17 Android targeted tests và 105 JVM.
- Không thay quyền nhạc online, Supabase, cloud/Auth hay khả năng phát nhạc offline không cần xét giấy phép streaming.

**Bước tiếp theo (mốc nhỏ): Playback Queue Actions Runtime Acceptance v1** — dùng Library/Search thật để thử `Phát tiếp` và `Thêm vào hàng đợi` trong lúc đang phát WAV offline, kiểm tra vị trí phát/độ ổn định của queue; Google/email thật và cloud E2E hai máy giữ đến gần cuối.
