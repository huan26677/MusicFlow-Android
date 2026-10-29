package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/** Exercises the real MediaSessionService and queue persistence on Android. */
@RunWith(AndroidJUnit4::class)
class QueueManagerIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun wav(): File {
        val sampleRate = 8000
        val dataSize = sampleRate * 45 * 2
        val bytes = ByteBuffer.allocate(44 + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun text(value: String) { bytes.put(value.toByteArray(Charsets.US_ASCII)) }
        text("RIFF")
        bytes.putInt(36 + dataSize)
        text("WAVE")
        text("fmt ")
        bytes.putInt(16)
        bytes.putShort(1.toShort())
        bytes.putShort(1.toShort())
        bytes.putInt(sampleRate)
        bytes.putInt(sampleRate * 2)
        bytes.putShort(2.toShort())
        bytes.putShort(16.toShort())
        text("data")
        bytes.putInt(dataSize)
        return File.createTempFile("musicflow_queue_manager_", ".wav", context.cacheDir)
            .also { it.writeBytes(bytes.array()) }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 15_000L
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(75)
        }
        assertTrue("Timed out waiting for MediaSession queue state", condition())
    }

    @Test
    fun reorderRemoveClearKeepCurrentPlayingAndPersisted() {
        val files = List(4) { wav() }
        val items = files.mapIndexed { index, file ->
            MediaItem.Builder()
                .setMediaId("qa-queue-manager-${index + 1}")
                .setUri(Uri.fromFile(file))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("QA Queue ${index + 1}")
                        .setArtist("MusicFlow QA")
                        .build()
                ).build()
        }
        val expectedInitial = items.map { it.mediaId }
        val prefs = context.getSharedPreferences(
            "musicflow_playback_queue_v1", Context.MODE_PRIVATE
        )
        val previousSnapshot = prefs.getString("snapshot", null)
        var future: ListenableFuture<MediaController>? = null
        var playback: MediaController? = null
        var queueApi: PlaybackController? = null

        try {
            instrumentation.runOnMainSync {
                queueApi = PlaybackController(context)
            }
            val connecting = MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, PlaybackService::class.java))
            ).buildAsync()
            future = connecting
            val media = connecting.get(20, TimeUnit.SECONDS)
            playback = media

            instrumentation.runOnMainSync {
                media.shuffleModeEnabled = false
                media.repeatMode = Player.REPEAT_MODE_OFF
                media.setMediaItems(items, 1, 5_000L)
                media.prepare()
                media.play()
            }

            await {
                val state = checkNotNull(queueApi).uiState.value
                state.connected && state.queueItems.map { it.mediaId } == expectedInitial &&
                    state.currentIndex == 2 && state.canEditQueue
            }
            await {
                var playing = false
                instrumentation.runOnMainSync { playing = media.isPlaying }
                playing
            }
            var beforePosition = 0L
            instrumentation.runOnMainSync { beforePosition = media.currentPosition }
            assertTrue(beforePosition >= 5_000L)

            val oldVersion = checkNotNull(queueApi).uiState.value.queueVersion
            var didMove = false
            instrumentation.runOnMainSync {
                didMove = checkNotNull(queueApi).moveQueueItem(
                    3, 2, checkNotNull(queueApi).uiState.value.queueVersion
                )
            }
            assertTrue("Move of future track should work", didMove)
            val movedIds = listOf(expectedInitial[0], expectedInitial[1],
                expectedInitial[3], expectedInitial[2])
            await { checkNotNull(queueApi).uiState.value.queueItems.map { it.mediaId } == movedIds }

            var staleRemoved = true
            var currentRemoved = true
            instrumentation.runOnMainSync {
                staleRemoved = checkNotNull(queueApi).removeQueueItem(0, oldVersion)
                currentRemoved = checkNotNull(queueApi).removeQueueItem(
                    1, checkNotNull(queueApi).uiState.value.queueVersion
                )
            }
            assertFalse("Stale timeline edit must be rejected", staleRemoved)
            assertFalse("Current track must be protected", currentRemoved)

            var didRemove = false
            instrumentation.runOnMainSync {
                didRemove = checkNotNull(queueApi).removeQueueItem(
                    0, checkNotNull(queueApi).uiState.value.queueVersion
                )
            }
            assertTrue(didRemove)
            val afterRemoveIds = movedIds.drop(1)
            await {
                val state = checkNotNull(queueApi).uiState.value
                state.queueItems.map { it.mediaId } == afterRemoveIds &&
                    state.currentIndex == 1
            }

            var didClear = false
            instrumentation.runOnMainSync {
                didClear = checkNotNull(queueApi).clearQueueOthers(
                    checkNotNull(queueApi).uiState.value.queueVersion
                )
            }
            assertTrue(didClear)
            await {
                val state = checkNotNull(queueApi).uiState.value
                state.queueItems.map { it.mediaId } == listOf(expectedInitial[1]) &&
                    state.currentIndex == 1
            }

            instrumentation.runOnMainSync {
                assertTrue("Playback was interrupted", media.isPlaying)
                assertEquals(expectedInitial[1], media.currentMediaItem?.mediaId)
                assertTrue(
                    "Current track position reset on queue edit",
                    media.currentPosition >= beforePosition - 150L
                )
            }

            await {
                val snapshot = prefs.getString("snapshot", null)
                runCatching {
                    val saved = JSONObject(checkNotNull(snapshot))
                    saved.getJSONArray("items").length() == 1 &&
                        saved.getJSONArray("items").getJSONObject(0)
                            .getString("mediaId") == expectedInitial[1] &&
                        saved.getInt("index") == 0 &&
                        saved.getLong("positionMs") >= 5_000L
                }.getOrDefault(false)
            }
        } finally {
            playback?.let { media ->
                instrumentation.runOnMainSync {
                    media.pause()
                    media.stop()
                    media.clearMediaItems()
                }
            }
            instrumentation.runOnMainSync {
                queueApi?.release()
                future?.let(MediaController::releaseFuture)
            }
            prefs.edit().apply {
                if (previousSnapshot == null) remove("snapshot")
                else putString("snapshot", previousSnapshot)
            }.commit()
            files.forEach(File::delete)
        }
    }
}
