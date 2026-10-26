package com.example.musicflow.core.playback

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline

/**
 * Tự bỏ qua lỗi thiếu file / thiếu quyền đọc, không quay về đầu queue.
 *
 * Chỉ truy cập class này trên application looper của Player. Callback phục hồi
 * được post sang lượt xử lý sau để Media3 kết thúc onPlayerError trước.
 *
 * Race hardening v3: generation thay đổi tại thời điểm nhận từng sự kiện
 * Pause/Play, thay playlist hay Seek. So sánh snapshot cuối cùng không đủ
 * khi thao tác mới đưa Player trở lại trạng thái/queue giống ban đầu.
 */
class PlaybackRuntimeAutoSkip(
    private val player: Player,
    private val post: (() -> Unit) -> Unit
) {
    private var recoveryPending = false
    private var disposed = false
    private var generation = 0L

    private val intentListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean,
            reason: Int
        ) {
            generation++
        }

        override fun onTimelineChanged(
            timeline: Timeline,
            reason: Int
        ) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                generation++
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                generation++
            }
        }
    }

    init {
        player.addListener(intentListener)
    }

    private fun queueIdentity(): List<Pair<String, String?>> {
        return (0 until player.mediaItemCount).map { index ->
            val item = player.getMediaItemAt(index)
            item.mediaId to item.localConfiguration?.uri?.toString()
        }
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        generation++
        recoveryPending = false
        player.removeListener(intentListener)
    }

    fun onPlayerError(error: PlaybackException) {
        if (disposed) return

        val recoverable =
            error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION

        if (!recoverable) {
            Log.w(
                "MusicFlowAutoSkip",
                "NOT_HANDLED code=${error.errorCodeName}"
            )
            return
        }

        if (!player.playWhenReady) return
        val failedIndex = player.currentMediaItemIndex
        if (failedIndex !in 0 until player.mediaItemCount || recoveryPending) {
            return
        }

        val failedId = player.currentMediaItem?.mediaId
        val originalQueue = queueIdentity()
        val errorGeneration = generation
        recoveryPending = true

        post {
            if (disposed) return@post
            recoveryPending = false

            if (
                generation != errorGeneration ||
                !player.playWhenReady ||
                player.currentMediaItemIndex != failedIndex ||
                player.currentMediaItem?.mediaId != failedId ||
                queueIdentity() != originalQueue
            ) {
                Log.i("MusicFlowAutoSkip", "RECOVERY_CANCELLED")
                return@post
            }

            val nextIndex = failedIndex + 1
            if (nextIndex >= player.mediaItemCount) {
                player.pause()
                player.stop()
                Log.i(
                    "MusicFlowAutoSkip",
                    "QUEUE_EXHAUSTED index=$failedIndex code=${error.errorCodeName}"
                )
                return@post
            }

            player.seekTo(nextIndex, 0L)
            player.prepare()
            player.play()
            Log.i(
                "MusicFlowAutoSkip",
                "SKIPPED_ERROR from=$failedIndex to=$nextIndex " +
                    "code=${error.errorCodeName}"
            )
        }
    }
}
