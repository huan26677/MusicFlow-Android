package com.example.musicflow.core.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.musicflow.core.online.OnlineTrack
import com.example.musicflow.core.online.OnlineRightsPolicy
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
        if (!OnlineRightsPolicy.canStream(track.rights)) return null
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
     * Stored references are trusted as a legacy data format, not as rights
     * metadata. Only the actual playback location determines whether a saved
     * item is local: a previously online track downloaded to device storage
     * must remain playable even when its old mediaId begins with "online:".
     * Android file/SAF access permissions still apply to that local file.
     */
    fun fromStored(
        mediaId: String,
        audioUri: String,
        title: String,
        artist: String?,
        album: String? = null
    ): MediaItem {
        require(isSafeStoredLocal(mediaId, audioUri)) {
            "Stored remote streams require renewed rights validation before playback"
        }
        return mediaItem(
            mediaId = MediaSourceContract.stored(mediaId).mediaId,
            uri = Uri.parse(audioUri),
            title = title,
            artist = artist,
            album = album
        )
    }

    fun fromStoredOrNull(
        mediaId: String,
        audioUri: String,
        title: String,
        artist: String?,
        album: String? = null
    ): MediaItem? =
        if (isSafeStoredLocal(mediaId, audioUri)) {
            fromStored(mediaId, audioUri, title, artist, album)
        } else null

    fun isSafeStoredLocal(mediaId: String, audioUri: String): Boolean {
        // mediaId is only a persistent identity, never proof that data is
        // remote. Local copies may legitimately retain the same mediaId.
        return runCatching {
            val uri = URI(audioUri)
            uri.scheme.equals("content", true) ||
                uri.scheme.equals("file", true) ||
                (uri.scheme == null &&
                    uri.host == null &&
                    audioUri.startsWith("/") &&
                    !audioUri.startsWith("//"))
        }.getOrDefault(false)
    }

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
