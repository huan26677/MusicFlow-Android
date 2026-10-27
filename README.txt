MusicFlow Real OpenMultipleDocuments SAF v1 — patch đề xuất (CHƯA CHẠY QA)

Sao chép đúng cấu trúc thư mục vào E:\Du_An\MusicFlow.
Tệp được thêm:
- app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt
- scripts/run-real-picker-saf-v1-tests.ps1

Chuẩn bị: một Android 11 (API 30) Emulator với AVD Pixel_5 và đóng những Emulator khác.
Chạy tại thư mục gốc: powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-v1-tests.ps1
Log: qa-logs/real-picker-saf-v1.log

Bộ kiểm thử dự kiến: 51 bài thông thường, 3 bài SAF giai đoạn được skip như thiết kế;
23 marker (20 cũ + 3 mới) bắt buộc PASS.
Bài mới dùng ACTION_CREATE_DOCUMENT để tạo file âm thanh test nhưng dùng UI MusicFlow
và ACTION_OPEN_DOCUMENT thật để chọn lại file; kiểm tra callback production lưu READ grant,
phát nhạc đúng URI. Chưa kiểm chứng persisted grant sau force-stop của URI từ picker.
Bài SAF Permission Persistence v1 đã kiểm tra force-stop đối với URI CreateDocument.

Chưa có log PASS; có thể cần điều chỉnh thao tác DocumentsUI theo API/system image cụ thể.
Không sửa logic Media3/PlaybackService; không xóa assertions khi test FAIL.
