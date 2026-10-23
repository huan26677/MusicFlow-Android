
package com.example.musicflow.core.playlist.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musicflow.core.playlist.LocalPlaylist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

fun PlaylistEntity.toModel() = LocalPlaylist(
    id = id,
    name = name,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs
)

fun LocalPlaylist.toEntity() = PlaylistEntity(
    id = id,
    name = name,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs
)
