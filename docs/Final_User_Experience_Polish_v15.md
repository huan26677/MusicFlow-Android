# MusicFlow v15 — Final UX Cleanup (không thêm tính năng)

**Ngày:** 2026-11-09. **Loại cập nhật:** điều chỉnh giao diện và câu chữ theo đề xuất UX; không bổ sung actor/use case, không thay API/RPC/SQL/RLS, không chạm Room, SAF, playlist, quyền phát nhạc, entitlement hay thanh toán.

## Phạm vi đã chỉnh

| Khu vực | Vấn đề còn lại từ v14 | UX được cải thiện |
|---|---|---|
| Online Catalog | Người nghe thấy trường địa chỉ API/HTTPS read-only dù không thể chỉnh; cách giới thiệu thiên về máy chủ | Khi danh mục do MusicFlow quản lý: chỉ giới thiệu nhạc đã kiểm quyền, ẩn trường máy chủ; nút Tải lại và trạng thái vẫn giữ nguyên. Nguồn nhạc thủ công vẫn có trường nhập, nút Lưu/Kết nối/Ngắt không đổi |
| Online Empty/Error | Nhắc máy chủ, API; trên font lớn nút Demo và nút điều hướng có thể khuất | Thông báo ngắn, nêu cách thử lại hoặc nghe nhạc offline; vùng trống tự cuộn, nút nguồn và tiêu đề xếp dọc khi hẹp/chữ lớn |
| Community | Ô bình luận nằm sau toàn bộ danh sách, nút 5 sao dễ chật trên màn hẹp, tên ngày giờ còn chuỗi máy chủ | Ô nhập/Đăng/Sửa lên trước danh sách; nút sao chia nhóm ở cỡ chữ lớn; giữ đóng màn cố định, nội dung cuộn; ngày giờ hiển thị giờ địa phương kiểu Việt; trạng thái Demo rõ ràng |
| Moderation | Hai nút thao tác đặt cạnh nhau dễ tràn ở chữ lớn; lỗi từ RPC xuất nguyên văn | Xếp nút dọc khi cần; ngày giờ dễ đọc; thông báo kết quả riêng cho Ẩn và Bỏ qua; lỗi an toàn và có hành động |
| Account/roles | Một số lời nhắc nhắc tên Supabase hoặc hiển thị nguyên văn lỗi HTTP thay vì hướng dẫn người nghe | Dùng thông báo đăng nhập, quyền, đăng ký và Demo bằng ngôn ngữ thông thường; gom lỗi xác thực kỹ thuật thành hướng dẫn thử lại, xác nhận email hoặc kiểm tra mật khẩu; không thay điều kiện kiểm quyền |

## Impact-driven Regression

- **Direct:** `ONLINE_RIGHTS`, `COMMUNITY_COMMENTS`, `COMMUNITY_MODERATION`, `AUTH`, `ROLES_ADMIN` và `RELEASE_BACKUP` nếu đóng gói APK QA mới.
- **Side effects phải theo dõi:** đăng nhập Guest/Free/Premium, Owner/Admin, đường vào Community, trạng thái rỗng có thể cuộn trên màn hình 320dp, người dùng phóng chữ 200%, lựa chọn thư viện offline, quyền sao/bình luận do server quyết định.
- **QA policy:** `full_required` do nhiều module giao diện/auth cùng đóng gói; phải phân biệt số `@Test` có trong registry với các bài đã thực chạy. Unit/Compose/instrumentation/signed APK chỉ PASS theo log tương ứng.
- **Regression mới:** `CommunityUiPresentationTest` (lời lỗi/giờ Việt, không hiển thị chi tiết backend), `AccountFriendlyErrorsTest` (thông báo xác thực dễ hiểu) và `CommunityDemoUiTest` (nguồn managed/manual và thao tác tại font 200%). Giữ toàn bộ test tag cũ.

## Bất biến

- Nghe nhạc offline không cần đăng nhập; không thay trình phát, MediaStore, SAF, Room hoặc thư viện nhạc.
- Community ghi thật chỉ khi người dùng có phiên đăng nhập và bài hát hợp lệ; giao diện Demo không phát sinh RPC lên Supabase.
- Không đổi quyền Owner/Admin/Moderator; server kiểm duyệt v14.1–v14.2 giữ nguyên; không tự tạo nội dung public/licensed.
- Không thêm thanh toán thật, thông báo push, nguồn nhạc hay quyền mới; APK nếu có vẫn chỉ ký Debug QA để báo cáo, không phải bản sản xuất.

## Bằng chứng và giới hạn

**Build / JVM:** `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` **PASS**, **218/218 JVM PASS**. Python impact-engine **19/19 PASS**, web-admin **4/4 PASS**; Project Brain script tests **15/15 PASS**. Modular QA catalog **22 module, 492 phương thức @Test đã đăng ký**.

**Android QA:** AVD riêng `MusicFlow_UxFinalV15_QA_API36`, không chạy trên thiết bị Android cá nhân đang kết nối. Bản Demo/Debug: **10/10 UI Community PASS** (sao/chữ lớn/nhập bình luận, nguồn do MusicFlow quản lý vs. nguồn thủ công, kiểm duyệt), **8/8 UI Auth/Owner/Admin PASS**, **4/4 UI v13 polish PASS**, **1/1 MainActivity → Community Demo PASS**; tổng **23/23 selected Android tests PASS** theo từng lượt. Lượt 10/10 cuối sau khi đổi bố cục tiêu đề/tên nút vẫn PASS. Log local tại `qa-logs/musicflow-v15-ux-*-api36.local.log`.

**APK QA v15:** `dist/MusicFlow-1.0-Demo-FinalUXV15-Android.apk`, **31.786.685 bytes**, SHA-256 `9BC366335021BBEF1827D4CBC7C0EA9755CD46D357A7A47BCC64F196EC14DF3D`. `Gradle Release`, zipalign, apksigner, hash PASS; ký bằng **Android Debug QA key**, `debuggable=false`. Đây là APK Demo dành cho môn học, **không phải Play Store/production**. APK trong `dist/` Git-ignored, cần sao lưu riêng.

**APK thực trên API36:** hash/chữ ký/zipalign **PASS**; `adb install -r` lên AVD `MusicFlow_UxFinalV15_QA_API36` **PASS**; cold launch `MainActivity` **PASS**, tiến trình sống, không có `FATAL EXCEPTION` trong log gần lúc mở. Test `SignedCommunityNavigationUiTest` chạy trên **chính APK ký QA v15**: **1/1 PASS** (không dùng debug-only `ComponentActivity`). Lưu ý: 23/23 Android chọn lọc ở trên chạy bản Debug; **1/1 UI signed là lần kiểm tra riêng**, không gộp thành full regression.

**QA evidence Git:** `qa-logs/musicflow-v15-ux-community-api36.log`, `qa-logs/musicflow-v15-ux-impacted-api36.log`, `qa-logs/musicflow-v15-signed-package.log`, `qa-logs/musicflow-v15-signed-smoke-api36.log`. Các file này được tạo từ log gốc và chỉ chứa marker/phương thức đã xác minh, không đưa bí mật hoặc log thô lên Git.

**Còn thiếu:** policy `full_required` trên 492 phương thức, chưa chạy toàn bộ 492; chưa nghiệm thu JWT đa tài khoản Community qua HTTP, TalkBack và UAT thiết bị thật, đa nhà cung cấp SAF hoặc cài đè v14→v15 có fixture Room/SAF. Không được suy diễn PASS từ riêng build/QA lựa chọn.
