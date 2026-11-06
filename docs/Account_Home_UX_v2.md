# MusicFlow — Account Home UX v2 (APK demo / báo cáo)

**Ngày:** 2026-11-06. **HEAD nền:** `795cc09`. **Mục tiêu:** làm trang Tài khoản dễ hiểu với từng actor, giảm cuộn và không nhầm quyền quản trị với gói Premium. Không triển khai billing, Google login, streaming thiếu bản quyền hay phát hành Google Play.

## Tính năng đã triển khai

- **Guest:** nút **Nghe nhạc ngay, không cần đăng nhập** được đặt trước form Đăng nhập / Tạo tài khoản. Nghe nhạc offline và thư viện địa phương không phụ thuộc Supabase; đăng nhập vẫn là lựa chọn khi muốn lưu/đồng bộ dữ liệu.
- **Người đã đăng nhập:** lời chào sử dụng tên hiển thị (hoặc phần đầu địa chỉ email), dòng **Gói tài khoản: Free / Premium / Demo trên thiết bị**, mô tả chính xác nguồn dữ liệu, nhãn vai trò tiếng Việt và một nút tác vụ ưu tiên. Thay phần hướng dẫn đăng nhập bằng phần hướng dẫn quản lý tài khoản.
- **Phân tách gói và vai trò:** gói Premium chỉ được đọc từ entitlement Supabase theo `AccountApi.currentAccount()`; Premium không mở công cụ Owner/Admin/Creator. Vai trò quản trị chỉ hiện khi backend cấp role/permission, diễn giải nhãn từ `AccountWorkspacePlan`.
- **Điểm vào theo actor:** Free/Premium vào đăng ký Nghệ sĩ/Tác giả; Artist/Author vào Creator Studio; Owner vào Quản trị; PlatformAdmin chỉ thấy nhiệm vụ được ủy quyền; Moderator vào Kiểm duyệt; Developer vào Công cụ QA nếu **BuildConfig.DEBUG**.
- **Hiển thị có chủ đích:** chỉnh sửa tên hiển thị và đồng bộ Playlist/Favorites/Lịch sử chỉ mở khi cần qua hai nút **Chỉnh sửa hồ sơ cá nhân** / **Đồng bộ và bản sao đám mây**. Các thao tác cũ vẫn có đầy đủ, nhưng không chiếm chỗ khi xem trang Tài khoản. Đăng xuất và thông báo lỗi ở khu vực dễ tìm.
- **Làm mới gói và quyền:** nút **Cập nhật gói và quyền** gọi `AccountViewModel.refreshAccount()`, truy xuất profile, entitlement và role trực tiếp từ Supabase bằng session hiện có; role RPC được xác minh rõ ràng lần cuối. Ngay khi bắt đầu làm mới, quyền đặc biệt và gói Premium cũ được tạm ẩn. Nếu mạng/phiên lỗi thì **không trả lại quyền cũ trên UI**, báo kiểm tra kết nối/đăng nhập lại; nhạc offline vẫn không bị ảnh hưởng. Đóng bảng quản trị cũng yêu cầu làm mới thông tin hiển thị.
- **Demo offline:** nhãn Demo trên thiết bị, không hiện công cụ quản trị, làm mới Supabase hay đồng bộ cloud. Không làm biến thể Demo thành Premium thật.

## Thực hiện trong mã nguồn

- `app/src/main/java/com/example/musicflow/core/account/AccountHomePlan.kt` — nội dung lời chào, gói, vai trò và CTA theo dữ liệu/điều kiện backend.
- `app/src/main/java/com/example/musicflow/feature/account/AccountScreen.kt` — layout và nút/nhóm mở rộng/khối Guest.
- `app/src/main/java/com/example/musicflow/feature/account/AccountViewModel.kt` — refresh account fail-closed, không ghi khóa/mật khẩu QA.
- `app/src/test/java/com/example/musicflow/core/account/AccountHomePlanTest.kt` — 6 JVM cases mới.
- `app/src/androidTest/java/com/example/musicflow/ui/AccountHomeV2UiTest.kt` — 8 UI cases mới.
- `scripts/run-account-home-v2-tests.ps1` — build + test có tùy chọn `-Serial` cho AVD QA sẵn sàng.

