
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
}
