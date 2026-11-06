# MusicFlow — Listening Journeys UX v1 (APK demo / báo cáo)

**Ngày:** 2026-11-07, Asia/Ho_Chi_Minh. **HEAD nền:** `34954e4` (Account Journeys UX v4), local `main` sạch lúc bắt đầu. **Phạm vi:** một đợt hoàn chỉnh tối ưu trải nghiệm người nghe **Home / Player / Playlist / Search**, rồi chạy kiểm thử tích hợp một lần và lặp lại **cả bộ** sau khi khắc phục các lỗi QA. Không sửa cơ chế Media3 queue, không ghi/đọc mật khẩu hoặc thay đổi dữ liệu Supabase/Owner/Creator, không bổ sung streaming chưa được cấp phép.

## 1. Các cải thiện đã triển khai

| Màn hình | Hành vi mới |
|---|---|
| **Trang chủ / lịch sử nghe** | Nếu có trên 4 bài gần đây, chỉ hiện 4 bài đầu với nút **Xem tất cả N bài đã nghe / Thu gọn lịch sử**. Chọn bài thứ N vẫn chuyển **toàn bộ hàng đợi lịch sử** và đúng vị trí N, không sửa dữ liệu Room. Nếu ít hơn 5 bài không tạo nút thừa. |
| **Player trống** | Ngay phía trên giao diện có hướng dẫn **Chưa chọn bài hát**, lối tắt mở thư viện hoặc chọn tệp offline mà **không yêu cầu tài khoản**. Không lặp hai lối tắt này ở cuối màn hình trống. |
| **Player đang phát** | Nhãn hàng đợi **Bài 1 / N** hiển thị đúng vị trí 1-based (thay cho khả năng hiện Bài 0 / N); khi trống hiển thị **Chưa có bài trong hàng đợi**. Nút quản lý hàng đợi chỉ bật khi thực sự có bài. Nút Trộn/Lặp có vùng bấm tối thiểu 48dp; chuyển xếp dọc khi màn hẹp / phóng chữ lớn để tránh ép chữ. Không viết lại logic phát, shuffle hay repeat. |
| **Playlist trống** | Có nút **Tìm bài hát để thêm** mở ngay tab Thư viện, chỉ dẫn chọn **Thêm vào playlist**, không tạo audio giả hay chuyển file. Nút này được nối với navigation thật từ `MusicFlowApp`, không chỉ là text trạng thái. |
| **Playlist đặt/đổi tên** | Không cho **Lưu** tên đổi mà chưa thay đổi sau chuẩn hóa; nhấn Done trên bàn phím có cùng hành vi Lưu hợp lệ, giảm thao tác. Tạo playlist vẫn hỗ trợ như cũ, ràng buộc tối đa 60 ký tự. |
| **Tìm kiếm** | Nếu tab hiện tại không có kết quả mà tab khác có, hiển thị một nút **Có kết quả ở Album/Nghệ sĩ/Thể loại/Bài hát — xem ngay**. Nhấn chuyển trực tiếp tab, không gọi quét lại MediaStore. Các filter chip có touch target tối thiểu 48dp, giữ tìm không dấu và cách phát hàng đợi hiện có. |
| **Giao diện tài khoản, chữ lớn** | Đợt regression phát hiện lựa chọn Sáng/Tối trong bản tài khoản cũ khó thao tác trên màn hẹp chữ lớn. Đã chuyển ba tùy chọn giao diện thành **nút xếp dọc** ở width nhỏ / font scale lớn (vẫn hàng ngang ở thiết bị rộng), bổ sung test tags; không thay đổi logic role/tier. |

## 2. File thay đổi

- **Mới:** `app/src/main/java/com/example/musicflow/core/ux/ListeningJourneyRules.kt`, các quy tắc UI độc lập cho preview lịch sử, hàng đợi và chuyển nhóm tìm kiếm.
- **Android UI:** `feature/home/HomeScreen.kt`, `feature/player/NowPlayingScreen.kt`, `feature/playlist/PlaylistScreens.kt`, `feature/search/SearchScreen.kt`, `feature/account/AccountScreen.kt`, `navigation/MusicFlowApp.kt`.
- **Tests:** `app/src/test/java/com/example/musicflow/core/ux/ListeningJourneyRulesTest.kt` (4 mới), `app/src/androidTest/java/com/example/musicflow/ui/ListeningJourneysV1UiTest.kt` (5 mới), cập nhật bài test cũ `MusicFlowUiPolishTest.kt`, `AdvancedSearchRuntimeTest.kt`, `PlaylistPlaybackOrderTest.kt` cho layout hiện tại/asynchronous Room và MediaStore.
- **Một script QA cuối đợt:** `scripts/run-listening-journeys-v1-qa.ps1`. Chạy full JVM + assembleDebug + androidTest; chỉ chạy Android khi truyền serial của AVD QA riêng. Không thay quyền hay dữ liệu Supabase.
- **Bằng chứng:** `qa-logs/musicflow-listening-journeys-v1-qa.log`, `qa-logs/demo-apk-listening-journeys-v1-package.log`.

