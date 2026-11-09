# MusicFlow — Báo cáo nghiệm thu trên điện thoại thật và Google/Gmail (v15.5)

**Ngày:** 09/11/2026. **Phạm vi:** đồ án Android Demo, không phải sản phẩm Play Store. **Kết luận:** **DEMO QA ĐẠT CÁC CA ĐÃ CHẠY; NGHIỆM THU TOÀN BỘ DỰ ÁN CÒN ĐIỀU KIỆN**. **Cập nhật mới nhất:** đợt QA OPPO sau khi bật Google tại `docs/Bao_Cao_Kiem_Thu_Thiet_Bi_That_Final_v15_5.md`; không sử dụng các trạng thái Google tắt bên dưới làm trạng thái hiện tại.

## 1. Thiết bị, phạm vi và bảo vệ dữ liệu

- Thiết bị kết nối qua ADB: **OPPO CPH2001**, Android **11 / API 30**, màn hình **1080×2400**. Không đưa số serial, tên tài khoản, email hoặc token vào báo cáo.
- Trước lượt kiểm thử không có MusicFlow trên thiết bị. Đã cài APK Demo QA v15.4, rồi cập nhật bản debug để thử UI, cuối cùng cài đè APK Demo QA **v15.5** cùng chữ ký. **Không xóa dữ liệu ứng dụng, không gỡ MusicFlow, không cấp quyền đọc toàn bộ nhạc và không đăng nhập tài khoản cá nhân.**
- Dữ liệu âm thanh dùng trong QA: WAV tự tạo; kiểm thử Media3 chạy WAV trong cache ứng dụng. Việc chọn thủ công WAV tại DocumentsUI trên OPPO chưa có bằng chứng hoàn thành, **không coi là PASS SAF picker**.
- APK cuối: `dist/MusicFlow-1.0-Demo-PhoneUATV15_5-Android.apk`; **31.835.837 byte**; SHA-256 `81864FE0D5D4ACE9F9B4B1D422C31D66D6AD661452BF53B510A5D4F1C5104D5A`. Ký bằng **Android Debug QA**, `debuggable=false`, hash/zipalign/apksigner/release build PASS. Không phải chữ ký production.

## 2. Kết quả kiểm thử (PASS/FAIL/NOT RUN)

| Nhóm | Bằng chứng thực tế | Kết luận |
|---|---|---|
| ADB/thiết bị vật lý | Điện thoại được nhận diện, APK signed cài thành công, MainActivity khởi chạy | **PASS** API30 |
| Home / Search / Library | Điều hướng qua 3 màn, thông điệp không bắt buộc đăng nhập/quét toàn bộ máy | **PASS** smoke thao tác |
| Account đăng nhập Google: khi provider bật | Compose UI test cho `googleAvailable=true`, click gọi callback một lần | **PASS** logic UI giả lập, **không phải OAuth thật** |
| Account Google chưa cấu hình | Compose UI test cho provider false và Demo; trên APK signed điện thoại có thông báo Google chưa bật, không có nút chết | **PASS** |
| Auth Guest / đăng ký Demo | Lượt đầu **FAIL 1** vì chạm khi startup đang tải; bổ sung chờ trạng thái enabled/selected, chạy lại trên điện thoại **1/1 PASS** | **PASS sau sửa test**, giữ lịch sử FAIL |
| Signed MainActivity → Community Demo | `SignedCommunityNavigationUiTest` chạy trên APK QA v15.5 đang cài | **1/1 PASS** API30 |
| Media3 PlaybackService thực | `PlaybackServiceIntegrationTest#playbackServiceIntegration`: phát WAV trong cache, hàng đợi playlist, Home→phát nền, Next/Previous, lịch sử trả lại trạng thái ban đầu | **1/1 PASS** API30; **7 marker PASS** trong logcat |
| JVM/QA engine | Gradle Debug + AndroidTest + Unit, **230/230 JVM**, Impact Python **19/19**, Brain **15/15**, Web Admin **9/9**, license policy **17/17**; danh mục **22 module, 513 @Test (230 JVM + 283 Android)** | **PASS** các ca chạy |
| Người dùng chọn WAV qua Files/SAF trên OPPO | Tệp QA hiện ở Downloads; thao tác chọn chưa có kết quả trả về ứng dụng | **NOT VERIFIED**; cần UAT thao tác thật |
| Đăng nhập Gmail bằng nút Google, nhận phiên Supabase thật | **Ban đầu** Google OAuth provider trên Supabase trả `external.google=false`; **sau khi cấu hình**, `external.google=true` và người dùng xác nhận đã đăng nhập Google được; màn tài khoản đã đăng nhập được quan sát trên OPPO | **Provider ENABLED**, **phiên tồn tại PASS**; OAuth fresh độc lập chưa chạy bằng QA |
| Full regression Android 283 ca, JWT nhiều vai trò, TalkBack, 3–5 người UAT, tải dữ liệu lớn, nhiều SAF provider | Chưa có chứng cứ đầy đủ trên bản v15.5 | **NOT RUN** |

