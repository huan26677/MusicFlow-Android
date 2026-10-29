# Kế hoạch, tiến độ và tiêu chí hoàn thành — MusicFlow Android

> **TÀI LIỆU BÀN GIAO CHÍNH THỨC GIỮA CÁC CUỘC TRÒ CHUYỆN**  
> Đường dẫn trong repository: `docs/Ke_Hoach_Du_An.md`  
> Mỗi lần bắt đầu công việc, **đọc file này trước**, sau đó đối chiếu `main` / commit mới nhất / source code / log trên GitHub. Không tiếp tục dựa trên trạng thái nhớ từ chat nếu đã có bằng chứng mới hơn.

## 0. Tóm tắt nhanh cho cuộc trò chuyện mới

| Mục | Trạng thái |
|---|---|
| HEAD Git hiện tại | `924301d` — Cloud Playlist & Favorites Sync v1 đang ở workspace, chưa commit |
| Session Recovery v1 | Full QA PASS |
| Android API 30 | 55 bài chính PASS |
| Marker API 30 | 40/40 PASS |
| Playback Speed v1 | Đã có source |
| Sleep Timer v1 | Đã có source; đã sửa lỗi MediaSession API; chưa Full QA |
| Lyrics v1 | Triển khai đọc LRC và hiển thị đồng bộ |
| Lyrics import | Android OpenDocument |
| Lưu liên kết LRC theo audio URI | Đã triển khai |
| Build Lyrics v1 + Sleep Timer v1 | `:app:assembleDebug` và `:app:assembleDebugAndroidTest` PASS ngày 2026-10-29; chỉ xác nhận biên dịch |
| Full QA | Để cuối giai đoạn phát triển |
| Equalizer v1 | Đã viết engine theo audio session, UI điều chỉnh dải tần, lưu cấu hình; Build PASS, chưa chạy QA thực tế |
| Crossfade/Gapless v1 | Có mã crossfade thử nghiệm (mặc định tắt) và giữ Media3 gapless tự nhiên; build-only, chưa runtime QA |
| Library nâng cao v1 | Bài hát / Album / Nghệ sĩ / Thể loại; có source, build ứng dụng + AndroidTest PASS, chưa runtime QA |
| Search nâng cao v1 | Tìm bài hát, album, nghệ sĩ, thể loại có bỏ dấu; 4 bộ lọc, mở nhóm/phát/thêm Playlist; build ứng dụng + AndroidTest PASS, chưa runtime QA |
| Online Catalog Foundation v1 | API HTTPS cấu hình được, repository metadata, phân trang và màn khám phá online; build ứng dụng + AndroidTest PASS; chưa có dịch vụ hay QA thực tế |
| Supabase Backend v1 | Dự án Supabase đã kết nối; bảng public.music_tracks RLS; Data API read-only; client Android đọc danh mục và có luồng phát HTTPS hợp lệ; build-only |
| Licensed Content Upload v1 | Hai bucket private/public, music_uploads RLS, RPC publish atomic, admin stage/review/publish; Python unit 8/8 PASS và Android build PASS; chưa upload thật |
| Account & Subscription Foundation v1 | Guest/Free/Premium model; Supabase Auth email/password, profile RLS, entitlement read-only, Android Keystore; build và unit PASS, chưa QA email thật |
| Cloud Playlist & Favorites Sync v1 | Manual opt-in, metadata-only, RPC merge/RLS theo owner, safe local relink; build/JVM PASS, chưa E2E đa thiết bị |
| Cloud Sync Hardening v2 | Trigger quota, tombstone xóa cloud, UI relink thủ công, RPC merge, có source; chưa E2E đa thiết bị |
| Bước tiếp theo | Kiểm tra E2E Auth + đồng bộ 2 thiết bị, rồi History Sync v1 và Google Login |
| Sản phẩm tổng thể | CHƯA NGHIỆM THU |

**LƯU Ý QUAN TRỌNG:** `31/31 PASS` là kết quả của bộ Android tests hiện có ở mốc v2; **không phải** tỷ lệ hoàn thành 100% ứng dụng. Commit HEAD mới nhất có thể chỉ thay đổi tài liệu, không đồng nghĩa một vòng QA mới đã chạy.

## 1. Thông tin dự án và nguồn sự thật

- **Dự án:** MusicFlow Android — ứng dụng nghe nhạc cục bộ trên Android.
- **GitHub:** `huan26677/MusicFlow-Android`; nhánh `main`.
- **Windows workspace:** `E:\Du_An\MusicFlow`.
- **Công nghệ:** Kotlin, Jetpack Compose, Media3 ExoPlayer / MediaSessionService, Room.
- **Cấu hình source đã xem:** `minSdk=29`, `targetSdk=36` trong `app/build.gradle.kts`.
- **Môi trường đã có log PASS:** Android Emulator `Pixel_5`, Android 11 (API 30), thường có serial `emulator-5554`.
- **Script kiểm thử:** `scripts/*.ps1`; **log:** `qa-logs/*.log`.
- **Tài liệu bổ sung:** [`docs/Tong_Quan_Tien_Do_Va_Tieu_Chi_Hoan_Thanh_MusicFlow.md`](Tong_Quan_Tien_Do_Va_Tieu_Chi_Hoan_Thanh_MusicFlow.md) — phạm vi MVP và các cổng nghiệm thu chi tiết.

**Thứ tự ưu tiên bằng chứng:** (1) commit + source và tests thực tế trên GitHub, (2) log QA gắn với vòng thay đổi, (3) tài liệu tiến độ, (4) lời mô tả từ chat trước. Nếu mâu thuẫn: nêu chênh lệch và cập nhật tài liệu, **không tự phán PASS**.

**Cách gắn commit với log:** Một script thường chạy **trước khi** kết quả được commit, nên trường `COMMIT` ghi bên trong log có thể chỉ về commit trước vòng sửa. Cần kiểm tra commit đã đưa chính file log và code tương ứng lên `main`.

## 2. Mục tiêu sản phẩm và phạm vi đề xuất

**MVP đề xuất — còn cần chủ dự án chốt:** người dùng có thể tìm/chọn nhạc cục bộ, xem thư viện, tìm kiếm, tạo playlist, nghe nhạc qua giao diện hoặc MediaSession, xem lịch sử và tiếp tục hàng đợi sau khởi động lại theo quy tắc an toàn. Ứng dụng phải xử lý file thiếu/quyền bị thu hồi mà không crash, không tự phát trái ý muốn hoặc lặp lỗi vô hạn.

Nhóm chức năng cần nghiệm thu của MVP:

1. Quét MediaStore và chọn nhiều file bằng Android file picker; quyền truy cập theo phiên bản Android.
2. Library và Search: hiển thị, tìm kiếm, chọn và bắt đầu phát đúng bài.
3. Player/Mini Player/Now Playing: Play/Pause, Next/Previous, Seek, Shuffle, Repeat, metadata/trạng thái/lỗi.
4. Playlist: tạo, đổi tên, xóa; thêm/xóa/sắp xếp bài; phát theo thứ tự và lưu bền vững.
5. Recently Played: ghi nhận, hiển thị, phát lại, xóa; Room migration không làm mất dữ liệu.
6. Background audio + MediaSession: hoạt động đúng khi app xuống nền, có điều khiển hệ thống/audio focus theo yêu cầu đã chốt.
7. Playback queue: lưu và khôi phục bài, thứ tự, vị trí; không tự phát sau restart; xử lý file thiếu và lỗi runtime.

**Chưa mặc định thuộc MVP:** streaming, tài khoản, cloud sync, lyrics, EQ, widget, Android Auto, monetization và phát hành Google Play. Chỉ bổ sung khi chủ dự án xác nhận. **Chưa có tài liệu yêu cầu sản phẩm đã được chủ dự án ký duyệt** trong những file đã rà soát.

## 3. Những gì đã có trong source — không đồng nghĩa đã nghiệm thu toàn bộ UI

| Nhóm | Source liên quan | Trạng thái |
|---|---|---|
| Home / Recent | `feature/home/*`, `core/history/*` | **Đã triển khai mã** và có test lớp dữ liệu; luồng UI end-to-end cần nghiệm thu |
| Library / MediaStore / chọn file | `feature/library/*`, `core/media/*`, `MainActivity.kt` | **Đã triển khai mã**; quyền/URI theo nhiều API cần kiểm thử bổ sung |
| Search | `feature/search/*` | **Đã triển khai mã**; có unit tests, cần nghiệm thu thao tác UI → phát |
| Player / Mini Player / Now Playing | `feature/player/*`, `core/playback/PlaybackController.kt` | **Đã triển khai mã**; cần nghiệm thu toàn bộ tương tác UI |
| Playlist | `feature/playlist/*`, `core/playlist/*` | **Đã triển khai và có nhiều vòng QA PASS**; cần kiểm thử hành trình người dùng hoàn chỉnh |
| Playback Service | `core/playback/PlaybackService.kt` | **Đã triển khai, có QA Service Integration PASS** |
| Queue persistence / restart | `PlaybackQueueStore.kt`, `PlaybackProcessRestartTest.kt` | **PASS theo phạm vi test hiện có** |
| Queue hardening | `PlaybackQueueRecovery.kt`, `PlaybackQueueHardeningTest.kt` | **PASS theo phạm vi test hiện có** |
| Runtime Auto-Skip | `PlaybackRuntimeAutoSkip.kt`, `PlaybackRuntimeAutoSkipTest.kt` | **PASS v1 + Integration v2**, còn rủi ro race cần kiểm chứng |

