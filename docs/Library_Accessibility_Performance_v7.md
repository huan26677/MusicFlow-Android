# MusicFlow — Library Accessibility & Performance v7

**Phạm vi:** APK offline phục vụ báo cáo; nâng chất lượng thư viện và khả năng tiếp cận, không thay đổi streaming, tài khoản cloud, dữ liệu người dùng hay quyền backend.

## Tính năng triển khai

1. **Danh sách lớn không dựng toàn bộ:** `MusicCollectionsContent` (nghệ sĩ, thể loại, nhóm album ở Tìm kiếm) dùng các `LazyColumn` item riêng `Header / PlayAll / Track` thay vì đặt toàn bộ `group.tracks.forEachIndexed` trong một item. Có key ổn định theo nhóm và chỉ số, kể cả khi URI trùng ở các thể loại khác nhau.
2. **Album thiết bị cũng ảo hóa:** `AlbumFavoritesContent` ở Thư viện sử dụng cùng chiến lược item phẳng, giữ nút yêu thích album, danh sách bài hát, phát toàn bộ, hàng đợi và vị trí phát đúng trong album.
3. **Trợ năng nhóm nhạc:** tiêu đề nhóm có `Role.Button`, mô tả trạng thái `Đang mở/Đang thu gọn`, các nút `Phát toàn bộ` có vùng chạm tối thiểu 48dp.
4. **Kiểm tra màu tự động:** `ReadabilityContrast` tính tỷ lệ theo WCAG 2.x; test kiểm tra 6 cặp màu chữ/action thiết yếu trong theme Light và Dark phải đạt **≥ 4,5:1**. Đây không phải kiểm toán toàn bộ hình ảnh hoặc đánh giá TalkBack thủ công.
5. **6 kiểm thử Android v7:** flatten 600 bài không tạo row khi thu gọn, key/queue album, cuộn danh sách nghệ sĩ và album đến bài thứ 500 ở chữ 200%, kiểm tra trạng thái nhóm, màu light/dark đạt ngưỡng.

## Kiểm thử và bằng chứng

**Build đã xác minh:** `gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease` PASS. Log `qa-logs/musicflow-library-accessibility-v7-build.log`.

**APK QA-signed đã xác minh:** `dist/MusicFlow-1.0-Demo-LibraryAccessibilityV7-Android.apk` (31.590.077 byte; SHA-256 `215F2DC4B97274768D9E9870C10A6B3D3188ECCABAC9110976878AD6E2E515B0`); zipalign, apksigner, AAPT, manifest PASS, package `com.example.musicflow`, versionCode 1, versionName 1.0, minSdk29, targetSdk36, không debuggable. **Ký QA/debug, không phải Play Store/production; dist bị Git-ignore.**

**Android 11 / API30 — ĐẠT QA đầy đủ:** **199/199 JVM**, **98/98** Android regression + **7/7** Library v4 + **6/6** Search v5 + **4/4** Search UX v3 + **2/2** Advanced Search runtime + **6/6** Accessibility v6 + **6/6** Library Accessibility v7 = **129/129 Android UI/hồi quy PASS**. **2/2 MediaStore WAV thật, 1/1 MainActivity SAF persisted READ, 1/1 SAF Quick Add, 1/1 JSON DocumentsUI, 4/4 Web Admin PASS**. Chính APK v7 QA-signed đã install/cold-launch API30 PASS. Log `qa-logs/musicflow-library-accessibility-v7-full-regression-api30.log`, `qa-logs/musicflow-library-accessibility-v7-signed-smoke-api30.log`.

6 bài v7 xác nhận riêng: tạo 600 items chỉ sau khi nhóm mở, cuộn đến bài 500 ở font 200% trong nghệ sĩ và album rồi phát với queue và index đúng, key không trùng, mô tả trạng thái nhóm và các cặp màu chữ thiết yếu Light/Dark đều đạt mức tương phản **4,5:1**. Đây là **test fixture UI**, không phải benchmark RAM hoặc kết quả TalkBack thủ công.

**Android 13 / API33 — ĐẠT nhóm kiểm thử mục tiêu:** 6/6 Search v5 + 4/4 Search UX v3 + 7/7 Library v4 + 2/2 MediaStore WAV thật + 1/1 SAF DocumentsUI persisted READ thật + 6/6 Accessibility v6 + 6/6 Library v7 = **32/32 Android targeted PASS**. **Signed APK v6→v7 nâng cấp thật PASS (2/2 bước)**: tạo Room playlist/track + favorite + WAV SAF qua DocumentsUI với persisted READ, cài đè v7 bằng `adb install -r` không clear, xác nhận tất cả còn nguyên và WAV còn đọc được, dọn chỉ fixture. **Chính signed v7 APK cold-launch API33 PASS.** AVD QA API33 được xóa sau nghiệm thu. Log `qa-logs/musicflow-library-accessibility-v7-targeted-api33.log`, `qa-logs/musicflow-library-accessibility-v7-signed-upgrade-api33.log`, `qa-logs/musicflow-library-accessibility-v7-signed-smoke-api33.log`. Đây không phải full 98 regression API33.

