
package com.example.musicflow

import android.content.Context
import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

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
                .setMediaId("device-${song.id}")
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
