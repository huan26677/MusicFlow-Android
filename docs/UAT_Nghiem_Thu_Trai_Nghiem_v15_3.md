# MusicFlow — Kịch bản UAT trải nghiệm người dùng (đóng băng chức năng, v15.3)

**Ngày chuẩn bị:** 2026-11-09 · **Trạng thái:** READY FOR HUMAN UAT / **CHƯA CÓ NGƯỜI DÙNG THẬT THỰC HIỆN**.

Mục đích của đợt này là kiểm tra các chức năng đã có theo góc nhìn người lần đầu sử dụng. **Không mở thêm Use Case, quyền, gói phí, nguồn nhạc hoặc tài khoản.** Kịch bản này dành cho chủ đồ án mời **3–5 người tự nguyện**, không phải danh sách người đã được tuyển. Các phép thử tự động AVD ghi riêng, không thay thế UAT thủ công.

## 1. Chuẩn bị và cách quan sát

- **Thiết bị:** một điện thoại Android phổ thông, ưu tiên màn nhỏ; Android API 29+; một lượt với cỡ chữ hệ thống bình thường và một lượt **200%**. Tắt mạng khi kiểm thử Guest/offline; bật mạng chỉ khi tự nguyện thử tài khoản thật đã được cấp. Khuyến nghị kiểm tra TalkBack trên thiết bị Android có người biết cách bật/tắt.
- **Dữ liệu:** sử dụng file WAV/MP3 hợp lệ do nhóm tự sở hữu, playlist tên “UAT thử”, các tài khoản **Demo/QA**. Không dùng nhạc online khi chưa xác nhận quyền phát. Không thu thập mật khẩu thật, token, dữ liệu riêng tư, ảnh chứng minh nhân thân hoặc thao tác quản trị trên tài khoản đang dùng ngoài đời.
- **Quy tắc quan sát:** người điều phối đọc *mục tiêu*, không chỉ vị trí nút. Ghi mỗi lượt có cần trợ giúp, số lần chạm nhầm, thời gian (nếu dễ đo), mức độ hiểu từ 1–5 và câu nói thực tế của người thử. Cho phép bỏ qua bước không có vai trò thích hợp. Khi gặp lỗi, ghi cách tái hiện rồi dừng thao tác có nguy cơ mất dữ liệu.
- **Phân vai:** tối thiểu 2 người chưa dùng MusicFlow thử Guest/Free; một người dùng Demo Premium; một người thử Owner/Admin/Moderator dưới **Demo Center**; một người thử Creator Demo nếu đủ người. Có thể một người làm hai hành trình, nhưng không được tính thành hai người.

## 2. Nhiệm vụ UAT — đọc mục tiêu, không đưa đường dẫn nút

