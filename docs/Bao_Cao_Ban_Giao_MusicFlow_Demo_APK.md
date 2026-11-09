# BÁO CÁO BÀN GIAO — MUSICFLOW ANDROID DEMO APK

> **NGHIỆM THU NÂNG CẤP v14 (2026-11-09):** Hai AVD sạch API33/36, APK ký cùng chứng thư v13→v14: **seed / install -r / verify Room playlist+favorite+SAF WAV persisted READ / cleanup PASS trên cả hai API**. Luồng Trang chủ → Khám phá online → Community Demo trên **APK Release ký QA**: **1/1 PASS trên API33, 1/1 PASS trên API36**. Lượt thử 6 Compose test dùng debug-only Activity trên Release đã **6 FAIL do test harness**, bằng chứng lỗi giữ lại, không đổi thành PASS; đã thiết kế bài test riêng dùng MainActivity. **Chưa** full 480, real JWT đa vai trò, TalkBack/máy thật, multi-SAF và Play Store signing. Xem `docs/Community_Signed_Upgrade_Acceptance_v14.md`.

> **MỚI NHẤT v14 — COMMUNITY (2026-11-09):** `dist/MusicFlow-1.0-Demo-CommunityV14-Android.apk` (**31.753.917 bytes**, SHA-256 `3C2FFD95231A1AECBE9A76E21EE34A4BA56FF320CD18F14FA6B3E6904B6DD0BD`). Bản Demo ký QA, Release không debuggable, zipalign/apksigner/hash **PASS**, `adb install -r` + cold launch trên AVD API36 **PASS**. Community v14 UI **6/6 PASS** và JVM **210/210 PASS**; Supabase moderation v14.1/v14.2 đã áp dụng, transaction rollback test **PASS** không tạo dữ liệu thật. Dữ liệu bình luận Demo RAM độc lập, catalog nhạc online thật còn cần bài có license. **Chưa** full 479, E2E JWT đa tài khoản, bảo toàn Room/SAF upgrade, máy thật/TalkBack/UAT. Tệp `dist/` cần chép riêng vì không được commit lên Git. Chi tiết `docs/Community_Ratings_Comments_v14.md`. Các ghi chú v13 trở xuống dưới đây là lịch sử.

> **BẢN DEMO CẬP NHẬT NHẤT — FINAL UX v13 (2026-11-08):** `dist/MusicFlow-1.0-Demo-FinalUXV13-Android.apk`, **31.622.845 bytes**, SHA-256 `D74CD7F4FEEE8819FBE6528554AC8EBC60885EBE7182FBFC7EBC77F1762F9D3F`. Giữ phạm vi chức năng cũ; chỉ đưa điều khiển phát nhạc lên dễ chạm, bổ sung chỉ dẫn danh mục Thư viện, hướng dẫn Playlist, chuyển thiết lập Sáng/Tối lên trước Demo và phân biệt vùng cuộn Owner/Admin. Release APK ký keystore Android Debug **chỉ để trình diễn**, zipalign/apksigner/hash và cài đè + cold-launch API36 QA **PASS**. Android v13 selected 14/14 PASS trên Debug theo runner riêng; lượt mixed 67 ban đầu 63 PASS/4 FAIL còn ghi log. **Chưa** full 462, UAT/TalkBack, máy thật, signed data-preservation upgrade v13 hay production release. Xem `docs/Final_User_Experience_Polish_v13.md`. Các dòng v10 trở xuống dưới đây là lịch sử.

> **BẢN HIỆN TẠI v10 — CLEAR JOURNEYS (2026-11-08):** `dist/MusicFlow-1.0-Demo-ClearJourneysV10-Android.apk`, **31.606.461 bytes**, SHA256 `C8136BE71BE7BEEB15CB2C37C9DAFDFE416257E60E4A855B9D7E580FED908097`. Tài khoản chưa có kết nối online hiển thị cách dùng offline/demo rõ ràng; nút mở rộng có trạng thái trợ năng; Owner/Admin được hướng dẫn cuộn khi chữ lớn. Chỉ ký QA, không phải production. Xem `docs/Clear_Journeys_UX_v10.md` để biết QA/giới hạn; v9 trở xuống là lịch sử.

