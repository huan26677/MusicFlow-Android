# Hướng dẫn cài đặt MusicFlow Demo APK — dự án báo cáo

> **KHUYẾN NGHỊ BẢN v6 (2026-11-08):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-AccessibilityUpgradeV6-Android.apk` (**31.573.693 bytes**, SHA256 `5BECC22142FCB5457A8E4D898AA536ED4805064CCD63B0816F8173BBCF3E9F0D`). Ứng dụng giữ mọi tính năng offline của v5, đồng thời dễ dùng với chữ rất lớn và công cụ đọc màn hình. **Nếu đã cài bản QA cùng chữ ký, hãy cài đè, không gỡ ứng dụng khi chưa sao lưu playlist/dữ liệu**. Đã kiểm thử thực tế trên QA AVD khi cài v5 có playlist/favorite/WAV SAF persisted READ rồi cài đè v6: dữ liệu được giữ nguyên; không suy diễn bảo đảm mọi thiết bị. **199 JVM + 123 UI/regression + SAF/WAV/JSON/Web Admin PASS trên API30; chính signed v6 APK cold launch PASS.** Không phải release Google Play, `dist/` bị Git ignore: sao chép APK riêng khi nộp. Các dòng v5/v4 dưới đây chỉ là lịch sử.

> **CÀI BẢN v5 (MỚI NHẤT, 2026-11-08):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-AccessibilitySearchV5-Android.apk` (**31.573.693 bytes**, SHA256 `2A6D6C6D7B3D21217CD2D966278C8F8614ACFC7F44F3F162B089FBB87C5F30BB`). Giữ nguyên ứng dụng cũ, **cài đè bằng cùng chứng chỉ ký QA** nếu muốn giữ dữ liệu Room/playlist; không gỡ bản đã dùng khi chưa sao lưu. Mở **Thư viện**, bạn chọn `Chọn tệp nhạc` để nghe ngay hoặc `Quét nhạc trên thiết bị` để tự cấp quyền; ở **Tìm kiếm**, file SAF còn quyền có thể được tìm/phát cả khi không cấp quyền quét. Giao diện chữ lớn có menu thao tác từng bài. Đã QA 199 JVM + 117 Android UI/hồi quy + SAF/WAV/JSON/Web Admin, chính APK ký QA khởi động lạnh API30 PASS. `dist/` bị Git-ignore: phải chép APK riêng để nộp. Các dòng v4/v3 bên dưới chỉ để tra lịch sử.

> **NÊN CÀI BẢN LIBRARY UX v4 (2026-11-07):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-LibraryUXV4-Android.apk` (**31.540.925 bytes**, SHA256 `5D1F9D6A5A8CE3CD50BA4C253A3F83B8D5459A23949A1F629ECB1671562977EA`). Cài **đè cùng chữ ký QA** để giữ nhạc/playlist cục bộ, không gỡ ứng dụng cũ khi dữ liệu chưa sao lưu. **Thư viện → Bài hát → Tìm trong Bài hát**: gõ `duong` để tìm tiêu đề `Đường`, sắp xếp rồi phát đúng danh sách đang lọc. **Thư viện → Nhạc đã chọn**: tìm trong nhạc SAF đã cấp quyền; khi không cấp quyền quét, dùng **Tiếp tục với nhạc đã chọn**. **Thể loại** tải metadata bổ sung khi bạn mở tab. **199 JVM + 105 Android UI/regression + 2 MediaStore WAV + 1 MainActivity DocumentsUI + 1 SAF Quick Add + 1 JSON + 4 Web Admin PASS**, APK đã ký QA/cài/cold launch API30. `dist` bị Git-ignore, cần chép APK riêng khi nộp. Xem [Library_Performance_UX_v4.md](Library_Performance_UX_v4.md). Các APK bên dưới là lịch sử.

> **NÊN CÀI BẢN OFFLINE DISCOVERY v3 (2026-11-07):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-OfflineDiscoveryV3-Android.apk` (**31.508.157 bytes**, SHA256 `2C4C28A6502C9370D4650AB77121696B944293E6E95B6DB820ACD7D4C640739B`). Cài đè cùng chữ ký QA để giữ playlist/cấu hình. Vào **Thư viện → Chọn tệp nhạc** rồi **Thư viện → Nhạc đã chọn** để mở lại file có quyền đọc đã lưu, kể cả khi không cấp quyền quét; dùng **Thư viện → Thông minh** để phát Mix từ nhạc trên máy và tệp đã chọn. Nếu file mất quyền, chọn lại hoặc **Bỏ lối tắt · không xóa tệp**. File chọn trước v3 cần chọn lại một lần nếu muốn xuất hiện ở tab mới. **199 JVM, 98 Android regression, 2 kiểm thử nhạc thật, 1 SAF WAV, 1 JSON, 4 Web Admin PASS**; chính APK mới đã cài/cold launch API30. Chép APK riêng khi nộp (`dist` bị Git-ignore), không gỡ bản cũ nếu còn dữ liệu Room. Xem [Offline_Discovery_v3.md](Offline_Discovery_v3.md); các bản bên dưới chỉ là lịch sử.

