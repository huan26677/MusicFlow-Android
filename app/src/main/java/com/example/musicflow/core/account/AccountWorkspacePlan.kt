package com.example.musicflow.core.account

/** Navigation only. The server-side RPCs remain the authority for every action. */
enum class AccountWorkspace(val title: String, val description: String) {
    CREATOR("Sáng tạo", "Hồ sơ nghệ sĩ, tác giả và tiến độ đăng ký"),
    MANAGEMENT("Quản trị", "Tài khoản, phân quyền và tổng quan Owner"),
    MODERATION("Kiểm duyệt", "Xét duyệt Creator, tác phẩm và nội dung online"),
    DEVELOPER("Công cụ QA", "Chẩn đoán kỹ thuật cho phiên bản Debug")
}

object AccountWorkspacePlan {
    fun roleLabel(role: String): String = when (role) {
        "owner" -> "Chủ nền tảng"
        "platform_admin" -> "Quản trị viên"
        "developer" -> "Nhà phát triển"
        "moderator" -> "Kiểm duyệt viên"
        "artist" -> "Nghệ sĩ"
        "author" -> "Tác giả"
        else -> "Vai trò khác"
    }

    fun primaryAction(access: MusicFlowAccess, isDebug: Boolean): String = when (
        initial(access, isDebug)
    ) {
        AccountWorkspace.MANAGEMENT -> "Mở khu vực quản trị"
        AccountWorkspace.MODERATION -> "Mở khu vực kiểm duyệt"
        AccountWorkspace.DEVELOPER -> "Mở công cụ QA"
        AccountWorkspace.CREATOR -> if (access.isCreator) {
            "Mở hồ sơ nghệ sĩ / tác giả"
        } else "Đăng ký nghệ sĩ / tác giả"
    }

    /** A restricted admin sees no management tab without the matching permission. */
    fun available(access: MusicFlowAccess, isDebug: Boolean): List<AccountWorkspace> = buildList {
        add(AccountWorkspace.CREATOR)
        if (access.canManageUsers) add(AccountWorkspace.MANAGEMENT)
        if (access.canReviewCreators || access.canReviewMetadata || access.canModerateCatalog) {
            add(AccountWorkspace.MODERATION)
        }
        if (isDebug && access.isDeveloper) add(AccountWorkspace.DEVELOPER)
    }

    /** Prioritize the actor's real task. No privileges are inferred from Premium tier. */
    fun initial(access: MusicFlowAccess, isDebug: Boolean): AccountWorkspace {
        val available = available(access, isDebug)
        return when {
            AccountWorkspace.MANAGEMENT in available -> AccountWorkspace.MANAGEMENT
            AccountWorkspace.MODERATION in available -> AccountWorkspace.MODERATION
            AccountWorkspace.DEVELOPER in available -> AccountWorkspace.DEVELOPER
            else -> AccountWorkspace.CREATOR
        }
    }
}
