# MusicFlow — Licensed Online Rights Metadata & Attribution v1

**Ngày:** 2026-10-29. **Git HEAD nền:** `4f65879`. **Trạng thái:** source + Supabase migration + QA PASS; mốc này chưa commit/push GitHub.

## Mục tiêu

Danh mục nhạc online phải có **nguồn, ghi công, loại giấy phép, quyền streaming/download/commercial và xác nhận người quản trị**, thay vì suy đoán bản nhạc dùng được chỉ vì có URL HTTPS. **Metadata tự khai không thay thế việc thẩm định bản quyền của chủ sở hữu.** Khách/Free/Premium không được tự nâng quyền tác phẩm.

## Schema Supabase — migration đã ÁP DỤNG trên dự án thực

- Project: `vzvxpbwrftlnyvuhfoqn`; migration: `music_tracks_rights_attribution_v1`, áp dụng 2026-10-29 thành công.
- Bảng `public.music_tracks` thêm: `license_code` (UNVERIFIED / CC0-1.0 / CC-BY-4.0 / DIRECT), `license_url`, `attribution_text`, `source_url`, `streaming_allowed`, `download_allowed`, `commercial_use_allowed`, `rights_verified_at`.
- **Mặc định tất cả hàng cũ đều không có quyền stream**: `license_code='UNVERIFIED'`, các cờ quyền FALSE, `rights_verified_at=NULL`.
- RLS SELECT công khai cần đồng thời: `is_published=true`, `rights_confirmed=true`, `rights_verified_at IS NOT NULL`, `streaming_allowed=true`, URL phát và nguồn HTTPS, ghi công không rỗng, loại giấy phép đã hỗ trợ, license URL HTTPS cho CC0/CC BY. Vai trò `anon`/`authenticated` chỉ được SELECT, không được tự ghi quyền.
- Đã kiểm tra SQL sau migration: có đủ tám cột, RLS bật, chính sách `published_licensed_tracks_visible` có điều kiện quyền mới, bảng vẫn **0 bài hát**.
- Security advisor: không có phát hiện mới liên quan `music_tracks`; một INFO `public.music_uploads` RLS không có policy là **thiết kế hiện tại** vì upload chỉ qua công cụ quản trị và client roles bị thu hồi quyền (xem `docs/Supabase_Licensed_Upload_v1.md`).

### SQL tham chiếu (migration đã có lịch sử trên Supabase)

```sql
alter table public.music_tracks
  add column if not exists license_code text not null default 'UNVERIFIED',
  add column if not exists license_url text,
  add column if not exists attribution_text text,
  add column if not exists source_url text,
  add column if not exists streaming_allowed boolean not null default false,
  add column if not exists download_allowed boolean not null default false,
  add column if not exists commercial_use_allowed boolean not null default false,
  add column if not exists rights_verified_at timestamptz;
alter table public.music_tracks
  add constraint music_tracks_license_code_allowed
  check (license_code in ('UNVERIFIED','CC0-1.0','CC-BY-4.0','DIRECT'));
drop policy if exists published_licensed_tracks_visible on public.music_tracks;
create policy published_licensed_tracks_visible
on public.music_tracks for select to anon, authenticated
using (
  is_published = true and rights_confirmed = true
  and rights_verified_at is not null and streaming_allowed = true
  and stream_url like 'https://%' and source_url like 'https://%'
  and license_code in ('CC0-1.0','CC-BY-4.0','DIRECT')
  and length(btrim(coalesce(attribution_text,''))) > 0
  and (license_code = 'DIRECT' or license_url like 'https://%')
  and (license_url is null or license_url like 'https://%')
);
alter table public.music_tracks enable row level security;
```

**Không chạy lại thủ công nếu migration đã tồn tại**, vì câu lệnh tạo constraint không được thiết kế để replay. Nguồn chính thức là migration đã áp dụng trên dự án Supabase. Không thêm service-role key vào source.

## Android — client fail closed