> **BẢN MỚI v9 — SIMPLE EXPERIENCE (2026-11-08):** `dist/MusicFlow-1.0-Demo-SimpleExperienceV9-Android.apk`, **31.606.461 bytes**, SHA256 `E6AE4F5EAA61B5D0262F63195A600E2757E5C551256D0C1262A1AA4CB5A066C9`. Trang chủ ưu tiên chọn nhạc/tìm kiếm; Playlist/Smart Mix ẩn sau tùy chọn mở rộng; khách nghe nhạc trước khi đăng nhập, tên chế độ Auth dễ hiểu; Demo Center/Premium giả lập được thu gọn; khu vực Owner/Admin/Creator có điều hướng công việc rõ ràng, đáp ứng chữ lớn. Xem `docs/Simple_Experience_UX_v9.md` để biết QA và giới hạn. **Ký QA, không phải bản production hoặc thanh toán/quyền thật.** v8 trở xuống là lịch sử.

> **BẢN MỚI v8 — KEYBOARD & INDEXED SEARCH (2026-11-08):** `dist/MusicFlow-1.0-Demo-KeyboardSearchIndexV8-Android.apk`, **31.606.461 bytes**, SHA-256 `A9A74F7CA1DFF797B88DECF410939008E88D4E065ACFF1F92A11AC0715AFA3BE`. Tiêu đề album/nghệ sĩ có viền focus bàn phím, D-pad/Enter hoạt động trên AVD QA; Advanced Search tái sử dụng chỉ mục album/nghệ sĩ/thể loại khi gõ, hủy catalog cũ khi quét lại. **8/8 Android targeted mới PASS trên API30**; các kết quả hồi quy/upgrade khác có log riêng. Xem `docs/Keyboard_SearchIndex_Accessibility_v8.md`. **Ký QA, không production, không benchmark nhạc thật/TalkBack thủ công.** v7 và các bản cũ là lịch sử.

> **APK DEMO MỚI NHẤT — LIBRARY ACCESSIBILITY v7 (2026-11-08):** `dist/MusicFlow-1.0-Demo-LibraryAccessibilityV7-Android.apk` (**31.590.077 bytes**, SHA256 `215F2DC4B97274768D9E9870C10A6B3D3188ECCABAC9110976878AD6E2E515B0`). Album, nghệ sĩ và thể loại ảo hóa từng bài, đúng queue/index, trạng thái mở/đóng cho trợ năng và kiểm tra màu chữ cơ bản theo WCAG 4,5:1. **199 JVM + 129 Android UI/hồi quy + SAF/WAV/JSON/Web Admin PASS API30**, **32/32 targeted PASS trên mỗi API33/36**, **nâng cấp APK ký QA v6→v7 giữ Room/favorite/SAF WAV PASS trên API33/36**, APK ký cold-launch API30/33/36 PASS. Các dòng v6 trở về trước là lịch sử; xem `docs/Library_Accessibility_Performance_v7.md`. **Không phải production/Google Play; chưa full regression API33/36, TalkBack thủ công hay benchmark máy thật.**

> **BẢN DEMO HIỆN HÀNH — ACCESSIBILITY & SIGNED UPGRADE v6 (2026-11-08):** `dist/MusicFlow-1.0-Demo-AccessibilityUpgradeV6-Android.apk` (**31.573.693 bytes**, SHA256 `5BECC22142FCB5457A8E4D898AA536ED4805064CCD63B0816F8173BBCF3E9F0D`). Chữ 200% thân thiện, nhãn heading/TalkBack, trình phát công bố tác vụ và vị trí, Account không ép hai lựa chọn vào một hàng khi chữ lớn; tiếp nối toàn bộ nhạc offline từ v5. **199 JVM + 123 Android UI/hồi quy + 2 MediaStore WAV + 1 MainActivity SAF + 1 SAF Quick Add + 1 JSON + 4 Web Admin PASS API30**. **Kiểm thử cài đè APK ký QA v5→v6 thật PASS**, Room playlist, favorite và persisted SAF WAV vẫn đọc được; signed v6 cold launch API30 PASS. Xem [Accessibility_Upgrade_Reliability_v6.md](Accessibility_Upgrade_Reliability_v6.md). Các đoạn v5 trở xuống là lịch sử dù giữ nguyên tiêu đề cũ.

