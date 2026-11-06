# MusicFlow — Account Task UX v3 (APK demo/báo cáo)

**Ngày:** 2026-11-06. **HEAD nền:** `da77585` (`Account Home v2`). **Trạng thái:** QA mục tiêu PASS, chưa commit/push trong lượt triển khai. Chỉ tối ưu giao diện Android; không đổi Supabase schema, mật khẩu, vai trò Owner/PlatformAdmin, streaming license, billing hoặc dữ liệu QA.

## Cải thiện hành trình từng actor

- **Owner:** mở khu vực Quản trị thấy ô tìm tài khoản ngay, thay vì phải cuộn qua báo cáo Owner dài. Có nút **Xem tổng quan và tiến độ demo / Thu gọn tổng quan Owner**, chỉ truy vấn tổng quan khi người dùng mở, vẫn giữ nguyên Owner RPC xác thực và nút làm mới.
- **PlatformAdmin:** thấy quản lý người dùng hoặc kiểm duyệt đúng với `manage_users` / `review_creators` / `moderate_catalog`; không thể tự cấp vai trò Owner. Khu vực có hướng dẫn nhiệm vụ tiếp theo bằng tiếng Việt.
- **Moderator:** gợi ý xử lý metadata, giải thích **duyệt hồ sơ không đồng nghĩa phát hành online**. Danh sách Creator pending, danh mục online và danh sách user chỉ hiển thị trạng thái trống sau khi máy chủ trả kết quả thành công, không lẫn với chưa tải hoặc mất mạng.
- **Artist/Author:** nút **Mở kho tác phẩm và bản nháp** giúp thu gọn Creator Studio khi chỉ muốn kiểm tra hồ sơ sáng tạo. Studio chưa tải dữ liệu sẽ hướng dẫn nhấn **Tải nháp của tôi** hoặc **Tạo nháp**. Đã tải mà trống thì chỉ rõ cách tạo tác phẩm đầu tiên. Khi tải nháp lỗi, giao diện không hiển thị dữ liệu bản nháp cũ như thể mới xác minh.
- **Guest, Free, Premium, Developer:** tiếp tục dùng Account Home v2 và hệ thống `AccountWorkspacePlan`; không tự nâng quyền khi Premium hoặc sử dụng Developer shortcut.
- **Trợ năng:** tiêu đề chính và mục công việc có heading semantics; các chip chọn khu vực và nút điều hướng chính có chiều cao tối thiểu 48dp; test chuyển khu vực trên emulator ở font scale 165%. Đây là bước kiểm thử sơ bộ, chưa thay thế cuộc đánh giá TalkBack thủ công.
- **Fail-closed UI:** làm mới role xóa cached users/audit/creator queue/catalog, thu gọn studio/tổng quan và cờ đã tải. Các lệnh tải danh sách xóa dữ liệu cũ trước khi truy vấn; khi lỗi không giữ danh sách trước đó trên màn hình, hiện lỗi để người dùng thử lại. Supabase vẫn là nơi xác định quyền thật cho từng RPC.

## Mã nguồn

- `app/src/main/java/com/example/musicflow/core/account/AccountTaskGuide.kt` — hướng dẫn bước tiếp theo theo workspace, tách khỏi nghiệp vụ phân quyền.
- `app/src/main/java/com/example/musicflow/feature/account/AccountRolePanel.kt` — nút mở rộng Owner/Creator, trạng thái đã tải/trống/lỗi, heading và touch targets.
- `app/src/main/java/com/example/musicflow/feature/account/CreatorDraftStudio.kt` — hướng dẫn và trạng thái khi danh sách tác phẩm chưa tải, trống hoặc lỗi mạng.
- `app/src/test/java/com/example/musicflow/core/account/AccountTaskGuideTest.kt` — 6 JVM tests mới.
- `app/src/androidTest/java/com/example/musicflow/ui/AccountTaskUxV3UiTest.kt` — 5 Android UI tests mới.
- `scripts/run-account-ux-v3-tests.ps1` — test runbook, dùng fake RPC, chỉ chạy UI khi truyền `-Serial` của AVD QA riêng.

## Kết quả QA

| Nhóm | Kết quả |
|---|---|
| `AccountTaskGuideTest` (6), `AccountHomePlanTest` (6), `AccountWorkspacePlanTest` (7), `MusicFlowAccessTest` (8), `AccountTierTest` (5) | **32/32 JVM PASS** |
| `AccountTaskUxV3UiTest` (5) + `AccountHomeV2UiTest` (8) + `AccountWorkspacesUiTest` (3) + `MusicFlowRolesUiTest` (7) + `CreatorApplicationReviewUiTest` (3) + `CreatorDraftStudioUiTest` (2) | **28/28 Android UI PASS** trên emulator Android API 30 QA |
| Gradle `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:assembleRelease` | **PASS** |
| `zipalign`, QA `apksigner`, `aapt`/manifest, SHA-256 | **PASS** |
| Cài nâng cấp và khởi động lạnh **chính APK bàn giao** trên AVD API30 QA | **PASS** (`Status: ok`, `LaunchState: COLD`, app process tồn tại) |

QA UI mới xác nhận: Owner không gọi summary trước khi bấm mở, Artist/Author vào Studio theo nhu cầu và thấy empty state, người kiểm duyệt chỉ thấy empty queue sau khi tải thành công, lỗi mạng khi tải lại admin list không giữ bản sao trước đó, thao tác chuyển workspace ở font scale 1.65. **Không có thao tác ghi nào vào Supabase trong các bài kiểm thử này.**

**Log:** `qa-logs/musicflow-account-ux-v3-build.log`, `qa-logs/musicflow-account-ux-v3-qa.log`, `qa-logs/demo-apk-account-ux-v3-package.log`.

## APK QA demo

`dist/MusicFlow-1.0-Demo-AccountUXV3-Android.apk` — **31.098.557 bytes**, SHA256: `E7F9C1DBBA1EEFAF9A1AE6B280668E474BC9040FA880D42903CFFAEC2D9A037B`.

APK Release-mode ký bằng debug keystore QA nội bộ (không phải production), `minSdk=29`, `targetSdk=36`, `versionName=1.0`, `versionCode=1`, `debuggable=false`. Đã cài và cold launch đúng tệp bàn giao trên emulator QA `MusicFlow_AccountUXV3_QA_API30` (`emulator-5580`). Không đụng AVD người dùng đã có.

## Tái hiện và giới hạn

```powershell
.\scripts\run-account-ux-v3-tests.ps1
# Nếu chủ dự án tự bật emulator QA riêng:
.\scripts\run-account-ux-v3-tests.ps1 -Serial emulator-5580
```

- **Chưa kiểm thử** đăng nhập thật bằng JWT 8 tài khoản demo, duyệt Creator trực tiếp trên Supabase, E2E streaming/licensing, full media regression, thiết bị thật hoặc TalkBack thực tế. Không được ghi các mục đó là PASS.
- **Bước ưu tiên tiếp:** kiểm tra accessibility ở font scale lớn trên tất cả tab, hành vi Android Back, trạng thái tải đồng bộ/cloud, tinh gọn biểu mẫu quyền/tác phẩm thành từng bước; hoàn thiện bảng actor/use-case/bằng chứng cho báo cáo. Nếu nghiệm thu 8 tài khoản bằng JWT live, chủ dự án tự nhập mật khẩu từ kho QA vào Android/Web, không chép vào file dự án.
- Phạm vi vẫn chỉ là **APK phục vụ báo cáo**, không triển khai thương mại/Google Play.
