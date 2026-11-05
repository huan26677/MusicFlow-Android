# MusicFlow — Search UX v3 (APK demo/báo cáo)

**Ngày:** 2026-11-05 (Asia/Ho_Chi_Minh)  
**HEAD nền:** `b33b6ec200dc0e85fbfaf9581cc90490374988c5`, `main = origin/main` và Git sạch đầu lượt.  
**Phạm vi:** giao diện tìm kiếm nhạc thiết bị, hướng dẫn trạng thái rỗng/thiếu quyền và điều hướng chọn tệp; không phát hành thật.

## Những điểm đã cập nhật

1. **Dễ hiểu nội dung được tìm:** ô nhập mang nhãn **Tìm nhạc trên thiết bị**, hướng dẫn tìm bằng tên bài hát, album, nghệ sĩ hoặc thể loại, **có thể nhập không dấu**. Không quảng cáo sai rằng Search tìm trên catalog online hoặc tất cả tệp SAF chưa được Android quét.
2. **Dễ chọn đúng nhóm kết quả:** các tab Bài hát/Album/Nghệ sĩ/Thể loại có **số kết quả mỗi nhóm** sau khi hoàn tất lọc. Đang lọc thì giữ nhãn ổn định, không hiển thị số 0 tạm làm người dùng hiểu nhầm. Test tag `search-category-songs/albums/artists/genres` thay thế selectors bằng chữ cố định trong test cũ.
3. **Trạng thái rỗng có hướng xử lý:** khi không có bài hát đã quét hoặc chưa cấp quyền MediaStore, nút **Mở Thư viện để chọn tệp nhạc** đưa về tab Thư viện, nơi đã có SAF picker. Nếu bài hát không khớp nhưng Album/Nghệ sĩ/Thể loại có kết quả, thông báo hướng dẫn chuyển tab thay vì coi toàn bộ tìm kiếm thất bại.
4. **Xóa từ khóa và làm mới rõ ràng:** nút Close có mô tả TalkBack **Xóa nội dung tìm kiếm**, nút **Xóa tìm kiếm** khi không có kết quả. Chỉ hiển thị nút Quét lại ở đầu trang khi thư viện đã tải và có bài, tránh trùng với nút thử lại của panel trạng thái rỗng/chưa tải/lỗi.
5. **Chữ lớn:** tiêu đề Tìm kiếm và nút Quét lại xếp dọc khi font scale từ 135% hoặc chiều rộng dưới 360dp. Bàn phím hiển thị tác vụ Search.

Không thay đổi `SongSearch` (tìm không dấu có sẵn), thuật toán lọc/tần suất debounce, Media3 playback, chức năng playlist, Cloud, Supabase Auth hoặc quy tắc cấp phép nhạc online.

## Kiểm thử tập trung (không chạy full regression)

| Phạm vi | Kết quả |
|---|---|
| `:app:assembleDebug :app:assembleDebugAndroidTest` | **PASS** |
| `MusicFlowSearchUxV3Test` (4 UI): thiếu quyền có lối chọn tệp, thư viện không có nhạc, số kết quả và chuyển nhóm Album, cỡ chữ 180% | **4/4 PASS** |
| `MusicFlowUiFoundationTest`: thông báo lỗi có Thử lại; nút xóa query có accessibility label | **2/2 PASS** |
| `AdvancedSearchRuntimeTest#searchRouteAlbumArtistAndGenreFilterTabsUseRealScanner`: tìm qua scanner MediaStore giả lập bằng fixture WAV thử nghiệm rồi chọn nhóm | **1/1 PASS** |
| Chính APK release-demo mới: install -r, cold start, Trang chủ → Tìm kiếm → Thư viện → thấy Chọn tệp nhạc | **PASS** |
| Chữ ký demo, `apksigner verify`, `zipalign`, `aapt badging` và checksum SHA-256 | **PASS** |

Trong lượt QA đầu, bài font lớn tìm thấy **hai nút Quét lại** cùng lúc; đã sửa theo trạng thái và chạy lại **4/4 PASS**. Bản test runtime cũ được đổi từ selector bằng chữ sang test tag để thích ứng nhãn tab có đếm kết quả.

QA chỉ chạy trên AVD mới `MusicFlow_SearchUX_V3_QA_API30` / Android API 30 / `emulator-5560`, không đụng `emulator-5556` đang có của người dùng. Dữ liệu MediaStore dùng các fixture tổng hợp thuộc test, có cleanup. Không kiểm thử thiết bị thật hoặc full suite.
Sau khi hoàn tất, **AVD QA đã được xóa cả cấu hình và dữ liệu**. Emulator `emulator-5556` của người dùng vẫn giữ nguyên.

## APK demo mới

```text
E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-SearchUXV3-Android.apk
```

- Kích thước: **30.787.261 byte**.
- SHA-256: `95F79772A410F33EEF4A4E90A99931BF9548B3C51D4038451C4C1C6FFB6C1BC5`.
- Package `com.example.musicflow`, version `1.0` (versionCode 1), minSdk 29 / targetSdk 36.
- **Release-mode không debuggable, ký Android debug keystore chỉ dành cho QA/demo**, không ký/phát hành production hay Play Store.
- `dist/` bị Git ignore; APK và file checksum `.sha256` ở máy phát triển, không tự upload GitHub. Các APK demo cũ được giữ nguyên.
- Build lại qua script `scripts/build-musicflow-demo-apk-v1.ps1 -OutputName MusicFlow-1.0-Demo-SearchUXV3-Android.apk` (từ chối đè nếu cùng tên nhưng checksum khác).

**Log:** `qa-logs/musicflow-search-ux-v3.log`, `qa-logs/demo-apk-search-ux-v3-package.log`.  
**Điểm dừng dự án:** chỉ APK demo có thể cài trên emulator và dùng cho báo cáo; các phạm vi sản phẩm chưa kiểm chứng tiếp tục được ghi trung thực.