| Đường đi | Cách bảo vệ |
|---|---|
| Supabase REST catalog | Select đủ các trường quyền/credit; parse bằng `OnlineRightsJson.fromSupabase` |
| HTTPS catalog riêng | Bắt buộc `rights` dạng object với camelCase; thiếu object => không phát |
| Player online | `TrackMediaItems.fromOnlineOrNull` cần `OnlineRightsPolicy.canStream` và URL HTTPS hợp lệ |
| Online Catalog UI | Hiển thị lý do từ chối, nút **Nguồn và giấy phép**, thông tin nghệ sĩ/ghi công, liên kết nguồn và giấy phép qua HTTPS; chưa có nút download |
| Playlist/recent/queue đã lưu | Online URL cũ không được tự play/restore; file local `content://`, `file://` hoặc đường dẫn tuyệt đối trên thiết bị vẫn hoạt động, **kể cả ID bắt đầu `online:`**. Nếu URL online hiện tại bị loại, khôi phục bài local gần nhất với position=0 |

**Quy tắc chủ dự án xác nhận:** MusicFlow **không kiểm tra bản quyền khi người dùng nghe file offline tự tải hoặc tự lưu vào máy**. Kiểm tra giấy phép chỉ áp dụng cho streaming trực tiếp từ danh mục/nguồn online mà MusicFlow phân phối. Không phân loại nhạc online/offline bằng tên file hay tiền tố `mediaId`: bản nhạc từng có ID `online:<id>` đã được lưu cục bộ vẫn được phát như file bình thường. Quyền đọc MediaStore/SAF của Android vẫn được áp dụng.

`OnlineTrackRights` phân biệt rõ `streamingAllowed`, `downloadAllowed`, `commercialUseAllowed`. Mốc này **không** tự triển khai tải nhạc online, quyền dùng thương mại hay signed URL renewal. Các trường này do backend đã được phân quyền cập nhật, không tin các dấu hiệu tự suy ra từ URL.

CC BY 4.0 yêu cầu ghi công và tham chiếu giấy phép; CC0 là public-domain dedication, nhưng MusicFlow vẫn yêu cầu nguồn/credit trong catalog để truy xuất. CC BY-NC chưa được chấp nhận trong chính sách streaming ứng dụng ở giai đoạn hiện tại; không kết luận rằng nội dung NC luôn bất hợp pháp cho mọi mục đích.

## QA

- Script `scripts/run-licensed-online-rights-v1-tests.ps1` → `qa-logs/licensed-online-rights-v1.log`, `FINAL_RESULT=PASS`.
- Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL**; `OnlineRightsPolicyTest` **10/10 JVM**; các JVM khác trong task cũng PASS.
- Emulator Android API 30: `OnlineRightsJsonTest` **4/4**, `OnlineRightsScreenTest` **2/2**, `TrackMediaItemsTest` **9/9**, `PlaybackQueuePersistenceTest` **5/5**, `QueueManagerIntegrationTest` **1/1**; tổng **21/21 PASS** sau sửa phân định online/offline.
- Vòng chạy đầu 17/18 do assertion UI test nhắm text rút gọn thay vì toàn bộ nhãn `Giấy phép: Chưa khai báo`; sửa test và chạy lại **19/19 PASS**. Không có bài nhạc thật nào được phát hoặc upload trong vòng này.
- Sau yêu cầu của chủ dự án: bổ sung 2 bài Android test xác nhận **tệp offline đã tải được khôi phục ngay cả với ID cũ `online:`**, trong khi URL online chưa xác minh vẫn không tự phát. Chạy script kiểm thử lại, **21/21 PASS**, `FINAL_RESULT=PASS`.

## Giới hạn và bước tiếp theo

**Quan trọng:** script quản trị `scripts/musicflow_licensed_upload.py` và RPC publish hiện chưa yêu cầu/ghi đủ metadata quyền mới, nên bản nhạc publish theo quy trình cũ **không xuất hiện công khai** sau thay đổi RLS, theo mặc định fail-closed. Trước khi thử bài nhạc thật, cần mốc **Licensed Content Review & Publish v2**: cập nhật công cụ quản trị để nhập/xác minh nguồn, giấy phép, credit, quyền streaming/download/commercial và đóng dấu `rights_verified_at` chỉ sau khi người quản trị đã thực sự kiểm tra; bảo toàn audit và quyền service_role.

Sau đó mới **Licensed Streaming E2E Smoke** bằng bài do chủ dự án sở hữu/được cấp phép. Còn cần signed URL renewal, gỡ bài/revocation, lưu lại attribution khi phát từ history/queue, kiểm thử đa API. Google Login/email thật và cloud hai thiết bị E2E vẫn để gần cuối như đã chốt.
