package com.example.musicflow.core.playback

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/** Real debug MediaSession + real WAV files; no online catalog or credentials. */
@RunWith(AndroidJUnit4::class)
class SleepTimerRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private data class OriginalQueue(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long,
        val playWhenReady: Boolean,
        val shuffle: Boolean,
        val repeat: Int
    )

    private fun command(
        controller: MediaController,
        minutes: Int,
        debugMs: Long? = null
    ): SessionResult {
        val args = Bundle().apply {
            putInt(SleepTimerContract.KEY_MINUTES, minutes)
            if (debugMs != null) {
                putLong(SleepTimerContract.KEY_DEBUG_DURATION_MS, debugMs)
            }
        }
        var future: ListenableFuture<SessionResult>? = null
        main {
            future = controller.sendCustomCommand(
                SessionCommand(SleepTimerContract.ACTION, Bundle.EMPTY), args
            )
        }
        return checkNotNull(future).get(10, TimeUnit.SECONDS)
    }

    private fun remaining(controller: MediaController): Long {
        val response = command(controller, -1)
        assertEquals(SessionResult.RESULT_SUCCESS, response.resultCode)
        return response.extras.getLong(SleepTimerContract.KEY_REMAINING_MS, -1)
    }

    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun awaitPlaying(
        controller: MediaController,
        expected: Boolean,
        timeoutMs: Long = 10_000L
    ) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            var isPlaying = false
            main { isPlaying = controller.isPlaying }
            if (isPlaying == expected) return
            SystemClock.sleep(100L)
        }
        throw AssertionError("MediaSession isPlaying did not become $expected")
    }

    private inline fun withController(test: (MediaController) -> Unit) {
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null
        var snapshot: OriginalQueue? = null
        var changedTimer = false
        try {
            future = MediaController.Builder(
                context,
                SessionToken(context, ComponentName(
                    context, PlaybackService::class.java
                ))
            ).buildAsync()
            controller = future.get(20, TimeUnit.SECONDS)
            val media = controller
            main {
                snapshot = OriginalQueue(
                    items = (0 until media.mediaItemCount)
                        .map(media::getMediaItemAt),
                    index = media.currentMediaItemIndex,
                    positionMs = media.currentPosition.coerceAtLeast(0L),
                    playWhenReady = media.playWhenReady,
                    shuffle = media.shuffleModeEnabled,
                    repeat = media.repeatMode
                )
            }
            // These tests are for a dedicated QA emulator; never disturb an
            // already-running user timer which cannot be restored exactly.
            assertEquals("A pre-existing Sleep Timer is active", 0L,
                remaining(media))
            changedTimer = true
            test(media)
        } finally {
            controller?.let { media ->
                if (changedTimer) {
                    runCatching { command(media, 0) }
                }
                snapshot?.let { original ->
                    main {
                        media.pause()
                        if (original.items.isEmpty()) {
                            media.clearMediaItems()
                        } else {
                            media.setMediaItems(
                                original.items,
                                original.index.coerceIn(0, original.items.lastIndex),
                                original.positionMs
                            )
                            media.prepare()
                        }
                        media.shuffleModeEnabled = original.shuffle
                        media.repeatMode = original.repeat
                        if (original.playWhenReady && original.items.isNotEmpty()) {
                            media.play()
                        }
                    }
                }
            }
            future?.let { toRelease ->
                main { MediaController.releaseFuture(toRelease) }
            }
        }
    }

    private fun wav(name: String): File {
        val rate = 8000
        val count = rate * 30
        val buffer = ByteBuffer.allocate(44 + count * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun ascii(s: String) { buffer.put(s.toByteArray(Charsets.US_ASCII)) }
        ascii("RIFF")
        buffer.putInt(36 + count * 2)
        ascii("WAVE")
        ascii("fmt ")
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putInt(rate)
        buffer.putInt(rate * 2)
        buffer.putShort(2.toShort())
        buffer.putShort(16.toShort())
        ascii("data")
        buffer.putInt(count * 2)
        return File.createTempFile(name, ".wav", context.cacheDir).apply {
            writeBytes(buffer.array())
        }
    }

    @Test
    fun validatesSetQueryReplaceAndCancel() = withController { media ->
        val badMinutes = command(media, 5)
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, badMinutes.resultCode)
        assertEquals(0L, remaining(media))
        val badDebug = command(media, 15, 50)
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, badDebug.resultCode)
        assertEquals(0L, remaining(media))
        Log.i("MusicFlowSleepQA", "PASS invalidValuesRejected")

        val set = command(media, 15)
        assertEquals(SessionResult.RESULT_SUCCESS, set.resultCode)
        assertTrue(set.extras.getLong(SleepTimerContract.KEY_REMAINING_MS) in
            1..900_000L)
        assertTrue(remaining(media) in 1..900_000L)
        Log.i("MusicFlowSleepQA", "PASS timerSetAndQueried")

        val replaced = command(media, 30)
        assertEquals(SessionResult.RESULT_SUCCESS, replaced.resultCode)
        assertTrue(remaining(media) in 900_001..1_800_000L)
        Log.i("MusicFlowSleepQA", "PASS timerReplaced")

        assertEquals(SessionResult.RESULT_SUCCESS, command(media, 0).resultCode)
        assertEquals(0L, remaining(media))
        SystemClock.sleep(350L)
        assertEquals(0L, remaining(media))
        Log.i("MusicFlowSleepQA", "PASS timerCancelled")
    }

    @Test
    fun expiryPausesRealMediaSessionWithoutClearingOfflineQueue() {
        val first = wav("musicflow_sleep_first_")
        val second = wav("musicflow_sleep_second_")
        try {
            withController { media ->
                val ids = listOf("sleep-qa-1", "sleep-qa-2")
                main {
                    media.shuffleModeEnabled = false
                    media.repeatMode = Player.REPEAT_MODE_OFF
                    media.setMediaItems(
                        listOf(first, second).mapIndexed { index, file ->
                            MediaItem.Builder()
                                .setMediaId(ids[index])
                                .setUri(Uri.fromFile(file))
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle("Sleep Timer QA ${index + 1}")
                                        .build()
                                ).build()
                        }, 0, 0L
                    )
                    media.prepare()
                    media.play()
                }
                awaitPlaying(media, true)
                Log.i("MusicFlowSleepQA", "PASS offlinePlaybackStarted")

                // Same production timer and callback, with a DEBUG-only
                // duration override; release accepts only 15/30/45/60 min.
                val short = command(media, 15, 2_000L)
                assertEquals(SessionResult.RESULT_SUCCESS, short.resultCode)
                assertTrue(remaining(media) in 1..2_000L)
                awaitPlaying(media, false, timeoutMs = 10_000L)
                assertEquals(0L, remaining(media))
                main {
                    assertEquals(2, media.mediaItemCount)
                    assertEquals(0, media.currentMediaItemIndex)
                    assertEquals(ids[0], media.currentMediaItem?.mediaId)
                    assertFalse(media.playWhenReady)
                    assertTrue(media.currentPosition >= 0L)
                }
                Log.i("MusicFlowSleepQA", "PASS expiryPausesAndPreservesQueue")
            }
        } finally {
            first.delete()
            second.delete()
        }
    }

    @Test
    fun cancellingActiveShortTimerDoesNotPauseOfflineMusic() {
        val audio = wav("musicflow_sleep_cancel_")
        try {
            withController { media ->
                main {
                    media.setMediaItem(
                        MediaItem.Builder()
                            .setMediaId("sleep-qa-cancel")
                            .setUri(Uri.fromFile(audio))
                            .build()
                    )
                    media.prepare()
                    media.play()
                }
                awaitPlaying(media, true)
                assertEquals(
                    SessionResult.RESULT_SUCCESS,
                    command(media, 15, 1_000L).resultCode
                )
                assertTrue(remaining(media) > 0L)
                assertEquals(
                    SessionResult.RESULT_SUCCESS,
                    command(media, 0).resultCode
                )
                assertEquals(0L, remaining(media))
                // Wait beyond the original deadline to verify the cancelled
                // coroutine cannot pause an active MediaSession.
                SystemClock.sleep(1_500L)
                var stillPlaying = false
                main {
                    stillPlaying = media.isPlaying
                    assertEquals("sleep-qa-cancel", media.currentMediaItem?.mediaId)
                }
                assertTrue("Cancelled timer paused music", stillPlaying)
                Log.i("MusicFlowSleepQA", "PASS cancelPreventsExpiryPause")
            }
        } finally {
            audio.delete()
        }
    }
}
