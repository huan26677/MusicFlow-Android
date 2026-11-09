# MusicFlow — Triển khai UX gộp v15.4 (không mở tính năng)

**Ngày:** 2026-11-09. **Phạm vi:** checklist 21 nhóm tại `docs/Tong_Ra_Soat_UX_Mot_Dot_v15_3.md`. **Đã thực hiện lô sửa UX và kiểm thử chọn lọc; CHƯA đạt toàn bộ cổng nghiệm thu.** Bản này mô tả thay đổi tầng hiển thị, không phải chứng nhận UAT người dùng thật. Nghiệp vụ, tài khoản/quyền, máy chủ, RLS/RPC, thanh toán Demo, chính sách bản quyền, Room/SAF và Media3 không chủ ý thay đổi.

## A. Kết quả theo từng mục kiểm toán

| Mục | Kết quả thực hiện | Giới hạn |
|---|---|---|
| UX-01 | Đã sửa: lưu hồ sơ, Google OAuth đang ẩn và reset biên nhận Demo không đưa `exception.message` thô ra UI. | Không kích hoạt nút Google; OAuth thật không kiểm thử. |
| UX-02 | Đã làm rõ giới hạn hiển thị 30 bài cần liên kết, 150 bài gợi ý trên máy, 30 bản sao playlist/yêu thích. | **Partial:** không thêm tìm kiếm liên kết hoặc thay giới hạn/logic sync; nhiều mục hơn ngưỡng chưa được thao tác trực tiếp từ cùng màn. |
| UX-03 | Đã sửa: Owner cấp/thu hồi vai trò và phạm vi quyền xếp dọc trên màn <380dp hoặc font ≥150%; màn rộng vẫn ngang. | Giữ RPC, kiểm tra quyền, xác nhận. |
| UX-04 | Đã sửa: Creator giải thích lý do chưa thể gửi bản nháp (trường thiếu, giấy phép, xác nhận quyền, chứng cứ, lưu thay đổi, tệp đính kèm). | Không nới điều kiện `canSubmit` hoặc giai đoạn xét duyệt. |
| UX-05 | Đã sửa Web Admin: Hủy không báo “hoàn thành”; phải có phản hồi xác nhận thực từ RPC cho cấp/thu hồi, duyệt/gỡ. | Chỉ dùng dữ liệu/mock QA, không tạo tài khoản thật. |
| UX-06 | Đã sửa Web Admin: khi thiếu lý do chuyển tiêu điểm về ô lý do, vai trò/quyền hiển thị nhãn dễ hiểu, chọn user ghi rõ email. | Không đổi payload/kiểm quyền. |
| UX-07 | Đã sửa: hướng dẫn thông tin kết nối, giới hạn 20 kết quả, lý do nút phát hành bị vô hiệu. | URL/khóa công khai vẫn do quản trị viên cung cấp; không chèn bí mật vào source. |
| UX-08 | Đã sửa: nhãn giấy phép rõ nghĩa, gợi ý cuộn, chọn xác nhận quyền bằng toàn bộ hàng checkbox, “Chủ sở hữu bản ghi âm”. | Vẫn truyền mã license/credit chính xác vào RPC. |
| UX-09 | Đã sửa: các cặp nút Creator và 2 nút tải tệp xếp dọc ở màn/chữ lớn, nút tải về nói rõ loại tệp. | Không thay chứng cứ riêng tư/Storage. |
| UX-10 | Đã sửa: chỉ dẫn vuốt ngang ở Search và Smart Library. | Giữ cuộn ngang cho hiệu năng; không thay bộ lọc/index. |
| UX-11 | Đã sửa: Player nâng cao có gợi ý vuốt và mô tả tiếng Việt cho hòa âm, chuyển bài, cân bằng âm thanh. | Không thay callbacks/media engine. |
| UX-12 | Đã dịch trạng thái kết nối MediaSession sang “bộ phát nhạc”; kiểm thử Queue 320dp/200% theo danh sách lazy. | Không đổi bố cục/nút khi chưa có lỗi hiển thị được xác nhận. |
| UX-13 | **Cần kiểm chứng thêm** các màn Playlist/IME dài ở nhiều kích thước; không thay Room/lazy layout khi chưa có bằng chứng. | Chưa có đo đạc đủ để tuyên bố xong. |
| UX-14 | Đã thêm nhãn trợ năng theo bài hát/bình luận/album/người đóng góp ở Home, Community, Creator, Library, Online, Quick Add. | TalkBack bằng người thật **NOT RUN**. |
| UX-15 | Đã thêm live-region lịch sự cho thông báo Account, Owner, Creator, tệp riêng tư, Community. | Không bao phủ mọi `Text` trạng thái; TalkBack thật **NOT RUN**. |
| UX-16 | Đã chuẩn hóa “metadata”, “cloud”, “Supabase” và từ kỹ thuật ở phần người nghe/quản trị thường gặp. | Developer diagnostics và định dạng giấy phép/.json vẫn giữ tên kỹ thuật cần thiết. |
| UX-17 | Đã làm rõ trạng thái thiếu nhạc online, lối nghe offline và lý do bài không đủ quyền phát; nút giấy phép/bình luận đọc kèm tên bài. | Không cấp phát bất kỳ bài thiếu giấy phép nào. |
| UX-18 | **Chưa sửa** xác nhận xóa biên nhận Demo vì checklist yêu cầu UAT chứng minh dễ nhấn nhầm; đã đơn giản hóa câu chữ. | Phản hồi người dùng thật **NOT RUN**. |
| UX-19 | **Giữ nguyên** cấu trúc hai vùng cuộn Owner đã có kiểm thử trước; cần UAT/TalkBack thực. | Không gắn “PASS người dùng” từ Compose. |
| UX-20 | Đã thêm đường mở Thư viện khi tải lịch sử thất bại; các trường hợp playlist vẫn giữ lối Back hiện có. | Cần người dùng xác nhận thao tác không bị kẹt. |
| UX-21 | QA tích hợp, APK QA ký, bảo toàn dữ liệu và UAT phải ghi kết quả độc lập theo môi trường. | **Không coi unit/mock là E2E thật**. |

