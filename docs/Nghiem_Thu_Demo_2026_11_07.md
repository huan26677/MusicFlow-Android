# MUSICFLOW — Biên bản nghiệm thu APK demo (07/11/2026)

**Loại dự án:** ứng dụng nghe nhạc Android, APK sideload và tài liệu báo cáo môn học. **Mốc nguồn:** `249ae22` (Playlist Quick Add v1) trước các chỉnh sửa nghiệm thu trong working tree. **Trạng thái:** **ĐẠT các cổng kiểm thử được liệt kê dưới đây**. Đây là nghiệm thu demo **không phải** xác nhận sẵn sàng phát hành thương mại.

## 1. Kết quả thực tế

| Cổng nghiệm thu | Kết quả | Bằng chứng |
|---|---|---|
| Toàn bộ JVM unit tests | **180/180 PASS**, 31 suites, 0 failures/errors | `app/build/test-results/testDebugUnitTest`, `qa-logs/musicflow-demo-acceptance-v1-qa.log` |
| Android regression trên emulator API30 | **55/55 PASS** | Cùng log, nhóm Account, Creator, Owner, Moderator, Room Playlist, Player/Queue, OfflineAcceptanceFlowTest |
| Android DocumentsUI chọn tệp SAF **thật**, không fake result | **1/1 PASS** | `PlaylistQuickAddRealSafUiTest`, cùng log |
| Web Admin role/permission fail-closed | **4/4 PASS** | `scripts/test-admin-web-role-refresh-v1.js`, cùng log |
| Gradle Debug, AndroidTest, Release | **PASS** | Log build + `qa-logs/demo-apk-acceptance-v1-package.log` |
| ZIP alignment, APK signature, manifest, SHA256 | **PASS** | `qa-logs/demo-apk-acceptance-v1-package.log` |
| Cài và cold launch **chính APK ký QA** trên Android API30 | **PASS** | ADB: `Status: ok`, `LaunchState: COLD`, `MainActivity` và app process còn chạy |
| Ảnh chụp từ APK demo trên API30 | **Đã lưu** | `qa-logs/screenshots/musicflow-demo-acceptance-home-api30.png` — màn Trang chủ Guest, không có thông tin tài khoản |

**Tổng Android trong lượt nghiệm thu:** 56 ca PASS (**55 regression + 1 DocumentsUI thực**). Các bài test có dùng dữ liệu QA nội bộ trên emulator riêng, không chạy bằng mật khẩu/role JWT sống của 8 persona. Tệp âm thanh test WAV là dữ liệu tự tạo và được xóa; playlist QA được tạo/xóa trong Room, không ảnh hưởng playlist người dùng.

## 2. Bổ sung nghiệm thu UC-G13 — Playlist Quick Add SAF

Trước đây Quick Add v1 đã có QA bằng giao diện Compose với trạng thái giả và một phép ghi Room thật; **chưa chạy trình chọn tệp của hệ thống**. Lượt này bổ sung bài test tự động `PlaylistQuickAddRealSafUiTest` theo luồng:

1. Tạo một playlist và tệp WAV ngắn **test-owned** trong `MediaStore.Downloads`, không sử dụng nhạc của bên thứ ba.
2. Mở **`PlaylistQuickAddRoute` thật**, bấm nút **Chọn tệp âm thanh riêng**.
3. Đi qua **Android DocumentsUI** (`ACTION_OPEN_DOCUMENT`), mở vị trí **Downloads** rồi chọn tệp thử nghiệm.
4. Android trả **content URI thực** vào callback `OpenDocument()`. Ứng dụng gọi `takePersistableUriPermission(READ)` và **mở được tệp ở chế độ đọc** trước khi lưu Room; callback từ chối nếu không còn quyền hay không đọc được.
5. Đợi Room có **đúng một bài** trong playlist đã chọn; xác minh `audioUri == mediaId == URI` và hiển thị **Đã thêm bài vào ...** trên giao diện.
6. Kiểm tra `persistedUriPermissions` có quyền READ và mở được descriptor từ URI, rồi xóa playlist/tệp QA và giải phóng quyền test.

**Kết quả cuối: PASS 1/1 trên API30**. Lượt kiểm thử đầu thất bại vì DocumentsUI mặc định ở mục Recent trống, còn tệp QA đặt ở thư mục Music; không phải lỗi phát nhạc. Kịch bản QA được sửa để tạo file thuộc Downloads và điều hướng chọn đúng root. Test chạy riêng **PASS**, sau đó **chạy lại cả bộ nghiệm thu 55+1 PASS**. Hành vi `openFileDescriptor(uri,"r")` đã được bổ sung trong ứng dụng để tránh lưu file bị thu hồi quyền/không thể đọc được vào playlist.

