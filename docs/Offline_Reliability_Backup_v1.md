# MusicFlow — Offline Reliability & Local Backup v1

**Ngày triển khai:** 2026-11-07. **Phạm vi:** APK sideload phục vụ báo cáo, không phát hành production, không cloud, không thu tiền. Nền: `main` commit `6dd8e9b`, Ocean Flow v1 đã nghiệm thu.

## 1. Chức năng mới

### Kiểm tra và sửa bài trong playlist

Mở **Playlist → chọn playlist → Kiểm tra tệp còn đọc được**. Ứng dụng chỉ kiểm tra khi người dùng bấm: mở descriptor đọc từ `content://`, hoặc kiểm tra sự tồn tại/quyền đọc của `file://`/tệp cục bộ. Bài không đọc được được đánh dấu ⚠; **Phát toàn bộ** sau khi kiểm tra sẽ bỏ qua các mục thiếu quyền, còn bấm bài không đọc được sẽ hướng dẫn chọn lại tệp.

Ở bài cần sửa, chọn **Tùy chọn bài hát → Chọn lại tệp nhạc**, duyệt bằng Android SAF `OpenDocument(audio/*)` và cấp persisted READ. Ứng dụng chỉ ghi đường dẫn mới vào Room nếu có quyền đọc đã lưu và file mở được. Lệnh cập nhật thực hiện trong **một giao dịch Room**: giữ vị trí trong playlist, tên, nghệ sĩ, ngày thêm; không xóa tệp gốc hoặc sửa các playlist khác. Không cho liên kết trùng URI trong cùng playlist. Nếu file thực sự không đọc được, một bài kiểm tra định kỳ có thể đánh dấu lại; một descriptor đọc được không bảo đảm codec giải mã được nhạc.

Để không làm thay đổi playlist cũ bất ngờ, bước rà soát là **theo yêu cầu**, không tự phát media hoặc xóa bài. Khi không bấm kiểm tra, Playback Error Auto Skip trước đây vẫn là cơ chế phát nhạc dự phòng.

### Sao lưu / khôi phục cục bộ

Mở **Danh sách phát → Sao lưu / khôi phục playlist**. Nút **Xuất sao lưu** gọi Android `CreateDocument(application/json)`; nút **Nhập bản sao lưu** gọi `OpenDocument(*/*)` để tương thích provider gắn MIME không chuẩn cho file `.json`, sau đó **tự kiểm tra chặt** format/version/size/nội dung, xem số playlist/bài và **xác nhận** trước khi viết Room. Chọn nhầm một tệp không phải bản sao lưu sẽ bị từ chối trước khi thay đổi database.

- Định dạng JSON version 1 có `format=musicflow.local.playlists`, `version=1`, `playlists[]` chứa `name`, `tracks[]` có `mediaId`, `audioUri`, `title`, `artist`, `addedAtMs` theo thứ tự.
- **Không chứa** nhạc, ảnh album, thông tin thanh toán, token, password, dữ liệu Supabase hay giấy phép streaming. Chỉ chấp nhận URI local (`content`/`file`/đường dẫn local được hỗ trợ); URL HTTPS streaming bị từ chối, không tạo quyền phát nhạc bản quyền.
- Tệp đọc tối đa **2 MiB**, tối đa **100 playlist**, **500 bài/playlist**. Mọi dữ liệu được parse/validate **trước** khi ghi DB; tệp sai định dạng, sai version, trùng ID bài trong playlist, quá giới hạn hoặc metadata hỏng đều không được nhập.
- Nhập không xóa/sửa các playlist hoặc lịch sử cũ; bản nhập được tạo playlist mới với hậu tố **`(nhập)`**, ID mới và vị trí bài được giữ. **Toàn bộ thao tác nhập nằm trong transaction Room**, rollback nếu DB lỗi.
- `content://` lưu từ thiết bị cũ **không tự chuyển quyền đọc sang thiết bị mới**. Bài thiếu quyền vẫn nằm trong playlist để người dùng sửa liên kết với SAF (tránh mất tên bài khi sao lưu). Không tuyên bố đây là bản sao file nhạc hoặc đồng bộ giữa hai máy.

## 2. Mã nguồn

- `core/playlist/PlaylistLocalBackup.kt`: validation, JSON encode/decode, giới hạn dữ liệu.
- `core/playlist/PlaylistLocalAccess.kt`: kiểm tra file local trên `Dispatchers.IO`.
- `core/playlist/local/PlaylistDao.kt`: lấy toàn bộ playlist và một bản ghi track có ID cụ thể.
- `core/playlist/RoomPlaylistRepository.kt`: snapshot nhất quán, import transaction và relink có điều kiện.
- `feature/playlist/PlaylistBackupControls.kt`: document picker, preview/confirm, xử lý lỗi, progress.
- `feature/playlist/PlaylistScreens.kt`: các nút kiểm tra/đánh dấu/mở dialog chọn lại, play chỉ dùng mục đọc được sau khi kiểm tra.
- `feature/playlist/PlaylistTrackOptions.kt`: action relink.
- `feature/playlist/PlaylistViewModel.kt`: gọi repository relink và báo lỗi.
- Kiểm thử mới: `PlaylistBackupRulesTest` (JVM), `PlaylistLocalBackupIntegrationTest` (Room + JSON + file local), `OfflineReliabilityUiTest` (Compose).
- QA tổng hợp: `scripts/run-offline-reliability-backup-v1-qa.ps1`; sử dụng AVD API30 riêng, không sửa AVD cá nhân.

