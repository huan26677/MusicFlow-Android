package com.example.musicflow.core.account

/** Self-contained presentation scenarios. Never map these personas to real JWT roles. */
enum class DemoPersona(val title: String, val summary: String) {
    GUEST("Khách", "Nghe nhạc offline mà không cần tài khoản"),
    FREE("Free", "Tài khoản thường và bản sao thư viện mô phỏng"),
    PREMIUM("Premium", "Quyền lợi và giới hạn gói mô phỏng"),
    ARTIST("Nghệ sĩ", "Tạo hồ sơ, gửi bản nháp và chờ duyệt"),
    AUTHOR("Tác giả", "Ghi nhận tín chỉ và gửi yêu cầu xác minh"),
    MODERATOR("Kiểm duyệt viên", "Xét duyệt metadata theo đúng phạm vi"),
    PLATFORM_ADMIN("Quản trị viên", "Duyệt yêu cầu và quản lý người dùng được ủy quyền"),
    OWNER("Owner", "Giao quyền cộng tác và kiểm tra nhật ký"),
    DEVELOPER("Developer", "Chẩn đoán và kiểm thử nội bộ")
}

data class DemoCenterStep(
    val title: String,
    val explanation: String,
    val action: String,
    val outcome: String
)

object DemoCenterPlan {
    fun steps(actor: DemoPersona): List<DemoCenterStep> = when (actor) {
        DemoPersona.GUEST -> listOf(
            DemoCenterStep("Mở thư viện cá nhân", "Giả lập quét tệp nhạc trong bộ nhớ; không truy cập tệp thật.",
                "Quét thử thư viện", "Đã tìm thấy 12 bài hát minh họa (không tạo tệp thực)."),
            DemoCenterStep("Nghe nhạc offline", "Danh sách phát mẫu có 3 bài, chỉ mô tả thao tác phát.",
                "Phát bản demo", "Đã mô phỏng Play/Pause và hàng đợi 3 bài. Player thật vẫn độc lập.")
        )
        DemoPersona.FREE -> listOf(
            DemoCenterStep("Đồng bộ thử", "Sao lưu 3 playlist và 8 mục yêu thích minh họa lên 'đám mây' giả.",
                "Đồng bộ demo", "Mô phỏng hoàn tất: 3 playlist và 8 yêu thích. Không có upload Supabase."),
            DemoCenterStep("Khôi phục thử", "Đọc bản sao giả, không thay đổi playlist đang có.",
                "Khôi phục demo", "Đã mô phỏng khôi phục 3 playlist. Dữ liệu thật được giữ nguyên.")
        )
        DemoPersona.PREMIUM -> listOf(
            DemoCenterStep("Xem quyền lợi", "Minh họa quota: Free 50 mục, Premium 500 mục (chưa phải giới hạn backend).",
                "Xem quota giả lập", "Đang dùng thử 48/500 mục trong kịch bản Premium."),
            DemoCenterStep("Đồng bộ tự động", "Mô phỏng quá trình sao lưu mà không bật cloud sync thật.",
                "Chạy đồng bộ demo", "Đồng bộ mẫu thành công, không phát sinh kết nối mạng.")
        )
        DemoPersona.ARTIST -> listOf(
            DemoCenterStep("Tạo hồ sơ nghệ sĩ", "Tạo hồ sơ 'Nghệ sĩ mẫu' và điền thông tin mô phỏng.",
                "Tạo hồ sơ mẫu", "Hồ sơ mẫu đã được tạo trong kịch bản, không có tài khoản mới."),
            DemoCenterStep("Gửi bản nháp", "Tác phẩm 'Bài hát demo', trạng thái chờ xem xét.",
                "Nộp nháp demo", "Nháp mẫu đã chuyển sang 'Chờ duyệt' trong bản trình diễn."),
            DemoCenterStep("Xem tiến độ", "Minh họa người kiểm duyệt yêu cầu bổ sung chứng cứ.",
                "Xem kết quả mẫu", "Nhận ghi chú mẫu: cần cung cấp quyền phát online. Không có quyết định thật.")
        )
        DemoPersona.AUTHOR -> listOf(
            DemoCenterStep("Khai báo tín chỉ", "Khai báo tác giả âm nhạc cho một bài mẫu.",
                "Ghi tín chỉ demo", "Đã có tín chỉ 'Sáng tác' trên bản nháp mô phỏng."),
            DemoCenterStep("Đăng ký xác minh", "Đơn tác giả ở trạng thái chờ duyệt, không gửi API.",
                "Gửi đơn demo", "Đơn mẫu đang chờ xem xét, không thay đổi vai trò thật.")
        )
        DemoPersona.MODERATOR -> listOf(
            DemoCenterStep("Xem hồ sơ chờ duyệt", "Nháp mẫu có tác giả và chứng cứ chưa hoàn chỉnh.",
                "Xem hồ sơ mẫu", "Đã mở hồ sơ mẫu, không truy cập tài liệu riêng tư thật."),
            DemoCenterStep("Yêu cầu bổ sung", "Lý do mẫu: thiếu chứng cứ về quyền phát online.",
                "Gửi phản hồi demo", "Đã mô phỏng yêu cầu chỉnh sửa metadata; không có RPC kiểm duyệt.")
        )
        DemoPersona.PLATFORM_ADMIN -> listOf(
            DemoCenterStep("Tra cứu người dùng", "Tìm trong 3 tài khoản mẫu, không đọc người dùng Supabase.",
                "Tìm thử", "Có 3 người dùng mẫu: Khách QA, Creator QA và Moderator QA."),
            DemoCenterStep("Duyệt hồ sơ Creator", "Phạm vi được ủy quyền: xử lý yêu cầu mẫu.",
                "Duyệt thử", "Đơn Creator mẫu được đánh dấu 'Đã duyệt' chỉ trong kịch bản demo.")
        )
        DemoPersona.OWNER -> listOf(
            DemoCenterStep("Xem tổng quan nền tảng", "Số liệu mẫu: 42 người dùng, 5 Creator, 3 đơn chờ.",
                "Xem tổng quan demo", "Đã mở tổng quan giả lập; không tải dữ liệu Owner thật."),
            DemoCenterStep("Ủy quyền quản trị viên", "Ví dụ gán 'review_creators' cho Quản trị viên mẫu.",
                "Cấp quyền demo", "Quản trị viên mẫu đã có quyền xét duyệt trong kịch bản; Supabase không đổi."),
            DemoCenterStep("Nhật ký kiểm toán", "Minh họa một thay đổi quyền mẫu trong nhật ký.",
                "Xem audit demo", "Đã ghi nhận sự kiện mô phỏng, không lưu nhật ký hệ thống thật.")
        )
        DemoPersona.DEVELOPER -> listOf(
            DemoCenterStep("Kiểm tra trạng thái", "Các kết quả QA giả định không thay thế log kiểm thử thực.",
                "Xem chẩn đoán mẫu", "Kiểm tra mẫu hoàn tất: build OK, MediaSession sẵn sàng (mô phỏng)."),
            DemoCenterStep("Xử lý lỗi phát nhạc", "Tình huống tệp không còn truy cập được trong hàng đợi.",
                "Chạy kịch bản lỗi", "Đã mô phỏng bỏ qua bài lỗi và tiếp tục bài sau.")
        )
    }

    fun boundedProgress(actor: DemoPersona, stepsDone: Int): Int =
        stepsDone.coerceIn(0, steps(actor).size)

    fun advance(actor: DemoPersona, stepsDone: Int): Int =
        (boundedProgress(actor, stepsDone) + 1).coerceAtMost(steps(actor).size)
}
