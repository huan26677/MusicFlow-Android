# MusicFlow — Tự chuẩn bị demo báo cáo bằng phương án miễn phí

> **Chỉ dành cho dự án báo cáo APK cài thủ công**. Không cần Google Play, tên miền trả phí, thẻ thanh toán, email doanh nghiệp, hay máy Android thật. Có thể demo hoàn chỉnh phần offline **không cần Supabase**. Các quy trình đụng đến tài khoản Owner hoặc xác minh danh tính phải do chủ dự án tự xác nhận.

## A. Các công cụ miễn phí

| Công cụ | Mục đích | Cách dùng |
|---|---|---|
| Android Studio + Android SDK/Emulator | Cài, chạy và chụp màn hình APK | Cài Android Studio, tạo Android Virtual Device API 30, kéo APK vào emulator |
| Supabase project đang kết nối | Auth thử nghiệm, bảng role, Creator metadata, Storage private | Dùng project hiện có; ở mức demo có thể phù hợp Free tier, kiểm tra quota trong Dashboard |
| VS Code hoặc trình soạn thảo Markdown | Mở báo cáo và kế hoạch | Mở thư mục dự án `E:\Du_An\MusicFlow\docs` |
| GitHub repository | Lưu mã nguồn và tiến độ | Commit/push bằng Git trên máy chủ dự án |
| Python có sẵn trên máy dev | Chạy admin-web nội bộ | `python -m http.server 8765 --directory admin-web` |
| diagrams.net (draw.io) | Vẽ Use Case UML để đưa vào báo cáo | Dùng web app bản miễn phí hoặc bản desktop miễn phí; tham khảo Mermaid trong file actor/usecase |

## B. Việc ChatGPT không nên tự làm: chỉ định Owner

**CẬP NHẬT 2026-11-05 — ĐÃ HOÀN THÀNH:** chủ dự án đã tự kích hoạt Owner và SQL chỉ đọc xác nhận `owner_count = 1`. **KHÔNG chạy lại SQL bootstrap Owner bên dưới!** Phần B cũ chỉ dùng tham khảo cách đã thực hiện; từ giờ dùng **`docs/Owner_Dashboard_Bao_Cao_v1.md`** và mục B2 để chuẩn bị tài khoản demo.

**Vì sao:** các tài khoản hiện có chưa chứng minh ai thực sự là chủ sở hữu. Supabase demo đang có chế độ đăng ký không xác nhận email, nên **chỉ nhìn email không đủ**. Nếu bot tự gán Owner cho một user bất kỳ có thể cấp quyền quản trị sai người.

**Các bước bạn có thể làm MIỄN PHÍ:**

1. Mở **Supabase Dashboard → Project MusicFlow → Authentication → Users**.
2. Nếu chưa có tài khoản riêng của chính mình, đăng ký trực tiếp trong ứng dụng bằng email thử nghiệm **do bạn kiểm soát** và mật khẩu đủ mạnh; không dùng tài khoản QA của người khác. Không cung cấp mật khẩu, access token, service_role key qua chat.
3. Vào mục **Authentication → Users**, xác nhận user bạn vừa tạo bằng thông tin bạn tự biết (ví dụ thời điểm đăng ký, UUID ứng với user đó) và kiểm tra **User ID (UUID)**. Nếu tài khoản không rõ nguồn gốc, không gán Owner.
4. Mở **SQL Editor → New query** trong chính project. Dựa vào hướng dẫn trong `docs/Trien_Khai_Vai_Tro_Owner_Admin_Developer_v1.md` mục “Khởi tạo Owner ban đầu”, điền **UUID + email khớp với UUID** sau khi tự xác minh; chạy **một lần** bằng tài khoản Dashboard của bạn.
5. Kiểm tra kết quả bằng truy vấn chỉ đọc:
   
   ```sql
   select count(*) as owner_count
   from musicflow_private.user_roles
   where role = 'owner';
   ```
   
   Kết quả mong đợi **1**; nếu vẫn 0, **đừng** sửa trực tiếp user bất kỳ theo phỏng đoán. Lưu kết quả đã che email/UUID vào log QA.
6. Đăng xuất/đăng nhập lại trong APK debug hoặc web console nội bộ, rồi mới dùng Owner để cấp role Developer/PlatformAdmin/Artist/Author cho **tài khoản thử được bạn tạo**. Không đưa khóa quản trị vào APK.

### B2. Sau khi đã có Owner (không tốn phí)

