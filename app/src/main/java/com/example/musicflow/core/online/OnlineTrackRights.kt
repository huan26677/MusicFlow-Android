package com.example.musicflow.core.online

import java.net.URI

/** Catalog metadata, NOT proof of ownership or a license grant. */
data class OnlineTrackRights(
    val licenseCode: String = "UNVERIFIED",
    val licenseUrl: String? = null,
    val attribution: String? = null,
    val sourceUrl: String? = null,
    val rightsConfirmed: Boolean = false,
    val streamingAllowed: Boolean = false,
    val downloadAllowed: Boolean = false,
    val commercialUseAllowed: Boolean = false,
    val verifiedAt: String? = null
)

enum class OnlineRightsStatus {
    VERIFIED_STREAMING, NOT_VERIFIED, STREAMING_NOT_ALLOWED,
    INCOMPLETE_CREDITS, UNSUPPORTED_LICENSE
}

/**
 * Additional client-side fail-closed guard for online playback.
 *
 * Rights metadata is always asserted by an authorized publisher/backend;
 * it must NEVER be treated as independent legal proof. Supabase RLS also
 * denies unverified rows before returning them to public clients.
 */
object OnlineRightsPolicy {
    private val supported = setOf("CC0-1.0", "CC-BY-4.0", "DIRECT")

    fun isSafeHttpsLink(address: String?): Boolean {
        if (address.isNullOrBlank() || !address.startsWith("https://", true)) return false
        return runCatching {
            val uri = URI(address)
            uri.scheme.equals("https", true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.rawFragment == null
        }.getOrDefault(false)
    }

    fun streamingStatus(rights: OnlineTrackRights?): OnlineRightsStatus {
        if (rights == null || !rights.rightsConfirmed || rights.verifiedAt.isNullOrBlank()) {
            return OnlineRightsStatus.NOT_VERIFIED
        }
        if (!rights.streamingAllowed) return OnlineRightsStatus.STREAMING_NOT_ALLOWED
        if (rights.licenseCode !in supported) {
            return OnlineRightsStatus.UNSUPPORTED_LICENSE
        }
        if (!isSafeHttpsLink(rights.sourceUrl) ||
            rights.attribution.isNullOrBlank()
        ) return OnlineRightsStatus.INCOMPLETE_CREDITS
        val licenseUrl = rights.licenseUrl
        if (rights.licenseCode != "DIRECT" && !isSafeHttpsLink(licenseUrl)) {
            return OnlineRightsStatus.INCOMPLETE_CREDITS
        }
        // A DIRECT permission may be held privately, without a public license
        // URL. The backend must retain proof and explicitly confirm rights.
        if (licenseUrl != null && !isSafeHttpsLink(licenseUrl)) {
            return OnlineRightsStatus.INCOMPLETE_CREDITS
        }
        return OnlineRightsStatus.VERIFIED_STREAMING
    }

    fun canStream(rights: OnlineTrackRights?): Boolean =
        streamingStatus(rights) == OnlineRightsStatus.VERIFIED_STREAMING

    fun statusLabel(status: OnlineRightsStatus): String = when (status) {
        OnlineRightsStatus.VERIFIED_STREAMING -> "Đã xác minh quyền phát"
        OnlineRightsStatus.NOT_VERIFIED -> "Chưa xác minh quyền phát"
        OnlineRightsStatus.STREAMING_NOT_ALLOWED -> "Giấy phép không cho phép phát"
        OnlineRightsStatus.INCOMPLETE_CREDITS -> "Thiếu nguồn hoặc thông tin ghi công"
        OnlineRightsStatus.UNSUPPORTED_LICENSE -> "Loại giấy phép chưa được hỗ trợ"
    }
}