Phân biệt ba thuật ngữ: **Đã triển khai** = có code; **QA PASS** = có bài kiểm thử + log cho hành vi cụ thể; **MVP hoàn thành** = mọi tiêu chí MVP được chốt và nghiệm thu đạt.

## 4. Các mốc đã làm và bằng chứng kiểm thử

| Mốc | Android tests ở lần chạy của mốc | Kết quả | Commit / Log |
|---|---:|---|---|
| Playback Error Handling v1 | — | Theo lịch sử QA trước đó; **chưa xác minh log riêng trong đợt rà soát** | Kiểm tra lịch sử khi cần |
| Playback Skip Recovery v1 | 9 | **PASS** | `qa-logs/playback-skip-v1.log` |
| Playlist Reorder v1 | 11 | **PASS** | `qa-logs/playlist-reorder-v1.log` |
| Playlist Playback Order v1 | 12 | **PASS** | `qa-logs/playlist-playback-order-v1.log` |
| Playback Service Integration v1 | 13 | **PASS** | `da8277b` · `qa-logs/playback-service-integration-v1.log` |
| Playback Queue Persistence v1 | 16 | **PASS** | `1f13264` · `qa-logs/playback-queue-persistence-v1.log` |
| Playback Process Restart Recovery v1 | 19 | **PASS** | `07ee7d3` · `qa-logs/playback-process-restart-v1.log` |
| Playback Queue Hardening v1 | 23 | **PASS** | `a3b9ec9` · `qa-logs/playback-queue-hardening-v1.log` |
| Playback Runtime Error Auto-Skip v1 | 28 | **PASS** | `5e4b1c9` · `qa-logs/playback-runtime-auto-skip-v1.log` |
| Playback Runtime Recovery Integration v2 | 31 | **PASS** | `1aec374` · `qa-logs/playback-runtime-recovery-v2.log` |
| Playback Runtime Recovery Race Hardening v3 | 36 | **PASS** | `qa-logs/playback-runtime-recovery-v3.log` — SHA sau khi push cần đối chiếu |
| Favorites + Album + Theme Batch v1 | 44 | **PASS tự động** — JVM PASS, 14/14 marker; SAF thực sau restart còn cần nghiệm thu thủ công | `511f93ea` · `qa-logs/favorites-batch-v1.log` |
| Background Playback & System Controls v1 | 45 | **PASS tự động** — JVM PASS, 18/18 marker | `663e3ba0` · `qa-logs/background-controls-v1.log` |
| Background/System Controls v2 | 47 | **PASS tự động** — JVM PASS, 20/20 marker; notification actions và transient audio focus | `1ab915f5` · `qa-logs/background-controls-v2.log` |
| SAF Permission Persistence v1 | 50 regression + 3 test theo pha | **PASS** — quyền SAF tồn tại sau force-stop, Favorites phát lại thành công | `77c546e9` · `qa-logs/saf-permission-v1.log` |
| Android 13 SAF Compatibility v1 | 50 regression + 3 test theo pha | **PASS** — API 33, SAF persisted read và Favorites playback sau force-stop | `7e2e7b59` · `qa-logs/android13-saf-compat-v1.log` |
| Android 16 Compatibility v1 | 50 regression + 3 test theo pha | **PASS** — API 36, SAF persistence, Favorites playback và background lifecycle | `69bf34f9` · `qa-logs/android16-saf-compat-v1.log` |
| Android 10 Compatibility v1 | 50 regression + 3 test theo pha | **PASS** — API 29, SAF persistence và Favorites playback | `74b9026c` · `qa-logs/android10-saf-compat-v1.log` |
| Real Picker SAF v1 | 51 PASS, 3 test theo pha SKIPPED | **PASS — manual-assisted, API 30** | `19f418ae` · `qa-logs/real-picker-saf-v1.log` |
| SAF Multi-file v1 | 52 PASS, 3 test theo pha SKIPPED | **PASS — Full Regression, API 30** | `94c03895` · `qa-logs/real-picker-saf-v1.log` |
| MVP Playlist Navigation v1 | 55 PASS, 3 test theo pha SKIPPED | **Full QA PASS — API 30** | `18d5893a` · `qa-logs/mvp-playlist-navigation-v1.log` |
| MVP Playlist → History → Replay v1 | 1 bài isolated PASS | **Fast QA PASS — API 30** | `69d5a559` · `qa-logs/mvp-playlist-history-v1.log` |
| Session Recovery UI Acceptance v1 | 55 bài chính PASS | **Full QA PASS — API 30** | `d430cfc7` · `qa-logs/playback-process-restart-v1.log` |

**Không cộng** các con số 9 + 11 + ... + 31: đây là số lượng test của **các lần chạy tăng dần** với nhiều bài trùng nhau.

### 4.1. Phạm vi PASS đã được chứng minh

- **Playlist:** reorder dữ liệu và phát đúng thứ tự theo các test đã viết.
- **Service:** tích hợp MediaController/MediaSession/ExoPlayer với kiểm thử Android thực tế.
- **Persistence:** lưu/khôi phục danh sách, chỉ số, vị trí, shuffle/repeat; bỏ qua JSON hỏng.
- **Restart v1:** `force-stop` và khởi chạy lại với PID thay đổi, khôi phục queue ở trạng thái Pause, phát tiếp được từ vị trí lưu; **không đồng nhất** với hệ thống thu hồi tiến trình vì thiếu RAM.
- **Hardening v1:** loại file `file://` đã bị xóa, mô phỏng quyền `content://` bị từ chối, xóa snapshot hỏng, không tự phát khi khôi phục.
- **Auto-Skip v1:** các trường hợp thiếu file liên tiếp và hết hàng đợi; test tiêm lỗi xác nhận trực tiếp hai nhánh xử lý.
- **Integration v2:** log thật cho thấy `PlaybackService` nhận 7 lỗi và chuyển tiếp cả 7; test Pause/thay queue đang chờ phục hồi đạt PASS.

**Giới hạn:** API 30 là môi trường có log QA đã đọc; quyền URI của document provider thực tế và Android tự thu hồi process chưa được khẳng định PASS; marker của test tiêm lỗi không phải marker production Auto-Skip. Không suy rộng kết quả sang mọi thiết bị / mọi nguồn nhạc.

## 5. Công việc tiếp theo: backlog theo thứ tự ưu tiên

**Chế độ phát triển hiện hành (2026-10-29):** Chủ dự án ưu tiên hoàn thiện tính năng Full Product trước rồi mới chạy vòng QA/bug tổng thể. Các mục P0 QA/Release bên dưới vẫn giữ nguyên để nghiệm thu cuối, không được hiểu là đã PASS khi chỉ build thành công. Supabase Catalog/Streaming, Licensed Content Upload, Account/Auth và Cloud Sync v1 đã có source. **Chưa có nhạc được cấp phép; chưa test tài khoản thật và đồng bộ đa thiết bị E2E**. Feature tiếp theo: Cloud Sync Hardening v2; khi có nhạc hợp lệ thì Licensed Streaming Smoke. Billing, signed URL, podcast/radio còn là backlog.

| Ưu tiên | Bước / việc cần làm | Trạng thái | Điều kiện kết thúc bước |
|---|---|---|---|
| **P0 — đã hoàn thành** | **Playback Runtime Recovery Race Hardening v3** | **PASS — 36/36 Android tests** | Xem `qa-logs/playback-runtime-recovery-v3.log`; yêu cầu 5 marker v3 + regression v2 |
| **P0** | **MVP End-to-End Acceptance v1** — Library/Search/Picker → Player → Playlist/History → restart | CHƯA NGHIỆM THU | Script UI/integration tests có assertions và log cho từng flow |
| **P0** | **Background Playback / System Controls QA** | V2 QA TỰ ĐỘNG PASS (API 30) — còn lockscreen và Bluetooth thực tế | Test lifecycle, audio focus, điều khiển từ MediaSession/notification/lock screen trong phạm vi khả thi; mọi điều kiện đã chốt đạt PASS |
| **P0 — kế tiếp** | **Android Compatibility Matrix v2** | Đang thực hiện | Bộ test tự động trên các API đại diện (29/30/33/36): 55 bài chính + 3 SKIPPED, 40 marker + 4 SAF |
| **P0** | **Release Candidate QA / Packaging** | CHƯA XÁC MINH | Bản release ký đúng, cài mới/nâng cấp, smoke test chạy trên artifact release, checksum và log có thể truy vết theo commit |
| **P1** | UI/accessibility/empty-state/error-state | CHƯA NGHIỆM THU | UI tests hoặc ảnh chụp kiểm tra tự động cho các trạng thái bắt buộc; tiêu chí khả dụng được xác nhận |
| **P1** | Hiệu năng và dữ liệu lớn | CHƯA NGHIỆM THU | Dataset cố định; ngưỡng thời gian/độ ổn định được chốt; không crash/ANR, không mất dữ liệu |
| **Theo quyết định** | Phát hành Google Play | CHƯA CHỐT PHẠM VI | Chỉ mở milestone nếu chọn hình thức phân phối này |

### 5.1. Playback Runtime Recovery Race Hardening v3 — PASS

- Ngày kiểm thử: 2026-10-26.
- Source: `PlaybackRuntimeAutoSkip.kt`.
- Android tests: 36/36 PASS.
- JVM tests: PASS.
- Log: `qa-logs/playback-runtime-recovery-v3.log`.
- Các trường hợp đã kiểm tra:
  - Pause rồi Play trước callback phục hồi.
  - Thay queue bằng nội dung giống hệt.
  - Seek trong cùng bài hát.
  - Dispose khi còn callback chờ.
  - Auto-Skip bình thường khi không có thao tác mới.
