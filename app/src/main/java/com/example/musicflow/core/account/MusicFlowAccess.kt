package com.example.musicflow.core.account

import org.json.JSONObject

/** Server-authorized roles are completely independent of Free/Premium entitlements. */
data class MusicFlowAccess(
    val roles: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet()
) {
    val isOwner get() = "owner" in roles
    val isPlatformAdmin get() = "platform_admin" in roles
    val isDeveloper get() = "developer" in roles
    val isCreator get() = "artist" in roles || "author" in roles
    val canManageUsers get() = isOwner ||
        (isPlatformAdmin && "manage_users" in permissions)
    val canReviewCreators get() = isOwner ||
        (isPlatformAdmin && "review_creators" in permissions)
    val canModerateCatalog get() = isOwner ||
        (isPlatformAdmin && "moderate_catalog" in permissions)

    companion object {
        val NONE = MusicFlowAccess()

        /** Only an authenticated backend RPC may supply these values. */
        fun fromResponse(json: JSONObject): MusicFlowAccess {
            fun strings(name: String): Set<String> {
                val data = json.optJSONArray(name) ?: return emptySet()
                return (0 until data.length()).mapNotNull { index ->
                    data.optString(index).takeIf { it.isNotBlank() }
                }.toSet()
            }
            return MusicFlowAccess(strings("roles"), strings("permissions"))
        }
    }
}
