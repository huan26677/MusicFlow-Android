package com.example.musicflow.core.account

import org.json.JSONObject

/** Aggregated, owner-only diagnostic counts; no personal information or credentials. */
data class OwnerDemoSnapshot(
    val ownerCount: Int,
    val userCount: Int,
    val platformAdmins: Int,
    val moderators: Int,
    val artists: Int,
    val authors: Int,
    val developers: Int,
    val pendingCreatorRequests: Int,
    val pendingDrafts: Int,
    val approvedMetadata: Int,
    val uploadedPrivateFiles: Int,
    val activePremiumUsers: Int,
    val nonPremiumUsers: Int,
    val fullyDelegatedAdmins: Int
) {
    val hasExactlyOneOwner get() = ownerCount == 1
    /** Counts describe demo prerequisites, not proof of login or role E2E success. */
    val hasCoreDemoActors get() = hasExactlyOneOwner && platformAdmins > 0 &&
        fullyDelegatedAdmins > 0 && moderators > 0 && artists > 0 && authors > 0 &&
        developers > 0
    val needsTestActors get() = !hasCoreDemoActors
    val hasDemoSubscriptionExamples get() = activePremiumUsers > 0 && nonPremiumUsers > 0
    val hasPendingCreatorExample get() = pendingCreatorRequests > 0

    companion object {
        fun fromResponse(raw: String): OwnerDemoSnapshot {
            val json = JSONObject(raw)
            fun count(key: String): Int =
                json.getInt(key).also { require(it >= 0) { "Invalid count: $key" } }
            return OwnerDemoSnapshot(
                ownerCount = count("owner_count"),
                userCount = count("user_count"),
                platformAdmins = count("platform_admin_count"),
                moderators = count("moderator_count"),
                artists = count("artist_count"),
                authors = count("author_count"),
                developers = count("developer_count"),
                pendingCreatorRequests = count("creator_requests_pending"),
                pendingDrafts = count("drafts_pending"),
                approvedMetadata = count("drafts_metadata_approved"),
                uploadedPrivateFiles = count("creator_files_uploaded"),
                activePremiumUsers = count("active_premium_count"),
                nonPremiumUsers = count("non_premium_count"),
                fullyDelegatedAdmins = count("delegated_admin_count")
            )
        }
    }
}

