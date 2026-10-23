
package com.example.musicflow

import android.content.ComponentName
import android.content.Context

import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val connected: Boolean = false,
    val hasMedia: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "Chưa chọn bài hát",
    val status: String = "Đang kết nối bộ phát...",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentIndex: Int = 0,
    val queueCount: Int = 0,
    val canNext: Boolean = false,
    val canPrevious: Boolean = false,
    val artist: String = "Không rõ nghệ sĩ",
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

class PlaybackController(context: Context) {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture:
            ListenableFuture<MediaController>? = null

    private var controller: MediaController? = null

    private data class QueueRequest(
        val items: List<MediaItem>,
        val startIndex: Int
    )

    private var pendingQueue: QueueRequest? = null

    private val listener = object : Player.Listener {
        override fun onEvents(
            player: Player,
            events: Player.Events
        ) {
            updateState(player)
        }
    }

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(
            appContext,
            ComponentName(
                appContext,
                PlaybackService::class.java
            )
        )

        val future = MediaController.Builder(
            appContext,
            token
        ).buildAsync()

        controllerFuture = future

        future.addListener(
            {
                if (controllerFuture === future) {
                    try {
                        val newController = future.get()

                        controller = newController
                        newController.addListener(listener)
                        updateState(newController)

                        pendingQueue?.let { request ->
                            pendingQueue = null
                            startQueue(
                                newController,
                                request.items,
                                request.startIndex
                            )
                        }

                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            connected = false,
                            status = "Không thể kết nối bộ phát: " +
                                    e.javaClass.simpleName
                        )
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun updateState(player: Player) {
        val duration = player.duration.let { value ->
            if (value > 0L && value != C.TIME_UNSET) {
                value
            } else {
                0L
            }
        }

        val hasMedia = player.mediaItemCount > 0

        val status = when {
            player.playerError != null ->
                "Lỗi phát nhạc: " +
                        player.playerError?.errorCodeName

            player.playbackState == Player.STATE_BUFFERING ->
                "Đang tải bài hát..."

            player.playbackState == Player.STATE_ENDED ->
                "Đã phát hết hàng đợi"

            player.isPlaying ->
                "Đang phát"

            player.playbackState == Player.STATE_READY ->
                "Đã tạm dừng"

            else ->
                "Bộ phát sẵn sàng"
        }

        _uiState.value = PlayerUiState(
            connected = true,
            hasMedia = hasMedia,
            isPlaying = player.isPlaying,
            title = player.currentMediaItem
                ?.mediaMetadata
                ?.title
                ?.toString()
                ?: "Chưa chọn bài hát",
            status = status,
            positionMs = player.currentPosition
                .coerceAtLeast(0L),
            durationMs = duration,
            currentIndex = if (hasMedia) {
                player.currentMediaItemIndex + 1
            } else {
                0
            },
            queueCount = player.mediaItemCount,
            canNext = player.hasNextMediaItem(),
            canPrevious = player.hasPreviousMediaItem(),

            artist = player.currentMediaItem
                ?.mediaMetadata
                ?.artist
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: "Không rõ nghệ sĩ",

            shuffleEnabled = player.shuffleModeEnabled,

            repeatMode = player.repeatMode
        )
    }

    fun refreshProgress() {
        controller?.let { updateState(it) }
    }

    fun playQueue(
        items: List<MediaItem>,
        startIndex: Int = 0
    ) {
        if (items.isEmpty()) return

        val safeIndex = startIndex.coerceIn(
            0,
            items.lastIndex
        )

        val player = controller

        if (player == null) {
            pendingQueue = QueueRequest(items, safeIndex)

            _uiState.value = _uiState.value.copy(
                status = "Đang chờ kết nối để phát nhạc..."
            )
            return
        }

        startQueue(player, items, safeIndex)
    }

    private fun startQueue(
        player: MediaController,
        items: List<MediaItem>,
        startIndex: Int
    ) {
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        player.play()
        updateState(player)
    }

    fun togglePlayPause() {
        val player = controller ?: return

        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekToDefaultPosition()
            }
            player.play()
        }

        updateState(player)
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return

        val duration = player.duration

        val target = if (
            duration > 0L && duration != C.TIME_UNSET
        ) {
            positionMs.coerceIn(0L, duration)
        } else {
            positionMs.coerceAtLeast(0L)
        }

        player.seekTo(target)
        updateState(player)
    }


    fun toggleShuffle() {
        val player = controller ?: return

        if (!player.isCommandAvailable(
                Player.COMMAND_SET_SHUFFLE_MODE
            )
        ) return

        player.setShuffleModeEnabled(
            !player.shuffleModeEnabled
        )

        updateState(player)
    }

    fun cycleRepeat() {
        val player = controller ?: return

        if (!player.isCommandAvailable(
                Player.COMMAND_SET_REPEAT_MODE
            )
        ) return

        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF ->
                Player.REPEAT_MODE_ALL

            Player.REPEAT_MODE_ALL ->
                Player.REPEAT_MODE_ONE

            else ->
                Player.REPEAT_MODE_OFF
        }

        player.setRepeatMode(nextMode)

        updateState(player)
    }

    fun release() {
        controller?.removeListener(listener)
        controller = null
        pendingQueue = null

        val future = controllerFuture
        controllerFuture = null

        if (future != null) {
            MediaController.releaseFuture(future)
        }
    }
}
