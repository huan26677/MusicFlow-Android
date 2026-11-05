# MusicFlow — Kế hoạch Owner, quản trị viên nền tảng, Developer, tác giả và nghệ sĩ

**Ngày bổ sung:** 2026-11-05  
**Trạng thái cập nhật 2026-11-05:** **ĐÃ TRIỂN KHAI BẢN NỀN TẢNG (MVP), CHƯA HOÀN TẤT TOÀN BỘ ROADMAP**. Đã có schema/RPC phân quyền Supabase, giao diện Owner/PlatformAdmin nội bộ, lối tắt Developer debug và yêu cầu/hồ sơ Nghệ sĩ/Tác giả. **Owner chưa được cấp** do cần xác minh tài khoản chủ sở hữu; Creator Studio tải audio/bằng chứng, khóa tài khoản Auth và xử lý khiếu nại nâng cao vẫn ở backlog. Xem `docs/Trien_Khai_Vai_Tro_Owner_Admin_Developer_v1.md`.

## 1. Tình trạng hiện có

- Android: đăng ký/đăng nhập Supabase bằng email + mật khẩu, Guest/Free/Premium và một số quyền cloud phụ thuộc entitlement máy chủ. Có màn Account cho người nghe.
- Supabase: có `auth.users`, `account_profiles`, `account_entitlements`, bảng catalog và công cụ quản trị nhạc bản quyền bằng **CLI nội bộ**. **CLI không phải giao diện quản trị viên**, không có vai trò Admin/Artist/Author riêng dành cho đăng nhập sử dụng app/web.
- **Hiện đã bổ sung mã nguồn/backend:** role Owner/PlatformAdmin/Developer, quản trị người dùng và cấp quyền trong dashboard nội bộ, lối tắt Developer debug, gửi/duyệt yêu cầu creator và hồ sơ cơ bản. **Chưa có Owner được chỉ định và chưa có** kho upload audio Creator Studio, duyệt chứng cứ bản quyền Creator end-to-end, đình chỉ Auth nâng cao hay hệ thống khiếu nại hoàn chỉnh. Không nhầm mốc code/MVP với toàn bộ sản phẩm đã nghiệm thu.

## 2. Mô hình tài khoản mục tiêu

**Tách biệt hai khái niệm:** `Gói sử dụng` (Guest, Free, Premium) và `Vai trò` (Listener, Artist, Author, Moderator, PlatformAdmin, Owner, Developer). Một nghệ sĩ có thể dùng Free/Premium; một tài khoản có thể vừa là tác giả vừa là nghệ sĩ. **Premium không mang quyền quản trị hoặc tự xuất bản; Developer không đồng nghĩa với Admin.**

| Vai trò | Quyền theo kế hoạch | Giới hạn |
|---|---|---|
| Khách (Guest) | Nghe nhạc offline và danh mục online hợp pháp cho phép khách; không đồng bộ cloud | Không có danh tính đăng nhập |
| Người nghe (Listener / Free / Premium) | Quản lý hồ sơ, playlist, lịch sử; quyền cloud theo entitlement hiện hành | Không quản lý người khác, không tự gán vai trò |
| Nghệ sĩ (Artist) | Hồ sơ nghệ sĩ, nghệ danh/ảnh/giới thiệu, danh sách tác phẩm đã tham gia biểu diễn, theo dõi trạng thái gửi bản thu | Không công khai bản thu khi chưa được duyệt quyền phát |
| Tác giả (Author/Composer/Lyricist) | Hồ sơ tác giả, tín chỉ sáng tác/soạn nhạc/viết lời, khai báo chủ sở hữu và giấy phép các tác phẩm mình quản lý | Không mặc định sở hữu bản ghi âm chỉ vì được ghi công tác giả |
| Kiểm duyệt viên (Moderator) | Kiểm tra metadata, bằng chứng quyền, duyệt/từ chối nội dung theo phạm vi được giao | Không xem mật khẩu, khóa API, không cấp Admin/Premium |
| **Owner (chủ sở hữu nền tảng)** | Quyền cao nhất trong **quản trị nghiệp vụ MusicFlow**: quyết định phân quyền, cấp/thu hồi quản trị viên cộng tác, quản lý cấu hình và duyệt các tác vụ nhạy cảm | Chỉ khởi tạo/chuyển Owner qua quy trình backend được bảo vệ; không nhúng khóa Supabase vào APK; không thể tự nhận Owner bằng sửa dữ liệu từ client |
| **Cộng tác quản trị viên nền tảng (PlatformAdmin)** | Dashboard quản lý người dùng, khóa/mở khóa theo quy trình, duyệt nghệ sĩ/tác giả và nội dung, xử lý báo cáo, xem log theo **quyền Owner giao** | Không thể thay Owner, khóa Owner hoặc tự nâng quyền; thao tác quan trọng có kiểm tra quyền ở backend và audit |
| **Developer (tài khoản phát triển)** | Lối tắt đến **công cụ phát triển/kiểm thử MusicFlow**: chẩn đoán, build/version, log đã che dữ liệu nhạy cảm, màn QA hoặc dữ liệu thử nghiệm | Không có quyền quản lý người dùng, cấp Premium hay xuất bản nhạc nếu không được cấp vai trò riêng; không phải cách bỏ qua đăng nhập |

