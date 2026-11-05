# MusicFlow — Player & Library UX Polish v3

**Ngày:** 2026-11-05 · **Dự án:** APK demo/báo cáo, không phát hành thật.  
**HEAD nền:** `a7c34d6e996756ed46976abe40088b9813eafc96` (`main = origin/main`, Git sạch đầu lượt).

## 1. Phạm vi cập nhật

### Màn Đang phát: ưu tiên thao tác cơ bản

- Giữ ngay trên màn chính: ảnh bìa, tiêu đề/nghệ sĩ, yêu thích, hàng đợi, thanh tiến trình, **Trước / Phát / Tiếp**, Trộn và Lặp.
- Gom **Tốc độ phát**, **Hẹn giờ tắt nhạc**, **Crossfade**, **Equalizer** vào nút **Tùy chỉnh phát nhạc**. Mặc định đóng để giảm độ dài màn hình; mở/thu gọn bằng một nút, có UI tag `player-advanced-toggle`.
- Hiển thị tóm tắt tốc độ hiện tại và trạng thái hẹn giờ ngay dưới nút, kể cả khi đang thu gọn.
- Giữ nguyên callbacks tới Media3, MediaSession, Speed, Sleep Timer, Equalizer, Crossfade; không thay đổi xử lý âm thanh, bộ nhớ queue hay cơ chế giấy phép online.
- Giữ mục lời bài hát và nhập .lrc bên ngoài nhóm nâng cao, nên người dùng vẫn tìm thấy.
- Điều chỉnh các bài kiểm thử runtime cũ cho speed/Crossfade để mở nhóm trước khi chạm chip; **không khẳng định đã chạy lại toàn bộ runtime tests**.

### Thư viện: sắp xếp dễ tìm nhạc

- Trên tab **Bài hát** thêm nút **Sắp xếp** có 4 lựa chọn: **Tên bài**, **Nghệ sĩ**, **Album**, **Mới thêm**.
- `LibrarySongSort.kt` dùng sắp xếp ổn định/nhất quán (tên bài và URI làm khóa phụ), không thay đổi dữ liệu gốc.
- **Mới thêm** lấy `MediaStore.Audio.Media.DATE_ADDED` từ scan và giảm dần. Thông tin này là **ngày Android ghi nhận tệp được thêm vào thư viện**, không bảo đảm bằng ngày tạo bài nhạc. Tệp được thêm bằng SAF nhưng chưa có metadata MediaStore dùng giá trị mặc định `0`, được xếp sau các tệp có thời điểm rõ ràng.
- Danh sách đã sắp xếp **được dùng cả để hiển thị và làm queue khi người dùng chạm bài hát**, tránh thứ tự phát khác với màn hình.
- Nút lọc yêu thích vẫn hoạt động; album/nghệ sĩ/thể loại và playlist giữ cơ chế sắp xếp, quản lý riêng trước đây. Các chức năng tạo/đổi tên/xóa và sắp xếp playlist đang dùng tốt nên không sửa trong lượt này.

## 2. Bằng chứng kiểm thử đúng chức năng được sửa

| Kiểm thử | Kết quả |
|---|---|
| `:app:assembleDebug :app:assembleDebugAndroidTest` | **BUILD SUCCESSFUL** |
| `MusicFlowPlayerLibraryUxV3Test` trên emulator API 30 riêng | **3/3 PASS** |
| Mở/thu gọn nâng cao; tốc độ/Crossfade ẩn hiện; lyrics vẫn có | PASS |
| Sắp xếp theo đủ 4 khóa với dữ liệu thử tổng hợp và metadata DATE_ADDED | PASS |
| Chọn **Mới thêm** trên UI, phát một bài, callback nhận đúng thứ tự hàng đợi | PASS |
| Build APK release-mode, zipalign, apksigner verify, aapt badging | PASS |
| Cài nâng cấp chính APK demo mới, khởi động, 4 tab tiếng Việt và CTA Thư viện | PASS |
| SHA-256 tệp APK và checksum sidecar trùng | PASS |

**QA AVD:** `MusicFlow_PlayerLibraryUX_QA_API30` trên `emulator-5560`; tách biệt `emulator-5556` của người dùng, không đụng dữ liệu đang có. Các tệp âm thanh/tài khoản thật không được thay đổi trong lượt test; kiểm thử danh sách dùng 3 `DeviceSong` tổng hợp.
Sau kiểm thử, **AVD QA được xóa cả cấu hình và thư mục dữ liệu**; `emulator-5556` của người dùng vẫn hoạt động và được giữ nguyên.

**Không chạy full-suite** Android/JVM, không kiểm thử máy thật, không kiểm thử lại Supabase Auth và không đo hiệu ứng âm thanh thực trên loa. QA xác minh màn hình và luồng sắp xếp, **không khẳng định đã đo chất lượng Crossfade/EQ**.

## 3. APK demo mới

```text
E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-PlayerLibraryUXV3-Android.apk
```

- Kích thước: **30.770.877 byte**.
- SHA-256: `1DDAB2611F941701277B1E50812ED209A92102B6D33CE9E9492B851E0026BDCD`.
- Package `com.example.musicflow`, versionName `1.0`, versionCode `1`, Android minSdk 29 / targetSdk 36.
- APK **release-mode, không debuggable, ký bằng Android Debug keystore để demo**. **Không phải bản production**, không Google Play/khóa ký thật.
- APK và checksum trong `dist/` được Git ignore. Không xóa hoặc ghi đè các APK demo cũ; không tự upload lên GitHub.
- Script đóng gói: `scripts/build-musicflow-demo-apk-v1.ps1 -OutputName MusicFlow-1.0-Demo-PlayerLibraryUXV3-Android.apk`. Bản cùng tên nhưng hash khác sẽ bị script từ chối ghi đè.

## 4. Bàn giao và hướng sau này

Tài liệu này + `docs/Ke_Hoach_Du_An.md` ghi phạm vi vừa hoàn thành. QA logs: `qa-logs/musicflow-player-library-ux-v3.log` và `qa-logs/demo-apk-player-library-ux-v3-package.log`.

Ứng dụng giữ mục tiêu **APK sideload cho báo cáo**, không mở rộng sang email thật, điện thoại thật hoặc phát hành. Nếu người dùng yêu cầu phát triển tiếp, ưu tiên đánh giá trực tiếp bố cục tại font scale lớn và cải thiện trình bày playlist (không tự mở mốc mới).
