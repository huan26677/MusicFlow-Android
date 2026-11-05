# MusicFlow — Owner / PlatformAdmin / Developer / Creator Foundation v1

**Ngày triển khai:** 2026-11-05 · **Mục tiêu:** APK sideload/demo và trang quản trị web nội bộ, **không phát hành Play**.  
**Trạng thái nghiệm thu 2026-11-05:** **PASS phạm vi MVP/QA**. Hoàn thành code và triển khai Supabase trước khi chạy test debug. **Chưa hoàn thành toàn bộ Admin/Creator production**, chưa có Owner được xác minh nên E2E dùng quyền Owner thật còn chờ. Bằng chứng tại `qa-logs/musicflow-roles-mvp-v1.log`.

## Các điểm kế hoạch đã rà soát và điều chỉnh

- **Owner** là chủ sở hữu nghiệp vụ nền tảng, có quyền quản lý người dùng và ủy quyền. Không phải mật khẩu hoặc service key được nhúng trong APK. Tài khoản Owner **không tự tạo bằng sign-up**.
- **PlatformAdmin** được Owner cấp vai trò và **phạm vi quyền độc lập**: `manage_users`, `review_creators`, `moderate_catalog`. Không thể tự nâng quyền, thay thế Owner, cấp thêm Owner.
- **Developer** là vai trò kỹ thuật có lối tắt QA **chỉ khi BuildConfig.DEBUG=true** và Supabase trả về role hợp lệ. Không có quyền quản trị mặc định hoặc đăng nhập bằng cửa hậu.
- **Artist / Author** dùng tài khoản Supabase thông thường gửi yêu cầu xét duyệt; duyệt xong mới cho sửa hồ sơ sáng tạo. Đây là vai trò độc lập với Guest/Free/Premium.
- **Moderator** là vai trò dự kiến kiểm duyệt, **chưa được tự động cấp bất kỳ API thao tác riêng**; cần chính sách duyệt tương ứng nếu triển khai thêm. Không đồng nghĩa PlatformAdmin.
- Chỉ nhạc **online** có bằng chứng stream/giấy phép theo pipeline hiện có mới được phép công bố. Nhạc offline tự lưu vẫn nghe như cũ.

## Backend Supabase

Tệp nguồn: `docs/sql/musicflow_roles_v1.sql`.

Các bảng đặt trong schema **`musicflow_private` không expose REST**, có RLS bật và thu hồi quyền `anon`/`authenticated` trực tiếp:

- `user_roles` — vai trò độc lập với `account_entitlements`, ràng buộc **tối đa một Owner**.
- `admin_permissions` — phạm vi riêng cho quản trị viên cộng tác.
- `creator_applications` — Nghệ sĩ/Tác giả xin duyệt, trạng thái pending/approved/rejected; không cho tạo 2 yêu cầu pending cùng vai trò.
- `creator_profiles` — nghệ danh/bút danh và giới thiệu, chỉ sửa sau khi đã được duyệt.
- `role_audit` — nhật ký cấp/thu hồi quyền, quyết định xét duyệt và xuất bản/gỡ nhạc online.

Các RPC public được mở `EXECUTE` **chỉ cho authenticated**; mọi hành động đều kiểm tra `auth.uid()` và vai trò **trực tiếp trong cơ sở dữ liệu**, không tin `raw_user_meta_data`, email, tier Premium hoặc trạng thái UI:

