# MusicFlow Android — Account Journeys UX v4 (bản demo báo cáo)

**Ngày hoàn tất QA:** 2026-11-07 (giờ Việt Nam; buổi phát triển bắt đầu tối 2026-11-06). **HEAD nền:** `05985d3` (`Account Task UX v3`), local `main` sạch trước khi sửa. **Phạm vi:** phát triển một đợt hoàn chỉnh rồi kiểm thử tổng hợp cuối đợt, không chia thành nhiều mốc UX nhỏ, không thay đổi cơ sở dữ liệu hoặc quyền tài khoản hiện có.

## 1. Kết quả bàn giao

**Account Journeys v4** làm gọn thao tác theo từng actor, bảo vệ các thay đổi chưa lưu và tránh cấp/thu hồi/xuất bản khi người dùng chưa xác nhận rõ.

| Actor | Trải nghiệm sau nâng cấp |
|---|---|
| Guest | Nút **Nghe nhạc ngay, không cần đăng nhập** giữ ở đầu trang. Form email/password chỉ dành cho người muốn tài khoản; không còn nút đăng nhập Google bị vô hiệu gây hiểu nhầm. |
| Free / Premium | Trang tài khoản tập trung vào gói/vai trò/tác vụ chính. Chỉ bật **Lưu hồ sơ** khi tên 2–80 ký tự và thật sự có thay đổi; IME Done gửi đăng nhập/đăng ký, giảm thao tác. Hướng dẫn xác nhận email hướng tới người dùng, không yêu cầu vào Supabase Dashboard. Premium không tự cấp vai trò quản trị. |
| Artist / Author | Danh sách đơn xét duyệt lên trước; khi vai trò đang pending sẽ không hiện lại form gửi đơn trùng (vẫn có thể chọn loại vai trò khác). Khi đã là Creator, kho tác phẩm mở riêng; **Chỉnh sửa hồ sơ** tự tải dữ liệu ngay một lần, không thêm nút "Tải hồ sơ" phụ. Creator Studio chia nhóm thông tin, album tùy chọn thu gọn. Thoát editor/Back với thay đổi chưa lưu phải chọn **Tiếp tục sửa** hoặc **Bỏ thay đổi**. |
| Owner | Giao diện quản trị ưu tiên tìm người dùng. Chỉ hiện công cụ cấp/thu hồi sau khi chọn tài khoản; thay quyền là quyết định có người nhận, loại vai trò/quyền và lý do được **chụp bất biến** vào hộp thoại xác nhận. Nhãn vai trò/quyền dễ hiểu bằng tiếng Việt; phản hồi backend phải hợp lệ trước khi báo thành công. |
| PlatformAdmin | Khu vực chỉ theo quyền được ủy nhiệm. Các nhóm công việc quản trị/kiểm duyệt không bị trộn lẫn trong cùng trang dài. Không có công cụ gán Owner nếu không đủ quyền. |
| Moderator | Khu vực kiểm duyệt chia theo nhiệm vụ **Đơn Creator / Hồ sơ tác phẩm / Nhạc online** nhưng chỉ hiện nhóm được phép. Hồ sơ tác phẩm chờ duyệt có phần tóm tắt; bấm mở hồ sơ mới hiển thị metadata, tín chỉ và chứng cứ riêng tư. Quyết định yêu cầu lý do, xác nhận, và server phải trả `true`; nếu server từ chối, hồ sơ vẫn ở hàng chờ. |
| Developer | Công cụ QA chỉ ở bản Debug và chỉ khi server trả role Developer; không thay Owner/Admin. |

## 2. Điều hướng / ít thao tác thừa

- Thanh **Sáng tạo, Quản trị, Kiểm duyệt, Công cụ QA** được đặt phía trên vùng cuộn của trang vai trò để vẫn chuyển khu vực được khi đang xem form dài. Vùng cuộn reset về đầu khi đổi khu vực hoặc nhóm kiểm duyệt.
- Với các nút Back trong bảng vai trò, ưu tiên thu gọn công việc bên trong (studio, hồ sơ đang sửa, tổng quan Owner, lựa chọn user, nhóm kiểm duyệt) rồi mới đóng dialog chính. Editor bản nháp yêu cầu xác nhận nếu đã sửa mà chưa lưu.
- Kiểm duyệt có một nhóm công việc đang mở thay vì hiển thị cả ba cùng lúc. Lý do xét duyệt đặt trước danh sách tác phẩm; chỉ gọi RPC thay đổi dữ liệu sau hộp thoại xác nhận. Xuất bản/gỡ phát nhạc vẫn phải được Supabase kiểm tra giấy phép hợp lệ.
- List trạng thái trống/lỗi vẫn tách biệt; khi tải lại người dùng/đơn/tác phẩm lỗi, dữ liệu đặc quyền cũ không được trình bày như kết quả mới. `musicflow_my_access` vẫn là nguồn quyền truy cập.
- Đã bỏ **một lần gọi Supabase `musicflow_my_access` trùng lặp** khi người dùng bấm cập nhật gói/quyền: `AccountApi.currentAccount(requireFreshRoles=true)` lấy profile, entitlement và role một lần, throw khi không xác minh được; Account Home fail-closed về quyền và Premium khi lỗi. Khôi phục/đăng nhập bình thường vẫn cho nghe offline nếu role endpoint gián đoạn, không tự thêm đặc quyền.

## 3. Code và runbook

