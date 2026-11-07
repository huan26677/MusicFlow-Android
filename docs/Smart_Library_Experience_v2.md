# MusicFlow — Smart Library Experience v2 (tìm trong bộ sưu tập)

**Ngày:** 2026-11-07. **Phạm vi:** APK demo Android phục vụ báo cáo, kế thừa Smart Library v1. Không đổi Supabase/Auth/Owner, quyền Premium, Room/playlist lưu thủ công, Media3 hay bản quyền nội dung online.

## Nâng cấp UX

- Trong **Thư viện → Thông minh**, chọn một trong 5 bộ sưu tập động như v1. Ô **Tìm trong [tên bộ sưu tập]** lọc ngay theo **tên bài hoặc nghệ sĩ**, hỗ trợ tiếng Việt **không dấu** (kể cả `đ` → `d`), giữ nguyên thứ tự của bộ sưu tập gốc.
- Hiện **số bài đang hiển thị / số bài gốc**. Bấm **Phát N bài** hoặc một dòng bài hát chỉ đưa **những bài đang hiển thị** vào queue, với đúng chỉ số dòng đã chọn. Không âm thầm phát các bài bị ẩn bởi bộ lọc.
- Không có kết quả tìm kiếm: báo rõ, cho **Xóa bộ lọc**, không làm mất playlist gốc. Khi chuyển sang bộ sưu tập khác, bộ lọc được xóa để tránh hiểu nhầm rằng bộ sưu tập mới không có bài. Tìm kiếm tối đa 80 ký tự.
- Một bộ sưu tập vốn trống: hiển thị nguyên nhân theo ngữ cảnh và nút **Xem mục Bài hát** trở về thư viện thường. Trên thiết bị không có nhạc hoặc quyền MediaStore chưa được cấp, dùng **Chọn tệp nhạc**/playlist thủ công cho SAF; file SAF riêng chưa mặc nhiên xuất hiện trong Smart như v1.
- Giữ các hành động thật đang có: tim yêu thích, thêm vào playlist và thao tác hàng đợi. Không tạo danh sách giả vào Room và không truyền dữ liệu ra Internet.

## Mã đã sửa

- `feature/library/SmartLibraryContent.kt`: bộ lọc không dấu dùng `SongSearch.matches`, số lượng kết quả, empty state có đường đi, queue khớp màn hình.
- `feature/library/LibraryScreen.kt`: kết nối nút **Xem mục Bài hát** cho cả thư viện rỗng và có nhạc.
- `app/src/androidTest/java/com/example/musicflow/ui/SmartLibraryUiTest.kt`: thêm 4 ca Compose cho lọc tên có dấu/nghệ sĩ, queue sau lọc, xóa lọc, đổi danh sách và điều hướng khi trống (tổng 9 ca).
- `scripts/build-musicflow-demo-apk-v1.ps1`: cho phép đóng gói bản v2 vào tên APK mới, không ghi đè APK v1.

## Cách demo

1. Quét nhạc offline vào Thư viện → **Thông minh** → **Mix của bạn**.
2. Nhập `duong` để tìm bài tên `Đường ...`; hoặc tên nghệ sĩ không dấu. Bấm **Phát** để chỉ nghe phần đang hiển thị.
3. Nhập một từ không tồn tại, bấm **Xóa bộ lọc**. Chuyển sang **Mới thêm** để thấy bộ lọc cũ đã được xóa.
4. Nếu danh sách trống, bấm **Xem mục Bài hát**; nhạc SAF chọn riêng vẫn có thể phát qua tính năng thủ công.

## Giới hạn nghiệm thu

Android UI tự động dùng `DeviceSong` dữ liệu QA; không suy ra mọi provider, TalkBack thủ công, file bị thu hồi quyền hoặc thiết bị vật lý đều đã được nghiệm thu. Chỉ phục vụ demo/báo cáo, không phải sản phẩm thương mại. Kết quả QA và hash APK được lưu trong log tương ứng sau khi đóng gói.

## QA và APK đã đóng gói

| Hạng mục | Kết quả |
|---|---|
| Toàn bộ JVM unit | **199/199 PASS** |
| Android UI / Room / Media3 / Account regression API30 riêng | **94/94 PASS**, trong đó **9/9 SmartLibraryUiTest**, thêm 4 ca v2 |
| DocumentsUI audio WAV SAF thật | **1/1 PASS** |
| DocumentsUI chọn JSON backup thật | **1/1 PASS** |
| Web Admin fail-closed/role | **4/4 PASS** |
| Gradle debug/androidTest/release, zipalign, apksigner, aapt, hash | **PASS** |
| Chính APK đã ký QA cài/cold launch API30 (`Status: ok`, `LaunchState: COLD`, PID còn) | **PASS** |

**Bản demo mới khuyến nghị:** `dist/MusicFlow-1.0-Demo-SmartLibraryV2-Android.apk` — **31.475.389 byte**, SHA256 **`1D1D907855A15119C17D454F567ED80C71BA9C11F32A05C8EB0C142717475C4E`**; package `com.example.musicflow`, `versionCode=1`, `versionName=1.0`, `minSdk=29`, `targetSdk=36`, `debuggable=false`. Ký bằng Android debug key dành cho QA/demo, không phải production release. APK v1 vẫn được giữ riêng; thư mục `dist` Git-ignore nên khi nộp cần chép file APK ngoài Git.

**Chứng cứ:** `qa-logs/musicflow-smart-library-v2-build.log`, `qa-logs/musicflow-smart-library-v2-focused-final.log`, `qa-logs/musicflow-smart-library-v2-regression.log`, `qa-logs/demo-apk-smart-library-v2-package.log`, `qa-logs/musicflow-smart-library-v2-release-smoke.log`. Lần chạy đầu của ca điều hướng không tìm thấy nút dưới vùng cuộn; đã sửa test cuộn tới item rồi chạy lại 9/9 và full 94/94 PASS. Không đánh đồng kiểm thử giao diện bằng `DeviceSong` QA với thao tác bằng nhạc thật trên signed APK.