Các log raw có thể chứa dữ liệu thiết bị được giữ **cục bộ, không commit**. Bản tóm tắt đã sàng lọc nằm ở `qa-logs/musicflow-v15_5-phone-acceptance-api30.log`. Không tính lại các bài retry thành số ca duy nhất của full suite.

## 3. Trạng thái Gmail thật và cách bật

Ứng dụng đã có luồng Google OAuth + PKCE, xử lý `musicflow://auth/callback`, mã xác minh một lần và lưu refresh token mã hóa Android Keystore. Trong v15.5, **nút “Tiếp tục bằng Google” chỉ hiển thị khi máy chủ xác nhận Google provider hoạt động**; khi tắt, ứng dụng giải thích rõ và vẫn cho nghe offline/dùng email.

Kiểm tra **read-only** từ `/auth/v1/settings` trên dự án Supabase của MusicFlow cho thấy:

| Thiết lập tại 09/11/2026 | Kết quả |
|---|---|
| Nhà cung cấp Email | **Bật** |
| Cho phép đăng ký email | **Bật** (`disable_signup=false`) |
| Nhà cung cấp Google | **Đã bật sau cấu hình** (`external.google=true`; trước khi cấu hình là `false`) |
| Tự xác nhận email | **Bật** (`mailer_autoconfirm=true`) |

Vì vậy địa chỉ **Gmail có thể được dùng làm email đăng ký/đăng nhập với mật khẩu riêng của MusicFlow**, độc lập với Google OAuth. Chủ dự án **xác nhận đã đăng nhập Google thành công**; kiểm tra mới cho thấy Provider bật và phiên trên thiết bị còn hoạt động. QA không tự thao tác nhập lại Gmail hoặc đọc mật khẩu/token.

**Các bước chủ dự án đã cấu hình; dùng làm checklist rà soát khi có lỗi:**

1. Trong **Google Auth Platform / Google Cloud Console**, cấu hình OAuth consent screen (đối tượng người dùng thử, scopes `openid`, `email`, `profile`), tạo OAuth **Web application** client và đặt Google **Authorized redirect URI**: `https://vzvxpbwrftlnyvuhfoqn.supabase.co/auth/v1/callback`.
2. Trong **Supabase Dashboard → Authentication → Providers → Google**, nhập **Client ID + Client Secret từ Google Cloud vào dashboard Supabase**, bật Google và lưu. **Không chép Client Secret vào mã Android, tài liệu GitHub hay cuộc trò chuyện.**
3. Trong **Supabase Authentication → URL Configuration → Redirect URLs**, thêm callback ứng dụng `musicflow://auth/callback**` để chấp nhận nonce `flow` trên URL; áp dụng allow-list càng hẹp càng tốt. Xem tài liệu chính thức: [Google Auth](https://supabase.com/docs/guides/auth/social-login/auth-google?platform=android) và [Redirect URLs](https://supabase.com/docs/guides/auth/redirect-urls).
4. Khi server trả `external.google=true`, mở lại Tài khoản MusicFlow, dùng **chính Gmail của người dùng** để cấp phép trong màn hình Google, kiểm tra đăng nhập/đăng xuất và quyền Free; không tự cấp vai trò Owner/Admin từ client. Người dùng báo **đã thực hiện thành công**; QA chỉ xác nhận **phiên tồn tại**, chưa kiểm thử OAuth mới độc lập.

## 4. Kiến trúc kiểm thử và mức độ hoàn thành

- **Module thay đổi trực tiếp:** `AUTH` (nút Google + trạng thái), test `UI_ACCESS/AUTH`, `RELEASE_BACKUP` (đặt tên APK QA mới). **Phụ thuộc:** `ROLES_ADMIN`, `CREATOR`, `CLOUD_SYNC`, `DEMO_CENTER`, `BILLING_DEMO`, `HOME_NAV` và các module liên quan đăng nhập qua bản đồ `qa/impact_modules.json`.
- Impact planner chọn **66 test cases** cho chỉnh sửa AccountScreen (direct AUTH + downstream smoke). Do đóng gói APK/nhánh Auth, cổng phát hành là **full_required**; không đánh tráo 513 đăng ký thành 513 PASS.
- Đối với đồ án, **ứng dụng đã có APK QA cài được và phát nhạc thật qua MediaSession trên điện thoại**. Để ký duyệt **hoàn thành dự án đầy đủ**, vẫn cần full Android regression, UAT người thật, picker SAF trên điện thoại, login Google thật sau cấu hình, thử đa vai trò JWT và xác nhận của chủ đồ án.
- Bản ký QA và thư mục `dist/` lưu **trên máy phát triển**, không đẩy APK lên GitHub. Muốn phát hành công khai/Google Play cần cổng bảo mật và signing riêng.

**Kết luận nghiệm thu sau cập nhật:** **PASS có điều kiện cho trình diễn Android offline/QA**. Google OAuth Provider đã **được bật**, người dùng báo đăng nhập thành công, phiên được quan sát; **NOT RUN** với thử lại OAuth mới độc lập, full Android trên máy thật và UAT người dùng. Xem biên bản chốt **181/181 ca Android chọn lọc PASS sau sửa test** tại `docs/Bao_Cao_Kiem_Thu_Thiet_Bi_That_Final_v15_5.md`. Không tự tuyên bố production ready.
