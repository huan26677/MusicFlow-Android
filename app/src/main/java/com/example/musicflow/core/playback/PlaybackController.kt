package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log


import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
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
    val playbackErrorMessage: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentIndex: Int = 0,
    val queueCount: Int = 0,
    val canNext: Boolean = false,
    val canPrevious: Boolean = false,
    val artist: String = "Không rõ nghệ sĩ",
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val artworkUri: Uri? = null,
    val audioUri: Uri? = null,
    val playbackSpeed: Float = 1f,
    val sleepTimerRemainingMs: Long = 0L,
    val equalizer: EqualizerUiState = EqualizerUiState()
)

class PlaybackController(context: Context) {

    private val appContext = context.applicationContext

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture:
            ListenableFuture<MediaController>? = null

    private var controller: MediaController? = null

    private var sleepTimerRequestId = 0L
    private var lastSleepTimerQueryMs = 0L
    private var lastEqualizerQueryMs = 0L

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
                        requestSleepTimer(-1)
                        requestEqualizer(EqualizerContract.OP_GET)

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

        val errorMessage = player.playerError?.let { error ->
            PlaybackErrorText.fromCode(error.errorCode)
        }

        val status = when {
            errorMessage != null -> errorMessage

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

        val currentItem = player.currentMediaItem
        val mediaId = currentItem?.mediaId.orEmpty()

        val audioUri: Uri? = when {
            mediaId.startsWith("device-") -> {
                mediaId.removePrefix("device-")
                    .toLongOrNull()
                    ?.let { id ->
                        ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    }
            }

            mediaId.startsWith("content://") -> {
                Uri.parse(mediaId)
            }

            else -> currentItem?.localConfiguration?.uri
        }

        val artworkUri = audioUri?.takeIf {
            it.scheme == "content"
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
            playbackErrorMessage = errorMessage,
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

            repeatMode = player.repeatMode,
            artworkUri = artworkUri,
            audioUri = audioUri,
            playbackSpeed = player.playbackParameters.speed,
            sleepTimerRemainingMs =
                _uiState.value.sleepTimerRemainingMs,
            equalizer = _uiState.value.equalizer
        )
    }

    fun refreshProgress() {
        controller?.let { updateState(it) }

        val now = SystemClock.elapsedRealtime()

        if (now - lastSleepTimerQueryMs >= 1_000L) {
            lastSleepTimerQueryMs = now
            requestSleepTimer(-1)
        }
        if (now - lastEqualizerQueryMs >= 2_000L) {
            lastEqualizerQueryMs = now
            requestEqualizer(EqualizerContract.OP_GET)
        }
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
            when (player.playbackState) {

                Player.STATE_IDLE -> {
                    if (
                        player.mediaItemCount > 0 &&
                        player.isCommandAvailable(
                            Player.COMMAND_PREPARE
                        )
                    ) {
                        player.prepare()
                    } else {
                        return
                    }
                }

                Player.STATE_ENDED -> {
                    player.seekToDefaultPosition()
                }
            }

            player.play()
        }

        updateState(player)
    }

    fun next() {
        val player = controller ?: return

        Log.i(
            "MusicFlowSkip",
            "REQUEST index=${player.currentMediaItemIndex} " +
                "count=${player.mediaItemCount} " +
                "state=${player.playbackState} " +
                "error=${player.playerError?.errorCode ?: 0}"
        )

        val handled = PlaybackSkipRecovery.next(player)

        Log.i(
            "MusicFlowSkip",
            "RESULT handled=$handled " +
                "index=${player.currentMediaItemIndex} " +
                "state=${player.playbackState}"
        )

        updateState(player)
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

    fun setPlaybackSpeed(speed: Float) {
        if (speed !in 0.5f..2f) return

        val player = controller ?: return

        player.setPlaybackSpeed(speed)
        updateState(player)
    }

    private fun requestSleepTimer(minutes: Int) {
        val media = controller ?: return
        val requestId = ++sleepTimerRequestId

        val command = SessionCommand(
            SleepTimerContract.ACTION,
            Bundle.EMPTY
        )

        val args = Bundle().apply {
            putInt(SleepTimerContract.KEY_MINUTES, minutes)
        }

        val future = media.sendCustomCommand(
            command, args
        )

        future.addListener(
            {
                if (
                    controller === media &&
                    requestId == sleepTimerRequestId
                ) {
                    try {
                        val result = future.get()

                        if (result.resultCode ==
                            SessionResult.RESULT_SUCCESS
                        ) {
                            val remaining = result.extras.getLong(
                                SleepTimerContract.KEY_REMAINING_MS,
                                0L
                            )

                            _uiState.value = _uiState.value.copy(
                                sleepTimerRemainingMs = remaining
                            )
                        }
                    } catch (_: Exception) {
                        // Không làm gián đoạn phát nhạc.
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun setSleepTimerMinutes(minutes: Int) {
        if (minutes !in setOf(0, 15, 30, 45, 60)) {
            return
        }

        requestSleepTimer(minutes)
    }

    private var equalizerRequestId = 0L

    private fun requestEqualizer(
        operation: Int,
        enabled: Boolean = false,
        index: Int = -1,
        levelMb: Int = 0
    ) {
        val media = controller ?: return
        val requestId = ++equalizerRequestId
        val args = Bundle().apply {
            putInt(EqualizerContract.KEY_OPERATION, operation)
            putBoolean(EqualizerContract.KEY_ENABLED, enabled)
            putInt(EqualizerContract.KEY_BAND_INDEX, index)
            putInt(EqualizerContract.KEY_LEVEL_MB, levelMb)
        }
        val future = media.sendCustomCommand(
            SessionCommand(EqualizerContract.ACTION, Bundle.EMPTY), args
        )
        future.addListener({
            if (controller === media && requestId == equalizerRequestId) {
                try {
                    val result = future.get()
                    _uiState.value = _uiState.value.copy(
                        equalizer = EqualizerUiState.fromBundle(result.extras)
                    )
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(
                        equalizer = EqualizerUiState()
                    )
                }
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun setEqualizerEnabled(enabled: Boolean) =
        requestEqualizer(EqualizerContract.OP_ENABLE, enabled = enabled)

    fun setEqualizerBand(index: Int, levelMb: Int) =
        requestEqualizer(
            EqualizerContract.OP_SET_BAND,
            index = index,
            levelMb = levelMb
        )

    fun release() {
        equalizerRequestId++
        sleepTimerRequestId++

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
