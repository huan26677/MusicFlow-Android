# Kế hoạch và nhật ký dự án MusicFlow Android

> **Tài liệu bàn giao giữa các cuộc trò chuyện.** Mỗi khi bắt đầu một chat mới trong dự án, đọc tài liệu này, sau đó **kiểm tra lại commit mới nhất và log tương ứng trên GitHub** trước khi kết luận trạng thái. Không giả định tài liệu luôn mới hơn GitHub.

## 1. Thông tin dự án

| Mục | Giá trị |
|---|---|
| Tên | MusicFlow Android |
| Repository | `huan26677/MusicFlow-Android` |
| Nhánh chính | `main` |
| Thư mục trên Windows | `E:\Du_An\MusicFlow` |
| Công nghệ | Kotlin, Android, Jetpack Compose, Media3 ExoPlayer/MediaSession, Room |
| Thiết bị QA | Pixel_5 emulator, Android 11 (API 30), thường là `emulator-5554` |
| Thư mục script | `scripts/` |
| Thư mục log | `qa-logs/` |
| Ngày rà soát tài liệu | 2026-10-25 (giờ Việt Nam) |

## 2. Trạng thái hiện tại (cập nhật sau mỗi bước)

- **Bước gần nhất:** Playback Runtime Recovery Integration v2.
- **Trạng thái:** **PASS** theo log đã kiểm tra trên GitHub.
- **Commit gần nhất đã xác nhận:** `1aec374d25046c75f39e81cc242f42165e56dc26` — `test: verify service error forwarding and pending recovery cancellation`.
- **Log:** `qa-logs/playback-runtime-recovery-v2.log`.
- **JVM unit tests:** PASS.
- **Android instrumented tests:** **31/31 PASS**.
- **Chi tiết:** `SERVICE_ERROR_COUNT=7`, `SERVICE_FORWARDED_COUNT=7`, `SKIP_BRANCH_COUNT=1`, `EXHAUSTED_BRANCH_COUNT=1`, `ALL_MARKERS_FOUND=True`, `FINAL_RESULT=PASS`.
- **Chưa bắt đầu bước mới.** Khi người dùng nói “Bước tiếp theo”, cần xác định tính năng tiếp theo từ trạng thái GitHub thực tế. Ưu tiên tăng cường bằng chứng end-to-end rằng Service thực hiện Auto-Skip trong tình huống lỗi thật, kiểm tra race condition/đổi queue/Pause nếu cần; không tuyên bố đã hoàn thành các kiểm thử chưa chạy.
- **Lưu ý:** Dòng `COMMIT` nằm trong log có thể là commit trước đó vì script chạy **trước khi** người dùng commit và push kết quả. Đối chiếu commit chứa file log thay vì kết luận log sai.

## 3. Các hạng mục đã xác nhận

| Hạng mục | Kết quả gần nhất | Log |
|---|---|---|
| Playback Error Handling v1 | PASS theo QA trước đó | Kiểm tra lịch sử nếu cần |
| Playback Skip Recovery v1 | PASS — 9 Android tests | `qa-logs/playback-skip-v1.log` |
| Playlist Reorder v1 | PASS — 11 Android tests | `qa-logs/playlist-reorder-v1.log` |
| Playlist Playback Order v1 | PASS — 12 Android tests | `qa-logs/playlist-playback-order-v1.log` |
| Playback Service Integration v1 | PASS — 13 Android tests | `qa-logs/playback-service-integration-v1.log` |
| Playback Queue Persistence v1 | PASS — 16 Android tests | `qa-logs/playback-queue-persistence-v1.log` |
| Playback Process Restart Recovery v1 | PASS — 19 Android tests, kiểm thử `force-stop` và khởi động lại | `qa-logs/playback-process-restart-v1.log` |
| Playback Queue Hardening v1 | PASS — 23 Android tests | `qa-logs/playback-queue-hardening-v1.log` |
| Playback Runtime Error Auto-Skip v1 | PASS — 28 Android tests | `qa-logs/playback-runtime-auto-skip-v1.log` |
| Playback Runtime Recovery Integration v2 | PASS — 31 Android tests | `qa-logs/playback-runtime-recovery-v2.log` |