> **BẢN DEMO ĐƯỢC ƯU TIÊN HIỆN HÀNH — ACCESSIBILITY & OFFLINE SEARCH v5 (2026-11-08):** `dist/MusicFlow-1.0-Demo-AccessibilitySearchV5-Android.apk` (**31.573.693 bytes**, SHA256 `2A6D6C6D7B3D21217CD2D966278C8F8614ACFC7F44F3F162B089FBB87C5F30BB`). Tìm cả nhạc MediaStore và SAF có quyền, không ép cấp quyền quét máy, thao tác màn hẹp/chữ lớn dễ hơn. **199 JVM, 117 Android UI/hồi quy (98+7+6+4+2), 2 MediaStore WAV, 1 MainActivity SAF, 1 Quick Add SAF, 1 JSON picker, 4 Web Admin PASS** trên API30; signed QA APK cài/cold launch PASS. Chi tiết và giới hạn: [Accessibility_Offline_Search_UX_v5.md](Accessibility_Offline_Search_UX_v5.md). Các bản v4 trở về trước bên dưới là **lịch sử**, dù tiêu đề cũ vẫn ghi “mới nhất”.

> **BẢN DEMO ƯU TIÊN MỚI NHẤT — LIBRARY PERFORMANCE & UX v4 (2026-11-07):** `dist/MusicFlow-1.0-Demo-LibraryUXV4-Android.apk` (**31.540.925 bytes**, SHA256 `5D1F9D6A5A8CE3CD50BA4C253A3F83B8D5459A23949A1F629ECB1671562977EA`). Tiếp nối v3, tìm tên bài/nghệ sĩ không dấu trong **Bài hát / Nhạc đã chọn**, phát đúng queue sau lọc, nút xóa dễ hiểu, giao diện danh sách ảo hóa dễ cuộn với chữ lớn, chỉ tải Genres khi mở tab; cấp quyền quét là tùy chọn vì vẫn có nhạc SAF đã chọn. **199 JVM + 105 Android regression/UI (98 cũ + 7 mới) + 2 real MediaStore WAV + 1 real MainActivity DocumentsUI WAV + 1 real SAF Quick Add + 1 real JSON picker + 4 Web Admin PASS**. Chính APK ký QA đã cài/cold launch API30 PASS; chưa benchmark hiệu năng thiết bị thật hay test full API33/36. Xem [Library_Performance_UX_v4.md](Library_Performance_UX_v4.md); các APK phía dưới là lịch sử.

> **BẢN DEMO ƯU TIÊN MỚI NHẤT — OFFLINE DISCOVERY v3 (2026-11-07):** `dist/MusicFlow-1.0-Demo-OfflineDiscoveryV3-Android.apk` (**31.508.157 bytes**, SHA256 `2C4C28A6502C9370D4650AB77121696B944293E6E95B6DB820ACD7D4C640739B`). Có **Nhạc đã chọn** tự ghi nhớ tệp SAF có quyền READ lâu dài, phát/yêu thích/playlist/queue, nhận diện tệp mất quyền và bỏ lối tắt an toàn; Smart Library hợp MediaStore + nhạc SAF đọc được, nhận diện bổ sung âm thanh >=30 giây trong thư mục Music. **199 JVM + 98 Android regression + 2 Android nhạc thật (MediaStore WAV & MainActivity DocumentsUI persisted catalog) + 1 SAF WAV Quick Add + 1 JSON picker + 4 Web Admin PASS**. Chính APK ký QA cài/cold launch API30 PASS. Chưa QA full API33/36, TalkBack hoặc thiết bị thật. Xem [Offline_Discovery_v3.md](Offline_Discovery_v3.md). Những APK phía dưới là lịch sử.

> **BẢN DEMO NÊN NỘP MỚI NHẤT — SMART LIBRARY EXPERIENCE v2 (2026-11-07):** `dist/MusicFlow-1.0-Demo-SmartLibraryV2-Android.apk` (**31.475.389 bytes**, SHA256 `1D1D907855A15119C17D454F567ED80C71BA9C11F32A05C8EB0C142717475C4E`). Nâng Smart v1 với tìm kiếm bài/nghệ sĩ không dấu từng bộ sưu tập, phát đúng queue sau lọc, trạng thái không kết quả và lối quay lại mục Bài hát. **199 JVM + 94 Android regression + 1 SAF WAV thật + 1 JSON DocumentsUI thật + 4 Web Admin PASS**; chính APK QA đã ký, cài/cold launch API30 PASS. Chưa E2E thủ công Smart đầy bài trên signed APK; không production. Xem [Smart_Library_Experience_v2.md](Smart_Library_Experience_v2.md); các APK phía dưới là lịch sử.

