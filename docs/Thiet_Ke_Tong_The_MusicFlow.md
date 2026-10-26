# THIẾT KẾ TỔNG THỂ MUSICFLOW

> **Tài liệu yêu cầu sản phẩm (Product Vision & Requirements Baseline)**  
> **Đường dẫn dự kiến:** `docs/Thiet_Ke_Tong_The_MusicFlow.md`  
> **Phiên bản:** 1.0 — 2026-10-26 (Asia/Ho_Chi_Minh)  
> **Trạng thái:** Danh mục chức năng **ĐÃ ĐƯỢC CHỦ DỰ ÁN LỰA CHỌN**; các quyết định chi tiết ở mục 7 **CHƯA CHỐT**.  
> **Nguyên tắc:** Giữ mã MusicFlow Android đã làm, mở rộng dần; không mặc định viết lại dự án.

## 1. Tầm nhìn và ranh giới

**MusicFlow** là một sản phẩm nghe và quản lý âm thanh với **Android là nền tảng chính**. Sản phẩm kết hợp **nhạc trong điện thoại, nhạc trực tuyến từ máy chủ, nguồn nhạc từ nền tảng khác, tải nội dung hợp lệ để nghe offline, podcast và radio**; đồng thời hỗ trợ tài khoản, xã hội, khám phá nội dung, nội dung do nghệ sĩ/admin cung cấp và gói Premium.

**ĐÃ CHỐT:** toàn bộ chức năng được tích chọn trong mục 2 là **phạm vi mục tiêu cuối cùng**, không đồng nghĩa phải phát hành cùng một phiên bản hoặc đã có code.

**CHƯA CHỐT:** chức năng nào bắt buộc trong bản ra mắt đầu tiên; thứ tự ưu tiên, thời hạn, ngân sách, hạ tầng, nguồn bản quyền, nhà cung cấp, hình thức phân phối và chỉ tiêu chất lượng. Phân kỳ tại mục 6 chỉ là **đề xuất**, chưa phải quyết định của chủ dự án.

### 1.1. Vai trò người dùng

| Vai trò | Mục đích | Trạng thái |
|---|---|---|
| Khách (Guest) | Nghe nhạc và sử dụng phần chức năng không đòi đăng nhập | **Đã chọn**; quyền cụ thể chưa chốt |
| Người dùng có tài khoản | Lưu hồ sơ, đồng bộ, yêu thích, playlist, theo dõi | **Đã chọn**; quyền cụ thể chưa chốt |
| Người dùng Premium | Sử dụng các quyền lợi gói trả phí | **Đã chọn**; quyền lợi/giá chưa chốt |
| Nghệ sĩ / người đăng nội dung | Đăng tải và quản lý nội dung được cấp quyền | **Đã chọn**; xác minh/duyệt chưa chốt |
| Quản trị viên (Admin) | Quản lý nội dung đăng tải và số liệu | **Đã chọn**; giao diện quản trị chưa chốt |

## 2. Chức năng đã xác nhận

**Quy ước:** “**Đã chọn**” = chủ dự án xác nhận cần có trong sản phẩm; “**Đã có mã**” = phát hiện mã triển khai ở repository; “**QA PASS theo phạm vi**” = có log tự động tương ứng. Không được coi “Đã chọn” là đã phát triển xong.

### 2.1. Nguồn nội dung

| Mã | Chức năng | Quyết định sản phẩm | Triển khai hiện tại |
|---|---|---|---|
| SRC-01 | Phát nhạc có sẵn trên điện thoại | **Đã chọn** | Có MediaStore, Android picker, Media3; QA từng phần PASS |
| SRC-02 | Nghe nhạc trực tuyến từ server | **Đã chọn** | Chưa xác nhận đã triển khai |
| SRC-03 | Tích hợp nhạc từ nền tảng khác | **Đã chọn** | Chưa xác nhận; nền tảng, API, giấy phép TBD |
| SRC-04 | Tải nhạc về nghe offline | **Đã chọn** | Chưa xác nhận; chỉ tải nội dung được phép |
| SRC-05 | Podcast trực tuyến | **Đã chọn** | Chưa xác nhận |
| SRC-06 | Radio trực tuyến | **Đã chọn** | Chưa xác nhận |

