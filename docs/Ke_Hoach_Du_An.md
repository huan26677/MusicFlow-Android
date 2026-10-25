# Kế hoạch, tiến độ và tiêu chí hoàn thành — MusicFlow Android

> **TÀI LIỆU BÀN GIAO CHÍNH THỨC GIỮA CÁC CUỘC TRÒ CHUYỆN**  
> Đường dẫn trong repository: `docs/Ke_Hoach_Du_An.md`  
> Mỗi lần bắt đầu công việc, **đọc file này trước**, sau đó đối chiếu `main` / commit mới nhất / source code / log trên GitHub. Không tiếp tục dựa trên trạng thái nhớ từ chat nếu đã có bằng chứng mới hơn.

## 0. Tóm tắt nhanh cho cuộc trò chuyện mới

| Mục | Trạng thái đã đối chiếu |
|---|---|
| Tình trạng toàn dự án | **ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP/RELEASE** |
| Mốc kỹ thuật gần nhất | **Playback Runtime Recovery Integration v2 — PASS** |
| JVM unit tests (mốc v2) | **PASS** (`exitCode=0`) |
| Android instrumented tests (mốc v2) | **31/31 PASS** — Pixel_5 emulator, Android 11 / API 30 |
| Bằng chứng Service nhận/chuyển lỗi | `SERVICE_ERROR_COUNT=7`, `SERVICE_FORWARDED_COUNT=7` |
| Nhánh phục hồi được kiểm tra | `SKIP_BRANCH_COUNT=1`, `EXHAUSTED_BRANCH_COUNT=1` |
| Kết quả log | `ALL_MARKERS_FOUND=True`, `FINAL_RESULT=PASS` |
| Commit chứa vòng QA v2 | `1aec374d25046c75f39e81cc242f42165e56dc26` |
| Commit HEAD tại thời điểm rà soát | `6a13defe1fba5cc3231123cee0375aae23b0d341` (`update tien do`) |
| Log gần nhất đã xác nhận | `qa-logs/playback-runtime-recovery-v2.log` |
| Bước đang phát triển tiếp | **Chưa bắt đầu**. Ưu tiên **Playback Runtime Recovery Race Hardening v3** |
| Ngày rà soát | **2026-10-25**, múi giờ Việt Nam (`Asia/Ho_Chi_Minh`) |

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
| **P0 — kế tiếp** | **Playback Runtime Recovery Race Hardening v3** | **CHƯA BẮT ĐẦU** | Test có kiểm soát cho Pause→Play nhanh, thay queue trùng `mediaId`/URI, lệnh đến muộn, dispose; không skip sai; toàn bộ test cũ + mới PASS; log riêng `FINAL_RESULT=PASS` |
| **P0** | **MVP End-to-End Acceptance v1** — các hành trình Library/Search/Picker → Player → Playlist/History → đóng/mở app | CHƯA NGHIỆM THU | Script tự động chạy UI/integration tests, assert đúng dữ liệu + hành vi + không crash; bằng chứng log cho từng flow bắt buộc |
| **P0** | **Background Playback / System Controls QA** | CHƯA NGHIỆM THU TỔNG HỢP | Test lifecycle, audio focus, điều khiển từ MediaSession/notification/lock screen trong phạm vi khả thi; mọi điều kiện đã chốt đạt PASS |
| **P0** | **Android Compatibility & Permissions Matrix** | CHƯA XÁC MINH | Bộ test tự động trên các API đại diện, đề xuất **29 / 30 / 33 / 36**, hoặc có ngoại lệ phạm vi được chủ dự án chấp nhận; file picker, MediaStore, URI và quyền đạt PASS |
| **P0** | **Release Candidate QA / Packaging** | CHƯA XÁC MINH | Bản release ký đúng, cài mới/nâng cấp, smoke test chạy trên artifact release, checksum và log có thể truy vết theo commit |
| **P1** | UI/accessibility/empty-state/error-state | CHƯA NGHIỆM THU | UI tests hoặc ảnh chụp kiểm tra tự động cho các trạng thái bắt buộc; tiêu chí khả dụng được xác nhận |
| **P1** | Hiệu năng và dữ liệu lớn | CHƯA NGHIỆM THU | Dataset cố định; ngưỡng thời gian/độ ổn định được chốt; không crash/ANR, không mất dữ liệu |
| **Theo quyết định** | Phát hành Google Play | CHƯA CHỐT PHẠM VI | Chỉ mở milestone nếu chọn hình thức phân phối này |

### 5.1. Đặc tả bước thực hiện ngay — Runtime Recovery Race Hardening v3

**Lý do:** `PlaybackRuntimeAutoSkip` hiện so sánh `playWhenReady`, chỉ số và snapshot `(mediaId, uri)` trước lúc xử lý callback đã xếp lịch. Một chuỗi thao tác **Pause→Play** hoặc thay queue bằng **nội dung y hệt** có thể giữ nguyên các giá trị này dù phiên phát đã thay đổi. Đây là **rủi ro nhận thấy khi đọc mã**, **chưa phải lỗi đã tái hiện**.

**Quy trình:**

1. Đọc lại HEAD, `PlaybackRuntimeAutoSkip.kt`, `PlaybackService.kt`, `PlaybackRuntimeAutoSkipTest.kt` và log v2.
2. Viết test deterministic **tái hiện trước**: giữ callback lỗi trong hàng chờ, Pause→Play rồi thả callback; thay queue bằng cùng bài/cùng URI rồi thả callback; thay chỉ số/seek nếu liên quan; dispose trước khi xử lý.
3. Nếu test chứng minh lỗi, sửa nhỏ nhất có thể (ví dụ theo dõi phiên bản/đời queue hoặc thế hệ yêu cầu phát) và bảo đảm không ảnh hưởng phát nhạc bình thường. **Không mặc định bug tồn tại khi chưa có test.**
4. Chạy JVM + toàn bộ Android instrumented tests qua **một script PowerShell**, xuất `qa-logs/playback-runtime-recovery-v3.log` và marker PASS/FAIL cụ thể.
5. Sau khi log được kiểm tra: cập nhật mục 0, bảng mốc, backlog và nhật ký ở cuối file này rồi **mới** hướng dẫn người dùng commit/push.

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
4. **Kiểm thử:** ưu tiên JVM, Android instrumented tests, Android Emulator qua **một script PowerShell**, một log cố định cho mỗi milestone; không bắt người dùng kiểm thử thủ công. Chạy các test cũ để phát hiện hồi quy.
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
