# Kế hoạch, tiến độ và tiêu chí hoàn thành — MusicFlow Android

> **TÀI LIỆU BÀN GIAO CHÍNH THỨC GIỮA CÁC CUỘC TRÒ CHUYỆN**  
> Đường dẫn trong repository: `docs/Ke_Hoach_Du_An.md`  
> Mỗi lần bắt đầu công việc, **đọc file này trước**, sau đó đối chiếu `main` / commit mới nhất / source code / log trên GitHub. Không tiếp tục dựa trên trạng thái nhớ từ chat nếu đã có bằng chứng mới hơn.

## QUYẾT ĐỊNH ƯU TIÊN 2026-11-01 — ĐIỂM DỪNG: APK DEMO / DỰ ÁN BÁO CÁO

**Mục tiêu mới được chủ dự án xác nhận:** MusicFlow là **dự án báo cáo**. Hoàn thiện ứng dụng để **tải tệp APK và cài đặt trực tiếp phục vụ demo**, hoàn tất bàn giao/báo cáo rồi **tạm dừng**. **Không phát hành thật, không Google Play, không dùng email thật, không kiểm thử điện thoại thật, không bắt buộc đăng nhập Google thật hoặc đồng bộ cloud qua hai thiết bị thật.**

**Các kế hoạch “Release Candidate / Packaging / Release Readiness”, production signing, Play App Signing và E2E tài khoản thật ở các mốc trước KHÔNG còn là yêu cầu bắt buộc tại điểm dừng demo.** Ưu tiên chức năng offline/local, APK cài được trên emulator, kiểm thử kỹ thuật phù hợp, và tài liệu báo cáo. Các tính năng online chỉ được phát nội dung có quyền; cloud/Auth/Premium chưa kiểm thử E2E phải công bố rõ trạng thái, không tuyên bố đã hoàn thiện toàn diện. **Đây là quyết định thay thế các mục tiêu “phát hành thật” cũ, không xóa lịch sử QA.**

**Tiêu chí điểm dừng và việc làm tiếp:** xem `docs/Muc_Tieu_Dung_Tam_APK_Bao_Cao.md`. **Trạng thái 2026-11-01: HOÀN THÀNH BẢN DEMO/BÁO CÁO — TẠM DỪNG.** APK demo thực tế có tại `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-Android.apk`; đã cài và kiểm thử chính APK này trên emulator. **Chưa phải sản phẩm thương mại hay nghiệm thu tính năng cloud/online E2E**. Không tự mở mốc phát triển mới.

## 0. Tóm tắt nhanh cho cuộc trò chuyện mới

| Mục | Trạng thái |
|---|---|
| **ĐÍCH DỰ ÁN BÁO CÁO (quyết định mới nhất)** | **APK demo tải/cài trực tiếp, nghiệm thu trên emulator + báo cáo rồi TẠM DỪNG; KHÔNG phát hành thật, email thật, điện thoại thật, Google Play hoặc cloud hai thiết bị thật.** `docs/Muc_Tieu_Dung_Tam_APK_Bao_Cao.md` |
| **TRẠNG THÁI DEMO** | **HOÀN THÀNH BẢN DEMO/BÁO CÁO — TẠM DỪNG**. `dist/MusicFlow-1.0-Demo-Android.apk` 30.230.097 byte; SHA-256 `ACE9AD765F75C7939C513210E2BBC1555F95E6504636303B423FB70598C2E750`; release-mode ký demo, WAV/MP3/FLAC smoke trên **chính APK bàn giao** API 30 PASS. `docs/Bao_Cao_Ban_Giao_MusicFlow_Demo_APK.md` |
| **Kiểm tra bổ sung 2026-11-05 — Tài khoản/Auth/Admin** | **Đã chạy trực tiếp trên APK demo/emulator**: tab Account, Signup/Signin với dữ liệu trống/thiếu và thông báo lỗi **PASS phạm vi UI validation**; Supabase email provider bật, Google provider tắt, email confirmation bắt buộc; backend hiện **0 Auth user, 0 profile, 0 entitlement**. **Đăng ký/xác nhận email/đăng nhập thành công E2E CHƯA THỬ vì không dùng email thật**. **Chưa có giao diện Admin app/web; chỉ có công cụ CLI quản trị nội dung**. 17/17 JVM liên quan tài khoản/OAuth PASS. `docs/Kiem_Tra_Tai_Khoan_Dang_Nhap_Admin_v1.md`; `qa-logs/account-auth-admin-audit-v1.log`. Demo vẫn ở trạng thái tạm dừng |
| **Kiểm tra đăng ký đầy đủ 2026-11-05 — PHÁT HIỆN LỖI SERVER** | Trên APK demo API 30, điền **đầy đủ email thử nghiệm/mật khẩu** và bấm `Đăng ký Free` → **HTTP 429 / Supabase log: email rate limit exceeded**; backend vẫn 0 người dùng. Log cũ từng có **HTTP 400 email bị từ chối**. Android đã được sửa để dịch từng mã lỗi Auth chính xác, **7/7 test mới + full 112/112 JVM PASS**; APK demo mới `dist/MusicFlow-1.0-Demo-AuthFeedback-Android.apk` đã build/ký/cài nâng cấp trên QA emulator PASS. **ĐĂNG KÝ THÀNH CÔNG VẪN CHƯA ĐẠT**, cần custom SMTP hoặc Auth demo tách biệt; không tự tắt email confirmation. `docs/Kiem_Tra_Dang_Ky_Thuc_Te_HTTP429_v1.md` |
| **CẬP NHẬT 2026-11-05 — ĐĂNG KÝ/ĐĂNG NHẬP SUPABASE KHÔNG XÁC NHẬN EMAIL** | **PASS E2E Android emulator API 30**: máy chủ hiện `mailer_autoconfirm=true`, email provider bật, signup bật. APK mới mặc định **Supabase online** (local Demo chỉ opt-in), báo đúng trạng thái Confirm email, kiểm tra mật khẩu. **Signup Free từ UI → Auth user được tự xác nhận + account_profiles tạo trên Supabase → đăng xuất → đăng nhập → khôi phục phiên sau force-stop PASS**. Một account QA được tạo trên Supabase bằng email miền thử nghiệm, không dùng email người dùng. **116/116 JVM PASS**. APK mới `dist/MusicFlow-1.0-Demo-SupabaseSignup-Android.apk` SHA-256 `74D5D93294B8B6AAB7283B12835FD53AD0E1AC6BCA179A13F7F17D2ACD795026`. Xem `docs/Kiem_Thu_Supabase_Auth_Khong_Xac_Nhan_Email_v1.md` và `qa-logs/supabase-signup-no-email-confirm-v1.log`. **Không thay đổi Auth config server bằng assistant**; tắt Confirm email là quyết định hiện tại của chủ dự án cho demo, không chứng minh quyền sở hữu email |
| **MỚI NHẤT 2026-11-05 — GIAO DIỆN AUTH UX THÂN THIỆN** | **PASS** trên QA emulator API 30: tab `Đăng nhập / Tạo tài khoản`, nhập lại mật khẩu, 2 nút hiện/ẩn, báo lỗi từng ô, Supabase online mặc định, nút Google vẫn hiển thị **disabled**, Guest điều hướng về Home. **121 JVM PASS / 1 Android UI test PASS**; trên giao diện đã **signup → signout → signin → cold restart/restore session PASS** trên Supabase (mailer_autoconfirm=true). Chỉ dọn 1 user QA tạo trong lượt này, không dọn tài khoản QA khác. APK mới `dist/MusicFlow-1.0-Demo-AuthUX-Android.apk` SHA-256 `C1B081CDD7AFE5BCB2C3FFC2E1B231CFFFD1A188503B18D0C06A0F1229A0AA04`. `docs/Account_Auth_UX_Supabase_v1.md` |
| **MỚI NHẤT 2026-11-05 — UX FOUNDATION v3** | **ĐÃ TRIỂN KHAI + 3/3 Android UI targeted PASS**: thanh điều hướng thuần Việt & biểu tượng Material, Home ưu tiên Tiếp tục nghe/Bắt đầu nghe, hướng dẫn người mới chỉ hiện một lần, nút Thư viện/Now Playing là `Chọn tệp nhạc`. APK demo mới `dist/MusicFlow-1.0-Demo-UXFoundationV3-Android.apk` (30.754.493 byte), SHA-256 `467B5D4D4664BD35DE5A8FF07E56D5682D316C23A6A5F05AFB76C3E8DB5F3AAF`, release-mode ký demo, cài và mở trên QA emulator PASS. **Không chạy full suite**, giữ nguyên Auth/Cloud/Playback. `docs/MusicFlow_UX_Foundation_v3.md` và `qa-logs/musicflow-ux-foundation-v3.log` |
| **MỚI NHẤT 2026-11-05 — PLAYER & LIBRARY UX v3** | **ĐÃ TRIỂN KHAI + 3/3 Android UI targeted PASS**: Player có nút mở/thu gọn phần nâng cao (Speed/Sleep/Crossfade/EQ), giữ transport/lyrics; Thư viện sắp xếp Tên bài/Nghệ sĩ/Album/Mới thêm bằng `MediaStore DATE_ADDED` và queue phát theo đúng thứ tự đã chọn. APK demo `dist/MusicFlow-1.0-Demo-PlayerLibraryUXV3-Android.apk` 30.770.877 byte, SHA-256 `1DDAB2611F941701277B1E50812ED209A92102B6D33CE9E9492B851E0026BDCD`; build, ký demo, cài và cold launch PASS. **Không chạy full-suite, không đổi Supabase/Auth/online/payments**, giữ nguyên playlist. `docs/MusicFlow_Player_Library_UX_v3.md`; `qa-logs/musicflow-player-library-ux-v3.log` |
| **MỚI NHẤT 2026-11-05 — PLAYLIST UX v3** | **ĐÃ TRIỂN KHAI + 4/4 JVM và 5/5 Android targeted PASS**: tìm playlist không dấu/chữ hoa, trạng thái không có kết quả/xóa tìm kiếm; bố cục tiêu đề tại font lớn và tên tối đa hai dòng; mỗi bài có menu gọn Lên/Xuống/Xóa, xóa vẫn qua hộp thoại xác nhận. Test tạo/đổi tên/xóa và phát theo queue cũ PASS. APK `dist/MusicFlow-1.0-Demo-PlaylistUXV3-Android.apk` 30.787.261 byte, SHA-256 `C40F59CE54747DC54637147C375E98263F4D774C92E6CB5568E44BA5D230A3AD`; ký QA, cài/cold launch/Home→Playlist PASS. **Không full regression, không đổi Supabase**, AVD QA riêng đã dọn. `docs/MusicFlow_Playlist_UX_v3.md` và `qa-logs/musicflow-playlist-ux-v3.log` |
| **MỚI NHẤT 2026-11-05 — SEARCH UX v3** | **ĐÃ TRIỂN KHAI + 7/7 Android targeted PASS**: tìm kiếm ghi rõ phạm vi nhạc được quét trên thiết bị, hỗ trợ không dấu, 4 tab có số kết quả, có nút chuyển sang Thư viện chọn file khi không có quyền/nhạc, sửa trùng nút Quét lại, nút xóa query và màn hình chữ lớn. Test tìm nhóm Album/Nghệ sĩ/Thể loại qua MediaStore fixture PASS. Chính APK `dist/MusicFlow-1.0-Demo-SearchUXV3-Android.apk` (30.787.261 byte), SHA-256 `95F79772A410F33EEF4A4E90A99931BF9548B3C51D4038451C4C1C6FFB6C1BC5`; build/ký/cài/cold start và nhấn Search→Library PASS. **Không full regression/không đổi Supabase, cloud, Media3**; `docs/MusicFlow_Search_UX_v3.md` và `qa-logs/musicflow-search-ux-v3.log` |
| **KẾ HOẠCH MỞ RỘNG 2026-11-05 — ADMIN / TÁC GIẢ / NGHỆ SĨ** | **ĐÃ THÊM VÀO BACKLOG — CHƯA TRIỂN KHAI**. MusicFlow hiện chỉ có tài khoản nghe nhạc Guest/Free/Premium và CLI nội bộ quản trị bản quyền; **không có dashboard Admin hay tài khoản/Studio Artist/Author**. Thống nhất định hướng **A1 backend vai trò/RLS → A2 dashboard quản trị người dùng/nội dung → A3 hồ sơ & cổng tác giả/nghệ sĩ → A4 kiểm duyệt/mở rộng**; Premium là gói quyền lợi, không phải vai trò. Xem `docs/Lo_Trinh_Quan_Tri_Vien_Tac_Gia_Nghe_Si.md`. Không cấp role thật, không tạo APK hay thay đổi Supabase trong lượt lập kế hoạch |
| HEAD Git nền đã đối chiếu | `b33b6ec` (`main = origin/main`) — Playlist UX v3 đã commit/push và Git sạch đầu lượt; **Search UX v3 + docs/log đang ở local workspace, chưa commit/push**. Các APK trong `dist/` đều bị Git ignore |
| Phạm vi MusicFlow 1.0 | **Đã chốt**: file offline người dùng tự tải/tự lưu **nghe bình thường, không xét duyệt giấy phép**; chỉ online từ MusicFlow cần quyền streaming; có library + cloud; dùng cá nhân trước; xem `docs/Pham_Vi_MusicFlow_1_0.md` |
| Quyết định mới 2026-10-29 | Phát triển Full Product trước là kế hoạch ban đầu; **đã được thay thế ở điểm dừng hiện tại bằng mục tiêu APK demo/báo cáo, không làm email/Google/cloud hai thiết bị thật** |
| Quyết định mới 2026-11-01 | **KHÔNG YÊU CẦU kiểm thử thiết bị thật**; các mốc tiếp theo dựa trên emulator/Gradle và artifact. Mục chỉ có thể đo trên phần cứng thật được ghi **chưa kiểm chứng**, không còn là điều kiện bắt buộc chặn phát triển |
| Session Recovery v1 | Full QA PASS |
| Android API 30 (full suite mới nhất) | **115 Android testcase: 112 PASS / 0 FAIL / 3 SKIP**, 105 JVM PASS ở Full Product Regression Gate v2 Part 1; 3 SKIP là test SAF theo pha riêng |
| Marker API 30 (mốc cũ) | 40/40 PASS tại Session Recovery; không dùng thay kết quả full suite v2 |
| Playback Speed v1/v2 | **Runtime + Touch API 30 PASS:** 5 chip 0.75–2x qua Compose semantics **và chạm Android thật** → MediaSession, pause/resume/next giữ tốc độ/queue; font scale 150% có overflow và swipe thành công; **JVM 105 + 12 Android PASS**, chưa điện thoại thật |
| Sleep Timer v1 | **Runtime Android API 30 PASS:** đặt/query/thay/hủy, debug-only expiry 1–2 giây kiểm tra MediaSession tạm dừng/giữ queue; 3/3 Sleep + 8/8 Playback regression = **11/11 PASS**; chưa test multi-API/real device |
| Lyrics v1 | Parser LRC, nhập/lưu URI SAF, đồng bộ dòng lời theo MediaSession seek; **6 JVM parser + 2 Android Lyrics + 8 Playback regression PASS** trên API 30; còn MainActivity picker-to-screen E2E |
| Lyrics import | Android OpenDocument |
| Lưu liên kết LRC theo audio URI | Đã triển khai |
| Build Lyrics v1 + Sleep Timer v1 | `:app:assembleDebug` và `:app:assembleDebugAndroidTest` PASS ngày 2026-10-29; chỉ xác nhận biên dịch |
| Full QA | **Full Android instrumentation API 30 v2 đã PASS**; release-mode smoke emulator API 29/30/33/36 đã PASS; **miễn yêu cầu máy thật theo quyết định 2026-11-01**; signing và Auth/Cloud/online có quyền thật vẫn còn mở |
| Equalizer v1 | **Runtime QA API 30 PASS:** AudioEffect gắn session Media3 thật, bật/tắt, chỉnh band, lưu cấu hình, controller reconnect; 2/2 EQ + 8/8 Playback/Queue = **10/10 Android PASS**. Chưa QA UI/tiến trình restart/thiết bị thật |
| Crossfade/Gapless v1/v2 | **Runtime + UI/Interruption API 30 PASS:** 0/1/2/3s, UI chip selected/SessionCommand, MIX_STARTED và HANDOFF_DONE 1/2/3s, **4 MIX_CANCELLED stage=MIXING** do pause/seek/sửa queue/tắt, phục hồi volume 1/queue; **105 JVM + 17 Android PASS**. Chưa đo gapless âm thanh thực tế |
| Library nâng cao v1 | **Runtime API 30 PASS**: READ_EXTERNAL_STORAGE, 3 WAV MediaStore/2 albumId thật, LibraryRoute scanner + play, Album/Artist playback + Genre fallback UI; **105 JVM + 14/14 Android PASS**; chưa test artist/genre tags thật hoặc máy thật |
| Search nâng cao v1 | **Runtime API 30 PASS**: SearchRoute thật quét 3 WAV MediaStore (tiêu đề có dấu), tìm không dấu, Album play, Artist/Genre fallback, phát và queue append/next/persist qua MediaSession; **105 JVM + 14/14 Android PASS**. Chưa test Artist/Genre tags MP3/FLAC thật |
| Online Catalog Foundation v1 | API HTTPS cấu hình được, repository metadata, phân trang và màn khám phá online; build ứng dụng + AndroidTest PASS; chưa có dịch vụ hay QA thực tế |
| Supabase Backend v1 | Dự án Supabase đã kết nối; bảng public.music_tracks RLS; Data API read-only; client Android đọc danh mục và có luồng phát HTTPS hợp lệ; build-only |
| Licensed Content Upload v1 | Hai bucket private/public, music_uploads RLS, RPC publish atomic, admin stage/review/publish; Python unit 8/8 PASS và Android build PASS; chưa upload thật |
| Account & Subscription Foundation v1 | Guest/Free/Premium model; Supabase Auth email/password, profile RLS, entitlement read-only, Android Keystore; build và unit PASS, chưa QA email thật |
| Cloud Playlist & Favorites Sync v1 | Manual opt-in, metadata-only, RPC merge/RLS theo owner, safe local relink; build/JVM PASS, chưa E2E đa thiết bị |
| Cloud Sync Hardening v2 | Trigger quota, tombstone xóa cloud, UI relink thủ công, RPC merge, có source; chưa E2E đa thiết bị |
| History Sync v1 | Supabase RLS, RPC metadata-only, UI đồng bộ thủ công; APK + AndroidTest build và JVM unit PASS (4/4 riêng History), chưa E2E |
| Google Login Foundation v1 | Android OAuth PKCE + nonce + callback + session; build Android + JVM PASS (7/7 Google unit tests); provider Supabase Google TẮT, chưa E2E |
| Account & Google Security Hardening v2 | Loại trừ phiên Auth/PKCE khỏi Android backup và device transfer; kiểm tra callback bổ sung; 4/4 Python, 12/12 Google JVM, Android app + AndroidTest build PASS; chưa E2E |
| Playback Queue Actions v1 | **Runtime Android API 30 PASS**: UI LibraryScreen **Phát tiếp** / SearchScreen **Thêm vào hàng đợi** trên MediaSession/WAV thật, snapshot đúng và bài NEXT phát thật; từ chối shuffle/repeat-one/empty; **105 JVM + 13/13 Android PASS**. Chưa kiểm thử MediaStore/MainActivity route toàn phần |
| Queue Manager UI v1 | Xem/xóa/di chuyển/dọn hàng đợi, giữ bài đang phát; 16/16 JVM và 3/3 Android instrumented trên API 30 PASS |
| Core Media Source Contract v1 | Một điểm dựng MediaItem từ MediaStore/SAF/online, bảo toàn legacy; JVM 11/11, Android API 30 9/9 PASS; log `qa-logs/media-source-contract-v1.log` |
| MusicFlow UI/UX Foundation v1 | Trạng thái UI thống nhất trên Home/Library/Search/Online/Queue, theme xanh ngọc–xanh đêm và TalkBack labels; BUILD + JVM, Android API 30 **13/13 PASS** |
| MusicFlow UI/UX Polish v2 | Responsive Now Playing/Playlist/Account, cỡ chữ lớn, theme dễ truy cập; BUILD + JVM, Android API 30 **19/19 PASS**; log `qa-logs/musicflow-ui-polish-v2.log` |
| Licensed Online Rights Metadata & Attribution v1 | Supabase đã áp dụng migration, RLS fail-closed; chỉ chặn URL online thiếu quyền, **không chặn offline tự tải**, kể cả ID `online:` cũ; 10/10 JVM + 21/21 Android API 30 PASS |
| Licensed Content Review & Publish v2 | Supabase RPC v2 chỉ service_role, v1 bị thu hồi EXECUTE; admin Python ghi rights + audit, 17/17 Python PASS, rollback SQL trên DB PASS, Gradle/JVM PASS; chưa upload nhạc thật |
| Offline End-to-End Acceptance v1 | MediaStore → Room Playlist → MediaSession → History → Queue Snapshot/Restore, 9/9 marker; build/JVM PASS, 22/22 Android API 30 PASS; `qa-logs/offline-e2e-acceptance-v1.log` |
| Android Compatibility Matrix v2 | API **29/30/33/36**, mỗi API 18 offline + 4 restart + 4 SAF; **104/104 instrumented PASS**, 9 E2E markers/API, đổi PID thật khi force-stop; `docs/Android_Compatibility_Matrix_v2.md` |
| Full Product Regression Gate v1 - Part 1 | BUILD/JVM **99/99 PASS**; toàn bộ Android suite API 30 **89 total / 83 PASS / 3 FAIL / 3 SKIP** - **GATE FAIL**; chờ sửa MVP Previous và real SAF picker; `docs/Full_Product_Regression_Gate_v1_Part_1.md` |
| Full Product Regression Gate v1 - Part 2A | Đã xác minh thao tác test trên Now Playing phải cuộn tới nút trước khi chạm; **BUILD/JVM PASS, 8/8 Android API 30 PASS**, `qa-logs/full-product-regression-v1-part2a-playback.log`; full gate vẫn chưa nghiệm thu |
| Full Product Regression Gate v1 - Part 2B | UIAutomator chọn file thật + `SELECT` trong DocumentsUI; **2/2 Android API 30 PASS**, persisted READ, MediaSession Playback, 2-file queue + Next/History, Gradle/JVM PASS; `docs/Full_Product_Regression_Gate_v1_Part_2B.md` |
| Full Product Regression Gate v1 - Part 3 | Toàn bộ Android/JVM API 30 **PASS**: JVM **99/99**, Android **86 PASS / 0 FAIL / 3 SKIP** từ 89 testcase; đã sửa lỗi timing/scroll Playlist Reorder và chạy lại suite; `qa-logs/full-product-regression-v1-part3.log` |
| Sleep Timer Runtime Acceptance v1 | Gradle Build/JVM PASS; **3/3 Sleep Timer + 8/8 Playback/Queue regression = 11/11 Android API 30 PASS**, 8/8 marker; release không nhận debug-duration override, không đổi nhạc offline/online rights |
| Equalizer Runtime Acceptance v1 | Gradle Build/JVM PASS; **2/2 Equalizer + 8/8 Playback/Queue regression = 10/10 Android API 30 PASS**, 6/6 marker; `docs/Equalizer_Runtime_Acceptance_v1.md` |
| Lyrics Runtime Acceptance v1 | **105/105 JVM PASS**, Android API 30 **2/2 Lyrics + 8/8 Playback/Queue regression = 10/10 PASS**; thực tế SAF CreateDocument + persisted READ + parser + highlight khi MediaSession seek; `docs/Lyrics_Runtime_Acceptance_v1.md` |
| Lyrics Import UI End-to-End v2 | **105/105 JVM PASS; 11/11 Android API 30 PASS** (1 MainActivity→DocumentsUI OpenDocument→Player E2E + 2 Lyrics v1 + 8 Playback/Queue), 7 E2E markers; `docs/Lyrics_Import_UI_E2E_v2.md` |
| Playback Speed Runtime Acceptance v1 | **105 JVM PASS, 10/10 Android API 30 PASS** (2 Speed + 8 Playback/Queue); 7 marker; chạm chip bằng accessibility action, còn cần physical touch QA; `docs/Playback_Speed_Runtime_Acceptance_v1.md` |
| Playback Speed Touch Interaction QA v2 | **JVM 105/105, Android API 30 12/12 PASS** (3 Speed + 8 Playback/Queue + 1 high-font physical touch), đủ 5 nút tốc độ chạm Android thật, swipe ngang ở font scale 150%; `docs/Playback_Speed_Touch_Interaction_QA_v2.md` |
| Crossfade/Gapless Runtime Acceptance v1 | **JVM 105/105 + Android API 30 12/12 PASS** (4 Crossfade + 8 Playback/Queue); thực tế WAV offline, 3 MIX_STARTED + 3 HANDOFF_DONE cho 1/2/3 giây; **chưa đo gapless audio**, `docs/Crossfade_Gapless_Runtime_Acceptance_v1.md` |
| Crossfade UI & Interruption QA v2 | **JVM 105/105 + Android API 30 17/17 PASS** (8 Crossfade runtime + 1 UI + 8 Playback/Queue), 4 hủy thật ở giai đoạn MIXING; `docs/Crossfade_UI_Interruption_QA_v2.md` |
| Playback Queue Actions Runtime Acceptance v1 | **105/105 JVM + 13/13 Android API 30 PASS** (2 Library/Search Queue Actions + 11 Playback/Queue regression), snapshot và next-track thật; `docs/Playback_Queue_Actions_Runtime_Acceptance_v1.md` |
| Advanced Library Runtime Acceptance v1 | **105/105 JVM + 14/14 Android API 30 PASS** (2 Advanced Library + 12 Playback/Queue/Offline regression); 3 WAV MediaStore thật, hai album thực, UI nhóm và playback; `docs/Advanced_Library_Runtime_Acceptance_v1.md` |
| Advanced Search Runtime Acceptance v1 | **105/105 JVM + 14/14 Android API 30 PASS** (2 SearchRoute/MediaStore + 12 Playback/Queue/Offline regression), **13/13 marker**, tìm tiếng Việt không dấu, Album và queue thực; `docs/Advanced_Search_Runtime_Acceptance_v1.md` |
| Full Product Regression Gate v2 — Part 1 | **PASS**: build/JVM **105/105**; toàn bộ Android instrumentation API 30 **112 PASS / 0 FAIL / 3 SKIP (115 total)**. Lượt đầu 2 FAIL tại CreateDocument Save, sửa QA helper và chạy lại 2/2 rồi full suite PASS. Xem `docs/Full_Product_Regression_Gate_v2_Part_1.md` |
| Release Candidate QA / Packaging v1 — Part 1 | **PASS cho APK release-mode ký debug chỉ dùng nội bộ**: `assembleRelease` + lintVital, kiểm tra chữ ký v3, SHA-256, cài mới/chạy MainActivity và nâng cấp debug→QA release giữ internal marker trên emulator API 30; **chưa thể phát hành production vì thiếu release signing chính thức**. Xem `docs/Release_Candidate_QA_Packaging_v1_Part_1.md` |
| Release Candidate QA / Packaging v1 — Part 2 | **PASS trên release-mode APK QA API 30**: 2 WAV MediaStore QA thật, Library → MediaSession phát, Pause/Resume/Next, Background Playback và MediaStyle notification; cleanup QA PASS. `scripts/run-release-playback-smoke-v1.ps1`, `qa-logs/release-playback-smoke-v1.log`, `docs/Release_Candidate_QA_Packaging_v1_Part_2.md` |
| Release Candidate QA / Packaging v1 — Part 3 | **PASS trên release-mode APK QA API 29/33/36** (API 30 đã PASS từ Part 2): 2 WAV/Library/MediaSession Play-Pause-Resume-Next, nền + MediaStyle và cleanup; API 36 dùng parser QA mới hiểu `PLAYING(3)`. `bundleRelease` BUILD SUCCESSFUL, **AAB chưa ký**, chưa phát hành. `docs/Release_Candidate_QA_Packaging_v1_Part_3.md` |
| Release Candidate QA / Packaging v1 — Part 4 | **PASS phạm vi QA emulator/API30**: MP3 và FLAC tổng hợp 40 giây, MediaStore đọc metadata và thời lượng thật, Library → MediaSession Play/Pause/Resume/Next, nền + MediaStyle, cleanup. **105/105 JVM + release APK/AAB build/audit PASS**, nhưng AAB chưa ký, app ID/version/production signing/R8 chưa chốt. `docs/Release_Candidate_QA_Packaging_v1_Part_4.md` |
| Kiến trúc mở rộng | Đã có kế hoạch module boundaries, source adapters, feature roadmap tại `docs/Kien_Truc_Mo_Rong_Va_Lo_Trinh_Full_Product.md`; chưa tách Gradle modules |
| **Backlog mới sau bản demo — Admin / Artist / Author** | **CHƯA LÀM:** A1 roles + RLS/anti-self-elevation; A2 Admin Web quản lý user và duyệt tác phẩm/quyền phát; A3 Creator Studio, hồ sơ Artist/Author, upload nháp và credit; A4 audit/khiếu nại/QA nhiều vai trò. **Không trộn Premium với vai trò quản trị**, không đổi điều kiện phát offline; chi tiết `docs/Lo_Trinh_Quan_Tri_Vien_Tac_Gia_Nghe_Si.md`. Không phải tiêu chí chặn APK báo cáo hiện tại |
| Bước tiếp theo | **TẠM DỪNG** — không còn mốc bắt buộc trong phạm vi demo/báo cáo. Nếu có yêu cầu mới thì ưu tiên xem `docs/Bao_Cao_Ban_Giao_MusicFlow_Demo_APK.md` và `docs/Huong_Dan_Cai_Dat_MusicFlow_Demo_APK.md`, tiếp tục đúng phạm vi người dùng yêu cầu. Không tự làm production signing, Google Play, tài khoản thật hay test máy thật |
| Sản phẩm tổng thể | CHƯA NGHIỆM THU |