- Kiểm thử hồi quy v2: PASS.
- Giới hạn: các bài race v3 dùng lỗi tiêm có kiểm soát; chưa phải nghiệm thu toàn bộ MVP.
- Bước tiếp theo: MVP End-to-End Acceptance v1.

## 6. Định nghĩa hoàn thành (Definition of Done — DoD)

### Cổng A — Chốt mục tiêu MVP

- [ ] Chủ dự án xác nhận phạm vi MVP mục 2, hoặc sửa bằng yêu cầu cụ thể.
- [ ] Quyết định quy tắc playback: resume/Pause sau restart, Skip/Repeat/Shuffle khi lỗi, quyền URI, hỗ trợ Android và phương thức phát hành.
- [ ] Phân loại mọi mục bắt buộc / có thể để sau; không để yêu cầu P0 mở mà không có quyết định.

### Cổng B — Nghiệm thu chức năng

- [ ] **Library, file picker, Search, Player, Playlist, History** đều có kiểm thử tự động theo đúng hành trình người dùng, log lưu vào `qa-logs/`.
- [ ] Các nút phát/tạm dừng/tua/trước/sau/shuffle/repeat và MediaController nhất quán với UI; không phát nhầm bài.
- [ ] Playlist và Recently Played giữ dữ liệu đúng sau mở lại/nâng cấp Room trong các trường hợp đã chốt.
- [ ] Lưu/khôi phục queue, lỗi nguồn nhạc, quyền, file mất và tác vụ Auto-Skip đến muộn đều xử lý an toàn; không vòng lặp, không tự phát trái ý muốn.
- [ ] Không còn lỗi **P0/P1 chặn nghiệm thu** trong phạm vi MVP đã thống nhất.

### Cổng C — Chất lượng & hồi quy

- [x] **Mốc Runtime Recovery Integration v2:** JVM PASS, Android 31/31 PASS, log đã đối chiếu (không có nghĩa toàn bộ cổng C đã hoàn thành).
- [ ] Có test và log PASS cho **race-condition v3** và các lỗi P0 còn lại.
- [ ] **Toàn bộ** test JVM + Android/UI đã chốt PASS trên **chính commit release candidate**, không chỉ một commit cũ.
- [ ] Ma trận phiên bản Android/thiết bị đã chốt PASS hoặc có ngoại lệ được duyệt và ghi rõ rủi ro.
- [ ] Không crash/ANR hoặc mất dữ liệu trong bộ ca nghiệm thu; mỗi lỗi tái hiện có test hồi quy.

### Cổng D — Release & bàn giao

- [ ] Build release đáp ứng phương thức phân phối được chọn (APK/AAB).
- [ ] Có ký release đúng cách, version, SHA commit và checksum artifact.
- [ ] Kiểm thử tự động hoặc quy trình CI có bằng chứng cài mới, nâng cấp và smoke test **trên bản release**.
- [ ] `docs/Ke_Hoach_Du_An.md`, log cuối và danh sách rủi ro/ngoại lệ đã cập nhật.
- [ ] Chủ dự án xác nhận nghiệm thu và quyết định phát hành/bàn giao.

**Khi nào được ghi `DỰ ÁN HOÀN THÀNH`?**

- **Mốc kỹ thuật PASS:** riêng test của mốc thành công; lưu commit, log, marker, phạm vi rõ ràng.
- **MVP HOÀN THÀNH:** toàn bộ **Cổng A + B + C** trong phạm vi được duyệt; không còn P0/P1 chặn; có xác nhận của chủ dự án.
- **SẴN SÀNG PHÁT HÀNH:** đã đạt MVP và **Cổng D** theo phương thức phân phối được chọn.
- **CHƯA HOÀN THÀNH:** bất kỳ cổng bắt buộc nào chưa đủ bằng chứng, dù vòng kiểm thử gần nhất báo `FINAL_RESULT=PASS`.

**Không sử dụng phần trăm hoàn thành tùy đoán.** Đánh giá theo checklist và bằng chứng thực tế.

## 7. Quy trình cố định giữa người dùng và trợ lý

1. **Đầu mỗi chat / đầu mỗi bước:** đọc file này; kiểm tra HEAD và vài commit gần nhất; đọc log của mốc đang làm; xem chính xác source/test/script liên quan. Tiếp tục từ trạng thái thực tế, không làm lại bước PASS.
2. **Lệnh “Kiểm tra tiếp” / “Kiểm tra [mốc]”:** dùng GitHub để đọc commit + log mới. Kết luận PASS chỉ nếu script chạy thành công **và** assertions/markers của mốc đủ; nếu FAIL nêu nguyên nhân, vị trí sửa tối thiểu và script chạy lại. Không che FAIL bằng cách bỏ điều kiện test.
3. **Lệnh “Bước tiếp theo”:** lấy backlog mục 5, đối chiếu mã hiện hành, chọn đúng một milestone; đưa code/test/script cụ thể để người dùng áp dụng.
4. **Kiểm thử:** ưu tiên JVM, Android instrumented tests, Android Emulator qua **một script PowerShell**, một log cố định cho mỗi milestone; không bắt người dùng kiểm thử thủ công. Chạy các test cũ để phát hiện hồi quy. **Chỉ tạo script kiểm thử tổng hợp khi hoàn thành nhóm chức năng lớn**; chức năng đặc thù cần kiểm thử sớm có thể viết test riêng và tích hợp vào script tổng hợp sau.
5. **Quyền thay đổi:** mặc định trợ lý **chỉ đọc/phân tích/chuẩn bị patch hoặc file**. Không tự ý sửa, commit hay push repository nếu người dùng chưa cho phép. Một file tải về không đồng nghĩa repo đã thay đổi.
6. **Lệnh Git:** khi cần hướng dẫn push, **chỉ ba lệnh** dưới đây; không thêm `git status`, `git pull`, `git log`, `git rev-parse`, v.v. vào hướng dẫn cho người dùng:

   ```bash
   git add .
   git commit -m "docs: update MusicFlow project progress and completion criteria"
   git push
   ```

7. **Cập nhật tài liệu:** sau **mỗi** lần thử, sửa lỗi hoặc hoàn thành tính năng, **bắt buộc chuẩn bị bản cập nhật file này trước phần hướng dẫn commit/push**; đồng thời cập nhật tài liệu DoD bổ sung nếu thay đổi tiêu chí nghiệm thu hoặc phạm vi.
8. **Không xóa lịch sử:** mốc PASS cũ giữ trong mục 4/8; vòng đang FAIL ghi rõ vòng lỗi, không sửa lịch sử để giả vờ PASS.
9. **Báo cáo bằng tiếng Việt**: có đường dẫn file, đoạn code sửa, lệnh PowerShell để chạy, log mong đợi, ba lệnh Git và hướng dẫn nhắn “Kiểm tra tiếp”.

### 7.1. Mẫu báo cáo/kết quả một bước

```markdown
### YYYY-MM-DD — <Tên milestone> — PASS | FAIL | ĐANG LÀM
- Commit chứa source/test/log: `<full SHA>`.
- HEAD đối chiếu: `<full SHA>`.
- File sửa / thêm: `<đường dẫn>`.
- Script: `scripts/<script>.ps1`.
- Log: `qa-logs/<log>.log`.
- JVM: PASS/FAIL; Android: x/x PASS hoặc chi tiết FAIL.
- Marker/điều kiện: `<những điều kiện có thể kiểm chứng>`.
- Nguyên nhân FAIL / giới hạn còn lại: `<chi tiết>`.
- Quyết định: `<giữ, sửa, nghiệm thu, không triển khai lại>`.
- Bước tiếp theo: `<một việc cụ thể>`.
```

### 7.2. Những phần bắt buộc cập nhật mỗi vòng

- **Mục 0:** tên mốc gần nhất, PASS/FAIL/ĐANG LÀM, HEAD, commit QA, log, số test, bước kế tiếp.
- **Mục 4:** thêm mốc mới **chỉ khi có PASS**; giữ mốc cũ.
- **Mục 5:** chuyển việc vừa xử lý sang PASS hoặc ghi nguyên nhân FAIL, xác định bước kế tiếp.
- **Mục 6:** chỉ đánh dấu `[x]` một cổng/tiêu chí khi đã có **bằng chứng đúng phạm vi**.
- **Mục 8:** thêm nhật ký mới lên trên; nếu FAIL cũng phải ghi lại.
- **Mục 1/2/7:** sửa khi môi trường, mục tiêu hay quy trình thay đổi.

## 8. Nhật ký thay đổi — giữ lịch sử

### 2026-10-29 — Cloud Sync Hardening v2

- Tiếp tục trên workspace chưa commit của Cloud Sync v1 tại HEAD `924301d`; không ghi đè hoặc tạo commit/push tự động.
- Supabase: migrations `musicflow_cloud_sync_hardening_quotas_tombstones_v2`, `musicflow_cloud_favorite_tombstone_v2`. Trigger server-side giới hạn Free 20 playlist + 500 favorites, Premium 200 + 2500 khi còn hạn; soft-delete RPC và gộp playlist có bảo vệ tombstone.
- Android: UI trong tab Account để ghép các bài chưa khớp, xóa cloud có xác nhận mà không xóa dữ liệu local, phân trang cloud, cải thiện giới hạn phản hồi.
- Bảo mật: RPC chỉ `authenticated`, không cấp cho `anon`; dữ liệu theo owner RLS. Chưa tạo nhạc mẫu hay tài khoản thật.
- Tài liệu `docs/Cloud_Sync_Hardening_v2.md`. Build/log: `qa-logs/cloud-sync-hardening-v2.log`, kiểm chứng thực tế theo kết quả Gradle cuối.
- Còn thiếu: thử thực tế hai thiết bị, mở rộng UI tìm kiếm bài relink và kiểm chứng các race conditions phức tạp. Full QA vẫn để cuối.

