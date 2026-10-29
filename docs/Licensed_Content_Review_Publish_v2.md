# MusicFlow — Licensed Content Review & Publish v2

**Ngày:** 2026-10-29. **Phạm vi:** chỉ nội dung **online do MusicFlow quản lý và cung cấp**. Tệp nhạc người dùng tự lưu/tải về máy qua MediaStore/SAF được phát offline bình thường, không cần xác nhận bản quyền trên MusicFlow.

## Trạng thái thực tế

- Supabase project `vzvxpbwrftlnyvuhfoqn`: **đã áp dụng** migration `musicflow_licensed_publish_review_v2`.
- SQL đối chiếu trong `scripts/sql/musicflow_licensed_publish_review_v2.sql` (không chạy lại nếu database đã migration).
- RPC `public.musicflow_publish_licensed_track_v2`: SECURITY INVOKER, **chỉ `service_role` có EXECUTE**. RPC `musicflow_publish_licensed_track` v1 bị thu hồi quyền gọi với `service_role`, `anon`, `authenticated` và `PUBLIC`; không tự đăng nội dung thiếu quyền cũ.
- RPC v2 khóa bản nháp và biên bản upload, kiểm tra SHA-256, đường dẫn public + hash prefix, URL public chuẩn, xác nhận streaming, mã giấy phép, đường dẫn giấy phép hợp lệ, nguồn HTTPS, credit, reviewer, bằng chứng quyền. Sau đó **một giao dịch** cập nhật `music_tracks` (rights/stream/published/verified timestamp) và `music_uploads` (audit/review/published timestamp).
- Python `scripts/musicflow_licensed_upload.py` đã chuyển RPC publish sang v2, yêu cầu các cờ thông tin quyền trước khi tải nhạc lên bucket public; Storage upload bật `x-upsert=false` để không đè tệp cũ.
- Kiểm tra SQL sau migration: quyền RPC v2 `service_role=true`, `anon=false`, `authenticated=false`; RPC v1 không còn quyền chạy cho các role này. Security advisor không có cảnh báo mới trên `music_tracks`; một INFO `music_uploads` RLS không có policy là **có chủ đích** để chặn client.
- Dữ liệu thử nghiệm RPC dùng giao dịch `BEGIN … ROLLBACK` (nguồn HTTP bị từ chối, nguồn/giấy phép HTTPS hợp lệ cập nhật bản ghi + audit đúng). **Không giữ bất kỳ dữ liệu QA nào**; cuối kiểm tra database có **0 music_tracks / 0 music_uploads**.

## Điều kiện xét duyệt

| Thông tin | Quy tắc |
|---|---|
| Quyền streaming | Bắt buộc xác nhận `--confirm-rights`; RPC đặt `streaming_allowed=true` sau khi duyệt |
| Loại giấy phép | `CC0-1.0`, `CC-BY-4.0` hoặc `DIRECT` |
| CC0, CC BY | URL giấy phép phải đúng địa chỉ canonical của Creative Commons |
| DIRECT | Giữ bằng chứng quyền riêng trong `license-reference`; `license-url` công khai có thể bỏ trống |
| Nguồn gốc | `--source-url` HTTPS công khai, không có credentials/fragment |
| Ghi công | `--attribution` không rỗng, nêu tên nghệ sĩ/tác phẩm theo giấy phép |
| Biên bản xét duyệt | `--license-reference` (tài liệu/hợp đồng), `--reviewer`, cờ `--confirm-rights` |
| Download | **Tắt mặc định**; chỉ bật `--allow-download` nếu được cấp quyền rõ ràng |
| Commercial use | **Tắt mặc định**; chỉ bật `--allow-commercial-use` nếu được cấp quyền rõ ràng |
| Dữ liệu audio | SHA-256 + kích thước staging phải trùng biên bản; path public phải bao gồm track ID và 24 ký tự đầu SHA |

**Chú ý:** mã giấy phép và thao tác xác nhận không phải chứng cứ pháp lý tự động. Người quản trị phải xác minh cả **quyền bản ghi âm** và quyền **tác phẩm âm nhạc** khi áp dụng, không suy đoán từ nguồn công khai. Không dùng bucket public cho nhạc riêng tư, DRM hoặc paywall.

## Quy trình vận hành (chỉ trên máy quản trị được tin cậy)

1. Chuẩn bị **bài nhạc có quyền phát online**, xác minh license/nguồn/ghi công. **Không dùng tệp bất kỳ người dùng tự tải về máy để xuất bản lên MusicFlow.**
2. Nhập secret API key `sb_secret_…` qua biến môi trường `MUSICFLOW_SUPABASE_SECRET_KEY` trên máy quản trị; không nhúng vào app, repo hay chat. Publishable key Android không đủ quyền.
3. Tạo bản nháp riêng:

```powershell
python scripts/musicflow_licensed_upload.py stage --file "C:\Music\OriginalTrack.mp3" --title "Original Track" --artist "Composer" --duration-ms 180000
```

4. Nhận Track ID; kiểm tra lại quyền và metadata. Ví dụ dưới đây **chỉ minh họa cú pháp**, không phải bản ghi được kiểm chứng quyền:

```powershell
python scripts/musicflow_licensed_upload.py publish --track-id "TRACK_ID_NHAN_DUOC" --license-code "CC-BY-4.0" --license-url "https://creativecommons.org/licenses/by/4.0/" --source-url "https://example.com/author/original-track" --attribution "Composer - Original Track (CC BY 4.0)" --license-reference "Bang chung da kiem tra so 123" --reviewer "Nguoi kiem duyet" --confirm-rights
```

5. Các quyền download/thương mại mặc định FALSE, chỉ thêm cờ `--allow-download`/`--allow-commercial-use` sau khi xác nhận trong giấy phép. Không tự mở download trong Android chỉ dựa trên việc bản nhạc được phép streaming.
6. Kiểm tra Data API trả về metadata/credit và nhạc xuất hiện trong Online Catalog; nghe thử Media3, thử lỗi mạng, background, seek và gỡ bài theo kế hoạch QA sau.

**Quy tắc xử lý lỗi:** nếu upload public đã thành công nhưng RPC thất bại do lỗi mạng/từ chối, có thể còn **tệp public mồ côi**. Không tự xóa khi không biết RPC đã commit hay chưa; quản trị viên phải đối chiếu receipt/track và đối tượng Storage, sau đó xử lý thủ công. Đây là giới hạn chưa nghiệm thu cho release.

## Bằng chứng QA và giới hạn

- Python offline tests dùng `FakeAdmin` **17/17 PASS**: hash, staging draft, khóa admin, xác nhận quyền, license/canonical URL, nguồn HTTPS, ghi công, quyền download/commercial opt-in và RPC v2.
- SQL giao dịch rollback trên DB thực: nhánh invalid source từ chối; nhánh hợp lệ cập nhật rights và receipt; sau rollback 0 hàng. Security role check đã PASS.
- Không có upload audio thật, không phát nhạc online thật, không lấy secret key quản trị, không kích hoạt Google Login/email thật. Nghe offline cá nhân không bị thay đổi.
- Bước tiếp theo: **Licensed Streaming E2E Smoke** khi có tệp và quyền hợp lệ; sau đó xử lý revocation/gỡ bài, public Storage orphan recovery, signed URL và QA nhiều API. Nếu chưa có nhạc thật, có thể tiếp tục các mốc QA/offline độc lập trong `docs/Ke_Hoach_Du_An.md`.
