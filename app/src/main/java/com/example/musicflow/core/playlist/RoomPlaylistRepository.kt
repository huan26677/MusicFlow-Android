
package com.example.musicflow.core.playlist

import androidx.room.withTransaction

import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.local.toEntity
import com.example.musicflow.core.playlist.local.toModel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import java.util.UUID

class RoomPlaylistRepository(
    private val database: RecentHistoryDatabase
) {
    private val dao = database.playlistDao()

    /** Adds cloud metadata without replacing any existing local playlist/track. */
    suspend fun mergeCloudPlaylist(
        playlist: LocalPlaylist,
        tracks: List<PlaylistTrack>
    ): Int = database.withTransaction {
        if (dao.getPlaylist(playlist.id) == null) {
            dao.insertPlaylist(playlist.toEntity())
        }
        val present = dao.getTracks(playlist.id).map { it.mediaId }.toMutableSet()
        var added = 0
        tracks.forEach { track ->
            if (track.mediaId.isNotBlank() && track.audioUri.isNotBlank() &&
                present.add(track.mediaId)
            ) {
                dao.insertTrack(
                    track.toEntity(playlist.id, dao.nextPosition(playlist.id))
                )
                added++
            }
        }
        added
    }

    fun observePlaylists(): Flow<List<LocalPlaylist>> =
        dao.observePlaylists().map { entities ->
            entities.map { it.toModel() }
        }

    fun observeTracks(
        playlistId: String
    ): Flow<List<PlaylistTrack>> =
        dao.observeTracks(playlistId).map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun getPlaylist(
        id: String
    ): LocalPlaylist? =
        dao.getPlaylist(id)?.toModel()

    suspend fun getTracks(
        playlistId: String
    ): List<PlaylistTrack> =
        dao.getTracks(playlistId).map {
            it.toModel()
        }

    suspend fun create(
        rawName: String
    ): LocalPlaylist? {
        val name = PlaylistRules
            .normalizedNameOrNull(rawName)
            ?: return null

        val now = System.currentTimeMillis()

        val playlist = LocalPlaylist(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAtMs = now,
            updatedAtMs = now
        )

        dao.insertPlaylist(playlist.toEntity())

        return playlist
    }

    suspend fun rename(
        id: String,
        rawName: String
    ): Boolean = database.withTransaction {
        val existing = dao.getPlaylist(id)
            ?.toModel()
            ?: return@withTransaction false

        val updated = PlaylistRules.rename(
            playlist = existing,
            newName = rawName,
            updatedAtMs = System.currentTimeMillis()
        ) ?: return@withTransaction false

        dao.renamePlaylist(
            id = id,
            name = updated.name,
            updatedAtMs = updated.updatedAtMs
        ) > 0
    }

    suspend fun deletePlaylist(
        id: String
    ): Boolean {
        if (id.isBlank()) return false

        return dao.deletePlaylist(id) > 0
    }

    suspend fun addTrack(
        playlistId: String,
        track: PlaylistTrack
    ): Boolean = database.withTransaction {
        if (dao.getPlaylist(playlistId) == null) {
            return@withTransaction false
        }

        val existing = dao.getTracks(playlistId)
            .map { it.toModel() }

        val updated = PlaylistRules.addTrack(
            existing = existing,
            track = track
        )

        // Bao gồm trường hợp trùng bài và thiếu URI.
        if (updated.size == existing.size) {
            return@withTransaction false
        }

        dao.insertTrack(
            track.toEntity(
                playlistId = playlistId,
                position = dao.nextPosition(playlistId)
            )
        )

        dao.touchPlaylist(
            id = playlistId,
            timeMs = System.currentTimeMillis()
        )

        true
    }

    suspend fun removeTrack(
        playlistId: String,
        mediaId: String
    ): Boolean = database.withTransaction {
        if (playlistId.isBlank() || mediaId.isBlank()) {
            return@withTransaction false
        }

        val deleted = dao.deleteTrack(
            playlistId = playlistId,
            mediaId = mediaId
        )

        if (deleted == 0) {
            return@withTransaction false
        }

        dao.touchPlaylist(
            id = playlistId,
            timeMs = System.currentTimeMillis()
        )

        true
    }

    suspend fun moveTrack(
        playlistId: String,
        mediaId: String,
        offset: Int
    ): Boolean = database.withTransaction {
        if (playlistId.isBlank() || mediaId.isBlank()) {
            return@withTransaction false
        }

        val before = dao.getTracks(playlistId)

        val after = PlaylistRules.moveTrack(
            existing = before.map { it.toModel() },
            mediaId = mediaId,
            offset = offset
        ) ?: return@withTransaction false

        val from = before.indexOfFirst {
            it.mediaId == mediaId
        }

        val selected = before[from]

        val neighborId = after[from].mediaId
        val neighbor = before.first {
            it.mediaId == neighborId
        }

        val updatedSelected = dao.updateTrackPosition(
            playlistId = playlistId,
            mediaId = selected.mediaId,
            position = neighbor.position
        )

        val updatedNeighbor = dao.updateTrackPosition(
            playlistId = playlistId,
            mediaId = neighbor.mediaId,
            position = selected.position
        )

        if (updatedSelected != 1 || updatedNeighbor != 1) {
            error("Không thể cập nhật thứ tự playlist.")
        }

        if (
            dao.touchPlaylist(
                id = playlistId,
                timeMs = System.currentTimeMillis()
            ) != 1
        ) {
            error("Playlist không còn tồn tại.")
        }

        true
    }
}