> **BẢN DEMO MỚI NHẤT — SMART LIBRARY / SMART PLAYLIST v1 FINAL (2026-11-07):** `dist/MusicFlow-1.0-Demo-SmartLibraryV1-Final-Android.apk` (**31.459.005 bytes**, SHA256 `2647528DED4F0483A1FBA918A918C16C7E2D1665A0EFDD4CBA222456BAC56C3C`). Năm danh sách offline tính động (Mix, Yêu thích, Nghe gần đây, Mới thêm, Nghe thật lâu), tối đa 30 bài, không ghi trùng Room, mở từ Home hoặc tab Thư viện. **199 JVM + 90 Android regression + 1 SAF WAV thật + 1 JSON picker thật + 4 Web Admin PASS**; APK ký QA/cài/cold launch API30, màn Smart khi trống PASS. Ảnh QA release là trạng thái **chưa có bài**; không tuyên bố đã chạy kiểm thử thủ công với nhạc thực trên signed release. Xem [Smart_Library_Playlists_v1.md](Smart_Library_Playlists_v1.md). Bản APK phía dưới là lịch sử.

> **BẢN DEMO MỚI NHẤT — OFFLINE RELIABILITY & LOCAL BACKUP v1 FINAL (2026-11-07):** `dist/MusicFlow-1.0-Demo-OfflineBackupV1-Final-Android.apk` (**31.409.853 bytes**, SHA256 `088AAA5118CC4D820E5251A321E03963DD11F63D5F41A65466C891EB6F86E08A`). Playlist có kiểm tra mất tệp/chọn lại bằng SAF, sao lưu/khôi phục metadata JSON qua DocumentsUI, nhập tạo bản sao không ghi đè. **193/193 JVM + 85/85 Android regression + 1/1 WAV SAF + 1/1 JSON picker + 4/4 Web Admin PASS**. Release-mode ký QA, cài/cold launch API30 và nhìn thấy Xuất/Nhập trên APK Final PASS. Xem [Offline_Reliability_Backup_v1.md](Offline_Reliability_Backup_v1.md). Các bản APK bên dưới là lịch sử; không Google Play/production.

> **BẢN DEMO GIAO DIỆN MỚI NHẤT — OCEAN FLOW UI v1 (2026-11-07):** `dist/MusicFlow-1.0-Demo-OceanFlowUIV1-Final-Android.apk` (**31.344.317 bytes**, SHA256 `2FA0D0A5C79E2DA9768429CE18FCE62BD80CE4F52B38BF3B72BAA576579B9400`). Ocean Flow Dark/Light, đồng bộ Home, Player, Mini Player, Search/Library/Playlist và Account/Demo Center. **190/190 JVM + 77/77 Android regression + 1/1 real SAF + 4/4 Web Admin PASS** sau khi sửa status bars. Chính APK Final đã ký/cài/cold launch API30 và được chụp ảnh Home/Account/Demo Center. Vẫn là **APK sideload QA**, không phải Google Play/production. Xem [Ocean_Flow_Visual_Refresh_v1.md](Ocean_Flow_Visual_Refresh_v1.md). Các APK nhắc sau đây là phiên bản lịch sử.

> **BẢN DEMO MỚI NHẤT — DEMO CENTER 9 ACTOR (2026-11-07):** `dist/MusicFlow-1.0-Demo-CenterV1-Android.apk` (**31.327.933 bytes**, SHA256 `47FBE9353CE700F432FE216F127AF2D50FBB35E3097607D5A752A697A352C481`). **190/190 JVM + 71/71 Android regression + 1/1 SAF thật + 4/4 Web Admin PASS**. Trên chính APK Release QA đã cài/cold launch API30 và chọn được Owner mô phỏng từ Guest. Có 9 actor trình diễn offline, kịch bản tương tác lưu tiến độ và đăng ký/đăng nhập Demo local; **không** cấp Owner/Admin/Premium thật. Chi tiết [Trung_Tam_Trinh_Dien_MusicFlow_v1.md](Trung_Tam_Trinh_Dien_MusicFlow_v1.md). Các bản APK nhắc phía dưới là lịch sử.