| RPC | Quyền |
|---|---|
| `musicflow_my_access` | Tự xem role và phạm vi quyền của mình |
| `musicflow_admin_users` | Owner, hoặc PlatformAdmin có `manage_users` |
| `musicflow_manage_role` | **Chỉ Owner**; không thể gán/thay Owner |
| `musicflow_set_admin_permission` | **Chỉ Owner**, chỉ cấp quyền cho PlatformAdmin |
| `musicflow_apply_creator`, `musicflow_my_creator_requests` | Người dùng đăng nhập, **chỉ dữ liệu của mình** |
| `musicflow_pending_creators`, `musicflow_review_creator` | Owner hoặc PlatformAdmin có `review_creators` |
| `musicflow_my_creator_profile`, `musicflow_save_creator_profile` | Chỉ hồ sơ của chính người dùng; sửa cần role Creator đã duyệt |
| `musicflow_admin_tracks`, `musicflow_review_track` | Owner hoặc PlatformAdmin có `moderate_catalog`; tái xuất bản chỉ khi có receipt được duyệt từ pipeline giấy phép |
| `musicflow_owner_audit` | Chỉ Owner |

Không đổi bảng Auth, account_profiles, account_entitlements hay token store đã có. Chưa tạo/cấp role Owner cho tài khoản cá nhân nào khi chưa xác minh danh tính chủ dự án.

## Giao diện đã bổ sung

- **Android:** màn Tài khoản có mục **Vai trò & công cụ MusicFlow**; người dùng đăng nhập có thể yêu cầu Artist/Author và xem trạng thái; người được duyệt có thể sửa hồ sơ. Owner thấy tìm người dùng/cấp role/phạm vi/xem audit; PlatformAdmin chỉ thấy chức năng được server cấp. Developer chỉ thấy **Công cụ nhà phát triển (QA)** trên bản debug và sau khi role được xác thực.
- **Web nội bộ:** `admin-web/index.html` — giao diện đăng nhập bằng Supabase email/mật khẩu, tra cứu người dùng, quyền ủy nhiệm của Owner, duyệt Creator, danh mục giấy phép online và audit. Chỉ lưu access token trong bộ nhớ trang; không lưu mật khẩu/service key hay localStorage. Các thao tác thay đổi quyền có xác nhận và lý do; backend vẫn là nơi quyết định cho phép.
- **Không cần Google OAuth**, nút Google của Android vẫn bị khóa theo yêu cầu trước.

### Khởi động web nội bộ

Trong thư mục gốc dự án (trên máy đang phát triển), ví dụ:

```powershell
python -m http.server 8765 --directory admin-web
```

Mở `http://127.0.0.1:8765`, nhập Supabase URL, **publishable key** và tài khoản đã được cấp Owner/PlatformAdmin. Không triển khai web console lên hosting công khai, chưa có HTTPS/deployment riêng; chỉ dùng localhost cho báo cáo.

## Khởi tạo Owner ban đầu — yêu cầu thực hiện có xác minh

**Chưa tự gán Owner cho bất kỳ tài khoản hiện có nào.** Khi chủ sở hữu đăng ký/đăng nhập tài khoản Supabase của chính mình, lấy đúng **UUID của tài khoản đó** trong Supabase Dashboard → Authentication → Users. **Không chỉ căn cứ email**, vì project đang tắt xác nhận email và người khác có thể đăng ký tên email chưa sở hữu.

Chỉ khi đối chiếu được danh tính/UUID với chủ dự án và Owner đồng ý, chủ dự án chạy trong **Supabase SQL Editor (quyền Dashboard)**, thay cả 2 dấu placeholder bằng giá trị thực:

```sql
begin;
do $$
declare
    target_id uuid := 'a20fb14d-31ef-4ef3-bc72-ecbb8c4bff78'::uuid;
    target_email text := 'huan26677@gmail.com';
begin
    if exists (select 1 from musicflow_private.user_roles where role = 'owner') then
        raise exception 'Owner exists: use a separately approved transfer procedure';
    end if;
    if not exists (
        select 1 from auth.users
        where id = target_id and lower(email) = lower(target_email)
    ) then
        raise exception 'Owner UUID/email do not match';
    end if;
    insert into musicflow_private.user_roles(user_id,role,granted_by)
    values (target_id,'owner',null);
end $$;
commit;
```

Đây là hành động **một lần**, không đưa vào migration tự chạy. Không dùng role Owner cho tài khoản QA ngẫu nhiên. Owner đăng xuất rồi đăng nhập lại app hoặc web để cập nhật role.

