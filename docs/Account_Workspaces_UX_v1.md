# MusicFlow — Account Workspaces UX v1 (2026-11-06)

## Phạm vi mốc

**Đích:** tiếp tục nâng cấp Android demo/báo cáo và tạo nền tảng cho việc làm tài khoản dễ sử dụng theo từng actor. **HEAD nền:** `0020579` (`Creator application review UX v2`). Không thay đổi cơ sở dữ liệu, mật khẩu, quyền Owner, tài khoản QA, phát nhạc online, thanh toán hoặc chữ ký production.

## Chức năng đã triển khai

- Màn hình `Vai trò và quản trị MusicFlow` không còn hiển thị tất cả module trong một danh sách cuộn dài. Có **Khu vực làm việc** bằng `FilterChip`, chỉ hiện khu vực đúng role/permission lấy từ `musicflow_my_access`:
  - **Sáng tạo**: hồ sơ cá nhân sáng tạo, nộp đơn Artist/Author, tiến độ xét duyệt, Creator Studio nếu đã được duyệt. Mọi tài khoản đã đăng nhập được xem mục này.
  - **Quản trị**: tổng quan Owner, quản lý tài khoản và ủy nhiệm phân quyền. Chỉ Owner hoặc PlatformAdmin được cấp `manage_users` nhìn thấy phần tương ứng; riêng cấp/thu hồi quyền chỉ Owner.
  - **Kiểm duyệt**: xét duyệt Creator (nếu `review_creators`), duyệt metadata tác phẩm (nếu Moderator/Owner/Admin được cấp), kiểm duyệt danh mục online (nếu `moderate_catalog`). Có ô nhập lý do dùng cả khi Owner duyệt Creator.
  - **Công cụ QA**: chỉ Developer trong **BuildConfig.DEBUG**, không có đường tắt quản trị trong bản Release QA.
- Tự mở **khu vực ưu tiên**: Owner/PlatformAdmin có `manage_users` → Quản trị; Moderator/Admin có quyền kiểm duyệt → Kiểm duyệt; Developer Debug → Công cụ QA; Artist/Author/Free/Premium → Sáng tạo. Nếu người dùng có nhiều vai trò, họ chủ động chuyển mục.
- Khi bấm **Kiểm tra lại quyền từ Supabase**, giao diện xóa danh sách/số liệu đặc quyền cũ, đóng các xác nhận đang chờ và bỏ lựa chọn khu vực cũ. Nếu role bị thu hồi hoặc RPC lỗi, các công cụ quản trị biến mất; các quyền thực thi vẫn được backend xác thực trong từng RPC.
- Tại trang tài khoản, tên vai trò hiển thị bằng tiếng Việt (Chủ nền tảng, Quản trị viên, Kiểm duyệt viên, Nghệ sĩ, Tác giả, Nhà phát triển). Nút tác vụ chính đổi nhãn theo vai trò và điểm vào ưu tiên; gói Premium **không tự cấp** quyền quản trị.
- Các đơn Creator, review, Owner Dashboard, quản lý users và Developer QA **tiếp tục dùng RPC/code trước đó**, không viết lại nghiệp vụ.

## Bằng chứng QA

| Hạng mục | Kết quả |
|---|---|
| `AccountWorkspacePlanTest` (7 ca) + `MusicFlowAccessTest` (8 ca) | **15/15 JVM PASS** |
| `AccountWorkspacesUiTest` (3) + `MusicFlowRolesUiTest` (7) + `CreatorApplicationReviewUiTest` (3) | **13/13 Android UI PASS** trên AVD QA API30 riêng |
| `assembleDebug`, `assembleDebugAndroidTest` | **PASS** |
| `assembleRelease`, zipalign, chữ ký QA, apksigner/aapt | **PASS** |
| Cài nâng cấp + khởi động lạnh **chính APK đã đóng gói** trên emulator API30 | **PASS** (`Status: ok`, `LaunchState: COLD`, app process tồn tại) |

Bộ test UI dùng RPC in-memory, **không đăng nhập live/JWT với 8 tài khoản QA**. Lần đầu chạy bộ 13 UI tests có **1 lỗi assertion từ test cũ** (`creator-load-requests` không thuộc khu vực Kiểm duyệt nữa). Đã cập nhật bài test chuyển sang Sáng tạo rồi quay lại Kiểm duyệt, chạy lại **13/13 PASS**. Lỗi này không phải Supabase đổi quyền hay phát nhạc lỗi.

