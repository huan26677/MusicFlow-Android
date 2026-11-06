# MusicFlow — Creator Application Review UX v2 (dự án báo cáo)

**Ngày:** 2026-11-06. **Baseline:** `df35479`. **Phạm vi:** giao diện quản lý đơn Nghệ sĩ/Tác giả trên Android, không thay đổi bảng Supabase, quyền Owner hoặc tài khoản QA hiện có.

## Mục tiêu và cách sử dụng

- Người dùng Free/Premium đăng nhập, vào **Tài khoản → Vai trò và quản trị MusicFlow**. Ứng dụng tải `musicflow_my_creator_requests` qua phiên Supabase hiện có trước khi cho gửi đơn. Trạng thái hiển thị tiếng Việt: **Đang chờ duyệt**, **Đã duyệt**, **Đã từ chối**, kèm phản hồi `review_note` nếu có.
- Đơn `artist` đang `pending` khóa nút gửi lại `artist`, nhưng không khóa đơn `author` nếu người dùng chưa có vai trò Creator. Nếu không tải được đơn hiện có, giao diện yêu cầu thử lại trước khi gửi — tránh gửi trùng khi offline/phiên lỗi.
- Khi backend chấp nhận `musicflow_apply_creator`, UI hiện đơn `pending` ngay trong danh sách và không cấp vai trò ngay lập tức. Chỉ backend `musicflow_review_creator` sau khi người có quyền xét duyệt xác nhận mới được cấp vai trò.
- PlatformAdmin có `review_creators` hoặc Owner xem danh sách pending từ `musicflow_pending_creators`, thông tin Nghệ danh/Tài khoản/Vai trò/Giới thiệu, nhập lý do 5–300 ký tự rồi nhấn **Duyệt**/**Từ chối**. Một hộp thoại xác nhận hiển thị lại lựa chọn + lý do và cho phép **Hủy** trước khi có RPC thay đổi dữ liệu.
- Sau khi người duyệt bấm **Xác nhận**, Android chỉ xóa đơn khỏi danh sách khi RPC trả **boolean `true`**. RPC từ chối, lỗi kết nối hoặc phản hồi `false` giữ nguyên đơn trong UI, hiện thông báo lỗi. Quyết định/audit vẫn chỉ được ghi ở backend.
- Khi chọn **Kiểm tra lại quyền từ Supabase**, ứng dụng xóa danh sách người dùng, đơn pending, audit, các lựa chọn và hộp thoại đã mở, tránh hiện dữ liệu quyền cũ khi đổi vai trò.

## Phân quyền và dữ liệu

- Không thay đổi schema, SQL/RLS, auth providers hay Premium. Bảng `musicflow_private.creator_applications` có sẵn index duy nhất `musicflow_creator_one_pending` theo `(user_id, requested_role)` cho bản ghi `pending`: UI là phòng lỗi thao tác; server vẫn chống gửi trùng khi có tranh chấp.
- Không có logic nào tự xuất bản nhạc. Duyệt vai trò Artist/Author **không chứng nhận giấy phép âm thanh và không mở phát streaming**. Nhạc offline do người dùng chọn tiếp tục hoạt động bình thường.
- Kiểm tra read-only trên hosted Supabase cho kết quả **1 Owner và 1 đơn Creator pending vẫn còn nguyên** sau QA. Không đăng nhập bằng mật khẩu Owner và không sử dụng mật khẩu QA trong script.

## Mã nguồn và kiểm thử

- `app/src/main/java/com/example/musicflow/core/account/CreatorApplicationUiRules.kt`: quy tắc UI thuần Kotlin (trạng thái, vai trò, pending và lý do duyệt).
- `app/src/main/java/com/example/musicflow/feature/account/AccountRolePanel.kt`: load trạng thái, khóa đơn trùng, hộp thoại quyết định và clear dữ liệu đã cấp quyền khi refresh.
- `app/src/test/java/com/example/musicflow/core/account/CreatorApplicationUiRulesTest.kt`: **4/4 JVM PASS**; hồi quy `MusicFlowAccessTest` **8/8 PASS**, tổng **12/12 JVM**.
- `app/src/androidTest/java/com/example/musicflow/ui/CreatorApplicationReviewUiTest.kt`: **3/3 Android instrumentation PASS**: pending khóa gửi trùng, duyệt cần xác nhận/Hủy và reason đúng, phản hồi `false` giữ đơn. Kèm `MusicFlowRolesUiTest` **7/7** = **10/10 Android PASS** trên Android emulator API30 riêng.
- Script chạy lại: `scripts/run-creator-application-review-v2-tests.ps1` (build/unit luôn chạy; UI chỉ chạy khi người dùng truyền serial của QA emulator); log `qa-logs/musicflow-creator-application-review-v2-build.log`, `qa-logs/musicflow-creator-application-review-v2-qa.log`.
- Lần đầu build có một lỗi **import Compose test `assertExists`** không phù hợp; đã sửa và chạy lại Gradle/Android **PASS**. Không dùng kết quả failed build làm bằng chứng nghiệm thu.

## Bản APK demo

- `dist/MusicFlow-1.0-Demo-CreatorReviewV2-Android.apk` — **31.065.789 bytes**; SHA-256: `B1A7B9A0D4010B49683F96CC67E80660BC89053E2745667C4ED7D3EE0663A95A`.
- `assembleRelease`, `zipalign`, `apksigner`, `aapt` **PASS**, dùng debug signing QA trong release-mode để cài trực tiếp, không debuggable, `minSdk=29` và `targetSdk=36`.
- **Chính APK bàn giao đã cài đặt và khởi động lạnh trên AVD Android API30: `Status: ok`, `LaunchState: COLD`, app process tồn tại — PASS.** Log: `qa-logs/demo-apk-creator-review-v2-package.log`.
- AVD QA riêng `MusicFlow_CreatorReviewV2_QA_API30` được dừng và xóa sau kiểm thử; không can thiệp AVD có sẵn của người dùng.

## Những gì chưa nghiệm thu

- Chưa chạy **E2E HTTP với JWT thật** của 8 tài khoản QA sau khi cấp role. Bộ Android UI tests là RPC giả lập; không được báo tương đương kiểm thử xét duyệt Supabase thật.
- Chưa duyệt/từ chối đơn đang `pending` của tài khoản demo để còn dùng trong buổi báo cáo. Người dùng có thể tự nhập mật khẩu QA do mình quản lý vào giao diện Android/Web; không phải cung cấp mật khẩu cho trợ lý.
- Chưa chạy full media regression, chưa xác minh thiết bị thật, chưa sản xuất/thương mại hóa, chưa rà soát toàn bộ Supabase Security Advisor.

## Hướng dẫn thử trong báo cáo

```powershell
.\scripts\run-creator-application-review-v2-tests.ps1
# Khi emulator QA riêng đã chạy, truyền đúng serial của nó:
.\scripts\run-creator-application-review-v2-tests.ps1 -Serial emulator-5580
```

Để demo thực tế, đăng nhập tài khoản Creator Pending trong Android → mở trang vai trò → xem trạng thái đơn → đăng nhập PlatformAdmin trên Web Admin (hoặc Android) → bấm tải đơn chờ → nhập lý do → chọn Duyệt/Từ chối → xem hộp thoại xác nhận. **Chỉ xác nhận quyết định cuối khi thật sự muốn thay đổi dữ liệu Supabase**; không phải thao tác bắt buộc của mốc QA này.