**Giới hạn bằng chứng:** Thử nghiệm `force-stop` không đồng nghĩa kiểm thử Android tự thu hồi process vì thiếu RAM. Quyền `content://` bị thu hồi đã được kiểm thử bằng mô phỏng, chưa phải mọi document provider thực. Các bài kiểm thử có thể dùng lỗi chủ động tiêm vào để kiểm tra nhánh xử lý; phải phân biệt với bằng chứng callback từ ExoPlayer/PlaybackService thật.

## 4. Quy tắc phối hợp bắt buộc

1. **Đầu mỗi chat:** đọc `docs/Ke_Hoach_Du_An.md`, sau đó đọc commit mới nhất trên nhánh `main`, log QA của bước đang làm và những file Kotlin/PowerShell liên quan. Nếu tài liệu và GitHub khác nhau, ưu tiên bằng chứng mới nhất và ghi lại chênh lệch.
2. **Khi người dùng yêu cầu “Kiểm tra tiếp” hoặc “Kiểm tra [tên bước]”:** kiểm tra trực tiếp GitHub. Chỉ kết luận PASS khi log và điều kiện kiểm thử xác nhận; nếu FAIL, nêu nguyên nhân và cung cấp đoạn sửa tối thiểu ở đúng đường dẫn.
3. **Khi người dùng nói “Bước tiếp theo”:** đề xuất và hướng dẫn một bước phát triển kế tiếp phù hợp với kiến trúc hiện tại. Xem source thực tế trước khi viết patch; tránh làm lại chức năng đã có.
4. **Kiểm thử tự động:** dùng Kotlin JVM/Android instrumented tests và **một script PowerShell** để chạy kiểm thử, thu log cố định ở `qa-logs/`. Không yêu cầu thao tác kiểm thử thủ công trên emulator. Giữ các bài kiểm thử cũ để kiểm tra hồi quy.
5. **Không tự sửa/push repository nếu chưa được người dùng cho phép.** Mặc định trợ lý đọc GitHub, phân tích, đưa mã và hướng dẫn để người dùng áp dụng. Nếu được cho phép thao tác trực tiếp, phải báo rõ những gì đã sửa.
6. **Chỉ đưa đúng ba lệnh Git cho người dùng** khi cần commit/push. Không thêm lệnh Git thứ tư vào phần hướng dẫn push:

   ```bash
   git add .
   git commit -m "mo ta thay doi"
   git push
   ```

7. **Sau mỗi lần làm/kiểm tra:** cập nhật file này **trước khi hướng dẫn commit**, hoặc cung cấp chính xác nội dung để người dùng cập nhật. Không coi tài liệu đã được lưu lên GitHub nếu người dùng chưa push hoặc trợ lý chưa có quyền chỉnh sửa và xác nhận thành công.
8. **Không xóa thông tin tiến độ cũ**; chuyển kết quả vào lịch sử. Khi có lỗi mới, đánh dấu bước hiện hành FAIL/đang sửa thay vì ghi đè các lần PASS trước đó.
9. Giao tiếp bằng **tiếng Việt**, chỉ dẫn rõ đường dẫn file, đoạn thay thế, lệnh PowerShell chạy test; tránh yêu cầu người dùng nhắc lại thông tin đã có trong tài liệu.

## 5. Quy trình cập nhật tài liệu sau mỗi vòng làm việc

Khi có thay đổi mới, cập nhật **tối thiểu** những phần sau:

