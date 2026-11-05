# MusicFlow Moderator Review v1 — Kiểm duyệt metadata (dự án báo cáo)

**Ngày:** 2026-11-05. **Phạm vi:** role Moderator đã có trong hệ thống nhưng chưa thao tác được; mốc này bổ sung **duyệt hồ sơ tác phẩm đã gửi**. Không triển khai kiểm duyệt thương mại, khóa người dùng, đối soát pháp lý hay phát hành nhạc trực tuyến. Không yêu cầu điện thoại thật, host web hay tài khoản Owner thật cho targeted QA.

## Actor → Use Case

| Actor | Use Case | Quyền thực tế |
|---|---|---|
| Moderator | UC-M01: Xem danh sách bản nháp đã gửi | RPC `musicflow_pending_drafts` |
| Moderator | UC-M02: Đọc metadata, credit, khai báo giấy phép | Chỉ bản nháp `submitted` |
| Moderator | UC-M03: Xem bản thu mẫu & chứng cứ riêng tư | RLS Storage + `musicflow_creator_upload_list` giới hạn submitted/metadata_approved |
| Moderator | UC-M04: Duyệt metadata hoặc yêu cầu bổ sung | RPC `musicflow_review_draft`, bắt buộc lý do 5–300 ký tự, ghi audit; tự duyệt bị chặn |
| Moderator | UC-M05: Duyệt người dùng thành Artist/Author | **KHÔNG**: dành cho Owner/PlatformAdmin có `review_creators` |
| Moderator | UC-M06: Cấp role / thao tác catalog public / khóa user | **KHÔNG**: không được gọi API, không hiện nút quản trị |

## Triển khai

- Migration idempotent: `docs/sql/musicflow_moderator_review_v1.sql`. Reuse bảng role và Storage đã có, thêm `musicflow_can_review_metadata()`; các API đều giới hạn authenticated và kiểm tra `auth.uid()` với role thực trên backend.
- Recreate 4 hàm cũ với kiểm tra quyền: `musicflow_pending_drafts`, `musicflow_review_draft`, `musicflow_creator_upload_list`, `musicflow_creator_storage_can_read`.
- Ứng dụng Android: `MusicFlowAccess.canReviewMetadata` không đồng nhất với `canReviewCreators`, menu Moderator từ màn Tài khoản, chỉ cho mở `CreatorDraftReviewPanel`.
- `admin-web/index.html`: Moderator có thể đăng nhập và thấy **chỉ** khu vực "Duyệt bản nháp", không thấy users/owner/duyệt vai trò/catalog.
- `CreatorDraftReviewPanel` dùng form nhập lý do, lưu quyết định và ghi audit như trước.
- **Không** chỉnh sửa file offline trên máy người nghe, không cập nhật `public.music_tracks`, không cấp phép streaming. File upload vẫn ở bucket private tối đa 6MB/file.

## Kiểm thử và giới hạn

- JVM: role Moderator được xem metadata nhưng không được role applications/users/online/Developer; forged permissions vẫn bị chặn.
- Android instrumentation: Moderator thấy nút "Tải bản nháp chờ duyệt", không thấy quản trị user, cấp role, catalog hay Developer QA.
- Supabase SQL: không Owner được tạo; anon không thể gọi RPC; tài khoản authenticated thường bị từ chối xem/duyệt; không có policy Storage public.
- JS syntax và APK debug/cài/cold start ở emulator QA **nếu được chạy**; giữ tất cả emulator của người dùng.
- **Không kiểm thử E2E Moderator thật** vì chưa có Owner xác minh/cấp role cho tài khoản Moderator thử nghiệm. Không giả kết quả.
- **Đối soát hợp đồng/kiểm duyệt giấy phép thực tế** là việc của người có quyền/đủ giấy tờ; metadata approval không đồng nghĩa bản thu được phát online.

## Tài liệu liên quan

- `Actor_UseCase_MusicFlow_Bao_Cao.md`: actor/UC tổng thể.
- `Huong_Dan_Demo_Bao_Cao_Mien_Phi.md`: cách dựng demo và xác minh Owner không tốn phí.
- `Ke_Hoach_Du_An.md`: nhật ký chính; mã nguồn và hạn chế chưa E2E.
- `qa-logs/musicflow-moderator-review-v1.log`: nhật ký targeted QA của lượt này.

### Kết quả targeted QA 2026-11-05

- Supabase script triển khai **PASS**, 5 API liên quan chỉ grant authenticated, deny anon; authenticated thường không có role bị từ chối xem queue, review metadata và Storage. **0 Owner, 0 Moderator, 0 creator draft và 0 upload** sau kiểm tra; không sửa dữ liệu user.
- Gradle `testDebugUnitTest --tests MusicFlowAccessTest` + `assembleDebug` + `assembleDebugAndroidTest` **PASS**. **8/8 JVM** (gồm 2 bài về Moderator), **5/5 Android Roles UI** (gồm vai trò Moderator) **PASS** trên emulator QA riêng API30.
- Admin web JavaScript syntax PASS. APK mới ký QA, verify và cold launch **PASS**: `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-ModeratorReviewV1-Android.apk`, **31.033.021 byte**, SHA-256 `74BC2CDE4CC375FF5FC6628E1F5E31CA781359E9A7AD407850DFF61874BD303E`. Guest không thấy công cụ Moderator/Owner/Admin; nút Google vẫn hiện nhưng disabled.
- AVD QA riêng đã dọn cả data/ini; giữ nguyên emulator sẵn có `emulator-5560`. Không chạy full media regression, không thử Moderator thật vì chưa có Owner được xác minh.
- Supabase Security Advisors còn cảnh báo cũ (RLS private không policy là deny-by-default theo thiết kế và nhóm `SECURITY DEFINER` cần tiếp tục kiểm tra trước khi phát hành thật). Bản này **chỉ là demo báo cáo**, không production ready.