### 2.2. Tài khoản và tương tác

| Mã | Chức năng | Quyết định sản phẩm | Triển khai hiện tại |
|---|---|---|---|
| ACC-01 | Sử dụng không cần đăng nhập (Guest) | **Đã chọn** | Luồng local hiện không đòi tài khoản; chính sách Guest TBD |
| ACC-02 | Đăng nhập Google / email | **Đã chọn** | Màn Account đang là placeholder |
| ACC-03 | Hồ sơ cá nhân | **Đã chọn** | Chưa xác nhận |
| ACC-04 | Đồng bộ dữ liệu giữa thiết bị | **Đã chọn** | Chưa xác nhận |
| SOC-01 | Theo dõi nghệ sĩ / người dùng | **Đã chọn** | Chưa xác nhận |

### 2.3. Bộ phát và điều khiển

| Mã | Chức năng | Quyết định sản phẩm | Triển khai hiện tại |
|---|---|---|---|
| PLAY-01 | Play/Pause, Next, Previous, Seek | **Đã chọn** | Đã có mã; QA PASS trong phạm vi local |
| PLAY-02 | Phát nền, màn hình khóa, thông báo | **Đã chọn** | Có MediaSessionService, test phát nền; chưa nghiệm thu đầy đủ lock screen/notification |
| PLAY-03 | Lời bài hát đồng bộ theo thời gian | **Đã chọn** | Chưa xác nhận |
| PLAY-04 | Equalizer / điều chỉnh âm thanh | **Đã chọn** | Chưa xác nhận |
| PLAY-05 | Hẹn giờ tắt nhạc | **Đã chọn** | Chưa xác nhận |
| PLAY-06 | Crossfade | **Đã chọn** | Chưa xác nhận |
| PLAY-07 | Gapless | **Đã chọn** | Chưa xác nhận |
| PLAY-08 | Điều chỉnh tốc độ phát | **Đã chọn** | Chưa xác nhận |

### 2.4. Khám phá, thư viện và cá nhân hóa

| Mã | Chức năng | Quyết định sản phẩm | Triển khai hiện tại |
|---|---|---|---|
| LIB-01 | Thư viện bài hát, album, nghệ sĩ, thể loại | **Đã chọn** | Library bài hát đã có; các phân loại chưa nghiệm thu |
| SRCH-01 | Tìm bài hát, nghệ sĩ, album | **Đã chọn** | Tìm bài hát/nghệ sĩ local đã có; album/online chưa nghiệm thu |
| LIST-01 | Tạo, sửa, xóa, sắp xếp playlist | **Đã chọn** | Đã có mã và QA database/playback PASS theo phạm vi |
| FAV-01 | Yêu thích bài hát / album | **Đã chọn** | Chưa xác nhận |
| DISC-01 | Đề xuất nội dung | **Đã chọn** | Chưa xác nhận |
| DISC-02 | Bảng xếp hạng, nhạc xu hướng | **Đã chọn** | Chưa xác nhận |
| HIST-01 | Lịch sử nghe, nghe tiếp | **Đã chọn** | Room history và queue persistence đã có; QA PASS theo phạm vi |
| SHARE-01 | Chia sẻ bài hát / playlist | **Đã chọn** | Chưa xác nhận |

### 2.5. Nội dung, thống kê và thương mại

| Mã | Chức năng | Quyết định sản phẩm | Triển khai hiện tại |
|---|---|---|---|
| CMS-01 | Admin / nghệ sĩ đăng tải bài hát | **Đã chọn** | Chưa xác nhận hệ thống upload/quản trị |
| DATA-01 | Thống kê lượt nghe và người dùng | **Đã chọn** | Chưa xác nhận hệ thống analytics |
| PAY-01 | Premium / gói trả phí | **Đã chọn** | Chưa xác nhận thanh toán/quyền lợi |

### 2.6. Giao diện và nền tảng