## B. Modular Testing & Impact-driven Regression

- **Thay đổi trực tiếp:** `AUTH, BILLING_DEMO, COMMUNITY_COMMENTS, CREATOR, DEMO_CENTER, HOME_NAV, LIBRARY, ONLINE_RIGHTS, PLAYBACK, PLAYLIST, RELEASE_BACKUP, ROLES_ADMIN, SEARCH, SMART`, Web Admin và QA/release script. Các module phụ thuộc `UI_ACCESS, FAVORITES, MEDIA_SAF` và downstream phải được đối chiếu qua `qa/impact_modules.json`.
- **Chính sách:** `full_required` do ≥4 module và có thay đổi giao diện quản trị/release. Bộ registry sau 3 bài Android mới: **22 module, 115 lớp, 510 `@Test` (230 JVM + 280 Android), 601 module-case links**. `510 registered` không đồng nghĩa `510 PASS`.
- **QA baseline v15.3:** 230 JVM, 53 Android UI selected và 1 signed navigation trên QA APK đã từng PASS. Chúng là tham chiếu, không tự xác nhận bản v15.4.
- **QA v15.4 thực chạy:** `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` **BUILD SUCCESSFUL**, **230/230 JVM PASS**; Python Impact **19/19**, Brain **15/15**, license policy **17/17**, Web Admin quyền **4/4** và Web Admin UX cancel/RPC **5/5 PASS**. QA catalog **510 registered @Test**, chính sách `full_required` giữ nguyên.
- **Android API36 runner 1:** 71 trường hợp Account/Creator/Community, **70 PASS / 1 FAIL**; lỗi Espresso `RootViewWithoutFocusException` tại `AccountJourneysV4UiTest#androidBackClosesOwnerOverviewInsteadOfLosingRolePage`. **Android API36 runner 2:** 86 trường hợp nghe nhạc/playlist/trợ năng, **84 PASS / 2 FAIL** do Espresso `closeSoftKeyboard()` không lấy được focus cửa sổ trong `LibraryPerformanceV4UiTest`. Các lỗi này **không bị xóa khỏi bằng chứng**.
- **Rerun độc lập trên AVD mới API36:** đúng ba test trên **3/3 PASS**, cùng với **2/2** test mới về vai trò Owner 320dp/200% và giấy phép Creator, **1/1** Queue 320dp/200%. Lượt `32` bài chạy lại sau sửa câu chữ cuối cùng: **15 PASS rồi ABORT vì hệ điều hành emulator báo `System has crashed`**. Không gọi lượt này là 32/32 PASS. Những kiểm thử trước sửa câu chữ nhỏ chỉ là hồi quy tham khảo, không tương đương một lượt full test trên APK cuối.
- **Bản QA ký sau rebuild cuối:** `dist/MusicFlow-1.0-Demo-FinalUXBatchV15_4-Android.apk`, **31.835.837 bytes**, SHA-256 `DBDD37AC0407BE7BA06A6DF418ED10567A08023043DCBEDDE6C64060CFEA546A`. Gradle release-mode, zipalign, apksigner, hash, `debuggable=false` **PASS**; cài đè, cold start, process alive, không có recent FATAL trên AVD API36 **PASS**; `SignedCommunityNavigationUiTest` chạy trên bản signed QA **1/1 PASS**. Đây là **khóa Debug QA**, không phải production.
- **Signed upgrade v15.3→v15.4 với Room/favorite/SAF:** **NOT VERIFIED**. Script `scripts/run-signed-upgrade-v15_4-qa.ps1` kiểm tra AVD riêng, chữ ký/sha, fixture hai giai đoạn nhưng lượt thử không đạt precondition AVD chưa cài MusicFlow; **không gỡ/xóa dữ liệu để ép chạy**. Bằng chứng upgrade v13→v14 trước đây không chứng minh cặp v15.3→v15.4 này.
- **Giới hạn QA cần nêu rõ:** full 510 (230 JVM + 280 Android registered) **NOT RUN as one complete suite**; TalkBack/người thật 3–5 người, thiết bị vật lý, live JWT các vai trò, nhiều SAF provider, signed upgrade fixture v15.3→v15.4, kiểm tra sức tải dữ liệu thật **NOT VERIFIED/NOT RUN**. Đừng suy các mục này từ mock/emulator.

## C. Tiêu chí bàn giao

Các cổng **build/package/signed smoke đã có chứng cứ PASS**, nhưng **full 510, signed upgrade fixture, UAT người thật, TalkBack thật chưa hoàn tất**. Bản ký Android Debug QA chỉ phục vụ đồ án, không phải production/Google Play. Không commit APK, khóa ký hay log chứa tài khoản.

**Bằng chứng có thể chia sẻ sau khi lọc:** `qa-logs/musicflow-v15_4-ux-final-api36.log`, `qa-logs/musicflow-v15_4-signed-package.log`, `qa-logs/musicflow-v15_4-signed-smoke-api36.log`, `qa-logs/musicflow-v15_4-signed-upgrade-api36.log`. Log instrument thô và APK ở thư mục cục bộ bị Git ignore. Phiếu **UAT 10 nhiệm vụ** đã có tại `docs/UAT_Nghiem_Thu_Trai_Nghiem_v15_3.md` (chưa có người thật thử).
