package com.example.musicflow.core.playback

import android.util.Log

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

/**
 * Chỉ tự bỏ qua lỗi thiếu file / thiếu quyền đọc.
 *
 * Luôn đi tới chỉ số lớn hơn trong queue để tránh
 * vòng lặp vô hạn, kể cả khi Repeat đang bật.
 *
 * Mọi thao tác với Player phải chạy trên
 * application looper của Player (main thread
 * trong PlaybackService).
 */
class PlaybackRuntimeAutoSkip(
    private val player: Player,
    private val post: (() -> Unit) -> Unit
) {
    private var recoveryPending = false
    private var disposed = false

    fun dispose() {
        disposed = true
        recoveryPending = false
    }

    fun onPlayerError(error: PlaybackException) {
        if (disposed) return

        val recoverable =
            error.errorCode ==
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
            error.errorCode ==
                PlaybackException.ERROR_CODE_IO_NO_PERMISSION

        if (!recoverable) {
            Log.w(
                "MusicFlowAutoSkip",
                "NOT_HANDLED code=${error.errorCodeName}"
            )
            return
        }

        // Người dùng đã tạm dừng thì không tự phát lại.
        if (!player.playWhenReady) return

        val failedIndex = player.currentMediaItemIndex

        if (
            failedIndex !in 0 until player.mediaItemCount ||
            recoveryPending
        ) {
            return
        }

        val failedId = player.currentMediaItem?.mediaId

        recoveryPending = true

        // Media3 chuyển sang STATE_IDLE ngay sau
        // callback onPlayerError. Thực hiện hồi phục
        // sau khi callback hiện tại kết thúc.
        post {
            if (disposed) return@post

            recoveryPending = false

            if (
                !player.playWhenReady ||
                player.currentMediaItemIndex != failedIndex ||
                player.currentMediaItem?.mediaId != failedId
            ) {
                Log.i(
                    "MusicFlowAutoSkip",
                    "RECOVERY_CANCELLED"
                )
                return@post
            }

            val nextIndex = failedIndex + 1

            if (nextIndex >= player.mediaItemCount) {
                // Không quay về đầu queue dưới Repeat ALL.
                player.pause()
                player.stop()

                Log.i(
                    "MusicFlowAutoSkip",
                    "QUEUE_EXHAUSTED index=$failedIndex " +
                        "code=${error.errorCodeName}"
                )
                return@post
            }

            player.seekTo(nextIndex, 0L)
            player.prepare()
            player.play()

            Log.i(
                "MusicFlowAutoSkip",
                "SKIPPED_ERROR from=$failedIndex " +
                    "to=$nextIndex " +
                    "code=${error.errorCodeName}"
            )
        }
    }
}