- `## 2. Trạng thái hiện tại`: tên bước, PASS/FAIL/ĐANG LÀM, SHA commit đã xác nhận, file log, số bài test, nguyên nhân nếu lỗi, hành động tiếp theo.
- `## 3. Các hạng mục đã xác nhận`: thêm dòng mới **chỉ khi** bước đã PASS.
- `## 6. Nhật ký thay đổi`: thêm một mục có ngày, commit, log, kết quả và quyết định.
- Nếu thay đổi quy trình hoặc môi trường, cập nhật `## 1` hoặc `## 4` tương ứng.

### Mẫu ghi nhật ký cho bước kế tiếp

```markdown
### YYYY-MM-DD — <Tên bước>
- Commit kiểm tra: `<SHA>`.
- Log: `qa-logs/<ten-log>.log`.
- JVM: PASS/FAIL. Android: X/X PASS hoặc lỗi chi tiết.
- Kết luận: PASS/FAIL/ĐANG LÀM.
- Vấn đề còn lại: <nếu có>.
- Hành động tiếp theo: <một bước cụ thể>.
```

## 6. Nhật ký thay đổi

### 2026-10-25 — Playback Runtime Recovery Integration v2
- Commit kiểm tra: `1aec374d25046c75f39e81cc242f42165e56dc26`.
- Log: `qa-logs/playback-runtime-recovery-v2.log`.
- JVM: PASS. Android: **31/31 PASS**.
- Callback Service: 7 lỗi được nhận, 7 lỗi được chuyển đến bộ xử lý.
- Hai nhánh phục hồi chủ động kiểm tra: Auto-Skip = 1, Queue Exhausted = 1.
- Kiểm thử hủy phục hồi khi Pause hoặc thay queue: PASS.
- Kết luận: **PASS**; chờ lựa chọn bước kế tiếp.

### 2026-10-25 — Playback Runtime Error Auto-Skip v1
- Commit xác nhận: `5e4b1c9386900f948af6297fdc4ad19345f451d8`.
- Log: `qa-logs/playback-runtime-auto-skip-v1.log`.
- JVM: PASS. Android: **28/28 PASS**.
- Bài test tiêm lỗi xác nhận hai nhánh xử lý và kiểm tra không phát lại khi Pause.
- Kết luận: **PASS** trong phạm vi v1.

### 2026-10-25 — Playback Queue Hardening v1
- Commit xác nhận: `a3b9ec9d1281f1b3e05c90ecdd1b5de1f0da8e16`.
- Log: `qa-logs/playback-queue-hardening-v1.log`.
- JVM: PASS. Android: **23/23 PASS**.
- Xử lý file không còn tồn tại, URI bị từ chối (mô phỏng), snapshot sai định dạng.
- Kết luận: **PASS**.

### 2026-10-25 — Playback Process Restart Recovery v1
- Commit xác nhận: `07ee7d3cac206baaac3364496d0107ca4ad7ca24`.
- Log: `qa-logs/playback-process-restart-v1.log`.
- JVM: PASS. Android: **19/19 PASS**.
- Kiểm thử force-stop, khôi phục queue tạm dừng, phát tiếp từ vị trí lưu; PID trước/sau khác nhau.
- Kết luận: **PASS** trong phạm vi force-stop.

## 7. Cách bắt đầu cuộc trò chuyện mới

Gửi đúng yêu cầu sau trong cuộc trò chuyện mới của cùng dự án:

> Đọc `docs/Ke_Hoach_Du_An.md` trong repository `huan26677/MusicFlow-Android` trên GitHub, kiểm tra commit và log mới nhất rồi tiếp tục đúng bước đang làm. Sau mỗi lần kiểm thử hoặc hoàn thành một bước, hãy chuẩn bị cập nhật file kế hoạch trước khi hướng dẫn tôi commit và push. Giữ nguyên quy trình chỉ ba lệnh Git.

**Nguyên tắc:** Tài liệu lưu ngữ cảnh; **GitHub và log** xác nhận kết quả thực tế. Một file Markdown riêng lẻ không tự cập nhật nếu không có người hoặc công cụ sửa và commit nó.