> **BẢN DEMO MỚI NHẤT — THANH TOÁN PREMIUM GIẢ LẬP v1 (2026-11-07):** `dist/MusicFlow-1.0-Demo-SimulatedBillingV1-Android.apk`, **31.278.781 bytes**, SHA256 `302E6415A56B1A0801BF92AA65C2A92A89D2446A7D31D45C22FAE02A9A3484C7`. **186/186 JVM + 61/61 Android regression + 1/1 SAF thật + 4/4 Web Admin PASS**; chính APK ký QA đã cài/cold launch, Guest mua thử và biên nhận được khôi phục sau restart PASS. Mọi thanh toán/giá/biên nhận đều **giả lập trên thiết bị**, không thu tiền hoặc cấp Premium thật. Xem [Thanh_Toan_Premium_Gia_Lap_v1.md](Thanh_Toan_Premium_Gia_Lap_v1.md). Các phiên bản APK được liệt kê phía dưới chỉ là hồ sơ lịch sử.

> **CẬP NHẬT NGHIỆM THU MỚI NHẤT — 2026-11-07:** bản APK demo ưu tiên hiện tại là **`dist/MusicFlow-1.0-Demo-AcceptanceV1-Android.apk`** (31.213.245 bytes; SHA256 `7D7E1D0131DC078CAA6394F1FC14F43BBB8820895FFF260C2BEF4C399CC95889`). **180/180 JVM + 55/55 Android regression + 1/1 DocumentsUI SAF thật + 4/4 Web Admin PASS**. Bản APK mới đã ký QA, cài và cold launch API30 PASS. Xem **[Nghiem_Thu_Demo_2026_11_07.md](Nghiem_Thu_Demo_2026_11_07.md)** và **[Checklist_Acceptance_Demo_MusicFlow_v1.md](Checklist_Acceptance_Demo_MusicFlow_v1.md)**. Bảng thông số `MusicFlow-1.0-Demo-Android.apk` ở các mục phía dưới là **hồ sơ lịch sử ngày 2026-11-01**, không phải checksum của bản demo mới.

**Ngày:** 2026-11-01  
**Loại dự án:** dự án báo cáo/trình diễn, không phát hành thương mại  
**Tình trạng:** **HOÀN THÀNH BẢN DEMO/BÁO CÁO — TẠM DỪNG** trong phạm vi đã được chủ dự án chốt  
**Git HEAD nền khi tạo artifact:** `1b8491d19e76b72f541e277bb95cc7506a5c9939`. Các thay đổi Part 4 và bàn giao demo đang trong working tree, chưa commit/push tại thời điểm viết.

## 1. Mục tiêu và điểm dừng

Xây dựng một ứng dụng Android nghe nhạc tên **MusicFlow** đủ để trình bày chức năng nghe nhạc, thư viện và kiến trúc trong báo cáo, có một **APK tải/cài trực tiếp** ngoài cửa hàng ứng dụng. Chủ dự án không yêu cầu email thật, tài khoản Google thật, đồng bộ hai thiết bị thật, kiểm thử điện thoại thật, Google Play, thanh toán thật hoặc phát hành công khai. Các phần chưa có E2E phải được ghi rõ, không xem như đã nghiệm thu.

**Điểm dừng được áp dụng:** hoàn tất APK demo đã ký/cài được, chạy thử các luồng quan trọng bằng emulator, tài liệu hướng dẫn cài và báo cáo tình trạng thực tế; **tạm ngưng phát triển sau bàn giao**, chỉ mở thêm khi có yêu cầu mới.

## 2. Thông tin tệp APK bàn giao

