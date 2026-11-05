# MusicFlow — Playlist UX v3 (APK demo / dự án báo cáo)

**Ngày:** 2026-11-05 · **HEAD nền:** `1ffe5d193d907fcd638f3dbe1eadea2dd4eb1814`  
**Phạm vi:** chỉ cải thiện tìm kiếm Playlist và thao tác trên bài hát, hỗ trợ cỡ chữ lớn; không phát hành thật.

## Những thay đổi đã triển khai

1. **Tìm kiếm playlist trực tiếp:** ô `Tìm playlist`, tìm không phân biệt hoa/thường hoặc dấu tiếng Việt (ví dụ `nhac thu` tìm được `Nhạc thư`). Danh sách và thứ tự gốc không bị sửa; hiển thị số kết quả, trạng thái không có kết quả và nút `Xóa tìm kiếm`. Từ khóa được lưu theo UI bằng `rememberSaveable`. Khi chưa tạo playlist nào, thông báo hướng dẫn tạo mới vẫn giữ nguyên.
2. **Playlist dễ đọc hơn khi phóng to chữ:** tiêu đề tách thành hai dòng/bố cục dọc nếu màn hình hẹp hoặc font scale từ 135% trở lên; tên playlist hiển thị tối đa hai dòng. Nút tùy chọn danh sách sử dụng icon ba chấm Material có nhãn TalkBack đầy đủ, không chiếm nhiều chỗ.
3. **Menu thao tác cho từng bài hát:** thay hàng ba nút riêng biệt (Lên/Xuống/Xóa) bằng một nút `Tùy chọn bài hát`, trong đó có `Đưa lên một vị trí`, `Đưa xuống một vị trí` và `Xóa khỏi playlist`. Thao tác di chuyển tại đầu/cuối danh sách tự khóa khi không hợp lệ. **Chọn Xóa vẫn chỉ mở hộp thoại xác nhận**; không xóa file âm thanh hoặc lịch sử nghe.
4. **Không thay đổi Room repository, thứ tự playlist hay Media3:** giữ logic create/rename/delete/reorder và queue phát nhạc hiện hữu. Không liên quan tới Supabase, Google, cloud hay quyền streaming.

Mã nguồn chính: `feature/playlist/PlaylistScreens.kt`, `PlaylistTrackOptions.kt`, `PlaylistNameFilter.kt`, thêm kiểm thử JVM/Android và điều chỉnh hai bài kiểm thử playlist hiện hữu để dùng menu mới.

## Kiểm thử mục tiêu

Chỉ chạy kiểm thử liên quan, **không chạy full regression**:

| Kiểm tra | Kết quả |
|---|---|
| `PlaylistNameFilterTest`: tìm không dấu, hoa/thường, từ khóa trắng, không khớp | **4/4 JVM PASS** |
| `MusicFlowPlaylistPolishV3Test`: menu ở font scale 180%, khóa nút di chuyển không hợp lệ, callback xóa | **2/2 Android PASS** |
| Hai test cũ `PlaylistPlaybackOrderTest`: đổi thứ tự/phát, tạo/đổi tên/xóa | **2/2 Android PASS** |
| `PlaylistSearchUiTest`: tạo QA playlist, tìm không dấu, mở playlist, trạng thái rỗng, xóa từ khóa, dọn QA | **1/1 Android PASS** |
| `:app:assembleDebug :app:assembleDebugAndroidTest` và targeted JVM | **BUILD SUCCESSFUL** |
| APK demo release-mode: build/zipalign/ký QA/apksigner/aapt/SHA-256 | **PASS** |
| Chính APK đã đóng gói: cài nâng cấp/cold start, Trang chủ → Danh sách phát và màn rỗng | **PASS** |

Android QA trên **AVD riêng** `MusicFlow_PlaylistUX_QA_API30` (API 30, `emulator-5560`). Những thao tác Room trong test tạo rồi xóa **chính playlist QA** trên AVD này; không thay đổi playlist hoặc tài khoản đang có trên `emulator-5556` của người dùng. Sau QA, AVD tạm được xóa.

Chưa kiểm thử điện thoại thật, trình đọc màn hình TalkBack thủ công hoặc toàn bộ tổ hợp độ phân giải/font scale; kết quả là kiểm thử mục tiêu tại font scale 180% và Android API 30.

## APK mới

```text
E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-PlaylistUXV3-Android.apk
```

- **30.787.261 byte**; SHA-256 `C40F59CE54747DC54637147C375E98263F4D774C92E6CB5568E44BA5D230A3AD`.
- Package `com.example.musicflow`, version `1.0`, Android minSdk 29 / targetSdk 36.
- **Release-mode QA/sideload, ký bằng Android debug keystore**, không debuggable. **Không phải bản production/Google Play**; không tạo keystore phát hành mới.
- `dist` bị Git ignore nên APK và `.sha256` không được đẩy lên GitHub. Các APK của mốc trước vẫn được giữ nguyên.
- Để build lại: `scripts/build-musicflow-demo-apk-v1.ps1 -OutputName MusicFlow-1.0-Demo-PlaylistUXV3-Android.apk`. Script từ chối ghi đè APK cùng tên nhưng SHA khác.

## Log / bàn giao

- `qa-logs/musicflow-playlist-ux-v3.log` — kết quả kiểm thử mục tiêu.
- `qa-logs/demo-apk-playlist-ux-v3-package.log` — log build/ký APK.
- `docs/Ke_Hoach_Du_An.md` — tóm tắt và nhật ký tiến độ cho chat sau.

**Trạng thái:** hoàn thành trong phạm vi demo/báo cáo. Chưa phát hành thật, không cần email hay thiết bị thật.
