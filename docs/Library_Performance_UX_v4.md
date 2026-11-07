# MusicFlow — Library Performance & UX v4

**Ngày triển khai:** 2026-11-07. **Phạm vi:** APK Android demo/báo cáo, không phát hành thương mại.

## Những gì người dùng nhận được

1. **Thư viện → Bài hát:** tìm tên bài hoặc nghệ sĩ bằng tiếng Việt **có dấu hoặc không dấu**, hỗ trợ nhiều từ và xóa tìm kiếm trong một chạm. Khi chưa tìm thấy bài, ứng dụng giải thích cách sửa từ khóa; không xóa dữ liệu hoặc playlist.
2. **Phát đúng danh sách đang hiển thị:** tìm kiếm, sắp xếp hoặc chỉ xem bài yêu thích không làm phát những bài đã bị lọc. Thứ tự và vị trí bài được chọn được giữ đúng trong hàng đợi.
3. **Nhạc đã chọn:** tìm nhanh trong cả những file còn đọc được lẫn lối tắt file thất lạc; vẫn xem và nghe bài đã chọn mà không phải cấp quyền quét toàn máy.
4. **Quyền Android là lựa chọn:** màn hình không có quyền quét giải thích rõ sự khác nhau giữa **Cấp quyền đọc âm thanh** và **Tiếp tục với nhạc đã chọn**. Không dùng quyền giả hoặc đọc file ngoài phạm vi quyền đã được cấp.
5. **Chữ lớn, thư viện dài:** phần lọc, thông tin kết quả, sắp xếp và danh sách bài đều ở chung trong **một danh sách LazyColumn**, có thể cuộn. Các bài nằm xa vùng xem không bị dựng giao diện hàng loạt.

## Cải thiện kỹ thuật

- `LibrarySongSearchIndex` chuẩn hóa tiêu đề/nghệ sĩ **một lần khi dữ liệu/sắp xếp thay đổi**. Mỗi lần gõ chỉ chuẩn hóa từ khóa và duyệt chuỗi đã chuẩn bị; không chuẩn hóa lại toàn bộ metadata. Kết quả vẫn giữ nguyên thứ tự và loại trừ bài không khớp khỏi queue. Không có backend search, telemetry hoặc cache file ngoài máy.
- `DeviceMusicScanner.scan(context, includeGenres)`: danh sách thường chỉ truy vấn metadata cốt lõi. Trước đây bước quét luôn gọi bảng Genres và một truy vấn Members cho mỗi genre; nay hoãn bước này **đến khi mở Thể loại**. Quét mặc định qua các caller cũ giữ tham số `includeGenres=true` để tránh đổi ngầm API. `MediaStoreLocalMusicRepository.getSongs()` dùng bản nhẹ; `getSongsWithGenres()` dùng bản đầy đủ. `LibraryViewModel.refresh(includeGenres)` hủy lần quét cũ và đặt Loading theo hành vi cũ.
- Kiểm tra hủy coroutine định kỳ khi duyệt cursor lớn; không nuốt `CancellationException` trong đọc metadata thể loại. Chỉ lưu genre cho ID thực có trong thư viện.
- `LibraryRoute` yêu cầu full metadata khi người dùng chọn tab Thể loại và tiếp tục yêu cầu đầy đủ khi nhấn Quét lại sau đó.
- Các trạng thái tìm kiếm có **số bài đang hiển thị / tổng**, nút xóa có nhãn contentDescription để hỗ trợ trình đọc màn hình.

## Phạm vi và giới hạn

- Không thay đổi Room schema, dữ liệu playlist, phát lại Media3, nguồn online, Supabase, thanh toán hay quyền tài khoản.
- Kết quả đo chính xác về thời gian quét và mức sử dụng RAM còn phụ thuộc thiết bị/provider. Kiểm thử với 1.500+ bài mô phỏng xác nhận logic và số lượng, **không phải benchmark hiệu năng máy thật**.
- Vẫn chỉ đọc nhạc Android khi được cấp quyền hoặc tệp SAF có quyền đọc riêng. Genre phụ thuộc provider Android.
- Bộ kiểm thử chính và APK ký QA được ghi nhận trong mục nghiệm thu sau khi hoàn tất.

## Nghiệm thu

**Nghiệm thu API30 emulator độc lập — PASS:** `199/199` JVM unit; `98/98` Android hồi quy (Smart Library/Account/Media3/Playlist), `7/7` Android UX v4 (1.502 bài metadata mô phỏng; queue/index sau lọc; tìm không dấu trong bài đã chọn; clear; quyền quét tùy chọn; genre chọn mới tải; chữ lớn 165%); **105 Android UI/regression** tổng. `2/2` Android MediaStore WAV thực (bao gồm đường quét nhanh không load genre và fallback WAV dài), `1/1` MainActivity DocumentsUI chọn WAV thực + persist READ + catalog nhạc đã chọn, `1/1` WAV Quick Add DocumentsUI, `1/1` JSON DocumentsUI, `4/4` Web Admin fail-closed. Không dùng tài khoản/thiết bị thật.

**APK ưu tiên nộp:** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-LibraryUXV4-Android.apk`, **31.540.925 bytes**, SHA256 **`5D1F9D6A5A8CE3CD50BA4C253A3F83B8D5459A23949A1F629ECB1671562977EA`**; `versionName=1.0`, `versionCode=1`, `minSdk=29`, `targetSdk=36`, `debuggable=false`. Gradle Release, zipalign, apksigner/aapt, SHA PASS; **chính APK ký QA cài/cold launch API30 PASS**. APK dùng chứng chỉ QA debug, **không phải production/Play Store**. `dist` được Git-ignore; nộp file trực tiếp ngoài Git.

**Chứng cứ:** `qa-logs/musicflow-library-ux-v4-build.log`, `qa-logs/musicflow-library-ux-v4-regression.log`, `qa-logs/demo-apk-library-ux-v4-package.log`, `qa-logs/musicflow-library-ux-v4-signed-apk-smoke.log`. Tài liệu vẫn lưu mốc Offline Discovery v3 riêng.
