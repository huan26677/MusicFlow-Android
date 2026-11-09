# MusicFlow — Nghiệm thu APK Community v14: cài đè và luồng người nghe

**Ngày:** 2026-11-09. **Mục đích:** kiểm tra APK v13 → v14 trên **hai máy ảo QA tạo mới**, không ảnh hưởng điện thoại và tài liệu của người dùng. APK chỉ ký bằng Android Debug key dành cho đồ án, **không phải bản Play Store**.

## Artifact thực đã dùng

| Bản | Đường dẫn local (Git-ignore) | SHA-256 |
|---|---|---|
| v13 (baseline) | `dist/MusicFlow-1.0-Demo-FinalUXV13-Android.apk` | `D74CD7F4FEEE8819FBE6528554AC8EBC60885EBE7182FBFC7EBC77F1762F9D3F` |
| v14 (target) | `dist/MusicFlow-1.0-Demo-CommunityV14-Android.apk` | `3C2FFD95231A1AECBE9A76E21EE34A4BA56FF320CD18F14FA6B3E6904B6DD0BD` |

Chạy `apksigner verify --print-certs`, checksum từng APK và so sánh SHA-256 certificate. **Cùng chữ ký**, không đổi package, cài đè bằng `adb install -r` (không `pm clear`/uninstall). Mỗi AVD lúc khởi chạy đều chưa cài MusicFlow.

## Kịch bản và kết quả

Script kiểm tra đủ 2 pha: `scripts/run-signed-upgrade-v14-qa.ps1 -Serial emulator-5554`. Script tự kiểm tra AVD phải là `MusicFlow_CommunityV14_QA_API33` hoặc `MusicFlow_CommunityV14_QA_API36`, đúng API và boot; không chạy khi đã có MusicFlow trước pha seed.

Pha **seed** chạy `SignedApkUpgradeV6Test#seedSignedV5PlaylistFavoriteAndRealSafGrant` trên v13 (tên method là lịch sử): tạo playlist Room + bài hát, favorite và tệp WAV nội bộ bằng **DocumentsUI CreateDocument thật**, lưu quyền SAF READ. Pha **verify** chỉ chạy **sau khi cài đè APK v14**, gọi `SignedApkUpgradeV6Test#verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly`: đối chiếu Room playlist/track, favorites, local picked catalog, SAF persisted READ, khả năng đọc `RIFF`; dọn đúng playlist/file/URI được tạo bởi test.

| Kiểm tra | API33 — Android 13 | API36 — Android 16 |
|---|---|---|
| Hash và cùng chữ ký APK v13/v14 | **PASS** | **PASS** |
| Seed fixture Room, favorites, SAF real DocumentsUI | **PASS** | **PASS** |
| Cài đè v14 không xóa app data, verify Room/favorites/SAF/WAV | **PASS** | **PASS** |
| Test cleanup fixture chỉ thuộc QA | **PASS** | **PASS** |
| `SignedCommunityNavigationUiTest#guestReachesCommunityDemoFromRealHomeNavigation` trên **APK v14 ký QA** | **1/1 PASS** | **1/1 PASS** |

**Đã chạy các test được nêu trên, không phải toàn bộ catalog.** UI smoke trên APK ký dùng `createAndroidComposeRule<MainActivity>()`: Trang chủ → Khám phá nhạc online → Xem thử đánh giá/bình luận (Demo), xác nhận nhãn Demo và tổng hợp điểm sao. Có thể xác minh ngay cả không đăng nhập.

### Vấn đề test harness và cách khắc phục

Lần thử thêm **6 test `createComposeRule()` trên APK Release ký** ban đầu bị **6/6 FAIL** ngay ở setup vì `androidx.activity.ComponentActivity` chỉ được đăng ký cho phiên bản debug/test, không nằm trong manifest của Release APK. Log và lỗi được giữ lại: `qa-logs/musicflow-v14-signed-compose6-limitation.log`. **Không được tính lượt này là PASS, cũng không suy diễn rằng logic Community bị lỗi.** Thay bằng test entrypoint `MainActivity` có thực trên Release; **1/1 PASS API33 và 1/1 PASS API36**. Bộ 6 UI Compose chạy trên bản Debug trước đó **6/6 PASS API36**, là phạm vi kiểm thử khác.

### Bằng chứng Git đã lọc

- `qa-logs/musicflow-v14-signed-upgrade-api33.log`, `qa-logs/musicflow-v14-signed-upgrade-api36.log`: mỗi log có đủ 2 pha PASS và marker cleanup.
- `qa-logs/musicflow-v14-signed-navigation-api33.log`, `qa-logs/musicflow-v14-signed-navigation-api36.log`: mỗi log `OK (1 test)` từ test MainActivity thật.
- `qa-logs/musicflow-v14-signed-compose6-limitation.log`: thất bại test harness Release được ghi nhận.
- `scripts/test_signed_upgrade_v14.py`: 2/2 test hợp đồng bảo vệ script và trình tự seed/verify PASS.

## Impact-driven QA và giới hạn

**Direct:** `RELEASE_BACKUP` (script ký/cài đè), `COMMUNITY_COMMENTS` (test đường vào). **Downstream:** `MEDIA_SAF`, `PLAYLIST`, `FAVORITES`, `HOME_NAV`, `ONLINE_RIGHTS` theo route. Script cài đặt và thao tác với Room/SAF là đường rủi ro cao → quy định `full_required`, **480 phương thức @Test được đăng ký nhưng chưa full run**. JVM 210/210 và các UI module cũ còn các log riêng; không thể lấy các kết quả đó cộng thành 480/480.

**Chưa nghiệm thu:** bảo toàn dữ liệu trên thiết bị vật lý/khi dùng khóa ký khác, nhiều nhà cung cấp SAF, TalkBack người thật, full Android API33/API36, và Community RPC chấm/bình luận/báo cáo/duyệt **qua JWT/HTTP từ các tài khoản thật**. Không dùng danh mục online giả có license công khai hay thay đổi quyền streaming.