**LƯU Ý QUAN TRỌNG:** Các số `31/31`, `55/55` ở mốc cũ không phải số liệu full suite v2. Hiện có bằng chứng **105 JVM + 112 Android PASS / 0 FAIL / 3 SKIP** trên API 30 nhưng **không phải tỷ lệ hoàn thành 100% MusicFlow 1.0**. Log/test phải gắn với đúng bản code; chỉ có commit sau khi người dùng push mới xác nhận các sửa đổi đã lên GitHub.

## 1. Thông tin dự án và nguồn sự thật

- **Dự án:** MusicFlow Android — ứng dụng nghe nhạc cục bộ trên Android.
- **GitHub:** `huan26677/MusicFlow-Android`; nhánh `main`.
- **Windows workspace:** `E:\Du_An\MusicFlow`.
- **Công nghệ:** Kotlin, Jetpack Compose, Media3 ExoPlayer / MediaSessionService, Room.
- **Cấu hình source đã xem:** `minSdk=29`, `targetSdk=36` trong `app/build.gradle.kts`.
- **Môi trường đã có log PASS:** Android Emulator `Pixel_5`, Android 11 (API 30), thường có serial `emulator-5554`.
- **Script kiểm thử:** `scripts/*.ps1`; **log:** `qa-logs/*.log`.
- **Tài liệu bổ sung:** [`docs/Tong_Quan_Tien_Do_Va_Tieu_Chi_Hoan_Thanh_MusicFlow.md`](Tong_Quan_Tien_Do_Va_Tieu_Chi_Hoan_Thanh_MusicFlow.md) — phạm vi MVP và các cổng nghiệm thu chi tiết.

**Thứ tự ưu tiên bằng chứng:** (1) commit + source và tests thực tế trên GitHub, (2) log QA gắn với vòng thay đổi, (3) tài liệu tiến độ, (4) lời mô tả từ chat trước. Nếu mâu thuẫn: nêu chênh lệch và cập nhật tài liệu, **không tự phán PASS**.

**Cách gắn commit với log:** Một script thường chạy **trước khi** kết quả được commit, nên trường `COMMIT` ghi bên trong log có thể chỉ về commit trước vòng sửa. Cần kiểm tra commit đã đưa chính file log và code tương ứng lên `main`.

## 2. Mục tiêu sản phẩm và phạm vi đề xuất

**MVP đề xuất — còn cần chủ dự án chốt:** người dùng có thể tìm/chọn nhạc cục bộ, xem thư viện, tìm kiếm, tạo playlist, nghe nhạc qua giao diện hoặc MediaSession, xem lịch sử và tiếp tục hàng đợi sau khởi động lại theo quy tắc an toàn. Ứng dụng phải xử lý file thiếu/quyền bị thu hồi mà không crash, không tự phát trái ý muốn hoặc lặp lỗi vô hạn.

Nhóm chức năng cần nghiệm thu của MVP:

1. Quét MediaStore và chọn nhiều file bằng Android file picker; quyền truy cập theo phiên bản Android.
2. Library và Search: hiển thị, tìm kiếm, chọn và bắt đầu phát đúng bài.
3. Player/Mini Player/Now Playing: Play/Pause, Next/Previous, Seek, Shuffle, Repeat, metadata/trạng thái/lỗi.
4. Playlist: tạo, đổi tên, xóa; thêm/xóa/sắp xếp bài; phát theo thứ tự và lưu bền vững.
5. Recently Played: ghi nhận, hiển thị, phát lại, xóa; Room migration không làm mất dữ liệu.
6. Background audio + MediaSession: hoạt động đúng khi app xuống nền, có điều khiển hệ thống/audio focus theo yêu cầu đã chốt.
7. Playback queue: lưu và khôi phục bài, thứ tự, vị trí; không tự phát sau restart; xử lý file thiếu và lỗi runtime.

**Chưa mặc định thuộc MVP:** streaming, tài khoản, cloud sync, lyrics, EQ, widget, Android Auto, monetization và phát hành Google Play. Chỉ bổ sung khi chủ dự án xác nhận. **Chưa có tài liệu yêu cầu sản phẩm đã được chủ dự án ký duyệt** trong những file đã rà soát.

## 3. Những gì đã có trong source — không đồng nghĩa đã nghiệm thu toàn bộ UI

| Nhóm | Source liên quan | Trạng thái |
|---|---|---|
| Home / Recent | `feature/home/*`, `core/history/*` | **Đã triển khai mã** và có test lớp dữ liệu; luồng UI end-to-end cần nghiệm thu |
| Library / MediaStore / chọn file | `feature/library/*`, `core/media/*`, `MainActivity.kt` | **Đã triển khai mã**; quyền/URI theo nhiều API cần kiểm thử bổ sung |
| Search | `feature/search/*` | **Đã triển khai mã**; có unit tests, cần nghiệm thu thao tác UI → phát |
| Player / Mini Player / Now Playing | `feature/player/*`, `core/playback/PlaybackController.kt` | **Đã triển khai mã**; cần nghiệm thu toàn bộ tương tác UI |
| Playlist | `feature/playlist/*`, `core/playlist/*` | **Đã triển khai và có nhiều vòng QA PASS**; cần kiểm thử hành trình người dùng hoàn chỉnh |
| Playback Service | `core/playback/PlaybackService.kt` | **Đã triển khai, có QA Service Integration PASS** |
| Queue persistence / restart | `PlaybackQueueStore.kt`, `PlaybackProcessRestartTest.kt` | **PASS theo phạm vi test hiện có** |
| Queue hardening | `PlaybackQueueRecovery.kt`, `PlaybackQueueHardeningTest.kt` | **PASS theo phạm vi test hiện có** |
| Runtime Auto-Skip | `PlaybackRuntimeAutoSkip.kt`, `PlaybackRuntimeAutoSkipTest.kt` | **PASS v1 + Integration v2**, còn rủi ro race cần kiểm chứng |

Phân biệt ba thuật ngữ: **Đã triển khai** = có code; **QA PASS** = có bài kiểm thử + log cho hành vi cụ thể; **MVP hoàn thành** = mọi tiêu chí MVP được chốt và nghiệm thu đạt.

## 4. Các mốc đã làm và bằng chứng kiểm thử

| Mốc | Android tests ở lần chạy của mốc | Kết quả | Commit / Log |
|---|---:|---|---|
| Playback Error Handling v1 | — | Theo lịch sử QA trước đó; **chưa xác minh log riêng trong đợt rà soát** | Kiểm tra lịch sử khi cần |
| Playback Skip Recovery v1 | 9 | **PASS** | `qa-logs/playback-skip-v1.log` |
| Playlist Reorder v1 | 11 | **PASS** | `qa-logs/playlist-reorder-v1.log` |
| Playlist Playback Order v1 | 12 | **PASS** | `qa-logs/playlist-playback-order-v1.log` |
| Playback Service Integration v1 | 13 | **PASS** | `da8277b` · `qa-logs/playback-service-integration-v1.log` |
| Playback Queue Persistence v1 | 16 | **PASS** | `1f13264` · `qa-logs/playback-queue-persistence-v1.log` |
| Playback Process Restart Recovery v1 | 19 | **PASS** | `07ee7d3` · `qa-logs/playback-process-restart-v1.log` |
| Playback Queue Hardening v1 | 23 | **PASS** | `a3b9ec9` · `qa-logs/playback-queue-hardening-v1.log` |
| Playback Runtime Error Auto-Skip v1 | 28 | **PASS** | `5e4b1c9` · `qa-logs/playback-runtime-auto-skip-v1.log` |
| Playback Runtime Recovery Integration v2 | 31 | **PASS** | `1aec374` · `qa-logs/playback-runtime-recovery-v2.log` |
| Playback Runtime Recovery Race Hardening v3 | 36 | **PASS** | `qa-logs/playback-runtime-recovery-v3.log` — SHA sau khi push cần đối chiếu |
| Favorites + Album + Theme Batch v1 | 44 | **PASS tự động** — JVM PASS, 14/14 marker; SAF thực sau restart còn cần nghiệm thu thủ công | `511f93ea` · `qa-logs/favorites-batch-v1.log` |
| Background Playback & System Controls v1 | 45 | **PASS tự động** — JVM PASS, 18/18 marker | `663e3ba0` · `qa-logs/background-controls-v1.log` |
| Background/System Controls v2 | 47 | **PASS tự động** — JVM PASS, 20/20 marker; notification actions và transient audio focus | `1ab915f5` · `qa-logs/background-controls-v2.log` |
| SAF Permission Persistence v1 | 50 regression + 3 test theo pha | **PASS** — quyền SAF tồn tại sau force-stop, Favorites phát lại thành công | `77c546e9` · `qa-logs/saf-permission-v1.log` |
| Android 13 SAF Compatibility v1 | 50 regression + 3 test theo pha | **PASS** — API 33, SAF persisted read và Favorites playback sau force-stop | `7e2e7b59` · `qa-logs/android13-saf-compat-v1.log` |
| Android 16 Compatibility v1 | 50 regression + 3 test theo pha | **PASS** — API 36, SAF persistence, Favorites playback và background lifecycle | `69bf34f9` · `qa-logs/android16-saf-compat-v1.log` |
| Android 10 Compatibility v1 | 50 regression + 3 test theo pha | **PASS** — API 29, SAF persistence và Favorites playback | `74b9026c` · `qa-logs/android10-saf-compat-v1.log` |
| Real Picker SAF v1 | 51 PASS, 3 test theo pha SKIPPED | **PASS — manual-assisted, API 30** | `19f418ae` · `qa-logs/real-picker-saf-v1.log` |
| SAF Multi-file v1 | 52 PASS, 3 test theo pha SKIPPED | **PASS — Full Regression, API 30** | `94c03895` · `qa-logs/real-picker-saf-v1.log` |
| MVP Playlist Navigation v1 | 55 PASS, 3 test theo pha SKIPPED | **Full QA PASS — API 30** | `18d5893a` · `qa-logs/mvp-playlist-navigation-v1.log` |
| MVP Playlist → History → Replay v1 | 1 bài isolated PASS | **Fast QA PASS — API 30** | `69d5a559` · `qa-logs/mvp-playlist-history-v1.log` |
| Session Recovery UI Acceptance v1 | 55 bài chính PASS | **Full QA PASS — API 30** | `d430cfc7` · `qa-logs/playback-process-restart-v1.log` |
| Full Product Regression Gate v2 — Part 1 | 115 Android total (112 PASS + 3 SKIP), 105 JVM | **PASS — API 30, 0 FAIL; 3 SAF phase skips theo thiết kế** | Commit `818be07` · `qa-logs/full-product-regression-v2-part1.log` · `docs/Full_Product_Regression_Gate_v2_Part_1.md` |
| Release Candidate QA / Packaging v1 — Part 1 | Build/lint release + ký QA, fresh/upgrade smoke API 30 | **PASS nội bộ; chưa có release ký production** | Commit `00f2d41` · `qa-logs/release-candidate-packaging-v1.log` · `docs/Release_Candidate_QA_Packaging_v1_Part_1.md` |
| Release Candidate QA / Packaging v1 — Part 2 | Release-mode QA APK + 2 WAV MediaStore thực | **PASS Android API 30** — phát nhạc, Pause/Resume/Next, background, notification, cleanup | Commit `b572414` · `qa-logs/release-playback-smoke-v1.log` · `docs/Release_Candidate_QA_Packaging_v1_Part_2.md` |
| Release Candidate QA / Packaging v1 — Part 3 | Release-mode APK QA 3 API mới; AAB unsigned | **PASS API 29/33/36** — đủ 2 WAV, MediaSession, background, MediaStyle và cleanup; AAB BUILD SUCCESSFUL | Commit `1b8491d` · `qa-logs/release-playback-smoke-api29-v1.log` / `api33-v1.log` / `api36-v1.log` · `docs/Release_Candidate_QA_Packaging_v1_Part_3.md` |
| Release Candidate QA / Packaging v1 — Part 4 | Release-mode API 30: MP3/FLAC + metadata; release artifact/JVM audit | **PASS internal QA: 2 codec / 105 JVM / APK+AAB audit; PRODUCTION_RELEASE_READY=FALSE** | HEAD nền `1b8491d` · `qa-logs/release-codec-smoke-api30-v1.log`, `qa-logs/release-readiness-v1.log` · `docs/Release_Candidate_QA_Packaging_v1_Part_4.md` (chưa commit) |

**Không cộng** các con số 9 + 11 + ... + 31: đây là số lượng test của **các lần chạy tăng dần** với nhiều bài trùng nhau.

### 4.1. Phạm vi PASS đã được chứng minh

- **Playlist:** reorder dữ liệu và phát đúng thứ tự theo các test đã viết.
- **Service:** tích hợp MediaController/MediaSession/ExoPlayer với kiểm thử Android thực tế.
- **Persistence:** lưu/khôi phục danh sách, chỉ số, vị trí, shuffle/repeat; bỏ qua JSON hỏng.
- **Restart v1:** `force-stop` và khởi chạy lại với PID thay đổi, khôi phục queue ở trạng thái Pause, phát tiếp được từ vị trí lưu; **không đồng nhất** với hệ thống thu hồi tiến trình vì thiếu RAM.
- **Hardening v1:** loại file `file://` đã bị xóa, mô phỏng quyền `content://` bị từ chối, xóa snapshot hỏng, không tự phát khi khôi phục.
- **Auto-Skip v1:** các trường hợp thiếu file liên tiếp và hết hàng đợi; test tiêm lỗi xác nhận trực tiếp hai nhánh xử lý.
- **Integration v2:** log thật cho thấy `PlaybackService` nhận 7 lỗi và chuyển tiếp cả 7; test Pause/thay queue đang chờ phục hồi đạt PASS.

**Giới hạn:** API 30 là môi trường có log QA đã đọc; quyền URI của document provider thực tế và Android tự thu hồi process chưa được khẳng định PASS; marker của test tiêm lỗi không phải marker production Auto-Skip. Không suy rộng kết quả sang mọi thiết bị / mọi nguồn nhạc.

## 5. Công việc tiếp theo: backlog theo thứ tự ưu tiên

**Chế độ phát triển hiện hành (2026-10-29 — quyết định mới):** Chủ dự án ưu tiên hoàn thiện Full Product, thiết kế có khả năng mở rộng, **không sử dụng email thật và không triển khai Google Login E2E cho tới giai đoạn cuối**. Auth nền tảng vẫn giữ nguyên nhưng không nằm trên critical path. Các P0 QA/Release bên dưới giữ nguyên để nghiệm thu cuối, không được hiểu PASS nếu chỉ build. Xem `docs/Kien_Truc_Mo_Rong_Va_Lo_Trinh_Full_Product.md` để biết thứ tự và kiến trúc. **Google provider được xác nhận TẮT ở lần kiểm tra gần nhất; chưa có nhạc được cấp phép; chưa test tài khoản thật hoặc đồng bộ đa thiết bị E2E**. Không yêu cầu credentials cho các bước độc lập. Billing, signed URL, podcast/radio vẫn là backlog.

| Ưu tiên | Bước / việc cần làm | Trạng thái | Điều kiện kết thúc bước |
|---|---|---|---|
| **P0 — đã hoàn thành** | **Playback Runtime Recovery Race Hardening v3** | **PASS — 36/36 Android tests** | Xem `qa-logs/playback-runtime-recovery-v3.log`; yêu cầu 5 marker v3 + regression v2 |
| **Phát triển — vừa xong source** | **Playback Queue Actions v1** | **BUILD + JVM PASS, chưa QA runtime** | `qa-logs/playback-queue-actions-v1.log`, 8/8 JVM queue test; cần E2E queue/restore |
| **Phát triển — đã test Android** | **Queue Manager UI v1** | **16 JVM + 3 Android instrumented PASS (API 30)** | `qa-logs/queue-manager-v1.log`; chưa kiểm thử đa API hoặc khôi phục sau force-stop |
| **Phát triển — đã test Android** | **Core Media Source Contract v1** | **11/11 JVM + 9/9 Android API 30 PASS** | `qa-logs/media-source-contract-v1.log`; chưa có streaming thật/cấp phép thật và signed URL resolver |
| **Phát triển — đã test Android** | **MusicFlow UI/UX Foundation v1** | **Build/JVM + 13/13 Android API 30 PASS** | `qa-logs/musicflow-ui-foundation-v1.log`, `docs/MusicFlow_UI_UX_Foundation_v1.md`; chưa nghiệm thu bố cục đa cỡ chữ/thiết bị |
| **Phát triển — đã test Android** | **MusicFlow UI/UX Polish v2** | **Build/JVM + 19/19 Android API 30 PASS** | `qa-logs/musicflow-ui-polish-v2.log`, `docs/MusicFlow_UI_UX_Polish_v2.md`; còn QA đa API và TalkBack thiết bị thật |
| **Phát triển — đã test Android + Supabase** | **Licensed Online Rights Metadata & Attribution v1** | **Supabase migration PASS, 10 JVM + 21 Android PASS API 30** | Chỉ online streaming cần quyền; offline tự tải không cần kiểm tra; `docs/Licensed_Online_Rights_Attribution_v1.md`, `qa-logs/licensed-online-rights-v1.log`; chưa có bài online thật |
| **Phát triển — đã kiểm thử backend** | **Licensed Content Review & Publish v2** | **Supabase migration + 17 Python unit + SQL rollback dry-run + Android build/JVM PASS** | `docs/Licensed_Content_Review_Publish_v2.md`, `qa-logs/licensed-publish-v2.log`; không có nhạc thật được upload |
| **Phát triển — kế tiếp khi đủ quyền nội dung** | **Licensed Streaming Smoke v1** | CHƯA THỰC HIỆN | Một bài do chủ sở hữu cấp quyền thật, stage/review/publish → Android Media3 + background/seek/error; chưa cung cấp secret/audio |
| **Hoãn tới cuối** | **Google Login + email thật + Auth/Cloud E2E** | HOÃN THEO QUYẾT ĐỊNH CHỦ DỰ ÁN | Chỉ kiểm thử thật khi gần hoàn thành Full Product và có credentials do chủ dự án tự cấu hình |
| **P0 — đã test phần offline API 30** | **Offline End-to-End Acceptance v1** — MediaStore → Playlist → MediaSession → History → Queue restore | **22/22 Android PASS, 9/9 marker; matrix v2 sau đó kiểm tra force-stop + đa API** | `qa-logs/offline-e2e-acceptance-v1.log`; `docs/Offline_End_To_End_Acceptance_v1.md` |
| **P0** | **Background Playback / System Controls QA** | V2 QA TỰ ĐỘNG PASS (API 30) — còn lockscreen và Bluetooth thực tế | Test lifecycle, audio focus, điều khiển từ MediaSession/notification/lock screen trong phạm vi khả thi; mọi điều kiện đã chốt đạt PASS |
| **P0 — đã kiểm thử 4 API** | **Android Compatibility Matrix v2** | **104/104 PASS (26/API, API 29/30/33/36)** | `docs/Android_Compatibility_Matrix_v2.md`, 4 file `qa-logs/android-compat-matrix-v2-api-*.log`; bộ test đại diện, chưa phải full connectedDebugAndroidTest đa API |
| **P0 — đang xử lý từng phần** | **Full Product Regression Gate v1 - Part 1** | **BUILD/JVM PASS 99/99; Android API 30 FAIL: 83/89 PASS, 3 FAIL, 3 SKIP** | `docs/Full_Product_Regression_Gate_v1_Part_1.md`, `qa-logs/full-product-regression-v1-part1.log`; tiếp tục 2A MVP Previous và 2B SAF picker |
| **P0 — đã sửa test và QA phần nhỏ** | **Full Product Regression Gate v1 - Part 2A** | **Build/JVM + 8/8 Android API 30 PASS** | `docs/Full_Product_Regression_Gate_v1_Part_2A.md`; không đổi production code, chờ Part 2B và full gate |
| **P0 — QA đạt phần nhỏ** | **Full Product Regression Gate v1 - Part 2B** | **Build/JVM PASS + 2/2 Android API 30 PASS** | `docs/Full_Product_Regression_Gate_v1_Part_2B.md`, `qa-logs/full-product-regression-v1-part2b-saf.log`; có URI trả về thật và persisted READ; chờ full gate Part 3 |
| **P0 — Full regression API 30 PASS** | **Full Product Regression Gate v1 - Part 3** | **99/99 JVM PASS, 86 Android PASS / 0 FAIL / 3 SKIP (89 total)** | `docs/Full_Product_Regression_Gate_v1_Part_3.md`, `qa-logs/full-product-regression-v1-part3.log`; vẫn cần runtime acceptance các tính năng build-only và thiết bị thật |
| **P0 — runtime QA đã đạt API 30** | **Sleep Timer Runtime Acceptance v1** | **11/11 Android PASS**, Build/JVM PASS | `docs/Sleep_Timer_Runtime_Acceptance_v1.md`, `qa-logs/sleep-timer-runtime-v1.log`; expiry test dùng debug timer, chưa thử service restart |
| **P0 — runtime QA đã đạt API 30** | **Equalizer Runtime Acceptance v1** | **10/10 Android PASS**, Build/JVM PASS | `docs/Equalizer_Runtime_Acceptance_v1.md`, `qa-logs/equalizer-runtime-v1.log`; chưa QA thực tế process-restart, audio frequency loopback |
| **P0 — runtime QA đã đạt API 30** | **Lyrics Runtime Acceptance v1** | **105 JVM PASS, 10/10 Android targeted PASS** | `docs/Lyrics_Runtime_Acceptance_v1.md`, `qa-logs/lyrics-runtime-v1.log`; có SAF thật/URI grant và Compose highlight theo MediaSession seek, chưa UI nhập hoàn chỉnh |
| **P0 — runtime QA đã đạt API 30** | **Lyrics Import UI E2E v2** | **105 JVM PASS, 11/11 Android targeted PASS** | `docs/Lyrics_Import_UI_E2E_v2.md`, `qa-logs/lyrics-import-ui-e2e-v2.log`; đã nhấn nút thật, chọn LRC trên DocumentsUI và đồng bộ highlight qua callback production |
| **P0 — runtime QA đã đạt API 30** | **Playback Speed Runtime Acceptance v1** | **105/105 JVM + 10/10 Android PASS** | `docs/Playback_Speed_Runtime_Acceptance_v1.md`, `qa-logs/playback-speed-runtime-v1.log`; chi tiết giới hạn touch riêng |
| **P0 — touch QA đã đạt API 30** | **Playback Speed Touch Interaction QA v2** | **105 JVM PASS, 12/12 Android targeted PASS** | `docs/Playback_Speed_Touch_Interaction_QA_v2.md`, `qa-logs/playback-speed-touch-v2.log`; đã chạm đủ 5 chip bằng UiDevice và cuộn ngang thật ở font 150% |
| **P0 — runtime QA API 30 đạt** | **Crossfade/Gapless Runtime Acceptance v1** | **105 JVM PASS, 12/12 Android PASS**, 13 marker | `docs/Crossfade_Gapless_Runtime_Acceptance_v1.md`, `qa-logs/crossfade-runtime-v1.log`; chưa đo audio gapless |
| **P0 — UI/interruption QA đạt API 30** | **Crossfade UI & Interruption QA v2** | **105 JVM PASS, 17/17 Android PASS** | `docs/Crossfade_UI_Interruption_QA_v2.md`, `qa-logs/crossfade-ui-interruption-v2.log`; bốn lượt cancel giai đoạn MIXING thật, queue/volume bảo toàn |
| **P0 — runtime QA API 30 đạt** | **Playback Queue Actions Runtime Acceptance v1** | **105/105 JVM, 13/13 Android PASS** | `docs/Playback_Queue_Actions_Runtime_Acceptance_v1.md`, `qa-logs/playback-queue-actions-v1.log`; 2 UI + 11 regression, snapshot queue chính xác |
| **P0 — runtime QA API 30 đạt** | **Advanced Library Runtime Acceptance v1** | **105 JVM PASS, 14/14 Android PASS** | `docs/Advanced_Library_Runtime_Acceptance_v1.md`, `qa-logs/advanced-library-runtime-v1.log`; 2 UI/MediaStore + 12 hồi quy, hai album thật; artist/genre fallback của WAV |
| **P0 — runtime QA API 30 đạt** | **Advanced Search Runtime Acceptance v1** | **105 JVM PASS, 14/14 Android PASS** | `docs/Advanced_Search_Runtime_Acceptance_v1.md`, `qa-logs/advanced-search-runtime-v1.log`; 2 E2E + 12 hồi quy, Album, Unicode search, queue; Artist/Genre fallback |
| **P0 — Full Regression API 30 đã đạt** | **Full Product Regression Gate v2 — Part 1** | **PASS: 105 JVM; 112 Android PASS / 0 FAIL / 3 SKIP (115 total)** | `docs/Full_Product_Regression_Gate_v2_Part_1.md`, `qa-logs/full-product-regression-v2-part1.log`; 2/2 SAF targeted và full suite sau sửa đã đạt |
| **P0 — nội bộ QA đã đạt** | **Release Candidate QA / Packaging v1 — Part 1** | **PASS internal QA, KHÔNG PHẢI RELEASE PRODUCTION** | Build/lint release, QA signature v3, cài mới, nâng cấp cùng cert, internal marker giữ nguyên, SHA-256; `docs/Release_Candidate_QA_Packaging_v1_Part_1.md`, `qa-logs/release-candidate-packaging-v1.log` |
| **P0 — QA release-mode API 30 đã đạt** | **Release Candidate QA / Packaging — Part 2** | **PASS internal APK QA API 30; chưa phải release production** | 2 WAV thật, UI Library, MediaSession Play/Pause/Resume/Next, nền + MediaStyle; `docs/Release_Candidate_QA_Packaging_v1_Part_2.md`, `qa-logs/release-playback-smoke-v1.log` |
| **P0 — QA release-mode đa API đã đạt** | **Release Candidate QA / Packaging — Part 3** | **PASS API 29/33/36; API 30 đã PASS trước; AAB unsigned build** | `docs/Release_Candidate_QA_Packaging_v1_Part_3.md`; chưa chạy full instrumentation 115 bài trên release-mode hay máy thật |
| **P0 — Release Readiness QA đạt** | **Release Candidate QA / Packaging — Part 4** | **PASS MP3/FLAC & 105 JVM / APK+AAB audit nội bộ; production vẫn CHƯA SẴN SÀNG** | `docs/Release_Candidate_QA_Packaging_v1_Part_4.md`, logs codec + readiness; thiết bị thật không còn là gate theo quyết định chủ dự án |
| **P0 — ĐÍCH DEMO ĐÃ ĐẠT** | **APK demo cài trực tiếp + báo cáo bàn giao + tạm dừng** | **HOÀN THÀNH TRONG PHẠM VI BÁO CÁO — TẠM DỪNG** | `dist/MusicFlow-1.0-Demo-Android.apk`, `qa-logs/demo-apk-package-v1.log`, hai log WAV/MP3/FLAC trên chính APK; `docs/Bao_Cao_Ban_Giao_MusicFlow_Demo_APK.md` và hướng dẫn cài. Không phải production |
| **Để sau khi chủ dự án yêu cầu lại** | R8/Play Signing/production release + login email/Google thật/cloud hai thiết bị thật | **HOÃN, KHÔNG LÀ ĐIỀU KIỆN ĐIỂM DỪNG** | Không tự khởi động các mốc này trong dự án báo cáo |
| **P1** | UI/accessibility/empty-state/error-state | Foundation v1 13/13 + Polish v2 19/19 Android PASS; toàn diện **CHƯA NGHIỆM THU** | Tiếp tục ma trận API, TalkBack thật, adaptive layout rộng và các trạng thái chưa chuẩn hóa |
| **P1** | Hiệu năng và dữ liệu lớn | CHƯA NGHIỆM THU | Dataset cố định; ngưỡng thời gian/độ ổn định được chốt; không crash/ANR, không mất dữ liệu |
| **Theo quyết định** | Phát hành Google Play | CHƯA CHỐT PHẠM VI | Chỉ mở milestone nếu chọn hình thức phân phối này |

