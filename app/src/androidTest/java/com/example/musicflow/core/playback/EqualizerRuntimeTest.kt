package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
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
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * Real Android Equalizer audio effect through the production MediaSession
 * while playing a generated local WAV. Never touches online music rights.
 */
@RunWith(AndroidJUnit4::class)
class EqualizerRuntimeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefs = context.getSharedPreferences(
        "musicflow_equalizer_v1", Context.MODE_PRIVATE
    )
    private val token by lazy {
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    }

    private data class QueueSnapshot(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long,
        val wasPlaying: Boolean,
        val shuffle: Boolean,
        val repeat: Int
    )

    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun request(
        controller: MediaController,
        operation: Int,
        enabled: Boolean? = null,
        band: Int? = null,
        level: Int? = null
    ): SessionResult {
        val arguments = Bundle().apply {
            putInt(EqualizerContract.KEY_OPERATION, operation)
            if (enabled != null) putBoolean(EqualizerContract.KEY_ENABLED, enabled)
            if (band != null) putInt(EqualizerContract.KEY_BAND_INDEX, band)
            if (level != null) putInt(EqualizerContract.KEY_LEVEL_MB, level)
        }
        var future: ListenableFuture<SessionResult>? = null
        main {
            future = controller.sendCustomCommand(
                SessionCommand(EqualizerContract.ACTION, Bundle.EMPTY), arguments
            )
        }
        return checkNotNull(future).get(10, TimeUnit.SECONDS)
    }

    private fun state(controller: MediaController): EqualizerUiState {
        val result = request(controller, EqualizerContract.OP_GET)
        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        val sessionId = result.extras.getInt(EqualizerContract.KEY_AUDIO_SESSION_ID, 0)
        assertTrue("Media3 must provide a real audio session ID", sessionId > 0)
        val state = EqualizerUiState.fromBundle(result.extras)
        assertTrue(
            "Equalizer unavailable on API 30 emulator; runtime cannot be accepted",
            state.supported
        )
        assertTrue("Audio effect must have bands", state.bands.isNotEmpty())
        assertTrue(state.minLevelMb <= state.maxLevelMb)
        assertTrue(state.bands.all {
            it.frequencyHz > 0 && it.levelMb in state.minLevelMb..state.maxLevelMb
        })
        return state
    }

    private fun makeWav(): File {
        val samples = 8000 * 30
        val data = ByteBuffer.allocate(44 + samples * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun label(s: String) { data.put(s.toByteArray(Charsets.US_ASCII)) }
        label("RIFF"); data.putInt(36 + samples * 2)
        label("WAVE"); label("fmt "); data.putInt(16)
        data.putShort(1.toShort()); data.putShort(1.toShort())
        data.putInt(8000); data.putInt(16000)
        data.putShort(2.toShort()); data.putShort(16.toShort())
        label("data"); data.putInt(samples * 2)
        return File.createTempFile("musicflow_eq_qa_", ".wav", context.cacheDir)
            .apply { writeBytes(data.array()) }
    }

    private fun awaitPlayback(media: MediaController) {
        val until = SystemClock.elapsedRealtime() + 12_000L
        while (SystemClock.elapsedRealtime() < until) {
            var playing = false
            main { playing = media.isPlaying }
            if (playing) return
            SystemClock.sleep(100)
        }
        fail("Real offline MediaSession did not start playing")
    }

    private fun withOfflineSession(check: (MediaController) -> Unit) {
        val file = makeWav()
        val future = MediaController.Builder(context, token).buildAsync()
        var media: MediaController? = null
        var originalQueue: QueueSnapshot? = null
        var originalEq: EqualizerUiState? = null
        val stored = prefs.all.toMap()
        try {
            media = future.get(20, TimeUnit.SECONDS)
            val controller = media
            main {
                originalQueue = QueueSnapshot(
                    (0 until controller.mediaItemCount).map(controller::getMediaItemAt),
                    controller.currentMediaItemIndex,
                    controller.currentPosition.coerceAtLeast(0),
                    controller.playWhenReady,
                    controller.shuffleModeEnabled,
                    controller.repeatMode
                )
                controller.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId("equalizer-local-qa")
                        .setUri(Uri.fromFile(file))
                        .build()
                )
                controller.prepare()
                controller.play()
            }
            awaitPlayback(controller)
            originalEq = state(controller)
            Log.i("MusicFlowEqualizerQA", "PASS liveAudioEffectSession")
            check(controller)
        } finally {
            media?.let { controller ->
                originalEq?.let { original ->
                    for (band in original.bands) {
                        runCatching {
                            request(controller, EqualizerContract.OP_SET_BAND,
                                band = band.index, level = band.levelMb)
                        }
                    }
                    runCatching {
                        request(controller, EqualizerContract.OP_ENABLE,
                            enabled = original.enabled)
                    }
                }
                // Restore precisely the preferences changed by these tests.
                // Do not erase any unrelated EQ settings from the device.
                prefs.edit().apply {
                    if (stored.containsKey("enabled"))
                        putBoolean("enabled", stored["enabled"] as Boolean)
                    else remove("enabled")
                    if (stored.containsKey("band_0"))
                        putInt("band_0", stored["band_0"] as Int)
                    else remove("band_0")
                }.commit()
                originalQueue?.let { snapshot ->
                    main {
                        controller.pause()
                        if (snapshot.items.isEmpty()) controller.clearMediaItems()
                        else {
                            controller.setMediaItems(
                                snapshot.items,
                                snapshot.index.coerceIn(0, snapshot.items.lastIndex),
                                snapshot.positionMs
                            )
                            controller.prepare()
                        }
                        controller.shuffleModeEnabled = snapshot.shuffle
                        controller.repeatMode = snapshot.repeat
                        if (snapshot.wasPlaying && snapshot.items.isNotEmpty())
                            controller.play()
                    }
                }
            }
            main { MediaController.releaseFuture(future) }
            file.delete()
        }
    }

    @Test
    fun validationAndPlaybackContinuity() = withOfflineSession { media ->
        val start = state(media)
        val rejectsOperation = request(media, -10)
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, rejectsOperation.resultCode)
        val badIndex = request(media, EqualizerContract.OP_SET_BAND,
            band = start.bands.size, level = 0)
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, badIndex.resultCode)
        val outOfRange = request(media, EqualizerContract.OP_SET_BAND,
            band = 0, level = start.maxLevelMb + 1)
        assertEquals(SessionResult.RESULT_ERROR_BAD_VALUE, outOfRange.resultCode)
        assertEquals(start, state(media))
        var stillPlaying = false
        main {
            stillPlaying = media.isPlaying
            assertEquals("equalizer-local-qa", media.currentMediaItem?.mediaId)
        }
        assertTrue(stillPlaying)
        Log.i("MusicFlowEqualizerQA", "PASS invalidSettingsDoNotStopPlayback")
    }

    @Test
    fun enableBandPersistAndControllerReconnect() = withOfflineSession { media ->
        val original = state(media)
        assertTrue("Need a nonzero EQ gain range",
            original.minLevelMb < original.maxLevelMb)

        val newEnabled = !original.enabled
        assertEquals(SessionResult.RESULT_SUCCESS,
            request(media, EqualizerContract.OP_ENABLE, enabled = newEnabled).resultCode)
        assertEquals(newEnabled, state(media).enabled)
        assertEquals(newEnabled, prefs.getBoolean("enabled", !newEnabled))
        Log.i("MusicFlowEqualizerQA", "PASS enableTogglePersisted")

        val current = original.bands[0].levelMb
        val adjusted = if (current != original.maxLevelMb)
            original.maxLevelMb else original.minLevelMb
        assertTrue(adjusted != current)
        assertEquals(SessionResult.RESULT_SUCCESS,
            request(media, EqualizerContract.OP_SET_BAND,
                band = 0, level = adjusted).resultCode)
        assertEquals(adjusted, state(media).bands[0].levelMb)
        assertEquals(adjusted, prefs.getInt("band_0", Int.MIN_VALUE))
        Log.i("MusicFlowEqualizerQA", "PASS bandGainPersisted")

        // A fresh MediaController reconnects to the *same service*, showing
        // the saved effect state. This is not a process-restart test.
        val otherFuture = MediaController.Builder(context, token).buildAsync()
        try {
            val another = otherFuture.get(20, TimeUnit.SECONDS)
            assertEquals(newEnabled, state(another).enabled)
            assertEquals(adjusted, state(another).bands[0].levelMb)
            Log.i("MusicFlowEqualizerQA", "PASS controllerReconnectKeepsState")
        } finally {
            main { MediaController.releaseFuture(otherFuture) }
        }

        var playing = false
        main { playing = media.isPlaying }
        assertTrue("Changing EQ interrupted offline music", playing)
        Log.i("MusicFlowEqualizerQA", "PASS equalizerDoesNotStopOfflineAudio")
    }
}
