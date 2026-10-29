package com.example.musicflow.core.playlist

import androidx.media3.common.MediaItem
import com.example.musicflow.core.media.TrackMediaItems

object PlaylistMediaItems {

    fun fromTracks(
        tracks: List<PlaylistTrack>
    ): List<MediaItem> = tracks.mapNotNull { track ->
        TrackMediaItems.fromStoredOrNull(
            track.mediaId, track.audioUri, track.title, track.artist
        )
    }

    fun playableSelection(
        tracks: List<PlaylistTrack>,
        selectedIndex: Int
    ): Pair<List<MediaItem>, Int>? {
        if (selectedIndex !in tracks.indices) return null
        val available = tracks.mapIndexedNotNull { index, track ->
            TrackMediaItems.fromStoredOrNull(
                track.mediaId, track.audioUri, track.title, track.artist
            )?.let { index to it }
        }
        val index = available.indexOfFirst { it.first == selectedIndex }
        return if (index < 0) null else available.map { it.second } to index
    }
}