| ID | Vai trò / điều kiện | Mục tiêu người tham gia | Quan sát cụ thể | Module kiểm thử |
|---|---|---|---|---|
| UAT-G01 | Guest, tắt Wi-Fi | Vào nghe một bài nhạc có sẵn, tạm dừng và tiếp tục mà không đăng nhập | Có tự tìm được Thư viện, hiểu quyền chọn tệp và thao tác Player? | HOME_NAV, MEDIA_SAF, LIBRARY, PLAYBACK |
| UAT-G02 | Guest | Tạo một playlist, thêm bài, sắp xếp thứ tự và phát | Có tìm thấy Thêm bài, thấy phản hồi khi bài trùng, không làm mất nhạc? | PLAYLIST, PLAYBACK |
| UAT-F01 | Free / tài khoản Demo | Tìm bài bằng từ khóa không dấu, đánh dấu Yêu thích, mở Lịch sử | Có hiểu các trạng thái rỗng và khác biệt giữa trên máy/đám mây? | SEARCH, FAVORITES, HISTORY, AUTH |
| UAT-P01 | Premium Demo | Xem gói Premium mô phỏng và thử thanh toán giả lập | Có hiểu rõ **không mất tiền thật**, **không cấp Premium thật**? | BILLING_DEMO, AUTH |
| UAT-R01 | Owner/Admin Demo | Tìm khu vực làm việc, vào Kiểm duyệt và chuyển qua Đơn Creator, Hồ sơ tác phẩm, Nhạc online | Có nhận ra cả 3 lựa chọn khi màn nhỏ/chữ 200%, không cần cuộn ngang ẩn? | ROLES_ADMIN, UI_ACCESS |
| UAT-R02 | Owner/Admin/Moderator Demo | Mở danh sách đang chờ, hiểu nút xác nhận/từ chối, thử hủy trước khi xác nhận | Có hiểu các thay đổi quyền hoặc xét duyệt thật chỉ do máy chủ cho phép? | ROLES_ADMIN, COMMUNITY_MODERATION |
| UAT-C01 | Artist/Author Demo | Vào Hồ sơ sáng tạo, tìm bản nháp, xem đường gửi xét duyệt và chứng cứ bản quyền | Có hiểu “bản nháp riêng tư”, “chưa xuất bản”, giới hạn đính kèm? | CREATOR, ONLINE_RIGHTS |
| UAT-O01 | Guest / Online Demo | Gặp danh mục online trống hoặc không kết nối; tìm đường quay về nghe offline; xem thử Community Demo | Có hiểu lỗi, Demo và khi nào nhạc được phép phát? | ONLINE_RIGHTS, COMMUNITY_COMMENTS |
| UAT-A01 | Chữ 200%, màn nhỏ | Thử lại UAT-G01 và UAT-R01; cuộn nội dung dài, nút Quay lại và ô nhập | Có nút bị khuất, chữ chồng, chỉ báo vùng cuộn mơ hồ? | UI_ACCESS, ROLES_ADMIN |
| UAT-A02 | TalkBack, người có khả năng thao tác | Dùng điều hướng theo tiêu đề và nút trên Tài khoản, Thư viện và vùng Kiểm duyệt | Có đọc được tên/trạng thái/đã chọn? Có mắc kẹt hoặc mất tiêu điểm? | UI_ACCESS, AUTH, ROLES_ADMIN |

**Bảo vệ quyền:** Owner/Admin/Creator trong Demo **không phải tài khoản thật**. Chỉ kiểm thử backend thật với tài khoản QA được chủ dự án cấp riêng, không tự cấp quyền hoặc thay đổi dữ liệu sản xuất.

## 3. Phiếu ghi nhận cho mỗi người tham gia (sao chép phần này khi thử)

**Mã ẩn danh:** ______ · **Ngày:** ______ · **Thiết bị/API:** ______ · **Cỡ chữ:** ______ · **TalkBack:** Bật / Tắt · **Kinh nghiệm với app nhạc:** Mới / Có.