| Thuộc tính | Giá trị |
|---|---|
| Tệp | `MusicFlow-1.0-Demo-Android.apk` |
| Đường dẫn trên máy phát triển | `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-Android.apk` |
| Kích thước | **30.230.097 byte** |
| SHA-256 | `ACE9AD765F75C7939C513210E2BBC1555F95E6504636303B423FB70598C2E750` |
| Package | `com.example.musicflow` |
| Version | `versionName=1.0`, `versionCode=1` |
| minSdk / targetSdk | **29 / 36** |
| Loại APK | `assembleRelease` (không debuggable), `zipalign`, `apksigner` |
| Chữ ký | **Android Debug certificate của máy phát triển**, dùng riêng cho demo |
| Kênh phân phối | **Chỉ tệp APK cài trực tiếp**, không Play Store |
| Tình trạng production release | **KHÔNG**; APK demo, không có khóa production |

**Đây là tệp APK thực tế nằm tại máy phát triển, không phải sandbox attachment hay liên kết tải công khai.** Người dùng có thể lấy tệp từ đường dẫn trên máy hoặc tái tạo bằng `scripts/build-musicflow-demo-apk-v1.ps1` nếu có source và Android SDK. Build output `dist/*.apk` được loại trừ khỏi Git; không mặc định đưa APK lên GitHub.

## 3. Công nghệ và cấu trúc đã triển khai

- **Android native Kotlin, Jetpack Compose:** giao diện điều hướng và màn hình MusicFlow.
- **Android Media3 ExoPlayer + MediaSessionService:** phát nhạc, điều khiển hệ thống, phát nền và notification.
- **MediaStore + SAF:** nhạc offline có sẵn/tự tải, quyền URI và truy cập tệp cục bộ.
- **Room:** nền tảng dữ liệu playlist, history và queue persistence.
- **Tầng dịch vụ online / Supabase và tài khoản:** đã có source và cấu trúc backend/RLS theo các mốc trước; **không xem là E2E thực tế chỉ vì có source**.

## 4. Kết quả kiểm thử được xác minh

### 4.1. Chính APK bàn giao — Android emulator API 30

Chạy độc lập với **đúng file `dist/MusicFlow-1.0-Demo-Android.apk`** bằng `-DemoApkPath`, không ký lại trong bước smoke:

| Phép thử | WAV | MP3/FLAC |
|---|---|---|
| Xác minh chữ ký và cùng SHA-256 với APK bàn giao | PASS | PASS |
| Cài mới APK release-mode, mở UI và Library | PASS | PASS |
| MediaStore nhận hai file QA tổng hợp | PASS | PASS |
| Đọc metadata artist/album và thời lượng media thật | Không nằm trong gate WAV | PASS |
| Phát bài A, Pause, Resume, Next sang bài B | PASS | PASS |
| MP3 phát, Next sang FLAC phát | Không áp dụng | PASS |
| Phát nền sau Home, thông báo MediaStyle | PASS | PASS |
| Gỡ đúng app và file/row MediaStore do QA tạo, không còn dấu vết | PASS | PASS |
| Kết quả | **FINAL_RESULT=PASS** | **FINAL_RESULT=PASS** |

Log có thể đối chiếu:

- `qa-logs/demo-apk-package-v1.log` — build/ký/verify/badging/checksum **PASS**.
- `qa-logs/demo-apk-smoke-wav-api30-v1.log` — đúng APK bàn giao, phát WAV **PASS**.
- `qa-logs/demo-apk-smoke-codec-api30-v1.log` — đúng APK bàn giao, MP3/FLAC và metadata **PASS**.

Tệp WAV, MP3, FLAC trong QA được tạo tổng hợp từ âm thanh không có lời bằng công cụ test, **không sử dụng nhạc bản quyền của bên thứ ba**.

### 4.2. Những bằng chứng từ các mốc trước

- `Full Product Regression Gate v2 Part 1`: **105 JVM PASS**, Android API 30 **112 PASS / 0 FAIL / 3 SKIP** (115 bài tổng cộng); ba SKIP là các bài SAF theo pha riêng, không phải full PASS 115/115. Bằng chứng `docs/Full_Product_Regression_Gate_v2_Part_1.md`.
- `Release Candidate QA / Packaging Part 1`: kiểm tra APK release-mode ký QA, cài mới và nâng cấp đồng chữ ký giữ dữ liệu marker **PASS**.
- `Release Playback Smoke Part 2–3`: WAV trên emulator API **29/30/33/36**: Library/MediaSession/điều khiển/phát nền/MediaStyle **PASS**. Đây là **smoke**, không phải 115 bài instrumentation chạy trên APK release.
- `Release Readiness Part 4`: phát **MP3/FLAC** trên API 30 **PASS**; `assembleRelease` / `bundleRelease` và **105 JVM PASS**, AAB hiện **chưa ký production**. Chi tiết `docs/Release_Candidate_QA_Packaging_v1_Part_4.md`.

