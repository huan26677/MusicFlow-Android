
package com.example.musicflow.core.history

data class RecentSong(
    val mediaId: String,
    val audioUri: String,
    val title: String,
    val artist: String,
    val playedAtMs: Long
)

object RecentHistory {

    const val MAX_ITEMS = 20

    fun add(
        existing: List<RecentSong>,
        song: RecentSong
    ): List<RecentSong> {
        if (
            song.mediaId.isBlank() ||
            song.audioUri.isBlank()
        ) {
            return existing
        }

        // Bỏ bản ghi cũ của cùng bài hát.
        val others = existing
            .filterNot {
                it.mediaId == song.mediaId
            }
            .take(MAX_ITEMS - 1)

        // Bài vừa nghe luôn đứng đầu.
        return listOf(song) + others
    }
}
