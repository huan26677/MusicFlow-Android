package com.example.musicflow.core.account

/** One task at a time: avoids long mixed reviewer pages and preserves RBAC. */
enum class AccountReviewArea(val label: String, val guidance: String) {
    APPLICATIONS("Đơn Creator", "Xét duyệt yêu cầu trở thành Nghệ sĩ hoặc Tác giả"),
    DRAFTS("Hồ sơ tác phẩm", "Kiểm tra metadata, tín chỉ và chứng cứ riêng tư"),
    CATALOG("Nhạc online", "Chỉ phát nhạc có quyền và đã qua kiểm duyệt")
}

object AccountReviewPlan {
    fun available(access: MusicFlowAccess): List<AccountReviewArea> = buildList {
        if (access.canReviewCreators) add(AccountReviewArea.APPLICATIONS)
        if (access.canReviewMetadata) add(AccountReviewArea.DRAFTS)
        if (access.canModerateCatalog) add(AccountReviewArea.CATALOG)
    }

    fun active(access: MusicFlowAccess, requested: AccountReviewArea?): AccountReviewArea? =
        available(access).let { allowed -> requested?.takeIf { it in allowed } ?: allowed.firstOrNull() }

    fun permissionLabel(permission: String): String = when (permission) {
        "manage_users" -> "Quản lý người dùng"
        "review_creators" -> "Duyệt Creator"
        "moderate_catalog" -> "Kiểm duyệt nhạc online"
        else -> "Quyền khác"
    }
}