### 5.1. Playback Runtime Recovery Race Hardening v3 — PASS

- Ngày kiểm thử: 2026-10-26.
- Source: `PlaybackRuntimeAutoSkip.kt`.
- Android tests: 36/36 PASS.
- JVM tests: PASS.
- Log: `qa-logs/playback-runtime-recovery-v3.log`.
- Các trường hợp đã kiểm tra:
  - Pause rồi Play trước callback phục hồi.
  - Thay queue bằng nội dung giống hệt.
  - Seek trong cùng bài hát.
  - Dispose khi còn callback chờ.
  - Auto-Skip bình thường khi không có thao tác mới.
- Kiểm thử hồi quy v2: PASS.
- Giới hạn: các bài race v3 dùng lỗi tiêm có kiểm soát; chưa phải nghiệm thu toàn bộ MVP.
- Bước tiếp theo: MVP End-to-End Acceptance v1.

## 6. Định nghĩa hoàn thành (Definition of Done — DoD)

### Cổng A — Chốt mục tiêu MVP

- [ ] Chủ dự án xác nhận phạm vi MVP mục 2, hoặc sửa bằng yêu cầu cụ thể.
- [ ] Quyết định quy tắc playback: resume/Pause sau restart, Skip/Repeat/Shuffle khi lỗi, quyền URI, hỗ trợ Android và phương thức phát hành.
- [ ] Phân loại mọi mục bắt buộc / có thể để sau; không để yêu cầu P0 mở mà không có quyết định.

### Cổng B — Nghiệm thu chức năng

- [ ] **Library, file picker, Search, Player, Playlist, History** đều có kiểm thử tự động theo đúng hành trình người dùng, log lưu vào `qa-logs/`.
- [ ] Các nút phát/tạm dừng/tua/trước/sau/shuffle/repeat và MediaController nhất quán với UI; không phát nhầm bài.
- [ ] Playlist và Recently Played giữ dữ liệu đúng sau mở lại/nâng cấp Room trong các trường hợp đã chốt.
- [ ] Lưu/khôi phục queue, lỗi nguồn nhạc, quyền, file mất và tác vụ Auto-Skip đến muộn đều xử lý an toàn; không vòng lặp, không tự phát trái ý muốn.
- [ ] Không còn lỗi **P0/P1 chặn nghiệm thu** trong phạm vi MVP đã thống nhất.

### Cổng C — Chất lượng & hồi quy

- [x] **Mốc Runtime Recovery Integration v2:** JVM PASS, Android 31/31 PASS, log đã đối chiếu (không có nghĩa toàn bộ cổng C đã hoàn thành).
- [ ] Có test và log PASS cho **race-condition v3** và các lỗi P0 còn lại.
- [ ] **Toàn bộ** test JVM + Android/UI đã chốt PASS trên **chính commit release candidate**, không chỉ một commit cũ.
- [ ] Ma trận phiên bản Android/thiết bị đã chốt PASS hoặc có ngoại lệ được duyệt và ghi rõ rủi ro.
- [ ] Không crash/ANR hoặc mất dữ liệu trong bộ ca nghiệm thu; mỗi lỗi tái hiện có test hồi quy.

### Cổng D — Release & bàn giao

- [ ] Build release đáp ứng phương thức phân phối được chọn (APK/AAB).
- [ ] Có ký release đúng cách, version, SHA commit và checksum artifact.
- [ ] Kiểm thử tự động hoặc quy trình CI có bằng chứng cài mới, nâng cấp và smoke test **trên bản release**.
- [ ] `docs/Ke_Hoach_Du_An.md`, log cuối và danh sách rủi ro/ngoại lệ đã cập nhật.
- [ ] Chủ dự án xác nhận nghiệm thu và quyết định phát hành/bàn giao.

**Khi nào được ghi `DỰ ÁN HOÀN THÀNH`?**

- **Mốc kỹ thuật PASS:** riêng test của mốc thành công; lưu commit, log, marker, phạm vi rõ ràng.
- **MVP HOÀN THÀNH:** toàn bộ **Cổng A + B + C** trong phạm vi được duyệt; không còn P0/P1 chặn; có xác nhận của chủ dự án.
- **SẴN SÀNG PHÁT HÀNH:** đã đạt MVP và **Cổng D** theo phương thức phân phối được chọn.
- **CHƯA HOÀN THÀNH:** bất kỳ cổng bắt buộc nào chưa đủ bằng chứng, dù vòng kiểm thử gần nhất báo `FINAL_RESULT=PASS`.

**Không sử dụng phần trăm hoàn thành tùy đoán.** Đánh giá theo checklist và bằng chứng thực tế.

## 7. Quy trình cố định giữa người dùng và trợ lý

1. **Đầu mỗi chat / đầu mỗi bước:** đọc file này; kiểm tra HEAD và vài commit gần nhất; đọc log của mốc đang làm; xem chính xác source/test/script liên quan. Tiếp tục từ trạng thái thực tế, không làm lại bước PASS.
2. **Lệnh “Kiểm tra tiếp” / “Kiểm tra [mốc]”:** dùng GitHub để đọc commit + log mới. Kết luận PASS chỉ nếu script chạy thành công **và** assertions/markers của mốc đủ; nếu FAIL nêu nguyên nhân, vị trí sửa tối thiểu và script chạy lại. Không che FAIL bằng cách bỏ điều kiện test.
3. **Lệnh “Bước tiếp theo”:** lấy backlog mục 5, đối chiếu mã hiện hành, chọn đúng một milestone; đưa code/test/script cụ thể để người dùng áp dụng.
4. **Kiểm thử:** ưu tiên JVM, Android instrumented tests, Android Emulator qua **một script PowerShell**, một log cố định cho mỗi milestone; không bắt người dùng kiểm thử thủ công. Chạy các test cũ để phát hiện hồi quy. **Chỉ tạo script kiểm thử tổng hợp khi hoàn thành nhóm chức năng lớn**; chức năng đặc thù cần kiểm thử sớm có thể viết test riêng và tích hợp vào script tổng hợp sau.
5. **Quyền thay đổi:** mặc định trợ lý **chỉ đọc/phân tích/chuẩn bị patch hoặc file**. Không tự ý sửa, commit hay push repository nếu người dùng chưa cho phép. Một file tải về không đồng nghĩa repo đã thay đổi.
6. **Lệnh Git:** khi cần hướng dẫn push, **chỉ ba lệnh** dưới đây; không thêm `git status`, `git pull`, `git log`, `git rev-parse`, v.v. vào hướng dẫn cho người dùng:

   ```bash
   git add .
   git commit -m "docs: update MusicFlow project progress and completion criteria"
   git push
   ```

7. **Cập nhật tài liệu:** sau **mỗi** lần thử, sửa lỗi hoặc hoàn thành tính năng, **bắt buộc chuẩn bị bản cập nhật file này trước phần hướng dẫn commit/push**; đồng thời cập nhật tài liệu DoD bổ sung nếu thay đổi tiêu chí nghiệm thu hoặc phạm vi.
8. **Không xóa lịch sử:** mốc PASS cũ giữ trong mục 4/8; vòng đang FAIL ghi rõ vòng lỗi, không sửa lịch sử để giả vờ PASS.
9. **Báo cáo bằng tiếng Việt**: có đường dẫn file, đoạn code sửa, lệnh PowerShell để chạy, log mong đợi, ba lệnh Git và hướng dẫn nhắn “Kiểm tra tiếp”.

### 7.1. Mẫu báo cáo/kết quả một bước

```markdown
### YYYY-MM-DD — <Tên milestone> — PASS | FAIL | ĐANG LÀM
- Commit chứa source/test/log: `<full SHA>`.
- HEAD đối chiếu: `<full SHA>`.
- File sửa / thêm: `<đường dẫn>`.
- Script: `scripts/<script>.ps1`.
- Log: `qa-logs/<log>.log`.
- JVM: PASS/FAIL; Android: x/x PASS hoặc chi tiết FAIL.
- Marker/điều kiện: `<những điều kiện có thể kiểm chứng>`.
- Nguyên nhân FAIL / giới hạn còn lại: `<chi tiết>`.
- Quyết định: `<giữ, sửa, nghiệm thu, không triển khai lại>`.
- Bước tiếp theo: `<một việc cụ thể>`.
```

### 7.2. Những phần bắt buộc cập nhật mỗi vòng

- **Mục 0:** tên mốc gần nhất, PASS/FAIL/ĐANG LÀM, HEAD, commit QA, log, số test, bước kế tiếp.
- **Mục 4:** thêm mốc mới **chỉ khi có PASS**; giữ mốc cũ.
- **Mục 5:** chuyển việc vừa xử lý sang PASS hoặc ghi nguyên nhân FAIL, xác định bước kế tiếp.
- **Mục 6:** chỉ đánh dấu `[x]` một cổng/tiêu chí khi đã có **bằng chứng đúng phạm vi**.
- **Mục 8:** thêm nhật ký mới lên trên; nếu FAIL cũng phải ghi lại.
- **Mục 1/2/7:** sửa khi môi trường, mục tiêu hay quy trình thay đổi.

## 8. Nhật ký thay đổi — giữ lịch sử

### 2026-11-05 — BỔ SUNG KẾ HOẠCH QUẢN TRỊ VIÊN VÀ TÀI KHOẢN TÁC GIẢ/NGHỆ SĨ — DOCUMENTATION ONLY

- Theo yêu cầu chủ dự án, ghi rõ **hiện chưa có UI đăng nhập Admin/dashboard quản trị người dùng, cũng chưa có đăng ký/hồ sơ/Studio Tác giả và Nghệ sĩ**. Existing `Guest/Free/Premium` và `scripts/musicflow_licensed_upload.py` CLI quản trị nội dung không được gọi nhầm là chức năng quản trị người dùng hoàn chỉnh.
- Bổ sung roadmap `docs/Lo_Trinh_Quan_Tri_Vien_Tac_Gia_Nghe_Si.md` gồm vai trò Listener/Artist/Author/Moderator/Admin **tách biệt gói Free/Premium**, backend RLS và quyền cấp role do server quản lý, dashboard Admin Web quản lý người dùng và duyệt nội dung, creator profile/upload nháp/credit/bằng chứng quyền phát online, nhật ký audit/khiếu nại và tiêu chí kiểm thử.
- Thứ tự tương lai **A1 role/backend security → A2 quản trị Admin Web → A3 Artist/Author Studio → A4 mở rộng và QA**. **Mọi mốc đều CHƯA TRIỂN KHAI**; không thay đổi Supabase/Auth, không tạo user/role Admin, không build APK, không tự phát hành hay yêu cầu email thật. Mục tiêu trước mắt vẫn APK demo cho báo cáo; việc phát triển vai trò mới chỉ bắt đầu khi chủ dự án yêu cầu.

### 2026-11-05 — SEARCH UX v3 — TRẠNG THÁI RỖNG/QUYỀN, NHÓM KẾT QUẢ, ĐIỀU HƯỚNG DỄ HIỂU

- HEAD nền `b33b6ec200dc0e85fbfaf9581cc90490374988c5`, Git `main = origin/main` sạch đầu lượt. Tiếp tục mục tiêu UX cho APK sideload/báo cáo, không chuyển release thật.
- **SearchScreen/SearchRoute:** nhãn `Tìm nhạc trên thiết bị` + gợi ý không dấu; FilterChip Bài hát/Album/Nghệ sĩ/Thể loại hiện số kết quả khi lọc xong, gắn test tag. Khi thiếu quyền quét MediaStore hoặc không có nhạc đã quét, có nút `Mở Thư viện để chọn tệp nhạc` để chuyển tab Library/SAF; không ngầm tuyên bố Search quét toàn bộ file SAF. Khi không có bài hát trực tiếp nhưng các nhóm khác có kết quả, thông báo mời chuyển nhóm, có thao tác xóa query. Nút xóa query chuyển thành Material Close với TalkBack label. Cỡ chữ từ 135% khiến tiêu đề/xử lý Quét lại xếp dọc trên màn hẹp. Sửa bug UI **hai nút Quét lại** xuất hiện khi thư viện chưa tải bằng việc ẩn nút đầu trang khi trạng thái còn rỗng/chưa có thư viện.
- **Navigation:** `MusicFlowApp` nối callback Search→Library; không thay đổi picker hoặc dữ liệu người dùng. Cập nhật selector các test `AdvancedSearchRuntimeTest` dùng testTag thay vì chữ tab cố định.
- **QA tập trung:** `:app:assembleDebug :app:assembleDebugAndroidTest` PASS; `MusicFlowSearchUxV3Test` 4/4 Android PASS, hai test cũ Search retry/clear 2/2 PASS, `AdvancedSearchRuntimeTest#searchRouteAlbumArtistAndGenreFilterTabsUseRealScanner` 1/1 PASS với MediaStore WAV fixtures. **Tổng 7/7 Android PASS**; không full suite/JVM theo yêu cầu. QA AVD riêng `MusicFlow_SearchUX_V3_QA_API30` Android API 30, không đụng emulator `5556` của người dùng.
- **APK demo mới:** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-SearchUXV3-Android.apk`, 30.787.261 byte, SHA-256 `95F79772A410F33EEF4A4E90A99931BF9548B3C51D4038451C4C1C6FFB6C1BC5`; Gradle release/zipalign/apksigner/aapt PASS, non-debuggable/ký debug key QA. Cài nâng cấp/cold launch trên emulator QA, chạm tab Tìm kiếm → nút Mở Thư viện → thấy `Nhạc thiết bị` và `Chọn tệp nhạc` PASS. APK cũ được giữ nguyên; không upload/push APK.
- Bằng chứng `docs/MusicFlow_Search_UX_v3.md`, `qa-logs/musicflow-search-ux-v3.log`, `qa-logs/demo-apk-search-ux-v3-package.log`. Trạng thái vẫn **demo/báo cáo — chưa phát hành thật**.

### 2026-11-05 — PLAYLIST UX v3 — TÌM KIẾM KHÔNG DẤU / MENU THAO TÁC / FONT LỚN

- HEAD nền `1ffe5d193d907fcd638f3dbe1eadea2dd4eb1814`, `main = origin/main`, working tree sạch đầu lượt. Tiếp tục phạm vi demo/báo cáo; không sử dụng email/thiết bị thật, không thay đổi streaming, Media3, Supabase, quyền truy cập hay nội dung người dùng.
- **Danh sách phát:** thêm ô `Tìm playlist` lưu trạng thái bằng `rememberSaveable`, so khớp tên không phân biệt dấu/hoa/thường (`PlaylistNameFilter.kt`), hiển thị kết quả và nút `Xóa tìm kiếm`. Khi phóng chữ từ 135% hoặc màn hình hẹp, tiêu đề chuyển sang bố cục dọc; tên playlist tối đa hai dòng, nút `Tùy chọn` đổi sang icon ba chấm với mô tả TalkBack.
- **Chi tiết playlist:** thay ba nút Lên/Xuống/Xóa ở mỗi bài bằng menu `Tùy chọn bài hát` (`PlaylistTrackOptions.kt`); chỉ cho di chuyển khi hợp lệ, giữ nguyên `PlaylistViewModel.moveTrack`; xóa vẫn có AlertDialog xác nhận và không xóa file gốc. Cập nhật 2 test PlaylistPlaybackOrder hiện hữu để dùng menu, test tạo/đổi tên sử dụng tag `playlist-name-input` tránh nhầm với ô tìm kiếm.
- **Targeted QA:** 4/4 JVM `PlaylistNameFilterTest` PASS; Android API30 AVD QA riêng `MusicFlow_PlaylistUX_QA_API30` có `MusicFlowPlaylistPolishV3Test` 2/2 PASS (font scale 180%, menu/bounds/xóa), hai test cũ reorder-playback + create/rename/delete 2/2 PASS, `PlaylistSearchUiTest` 1/1 PASS (tìm không dấu, click đúng playlist, trạng thái rỗng, clear search, xóa playlist QA của test). **Tổng 5/5 Android PASS**, build debug + AndroidTest PASS. Không chạy full suite theo yêu cầu.
- **APK demo sideload mới:** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-PlaylistUXV3-Android.apk`, 30.787.261 byte, SHA-256 `C40F59CE54747DC54637147C375E98263F4D774C92E6CB5568E44BA5D230A3AD`; Gradle release/zipalign/apksigner/aapt/sha PASS, ký bằng debug key QA, không debuggable nhưng không phải production. Cài bản mới trên QA emulator và cold launch/Trang chủ→Danh sách phát/màn playlist trống PASS. APK cũ bảo toàn trong `dist` (Git ignored).
- Báo cáo `docs/MusicFlow_Playlist_UX_v3.md`; logs `qa-logs/musicflow-playlist-ux-v3.log` và `qa-logs/demo-apk-playlist-ux-v3-package.log`. Các cải tiến tiếp theo chỉ khi người dùng yêu cầu, điểm dừng vẫn là **APK demo báo cáo**.

### 2026-11-05 — PLAYER & LIBRARY UX POLISH v3 — TÙY CHỈNH THU GỌN / SẮP XẾP BÀI HÁT

- HEAD nền `a7c34d6e996756ed46976abe40088b9813eafc96`, `main = origin/main`, Git sạch đầu lượt. Người dùng yêu cầu tiếp tục lượt UX sau UX Foundation v3, không cần full test.
- **Player:** `NowPlayingScreen.kt` giữ ngay ảnh bìa, phát/tạm dừng/chuyển bài, shuffle/repeat/queue và lyrics. Tốc độ/Hẹn giờ/Crossfade/Equalizer mặc định thu gọn sau nút `Tùy chỉnh phát nhạc`, có tóm tắt tốc độ và trạng thái hẹn giờ, testTag `player-advanced-toggle`. **Không sửa Media3 hoặc xử lý âm thanh.** Cập nhật 2 test runtime cũ Speed/Crossfade để chúng mở nhóm trước khi tìm các chip, chưa chạy lại các test full runtime này.
- **Library:** `DeviceMusicScanner.kt` thêm cột `MediaStore.Audio.Media.DATE_ADDED` và `DeviceSong.dateAddedSeconds` mặc định 0, không phá các callsite cũ. `LibrarySongSort.kt` cung cấp sort Tên bài/Nghệ sĩ/Album/Mới thêm. `LibraryScreen.kt` hiển thị menu sắp xếp riêng cho danh sách Bài hát; cả UI và `onSongSelected` sử dụng cùng `visibleSongs` đã sort, bao gồm lọc yêu thích. Playlist, albums/artists/genres giữ chức năng cũ. `DATE_ADDED` phản ánh lúc Android thêm file, không phải ngày sản xuất bản nhạc.
- **QA đúng phạm vi:** `:app:assembleDebug :app:assembleDebugAndroidTest` BUILD SUCCESSFUL; `MusicFlowPlayerLibraryUxV3Test` trên QA AVD Android API30 riêng **3/3 PASS**: vùng nâng cao collapsed/expand, sắp xếp 4 cách, chọn Mới thêm trên UI và queue callback đúng thứ tự. Không chạy full JVM/Android instrumentation, không email/điện thoại thật.
- **APK sideload:** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-PlayerLibraryUXV3-Android.apk`, 30.770.877 byte, SHA-256 `1DDAB2611F941701277B1E50812ED209A92102B6D33CE9E9492B851E0026BDCD`; Gradle release/zipalign/apksigner/aapt PASS, APK demo QA ký debug keystore và không debuggable, cài nâng cấp/cold launch + 5 UI marker trên QA emulator PASS. Giữ nguyên các APK cũ trong `dist`; không upload lên GitHub/Play, không thay đổi production signing.
- Báo cáo `docs/MusicFlow_Player_Library_UX_v3.md`; log `qa-logs/musicflow-player-library-ux-v3.log` và `qa-logs/demo-apk-player-library-ux-v3-package.log`. **Tiếp tục coi MusicFlow là APK demo/báo cáo**, chỉ mở bước mới theo yêu cầu người dùng.

### 2026-11-05 — UX FOUNDATION v3 — VIỆT HÓA ĐIỀU HƯỚNG / HOME / ONBOARDING / CHỌN TỆP

- HEAD nền `407939c492d4d67e47ad8b7d1013c74949c56722`, `main = origin/main`, working tree sạch đầu lượt. Người dùng yêu cầu thực hiện lượt cải thiện UX ưu tiên, không mở rộng phát hành thật; không thay đổi Supabase Auth, bản quyền nhạc online, cloud, Media3 hay database.
- **Navigation:** 4 tab `Trang chủ / Tìm kiếm / Thư viện / Tài khoản`, icon Material nhất quán, có UI test tag. Thêm dependency Compose BOM `material-icons-core`.
- **Home:** nút mở lại trình phát ở đầu màn khi đã có nhạc; card Bắt đầu nghe chỉ giữ ba đường thao tác chính; yêu thích và online chuyển xuống Khám phá thêm; hướng dẫn lần đầu 3 bước + nút Đã hiểu, nhớ lựa chọn bằng SharedPreferences `musicflow_home_guide_v3`; không chặn thao tác thư viện.
- **Library/Now Playing:** `Chọn MP3` thành `Chọn tệp nhạc`, cập nhật mô tả quyền và trạng thái rỗng để không gây hiểu nhầm; các callback và hỗ trợ file không thay đổi. Điều chỉnh chính xác các test cũ phụ thuộc nhãn.
- **Kiểm thử targeted:** `:app:assembleDebug :app:assembleDebugAndroidTest` BUILD SUCCESSFUL; `MusicFlowUxFoundationV3Test` 2/2 PASS và `AccountAuthUiTest` 1/1 PASS trên AVD QA Android API30 riêng. UI dump xác nhận 4 nhãn nav và CTA Home; guide Đã hiểu rồi cold restart **không hiện lại** PASS. **Không chạy full JVM / full instrumentation** theo yêu cầu.
- **APK demo mới giữ lại các bản cũ:** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-UXFoundationV3-Android.apk`, 30.754.493 byte, SHA-256 `467B5D4D4664BD35DE5A8FF07E56D5682D316C23A6A5F05AFB76C3E8DB5F3AAF`; zipalign, sign QA, apksigner, aapt và cài nâng cấp/cold launch/5 UI marker PASS. Không production signing, không Play, không auto push APK. Hướng dẫn `docs/MusicFlow_UX_Foundation_v3.md`; logs `qa-logs/musicflow-ux-foundation-v3.log` và `qa-logs/demo-apk-ux-foundation-v3-package.log`.
- **Hướng sau này nếu chủ dự án yêu cầu:** Player & Library Polish v3 (gọn phần điều khiển nâng cao, sắp xếp bài/album, thao tác playlist), vẫn chỉ nghiệm thu targeted trên emulator và đóng APK demo; trạng thái dự án **báo cáo/tạm dừng** không tự chuyển thành release thật.

### 2026-11-05 — AUTH UX v1 / SUPABASE LOGIN-REGISTER THÂN THIỆN — E2E + UI PASS

- Mục tiêu người dùng: thiết kế lại đăng ký/đăng nhập **Supabase online**, không cần email confirmation; giữ nút Google **hiển thị nhưng chưa hoạt động**. Cấu hình server read-only: email ON, `mailer_autoconfirm=true`, Google OFF. Giữ nguyên Supabase Auth/RLS và chế độ phát nhạc offline.
- UI `AccountScreen.kt`: tab Đăng nhập / Tạo tài khoản, chỉ một nút submit chính, email/mật khẩu/xác nhận và Hiện/Ẩn, lỗi theo trường, nút guest điều hướng Home; đưa cài đặt theme xuống sau form. `AccountFormValidation.kt` với 5 JVM tests; `AccountViewModel.kt` giữ Supabase mặc định và không tự restore tài khoản Demo cục bộ cũ. `MusicFlowApp.kt` chuyển tới Home khi dùng khách. Nút Google disabled có giải thích.
- **121/121 JVM PASS** (0 fail/skip), **1/1 instrumented Android Auth UI PASS**; đăng ký tài khoản QA bằng email thử nghiệm trên emulator → Supabase Auth tạo user/`account_profiles`, email được autoconfirm; signout → signin cùng tài khoản PASS, đóng/mở app → restore PASS. **Chỉ xóa user QA tự tạo trong lượt này**; account QA từ mốc khác vẫn còn; không dùng email thật.
- APK demo cuối `dist/MusicFlow-1.0-Demo-AuthUX-Android.apk`, 30.279.249 byte, SHA-256 `C1B081CDD7AFE5BCB2C3FFC2E1B231CFFFD1A188503B18D0C06A0F1229A0AA04`, `assembleRelease`/zipalign/ký QA/apksigner/aapt PASS. Cài nâng cấp trên emulator QA và giữ phiên đăng nhập trước khi xóa test user PASS. Không phải Play/production; APK không push.
- Bằng chứng `docs/Account_Auth_UX_Supabase_v1.md`, `qa-logs/account-auth-ux-supabase-v1.log`, `qa-logs/demo-apk-auth-ux-package-v1.log`. Bản báo cáo hoàn thiện trong phạm vi demo, không mặc định đẩy giai đoạn phát hành.

### 2026-11-05 — SUPABASE AUTH KHÔNG EMAIL CONFIRM — UI SIGNUP / SIGNIN / RESTORE E2E PASS

