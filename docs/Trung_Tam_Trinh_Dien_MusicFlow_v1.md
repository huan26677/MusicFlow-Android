# MusicFlow — Trung tâm trình diễn 9 vai trò (Demo Center v1)

**Ngày triển khai:** 2026-11-07. **HEAD nền:** `7d1bb06` (Simulated Premium Billing v1). **Mục tiêu:** cho phép bảo vệ báo cáo bằng các hành trình actor minh họa ngay trong APK, không cần mật khẩu QA, không cần kết nối Internet và không thay bất cứ dữ liệu thật nào trên Supabase.

## Cách dùng

**Tài khoản → Trình diễn 9 loại tài khoản (giả lập)**. Chọn vai trò trong menu, thực hiện từng bước có sẵn, xem phản hồi minh họa, đổi vai trò hoặc **Làm lại**. Tiến độ của từng kịch bản riêng được lưu trên máy và khôi phục khi mở lại ứng dụng. Những vai trò mẫu này **không phải tài khoản người dùng**, không được thêm vào `AccountIdentity.access`, không cấp JWT và không được chuyển vào `AccountRolePanel`.

| Vai trò mẫu | Tình huống có thể trình diễn |
|---|---|
| Guest | Quét thư viện offline mẫu, phát bản demo và hàng đợi 3 bài giả định |
| Free | Đồng bộ 3 playlist/8 yêu thích mẫu, khôi phục thư viện minh họa |
| Premium | So sánh quota Free/Premium minh họa, đồng bộ tự động mẫu, mở màn **thanh toán Premium giả lập** đã có |
| Nghệ sĩ (Artist) | Tạo hồ sơ mẫu, nộp bản nháp giả, xem yêu cầu bổ sung chứng cứ mẫu |
| Tác giả (Author) | Ghi tín chỉ sáng tác mẫu, gửi đơn xác minh giả |
| Moderator | Xem bản nháp chờ kiểm duyệt, phản hồi yêu cầu bổ sung chứng cứ giả |
| PlatformAdmin | Tìm ba người dùng mẫu, xét duyệt đơn Creator mẫu |
| Owner | Xem thống kê nền tảng mẫu, cấp quyền xét duyệt cho Admin mẫu, xem audit mẫu |
| Developer | Xem trạng thái QA mẫu, mô phỏng xử lý lỗi phát nhạc |

**Lưu ý:** các con số quota, người dùng, playlist, dữ liệu review và trạng thái QA bên trên **chỉ là ví dụ**. Không được trình bày chúng như số liệu thật từ cơ sở dữ liệu hoặc kết quả kiểm thử phần cứng.

## Tài khoản Demo offline — lối đăng ký/đăng nhập riêng

Trên Trang Tài khoản, khi chưa đăng nhập, chọn **Demo offline** thay vì **Supabase**. Sau đó chọn **Tạo tài khoản** hoặc **Đăng nhập**, nhập email demo và mật khẩu dùng trên thiết bị (không dùng mật khẩu Supabase). `DemoAccountStore` đã có cơ chế băm mật khẩu bằng salt và PBKDF2, chỉ giữ tài khoản local. Bản này **đưa lại nút chọn Demo lên UI**, cho phép tạo/đăng nhập khi Supabase chưa cấu hình, đồng thời có thể khôi phục một phiên Demo đã chủ động đăng nhập khi mở lại app.

Đăng nhập Demo và Demo Center là hai luồng độc lập: người xem **không cần tạo tài khoản Demo** để xem các kịch bản actor. Tài khoản Demo offline luôn là Free và `MusicFlowAccess.NONE`, **không bao giờ thành Owner/Admin/Moderator thật**. Đăng nhập Supabase thật, role refresh, cloud sync và Premium server vẫn chỉ có thể thực hiện qua các luồng cũ được xác thực.

## Kỹ thuật và ranh giới bảo mật

- `core/account/DemoCenterPlan.kt`: chín persona, các bước biểu diễn và quy tắc tiến độ thuần Kotlin. Không lưu `MusicFlowAccess` hay bearer token.
- `core/account/DemoCenterStore.kt`: chỉ dùng `SharedPreferences("musicflow_demo_center_v1_ui_only")` cho persona và bước đã hoàn thành. Không liên kết dữ liệu trong bảng Auth, Owner, Catalog, Playlist, Premium.
- `feature/account/DemoCenterScreen.kt`: màn hình tương tác, đổi actor, hoàn thành/reset, và chuyển sang thanh toán giả lập dành cho Premium; các nút chỉ gọi callbacks cục bộ. Không có tham chiếu `AccountApi`, `roleRpc`, `CloudLibrarySync` hoặc media player.
- `feature/account/AccountScreen.kt`: lối vào Demo Center ở đầu màn hình, nút chuyển Supabase/Demo offline cho Guest, và liên kết thanh toán mẫu.
- `feature/account/AccountViewModel.kt`: khôi phục phiên local Demo khi đã được đăng nhập trước đó và không có phiên server được khôi phục; không suy diễn role từ dữ liệu local, không tự động gửi Supabase nếu local demo login.
- `scripts/run-demo-center-v1-qa.ps1`: full JVM, Web Admin 4 phép kiểm tra, bộ Android account/owner/creator/playlist/offline và DocumentsUI thật trên emulator QA được chọn; **không chạy trên emulator đang sử dụng của chủ dự án**.

