package com.example.musicflow.core.history

import java.util.Locale

/**
 * History sync carries only metadata, never file paths or device-specific URIs.
 * Existing local listening activity is not overwritten by older cloud entries.
 */
object CloudHistoryRules {
    private fun normalize(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    fun key(title: String, artist: String): String =
        normalize(title) + "\u0000" + normalize(artist)

    fun merge(
        current: List<RecentSong>,
        restored: List<RecentSong>
    ): List<RecentSong> =
        (current + restored)
            .filter { it.mediaId.isNotBlank() && it.audioUri.isNotBlank() }
            .groupBy { it.mediaId }
            .values.mapNotNull { sameTrack ->
                sameTrack.maxByOrNull { it.playedAtMs }
            }
            .sortedWith(
                compareByDescending<RecentSong> { it.playedAtMs }
                    .thenBy { it.mediaId }
            )
            .take(RecentHistory.MAX_ITEMS)
}