**Android 16 / API36 — ĐẠT nhóm kiểm thử mục tiêu:** **32/32 Android targeted PASS** sau khi chỉnh QA cũ cho đúng thao tác: trên Android 16 bàn phím ảo có thể giữ focus/che dòng kết quả tìm kiếm. `LibraryPerformanceV4UiTest` đóng IME, cuộn theo khóa URI ổn định và chạm đúng nút phát có semantics thay vì node chữ; lớp Thư viện được chạy lại **7/7 PASS** rồi chạy lại toàn bộ đợt **32/32 PASS**. Không thay logic queue trong production để che lỗi test. Log `qa-logs/musicflow-library-accessibility-v7-targeted-api36.log` và `qa-logs/musicflow-library-accessibility-v7-api36-library-retest.log` (log retest riêng chỉ ở QA machine).

**Signed upgrade API36 — PASS:** cài đặt APK ký QA v6, tạo playlist Room/favorite và WAV SAF với persisted READ, cài đè v7 cùng chữ ký bằng `adb install -r` (không clear dữ liệu), kiểm tra playlist/favorite/SAF URI và WAV còn truy cập được; **2/2 giai đoạn PASS**. Log `qa-logs/musicflow-library-accessibility-v7-signed-upgrade-api36.log`. Chính APK v7 ký QA **cold-start API36 PASS**, log cục bộ `qa-logs/musicflow-library-accessibility-v7-signed-smoke-api36.log`; AVD được xóa sau QA.

**Chứng cứ Git / phiên AI sau:** Các transcript Android API30/33/36 và signed-upgrade API33/36 được lưu trong `qa-logs/` trên Git sau khi rà soát thông tin nhạy cảm. Log dài của Gradle, đóng gói và smoke test còn ở máy phát triển, không đưa lên Git. Brain v2 phân biệt bằng chứng ở Git (`verified_repo`) với file cục bộ (`verified_local`).

## Nghiệm thu cần chạy

- Build JVM/debug/AndroidTest/release; chạy hồi quy Android API30 và 6 bài v7 mới.
- API33/36: targeted v5/v6/v7 và signed APK cold-launch.
- **Signed APK v6 → v7** trên AVD QA API33/36, dùng fixture Room playlist/favorite/SAF persisted READ; kiểm tra file WAV còn đọc được sau `adb install -r` và cleanup chỉ fixture QA.
- Không dùng thiết bị thật/AVD cá nhân; máy vật lý, TalkBack manual, keyboard focus traversal thực tế và hiệu năng RAM/scroll đo bằng nhạc thật vẫn cần kiểm chứng.

**Lưu ý:** Hệ thống QA Android có thể có 600 bài nhạc *giả lập* trong test Compose, không thể được diễn giải thành benchmark hiệu năng thiết bị thật.

## Checklist nghiệm thu thủ công còn mở (không đánh dấu PASS tự động)

1. Trên máy Android có **TalkBack**, mở `Trang chủ → Thư viện → Nghệ sĩ → mở nhóm → Phát` bằng thao tác vuốt/đọc; kiểm tra tiêu đề nhóm, trạng thái mở/đóng, tên bài và các nút yêu thích. Không tự phát nhạc khi TalkBack chỉ di chuyển focus.
2. Mở Album và thể loại, cuộn nhóm chứa ít nhất 300 bài thật. Xác nhận menu bài hát, nút Phát toàn bộ, hàng đợi và vị trí bài phát không thay đổi.
3. Gắn bàn phím ngoài hoặc dùng D-pad: di chuyển focus qua các tab, chip, album, mở/đóng nhóm bằng Enter, quay lại bằng Back/Escape. Focus phải hiển thị và không mắc kẹt ở item ngoài màn hình.
4. Đổi Light/Dark và cỡ chữ 200%, kiểm tra trạng thái disabled/error, nút đang được chọn, album art và nội dung dài không che chức năng chính. Tính tương phản từng cặp thực tế nếu có lớp phủ hoặc artwork.
5. Chạy đo thời gian scroll, first frame, bộ nhớ với hàng trăm MP3/FLAC/WAV thật; lưu thiết bị, SDK, số bài, phương pháp đo. Chỉ sau đó mới ghi benchmark production-like.
