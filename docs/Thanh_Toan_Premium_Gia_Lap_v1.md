# MusicFlow — Thanh toán Premium GIẢ LẬP v1 (APK báo cáo)

**Ngày nghiệm thu:** 2026-11-07 (Asia/Ho_Chi_Minh). **HEAD nền:** `55d9c6d` (Demo Acceptance v1); nhánh `main` sạch trước đợt phát triển. **Mục đích:** cung cấp quy trình chọn gói, thanh toán, gia hạn, lỗi, hủy và biên nhận **để minh họa cho báo cáo** mà không kết nối bất kỳ hệ thống thu tiền nào.

## 1. Hành trình demo cho mọi actor

Vào **Tài khoản → Trải nghiệm thanh toán Premium (giả lập)**, kể cả khi đang ở Guest. Giao diện có nhãn rõ **MÔ PHỎNG**, cảnh báo không thu tiền và không mở quyền thật. Các bước:

1. **Chọn gói:** 30 ngày (**29.000đ**) hoặc 365 ngày (**299.000đ**). Mọi mức giá đều chỉ là **ví dụ**; không phải bảng giá thương mại của MusicFlow.
2. **Chọn phương thức:** Ví điện tử giả lập hoặc Thẻ ngân hàng giả lập. **Không có form số thẻ/CVV/số tài khoản/mật khẩu ngân hàng**, không yêu cầu tài khoản thật.
3. **Chọn kết quả thử nghiệm:** Thành công hoặc Thất bại. Bấm **Mô phỏng thanh toán / Mô phỏng gia hạn** rồi xác nhận ở hộp thoại; **Không thực hiện** không tạo giao dịch.
4. **Thành công:** tạo biên nhận `MF-DEMO-xxxxxxxx`, lưu thời điểm/gói/phương thức/ngày hết hạn trong SharedPreferences trên máy. Mở lại ứng dụng vẫn thấy đúng biên nhận. Mua thêm khi còn hạn sẽ **cộng ngày vào thời hạn hiện tại**; gói hết hạn bắt đầu lại từ hiện tại.
5. **Thất bại:** chỉ báo thử nghiệm từ chối, không sửa biên nhận hay ngày hết hạn của gói demo đang có.
6. **Hủy gói:** cần hộp thoại xác nhận trước khi dừng hiệu lực ngay của **gói mô phỏng**. Khi đã hủy, có thể xóa riêng biên nhận demo hoặc mô phỏng mua mới.

**Giới hạn quan trọng:** Đây là **trạng thái hiển thị riêng**, KHÔNG thay `AccountIdentity.tier`, không ghi bảng `account_entitlements`, không cho Premium server, không thanh toán thật, không mở cloud quota hay sync tự động, không mở khóa audio streaming, không tự cấp `owner`/`platform_admin`/`moderator`/`developer`/`artist`/`author`. Tên gói demo và biên nhận không phải bằng chứng thanh toán hay quyền truy cập thực tế.

## 2. Thiết kế và tách biệt quyền

- `core/account/DemoBillingStore.kt`: hai gói và phương thức giả lập, `DemoBillingRules` pure Kotlin, biên nhận, thời hạn, gia hạn, hủy, thất bại và reset. `DemoBillingStore` chỉ dùng `SharedPreferences` tên `musicflow_demo_billing_v1_not_real`.
- **Cách ly theo phiên tài khoản:** dùng `userId` hiện có làm phạm vi hoặc `guest-offline-demo` khi chưa đăng nhập; scope băm SHA-256 thành khóa cục bộ (không gửi mạng). Mua Guest không đưa gói cho tài khoản Supabase, và gói của hai account khác nhau không trộn nhau. Có thể giả lập Premium khi chưa đăng nhập nhưng **không biến Guest thành account**.
- Không lưu số thẻ, thông tin ngân hàng, thông tin liên hệ hay mật khẩu trong biên nhận. Mã hóa/chuỗi chữ ký thanh toán thật **không thuộc bản demo**. Thao tác ghi sử dụng `commit()` để báo lỗi thay vì hiển thị giao dịch thành công khi chưa lưu được.
- `feature/account/DemoBillingScreen.kt`: màn hình mô phỏng có chọn gói, phương thức, kết quả, confirm, biên nhận, hủy/reset và chỉ dẫn. Vùng bấm ít nhất 48dp, có test tags cho thao tác tự động.
- `feature/account/AccountScreen.kt`: bổ sung lối vào ở đầu Trang Tài khoản; modal chạy trong `AccountRoute` và chỉ thay state mock local. Các công cụ Owner/Moderator, cloud sync và quyền Premium thật giữ logic cũ.
- Không có SDK thanh toán, API gateway, webhook, hóa đơn VAT thật, subscription billing thực hoặc Supabase mutation trong bất kỳ màn hình mô phỏng nào.