## Bản trình diễn đề xuất (không cần Internet)

Mở MusicFlow → nghe nhạc Guest → Trang Tài khoản → Demo Center → chọn Owner, bấm ba bước minh họa ủy quyền/audit → đổi sang Artist, mô phỏng nộp bản nháp → chọn Moderator xem phản hồi mẫu → chọn Free/Premium mô phỏng cloud/quota → từ Premium mở **thanh toán giả lập** → trở lại Tài khoản chọn **Demo offline** để minh họa tạo và đăng nhập tài khoản riêng trên thiết bị. Kết thúc bằng Playlist Quick Add SAF và phát offline thật (đã có QA trước đây).

**Phân loại trong báo cáo:** Demo Center, tài khoản Demo, Premium payment và cloud giả là **mô phỏng**; player local/Room/SAF là các luồng kỹ thuật có bài kiểm thử tích hợp; các RPC Supabase/role JWT E2E của 8 người dùng thật **chưa được đánh PASS**.

## Bằng chứng nghiệm thu

- Source test mới: `app/src/test/java/com/example/musicflow/core/account/DemoCenterPlanTest.kt` và `app/src/androidTest/java/com/example/musicflow/ui/DemoCenterV1UiTest.kt`.
- Bằng chứng chạy cuối đợt lưu tại `qa-logs/musicflow-demo-center-v1-qa.log`; APK release QA và checksum lưu tại `qa-logs/demo-apk-center-v1-package.log`.
- Để tái chạy: `scripts/run-demo-center-v1-qa.ps1 -Serial emulator-5580` **chỉ khi đã khởi động AVD QA riêng**.

**Kết quả kiểm thử đã xác nhận sau khi hoàn thành mã (QA riêng Android 11/API30):**

| Nhóm | Kết quả |
|---|---|
| Full JVM unit tests | **190/190 PASS**, 33 suites, 0 errors/failures; có 4 `DemoCenterPlanTest` mới |
| Android regression / actor/UI/playback | **71/71 PASS**, có 7 `DemoCenterV1UiTest` mới |
| SAF thật qua DocumentsUI | **1/1 PASS** |
| Web Admin role refresh/fail-closed | **4/4 PASS** |
| Debug + AndroidTest + Release build, zipalign, chữ ký, aapt và SHA256 | **PASS** |
| Cài và cold launch **chính APK Release QA** Android API30 | **PASS**, `Status: ok`, `LaunchState: COLD`, PID còn |
| Mở Demo Center bằng Guest, chọn Owner trên APK Release đã ký | **PASS**, có ảnh chụp từ emulator |
| Android 13 / API33: cài/cold launch cùng APK đã ký, mở Demo Center từ Guest | **PASS (smoke)**, không phải full instrumentation API33 |

**APK mới:** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-CenterV1-Android.apk` — **31.327.933 bytes**, SHA256 **`47FBE9353CE700F432FE216F127AF2D50FBB35E3097607D5A752A697A352C481`**; package `com.example.musicflow`, versionName 1.0, versionCode 1, minSdk29/targetSdk36, không debuggable; Release-mode ký bằng debug key QA, **không dùng cho Google Play/production**. Chứng cứ ký/manifest/hash: `qa-logs/demo-apk-center-v1-package.log`.

Ảnh QA từ đúng APK: `qa-logs/screenshots/musicflow-demo-center-guest-api30.png`, `qa-logs/screenshots/musicflow-demo-center-owner-api30.png`; log smoke `qa-logs/musicflow-demo-center-release-smoke.log`. AVD dùng riêng `MusicFlow_DemoCenterV1_QA_API30` và kiểm thử tương thích nếu có `MusicFlow_DemoCenterV1_QA_API33`, phải xóa các AVD này khi kết thúc, không thay đổi emulator sẵn có của người dùng.

**Bằng chứng API33:** `qa-logs/musicflow-demo-center-api33-smoke.log`, `qa-logs/screenshots/musicflow-demo-center-guest-api33.png`. Ảnh chỉ chứa kịch bản Guest mô phỏng, không chứa thông tin tài khoản thực. Chưa thực hiện toàn bộ 72 instrumentation tests trên API33; không được gọi đó là full compatibility PASS.

## Giới hạn

Không sinh user thật, không quyết định đơn thật, không phát hành catalog online, không thu tiền, không bật cloud sync/auto sync bằng fake Premium, không đổi quyền Owner. Dự án giữ mục tiêu **APK sideload phục vụ môn học**. Để phát hành thật cần kiểm thử Auth JWT, Supabase RLS, giấy phép online, billing thực và nhiều thiết bị/API hơn.