**Giới hạn:** Test khẳng định quyền READ đã được lưu lâu dài bởi Android, nhưng **không buộc dừng process/force-stop rồi phát lại ngay trong chính test SAF này**. Phát nhạc offline và queue restart đã có bài tích hợp riêng. Không suy ra mọi nhà cung cấp tài liệu của bên thứ ba và mọi phiên bản Android đều hoạt động giống emulator API30.

## 3. APK bàn giao mới nhất

| Thuộc tính | Giá trị |
|---|---|
| Tên | **`MusicFlow-1.0-Demo-AcceptanceV1-Android.apk`** |
| Đường dẫn trên máy phát triển | `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-AcceptanceV1-Android.apk` |
| Dung lượng | **31.213.245 bytes** (xấp xỉ 31,2 MB) |
| SHA-256 | **`7D7E1D0131DC078CAA6394F1FC14F43BBB8820895FFF260C2BEF4C399CC95889`** |
| Application ID | `com.example.musicflow` |
| Version | `versionName=1.0`, `versionCode=1` |
| Android | `minSdk=29`, `targetSdk=36` |
| Chữ ký | Debug keystore QA của dự án; **APK Release-mode không debuggable**, chỉ để demo |
| Google Play / production billing | **Không thuộc phạm vi** |

**Không mặc định đính kèm APK vào repository GitHub:** file `dist/` nằm tại máy phát triển và có thể copy/cài trực tiếp. Không tự công bố mã nguồn hay file APK qua Internet.

## 4. Hướng dẫn nghiệm thu nhanh trên emulator

1. Cài APK mới nhất từ `dist/` vào emulator Android API29 trở lên, không gỡ APK cũ nếu cần giữ dữ liệu và chữ ký cùng khóa QA (nếu khác chữ ký, cần sao lưu trước).
2. Mở MusicFlow → Guest → Thư viện → tệp WAV/MP3/FLAC demo. Phát bài, tạm dừng, mở Player, chuyển bài và xem lịch sử.
3. Mở Playlist → tạo playlist → **Thêm bài hát vào playlist**. Thêm nhiều bài trong cùng màn, kiểm tra khóa nút **Đã có**.
4. Không cần cấp quyền quét MediaStore nếu muốn chọn tệp đơn qua **Chọn tệp âm thanh riêng**; thao tác Files/Downloads trên Android, kiểm tra bài có trong playlist.
5. Trang Tài khoản chỉ dùng để giới thiệu vai trò và quyền; không bắt buộc đăng nhập email thật để demo offline.

Kịch bản đầy đủ và phạm vi actor: `docs/Checklist_Acceptance_Demo_MusicFlow_v1.md`, `docs/Actor_UseCase_MusicFlow_Bao_Cao.md`.

## 5. Những gì **chưa** được nghiệm thu E2E

- Đăng nhập JWT thật đầy đủ 8 persona QA và thay đổi role/entitlement thật theo toàn chu trình Owner → Admin → Creator → Moderator.
- Đồng bộ cloud trên 2 thiết bị, production Google OAuth, Premium billing/thanh toán thật.
- Xuất bản streaming online với xác minh giấy phép thương mại và nội dung có bản quyền.
- Full instrumentation xuyên mọi phiên bản Android, TalkBack thủ công với người dùng, thiết bị vật lý, độ ổn định của tất cả SAF providers ngoài emulator.

Những hạn chế này **không cản trở APK demo nghe nhạc offline cho báo cáo**, nhưng phải trình bày trung thực, không đánh dấu PASS ngoài chứng cứ thực tế.

## 6. Tái hiện trên máy phát triển

```powershell
# Full JVM + Web Admin + Android regression + DocumentsUI (cần QA emulator riêng):
.\scripts\run-demo-acceptance-v1-qa.ps1 -Serial emulator-5580

# Build và kiểm tra APK demo đã ký QA:
.\scripts\build-musicflow-demo-apk-v1.ps1 -OutputName 'MusicFlow-1.0-Demo-AcceptanceV1-Android.apk'
```

**Dừng phát triển tại phạm vi báo cáo:** Không phát hành production hoặc thay đổi role/cấu hình tài khoản thật khi chỉ làm demo; nếu sau này chủ dự án muốn triển khai thực tế sẽ cần kế hoạch security/auth/copyright/billing độc lập.
