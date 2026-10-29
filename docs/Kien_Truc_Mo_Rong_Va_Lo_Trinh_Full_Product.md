# MusicFlow — Kiến trúc mở rộng và lộ trình Full Product

**Quyết định của chủ dự án ngày 2026-10-29:** Tiếp tục phát triển sản phẩm trước. **Hoãn sử dụng email/tài khoản thật và Google Login E2E đến giai đoạn cuối.** Giữ phần Auth đã viết nhưng không để Auth chặn các tính năng nghe nhạc offline. Không tự tạo email, Google credential, bản ghi quyền nhạc hoặc nâng Premium giả.

## 1. Kiến trúc mục tiêu — mở rộng mà không viết lại lõi

Tham khảo [Android app architecture](https://developer.android.com/topic/architecture), [Android modularization](https://developer.android.com/topic/modularization) và mẫu [Now in Android](https://github.com/android/nowinandroid).

**Trạng thái hiện tại:** Mã vẫn thuộc Gradle module `:app`, chia theo package `core/*` và `feature/*`. Các module mô tả dưới đây là **đích thiết kế**, chưa phải các module Gradle đã được tạo. Tách dần từng phần ổn định theo test, **không** đại tu toàn bộ mã trong một commit.

| Ranh giới dài hạn | Trách nhiệm | Quy tắc phụ thuộc |
|---|---|---|
| `:app` | Activity, navigation, composition root, dependency wiring | Kết nối các feature; không chứa luật phát nhạc |
| `:core:model` | Track identity, media source, playback rules, account tier | Không phụ thuộc Android UI/Supabase |
| `:core:playback` | MediaSessionService, MediaController adapter, queue, error recovery | **Một** nguồn sự thật về queue và playback |
| `:core:database` | Room và các migration dữ liệu local | Không gọi UI, không tự quyết định quyền Premium |
| `:core:network` | HTTP client, retry/timeouts, remote DTO, Supabase adapter | Không nhập service role/secret vào APK |
| `:core:identity` | Guest session, Auth provider adapter, entitlement server-only | Không bắt buộc đăng nhập để phát file local |
| `:feature:library/search/playlist/player` | StateFlow + ViewModel + Compose | Gọi use case/repository, không chỉnh MediaSession trực tiếp |
| `:feature:catalog/downloads/subscription` | Tính năng mở rộng theo nguồn/khả năng | Chỉ dùng giao diện trừu tượng; không sửa lại player lõi |

### Các nguyên tắc giữ ổn định

1. **Playback single source of truth:** mọi nguồn audio quy về `MediaItem` và một `MediaSessionService`; UI chỉ gửi action. Không tạo ExoPlayer mới riêng cho mỗi nguồn.
2. **Nguồn nhạc có thể bổ sung:** local MediaStore, SAF, licensed HTTPS, podcast/radio về sau dùng adapter và domain identity duy nhất. Phân biệt `sourceType + stableId`; không coi tên bài là identity.
3. **Phát nhạc không phụ thuộc Auth:** tài khoản là adapter tùy chọn; guest luôn có thư viện local. Cloud Sync chỉ hoạt động khi chủ động đăng nhập + opt-in.
4. **Bảo vệ dữ liệu:** local audio URI không upload cloud; tác phẩm online chỉ phát/tải khi có quyền; không tự cấp Premium tại client.
5. **State/async:** UI là immutable state + one-way events; background I/O dùng coroutine; thao tác bất đồng bộ cần cancellation/idempotency; queue có snapshot và migration.
6. **Hồi quy theo ranh giới:** mỗi quy tắc nghiệp vụ có JVM unit test; mỗi hành vi MediaSession quan trọng có Android integration test; E2E toàn bộ sản phẩm vào giai đoạn cuối.
7. **Không overengineering:** tách module Gradle sau khi interface và tests ổn định. Tránh tạo nhiều module rỗng chỉ để trông có vẻ mở rộng.

## 2. Lộ trình thực thi theo thứ tự

| Thứ tự | Giai đoạn | Điều kiện có thể xác nhận |
|---|---|---|
| 1 — đang làm | Playback Queue Actions: Play Next, Add to Queue; tách luật chèn queue | JVM test + Android build; sau đó instrumented persistence/ordering |
| 2 | Queue Manager UI: xem, xóa, di chuyển, clear, hỗ trợ trạng thái shuffle/repeat | Không đổi bài hiện tại ngoài ý muốn, lưu/restore đúng |
| 3 | Tách media item identity/factory, repository contracts; chuẩn hóa local/offline & online source | Không thay đổi kết quả playback/Room; tests chứng minh tính tương thích |
| 4 | UI hoàn thiện: empty/error/loading, accessibility, tìm kiếm lớn, layout và navigation | UI assertions + chức năng hoạt động không cần tài khoản |
| 5 | Licensed online: streaming smoke có quyền, link hết hạn, retry/caching hợp lệ, chính sách offline | Không đưa nhạc không có quyền; network tests và Media3 integration |
| 6 | Quyền Free/Premium, billing adapter, quotas và subscription UX | Premium do backend xác nhận; billing chỉ có thật khi cấu hình merchant |
| 7 | Full regression và đóng lỗi: SAF, Media3, Android API matrix, hiệu năng, crash, release | QA từ chính artifact + commit, log, DoD/các lỗi chặn |
| 8 — cuối cùng | Email thật + Google OAuth E2E + Cloud Sync hai thiết bị + identity linking | Credentials của chủ dự án; thực hiện sau khi lõi sản phẩm sẵn sàng |

Các nhánh tương lai (podcast, radio, Android Auto, widget, localization) chỉ khởi động khi core contracts đủ vững và có yêu cầu sản phẩm cụ thể; không mặc nhiên coi chúng đã được hoàn thành.

## 3. Queue Actions v1 — phạm vi

- Trong Library và Search (bao gồm nhóm Album/Nghệ sĩ/Thể loại), khi đang có bài trong queue, có tùy chọn **Phát tiếp** và **Thêm vào hàng đợi**.
- **Phát tiếp** chèn ngay sau vị trí playlist vật lý hiện tại khi tắt shuffle và không ở Repeat One; không hiển thị khi ngữ nghĩa 'tiếp theo' không chắc chắn.
- **Thêm vào hàng đợi** luôn chèn cuối, không đổi vị trí và không tự phát bài vừa thêm.
- Cả hai dùng `PlaybackController.enqueue` qua MediaController/MediaSession, không tự lưu queue song song. `PlaybackService` đã lắng nghe timeline để lưu snapshot.
- Không chèn khi chưa có hàng đợi hoặc controller chưa kết nối. Đây là quyết định v1, không phải lỗi bị bỏ qua.
- Giới hạn chưa nghiệm thu: thứ tự thật khi shuffle/repeat thay đổi giữa thao tác, thay đổi timeline từ controller khác, restart persistence khi có thêm bài, và giao diện Queue Manager. Các tình huống này cần Android integration tests ở giai đoạn kế tiếp.

## 4. Điều kiện nghiệm thu hệ thống cuối cùng

- Không dựa trên số test build để tuyên bố Full Product hoàn thành.
- Mỗi capability phải có source + unit/regression + log; các flow cần Android runtime có bằng chứng chạy thật.
- Không giả lập Google Login E2E, thanh toán hay bản quyền. Nếu thiếu quyền/credential thì ghi rõ **BLOCKED** và tiếp tục hạng mục độc lập.
- Chỉ phát hành khi toàn bộ lỗi chặn đã đóng và artifact release có thể truy vết nguồn, chữ ký và checksum.
