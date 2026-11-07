# MusicFlow — Offline Discovery v3 (2026-11-07)

## Mục tiêu và trải nghiệm

Làm thư viện nhạc offline dễ hiểu hơn mà không cần tài khoản/Internet: ngoài những bài được Android đưa vào MediaStore, người nghe có thể **chọn tệp âm thanh bằng DocumentsUI**, quay lại **Thư viện → Nhạc đã chọn** và thấy lối tắt để phát tiếp trong các lần mở ứng dụng sau.

### Người dùng thao tác

1. Mở **Thư viện → Chọn tệp nhạc**. Chọn một hoặc nhiều tệp âm thanh bằng trình quản lý tệp Android. MusicFlow phát các tệp đã chọn như trước và **ghi nhớ lối tắt** khi hệ thống cấp quyền đọc lâu dài.
2. Mở **Thư viện → Nhạc đã chọn** để phát bài, thêm vào playlist, yêu thích hoặc đưa vào hàng đợi. Khu vực này vẫn hoạt động **không cần quyền READ_MEDIA_AUDIO/READ_EXTERNAL_STORAGE**.
3. Mở **Thư viện → Thông minh** (hoặc từ Home) để thấy các tệp đã chọn còn đọc được trong **Mix, Yêu thích, Nghe gần đây, Mới thêm**; **Nghe thật lâu** dùng metadata thời lượng nếu đọc được. MediaStore và SAF trùng URI chỉ tính một lần.
4. Nếu tệp bị xóa/chuyển chỗ hoặc mất quyền đọc, **Nhạc đã chọn** hiển thị số bài cần chọn lại cùng hướng dẫn. **Bỏ lối tắt · không xóa tệp** chỉ bỏ mục trong MusicFlow, không gọi lệnh xóa file hoặc thu hồi quyền đang dùng bởi playlist khác.
5. Nếu không có tệp, màn hình có hướng dẫn và nút **Chọn tệp nhạc**. Với Smart Library được mở trực tiếp, ứng dụng không tự yêu cầu quyền quét MediaStore nếu chỉ cần xem các tệp đã chọn.

### Thay đổi nội bộ

- `core/media/LocalPickedMusicStore.kt`: SharedPreferences JSON lưu tối đa 250 lối tắt URI/metadata, StateFlow cập nhật UI; chỉ ghi nhận `content://` còn đọc được và đã được Android cấp **persisted READ grant**. Mở lại URI trên `Dispatchers.IO`, chia tệp **available/unavailable**; không tự xóa dữ liệu cũ/playlist/tệp.
- `MainActivity.kt`: `OpenMultipleDocuments` vẫn phát ngay; sau picker, nếu có grant vĩnh viễn thì lưu metadata tiêu đề/nghệ sĩ/thời lượng vào danh sách. Không làm phát nhạc lỗi chỉ vì việc ghi nhớ thất bại.
- `feature/library/PickedMusicContent.kt` và `LibraryScreen.kt`: tab **Nhạc đã chọn**, số bài đọc được / cần chọn lại, action rõ nghĩa, truy cập không cần quyền quét, Smart lấy hợp của nhạc quét và nhạc chọn đã kiểm tra.
- `DeviceMusicScanner.kt`: ngoài `IS_MUSIC != 0`, bổ sung điều kiện thận trọng **tệp >= 30 giây**, MIME audio nhận dạng được, thư mục `Music/`. Không quét toàn bộ ghi âm/thông báo ở mọi thư mục.
- `SmartLibraryContent.kt`: lời giải thích cập nhật theo nguồn nhạc thực; nút xóa bộ lọc có nhãn dành cho trình đọc màn hình.

### Giới hạn rõ ràng

- Các tệp từng chọn **trước v3** chưa được lưu vào catalog mới cần chọn lại một lần nếu muốn hiện trong **Nhạc đã chọn**. Có thể vẫn nằm trong playlist/yêu thích hiện tại.
- Tệp chỉ có quyền tạm thời vẫn phát được trong phiên picker, nhưng **không được hứa sẽ ghi nhớ** đến lần mở sau.
- Nhạc SAF mới được bổ sung vào **tab Nhạc đã chọn** và **Smart Library**, không mặc định nhập vào mọi album/nghệ sĩ/thể loại của MediaStore.
- Dữ liệu lối tắt chỉ chứa metadata và URI; không sao chép file, không mang quyền SAF sang thiết bị khác. Metadata duration có thể không lấy được từ một số provider.
- Giữ giới hạn hiện hành của **APK demo môn học**: không billing thật, streaming chưa có giấy phép, quyền Supabase, phát hành Google Play hoặc kiểm thử máy thật.

## Kiểm thử & gói bàn giao

Kịch bản đã kiểm thử trên **emulator API30 riêng**: **199/199 JVM PASS**, **98/98 Android Compose/Room/Media3 regression PASS** (trong đó 13 Smart UI, thêm 4 test v3), **4/4 Web Admin fail-closed PASS**. Các luồng thực: **1/1 WAV 31 giây do test tạo trong MediaStore** và `DeviceMusicScanner.scan()`, **1/1 OpenMultipleDocuments thực qua `MainActivity` → đọc WAV → persisted READ → `LocalPickedMusicStore` lưu shortcut → `inspect()` thấy có thể phát**, **1/1 SAF WAV Quick Add thật**, **1/1 JSON DocumentsUI thật**, toàn bộ PASS. Không suy ra mọi thiết bị/provider/codec đều đạt.

**APK bàn giao ưu tiên:** `dist/MusicFlow-1.0-Demo-OfflineDiscoveryV3-Android.apk`, **31.508.157 bytes**, SHA256 **`2C4C28A6502C9370D4650AB77121696B944293E6E95B6DB820ACD7D4C640739B`**. `versionCode=1`, `versionName=1.0`, `minSdk=29`, `targetSdk=36`. Gradle release build, zipalign, apksigner, aapt và hash **PASS**; chính bản APK ký **QA debug certificate** đã cài bằng `adb install -r`, **cold launch API30 PASS** (PID 9334). **Không phải release production**; không tự đưa APK lên GitHub (thư mục `dist` được Git-ignore).

Log: `qa-logs/musicflow-offline-discovery-v3-build.log`, `qa-logs/musicflow-offline-discovery-v3-regression.log`, `qa-logs/musicflow-offline-discovery-v3-real-mediastore.log`, `qa-logs/musicflow-offline-discovery-v3-real-picker.log`, `qa-logs/demo-apk-offline-discovery-v3-package.log`, `qa-logs/musicflow-offline-discovery-v3-signed-apk-smoke.log`.