## 3. Nghiệm thu — cuối đợt

| Cổng | Kết quả cuối |
|---|---|
| Full `:app:testDebugUnitTest` | **176/176 PASS**, 30 JVM suites, 0 fail, 0 error |
| Tích hợp Android (Home, Player, Search, Playlist, Queue, thao tác chữ lớn và MediaStore) | **27/27 PASS** trên emulator API30 riêng, gồm 5 UI cases mới và 22 regression cases |
| Gradle assembleDebug, assembleDebugAndroidTest, assembleRelease | **PASS** |
| APK QA release-mode: zipalign, chữ ký debug QA, aapt/manifest, SHA256 | **PASS**, không debuggable |
| Cài nâng cấp + cold launch **đúng APK bàn giao** trên Android 11/API30 | **PASS**, `Status: ok`, `LaunchState: COLD`, app process tồn tại |

**Ghi rõ các lượt không đạt:** Lượt Android đầu có **2 bài cũ FAIL** vì nhãn Guest đã đổi từ Account Home v4 và test chọn chữ “Đăng nhập” trùng nút/tab; đã sửa fixture theo đúng test tag. Các lần chạy sau có 2 trường hợp QA: lựa chọn giao diện tối khi font lớn chưa thao tác được trong layout ngang, đã **sửa UI thực** xếp dọc; test Album bằng MediaStore chọn chip khi chưa ổn định semantics, thêm chờ/tag unmerged; một lần bài test sắp xếp Playlist chưa đợi Room hoàn tất, đổi điểm chờ sang LazyColumn thật. Mỗi lần sửa đều chạy lại **full nhóm Android**, kết quả cuối **27/27 PASS**. Các lỗi import trong file test phát hiện khi biên dịch cũng đã sửa.

**Phân biệt phạm vi:** 27 bài Android là **bộ tích hợp được chọn** liên quan trải nghiệm nghe, không phải toàn bộ tập instrumentation hay nghiệm thu thiết bị thật. Full JVM unit tests là toàn bộ test JVM hiện có. Chưa làm E2E JWT 8 account, chưa nghiệm thu Bluetooth/lockscreen/SAF thực trên máy người dùng, chưa chạy tất cả bài Media3 runtime hoặc TalkBack thủ công trên mọi API. Không được coi những mục đó là PASS.

## 4. APK demo

- `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-ListeningJourneysV1-Android.apk` — **31.180.477 bytes**.
- SHA256: `379E35F7D2AC2A30E60D3ECAEB5625E7E7662855D0A1997EB3D54237E1D93440`.
- `package=com.example.musicflow`, versionName `1.0`, versionCode `1`, minSdk `29`, targetSdk `36`, not debuggable; dùng **debug keystore QA** trong Release-mode. Đây là APK sideload **chỉ phục vụ báo cáo**, chưa phù hợp sản xuất/Google Play.
- AVD mới chỉ dành cho lượt test: `MusicFlow_ListeningJourneysV1_QA_API30`, serial `emulator-5580`. Chỉ dọn AVD này, giữ các emulator người dùng đang có, đặc biệt emulator-5554 đang chạy sẵn.

## 5. Cách lặp lại QA và kế hoạch tiếp theo

```powershell
.\scripts\run-listening-journeys-v1-qa.ps1
# Chỉ khi đã khởi động emulator QA riêng:
.\scripts\run-listening-journeys-v1-qa.ps1 -Serial emulator-5580
```

**Tiếp theo cho APK báo cáo:** chốt checklist actor/use case, thực nghiệm thủ công phần nghe offline, SAF, phát nền/lockscreen trên máy thật hoặc emulator API cao khi thuận tiện; thử từng tài khoản QA qua phiên đăng nhập thực do chủ dự án tự nhập; kiểm tra tác vụ đăng nhập/Creator bằng JWT, TalkBack và chốt một APK/tài liệu final. Không mở thương mại/billing/streaming thiếu giấy phép.
