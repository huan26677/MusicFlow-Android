# MusicFlow — Online Catalog API v1

## Phạm vi

Đây là **hợp đồng API cho danh mục metadata**, không phải dịch vụ streaming. Ứng dụng không tích hợp nguồn bài hát bên thứ ba mặc định và không tự nhận quyền truy cập nhạc có bản quyền.

Chỉ nhập địa chỉ máy chủ HTTPS của chính bạn hoặc một nhà cung cấp mà bạn có quyền tích hợp. Không đưa API token/mật khẩu vào URL; v1 chưa hỗ trợ xác thực.

## Cấu hình và điểm truy cập

- Trong MusicFlow: **Home → Khám phá nhạc online**.
- Nhập URL gốc, chẳng hạn `https://music.example.com` (chỉ là ví dụ, không phải máy chủ đang hoạt động).
- Client gọi: `GET {baseUrl}/v1/catalog/tracks`.
- Khi phân trang: `GET {baseUrl}/v1/catalog/tracks?cursor={urlEncodedCursor}`.
- Phản hồi HTTP 2xx dạng JSON, mã hóa UTF-8.
- Không tự động theo HTTP redirect; chuyển hướng phải được xử lý bằng cấu hình máy chủ.

## JSON mẫu

```json
{
  "items": [
    {
      "id": "track-001",
      "title": "Example Track",
      "artist": "Example Artist",
      "album": "Example Album",
      "durationMs": 183000
    }
  ],
  "nextCursor": null
}
```

Đây chỉ là dữ liệu minh họa cho lập trình API, **không phải nhạc đã được MusicFlow cấp phép hoặc có thể phát**.

## Trường dữ liệu

| Trường | Bắt buộc | Ý nghĩa |
|---|---|---|
| `items` | Có | Mảng tối đa 100 bản ghi mỗi trang |
| `items[].id` | Có | ID ổn định trong danh mục |
| `items[].title` | Có | Tiêu đề bài hát |
| `items[].artist` | Không | Tên nghệ sĩ |
| `items[].album` | Không | Album, có thể là `null` |
| `items[].durationMs` | Không | Thời lượng tính bằng mili-giây |
| `nextCursor` | Không | Chuỗi cho trang kế tiếp; `null` khi hết |

## Giới hạn hiện tại

- Chỉ hỗ trợ HTTPS, thời gian chờ kết nối/đọc 10 giây, phản hồi tối đa 1 MiB.
- Không gửi cookie, token hoặc thông tin đăng nhập.
- URL gốc không nhận userinfo/query/fragment; mã phân trang tối đa 256 ký tự.
- Giao diện chỉ hiển thị metadata; không có nút Play/Download giả.
- Các thao tác cần phân quyền, cấp phép streaming và tải offline phải được triển khai ở giai đoạn riêng.
- Chưa có kiểm thử API thật trên thiết bị, QA đa mạng hoặc kiểm thử tải.

## Mốc tiếp theo đề xuất

**Licensed Streaming Integration v1**: chọn nhà cung cấp/endpoint được cấp quyền, xác nhận chính sách sử dụng, định nghĩa quyền stream và thời hạn URL, sau đó mới nối Media3 vào luồng phát HTTPS và xử lý lỗi streaming.
