# MusicFlow — Mục tiêu hoàn thành dự án báo cáo và điểm dừng tạm thời

> **CẬP NHẬT 2026-11-07 — BỔ SUNG THANH TOÁN PREMIUM GIẢ LẬP:** `dist/MusicFlow-1.0-Demo-SimulatedBillingV1-Android.apk` là bản APK sideload hiện hành (31.278.781 bytes, SHA256 `302E6415A56B1A0801BF92AA65C2A92A89D2446A7D31D45C22FAE02A9A3484C7`). **186/186 JVM, 61/61 Android regression, 1/1 DocumentsUI thật, 4/4 Web Admin PASS**. Luồng mô phỏng chọn gói, biên nhận, lỗi, gia hạn, hủy và khôi phục sau restart đã kiểm thử. **Vẫn giữ nguyên điểm dừng APK báo cáo**: không tiền thật, không billing/Play, không tự đổi quyền Supabase, không yêu cầu E2E tài khoản hoặc thiết bị vật lý. Xem `docs/Thanh_Toan_Premium_Gia_Lap_v1.md`.

> **CẬP NHẬT 2026-11-07 — ĐẠT NGHIỆM THU BẢN DEMO MỚI:** APK được ưu tiên bàn giao đã chuyển sang `dist/MusicFlow-1.0-Demo-AcceptanceV1-Android.apk` (31.213.245 bytes, SHA256 `7D7E1D0131DC078CAA6394F1FC14F43BBB8820895FFF260C2BEF4C399CC95889`). **180 JVM + 55 Android regression + 1 Android DocumentsUI SAF thật + 4 Web Admin PASS**, ký/cài/cold launch chính APK API30 PASS. Xem `docs/Nghiem_Thu_Demo_2026_11_07.md` và `docs/Checklist_Acceptance_Demo_MusicFlow_v1.md`. **Vẫn giữ nguyên điểm dừng dự án báo cáo**, không chuyển sang production, không yêu cầu JWT live 8 persona, không Play/billing/streaming thiếu giấy phép. Các kiểm thử và APK cũ ngày 2026-11-01 bên dưới là hồ sơ lịch sử.

**Quyết định của chủ dự án: 2026-11-01.** Tài liệu này được ưu tiên hơn các kế hoạch phát hành/Google Play/kiểm thử tài khoản thật nêu trong các mốc cũ.

## Mục tiêu

**Hoàn thiện MusicFlow đủ để trình bày, kiểm thử bằng emulator và cung cấp một tệp APK có thể tải về, cài đặt trực tiếp (sideload) phục vụ báo cáo/demo; sau đó tạm dừng phát triển.**

Đây **không phải phát hành thương mại**, không cần đưa lên Google Play, không cần công khai APK trên Internet và không mặc định triển khai dịch vụ có người dùng thật.

## Các việc không làm trong phạm vi báo cáo hiện tại

- **Không dùng email thật** để đăng ký/xác thực; không yêu cầu Google Login với tài khoản thật.
- **Không kiểm thử trên điện thoại/thiết bị vật lý**, chỉ dùng Android emulator và kiểm tra build/artifact. Những hành vi chỉ đo được bằng thiết bị thật được ghi *chưa kiểm chứng*, không gắn nhãn PASS.
- **Không kiểm thử đồng bộ cloud trên hai thiết bị thật**; các chức năng cloud/account có thể trình bày dưới dạng foundation, giao diện hoặc mô phỏng phù hợp với phạm vi demo, và phải ghi rõ tính năng nào chưa E2E.
- **Không phát hành thật** qua Google Play, không mở thanh toán/billing thật, không triển khai công khai cho người dùng ngoài phạm vi báo cáo.
- **Không yêu cầu khóa ký production hoặc Play App Signing** trước điểm dừng. Một APK demo có chữ ký QA phù hợp có thể cài đặt bằng sideload; ghi rõ giới hạn chữ ký và nâng cấp ứng dụng.

## Tiêu chí đạt điểm dừng

1. Các chức năng đã chọn cho bản demo có thể mở và thao tác trong ứng dụng, ưu tiên nghe **offline từ file người dùng có sẵn/tự tải** (không xét duyệt giấy phép streaming), thư viện cá nhân, queue, playlist, lịch sử, tìm kiếm, UI và chức năng phát nhạc.
2. Các màn hình online, cloud, Free/Premium và tài khoản có thể tồn tại để mô tả kiến trúc và định hướng, **nhưng không được tuyên bố đã chạy E2E nếu thiếu dịch vụ, tài khoản hoặc nội dung có quyền hợp lệ**. Chỉ phát nhạc online do MusicFlow cung cấp khi thực sự có quyền streaming; không cố vượt kiểm tra bản quyền.
3. Build + bài test phù hợp trên emulator hoàn tất; không còn lỗi chặn việc chạy demo đã xác nhận. Nếu có kiểm thử bị bỏ qua hoặc hạn chế, phải ghi vào báo cáo.
4. Tạo **một APK cài được**, kiểm tra chữ ký và cài mới/khởi động trên emulator; chạy smoke playback theo phạm vi đã chọn và ghi SHA-256, versionName, versionCode và nguồn build. Đặt tên rõ **APK demo/báo cáo**, không phải bản phát hành production.
5. Soạn tài liệu báo cáo/biên bản nghiệm thu mô tả chức năng đã hoàn thành, tính năng còn giả lập hoặc chưa E2E, bước cài APK, giới hạn và ý tưởng phát triển sau này.
6. Khi đủ các tiêu chí trên, cập nhật `docs/Ke_Hoach_Du_An.md` thành **“HOÀN THÀNH BẢN DEMO/BÁO CÁO — TẠM DỪNG”**. Không tự chuyển sang mốc phát hành, khóa ký production, đăng nhập thật hoặc kiểm thử máy thật nếu chưa có yêu cầu mới.

## Việc tiếp tục ngay sau quyết định này

Hoàn thiện và xác nhận **APK demo có thể tải/cài**, kiểm thử cơ bản trên emulator, rà soát các chức năng để trình bày và chuẩn bị tài liệu báo cáo. **Không tự động triển khai một quy trình release production** chỉ vì `assembleRelease` / `bundleRelease` thành công.

**Trạng thái cập nhật 2026-11-01:** **HOÀN THÀNH BẢN DEMO/BÁO CÁO — TẠM DỪNG** trong phạm vi đã chốt. APK demo thực tế đã tạo tại `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-Android.apk`, có chữ ký demo và SHA-256 đã đối chiếu, cài mới/phát WAV và MP3/FLAC trên chính APK này **PASS** ở emulator API 30. Tài liệu bàn giao và hướng dẫn cài đặt: `docs/Bao_Cao_Ban_Giao_MusicFlow_Demo_APK.md` và `docs/Huong_Dan_Cai_Dat_MusicFlow_Demo_APK.md`. Việc này **không đồng nghĩa sản phẩm thương mại, Auth/Cloud/online E2E hoặc thiết bị thật đã nghiệm thu**.