- **Quyết định mới từ chủ dự án:** đăng ký và lưu tài khoản thật trên **Supabase Auth**, nhưng **không xác nhận email**. Không chuyển signup mặc định sang tài khoản local demo. Ảnh người dùng cho thấy ô mật khẩu trống; UI cần nhắc tối thiểu sáu ký tự. Điều này thay thế yêu cầu cũ ưu tiên dùng local demo vì chưa có email thật.
- **Xác minh hosted Supabase thực tế:** `external.email=true`, `mailer_autoconfirm=true` (Confirm email OFF), `disable_signup=false`. Assistant **không thay đổi cấu hình máy chủ** và không dùng service_role key. Phải giữ RLS theo chủ sở hữu; không dùng thông tin email chưa xác minh làm bằng chứng danh tính.
- **Android đã sửa:** `AccountViewModel.kt` mặc định `demoMode=false` (Supabase online), thông báo riêng cho email sai/mật khẩu rỗng hoặc dưới 6 ký tự, signup thành công nói rõ tài khoản ở Supabase. `AccountApi.kt` thêm hàm GET trạng thái email confirmation từ Supabase. `AccountScreen.kt` hiển thị chính xác trạng thái máy chủ, trợ giúp mật khẩu. Các thay đổi local demo có sẵn trong worktree đầu phiên được **giữ nguyên**, chỉ sửa hành vi mặc định. `scripts/build-musicflow-demo-apk-v1.ps1` thêm tên output mới, không ghi đè APK trước.
- **E2E PASS trên AVD QA riêng API 30:** cài `dist/MusicFlow-1.0-Demo-SupabaseSignup-Android.apk` (30.262.865 bytes; SHA-256 `74D5D93294B8B6AAB7283B12835FD53AD0E1AC6BCA179A13F7F17D2ACD795026`), UI chọn Supabase online, hiển thị Confirm email đã tắt, signup bằng email thử nghiệm và mật khẩu ngẫu nhiên; kết quả UI account Free. Supabase SQL read-only xác nhận 1 `auth.users` QA đã auto-confirm và 1 `public.account_profiles` QA, không có entitlement Premium (Free). Đăng xuất → đăng nhập cùng tài khoản và restore sau process-stop **PASS**. Test fixture không chứa mật khẩu trong logs/Git, một QA-only account còn trên Supabase để quản lý/xóa bằng Dashboard khi cần.
- `:app:testDebugUnitTest :app:assembleRelease` BUILD SUCCESSFUL, **116 JVM PASS / 0 FAIL / 0 SKIP**, APK mới đã ký QA và `apksigner` verify PASS. Tài liệu `docs/Kiem_Thu_Supabase_Auth_Khong_Xac_Nhan_Email_v1.md`; log `qa-logs/supabase-signup-no-email-confirm-v1.log` và `qa-logs/demo-apk-supabase-signup-package-v1.log`.
- Phạm vi vẫn **APK demo/báo cáo**, chưa Play, chưa thiết bị thật. Không bảo đảm sở hữu email khi tắt confirm. Người dùng sau này muốn đưa sản phẩm ra ngoài cần đánh giá bật xác minh email và SMTP/khôi phục tài khoản.

### 2026-11-05 — KIỂM THỬ ĐĂNG KÝ TRÊN UI THẬT — SUPABASE 429 / AUTH FEEDBACK HARDENING

- HEAD nền `89dbfda8d4c1408b94a88b946c7f267008e657b8`, working tree sạch đầu lượt. Trên AVD QA sạch `MusicFlow_SignUpQA_API30`, cài bản APK demo gốc, nhập **đủ email thử nghiệm tại miền `example.invalid` + mật khẩu QA ngẫu nhiên**, bấm `Đăng ký Free` thực tế. UI hiển thị `Thao tác quá nhanh. Hãy thử lại sau.`; Supabase Auth log xác nhận **HTTP 429: email rate limit exceeded**. SQL read-only xác minh 0 `auth.users` trước/sau; không tạo tài khoản hoặc gửi email cho người thật. Log lịch sử có thêm HTTP 400 email không hợp lệ.
- Căn nguyên là **hosted Supabase email send quota**, không phải sự cố nút signup Android. Mặc định Supabase hosted chỉ gửi email đến người dùng thuộc nhóm dự án khi chưa có custom SMTP. Không thể sửa client để vượt quota. **Đăng ký hoàn tất E2E vẫn BỊ CHẶN**; nếu cần Auth thật phải thêm SMTP/Send Email Hook và giữ xác minh email; nếu cần demo không email thật thì dùng Supabase local + Mailpit hoặc demo account offline có gắn nhãn. Không tự thay đổi Auth provider, RLS hoặc tắt confirm email.
- **Mã Android cải thiện:** `AccountApi.kt` đọc JSON error code từ `errorStream` (giới hạn 4096 ký tự); `AccountAuthErrorLabels.kt` ánh xạ lỗi quota, email bị cấm/không hợp lệ, chưa xác nhận, sai mật khẩu… sang hướng dẫn cụ thể, không hiển thị nguyên văn log chứa PII. `AccountAuthErrorLabelsTest.kt`: 7/7 PASS; full JVM **112 PASS / 0 FAIL / 0 SKIP**.
- **APK demo mới, không ghi đè bản cũ:** `dist/MusicFlow-1.0-Demo-AuthFeedback-Android.apk`, 30.246.481 byte, SHA-256 `27FCF8E9B55F26ACB4DCF3B6DC816FC87EF81F8D3CBD69003DFECF434B293F04`; ký QA, verify PASS, nâng cấp trên AVD QA và mở màn Account PASS. Script đóng gói hỗ trợ `-OutputName` mới, giữ nguyên log và APK bàn giao cũ. **Không bấm signup lại sau chỉnh sửa để tránh gửi yêu cầu vào backend đang rate-limited**.
- Báo cáo `docs/Kiem_Tra_Dang_Ky_Thuc_Te_HTTP429_v1.md`, log `qa-logs/auth-signup-http429-v1.log` + `qa-logs/demo-apk-auth-feedback-package-v1.log`. Giữ bản demo/báo cáo ở phạm vi sideload, không coi Auth thật là đã đạt.

### 2026-11-05 — KIỂM TRA BỔ SUNG TÀI KHOẢN / ĐĂNG KÝ / ĐĂNG NHẬP / ADMIN

- Git HEAD `8095a22`, working tree sạch đầu lượt; dùng APK demo đã bàn giao, **không sửa Kotlin production**. Tạo AVD QA API 30 riêng, cài `dist/MusicFlow-1.0-Demo-Android.apk` và cold launch PASS; tab Account có email, mật khẩu, `Đăng ký Free` và `Đăng nhập`. Ở form trống/thiếu mật khẩu, hai thao tác trả lỗi xác minh dữ liệu đúng, không tạo tài khoản.
- Supabase Auth settings trực tiếp `/auth/v1/settings`: `external.email=true`, `external.google=false`, `mailer_autoconfirm=false`; Google Login trong app bị khóa và thông báo provider chưa được cấu hình. SQL **read-only**: `auth.users=0`, `public.account_profiles=0`, `public.account_entitlements=0`; bảng hồ sơ/entitlement có RLS, không thấy bảng role/admin trong schema `public`. **Không có signup confirm email hay signin thành công E2E vì chủ dự án không dùng email thật**. 17/17 JVM (5 AccountTier + 12 GoogleOAuthFlow) PASS; không đánh đồng unit tests với E2E.
- Kiểm tra source `AccountScreen.kt` / `AccountApi.kt` và navigation: **có giao diện tài khoản Guest/Free/Premium**, **chưa có giao diện Admin Android/web**. Có `scripts/musicflow_licensed_upload.py` CLI `stage`/`publish` dùng quyền backend riêng, không phải dashboard admin.
- QA AVD `MusicFlow_AuthQA_API30` và thư mục còn sót đã được xóa; giữ nguyên bốn AVD gốc. Không đổi Supabase schema, không thêm người dùng, không dùng mật khẩu/email thật hoặc mở phát hành. Báo cáo `docs/Kiem_Tra_Tai_Khoan_Dang_Nhap_Admin_v1.md`; log `qa-logs/account-auth-admin-audit-v1.log`. **Trạng thái dự án demo/báo cáo: vẫn hoàn thành và tạm dừng**.

### 2026-11-01 — MUSICFLOW DEMO APK BÀN GIAO — HOÀN THÀNH PHẠM VI BÁO CÁO / TẠM DỪNG

- Từ `HEAD=1b8491d19e76b72f541e277bb95cc7506a5c9939` và working tree hiện tại, tạo **APK demo thực tế có thể sideload** bằng `scripts/build-musicflow-demo-apk-v1.ps1`: Gradle `assembleRelease` PASS, `zipalign` + `apksigner` bằng Android Debug keystore QA, certificate v3 VERIFIED, APK **không debuggable**, `aapt` xác minh ID `com.example.musicflow`, `versionCode=1`, `versionName=1.0`, minSdk29 / targetSdk36.
- **Tệp bàn giao cục bộ:** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-Android.apk` (30.230.097 byte), SHA-256 **`ACE9AD765F75C7939C513210E2BBC1555F95E6504636303B423FB70598C2E750`**; `dist/MusicFlow-1.0-Demo-Android.apk.sha256` lưu checksum. Binary APK nằm ngoài Git, không upload hoặc phát hành công khai.
- Chạy smoke **trên chính APK trong `dist`**, không rebuild/re-sign: `qa-logs/demo-apk-smoke-wav-api30-v1.log` và `qa-logs/demo-apk-smoke-codec-api30-v1.log` đều **FINAL_RESULT=PASS**: fresh install, Library, MediaStore nhận WAV/MP3/FLAC, metadata codec, MediaSession Play/Pause/Resume/Next, Background Playback, MediaStyle notification, QA_CLEANUP=PASS. Cùng SHA trong cả hai log.
- Bàn giao tài liệu `docs/Huong_Dan_Cai_Dat_MusicFlow_Demo_APK.md` (đường dẫn APK/checksum, cài emulator, sideload và lưu ý chữ ký) + `docs/Bao_Cao_Ban_Giao_MusicFlow_Demo_APK.md` (tóm tắt kiến trúc, test, chức năng, giới hạn và kết luận). `docs/Muc_Tieu_Dung_Tam_APK_Bao_Cao.md` chuyển sang trạng thái hoàn thành; **không cần email thật, điện thoại thật, sync hai thiết bị thật, Play, ký production**.
- **Giới hạn nghiệm thu rõ ràng:** bản demo đạt theo phạm vi báo cáo, không phải ứng dụng thương mại hoàn chỉnh. Online licensed streaming thật, Google/email E2E và cloud hai thiết bị chưa nghiệm thu; full 115 bài Android instrumentation là debug test trước đây, không phải release full suite. Hệ thống chỉ kiểm tra giấy phép với nội dung online được MusicFlow cung cấp, không chặn nhạc offline người dùng tự lưu/tải.
- **Điểm dừng tạm thời đã đến**: dừng các mốc phát triển tự động sau bàn giao, chỉ tiếp tục khi có yêu cầu mới của chủ dự án. Git thay đổi còn local chưa commit/push; commit code/tài liệu không đính kèm binary APK do `.gitignore`.

### 2026-11-01 — ĐIỀU CHỈNH ĐÍCH DỰ ÁN — APK DEMO BÁO CÁO, KHÔNG RELEASE THẬT

- Chủ dự án yêu cầu **tạm dừng tại bản MusicFlow hoàn thiện để tải APK cài trực tiếp và phục vụ báo cáo**. Không phát hành thật, không email thật, không thiết bị thật, không yêu cầu Google Login thật, không cloud E2E hai thiết bị thật. Đây là quyết định mới nhất thay thế roadmap production.
- **Không thay đổi mã nguồn sản phẩm hoặc xóa QA cũ ở bước điều chỉnh phạm vi này.** Tiếp tục dựa vào bằng chứng release-mode APK QA/emulator đã có; vẫn phân biệt tính năng có source với tính năng đã nghiệm thu.
- Đích cuối: chức năng demo hoạt động, APK có thể sideload và có chữ ký demo ổn định phù hợp phạm vi, checksum/hướng dẫn cài, báo cáo đã hoàn tất, rồi ghi trạng thái **HOÀN THÀNH BẢN DEMO/BÁO CÁO — TẠM DỪNG**. Hiện chưa kết luận hoàn thành; `docs/Muc_Tieu_Dung_Tam_APK_Bao_Cao.md` là checklist ưu tiên.

### 2026-11-01 — Release Candidate QA / Packaging v1 Part 4 — MP3/FLAC + ARTIFACT/JVM AUDIT PASS

- **Quyết định chủ dự án:** bỏ kiểm thử trên thiết bị thật khỏi cổng QA hiện tại. Giữ bằng chứng emulator, kiểm tra build và artifact; mọi phần phụ thuộc phần cứng thật vẫn ghi chưa kiểm chứng và **không đánh dấu PASS**.
- HEAD nền `1b8491d19e76b72f541e277bb95cc7506a5c9939` (`main = origin/main`), Git sạch đầu phiên. Không đổi Kotlin production, Supabase, Auth/Cloud hay bản quyền online. Chỉ bổ sung công cụ QA trong `scripts/generate-release-codec-fixtures.py`, `scripts/run-release-playback-smoke-v1.ps1` (`-FixtureMode codec`), `scripts/run-release-readiness-audit-v1.ps1`.
- **Release codec smoke Android API 30 PASS:** hai tệp tổng hợp nguyên bản MP3 và FLAC 40 giây, MediaStore nhận thời lượng thật + title/artist/album, UI Library phát MP3, MediaSession Pause/Resume/Next chuyển sang FLAC, phát nền + MediaStyle; `QA_CLEANUP=PASS` và `FINAL_RESULT=PASS`. Log `qa-logs/release-codec-smoke-api30-v1.log`; SHA-256 APK QA `ACE9AD765F75C7939C513210E2BBC1555F95E6504636303B423FB70598C2E750`. **WAV backcompat trên APK release API 30 cũng chạy lại PASS** trong `qa-logs/release-playback-smoke-wav-backcompat-v1.log`; log WAV cũ được khôi phục nguyên trạng. Không sửa điều kiện offline/online rights.
- **Release artifact/JVM audit PASS:** Gradle `assembleRelease + bundleRelease + testDebugUnitTest` BUILD SUCCESSFUL; **105/105 JVM PASS**, APK release không debuggable, package/version/min/target đúng hiện tại; AAB có manifest+DEX. `qa-logs/release-readiness-v1.log` có `FINAL_RESULT=PASS` **chỉ cho audit**, `PRODUCTION_RELEASE_READY=FALSE`. AAB chưa ký SHA-256 `3E3DCE6793CF08A6653C7C1EF9C8E22B2E2265CBA797F37F7E947034D832E88C`.
- Vẫn CHƯA release chính thức: `applicationId=com.example.musicflow` chưa chốt, versionCode=1/name=1.0 chưa có chiến lược, production signing/Play Signing chưa có, `optimization.enable=false` (R8 tắt), chưa chọn kênh phân phối. Không dùng khóa thật, không upload APK/AAB. Generator dùng FFmpeg dev-only trong `app/build/rc-qa` bị Git ignore.
- **Kế tiếp:** RC QA/Packaging Part 5 — R8, kích thước và startup trên emulator, chuẩn bị hướng ký/phân phối khi ID/kênh được chốt; không thêm kiểm thử máy thật. Email/Google/cloud hai thiết bị tiếp tục ở cuối. Báo cáo `docs/Release_Candidate_QA_Packaging_v1_Part_4.md`.

### 2026-11-01 — Release Candidate QA / Packaging v1 Part 3 — MULTI-API RELEASE SMOKE PASS

- HEAD nền `b5724143699173c97e700169e3c3cb4f9a00527b` (`main = origin/main`), Git sạch đầu lượt. **Không thay production Kotlin, Supabase, Auth/Cloud hay rights enforcement**. Test dùng APK release-mode ký bằng debug keystore chỉ phục vụ QA; cùng SHA-256 `5D28135ABA17EC333E30FB3064AFCDFF3EB317FAA73EF7F25CF7F254D1A85B6C` trên API 29/33/36.
- **API 29 PASS / API 33 PASS / API 36 PASS**: mỗi API tạo 2 WAV local QA, MediaStore indexed, UI Library phát bài A (MediaSession state PLAYING), Pause/Resume/Next chuyển sang B, phát nền và MediaStyle, `QA_CLEANUP=PASS`; ba log `qa-logs/release-playback-smoke-api29-v1.log`, `api33-v1.log`, `api36-v1.log` đều `FINAL_RESULT=PASS`. API 30 cùng phạm vi đã PASS ở Part 2.
- **Hai sự cố đã phân loại:** AVD API29 ban đầu có sẵn MusicFlow nên script dừng an toàn trước khi cài/gỡ, lưu log blocked; chuyển sang AVD QA sạch. API36 vòng đầu báo FAIL do `dumpsys` trả `state=PLAYING(3)` thay vì `state=3`, trong khi playback thực sự PLAYING và metadata đúng; cập nhật **chỉ parser QA** cho cả số và tên trạng thái, chạy lại API36 full smoke PASS. Lưu log FAIL gốc + diagnostics trích đoạn; không quy kết là bug playback.
- Cả ba AVD QA mới do lượt này tạo đã được gỡ sau kiểm thử; 4 AVD người dùng giữ nguyên. Script cập nhật `scripts/run-release-playback-smoke-v1.ps1`, tạo log riêng theo API, không ghi đè log API30 đã commit, không xóa app có sẵn.
- `:app:bundleRelease` **BUILD SUCCESSFUL**, tạo `app/build/outputs/bundle/release/app-release.aab` 10.934.255 byte, SHA-256 `CAACCB7D022B1D5B90273B75726BEA45DDDCA1AF0DE845CC50B3D8FE35E44738`; `jarsigner` xác nhận **AAB unsigned**. Chưa production signing, chưa chốt package ID `com.example.musicflow`/versionCode=1/versionName=1.0, release optimization hiện tắt. Không công bố APK/AAB.
- **Giới hạn:** đây chỉ là release-mode smoke, **không phải full 115-case release suite hay máy thật**. Chưa QA MP3/FLAC metadata thật, Bluetooth/tai nghe/gapless audio, ANR/hiệu năng thư viện lớn, chữ ký production, Google Login/email thật/cloud hai thiết bị, licensed online thật. Chi tiết `docs/Release_Candidate_QA_Packaging_v1_Part_3.md`.
- **Kế tiếp:** RC QA / Packaging Part 4 — Release Readiness và checklist máy thật/codec, production signing/version/applicationId và kế hoạch tối ưu hóa. Giữ email/Google/cloud E2E cho cuối.

### 2026-11-01 — Release Candidate QA / Packaging v1 Part 2 — RELEASE-MODE PLAYBACK API 30 PASS

- HEAD nền `00f2d41c36bceb422caadcff34354390c7c6c2f5`, Git sạch lúc bắt đầu. Không sửa code Kotlin production, Supabase, kiểm tra bản quyền online hay tài khoản. Script `scripts/run-release-playback-smoke-v1.ps1` tự build assembleRelease, zipalign + ký QA bằng Android Debug keystore, verify signature v3 rồi dùng APK release-mode thật; SHA-256 `7A6E18284BB2923DF97A1C29B74CB721CEB2164F4A601367794D8E0E2DB13BDD`.
- Trên emulator Pixel_5 API 30 tạo **hai WAV 40 giây thật** trong thư mục riêng do QA sở hữu, công bố đúng hai row MediaStore; nhấn thật Home → Library → bài A. MediaSession `state=3` và metadata đúng A. Android media key Pause `state=2`, Resume `state=3`, Next phát B `state=3`; nhấn Home còn phát ở nền, notification MediaStyle của MusicFlow xuất hiện. Tất cả marker PASS.
- Script từ chối đè ứng dụng đã cài sẵn, chỉ xoá app, tệp WAV, row MediaStore và XML dump do chính lần QA tạo; `QA_CLEANUP=PASS`. Trong lần đầu helper chạy PowerShell 5.1 đọc sai Unicode khi script thiếu UTF-8 BOM, đã sửa encoding và chạy lại thành công. Lượt PASS sau khi bổ sung dọn XML vẫn đạt `FINAL_RESULT=PASS`. Log: `qa-logs/release-playback-smoke-v1.log`, báo cáo `docs/Release_Candidate_QA_Packaging_v1_Part_2.md`.
- **Giới hạn:** Đây là smoke release QA trên API30, không phải full 115 Android tests trên APK release; chưa điện thoại thật, chưa API29/33/36 release, chưa ký production, chưa Audio Bluetooth/gapless quality, chưa tài khoản Google thật/cloud đa thiết bị/nhạc online cấp quyền. Chỉ file online do MusicFlow cung cấp mới cần quyền streaming; offline người dùng tự lưu nghe bình thường.
- **Kế tiếp:** RC QA / Packaging Part 3 — release smoke đa API 29/33/36 và cổng phát hành artifact thật. Chưa phát hành.

### 2026-11-01 — Release Candidate QA / Packaging v1 Part 1 — INTERNAL QA PASS

- HEAD nền `818be079c29312afd5b627267eef5023a2cec11f` (`main = origin/main`, Git sạch lúc bắt đầu). `app/build.gradle.kts` có `versionCode=1`, `versionName=1.0`, `applicationId=com.example.musicflow`, `minSdk=29`, `targetSdk=36`; **chưa có signingConfig release**, optimization đang tắt. Không thay code production hoặc cấp quyền online.
- `assembleRelease` + `lintVitalRelease` **BUILD SUCCESSFUL**, APK gốc **chưa ký**. Dùng local Android Debug keystore để `zipalign` + `apksigner` tạo APK release-mode ký QA **chỉ dùng nội bộ**, chữ ký APK v3 được xác minh và trùng certificate với APK debug. Không tạo/chạm khóa ký production, không phân phối APK.
- Trên `emulator-5554`, API 30, xác nhận ban đầu **không có MusicFlow cài sẵn**. Cài debug baseline, tạo internal marker QA, nâng cấp tại chỗ sang release-mode QA APK, cold-launch MainActivity và có PID, khôi phục debug baseline để đọc lại đúng marker; **upgrade giữ data marker PASS**. Gỡ baseline QA, cài mới APK release-mode QA và khởi động **PASS**; gỡ toàn bộ gói do QA cài, giữ lại trạng thái emulator ban đầu.
- APK QA trong `app/build/rc-qa/MusicFlow-1.0-qa-signed.apk` (được .gitignore), **SHA-256 `0A5F7ED7660EAFB4D111C4489F34B631A2D248C6C790FD50A30E59858171FB36`**, 30.230.097 byte. Script tái hiện `scripts/run-release-candidate-packaging-v1.ps1`; log `qa-logs/release-candidate-packaging-v1.log` có `FINAL_RESULT=PASS`. Report `docs/Release_Candidate_QA_Packaging_v1_Part_1.md`.
- Phạm vi chưa đạt: APK chưa ký production và chưa chốt applicationId/version strategy; mới smoke cài/launch/marker trên API 30, **chưa test playback đầy đủ trên release APK** hoặc thiết bị thật, đa API, AAB/Play Signing, Auth/Cloud E2E, hay licensed online thật. Không đánh dấu sẵn sàng phát hành Google Play.
- **Kế tiếp:** RC QA / Packaging Part 2 — offline/playback QA trên release-mode APK, mở rộng ma trận API và thiết bị thật, chiến lược production signing và version/app ID. Google Login/email thật và cloud 2 thiết bị vẫn ở cuối như đã chốt.

### 2026-11-01 — Full Product Regression Gate v2 / Part 1 — 105 JVM + 112 ANDROID API 30 PASS

- HEAD nền `87efadd` (`main = origin/main`), Android emulator `emulator-5554` / Pixel_5_API_30, API 30. Không thay production Kotlin, Supabase, online rights, cloud hay tài khoản.
- Lượt full unfiltered đầu tiên: **105 JVM PASS**, **110 Android PASS / 2 FAIL / 3 SKIP** (115 total), `FINAL_RESULT=FAIL`. Hai FAIL thuộc `RealPickerSafTest`, thông báo `Save not found` từ DocumentsUI CreateDocument. Giữ log ở `qa-logs/full-product-regression-v2-part1-initial-failure.log`.
- Emulator từng có hộp thoại hệ thống `Bluetooth keeps stopping` che DocumentsUI; đã đóng hộp thoại và kiểm tra UI. Trên API 30 nút `SAVE` thật có id `android:id/button1`, đôi khi accessibility báo bounds rỗng. Cập nhật test helper để ưu tiên id này, giữ xác nhận ActivityResult + URI thật và fallback chạm màn hình chỉ khi nhận diện đúng native SAVE. Không suy diễn lỗi playback từ lỗi UI automation.
- Chạy lại hai bài SAF từng lỗi bằng APK AndroidTest mới: **2/2 PASS**, log `qa-logs/full-product-regression-v2-part1-saf-fix.log`.
- Chạy lại **toàn bộ** `scripts/run-full-regression-v2-part1.ps1` không lọc: **Gradle build/JVM PASS (105/105), Android 112 PASS / 0 FAIL / 3 SKIP (115 total)**; `ANDROID_UNEXPECTED_SKIP=0`, `GRADLE_ANDROID_EXIT=0`, `GIT_DIFF_CHECK_EXIT=0`, `FINAL_RESULT=PASS`. Log `qa-logs/full-product-regression-v2-part1.log`, chi tiết `docs/Full_Product_Regression_Gate_v2_Part_1.md`.
- Ba bài SKIP duy nhất là `SafPermissionRestartTest` theo pha riêng đã kiểm thử trước đó trong `docs/Android_Compatibility_Matrix_v2.md`; không tính thành PASS của lượt này. Kết quả PASS giới hạn ở Android instrumentation API 30, không phải nghiệm thu hoàn chỉnh MusicFlow 1.0.
- **Kế tiếp:** Release Candidate QA / Packaging — Part 1. Licensed Streaming Smoke phải đợi nội dung thật có quyền; Google/email thật và cloud E2E hai thiết bị để cuối.

### 2026-11-01 — Advanced Search Runtime Acceptance v1 — JVM 105 + 14/14 ANDROID API 30 PASS

- HEAD nền `a3055de` (Advanced Library v1 đã commit/push), Git sạch lúc bắt đầu. Chỉ thêm Android test + QA script/log + tài liệu; không thay Kotlin production, quyền streaming hay Supabase/Auth/Cloud.
- `AdvancedSearchRuntimeTest.kt` publish **3 WAV 35 giây vào MediaStore thật**, hai album và ba title **Tình Yêu / Đường Về / Khúc Nhạc** có dấu; test `SearchRoute`/SearchViewModel thực tìm **tinh yeu / duong ve / khuc nhac** không dấu, phát audio offline MediaSession, thêm cuối/chèn tiếp, xác nhận snapshot/Next phát bài vừa chèn. Bài thứ hai kiểm tra nhóm Album và phát nhóm, nhóm fallback Artist/Genre, no-results UI.
- Test build lượt đầu FAIL vì import `onNode` không tồn tại, đã sửa. Android lượt đầu bài nhóm Album FAIL do trùng text ở ô query và album result; selector được sửa `onAllNodesWithText(...).onLast()`, chạy lại **2/2 Android Search PASS**. Không có thay đổi source production.
- `scripts/run-advanced-search-runtime-v1-tests.ps1` + `qa-logs/advanced-search-runtime-v1.log`: **105/105 JVM PASS**, Search 2/2 với **13/13 marker**, PlaybackService 3/3 + QueuePersistence 5/5 + QueueManagerIntegration 1/1 + QueueManagerScreen 2/2 + OfflineAcceptanceFlow 1/1, **12/12 regression**, tổng **14/14 Android API 30 PASS**, Git diff check PASS, `FINAL_RESULT=PASS`.
- Giới hạn: Artist/Genre metadata của WAV thử là unknown/fallback, chưa tagged MP3/FLAC; route dựng Compose trực tiếp, chưa qua tab MainActivity; quyền READ_EXTERNAL_STORAGE cấp qua pm grant, chưa Android permission dialog; chưa điện thoại thật/multi API hoặc full unfiltered Android suite. Báo cáo `docs/Advanced_Search_Runtime_Acceptance_v1.md`.
- **Kế tiếp:** Full Product Regression Gate v2 Part 1 (toàn bộ Android suite API30 mới), sau đó sửa lỗi từng phần và Release Candidate QA/Packaging. Google/email thật và Cloud E2E hai thiết bị vẫn ở cuối, Licensed Streaming chỉ khi có nhạc đã được cấp quyền.

### 2026-11-01 — Advanced Library Runtime Acceptance v1 — JVM 105 + 14/14 ANDROID API 30 PASS

- HEAD nền `a4b045c`, Git sạch trước khi bắt đầu. Chỉ thêm test Android và script/log/tài liệu; không đổi production source, streaming rights hay Supabase.
- `AdvancedLibraryRuntimeTest.kt` tạo/publish **3 WAV 28 giây vào MediaStore thật**, hai file chung thư mục và file thứ ba ở thư mục khác. Kiểm tra cấp `READ_EXTERNAL_STORAGE` API 30 (dùng `pm grant` nếu cần), `MediaStoreLocalMusicRepository`/scanner trả đủ URI, ID, duration thực. Android tự gán hai `albumId` khác nhau theo hai thư mục.
- Bài 1: `LibraryRoute` thật quét/hiển thị ba bài sau publish, nhấn bài từ UI và xác nhận PlaybackService/MediaSession phát URI `content://` đúng. Bài 2: `LibraryScreen` nhận chính bài đã quét, **2 Album thật**, mở và phát toàn bộ album, mở/phát nhóm Nghệ sĩ và hiển thị nhóm Thể loại; `2/2 Android PASS`. **Lưu ý:** WAV không có tag nghệ sĩ/thể loại, MediaStore trả `Không rõ nghệ sĩ` và genre fallback; chưa nghiệm thu metadata artist/genre có tag.
- `scripts/run-advanced-library-runtime-v1-tests.ps1` + `qa-logs/advanced-library-runtime-v1.log`: Gradle/JVM **105/105 PASS**, Advanced Library **2/2** với 8/8 marker, PlaybackService **3/3**, QueuePersistence **5/5**, QueueManagerIntegration **1/1**, QueueManagerScreen **2/2**, OfflineAcceptanceFlow **1/1**, hồi quy **12/12**, tổng **14/14 Android API 30**, git diff check PASS, `FINAL_RESULT=PASS`. Test cleanup xóa WAV MediaStore QA và khôi phục queue/snapshot.
- Giới hạn: không xác minh hộp thoại xin quyền thủ công, metadata Artist/Genre tag thật, máy thật/đa API/điều hướng từ MainActivity tới nhóm hoặc full unfiltered Android suite. Báo cáo `docs/Advanced_Library_Runtime_Acceptance_v1.md`.
- **Tiếp theo:** Advanced Search Runtime Acceptance v1 với MediaStore/search không dấu/lọc nhóm, sau đó Release Candidate packaging và các hạng mục QA còn thiếu. Google/email thật và Cloud Sync hai thiết bị vẫn để gần cuối.

