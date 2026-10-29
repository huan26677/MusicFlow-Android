package com.example.musicflow.feature.player

import android.content.Context
import android.net.Uri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.TrackMediaItems
import com.example.musicflow.core.playback.PlaybackController
import com.example.musicflow.core.playback.QueuePlacement
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.PlaylistMediaItems
import com.example.musicflow.core.online.OnlineTrack

data class PickedAudioFile(
    val uri: Uri,
    val title: String,
    val artist: String?
)

class PlayerViewModel(
    private val playbackController: PlaybackController
) : ViewModel() {

    val uiState = playbackController.uiState

    fun playOnlineTracks(tracks: List<OnlineTrack>, selectedIndex: Int) {
        val queue = TrackMediaItems.onlineQueue(tracks, selectedIndex) ?: return
        playbackController.playQueue(queue.items, queue.startIndex)
    }

    fun playPickedFiles(
        files: List<PickedAudioFile>
    ) {
        val items = files.map { TrackMediaItems.fromPicked(it.uri, it.title, it.artist) }

        playbackController.playQueue(items)
    }

    fun playDeviceSongs(
        songs: List<DeviceSong>,
        selectedIndex: Int
    ) {
        val items = songs.map(TrackMediaItems::fromDevice)

        playbackController.playQueue(
            items,
            selectedIndex
        )
    }

    /**
     * The same MediaItem conversion is used by Play, Play Next and Queue End.
     * More sources can call PlaybackController.enqueue with their own item.
     */
    fun queueDeviceSong(song: DeviceSong, placement: QueuePlacement): Boolean =
        playbackController.enqueue(TrackMediaItems.fromDevice(song), placement)

    fun removeQueueItem(index: Int, expectedVersion: Long): Boolean =
        playbackController.removeQueueItem(index, expectedVersion)

    fun moveQueueItem(fromIndex: Int, toIndex: Int, expectedVersion: Long): Boolean =
        playbackController.moveQueueItem(fromIndex, toIndex, expectedVersion)

    fun clearQueueOthers(expectedVersion: Long): Boolean =
        playbackController.clearQueueOthers(expectedVersion)

    fun playRecentSongs(
        songs: List<RecentSong>,
        selectedIndex: Int
    ) {
        if (selectedIndex !in songs.indices) {
            return
        }

        val items = songs.map { song ->
            TrackMediaItems.fromStored(
                song.mediaId, song.audioUri, song.title, song.artist
            )
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

    fun setSleepTimerMinutes(minutes: Int) =
        playbackController.setSleepTimerMinutes(minutes)

    fun setEqualizerEnabled(enabled: Boolean) =
        playbackController.setEqualizerEnabled(enabled)

    fun setEqualizerBand(index: Int, levelMb: Int) =
        playbackController.setEqualizerBand(index, levelMb)

    fun setCrossfadeDuration(durationMs: Int) =
        playbackController.setCrossfadeDuration(durationMs)

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
