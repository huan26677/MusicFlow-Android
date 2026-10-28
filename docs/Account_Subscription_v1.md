# MusicFlow — Account & Subscription Foundation v1

Ngày triển khai: 2026-10-29. Supabase project: `vzvxpbwrftlnyvuhfoqn`.

## Quyết định sản phẩm

| Cấp độ | Trạng thái v1 | Quyền lợi đã triển khai |
|---|---|---|
| Khách | Không có tài khoản Supabase | Nghe nhạc offline, thư viện, playlist, lịch sử local, lyrics, equalizer |
| Free | Đăng ký/đăng nhập email + mật khẩu | Tất cả quyền Khách + hồ sơ Supabase, phiên đăng nhập |
| Premium | Quyền lợi bảo vệ bằng bảng RLS và ngày hết hạn | Nhận diện trạng thái gói từ server; **chưa bán gói hoặc mở khóa quyền thương mại** |

Quyền lợi dự kiến đã thống nhất cho giai đoạn sau: Free có đồng bộ cơ bản (đề xuất tối đa 20 playlist và 2 thiết bị); Premium có đồng bộ mở rộng (đề xuất 5 thiết bị), lịch sử lâu dài, các lợi ích streaming được cấp quyền, tải offline khi có giấy phép, và loại bỏ quảng cáo nếu sản phẩm bổ sung quảng cáo. **Các giới hạn và quyền thương mại chưa được thực thi trong v1.** Không được hiển thị thành lời cam kết hiện đã có.

## Supabase schema & bảo mật

Migration: `musicflow_account_profiles_entitlements_v1`.

- `public.account_profiles`: `id` tham chiếu `auth.users.id`, `display_name`, thời gian tạo/cập nhật. RLS yêu cầu `auth.uid() = id` cho SELECT/INSERT/UPDATE; `authenticated` chỉ được UPDATE cột `display_name`, không được đổi `id`.
- `public.account_entitlements`: `user_id`, `tier` (`free` hoặc `premium`), `expires_at`, `source` (admin / play_billing / promotion), `updated_at`. `authenticated` chỉ SELECT dòng của mình; **không INSERT/UPDATE/DELETE**. Chỉ backend tin cậy được cấp quyền thay đổi subscription.
- Không có entitlement thì mặc định Free. Premium chỉ hiển thị khi `tier=premium` và `expires_at` ở tương lai. Khi hết hạn, về Free. Không lấy quyền từ `user_metadata` hay tùy chọn trên thiết bị.
- **RLS phải áp dụng riêng trên từng API Premium tương lai**. Việc giao diện hiển thị Premium không tự bảo vệ endpoints, tải nhạc riêng tư hay số lượt phát.
- Publishable key được sử dụng trong client. Service role / secret key không nằm trong ứng dụng.

## Android

- `AccountApi.kt`: Supabase Auth email/password qua HTTPS; đăng ký (`/auth/v1/signup`), đăng nhập (`/auth/v1/token?grant_type=password`), làm mới token, logout, đọc/sửa profile qua Data API.
- `AccountTokenStore`: chỉ lưu **refresh token mã hóa bằng AES-GCM trong Android Keystore**. Không lưu mật khẩu; access token trong bộ nhớ tiến trình.
- `AccountViewModel.kt` và `AccountScreen.kt`: tab Account hiển thị Khách/Free/Premium, đăng nhập, đăng ký, chỉnh tên, logout; giữ tùy chọn giao diện hiện có.
- Khách vẫn sử dụng local library/playlist/history khi không có mạng.
- Xác nhận email: dự án Supabase hosted có thể yêu cầu xác nhận qua email. Khi signup không nhận session, ứng dụng yêu cầu xác nhận email rồi đăng nhập. **Cần cấu hình SMTP/URL và kiểm tra email thực tế trước phát hành.**
- Chưa tích hợp Google OAuth, quên mật khẩu, Google Play Billing, đồng bộ đa thiết bị hoặc Premium-only streaming. Các mục này là milestone tiếp theo, không coi là PASS.

## Bằng chứng và giới hạn

- Kiểm tra Supabase: user không được sửa `account_entitlements`, không sửa `account_profiles.id`, chỉ sửa tên hiển thị; khách không đọc hồ sơ người khác. Security Advisors: không có vấn đề mới ở bảng tài khoản. Thông báo INFO `rls_enabled_no_policy` cho `music_uploads` là chủ ý khóa bảng admin.
- JVM unit tests `AccountTierTest.kt`: Free mặc định, Premium còn hạn, hết hạn, không có hạn và dữ liệu không hợp lệ.
- Build-only: `:app:assembleDebug`, `:app:assembleDebugAndroidTest`; JVM targeted unit `:app:testDebugUnitTest`; log `qa-logs/account-subscription-foundation-v1.log`.
- Chưa đăng ký tài khoản email thật, kiểm tra inbox xác nhận, refresh token sau nhiều ngày, xử lý network race, logout từ nhiều thiết bị hay test Google Play Billing. Full QA để cuối giai đoạn.

## Bước tiếp theo

**Cloud Playlist & Favorites Sync v1**, dựa trên Supabase Auth với RLS theo user và đồng bộ opt-in không làm mất dữ liệu local. Chỉ mở thanh toán Premium khi có kế hoạch Google Play Billing, xác thực server-side và giấy phép nội dung.
