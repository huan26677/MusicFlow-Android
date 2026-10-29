# MusicFlow — Cloud Sync Hardening v2

## Thực hiện

- Hạn mức trên Supabase: Free 20 playlist + 500 favorites, Premium 200 playlist + 2.500 favorites; kiểm tra quyền Premium qua bảng `account_entitlements` và ngày hết hạn. Trigger có advisory lock theo owner để giảm tranh chấp đồng thời.
- RPC `musicflow_merge_cloud_playlist` gộp nội dung nguyên tử, chặn khôi phục playlist đã đánh dấu xóa.
- Soft delete cloud qua RPC `musicflow_trash_cloud_playlist` và `musicflow_trash_cloud_favorite` với RLS. Xóa cloud qua tab Account có bước xác nhận; bản local vẫn giữ nguyên. Bản ghi đã xóa được giữ làm tombstone để tránh máy khác tự đưa trở lại.
- Tab Account: hiển thị bài cloud chưa khớp và cho chọn bài ở thiết bị hiện tại để liên kết; chỉ lưu ánh xạ URI ở thiết bị, không tải file nhạc lên Supabase.
- Hỗ trợ đọc cloud có phân trang; refresh JWT khi hết hạn.

## Giới hạn, cần kiểm thử thực tế

- Chưa thử E2E với 2 điện thoại, tài khoản email thật hay data lớn.
- Chưa có cloud đồng bộ xóa tự động theo hành động xóa local. Xóa cloud được thao tác rõ ràng trong Account và giữ bản local.
- Giao diện lựa chọn tối đa 150 bài quét ở lần sync và hiển thị 30 mục đầu; cần tìm kiếm/pagination UI ở bản sau.
- Một phiên đồng bộ có thể thất bại giữa chừng; thao tác lặp lại hướng tới idempotent nhưng cần kiểm tra lỗi mạng thực tế.
- Người dùng cần đảm bảo quyền truy cập MediaStore để liên kết bài; dữ liệu chưa có tại thiết bị khác sẽ không thể phát cho tới khi có file tương ứng.
- Không mở giới hạn premium trong Android theo cờ client; chỉ backend có quyền quyết định.