**Quy tắc quyền:** Owner có thể ủy quyền và thu hồi PlatformAdmin; PlatformAdmin chỉ làm việc trong phạm vi được giao; Moderator chỉ kiểm duyệt; Developer là **vai trò kỹ thuật độc lập**, không nằm trong cấp bậc quản trị. Mỗi vai trò có thể dùng gói Free/Premium độc lập.

**“Lối tắt vào ứng dụng” cho Developer:** thiết kế một đường vào nhanh từ **Tài khoản → Công cụ nhà phát triển** trong bản debug/QA nội bộ, giúp đến khu vực kiểm thử và chẩn đoán. Shortcut **không bỏ qua xác thực Supabase**, không tự đăng nhập vào tài khoản khác và không tự gán Owner/Admin. Khi gọi backend, vẫn phải đăng nhập bình thường và kiểm tra role phía server. Không cài sẵn email, mật khẩu hoặc token trong APK; mặc định ẩn công cụ này trên bản demo bàn giao thông thường.

**Không xây tài khoản đăng nhập có quyền `service_role` hoặc đưa service/secret key vào APK.** Owner, PlatformAdmin, Moderator và Developer cần phân quyền phía Supabase/backend đáng tin cậy; client chỉ hiển thị chức năng được phép. “Toàn quyền Owner” là quyền nghiệp vụ có kiểm soát, không phải truy cập khóa quản trị cơ sở dữ liệu từ Android.

## 3. Các mốc triển khai theo thứ tự

### A1 — Vai trò & bảo mật nền tảng (Ưu tiên cao, làm trước UI)

- Thiết kế bảng phân vai trò (`user_roles`) và yêu cầu nâng vai trò (`role_applications`), quản lý trên backend; một người có thể giữ nhiều vai trò.
- Có role `owner`, `platform_admin`, `developer` tách biệt; chỉ quy trình backend tin cậy được khởi tạo/chuyển Owner. Quyền PlatformAdmin được Owner ủy quyền có phạm vi rõ ràng, có thể thu hồi. Developer không mặc nhiên kế thừa quyền của quản trị viên.
- Cấp/thu hồi vai trò chỉ qua nghiệp vụ server-side được bảo vệ; không lưu quyền vào `user_metadata` do người dùng tự chỉnh. Giữ `account_entitlements` độc lập.
- RLS cho mọi bảng mới: chỉ đọc/sửa dữ liệu mình được phép, chống IDOR và tự thăng quyền; không cấp quyền `anon/authenticated` chỉnh role/entitlement.
- Chính sách xác minh tài khoản creator và phê duyệt bằng chứng quyền, trạng thái `pending / approved / rejected / suspended`; không đồng nghĩa email tự xác nhận là xác minh nghệ sĩ/tác giả.
- **Nghiệm thu:** người nghe không đọc/sửa vai trò người khác; request giả mạo bị từ chối, self-elevation bị chặn; PlatformAdmin không thể đổi Owner hay tự tăng phạm vi quyền; Developer không gọi được API quản trị; không ảnh hưởng nghe nhạc offline.

### A2 — Dashboard Owner và quản trị viên nền tảng (Ưu tiên cao)