**Các file:**

- `app/src/main/java/com/example/musicflow/core/account/AccountWorkspacePlan.kt`
- `app/src/main/java/com/example/musicflow/feature/account/AccountRolePanel.kt`
- `app/src/main/java/com/example/musicflow/feature/account/AccountScreen.kt`
- `app/src/test/java/com/example/musicflow/core/account/AccountWorkspacePlanTest.kt`
- `app/src/androidTest/java/com/example/musicflow/ui/AccountWorkspacesUiTest.kt`
- `app/src/androidTest/java/com/example/musicflow/ui/CreatorApplicationReviewUiTest.kt`
- `scripts/run-account-workspaces-v1-tests.ps1`
- `qa-logs/musicflow-account-workspaces-v1-build.log`
- `qa-logs/musicflow-account-workspaces-v1-qa.log`
- `qa-logs/demo-apk-account-workspaces-v1-package.log`

## APK demo mới

`dist/MusicFlow-1.0-Demo-AccountWorkspacesV1-Android.apk` — **31.065.789 bytes**; SHA-256: `89DF0D36E8878D77168877E81100E3E1C9DECB829F11F134A8E0D1BC4A57F1F6`. Version code `1`, version name `1.0`, minSdk `29`, targetSdk `36`, không debuggable; ký debug-keystore **QA demo only**, không phải phát hành Google Play.

## Lặp lại kiểm thử

```powershell
.\scripts\run-account-workspaces-v1-tests.ps1
# Chỉ chạy Android UI khi đã có emulator QA RIÊNG được bật trước:
.\scripts\run-account-workspaces-v1-tests.ps1 -Serial emulator-5580
```

Script không khởi tạo emulator, không đọc mật khẩu, không thao tác role/database. QA đã dùng AVD mới `MusicFlow_AccountWorkspacesV1_QA_API30` trên `emulator-5580`; dọn đúng AVD này sau nghiệm thu, giữ các AVD đã có.

## Roadmap tiếp theo: thân thiện với người dùng

1. **Account Home v2 — ưu tiên cao:** thiết kế trang tài khoản theo từng persona (Guest, Free, Premium, Creator, Moderator, PlatformAdmin, Owner, Developer). Ưu tiên mục thường dùng, giải thích quyền lợi Free/Premium bằng dữ liệu entitlement thật, gom các thao tác ít gặp vào phần mở rộng. Giao diện đăng nhập/đăng ký đơn giản, có trạng thái lỗi/đang tải rõ ràng; Guest vẫn nghe offline không bị ép đăng ký.
2. **Role-specific task flow v2:** Owner có bảng tổng quan → quản lý cộng tác viên → nhật ký; Admin theo đúng phạm vi được ủy quyền; Moderator thấy hàng chờ duyệt metadata; Artist/Author theo bước tạo tác phẩm → thêm bằng chứng quyền → gửi duyệt → xem trạng thái. Không nhầm vai trò quản trị với gói Premium.
3. **Refresh/session consistency:** cập nhật badge/tên vai trò trên màn hình Tài khoản khi đã kiểm tra lại quyền, thay vì chỉ cập nhật trong popup. Có nút làm mới entitlement và chỉ dẫn khi phiên hết hạn, không hiển thị role cũ như một quyền còn hiệu lực.
4. **Accessibility/UX polishing:** phân cấp heading, nhãn Tiếng Việt dễ hiểu, touch target, trạng thái trống/lỗi/success, màn hình phóng chữ, giảm cuộn, hỗ trợ điều hướng quay lại. Kiểm thử từng actor bằng demo credentials do chủ dự án tự nhập (không lưu repo), không tự thay đổi/duyệt mất đơn QA.
5. **Nghiệm thu & báo cáo:** checklist theo actor/use case, bằng chứng Android/Web Admin, APK duy nhất được chọn để trình bày, giới hạn Auth/Cloud/streaming cần công bố minh bạch.

**Giới hạn:** Không thực hiện E2E JWT 8 account trong milestone này; chưa chạy full playback/media regression hoặc thử thiết bị vật lý. Các mục roadmap chưa được triển khai không được đánh dấu PASS.