### 2026-10-29 — Cloud Playlist & Favorites Sync v1

- HEAD nền: `924301d` (`feat: add Supabase account authentication and subscription foundation`), working tree sạch trước khi sửa.
- Supabase migrations: `musicflow_cloud_playlists_favorites_v1` tạo `cloud_playlists`, `cloud_favorites` với RLS owner; `musicflow_cloud_playlist_merge_rpc_v1` thêm RPC security-invoker, gộp metadata bài khi sync hai thiết bị và không cho anon gọi.
- Đã kiểm tra security grants: anon không đọc cloud tables, không gọi RPC; authenticated không DELETE cloud data. Test RPC với identity giả trong transaction, cập nhật playlist hai lượt, xác nhận 2 bài + tên mới rồi ROLLBACK; dữ liệu cloud không lưu test record.
- Android: `CloudLibrarySync.kt` + `CloudSyncRules.kt`; thêm merge idempotent vào `RoomPlaylistRepository` và `LocalFavoritesStore`, nút thủ công trong tab Account, trạng thái và kết quả sync; refresh Supabase Auth khi JWT hết hạn.
- Chỉ đồng bộ tên + metadata, không upload file hay URI cục bộ; tải về chỉ liên kết bài nếu MediaStore có một ứng viên duy nhất. Không xóa Playlist/Favorites khi merge; chặn dùng cùng local library để upload sang tài khoản khác sau lần đồng bộ đầu.
- JVM `CloudSyncRulesTest.kt`; build `:app:assembleDebug` + `:app:assembleDebugAndroidTest` + `:app:testDebugUnitTest` PASS, log `qa-logs/cloud-sync-v1.log`.
- Chưa xác nhận sync thực tế bằng tài khoản/2 thiết bị; không có cloud quota hay xóa đồng bộ; chưa Full QA.
- Tài liệu: `docs/Cloud_Playlists_Favorites_Sync_v1.md`. Bước tiếp theo: Cloud Sync Hardening v2 / Track Relink UX; sản phẩm CHƯA NGHIỆM THU.

### 2026-10-29 — Account & Subscription Foundation v1 — Build + JVM PASS

- HEAD nền: `e7446bc` (`feat: add secure licensed music upload and rights verification`), working tree sạch lúc bắt đầu.
- Supabase migration `musicflow_account_profiles_entitlements_v1` tạo `account_profiles` và `account_entitlements`; tất cả bật RLS, profile theo `auth.uid() = id`. Chỉ cho sửa `display_name` ở profile; người dùng chỉ SELECT entitlement của mình, không INSERT/UPDATE gói.
- Free mặc định nếu không có entitlement; Premium chỉ còn hiệu lực khi backend có row `premium` và `expires_at` chưa qua. Không kích hoạt thanh toán, không tự nâng quyền từ Android.
- Tạo `core/account/AccountApi.kt` + `AccountTier.kt`, `feature/account/AccountViewModel.kt` + `AccountScreen.kt`, thay tab Account placeholder bằng luồng Auth email/password/hồ sơ, vẫn có tùy chọn theme và Guest offline. Refresh token mã hóa Android Keystore, không đưa secret/service_role vào app.
- JVM `AccountTierTest` kiểm tra Free/Premium theo thời hạn; Supabase grants xác minh anon không đọc profile, authenticated không sửa gói hoặc id, chỉ sửa display_name.
- Build `:app:assembleDebug` + `:app:assembleDebugAndroidTest` và `:app:testDebugUnitTest`: PASS; log `qa-logs/account-subscription-foundation-v1.log`.
- Chưa có tài khoản đăng ký thử thực tế, chưa test email xác nhận, chưa Cloud Sync/Billing; không tuyên bố Auth E2E PASS. Full QA vẫn để cuối.
- Tài liệu: `docs/Account_Subscription_v1.md`; bước tiếp theo Cloud Playlist & Favorites Sync v1; Licensed Streaming Smoke chờ nhạc.

### 2026-10-29 — Licensed Content Upload + Rights Verification v1

- HEAD nền: `66ab690` (`feat: integrate Supabase licensed music catalog and streaming`); workspace sạch trước khi sửa.
- Supabase: tạo hai bucket `musicflow-private-drafts` (private) và `musicflow-public-audio` (public), giới hạn 50 MiB, định dạng audio có danh sách cho phép.
- Tạo `public.music_uploads` lưu staging path/hash/kích thước và bằng chứng xác nhận quyền; bật RLS, **thu hồi toàn bộ quyền anon/authenticated**; không có chính sách cho client. Supabase advisor thông tin `rls_enabled_no_policy` là chủ ý khóa bảng nội bộ.
- RPC `musicflow_publish_licensed_track` dùng SECURITY INVOKER, quyền EXECUTE chỉ dành cho `service_role`, atomically cập nhật receipt và trạng thái public catalog. Kiểm tra grant thực tế: anon không được SELECT receipt/EXECUTE RPC; service role có quyền.
- Công cụ quản trị `scripts/musicflow_licensed_upload.py` hỗ trợ `stage` riêng tư rồi `publish` khi đã xác nhận bản quyền, kiểm tra MIME/magic bytes, SHA-256, kích thước; chỉ nhận secret key trong biến môi trường máy quản trị, không trong Android/Git.
- Bộ kiểm tra offline `scripts/test_musicflow_licensed_upload.py`: **8/8 PASS**; ngăn giả file, không công khai trước xác nhận, từ chối key publishable làm admin, phát hiện thay đổi hash. CLI `--help` PASS trên Windows.
- Đã kiểm tra RPC bằng giao dịch SQL dùng role service_role, xác minh cập nhật cả hai bảng và ROLLBACK; Supabase không lưu nhạc giả. Grant: anon không đọc receipt, không gọi RPC; role service_role có quyền.
- Build `:app:assembleDebug` + `:app:assembleDebugAndroidTest`: PASS, build-only, không chạy instrumentation.
- Tài liệu triển khai `docs/Supabase_Licensed_Upload_v1.md`. Log `qa-logs/licensed-content-upload-v1.log`: `PYTHON_UNIT_EXIT=0`, `ANDROID_BUILD_EXIT=0`, `FINAL_RESULT=BUILD_AND_UNIT_PASS`.
- **Chưa upload hoặc phát nhạc online thực tế** vì không có file + giấy phép + secret key quản trị; không suy diễn runtime PASS. Full QA vẫn để cuối.
- Bước tiếp theo: khi có nhạc hợp lệ → Licensed Streaming Smoke; khi chưa có → Account/Auth Foundation v1. Sản phẩm CHƯA NGHIỆM THU.

### 2026-10-29 — Supabase backend / streaming foundation v1

- HEAD nền: `b310d9f`; không chỉnh dữ liệu nhạc offline đã có.
- Xác minh Supabase project `vzvxpbwrftlnyvuhfoqn` ACTIVE_HEALTHY; schema public ban đầu không có bảng. Tạo bảng `music_tracks` bằng migration `musicflow_licensed_catalog_v1`.
- Bật RLS, anon/authenticated chỉ có SELECT bài `is_published and rights_confirmed` và stream_url hợp lệ; không cho ghi từ client. Supabase security advisors: không có cảnh báo tại thời điểm kiểm tra.
- Lưu cấu hình Supabase ở `secrets.properties` (gitignored); Gradle BuildConfig đưa publishable key vào Android; tuyệt đối không dùng service-role key.
- Thêm `SupabaseCatalogRepository.kt`; hỗ trợ đọc REST catalog có phân trang. `OnlineCatalogViewModel` dùng Supabase khi build đã cấu hình.
- Thêm nút phát cho bản ghi có stream_url HTTPS; queue và MediaSession tiếp tục dùng ExoPlayer chung.
- Chưa thêm dữ liệu hoặc URL nhạc, do chưa có nội dung/quyền phát hợp lệ. Không có bằng chứng phát online thực tế; QA cuối giai đoạn.
- Tài liệu: `docs/Supabase_MusicFlow_v1.md`.
- Build debug + AndroidTest: chạy build-only; log `qa-logs/assemble-debug-supabase-v1.log`.
- Bước tiếp theo: nhập bản ghi nhạc được cấp phép rồi QA streaming; sau đó signed URL/Storage/Auth cho nội dung riêng tư.

### 2026-10-29 — Online Catalog Foundation v1 — Build PASS

- HEAD trước thay đổi: `036b504` (`feat: add advanced search for songs albums artists and genres`); thay đổi ở máy Windows, chưa commit/push.
- Thêm `core/online/OnlineCatalog.kt`: model metadata, interface repository, cài đặt URL gốc HTTPS và implementation bằng `HttpsURLConnection`. Không tích hợp URL nhạc hoặc nguồn không được cấp phép.
- Hợp đồng API: `GET /v1/catalog/tracks` có `items` và `nextCursor`; dùng `cursor` cho phân trang. Tài liệu `docs/Online_Catalog_API_v1.md`.
- Thêm `feature/online/OnlineCatalogViewModel.kt` và `OnlineCatalogScreen.kt`: trạng thái chưa cấu hình/đang tải/rỗng/lỗi, cấu hình máy chủ và tải thêm.
- Kết nối từ Home qua nút `Khám phá nhạc online` và Navigation3 `OnlineCatalogPage`; không chỉnh MediaSession/offline queue.
- Giới hạn: HTTPS, không theo redirect, timeout 10s, mỗi trang tối đa 100 bản ghi/1 MiB, không gửi token, không autoplay hay tải xuống.
- `:app:assembleDebug` + `:app:assembleDebugAndroidTest`: BUILD PASS; log `qa-logs/assemble-debug-online-catalog-v1.log` (build-only; không chạy instrumentation).
- Chưa có endpoint nhà cung cấp thực tế, chưa kiểm thử server/mạng hoặc quyền streaming; chưa Full QA. Sản phẩm tổng thể CHƯA NGHIỆM THU.
- Bước tiếp theo: Licensed Streaming Integration v1 sau khi có backend/API và quyền sử dụng nội dung; nếu chưa có backend, ưu tiên xây hợp đồng backend và dữ liệu quyền phát trước.

