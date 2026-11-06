package com.example.musicflow.core.account

/** Presentation-only state. Supabase must still enforce all application rules and decisions. */
object CreatorApplicationUiRules {
    fun roleLabel(role: String): String = when (role) {
        "artist" -> "Nghệ sĩ"
        "author" -> "Tác giả"
        else -> "Vai trò sáng tạo"
    }

    fun statusLabel(status: String): String = when (status) {
        "pending" -> "Đang chờ duyệt"
        "approved" -> "Đã duyệt"
        "rejected" -> "Đã từ chối"
        else -> "Chưa xác định"
    }

    /** Prevent accidental duplicate submissions while a same-role application is pending. */
    fun hasPendingRole(requests: Collection<Pair<String, String>>, role: String): Boolean =
        requests.any { (requestedRole, status) -> requestedRole == role && status == "pending" }

    fun canSubmit(
        hasCreatorRole: Boolean,
        requestsLoaded: Boolean,
        isBusy: Boolean,
        stageName: String,
        hasPendingSelectedRole: Boolean
    ): Boolean = !hasCreatorRole && requestsLoaded && !isBusy &&
        stageName.trim().length in 2..80 && !hasPendingSelectedRole

    fun canReview(isBusy: Boolean, reason: String): Boolean =
        !isBusy && reason.trim().length in 5..300
}
