package com.example.musicflow.core.community

import org.json.JSONObject

object CommunityRules {
    const val PAGE_SIZE = 20
    const val COMMENT_LIMIT = 500
    const val REASON_LIMIT = 300
    fun validStars(stars: Int) = stars in 1..5
    fun cleanComment(body: String): String =
        body.trim().also { require(it.length in 1..COMMENT_LIMIT) {
            "Bình luận cần từ 1 đến 500 ký tự."
        } }
    fun cleanReason(reason: String): String =
        reason.trim().also { require(it.length in 5..REASON_LIMIT) {
            "Lý do cần từ 5 đến 300 ký tự."
        } }
    fun isCloudTrackId(value: String): Boolean = runCatching {
        java.util.UUID.fromString(value).toString() == value.lowercase()
    }.getOrDefault(false)
}

data class CommunityComment(
    val id: String,
    val body: String,
    val displayName: String,
    val mine: Boolean,
    val createdAt: String
)

data class CommunitySnapshot(
    val averageStars: Double = 0.0,
    val ratingCount: Int = 0,
    val myStars: Int = 0,
    val commentCount: Int = 0,
    val comments: List<CommunityComment> = emptyList(),
    val authenticated: Boolean = false,
    val demo: Boolean = false
) {
    companion object {
        fun decode(json: JSONObject, authenticated: Boolean): CommunitySnapshot {
            val rows = json.optJSONArray("comments")
            val comments = (0 until (rows?.length() ?: 0)).mapNotNull { index ->
                val item = rows?.optJSONObject(index) ?: return@mapNotNull null
                CommunityComment(
                    id = item.optString("id"),
                    body = item.optString("body"),
                    displayName = item.optString("display_name", "Người nghe"),
                    mine = item.optBoolean("mine", false),
                    createdAt = item.optString("created_at")
                )
            }
            return CommunitySnapshot(
                averageStars = json.optDouble("average_stars", 0.0),
                ratingCount = json.optInt("rating_count", 0),
                myStars = json.optInt("my_stars", 0),
                commentCount = json.optInt("comment_count", 0),
                comments = comments,
                authenticated = authenticated
            )
        }
    }
}
