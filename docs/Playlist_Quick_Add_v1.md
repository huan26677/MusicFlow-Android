# MusicFlow — Playlist Quick Add v1 (bản demo báo cáo)

> **BỔ SUNG NGHIỆM THU 2026-11-07:** Phần DocumentsUI thực từng ghi "chưa kiểm thử" bên dưới đã được bổ sung bài `PlaylistQuickAddRealSafUiTest` và **PASS trên emulator API30** (OpenDocument → persisted READ grant → Room lưu bài + mở được WAV). Ứng dụng cũng kiểm tra tệp thực sự đọc được trước khi lưu. Xem `docs/Nghiem_Thu_Demo_2026_11_07.md`, log `qa-logs/musicflow-demo-acceptance-v1-qa.log`. Các giới hạn từng nêu bên dưới là trạng thái tại thời điểm phát hành Quick Add v1 ban đầu, không phải trạng thái nghiệm thu hiện hành.

**Ngày:** 2026-11-07 (Asia/Ho_Chi_Minh). **HEAD nền:** `a92866a` (Listening Journeys UX v1), local main sạch khi bắt đầu. **Phạm vi:** hoàn thiện trực tiếp hành trình thêm nhạc offline vào playlist, sau đó mới chạy bộ QA tổng hợp. Không sửa schema, Supabase/Auth/Owner/Creator, không phát hành công khai hay làm thay đổi hàng đợi phát đang chạy.

## 1. Hành trình người dùng

**Trước đây:** mở playlist trống → chuyển sang Thư viện → chọn một bài → mở hộp thoại thêm → chọn lại playlist → quay lại danh sách. Khi playlist đã có nhạc, nút thêm bài chưa nằm ở màn chi tiết.

**Hiện tại:** mở playlist (trống hoặc có nhạc) → bấm **Tìm bài hát để thêm / Thêm bài hát vào playlist** → vào màn riêng có tiêu đề **Đang thêm vào: tên playlist** → chọn **Thêm** bên cạnh từng bài → tiếp tục chọn nhiều bài → bấm **Xong** để trở về playlist. Mỗi thao tác thêm được ghi vào Room; không thay queue hay phát bài khi người dùng chỉ muốn lưu vào playlist.

- Nếu cho phép đọc nhạc trên thiết bị: danh sách lấy từ MediaStore, ô tìm theo tên bài hoặc nghệ sĩ, hỗ trợ không dấu; các bài đã có hiển thị **Đã có** và khóa nút Thêm. Trong lúc ghi dữ liệu khóa nút khác và Xong để tránh tác vụ chồng chéo.
- Nếu không cấp quyền quét nhạc: **không tự mở hộp thoại xin quyền**. Người dùng vẫn có thể **Chọn tệp âm thanh riêng** (Android OpenDocument, MIME `audio/*`) hoặc tự bấm **Cho phép quét nhạc** khi muốn thấy MediaStore.
- Tệp SAF chỉ được thêm khi Android cho phép lưu **quyền đọc tồn tại qua lần khởi động lại** (`takePersistableUriPermission`). Nếu không lưu được quyền, báo lỗi và **không tạo mục playlist hỏng**. Media ID là URI content bất biến để tránh thêm trùng; tên tệp đọc từ `OpenableColumns.DISPLAY_NAME`.
- Nếu playlist bị xóa hoặc không còn truy cập được, chỉ hiển thị lỗi và nút quay lại, không cho thêm bài vào một playlist không tồn tại.
- Logic phân biệt album, nhạc online bản quyền, tài khoản Free/Premium/Creator/Owner được giữ nguyên. Danh sách vẫn là dữ liệu cục bộ; không tải file lên Supabase và không cần đăng nhập.

## 2. Code / QA

| Thành phần | Công việc |
|---|---|
| `core/playlist/PlaylistQuickAddRules.kt` | Quy tắc media ID thống nhất, kiểm tra trùng, tìm kiếm không dấu; `PlaylistViewModel` dùng đúng key cùng UI |
| `feature/playlist/PlaylistQuickAddScreen.kt` | Màn chọn nhiều bài, quyền quét tùy ý, bộ chọn SAF, trạng thái đang lưu, thông báo thành công/lỗi và empty state |
| `navigation/MusicFlowApp.kt` | Route `PlaylistAddSongs(playlistId)` chính thức, trả về playlist chi tiết khi nhấn Xong/Back |
| `feature/playlist/PlaylistScreens.kt` | Nút thêm bài cho playlist có nhạc, CTA playlist trống chuyển thẳng vào màn Quick Add |
| `app/src/test/.../PlaylistQuickAddRulesTest.kt` | **4 JVM tests**: khóa khi saving, key MediaStore, URI SAF, tìm tiếng Việt không dấu |
| `app/src/androidTest/.../PlaylistQuickAddUiTest.kt` | **5 Android UI tests**: không xin quyền tự động, tìm/thêm một chạm, xử lý playlist không tồn tại, chữ lớn/khóa khi saving và **ghi thật vào Room/test-owned playlist** rồi xóa dữ liệu QA |
| `scripts/run-playlist-quick-add-v1-qa.ps1` | Full JVM + Debug/AndroidTest build; Android chọn một nhóm liên quan playlist, player, tìm kiếm, offline và hàng đợi trên serial QA cô lập |