> **NÊN CÀI BẢN SMART LIBRARY v2 (2026-11-07):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-SmartLibraryV2-Android.apk` (**31.475.389 bytes**, SHA256 `1D1D907855A15119C17D454F567ED80C71BA9C11F32A05C8EB0C142717475C4E`). Gồm mọi tính năng demo cũ và thêm tìm kiếm không dấu **ngay trong Playlist thông minh**, chỉ phát bài sau khi lọc, xóa lọc và chuyển sang mục Bài hát khi trống. **199 JVM, 94 Android, 1 SAF, 1 JSON, 4 Web Admin PASS**; cài/cold launch chính APK ký QA trên API30 PASS. Giữ app và **cài đè** cùng chữ ký QA để không mất dữ liệu local; nếu khác chữ ký, không gỡ ngay khi còn playlist cần giữ. `dist` bị Git-ignore, cần sao chép APK riêng khi nộp. Xem [Smart_Library_Experience_v2.md](Smart_Library_Experience_v2.md). Các tệp dưới là phiên bản lịch sử.

> **NÊN CÀI BẢN SMART LIBRARY v1 FINAL (2026-11-07):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-SmartLibraryV1-Final-Android.apk`, **31.459.005 bytes**, SHA256 `2647528DED4F0483A1FBA918A918C16C7E2D1665A0EFDD4CBA222456BAC56C3C`. Đây là APK mới nhất, gồm Ocean Flow, Demo Center, thanh toán giả lập, Offline Backup v1 và **Smart Library / Smart Playlist v1**. Vào **Trang chủ → Playlist thông minh · Tự cập nhật** hoặc **Thư viện → Thông minh**. Các bộ sưu tập chỉ hiển thị nhạc đã quét; nếu trống hãy cấp quyền/quét file nhạc offline. Ký QA/cài/cold launch API30 PASS, kiểm thử 199 JVM + 90 Android + 1 SAF + 1 JSON + 4 Web Admin PASS. **Không gỡ bản cũ nếu cần giữ dữ liệu Room**, cài đè cùng chữ ký QA. `dist` bị Git ignore: chép APK riêng khi nộp. Xem [Smart_Library_Playlists_v1.md](Smart_Library_Playlists_v1.md); các bản bên dưới chỉ là lịch sử.

> **NÊN CÀI BẢN OFFLINE BACKUP v1 FINAL (2026-11-07):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-OfflineBackupV1-Final-Android.apk` (**31.409.853 bytes**, SHA256 `088AAA5118CC4D820E5251A321E03963DD11F63D5F41A65466C891EB6F86E08A`). Cài đè APK cùng chữ ký để giữ playlist đang có; không gỡ ứng dụng nếu cần dữ liệu cục bộ. Màn **Danh sách phát → Sao lưu / khôi phục playlist** hỗ trợ Xuất JSON, Nhập JSON (cần xác nhận) và **chi tiết playlist → Kiểm tra tệp còn đọc được → Tùy chọn bài hát → Chọn lại tệp nhạc**. JSON **không chứa file nhạc** và không tự mang grant SAF qua máy mới. Trên emulator API30: QA **193 JVM + 85 Android + 1 SAF WAV + 1 JSON picker + 4 Web Admin PASS**, ký/cài/cold launch chính APK Final PASS. Xem [Offline_Reliability_Backup_v1.md](Offline_Reliability_Backup_v1.md); các bản dưới là lịch sử.

> **NÊN CÀI BẢN OCEAN FLOW UI v1 FINAL (2026-11-07):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-OceanFlowUIV1-Final-Android.apk`, **31.344.317 bytes**, SHA256 `2FA0D0A5C79E2DA9768429CE18FCE62BD80CE4F52B38BF3B72BAA576579B9400`. Đã ký QA, cài và cold launch API30 PASS, dark mặc định cho cài mới, light/system đổi từ Trang Tài khoản. **Không dùng** `OceanFlowUIV1-Android.apk` trước bản Final vì bản đó chưa có màu status/navigation bar đúng theme. Các đường dẫn APK phía dưới là lịch sử; thay tên file trong lệnh `adb install` nếu cần và không gỡ phiên bản cũ nếu muốn giữ playlist/cấu hình. Xem [Ocean_Flow_Visual_Refresh_v1.md](Ocean_Flow_Visual_Refresh_v1.md).

