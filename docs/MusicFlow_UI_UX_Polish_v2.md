# MusicFlow UI/UX Polish v2 — Android large text and adaptive controls

**Ngày thực hiện:** 2026-10-29. **Nền Git:** `c31c6e1` (UI/UX Foundation v1 đã commit). Mốc này chưa được tự động commit/push.

## Phạm vi đã làm

1. **Now Playing:** ảnh bìa có chiều rộng tối đa 300 dp và tỉ lệ 1:1, tránh ảnh bị kéo rộng; nút Trước/Tạm dừng/Tiếp sử dụng layout một hàng bình thường và chuyển thành cột có nút tối thiểu 48 dp khi chiều rộng nhỏ hơn 360 dp hoặc font scale từ 1.35 trở lên. Thêm `contentDescription` cho Seek và EQ band sliders. Callback, Media3/MediaSession, seek, playback status không thay đổi.
2. **Playlist:** trạng thái Loading/Error/Empty/playlist bị xóa được trình bày với `MusicFlowStatusPanel`, chỉ hướng hành động hợp lý. Tách nút lên/xuống/xóa khỏi `ListItem.trailingContent` sang hàng riêng để có chỗ cho tên bài và nghệ sĩ trên màn hình hẹp/chữ lớn. Thêm nhãn accessibility cho thao tác xóa, giữ tag/ID, thứ tự bài và xác nhận xóa.
3. **Account:** các nút Đăng nhập/Đăng ký Free tự xếp dọc với màn nhỏ hoặc cỡ chữ lớn; theme chips có cuộn ngang. Đưa phần **Giao diện** lên đầu màn hình, vì lựa chọn cuối trang trong hai vùng cuộn lồng nhau khó chạm ở một số viewport; Guest đổi theme mà không phải tìm qua các biểu mẫu Auth. Trạng thái loading/error dùng `MusicFlowStatusPanel`. Không bật provider Google, dùng email thật, hoặc thay đổi Supabase/Auth logic.

## Bằng chứng QA

- `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain`: **BUILD SUCCESSFUL**, JVM unit test task PASS.
- Android emulator `emulator-5554` API 30: `MusicFlowUiPolishTest` **3/3** (Now Playing điều khiển đúng với width 310 dp / font scale 1.8, guest theme và hai nút Auth theo callback giả trong UI test), `PlaylistPlaybackOrderTest` **3/3**, `MusicFlowUiFoundationTest` **10/10**, `QueueManagerScreenTest` **2/2**, `ThemeBatchTest` **1/1** (chạm và khởi động lại theme); tổng **19/19 PASS**.
- Script `scripts/run-musicflow-ui-polish-v2-tests.ps1` và log `qa-logs/musicflow-ui-polish-v2.log`, marker `FINAL_RESULT=PASS`.
- Vòng hồi quy đầu gặp `ThemeBatchTest` **FAIL** do lựa chọn theme nằm cuối màn Account trong các vùng scroll lồng nhau, không nhận thao tác chạm trong viewport test. Đã đưa theme lên trên và giữ kiểm thử **physical click** (không thay bằng invocation giả); chạy lại **19/19 PASS**.

## Giới hạn

- Đã kiểm thử UI với font scale mô phỏng 1.8 và cửa sổ 310 dp trên API 30; **chưa** đạt ma trận đa API, kiểm thử TalkBack thủ công, màn hình gập/tablet thật, và tất cả cấu hình Dynamic Text. Không coi 19/19 là chứng nhận accessibility toàn ứng dụng.
- Crossfade, EQ, lyrics và các tác vụ Player nâng cao chưa được nghiệm thu end-to-end trong mốc giao diện này.
- Chưa có nhạc online thật với quyền streaming đã xác minh; không tuyên bố đã đạt điều kiện licensed online.
- Google Login, email thật, cloud đa thiết bị E2E vẫn ở giai đoạn cuối theo `docs/Pham_Vi_MusicFlow_1_0.md`.

## Việc kế tiếp

**Licensed Online Rights Metadata & Attribution v1:** xác định trường quyền phát/ghi công ở hợp đồng dữ liệu, kiểm soát UI và điều kiện phát, test bằng dữ liệu giả không cần upload nhạc thật; chỉ thử streaming E2E khi chủ dự án có nguồn hợp lệ.