| Mã | Chức năng / định hướng | Quyết định |
|---|---|---|
| PLAT-01 | Android là nền tảng chính | **Đã chọn** |
| UI-01 | Giao diện tối hiện đại | **Đã chọn** |
| UI-02 | Có chế độ sáng và tối | **Đã chọn** |
| ENG-01 | Giữ mã đã có, bổ sung chức năng còn thiếu | **Đã chọn** |

**Không được tự hiểu là “bị loại bỏ”:** iOS, web player, dashboard quản trị trên web, quảng cáo/không quảng cáo, phát hành Google Play, hỗ trợ widget/Android Auto và các tính năng khác **chưa được lựa chọn hoặc bác bỏ rõ ràng**. Không thêm chúng vào phạm vi bắt buộc khi chưa có quyết định.

## 3. Hiện trạng mã nguồn — mốc kỹ thuật, không phải nghiệm thu sản phẩm đầy đủ

- Repository: `huan26677/MusicFlow-Android`, nhánh `main`.
- **HEAD khi lập thiết kế:** `57e363c95ef61be97b5d719ff11b4577d37f6f81` — `test: verify real MediaStore scanning and UI playback`.
- Công nghệ hiện có: **Kotlin, Jetpack Compose, Media3 ExoPlayer/MediaSessionService, Room**; `minSdk=29`, `targetSdk=36` theo `app/build.gradle.kts` tại HEAD trên.
- Các màn/luồng đã có mã: Home, Library, Search, Playlist, Mini Player, Now Playing. `AccountTab` hiện là **màn placeholder**, chưa phải login/profiles.
- Tài liệu thực thi và kiểm thử: `docs/Ke_Hoach_Du_An.md`, `qa-logs/*.log`, `scripts/*.ps1`.
- **QA mốc v3:** 36/36 Android tests PASS (runtime recovery race hardening), có log riêng.
- **QA mới nhất:** `qa-logs/mvp-e2e-v1.log` cho thấy **JVM PASS, 38/38 Android tests PASS**, đủ **7 marker** gồm Library/Search UI → phát nhạc/History và quét MediaStore thật → phát nhạc từ UI. Đó mới là **một phần** nghiệm thu E2E, **chưa nghiệm thu** file picker thực, đồng bộ, online, Premium, podcast/radio và toàn bộ sản phẩm.
- **Không xóa hoặc viết lại** dữ liệu Room, logic PlaybackService/queue, playlist và những test đã PASS nếu không có lý do kỹ thuật và bài test hồi quy.
- File `docs/Ke_Hoach_Du_An.md` chưa nhất thiết phản ánh các mốc QA mới; **không tự đánh dấu toàn bộ MVP hoàn thành**.

## 4. Thiết kế trải nghiệm và nhóm màn hình (đề xuất, chưa duyệt bố cục cuối)

1. **Home / Khám phá:** nghe tiếp, gần đây, đề xuất, xu hướng, nội dung nghệ sĩ theo dõi.
2. **Search:** tìm trên máy và online theo bài hát, album, nghệ sĩ; lọc theo loại nội dung; podcast/radio.
3. **Library / Thư viện:** nhạc máy, nhạc đã tải, bài/album yêu thích, playlist, lịch sử và thư viện cá nhân.
4. **Now Playing:** bộ phát chung cho nội dung hỗ trợ, hàng đợi, lời đồng bộ, tốc độ, shuffle/repeat, equalizer, sleep timer.
5. **Playlist / Collection:** CRUD, sắp xếp, chia sẻ, đồng bộ theo chính sách tài khoản.
6. **Artist / Album / Podcast / Radio detail:** thông tin, danh sách bài/tập/luồng, theo dõi, phát, tải hợp lệ.
7. **Account / Profile:** Guest → đăng nhập, hồ sơ, theo dõi, quản lý đồng bộ, Premium, cài đặt chủ đề.
8. **Artist upload / Admin:** khu vực đăng tải, quản lý nội dung và thống kê; **giao diện nằm trong app hay dashboard web vẫn TBD**.

**Đề xuất điều hướng:** Giữ Navigation hiện có (Home, Search, Library, Account, Now Playing, Playlists); chỉ bổ sung màn mới, tối ưu tab và trạng thái sau khi chốt sitemap. Chế độ tối là phong cách chủ đạo nhưng phải có chế độ sáng.

