# MusicFlow — Full Product Regression Gate v2 / Part 1

**Ngày:** 2026-11-01. **Git HEAD nền:** `87efadd` (`main`, Advanced Search Runtime v1 đã commit). **Kết quả được xác minh từ Gradle + JUnit XML mới trên emulator Android 11 / API 30:** **105/105 JVM PASS; Android 112 PASS / 0 FAIL / 3 SKIP trên 115 testcase**. Trạng thái gate ở phạm vi này: **PASS**; **chưa nghiệm thu MusicFlow 1.0**.

## Mục tiêu và phạm vi

- Chạy `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` và **toàn bộ** `:app:connectedDebugAndroidTest` **không lọc bài** trên `emulator-5554`, `Pixel_5_API_30`. Đã có 115 Android testcase được công cụ chạy phát hiện sau những mốc Sleep Timer, Lyrics, Equalizer, Crossfade, Playback Speed, Queue Actions, Advanced Library/Search, v.v.
- Xóa Android JUnit XML cũ trước lần chạy, đọc từng `testcase` từ **XML mới** để báo số PASS, FAIL, SKIP. Android instrumentation có thể trả mã trạng thái gây hiểu nhầm nếu chỉ nhìn console; chỉ kết luận từ Gradle và XML. Kiểm tra `git diff --check`.
- Ba bài `SafPermissionRestartTest` phụ thuộc **thứ tự pha và force-stop thật** được SKIP theo thiết kế trong lượt full unfiltered, không tính thành PASS. Đã có bằng chứng test theo pha trên API 29/30/33/36 ở `docs/Android_Compatibility_Matrix_v2.md`, nhưng **không chạy lại ma trận đó trong mốc này**.
- Toàn bộ nội dung test sử dụng nhạc offline sẵn có do QA tạo; không kích hoạt streaming nội dung chưa có quyền. Tệp nhạc offline do người dùng tự tải/tự lưu **không yêu cầu kiểm tra giấy phép streaming**.

## Diễn biến thực tế

| Lượt kiểm thử | JVM | Android API 30 | Nhận định |
|---|---|---|---|
| Baseline v2 không lọc | **105/105 PASS** | **110 PASS / 2 FAIL / 3 SKIP** (115) | FAIL ở hai bài CreateDocument của `RealPickerSafTest`, chưa đạt gate |
| Chạy lại riêng hai bài sau sửa helper | Không chạy JVM mới | **2/2 PASS** | Đã tạo/chọn WAV bằng DocumentsUI thật, nhận URI, persisted READ, MediaSession playback, Next/History |
| Full v2 sau sửa | **105/105 PASS** | **112 PASS / 0 FAIL / 3 SKIP** (115) | `GRADLE_ANDROID_EXIT=0`, JUnit XML mới, **GATE PASS** |

**Hai testcase từng bị lỗi**:

- `RealPickerSafTest.realOpenMultipleDocumentsGrantsPersistedReadAndPlays`
- `RealPickerSafTest.realOpenMultipleDocumentsSelectsTwoFilesAndNavigates`

Lỗi xảy ra khi test chuẩn bị WAV QA qua `ActivityResultContracts.CreateDocument`, trước khi vào luồng phát. Helper cũ chỉ tìm nút Save/Lưu bằng text hoặc nút `action_menu_save` của toolbar. Trên DocumentsUI API 30, nút Save thật ở **`android:id/button1`**; trong một số trạng thái accessibility báo cửa sổ gốc là `android` dù `PickActivity` đang focus. Vì thế test đã không bấm được Save, **không phải bằng chứng lỗi MediaSession hoặc SAF playback**.

Thay đổi `RealPickerSafTest.kt`: thêm `clickRealSaveAction`, ưu tiên UIAutomator nhận diện **`android:id/button1` có nhãn SAVE và enabled**, sau đó mới dùng đường hỗ trợ accessibility text/fallback có kiểm chứng nút Save. Giữ nguyên việc bắt buộc callback `CreateDocument` trả URI thật, persisted READ ban đầu chưa tồn tại và callback `OpenMultipleDocuments` phải cấp READ thật, MediaSession phải phát được WAV, Next/History hoạt động đúng. **Không sửa Kotlin production**.