### 2026-11-01 — Playback Queue Actions Runtime Acceptance v1 — 105 JVM + 13/13 ANDROID API 30 PASS

- HEAD nền `8c39415`, Git sạch trước khi bắt đầu. Chỉ thêm `QueueActionsRuntimeTest.kt` và script/tài liệu, **không thay đổi production code** hoặc chính sách streaming nhạc offline/online.
- Hai bài Android dùng `LibraryScreen`/`SearchScreen` production, với `DeviceSong` WAV trong cache do test cấp, `PlayerViewModel.queueDeviceSong` và `PlaybackService`/MediaSession thực. Library **Phát tiếp** chèn vào ngay sau Alpha đang phát, Search **Thêm vào hàng đợi** chèn cuối; queue bốn bài và persisted snapshot giữ đúng thứ tự/vị trí, không pause hay reset, dùng Next thực sự phát bài mới chèn. Từ chối NEXT khi shuffle/repeat-one, item thiếu URI, và không tạo queue tự động khi rỗng.
- Lượt đầu FAIL ở guard shuffle do **race giữa hai MediaController** cập nhật bất đồng bộ; sửa bài test chờ trạng thái `PlaybackController.uiState` đồng bộ trước khi gọi enqueue, không nới điều kiện guard. Chạy lại **2/2 PASS**.
- `scripts/run-playback-queue-actions-v1-tests.ps1` + `qa-logs/playback-queue-actions-v1.log`: Gradle Build/JVM **105/105 PASS**, Queue Actions Android **2/2**, PlaybackService **3/3** + QueuePersistence **5/5** + QueueManagerIntegration **1/1** + QueueManagerScreen **2/2** = **11/11 regression**, tổng **13/13 Android API 30 PASS**, 7/7 marker, Git diff check PASS, `FINAL_RESULT=PASS`. Báo cáo `docs/Playback_Queue_Actions_Runtime_Acceptance_v1.md`.
- **Giới hạn:** UI thật nhưng dữ liệu DeviceSong thử được cấp trực tiếp, chưa đi qua `MainActivity` navigation, MediaStore quét thật, quyền Android hoặc đầy đủ Library/Search route; chưa máy thật/multi API/full Android suite. Không tuyên bố nghiệm thu 100% MusicFlow 1.0.
- **Tiếp theo:** Advanced Library Runtime Acceptance v1 theo từng mốc, sau đó Search Runtime v1; Google/email thật/Cloud E2E hai thiết bị vẫn ở cuối.

### 2026-11-01 — Crossfade UI & Interruption QA v2 — 105 JVM + 17/17 ANDROID API 30 PASS

- HEAD nền `6aa0549`, Git sạch khi bắt đầu. Tập trung riêng Crossfade UI và gián đoạn khi engine MIXING đang hoạt động; không đổi chính sách bản quyền online, Auth/Supabase hoặc cloud.
- `NowPlayingScreen.kt` thêm `Modifier.testTag("crossfade-duration-${duration}ms")`; `CrossfadeUiE2eTest.kt` mở MainActivity/MiniPlayer/Now Playing, cuộn viewport dọc, **performClick** Tắt/1/2/3/Tắt, xác nhận FilterChip selected và MediaSession nhận đúng custom command. **1/1 Android PASS**.
- `CrossfadeRuntimeTest.kt` mở rộng 4→**8 bài**: bên cạnh config, transition 0/1/2/3s và fallback 1.25x của v1, kiểm thử **pause / seek ngược / sửa queue / tắt Crossfade** ngay khi đang trộn. Trong cả bốn trường hợp, primary volume trở về 1, item/queue không mất/trùng, chuyển tiếp bình thường khi thích hợp. Có **4 log MIX_CANCELLED stage=MIXING**. `PlaybackCrossfade.kt` chỉ bổ sung log hủy; cleanup handoff hoàn tất ghi `MIX_CLEANUP` thay vì báo hủy sai.
- Script `scripts/run-crossfade-ui-interruption-v2-tests.ps1` + `qa-logs/crossfade-ui-interruption-v2.log`: Gradle/JVM **105/105 PASS**, Crossfade Runtime **8/8**, UI **1/1**, PlaybackService **3/3** + QueuePersistence **5/5**, **17/17 Android API 30 PASS**, marker runtime 17 + UI 5, Git diff check PASS, `FINAL_RESULT=PASS`. Báo cáo `docs/Crossfade_UI_Interruption_QA_v2.md`.
- **Giới hạn:** WAV PCM yên lặng, chưa đo âm thanh crossfade/gapless thực tế, chưa test physical device/đa API/full Android regression sau mốc; test UI Compose touch, chưa raw UiDevice tap. Không tuyên bố đã nghiệm thu Full MusicFlow 1.0.
- **Tiếp theo:** Playback Queue Actions Runtime Acceptance v1 với thư viện/tìm kiếm bài WAV offline; Google/email thật và Cloud E2E hai máy vẫn dời gần cuối.

### 2026-11-01 — Crossfade/Gapless Runtime Acceptance v1 — 105 JVM + 12/12 ANDROID PASS

- HEAD nền `36c55cc`, working tree sạch trước khi bắt đầu. Kiểm tra `PlaybackCrossfade`, `TransitionContract`, `PlaybackService` qua MediaController thật, nhạc WAV offline do QA tạo; không thay bản quyền online, Auth/Supabase/cloud.
- Bổ sung `CrossfadeRuntimeTest.kt` với 4 Android tests: GET/SET duration 0/1/2/3s và invalid; Crossfade tắt dùng Media3 forward transition; khi bật 1/2/3s qua real playback, **MIX_STARTED và HANDOFF_DONE đầy đủ cho từng mức**, queue/index/speed giữ đúng; khi speed 1.25x fallback chuyển bài thông thường.
- `PlaybackCrossfade.kt` chỉ thêm `durationMs` vào hai log MIX/HANDOFF có sẵn để kiểm chứng chính xác từng cấu hình; không thay đổi thuật toán volume hoặc Media3 chính. Test khôi phục queue, trạng thái phát và Crossfade preference sau kiểm thử.
- `scripts/run-crossfade-runtime-v1-tests.ps1` và `qa-logs/crossfade-runtime-v1.log`: Gradle Build/JVM **105/105 PASS**, Crossfade Android **4/4 PASS**, PlaybackServiceIntegration **3/3**, PlaybackQueuePersistence **5/5**, tổng **12/12 Android API 30**, 13/13 marker, `git diff --check` PASS, `FINAL_RESULT=PASS`. Đã sửa lỗi test đọc MediaController sai luồng; script QA lần đầu còn marker cũ và được sửa/chạy lại đến PASS.
- **Giới hạn:** hai WAV PCM thử là âm thanh yên lặng; chỉ có bằng chứng runtime/engine state, chưa đo gapless âm thanh hay chất lượng mix thật, chưa UI Crossfade E2E, thiết bị thật, đa API và full Android suite. Báo cáo `docs/Crossfade_Gapless_Runtime_Acceptance_v1.md`.
- **Tiếp theo:** Crossfade UI & Interruption QA v2 (mốc nhỏ), Google/email thật/Cloud hai thiết bị vẫn để cuối.

### 2026-11-01 — Playback Speed Touch Interaction QA v2 — 12/12 ANDROID + 105 JVM PASS

- HEAD nền `70704d3`, working tree sạch khi bắt đầu. Lỗi tồn đọng từ v1 không phải UI music speed bị hỏng: `performScrollTo` trên FilterChip nằm trong `horizontalScroll` không đưa hàng nút tới viewport dọc của Player. Cuộn tới heading `Hẹn giờ tắt nhạc` ngay dưới speed row rồi xác minh `assertIsDisplayed` mới chạm.
- Thêm test `physicalTouchOnSpeedChipsWorksAcrossHorizontalScroll` trong `PlaybackSpeedRuntimeTest`: lấy bounds trên màn hình từ Compose, `UiDevice.click` năm tốc độ **0.75/1/1.25/1.5/2x** và `UiDevice.swipe` ngang; MediaController thật xác nhận `playbackParameters.speed` sau mỗi chạm. Không dùng `SemanticsActions.OnClick` làm fallback cho bài physical touch.
- Kiểm tra cỡ chữ `font_scale=1.5`: `2.0x` ban đầu ngoài viewport (bounds rỗng), sau swipe ngang trở nên visible, chạm thật có hiệu lực; đánh dấu `horizontalOverflowRevealedByPhysicalSwipe`. Script luôn khôi phục font_scale qua finally. Lượt QA đầu gặp một sự kiện chạm đầu không ổn định sau đổi font, đã ghi FAIL và khôi phục; helper tăng ổn định bằng `waitForIdle` và tối đa 3 lần chạm Android thật. Lượt QA chính thức cuối PASS, mọi chạm được xác nhận từ attempt 1.
- `scripts/run-playback-speed-touch-v2-tests.ps1`, `qa-logs/playback-speed-touch-v2.log`: Gradle Build/JVM **105 PASS**, Speed Android **3/3**, Playback/Queue **8/8**, large-font physical touch **1/1**, tổng **12/12 Android API 30 PASS**, Git diff check exit 0, `FINAL_RESULT=PASS`. Báo cáo `docs/Playback_Speed_Touch_Interaction_QA_v2.md`.
- **Không thay production UI, player logic, bản quyền online, dữ liệu Supabase**. Chưa kiểm thử điện thoại thật/đa API/full instrumentation sau mốc, không tuyên bố MusicFlow 1.0 đã nghiệm thu.
- **Tiếp theo:** Crossfade/Gapless Runtime Acceptance v1, theo từng phần nhỏ; Auth/email/Google thật và Cloud E2E hai thiết bị dời cuối dự án.

### 2026-11-01 — Playback Speed Runtime Acceptance v1 — BUILD/JVM + 10/10 ANDROID API 30 PASS

- HEAD nền `3a59bc7`, working tree sạch khi bắt đầu. Đúng phạm vi Playback Speed, không thay Media3 logic, Supabase, online rights hoặc Auth/Cloud.
- Tìm và sửa lỗi biên dịch **có sẵn ở LyricsImportUiE2eTest** từ commit trước: thêm thiếu import `android.content.Context`; nhờ đó toàn bộ AndroidTest build trở lại.
- Thêm `PlaybackSpeedRuntimeTest.kt` với hai Media3 WAV offline thật: năm chip 0.75/1/1.25/1.5/2x cập nhật MediaSession và selected state; pause/resume và Next giữ 2x + queue, reset 1x; PlaybackController không nhận -1/0/0.49/2.01/NaN/+Inf, hai biên 0.5/2x hợp lệ. Cleanup tốc độ/queue/audio QA.
- Tăng khả năng xác định chip tốc độ qua `Modifier.testTag("playback-speed-${speed}x")`, không thay đổi giao diện. **Ghi rõ giới hạn:** test touch theo tọa độ `performClick` không kích hoạt chip ổn định khi nằm trong scroll dọc/ngang; kiểm chứng callback bằng `SemanticsActions.OnClick` (accessibility action) là PASS, chưa chứng minh chạm vật lý. MiniPlayer/Play/Pause/Next vẫn được bấm bằng `performClick` trong test.
- Script `scripts/run-playback-speed-runtime-v1-tests.ps1` + log `qa-logs/playback-speed-runtime-v1.log`: **105/105 JVM PASS**, **2/2 Speed + 8/8 Playback/Queue Android API 30 PASS**, 7/7 marker, Git diff check PASS, `FINAL_RESULT=PASS`. `docs/Playback_Speed_Runtime_Acceptance_v1.md` chứa bằng chứng, giới hạn, cleanup.
- **Kế tiếp:** Playback Speed Touch Interaction QA v2 để xử lý nốt kiểm chứng thao tác chạm trên các chip, sau đó Crossfade/Gapless Runtime Acceptance v1. Google/email thật và Cloud E2E hai thiết bị để gần cuối; Licensed Streaming Smoke chờ audio có quyền.

### 2026-10-29 — Lyrics Import UI End-to-End v2 — FULL PICKER→MAINACTIVITY→PLAYER PASS

- HEAD nền `6cf6d21` (Lyrics Runtime v1 đã được commit/push), Git sạch khi bắt đầu. Không thay production code, Supabase, Google Login hoặc cloud.
- Thêm `LyricsImportUiE2eTest.kt`: tạo LRC bằng DocumentsUI CreateDocument thật, nạp WAV offline Media3, nhấn MiniPlayer → Now Playing → **Nhập file .lrc**, dùng UIAutomator chọn thẻ tài liệu ở Android OpenDocument và bấm SELECT; xác nhận **callback MainActivity production** lưu URI đúng audio, persisted READ, lyrics tự hiển thị; MediaSession seek 1.4/3.4/6.4/1.6 giây làm highlight thay đổi đúng (cả tua ngược).
- Sửa lỗi duy nhất ở selector bài test: nhiều dòng Home/History/MiniPlayer trùng tên bài, nên chuyển sang `onNodeWithContentDescription` riêng của MiniPlayer. Không đổi hành vi ứng dụng. Script QA lần đầu nhận nhầm `OK (1 test)` là FAIL vì dùng chuỗi `OK (1 tests)`; sửa số ít/số nhiều và chạy lại đầy đủ.
- `scripts/run-lyrics-import-ui-e2e-v2-tests.ps1` + `qa-logs/lyrics-import-ui-e2e-v2.log`: Gradle Build/JVM **105/105 PASS**, Lyrics UI E2E **1/1 PASS**, Lyrics v1 **2/2 PASS**, Playback/Queue **8/8 PASS**, tổng **11/11 Android API 30**, 7/7 marker, `git diff --check` PASS, `FINAL_RESULT=PASS`. Có cleanup URI grant/document WAV/preferences/queue test.
- `docs/Lyrics_Import_UI_E2E_v2.md` ghi giới hạn: API30 emulator, chưa điện thoại thật/đa API/process kill, chưa chạy full connectedDebugAndroidTest sau mốc. Nhạc offline tự tải vẫn không cần xác minh quyền online.
- **Tiếp theo:** Playback Speed Runtime Acceptance v1 (mốc nhỏ) rồi các tính năng build-only còn lại; Google/email thật, cloud E2E hai thiết bị và licensed streaming thật vẫn chờ.

### 2026-10-29 — Runtime Feature Acceptance: Lyrics v1 — JVM 105/105 + ANDROID API 30 10/10 PASS

- HEAD nền `510de79` (Equalizer Runtime Acceptance đã push), Git sạch trước khi bắt đầu; tập trung Lyrics, không đổi phát nhạc offline hay chính sách quyền nội dung online.
- Đọc đường `LrcParser` → `LocalLyricsRepository` → `MainActivity` → `NowPlayingScreen`. Thêm `LyricsTimeline.activeIndex` dùng chung cho UI và test, và tag semantics `lyrics-active-line` cho dòng hiện hành. Nhập LRC nay **đọc/parse trước khi lưu URI grant**; tệp LRC invalid không giữ quyền SAF hay liên kết không cần thiết.
- `LrcParserTest.kt` thêm **6 JVM tests PASS** (timestamp, offset, BOM, nhiều timestamps, invalid, giới hạn text, seek timeline), tổng JVM **105/105 PASS**.
- `LyricsSafImportTest.kt`: DocumentsUI CreateDocument thật, URI `content://`, import vào repository, persisted READ grant, nạp lại liên kết audio và từ chối LRC không hợp lệ. Đã sửa test sử dụng `openOutputStream(..., "rwt")` thay vì `"w"` để **truncate** dữ liệu cũ của DocumentsProvider, tránh kết quả sai.
- `LyricsScreenSyncTest.kt`: NowPlaying Compose xác nhận dòng lyrics nổi bật khi seek **MediaSession thật** đến 1.2/3.3/6.2/1.8 giây; file WAV offline thử; cleanup và khôi phục trạng thái hàng đợi.
- `scripts/run-lyrics-runtime-v1-tests.ps1` + `qa-logs/lyrics-runtime-v1.log`: Gradle build/JVM **PASS**, Lyrics Android **2/2 PASS**, PlaybackService 3/3 và QueuePersistence 5/5, tổng **10/10 Android PASS**, 3/3 marker, Git diff check PASS, `FINAL_RESULT=PASS`. Tài liệu `docs/Lyrics_Runtime_Acceptance_v1.md`.
- **Giới hạn:** bài test SAF tạo file thật + repository import, chưa test một vòng trọn vẹn từ **nút `Nhập file .lrc` của MainActivity qua OpenDocument**; UI lyric nhận dữ liệu LRC test và vị trí MediaSession thật. Chưa toàn bộ Android suite sau mốc này, thiết bị thật, đa API.
- **Bước kế tiếp:** Lyrics Import UI End-to-End v2 (mốc nhỏ), sau đó các tính năng còn thiếu runtime QA; Google/email thật, Cloud E2E hai thiết bị và Licensed Streaming Smoke vẫn ở cuối hoặc phụ thuộc nhạc có quyền thật.

### 2026-10-29 — Runtime Feature Acceptance: Equalizer v1 — BUILD/JVM + 10/10 ANDROID API 30 PASS

- HEAD nền `38af246` (Sleep Timer v1 đã push), working tree sạch trước khi làm; chỉ tập trung Equalizer không đụng Lyrics, Auth/Supabase hoặc chính sách streaming.
- Đọc `EqualizerEngine`, `EqualizerContract`, `PlaybackService`: effect gắn audio session thật, lưu `enabled` và `band_N` vào SharedPreferences. Bổ sung `KEY_AUDIO_SESSION_ID` chỉ để MediaSession trả ID audio session hiện tại trong state bundle cho QA, không đổi cách nghe nhạc offline hay quyền online.
- Thêm `EqualizerRuntimeTest.kt`: phát WAV offline trên ExoPlayer/MediaSession thật, xác nhận EQ supported + sessionID dương + bands/range/frequencies hợp lệ; command invalid bị từ chối, bật/tắt và đổi gain được lưu và đọc lại, MediaController thứ hai cùng service nhận trạng thái; nhạc vẫn tiếp tục phát. Test khôi phục queue, gain/enable, SharedPreferences và xóa WAV QA.
- `scripts/run-equalizer-runtime-v1-tests.ps1` + `qa-logs/equalizer-runtime-v1.log`: Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL/JVM PASS**, Android API 30 **EqualizerRuntime 2/2**, **PlaybackServiceIntegration 3/3 + PlaybackQueuePersistence 5/5**, tổng **10/10 PASS**, 6/6 marker, `git diff --check` exit 0; `FINAL_RESULT=PASS`.
- Chưa xác minh audio frequency response bằng đo tín hiệu, chưa thực hiện UI EQ end-to-end, process/service restart thật và các API/thiết bị khác. Không tuyên bố full MusicFlow 1.0 PASS. Xem `docs/Equalizer_Runtime_Acceptance_v1.md`.
- **Kế tiếp:** Runtime Feature Acceptance — Lyrics v1 (SAF/LRC parser + timestamp sync/display + URI liên kết offline), Google Login/email thật/Cloud E2E hai máy giữ đến cuối.

### 2026-10-29 — Runtime Feature Acceptance: Sleep Timer v1 — BUILD/JVM + 11 ANDROID PASS

- HEAD nền `5350ce5`, Git sạch khi bắt đầu. Tập trung riêng Sleep Timer (không động EQ/Lyrics/online rights), giữ mốc hẹn người dùng **15/30/45/60 phút** và hủy.
- `PlaybackService.kt`: tái sử dụng logic timer với thời lượng millisecond, nhận `KEY_DEBUG_DURATION_MS` **chỉ nếu BuildConfig.DEBUG + controller nội bộ cùng package + minutes hợp lệ + thời gian 200..15000ms**. APK release không nhận override theo guard, đường production vẫn hẹn theo thời lượng phút thật; expiration pause nhưng không xóa queue.
- Thêm `SleepTimerRuntimeTest.kt` chạy MediaController/MediaSession thật với WAV offline: (1) invalid/set/query/replace/cancel, (2) expiry pause giữ queue/ID/index, (3) cancel trước deadline bảo đảm nhạc tiếp tục phát. Sau sửa lỗi test MediaController sai thread, **3/3 Android PASS** và 8/8 marker.
- `scripts/run-sleep-timer-runtime-v1-tests.ps1`, `qa-logs/sleep-timer-runtime-v1.log`: Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` PASS, Android API 30 **Sleep 3/3 + PlaybackServiceIntegration 3/3 + PlaybackQueuePersistence 5/5 = 11/11 PASS**, Git diff check PASS, `FINAL_RESULT=PASS`.
- Tài liệu `docs/Sleep_Timer_Runtime_Acceptance_v1.md` ghi rõ giới hạn: không chờ thật 15–60 phút, chưa test điện thoại thật/đa API/service restart với timer đang hoạt động; chưa làm UI Compose E2E. Nhạc offline tự tải luôn nghe bình thường, không kiểm tra quyền phát online.
- **Bước kế tiếp:** Runtime Feature Acceptance — Equalizer v1; sau đó Lyrics v1 riêng. Google Login/email thật, Cloud E2E hai thiết bị và Licensed Streaming Smoke đợi giai đoạn/cuộc thử phù hợp.

### 2026-10-29 — Full Product Regression Gate v1 - Part 3 — FULL ANDROID API 30 + JVM PASS

- HEAD nền `84fa5d2`, working tree sạch trước khi sửa. Chạy đầy đủ `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` và `:app:connectedDebugAndroidTest` trên `Pixel_5_API_30`, lấy kết quả từ JUnit XML mới (tránh XML cũ).
- Lượt 1: JVM **99/99 PASS**, Android **85 PASS / 1 FAIL / 3 SKIP** (89 total). Ba lỗi MVP Previous/SAF từ baseline Part 1 đã được giải quyết ở Part 2A/2B; phát hiện riêng test UI Playlist Reorder gặp race giữa `setContent` và Room Flow cùng hàng bài thứ 3 nằm ngoài vùng lazy list.
- Sửa `PlaylistPlaybackOrderTest` chờ danh sách tải và cuộn `performScrollToIndex` tới index 2/1/2 trước thao tác; thêm semantics testTag `playlist-detail-tracks` cho LazyColumn để test tìm được danh sách. Không đổi logic phát nhạc, database schema hay bản quyền online. Chạy riêng test **1/1 PASS** trên APK mới.
- Lượt đầy đủ sau sửa: Gradle/JVM **PASS 99/99**, Android API 30 **86 PASS / 0 FAIL / 3 SKIP** (89 total), `GRADLE_ANDROID_EXIT=0`, `FINAL_RESULT=PASS`. Ba SKIP là các pha `SafPermissionRestartTest` cần runner riêng, từng được kiểm tra ở Android Compatibility Matrix v2 trên API 29/30/33/36. Script `scripts/run-full-regression-v1-part3.ps1`, log `qa-logs/full-product-regression-v1-part3.log`.
- Kết quả xác nhận **full regression gate trên API 30**, không phải hoàn thành 100% MusicFlow 1.0. Còn Runtime QA Sleep Timer/EQ/Lyrics và các tính năng build-only, test thiết bị thật, release packaging, Google/email thật + cloud 2 thiết bị E2E để cuối, online audio phải có bản quyền.
- **Tiếp theo:** Runtime Feature Acceptance — Sleep Timer v1 (mốc nhỏ); sau đó QA Equalizer/Lyrics riêng.

### 2026-10-29 — Full Product Regression Gate v1 - Part 2B — REAL SAF PICKER ĐÃ SỬA, 2/2 PASS

- HEAD nền `0d47da9`; giữ lại thay đổi local có sẵn trong `RealPickerSafTest.kt`; không ghi đè/revert và không auto commit/push.
- Chẩn đoán đúng gốc lỗi với Android UIAutomator: DocumentsUI có tệp thật và sau long-click `selected=true`, nhưng nút hoàn tất là **SELECT** (`action_menu_select`), không phải Open. Thêm `androidTestImplementation(androidx.test.uiautomator:uiautomator:2.3.0)` để chạm card thực/confirm, kiểm tra selected title trước khi confirm; vẫn yêu cầu ActivityResult/URI thật và assertions Media3.
- QA Android 11 API 30: test chọn 1 file **PASS** (content URI, RIFF, persisted READ, MediaSession isPlaying); test chọn 2 file **PASS** (2 queue entries, persisted READ, next/Recent History). `run-full-regression-v1-part2b-saf-tests.ps1` chạy Build/JVM PASS, Android **2/2 PASS**, 9/9 marker, xác nhận picker returned 1 và 2, Git diff check PASS. Log `qa-logs/full-product-regression-v1-part2b-saf.log`, `FINAL_RESULT=PASS`.
- Một lượt đầu trong quá trình làm gặp timeout gián đoạn khi CreateDocument thứ hai; bài đó chạy riêng PASS, rerun chung **2/2 PASS**. Full Android 89 test baseline **chưa chạy lại**, không kết luận full product nghiệm thu.
- Chỉ thay QA source, không thay logic phát offline hoặc kiểm soát quyền nhạc online. **Kế tiếp Part 3**: chạy lại full connectedDebugAndroidTest/JVM API 30 để xác nhận cổng hồi quy.

### 2026-10-29 — Full Product Regression Gate v1 - Part 2B — SAF REAL PICKER BLOCKER, CHƯA PASS

- HEAD nền `0d47da9`; `RealPickerSafTest.kt` đã có chỉnh sửa local chưa commit khi bắt đầu, được giữ nguyên và phát triển tiếp, không reset/ghi đè thay đổi sẵn có.
- Kiểm tra Google DocumentsUI trên API 30 với file WAV test thực. Thử Accessibility click, tap vào tên, chạm card, shell input tap và long-press, hỗ trợ tìm nút Open và chuyển List view. UI có file, nhưng **không trả kết quả chọn tệp** cho app. Hai test real picker chạy riêng vẫn FAIL tại điều kiện chờ ActivityResult.
- Test không giả lập URI hay callback; log được làm rõ: hành động `TAP_SENT` không phải xác nhận `SELECTED`. Source production, nhạc offline, quyền bản quyền online không thay đổi. Gradle AndroidTest build PASS.
- Báo cáo `docs/Full_Product_Regression_Gate_v1_Part_2B.md`, log tóm tắt `qa-logs/full-product-regression-v1-part2b-saf.log`, trạng thái **FINAL_RESULT=FAIL**. Full suite baseline 89 Android tests **chưa được chạy lại** sau Part 2B.
- Bước tiếp theo vẫn **Part 2B**: nghiên cứu UIAutomator/selection thật của DocumentsUI, 2/2 PASS trước khi chạy lại full Android gate. Không tuyên bố nghiệm thu Full Product 1.0.

### 2026-10-29 — Full Product Regression Gate v1 - Part 2A — NOW PLAYING E2E FIX, 8/8 ANDROID PASS

- HEAD nền `a82fea3`; working tree sạch lúc bắt đầu. Tập trung duy nhất bài MVP Previous/Next trên Now Playing, chưa đụng đến SAF picker.
- Phát hiện nguyên nhân: `ExampleInstrumentedTest.mvpLibrarySearchToRealPlayer` click các nút ngoài viewport của màn cuộn dọc mà không `performScrollTo`. Instrument log xác nhận callback Previous được gọi khi nút hiện ra và MediaSession chuyển từ MVP Beta (2/2) về MVP Alpha (1/2); sau đó Next chưa scroll dẫn đến timeout tương tự.
- Sửa **instrumentation test**: trước mọi `Trước`, `Tiếp`, `Tạm dừng`, `Phát`, `‹ Trở về` đều `performScrollTo().performClick()`; xác nhận callback Previous đúng 1 lần và vẫn bắt buộc Player state thật. Không thay production Media3, business logic, nhạc offline hoặc online rights.
- QA `scripts/run-full-regression-v1-part2a-playback-tests.ps1` trên API 30: Gradle Build + JVM PASS; `ExampleInstrumentedTest` 3/3, `PlaybackServiceIntegrationTest` 3/3, `PlaybackSkipRecoveryTest` 2/2, tổng **8/8 Android PASS**, 5/5 marker MVP. Log `qa-logs/full-product-regression-v1-part2a-playback.log`, `FINAL_RESULT=PASS`, git diff check PASS.
- Chưa chạy lại unfiltered 89 Android tests trong phần 2A, nên **Full Product Regression Gate vẫn FAIL/CHƯA NGHIỆM THU** cho tới khi Part 2B sửa hai bài `RealPickerSafTest` (test còn yêu cầu người chọn tài liệu thủ công) và chạy lại toàn bộ suite. 3 SKIP phase-specific SAF của Part 1 có test riêng ở Compatibility Matrix v2.
- Bước kế tiếp: **Full Product Regression Gate v1 - Part 2B: Real OpenMultipleDocuments SAF**.

### 2026-10-29 — Full Product Regression Gate v1 - Part 1 - Baseline Android FAIL, JVM PASS

- HEAD nền `23c4835`, working tree sạch trước khi làm. Theo yêu cầu chủ dự án **chia Full QA thành các phần nhỏ**, không cố chạy toàn bộ Release QA hoặc thay đổi nhiều module cùng lúc.
- `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL**; Gradle XML **99/99 JVM PASS, 0 fail/skip** (15 suites).
- `:app:connectedDebugAndroidTest` trên emulator Android 11 / API 30: Gradle báo FAIL; JUnit XML có **89 total / 83 PASS / 3 FAIL / 3 SKIP**. Ba lỗi: `ExampleInstrumentedTest.mvpLibrarySearchToRealPlayer` (timeout sau Previous), hai bài `RealPickerSafTest` (test chờ người chọn tài liệu thủ công). Bài MVP **chạy riêng vẫn FAIL**, cần kiểm tra logic thật, chưa kết luận lỗi app/test. Ba SKIP `SafPermissionRestartTest` là pha yêu cầu runner flags, từng PASS riêng ở matrix v2; không tự coi lần generic run là xác minh restart.
- Thêm `scripts/run-full-product-regression-v1-part1.ps1`: đếm pass/fail/skip dựa trên JUnit XML, không hardcode test count; có `-AnalyzeOnly` để đọc lại XML mà không chạy device; luôn trả kết quả FAIL nếu có regression fail. Log `qa-logs/full-product-regression-v1-part1.log`, **FINAL_RESULT=FAIL** là kết quả đúng, không được báo PASS.
- Giữ nguyên production source và quyền nghe offline tự tải; Supabase/email thật/Google Login không được đụng tới. `docs/Full_Product_Regression_Gate_v1_Part_1.md` ghi bằng chứng và giới hạn.
- **Kế tiếp Part 2A:** chẩn đoán MVP Previous, sửa/test riêng; **Part 2B:** hoàn thiện tự động thao tác OpenMultipleDocuments thay vì đợi thủ công; sau đó mới chạy lại toàn bộ Android suite.

