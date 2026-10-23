
package com.example.musicflow.core.playlist.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

import com.example.musicflow.core.playlist.PlaylistTrack

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"])
    ]
)
data class PlaylistTrackEntity(
    val playlistId: String,
    val mediaId: String,
    val audioUri: String,
    val title: String,
    val artist: String,
    val addedAtMs: Long,
    val position: Int
)

fun PlaylistTrackEntity.toModel() = PlaylistTrack(
    mediaId = mediaId,
    audioUri = audioUri,
    title = title,
    artist = artist,
    addedAtMs = addedAtMs
)

fun PlaylistTrack.toEntity(
    playlistId: String,
    position: Int
) = PlaylistTrackEntity(
    playlistId = playlistId,
    mediaId = mediaId,
    audioUri = audioUri,
    title = title,
    artist = artist,
    addedAtMs = addedAtMs,
    position = position
)
