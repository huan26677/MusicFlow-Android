# MusicFlow 1.0 — Phạm vi sản phẩm đã thống nhất

> **CẬP NHẬT ƯU TIÊN 2026-11-01:** Chủ dự án đã xác định **điểm dừng tạm thời là bản APK demo có thể tải/cài trực tiếp để phục vụ dự án báo cáo**, không phải release 1.0 thương mại. **Không yêu cầu email thật, Google Login thật, thiết bị vật lý, đồng bộ cloud qua hai thiết bị thật, Google Play hoặc phát hành công khai.** Các chức năng chưa có kiểm thử E2E phải nêu rõ giới hạn; không trình bày như đã nghiệm thu. Khi hoàn thành APK demo, kiểm thử emulator và tài liệu báo cáo thì tạm dừng. Xem `docs/Muc_Tieu_Dung_Tam_APK_Bao_Cao.md` và `docs/Ke_Hoach_Du_An.md`. Các mốc sản phẩm/1.0 cũ bên dưới được giữ làm tài liệu kiến trúc và định hướng dài hạn, không phải checklist bắt buộc cho điểm dừng demo.

**Ngày chốt:** 2026-10-29. **Trạng thái:** xác nhận phạm vi phát triển, KHÔNG đồng nghĩa đã nghiệm thu tất cả tính năng.

## Mục tiêu

Ứng dụng Android MusicFlow kết hợp **nghe offline, nghe online từ nguồn miễn phí có quyền sử dụng hợp pháp, thư viện cá nhân và đồng bộ đám mây**. Trước hết phục vụ nhu cầu cá nhân, sau mới mở rộng. Giao diện có bản sắc riêng, không sao chép ứng dụng đối thủ. Google Login và kiểm thử dùng email thật để gần cuối quá trình hoàn thiện sản phẩm.

## Các phần phải hoàn thiện trong 1.0

| Thành phần | Phạm vi | Nghiệm thu cần có |
|---|---|---|
| Offline | Nhạc người dùng đã có hoặc tự tải về máy, MediaStore, SAF, audio playback nền; không yêu cầu metadata giấy phép online | Mất quyền/file, resume, notification, restart, playlist/history/queue |
| Library | Bài hát, album, nghệ sĩ, thể loại, favorites | Không mất hoặc trùng dữ liệu cá nhân |
| Player | Play/pause, seek, next/previous, shuffle/repeat, tốc độ | MediaSession nhất quán với UI |
| Queue Manager | Phát tiếp, thêm cuối, xem, di chuyển, xóa, dọn | Không ngắt bài đang phát; lưu và khôi phục |
| Playlist | Tạo, đổi tên, xóa, thêm bài, sắp xếp, phát đúng thứ tự | Room persistence và migrations |
| Tìm kiếm | Bài hát, album, nghệ sĩ, thể loại; hỗ trợ tiếng Việt bỏ dấu | Kết quả đúng, dữ liệu lớn |
| Audio nâng cao | EQ cơ bản, Sleep Timer, LRC lyrics; Crossfade nếu ổn định | Không gây lỗi playback hoặc audio session |
| Online hợp pháp | Danh mục, metadata/credit/giấy phép, phát HTTPS | Nội dung thật được quyền streaming, kiểm thử E2E |
| Cloud | Playlist/favorites/history opt-in, metadata-only | RLS chủ sở hữu, merge an toàn, kiểm thử 2 thiết bị ở cuối |
| Account | Guest/Free/Premium | Guest phát offline độc lập; Premium được backend xác nhận |
| UI | Thiết kế MusicFlow riêng, dark/light, trạng thái empty/error/loading, accessibility | Kiểm tra UI trên các kích thước màn hình |
| Chất lượng | Android compatibility, QA, release | Không còn lỗi P0, nghiệm thu trên artifact cuối |

## Quyền lợi tài khoản — chính sách mục tiêu

**Guest:** được sử dụng tất cả chức năng local cốt lõi (nghe tệp đã có/tự tải về điện thoại, queue, playlist, favorites, lyrics, EQ, Sleep Timer), và nội dung online nếu nguồn cho phép. Nhạc offline không cần Internet, tài khoản hoặc xác minh giấy phép bởi MusicFlow. Không có cloud sync.

**Free:** như Guest và có tài khoản, cloud sync thủ công với quota do backend kiểm soát; lịch sử cloud Free hiện thiết kế giới hạn 30 ngày.