## 5. Kiến trúc mở rộng — định hướng, không khóa công nghệ khi chưa duyệt

```text
MusicFlow Android (giữ ứng dụng hiện có)
  ├─ Presentation: Compose, Navigation, ViewModel, UI state
  ├─ Playback: Media3 / MediaSessionService / Queue / Audio features
  ├─ Data sources
  │    ├─ Local: MediaStore, SAF picker, Room, downloaded cache
  │    ├─ First-party API: catalog, stream, search, profile, playlists
  │    ├─ Third-party adapters: chỉ qua API/SDK và quyền hợp lệ
  │    └─ Podcasts / radio: directory, RSS/feeds, stream URLs hợp lệ
  └─ Auth, sync, download manager, billing, telemetry (mở rộng)

Backend / dịch vụ được chọn sau
  ├─ Identity + guest-to-account migration
  ├─ Music catalog + content/artist ingestion + moderation
  ├─ Streaming delivery + authorization / entitlement
  ├─ Search + discovery + charts / recommendations
  ├─ User library + playlists + favorites + follows + sync
  ├─ Podcast / radio catalog and metadata
  ├─ Playback event analytics + reporting
  └─ Premium / payment verification + admin access control
```

**Nguyên tắc kỹ thuật đã xác định theo yêu cầu:** phát triển tiếp trên repository hiện tại, tách chức năng local đang ổn định khỏi các module online mới; giữ khả năng chạy offline ngay cả khi mất kết nối hoặc người dùng chưa đăng nhập (trong giới hạn quyền truy cập nội dung); mọi nội dung online/tải xuống phải có quyền sử dụng phù hợp.

**Chỉ là phương án, chưa chốt:** backend tự xây hay nền tảng managed; database máy chủ; CDN; chuẩn streaming; DRM/mã hóa offline; kiến trúc dịch vụ; công cụ analytics; hình thức mua Premium. Không tự chọn nhà cung cấp hoặc tích hợp nền tảng khác trước khi được chủ dự án phê duyệt.

## 6. Lộ trình đề xuất (các cổng chưa phải lịch phát hành)

| Giai đoạn | Đầu ra cần hoàn tất | Trạng thái |
|---|---|---|
| 0. Giữ baseline Android | Không hồi quy Player, Playlist, Queue, Recovery; cập nhật tài liệu thiết kế | Mã local nền tảng đã có; phạm vi này đã QA một phần |
| 1. Khép kín Android local E2E | Picker/MediaStore/Library/Search → Player → Playlist/History → restart; nghiệm thu UI + quyền | **ĐANG LÀM**, chưa nghiệm thu toàn bộ |
| 2. Quyết định sản phẩm & hạ tầng online | Chốt nguồn nội dung, quyền nhạc, Guest/Auth, backend, API, download, chi phí | **CHƯA CHỐT** |
| 3. Online catalog + streaming | Catalog/search online, phát qua server, lỗi mạng, podcast/radio, tích hợp nguồn hợp lệ | **CHƯA XÁC NHẬN TRIỂN KHAI** |
| 4. User + đồng bộ + social | Google/email, profile, favorites, playlist sync, theo dõi, chia sẻ | **CHƯA XÁC NHẬN TRIỂN KHAI** |
| 5. Nội dung & thương mại | Upload nghệ sĩ/admin, analytics, phân quyền, Premium và đối soát | **CHƯA XÁC NHẬN TRIỂN KHAI** |
| 6. Player nâng cao + chất lượng | Lyrics time-synced, EQ, sleep timer, crossfade, gapless, speed, background QA, light/dark | **CHƯA XÁC NHẬN TRIỂN KHAI TOÀN BỘ** |
| 7. Release | Compatibility, accessibility, performance, bảo mật, bản release ký, cài/nâng cấp và nghiệm thu | **CHƯA ĐẠT** |