- `app/src/main/java/com/example/musicflow/feature/account/AccountScreen.kt` — biểu mẫu Guest, trạng thái đăng ký, nút Lưu hồ sơ, IME Done.
- `app/src/main/java/com/example/musicflow/core/account/AccountApi.kt` — chế độ strict roles khi refresh, không lặp RPC.
- `app/src/main/java/com/example/musicflow/feature/account/AccountViewModel.kt` — dùng đường strict, tiếp tục fail-closed.
- `app/src/main/java/com/example/musicflow/core/account/AccountReviewPlan.kt` — chỉ ra các khu vực review được phép theo RBAC, nhãn quyền cộng tác tiếng Việt.
- `app/src/main/java/com/example/musicflow/feature/account/AccountRolePanel.kt` — thanh công việc cố định, điều hướng Back, chọn Owner target trước khi hiện cấp/thu hồi, moderator task switch, hộp thoại xác nhận nhạc online và quyết định bất biến.
- `app/src/main/java/com/example/musicflow/feature/account/CreatorDraftStudio.kt` — nhóm trường cần thiết, album tùy chọn, xác nhận bản nháp chưa lưu, reviewer chỉ mở chi tiết khi cần, chờ server `true`.
- `app/src/test/java/com/example/musicflow/core/account/AccountReviewPlanTest.kt` — 6 unit tests về phân tách reviewer quyền hạn.
- `app/src/androidTest/java/com/example/musicflow/ui/AccountJourneysV4UiTest.kt` — 7 UI tests tích hợp về Owner, review tabs, catalog confirm, Creator profile, editor unsaved, xét duyệt metadata và Android Back.
- `app/src/androidTest/java/com/example/musicflow/ui/AccountHomeV2UiTest.kt`, `CreatorApplicationReviewUiTest.kt` — hồi quy cập nhật cho biểu mẫu pending/Guest mới.
- `scripts/run-account-journeys-v4-qa.ps1` — chạy **tất cả JVM unit tests**, **4 Web Admin role tests** và kiểm thử Android actor/Creator/Owner nếu truyền serial emulator riêng. Không đọc password/token QA, không thay đổi Supabase.

## 4. Nghiệm thu một lượt sau triển khai

| Kiểm thử | Kết quả cuối |
|---|---|
| Full `:app:testDebugUnitTest` | **172/172 PASS**, 29 test suites, 0 failures, 0 errors |
| `scripts/test-admin-web-role-refresh-v1.js` | **4/4 PASS** |
| Android targeted + regression, 9 UI suites trên isolated Android API30 AVD | **41/41 PASS**, bao gồm 7 ca mới |
| Gradle `assembleDebug`, `assembleDebugAndroidTest`, `assembleRelease` | **PASS** |
| QA Release APK `zipalign`, `apksigner`, `aapt` và SHA256 | **PASS** |
| Cài nâng cấp và khởi động lạnh **đúng APK đã bàn giao** trên emulator API30 | **PASS**, `Status: ok`, `LaunchState: COLD`, app process còn hoạt động |

**Lịch sử lỗi đã xử lý:** lần chạy Android tích hợp đầu đạt 40/41, 1 case test "unsaved draft" không đạt; sửa bổ sung dirty tracking. Lần thứ hai vẫn 40/41 vì test dựng một màn Creator Studio độc lập **không có vertical scroll**, khiến thao tác bấm nút ở cuối màn hình không tới nút thật. Cập nhật fixture test để giống bố cục ứng dụng (scrollable parent, `performScrollTo`) rồi **chạy lại TOÀN BỘ**: 41/41 PASS. Không giấu các lượt FAIL và không coi lỗi fixture là lỗi Supabase.

**Log lưu trong repo:** `qa-logs/musicflow-account-journeys-v4-qa.log`, `qa-logs/demo-apk-account-journeys-v4-package.log`.

## 5. APK demo

- **Tệp:** `dist/MusicFlow-1.0-Demo-AccountJourneysV4-Android.apk` (tệp trên máy phát triển, không phải sandbox download).
- **Kích thước:** 31.147.709 bytes.
- **SHA256:** `F5718EC4DB09E5E8DAAA456340FA191D26573053CA670901BE859F4A06BC2602`.
- **Package:** `com.example.musicflow`; `versionName=1.0`, `versionCode=1`; minSdk=29, targetSdk=36; `debuggable=false`.
- **Ký:** debug key QA trong APK release-mode, chỉ cài trực tiếp phục vụ demo/báo cáo, không phải Play release.
- QA AVD riêng: `MusicFlow_AccountJourneysV4_QA_API30`, `emulator-5580`. Sau nghiệm thu dừng/xóa AVD này, giữ nguyên AVD của người dùng.

## 6. Tái hiện QA / việc còn lại

```powershell
.\scripts\run-account-journeys-v4-qa.ps1
# Khi đã tự bật AVD QA riêng:
.\scripts\run-account-journeys-v4-qa.ps1 -Serial emulator-5580
```

**Chưa nghiệm thu:** đăng nhập JWT thực tế của cả 8 account QA sau seed, full media/playback regression trên bản thay đổi này, TalkBack thủ công ở mọi trang và thiết bị vật lý. Nếu cần kiểm thử live, chủ dự án tự nhập mật khẩu QA trong UI, không yêu cầu lưu/đọc mật khẩu từ repository. Quyền Owner, phân quyền Supabase, creator pending, nội dung streaming hợp pháp vẫn giữ nguyên, không phát hành thương mại.

**Bước tiếp:** chuẩn hóa checklist actor/use case theo mẫu báo cáo, cải thiện trải nghiệm người nghe trên màn chính/playlist ở một đợt hoàn chỉnh, thử TalkBack/chữ lớn khi có emulator QA và chuẩn bị một APK demo cuối cùng. Không lặp các milestone đã PASS.
