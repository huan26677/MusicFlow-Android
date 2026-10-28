# Kế hoạch, tiến độ và tiêu chí hoàn thành — MusicFlow Android

> **TÀI LIỆU BÀN GIAO CHÍNH THỨC GIỮA CÁC CUỘC TRÒ CHUYỆN**  
> Đường dẫn trong repository: `docs/Ke_Hoach_Du_An.md`  
> Mỗi lần bắt đầu công việc, **đọc file này trước**, sau đó đối chiếu `main` / commit mới nhất / source code / log trên GitHub. Không tiếp tục dựa trên trạng thái nhớ từ chat nếu đã có bằng chứng mới hơn.

## 0. Tóm tắt nhanh cho cuộc trò chuyện mới

| Mục | Trạng thái đã đối chiếu |
|---|---|
| Tình trạng toàn dự án | **ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP/RELEASE** |
| Mốc kỹ thuật gần nhất | **MVP Session Recovery v1 — Fast QA PASS** |
| JVM unit tests (mốc v2) | **PASS** |
| Android instrumented tests (mốc v2) | **55 regression PASS + 3 test theo pha SKIPPED** — Pixel_5, Android 11 (API 30) |
| Marker | **40/40 PASS** |
| Kết quả log | `FINAL_RESULT=PASS` (MVP Session Recovery v1 Fast QA) |
| Commit chứa vòng QA v2 | `580681c3` |
| Commit HEAD tại thời điểm rà sow | `a1b39367` (MVP Session Recovery v1 — Fast QA PASS) |
| Log gần nhất đã xác nhận | `qa-logs/playback-process-restart-v1.log` |
| Bước đang phát triển tiếp | **Session Recovery UI Acceptance v1 — PENDING QA** |
| Ngày rà Sox | **2026-10-28**, múi giờ Việt Nam (`Asia/Ho_Chi_Minh`) |

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

| Ưu tiên | Bước / việc cần làm | Trạng thái | Điều kiện kết thúc bước |
|---|---|---|---|
| **P0 — đã hoàn thành** | **Playback Runtime Recovery Race Hardening v3** | **PASS — 36/36 Android tests** | Xem `qa-logs/playback-runtime-recovery-v3.log`; yêu cầu 5 marker v3 + regression v2 |
| **P0 — kế tiếp** | **MVP End-to-End Acceptance v1** — Library/Search/Picker → Player → Playlist/History → restart | CHƯA NGHIỆM THU | Script UI/integration tests có assertions và log cho từng flow |
| **P0** | **Background Playback / System Controls QA** | V2 QA TỰ ĐỘNG PASS (API 30) — còn lockscreen và Bluetooth thực tế | Test lifecycle, audio focus, điều khiển từ MediaSession/notification/lock screen trong phạm vi khả thi; mọi điều kiện đã chốt đạt PASS |
| **P0** | **Android Compatibility & Permissions Matrix** | API 29 / 30 / 33 / 36 — QA tự động PASS trong phạm vi hiện có | Bộ test tự động trên các API đại diện, đề xuất **29 / 30 / 33 / 36**, hoặc có ngoại lệ phạm vi được chủ dự án chấp nhận; file picker, MediaStore, URI và quyền đạt PASS |
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

### 2026-10-28 — Session Recovery UI Acceptance v1

- Mốc trước: Session Recovery Fast QA PASS.
- Commit trước: a1b39367.
- History hiển thị trên Home sau restart: PENDING QA.
- Playlist hiển thị sau restart: PENDING QA.
- Hai bài hát còn trong Playlist UI: PENDING QA.
- Điều hướng UI không tự phát nhạc: PENDING QA.
- Resume sau khi xem UI: PENDING QA.
- Log: qa-logs/playback-process-restart-v1.log.
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