Trong quá trình chạy lại có một lần **hai Gradle full instrumentation chồng lên cùng thư mục Android logcat**, dẫn đến lỗi Windows `FileSystemException` (file đang được tiến trình khác sử dụng). Đây là lỗi **điều phối chạy QA**, không phải testcase fail. Bổ sung khóa mutex và kiểm tra tiến trình Gradle `connectedDebugAndroidTest` đang chạy trong `scripts/run-full-regression-v2-part1.ps1` để ngăn chạy song song trong lần QA sau; khi Gradle chưa ghi XML, script có thông báo lỗi riêng thay vì báo số test cũ. Bản log lỗi chồng chéo được lưu riêng phục vụ truy vết, **không dùng nó làm bằng chứng duy nhất**.

## Kết quả và nguồn bằng chứng

- Runner tái hiện: `scripts/run-full-regression-v2-part1.ps1` với `param(-DeviceId emulator-5554)`, kiểm tra emulator boot/API30, tránh concurrent Gradle và parse JUnit XML.
- Log chính thức: `qa-logs/full-product-regression-v2-part1.log`. Kết quả PASS cuối: `JVM_TOTAL=105`, `JVM_FAIL=0`, `JVM_SKIP=0`; `ANDROID_TOTAL=115`, `ANDROID_FAIL=0`, `ANDROID_SKIP=3`, `ANDROID_UNEXPECTED_SKIP=0`, `GRADLE_ANDROID_EXIT=0`, `GIT_DIFF_CHECK_EXIT=0`, `FINAL_RESULT=PASS`.
- Bằng chứng XML gốc tại máy QA: `app/build/test-results/testDebugUnitTest/TEST-*.xml` và `app/build/outputs/androidTest-results/connected/debug/TEST-*.xml`. XML được tạo ở mốc chạy tương ứng; thư mục Gradle `build/` không đưa lên Git.
- Log chẩn đoán: `qa-logs/full-product-regression-v2-part1-initial-failure.log` (hai SAF FAIL ban đầu), `qa-logs/full-product-regression-v2-part1-saf-fix.log` (2/2 targeted), `qa-logs/full-product-regression-v2-part1-overlap-raw.log` (bằng chứng phiên Gradle chồng tiến trình khi điều tra).

**Ba testcase SKIP duy nhất**:

1. `SafPermissionRestartTest.cleanupSafDocument`
2. `SafPermissionRestartTest.prepareSafDocument`
3. `SafPermissionRestartTest.verifySafAfterForceStop`

Không tự thay đổi SKIP thành PASS. Các case này cần script kiểm thử có thao tác force-stop/khởi động lại và được ghi ở `docs/Android_Compatibility_Matrix_v2.md` của mốc trước.

## Giới hạn và bước kế tiếp

**PASS chỉ áp dụng cho toàn bộ suite trên emulator API 30 ở commit nền và thay đổi test trong workspace.** Chưa chạy toàn suite mới trên API 29/33/36 hoặc điện thoại vật lý; chưa đo chất lượng gapless/loopback thật, full accessibility/performance/thư viện lớn, release signing, cài mới/nâng cấp release, Google/email thực, cloud sync hai thiết bị E2E, hoặc licensed streaming bằng nhạc có quyền thật. Kết quả không có nghĩa mọi tính năng MusicFlow 1.0 đã được nghiệm thu.

**Tiếp theo:** **Release Candidate QA / Packaging — Part 1**: kiểm tra cấu hình build release/ký APK, kiểm thử cài mới/nâng cấp và smoke test trên artifact release, lấy SHA-256 của artifact và bảo đảm truy vết commit. Chưa phát hành public hoặc sử dụng khóa ký production khi chủ dự án chưa cấu hình; Google/email thật và cloud hai thiết bị giữ cuối như đã thống nhất.
