package com.example.musicflow.feature.player

import android.content.Context
import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playback.PlaybackController
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.PlaylistMediaItems

data class PickedAudioFile(
    val uri: Uri,
    val title: String,
    val artist: String?
)

class PlayerViewModel(
    private val playbackController: PlaybackController
) : ViewModel() {

    val uiState = playbackController.uiState

    fun playPickedFiles(
        files: List<PickedAudioFile>
    ) {
        val items = files.map { file ->
            MediaItem.Builder()
                .setMediaId(file.uri.toString())
                .setUri(file.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(file.title)
                        .setArtist(file.artist)
                        .build()
                )
                .build()
        }

        playbackController.playQueue(items)
    }

    fun playDeviceSongs(
        songs: List<DeviceSong>,
        selectedIndex: Int
    ) {
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.mediaId ?: "device-${song.id}")
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .build()
                )
                .build()
        }

        playbackController.playQueue(
            items,
            selectedIndex
        )
    }

    fun playRecentSongs(
        songs: List<RecentSong>,
        selectedIndex: Int
    ) {
        if (selectedIndex !in songs.indices) {
            return
        }

        val items = songs.map { song ->
            MediaItem.Builder()
                // Giữ mediaId gốc để lịch sử không tạo
                // bản ghi khác khi phát lại cùng bài.
                .setMediaId(song.mediaId)
                .setUri(Uri.parse(song.audioUri))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .build()
                )
                .build()
        }

        playbackController.playQueue(
            items = items,
            startIndex = selectedIndex
        )
    }

    fun playPlaylistTracks(
        tracks: List<PlaylistTrack>,
        selectedIndex: Int = 0
    ) {
        if (selectedIndex !in tracks.indices) return

        playbackController.playQueue(
            items = PlaylistMediaItems.fromTracks(tracks),
            startIndex = selectedIndex
        )
    }

    fun togglePlayPause() =
        playbackController.togglePlayPause()

    fun next() =
        playbackController.next()

    fun previous() =
        playbackController.previous()

    fun toggleShuffle() =
        playbackController.toggleShuffle()

    fun cycleRepeat() =
        playbackController.cycleRepeat()

    fun seekTo(positionMs: Long) =
        playbackController.seekTo(positionMs)

    fun setPlaybackSpeed(speed: Float) =
        playbackController.setPlaybackSpeed(speed)

    fun refreshProgress() =
        playbackController.refreshProgress()

    override fun onCleared() {
        playbackController.release()
        super.onCleared()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    require(
                        modelClass.isAssignableFrom(
                            PlayerViewModel::class.java
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return PlayerViewModel(
                        PlaybackController(
                            context.applicationContext
                        )
                    ) as T
                }
            }
    }
}