## 3. Bảo vệ dữ liệu và giới hạn

Xuất sao lưu là thao tác do người dùng chọn nơi ghi bằng Android DocumentsUI, không tự tải lên mạng. Nhập bản sao lưu không ghi đè playlist cũ, không sửa `AccountIdentity`, Premium, Role RPC, Media3 player hoặc lịch sử đang nghe. Những tệp URI chuyển thiết bị bị mất quyền sẽ được hiển thị có vấn đề khi người dùng kiểm tra; sửa liên kết từng bài qua picker thật.

**Không đảm bảo:** bản sao lưu không chứa audio; không tự động khôi phục grant SAF trên thiết bị mới; không xác minh bitstream codec chỉ bằng open descriptor; chưa nhập đè/trộn playlist cũ theo tên; không bảo mật/mã hóa tệp JSON (metadata có thể chứa tên bài và URI cá nhân); không production/cloud E2E.

## 4. Nghiệm thu

### Kết quả hoàn tất (API30 QA emulator)

| QA gate | Kết quả thực |
|---|---|
| JVM đầy đủ | **193/193 PASS**, 34 suites, 0 failed/error |
| Android UI + Room + Media3/queue regression (selected 20 classes) | **85/85 PASS**, gồm 6 `PlaylistLocalBackupIntegrationTest` và 2 `OfflineReliabilityUiTest` mới |
| Android OpenDocument chọn WAV thật, persisted READ + Room | **1/1 PASS** |
| Android OpenDocument chọn JSON thật, xác nhận rồi import | **1/1 PASS** |
| Web Admin fail-closed/role refresh | **4/4 PASS** |
| Gradle assembleDebug/assembleDebugAndroidTest/assembleRelease | **PASS** |
| Release-mode QA key, `zipalign`/`apksigner`/`aapt`/SHA256 | **PASS** |
| Cài **đúng APK Final đã ký** và cold launch API30 | **PASS** (`Status: ok`, `LaunchState: COLD`, app PID tồn tại) |
| APK Final mở Danh sách phát, bật Xuất/Nhập, chụp ảnh màn hình | **PASS** |

**Lỗi đã sửa trước khi nghiệm thu:** Import JSON ở Android DocumentsUI có provider gắn MIME khác, bộ lọc `application/json,text/*` khiến vài tệp không thể chọn → mở `*/*` nhưng parse/validate vẫn chặn mọi tệp không đúng MusicFlow. Test chọn WAV cũ dùng cách bấm ancestor bao phủ cả RecyclerView, báo chọn file sai dù DocumentsUI không đóng → sửa bấm đúng bounds child và chờ quay về app. Lượt QA đầu còn timeout SAF, sau sửa test đã **rerun full 85 + 1 WAV + 1 JSON đều PASS**; log cuối có `OFFLINE_BACKUP_V1_FINAL_RESULT=PASS`.

**APK mới nhất dùng nộp:**

- `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-OfflineBackupV1-Final-Android.apk`
- **31.409.853 bytes**; SHA-256 **`088AAA5118CC4D820E5251A321E03963DD11F63D5F41A65466C891EB6F86E08A`**.
- Package `com.example.musicflow`, `versionName=1.0`, `versionCode=1`, minSdk29/targetSdk36, `debuggable=false`; **ký bằng Android debug QA key**, không phải Play/production.
- Có bản trung gian `OfflineBackupV1-Android.apk` từ trước sửa JSON picker; **không dùng bản trung gian khi nộp**.
- Chứng cứ: `qa-logs/musicflow-offline-backup-v1-qa.log`, `qa-logs/demo-apk-offline-backup-v1-final-package.log`, ảnh `qa-logs/screenshots/musicflow-offline-backup-final-expanded-api30.png` từ chính APK Final; ảnh trung gian trong cùng thư mục chỉ để tham khảo lịch sử.

**Giới hạn:** 85 test Android là *tập hợp hồi quy trọng điểm*, chưa phải toàn bộ instrumentation của repo; chưa chạy API33/36, thiết bị vật lý hoặc TalkBack thủ công cho mốc này; chưa có test trên mọi cloud document provider. Bản sao lưu không mang quyền SAF sang máy mới và không chứa tệp audio.
