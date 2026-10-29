# MusicFlow — Lyrics Runtime Acceptance v1

**Ngày thực hiện:** 2026-10-29. **HEAD nền:** `510de79` (Equalizer Runtime v1 đã được commit). **Kết quả:** Build/JVM **105/105 PASS**; Android API 30 **Lyrics 2/2 + Playback/Queue 8/8 = 10/10 PASS**. Đây là QA riêng Lyrics, không phải nghiệm thu toàn bộ MusicFlow 1.0.

## Phạm vi

File `.lrc` được liên kết với **URI của bài nhạc offline**. Có thể nhập từ hệ thống Android SAF; ứng dụng lưu URI nguồn lời và persisted READ grant để lần sau mở lại lời. Chỉ **kho nhạc online do MusicFlow phân phối** mới phải được xác minh quyền streaming; nhạc offline người dùng tự lưu/tải về vẫn nghe như bình thường, không cần tài khoản hay Internet.

## Thay đổi mã nguồn

- `core/lyrics/LocalLyricsRepository.kt`: thêm `LyricsTimeline.activeIndex` (-1 trước mốc đầu tiên; thay đổi theo thời gian và khi seek tới mốc trước đó); thay đổi trình tự nhập LRC thành **đọc/kiểm tra nội dung trước khi yêu cầu persisted URI grant**. Tệp thiếu timestamp sẽ không tạo liên kết audio, tránh giữ thêm một quyền SAF không cần thiết.
- `feature/player/NowPlayingScreen.kt`: dùng cùng `LyricsTimeline.activeIndex` để chọn dòng đang được làm nổi bật. Dòng hiện tại có semantics test tag `lyrics-active-line`, dòng khác là `lyrics-inactive-line` để kiểm thử có thể khẳng định chính xác. Không thay đổi logic Playback, màu sắc hay cách hiển thị lời cho người dùng.

## QA tự động đã thực hiện

- **6 JVM parser tests** trong `LrcParserTest`: BOM/metadata, timestamp phần trăm/phần nghìn giây, nhiều timestamp cho cùng dòng và sort, offset âm + clamp, bỏ dòng không hợp lệ/thiếu lời, cắt nội dung quá dài, và timeline trước/sau mốc + tua ngược.
- **1 Android SAF test** `LyricsSafImportTest`: mở `ACTION_CREATE_DOCUMENT` thật trên DocumentsUI, ghi tệp LRC UTF-8, nhập qua `LocalLyricsRepository.import`, kiểm tra URI `content://` thật, **persisted READ** và mở lại liên kết bằng repository mới. Sau đó ghi đè cùng tài liệu bằng `rwt` để bảo đảm **truncate** thật trước khi thử nội dung LRC không hợp lệ; từ chối, không ghi liên kết nhạc sai. Cuối test xóa document và thu hồi quyền đã cấp, xóa preference QA.
- **1 Android UI + MediaSession test** `LyricsScreenSyncTest`: phát WAV tạo riêng trong cache qua `PlaybackService`/MediaController thật, pause và seek 1.2s, 3.3s, 6.2s rồi quay lại 1.8s; Compose `NowPlayingScreen` nhận vị trí từ real MediaSession, đúng dòng lời được đánh dấu `lyrics-active-line` ở mỗi điểm. Kiểm thử giữ/khôi phục queue, playback state và xóa WAV test.
- **8 Android playback regression**: `PlaybackServiceIntegrationTest` 3/3, `PlaybackQueuePersistenceTest` 5/5, phiên instrumentation riêng.

Script `scripts/run-lyrics-runtime-v1-tests.ps1` chạy Build+JVM, đọc kết quả JUnit XML của parser, cài APK, chạy instrumentation, đòi **3 marker Lyrics** (`realSafImportAndPersistedRead`, `invalidLrcNotLinked`, `realPlaybackSeekLyricsHighlight`) và hồi quy Playback, kiểm tra `git diff --check`. Log lưu tại `qa-logs/lyrics-runtime-v1.log`, `FINAL_RESULT=PASS`.

### Lỗi test được phát hiện và khắc phục

- Bản thử đầu nhập LRC hợp lệ và đồng bộ Player đã thành công nhưng kiểm thử tệp LRC không hợp lệ bị FAIL giả: `ContentResolver.openOutputStream(uri, "w")` trên DocumentsProvider **không đảm bảo xóa dữ liệu cũ**. Chuyển sang `"rwt"`, đọc lại tệp để chứng minh chỉ còn dữ liệu mới, sau đó **2/2 Android PASS**.
- Trước khi chạy lại Android, bài Compose test cần sửa chữ ký callback `onSetEqualizerBand` thành lambda hai tham số; đó là lỗi biên dịch test, không phải lỗi ứng dụng.

## Giới hạn

- Chưa tự động nhấn `Nhập file .lrc` từ UI `MainActivity` rồi chọn lại LRC qua `ACTION_OPEN_DOCUMENT` trong **một bài E2E hoàn chỉnh**. Bài SAF hiện dùng CreateDocument thật + production repository; bài UI kiểm tra highlight nhưng cấp state lyrics trực tiếp từ LrcParser.
- Chưa kiểm thử đa API 29/33/36, thiết bị thật, TalkBack lyrics, file LRC quá lớn từ các DocumentsProvider khác nhau hay MusicFlow bị kill/reboot lúc lyric đang hiển thị.
- Chưa chạy lại toàn bộ `connectedDebugAndroidTest` sau mốc mới; chỉ nhóm targeted Android 10 test và JVM 105.
- Không thay đổi Supabase, Google Login, nội dung online, bản quyền streaming hay sync cloud.

**Bước nhỏ tiếp theo:** **Lyrics Import UI End-to-End v2** — kiểm thử toàn bộ đường `MainActivity`/Now Playing nút `Nhập file .lrc` → Android DocumentsUI `OpenDocument` → callback → màn Player tự nạp lời, trước khi chuyển sang các tính năng build-only/release QA khác. Google/email thật và cloud hai thiết bị vẫn ở cuối.
