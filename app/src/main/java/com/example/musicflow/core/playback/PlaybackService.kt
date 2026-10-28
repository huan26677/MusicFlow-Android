package com.example.musicflow.core.playback

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

import com.example.musicflow.core.history.RecentPlaybackTracker
import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    private var runtimeAutoSkip: PlaybackRuntimeAutoSkip? = null

    private val artworkScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var artworkJob: Job? = null
    private var artworkRequestId = 0L

    private val recentPlaybackTracker = RecentPlaybackTracker()

    private val historyScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val historyWriteMutex = Mutex()

    private val historyRepository by lazy {
        RoomRecentHistoryRepository(
            RecentHistoryDatabase.getInstance(
                applicationContext
            )
        )
    }

    private val queueStore by lazy {
        PlaybackQueueStore(applicationContext)
    }

    private val queueScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private val playerListener = object : Player.Listener {

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            // Giữ nguyên xử lý ảnh bìa đang ổn định.
            updateCurrentArtwork()
        }

    override fun onPlayerError(
        error: PlaybackException
    ) {
        Log.i(
            "MusicFlowRuntimeV2",
            "SERVICE_PLAYER_ERROR code=${error.errorCode}"
        )

        runtimeAutoSkip?.let { recovery ->
            recovery.onPlayerError(error)

            Log.i(
                "MusicFlowRuntimeV2",
                "SERVICE_FORWARDED_ERROR code=${error.errorCode}"
            )
        }
    }

    override fun onEvents(
            player: Player,
            events: Player.Events
        ) {
            if (
                events.contains(Player.EVENT_IS_PLAYING_CHANGED) ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
            ) {
                maybeRecordRecentlyPlayed(player)
            }

            if (
                events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) ||
                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED) ||
                events.contains(Player.EVENT_REPEAT_MODE_CHANGED)
            ) {
                queueStore.saveFromPlayer(player)
            }
        }
    }

    private val sleepTimerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    private var sleepTimerJob: Job? = null
    private var sleepDeadlineElapsedMs = 0L

    private fun sleepRemainingMs(): Long =
        if (sleepDeadlineElapsedMs == 0L) {
            0L
        } else {
            (sleepDeadlineElapsedMs -
                SystemClock.elapsedRealtime())
                .coerceAtLeast(0L)
        }

    private fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepDeadlineElapsedMs = 0L

        if (minutes == 0) return

        val durationMs = minutes.toLong() * 60_000L

        sleepDeadlineElapsedMs =
            SystemClock.elapsedRealtime() + durationMs

        sleepTimerJob = sleepTimerScope.launch {
            delay(durationMs)

            sleepDeadlineElapsedMs = 0L
            sleepTimerJob = null

            // Tạm dừng nhưng giữ nguyên queue.
            mediaSession?.player?.pause()

            Log.i("MusicFlowSleepTimer", "TIMER_FINISHED")
        }
    }

    private val sleepTimerCallback =
        object : MediaSession.Callback {

            override fun onConnectAsync(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.ConnectionResult> {

                if (controller.packageName != packageName) {
                    return super.onConnectAsync(
                        session, controller
                    )
                }

                val commands =
                    MediaSession.ConnectionResult
                        .DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(
                            SessionCommand(
                                SleepTimerContract.ACTION,
                                Bundle.EMPTY
                            )
                        )
                        .build()

                val result =
                    MediaSession.ConnectionResult
                        .AcceptedResultBuilder(
                            session, controller
                        )
                        .setAvailableSessionCommands(commands)
                        .build()

                return Futures.immediateFuture(result)
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {

                if (customCommand.customAction !=
                    SleepTimerContract.ACTION
                ) {
                    return super.onCustomCommand(
                        session, controller,
                        customCommand, args
                    )
                }

                val minutes = args.getInt(
                    SleepTimerContract.KEY_MINUTES,
                    -2
                )

                if (minutes !in
                    setOf(-1, 0, 15, 30, 45, 60)
                ) {
                    return Futures.immediateFuture(
                        SessionResult(
                            SessionResult.RESULT_ERROR_BAD_VALUE
                        )
                    )
                }

                // -1: hỏi trạng thái
                //  0: hủy timer
                // >0: đặt timer mới
                if (minutes >= 0) {
                    setSleepTimer(minutes)
                }

                val extras = Bundle().apply {
                    putLong(
                        SleepTimerContract.KEY_REMAINING_MS,
                        sleepRemainingMs()
                    )
                }

                return Futures.immediateFuture(
                    SessionResult(
                        SessionResult.RESULT_SUCCESS,
                        extras
                    )
                )
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

        val restored = queueStore.restoreInto(player)

        val mainHandler = Handler(
            Looper.getMainLooper()
        )

        runtimeAutoSkip = PlaybackRuntimeAutoSkip(
            player
        ) { action ->
            mainHandler.post(action)
        }

        mediaSession = MediaSession.Builder(
            this,
            player
        )
            .setCallback(sleepTimerCallback)
            .build()

        player.addListener(playerListener)

        if (restored) {
            queueScope.launch {
                val removed = PlaybackQueueRecovery(
                    applicationContext,
                    queueStore
                ).pruneUnavailable(player)

                Log.i(
                    "MusicFlowQueueRecovery",
                    "CHECK_FINISHED removed=$removed"
                )
            }
        }

        queueScope.launch {
            while (isActive) {
                delay(5_000)

                if (player.isPlaying) {
                    queueStore.saveFromPlayer(player)
                }
            }
        }

        Log.i(
            "MusicFlowQueue",
            "SERVICE_CREATED restored=$restored"
        )
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

    private fun maybeRecordRecentlyPlayed(player: Player) {
        if (!player.isPlaying) return

        val item = player.currentMediaItem ?: return

        val audioUri = item.localConfiguration
            ?.uri
            ?.toString()
            ?: return

        val mediaId = item.mediaId

        if (
            !recentPlaybackTracker.shouldRecord(
                isPlaying = player.isPlaying,
                mediaId = mediaId,
                audioUri = audioUri
            )
        ) {
            return
        }

        val title = item.mediaMetadata.title
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: "Bài hát không tên"

        val artist = item.mediaMetadata.artist
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: "Không rõ nghệ sĩ"

        val recentSong = RecentSong(
            mediaId = mediaId,
            audioUri = audioUri,
            title = title,
            artist = artist,
            playedAtMs = System.currentTimeMillis()
        )

        // Không thực hiện truy vấn SQLite trên Main thread.
        historyScope.launch {
            historyWriteMutex.withLock {
                try {
                    withContext(Dispatchers.IO) {
                        historyRepository.record(recentSong)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Lỗi lưu lịch sử không được làm dừng nhạc.
                    Log.w(
                        "MusicFlowHistory",
                        "Không thể lưu bài nghe gần đây",
                        e
                    )
                }
            }
        }
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        runtimeAutoSkip?.dispose()
        runtimeAutoSkip = null

        sleepTimerJob?.cancel()
        sleepTimerScope.cancel()
        sleepDeadlineElapsedMs = 0L

        artworkRequestId++
        artworkJob?.cancel()
        artworkScope.cancel()
        historyScope.cancel()
        queueScope.cancel()

        mediaSession?.let { session ->
            session.player.removeListener(playerListener)

            queueStore.saveFromPlayer(session.player)

            session.player.release()
            session.release()
        }

        mediaSession = null
        super.onDestroy()
    }
}