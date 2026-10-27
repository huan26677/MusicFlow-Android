MusicFlow: tách QA để sửa lỗi nhanh (2026-10-27)

Nguồn đối chiếu: GitHub main commit d5ed525c, nơi RealPickerSafTest.kt còn đoạn hàm cũ sau selectRealDocument(), gây hàng loạt lỗi Kotlin compile.

CÀI ĐẶT
1. Giải nén nội dung ZIP vào E:\Du_An\MusicFlow (giữ cấu trúc scripts/).
2. Trong PowerShell tại E:\Du_An\MusicFlow, chạy:
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\fix-real-picker-and-split-qa-v1.ps1

KIỂM TRA NHANH (không cần Emulator)
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1 -CompileOnly

KIỂM TRA RIÊNG REAL PICKER (Android 11 API 30 Emulator)
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1
   Chọn bằng chuột file musicflow_real_picker_*.wav mới tạo khi DocumentsUI xuất hiện.
   Log: qa-logs/real-picker-saf-fast-v1.log

KIỂM TRA ĐẦY ĐỦ - CHỈ SAU KHI FAST PASS
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-v1-tests.ps1
   Log: qa-logs/real-picker-saf-v1.log

Lưu ý: PASS nhanh không thay thế PASS hồi quy đầy đủ. Lệnh CompileOnly xác nhận mã biên dịch, không được dùng làm bằng chứng picker hoạt động. Script sửa Kotlin được thiết kế để dừng nếu không tìm được đúng version và vị trí test. Nếu GitHub có commit mới sửa cùng hàm, cần kiểm tra trước khi áp dụng.