### 2026-10-29 — Search nâng cao v1 — Build PASS

- HEAD nền: `a2622f4` (`feat: add artist and genre browsing to local music library`).
- Giữ tìm kiếm bài hát hiện tại (không dấu tiếng Việt, debounce 150ms, kết quả bất đồng bộ, Playlist/Favorites) và nâng cấp màn Search thành bốn bộ lọc: Bài hát / Album / Nghệ sĩ / Thể loại.
- `AdvancedMusicSearch.kt` tạo kết quả theo nhóm từ một lần quét MediaStore, tìm theo tên nhóm có bỏ dấu; `SearchViewModel` cập nhật các nhóm kết quả cùng lúc và bỏ kết quả cũ khi đổi từ khóa.
- `MusicCollectionsContent.kt` thêm nhóm Album để tái sử dụng giao diện mở nhóm, Phát toàn bộ, chọn bài, yêu thích và Thêm Playlist trong Search.
- File thay đổi: `SearchScreen.kt`, `SearchViewModel.kt`, `SongSearch.kt`, `MusicCollectionsContent.kt`; file thêm: `AdvancedMusicSearch.kt`.
- Build ứng dụng và APK instrumentation: `:app:assembleDebug` + `:app:assembleDebugAndroidTest` PASS (`BUILD_EXIT_CODE=0`). Log: `qa-logs/assemble-debug-advanced-search-v1.log`.
- Phạm vi kiểm tra chỉ là biên dịch; chưa kiểm thử thao tác UI với thư viện thật, nhiều metadata, quyền và hiệu năng.
- Chưa commit/push. Bước tiếp theo: Online Catalog Foundation v1. Sản phẩm tổng thể CHƯA NGHIỆM THU.

### 2026-10-29 — Library Collections v1 — Build PASS

- HEAD nền: `0ea2510` (`feat: add experimental crossfade and gapless transition controls`).
- Duy trì tab Album, Favorites, Playlist và các luồng phát nhạc hiện tại.
- Bổ sung tab Nghệ sĩ và Thể loại vào Library; mở nhóm, xem bài và Phát toàn bộ hoặc chọn một bài; nút yêu thích và Thêm Playlist tái sử dụng `SongRow`.
- Mở rộng `DeviceSong`: artistId / genres, giá trị mặc định tương thích nơi tạo mẫu; `DeviceMusicScanner` đọc Artist ID và ánh xạ Genre → các bài qua MediaStore.
- Nếu bảng Genres của nhà cung cấp MediaStore không đọc được, thư viện bài hát vẫn hoạt động; bài không có thể loại được nhóm là `Không rõ thể loại`.
- Nhóm nghệ sĩ ưu tiên Artist ID, dự phòng theo tên; hỗ trợ nhiều thể loại trên cùng bài hát.
- File mới: `feature/library/MusicCollectionsContent.kt`; file thay đổi: `core/media/DeviceMusicScanner.kt`, `feature/library/LibraryScreen.kt`.
- Build-only: `:app:assembleDebug` và `:app:assembleDebugAndroidTest` PASS (`BUILD_EXIT_CODE=0`), log `qa-logs/assemble-debug-library-collections-v1.log`. Chưa chạy instrumentation hay QA hành vi.
- Bước tiếp theo: Search nâng cao v1 — tìm theo bài/album/nghệ sĩ/thể loại. Sản phẩm tổng thể CHƯA NGHIỆM THU.

### 2026-10-29 — Crossfade / Gapless Playback v1 — Build-only

- HEAD trước sửa: `1c57e56`.
- Giữ ExoPlayer/MediaSession và queue persistence làm đường phát chuẩn; Media3 quản lý khoảng nối gapless khi dữ liệu và định dạng hỗ trợ.
- Thêm `PlaybackCrossfade.kt` và `TransitionContract.kt`. Crossfade tùy chọn 1s/2s/3s, mặc định tắt; phát lớp âm thanh tạm cho bài kế tiếp, handoff trở lại ExoPlayer chính và giữ queue/index.
- Chỉ chạy khi phát tuần tự bình thường, repeat/shuffle tắt, tốc độ 1.0x; hủy khi người dùng seek hoặc trạng thái không còn đủ điều kiện. Trở về volume bình thường khi hủy.
- Đã thêm lệnh MediaSession, trạng thái `PlayerUiState`, giao diện Now Playing và callback qua ViewModel/Activity, cập nhật lời gọi trong instrumented test.
- Build debug + build AndroidTest: PASS (chưa kiểm thử phát âm thanh, race conditions hay tác động recovery). Log: `qa-logs/assemble-debug-transition-v1.log`.
- Hạn chế: hai player có thể tạo khác biệt về audio focus/equalizer; cần kiểm thử âm thanh thực trên thiết bị trước khi chấp nhận crossfade. Tắt mặc định để không ảnh hưởng bản local ổn định.
- Bước tiếp theo: Library nâng cao (Album / Artist / Genre). Sản phẩm tổng thể CHƯA NGHIỆM THU.

### 2026-10-29 — Equalizer v1 — Build PASS

- HEAD trước thay đổi: `49603b0`, workspace `E:\Du_An\MusicFlow`.
- Đã thêm `EqualizerContract.kt` và `EqualizerEngine.kt`; hiệu ứng gắn theo audio session của ExoPlayer trong `PlaybackService`.
- Điều khiển Bật/Tắt và từng băng tần trong `NowPlayingScreen`; dải tần và giới hạn gain theo thiết bị, không giả định 5 băng tần.
- Dùng MediaSession custom command để gửi/nhận trạng thái; lưu cấu hình trong SharedPreferences; tự làm mới UI sau khi có audio session.
- Đã cập nhật dây nối `PlaybackController` / `PlayerViewModel` / `MusicFlowApp` / `MainActivity` và lời gọi ở `ExampleInstrumentedTest`.
- Build ứng dụng và APK instrumentation: `:app:assembleDebug` + `:app:assembleDebugAndroidTest` PASS. Log: `qa-logs/assemble-debug-equalizer-v1.log`.
- Giới hạn: chỉ build PASS, chưa có runtime QA xác nhận audio effect thực sự hoạt động trên mọi thiết bị. Nếu thiết bị không hỗ trợ thì UI báo không hỗ trợ.
- Chưa chạy Full Regression theo kế hoạch hoàn thiện chức năng trước; chưa commit/push.
- Bước tiếp theo: Crossfade/Gapless; MVP tổng thể CHƯA NGHIỆM THU.

### 2026-10-29 — Fix MediaSession API, build Lyrics + Sleep Timer v1

- HEAD trước sửa: `0d6dc08` (`feat: add synchronized local LRC lyrics support`).
- Log FAIL ban đầu: `qa-logs/buidfail` và `qa-logs/assemble-debug-sleep-timer.log`.
- Nguyên nhân: Media3 1.10.1 không có callback `onConnectAsync`; `AcceptedResultBuilder` chỉ nhận `session`.
- Sửa `PlaybackService.kt`: dùng `onConnect`, `super.onConnect`, `AcceptedResultBuilder(session)`; bỏ `@OptIn` không có hiệu lực để loại cảnh báo build.
- Kiểm chứng: `:app:assembleDebug` BUILD SUCCESSFUL (9s), `RESULT ASSEMBLE_DEBUG exitCode=0`; `:app:assembleDebugAndroidTest` BUILD SUCCESSFUL (16s), `RESULT ASSEMBLE_DEBUG_ANDROID_TEST exitCode=0`.
- Log build mới: `qa-logs/assemble-debug-lyrics-v1.log`.
- Phạm vi: build-only, chưa kiểm thử chức năng Sleep Timer / Lyrics trên thiết bị, chưa chạy Full QA.
- Code sửa trên máy Windows, **chưa commit/push**; cần đối chiếu lại HEAD khi đẩy GitHub.
- Bước tiếp theo: Equalizer v1 sau khi chốt build.
- MVP / sản phẩm tổng thể: CHƯA NGHIỆM THU.

### 2026-10-28 — Feature Development: Lyrics v1

- HEAD nền: 2b04b1e6.
- Playback Speed v1: đã có source.
- Sleep Timer v1: đã có source, chưa Full QA.
- Lyrics v1: triển khai đọc LRC và hiển thị đồng bộ.
- Lyrics import: Android OpenDocument.
- Lưu liên kết file LRC theo audio URI: đã triển khai.
- Build Lyrics v1: PASS ngày 2026-10-29 sau khi sửa API MediaSession trong Sleep Timer; chỉ kiểm tra biên dịch.
- Full QA: Để cuối giai đoạn phát triển.
- Bước tiếp theo: Equalizer v1.
- Sản phẩm tổng thể: CHƯA NGHIỆM THU.

### 2026-10-28 — Feature Development: Playback Speed & Sleep Timer

