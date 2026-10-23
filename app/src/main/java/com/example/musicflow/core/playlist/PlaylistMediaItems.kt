package com.example.musicflow.core.playlist

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

object PlaylistMediaItems {

    fun fromTracks(
        tracks: List<PlaylistTrack>
    ): List<MediaItem> = tracks.map { track ->
        MediaItem.Builder()
            .setMediaId(track.mediaId)
            .setUri(Uri.parse(track.audioUri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .build()
            )
            .build()
    }
}
