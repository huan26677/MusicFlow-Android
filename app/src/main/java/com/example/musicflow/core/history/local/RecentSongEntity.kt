
package com.example.musicflow.core.history.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musicflow.core.history.RecentSong

@Entity(tableName = "recent_songs")
data class RecentSongEntity(
    @PrimaryKey
    val mediaId: String,
    val audioUri: String,
    val title: String,
    val artist: String,
    val playedAtMs: Long,
    val position: Int
)

fun RecentSongEntity.toRecentSong() = RecentSong(
    mediaId = mediaId,
    audioUri = audioUri,
    title = title,
    artist = artist,
    playedAtMs = playedAtMs
)

fun RecentSong.toEntity(position: Int) =
    RecentSongEntity(
        mediaId = mediaId,
        audioUri = audioUri,
        title = title,
        artist = artist,
        playedAtMs = playedAtMs,
        position = position
    )