### 2026-10-29 — Android Compatibility Matrix v2 — 104/104 ANDROID INSTRUMENTATION PASS

- HEAD nền `d199bbb` (Offline End-to-End Acceptance v1 đã push), working tree sạch trước khi thực hiện; mốc này chưa tự commit/push.
- 4 emulator AVD có sẵn: `Pixel_5_API_29`, `Pixel_5_API_30`, `Pixel_5_API_33`, `Pixel_7_API_36`; API thực được xác nhận bằng `ro.build.version.sdk`. Giữ emulator API 30 đang dùng, chạy các API khác bằng serial riêng và dọn emulator QA sau cùng.
- Thêm `scripts/run-android-compat-matrix-v2-device.ps1`: chạy trên serial được chỉ định, kiểm tra API/boot, cài APK, 18 bài offline/Media3/Playlist/Queue Persistence + 9 E2E markers, rồi chạy 4 pha restart thực và 4 pha SAF (prepare/verify sau force-stop và cleanup), kiểm tra marker và PID đổi thật. Không dùng số lượng test hard-coded của script QA legacy.
- Kết quả `qa-logs/android-compat-matrix-v2-api-{29,30,33,36}.log`: **từng API 18/18 offline core, 4/4 restart, 4/4 SAF, FINAL_RESULT=PASS**; tổng **104/104 PASS**. Điểm mạnh: quyền SAF còn sau force-stop, file yêu thích SAF phát được; playlist/history/queue khôi phục trong tiến trình mới và không tự phát trái ý muốn.
- API 36 lượt SAF đầu **FAIL** vì `System UI isn't responding` che DocumentsUI; cây accessibility ghi rõ hộp thoại Android, không suy diễn là lỗi MusicFlow. Cập nhật `SafPermissionRestartTest.kt` để nhận diện nút `android:id/aerr_wait`, chờ System UI hồi phục, hỗ trợ resource-id Save, tăng timeout API36, log cây UI khi fail; **phải nhận URI + persisted grant thật mới PASS**. Sau sửa, chạy lại full API36 **26/26 PASS**. API29/30/33 đã PASS trước sửa QA helper; production Android source, MediaSession và kiểm soát quyền online không đổi.
- Tài liệu `docs/Android_Compatibility_Matrix_v2.md`. **Giới hạn:** chỉ representative regression 26/API, chưa full connectedDebugAndroidTest 4 API, chưa thiết bị thật/BT/lockscreen, chưa streaming thật; Google Login/email thật và cloud 2 thiết bị về cuối. Nhạc offline người dùng tự tải **không cần** MusicFlow xác minh bản quyền.
- Bước tiếp theo: **Full Product Regression Gate v1** và sau đó Release Candidate QA.

### 2026-10-29 — Offline End-to-End Acceptance v1 — BUILD/JVM + 22 ANDROID API 30 PASS

- HEAD nền `e6cd180` (Licensed Content Review & Publish v2 đã push), Git tree sạch trước khi sửa. Không thay đổi production playback/online rights, chỉ thêm test E2E, điều chỉnh đồng bộ thao tác UI của test cũ, script QA và tài liệu; thay đổi mốc này chưa tự commit/push.
- `OfflineAcceptanceFlowTest.kt`: tạo 2 tệp WAV kiểm thử lên MediaStore, quét nhạc qua `DeviceMusicScanner`, thêm vào Playlist Room, phát qua MediaSessionService, điều khiển bài tiếp, xác nhận lịch sử, tạm dừng/seek, snapshot và restore vào ExoPlayer mới; kiểm tra 9 marker bắt buộc. Dọn tệp/playlist/history test và phục hồi snapshot trước test. Không cần Supabase hoặc metadata bản quyền cho nhạc offline tự tải.
- Bài service UI regression `mvpHomePlaylistNowPlayingThroughMediaSession` phát hiện chạm nút `‹ Trở về` ngoài viewport ngay sau nút `Tiếp` khiến test không vào Home; thêm `performScrollTo` trước click và đợi semantics Home xuất hiện. Chạy lại **3/3 service UI tests PASS**, gồm History UI và phát lại.
- `scripts/run-offline-e2e-acceptance-v1-tests.ps1` chạy Gradle build/JVM, cài APK, 3 lượt Android instrumentation độc lập: core 10/10, Service UI 3/3, Media compatibility 9/9; yêu cầu đủ 9 marker logcat của E2E, `git diff --check`. Log `qa-logs/offline-e2e-acceptance-v1.log`, `TOTAL_ANDROID_TESTS=22`, `FINAL_RESULT=PASS`.
- **Giới hạn:** bài E2E mới test restore bằng ExoPlayer mới, không thực sự force-stop/restart process, không có kiểm thử đầy đủ SAF picker/permission reboot/đa Android API trong mốc này; full product chưa nghiệm thu. Chi tiết tại `docs/Offline_End_To_End_Acceptance_v1.md`.
- **Kế tiếp:** Android Compatibility Matrix v2 (API 29/30/33/36, SAF/permission/restart). Licensed Streaming Smoke giữ BLOCKED tới khi có nhạc có quyền phát thật; Google Login/email thật và cloud 2 máy E2E vẫn ở cuối.

### 2026-10-29 — Licensed Content Review & Publish v2 — SUPABASE + PYTHON + JVM BUILD PASS

- HEAD nền `2fe2194`, working tree sạch khi bắt đầu; chưa tự commit/push mã nguồn hiện tại. Nhạc offline người dùng tự tải **không** qua công cụ quản trị hoặc bộ lọc bản quyền này.
- Supabase `vzvxpbwrftlnyvuhfoqn`: **đã áp dụng** migration `musicflow_licensed_publish_review_v2`. RPC `public.musicflow_publish_licensed_track_v2` dùng **SECURITY INVOKER**, chỉ `service_role` được EXECUTE; thu hồi EXECUTE của `musicflow_publish_licensed_track` v1 với `service_role` và client roles. Không sửa RLS bảo vệ danh mục.
- RPC v2 yêu cầu full rights metadata/credit/source/streaming flag + reviewer và bằng chứng, kiểm tra SHA/path/URL trước khi cập nhật **cùng giao dịch** `music_tracks` và biên bản `music_uploads`. `rights_verified_at` được đóng dấu server-side `now()`; download và commercial-use là cờ riêng, mặc định FALSE.
- `scripts/musicflow_licensed_upload.py` nhận `--license-code`, `--license-url`, `--source-url`, `--attribution`, `--allow-download`, `--allow-commercial-use`, giữ `--license-reference`, `--reviewer`, `--confirm-rights`. Python kiểm tra metadata trước upload public; Storage không tự upsert/ghi đè tệp cũ; không nhúng admin secret vào repo.
- Xác nhận trực tiếp DB: RPC v1 (service_role/anon/authenticated) **không** còn EXECUTE; RPC v2 chỉ `service_role` được gọi. Chạy thử SQL `BEGIN ... ROLLBACK` trên bản ghi QA tạm: URL nguồn HTTP bị từ chối, dữ liệu quyền CC0/HTTPS hợp lệ cập nhật rights + receipt như mong đợi; sau rollback `music_tracks=0`, `music_uploads=0`. Security Advisor chỉ có INFO trước đây cho `music_uploads` không có client policy (cố ý).
- `python -m unittest discover -s scripts -p test_musicflow_licensed_upload.py -v`: **17/17 PASS** (trước là 8/8), biên dịch Python PASS. Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL**, JVM task PASS. Script `scripts/run-licensed-publish-v2-tests.ps1`, log `qa-logs/licensed-publish-v2.log`, `FINAL_RESULT=PASS`. Không rerun Android instrumentation vì mốc này không sửa source APK; kết quả Android 21/21 của mốc trước vẫn là bằng chứng riêng.
- Tài liệu `docs/Licensed_Content_Review_Publish_v2.md`, SQL đối chiếu `scripts/sql/musicflow_licensed_publish_review_v2.sql`; hướng dẫn v1 được đánh dấu là lịch sử. **Giới hạn:** chưa có audio thực được cấp phép, chưa thực hiện upload/public streaming E2E, rollback dữ liệu không kiểm chứng đối tượng Storage, có thể phát sinh orphan khi RPC lỗi mạng; không tuyên bố full-product PASS.
- Bước kế tiếp: **Licensed Streaming Smoke v1** khi có quyền và file thật; nếu chưa có, tiếp tục QA offline/E2E độc lập và ma trận Android theo phạm vi 1.0. Google Login/email thật vẫn để cuối.

### 2026-10-29 — Xác nhận phạm vi bản quyền: CHỈ ONLINE, OFFLINE TỰ TẢI PHÁT BÌNH THƯỜNG — BUILD + TEST PASS

- Chủ dự án xác nhận **chỉ nhạc phát từ catalog online MusicFlow mới cần quyền streaming đã xác minh**. Nhạc offline người dùng tự tải/tự lưu vào điện thoại vẫn phát bình thường qua MediaStore/SAF/file path, không cần đăng nhập, Internet, metadata giấy phép hay xét duyệt từ MusicFlow.
- Sửa `TrackMediaItems.isSafeStoredLocal` dựa trên **URI thực tế** (`content://`, `file://`, đường dẫn file tuyệt đối), không chặn chỉ vì `mediaId` bắt đầu `online:`. Cùng lúc tiếp tục chặn URL remote `http://`/`https://` đã lưu mà không có quyền được xác minh lại. Không đổi identity, Room schema hay Supabase policy.
- Android thêm `personalDownloadedOfflineFilesPlayEvenWithLegacyOnlineIds` (MediaStore, Downloads SAF, file URI và đường dẫn tuyệt đối), `downloadedOfflineTrackWithOldOnlineIdRestoresNormally` (queue snapshot offline với ID `online:`, khôi phục vị trí, không tự phát).
- Chạy lại `scripts/run-licensed-online-rights-v1-tests.ps1`: `BUILD_AND_JVM_EXIT=0`, `OnlineRightsPolicyTest` **10/10**, emulator API 30 **21/21 Android PASS**, `FINAL_RESULT=PASS`, log `qa-logs/licensed-online-rights-v1.log`. Đây là kết quả trên workspace, chưa auto commit/push.
- Phân biệt công cụ kiểm tra quyền của hệ thống với trách nhiệm nguồn tệp của người dùng: việc ứng dụng không kiểm tra nhạc offline không phải cho phép phân phối/tải trái phép. Bước tiếp theo vẫn **Licensed Content Review & Publish v2**; email/Google Auth thật để gần cuối.

### 2026-10-29 — Licensed Online Rights Metadata & Attribution v1 — SUPABASE + BUILD + JVM + ANDROID PASS

- HEAD nền `4f65879` (UI/UX Polish v2 đã commit), working tree sạch trước thay đổi; source mốc này chưa được tự commit/push.
- Supabase dự án `vzvxpbwrftlnyvuhfoqn`: **đã áp dụng** migration `music_tracks_rights_attribution_v1`; thêm 8 trường `license_code, license_url, attribution_text, source_url, streaming_allowed, download_allowed, commercial_use_allowed, rights_verified_at`. Defaults không cấp quyền; CHECK code giấy phép. RLS public SELECT yêu cầu published + rights confirmed + verified timestamp + streamingAllowed + HTTPS source/stream + license + attribution. Kiểm tra lại cột và RLS, có **0 bài nhạc**, vai trò anon/authenticated chỉ SELECT. Security Advisor có một INFO `music_uploads` thiếu policy theo thiết kế không cho client truy cập, không phát hiện mới cho `music_tracks`.
- Android: `OnlineTrackRights.kt` và `OnlineRightsJson.kt`, parse contract Supabase và external HTTPS catalog; `OnlineRightsPolicy` mặc định từ chối thiếu quyền, không suy diễn quyền tải từ quyền streaming; `TrackMediaItems.fromOnlineOrNull` + `OnlineCatalogScreen` kiểm tra quyền trước phát, hiển thị nguồn, ghi công, giấy phép và giới hạn tải.
- Không cho khôi phục/phát lại URL online cũ thiếu rights từ Room Playlist, recent history hoặc Queue Store. Queue cũ chứa nhạc local xen online sẽ giữ các bài local và đặt vị trí phát 0 nếu bài cũ bị từ chối; không tự phát.
- Build `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` PASS; `OnlineRightsPolicyTest` **10/10 JVM**. Emulator `emulator-5554` API 30: `OnlineRightsJsonTest` 4/4, `OnlineRightsScreenTest` 2/2, `TrackMediaItemsTest` 8/8, `PlaybackQueuePersistenceTest` 4/4, `QueueManagerIntegrationTest` 1/1 — **19/19 PASS**. Vòng đầu gặp 1 assertion nhãn UI sai, sửa và chạy lại PASS.
- Script `scripts/run-licensed-online-rights-v1-tests.ps1`, log `qa-logs/licensed-online-rights-v1.log`, marker `FINAL_RESULT=PASS`. Tài liệu `docs/Licensed_Online_Rights_Attribution_v1.md`; cập nhật cảnh báo hướng dẫn cũ `docs/Supabase_MusicFlow_v1.md` và `docs/Supabase_Licensed_Upload_v1.md`.
- **Giới hạn P0:** công cụ quản trị `musicflow_licensed_upload.py` / RPC publish v1 chưa đưa tám trường quyền mới nên publish theo flow cũ sẽ **không hiện online**, đây là fail-closed có chủ đích; chưa thử audio thật có quyền, CDN/cache, signed URL renewal, phát từ lịch sử online sau rights refresh. Không giả định đã nghiệm thu licensed streaming.
- **Kế tiếp:** Licensed Content Review & Publish v2; sau đó Licensed Streaming E2E Smoke với bài thật được chủ sở hữu cấp quyền. Google Login/email thật vẫn để cuối.

### 2026-10-29 — MusicFlow UI/UX Polish v2 — BUILD + JVM + ANDROID PASS

- HEAD nền `c31c6e1`; Git tree sạch trước khi sửa; chưa tự commit/push mốc mới.
- Now Playing: ảnh bìa 1:1 tối đa 300 dp, chuyển ba nút transport từ Row sang Column trên màn nhỏ/cỡ chữ từ 135%; nút cao tối thiểu 48 dp, Seek/EQ có nhãn TalkBack. Giữ MediaSession/playback callbacks.
- Playlist: dùng bảng trạng thái Loading/Empty/Error/removed playlist; tách action reorder/remove ra hàng riêng để tránh che tên bài khi chữ lớn, có nhãn xóa theo bài. Không đổi Room data/playlist order.
- Account: nút Auth responsive; theme chips cuộn ngang; đưa phần Giao diện lên gần đầu thay vì cuối trang để dùng cả ở Guest, nhất là viewport nhỏ; loading/error hiển thị nhất quán. Không thay đổi Auth backend hay bật Google.
- Vòng test đầu 18/19: `ThemeBatchTest` FAIL do thao tác click chip theme ở cuối màn trong vùng scroll lồng nhau bị che/tap không tới. Đưa lựa chọn theme lên đầu; giữ test thao tác click và khởi động lại; vòng cuối **19/19 PASS**.
- Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL**. Android emulator `emulator-5554` API 30: `MusicFlowUiPolishTest` 3/3, `PlaylistPlaybackOrderTest` 3/3, `MusicFlowUiFoundationTest` 10/10, `QueueManagerScreenTest` 2/2, `ThemeBatchTest` 1/1. Script `scripts/run-musicflow-ui-polish-v2-tests.ps1`, log `qa-logs/musicflow-ui-polish-v2.log`, `FINAL_RESULT=PASS`.
- Giới hạn: font scale 1.8 và vùng 310 dp là test mô phỏng, chưa làm QA TalkBack thật / ma trận Android API / điện thoại gập, không phải Full Product acceptance. Chi tiết `docs/MusicFlow_UI_UX_Polish_v2.md`.
- Bước tiếp theo: **Licensed Online Rights Metadata & Attribution v1**, giữ Google Login/email thật E2E ở cuối.

### 2026-10-29 — MusicFlow UI/UX Foundation v1 — BUILD + JVM + ANDROID PASS

- HEAD nền `3774b88` (`feat: unify media source identity and Media3 item factories v1`); workspace sạch trước khi chỉnh sửa; mốc UI/UX v1 **chưa commit/push**.
- Thêm `MusicFlowStatusPanel.kt` dùng chung với trạng thái Loading/Empty/Error/Permission/Info, thông điệp hướng dẫn và hành động tùy chọn, `liveRegion=Polite` và test tag; áp dụng trên Home History, Library/Favorites/Albums/Artists/Genres, Search, Online Catalog, Queue Manager.
- Bộ nhận diện màu **xanh ngọc / xanh đêm / hổ phách** trong `Color.kt`, `Theme.kt`. Search có nhãn TalkBack cho xóa query; Mini Player có nhãn mô tả bài và nghệ sĩ; các callback và luồng guest/offline không thay đổi.
- Phát hiện 2 nút `Tải lại` trùng trong Online Catalog qua test UI lần đầu; bỏ CTA trùng, tương tự nút `Chọn MP3` trong thư viện. Sửa `ThemeBatchTest` cũ dùng nhãn `Chế độ tối` không còn đúng (UI là `Tối`) và thêm cuộn để test thao tác thực.
- Script `scripts/run-musicflow-ui-foundation-v1-tests.ps1`, log `qa-logs/musicflow-ui-foundation-v1.log`, `FINAL_RESULT=PASS`. Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL**, toàn bộ JVM unit trong task PASS.
- Emulator `emulator-5554` Android API 30: `MusicFlowUiFoundationTest` **10/10 PASS** (trạng thái và callback, nhãn trợ năng, tỷ lệ tương phản primary), `QueueManagerScreenTest` **2/2 PASS**, `ThemeBatchTest` **1/1 PASS**; tổng **13/13 PASS**.
- Tài liệu `docs/MusicFlow_UI_UX_Foundation_v1.md`. **Giới hạn:** chưa review UI trực quan nhiều kích thước/font scale; TalkBack thật, đa API, toàn bộ màu tương phản và toàn hệ thống vẫn cần nghiệm thu; không tuyên bố MusicFlow 1.0 hoàn thành.
- Bước tiếp theo: **MusicFlow UI/UX Polish v2**, sau đó **Licensed Online Rights Metadata & Attribution v1** khi chuẩn bị nội dung online hợp pháp; email thật/Google Login E2E về cuối.

### 2026-10-29 — Core Media Source Contract v1 — BUILD + JVM + ANDROID PASS

- HEAD nền `2dc1d61` (Queue Manager v1 đã được chủ dự án push), working tree sạch trước khi sửa; thay đổi của mốc này chưa commit/push.
- Tạo `MediaSourceContract.kt` (Kotlin thuần): MediaStore, SAF, licensed online và legacy IDs; giữ `device-<id>`, URI picker, `online:<id>`, và IDs đã lưu byte-for-byte thay vì âm thầm thay khóa lịch sử/playlist/queue.
- Tạo `TrackMediaItems.kt`: một factory Media3 cho device, picker, online và persisted tracks; `PlayerViewModel`, `PlaylistMediaItems`, `PlaybackQueueStore` dùng chung adapter. Không thay đổi Room schema hoặc queue snapshot version.
- Online từ chối URL không có HTTPS host, có credentials hoặc fragment; vẫn cho signed query. Chọn bài theo index gốc, tránh chọn nhầm bài khi catalog có ID trùng/bản ghi không phát được. UI catalog chỉ cho chạm bài có URL phù hợp.
- JVM `MediaSourceContractTest` **11/11 PASS**: ID cũ, raw URI, explicit ID, SAF/online; Android `TrackMediaItemsTest` **5/5**, `QueueManagerIntegrationTest` **1/1**, `PlaybackQueuePersistenceTest` **3/3** trên emulator API 30, tổng **9/9 PASS**. `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` BUILD SUCCESSFUL. `scripts/run-media-source-contract-v1-tests.ps1`, log `qa-logs/media-source-contract-v1.log`, `FINAL_RESULT=PASS`.
- Tài liệu chi tiết `docs/Media_Source_Contract_v1.md`. **Giới hạn:** vẫn chưa có audio thật được cấp phép; online mediaId chưa namespace nhiều catalog, không tự refresh signed URLs hết hạn, chưa chứng minh nguồn remote E2E hay đầy đủ Android API matrix. Không sửa Google Login/E2E email thật.
- Bước tiếp theo: **MusicFlow UI/UX Foundation v1** — empty/loading/error/accessibility, sau đó licensed streaming smoke khi có nội dung hợp lệ.

### 2026-10-29 — Queue Manager UI v1 — BUILD + JVM + ANDROID PASS

- Giữ HEAD nền `69d3a51` và toàn bộ thay đổi chưa commit trước đó; không tự commit/push.
- Tạo `QueueEditRules.kt` độc lập Android: giữ bài đang phát, không cho di chuyển qua vị trí hiện tại, hạn chế reorder khi Shuffle.
- `PlaybackController`: thêm `queueItems`, `queueVersion` trong `PlayerUiState`; cập nhật theo timeline Media3; lệnh remove/move/clear kiểm tra MediaController permission và snapshot version để từ chối thao tác cũ.
- `QueueManagerScreen.kt`: xem hàng đợi, lên/xuống/xóa, xác nhận dọn; mở từ Now Playing; callbacks qua Navigation → MainActivity → PlayerViewModel → PlaybackController.
- Phạm vi được chủ dự án duyệt: offline + licensed online + cloud opt-in, Guest/Free/Premium, bản sắc riêng; Auth E2E về cuối và mở rộng sản phẩm sau 1.0.
- Build `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **PASS**. `QueueEditRulesTest` **8/8**, `QueueInsertRulesTest` **8/8**.
- Android emulator API 30: `QueueManagerIntegrationTest` **1/1** (move/remove/clear, stale version, media đang phát, snapshot persistence); `QueueManagerScreenTest` **2/2** (bảo vệ current item, callback version, xác nhận dọn). Script `scripts/run-queue-manager-v1-tests.ps1`, log `qa-logs/queue-manager-v1.log`, `FINAL_RESULT=PASS`.
- Giới hạn: dùng nút lên/xuống thay vì kéo thả; chưa instrumentation đa API hoặc force-stop restore thực tế cho Queue Manager. Full Product chưa nghiệm thu.
- Bước tiếp theo: **Core Media Source Contract v1** — thống nhất identity/factory cho MediaStore, SAF và licensed online; giữ tương thích playback/playlist/queue persistence.

### 2026-10-29 — Playback Queue Actions v1 + kiến trúc mở rộng — BUILD + UNIT PASS

- Quyết định chính thức mới: hoãn dùng email/tài khoản thật và Google Login E2E đến cuối giai đoạn Full Product; không xóa nền tảng Auth đã có.
- HEAD nền `69d3a51`; các chỉnh sửa Account/Google Security Hardening v2 từ bước trước vẫn nằm trong workspace, giữ nguyên; Queue Actions v1 chưa commit/push.
- Tách luật `QueueInsertRules.kt` / `QueuePlacement` (JVM pure), thêm `PlaybackController.enqueue` dựa trên Media3 `addMediaItem(index, item)`; kiểm tra `COMMAND_CHANGE_MEDIA_ITEMS`, không thay bài/vị trí hiện tại, không tự phát khi queue trống.
- Bổ sung `PlayerUiState.canEditQueue` để ẩn action khi MediaSession không có quyền sửa queue. **Phát tiếp** bị ẩn khi Shuffle hoặc Repeat One (vì không bảo đảm ngay sau bài hiện tại); **Thêm vào hàng đợi** vẫn cho phép thêm cuối playlist.
- `PlayerViewModel` dùng chung phương thức dựng `MediaItem` từ DeviceSong cho cả Play/Queue; nối callback từ MainActivity → Navigation → Library/Search, bao gồm nhóm Album/Nghệ sĩ/Thể loại và Favorites.
- JVM: `QueueInsertRulesTest` **8/8 PASS**, 0 failure/error. `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL**; chưa chạy Android instrumentation trên emulator. Script `scripts/run-playback-queue-actions-v1-tests.ps1`, log `qa-logs/playback-queue-actions-v1.log` ghi `FINAL_RESULT=BUILD_AND_UNIT_PASS`.
- Tạo `docs/Kien_Truc_Mo_Rong_Va_Lo_Trinh_Full_Product.md`: kiến trúc đích theo ranh giới, lộ trình chia giai đoạn, chiến lược mô-đun hóa dần; **không** tuyên bố đã tách modules.
- Giới hạn: chưa có UI quản lý toàn bộ queue, chưa có Android runtime test cho insertion/persistence sau restart, chưa xác nhận thao tác queue từ các thiết bị/controller ngoài.
- Bước tiếp theo: **Queue Manager UI v1** (xem/sắp xếp/xóa hàng đợi), giữ Google Login E2E ở cuối.