**Premium:** mở rộng quota, tự động đồng bộ, lịch sử dài hơn và các tùy chỉnh chuyên sâu **chỉ khi thực tế đã được xây dựng và xác minh**. Không tự cấp Premium tại client.

Không bật thanh toán thật hoặc quảng cáo bắt buộc trong giai đoạn dùng cá nhân. **MusicFlow chỉ xác minh quyền streaming đối với nội dung online do MusicFlow cung cấp.** File offline người dùng tự lưu/tải xuống được phát như dữ liệu cục bộ; MusicFlow không xét duyệt bản quyền từng tệp riêng tư. Người dùng vẫn chịu trách nhiệm về nguồn tệp và việc sử dụng theo pháp luật; Premium không thay thế quyền bản quyền.

## Nguyên tắc bản quyền và kỹ thuật

- Từng track online cần nguồn, định danh ổn định, credit, giấy phép, quyền streaming/download/commercial được xác minh. Ưu tiên CC0, CC BY hoặc giấy phép trực tiếp, tùy điều khoản cụ thể.
- **Phân cách online/offline là URI thực, không phải tên hoặc ID:** tệp local qua `content://`, `file://`, hay đường dẫn tuyệt đối trên thiết bị được phát không cần `rights_confirmed`, ngay cả khi còn giữ ID bắt đầu bằng `online:` từ nguồn cũ. URL `https://` lưu cũ phải kiểm tra quyền trước khi phát lại.
- Một MediaSessionService là nguồn chân lý về phát nhạc; source local/SAF/online quy về MediaItem; UI Compose dùng StateFlow và gửi commands tới ViewModel/controller.
- Dữ liệu `content://` cá nhân không được gửi cloud; chỉ metadata tối thiểu và link lại trên từng thiết bị. RLS và entitlement kiểm soát ở backend.
- Phát triển tính năng theo mốc build/JVM/instrumentation; Full QA và Auth E2E ở giai đoạn cuối. Không xem build-only là nghiệm thu.

## Thứ tự thực hiện

1. Queue Manager (đã có source, JVM + Android API 30 tests; còn kiểm thử ma trận Android và restart tổng thể).
2. Chuẩn hóa media identity, factories và source contracts cho MediaStore, SAF và licensed online.
3. Hoàn thiện UI/UX MusicFlow, accessibility và các tính năng đang có source.
4. Xác minh nguồn licensed streaming thật; URL hết hạn, mạng yếu và attribution.
5. Hoàn thiện Guest/Free/Premium + cloud opt-in, không mở billing thật.
6. Android full QA, tương thích API, bảo mật, phát hành.
7. **Email thật, Google Login E2E, đồng bộ hai thiết bị gần cuối**, sau đó nghiệm thu release 1.0.

**Sau 1.0** (không phải điều kiện chặn): AI DJ, podcast, radio, mạng xã hội, Android Auto, widget nâng cao, iOS/web, mô hình doanh thu sâu hơn. Xem thêm `docs/Kien_Truc_Mo_Rong_Va_Lo_Trinh_Full_Product.md` và `docs/Ke_Hoach_Du_An.md`.

**Backlog mở rộng theo yêu cầu 2026-11-05 — CHƯA TRIỂN KHAI:** thêm **Owner** (chủ sở hữu MusicFlow, toàn quyền quản trị nghiệp vụ, cấp/thu hồi cộng tác), **Cộng tác quản trị viên nền tảng / PlatformAdmin** (quản lý người dùng, duyệt nội dung theo quyền Owner cấp), **Developer** (tài khoản kỹ thuật với **lối tắt vào Công cụ nhà phát triển** trên bản debug/QA, không tự có quyền Admin hoặc bỏ qua xác thực), **Moderator**, cùng **tài khoản Tác giả/Nghệ sĩ** (hồ sơ, yêu cầu xác minh, Creator Studio, bản nháp, upload có giấy phép, credit và quy trình duyệt). Vai trò tách biệt với **gói Guest/Free/Premium**; Premium không tự thành Owner/PlatformAdmin/Developer/Creator. Thứ tự A1 role/RLS → A2 Owner/PlatformAdmin Web → A2.5 Developer Quick Access → A3 Creator Studio → A4 QA/mở rộng. Đây là **giai đoạn sau điểm dừng APK demo/báo cáo**, không làm phát sinh nghĩa vụ triển khai trong 1.0 hiện tại; chi tiết tại `docs/Lo_Trinh_Quan_Tri_Vien_Tac_Gia_Nghe_Si.md`. Tệp offline của người dùng vẫn phát như trước, không bị duyệt bản quyền theo cơ chế catalog online.