- HEAD nền: e16348ac.
- Playback Speed v1: đã có source (chưa QA).
- Sleep Timer v1: đang phát triển.
- Giữ các mốc QA cũ (Session Recovery v1 Full QA PASS, Android API 30 55 bài chính PASS, Marker 40/40 PASS).
- Build debug: FAIL (compile Kotlin — PlaybackService onConnectAsync/AcceptedResultBuilder API). Log: qa-logs/assemble-debug-sleep-timer.log.
- MVP / sản phẩm tổng thể: Chưa nghiệm thu.

### 2026-10-28 — Feature Development — Full Product

- Chuyển sang giai đoạn Feature Development — Full Product.
- HEAD nền: ffe42e7c.
- Hoàn thiện chức năng trước, Full QA cuối giai đoạn.
- Đang triển khai: PLAY-08 Playback Speed v1.
- Các giai đoạn còn lại: theo bảng lộ trình ở trên.
- Không đánh dấu sản phẩm hoàn thành khi chưa có đủ chức năng.
- MVP / sản phẩm tổng thể: Chưa nghiệm thu.

### 2026-10-28 — Android Compatibility Matrix v2

- Baseline: d430cfc7.
- Session Recovery v1: Full QA PASS trên API 30.
- Android 10 / API 29: PENDING QA v2.
- Android 13 / API 33: PENDING QA v2.
- Android 16 / API 36: PENDING QA v2.
- Regression mục tiêu: 55 bài chính, 3 SKIPPED.
- Marker mục tiêu: 40 chức năng + 4 SAF.
- Log API 29: qa-logs/android10-saf-compat-v1.log.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — Session Recovery UI Acceptance v1 — PASS

- Commit: d84c9a11.
- Build APK: PASS.
- History tồn tại và hiển thị sau restart: PASS.
- Playlist và hai bài hát tồn tại sau restart: PASS.
- Không tự phát khi điều hướng UI: PASS.
- PID thay đổi sau force-stop: PASS.
- Resume: PASS.
- Cleanup: PASS.
- Fast QA ba pha: PASS.
- Full Regression: PENDING.
- Log: qa-logs/playback-process-restart-v1.log.
- Bước tiếp theo: Full Regression 55 bài chính, 40 marker.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — MVP Session Recovery v1

- Mốc trước: Playlist → History → Replay Full QA PASS.
- Commit mốc trước: 580681c3.
- Playlist tồn tại sau process restart: PASS ở mốc trước.
- Queue khôi phục ở trạng thái Pause: PASS ở mốc trước.
- History được ghi trước restart: PENDING QA.
- History tồn tại sau restart: PENDING QA.
- Cleanup dữ liệu History: PENDING QA.
- Script: run-playback-process-restart-tests.ps1 -PhasesOnly.
- Log: qa-logs/playback-process-restart-v1.log.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — MVP Playlist → History → Replay v1

- Commit: 69d5a559.
- Playlist → Playback: PASS.
- Ghi bài đầu vào Recently Played: PASS.
- Next cập nhật thứ tự History: PASS.
- History hiển thị tại Home: PASS.
- Phát lại từ History: PASS.
- Fast QA: PASS.
- Full Regression: PENDING.
- Log: qa-logs/mvp-playlist-history-v1.log.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — MVP Playlist Navigation v1

- Commit: 1ae5c36c.
- Home → Playlist → Now Playing: PASS.
- Playlist → MediaSession: PASS.
- Next từ Now Playing: PASS.
- Fast QA: PASS.
- Full Regression: PENDING.
- Log: qa-logs/mvp-playlist-navigation-v1.log.
- Bước tiếp theo: Full Regression 55 bài chính.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — Playlist → Playback → Restart v1

- Commit: 56011698
- QA ba pha: PASS
- Playlist Room: PASS
- Thứ tự playlist: PASS
- Process Restart: PASS
- Queue Recovery: PASS
- Không tự phát sau restart: PASS
- Resume: PASS
- Cleanup: PASS
- Full Regression: PENDING
- MVP: Chưa nghiệm thu

### 2026-10-28 — SAF Multi-file v1

- Fast QA: PASS — Android API 30
- Queue 2 file: PASS
- Thứ tự theo picker: PASS
- Persisted READ: PASS
- Next: PASS
- Full Regression: PENDING
- MVP: Chưa nghiệm thu

### 2026-10-28 — SAF → History v1 — PENDING QA

- Multi-file picker writes second track to recent history: PENDING QA
- Full Regression: PENDING
- MVP: Chưa nghiệm thu

### 2026-10-28 — SAF → History v1

- Commit: d7243128
- Fast QA: PASS
- Chọn 2 file SAF: PASS
- Queue và Next: PASS
- Persisted READ: PASS
- Recently Played (Room): PASS
- Full Regression: PENDING
- MVP: Chưa nghiệm thu


### 2026-10-28 - Real Picker SAF accessibility click diagnosis

- Reviewed main 9231b73a and qa-logs/real-picker-saf-fast-v1.log.
- Compile: PASS. Fast QA: one instrumentation test executed, FAIL.
- Source WAV preparation: PASS; initial Home->Library click: PASS.
- Permissioncontroller not observed; foreground was com.example.musicflow.
- Failure at second Compose UI click (Choose MP3): No compose hierarchies found.
- Test-only change: use clickNamedAction for the Library picker button and log accessibility foreground if it fails.
- Next step: run isolated Fast QA only, assisted with manual DocumentsUI selection when it opens.
- Playback and persisted READ are NOT YET verified. Previous Android compatibility PASS baselines unchanged.
### 2026-10-28 - Real Picker SAF library permission dialog diagnosis

- GitHub main reviewed: dfd8e452084eef3da967ff294ae86fb22fb82e7f.
- Fast QA: Kotlin compile PASS, exactly 1 test executed, final FAIL.
- Actual failure: No compose hierarchies found at second UI action (Choose MP3).
- PASS composeUiReady was logged immediately after Home->Library click.
- LibraryRoute can request READ_EXTERNAL_STORAGE on Android 11.
- Added diagnosis for foreground package and denial of permission prompt to preserve SAF-only testing.
- Fast QA remains the only required run until it PASS; full regression later.
- Playback + persisted READ not yet verified. No MVP/release acceptance.
### 2026-10-28 - Real Picker SAF Compose rule fix

- Reviewed main 19e39627 and qa-logs/real-picker-saf-fast-v1.log.
- FAST QA compile: PASS; 1 Android instrumentation test started.
- FAIL at RealPickerSafTest.kt:387: No compose hierarchies found.
- Prepared change: createAndroidComposeRule<MainActivity>(), remove manual ActivityScenario.launch, synchronize Compose before opening picker.
- Do not change PlaybackService or previous PASS baselines.
- Next run: scripts/run-real-picker-saf-fast-v1-tests.ps1 -CompileOnly; then scripts/run-real-picker-saf-fast-v1-tests.ps1.
- Real Picker SAF is NOT yet PASS; perform full regression only after isolated Fast QA PASS.
### 2026-10-28 — Real Picker SAF v1 — PASS

- Commit: 19f418ae
- JVM: PASS
- Android API 30: 51 PASS, 3 SKIPPED
- Real DocumentsUI: PASS (manual-assisted)
- Playback: PASS
- Persisted READ: PASS
- FINAL_RESULT=PASS
- Ghi nhận riêng: com.android.bluetooth crash trên Emulator
- Chưa nghiệm thu toàn bộ MVP

- Commit: `39f29adaa3452885cdfab26ff49188b598249643`
- Log: `qa-logs/real-picker-saf-v1.log`
- JVM: PASS.
- Android Kotlin Compilation: FAIL.
- Lỗi: RealPickerSafTest.kt:379.
- Nguyên nhân: dấu ngoặc đóng thừa.
- Android instrumentation: chưa chạy.
- DocumentsUI / Playback / Persisted READ:
  chưa xác nhận.
- FINAL_RESULT=FAIL.

Đã hướng dẫn sửa:
- Xóa dấu ngoặc thừa.
- Khôi phục chuỗi "Gần đây" đúng Unicode.
- Biên dịch riêng trước khi chạy QA đầy đủ.

Mốc PASS gần nhất:
Android 10 Compatibility v1.

Bước tiếp theo:
Chạy lại Real Picker SAF v1.

Toàn dự án:
ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP.

### 2026-10-27 — Real Picker SAF v1 — FAIL lần 4

- Commit: `38bba835b76d17a80f83bda33e3bc263e40363ec`
- Log: `qa-logs/real-picker-saf-v1.log`
- Android: API 30.
- JVM: PASS.
- Android instrumentation: 1 test FAIL.
- Tìm đúng android:id/title: PASS.
- Long press bằng lệnh shell: chưa tạo được lựa chọn.
- DocumentsUI vẫn hiển thị sau thao tác.
- Playback và persisted READ: chưa được xác nhận.
- FINAL_RESULT=FAIL.

Đã chuẩn bị:
- Thay shell swipe bằng UiAutomation.injectInputEvent.
- Ghi kết quả DOWN/UP.
- Thêm fallback bấm phần tử cha của tên file.
- Giảm log chứa tên file không thuộc QA.
- Giữ nguyên kiểm tra playback và quyền URI.

Bước tiếp theo:
Chạy lại Real Picker SAF v1.

Mốc PASS gần nhất:
Android 10 Compatibility v1.

Toàn dự án:
ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP/RELEASE.

### 2026-10-27 — Real Picker SAF v1 — FAIL lần 3

