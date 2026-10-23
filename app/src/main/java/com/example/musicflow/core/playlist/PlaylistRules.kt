
package com.example.musicflow.core.playlist

data class LocalPlaylist(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

data class PlaylistTrack(
    val mediaId: String,
    val audioUri: String,
    val title: String,
    val artist: String,
    val addedAtMs: Long
)

object PlaylistRules {

    const val MAX_NAME_LENGTH = 60

    private val whitespacePattern = Regex("\\s+")

    fun normalizedNameOrNull(rawName: String): String? {
        val name = rawName
            .trim()
            .replace(whitespacePattern, " ")

        return name.takeIf {
            it.isNotEmpty() &&
                    it.length <= MAX_NAME_LENGTH
        }
    }

    fun rename(
        playlist: LocalPlaylist,
        newName: String,
        updatedAtMs: Long
    ): LocalPlaylist? {
        val validName =
            normalizedNameOrNull(newName) ?: return null

        return playlist.copy(
            name = validName,
            updatedAtMs = updatedAtMs
        )
    }

    fun addTrack(
        existing: List<PlaylistTrack>,
        track: PlaylistTrack
    ): List<PlaylistTrack> {
        if (
            track.mediaId.isBlank() ||
            track.audioUri.isBlank()
        ) {
            return existing
        }

        // Không thêm trùng bài vào cùng một playlist.
        if (existing.any { it.mediaId == track.mediaId }) {
            return existing
        }

        // Giữ thứ tự do người dùng thêm.
        return existing + track
    }

    fun removeTrack(
        existing: List<PlaylistTrack>,
        mediaId: String
    ): List<PlaylistTrack> {
        if (mediaId.isBlank()) return existing

        return existing.filterNot {
            it.mediaId == mediaId
        }
    }

    fun moveTrack(
        existing: List<PlaylistTrack>,
        mediaId: String,
        offset: Int
    ): List<PlaylistTrack>? {
        if (offset != -1 && offset != 1) return null

        val from = existing.indexOfFirst {
            it.mediaId == mediaId
        }

        if (from == -1) return null

        val to = from + offset
        if (to !in existing.indices) return null

        return existing.toMutableList().apply {
            val item = this[from]
            this[from] = this[to]
            this[to] = item
        }
    }
}
