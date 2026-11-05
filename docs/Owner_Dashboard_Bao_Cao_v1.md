# MusicFlow — Owner Dashboard v1 cho APK báo cáo

**Ngày:** 2026-11-05. **Mục tiêu:** Sau khi chủ dự án đã tự kích hoạt Owner, bổ sung màn tổng quan và chỉ dẫn dùng tài khoản QA, phục vụ trình bày actor/use case. **Không phải bảng điều khiển thương mại**, không thay đổi quyền bất kỳ tài khoản nào khi xem thống kê.

## Đối chiếu trước khi triển khai

- SQL Supabase chỉ đọc: **1 Owner, 3 tài khoản Auth, 0 PlatformAdmin, 0 Moderator**. Đã xác minh Owner tồn tại nhưng **chưa đăng nhập bằng tài khoản Owner thực**; không yêu cầu cung cấp mật khẩu/refresh token.
- Git baseline `9f7758e`, `main...origin/main`; có thay đổi cục bộ sẵn ở `docs/Trien_Khai_Vai_Tro_Owner_Admin_Developer_v1.md` trước lượt này, được giữ nguyên.

## Chức năng

- RPC chỉ đọc `musicflow_owner_demo_summary()` trả số lượng Owner, user, PlatformAdmin, Moderator, Artist, Author, Developer, yêu cầu Creator chờ, nháp chờ, metadata duyệt và file private đã xác nhận.
- RPC có `SECURITY DEFINER set search_path=''` vì đọc schema private, nhưng kiểm tra `auth.uid()` và role **Owner** trong DB trước khi trả dữ liệu; `anon` không có quyền EXECUTE. Không trả email, UUID, mật khẩu hoặc token.
- Android `Tài khoản → Vai trò và quản trị MusicFlow → Owner • Tổng quan bản demo`: thống kê, trạng thái Owner, hướng dẫn chuẩn bị tài khoản QA; không tự thay đổi quyền.
- Web quản trị nội bộ `admin-web/index.html`: chỉ Owner mới thấy khối tổng quan, lấy số liệu từ server; nút làm mới và cập nhật sau khi cấp quyền.

## Luồng demo hoàn toàn miễn phí

1. Đăng xuất và đăng nhập lại bằng tài khoản Owner bạn vừa kích hoạt.
2. Mở Owner Dashboard để xem còn thiếu những actor nào. Nếu không hiện, kiểm tra đúng tài khoản và dự án Supabase.
3. Đăng ký 3–4 tài khoản **Free QA** do bạn quản lý qua ứng dụng, dùng mật khẩu thử **riêng**; chỉ dùng một máy/emulator rồi đăng xuất để đổi account.
4. Owner mở danh sách Users → chọn tài khoản QA → nhập lý do → cấp `platform_admin` / `moderator` / `artist` / `author`. **Không gán thêm Owner**.
5. Nếu PlatformAdmin cần quyền cụ thể, Owner mới cấp `manage_users`, `review_creators` hoặc `moderate_catalog` cho tài khoản đã có role `platform_admin`.
6. Creator tạo nháp và tải bản thu mẫu/chứng cứ **tự tạo hoặc có quyền sử dụng**. Moderator duyệt metadata, không cấp phép phát nhạc online.
7. Không cần mua Premium, Play Store hay hosting web. Không gửi mật khẩu và key bí mật cho người khác.

## Giới hạn

- SQL chỉ đọc xác minh Owner=1; **chưa thực hiện E2E Owner Auth → cấp role → Creator upload → Moderator duyệt** do chưa đăng nhập bằng tài khoản Owner. Mock UI và kiểm tra deny quyền không thay thế E2E.
- Web console chỉ chạy localhost. Trước khi phát hành thương mại cần hardening các cảnh báo bảo mật Supabase.
- Nhạc offline cá nhân phát bình thường, nhạc online chỉ từ pipeline quyền phát đã được xác minh.

## Tài liệu và QA

- SQL: `docs/sql/musicflow_owner_demo_summary_v1.sql`.
- Android: `core/account/OwnerDemoSnapshot.kt`, `feature/account/OwnerDemoDashboard.kt` và màn Account.
- Web: `admin-web/index.html`.
- QA: `qa-logs/musicflow-owner-dashboard-v1.log`, `qa-logs/demo-apk-owner-dashboard-v1-package.log`.
- **Không tự commit/push**. Giữ nguyên file cục bộ đã có của người dùng.

### Kết quả targeted QA 2026-11-05

- SQL triển khai Supabase **PASS**; RPC `anon` không EXECUTE, user thường bị từ chối qua kiểm tra `auth.uid()`/role, Owner được phép đọc thống kê. Kiểm tra SQL không trả email/UUID/token; **chưa gọi RPC bằng JWT Owner thật**.
- Gradle `:app:testDebugUnitTest --tests OwnerDemoSnapshotTest :app:assembleDebug :app:assembleDebugAndroidTest` **PASS**; **3/3 JVM**, **2/2 Owner Dashboard Android UI** và **5/5 Android Role UI hồi quy** PASS. JavaScript admin-web qua `node --check`.
- APK đã build, ký QA, zipalign, verify và cài/cold launch PASS trên AVD riêng Android API30. `dist/MusicFlow-1.0-Demo-OwnerDashboardV1-Android.apk` có **31.049.405 byte**, SHA-256 `B536AED7285C82DBFF176C63BB0C4D2B8F2CE101222CBA9CE4007B899E5CBFDC`. Guest thấy tab đăng nhập/đăng ký và Google vẫn chưa hoạt động, không thấy công cụ Owner.
- QA AVD `MusicFlow_OwnerDashboard_QA_API30` đã dừng và xóa cả data/ini; giữ nguyên emulator `emulator-5554`. **Không chạy full media regression** hoặc tự cấp bất kỳ role nào.
- Log: `qa-logs/musicflow-owner-dashboard-v1.log` và `qa-logs/demo-apk-owner-dashboard-v1-package.log`. Chưa E2E đăng nhập Owner / cấp quyền / upload Creator thực tế; cần người dùng thao tác bằng tài khoản Owner của họ.
