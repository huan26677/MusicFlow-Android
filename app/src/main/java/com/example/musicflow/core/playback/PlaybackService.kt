package com.example.musicflow.core.playback

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    private val artworkScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var artworkJob: Job? = null
    private var artworkRequestId = 0L

    private val playerListener = object : Player.Listener {

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            updateCurrentArtwork()
        }
    }

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.DEFAULT,
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(
            this,
            player
        ).build()

        player.addListener(playerListener)
    }

    private fun updateCurrentArtwork() {
        val player = mediaSession?.player ?: return

        // Vô hiệu hóa tác vụ thuộc bài trước.
        artworkRequestId++
        artworkJob?.cancel()

        val item = player.currentMediaItem ?: return

        // Nếu đã gắn ảnh vào MediaItem thì không đọc lại.
        if (
            item.mediaMetadata.artworkData != null ||
            item.mediaMetadata.artworkUri != null
        ) {
            return
        }

        val audioUri: Uri =
            item.localConfiguration?.uri ?: return

        val originalIndex =
            player.currentMediaItemIndex

        val requestId = artworkRequestId

        artworkJob = artworkScope.launch {

            val artworkBytes = withContext(
                Dispatchers.IO
            ) {
                SessionArtworkReader.read(
                    this@PlaybackService,
                    audioUri
                )
            } ?: return@launch

            // Không sử dụng ảnh nếu đã chuyển bài.
            if (requestId != artworkRequestId) {
                return@launch
            }

            val currentItem =
                player.currentMediaItem ?: return@launch

            if (
                player.currentMediaItemIndex != originalIndex ||
                currentItem.mediaId != item.mediaId ||
                currentItem.localConfiguration?.uri != audioUri
            ) {
                return@launch
            }

            // Tránh cập nhật trùng lặp.
            if (
                currentItem.mediaMetadata.artworkData != null ||
                currentItem.mediaMetadata.artworkUri != null
            ) {
                return@launch
            }

            if (!player.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS
                )
            ) {
                return@launch
            }

            val updatedMetadata =
                currentItem.mediaMetadata
                    .buildUpon()
                    .setArtworkData(
                        artworkBytes,
                        MediaMetadata.PICTURE_TYPE_FRONT_COVER
                    )
                    .build()

            val updatedItem = currentItem
                .buildUpon()
                .setMediaMetadata(updatedMetadata)
                .build()

            // Giữ nguyên URI âm thanh và vị trí hiện tại.
            player.replaceMediaItem(
                originalIndex,
                updatedItem
            )
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        artworkRequestId++
        artworkJob?.cancel()
        artworkScope.cancel()

        mediaSession?.let { session ->
            session.player.removeListener(playerListener)
            session.player.release()
            session.release()
        }

        mediaSession = null
        super.onDestroy()
    }
}