- Commit: `c8b10a7c8fbe0f1028f7ed8f71e4c3303df2c047`
- Log: `qa-logs/real-picker-saf-v1.log`
- Android: API 30.
- JVM: PASS.
- Android regression: 1 bài FAIL.
- Tạo tài liệu WAV: PASS.
- DocumentsUI hiển thị file: PASS.
- Nhấn giữ qua Accessibility: thất bại.
- Bấm ngắn: mở YouTube Music thay vì trả về MusicFlow.
- FINAL_RESULT=FAIL.

Nguyên nhân đã xác định:
Hàm tìm file có thể chọn nhầm preview_icon,
do contentDescription cũng chứa tên file.

Đã chuẩn bị sửa:
- Chỉ chọn android:id/title có tên file chính xác.
- Sử dụng thao tác nhấn giữ thực tế.
- Không fallback sang bấm preview.
- Ghi lỗi nếu chuyển sang ứng dụng ngoài.
- Sửa tên AVD trong script kiểm thử.

Bước tiếp theo:
Chạy lại Real Picker SAF v1 và xác nhận
DocumentsUI trả URI về MusicFlow.

Mốc PASS gần nhất: Android 10 Compatibility v1.
Toàn dự án: ĐANG PHÁT TRIỂN, chưa nghiệm thu MVP.

### 2026-10-27 — Real Picker SAF v1 — FAIL lần 2

- Commit: `eef7b07af779b43a5d9aedbb964c144d110888df`
- Log: `qa-logs/real-picker-saf-v1.log`
- Android: API 30.
- JVM: PASS.
- Android regression: 1 test FAIL.
- Tạo file SAF: PASS.
- DOCUMENT_ROW_CLICKED: có.
- PICKER_CONFIRM_CLICKED: có.
- PICKER_NOT_RETURNED_TO_APP: có.
- Playback và persisted READ: chưa xác nhận.
- FINAL_RESULT=FAIL.

Đã chuẩn bị cải thiện bài test:
- Thử nhấn giữ để chọn nhiều tài liệu.
- Xác nhận đúng nút Open/Select.
- Ghi package của cửa sổ đang hiển thị.
- Ghi accessibility hierarchy khi picker không đóng.
- Giữ nguyên assertions playback và persisted READ.

Bước tiếp theo:
Chạy lại Real Picker SAF v1, phân tích log mới.

Mốc PASS gần nhất: Android 10 Compatibility v1.
Trạng thái toàn dự án: CHƯA NGHIỆM THU MVP.

### 2026-10-27 — Real OpenMultipleDocuments SAF v1 — FAIL

- Commit: `ea317255c9a92d143f8de78a4c498309fa06504d`
- Test: `RealPickerSafTest.kt`
- Script: `scripts/run-real-picker-saf-v1-tests.ps1`
- Log: `qa-logs/real-picker-saf-v1.log`
- Android: API 30, Emulator.
- JVM: PASS.
- Android regression: 1 test FAIL.
- Tạo tài liệu SAF thật: PASS.
- Phát nhạc từ DocumentsUI: chưa xác nhận.
- Quyền READ sau picker: chưa xác nhận.
- FINAL_RESULT=FAIL.

Nguyên nhân đang điều tra:
- Kiểm thử so sánh mediaId với URI CreateDocument.
- Chưa xác nhận DocumentsUI thực sự hoàn tất lựa chọn.
- Chưa có log URI và playback state thực tế.

Đã chuẩn bị:
- Kiểm tra DocumentsUI quay về ứng dụng.
- Đối chiếu metadata và nội dung WAV đã chọn.
- Kiểm tra persisted READ trên URI thực tế.
- Bổ sung log trạng thái playback để chẩn đoán.

Bước tiếp theo:
Chạy lại Real OpenMultipleDocuments SAF v1.

Mốc PASS gần nhất:
Android 10 Compatibility v1.

Trạng thái toàn dự án:
ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP/RELEASE.

### 2026-10-27 — Android 10 Compatibility v1 — PASS

- Commit: `74b9026c1cdb70c9d2ed0563bb747d1a047a061a`
- Script: `scripts/run-android10-saf-compat-v1-tests.ps1`
- Log: `qa-logs/android10-saf-compat-v1.log`
- Android: API 29, Pixel 5 Emulator.
- JVM: PASS.
- Android regression: 50 bài PASS.
- Ba bài SAF theo pha: PASS.
- Process restart: PID 8237 → 8373.
- QA markers: 24/24 PASS.
- FINAL_RESULT=PASS.

Ma trận đã có bằng chứng QA tự động:
- Android 10 — API 29: PASS.
- Android 11 — API 30: PASS.
- Android 13 — API 33: PASS.
- Android 16 — API 36: PASS.

Giới hạn:
- Chưa xác nhận chọn file thật qua toàn bộ OpenMultipleDocuments.
- Chưa nghiệm thu Bluetooth và lockscreen trên thiết bị thực.
- Chưa hoàn tất toàn bộ các cổng nghiệm thu MVP.

Bước tiếp theo:
Real OpenMultipleDocuments SAF v1.

Trạng thái toàn dự án:
ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP/RELEASE.

### 2026-10-27 — Android 16 Compatibility v1 — PASS

- Commit: `69bf34f96608e11f9cc537338a2347931ab2f010`
- Script: `scripts/run-android16-saf-compat-v1-tests.ps1`
- Log: `qa-logs/android16-saf-compat-v1.log`
- Android: API 36, Pixel 7 Emulator.
- JVM: PASS.
- Android regression: 50 test PASS.
- SAF PREPARE / VERIFY / CLEANUP: PASS.
- Process restart: PID 9361 → 9547.
- QA markers: 24/24 PASS.
- FINAL_RESULT=PASS.

Đã xác nhận:
- Kiểm thử lifecycle sau phím Home đạt.

### 2026-10-27 — Android 13 SAF Compatibility v1 — PASS

- Commit: `7e2e7b59cb45ed0a7bc702de5a39659aa8bb93bb`
- Log: `qa-logs/android13-saf-compat-v1.log`
- API: 33 — Android 13.
- JVM: PASS.
- Android regression: PASS.
- SAF PREPARE, VERIFY, CLEANUP: PASS.
- PID trước/sau force-stop: 7491 → 8196.
- Markers: 24/24 PASS.
- FINAL_RESULT=PASS.

Giới hạn:
- Chưa kiểm thử OpenMultipleDocuments đầy đủ với tài liệu thật.
- Chưa nghiệm thu thiết bị Bluetooth thật và lockscreen.
- Kết quả không đại diện cho tất cả thiết bị Android 13.

Bước tiếp theo: Android 16 Compatibility v1 (API 36).

### 2026-10-27 — SAF Permission Persistence v1 — PASS

- Commit: `77c546e9a2b2f621f21ae114a240d9b916309735`
- Script: `scripts/run-saf-permission-v1-tests.ps1`
- Log: `qa-logs/saf-permission-v1.log`
- JVM: PASS.
- Android regression: PASS.
- Ba test SAF được chạy theo pha riêng: PASS.
- PREPARE, VERIFY, CLEANUP: PASS.
- Tiến trình mới: PID 15108 → 15251.
- Kiểm tra persisted URI read: PASS.
- Phát lại SAF từ Favorites: PASS.
- FINAL_RESULT=PASS.

Giới hạn:
- Kiểm thử trên Android 11 Emulator.
- Sử dụng ACTION_CREATE_DOCUMENT, chưa nghiệm thu
  trực tiếp toàn bộ OpenMultipleDocuments của MusicFlow.
- Chưa kiểm thử Android 13–16 và thiết bị thực.

Bước tiếp theo:
Android Compatibility Matrix v1 — Android 13, API 33.

### 2026-10-27 — Background/System Controls v2 — PASS

- Commit: `1ab915f572bbf48d67b91a69cc916803fc62ab0e`
- Script: `scripts/run-background-controls-v2-tests.ps1`
- Log: `qa-logs/background-controls-v2.log`
- JVM: PASS.
- Android: 47/47 PASS — Pixel 5 Emulator, Android 11.
- QA markers: 20/20 PASS.
- FINAL_RESULT=PASS.

Kết quả:
- Các nút Play/Pause/Next/Previous trên thông báo hoạt động.
- MusicFlow nhường audio focus khi có yêu cầu tạm thời.
- 45 bài kiểm thử trước đó PASS.
- Không cần thay đổi PlaybackService.

Giới hạn:
- Chưa nghiệm thu điều khiển trực tiếp trên màn hình khóa.
- Chưa kiểm thử tai nghe Bluetooth và thay đổi đầu ra âm thanh thực tế.
- Chưa xác nhận trên Android 13, 14, 15 và 16.
- Emulator có crash ở tiến trình hệ thống com.android.bluetooth;
  không có bằng chứng crash của tiến trình MusicFlow.

Trạng thái:
- Background/System Controls v2: QA tự động PASS.
- Toàn dự án: ĐANG PHÁT TRIỂN, chưa nghiệm thu MVP/Release.

Bước tiếp theo:
- MVP E2E mở rộng: SAF URI permission persistence.
- Kiểm thử Android Compatibility & Permissions Matrix.
- Hoàn thiện nghiệm thu lockscreen/Bluetooth trên thiết bị phù hợp.

### 2026-10-27 — Background Playback & System Controls v1 — PASS

- Commit: `663e3ba08565598ceb46a323bfdc41f2c42b7e43`
- Script: `scripts/run-background-controls-v1-tests.ps1`
- Log: `qa-logs/background-controls-v1.log`
- JVM: PASS.
- Android: 45/45 PASS.
- Marker: 18/18 PASS.
- FINAL_RESULT=PASS.