> **NÊN CÀI BẢN DEMO MỚI NHẤT — 9 ACTOR / DEMO CENTER v1 (2026-11-07):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-CenterV1-Android.apk`, **31.327.933 bytes**, SHA256 `47FBE9353CE700F432FE216F127AF2D50FBB35E3097607D5A752A697A352C481`. Ký QA, cài/cold launch API30 PASS. Vào **Tài khoản → Trình diễn 9 loại tài khoản (giả lập)** hoặc chọn **Demo offline** để đăng nhập local; mọi vai trò mẫu không cấp quyền thật. Xem [Trung_Tam_Trinh_Dien_MusicFlow_v1.md](Trung_Tam_Trinh_Dien_MusicFlow_v1.md). Những bản bên dưới là các APK lịch sử; chỉ thay đúng tên tệp khi cài bản mới và giữ dữ liệu nếu có thể.

> **APK mới nhất cho trình diễn (2026-11-07):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-SimulatedBillingV1-Android.apk`, **31.278.781 bytes**, SHA256 `302E6415A56B1A0801BF92AA65C2A92A89D2446A7D31D45C22FAE02A9A3484C7`. Bản này thêm **thanh toán Premium giả lập** qua Trang Tài khoản → **Trải nghiệm thanh toán Premium (giả lập)** (không thu tiền/không cấp quyền Premium thật). Đã ký QA, cài và cold launch API30 PASS. Các tệp ở những mục phía dưới là bản bàn giao lịch sử; thay tên APK vào lệnh `adb install` để cài bản hiện hành, không gỡ bản cũ nếu muốn giữ dữ liệu. Xem `docs/Thanh_Toan_Premium_Gia_Lap_v1.md`.