### 2026-10-29 — Account & Google Login Security Hardening v2 — BUILD + UNIT PASS

- HEAD nền `69d3a51` (`feat: add Google OAuth login foundation with PKCE`), workspace sạch trước khi sửa; mã v2 **chưa commit/push**.
- Sửa `app/src/main/res/xml/backup_rules.xml` (Android <= 11) và `data_extraction_rules.xml` (Android >= 12): loại riêng `musicflow_account_session_v1.xml` khỏi cloud backup và device transfer. Giữ nguyên backup dữ liệu khác.
- `GoogleOAuthFlow.kt`: từ chối callback có cổng ngoài dự kiến hoặc query quá dài; mở rộng test callback có duplicate flow, explicit port, oversized query, OAuth error và fragment.
- `AccountApi.kt` + `AccountViewModel.kt`: xóa Google PKCE pending nếu không khởi chạy được trình duyệt.
- Python tests `scripts/test_account_backup_rules.py`: **4/4 PASS** (legacy backup, cloud/device transfer, Manifest, tên SharedPreferences).
- Google JVM `GoogleOAuthFlowTest`: **12/12 PASS**, 0 lỗi. Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain`: **BUILD SUCCESSFUL**; chỉ biên dịch AndroidTest APK, chưa chạy trên emulator.
- Script `scripts/run-account-google-hardening-v2-tests.ps1`, log `qa-logs/account-google-hardening-v2.log`: `FINAL_RESULT=BUILD_AND_UNIT_PASS`. Lần chạy đầu lỗi **script QA** do PowerShell 5.1 coi stderr từ Python unittest là terminating error; sửa cách nhận stderr theo exit code, lần chạy lại PASS; không có bằng chứng lỗi Android app ở lần đầu.
- Tham khảo Android Auto Backup chính thức, OWASP Mobile Security và tài liệu Supabase Auth PKCE; chưa thay đổi Supabase schema/config.
- **Giới hạn:** chưa kiểm thử Android Backup restore thực tế; Google provider chưa có xác nhận bật, không có Google Login E2E; Full Regression cuối giai đoạn chưa chạy. Không tuyên bố ứng dụng hoàn thành.
- Bước tiếp theo: chủ dự án bật Google provider bằng credential riêng tại Supabase Dashboard, sau đó chạy Google Login E2E; nếu chưa cấu hình, tiếp tục một milestone Full Product không cần tài khoản thật.

### 2026-10-29 — Google Login Foundation v1 — Build-only

- HEAD nền `71944fb` (`feat: add Supabase listening history sync v1`), working tree sạch trước khi sửa.
- Đã xác minh Supabase Auth `/auth/v1/settings`: `external.google=false`, `external.email=true`. Không thay đổi cấu hình Google Cloud hoặc Google provider khi chưa có Client ID/Client Secret và quyền sở hữu.
- Android: `GoogleOAuthFlow.kt` và `AccountOAuthRedirects.kt`, thêm OAuth pending encrypted vào `AccountTokenStore`, exchange code thành phiên Supabase trong `AccountApi`, nhận deep link `MainActivity`, điều hướng sang Account, nút Google theo trạng thái provider. PKCE SHA-256 + nonce + expiration + one-time verifier; không lưu mật khẩu Google.
- Thêm JVM `GoogleOAuthFlowTest.kt`: known challenge vector, callback hợp lệ/sai nonce/hết hạn, URL nguồn lạ, mã code trùng.
- Tài liệu cấu hình `docs/Google_Login_Foundation_v1.md`; log dự kiến `qa-logs/google-login-foundation-v1.log`.
- Android app/AndroidTest build + JVM unit: **PASS**, `BUILD_AND_JVM_EXIT=0` và `GoogleOAuthFlowTest` **7/7 PASS, 0 lỗi**; log `qa-logs/google-login-foundation-v1.log`. **Chưa có Google Login E2E do provider đang tắt**; không tuyên bố tính năng đăng nhập thật đã hoạt động.
- Chưa commit/push; bước tiếp theo bật Google provider qua Dashboard và chạy Auth E2E; Full QA tổng thể vẫn để cuối.

### 2026-10-29 — History Sync v1 — BUILD + JVM PASS

- HEAD trước sửa: `8f66af4` (`feat: harden cloud sync with quotas relink and soft delete`); workspace sạch khi bắt đầu, đến nay chưa commit/push.
- Supabase migration `musicflow_cloud_recent_history_v1`: `cloud_recent_history` với RLS owner, chính sách Free 30 ngày/Premium còn hiệu lực, RPC `musicflow_merge_recent_history` security-invoker.
- Đã xác minh grants: `anon_select=false`, `anon_rpc=false`, `authenticated_select=true`, `authenticated_rpc=true`, `authenticated_delete=false`; có `0` bản ghi lịch sử cloud.
- Android thêm `CloudHistorySync.kt`, `CloudHistoryRules.kt`, `CloudHistoryRulesTest.kt`; merge Room history có giữ thời gian nghe mới hơn và tối đa 20 mục; nút đồng bộ lịch sử trong Account.
- Chỉ truyền metadata tên bài/nghệ sĩ/thời điểm, không truyền URI file cục bộ.
- Đã chạy Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain` thực tế: `BUILD SUCCESSFUL in 31s`, mã thoát `0`. `CloudHistoryRulesTest`: 4/4 PASS, 0 skipped.
- Log `qa-logs/history-sync-v1.log`, tài liệu `docs/History_Sync_v1.md`.
- Giới hạn: build + JVM test, **chưa chạy instrumentation hay E2E tài khoản thật/2 máy**. Không đồng bộ xóa cloud khi xóa local. Full QA vẫn để cuối.
- Bước tiếp theo: Google Login Foundation v1. MVP tổng thể CHƯA NGHIỆM THU.

### 2026-10-29 — Cloud Sync Hardening v2

- Tiếp tục trên workspace chưa commit của Cloud Sync v1 tại HEAD `924301d`; không ghi đè hoặc tạo commit/push tự động.
- Supabase: migrations `musicflow_cloud_sync_hardening_quotas_tombstones_v2`, `musicflow_cloud_favorite_tombstone_v2`. Trigger server-side giới hạn Free 20 playlist + 500 favorites, Premium 200 + 2500 khi còn hạn; soft-delete RPC và gộp playlist có bảo vệ tombstone.
- Android: UI trong tab Account để ghép các bài chưa khớp, xóa cloud có xác nhận mà không xóa dữ liệu local, phân trang cloud, cải thiện giới hạn phản hồi.
- Bảo mật: RPC chỉ `authenticated`, không cấp cho `anon`; dữ liệu theo owner RLS. Chưa tạo nhạc mẫu hay tài khoản thật.
- Tài liệu `docs/Cloud_Sync_Hardening_v2.md`. Build/log: `qa-logs/cloud-sync-hardening-v2.log`, kiểm chứng thực tế theo kết quả Gradle cuối.
- Còn thiếu: thử thực tế hai thiết bị, mở rộng UI tìm kiếm bài relink và kiểm chứng các race conditions phức tạp. Full QA vẫn để cuối.

### 2026-10-29 — Cloud Playlist & Favorites Sync v1

- HEAD nền: `924301d` (`feat: add Supabase account authentication and subscription foundation`), working tree sạch trước khi sửa.
- Supabase migrations: `musicflow_cloud_playlists_favorites_v1` tạo `cloud_playlists`, `cloud_favorites` với RLS owner; `musicflow_cloud_playlist_merge_rpc_v1` thêm RPC security-invoker, gộp metadata bài khi sync hai thiết bị và không cho anon gọi.
- Đã kiểm tra security grants: anon không đọc cloud tables, không gọi RPC; authenticated không DELETE cloud data. Test RPC với identity giả trong transaction, cập nhật playlist hai lượt, xác nhận 2 bài + tên mới rồi ROLLBACK; dữ liệu cloud không lưu test record.
- Android: `CloudLibrarySync.kt` + `CloudSyncRules.kt`; thêm merge idempotent vào `RoomPlaylistRepository` và `LocalFavoritesStore`, nút thủ công trong tab Account, trạng thái và kết quả sync; refresh Supabase Auth khi JWT hết hạn.
- Chỉ đồng bộ tên + metadata, không upload file hay URI cục bộ; tải về chỉ liên kết bài nếu MediaStore có một ứng viên duy nhất. Không xóa Playlist/Favorites khi merge; chặn dùng cùng local library để upload sang tài khoản khác sau lần đồng bộ đầu.
- JVM `CloudSyncRulesTest.kt`; build `:app:assembleDebug` + `:app:assembleDebugAndroidTest` + `:app:testDebugUnitTest` PASS, log `qa-logs/cloud-sync-v1.log`.
- Chưa xác nhận sync thực tế bằng tài khoản/2 thiết bị; không có cloud quota hay xóa đồng bộ; chưa Full QA.
- Tài liệu: `docs/Cloud_Playlists_Favorites_Sync_v1.md`. Bước tiếp theo: Cloud Sync Hardening v2 / Track Relink UX; sản phẩm CHƯA NGHIỆM THU.

### 2026-10-29 — Account & Subscription Foundation v1 — Build + JVM PASS

- HEAD nền: `e7446bc` (`feat: add secure licensed music upload and rights verification`), working tree sạch lúc bắt đầu.
- Supabase migration `musicflow_account_profiles_entitlements_v1` tạo `account_profiles` và `account_entitlements`; tất cả bật RLS, profile theo `auth.uid() = id`. Chỉ cho sửa `display_name` ở profile; người dùng chỉ SELECT entitlement của mình, không INSERT/UPDATE gói.
- Free mặc định nếu không có entitlement; Premium chỉ còn hiệu lực khi backend có row `premium` và `expires_at` chưa qua. Không kích hoạt thanh toán, không tự nâng quyền từ Android.
- Tạo `core/account/AccountApi.kt` + `AccountTier.kt`, `feature/account/AccountViewModel.kt` + `AccountScreen.kt`, thay tab Account placeholder bằng luồng Auth email/password/hồ sơ, vẫn có tùy chọn theme và Guest offline. Refresh token mã hóa Android Keystore, không đưa secret/service_role vào app.
- JVM `AccountTierTest` kiểm tra Free/Premium theo thời hạn; Supabase grants xác minh anon không đọc profile, authenticated không sửa gói hoặc id, chỉ sửa display_name.
- Build `:app:assembleDebug` + `:app:assembleDebugAndroidTest` và `:app:testDebugUnitTest`: PASS; log `qa-logs/account-subscription-foundation-v1.log`.
- Chưa có tài khoản đăng ký thử thực tế, chưa test email xác nhận, chưa Cloud Sync/Billing; không tuyên bố Auth E2E PASS. Full QA vẫn để cuối.
- Tài liệu: `docs/Account_Subscription_v1.md`; bước tiếp theo Cloud Playlist & Favorites Sync v1; Licensed Streaming Smoke chờ nhạc.

### 2026-10-29 — Licensed Content Upload + Rights Verification v1

- HEAD nền: `66ab690` (`feat: integrate Supabase licensed music catalog and streaming`); workspace sạch trước khi sửa.
- Supabase: tạo hai bucket `musicflow-private-drafts` (private) và `musicflow-public-audio` (public), giới hạn 50 MiB, định dạng audio có danh sách cho phép.
- Tạo `public.music_uploads` lưu staging path/hash/kích thước và bằng chứng xác nhận quyền; bật RLS, **thu hồi toàn bộ quyền anon/authenticated**; không có chính sách cho client. Supabase advisor thông tin `rls_enabled_no_policy` là chủ ý khóa bảng nội bộ.
- RPC `musicflow_publish_licensed_track` dùng SECURITY INVOKER, quyền EXECUTE chỉ dành cho `service_role`, atomically cập nhật receipt và trạng thái public catalog. Kiểm tra grant thực tế: anon không được SELECT receipt/EXECUTE RPC; service role có quyền.
- Công cụ quản trị `scripts/musicflow_licensed_upload.py` hỗ trợ `stage` riêng tư rồi `publish` khi đã xác nhận bản quyền, kiểm tra MIME/magic bytes, SHA-256, kích thước; chỉ nhận secret key trong biến môi trường máy quản trị, không trong Android/Git.
- Bộ kiểm tra offline `scripts/test_musicflow_licensed_upload.py`: **8/8 PASS**; ngăn giả file, không công khai trước xác nhận, từ chối key publishable làm admin, phát hiện thay đổi hash. CLI `--help` PASS trên Windows.
- Đã kiểm tra RPC bằng giao dịch SQL dùng role service_role, xác minh cập nhật cả hai bảng và ROLLBACK; Supabase không lưu nhạc giả. Grant: anon không đọc receipt, không gọi RPC; role service_role có quyền.
- Build `:app:assembleDebug` + `:app:assembleDebugAndroidTest`: PASS, build-only, không chạy instrumentation.
- Tài liệu triển khai `docs/Supabase_Licensed_Upload_v1.md`. Log `qa-logs/licensed-content-upload-v1.log`: `PYTHON_UNIT_EXIT=0`, `ANDROID_BUILD_EXIT=0`, `FINAL_RESULT=BUILD_AND_UNIT_PASS`.
- **Chưa upload hoặc phát nhạc online thực tế** vì không có file + giấy phép + secret key quản trị; không suy diễn runtime PASS. Full QA vẫn để cuối.
- Bước tiếp theo: khi có nhạc hợp lệ → Licensed Streaming Smoke; khi chưa có → Account/Auth Foundation v1. Sản phẩm CHƯA NGHIỆM THU.

### 2026-10-29 — Supabase backend / streaming foundation v1

- HEAD nền: `b310d9f`; không chỉnh dữ liệu nhạc offline đã có.
- Xác minh Supabase project `vzvxpbwrftlnyvuhfoqn` ACTIVE_HEALTHY; schema public ban đầu không có bảng. Tạo bảng `music_tracks` bằng migration `musicflow_licensed_catalog_v1`.
- Bật RLS, anon/authenticated chỉ có SELECT bài `is_published and rights_confirmed` và stream_url hợp lệ; không cho ghi từ client. Supabase security advisors: không có cảnh báo tại thời điểm kiểm tra.
- Lưu cấu hình Supabase ở `secrets.properties` (gitignored); Gradle BuildConfig đưa publishable key vào Android; tuyệt đối không dùng service-role key.
- Thêm `SupabaseCatalogRepository.kt`; hỗ trợ đọc REST catalog có phân trang. `OnlineCatalogViewModel` dùng Supabase khi build đã cấu hình.
- Thêm nút phát cho bản ghi có stream_url HTTPS; queue và MediaSession tiếp tục dùng ExoPlayer chung.
- Chưa thêm dữ liệu hoặc URL nhạc, do chưa có nội dung/quyền phát hợp lệ. Không có bằng chứng phát online thực tế; QA cuối giai đoạn.
- Tài liệu: `docs/Supabase_MusicFlow_v1.md`.
- Build debug + AndroidTest: chạy build-only; log `qa-logs/assemble-debug-supabase-v1.log`.
- Bước tiếp theo: nhập bản ghi nhạc được cấp phép rồi QA streaming; sau đó signed URL/Storage/Auth cho nội dung riêng tư.

### 2026-10-29 — Online Catalog Foundation v1 — Build PASS

- HEAD trước thay đổi: `036b504` (`feat: add advanced search for songs albums artists and genres`); thay đổi ở máy Windows, chưa commit/push.
- Thêm `core/online/OnlineCatalog.kt`: model metadata, interface repository, cài đặt URL gốc HTTPS và implementation bằng `HttpsURLConnection`. Không tích hợp URL nhạc hoặc nguồn không được cấp phép.
- Hợp đồng API: `GET /v1/catalog/tracks` có `items` và `nextCursor`; dùng `cursor` cho phân trang. Tài liệu `docs/Online_Catalog_API_v1.md`.
- Thêm `feature/online/OnlineCatalogViewModel.kt` và `OnlineCatalogScreen.kt`: trạng thái chưa cấu hình/đang tải/rỗng/lỗi, cấu hình máy chủ và tải thêm.
- Kết nối từ Home qua nút `Khám phá nhạc online` và Navigation3 `OnlineCatalogPage`; không chỉnh MediaSession/offline queue.
- Giới hạn: HTTPS, không theo redirect, timeout 10s, mỗi trang tối đa 100 bản ghi/1 MiB, không gửi token, không autoplay hay tải xuống.
- `:app:assembleDebug` + `:app:assembleDebugAndroidTest`: BUILD PASS; log `qa-logs/assemble-debug-online-catalog-v1.log` (build-only; không chạy instrumentation).
- Chưa có endpoint nhà cung cấp thực tế, chưa kiểm thử server/mạng hoặc quyền streaming; chưa Full QA. Sản phẩm tổng thể CHƯA NGHIỆM THU.
- Bước tiếp theo: Licensed Streaming Integration v1 sau khi có backend/API và quyền sử dụng nội dung; nếu chưa có backend, ưu tiên xây hợp đồng backend và dữ liệu quyền phát trước.

### 2026-10-29 — Search nâng cao v1 — Build PASS

- HEAD nền: `a2622f4` (`feat: add artist and genre browsing to local music library`).
- Giữ tìm kiếm bài hát hiện tại (không dấu tiếng Việt, debounce 150ms, kết quả bất đồng bộ, Playlist/Favorites) và nâng cấp màn Search thành bốn bộ lọc: Bài hát / Album / Nghệ sĩ / Thể loại.
- `AdvancedMusicSearch.kt` tạo kết quả theo nhóm từ một lần quét MediaStore, tìm theo tên nhóm có bỏ dấu; `SearchViewModel` cập nhật các nhóm kết quả cùng lúc và bỏ kết quả cũ khi đổi từ khóa.
- `MusicCollectionsContent.kt` thêm nhóm Album để tái sử dụng giao diện mở nhóm, Phát toàn bộ, chọn bài, yêu thích và Thêm Playlist trong Search.
- File thay đổi: `SearchScreen.kt`, `SearchViewModel.kt`, `SongSearch.kt`, `MusicCollectionsContent.kt`; file thêm: `AdvancedMusicSearch.kt`.
- Build ứng dụng và APK instrumentation: `:app:assembleDebug` + `:app:assembleDebugAndroidTest` PASS (`BUILD_EXIT_CODE=0`). Log: `qa-logs/assemble-debug-advanced-search-v1.log`.
- Phạm vi kiểm tra chỉ là biên dịch; chưa kiểm thử thao tác UI với thư viện thật, nhiều metadata, quyền và hiệu năng.
- Chưa commit/push. Bước tiếp theo: Online Catalog Foundation v1. Sản phẩm tổng thể CHƯA NGHIỆM THU.

### 2026-10-29 — Library Collections v1 — Build PASS

- HEAD nền: `0ea2510` (`feat: add experimental crossfade and gapless transition controls`).
- Duy trì tab Album, Favorites, Playlist và các luồng phát nhạc hiện tại.
- Bổ sung tab Nghệ sĩ và Thể loại vào Library; mở nhóm, xem bài và Phát toàn bộ hoặc chọn một bài; nút yêu thích và Thêm Playlist tái sử dụng `SongRow`.
- Mở rộng `DeviceSong`: artistId / genres, giá trị mặc định tương thích nơi tạo mẫu; `DeviceMusicScanner` đọc Artist ID và ánh xạ Genre → các bài qua MediaStore.
- Nếu bảng Genres của nhà cung cấp MediaStore không đọc được, thư viện bài hát vẫn hoạt động; bài không có thể loại được nhóm là `Không rõ thể loại`.
- Nhóm nghệ sĩ ưu tiên Artist ID, dự phòng theo tên; hỗ trợ nhiều thể loại trên cùng bài hát.
- File mới: `feature/library/MusicCollectionsContent.kt`; file thay đổi: `core/media/DeviceMusicScanner.kt`, `feature/library/LibraryScreen.kt`.
- Build-only: `:app:assembleDebug` và `:app:assembleDebugAndroidTest` PASS (`BUILD_EXIT_CODE=0`), log `qa-logs/assemble-debug-library-collections-v1.log`. Chưa chạy instrumentation hay QA hành vi.
- Bước tiếp theo: Search nâng cao v1 — tìm theo bài/album/nghệ sĩ/thể loại. Sản phẩm tổng thể CHƯA NGHIỆM THU.

### 2026-10-29 — Crossfade / Gapless Playback v1 — Build-only

- HEAD trước sửa: `1c57e56`.
- Giữ ExoPlayer/MediaSession và queue persistence làm đường phát chuẩn; Media3 quản lý khoảng nối gapless khi dữ liệu và định dạng hỗ trợ.
- Thêm `PlaybackCrossfade.kt` và `TransitionContract.kt`. Crossfade tùy chọn 1s/2s/3s, mặc định tắt; phát lớp âm thanh tạm cho bài kế tiếp, handoff trở lại ExoPlayer chính và giữ queue/index.
- Chỉ chạy khi phát tuần tự bình thường, repeat/shuffle tắt, tốc độ 1.0x; hủy khi người dùng seek hoặc trạng thái không còn đủ điều kiện. Trở về volume bình thường khi hủy.
- Đã thêm lệnh MediaSession, trạng thái `PlayerUiState`, giao diện Now Playing và callback qua ViewModel/Activity, cập nhật lời gọi trong instrumented test.
- Build debug + build AndroidTest: PASS (chưa kiểm thử phát âm thanh, race conditions hay tác động recovery). Log: `qa-logs/assemble-debug-transition-v1.log`.
- Hạn chế: hai player có thể tạo khác biệt về audio focus/equalizer; cần kiểm thử âm thanh thực trên thiết bị trước khi chấp nhận crossfade. Tắt mặc định để không ảnh hưởng bản local ổn định.
- Bước tiếp theo: Library nâng cao (Album / Artist / Genre). Sản phẩm tổng thể CHƯA NGHIỆM THU.

### 2026-10-29 — Equalizer v1 — Build PASS

- HEAD trước thay đổi: `49603b0`, workspace `E:\Du_An\MusicFlow`.
- Đã thêm `EqualizerContract.kt` và `EqualizerEngine.kt`; hiệu ứng gắn theo audio session của ExoPlayer trong `PlaybackService`.
- Điều khiển Bật/Tắt và từng băng tần trong `NowPlayingScreen`; dải tần và giới hạn gain theo thiết bị, không giả định 5 băng tần.
- Dùng MediaSession custom command để gửi/nhận trạng thái; lưu cấu hình trong SharedPreferences; tự làm mới UI sau khi có audio session.
- Đã cập nhật dây nối `PlaybackController` / `PlayerViewModel` / `MusicFlowApp` / `MainActivity` và lời gọi ở `ExampleInstrumentedTest`.
- Build ứng dụng và APK instrumentation: `:app:assembleDebug` + `:app:assembleDebugAndroidTest` PASS. Log: `qa-logs/assemble-debug-equalizer-v1.log`.
- Giới hạn: chỉ build PASS, chưa có runtime QA xác nhận audio effect thực sự hoạt động trên mọi thiết bị. Nếu thiết bị không hỗ trợ thì UI báo không hỗ trợ.
- Chưa chạy Full Regression theo kế hoạch hoàn thiện chức năng trước; chưa commit/push.
- Bước tiếp theo: Crossfade/Gapless; MVP tổng thể CHƯA NGHIỆM THU.

### 2026-10-29 — Fix MediaSession API, build Lyrics + Sleep Timer v1

- HEAD trước sửa: `0d6dc08` (`feat: add synchronized local LRC lyrics support`).
- Log FAIL ban đầu: `qa-logs/buidfail` và `qa-logs/assemble-debug-sleep-timer.log`.
- Nguyên nhân: Media3 1.10.1 không có callback `onConnectAsync`; `AcceptedResultBuilder` chỉ nhận `session`.
- Sửa `PlaybackService.kt`: dùng `onConnect`, `super.onConnect`, `AcceptedResultBuilder(session)`; bỏ `@OptIn` không có hiệu lực để loại cảnh báo build.
- Kiểm chứng: `:app:assembleDebug` BUILD SUCCESSFUL (9s), `RESULT ASSEMBLE_DEBUG exitCode=0`; `:app:assembleDebugAndroidTest` BUILD SUCCESSFUL (16s), `RESULT ASSEMBLE_DEBUG_ANDROID_TEST exitCode=0`.
- Log build mới: `qa-logs/assemble-debug-lyrics-v1.log`.
- Phạm vi: build-only, chưa kiểm thử chức năng Sleep Timer / Lyrics trên thiết bị, chưa chạy Full QA.
- Code sửa trên máy Windows, **chưa commit/push**; cần đối chiếu lại HEAD khi đẩy GitHub.
- Bước tiếp theo: Equalizer v1 sau khi chốt build.
- MVP / sản phẩm tổng thể: CHƯA NGHIỆM THU.

### 2026-10-28 — Feature Development: Lyrics v1

- HEAD nền: 2b04b1e6.
- Playback Speed v1: đã có source.
- Sleep Timer v1: đã có source, chưa Full QA.
- Lyrics v1: triển khai đọc LRC và hiển thị đồng bộ.
- Lyrics import: Android OpenDocument.
- Lưu liên kết file LRC theo audio URI: đã triển khai.
- Build Lyrics v1: PASS ngày 2026-10-29 sau khi sửa API MediaSession trong Sleep Timer; chỉ kiểm tra biên dịch.
- Full QA: Để cuối giai đoạn phát triển.
- Bước tiếp theo: Equalizer v1.
- Sản phẩm tổng thể: CHƯA NGHIỆM THU.

### 2026-10-28 — Feature Development: Playback Speed & Sleep Timer

- HEAD nền: e16348ac.
- Playback Speed v1: đã có source (chưa QA).
- Sleep Timer v1: đang phát triển.
- Giữ các mốc QA cũ (Session Recovery v1 Full QA PASS, Android API 30 55 bài chính PASS, Marker 40/40 PASS).
- Build debug: FAIL (compile Kotlin — PlaybackService onConnectAsync/AcceptedResultBuilder API). Log: qa-logs/assemble-debug-sleep-timer.log.
- MVP / sản phẩm tổng thể: Chưa nghiệm thu.

