# MusicFlow — Checklist nghiệm thu APK demo/báo cáo (2026-11-07)

**Mục đích:** hướng dẫn trình bày trước giảng viên và phân biệt chức năng **có mã**, **đã QA bằng dữ liệu test** và **chưa nghiệm thu bằng tài khoản/dịch vụ thật**. Đây là APK sideload cho dự án môn học, không phải release thương mại. Các mục gắn `PASS` chỉ khi có chứng cứ trong `qa-logs` và tài liệu tương ứng.

> **CẬP NHẬT OCEAN FLOW UI v1 (2026-11-07):** Home, Player, Mini Player, Thư viện, Tìm kiếm, Playlist, Tài khoản, Demo Center đã đồng bộ nhận diện xanh đêm/mint và Dark/Light. APK hiện hành `dist/MusicFlow-1.0-Demo-OceanFlowUIV1-Final-Android.apk`, **190 JVM + 77 Android regression + 1 DocumentsUI SAF + 4 Web Admin PASS**. Đã ký và cài/cold launch API30 chính APK, lưu ảnh Home/Account/Demo Center. Xem [Ocean_Flow_Visual_Refresh_v1.md](Ocean_Flow_Visual_Refresh_v1.md); không ghi nhận API33/thiết bị thật/TalkBack là đã đạt.

> **Nghiệm thu Demo Center v1 (2026-11-07):** 9 actor có kịch bản **hoàn toàn mô phỏng** trên APK, có tiến độ riêng và lối đăng nhập Demo offline. **190 JVM + 71 Android regression + 1 SAF thật + 4 Web Admin PASS**; APK hiện hành `dist/MusicFlow-1.0-Demo-CenterV1-Android.apk`. Xem [Trung_Tam_Trinh_Dien_MusicFlow_v1.md](Trung_Tam_Trinh_Dien_MusicFlow_v1.md). Các luồng quyền Owner/Admin/Creator qua JWT server vẫn không được đánh PASS live.

## 1. Checklist theo actor và use case

