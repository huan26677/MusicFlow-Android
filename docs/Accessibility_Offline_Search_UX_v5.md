# MusicFlow — Accessibility & Offline Search UX v5

**Ngày phát triển:** 2026-11-08. **Phạm vi:** APK Android offline phục vụ báo cáo, không nâng quyền tài khoản/billing hoặc backend.

## Người dùng nhận được

1. **Tìm kiếm hợp nhất:** tìm tên bài/nghệ sĩ, album và thể loại từ MediaStore (nếu cấp quyền) và tệp âm thanh SAF đã chọn còn đọc được; nhập tiếng Việt có hoặc không dấu. Loại trùng theo URI và phát đúng danh sách hiện ra.
2. **Không ép cấp quyền quét máy:** mở Thư viện sẽ không tự bật Android permission dialog. Hai lựa chọn rõ ràng: quét nhạc thiết bị hoặc chọn tệp qua DocumentsUI. Tìm kiếm có thể làm việc ngay với các tệp SAF mà không cần READ_MEDIA_AUDIO.
3. **Cập nhật khi quay lại:** Tìm kiếm kiểm tra lại quyền quét và các tệp SAF còn đọc được, bỏ bài MediaStore khỏi kết quả nếu quyền Android bị thu hồi. Không xóa nhạc, playlist hay URI có quyền của người dùng.
4. **Chữ lớn/màn hình hẹp:** thanh công cụ Thư viện chuyển dọc khi cần; hàng bài hát giữ vùng Phát và gom hành động yêu thích/playlist/queue vào một menu riêng có nhãn dành cho hỗ trợ đọc màn hình.
5. **Không thay đổi** playback engine, Room, role RPC, Supabase hay thanh toán mô phỏng.

## Kỹ thuật

- `feature/search/OfflineSearchRepository.kt`: gộp `LocalMusicRepository` với danh sách SAF khả dụng từ `LocalPickedMusicStore.inspect()`; không quét MediaStore khi chưa có permission, ưu tiên metadata MediaStore khi URI trùng; nếu provider MediaStore lỗi thì vẫn giữ tệp SAF độc lập và còn đọc được.
- `SearchRoute`: nạp lại khi quay lại màn hình, danh sách shortcut SAF đổi hoặc quyền quét đổi; không chặn ô tìm kiếm khi thiếu quyền.
- `LibraryRoute`: bỏ yêu cầu permission tự động ở `LaunchedEffect(Unit)`, chỉ xin quyền sau khi người dùng chọn quét.
- `SongRow`: hành động trong menu khi font scale từ 135% hoặc chiều rộng nhỏ, nhãn semantics cho nút tùy chọn; `LibraryScreen` chuyển thanh hành động thành các nút toàn hàng.
- `AccessibilityOfflineSearchV5Test`: 6 bài Android mới về hợp nhất/loại trùng, fallback SAF, tìm/phát khi thiếu quyền, chọn quyền rõ ràng, menu chữ lớn, thanh công cụ hẹp.
- `run-accessibility-offline-search-v5-qa.ps1`: giữ regression v4 trên API30, kiểm thử mục tiêu v5 và smoke API33/36 trên QA AVD tách biệt.

## Nghiệm thu

**API30 — ĐẠT (kiểm thử tổng hợp cuối sau sửa chữ lớn):**

- Gradle `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease` **PASS**; **199/199 JVM**, không lỗi.
- **98/98** Android hồi quy cũ; **7/7** Library UX v4; **6/6** Accessibility & Offline Search v5; **4/4** Search UX v3; **2/2** Advanced Search runtime (MediaStore WAV thật, queue/playback): tổng **117 kiểm thử Android UI/hồi quy**.
- **2/2** MediaStore WAV thật; **1/1** DocumentsUI MainActivity persisted READ WAV; **1/1** playlist Quick Add SAF WAV; **1/1** JSON DocumentsUI; **4/4** Web Admin fail-closed. **Chính APK v5 ký QA** cài và cold-launch API30 thành công.
- Lỗi đã giải quyết: test chữ 180% từng không bấm được nút “Quét lại”; chuyển thao tác lên vị trí luôn thấy và cho cuộn trạng thái trống. **Chạy lại toàn bộ API30 sau sửa và PASS**, không chỉ sửa test.
- Bằng chứng: `qa-logs/musicflow-accessibility-search-v5-build.log`, `qa-logs/musicflow-accessibility-search-v5-final-regression-api30.log`, `qa-logs/musicflow-accessibility-search-v5-signed-apk-smoke-api30.log`.

**APK đề xuất nộp báo cáo:** `dist/MusicFlow-1.0-Demo-AccessibilitySearchV5-Android.apk` (**31.573.693 bytes**; SHA-256 `2A6D6C6D7B3D21217CD2D966278C8F8614ACFC7F44F3F162B089FBB87C5F30BB`). Đã kiểm tra zipalign, apksigner, aapt, SHA; package `com.example.musicflow`, versionName 1.0, minSDK29, targetSDK36. Chứng chỉ ký **QA/debug**, không phải production.

**API33 — ĐẠT trong phạm vi kiểm thử mục tiêu:** 6/6 v5, 4/4 Search UX v3, 7/7 Library UX v4, 2/2 MediaStore WAV thật, 1/1 MainActivity DocumentsUI SAF WAV thật và persisted READ: **20 bài Android kiểm thử mục tiêu PASS**; chính APK ký QA install/cold-launch PASS. Log `qa-logs/musicflow-accessibility-search-v5-targeted-api33.log`, `qa-logs/musicflow-accessibility-search-v5-signed-apk-smoke-api33.log`. **Không gọi là 98 test hồi quy API33**.

**Android 16 API36 — ĐẠT trong phạm vi kiểm thử mục tiêu:** **20/20** Android (6 v5 + 4 Search UX v3 + 7 Library UX v4 + 2 MediaStore WAV thật + 1 DocumentsUI SAF WAV thật/persisted READ) PASS trên isolated QA AVD Android 16; **chính APK v5 ký QA** install/cold-launch thành công. Bằng chứng `qa-logs/musicflow-accessibility-search-v5-targeted-api36.log` và `qa-logs/musicflow-accessibility-search-v5-signed-apk-smoke-api36.log`. AVD QA API30/33/36 đã xóa sau nghiệm thu; API33/36 **không phải** full 98-test regression.

## Giới hạn

- Chỉ nhạc offline hợp lệ đã cấp quyền; không copy hoặc thay đổi file gốc, không chuyển SAF permission khi sang máy khác.
- Kết quả kiểm thử tự động Compose accessibility không thay thế kiểm tra TalkBack thủ công hoặc contrast trên mọi loại máy.
- APK ký bằng chứng chỉ debug/QA để nộp báo cáo, không phải Play Store/production release.
