# MusicFlow Android — Biên bản kiểm thử trên thiết bị thật (đợt chốt QA v15.5)

**Ngày kiểm thử:** 09/11/2026. **Mục đích:** cung cấp bằng chứng trung thực phục vụ báo cáo đồ án và nghiệm thu bản Demo. **Không phải giấy chứng nhận phát hành production.**

## 1. Môi trường và đối tượng kiểm thử

| Hạng mục | Giá trị |
|---|---|
| Thiết bị vật lý | OPPO CPH2001, Android 11 (API 30), 1080 × 2400 |
| Ứng dụng | MusicFlow Android, bản Demo QA v15.5, package `com.example.musicflow` |
| Bản cài cuối | `dist/MusicFlow-1.0-Demo-PhoneUATV15_5-Android.apk` |
| SHA-256 APK | `81864FE0D5D4ACE9F9B4B1D422C31D66D6AD661452BF53B510A5D4F1C5104D5A` |
| Ký số | Chữ ký Android Debug QA cho sideload; **không phải khóa production** |
| Công cụ | ADB, AndroidJUnitRunner, Compose UI Test, Media3, Gradle, Impact Regression QA |
| Phạm vi dữ liệu | Dữ liệu QA giả lập; không gửi email hay bình luận thật, không đổi vai trò thật, không xóa dữ liệu điện thoại |

## 2. Bảng kết quả thực chạy trên OPPO Android 11

| Mã nhóm | Nội dung | Lần chạy ban đầu | Sau điều chỉnh / kiểm thử lại |
|---|---|---:|---:|
| P-01 | Tài khoản Free/Premium/Owner, Creator, quyền & màn quản trị (10 lớp) | **53 PASS / 0 FAIL** | **53/53 PASS** |
| P-02 | Thư viện, Smart Mix, Search, Home, Player UI, trợ năng, giao diện (13 lớp) | **70 PASS / 1 FAIL** | **71/71 PASS** sau sửa thao tác cuộn test |
| P-03 | Playlist QA, quy tắc bản quyền online, Community Demo, JSON license, Room in-memory (lớp và ca chọn lọc) | **55 PASS / 2 FAIL** | **57/57 PASS** sau sửa 2 test |
| P-04 | APK **signed** thật: mở MainActivity → Community Demo | — | **1/1 PASS** |
| P-05 | APK signed: Media3 Service + WAV, phát nền, Next/Previous, lịch sử | Đã chạy tại lượt QA v15.5 trước đó | **1/1 PASS có log riêng từ cùng APK/SHA**; không chạy lại để tránh đụng hàng đợi người dùng |
| P-06 | Mở trang Tài khoản trên APK signed sau khi cài đè Debug → Release | — | **PASS**; thấy trạng thái tài khoản đã đăng nhập, không hiển thị biểu mẫu |
| P-07 | Google/Auth settings từ máy chủ thực (GET read-only) | — | **Google=true, Email=true, signup enabled=true** |

**Kết quả của đợt hiện tại:** **181 ca Android chọn lọc, duy nhất = 181 PASS sau các lượt sửa kiểm thử**, cộng **1 ca Signed Community Navigation PASS**. Một ca Media3 Service đã PASS ở lượt trước trên **cùng APK ký v15.5**. Không cộng những lần retry thành bài mới; không ghi **283/283 PASS**.

Lần thử ban đầu chạy 53 ca `createComposeRule` trực tiếp trên APK Release ký QA đều lỗi **không tìm thấy Activity kiểm thử `androidx.activity.ComponentActivity`**. Đây là **giới hạn test harness**, không phải 53 bug của app. Đã chuyển sang cài đè **Debug cùng chữ ký** để chạy test Compose mà **không xóa dữ liệu**, rồi cài lại **Release QA cùng chữ ký** và kiểm tra phiên đăng nhập còn nguyên. Lịch sử lỗi được giữ trong log cục bộ.

## 3. Ba lỗi kiểm thử được sửa và tái kiểm

| Mã | Lần đầu | Nguyên nhân xác minh | Xử lý | Tái kiểm |
|---|---|---|---|---|
| QA-01 | `SmartLibraryUiTest#pickedTrackIsIncludedInSmartMixEvenWithEmptyMediaStore` FAIL | Bấm nút `smart-play-all` đang nằm ngoài vùng cuộn nhìn thấy trên API30; callback không được gọi | `performScrollTo().performClick()` | **1/1 PASS** |
| QA-02 | `CommunityDemoUiTest#editExistingCommentMovesFocusToExistingComposer` FAIL | Dữ liệu bình luận Demo chưa tải xong, submit chưa sẵn sàng; bàn phím còn mở | Đợi `Bình luận (2)`, đóng bàn phím, xác nhận submit enabled | **1/1 PASS** |
| QA-03 | `OnlineRightsScreenTest#unverifiedTrackShowsDetailsButIsNotPlayable` FAIL | Kiểm tra chuỗi UI cũ, trong khi app đã đổi thành thông báo có giải thích `Chưa phát được: Chưa xác minh quyền phát` | Đối chiếu lại câu chữ hiện hành, giữ nguyên logic quyền phát | **1/1 PASS** |