Đã xác nhận:
- Phát nhạc khi Activity xuống nền.
- Thông báo MediaSession xuất hiện.
- Playback tiếp tục sau khi Activity bị hủy.
- Play/Pause/Next/Previous qua MediaController.
- Các kiểm thử hồi quy trước đó đều PASS.

Giới hạn:
- Chưa kiểm thử thao tác nút trực tiếp trên thông báo/màn hình khóa.
- Chưa nghiệm thu audio focus và tai nghe Bluetooth thật.
- Chưa kiểm thử trên nhiều phiên bản Android.

Bước tiếp theo:
Background/System Controls v2 — lockscreen, notification actions,
audio focus và tình huống ngắt kết nối thiết bị âm thanh.

Trạng thái toàn dự án: ĐANG PHÁT TRIỂN.


### 2026-10-27 — Favorites + Album + Theme Batch v1 — QA TỰ ĐỘNG PASS

- Commit: `511f93ea157aef56b2115d22e15870fba52e87b9`
- Log: `qa-logs/favorites-batch-v1.log`
- Script: `scripts/run-favorites-batch-v1-tests.ps1`
- JVM: PASS.
- Android: 44/44 PASS — Pixel_5, Android 11 (API 30).
- Marker: 14/14 PASS.
- FINAL_RESULT=PASS.

Đã kiểm thử:
- Favorites bài hát và album, lưu dữ liệu cục bộ.
- Lọc Favorites trong Library.
- Điều hướng Favorites từ Home.
- Hiển thị Favorites SAF không cần quyền MediaStore.
- Giao diện tối được duy trì sau khi khởi động lại.
- Hồi quy các chức năng playback và file picker trước đó.

Giới hạn:
- Chưa xác nhận phát lại file SAF thật sau khi khởi động lại.
- Chưa kiểm thử trên ma trận nhiều phiên bản Android.
- Emulator ghi nhận crash ở tiến trình hệ thống com.android.bluetooth;
  không phải crash của MusicFlow, cần theo dõi khi kiểm thử Bluetooth.

Trạng thái: Mốc QA tự động PASS; chưa nghiệm thu toàn bộ MVP.
Bước tiếp theo: Hoàn thiện nghiệm thu SAF và background/system controls.


### 2026-10-26 — MVP E2E v1, lần kiểm thử đầu — FAIL
- Commit: `e4ed4a4accdf31eee03b0290f6c6ccc3bb7899f3`.
- Log: `qa-logs/mvp-e2e-v1.log`.
- JVM: PASS.
- Android: FAIL tại bước biên dịch Kotlin, chưa chạy tests.
- Nguyên nhân: import không hợp lệ `androidx.compose.ui.test.onNode`.
- Cách sửa: xóa dòng import sai trong `ExampleInstrumentedTest.kt`.
- Trạng thái: chờ chạy lại QA.
- Mốc Runtime Recovery v3: giữ nguyên PASS 36/36.

### 2026-10-26 12:41:36 +07:00 — Playback Runtime Recovery Race Hardening v3 — PASS
- Baseline HEAD trước sửa: `e0141a8d7d5d55ca8bf6bf2aa30492e9458db253`.
- HEAD lúc chạy test: `313269e` (SHA commit source/test/log cần đối chiếu sau khi push).
- Mã nguồn: `PlaybackRuntimeAutoSkip.kt`; test: `PlaybackRuntimeRaceHardeningTest.kt`.
- Script: `scripts/run-playback-runtime-recovery-v3-tests.ps1`.
- Log: `qa-logs/playback-runtime-recovery-v3.log`.
- Android: 36 tests; kết quả: **PASS**. JVM và marker chi tiết theo log.
- Kịch bản race: Pause→Play, queue giống hệt, Seek cùng index, dispose, và uninterrupted skip.
- Giới hạn: mới có test chủ động tiêm lỗi cho các race; test Service thực tế cũ là regression. Không coi kết quả này là nghiệm thu MVP.
- Hành động kế tiếp: MVP End-to-End Acceptance v1.


### 2026-10-26 — Runtime Recovery Race Hardening v3 — FAIL môi trường
- Commit chứa thay đổi v3: `da2c5d487ea192c201d41d9168cd53c758098704`.
- Log: `qa-logs/playback-runtime-recovery-v3.log`.
- JVM: CHƯA CHẠY.
- Android: CHƯA CHẠY.
- Kết quả script: `FINAL_RESULT=FAIL`.
- Nguyên nhân: `REQUIRES_ONE_EMULATOR` và lỗi mã hóa Unicode trong script cập nhật kế hoạch.
- Đã chuẩn bị sửa: tự khởi động/chờ Emulator và lưu script PowerShell bằng UTF-8 BOM.
- Bước tiếp theo: chạy lại toàn bộ QA v3, xác nhận 36/36 tests và các marker.
- Giữ nguyên kết quả Playback Runtime Recovery v2 PASS.

### 2026-10-25 — Đối chiếu tiến độ và Definition of Done
- HEAD tại thời điểm đọc GitHub: `6a13defe1fba5cc3231123cee0375aae23b0d341` (`update tien do`).
- Tài liệu sẵn có: `docs/Ke_Hoach_Du_An.md` và `docs/Tong_Quan_Tien_Do_Va_Tieu_Chi_Hoan_Thanh_MusicFlow.md`.
- Xác nhận lại `qa-logs/playback-runtime-recovery-v2.log`: JVM PASS, Android **31/31 PASS**, `SERVICE_ERROR_COUNT=7`, `SERVICE_FORWARDED_COUNT=7`, `SKIP_BRANCH_COUNT=1`, `EXHAUSTED_BRANCH_COUNT=1`, `FINAL_RESULT=PASS`.
- Không có vòng QA mới sau v2 trong những commit đã xem; HEAD mới là cập nhật tài liệu.
- Kết luận **mốc v2 PASS; toàn dự án chưa hoàn thành** vì còn các cổng nghiệm thu MVP/release.
- Bước tiếp theo đề xuất: Runtime Recovery Race Hardening v3; chưa triển khai.

### 2026-10-25 — Playback Runtime Recovery Integration v2 — PASS
- Commit QA: `1aec374d25046c75f39e81cc242f42165e56dc26`.
- Log: `qa-logs/playback-runtime-recovery-v2.log` (log tạo lúc `2026-10-25T15:14:53+07:00`; dòng `COMMIT` trong log là commit trước khi push).
- JVM PASS; Android **31/31 PASS** trên emulator API 30.
- Service nhận và chuyển tiếp **7/7** callback lỗi; test tự xác nhận hai nhánh Skip/Exhausted; tình huống Pause và thay queue lúc phục hồi chờ đạt PASS.
- Giới hạn: còn các race tương đồng queue / Pause→Play nhanh chưa có test xác nhận.

### 2026-10-25 — Playback Runtime Error Auto-Skip v1 — PASS
- Commit: `5e4b1c9386900f948af6297fdc4ad19345f451d8`.
- Log: `qa-logs/playback-runtime-auto-skip-v1.log`.
- JVM PASS; Android **28/28 PASS**; nhánh auto-skip và hết queue kiểm tra qua test chủ động tiêm lỗi; integration có trạng thái phát đúng.

### 2026-10-25 — Playback Queue Hardening v1 — PASS
- Commit: `a3b9ec9d1281f1b3e05c90ecdd1b5de1f0da8e16`.
- Log: `qa-logs/playback-queue-hardening-v1.log`.
- JVM PASS; Android **23/23 PASS**; file thiếu/URI bị từ chối (mô phỏng), snapshot hỏng được xử lý.

### 2026-10-25 — Playback Process Restart Recovery v1 — PASS
- Commit: `07ee7d3cac206baaac3364496d0107ca4ad7ca24`.
- Log: `qa-logs/playback-process-restart-v1.log`.
- JVM PASS; Android **19/19 PASS**; force-stop và tạo tiến trình mới, khôi phục queue Pause và tiếp tục phát đúng vị trí.

### 2026-10-25 — Các mốc nền tảng đã PASS
- Playback Queue Persistence v1: `1f13264`, `qa-logs/playback-queue-persistence-v1.log`, **16 Android tests PASS**.
- Playback Service Integration v1: `da8277b`, `qa-logs/playback-service-integration-v1.log`, **13 Android tests PASS**.
- Playlist Playback Order v1: `qa-logs/playlist-playback-order-v1.log`, **12 Android tests PASS**.
- Playlist Reorder v1: `qa-logs/playlist-reorder-v1.log`, **11 Android tests PASS**.
- Playback Skip Recovery v1: `qa-logs/playback-skip-v1.log`, **9 Android tests PASS**.

## 9. Câu lệnh khởi động cuộc trò chuyện mới

> **Tiếp tục dự án MusicFlow Android.** Đọc file `docs/Ke_Hoach_Du_An.md` trên GitHub `huan26677/MusicFlow-Android`, sau đó kiểm tra HEAD, source và log QA mới nhất. Không làm lại mốc PASS. Hãy tiếp tục milestone đang mở trong backlog, chỉ ra patch Kotlin và một script PowerShell kiểm thử tự động. Sau mỗi lần thử/sửa/hoàn thành, cung cấp cập nhật file kế hoạch (PASS/FAIL, commit, log, việc đã làm, bước tiếp theo) **trước** ba lệnh Git. Không tự sửa hoặc push repository khi chưa được phép. Chỉ đưa ba lệnh Git: `git add .`, `git commit -m "..."`, `git push`.

**File này được duy trì bằng commit**; việc tạo hoặc tải một bản Markdown riêng lẻ không cập nhật GitHub cho đến khi người dùng thực sự thay thế file trong `docs/` và push.

