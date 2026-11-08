# MusicFlow — 6 hành trình nghiệm thu UX v10

**Phạm vi:** đồ án/demo, Android 10–16. **Mục tiêu:** người dùng không cần biết Android permission, Supabase, RLS hay SAF là gì để thực hiện công việc chính. Đây là kịch bản cho người kiểm thử, **không phải bằng chứng người thật đã thực hiện**.

| Nhóm | Bắt đầu | Tác vụ người dùng | Tiêu chí thành công | Tình trạng |
|---|---|---|---|---|
| **Khách** | Mở Trang chủ | Chọn nhạc → Chọn tệp WAV/MP3 bằng Android DocumentsUI → Phát bài → Tạm dừng | Không đăng nhập; SAF không cần xin quyền quét cả máy; chọn bài, nghe và quay lại được | Automated SAF/WAV trên AVD trước đó; **manual chưa chạy** |
| **Free** | Đăng nhập hoặc tạo tài khoản demo offline | Vào thư viện, tạo playlist, thêm bài từ nhạc đã chọn, phát playlist | Không ép Premium, không xóa file gốc, queue đúng thứ tự | Automated playlist/SAF regression; **manual chưa chạy** |
| **Premium** | Tài khoản Free/Premium fixture | Xem trạng thái gói và phân biệt `Premium thật` với thanh toán giả lập | Không nhầm giao dịch giả với thu tiền; Premium không tự có quyền Admin | Automated UI/role fixture; **production entitlement chưa thử** |
| **Creator** | Đăng nhập với quyền creator fixture | Xem tiến độ đơn đăng ký hoặc tạo hồ sơ/draft, gửi xét duyệt | Thấy trạng thái xử lý, biết cần làm gì tiếp theo, không gửi đơn trùng | Automated actor/UI fixture; **backend thực chưa thử** |
| **Moderator/Admin** | Vai trò kiểm duyệt/quản trị fixture | Chọn công việc, xem yêu cầu chờ, duyệt/từ chối có lý do và xác nhận | Chỉ thấy phạm vi server đã cấp, thông báo hoàn tất rõ ràng | Automated RPC fake/fail-closed; **không xác minh live RBAC** |
| **Owner** | Owner fixture | Mở Quản trị, tìm người dùng, xem quyền, chọn cấp/thu hồi, xác nhận bằng snapshot | Không cấp nhầm quyền; đổi vai trò/thoát khu vực không để lại quyết định chưa xác nhận | Automated Owner panel UI; **thao tác Owner thật chưa thử** |

## Checklist theo phiên test

1. Cài APK QA đang bàn giao, ghi Android API, chiều rộng màn hình, chế độ sáng/tối và font scale.
2. Thực hiện sáu hành trình bằng **tài khoản thử không có quyền thật**, riêng bài Owner live chỉ dùng môi trường kiểm thử có owner được phép và xác nhận rõ.
3. Kiểm tra màn 320 dp/200% chữ: các nút 48 dp, không che bởi bàn phím, không tràn màn và người dùng biết cuộn đến hành động tiếp theo.
4. Bật TalkBack thực: nghe đọc tên nút, **Đã mở/Đã thu gọn**, trạng thái lỗi đăng nhập, thứ tự focus, nút đóng dialog và thông báo sau thao tác.
5. Thử bàn phím vật lý/D-pad: focus thấy được, Enter mở/đóng danh sách, Back hủy hộp xác nhận, không tự chạy thao tác nguy hiểm.
6. Đánh giá sau từng hành trình: hoàn thành được không? Số chạm, thời gian, nút gây nhầm, ngôn ngữ khó hiểu và hành vi khi mất mạng/không có file.

## Phiếu kết quả

```text
Ngày / thiết bị / API / build hash:
Nhóm và nhiệm vụ:
Trạng thái: PASS / FAIL / BLOCKED / NOT RUN
Số thao tác và thời gian:
Điểm gây nhầm:
TalkBack / phóng chữ / D-pad:
Ảnh màn hình hoặc log QA (không lộ tài khoản/token):
Đề xuất sửa:
```

**Không báo tỷ lệ hoàn thành UX khi chỉ chạy Compose fixture/AVD.** Tệp này là biểu mẫu để thực hiện UAT sau khi có người kiểm thử thật. Các ghi nhận QA tự động và giới hạn theo API nằm ở `docs/Clear_Journeys_UX_v10.md`.
