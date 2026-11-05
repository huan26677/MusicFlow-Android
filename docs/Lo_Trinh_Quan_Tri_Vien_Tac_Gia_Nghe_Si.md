# MusicFlow — Kế hoạch quản trị viên, tác giả và nghệ sĩ

**Ngày bổ sung:** 2026-11-05  
**Trạng thái:** **BACKLOG / CHƯA TRIỂN KHAI**. Đây là phạm vi mở rộng theo yêu cầu mới, không phải tính năng đã có trong APK demo hiện tại.

## 1. Tình trạng hiện có

- Android: đăng ký/đăng nhập Supabase bằng email + mật khẩu, Guest/Free/Premium và một số quyền cloud phụ thuộc entitlement máy chủ. Có màn Account cho người nghe.
- Supabase: có §auth.users§, §account_profiles§, §account_entitlements§, bảng catalog và công cụ quản trị nhạc bản quyền bằng **CLI nội bộ**. **CLI không phải giao diện quản trị viên**, không có vai trò Admin/Artist/Author riêng dành cho đăng nhập sử dụng app/web.
- **Chưa có:** trang quản trị người dùng, đăng nhập và phân quyền Admin, hồ sơ tác giả/nghệ sĩ, cổng gửi tác phẩm, xác minh danh tính/quyền sử dụng và duyệt bài qua dashboard. Không tuyên bố các chức năng này đã nghiệm thu.

## 2. Mô hình tài khoản mục tiêu

**Tách biệt hai khái niệm:** §Gói sử dụng§ (Guest, Free, Premium) và §Vai trò§ (Listener, Artist, Author, Moderator, Admin). Một nghệ sĩ có thể dùng Free/Premium; một tài khoản có thể vừa là tác giả vừa là nghệ sĩ. **Premium không mang quyền quản trị hoặc tự xuất bản.**

| Vai trò | Quyền theo kế hoạch | Giới hạn |
|---|---|---|
| Khách (Guest) | Nghe nhạc offline và danh mục online hợp pháp cho phép khách; không đồng bộ cloud | Không có danh tính đăng nhập |
| Người nghe (Listener / Free / Premium) | Quản lý hồ sơ, playlist, lịch sử; quyền cloud theo entitlement hiện hành | Không quản lý người khác, không tự gán vai trò |
| Nghệ sĩ (Artist) | Hồ sơ nghệ sĩ, nghệ danh/ảnh/giới thiệu, danh sách tác phẩm đã tham gia biểu diễn, theo dõi trạng thái gửi bản thu | Không công khai bản thu khi chưa được duyệt quyền phát |
| Tác giả (Author/Composer/Lyricist) | Hồ sơ tác giả, tín chỉ sáng tác/soạn nhạc/viết lời, khai báo chủ sở hữu và giấy phép các tác phẩm mình quản lý | Không mặc định sở hữu bản ghi âm chỉ vì được ghi công tác giả |
| Kiểm duyệt viên (Moderator) | Kiểm tra metadata, bằng chứng quyền, duyệt/từ chối nội dung theo phạm vi được giao | Không xem mật khẩu, khóa API, không cấp Admin/Premium |
| Quản trị viên (Admin) | Dashboard người dùng, xét duyệt vai trò, trạng thái tài khoản, bài hát và báo cáo vi phạm; kiểm tra nhật ký hành động | Mọi hành động nhạy cảm phải xác thực, có kiểm tra quyền máy chủ và audit |

**Không xây tài khoản đăng nhập có quyền §service_role§ hoặc đưa service/secret key vào APK.** Admin/Moderator cần phân quyền phía Supabase/backend đáng tin cậy; client chỉ hiển thị chức năng được cho phép.

## 3. Các mốc triển khai theo thứ tự

### A1 — Vai trò & bảo mật nền tảng (Ưu tiên cao, làm trước UI)

- Thiết kế bảng phân vai trò (§user_roles§) và yêu cầu nâng vai trò (§role_applications§), quản lý trên backend; một người có thể giữ nhiều vai trò.
- Cấp/thu hồi vai trò chỉ qua nghiệp vụ server-side được bảo vệ; không lưu quyền vào §user_metadata§ do người dùng tự chỉnh. Giữ §account_entitlements§ độc lập.
- RLS cho mọi bảng mới: chỉ đọc/sửa dữ liệu mình được phép, chống IDOR và tự thăng quyền; không cấp quyền §anon/authenticated§ chỉnh role/entitlement.
- Chính sách xác minh tài khoản creator và phê duyệt bằng chứng quyền, trạng thái §pending / approved / rejected / suspended§; không đồng nghĩa email tự xác nhận là xác minh nghệ sĩ/tác giả.
- **Nghiệm thu:** người nghe không đọc/sửa vai trò người khác; request giả mạo bị từ chối, self-elevation bị chặn; không ảnh hưởng nghe nhạc offline.

### A2 — Trang quản trị Admin (Ưu tiên cao)