**Kết quả cuối đã xác minh:**

- **180/180 JVM PASS**, 31 suites, không failures/errors.
- **30/30 Android instrumentation PASS** trên AVD riêng Android API30; có **5 test mới**, hồi quy PlaylistRoom, chuyển thứ tự phát, Home/Search/Player và `OfflineAcceptanceFlowTest` (phát offline, lịch sử, queue restoration), không tác động tài khoản Supabase.
- Gradle Debug/AndroidTest/Release **PASS**; `git diff --check` **PASS**.
- Lần build đầu **FAIL** do nhầm import `LifecycleEventEffect`. Đã sửa sang `androidx.lifecycle.compose.LifecycleEventEffect`, **chạy lại toàn bộ nhóm QA**, kết quả cuối trên. Không bỏ qua testcase lỗi.

**Lưu ý giới hạn QA:** Các bài UI mới dùng trạng thái MediaStore giả lập và một lượt lưu metadata playlist thật vào Room. Kiểm thử **không tự động thực hiện thao tác OpenDocument của hệ thống trong màn Quick Add**, nên việc cấp persistable grant với nhà cung cấp tệp thực tế còn cần nghiệm thu thủ công nếu mở rộng sau báo cáo. `OfflineAcceptanceFlowTest` là bài playback tích hợp bằng MediaStore trên emulator, không phải chứng nhận cho toàn bộ thiết bị hoặc audio streaming online.

## 3. Tệp APK demo

- Đường dẫn **trên máy phát triển** (không phải link sandbox): `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-PlaylistQuickAddV1-Android.apk`.
- Kích thước **31.213.245 bytes**; SHA256 `2A2B681DD1FEC0EF7DC41C64485BA08FBCEADEFECC354819417CB6D8077797CA`.
- Package `com.example.musicflow`, version `1.0`/`1`, minSdk `29`, targetSdk `36`, `debuggable=false`.
- APK release-mode dùng **debug keystore QA**, kiểm tra `zipalign`, `apksigner`, `aapt`, SHA256 **PASS**. Đã cài chính APK và khởi động lạnh emulator API30: `Status: ok`, `LaunchState: COLD`, app process còn chạy **PASS**.
- QA-only AVD: `MusicFlow_PlaylistQuickAddV1_QA_API30` serial `emulator-5580`; **dọn sau kiểm thử**, giữ AVD của người dùng và emulator `5554` nếu đang chạy.
- Log: `qa-logs/musicflow-playlist-quick-add-v1-qa.log`, `qa-logs/demo-apk-playlist-quick-add-v1-package.log`.

## 4. Cách thử ngắn trong bản demo

1. Mở MusicFlow → Playlist → tạo hoặc chọn playlist → **Tìm bài hát để thêm / Thêm bài hát vào playlist**.
2. Nếu có nhạc đã quét, tìm theo tên tiếng Việt không dấu, bấm **Thêm**; nút bài vừa có trong playlist đổi thành **Đã có** sau khi Room cập nhật. Chọn tiếp bài khác rồi **Xong** để kiểm tra danh sách.
3. Nếu không có quyền quét, bấm **Chọn tệp âm thanh riêng**, chọn một tệp âm thanh hợp lệ qua Android DocumentsUI, sau đó **Xong**. Việc file có thể phát sau khi restart phụ thuộc vào quyền SAF từ tài liệu của nhà cung cấp, chưa được QA thật trong đợt này.
4. Nhấn **Phát toàn bộ** để chứng minh queue theo đúng thứ tự, hoặc sắp xếp lại bài.

**Bước sau (trong phạm vi báo cáo):** bổ sung ảnh minh họa và checklist nghiệm thu từng actor/use case; smoke thủ công luồng OpenDocument Quick Add trên emulator, nếu thuận tiện thử TalkBack và Auth 8 persona bằng tài khoản QA do chủ dự án tự nhập mật khẩu. Không tự phát hành Play/production hoặc mở streaming thiếu giấy phép.
