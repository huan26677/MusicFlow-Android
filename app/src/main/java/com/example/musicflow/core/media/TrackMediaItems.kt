package com.example.musicflow.core.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.musicflow.core.online.OnlineTrack
import java.net.URI

/**
 * The single conversion point for device, picked, remote and persisted audio.
 * Never resolves/refreshes remote URLs here: that belongs to a licensed
 * streaming provider and must be enforced by the backend.
 */
object TrackMediaItems {
    data class PlayableQueue(val items: List<MediaItem>, val startIndex: Int)

    /**
     * Preserve which row was selected even if the catalog contains a duplicate
     * ID or unplayable rows before it.
     */
    fun onlineQueue(tracks: List<OnlineTrack>, selectedIndex: Int): PlayableQueue? {
        if (selectedIndex !in tracks.indices) return null
        val playable = tracks.mapIndexedNotNull { index, track ->
            fromOnlineOrNull(track)?.let { index to it }
        }
        val index = playable.indexOfFirst { it.first == selectedIndex }
        if (index < 0) return null
        return PlayableQueue(playable.map { it.second }, index)
    }

    fun fromDevice(song: DeviceSong): MediaItem =
        mediaItem(
            mediaId = MediaSourceContract.device(song.id, song.mediaId).mediaId,
            uri = song.uri,
            title = song.title,
            artist = song.artist
        )

    fun fromPicked(uri: Uri, title: String, artist: String?): MediaItem =
        mediaItem(
            mediaId = MediaSourceContract.picked(uri.toString()).mediaId,
            uri = uri,
            title = title,
            artist = artist
        )

    fun fromOnlineOrNull(track: OnlineTrack): MediaItem? {
        val address = track.streamUrl ?: return null
        if (!isSafeHttpsMediaUrl(address)) return null
        val source = runCatching {
            MediaSourceContract.online(track.id)
        }.getOrNull() ?: return null
        return mediaItem(
            mediaId = source.mediaId,
            uri = Uri.parse(address),
            title = track.title,
            artist = track.artist,
            album = track.album
        )
    }

    /**
     * Stored references are already trusted as *legacy data format*, not as
     * licensed network content. Preserve IDs/URIs without a silent migration;
     * actual playback remains subject to Android's URI permissions.
     */
    fun fromStored(
        mediaId: String,
        audioUri: String,
        title: String,
        artist: String?,
        album: String? = null
    ): MediaItem =
        mediaItem(
            mediaId = MediaSourceContract.stored(mediaId).mediaId,
            uri = Uri.parse(audioUri),
            title = title,
            artist = artist,
            album = album
        )

    fun isSafeHttpsMediaUrl(raw: String): Boolean {
        if (!raw.startsWith("https://", ignoreCase = true)) return false
        return try {
            val parsed = URI(raw)
            parsed.scheme.equals("https", ignoreCase = true) &&
                !parsed.host.isNullOrBlank() &&
                parsed.userInfo == null &&
                parsed.rawFragment == null
        } catch (_: Exception) {
            false
        }
    }

    private fun mediaItem(
        mediaId: String,
        uri: Uri,
        title: String,
        artist: String?,
        album: String? = null
    ): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .build()
            )
            .build()
}
