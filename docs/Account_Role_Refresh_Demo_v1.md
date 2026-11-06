# MusicFlow — Kiểm tra lại quyền tài khoản v1 (APK demo/báo cáo)

**Ngày:** 2026-11-06. **HEAD nền:** `6061fba`. **Phạm vi:** Android và Web Admin. Đây là một mốc QA/UX chỉ đọc để phục vụ demo nhiều actor, **không phải E2E đăng nhập qua JWT bằng các tài khoản thật**.

## Tính năng đã triển khai

- Android: trong `Tài khoản → Vai trò và quản trị MusicFlow` có nút **Kiểm tra lại quyền từ Supabase**. Nút gọi RPC `musicflow_my_access` bằng phiên đăng nhập hiện tại, cập nhật lại role/permission do backend cấp, không dựa vào Premium, email hay `user_metadata`.
- **Fail-closed:** khi người dùng yêu cầu kiểm tra lại, quyền hiển thị cũ được xóa trước khi gửi RPC. Nếu mạng lỗi, session hết hạn hoặc API từ chối, không giữ menu Developer/Admin/Owner từ quyền cũ. Người dùng có thể đăng nhập lại hoặc thử lại khi có mạng; mọi RPC nhạy cảm vẫn được server kiểm tra độc lập.
- Web `admin-web/index.html`: nút **Kiểm tra lại vai trò và quyền** cập nhật danh mục panel theo phạm vi hiện tại. Nếu role hoặc quyền bị thu hồi, các panel liên quan bị ẩn, dữ liệu chọn/người dùng/Creator/danh mục đã tải trước đó bị xóa khỏi DOM; nếu mất hẳn quyền quản trị hoặc xác minh lỗi, phiên web trong bộ nhớ bị xóa và màn hình đăng nhập hiện lại.
- Sửa script `run-owner-demo-readiness-v2-tests.ps1` theo cách duy trì mốc tối thiểu 8 test, không lỗi giả khi nhóm test Roles tăng số lượng.
- Script QA `scripts/run-account-role-refresh-v1-tests.ps1` chỉ sử dụng AVD do người chạy chỉ định qua `-Serial`. Không dùng mật khẩu, không mở khóa service_role, không tự tạo tài khoản hay thay đổi vai trò trên Supabase.
- Web unit-style kiểm thử bằng Node built-in `vm` với DOM và network giả lập, file `scripts/test-admin-web-role-refresh-v1.js`, không kết nối Supabase.

## Kiểm thử và bằng chứng

| Nhóm | Kết quả |
|---|---|
| Supabase SQL read-only, 8 account QA | **PASS 8/8** — roles, quyền ủy nhiệm, chặn quản trị của user thường, chặn Owner summary của non-Owner |
| HTTP public/anon trực tiếp | **3/3 DENIED 401** — `musicflow_my_access`, `musicflow_owner_demo_summary`, `musicflow_admin_users` |
| JVM `MusicFlowAccessTest` | **8/8 PASS** |
| Android `MusicFlowRolesUiTest` | **7/7 PASS**, gồm 2 tình huống mới: thu hồi Developer, Supabase lỗi khi làm mới Admin |
| Web Node tests | **4/4 PASS** — scoped panels, rút quyền, mất role, mạng lỗi |
| Debug build + AndroidTest build | **PASS** |
| Demo APK Release QA | **PASS** — build, zipalign, ký khóa QA và kiểm tra manifest/chữ ký |
| Cài nâng cấp và khởi động lạnh **chính APK bàn giao** trên API30 QA | **PASS** (`Status: ok`, `LaunchState: COLD`, có app process) |

APK: `dist/MusicFlow-1.0-Demo-RoleRefreshV1-Android.apk` — **31.065.789 bytes**, SHA-256 `5CBD72907E1553E860FBE354664804853472B32395AC7C45081788CE86BBBF60`. Ký debug keystore QA dưới dạng release-mode, `minSdk 29`, `targetSdk 36`, **không dùng phát hành Play Store**.

Logs: `qa-logs/account-role-refresh-v1-build.log`, `qa-logs/account-role-refresh-v1-qa.log`, `qa-logs/demo-apk-role-refresh-v1-package.log`. AVD dùng riêng `MusicFlow_RoleRefreshV1_QA_API30` (`emulator-5580`) được xóa sau khi test; không xóa/thay đổi các AVD có sẵn của chủ dự án.

## Cách tái hiện

```powershell
.\scripts\run-account-role-refresh-v1-tests.ps1
# Nếu đã tự bật AVD QA riêng, có thể truyền serial tương ứng:
.\scripts\run-account-role-refresh-v1-tests.ps1 -Serial emulator-5580

node scripts/test-admin-web-role-refresh-v1.js
```

## Giới hạn và việc làm sau

- Đã kiểm tra trực tiếp **DB role matrix** và **HTTP anonymous**, nhưng chưa đăng nhập thật 8 tài khoản sau lúc seed bằng JWT; không được coi là đủ chứng cứ Auth/role E2E. Lệnh tự động xử lý mật khẩu từ file đã bị cơ chế bảo vệ chặn nên không cố vượt qua. Cách thực hiện tiếp: chủ dự án dùng mật khẩu QA trong password manager đăng nhập qua Android/Web, bấm **Kiểm tra lại quyền**, chụp màn hình bằng chứng actor tương ứng.
- Chưa thực hiện việc duyệt/thu hồi role thật hoặc upload âm thanh có quyền trên tài khoản QA. Không giả mạo nhật ký Owner hoặc tự cấp quyền bằng cách sửa dữ liệu người dùng đang dùng. Giữ 1 Owner, 8 tài khoản QA đã có và yêu cầu Creator đang chờ demo.
- RLS/permission vẫn do Supabase RPC quyết định. Các cảnh báo Security Advisor trước đó chưa được xem là giải quyết; vẫn cần rà soát trước sản phẩm thương mại. Không thay đổi email confirmation, Google provider, gói thanh toán hay quy tắc phát nhạc offline.
