# MusicFlow Android — Tiến độ thực tế và tiêu chí hoàn thành dự án

> **Loại tài liệu:** Phạm vi sản phẩm, trạng thái triển khai, định nghĩa hoàn thành (Definition of Done) và kế hoạch nghiệm thu.
>
> **Vị trí đề xuất trong repository:** `docs/Tong_Quan_Tien_Do_Va_Tieu_Chi_Hoan_Thanh_MusicFlow.md`.
>
> **Ngày đối chiếu:** 2026-10-25 (giờ Việt Nam). **Repository:** [`huan26677/MusicFlow-Android`](https://github.com/huan26677/MusicFlow-Android), nhánh `main`.
>
> **Nguyên tắc:** Đây là ảnh chụp trạng thái và bộ tiêu chí đề xuất để định nghĩa đích đến. `docs/Ke_Hoach_Du_An.md` giữ nhật ký triển khai chi tiết; **commit, source code và log kiểm thử mới nhất** là bằng chứng quyết định. Đọc và đối chiếu lại trước khi thực hiện bước kế tiếp.

## 1. Kết luận ngắn

**Tình trạng tổng thể: ĐANG PHÁT TRIỂN — chưa đủ bằng chứng để tuyên bố toàn bộ dự án hoàn thành.**

- **Đã PASS một mốc quan trọng:** *Playback Runtime Recovery Integration v2*. Log ghi JVM unit tests PASS, **31/31 Android instrumented tests PASS** trên Pixel_5 emulator (Android 11/API 30), `FINAL_RESULT=PASS`.
- **HEAD đã đối chiếu:** `39b5a297a443f92d5e9e32c226d95b3351dde207` (`update`, 2026-10-25 15:33 giờ Việt Nam); commit này bổ sung tài liệu kế hoạch, không phải một vòng QA mới.
- **Commit chứa kết quả QA v2:** `1aec374d25046c75f39e81cc242f42165e56dc26` (`test: verify service error forwarding and pending recovery cancellation`).
- **Log có bằng chứng:** [`qa-logs/playback-runtime-recovery-v2.log`](../qa-logs/playback-runtime-recovery-v2.log). Các marker: `SERVICE_ERROR_COUNT=7`, `SERVICE_FORWARDED_COUNT=7`, `SKIP_BRANCH_COUNT=1`, `EXHAUSTED_BRANCH_COUNT=1`, `ALL_MARKERS_FOUND=True`, `FINAL_RESULT=PASS`.
- **Chưa xác nhận:** toàn bộ chức năng/UX đáp ứng mục tiêu sản phẩm, độ tin cậy trên các phiên bản Android hỗ trợ, bộ ca nghiệm thu cuối, bản dựng phát hành và khả năng phân phối.

**Không dùng con số 31/31 để suy ra ứng dụng đã hoàn thành 100%.** Đó là kết quả của *bộ test hiện có*, không phải phần trăm hoàn thành sản phẩm.

### Phân biệt ba trạng thái

| Trạng thái | Ý nghĩa |
|---|---|
| **Có mã nguồn** | Tính năng đã được viết; chưa mặc nhiên được chứng minh hoạt động đúng. |
| **PASS đã xác minh** | Có commit + kiểm thử phù hợp + log lưu trữ với điều kiện thành công rõ ràng. |
| **Hoàn thành dự án** | Phạm vi được chốt, toàn bộ tiêu chí bắt buộc đạt, không có lỗi chặn, bản phát hành đáp ứng mức giao hàng đã thống nhất. |

## 2. Phạm vi sản phẩm: định nghĩa mục tiêu để có điểm kết thúc

**Mục tiêu MVP đề xuất (cần chủ dự án xác nhận):** MusicFlow là ứng dụng nghe **nhạc cục bộ trên Android**, phát các tệp âm thanh được người dùng chọn hoặc lấy từ thư viện thiết bị, có tìm kiếm, playlist, điều khiển phát và lịch sử nghe. Dữ liệu playlist/lịch sử/hàng đợi phải bền vững trong các tình huống khởi động lại phù hợp, và ứng dụng phải xử lý được bài hát bị xóa hoặc không còn quyền truy cập mà không gây vòng lặp lỗi.

**Trong phạm vi MVP đề xuất:**

1. Duyệt/đọc thư viện âm thanh thiết bị và chọn một hoặc nhiều tệp qua Android file picker; xử lý quyền truy cập theo phiên bản Android.
2. Tìm kiếm bài hát theo tên/nghệ sĩ; phát bài từ Library, Search và các nguồn được hỗ trợ.
3. Mini Player và Now Playing: phát/tạm dừng, bài trước/sau, tua, shuffle, repeat, hiển thị tên bài/nghệ sĩ/trạng thái/lỗi.
4. Playlist: tạo, đổi tên, xóa; thêm/xóa bài, sắp xếp lại thứ tự và phát đúng thứ tự.
5. Lịch sử bài đã nghe: hiển thị, phát lại và xóa theo hành vi đã thiết kế.
6. PlaybackService/MediaSession hoạt động ổn định; lưu/khôi phục hàng đợi và vị trí theo quy tắc xác định.
7. Phục hồi hợp lý sau lỗi mất file/thiếu quyền; không tự phát lại trái ý người dùng hoặc rơi vào vòng lặp vô hạn.

**Không tự bổ sung vào phạm vi MVP** nếu chưa có quyết định: streaming, tài khoản/đồng bộ đám mây, lời bài hát, equalizer, tải nhạc trực tuyến, quảng cáo/trả phí, hỗ trợ Android Auto, widget, hoặc công bố trên Google Play. Các việc này chỉ trở thành điều kiện hoàn thành khi chủ dự án chọn.

> **QUYẾT ĐỊNH CÒN THIẾU:** Chủ dự án chưa có một bản yêu cầu/thiết kế sản phẩm được xác nhận trong repository tại thời điểm rà soát. Phạm vi trên là **đề xuất dựa vào source hiện có**, không phải lời khẳng định mọi yêu cầu ban đầu đã được đáp ứng.

## 3. Những gì đã tồn tại trong source code

| Nhóm | Bằng chứng triển khai đã nhìn thấy | Kết luận |
|---|---|---|
| Library và quyền truy cập | `feature/library/LibraryScreen.kt`, `core/media/DeviceMusicScanner.kt`, `core/media/LocalMusicRepository.kt` | **Có mã** quét MediaStore, xin quyền, quét lại, chọn audio. |
| Tệp do người dùng chọn | `MainActivity.kt` | **Có mã** `OpenMultipleDocuments`, xử lý URI và đọc metadata. |
| Search | `feature/search/SearchScreen.kt`, `SongSearch.kt`, `SearchViewModel.kt` | **Có mã** tìm bài hát; có unit test cho luật tìm kiếm. |
| Player và giao diện | `NowPlayingScreen.kt`, `MiniPlayer.kt`, `PlaybackController.kt` | **Có mã** giao diện và điều khiển phát, tua, shuffle, repeat, lỗi. |
| Playlist | `feature/playlist/PlaylistScreens.kt`, `PlaylistViewModel.kt`, `core/playlist/*` | **Có mã** CRUD playlist, thêm/xóa/sắp xếp bài và phát từ playlist; đã có nhiều test liên quan. |
| Lịch sử nghe | `feature/home/HomeScreen.kt`, `core/history/*`, Room | **Có mã** xem/xóa/phát lại gần đây; có test lớp dữ liệu. |
| Service và khôi phục | `PlaybackService.kt`, `PlaybackQueueStore.kt`, `PlaybackQueueRecovery.kt`, `PlaybackRuntimeAutoSkip.kt` | **Có mã** Media3 service và logic xử lý lỗi/khôi phục. |
| Android nền tảng | `AndroidManifest.xml`, `app/build.gradle.kts` | Khai báo `minSdk=29`, `targetSdk=36`, foreground media playback service; chưa suy ra mọi phiên bản đều đã được test. |

Mục này chỉ ghi nhận **chức năng có trong source**, không chuyển thành PASS nghiệm thu giao diện nếu chưa có test và bằng chứng tương ứng.

## 4. Các mốc đã hoàn thành theo nhật ký kiểm thử

| Mốc | Kết quả từng vòng | File log |
|---|---|---|
| Playback Skip Recovery v1 | 9 Android tests PASS | `qa-logs/playback-skip-v1.log` |
| Playlist Reorder v1 | 11 Android tests PASS | `qa-logs/playlist-reorder-v1.log` |
| Playlist Playback Order v1 | 12 Android tests PASS | `qa-logs/playlist-playback-order-v1.log` |
| Playback Service Integration v1 | 13 Android tests PASS | `qa-logs/playback-service-integration-v1.log` |
| Playback Queue Persistence v1 | 16 Android tests PASS | `qa-logs/playback-queue-persistence-v1.log` |
| Playback Process Restart Recovery v1 | 19 Android tests PASS | `qa-logs/playback-process-restart-v1.log` |
| Playback Queue Hardening v1 | 23 Android tests PASS | `qa-logs/playback-queue-hardening-v1.log` |
| Playback Runtime Error Auto-Skip v1 | 28 Android tests PASS | `qa-logs/playback-runtime-auto-skip-v1.log` |
| Playback Runtime Recovery Integration v2 | **31 Android tests PASS** + JVM PASS | `qa-logs/playback-runtime-recovery-v2.log` |

Đây là **các mốc có số test tăng dần**, không được cộng thành 172 bài test độc lập. Mốc v2 đã có test end-to-end cho các trường hợp thiếu file theo queue, đồng thời có test chủ động tiêm lỗi để kiểm chứng các nhánh phục hồi. Cần phân biệt hai loại bằng chứng.

**Giới hạn hiện có:** `force-stop` khác với hệ thống tự thu hồi process khi thiếu bộ nhớ; mô phỏng thu hồi quyền `content://` không đại diện mọi document provider; 31 Android tests chỉ được đối chiếu từ log chạy trên emulator API 30.

## 5. Lỗ hổng bằng chứng cần đóng trước khi gọi là hoàn thành

Các mục dưới đây là **CHƯA XÁC MINH / cần mở rộng kiểm thử**, **không tự động có nghĩa là FAIL**.

| Mức | Khoảng trống | Bằng chứng cần có |
|---|---|---|
| **P0** | Race condition: Pause rồi Play nhanh khi recovery đang chờ; thay queue bằng nội dung giống queue cũ. | Test tái hiện có kiểm soát lịch thực thi, assert không bị skip ngoài ý muốn; chạy qua đường thực nếu khả thi. |
| **P0** | Toàn bộ hành trình người dùng (chọn file → phát → thao tác queue → thoát/mở lại) chưa có một bộ nghiệm thu cuối được xác nhận. | Instrumented/Compose UI tests hoặc bài test tự động tương đương; log PASS cho từng luồng bắt buộc. |
| **P0** | Hành vi phát nền, thông báo/khóa màn hình và audio focus trên thiết bị/Android đã chọn chưa có kết quả nghiệm thu tổng hợp. | Kiểm thử tự động có assertion cho trạng thái service/controller; phạm vi không thể tự động hóa phải được chấp nhận rõ ràng. |
| **P0** | Tương thích trên các phiên bản Android mục tiêu ngoài API 30 chưa được chứng minh. | Ma trận thiết bị/emulator tối thiểu: API 29, 30, 33 và 36 (hoặc ngoại lệ được phê duyệt). |
| **P0** | Quyền truy cập bị từ chối/thu hồi; URI thực từ nhiều provider; tệp hỏng/đã bị xóa. | Test môi trường thực hoặc fixture tự động sát hệ điều hành, không chỉ tiêm exception. |
| **P1** | Chất lượng giao diện: các trạng thái trống/lỗi, điều hướng, hỗ trợ kích thước màn hình và thao tác nhanh. | Bộ UI assertions/screenshot hoặc tiêu chuẩn nghiệm thu tự động phù hợp. |
| **P1** | Hiệu năng và độ ổn định với thư viện/playlist lớn, nhiều lần khôi phục. | Mốc dữ liệu thử cố định, giới hạn thời gian/tài nguyên được thống nhất, không crash/ANR. |
| **P0 nếu giao bản cài đặt** | Release build, chữ ký, cài đặt sạch/nâng cấp, truy vết phiên bản chưa có bằng chứng trong log đã kiểm. | Artifact phát hành đã ký, cài/cập nhật chạy được, báo cáo smoke test và checksum. |
| **Tùy chọn** | Google Play listing/policy/distribution. | Chỉ áp dụng nếu mục tiêu cuối là xuất bản Play Store. |

**Rủi ro từ đọc tĩnh mã nguồn, chưa phải bug đã tái hiện:** `PlaybackRuntimeAutoSkip` đang kiểm `playWhenReady`, chỉ số hiện tại, `mediaId` và snapshot `(mediaId, uri)` trước khi chạy callback đã xếp lịch. Nếu người dùng Pause rồi Play trước callback, hoặc thay queue bằng danh sách có cùng định danh/URI, điều kiện so sánh có thể không phát hiện được thao tác mới. Phải viết test FAIL tái hiện trước khi kết luận có lỗi và đưa patch nhỏ nhất phù hợp.

## 6. Definition of Done (DoD): khi nào được tuyên bố xong?

### 6.1. Cổng A — Phạm vi được chốt

- [ ] Chủ dự án xác nhận danh sách tính năng **bắt buộc của MVP** ở mục 2 hoặc sửa lại phạm vi.
- [ ] Quy tắc hành vi chưa rõ được quyết định: resume sau khởi động lại, pause/auto-skip, media bị xóa, queue repeat/shuffle, quyền URI, thiết bị hỗ trợ.
- [ ] Phân loại mọi yêu cầu thành **P0 (chặn), P1 (quan trọng), P2 (để sau)**. Không để tính năng “ngầm hiểu” chưa có nghiệm thu.

### 6.2. Cổng B — Chức năng cốt lõi hoạt động đúng

- [ ] Tất cả luồng người dùng trong phạm vi MVP được chạy qua automation với dữ liệu đầu vào đã cố định; có assert đầu ra, không chỉ kiểm tra “không crash”.
- [ ] Phát file từ picker, Library, Search, playlist và lịch sử theo đúng thứ tự đã chọn.
- [ ] Play/Pause/Previous/Next/Seek/Shuffle/Repeat đúng quy tắc; UI và MediaController phản ánh trạng thái thống nhất.
- [ ] CRUD playlist và reorder được lưu bền vững, không tạo bản ghi trùng ngoài ý muốn, không mất dữ liệu sau mở lại.
- [ ] Lịch sử nghe ghi nhận/xóa đúng; migration Room hiện có giữ được dữ liệu qua nâng cấp.
- [ ] Recovery khi gặp nhiều file thiếu/không đọc được không lặp vô hạn; Pause/thay queue/hành động mới không bị callback cũ ghi đè.

### 6.3. Cổng C — Độ tin cậy và hồi quy

- [ ] Toàn bộ JVM unit tests và Android instrumented tests **PASS trong lần chạy cuối trên chính commit chuẩn bị phát hành**.
- [ ] Tất cả bài test cũ được giữ, không sửa/xóa assertion để che lỗi; mọi test mới có đầu ra và marker rõ ràng.
- [ ] Test trực tiếp Media3/PlaybackService cho các đường lỗi quan trọng; nhận diện rõ test tiêm lỗi so với lỗi thực từ framework.
- [ ] Ma trận Android đại diện đạt PASS, hoặc ngoại lệ có ghi rõ rủi ro và người chấp nhận.
- [ ] Không còn P0/P1 chưa giải quyết ở thời điểm khóa bản MVP; P2 được ghi lại để làm sau.
- [ ] Không có crash/ANR hoặc mất dữ liệu trong các ca nghiệm thu đã chốt.

### 6.4. Cổng D — Đóng gói và bàn giao

- [ ] Build `release` thành công, có APK/AAB đúng mục tiêu bàn giao (APK cài nội bộ hoặc AAB để phát hành).
- [ ] Artifact phát hành được ký đúng cách, cài mới và nâng cấp thành công; smoke test trên bản **release** chứ không chỉ `debug`.
- [ ] Có thông tin version/build, commit SHA, checksum artifact, thiết bị/OS kiểm thử và log QA cuối.
- [ ] `docs/Ke_Hoach_Du_An.md` ghi `PASS` cuối cùng, liệt kê giới hạn còn lại; tài liệu này được đánh dấu các checklist bằng bằng chứng kiểm chứng được.
- [ ] Chủ dự án ký duyệt/chấp nhận kết quả và phạm vi. Nếu bàn giao cho Play Store, thêm cổng riêng về phân phối và chính sách tương ứng.

**Quy tắc kết luận:**

- **Hoàn thành kỹ thuật một mốc** = test của mốc PASS và log gắn với commit đúng.
- **Hoàn thành MVP** = Cổng **A + B + C**, không có lỗi P0/P1 mở; xác nhận của chủ dự án.
- **Sẵn sàng phát hành/bàn giao** = Hoàn thành MVP **+ Cổng D** (các điều kiện liên quan đến phương thức phân phối đã chọn).
- Nếu chưa thống nhất phạm vi hoặc chưa chạy nghiệm thu, **không dùng “đã hoàn thành dự án”**, kể cả khi mọi test hiện hành đều PASS.

## 7. Lộ trình ngắn nhất để đi từ hiện tại đến hoàn thành

| Thứ tự | Việc cần làm | Tiêu chí kết thúc | Trạng thái tại 2026-10-25 |
|---|---|---|---|
| **0** | Chốt baseline Playback Runtime Recovery v2 | Log JVM PASS, Android 31/31 PASS, SHA được đối chiếu | **PASS theo log GitHub** |
| **1 — tiếp theo** | Runtime Recovery v3: race-condition hardening | Tái hiện các tình huống Pause→Play nhanh, thay queue trùng nội dung; kiểm thử deterministic + Service integration; đủ marker + hồi quy PASS | **CHƯA BẮT ĐẦU** |
| **2** | Đóng các luồng nghiệm thu MVP và kiểm thử UI/tích hợp | Tất cả flow bắt buộc PASS bằng automation, log lưu `qa-logs/` | **CHƯA XÁC NHẬN** |
| **3** | Ma trận tương thích, quyền URI thực, phát nền/điều khiển hệ thống | PASS hoặc có ngoại lệ/giới hạn được duyệt | **CHƯA XÁC NHẬN** |
| **4** | Release candidate và bàn giao | Build release, ký, cài/nâng cấp, smoke test, tài liệu, chấp nhận | **CHƯA XÁC NHẬN** |

**Không tái triển khai** các chức năng đã có test PASS trừ khi phát hiện lỗi hồi quy hoặc thay đổi yêu cầu. Với mỗi vấn đề mới: kiểm tra code hiện hành → tìm nguyên nhân và bài test tái hiện → sửa tối thiểu → chạy test cũ/mới → lưu log → cập nhật tài liệu → mới hướng dẫn commit.

## 8. Cách lưu bằng chứng và cập nhật tài liệu sau từng bước

Sau một vòng làm việc, **luôn cập nhật `docs/Ke_Hoach_Du_An.md`**, và khi liên quan đến mức hoàn thành MVP/release thì cập nhật checklist của file này:

1. Đối chiếu HEAD `main`, commit có thay đổi source/test và file log đi kèm; không dựa chỉ vào một dòng `COMMIT` của script đã chạy trước khi tạo commit.
2. Ghi `PASS`, `FAIL` hoặc `ĐANG LÀM`; tên bước; SHA; đường dẫn log; số bài test; marker; điều kiện test; giới hạn; việc kế tiếp.
3. Nếu FAIL: giữ lịch sử PASS cũ, nêu lỗi hiện hành và bản sửa tối thiểu; không đánh dấu mốc mới PASS.
4. Nếu PASS: bổ sung mục lịch sử và liên kết từ DoD tương ứng tới bằng chứng thực tế.
5. **Không tự sửa/push repository khi chưa được phép.** Chuẩn bị nội dung file để chủ dự án áp dụng. Khi cần hướng dẫn Git, chỉ xuất đúng ba lệnh sau, không thêm lệnh Git khác:

```bash
git add .
git commit -m "docs: define MusicFlow completion criteria and project status"
git push
```

**Lưu ý:** Ba lệnh trên chỉ dùng *sau khi* file Markdown thực sự được đặt vào `docs/` và người dùng quyết định commit/push. Việc tạo tài liệu tải xuống không đồng nghĩa GitHub đã được cập nhật.

## 9. Mẫu biên bản kết thúc (chỉ điền khi đã đủ bằng chứng)

```markdown
### MusicFlow Android — Nghiệm thu MVP/Release
- Ngày: YYYY-MM-DD
- Loại nghiệm thu: MVP | RELEASE
- Commit SHA: <full SHA của mã nguồn thực sự nghiệm thu>
- Phạm vi được chấp thuận: <liên kết / phiên bản yêu cầu>
- JVM tests: <x/x PASS; log>
- Android instrumented/UI tests: <x/x PASS; log>
- Ma trận phiên bản Android: <API/thiết bị/kết quả; log>
- Tình huống lỗi và phục hồi: <test + kết quả>
- Release artifact: <tên, version, SHA-256, ký, cài đặt/nâng cấp>
- P0 còn mở: 0
- P1 còn mở: 0
- Ngoại lệ được chấp nhận: <danh sách hoặc không có>
- Kết luận: PASS | FAIL | CHƯA ĐỦ BẰNG CHỨNG
- Người chấp thuận: <chủ dự án>
- Bước tiếp theo: <bàn giao / backlog v1.1 / xử lý lỗi>
```

---

### Liên kết bằng chứng chính

- [Kế hoạch và nhật ký dự án](Ke_Hoach_Du_An.md)
- [HEAD tài liệu tại thời điểm đối chiếu `39b5a297`](https://github.com/huan26677/MusicFlow-Android/commit/39b5a297a443f92d5e9e32c226d95b3351dde207)
- [Commit QA v2 `1aec374d`](https://github.com/huan26677/MusicFlow-Android/commit/1aec374d25046c75f39e81cc242f42165e56dc26)
- [Log v2](../qa-logs/playback-runtime-recovery-v2.log)
- [Test Auto-Skip hiện có](../app/src/androidTest/java/com/example/musicflow/core/playback/PlaybackRuntimeAutoSkipTest.kt)
- [Logic Auto-Skip hiện có](../app/src/main/java/com/example/musicflow/core/playback/PlaybackRuntimeAutoSkip.kt)

*Tài liệu này không thay thế kết quả kiểm thử. Khi GitHub có commit mới, phải kiểm tra lại trước khi tiếp tục.*
