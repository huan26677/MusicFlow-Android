# MusicFlow UI/UX Foundation v1 — trạng thái nhất quán và trợ năng

**Ngày:** 2026-10-29. **Mục tiêu:** cải thiện trải nghiệm sử dụng Android cá nhân không cần tài khoản, giữ nguyên các luồng Player/Playlist/MediaSession. Đây là một mốc **UI foundation**, không phải xác nhận thiết kế đầy đủ MusicFlow 1.0.

## Những phần đã triển khai

- Thành phần dùng chung `ui/components/MusicFlowStatusPanel.kt` với các trạng thái `LOADING`, `EMPTY`, `ERROR`, `PERMISSION`, `INFO`. Có tiêu đề, hướng dẫn phù hợp, trạng thái tải, nút hành động tùy chọn, `liveRegion=Polite` cho trình đọc màn hình, và test tag ổn định.
- Dùng chung trên **Home History**, **Library** (bao gồm Favorites/Album/Artists/Genres), **Search**, **Online Catalog** và **Queue Manager**; thao tác thử lại/cấp quyền/đi tới thư viện đều gọi callback hiện có, không tạo nguồn dữ liệu khác.
- Library khi thiếu quyền MediaStore vẫn cho chọn tệp riêng qua SAF; trạng thái rỗng hướng dẫn hành động mà không tạo nút `Chọn MP3` trùng lặp. Search có nút xóa query với nhãn TalkBack `Xóa nội dung tìm kiếm`.
- Online Catalog hiển thị lỗi kết nối và đường thử lại, trạng thái nguồn/danh mục trống. Loại bỏ nút `Tải lại` trùng nhau giữa thanh công cụ và bảng trạng thái.
- Mini Player gắn nhãn `Mở trình phát: tên bài, nghệ sĩ`, giữ nút Play/Pause tách biệt.
- Đổi token màu MusicFlow thành xanh ngọc đậm `#0F766E`, xanh ngọc sáng `#5EEAD4`, nền đêm `#101827`, nền sáng `#F5FAF9`, nhấn hổ phách. Giữ cơ chế System/Light/Dark đã có.
- Sửa bài regression `ThemeBatchTest` cũ dùng nhãn không tồn tại: thực tế chip trong Account là `Tối`, không phải `Chế độ tối`; thêm cuộn đến chip trước khi thử.

## Kiểm thử và bằng chứng

- Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest`: PASS.
- Android emulator Pixel 5 API 30 `emulator-5554`: **13/13 PASS**, gồm **10/10 `MusicFlowUiFoundationTest`** (các trạng thái, callback, TalkBack labels, tương phản màu), **2/2 `QueueManagerScreenTest`** và **1/1 `ThemeBatchTest`**.
- Script `scripts/run-musicflow-ui-foundation-v1-tests.ps1`, kết quả `qa-logs/musicflow-ui-foundation-v1.log`, marker `FINAL_RESULT=PASS`.
- Vòng UI test ban đầu bắt được **hai nút Tải lại** trùng và đã sửa UI. Vòng regression ban đầu phát hiện nhãn test chủ đề lỗi thời, đã sửa test đúng với giao diện thực; chạy lại **13/13 PASS**.

## Giới hạn cần tiếp tục

- Chưa review thủ công bố cục trên các cỡ màn hình, chế độ chữ lớn/font scale, tương tác TalkBack trên thiết bị thật và chụp ảnh so sánh Dark/Light. Kiểm tra độ tương phản tự động mới phủ **primary** token, không phải tất cả màu và thành phần.
- Chưa hoàn thiện nhận diện thương hiệu cuối cùng, typography/motion, bố cục Now Playing nhiều kích thước, Account và Playlist error-state chi tiết.
- Không tự nâng trạng thái nhạc online thành `đã cấp phép`; catalog hiện chưa có E2E bài nhạc thật có xác minh quyền sử dụng.
- Không đổi database schema, không thay đổi âm thanh/MediaSession, không bật Google Login/email thật. Full Product 1.0 vẫn **CHƯA NGHIỆM THU**.

## Bước kế tiếp

**MusicFlow UI/UX Polish v2:** kiểm thử adaptive layout + font scale, cải thiện Now Playing/Playlist/Account, nhãn và vùng chạm; tiếp theo là **Licensed Online Rights Metadata & Attribution v1** trước khi thử streaming E2E bằng bài có quyền.
