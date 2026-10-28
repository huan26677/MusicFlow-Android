# MusicFlow — chuẩn bị bản Release Candidate (RC)

## Tình trạng

Cấu hình đóng gói đã được bổ sung nhưng **chưa kiểm thử build/cài đặt/smoke test và chưa duyệt phát hành**. Phần này được triển khai riêng theo yêu cầu hoàn thiện trước, kiểm thử và sửa bug sau. Không xem RC là bản production.

## Cách đóng gói

Trong PowerShell tại thư mục gốc dự án:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\package-release-candidate.ps1
```

Mặc định script tạo **release APK chưa ký**, tên `musicflow-rc-<sha>-unsigned.apk` trong thư mục `dist/`. APK chưa ký không cài trực tiếp lên thiết bị.

Để tạo bản ký bằng **keystore của chính bạn**, thiết lập bốn biến môi trường trong phiên PowerShell hiện tại: `MUSICFLOW_KEYSTORE_PATH` (đường dẫn tới tệp keystore), `MUSICFLOW_STORE_PASSWORD`, `MUSICFLOW_KEY_ALIAS`, `MUSICFLOW_KEY_PASSWORD`. Không đưa giá trị, keystore hay mật khẩu vào Git, chat, log hoặc tài liệu. Sau đó chạy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\package-release-candidate.ps1 -Signed
```

Script không tạo khóa ký và không ký bằng debug key. Nó yêu cầu đủ cả bốn biến ở chế độ `-Signed`, báo lỗi nếu cấu hình thiếu. Phần ký chỉ thành công nếu đường dẫn, alias và mật khẩu đúng.

Sau khi chạy thành công, `dist/` chứa APK và báo cáo gồm commit SHA, kiểu ký và checksum SHA-256. Thư mục `dist/` được bỏ qua trong Git. Nếu cần đóng gói APK đã build mà không build lại, có thể dùng `-SkipBuild` với chế độ tương ứng; chỉ dùng khi chắc chắn đầu ra APK thuộc commit đang được báo cáo.

## Kiểm thử để thực hiện sau khi hoàn thiện

Trước khi công bố, xác minh release build thực sự thành công; APK đã ký bằng khóa đúng; cài mới/cập nhật hoạt động; phát nhạc Library, Search, file picker, Playlist và History hoạt động; dịch vụ phát nền, notification, recovery sau force-stop; quyền trên API 29/30/33/36; đối chiếu checksum và commit thực tế. Khác biệt từ debug phải được kiểm tra bằng chính APK release đã ký. Không đánh dấu PASS chỉ nhờ kết quả test debug.

## Bàn giao

Hiện chưa có quyết định hình thức phân phối (APK nội bộ hay Google Play), keystore, chính sách versionCode/versionName chính thức, và phê duyệt. Không lưu khóa ký vào repository. Chỉ khi chủ dự án xác nhận phạm vi và toàn bộ cổng QA/release đạt yêu cầu mới chuyển trạng thái sẵn sàng phát hành.