## 3. Bằng chứng kiểm thử (một đợt tổng hợp sau triển khai)

| Hạng mục | Kết quả |
|---|---|
| Toàn bộ JVM unit tests | **186/186 PASS**, 32 suites, 0 failures/errors; gồm 6 `DemoBillingRulesTest` mới |
| Android UI + account/role/creator/playlist/playback regressions | **61/61 PASS** (6 `DemoBillingUiTest` mới) trên isolated API30 emulator |
| Android DocumentsUI SAF file thật | **1/1 PASS** |
| Web Admin fail-closed roles | **4/4 PASS** |
| Debug / AndroidTest / Release build | **PASS** |
| APK ZIP alignment, signature QA, manifest và SHA256 | **PASS** |
| Cài nâng cấp và cold launch đúng bản APK Release QA trên emulator API30 | **PASS**, `Status: ok`, `LaunchState: COLD`, app process sống |
| Thao tác **mua thử trực tiếp trên APK đã ký**, Guest, gói 30 ngày/ví demo | **PASS** — biên nhận `MF-DEMO-…` hiển thị |
| **Force-stop / cold launch lại**, vào Tài khoản/mở thanh toán mô phỏng | **PASS** — biên nhận mock vẫn còn và gói demo đang có hiệu lực |

**Chi tiết test mới:**

- 6 JVM: giá gói và thời hạn, mua năm, gia hạn còn hạn, mua sau hết hạn, mua sau hủy và xác nhận tier backend Free không đổi.
- 6 Android: Guest thấy lối vào không cần login; chọn gói năm/thẻ rồi xác nhận mới lưu; giao dịch mô phỏng thất bại giữ nguyên biên nhận cũ; hủy qua hộp thoại và reset; thoát ở bước confirm không ghi nhận; `SharedPreferences` lưu lại và cách ly scope của tài khoản.
- Cổng E2E offline cũ vẫn chạy trong nhóm 61; test SAF thật chạy riêng tiếp theo. Không chạy các giao dịch với cổng thanh toán thật.

**Lỗi đã xử lý:** lần build đầu không biên dịch vì import `rememberSaveable` sai package trong màn mới. Đã sửa sang `androidx.compose.runtime.saveable.rememberSaveable` và chạy lại toàn bộ QA, cuối cùng **PASS**. `git diff --check` không có whitespace error (có thể có cảnh báo chuyển LF/CRLF của Windows).

**Bằng chứng:** `qa-logs/musicflow-simulated-billing-v1-qa.log`, `qa-logs/demo-apk-simulated-billing-v1-package.log`, `qa-logs/musicflow-simulated-billing-release-smoke.log`; screenshot `qa-logs/screenshots/musicflow-simulated-billing-api30.png` (màn chọn gói) và `qa-logs/screenshots/musicflow-simulated-billing-receipt-api30.png` (biên nhận Guest). Scripts `scripts/run-simulated-billing-v1-qa.ps1`, `scripts/build-musicflow-demo-apk-v1.ps1`.

## 4. APK demo mới

- **Đường dẫn trên máy phát triển:** `E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-SimulatedBillingV1-Android.apk`.
- **Dung lượng:** 31.278.781 bytes (~31,3 MB).
- **SHA256:** `302E6415A56B1A0801BF92AA65C2A92A89D2446A7D31D45C22FAE02A9A3484C7`.
- `package=com.example.musicflow`, `versionName=1.0`, `versionCode=1`, minSdk29/targetSdk36, `debuggable=false`.
- APK release-mode **ký debug QA key**, để sideload dùng trong lớp học; chưa phải production/Google Play hoặc app thu phí thật.
- QA AVD riêng `MusicFlow_SimulatedBillingV1_QA_API30` (`emulator-5580`) dùng để kiểm thử; **chỉ dọn AVD mới này**, giữ nguyên emulator đang chạy của người dùng và các AVD cũ.

## 5. Việc KHÔNG thay thế bằng mô phỏng

`DemoBillingStore` **không phải** cổng thanh toán và không chứng minh E2E các dịch vụ online. Nếu muốn sau này phát hành thật, cần lập kế hoạch độc lập cho Payment SDK/store billing, bảo vệ hóa đơn, backend reconciliation, chống giả mạo, subscription renewal, refund, thuế và luật thanh toán. Hiện tại các tính năng cloud/Premium do backend xác nhận vẫn hoạt động theo quyền được Supabase cấp; mock local không thay thế kiểm tra server. Đây là giới hạn rõ ràng để demo ổn định và bảo vệ báo cáo trung thực.