| Actor | Chức năng nên demo | Trạng thái được phép công bố | Chứng cứ |
|---|---|---|---|
| **Guest** | Mở ứng dụng, chọn WAV/MP3/FLAC offline; MediaStore/SAF; phát/dừng, Next/Previous, hàng đợi, lịch sử, phát nền; tìm không dấu | **Đã có mã và QA offline bằng emulator**. Không cần tài khoản | `OfflineAcceptanceFlowTest`, `docs/Bao_Cao_Ban_Giao_MusicFlow_Demo_APK.md`, `docs/Listening_Journeys_UX_v1.md` |
| **Guest / Free / Premium** | Tạo playlist, phát/sắp xếp, thêm bài; **Quick Add** từ playlist trống/có bài, tìm tiếng Việt không dấu, chống thêm trùng, dùng SAF mà không cần quyền quét | **Đã có mã và QA Room/Android**; kiểm chứng đường chọn SAF thực riêng theo log nghiệm thu mới | `docs/Playlist_Quick_Add_v1.md`, `PlaylistQuickAddUiTest`, `PlaylistQuickAddRealSafUiTest` |
| **Free** | Tạo tài khoản demo, đăng nhập/đăng xuất, sửa hồ sơ, đồng bộ thủ công | UI/kiến trúc đã có; **chưa chạy đủ JWT live 8 persona**. Không mô tả E2E như đã đạt | `docs/Account_Journeys_UX_v4.md`, `AccountHomeV2UiTest`, `AccountJourneysV4UiTest` |
| **Premium** | Hiển thị gói thật do backend xác minh; **mô phỏng mua/gia hạn/hủy Premium** với giá minh họa, ví/thẻ giả, biên nhận cục bộ | **Thanh toán giả lập đã được QA**, không thu tiền hay thay backend entitlement. Gia hạn, quota, payment production vẫn chưa nằm trong phạm vi báo cáo | `docs/Thanh_Toan_Premium_Gia_Lap_v1.md`, `DemoBillingRulesTest`, `DemoBillingUiTest` |
| **Artist / Author** | Đăng ký vai trò, xem xét duyệt, sửa hồ sơ, nháp, tín chỉ, bằng chứng và gửi xét duyệt | UI + giả lập RPC QA đạt, **chưa quyết định/xuất bản qua JWT thật** | `docs/Account_Journeys_UX_v4.md`, `CreatorDraftStudioUiTest`, `CreatorApplicationReviewUiTest` |
| **Moderator** | Chọn việc duyệt Creator/metadata/catalog **theo quyền thực**, xem hồ sơ, đưa lý do, xác nhận | **RBAC và UI có QA**, không được tự cấp vai trò, không tự phát hành audio khi chưa có quyền | `AccountReviewPlanTest`, `AccountJourneysV4UiTest`, `MusicFlowRolesUiTest` |
| **PlatformAdmin** | Tìm người dùng nếu có `manage_users`, xét yêu cầu nếu có `review_creators`, quản lý catalog theo phạm vi | Quyền được Supabase RPC xác định; **QA giả lập + Web Admin regression**; chưa thao tác tài khoản QA live tất cả role | `scripts/test-admin-web-role-refresh-v1.js`, `docs/Account_Journeys_UX_v4.md` |
| **Owner** | Mở tổng quan, chọn user rồi cấp/thu hồi vai trò/quyền có xác nhận, audit | Backend/UX QA đạt; bootstrap Owner được ghi trong lịch sử dự án; **chưa đăng nhập Owner JWT E2E trong đợt này** | `docs/Account_Task_UX_v3.md`, `AccountJourneysV4UiTest` |
| **Developer** | Lối tắt công cụ QA khi chạy Debug và có vai trò được backend xác minh | Có nền tảng và UI QA; **APK Release-mode demo không mở menu Developer Debug** | `AccountWorkspacePlanTest`, `MusicFlowRolesUiTest` |

## 2. Kịch bản demo offline không cần Internet (7–10 phút tùy thao tác)

1. **Mở MusicFlow** bằng APK demo đã ký QA → Trang chủ, chọn nghe nhạc offline với vai trò Guest.
2. **Thư viện** → Chọn tệp âm thanh trên emulator (có thể dùng WAV/MP3/FLAC được tạo sẵn hợp lệ), hoặc cấp quyền quét nhạc để dùng MediaStore.
3. **Player** → Play/Pause, Next/Previous, seek, hàng đợi. Trở về màn chính để minh họa Mini Player và MediaSession thông báo.
4. **Playlist** → tạo "Nhạc demo" → mở playlist → **Thêm bài hát vào playlist** → chọn nhiều bài bằng một chạm, tìm không dấu, thử thêm trùng (nút "Đã có").
5. **Không cấp quyền quét** → bấm **Chọn tệp âm thanh riêng**. Chọn một tệp trong DocumentsUI, kiểm tra nó xuất hiện trong playlist. Đây là thao tác hệ thống; nếu nguồn tệp thu hồi quyền hoặc tệp bị xóa, tính năng có thể báo lỗi.
6. **Sắp xếp playlist** → phát cả danh sách theo thứ tự đã lưu; vào Tìm kiếm, thử gõ không dấu, xem album/nghệ sĩ/thể loại. Minh họa lịch sử và yêu thích.
7. **Trang Tài khoản** → giới thiệu các vai trò và công cụ đã lập trình; không cần đăng nhập thật hay bật online để hoàn tất demo nghe offline.
8. **Thanh toán mô phỏng** → từ Trang Tài khoản bấm **Trải nghiệm thanh toán Premium (giả lập)** → chọn 30/365 ngày và ví/thẻ giả, xác nhận thành công, xem biên nhận → thử kết quả **Thất bại** (không mất biên nhận) → thử **Hủy** sau hộp thoại xác nhận. Mọi giá và quyền chỉ để minh họa, không có thanh toán thật.
9. **Demo Center (9 actor)** → từ Trang Tài khoản bấm **Trình diễn 9 loại tài khoản (giả lập)** → chọn Owner, xem tổng quan/cấp quyền/audit **mẫu** → đổi Artist, Moderator, Free/Premium và xem tác vụ minh họa; có thể chọn **Demo offline** ở màn đăng nhập để tạo một tài khoản local, không gửi mật khẩu/email thật qua Supabase.