## Kiểm thử thực tế

| Hạng mục | Kết quả |
|---|---|
| JVM `AccountHomePlanTest` (6) + `AccountWorkspacePlanTest` (7) + `MusicFlowAccessTest` (8) + `AccountTierTest` (5) | **26/26 PASS** |
| Android `AccountHomeV2UiTest` (8) + `AccountWorkspacesUiTest` (3) + `MusicFlowRolesUiTest` (7) + `CreatorApplicationReviewUiTest` (3) | **21/21 PASS**, emulator Android 11/API 30 |
| Gradle `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:assembleRelease` | **PASS** |
| APK QA zipalign, apksigner, aapt, bản Release không debuggable | **PASS** |
| Cài nâng cấp và khởi động lạnh **chính APK đã bàn giao** trên AVD API30 | **PASS**, `Status: ok`, `LaunchState: COLD`, app process đang chạy |

Hai lần chạy UI: **18/18 PASS** trước khi bổ sung ba ca Moderator/Artist/Developer và chỉnh nội dung giới thiệu; sau đó chạy lại phiên bản cuối **21/21 PASS**. Android tests dùng fixture `AccountUiState` và fake RPC in-memory; không thay đổi tài khoản thật hoặc nội dung hosted Supabase.

**Logs:** `qa-logs/musicflow-account-home-v2-build.log`, `qa-logs/musicflow-account-home-v2-qa.log`, `qa-logs/demo-apk-account-home-v2-package.log`.

## APK bàn giao

- Tệp: `dist/MusicFlow-1.0-Demo-AccountHomeV2-Android.apk`.
- Kích thước: **31.082.173 bytes**.
- SHA-256: `034E6A976D062315B750D49A98F4DAC200F175F1F63569D8CAAE5F46BCA03E56`.
- `versionName=1.0`, `versionCode=1`, `minSdk=29`, `targetSdk=36`, `debuggable=false`.
- Chữ ký bằng **debug-keystore QA của bản demo** trên APK release-mode. Không phải chứng thư ký production, không phát hành thương mại.
- Emulator QA riêng `MusicFlow_AccountHomeV2_QA_API30`, serial `emulator-5580`; chỉ dọn AVD riêng sau kiểm thử, giữ nguyên các AVD có sẵn.

## Lặp lại kiểm thử

```powershell
.\scripts\run-account-home-v2-tests.ps1
# Nếu đã bật emulator QA riêng, chạy đầy đủ Android UI:
.\scripts\run-account-home-v2-tests.ps1 -Serial emulator-5580
```

## Giới hạn và bước tiếp theo

- **Chưa nghiệm thu** HTTP JWT thực tế bằng đủ 8 tài khoản QA sau khi seed role. Bộ UI fixture không chứng minh toàn bộ hành vi Supabase khi đổi vai trò; bước kế tiếp phải thử trong giao diện bằng mật khẩu QA do chủ dự án tự nhập, không đưa vào script/repo.
- **Chưa chạy** full playback/media regression, đa phiên bản emulator, thiết bị thật, Creator review E2E hay thanh toán. Bản demo offline/playlist từng có QA riêng; mốc này chỉ kiểm tra các thay đổi account/role.
- **Account UX v3 đề xuất:** thêm trang điểm đến với nhóm tác vụ rõ ràng cho Owner/Moderator/Artist/Author (có trạng thái trống, lỗi và nút làm lại); giảm số lần cuộn trong Creator Studio và Owner Dashboard; kiểm tra chữ to, vùng bấm, TalkBack và hành vi Android Back; hoàn thiện checklist actor/use case phục vụ bảo vệ báo cáo.
- Bảo toàn phạm vi **APK demo và báo cáo**, không tự mở streaming có bản quyền chưa được cấp phép hoặc tính năng thu phí.