**Không thay đổi mã sản phẩm** cho ba mục trên. Android app, Google OAuth, RLS, quyền, thanh toán, dữ liệu người dùng, Room, SAF và Media3 vẫn giữ nguyên. Chỉ điều chỉnh phương thức/chờ/khẳng định trong test.

## 4. Tình trạng Google thật và tài khoản

- `/auth/v1/settings` của Supabase thực cho thấy **Google enabled, Email enabled, đăng ký email không bị khóa**. Việc bật Google không làm tắt đăng nhập/đăng ký thường.
- Chủ dự án đã **xác nhận tự đăng nhập Google thành công** trên MusicFlow. Trong lượt QA này, Android thật hiển thị **màn tài khoản đã đăng nhập** và sau khi cài đè Debug rồi khôi phục APK ký QA, **phiên vẫn tồn tại**. Đây là bằng chứng trạng thái phiên, **không phải chứng cứ độc lập cho một lượt OAuth mới hoặc danh tính nhà cung cấp của token**.
- Không đọc/ghi mật khẩu Google, refresh token, secret OAuth hay email cá nhân vào báo cáo.
- Chưa kiểm tra hai tài khoản Google khác nhau, liên kết email hiện hữu, refresh token nhiều ngày, quy tắc JWT nhiều vai trò thật, mạng chập chờn khi callback OAuth.

## 5. Đối chiếu với Modular Testing & Impact-driven Regression

- Bộ đăng ký hiện có **22 module, 115 lớp test, 513 phương thức @Test**: **230 JVM + 283 Android**. Phần Android được chọn lọc theo rủi ro và an toàn dữ liệu điện thoại người dùng.
- Module đã chạy đại diện: `AUTH`, `ROLES_ADMIN`, `CREATOR`, `COMMUNITY_COMMENTS`, `COMMUNITY_MODERATION`, `BILLING_DEMO`, `HOME_NAV`, `SEARCH`, `SMART`, `LIBRARY`, `PLAYBACK` (UI + signed service trước đó), `PLAYLIST`, `ONLINE_RIGHTS`, `RELEASE_BACKUP`, `UI_ACCESS`.
- Build Debug/AndroidTest và `testDebugUnitTest` PASS; **230/230 JVM** theo kết quả XML baseline v15.5, không có thay đổi code sản phẩm trong đợt này.
- Ba thay đổi thuộc **test only**. Impact planner ghi các module trực tiếp `SMART/FAVORITES`, `COMMUNITY_COMMENTS`, `ONLINE_RIGHTS` và các module liên quan; **không coi Impact plan là test PASS**.

## 6. Những mục chưa thể ký PASS

1. **101/283 ca Android còn lại chưa chạy trong riêng đợt hiện tại trên OPPO** (181 ca chọn lọc + 1 ca điều hướng signed = 182). Có **1 ca Media3 signed PASS ở lượt trước** trên cùng APK/SHA; vì vậy xét bằng chứng cộng dồn là **183 ca Android khác nhau đã PASS trên máy thật, 100 ca chưa có bằng chứng**. Nhiều ca sẽ tạo/xóa Room, MediaStore, DocumentsUI, queue, permission hoặc thay đổi process; không chạy hàng loạt trên điện thoại đang có tài khoản và dữ liệu thực. Đây là **NOT RUN**, không phải PASS.
2. **Chọn tệp SAF thủ công trên OPPO:** lượt trước tệp WAV thử hiện trong Downloads nhưng picker chưa trả kết quả, **NOT VERIFIED**. Đã có bằng chứng SAF/Room nâng cấp **PASS trên AVD API36 v15.4**, không thay thế test tay trên OPPO.
3. **TalkBack thực, đo pin/hiệu năng dài hạn, 3–5 người UAT, nhiều thiết bị/phiên bản Android, các vai trò JWT live, liên kết Google nhiều tài khoản, quyền Premium sản xuất:** **NOT RUN**.
4. Bản APK ký bằng **Debug QA** phục vụ trình diễn/đồ án, không đủ điều kiện Play Store; thanh toán vẫn là Demo, nguồn online phải qua kiểm tra bản quyền.

## 7. Kết luận dùng trong báo cáo đồ án

**ĐỦ BẰNG CHỨNG ĐỂ BẮT ĐẦU VIẾT VÀ TRÌNH BÀY BÁO CÁO ĐỒ ÁN.** Ứng dụng chạy trên điện thoại Android thật; các luồng chọn lọc trải rộng người dùng, Creator/quản trị, nghe nhạc offline, phát nền, Smart Mix, Playlist, Community Demo và quyền phát online có kết quả kiểm chứng. Kết quả kiểm thử được ghi **PASS/FAIL/RETEST/NOT RUN** minh bạch.

**Không kết luận dự án đã hoàn tất toàn bộ 513 test hay sẵn sàng vận hành thương mại.** Nếu hội đồng yêu cầu nghiệm thu đầy đủ, phần chưa kiểm thử phải được triển khai trên máy/AVD thử riêng và có biên bản UAT người thật.

**Tệp liên quan:** `docs/Bao_Cao_Nghiem_Thu_Dien_Thoai_Gmail_v15_5.md`, `docs/Modular_Test_Catalog.md`, `qa/impact_modules.json`, `qa-logs/musicflow-v15_5-phone-acceptance-api30.log` (bản tóm tắt không chứa thông tin cá nhân).