**Cần chủ dự án quyết định:** Có thể phân lại thứ tự và nhóm thành các bản phát hành nhỏ. Chưa có định nghĩa “MVP” mới được duyệt sau khi mở rộng tầm nhìn sang online/Premium; **không tự xem giai đoạn local là toàn bộ sản phẩm**.

## 7. Những quyết định CÒN CHƯA XÁC ĐỊNH (TBD)

| Mã | Câu hỏi cần chốt | Vì sao quan trọng |
|---|---|---|
| TBD-01 | **Ưu tiên:** Bản phát hành đầu tiên phải có những tính năng nào? Toàn bộ hay từng đợt? | Xác định MVP/DoD và thứ tự code |
| TBD-02 | **Nguồn nhạc trực tuyến:** Ai sở hữu nội dung? Kho âm thanh tự cung cấp, nghệ sĩ cấp phép, đối tác hay hình thức khác? | Bản quyền, khả năng phát/tải/chia sẻ |
| TBD-03 | **Nguồn bên thứ ba:** tên nền tảng cụ thể, phạm vi liên kết, API hợp pháp, cách xác thực và chia sẻ dữ liệu? | Không được mặc định có thể trích xuất/phát lại nội dung từ mọi dịch vụ |
| TBD-04 | **Backend:** đã có server/domain/API/storage/CDN chưa? Ngôn ngữ/framework, hosting, giới hạn ngân sách? | Chốt kiến trúc và môi trường tích hợp |
| TBD-05 | **Tải offline:** nguồn nào được tải, chất lượng, lưu có mã hóa không, hạn lưu/quyền sau khi hết Premium? | Thiết kế download + entitlement |
| TBD-06 | **Guest vs đăng nhập:** guest làm được gì, khi đăng nhập có chuyển playlist/history, dữ liệu sync nào ưu tiên? | Tránh mất dữ liệu hoặc trùng bản ghi |
| TBD-07 | **Google/email:** đăng nhập Google, email OTP/mật khẩu, xác minh, phục hồi tài khoản, độ tuổi? | Chọn cơ chế danh tính |
| TBD-08 | **Premium:** quyền lợi, phân cấp, giá, thời hạn, dùng thử, thanh toán, hoàn tiền? | Thiết kế sản phẩm và billing |
| TBD-09 | **Quảng cáo:** có/không/quyền lợi của gói miễn phí? | Người dùng chỉ chọn Premium, chưa xác nhận mô hình quảng cáo |
| TBD-10 | **Admin/nghệ sĩ:** giao diện trong Android hay web? Ai duyệt nội dung, điều kiện đăng tải, xử lý vi phạm? | CMS, phân quyền, moderation |
| TBD-11 | **Khám phá/đề xuất:** danh sách biên tập, thống kê theo lượt nghe hay cá nhân hóa? | Thu thập dữ liệu, thuật toán và quyền riêng tư |
| TBD-12 | **Podcast/radio:** danh mục nguồn nào, live stream/episode, timeshift/download có cần không? | Mô hình dữ liệu và bộ phát |
| TBD-13 | **Lyrics:** ai cấp lời, định dạng LRC/timestamp, bản quyền hiển thị? | Đồng bộ chính xác và giấy phép |
| TBD-14 | **EQ/crossfade/gapless/speed:** hành vi khi phát local vs streaming, khi dùng Bluetooth/cast, giới hạn thiết bị? | Tiêu chí nghiệm thu audio |
| TBD-15 | **Thương hiệu/UI:** màu sắc, font, logo, bản thiết kế màn hình, ngôn ngữ hỗ trợ, accessibility? | Thiết kế giao diện cuối |
| TBD-16 | **Thiết bị/phát hành:** Android API min, phân phối APK hay Google Play, yêu cầu tablet/Android Auto/iOS/web? | Ma trận kiểm thử & release |
| TBD-17 | **Phi chức năng:** số người dùng dự kiến, catalogue, độ trễ, chất lượng âm thanh, cache, offline sync, uptime? | Phạm vi năng lực và dự toán |
| TBD-18 | **Pháp lý/bảo mật:** quyền phát/tải, dữ liệu cá nhân, retention, xử lý xóa tài khoản, báo cáo vi phạm, địa bàn phát hành? | Điều kiện vận hành dịch vụ |

