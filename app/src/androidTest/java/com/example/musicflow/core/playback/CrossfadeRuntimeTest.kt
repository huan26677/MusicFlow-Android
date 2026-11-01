package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Context
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Real Media3 playback, actual WAV decoding, and production transition command. */
@RunWith(AndroidJUnit4::class)
class CrossfadeRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private data class Snapshot(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long,
        val playWhenReady: Boolean,
        val speed: Float,
        val repeat: Int,
        val shuffle: Boolean,
        val durationMs: Int
    )

    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun command(media: MediaController, op: Int, duration: Int = 0): SessionResult {
        val args = Bundle().apply {
            putInt(TransitionContract.KEY_OPERATION, op)
            putInt(TransitionContract.KEY_DURATION_MS, duration)
        }
        var request: ListenableFuture<SessionResult>? = null
        main {
            request = media.sendCustomCommand(
                SessionCommand(TransitionContract.ACTION, Bundle.EMPTY), args
            )
        }
        return checkNotNull(request).get(10, TimeUnit.SECONDS)
    }

    private fun duration(media: MediaController): Int {
        val result = command(media, TransitionContract.OP_GET)
        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        return result.extras.getInt(TransitionContract.KEY_DURATION_MS, -1)
    }

    private fun setDuration(media: MediaController, value: Int) {
        val result = command(media, TransitionContract.OP_SET, value)
        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        assertEquals(value, result.extras.getInt(TransitionContract.KEY_DURATION_MS, -1))
        assertEquals(value, duration(media))
    }

    private fun wav(name: String): File {
        val samples = 8000 * 12
        val bytes = ByteBuffer.allocate(44 + samples * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun text(s: String) { bytes.put(s.toByteArray(Charsets.US_ASCII)) }
        text("RIFF")
        bytes.putInt(36 + samples * 2)
        text("WAVE")
        text("fmt ")
        bytes.putInt(16)
        bytes.putShort(1.toShort())
        bytes.putShort(1.toShort())
        bytes.putInt(8000)
        bytes.putInt(16000)
        bytes.putShort(2.toShort())
        bytes.putShort(16.toShort())
        text("data")
        bytes.putInt(samples * 2)
        return File.createTempFile(name, ".wav", context.cacheDir).apply {
            writeBytes(bytes.array())
        }
    }

    private fun await(
        description: String,
        timeoutMs: Long = 13_000L,
        condition: () -> Boolean
    ) {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            var ready = false
            main { ready = condition() }
            if (ready) return
            SystemClock.sleep(100L)
        }
        throw AssertionError("Timed out: $description")
    }

    private fun resetToFirst(media: MediaController, speed: Float = 1f) {
        main {
            media.pause()
            media.setPlaybackSpeed(speed)
            media.shuffleModeEnabled = false
            media.repeatMode = Player.REPEAT_MODE_OFF
            media.seekTo(0, 0L)
            media.prepare()
            media.play()
        }
        await("first local WAV playing") {
            media.isPlaying && media.currentMediaItem?.mediaId == "crossfade-alpha" &&
                media.currentMediaItemIndex == 0 && media.mediaItemCount == 2
        }
    }

    private fun awaitSecond(media: MediaController, message: String) {
        await(message) {
            media.isPlaying && media.mediaItemCount == 2 &&
                media.currentMediaItemIndex == 1 &&
                media.currentMediaItem?.mediaId == "crossfade-beta"
        }
        main {
            assertEquals("crossfade-alpha", media.getMediaItemAt(0).mediaId)
            assertEquals("crossfade-beta", media.getMediaItemAt(1).mediaId)
            assertEquals(Player.REPEAT_MODE_OFF, media.repeatMode)
            assertTrue(!media.shuffleModeEnabled)
        }
    }

    private fun withLocalQueue(run: (MediaController) -> Unit) {
        val first = wav("musicflow_crossfade_a_")
        val second = wav("musicflow_crossfade_b_")
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(
                context, PlaybackService::class.java
            ))
        ).buildAsync()
        var media: MediaController? = null
        var previous: Snapshot? = null
        try {
            media = future.get(20, TimeUnit.SECONDS)
            val controller = media
            val originalDuration = duration(controller)
            main {
                previous = Snapshot(
                    items = (0 until controller.mediaItemCount)
                        .map(controller::getMediaItemAt),
                    index = controller.currentMediaItemIndex,
                    positionMs = controller.currentPosition.coerceAtLeast(0L),
                    playWhenReady = controller.playWhenReady,
                    speed = controller.playbackParameters.speed,
                    repeat = controller.repeatMode,
                    shuffle = controller.shuffleModeEnabled,
                    durationMs = originalDuration
                )
                controller.pause()
                controller.setMediaItems(listOf(first, second).mapIndexed { i, file ->
                    MediaItem.Builder()
                        .setMediaId(if (i == 0) "crossfade-alpha" else "crossfade-beta")
                        .setUri(Uri.fromFile(file))
                        .setMediaMetadata(MediaMetadata.Builder()
                            .setTitle("Crossfade QA WAV ${i + 1}")
                            .build())
                        .build()
                }, 0, 0L)
                controller.setPlaybackSpeed(1f)
                controller.shuffleModeEnabled = false
                controller.repeatMode = Player.REPEAT_MODE_OFF
                controller.prepare()
            }
            setDuration(controller, 0)
            resetToFirst(controller)
            Log.i("MusicFlowCrossfadeQA", "PASS offlineMediaSessionQueueReady")
            run(controller)
        } finally {
            media?.let { controller ->
                runCatching { setDuration(controller, 0) }
                previous?.let { original ->
                    main {
                        controller.pause()
                        if (original.items.isEmpty()) controller.clearMediaItems()
                        else {
                            controller.setMediaItems(
                                original.items,
                                original.index.coerceIn(0, original.items.lastIndex),
                                original.positionMs
                            )
                            controller.prepare()
                        }
                        controller.setPlaybackSpeed(original.speed)
                        controller.shuffleModeEnabled = original.shuffle
                        controller.repeatMode = original.repeat
                        if (original.playWhenReady && original.items.isNotEmpty()) {
                            controller.play()
                        }
                    }
                    runCatching { setDuration(controller, original.durationMs) }
                }
            }
            main { MediaController.releaseFuture(future) }
            first.delete()
            second.delete()
        }
    }

    @Test
    fun transitionCommandValidatesAndPersistsSupportedDurations() =
        withLocalQueue { media ->
            val before = context.getSharedPreferences(
                "musicflow_crossfade_v1", Context.MODE_PRIVATE
            )
            for (value in listOf(0, 1000, 2000, 3000)) {
                setDuration(media, value)
                assertEquals(value, before.getInt("durationMs", -1))
            }
            for (invalid in listOf(-1, 500, 1500, 4000)) {
                val rejected = command(media, TransitionContract.OP_SET, invalid)
                assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, rejected.resultCode)
                assertEquals(3000, duration(media))
            }
            val unknown = command(media, -99)
            assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, unknown.resultCode)
            assertEquals(3000, duration(media))
            Log.i("MusicFlowCrossfadeQA", "PASS allowedDurationsAndInvalidRejected")
        }

    @Test
    fun zeroDurationUsesNaturalMedia3ForwardTransition() = withLocalQueue { media ->
        setDuration(media, 0)
        main { media.seekTo(0, 10_200L) }
        awaitSecond(media, "Media3 forward transition with crossfade disabled")
        assertEquals(0, duration(media))
        Log.i("MusicFlowCrossfadeQA", "PASS defaultDisabledMedia3Forward")
    }

    @Test
    fun enabledCrossfadeMixesAndHandsOffAtOneTwoThreeSeconds() =
        withLocalQueue { media ->
            for (crossfadeMs in listOf(1000, 2000, 3000)) {
                setDuration(media, crossfadeMs)
                resetToFirst(media)
                // Seeking *before* the preload window preserves the production
                // timer and makes the test take seconds instead of 12 sec/track.
                main { media.seekTo(0, 12_000L - crossfadeMs - 2_200L) }
                awaitSecond(media, "crossfade handoff at $crossfadeMs ms")
                await("primary MediaSession moved into next track at $crossfadeMs") {
                    media.currentPosition > 500L
                }
                assertEquals(crossfadeMs, duration(media))
                main {
                    assertTrue("Crossfade changed speed",
                        abs(media.playbackParameters.speed - 1f) < 0.001f)
                }
                // Let the production engine finish its 250 ms primary/secondary
                // handoff before the following test resets the playlist.
                SystemClock.sleep(650L)
                Log.i("MusicFlowCrossfadeQA",
                    "PASS activeCrossfadeHandoff durationMs=$crossfadeMs")
            }
        }

    @Test
    fun variableSpeedFallsBackToOrdinaryForwardTransition() =
        withLocalQueue { media ->
            setDuration(media, 3000)
            resetToFirst(media, speed = 1.25f)
            main { media.seekTo(0, 10_200L) }
            awaitSecond(media, "forward transition at non-unit speed")
            main {
                assertEquals(1.25f, media.playbackParameters.speed, 0.001f)
            }
            assertEquals(3000, duration(media))
            Log.i("MusicFlowCrossfadeQA", "PASS variableSpeedForwardFallback")
        }
}
