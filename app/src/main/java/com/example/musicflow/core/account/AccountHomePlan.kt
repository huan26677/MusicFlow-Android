package com.example.musicflow.core.account

/** Display hints only. Tier/role data must originate from the authenticated backend. */
data class AccountHomeOverview(
    val greeting: String,
    val tierLabel: String,
    val tierExplanation: String,
    val rolesLabel: String,
    val primaryAction: String?
)

object AccountHomePlan {
    fun overview(identity: AccountIdentity, demoSession: Boolean, isDebug: Boolean): AccountHomeOverview {
        val name = identity.displayName.trim().ifBlank { identity.email.substringBefore('@') }
            .ifBlank { "bạn" }
        val tier = when {
            demoSession -> "Demo trên thiết bị"
            identity.tier == "premium" -> "Premium"
            else -> "Free"
        }
        val tierInfo = when {
            demoSession -> "Không đồng bộ Supabase. Bạn vẫn nghe nhạc offline bình thường."
            identity.tier == "premium" ->
                "Gói Premium đã được máy chủ xác nhận. Quyền quản trị được cấp riêng."
            else -> "Gói Free — vẫn nghe nhạc offline và quản lý thư viện cá nhân."
        }
        val roles = when {
            demoSession -> "Người nghe (Demo)"
            identity.access.roles.isEmpty() -> "Người nghe"
            else -> identity.access.roles.sorted()
                .joinToString(", ") { AccountWorkspacePlan.roleLabel(it) }
        }
        return AccountHomeOverview(
            greeting = "Xin chào, $name",
            tierLabel = tier,
            tierExplanation = tierInfo,
            rolesLabel = roles,
            primaryAction = if (demoSession) null else
                AccountWorkspacePlan.primaryAction(identity.access, isDebug)
        )
    }
}
