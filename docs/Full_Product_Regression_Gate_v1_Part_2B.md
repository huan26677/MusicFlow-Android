# MusicFlow — Full Product Regression Gate v1 / Part 2B (SAF OpenMultipleDocuments)

**Ngày:** 2026-10-29. **Git HEAD nền:** `0d47da9` (Part 2A đã commit). **Trạng thái cuối:** **BUILD/JVM PASS + 2/2 Android API 30 PASS**. Đã hoàn tất riêng Part 2B, **Full Product Regression Gate chưa chạy lại**. Phần 2B được chia nhỏ theo yêu cầu của chủ dự án.

## Phạm vi

Chỉ kiểm tra hai bài `RealPickerSafTest` dùng Android `OpenMultipleDocuments` thật, trên emulator `emulator-5554` API 30:

- `realOpenMultipleDocumentsGrantsPersistedReadAndPlays` — chọn 1 tệp WAV, xác nhận URI/READ grant, phát qua MediaSession.
- `realOpenMultipleDocumentsSelectsTwoFilesAndNavigates` — chọn 2 tệp WAV, xác nhận hàng đợi, READ grant, Next và Recent History.

Không sửa logic phát nhạc, Media3, dữ liệu người dùng, Supabase hoặc hệ thống bản quyền online. **Nhạc offline tự tải không cần xác minh bản quyền trong MusicFlow.**

## Tiến độ và phát hiện có bằng chứng

Khi bắt đầu, `RealPickerSafTest.kt` **đã có thay đổi chưa commit** từ lượt trước (tự tìm/chạm tệp trong DocumentsUI). Giữ nguyên các thay đổi đó, chỉ cải thiện phương pháp chạm và log, không reset file.

1. APK AndroidTest build thành công trên API 30.
2. Cả hai bài test đều tạo tệp WAV thông qua `ACTION_CREATE_DOCUMENT`, ghi âm mẫu và mở Android DocumentsUI thật. Bài 1 log `PASS sourcePreparedWithoutPersistedRead` và `PASS productionPickerLaunched`; bài 2 log `PASS multiSourcesPrepared`.
3. Cây Accessibility và ảnh chụp tạm cho thấy DocumentsUI hiển thị thẻ file ở chế độ Grid, title nằm ở thành phần `android:id/title`, ancestor là `com.google.android.documentsui:id/item_root`. Đã thử chạm vào nhãn, chạm giữa card, `ACTION_CLICK` từ ancestor, shell input tap/swipe, nhấn giữ vào chế độ chọn, và cố chuyển sang List view. Trình chọn vẫn **không trả URI** cho app trong lượt chạy được quan sát.
4. Test 1 chạy riêng **FAIL**: `Document was not selectable in real OpenMultipleDocuments UI`; log `AUTO_PICKER_TIMEOUT ... package=com.google.android.documentsui`. Test 2 chạy riêng **FAIL**: `Picker did not return`; có hai tệp nhìn thấy và hai lần dispatch hành động, nhưng chưa xác nhận Android selection hay kết quả thực.
5. Mã QA được cập nhật để log `AUTO_PICKER_TAP_SENT` thay vì gắn nhãn sai `SELECTED` khi mới gửi sự kiện. **Không bao giờ giả mạo ActivityResult hoặc URI để làm test pass**. Cleanup và các assert thật về quyền đọc/Media3 vẫn giữ nguyên.
6. `:app:assembleDebugAndroidTest` sau thay đổi cuối **BUILD SUCCESSFUL**. Không có bằng chứng cả hai test PASS, nên kết quả Part 2B là **FAIL/OPEN**, chưa chạy lại toàn bộ 89 Android tests trong lần này.

## Kết quả hoàn thành sau bước chẩn đoán (2026-10-29)

- Nguyên nhân chính đã được xác nhận bằng log Accessibility: sau khi tệp được **UIAutomator long-click chọn thật**, DocumentsUI đổi `selected=true` và hiển thị nút **`SELECT`** với ID `com.google.android.documentsui:id/action_menu_select`. Bộ test trước đó chỉ tìm `Open/Mở`, không tìm `SELECT`; các thao tác shell input cũ không xác minh được selection.
- Bổ sung dependency **androidx.test.uiautomator:uiautomator:2.3.0** chỉ ở `androidTestImplementation`, dùng `UiDevice`/By để bấm đúng card `item_root` và **bấm SELECT sau khi kiểm tra các title được chọn**. Không gọi giả kết quả `ActivityResult`, không cấp quyền đọc giả, không bỏ assert playback/URI/history.
- Bài một tệp: DocumentsUI trả `content://com.android.providers.downloads.documents/document/...`, `MediaSession` phát WAV thật, URI đọc đủ RIFF và persisted READ = **PASS**.
- Bài hai tệp: DocumentsUI trả hai URI thật; player có 2 MediaItem đúng thứ tự, cả hai có persisted READ, Next phát bài thứ hai và Recent History được ghi = **PASS**.
- Bằng chứng chính thức: `scripts/run-full-regression-v1-part2b-saf-tests.ps1` chạy `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` **BUILD SUCCESSFUL / JVM PASS**, rồi test nguyên class `RealPickerSafTest` trên emulator API 30 **2/2 PASS**, 9 marker và 2 lần DocumentsUI return, `git diff --check` PASS. Log `qa-logs/full-product-regression-v1-part2b-saf.log` có `FINAL_RESULT=PASS`.
- Trong quá trình kiểm tra, lượt chạy chung đầu tiên từng FAIL gián đoạn tại **CreateDocument thứ hai**; test đó chạy riêng PASS và các lượt chạy chung tiếp theo PASS, bao gồm script QA chính thức. Đây là một điểm cần theo dõi khi chạy full suite, không diễn giải là đã thử trên thiết bị thật.
- **Không đổi production code**: offline của người dùng không cần online rights và không cần Supabase.

## Việc tiếp theo sau Part 2B — Full Regression Gate rerun

- Chạy lại **`:app:connectedDebugAndroidTest` đầy đủ trên API 30**, kết hợp JVM, ghi pass/fail/skip theo JUnit XML. Baseline Part 1 trước sửa là 83 PASS / 3 FAIL / 3 SKIP.
- Theo dõi test CreateDocument có thể trễ khi chạy suite dài; không đánh đồng lượt kiểm thử riêng **2/2 PASS** với toàn bộ 89 bài Android.
- Giữ các kiểm thử SAF/process-restart cần nhiều pha ở harness riêng; không gán các bài SKIP mặc định thành PASS.

Ba SKIP do pha SAF/restart riêng trong full-suite baseline thuộc kiểm thử nhiều giai đoạn; bằng chứng đã có ở `docs/Android_Compatibility_Matrix_v2.md`. Google Login/email thật và cloud hai thiết bị E2E vẫn để gần cuối. Không tuyên bố MusicFlow 1.0 nghiệm thu.