| Mã nhiệm vụ | Hoàn thành không trợ giúp? (Y/N/NA) | Gợi ý (#) | Thời gian (nếu đo) | Dễ hiểu 1–5 | Vấn đề, câu nói thực tế |
|---|---|---|---|---|---|
| UAT-G01 | | | | | |
| UAT-G02 | | | | | |
| UAT-F01 | | | | | |
| UAT-P01 | | | | | |
| UAT-R01 | | | | | |
| UAT-R02 | | | | | |
| UAT-C01 | | | | | |
| UAT-O01 | | | | | |
| UAT-A01 | | | | | |
| UAT-A02 | | | | | |

**Nhận xét cuối:** Điều gì làm bạn khó hiểu nhất? ______ · Bạn muốn thay đổi *cách trình bày nào trong tính năng đang có*? ______.

## 4. Tiêu chí đóng nghiệm thu và quy tắc sửa lỗi

- **Mục tiêu đề xuất (chưa đo):** không có lỗi P0 gây mất dữ liệu/cấp quyền sai; không có P1 khiến người chưa dùng bị kẹt ở Guest, Playlist hoặc Owner Review. Mục tiêu ít nhất **80% nhiệm vụ đủ điều kiện** hoàn thành không cần trợ giúp và điểm dễ hiểu trung vị **≥4/5**. Nếu không đạt, ưu tiên sửa *đúng trở ngại được người dùng ghi nhận*, không thêm chức năng.
- **P0:** mất dữ liệu nhạc/playlist, thay đổi quyền thật trái phép, thanh toán thật khi báo Demo; **P1:** không hoàn thành luồng chính, bị kẹt không thoát, TalkBack không thể truy cập nút chính; **P2:** thao tác khó tìm, thuật ngữ khó hiểu, quá nhiều bước; **P3:** thẩm mỹ/căn chỉnh ít ảnh hưởng.
- **Bắt buộc cho từng lỗi:** ghi `UAT-<ID>`, phiên bản APK, Android API, cỡ chữ, màn hình, thao tác tái hiện, ảnh *đã che thông tin cá nhân* nếu có, mức độ, module trực tiếp/phụ thuộc và test case hồi quy. Chỉ sửa P0/P1 hoặc P2 lặp lại có chứng cứ; test module trực tiếp và smoke phụ thuộc trước commit.
- **Các cổng chưa hoàn thành:** Full registry `@Test`, JWT đa vai trò thật, UAT 3–5 người, TalkBack có người thao tác, tương phản thực tế, kiểm thử trên điện thoại vật lý, nâng cấp APK có fixture Room/SAF — giữ trạng thái **NOT RUN** cho tới khi có log/biên bản.

## 5. Bằng chứng tự động (khác với UAT người thật)

- **Tái hiện và sửa đúng điểm UI:** trên AVD sạch `MusicFlow_UAT320_UXFreezeQA_API36`, bài `AccountTaskUxV3UiTest#ownerReviewChoicesAreStackedAndSelectableAt320dpAnd200PercentFont` ban đầu **FAIL**, vì ba tab xét duyệt cùng nằm trên hàng ngang khó phát hiện. Chỉ thay bố cục `AccountRolePanel`: khi vùng hẹp dưới 380dp hoặc chữ lớn từ 150%, ba lựa chọn thành các hàng xếp dọc; màn rộng vẫn dùng hàng ngang. Không thêm tác vụ, không thay quyền/SQL/RPC. Bài 320dp/chữ 200% sau sửa **1/1 PASS**; bài giữ bố cục ngang rộng 420dp/chữ thường cũng **PASS**.
- **Hồi quy:** `AccountTaskUxV3UiTest` + `AccountJourneysV4UiTest` + `MusicFlowRolesUiTest`: **21/21 Android PASS API36**; `AccountHomeV2UiTest` + `CreatorDraftStudioUiTest` + `CreatorApplicationReviewUiTest` + `CommunityModerationUiTest` + `DemoCenterV1UiTest` + `DemoBillingUiTest`: **32/32 Android PASS API36**, tổng **53/53 selected Android UI PASS**. `assembleDebug`, `assembleDebugAndroidTest` và **230/230 JVM PASS**; impact Python **19/19**, Brain Python **15/15**, Web Admin **4/4**, license policy **17/17 PASS**.
- **QA signed APK UAT-ready:** `dist/MusicFlow-1.0-Demo-UATReadyV15_3-Android.apk` (**31.803.069 bytes**, SHA-256 `92B176675344964A3130D9FBABD084C8E1D77CBE42742A88285B97B2F62C2BE0`). Gradle Release, zipalign, apksigner, hash, `debuggable=false`, cài đặt/cold-launch trên AVD API36 **PASS**; `SignedCommunityNavigationUiTest` chạy trên chính APK ký QA **1/1 PASS**. Đây là APK **ký Android Debug QA**, không phải bản sản xuất/Google Play; chỉ lưu tại `dist/` cục bộ.
- **Modular QA:** `ROLES_ADMIN` trực tiếp, `BILLING_DEMO`, `COMMUNITY_MODERATION`, `CREATOR`, `DEMO_CENTER` ảnh hưởng cần smoke; đóng gói APK nâng policy thành `full_required`. Catalog hiện có **22 module, 115 lớp, 507 @Test đăng ký**, không phải 507 bài đã chạy. **Full 507, UAT 3–5 người thật, TalkBack thủ công, JWT multi-role, upgrade signed với Room/SAF: NOT RUN**.
- **Bằng chứng Git đã sàng lọc:** `qa-logs/musicflow-v15_3-owner-accessibility-api36.log`, `qa-logs/musicflow-v15_3-signed-package.log`, `qa-logs/musicflow-v15_3-signed-smoke-api36.log`. Log Android thô và log FAIL trước sửa lưu cục bộ riêng, không đưa dữ liệu riêng tư lên repository.

**Kết luận:** bản QA đã sẵn sàng *để tổ chức UAT*, **chưa được người dùng thật nghiệm thu**. Chỉ điền phiếu ở mục 3 sau khi có người thực sự thử.
