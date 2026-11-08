# MusicFlow — Keyboard Accessibility & Indexed Search v8

**Ngày:** 2026-11-08. **Phạm vi:** đồ án Android nghe nhạc offline, không thay đổi Supabase/billing/streaming, Room schema, SAF grants hay dữ liệu người dùng.

## Đã triển khai

1. **Bàn phím và D-pad:** viền focus 3dp nổi bật theo màu theme khi tiêu đề nghệ sĩ/album được chọn bằng phím, không phụ thuộc gesture. Tiếp tục dùng semantic Role.Button và trạng thái Đang mở/Đang thu gọn. Kiểm thử `LibraryKeyboardAccessibilityV8Test` bơm phím Android (thoát touch-mode bằng D-pad) để xác nhận Enter mở/đóng album/artist và D-pad chuyển focus, không vô tình phát bài.
2. **Tìm kiếm nâng cao dùng lại chỉ mục:** `AdvancedMusicSearchIndex` chuẩn bị `LibrarySongSearchIndex` cùng Album, Artist, Genre và chuỗi không dấu một lần cho mỗi catalog. `SearchViewModel` sở hữu đúng một index ứng với `songs` đã quét, hủy index khi refresh/clear/error, giữ cơ chế debounce 150ms và từ chối kết quả cũ sau thay đổi query/catalog. Không cache toàn cục hoặc lưu file/danh sách bài của người dùng.
3. **Kiểm thử kết quả:** `AdvancedSearchIndexV8Test` so sánh với logic lọc cũ cho cả bốn loại kết quả, kiểm tra nhóm được tái sử dụng thay vì tạo mới, catalog giả lập 1.502 bài, thứ tự queue, query rỗng, quét lại và thu hồi quyền không giữ nhạc cũ.
4. **APK ký QA:** `dist/MusicFlow-1.0-Demo-KeyboardSearchIndexV8-Android.apk`. **31.606.461 bytes**, SHA-256 **`A9A74F7CA1DFF797B88DECF410939008E88D4E065ACFF1F92A11AC0715AFA3BE`**. zipalign/apksigner/AAPT PASS; package `com.example.musicflow`, minSdk29, targetSdk36, versionCode 1. **Ký QA/debug, không production/Google Play**. `dist/` không được Git theo dõi.

## Bằng chứng và giới hạn

- Build `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease`, `testDebugUnitTest` PASS. Log local `qa-logs/musicflow-search-index-keyboard-v8-build.log`.
- **Android 11 (API30):** `3/3` kiểm thử phím phần cứng giả lập và `5/5` kiểm thử chỉ mục/tái quét = **8/8 targeted PASS**. **199 JVM + 129/129 Android UI/hồi quy v7 + 8/8 bài v8 = 137/137 Android UI/hồi quy PASS**, MediaStore WAV/SAF/DocumentsUI/JSON picker và 4 Web Admin PASS theo log. Signed QA APK v8 đã cài và cold-launch API30 PASS (`LaunchState: COLD`), AVD API30 được xóa sau QA. Log `qa-logs/musicflow-keyboard-searchindex-v8-regression-api30.log`, `qa-logs/musicflow-keyboard-searchindex-v8-targeted-api30.log` và signed smoke local `qa-logs/musicflow-keyboard-searchindex-v8-signed-smoke-api30.log`.
- **Android 13 (API33):** 32 bài mục tiêu kế thừa v7 + 8 bài mới v8 = **40/40 targeted PASS**, gồm MediaStore WAV thực, DocumentsUI SAF persisted READ thực, 200% chữ, hàng đợi 600 bài, D-pad/Enter và tái sử dụng chỉ mục. **Signed QA APK v7→v8 nâng cấp thật 2/2 giai đoạn PASS**: dữ liệu playlist Room, favorites, persisted SAF WAV còn nguyên sau `adb install -r` không clear; signed v8 APK cold-launch API33 PASS. AVD QA API33 được xóa. Log `qa-logs/musicflow-keyboard-searchindex-v8-targeted-api33.log`, `qa-logs/musicflow-keyboard-searchindex-v8-signed-upgrade-api33.log`; smoke local `qa-logs/musicflow-keyboard-searchindex-v8-signed-smoke-api33.log`. **Không phải full 98 Android regression API33.**
- **Android 16 (API36):** 32 bài mục tiêu kế thừa v7 + 8 bài mới v8 = **40/40 targeted PASS** trên AVD QA biệt lập. Log `qa-logs/musicflow-keyboard-searchindex-v8-targeted-api36.log`. Chưa xác nhận signed v7→v8 upgrade và cold-start APK v8 trên API36 trong tài liệu này; không tự nâng thành PASS từ targeted tests. **Không phải full 98 Android regression API36.**
- Android hồi quy đầy đủ và signed APK smoke/upgrade có log riêng. **Không công bố PASS trước khi có transcript hoàn thành**.
- Fixture 1.502 bài **không phải benchmark latency/RAM với nhạc thật**, các phím UiAutomator trên AVD **không phải** chứng cứ TalkBack/keyboard Bluetooth thủ công trên điện thoại. Thử TalkBack voice navigation, D-pad dài, focus với dialog thật, tương phản trên album art/overlay và máy vật lý vẫn còn mở.

## Trình tự QA chuẩn

```powershell
.\scripts\run-keyboard-searchindex-v8-qa.ps1 -Serial emulator-5580 -FullRegression
.\scripts\run-signed-upgrade-v8-qa.ps1 -Serial emulator-5580
```

**Chỉ chạy các script trên AVD QA mới, đã xác minh danh tính; không chạy trên điện thoại thật hoặc thiết bị người dùng.** Signed upgrade cài v7, seed fixture Room/favorites/WAV SAF persisted READ, cài v8 `install -r` và verify dữ liệu giữ nguyên.