- **Ưu tiên trang quản trị Web nội bộ** tách khỏi APK nghe nhạc; sau này có thể làm màn Admin trong ứng dụng nếu được yêu cầu.
- Đăng nhập Admin và danh sách người dùng: tìm kiếm, lọc, trạng thái đăng nhập/khóa, Free/Premium, vai trò, ngày tạo; xem chi tiết hồ sơ.
- Tạm khóa/mở khóa có lý do và xác nhận; duyệt yêu cầu Artist/Author; quyền cấp/thu hồi do backend xác nhận. Không hiển thị hoặc lấy lại mật khẩu của người dùng.
- Quản trị nội dung: danh sách tác phẩm chờ duyệt, giấy phép/chủ sở hữu/credit, chấp thuận/từ chối, gỡ phát online, báo cáo vi phạm, lịch sử xét duyệt.
- Audit log bất biến về mặt nghiệp vụ: ai, hành động, đối tượng, thời gian, lý do; hạn chế quyền truy cập log.
- **Nghiệm thu:** User/Artist không mở được API Admin; quy trình khóa/duyệt đúng quyền, có log, có phân biệt thao tác bản quyền online với tệp offline cá nhân.

### A3 — Tài khoản Nghệ sĩ và Tác giả (Ưu tiên cao)

- Luồng đăng ký người nghe bình thường → gửi yêu cầu **trở thành Nghệ sĩ/Tác giả** → Admin/Moderator duyệt → hồ sơ được bật tính năng tương ứng.
- Hồ sơ công khai khi được duyệt: nghệ danh/tên bút danh, ảnh, giới thiệu, liên kết được xác minh, bài hát/tác phẩm/album và tín chỉ.
- **Studio riêng**: lưu nháp tác phẩm, tải bản thu lên vùng riêng, thêm metadata và credit; khai báo bên nắm bản quyền bản ghi âm, quyền tác giả, quyền phát trực tuyến và quyền tải về (nếu có), đính kèm bằng chứng hợp lệ.
- Trạng thái nội dung: Nháp → Chờ xét duyệt → Được duyệt/Từ chối → Xuất bản; cho phép sửa/bổ sung hồ sơ và xem lý do từ chối.
- Hỗ trợ **nhiều người tham gia một tác phẩm** (nghệ sĩ biểu diễn, tác giả nhạc, lời, nhà sản xuất, chủ master), không đồng nhất tên nghệ sĩ với người có quyền phát hành.
- **Nghiệm thu:** creator chỉ sửa nháp của mình; chưa duyệt không hiện công khai; catalog chỉ phát khi quyền streaming được duyệt và còn hiệu lực; tác phẩm của người khác không bị truy cập trái phép.

### A4 — Quản trị mở rộng & chất lượng (Sau A1–A3)

- Dashboard tổng quan số người nghe, Creator và tác phẩm (chỉ số tổng hợp, không công bố dữ liệu riêng tư); công cụ xử lý khiếu nại bản quyền, chặn/gỡ/quay lại xét duyệt có log.
- Kiểm thử quyền đa tài khoản/đa vai trò, refresh token, phiên hết hạn, trạng thái bị đình chỉ, hiệu năng danh sách và accessibility.
- Nếu có trả phí/doanh thu cho nghệ sĩ: **đề xuất và phê duyệt phạm vi pháp lý/hợp đồng/thanh toán riêng**; không tự xây phần trả tiền hay chia doanh thu trong demo.

## 4. Tiêu chí nghiệm thu & bảo vệ dữ liệu

1. **Đăng nhập được không có nghĩa là có quyền Admin:** quyền được xác minh ở máy chủ ở từng API/RPC, không chỉ ẩn nút trên UI.
2. **Trạng thái Premium độc lập vai trò Creator**; không thể tự cấp Premium/Artist/Admin bằng sửa request hoặc chỉnh metadata.
3. Chỉ công bố nhạc **online** khi có giấy phép/ủy quyền phát hợp lệ và credit rõ ràng. **Tệp offline do người dùng tự tải/lưu vẫn được phát bình thường**, không bắt đăng ký creator hay duyệt bản quyền từng tệp cá nhân.
4. Người dùng có quyền quản lý hồ sơ và yêu cầu xử lý dữ liệu của mình theo phạm vi thiết kế; Admin chỉ dùng thông tin cần thiết, không truy xuất mật khẩu.
5. Chạy unit tests cho luật role/rights + integration RLS/Auth cho phân quyền + Android/Web UI targeted tests; **không gắn nhãn PASS trước khi thực hiện**.

## 5. Thứ tự ưu tiên so với điểm dừng hiện tại

- **Hiện tại:** MusicFlow đã có APK demo phục vụ báo cáo; các mốc A1–A4 **được ghi nhận, chưa được bắt đầu**. Không làm thay đổi yêu cầu sideload demo, không yêu cầu email thật hay phát hành Play.
- **Khi chủ dự án yêu cầu phát triển nhánh vai trò:** bắt đầu bằng **A1 (role schema + security acceptance)**, tiếp theo A2 Admin Web, A3 Creator Studio; tránh làm UI giả có nút quản trị nhưng backend chưa kiểm quyền.
- **Tài khoản QA:** trước khi cho thử Admin/Creator phải có backend đúng vai trò và kịch bản QA tách biệt; không tự cấp quyền cho tài khoản người dùng đang tồn tại.

**Nguồn liên kết:** §docs/Ke_Hoach_Du_An.md§, §docs/Pham_Vi_MusicFlow_1_0.md§, §docs/Kien_Truc_Mo_Rong_Va_Lo_Trinh_Full_Product.md§.
