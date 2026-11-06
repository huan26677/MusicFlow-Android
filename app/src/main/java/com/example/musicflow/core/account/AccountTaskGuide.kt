package com.example.musicflow.core.account

/** Brief, role-scoped explanations; no client-side grants are inferred here. */
object AccountTaskGuide {
    fun nextStep(workspace: AccountWorkspace, access: MusicFlowAccess): String = when (workspace) {
        AccountWorkspace.CREATOR -> if (access.isCreator) {
            "Mở kho tác phẩm để tạo bản nháp, bổ sung chứng cứ quyền và theo dõi xét duyệt."
        } else {
            "Xem tình trạng đơn hoặc đăng ký Nghệ sĩ/Tác giả. Chỉ được tạo tác phẩm sau khi duyệt."
        }
        AccountWorkspace.MANAGEMENT -> if (access.isOwner) {
            "Tìm tài khoản để quản lý vai trò; mở tổng quan hoặc nhật ký khi cần."
        } else {
            "Tìm tài khoản và xử lý đúng quyền Owner đã ủy nhiệm."
        }
        AccountWorkspace.MODERATION -> when {
            access.canReviewCreators -> "Tải các yêu cầu đang chờ, đọc hồ sơ và nhập lý do trước khi quyết định."
            access.canReviewMetadata -> "Tải tác phẩm chờ xét duyệt; duyệt hồ sơ không đồng nghĩa xuất bản nhạc."
            access.canModerateCatalog -> "Kiểm tra giấy phép và trạng thái trước khi điều chỉnh danh mục online."
            else -> "Bạn chưa có quyền kiểm duyệt; hãy kiểm tra lại quyền."
        }
        AccountWorkspace.DEVELOPER -> "Kiểm tra phiên và kết nối bằng công cụ QA; không thay thế quyền Owner."
    }
}