- **Ưu tiên trang quản trị Web nội bộ** tách khỏi APK nghe nhạc; sau này có thể làm màn Admin trong ứng dụng nếu được yêu cầu.
- **Owner Console:** quản lý tài khoản quản trị viên cộng tác, cấp/thu hồi phạm vi quyền, giám sát hệ thống và xác nhận thay đổi nhạy cảm, xem nhật ký hành động.
- **PlatformAdmin Console:** danh sách người dùng (tìm kiếm, lọc, trạng thái khóa, Free/Premium, vai trò, ngày tạo); xem chi tiết hồ sơ, duyệt yêu cầu trong phạm vi Owner đã giao.
- Tạm khóa/mở khóa người dùng có lý do và xác nhận; duyệt yêu cầu Artist/Author; quyền cấp/thu hồi do backend xác nhận. Không hiển thị hoặc lấy lại mật khẩu của người dùng. Cộng tác viên không thể khóa Owner.
- Quản trị nội dung: danh sách tác phẩm chờ duyệt, giấy phép/chủ sở hữu/credit, chấp thuận/từ chối, gỡ phát online, báo cáo vi phạm, lịch sử xét duyệt.
- Audit log bất biến về mặt nghiệp vụ: ai, hành động, đối tượng, thời gian, lý do; hạn chế quyền truy cập log.
- **Nghiệm thu:** User/Artist/Developer không mở được API quản trị; Owner cấp và thu hồi quyền cộng tác có audit; PlatformAdmin không vượt phạm vi, không thao tác trên Owner; quy trình khóa/duyệt đúng quyền và có phân biệt nhạc online với tệp offline cá nhân.

### A2.5 — Developer Quick Access (sau A1; không chặn A3)

- Bổ sung tài khoản `developer` được xác minh role trên Supabase; trong **bản debug/QA nội bộ** hiển thị **“Công cụ nhà phát triển”** từ màn Tài khoản để vào nhanh màn thử nghiệm, xem build/version, chẩn đoán và log đã ẩn dữ liệu nhạy cảm.
- Nếu cần đăng nhập để dùng cloud hoặc API, Developer vẫn dùng cơ chế xác thực chuẩn; shortcut **không** sử dụng mật khẩu hardcode, token dùng chung, chế độ impersonate hoặc bypass RLS. Test data phải nằm trong phạm vi QA/sandbox được cho phép.
- Bản APK demo bàn giao mặc định không tự hiện Developer tools cho người dùng bình thường; nếu chủ dự án muốn trình diễn công cụ này thì tạo **bản QA có định danh và phân quyền rõ**, không biến nó thành cửa hậu trên bản phát hành.
- **Nghiệm thu:** user thường không vào công cụ Developer; role giả mạo ở client không gọi được API đặc quyền; Developer không có quyền Owner/PlatformAdmin chỉ vì dùng lối tắt.

### A3 — Tài khoản Nghệ sĩ và Tác giả (Ưu tiên cao)

- Luồng đăng ký người nghe bình thường → gửi yêu cầu **trở thành Nghệ sĩ/Tác giả** → Owner, PlatformAdmin hoặc Moderator **có quyền duyệt** xét duyệt → hồ sơ được bật tính năng tương ứng.
- Hồ sơ công khai khi được duyệt: nghệ danh/tên bút danh, ảnh, giới thiệu, liên kết được xác minh, bài hát/tác phẩm/album và tín chỉ.
- **Studio riêng**: lưu nháp tác phẩm, tải bản thu lên vùng riêng, thêm metadata và credit; khai báo bên nắm bản quyền bản ghi âm, quyền tác giả, quyền phát trực tuyến và quyền tải về (nếu có), đính kèm bằng chứng hợp lệ.
- Trạng thái nội dung: Nháp → Chờ xét duyệt → Được duyệt/Từ chối → Xuất bản; cho phép sửa/bổ sung hồ sơ và xem lý do từ chối.
- Hỗ trợ **nhiều người tham gia một tác phẩm** (nghệ sĩ biểu diễn, tác giả nhạc, lời, nhà sản xuất, chủ master), không đồng nhất tên nghệ sĩ với người có quyền phát hành.
- **Nghiệm thu:** creator chỉ sửa nháp của mình; chưa duyệt không hiện công khai; catalog chỉ phát khi quyền streaming được duyệt và còn hiệu lực; tác phẩm của người khác không bị truy cập trái phép.

