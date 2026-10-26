package com.example.musicflow.core.playback

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mỗi test chạy trên main looper, hoãn callback được post để mô phỏng lệnh
 * người dùng đến giữa onPlayerError và thời điểm phục hồi.
 * Đây là test tiêm lỗi có kiểm soát, KHÔNG giả làm Service end-to-end.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackRuntimeRaceHardeningTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun runRace(
        marker: String,
        expectedIndex: Int,
        actionBeforeRecovery: (
            ExoPlayer,
            List<MediaItem>,
            PlaybackRuntimeAutoSkip
        ) -> Unit
    ) {
        instrumentation.runOnMainSync {
            val player = ExoPlayer.Builder(context).build()
            var recovery: PlaybackRuntimeAutoSkip? = null
            try {
                val items = listOf("first", "second").map { id ->
                    MediaItem.Builder()
                        .setMediaId(id)
                        .setUri(Uri.parse("file:///qa_v3_$id.wav"))
                        .build()
                }
                player.setMediaItems(items, 0, 0L)
                // Không prepare: không phát sinh lỗi nền làm test race thiếu ổn định.
                player.play()
                assertTrue(player.playWhenReady)

                var postedAction: (() -> Unit)? = null
                recovery = PlaybackRuntimeAutoSkip(player) { posted ->
                    postedAction = posted
                }
                recovery.onPlayerError(
                    PlaybackException(
                        "v3 deterministic injected error",
                        null,
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
                    )
                )
                assertNotNull("Callback phục hồi chưa được post", postedAction)

                actionBeforeRecovery(player, items, checkNotNull(recovery))
                checkNotNull(postedAction).invoke()

                assertEquals(expectedIndex, player.currentMediaItemIndex)
                assertEquals(
                    if (expectedIndex == 0) "first" else "second",
                    player.currentMediaItem?.mediaId
                )
                assertEquals(2, player.mediaItemCount)
            } finally {
                recovery?.dispose()
                player.release()
            }
        }
        // Chỉ ghi marker sau khi mọi assertion đã qua.
        Log.i("MusicFlowRaceV3", "PASS $marker")
    }

    @Test
    fun pauseThenResumeInvalidatesPendingRecovery() {
        runRace("pauseResumeCancelsStaleRecovery", 0) { player, _, _ ->
            player.pause()
            assertFalse(player.playWhenReady)
            player.play()
            assertTrue(player.playWhenReady)
        }
    }

    @Test
    fun identicalQueueReplacementInvalidatesPendingRecovery() {
        runRace("identicalQueueCancelsStaleRecovery", 0) { player, items, _ ->
            // Cùng mediaId và URI, nhưng đây là một lệnh thay queue MỚI.
            player.setMediaItems(items, 0, 0L)
            assertTrue(player.playWhenReady)
        }
    }

    @Test
    fun seekWithinSameIndexInvalidatesPendingRecovery() {
        runRace("sameIndexSeekCancelsStaleRecovery", 0) { player, _, _ ->
            player.seekTo(0, 250L)
            assertEquals(0, player.currentMediaItemIndex)
        }
    }

    @Test
    fun disposeInvalidatesPendingRecovery() {
        runRace("disposeCancelsStaleRecovery", 0) { _, _, recovery ->
            recovery.dispose()
        }
    }

    @Test
    fun noNewIntentStillSkipsNextTrack() {
        runRace("uninterruptedRecoveryStillSkips", 1) { _, _, _ ->
            // Không gửi lệnh nào: callback cũ vẫn phải phục hồi bình thường.
        }
    }
}
