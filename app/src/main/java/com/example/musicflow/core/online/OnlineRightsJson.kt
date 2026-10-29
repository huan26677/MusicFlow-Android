package com.example.musicflow.core.online

import org.json.JSONObject

/**
 * Supabase uses snake_case flat columns. A standalone HTTPS catalog can send
 * a nested "rights" JSON object with camelCase fields.
 *
 * Missing fields are always denied, never default-granted.
 */
object OnlineRightsJson {
    fun fromSupabase(item: JSONObject): OnlineTrackRights = OnlineTrackRights(
        licenseCode = text(item, "license_code") ?: "UNVERIFIED",
        licenseUrl = text(item, "license_url"),
        attribution = text(item, "attribution_text"),
        sourceUrl = text(item, "source_url"),
        rightsConfirmed = item.optBoolean("rights_confirmed", false),
        streamingAllowed = item.optBoolean("streaming_allowed", false),
        downloadAllowed = item.optBoolean("download_allowed", false),
        commercialUseAllowed = item.optBoolean("commercial_use_allowed", false),
        verifiedAt = text(item, "rights_verified_at")
    )

    fun fromExternal(item: JSONObject): OnlineTrackRights? {
        val rights = item.optJSONObject("rights") ?: return null
        return OnlineTrackRights(
            licenseCode = text(rights, "licenseCode") ?: "UNVERIFIED",
            licenseUrl = text(rights, "licenseUrl"),
            attribution = text(rights, "attribution"),
            sourceUrl = text(rights, "sourceUrl"),
            rightsConfirmed = rights.optBoolean("rightsConfirmed", false),
            streamingAllowed = rights.optBoolean("streamingAllowed", false),
            downloadAllowed = rights.optBoolean("downloadAllowed", false),
            commercialUseAllowed = rights.optBoolean("commercialUseAllowed", false),
            verifiedAt = text(rights, "verifiedAt")
        )
    }

    private fun text(objectValue: JSONObject, key: String): String? =
        if (objectValue.isNull(key)) null
        else objectValue.optString(key).trim().take(500).ifBlank { null }
}
