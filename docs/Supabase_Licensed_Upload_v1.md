# MusicFlow — Licensed Content Upload v1

## Trạng thái triển khai (2026-10-29)

- Supabase project: `vzvxpbwrftlnyvuhfoqn`.
- Hai bucket: `musicflow-private-drafts` (**private**) và `musicflow-public-audio` (**public**).
- Giới hạn: 50 MiB mỗi tệp; hỗ trợ MP3, M4A, FLAC, OGG, WAV.
- Bảng `public.music_uploads` lưu hash SHA-256, đường dẫn nháp, trạng thái, người xác nhận và tham chiếu giấy phép.
- RLS trên `music_uploads` **không có chính sách client**, đồng thời đã thu hồi quyền `anon` / `authenticated`. Chỉ quy trình quản trị được quyền xem/sửa.
- RPC `public.musicflow_publish_licensed_track` chạy bằng **SECURITY INVOKER** và chỉ role `service_role` có quyền EXECUTE. RPC cập nhật bản ghi nhạc và biên bản xuất bản trong cùng giao dịch.
- Chưa có file nhạc được upload; chưa có quyền sử dụng tác phẩm nào được tự động suy đoán.

## Quy trình bảo mật

1. **Stage**: kiểm tra phần đầu file (magic bytes), kích thước và SHA-256, tải nhạc lên bucket nháp riêng tư; tạo bản ghi `music_tracks` với `is_published=false` và `rights_confirmed=false`.
2. **Review**: chủ dự án xác minh quyền **lưu trữ, phát trực tuyến và công khai** nhạc, và giữ giấy phép/biên bản tại nơi quản trị. `license-reference` chỉ là tham chiếu, **không phải kiểm chứng pháp lý tự động**.
3. **Publish**: công cụ đọc lại bản nháp riêng tư, đối chiếu SHA-256/kích thước, yêu cầu `--confirm-rights`, tải bản đã duyệt vào bucket public và gọi RPC để công khai catalog.
4. Khi đã xuất bản, URL public có thể được truy cập bởi người biết đường dẫn; **không dùng quy trình này cho nội dung DRM, riêng tư, cần paywall hay chỉ cấp phép người dùng nhất định**.

## Công cụ quản trị trên Windows

File: `scripts/musicflow_licensed_upload.py`, dùng Python 3 và thư viện chuẩn; không thêm dependency.

**Chỉ dùng trên máy quản trị tin cậy**, không trong ứng dụng Android, web client, CI công khai hoặc GitHub Actions.

Lấy **secret API key dạng `sb_secret_...`** tại Supabase Dashboard → Settings → API Keys. Đây là quyền nâng cao; **không gửi khóa vào chat, không ghi trong source, lệnh Git, log hay screenshot**. Publishable key từ cấu hình Android **không đủ quyền upload**.

Trong PowerShell của máy cá nhân, có thể nhập secret mà không hiển thị rõ khi gõ:

```powershell
$secure = Read-Host -Prompt "Supabase secret API key" -AsSecureString
$env:MUSICFLOW_SUPABASE_SECRET_KEY = [System.Net.NetworkCredential]::new("", $secure).Password
```

Tạo bản nháp (thay đường dẫn và metadata bằng bài do bạn sở hữu hoặc có quyền sử dụng):

```powershell
python scripts/musicflow_licensed_upload.py stage --file "C:\Music\OriginalTrack.mp3" --title "Original Track" --artist "Composer" --duration-ms 180000
```

Lệnh sẽ in một `Track ID`. Sau khi **đã kiểm tra giấy phép** và có tài liệu tham chiếu, mới xuất bản:

```powershell
python scripts/musicflow_licensed_upload.py publish --track-id "TRACK_ID_SAU_KHI_STAGE" --license-reference "Hop dong so 123 cho phep stream cong khai" --reviewer "Chu du an" --confirm-rights
```

Xóa secret khỏi phiên PowerShell khi dùng xong:

```powershell
Remove-Item Env:\MUSICFLOW_SUPABASE_SECRET_KEY
```

**Không dùng URL stream từ bucket nháp**. Sau khi xuất bản, Android sẽ tự tải danh mục qua Supabase Data API với publishable key và Media3 có thể phát `stream_url` của bucket công khai.

## Kiểm tra và giới hạn

- Chạy các bài kiểm tra logic offline: `python -m unittest discover -s scripts -p test_musicflow_licensed_upload.py -v`.
- Supabase RLS/grants kiểm tra bằng công cụ Supabase; Android build được kiểm tra riêng.
- Chưa chạy upload/download/publish thực tế vì chưa có file nhạc và secret key quản trị.
- Không hứa hẹn rằng nhận diện magic bytes là kiểm tra tính hợp pháp hoặc kiểm tra toàn bộ file audio.
- Public storage URL là URL có thể truy cập; việc gỡ bài sau này còn cần cơ chế xóa bucket public và xử lý CDN cache.
- File trên 50 MiB và nguồn nội dung cần cấp phép riêng/DRM sẽ cần quy trình khác.

## Bước tiếp theo

**Licensed Streaming Smoke v1:** sau khi có file và quyền thực tế, stage → review → publish → xác nhận nhạc xuất hiện trong Online Catalog → chọn phát → kiểm tra buffering, lỗi mạng và điều khiển Media3. Tiếp theo mới phát triển Signed URL / Account / Private Content.