### A4 — Quản trị mở rộng & chất lượng (Sau A1–A3)

- Dashboard tổng quan số người nghe, Creator và tác phẩm (chỉ số tổng hợp, không công bố dữ liệu riêng tư); công cụ xử lý khiếu nại bản quyền, chặn/gỡ/quay lại xét duyệt có log.
- Kiểm thử quyền đa tài khoản/đa vai trò (Owner/PlatformAdmin/Developer/Creator/Listener), refresh token, phiên hết hạn, trạng thái bị đình chỉ, quyền bị thu hồi, hiệu năng danh sách và accessibility.
- Nếu có trả phí/doanh thu cho nghệ sĩ: **đề xuất và phê duyệt phạm vi pháp lý/hợp đồng/thanh toán riêng**; không tự xây phần trả tiền hay chia doanh thu trong demo.

## 4. Tiêu chí nghiệm thu & bảo vệ dữ liệu

1. **Đăng nhập được không có nghĩa là có quyền Owner/PlatformAdmin/Developer:** quyền được xác minh ở máy chủ cho từng API/RPC, không chỉ ẩn nút hoặc tin vào shortcut ở client.
2. **Trạng thái Premium độc lập vai trò Creator/Developer/Owner/PlatformAdmin**; không thể tự cấp Premium/Artist/Owner/PlatformAdmin/Developer bằng sửa request hoặc metadata.
3. Chỉ công bố nhạc **online** khi có giấy phép/ủy quyền phát hợp lệ và credit rõ ràng. **Tệp offline do người dùng tự tải/lưu vẫn được phát bình thường**, không bắt đăng ký creator hay duyệt bản quyền từng tệp cá nhân.
4. Người dùng có quyền quản lý hồ sơ và yêu cầu xử lý dữ liệu của mình theo phạm vi thiết kế; Admin chỉ dùng thông tin cần thiết, không truy xuất mật khẩu.
5. Chạy unit tests cho luật role/rights, kiểm thử RLS/Auth cho phân quyền (gồm chống chiếm Owner, quản trị cộng tác bị giới hạn và Developer không bypass) + Android/Web UI targeted tests; **không gắn nhãn PASS trước khi thực hiện**.

## 5. Thứ tự ưu tiên so với điểm dừng hiện tại

- **Hiện tại:** MusicFlow đã có APK demo phục vụ báo cáo. **A1 có schema/RPC nền tảng**, A2 dashboard nội bộ, A2.5 debug quick access; **A3 đã bổ sung Creator Studio v1 với bản nháp metadata/tín chỉ/bằng chứng khai báo, gửi duyệt và quản trị viên duyệt/trả sửa trên Supabase**. A4 có audit + moderation cơ bản. **Chưa nghiệm thu E2E A2–A4 vì chưa có Owner thực được xác minh**; chưa có Creator upload audio/ảnh/giấy phép lên Storage, host web hay khóa tài khoản đầy đủ. Kết quả targeted QA của v1 tại `docs/Creator_Studio_v1.md`. Không làm thay đổi mục tiêu sideload/demo, không yêu cầu email thật hay phát hành Play.
- **Khi chủ dự án yêu cầu phát triển nhánh vai trò:** bắt đầu bằng **A1 (role schema + security acceptance)**, tiếp theo A2 Owner/PlatformAdmin Web, A2.5 Developer Quick Access khi cần bản QA, A3 Creator Studio; tránh làm UI giả có nút quản trị nhưng backend chưa kiểm quyền.
- **Tài khoản QA:** trước khi cho thử Owner/PlatformAdmin/Developer/Creator phải có backend đúng vai trò và kịch bản QA tách biệt; không tự cấp quyền cho tài khoản người dùng đang tồn tại.

**Nguồn liên kết:** `docs/Ke_Hoach_Du_An.md`, `docs/Pham_Vi_MusicFlow_1_0.md`, `docs/Kien_Truc_Mo_Rong_Va_Lo_Trinh_Full_Product.md`.