## Giới hạn đã nhận diện

- Gói này là **MVP phân quyền và quản trị nội bộ**, không phải hoàn thành toàn bộ sản phẩm thương mại. Phần Creator Studio tải bản thu và chứng cứ lên kho riêng, quản lý bản quyền/hợp đồng, khóa tài khoản ở Supabase Auth, Owner transfer có xác minh nhiều bước, chống lạm dụng/rate limit và xử lý khiếu nại đầy đủ tiếp tục thuộc backlog A3/A4.
- Web console chưa được deploy/kiểm thử trên domain public; không có cơ chế gọi admin service role từ client.
- Kiểm thử sau khi code/backend hoàn thành: tập trung build debug, JVM role rules, UI role/Developer, Supabase RPC/RLS; **không cần chạy full media regression**, không thử sửa dữ liệu của người dùng.

## Checklist nghiệm thu

- [x] Supabase đã áp dụng SQL; **5/5 bảng** schema private bật RLS, `anon/authenticated` không SELECT trực tiếp; **13/13 RPC** chặn `anon`, `authenticated` chỉ được EXECUTE để backend kiểm tra vai trò.
- [x] Negative tests: user đang tồn tại nhưng không role được giả lập authenticated trong SQL, gọi `manage_role`/`admin_users`/`pending_creators` đều bị từ chối; anon HTTPS bị **401**. Không có dữ liệu thật nào thay đổi.
- [x] Owner/bootstrap **để trống**: 0 Owner, 0 role, 0 audit event sau kiểm thử.
- [x] Gradle debug + AndroidTest build PASS; **6/6 JVM role**, **4/4 Android role UI**, **1/1 Android Account UI cũ PASS**. Không full regression.
- [x] Android và web không nhúng secret/service key hoặc hardcode mật khẩu; HTML/JS console `node --check` PASS. Chưa web Owner E2E.
- [x] APK demo riêng build, zipalign, ký QA, apksigner, aapt, SHA256, cài nâng cấp và cold launch PASS.
- [x] Kế hoạch và log QA được cập nhật.

### APK bàn giao và giới hạn

- APK demo: `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-RolesMVP-Android.apk`.
- **30.885.565 byte**; SHA-256 `7515DB0136E92A2D5750F935532515F2D10F8808EF89F85188C58DCE9BB6A15F`.
- APK release-mode **ký debug keystore QA**, không debuggable; không phải bản phát hành thật. APK các mốc trước được giữ nguyên. `dist` Git-ignored.
- APK debug có shortcut Developer khi Supabase đã cấp role: `app/build/outputs/apk/debug/app-debug.apk`. Bản demo release-mode **không hiển thị shortcut Developer** vì `BuildConfig.DEBUG=false`.
- Script: `scripts/build-musicflow-demo-apk-v1.ps1 -OutputName MusicFlow-1.0-Demo-RolesMVP-Android.apk`.
- Security Advisor cảnh báo 13 RPC `SECURITY DEFINER` callable bởi `authenticated`: chủ ý dùng authenticated RPC có kiểm tra quyền trực tiếp, nhưng cần audit bổ sung trước production. Tham khảo https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable.
- RLS trên schema private không có policy là **default deny có chủ ý**. Supabase còn cảnh báo `Leaked Password Protection Disabled` từ trước, chưa chỉnh trong lượt này: https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection.
- Chưa gán Owner, chưa test cấp quyền thật Owner/PlatformAdmin end-to-end, chưa host web công khai hoặc làm Creator Studio upload audio. Bản MVP **không đồng nghĩa hoàn tất full product**.

**Hướng triển khai tiếp:** sau bước Owner bootstrap, dùng console cấp PlatformAdmin/Developer có phạm vi, rồi test E2E đầy đủ với những tài khoản do chủ dự án cấp quyền. Tuyệt đối không mặc định tạo đặc quyền cho tài khoản Free/Premium đang tồn tại.
