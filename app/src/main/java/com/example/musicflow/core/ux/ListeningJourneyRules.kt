package com.example.musicflow.core.ux

/** Presentation rules only: never mutate playback queues, files, or playlists. */
object ListeningJourneyRules {
    const val RECENT_PREVIEW_LIMIT = 4

    fun recentVisibleCount(total: Int, expanded: Boolean): Int =
        if (expanded) total.coerceAtLeast(0)
        else total.coerceAtLeast(0).coerceAtMost(RECENT_PREVIEW_LIMIT)

    fun queuePosition(index: Int, count: Int, hasMedia: Boolean): String =
        if (!hasMedia || count <= 0) "Chưa có bài trong hàng đợi"
        else "Bài ${index.coerceIn(0, count - 1) + 1} / $count"

    /** Find a nonempty result group when the user is looking at an empty tab. */
    fun suggestedSearchCategory(
        selected: String,
        songCount: Int,
        albumCount: Int,
        artistCount: Int,
        genreCount: Int
    ): String? {
        val categories = listOf(
            "songs" to songCount,
            "albums" to albumCount,
            "artists" to artistCount,
            "genres" to genreCount
        )
        if (categories.any { it.first == selected && it.second > 0 }) return null
        return categories.firstOrNull { it.first != selected && it.second > 0 }?.first
    }

    fun searchCategoryLabel(value: String): String = when (value) {
        "songs" -> "Bài hát"
        "albums" -> "Album"
        "artists" -> "Nghệ sĩ"
        "genres" -> "Thể loại"
        else -> "Kết quả"
    }
}
