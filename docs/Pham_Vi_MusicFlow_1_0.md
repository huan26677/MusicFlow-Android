# MusicFlow 1.0 — Phạm vi sản phẩm đã thống nhất

**Ngày chốt:** 2026-10-29. **Trạng thái:** xác nhận phạm vi phát triển, KHÔNG đồng nghĩa đã nghiệm thu tất cả tính năng.

## Mục tiêu

Ứng dụng Android MusicFlow kết hợp **nghe offline, nghe online từ nguồn miễn phí có quyền sử dụng hợp pháp, thư viện cá nhân và đồng bộ đám mây**. Trước hết phục vụ nhu cầu cá nhân, sau mới mở rộng. Giao diện có bản sắc riêng, không sao chép ứng dụng đối thủ. Google Login và kiểm thử dùng email thật để gần cuối quá trình hoàn thiện sản phẩm.

## Các phần phải hoàn thiện trong 1.0

| Thành phần | Phạm vi | Nghiệm thu cần có |
|---|---|---|
| Offline | MediaStore, SAF, audio playback nền | Mất quyền/file, resume, notification, restart |
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

**Guest:** được sử dụng tất cả chức năng local cốt lõi (nghe file, queue, playlist, favorites, lyrics, EQ, Sleep Timer), và nội dung online nếu nguồn cho phép. Không có cloud sync.

**Free:** như Guest và có tài khoản, cloud sync thủ công với quota do backend kiểm soát; lịch sử cloud Free hiện thiết kế giới hạn 30 ngày.

**Premium:** mở rộng quota, tự động đồng bộ, lịch sử dài hơn và các tùy chỉnh chuyên sâu **chỉ khi thực tế đã được xây dựng và xác minh**. Không tự cấp Premium tại client.

Không bật thanh toán thật hoặc quảng cáo bắt buộc trong giai đoạn dùng cá nhân. Nội dung âm nhạc phải có quyền sử dụng hợp pháp cho từng hình thức phát, tải, phân phối và thương mại; Premium không thay thế quyền bản quyền.

## Nguyên tắc bản quyền và kỹ thuật

- Từng track online cần nguồn, định danh ổn định, credit, giấy phép, quyền streaming/download/commercial được xác minh. Ưu tiên CC0, CC BY hoặc giấy phép trực tiếp, tùy điều khoản cụ thể.
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