1. Trên APK mới, vào **Tài khoản → Đăng nhập** bằng tài khoản Owner do bạn kiểm soát. Nếu đã đăng nhập từ trước, hãy đăng xuất rồi đăng nhập lại để làm mới vai trò.
2. Vào **Vai trò và quản trị MusicFlow → Owner • Tổng quan bản demo**. Nhấn **Làm mới tổng quan**: phải có **1 Owner**. Nếu không hiển thị, chưa xác nhận E2E; kiểm tra đúng email/UUID ở Supabase Dashboard, không cung cấp mật khẩu cho assistant.
3. Muốn tiện xem trên PC: trong thư mục gốc dự án chạy `python -m http.server 8765 --directory admin-web`, mở `http://127.0.0.1:8765` và đăng nhập Owner. Chỉ dùng localhost, không đưa web console lên Internet.
4. Tạo các tài khoản **Free QA riêng** (mỗi actor một email thử do bạn kiểm soát), sau đó Owner mở **Quản lý người dùng → Tải danh sách**; chọn đúng tài khoản QA, nhập lý do và cấp `platform_admin`/`moderator`/`artist`/`author`. **Không chọn chính tài khoản Owner** để tự cấp vai trò khác.
5. Khi cấp quyền PlatformAdmin, chỉ định phạm vi riêng `manage_users`, `review_creators` hoặc `moderate_catalog` nếu cần. Moderator **không cần** và cũng không được cấp phạm vi PlatformAdmin.
6. Đăng xuất/đăng nhập từng account QA để kiểm tra từng actor, nhập bài nháp và chứng cứ **tự tạo/có quyền sử dụng** để demo. Moderator chỉ duyệt metadata, không phát hành trực tuyến.
7. Nếu E2E không đạt, giữ trạng thái **chưa kiểm thử**, vẫn có thể trình bày luồng offline/role Dashboard theo mức MVP. Không cần trả phí hoặc mua gói Premium.

## C. Demo phần offline mà không cần Owner

1. Lấy APK demo mới nhất trong thư mục `dist/` trên máy dự án; tệp này bị Git ignore, **không có trong bản clone GitHub**.
2. Mở emulator API 30 trong Android Studio; kéo APK vào màn hình emulator để cài.
3. Chuẩn bị một file WAV/MP3 có quyền sử dụng, ví dụ **đoạn âm thanh tự thu hoặc tự tạo**; đưa vào bộ nhớ emulator qua Device Explorer hoặc kéo/thả.
4. Trình bày đường đi **Thư viện → chọn tệp → phát nhạc → tìm kiếm → playlist → EQ/Sleep Timer → hàng đợi**.
5. Tắt mạng emulator rồi phát lại tệp local để chứng minh nhạc offline không phụ thuộc Internet/role/license server.
6. Chụp ảnh màn hình emulator bằng Android Studio; không cần điện thoại thật.

## D. Demo Actor và quản trị

- Mở `docs/Actor_UseCase_MusicFlow_Bao_Cao.md` để lấy bảng tác nhân + UC ID; các luồng Premium, Google Login, cloud hai thiết bị, Owner thật phải ghi đúng trạng thái “kế hoạch/MVP/chưa E2E”.
- Admin web chỉ chạy **localhost** cho báo cáo: trong thư mục gốc dùng:
  
  ```powershell
  python -m http.server 8765 --directory admin-web
  ```
  
  Mở `http://127.0.0.1:8765`, nhập **Supabase URL + publishable (không phải secret) key** và email/password **tài khoản đã được cấp role**. Không đưa admin-web lên hosting công khai; không quay màn hình có password/token.
- Moderator chỉ xét duyệt hồ sơ tác phẩm và trả sửa; **không cấp role**, không khóa tài khoản, không xuất bản nhạc streaming. Nếu chưa có user Moderator hợp lệ, có thể trình bày flow trên sơ đồ mà không giả mạo E2E.
- Có thể quay video demo bằng phần mềm quay màn hình tích hợp Windows hoặc OBS Studio miễn phí nếu đã cài; không cần mua công cụ chỉnh video.

## E. Khi ChatGPT bị chặn bởi công cụ hoặc việc bên ngoài

- **Xác minh danh tính Owner:** chủ dự án đã tự thực hiện và SQL xác nhận **1 Owner**. Không cần bootstrap lần nữa; khi thử E2E, chỉ chính chủ đăng nhập bằng mật khẩu của mình.
- **Nhận giấy phép cho nhạc của bên thứ ba:** không được suy đoán. Dùng nhạc tự tạo hoặc nhạc CC0/CC-BY có nguồn và điều khoản phù hợp; lưu giấy phép/credit; bản có CC-BY phải ghi attribution.
- **Google OAuth thật, email SMTP, Play Store, thanh toán Premium:** **không bắt buộc cho báo cáo**; để lại backlog, không mua dịch vụ.
- **Kiểm thử phần cứng hoặc cloud 2 thiết bị thật:** không phải tiêu chí bàn giao hiện tại. Nếu sau này trường yêu cầu, có thể dùng hai emulator để thử, tránh tạo phụ phí.
- **Kiểm thử E2E Creator/Owner:** Owner đã có, nhưng vẫn cần tạo các tài khoản QA hợp lệ và đăng nhập bằng từng actor để thử thực tế; **chưa được làm E2E**.

## F. Điểm dừng phù hợp báo cáo

1. Có APK tải/cài thủ công trên emulator và chạy luồng offline không lỗi.
2. Có bảng actor/usecase, sơ đồ UML, mô tả phân quyền, mô hình dữ liệu và nhật ký kiểm thử đúng thực tế.
3. Có màn hình/luồng minh họa quản trị/Creator Studio v2 nhưng ghi rõ phần cloud/Owner E2E còn thiếu.
4. **Không** buộc phải mua tên miền, đăng ký Google Play, thanh toán thật hay sở hữu kho nhạc thương mại.

Mỗi lần tiếp tục dự án: đối chiếu file này với `docs/Ke_Hoach_Du_An.md`. Cần thay đổi phạm vi thương mại thì phải có yêu cầu mới, không tự chuyển từ **báo cáo** thành **sản phẩm public**.