## 3. Nghiệm thu kỹ thuật bắt buộc trước bàn giao APK cuối

- [x] `:app:testDebugUnitTest` — toàn bộ test JVM hiện có chạy không có failure.
- [x] `test-admin-web-role-refresh-v1.js` — 4 phép kiểm tra fail-closed khi thu hồi role/permission.
- [x] Android instrumentation — Playlist/Player/Guest/Owner/Creator/Moderator regressions qua emulator API 30; kết quả cuối ở `qa-logs/musicflow-demo-acceptance-v1-qa.log`.
- [x] Bài SAF riêng sử dụng **DocumentsUI thật** (file WAV QA) để nhận URI, cấp persisted read grant, lưu Room và đọc lại file; kết quả cuối cũng ghi ở log trên.
- [x] Gói Premium **thanh toán mô phỏng**: 186 JVM, 61 Android regression, 1 DocumentsUI thật, 4 Web Admin PASS; mua thử và lưu biên nhận sau force-stop trên chính APK release QA PASS. Xem `docs/Thanh_Toan_Premium_Gia_Lap_v1.md`.
- [x] **Demo Center 9 actor** và đăng nhập Demo offline có lối vào thực trên UI: 190/190 JVM, 71/71 Android regression, 1/1 DocumentsUI thật, 4/4 Web Admin PASS. Cài/cold launch và Guest chọn Owner mẫu trên chính APK Release QA PASS. Không gọi RPC cấp quyền thật. Xem `docs/Trung_Tam_Trinh_Dien_MusicFlow_v1.md`.
- [x] **Ocean Flow Visual Identity v1:** 190 JVM, 77 Android UI/playback regressions (6 visual tests mới), 1 SAF thật, 4 Web Admin PASS; `MusicFlow-1.0-Demo-OceanFlowUIV1-Final-Android.apk` ký QA/cài/cold launch PASS; ảnh Home, Account, Demo Center Dark đã lưu. Chưa có ảnh Player với file QA thủ công. Xem `docs/Ocean_Flow_Visual_Refresh_v1.md`.
- [x] Ký/verify/cài/cold launch APK Acceptance v1 (`MusicFlow-1.0-Demo-AcceptanceV1-Android.apk`), SHA256 `7D7E1D0131DC078CAA6394F1FC14F43BBB8820895FFF260C2BEF4C399CC95889` khớp log và tệp thực tế.
- [x] Hoàn thiện `docs/Ke_Hoach_Du_An.md`, `docs/Nghiem_Thu_Demo_2026_11_07.md`, thông báo phiên bản bàn giao và hướng dẫn cài đặt.

**Chưa bắt buộc trong phạm vi báo cáo:** E2E JWT bằng 8 tài khoản QA, đồng bộ hai thiết bị thật, Google Login, payment Premium, Google Play, thiết bị vật lý, bản quyền streaming thương mại, đo TalkBack bằng người dùng thật. Các mục đó **không được tự động đánh PASS**.

## 4. Chứng cứ và minh bạch

Mỗi lượt chỉ cập nhật các mục `[x]` sau khi đối chiếu công cụ chạy thật. Bài SAF DocumentsUI thực tế có thể bị ảnh hưởng bởi khác biệt giao diện theo phiên bản Android, vì vậy QA mới chỉ xác nhận trên emulator API30 được dùng; không suy ra Android mọi phiên bản. Tệp âm thanh QA và playlist được tạo dành riêng cho test và xóa sau khi hoàn thành.