> **Phiên bản demo nên cài hiện tại (2026-11-07):** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-AcceptanceV1-Android.apk`, **31.213.245 bytes**, SHA256 **`7D7E1D0131DC078CAA6394F1FC14F43BBB8820895FFF260C2BEF4C399CC95889`**. Đã cài/cold launch API30 thành công và có nghiệm thu DocumentsUI SAF thật. Các đường dẫn/tệp `MusicFlow-1.0-Demo-Android.apk` bên dưới là **bản bàn giao lịch sử 2026-11-01**. Khi cài bản mới, chỉ cần thay tên file trong lệnh `adb install`; không gỡ bản cũ nếu cần giữ playlist/dữ liệu cục bộ. Xem [Nghiem_Thu_Demo_2026_11_07.md](Nghiem_Thu_Demo_2026_11_07.md).

**Ngày đóng gói:** 2026-11-01. **Mục đích:** cài trực tiếp bản demo MusicFlow, không cần Google Play hoặc tài khoản thật.

## Tệp APK bàn giao

Tên tệp: `MusicFlow-1.0-Demo-Android.apk`

Vị trí trên máy đang xây dựng dự án:

```text
E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-Android.apk
```

Kích thước: **30.230.097 byte** (xấp xỉ **30,2 MB**).

**SHA-256:**

```text
ACE9AD765F75C7939C513210E2BBC1555F95E6504636303B423FB70598C2E750
```

Một file checksum cũng có tại `dist/MusicFlow-1.0-Demo-Android.apk.sha256`. Nếu sao chép tệp APK sang máy khác, dùng SHA-256 để xác nhận đúng bản đã kiểm thử.

| Thuộc tính | Giá trị |
|---|---|
| Package ID | `com.example.musicflow` |
| versionName | `1.0` |
| versionCode | `1` |
| Android tối thiểu | Android 10 — API 29 |
| Android target | API 36 |
| Chữ ký | **Android Debug certificate cục bộ**, chỉ cho bản demo/báo cáo |
| Chế độ APK | **release-mode, không debuggable** |
| Google Play | Không sử dụng |

## Cách cài trên Android emulator

Đảm bảo emulator đã chạy và chưa có MusicFlow cài trước đó, rồi mở PowerShell tại thư mục dự án:

```powershell
adb devices
adb -s emulator-5554 install ".\dist\MusicFlow-1.0-Demo-Android.apk"
adb -s emulator-5554 shell am start -n com.example.musicflow/.MainActivity
```

`emulator-5554` là mã của emulator đã được kiểm thử, có thể khác trên máy khác. Nếu đã có MusicFlow trên emulator với **chữ ký khác**, lệnh cập nhật sẽ thất bại do Android bảo vệ ứng dụng. **Không gỡ ứng dụng đã có nếu chưa sao lưu dữ liệu của ứng dụng đó.**

## Cài trực tiếp từ tệp APK

Có thể sao chép file `.apk` sang thiết bị Android phù hợp, mở tệp bằng trình quản lý tệp, cho phép cài ứng dụng từ nguồn đó theo hướng dẫn Android, rồi mở MusicFlow. **Đây là hướng dẫn thông thường; dự án không kiểm thử trên điện thoại thật theo yêu cầu.**

Nên chỉ cài từ tệp gốc trong `dist` đã đối chiếu SHA-256. APK sử dụng debug certificate chỉ dành cho demo, không phải chữ ký phát hành chuyên nghiệp. Nếu sau này đổi khóa ký, APK mới có thể không nâng cấp trực tiếp lên bản demo này.

## Cách trình diễn offline

1. Mở MusicFlow và chọn sử dụng ở chế độ khách nếu có màn hình tài khoản; **không cần email thật**.
2. Cho phép quyền đọc nhạc khi Android yêu cầu, rồi vào **Thư viện thiết bị**.
3. Chọn tệp âm thanh đã có trên Android/emulator, thử Play/Pause, Next và màn hình Now Playing.
4. Có thể giới thiệu thêm playlist, queue, lịch sử, tìm kiếm, lyrics, Sleep Timer, EQ theo tình trạng đã được ghi trong báo cáo kỹ thuật.
5. Thoát ra Home để minh họa MediaSession và thông báo điều khiển nhạc nền.

Nhạc **offline do người dùng tự lưu/tải** không bị MusicFlow yêu cầu xác nhận quyền streaming online. Nhạc **online do MusicFlow cung cấp** chỉ phát khi có nguồn và quyền phát hợp lệ; không dùng nguồn không có quyền để demo.

## Nếu cần tạo lại APK demo

Từ root dự án, chạy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-musicflow-demo-apk-v1.ps1
```

Script build từ nguồn Kotlin, căn chỉnh và ký bằng Android Debug keystore của máy đó, kiểm tra `aapt` và `apksigner`, tạo file `.sha256` trong `dist` và log tại `qa-logs/demo-apk-package-v1.log`. Script **không ghi đè** một APK demo cùng tên nhưng khác SHA-256, để bảo vệ bản bàn giao đã chốt. Muốn tạo bản mới khác, cần lên phiên bản/tên file khác trong một bước phát triển sau.

**Lưu ý:** Nếu chạy lại trên máy khác, debug keystore khác có thể tạo chữ ký/hash khác; không coi đó là cùng artifact bàn giao. **APK không được tự động upload lên GitHub hoặc Play Store.**

## Tình trạng nghiệm thu

Đã kiểm chứng trên Android emulator API 30 bằng **chính APK trong thư mục dist**: cài mới, mở Library, chơi WAV, MP3, FLAC, Play/Pause/Next, phát nền và MediaStyle Notification đều PASS, có cleanup QA. Xem `docs/Bao_Cao_Ban_Giao_MusicFlow_Demo_APK.md`.
