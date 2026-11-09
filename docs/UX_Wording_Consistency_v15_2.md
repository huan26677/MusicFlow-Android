# MusicFlow v15.2 — Chốt câu chữ và lỗi hiển thị (không thêm tính năng)

**Ngày:** 2026-11-09. **Mục tiêu:** sửa các điểm UX còn chưa nhất quán được tìm thấy sau v15.1; không đổi Actor, Use Case, dữ liệu, quyền, hệ thống phát nhạc hay cấu hình Supabase.

## Phạm vi

| Màn hình đang sử dụng | Điều chỉnh | Không thay đổi |
|---|---|---|
| Tài khoản Free/Premium | “Xóa cloud”, “Playlist local”, “Supabase” trong bảng đồng bộ và hộp thoại xác nhận được diễn đạt thành “bản sao trên đám mây” và “trên thiết bị”. Xác nhận rõ dữ liệu trên thiết bị còn nguyên | Các hàm đồng bộ, xóa dấu bản sao, Premium thật và Demo |
| Owner/Admin | Thông báo thất bại không hiện nguyên văn RPC/HTTP, thay bằng bước kiểm tra quyền, kết nối hoặc phiên đăng nhập | RPC có quyền, điều kiện vai trò, nhật ký/audit |
| Creator Studio / tệp riêng tư | “Lưu nháp lên Supabase” → “Lưu bản nháp riêng tư”, nhấn mạnh chưa xuất bản; lời nhắc đóng chưa lưu và trạng thái tệp dễ hiểu; lỗi giữ hướng dẫn về giới hạn 6 MB, định dạng hoặc quyền | Lưu nháp, xét duyệt, kiểm tra quyền nhạc online, Storage RLS, PDF/JPG/PNG và audio |
| Khám phá online hiện tại | Thông báo nguồn HTTPS, ngắt kết nối và tải danh mục an toàn, không lộ chi tiết máy chủ; vẫn gợi ý thử lại | Catalog, giấy phép/đường phát, repository hoặc URL |

**Đường đi màn hình:** `navigation/MusicFlowApp.kt` dùng `feature.online.OnlineCatalogRoute`. `feature.catalog.OnlineCatalogScreen.kt` là mã cũ không nằm trên luồng điều hướng hiện tại, **không sửa** để tránh tác động không cần thiết.

## QA Lead — Modular Testing & Impact-driven Regression

- Module thay đổi trực tiếp: `AUTH`, `ROLES_ADMIN`, `CREATOR`, `ONLINE_RIGHTS`. Kiểm tra tác động lan truyền các màn tài khoản, mô phỏng Premium, Owner/Admin, Creator và online; **không** sửa `core.account`, API, RLS, schema, thanh toán, offline/SAF hoặc Media3.
- Quy tắc QA `full_required` được bật vì phạm vi đa module và có thông báo quyền tài khoản. Test case trong catalog là **đăng ký**, không phải mặc định đã PASS.
- Unit tests mới: `AccountOperationFeedbackTest` và `OnlineCatalogFeedbackTest` kiểm tra không rò raw RPC/URL/token, thông báo quyền/phiên hết hạn, định dạng tệp, lỗi mạng, HTTPS. Compose tests cũ có assertion câu chữ mới ở `AccountHomeV2UiTest` và `CreatorDraftStudioUiTest`.
- Các kết quả Android, APK ký QA và số lượng test thực chạy sẽ được ghi lại theo log. Nghiệm thu thật với Guest/Free/Premium/Owner, TalkBack và 200% font vẫn là thử nghiệm riêng.

## Bất biến nghiệm thu

- Nghe nhạc offline không cần đăng nhập; bảo toàn Room/SAF/playlist/queue.
- Không biến quyền Premium/Demo thành quyền thật, không thay điều kiện xác minh Creator hoặc quản trị.
- Không tải nhạc chưa có bản quyền; Community Demo không ghi Supabase.
- Không phát hành Play Store. APK nếu được tạo chỉ là bản QA ký Debug cho báo cáo và lưu cục bộ dưới `dist/`.

**Giới hạn:** Full regression, JWT multi-role HTTP, máy thật/TalkBack/UAT và signed-upgrade bảo toàn Room/SAF sẽ ghi **NOT RUN** trừ khi có bằng chứng thực thi.

## Kết quả kiểm thử thực chạy

- Gradle `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`: **PASS**; **230/230 JVM unit tests PASS**.
- `scripts/test_impact_regression.py` **19/19 PASS**, `scripts/test_project_brain_v2.py` **15/15 PASS**. Bộ kiểm thử phân quyền web-admin **4/4 PASS**; quy tắc kiểm tra nguồn nhạc có giấy phép **17/17 PASS**.
- Android AVD sạch riêng `MusicFlow_UxFreezeV15_2_QA_API36`: nhóm AccountHome/AccountTaskUx/Roles **23/23 PASS**; nhóm CreatorDraftStudio/CreatorPrivateAttachment/CreatorApplicationReview/AccountJourneys/OnlineRights/Community **24/24 PASS**. Tổng **47/47 selected Android UI PASS**, không phải toàn bộ 505.
- **Lỗi có ghi lại:** lượt mixed Creator/online ban đầu **23 PASS / 1 FAIL** tại `AccountJourneysV4UiTest#oneReviewerAreaAtATimeAndNoUnrelatedPanels`. Bài test chạm tab đánh giá ở dưới viewport, do chưa cuộn `role-workspace-content`; test chạy lại độc lập chưa cuộn vẫn FAIL. Sau khi dùng thao tác cuộn thực trước khi chọn tab, **1/1 isolated PASS** và **24/24 mixed PASS**. Chỉ thêm testTag vùng cuộn, không đổi quyền hoặc trạng thái xét duyệt. Giữ log lỗi thô cục bộ, không gộp lượt thất bại vào kết quả PASS.
- Catalog sau thay đổi **22 module, 115 lớp, 505 @Test được đăng ký**, chính sách `full_required`. **Full 505, real JWT multi-role, TalkBack/UAT thiết bị thật và upgrade signed có fixture Room/SAF: NOT RUN**.

## APK ký QA v15.2 và bàn giao

**Tệp cục bộ:** `dist/MusicFlow-1.0-Demo-UXFreezeV15_2-Android.apk` — **31.786.685 bytes**, SHA-256 `FE4DD00677177AD79211D336632914FCAE2EB643B4D5508F394B2403BBDA7025`. `Gradle Release`, `zipalign`, `apksigner`, hash và `debuggable=false` đều **PASS**. Đây là **APK ký Android Debug QA** chỉ cho báo cáo, không phải bản sản xuất/Play Store; `dist/` được Git ignore.

Kiểm tra độc lập trên AVD API36: `adb install -r` **PASS**, cold-launch `MainActivity` **PASS**, tiến trình còn chạy, log gần lúc mở không có `FATAL EXCEPTION`. `SignedCommunityNavigationUiTest` chạy **trên chính APK ký QA v15.2**, đường Trang chủ → Khám phá online → Community Demo **1/1 PASS**. Đây là một lượt signed riêng, không phải 505 test.

**Chứng cứ portable đã lọc:** `qa-logs/musicflow-v15_2-ux-selected-api36.log`, `qa-logs/musicflow-v15_2-signed-package.log`, `qa-logs/musicflow-v15_2-signed-smoke-api36.log`; file thất bại đầu tiên và log thô nằm cục bộ, không được tuyên bố là PASS hoặc đưa thông tin riêng tư lên Git.
