package com.example.musicflow.core.playlist

import androidx.media3.common.MediaItem
import com.example.musicflow.core.media.TrackMediaItems

object PlaylistMediaItems {

    fun fromTracks(
        tracks: List<PlaylistTrack>
    ): List<MediaItem> = tracks.map { track ->
        TrackMediaItems.fromStored(
            track.mediaId, track.audioUri, track.title, track.artist
        )
    }
}