## 5. Phạm vi chức năng đã có và giới hạn trình bày

| Nhóm chức năng | Có thể trình bày gì | Giới hạn phải nói rõ |
|---|---|---|
| Offline và Media Player | File cục bộ qua MediaStore/SAF, Play/Pause/Next, tốc độ, phát nền, MediaSession | Phát nhạc WAV/MP3/FLAC đã kiểm tra trên emulator; không khẳng định chất lượng âm thanh trên phần cứng thật |
| Thư viện, tìm kiếm, playlist, queue, history | Các chức năng có source, JVM/UI/instrumented test theo các mốc đã lưu | Không phải mọi ngõ ngách UI đều chạy E2E trên bản APK demo cuối cùng |
| Lyrics, Sleep Timer, EQ, Crossfade | Các luồng đã có mã và test tại các mốc Android/JVM trước | Chưa đo gapless/âm thanh trên thiết bị thật; không mô tả mọi chất lượng âm thanh là đã nghiệm thu |
| Guest / Free / Premium | Mô hình quyền và giao diện, nền tảng Auth/entitlement | **Không dùng email thật, không cấp Premium thật, không có billing thật** |
| Online catalog, bản quyền phát | Cấu trúc danh mục, metadata quyền, tuyến streaming HTTPS khi hợp lệ | **Không có nghiệm thu phát nhạc licensed online thật** tại điểm dừng |
| Cloud metadata / Supabase | Kiến trúc opt-in metadata sync, RLS và các nền tảng backend đã triển khai | **Chưa chạy E2E tài khoản thật hoặc sync hai thiết bị thật** |

**Quy tắc nội dung:** nhạc offline người dùng tự lưu/tải phát như dữ liệu local; chỉ nhạc online được MusicFlow cung cấp yêu cầu quyền streaming hợp lệ. Không vô hiệu hóa ràng buộc quyền để phục vụ demo.

## 6. Những điều được chủ dự án miễn kiểm thử

**Không có kiểm thử điện thoại thật, email thật, Google Login thật, đồng bộ hai thiết bị thật, phát hành Google Play, khóa ký production hoặc thanh toán thật.** Đây không phải lỗi của bản demo: những việc trên được loại khỏi phạm vi bàn giao theo yêu cầu. Những kết luận chỉ có thể đưa ra bằng các phép thử đó giữ trạng thái **CHƯA KIỂM CHỨNG** thay vì PASS.

## 7. Hướng dẫn cài đặt và sử dụng

Xem `docs/Huong_Dan_Cai_Dat_MusicFlow_Demo_APK.md`. Trên emulator sạch:

```powershell
adb install ".\dist\MusicFlow-1.0-Demo-Android.apk"
adb shell am start -n com.example.musicflow/.MainActivity
```

Giữ đúng tệp và checksum để báo cáo có thể đối chiếu kết quả. Vì dùng chữ ký demo từ debug keystore cục bộ, không dùng bản này như bản phát hành công khai. Nâng cấp tương lai dùng chữ ký khác có thể yêu cầu gỡ bản cũ và xử lý dữ liệu.

## 8. Kết luận / điểm dừng

**Tại phạm vi được xác định (APK demo cài trực tiếp + kiểm thử emulator + báo cáo), MusicFlow đã có artifact cài được và bằng chứng chạy thật trên emulator.** Bản demo/báo cáo **đạt điểm dừng tạm thời**, không tương đương sản phẩm thương mại hoàn thiện hoặc bộ tính năng cloud/online đã E2E.

**Trạng thái sau bàn giao:** **HOÀN THÀNH BẢN DEMO/BÁO CÁO — TẠM DỪNG**. Không tiếp tục chạy roadmap production, tạo khóa production, Google/email thật hoặc test thiết bị vật lý trừ khi chủ dự án chủ động mở lại.