**Chưa xác định ≠ Không cần.** Không chọn một phương án ngầm định để lấp những mục TBD này.

## 8. Tiêu chí hoàn thành và nghiệm thu theo nhóm

Một nhóm chức năng chỉ được đánh dấu **hoàn thành** khi: (1) yêu cầu và UX đã chốt; (2) code đã tích hợp; (3) có kiểm thử phù hợp + log PASS và hồi quy; (4) các lỗi chặn đã xử lý; (5) được chủ dự án chấp nhận.

- **Local & Player:** quét/chọn file, Library/Search → phát, playlist, lịch sử, queue/recovery, điều khiển nền, trường hợp mất quyền/file lỗi; test tự động trên phiên bản Android đã chốt.
- **Online & external:** phát đúng nội dung có quyền, xử lý mất mạng/buffering/seek, nguồn bên thứ ba có cơ chế tích hợp được phê duyệt, không vượt quyền nội dung.
- **Downloads:** phát được khi offline, xử lý file thiếu, quyền tải/hết hạn, dung lượng, hủy/xóa tải.
- **Identity & sync:** Guest và tài khoản, nhất quán playlist/favorites/history trên nhiều thiết bị, xử lý xung đột và mất mạng; bảo mật.
- **Social & discovery:** follow/unfollow, favorites/share, top charts và đề xuất theo quy tắc thống nhất, đo lường có kiểm chứng.
- **Content & Premium:** đăng tải, quản lý quyền/duyệt, analytics đúng nguồn sự kiện, thanh toán + kiểm tra quyền Premium + các tình huống hết hạn.
- **Advanced player & UI:** kiểm tra lyrics/time sync, EQ/timer/speed/crossfade/gapless, light/dark, accessibility và trạng thái lỗi.
- **Release:** build có thể phân phối, ký và nâng cấp, smoke test, bảo mật/riêng tư/bản quyền trong địa bàn mục tiêu, checklist chốt với chủ dự án.

**Không dùng 38/38 Android tests hoặc bất kỳ mốc PASS local nào làm bằng chứng hoàn thành tất cả các chức năng trên.**

## 9. Quy ước duy trì tài liệu và workflow

1. **File này là nguồn sự thật về phạm vi sản phẩm đã chọn.** Nếu chủ dự án đổi yêu cầu, cập nhật mục 2 và mục 7, ghi ngày/phiên bản và người xác nhận; không thay âm thầm.
2. `docs/Ke_Hoach_Du_An.md` tiếp tục là nguồn sự thật về **tiến độ kỹ thuật, QA, backlog và lịch sử**. Theo yêu cầu hiện hành: **chỉ cập nhật kế hoạch khi hoàn tất một chức năng/milestone lớn**; với mỗi vòng FAIL/PASS nhỏ, vẫn lưu log trong `qa-logs/` và không tự kết luận toàn bộ milestone PASS.
3. Mỗi phiên làm việc: đọc thiết kế này + kế hoạch + HEAD + log hiện hành; hướng dẫn **sửa trực tiếp file có sẵn**, tạo file mới khi thật cần thiết; chạy automated QA; chỉ đưa **3 lệnh Git** khi cần commit/push.
4. Không tự sửa/push repository nếu chưa được phép. File tải về này **chưa nằm trong GitHub** cho đến khi chủ dự án đặt vào `docs/` và commit.
5. Không xóa hoặc sửa lịch sử PASS trước đó. Phân biệt **đã chọn**, **đang làm**, **có source**, **QA PASS**, **chưa chốt** và **đã nghiệm thu**.

## 10. Nhật ký phiên bản

| Ngày | Phiên bản | Nội dung |
|---|---|---|
| 2026-10-26 | 1.0 | Chốt danh mục chức năng do chủ dự án tích chọn; bổ sung danh sách TBD, kiến trúc đề xuất, lộ trình không ràng buộc và hiện trạng GitHub `57e363c9`. |

**Tài liệu này xác nhận mục tiêu sản phẩm, nhưng không tự đưa ra các quyết định mà chủ dự án chưa cung cấp.**