### 2026-10-28 — Feature Development — Full Product

- Chuyển sang giai đoạn Feature Development — Full Product.
- HEAD nền: ffe42e7c.
- Hoàn thiện chức năng trước, Full QA cuối giai đoạn.
- Đang triển khai: PLAY-08 Playback Speed v1.
- Các giai đoạn còn lại: theo bảng lộ trình ở trên.
- Không đánh dấu sản phẩm hoàn thành khi chưa có đủ chức năng.
- MVP / sản phẩm tổng thể: Chưa nghiệm thu.

### 2026-10-28 — Android Compatibility Matrix v2

- Baseline: d430cfc7.
- Session Recovery v1: Full QA PASS trên API 30.
- Android 10 / API 29: PENDING QA v2.
- Android 13 / API 33: PENDING QA v2.
- Android 16 / API 36: PENDING QA v2.
- Regression mục tiêu: 55 bài chính, 3 SKIPPED.
- Marker mục tiêu: 40 chức năng + 4 SAF.
- Log API 29: qa-logs/android10-saf-compat-v1.log.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — Session Recovery UI Acceptance v1 — PASS

- Commit: d84c9a11.
- Build APK: PASS.
- History tồn tại và hiển thị sau restart: PASS.
- Playlist và hai bài hát tồn tại sau restart: PASS.
- Không tự phát khi điều hướng UI: PASS.
- PID thay đổi sau force-stop: PASS.
- Resume: PASS.
- Cleanup: PASS.
- Fast QA ba pha: PASS.
- Full Regression: PENDING.
- Log: qa-logs/playback-process-restart-v1.log.
- Bước tiếp theo: Full Regression 55 bài chính, 40 marker.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — MVP Session Recovery v1

- Mốc trước: Playlist → History → Replay Full QA PASS.
- Commit mốc trước: 580681c3.
- Playlist tồn tại sau process restart: PASS ở mốc trước.
- Queue khôi phục ở trạng thái Pause: PASS ở mốc trước.
- History được ghi trước restart: PENDING QA.
- History tồn tại sau restart: PENDING QA.
- Cleanup dữ liệu History: PENDING QA.
- Script: run-playback-process-restart-tests.ps1 -PhasesOnly.
- Log: qa-logs/playback-process-restart-v1.log.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — MVP Playlist → History → Replay v1

- Commit: 69d5a559.
- Playlist → Playback: PASS.
- Ghi bài đầu vào Recently Played: PASS.
- Next cập nhật thứ tự History: PASS.
- History hiển thị tại Home: PASS.
- Phát lại từ History: PASS.
- Fast QA: PASS.
- Full Regression: PENDING.
- Log: qa-logs/mvp-playlist-history-v1.log.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — MVP Playlist Navigation v1

- Commit: 1ae5c36c.
- Home → Playlist → Now Playing: PASS.
- Playlist → MediaSession: PASS.
- Next từ Now Playing: PASS.
- Fast QA: PASS.
- Full Regression: PENDING.
- Log: qa-logs/mvp-playlist-navigation-v1.log.
- Bước tiếp theo: Full Regression 55 bài chính.
- MVP tổng thể: Chưa nghiệm thu.

### 2026-10-28 — Playlist → Playback → Restart v1

- Commit: 56011698
- QA ba pha: PASS
- Playlist Room: PASS
- Thứ tự playlist: PASS
- Process Restart: PASS
- Queue Recovery: PASS
- Không tự phát sau restart: PASS
- Resume: PASS
- Cleanup: PASS
- Full Regression: PENDING
- MVP: Chưa nghiệm thu

### 2026-10-28 — SAF Multi-file v1

- Fast QA: PASS — Android API 30
- Queue 2 file: PASS
- Thứ tự theo picker: PASS
- Persisted READ: PASS
- Next: PASS
- Full Regression: PENDING
- MVP: Chưa nghiệm thu

### 2026-10-28 — SAF → History v1 — PENDING QA

- Multi-file picker writes second track to recent history: PENDING QA
- Full Regression: PENDING
- MVP: Chưa nghiệm thu

### 2026-10-28 — SAF → History v1

- Commit: d7243128
- Fast QA: PASS
- Chọn 2 file SAF: PASS
- Queue và Next: PASS
- Persisted READ: PASS
- Recently Played (Room): PASS
- Full Regression: PENDING
- MVP: Chưa nghiệm thu


### 2026-10-28 - Real Picker SAF accessibility click diagnosis

- Reviewed main 9231b73a and qa-logs/real-picker-saf-fast-v1.log.
- Compile: PASS. Fast QA: one instrumentation test executed, FAIL.
- Source WAV preparation: PASS; initial Home->Library click: PASS.
- Permissioncontroller not observed; foreground was com.example.musicflow.
- Failure at second Compose UI click (Choose MP3): No compose hierarchies found.
- Test-only change: use clickNamedAction for the Library picker button and log accessibility foreground if it fails.
- Next step: run isolated Fast QA only, assisted with manual DocumentsUI selection when it opens.
- Playback and persisted READ are NOT YET verified. Previous Android compatibility PASS baselines unchanged.
### 2026-10-28 - Real Picker SAF library permission dialog diagnosis

- GitHub main reviewed: dfd8e452084eef3da967ff294ae86fb22fb82e7f.
- Fast QA: Kotlin compile PASS, exactly 1 test executed, final FAIL.
- Actual failure: No compose hierarchies found at second UI action (Choose MP3).
- PASS composeUiReady was logged immediately after Home->Library click.
- LibraryRoute can request READ_EXTERNAL_STORAGE on Android 11.
- Added diagnosis for foreground package and denial of permission prompt to preserve SAF-only testing.
- Fast QA remains the only required run until it PASS; full regression later.
- Playback + persisted READ not yet verified. No MVP/release acceptance.
### 2026-10-28 - Real Picker SAF Compose rule fix

- Reviewed main 19e39627 and qa-logs/real-picker-saf-fast-v1.log.
- FAST QA compile: PASS; 1 Android instrumentation test started.
- FAIL at RealPickerSafTest.kt:387: No compose hierarchies found.
- Prepared change: createAndroidComposeRule<MainActivity>(), remove manual ActivityScenario.launch, synchronize Compose before opening picker.
- Do not change PlaybackService or previous PASS baselines.
- Next run: scripts/run-real-picker-saf-fast-v1-tests.ps1 -CompileOnly; then scripts/run-real-picker-saf-fast-v1-tests.ps1.
- Real Picker SAF is NOT yet PASS; perform full regression only after isolated Fast QA PASS.
### 2026-10-28 — Real Picker SAF v1 — PASS

- Commit: 19f418ae
- JVM: PASS
- Android API 30: 51 PASS, 3 SKIPPED
- Real DocumentsUI: PASS (manual-assisted)
- Playback: PASS
- Persisted READ: PASS
- FINAL_RESULT=PASS
- Ghi nhận riêng: com.android.bluetooth crash trên Emulator
- Chưa nghiệm thu toàn bộ MVP

- Commit: `39f29adaa3452885cdfab26ff49188b598249643`
- Log: `qa-logs/real-picker-saf-v1.log`
- JVM: PASS.
- Android Kotlin Compilation: FAIL.
- Lỗi: RealPickerSafTest.kt:379.
- Nguyên nhân: dấu ngoặc đóng thừa.
- Android instrumentation: chưa chạy.
- DocumentsUI / Playback / Persisted READ:
  chưa xác nhận.
- FINAL_RESULT=FAIL.

Đã hướng dẫn sửa:
- Xóa dấu ngoặc thừa.
- Khôi phục chuỗi "Gần đây" đúng Unicode.
- Biên dịch riêng trước khi chạy QA đầy đủ.

Mốc PASS gần nhất:
Android 10 Compatibility v1.

Bước tiếp theo:
Chạy lại Real Picker SAF v1.

Toàn dự án:
ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP.

### 2026-10-27 — Real Picker SAF v1 — FAIL lần 4

- Commit: `38bba835b76d17a80f83bda33e3bc263e40363ec`
- Log: `qa-logs/real-picker-saf-v1.log`
- Android: API 30.
- JVM: PASS.
- Android instrumentation: 1 test FAIL.
- Tìm đúng android:id/title: PASS.
- Long press bằng lệnh shell: chưa tạo được lựa chọn.
- DocumentsUI vẫn hiển thị sau thao tác.
- Playback và persisted READ: chưa được xác nhận.
- FINAL_RESULT=FAIL.

Đã chuẩn bị:
- Thay shell swipe bằng UiAutomation.injectInputEvent.
- Ghi kết quả DOWN/UP.
- Thêm fallback bấm phần tử cha của tên file.
- Giảm log chứa tên file không thuộc QA.
- Giữ nguyên kiểm tra playback và quyền URI.

Bước tiếp theo:
Chạy lại Real Picker SAF v1.

Mốc PASS gần nhất:
Android 10 Compatibility v1.

Toàn dự án:
ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP/RELEASE.

### 2026-10-27 — Real Picker SAF v1 — FAIL lần 3

- Commit: `c8b10a7c8fbe0f1028f7ed8f71e4c3303df2c047`
- Log: `qa-logs/real-picker-saf-v1.log`
- Android: API 30.
- JVM: PASS.
- Android regression: 1 bài FAIL.
- Tạo tài liệu WAV: PASS.
- DocumentsUI hiển thị file: PASS.
- Nhấn giữ qua Accessibility: thất bại.
- Bấm ngắn: mở YouTube Music thay vì trả về MusicFlow.
- FINAL_RESULT=FAIL.

Nguyên nhân đã xác định:
Hàm tìm file có thể chọn nhầm preview_icon,
do contentDescription cũng chứa tên file.

Đã chuẩn bị sửa:
- Chỉ chọn android:id/title có tên file chính xác.
- Sử dụng thao tác nhấn giữ thực tế.
- Không fallback sang bấm preview.
- Ghi lỗi nếu chuyển sang ứng dụng ngoài.
- Sửa tên AVD trong script kiểm thử.

Bước tiếp theo:
Chạy lại Real Picker SAF v1 và xác nhận
DocumentsUI trả URI về MusicFlow.

Mốc PASS gần nhất: Android 10 Compatibility v1.
Toàn dự án: ĐANG PHÁT TRIỂN, chưa nghiệm thu MVP.

### 2026-10-27 — Real Picker SAF v1 — FAIL lần 2

- Commit: `eef7b07af779b43a5d9aedbb964c144d110888df`
- Log: `qa-logs/real-picker-saf-v1.log`
- Android: API 30.
- JVM: PASS.
- Android regression: 1 test FAIL.
- Tạo file SAF: PASS.
- DOCUMENT_ROW_CLICKED: có.
- PICKER_CONFIRM_CLICKED: có.
- PICKER_NOT_RETURNED_TO_APP: có.
- Playback và persisted READ: chưa xác nhận.
- FINAL_RESULT=FAIL.

Đã chuẩn bị cải thiện bài test:
- Thử nhấn giữ để chọn nhiều tài liệu.
- Xác nhận đúng nút Open/Select.
- Ghi package của cửa sổ đang hiển thị.
- Ghi accessibility hierarchy khi picker không đóng.
- Giữ nguyên assertions playback và persisted READ.

Bước tiếp theo:
Chạy lại Real Picker SAF v1, phân tích log mới.

Mốc PASS gần nhất: Android 10 Compatibility v1.
Trạng thái toàn dự án: CHƯA NGHIỆM THU MVP.

### 2026-10-27 — Real OpenMultipleDocuments SAF v1 — FAIL

- Commit: `ea317255c9a92d143f8de78a4c498309fa06504d`
- Test: `RealPickerSafTest.kt`
- Script: `scripts/run-real-picker-saf-v1-tests.ps1`
- Log: `qa-logs/real-picker-saf-v1.log`
- Android: API 30, Emulator.
- JVM: PASS.
- Android regression: 1 test FAIL.
- Tạo tài liệu SAF thật: PASS.
- Phát nhạc từ DocumentsUI: chưa xác nhận.
- Quyền READ sau picker: chưa xác nhận.
- FINAL_RESULT=FAIL.

Nguyên nhân đang điều tra:
- Kiểm thử so sánh mediaId với URI CreateDocument.
- Chưa xác nhận DocumentsUI thực sự hoàn tất lựa chọn.
- Chưa có log URI và playback state thực tế.

Đã chuẩn bị:
- Kiểm tra DocumentsUI quay về ứng dụng.
- Đối chiếu metadata và nội dung WAV đã chọn.
- Kiểm tra persisted READ trên URI thực tế.
- Bổ sung log trạng thái playback để chẩn đoán.

Bước tiếp theo:
Chạy lại Real OpenMultipleDocuments SAF v1.

Mốc PASS gần nhất:
Android 10 Compatibility v1.

Trạng thái toàn dự án:
ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP/RELEASE.

### 2026-10-27 — Android 10 Compatibility v1 — PASS

- Commit: `74b9026c1cdb70c9d2ed0563bb747d1a047a061a`
- Script: `scripts/run-android10-saf-compat-v1-tests.ps1`
- Log: `qa-logs/android10-saf-compat-v1.log`
- Android: API 29, Pixel 5 Emulator.
- JVM: PASS.
- Android regression: 50 bài PASS.
- Ba bài SAF theo pha: PASS.
- Process restart: PID 8237 → 8373.
- QA markers: 24/24 PASS.
- FINAL_RESULT=PASS.

Ma trận đã có bằng chứng QA tự động:
- Android 10 — API 29: PASS.
- Android 11 — API 30: PASS.
- Android 13 — API 33: PASS.
- Android 16 — API 36: PASS.

Giới hạn:
- Chưa xác nhận chọn file thật qua toàn bộ OpenMultipleDocuments.
- Chưa nghiệm thu Bluetooth và lockscreen trên thiết bị thực.
- Chưa hoàn tất toàn bộ các cổng nghiệm thu MVP.

Bước tiếp theo:
Real OpenMultipleDocuments SAF v1.

Trạng thái toàn dự án:
ĐANG PHÁT TRIỂN — CHƯA NGHIỆM THU MVP/RELEASE.

### 2026-10-27 — Android 16 Compatibility v1 — PASS

- Commit: `69bf34f96608e11f9cc537338a2347931ab2f010`
- Script: `scripts/run-android16-saf-compat-v1-tests.ps1`
- Log: `qa-logs/android16-saf-compat-v1.log`
- Android: API 36, Pixel 7 Emulator.
- JVM: PASS.
- Android regression: 50 test PASS.
- SAF PREPARE / VERIFY / CLEANUP: PASS.
- Process restart: PID 9361 → 9547.
- QA markers: 24/24 PASS.
- FINAL_RESULT=PASS.

Đã xác nhận:
- Kiểm thử lifecycle sau phím Home đạt.

### 2026-10-27 — Android 13 SAF Compatibility v1 — PASS

- Commit: `7e2e7b59cb45ed0a7bc702de5a39659aa8bb93bb`
- Log: `qa-logs/android13-saf-compat-v1.log`
- API: 33 — Android 13.
- JVM: PASS.
- Android regression: PASS.
- SAF PREPARE, VERIFY, CLEANUP: PASS.
- PID trước/sau force-stop: 7491 → 8196.
- Markers: 24/24 PASS.
- FINAL_RESULT=PASS.

Giới hạn:
- Chưa kiểm thử OpenMultipleDocuments đầy đủ với tài liệu thật.
- Chưa nghiệm thu thiết bị Bluetooth thật và lockscreen.
- Kết quả không đại diện cho tất cả thiết bị Android 13.

Bước tiếp theo: Android 16 Compatibility v1 (API 36).

### 2026-10-27 — SAF Permission Persistence v1 — PASS

- Commit: `77c546e9a2b2f621f21ae114a240d9b916309735`
- Script: `scripts/run-saf-permission-v1-tests.ps1`
- Log: `qa-logs/saf-permission-v1.log`
- JVM: PASS.
- Android regression: PASS.
- Ba test SAF được chạy theo pha riêng: PASS.
- PREPARE, VERIFY, CLEANUP: PASS.
- Tiến trình mới: PID 15108 → 15251.
- Kiểm tra persisted URI read: PASS.
- Phát lại SAF từ Favorites: PASS.
- FINAL_RESULT=PASS.

Giới hạn:
- Kiểm thử trên Android 11 Emulator.
- Sử dụng ACTION_CREATE_DOCUMENT, chưa nghiệm thu
  trực tiếp toàn bộ OpenMultipleDocuments của MusicFlow.
- Chưa kiểm thử Android 13–16 và thiết bị thực.

Bước tiếp theo:
Android Compatibility Matrix v1 — Android 13, API 33.

### 2026-10-27 — Background/System Controls v2 — PASS

- Commit: `1ab915f572bbf48d67b91a69cc916803fc62ab0e`
- Script: `scripts/run-background-controls-v2-tests.ps1`
- Log: `qa-logs/background-controls-v2.log`
- JVM: PASS.
- Android: 47/47 PASS — Pixel 5 Emulator, Android 11.
- QA markers: 20/20 PASS.
- FINAL_RESULT=PASS.

Kết quả:
- Các nút Play/Pause/Next/Previous trên thông báo hoạt động.
- MusicFlow nhường audio focus khi có yêu cầu tạm thời.
- 45 bài kiểm thử trước đó PASS.
- Không cần thay đổi PlaybackService.

Giới hạn:
- Chưa nghiệm thu điều khiển trực tiếp trên màn hình khóa.
- Chưa kiểm thử tai nghe Bluetooth và thay đổi đầu ra âm thanh thực tế.
- Chưa xác nhận trên Android 13, 14, 15 và 16.
- Emulator có crash ở tiến trình hệ thống com.android.bluetooth;
  không có bằng chứng crash của tiến trình MusicFlow.

Trạng thái:
- Background/System Controls v2: QA tự động PASS.
- Toàn dự án: ĐANG PHÁT TRIỂN, chưa nghiệm thu MVP/Release.

Bước tiếp theo:
- MVP E2E mở rộng: SAF URI permission persistence.
- Kiểm thử Android Compatibility & Permissions Matrix.
- Hoàn thiện nghiệm thu lockscreen/Bluetooth trên thiết bị phù hợp.

### 2026-10-27 — Background Playback & System Controls v1 — PASS

- Commit: `663e3ba08565598ceb46a323bfdc41f2c42b7e43`
- Script: `scripts/run-background-controls-v1-tests.ps1`
- Log: `qa-logs/background-controls-v1.log`
- JVM: PASS.
- Android: 45/45 PASS.
- Marker: 18/18 PASS.
- FINAL_RESULT=PASS.

Đã xác nhận:
- Phát nhạc khi Activity xuống nền.
- Thông báo MediaSession xuất hiện.
- Playback tiếp tục sau khi Activity bị hủy.
- Play/Pause/Next/Previous qua MediaController.
- Các kiểm thử hồi quy trước đó đều PASS.

Giới hạn:
- Chưa kiểm thử thao tác nút trực tiếp trên thông báo/màn hình khóa.
- Chưa nghiệm thu audio focus và tai nghe Bluetooth thật.
- Chưa kiểm thử trên nhiều phiên bản Android.

Bước tiếp theo:
Background/System Controls v2 — lockscreen, notification actions,
audio focus và tình huống ngắt kết nối thiết bị âm thanh.

Trạng thái toàn dự án: ĐANG PHÁT TRIỂN.


### 2026-10-27 — Favorites + Album + Theme Batch v1 — QA TỰ ĐỘNG PASS

- Commit: `511f93ea157aef56b2115d22e15870fba52e87b9`
- Log: `qa-logs/favorites-batch-v1.log`
- Script: `scripts/run-favorites-batch-v1-tests.ps1`
- JVM: PASS.
- Android: 44/44 PASS — Pixel_5, Android 11 (API 30).
- Marker: 14/14 PASS.
- FINAL_RESULT=PASS.

Đã kiểm thử:
- Favorites bài hát và album, lưu dữ liệu cục bộ.
- Lọc Favorites trong Library.
- Điều hướng Favorites từ Home.
- Hiển thị Favorites SAF không cần quyền MediaStore.
- Giao diện tối được duy trì sau khi khởi động lại.
- Hồi quy các chức năng playback và file picker trước đó.

Giới hạn:
- Chưa xác nhận phát lại file SAF thật sau khi khởi động lại.
- Chưa kiểm thử trên ma trận nhiều phiên bản Android.
- Emulator ghi nhận crash ở tiến trình hệ thống com.android.bluetooth;
  không phải crash của MusicFlow, cần theo dõi khi kiểm thử Bluetooth.

Trạng thái: Mốc QA tự động PASS; chưa nghiệm thu toàn bộ MVP.
Bước tiếp theo: Hoàn thiện nghiệm thu SAF và background/system controls.


### 2026-10-26 — MVP E2E v1, lần kiểm thử đầu — FAIL
- Commit: `e4ed4a4accdf31eee03b0290f6c6ccc3bb7899f3`.
- Log: `qa-logs/mvp-e2e-v1.log`.
- JVM: PASS.
- Android: FAIL tại bước biên dịch Kotlin, chưa chạy tests.
- Nguyên nhân: import không hợp lệ `androidx.compose.ui.test.onNode`.
- Cách sửa: xóa dòng import sai trong `ExampleInstrumentedTest.kt`.
- Trạng thái: chờ chạy lại QA.
- Mốc Runtime Recovery v3: giữ nguyên PASS 36/36.

### 2026-10-26 12:41:36 +07:00 — Playback Runtime Recovery Race Hardening v3 — PASS
- Baseline HEAD trước sửa: `e0141a8d7d5d55ca8bf6bf2aa30492e9458db253`.
- HEAD lúc chạy test: `313269e` (SHA commit source/test/log cần đối chiếu sau khi push).
- Mã nguồn: `PlaybackRuntimeAutoSkip.kt`; test: `PlaybackRuntimeRaceHardeningTest.kt`.
- Script: `scripts/run-playback-runtime-recovery-v3-tests.ps1`.
- Log: `qa-logs/playback-runtime-recovery-v3.log`.
- Android: 36 tests; kết quả: **PASS**. JVM và marker chi tiết theo log.
- Kịch bản race: Pause→Play, queue giống hệt, Seek cùng index, dispose, và uninterrupted skip.
- Giới hạn: mới có test chủ động tiêm lỗi cho các race; test Service thực tế cũ là regression. Không coi kết quả này là nghiệm thu MVP.
- Hành động kế tiếp: MVP End-to-End Acceptance v1.


### 2026-10-26 — Runtime Recovery Race Hardening v3 — FAIL môi trường
- Commit chứa thay đổi v3: `da2c5d487ea192c201d41d9168cd53c758098704`.
- Log: `qa-logs/playback-runtime-recovery-v3.log`.
- JVM: CHƯA CHẠY.
- Android: CHƯA CHẠY.
- Kết quả script: `FINAL_RESULT=FAIL`.
- Nguyên nhân: `REQUIRES_ONE_EMULATOR` và lỗi mã hóa Unicode trong script cập nhật kế hoạch.
- Đã chuẩn bị sửa: tự khởi động/chờ Emulator và lưu script PowerShell bằng UTF-8 BOM.
- Bước tiếp theo: chạy lại toàn bộ QA v3, xác nhận 36/36 tests và các marker.
- Giữ nguyên kết quả Playback Runtime Recovery v2 PASS.

### 2026-10-25 — Đối chiếu tiến độ và Definition of Done
- HEAD tại thời điểm đọc GitHub: `6a13defe1fba5cc3231123cee0375aae23b0d341` (`update tien do`).
- Tài liệu sẵn có: `docs/Ke_Hoach_Du_An.md` và `docs/Tong_Quan_Tien_Do_Va_Tieu_Chi_Hoan_Thanh_MusicFlow.md`.
- Xác nhận lại `qa-logs/playback-runtime-recovery-v2.log`: JVM PASS, Android **31/31 PASS**, `SERVICE_ERROR_COUNT=7`, `SERVICE_FORWARDED_COUNT=7`, `SKIP_BRANCH_COUNT=1`, `EXHAUSTED_BRANCH_COUNT=1`, `FINAL_RESULT=PASS`.
- Không có vòng QA mới sau v2 trong những commit đã xem; HEAD mới là cập nhật tài liệu.
- Kết luận **mốc v2 PASS; toàn dự án chưa hoàn thành** vì còn các cổng nghiệm thu MVP/release.
- Bước tiếp theo đề xuất: Runtime Recovery Race Hardening v3; chưa triển khai.

### 2026-10-25 — Playback Runtime Recovery Integration v2 — PASS
- Commit QA: `1aec374d25046c75f39e81cc242f42165e56dc26`.
- Log: `qa-logs/playback-runtime-recovery-v2.log` (log tạo lúc `2026-10-25T15:14:53+07:00`; dòng `COMMIT` trong log là commit trước khi push).
- JVM PASS; Android **31/31 PASS** trên emulator API 30.
- Service nhận và chuyển tiếp **7/7** callback lỗi; test tự xác nhận hai nhánh Skip/Exhausted; tình huống Pause và thay queue lúc phục hồi chờ đạt PASS.
- Giới hạn: còn các race tương đồng queue / Pause→Play nhanh chưa có test xác nhận.

### 2026-10-25 — Playback Runtime Error Auto-Skip v1 — PASS
- Commit: `5e4b1c9386900f948af6297fdc4ad19345f451d8`.
- Log: `qa-logs/playback-runtime-auto-skip-v1.log`.
- JVM PASS; Android **28/28 PASS**; nhánh auto-skip và hết queue kiểm tra qua test chủ động tiêm lỗi; integration có trạng thái phát đúng.

### 2026-10-25 — Playback Queue Hardening v1 — PASS
- Commit: `a3b9ec9d1281f1b3e05c90ecdd1b5de1f0da8e16`.
- Log: `qa-logs/playback-queue-hardening-v1.log`.
- JVM PASS; Android **23/23 PASS**; file thiếu/URI bị từ chối (mô phỏng), snapshot hỏng được xử lý.

### 2026-10-25 — Playback Process Restart Recovery v1 — PASS
- Commit: `07ee7d3cac206baaac3364496d0107ca4ad7ca24`.
- Log: `qa-logs/playback-process-restart-v1.log`.
- JVM PASS; Android **19/19 PASS**; force-stop và tạo tiến trình mới, khôi phục queue Pause và tiếp tục phát đúng vị trí.

### 2026-10-25 — Các mốc nền tảng đã PASS
- Playback Queue Persistence v1: `1f13264`, `qa-logs/playback-queue-persistence-v1.log`, **16 Android tests PASS**.
- Playback Service Integration v1: `da8277b`, `qa-logs/playback-service-integration-v1.log`, **13 Android tests PASS**.
- Playlist Playback Order v1: `qa-logs/playlist-playback-order-v1.log`, **12 Android tests PASS**.
- Playlist Reorder v1: `qa-logs/playlist-reorder-v1.log`, **11 Android tests PASS**.
- Playback Skip Recovery v1: `qa-logs/playback-skip-v1.log`, **9 Android tests PASS**.

## 9. Câu lệnh khởi động cuộc trò chuyện mới

> **Tiếp tục dự án MusicFlow Android.** Đọc file `docs/Ke_Hoach_Du_An.md` trên GitHub `huan26677/MusicFlow-Android`, sau đó kiểm tra HEAD, source và log QA mới nhất. Không làm lại mốc PASS. Hãy tiếp tục milestone đang mở trong backlog, chỉ ra patch Kotlin và một script PowerShell kiểm thử tự động. Sau mỗi lần thử/sửa/hoàn thành, cung cấp cập nhật file kế hoạch (PASS/FAIL, commit, log, việc đã làm, bước tiếp theo) **trước** ba lệnh Git. Không tự sửa hoặc push repository khi chưa được phép. Chỉ đưa ba lệnh Git: `git add .`, `git commit -m "..."`, `git push`.

**File này được duy trì bằng commit**; việc tạo hoặc tải một bản Markdown riêng lẻ không cập nhật GitHub cho đến khi người dùng thực sự thay thế file trong `docs/` và push.

