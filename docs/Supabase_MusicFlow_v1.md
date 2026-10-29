# MusicFlow — Supabase backend v1 (2026-10-29)

> **Cập nhật ngày 2026-10-29:** RLS và metadata bản quyền đã được nâng cấp qua migration `music_tracks_rights_attribution_v1`. Mô tả RLS/streaming ở phần v1 phía dưới là lịch sử, **không còn đủ để xuất bản nhạc**. Xem `docs/Licensed_Online_Rights_Attribution_v1.md`. Quyền streaming được xác minh, nguồn, ghi công và giấy phép hợp lệ là bắt buộc; công cụ publish cũ chưa gửi đủ trường này.

## Dự án đã kết nối

- URL dự án: `https://vzvxpbwrftlnyvuhfoqn.supabase.co`.
- Publishable key được lưu ở `secrets.properties` cục bộ, có trong `.gitignore`. Không đưa service-role/secret key vào Android.
- Trên máy mới, tạo `secrets.properties` (file riêng, không commit), đặt `SUPABASE_URL` và `SUPABASE_PUBLISHABLE_KEY` tương ứng; xem dashboard Supabase để lấy key.
- Bảng `public.music_tracks` đã tạo trực tiếp trên Supabase, migration `musicflow_licensed_catalog_v1`.

## Trạng thái dữ liệu

- Chưa upload bài hát / dữ liệu mẫu giả.
- Chính sách RLS v1 từng yêu cầu `is_published=true`, `rights_confirmed=true`, `stream_url`. **Hiện đã thay bằng chính sách chặt hơn:** cần cả `streaming_allowed`, `rights_verified_at`, `license_code`, `attribution_text`, `source_url` và license URL thích hợp.
- Role anon/authenticated chỉ được SELECT; không được INSERT/UPDATE/DELETE từ ứng dụng.
- Không tự đưa nhạc có bản quyền lên hệ thống nếu thiếu quyền cung cấp hoặc phát trực tuyến.

## Bảng music_tracks

| Cột | Kiểu | Nội dung |
|---|---|---|
| id | uuid | ID bài hát, mặc định sinh tự động |
| title | text | Tên bài |
| artist | text | Nghệ sĩ |
| album | text/null | Album |
| duration_ms | bigint | Thời lượng |
| stream_url | text/null | URL HTTPS có quyền phát, có thể phát trực tiếp trong Media3 |
| rights_confirmed | boolean | Chủ dự án xác nhận đã có quyền |
| is_published | boolean | Xuất bản bản ghi công khai |
| created_at | timestamptz | Ngày thêm |

Ứng dụng gọi Supabase Data API `/rest/v1/music_tracks` qua HTTPS, gửi publishable key trong header `apikey`. Truy vấn được RLS lọc theo bản ghi được công khai. Phân trang `limit=25&offset=...`.

## Về streaming

- **Từ Rights v1:** chỉ URL HTTPS **và metadata quyền đầy đủ, đã được xác minh từ backend** mới có thể phát; URL một mình không còn đủ.
- Không hỗ trợ token riêng từng người, signed URL hết hạn, DRM, download offline hoặc nội dung trả phí ở v1.
- Vì URL xuất hiện trong Data API công khai, **không dùng URL bí mật hoặc luồng phát yêu cầu quyền riêng tư** trong bảng này. Bước tiếp theo phải thiết kế máy chủ cấp URL ngắn hạn hoặc Supabase Edge Function xác thực quyền truy cập.
- Đặc biệt không sử dụng service_role hoặc secret key trong ứng dụng Android.
- Chưa QA phát online thực tế: hiện chưa có nhạc hợp lệ trên backend.

## Bước tiếp theo

Licensed Content Upload + Rights Verification v1: nhập ít nhất một bài do bạn sở hữu/được cấp phép, xác minh file URL và playback trên thiết bị; hoặc triển khai phát qua signed URL được cấp quyền trước khi xuất bản nội dung riêng tư.
