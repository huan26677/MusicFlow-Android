
package com.example.musicflow.core.playlist.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query(
        "SELECT * FROM playlists " +
                "ORDER BY updatedAtMs DESC, id ASC"
    )
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: String): PlaylistEntity?

    @Query(
        "UPDATE playlists " +
                "SET name = :name, updatedAtMs = :updatedAtMs " +
                "WHERE id = :id"
    )
    suspend fun renamePlaylist(
        id: String,
        name: String,
        updatedAtMs: Long
    ): Int

    @Query(
        "UPDATE playlists SET updatedAtMs = :timeMs " +
                "WHERE id = :id"
    )
    suspend fun touchPlaylist(
        id: String,
        timeMs: Long
    ): Int

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String): Int

    @Insert
    suspend fun insertTrack(track: PlaylistTrackEntity)

    @Query(
        "SELECT * FROM playlist_tracks " +
                "WHERE playlistId = :playlistId " +
                "ORDER BY position ASC, mediaId ASC"
    )
    suspend fun getTracks(
        playlistId: String
    ): List<PlaylistTrackEntity>

    @Query(
        "SELECT * FROM playlist_tracks " +
                "WHERE playlistId = :playlistId " +
                "ORDER BY position ASC, mediaId ASC"
    )
    fun observeTracks(
        playlistId: String
    ): Flow<List<PlaylistTrackEntity>>

    @Query(
        "SELECT COALESCE(MAX(position), -1) + 1 " +
                "FROM playlist_tracks " +
                "WHERE playlistId = :playlistId"
    )
    suspend fun nextPosition(playlistId: String): Int

    @Query(
        "DELETE FROM playlist_tracks " +
                "WHERE playlistId = :playlistId " +
                "AND mediaId = :mediaId"
    )
    suspend fun deleteTrack(
        playlistId: String,
        mediaId: String
    ): Int

    @Query(
        "UPDATE playlist_tracks SET position = :position " +
            "WHERE playlistId = :playlistId " +
            "AND mediaId = :mediaId"
    )
    suspend fun updateTrackPosition(
        playlistId: String,
        mediaId: String,
        position: Int
    ): Int
}
