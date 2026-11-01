package com.example.musicflow.core.playback

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CrossfadeUiE2eTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun wav(): File {
        val count = 8000 * 30
        val bytes = ByteBuffer.allocate(44 + count * 2).order(ByteOrder.LITTLE_ENDIAN)
        fun text(v: String) { bytes.put(v.toByteArray(Charsets.US_ASCII)) }
        text("RIFF"); bytes.putInt(36 + count * 2)
        text("WAVE"); text("fmt "); bytes.putInt(16)
        bytes.putShort(1.toShort()); bytes.putShort(1.toShort())
        bytes.putInt(8000); bytes.putInt(16000)
        bytes.putShort(2.toShort()); bytes.putShort(16.toShort())
        text("data"); bytes.putInt(count * 2)
        return File.createTempFile("musicflow_crossfade_ui_", ".wav",
            context.cacheDir).apply { writeBytes(bytes.array()) }
    }

    private fun duration(media: MediaController): Int {
        val args = Bundle().apply {
            putInt(TransitionContract.KEY_OPERATION, TransitionContract.OP_GET)
        }
        var request: com.google.common.util.concurrent.ListenableFuture<
            androidx.media3.session.SessionResult>? = null
        main {
            request = media.sendCustomCommand(
                SessionCommand(TransitionContract.ACTION, Bundle.EMPTY), args
            )
        }
        return checkNotNull(request).get(10, TimeUnit.SECONDS).extras
            .getInt(TransitionContract.KEY_DURATION_MS, -1)
    }

    @Test
    fun nowPlayingCrossfadeChipsReachRealMediaSession() {
        val audio = wav()
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(
                context, PlaybackService::class.java
            ))
        ).buildAsync()
        var media: MediaController? = null
        var oldItems = emptyList<MediaItem>()
        var oldIndex = 0
        var oldPosition = 0L
        var oldPlaying = false
        var oldSpeed = 1f
        var oldShuffle = false
        var oldRepeat = Player.REPEAT_MODE_OFF
        var oldDuration: Int? = null
        try {
            media = future.get(20, TimeUnit.SECONDS)
            val controller = media
            oldDuration = duration(controller)
            main {
                oldItems = (0 until controller.mediaItemCount)
                    .map(controller::getMediaItemAt)
                oldIndex = controller.currentMediaItemIndex
                oldPosition = controller.currentPosition.coerceAtLeast(0L)
                oldPlaying = controller.playWhenReady
                oldSpeed = controller.playbackParameters.speed
                oldShuffle = controller.shuffleModeEnabled
                oldRepeat = controller.repeatMode
                controller.pause()
                controller.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId("crossfade-ui-qa")
                        .setUri(Uri.fromFile(audio))
                        .setMediaMetadata(MediaMetadata.Builder()
                            .setTitle("Crossfade UI QA").setArtist("MusicFlow QA").build())
                        .build()
                )
                controller.setPlaybackSpeed(1f)
                controller.prepare()
                controller.play()
            }
            compose.waitUntil(15_000L) {
                compose.onAllNodesWithContentDescription(
                    "Mở trình phát: Crossfade UI QA, MusicFlow QA"
                ).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription(
                "Mở trình phát: Crossfade UI QA, MusicFlow QA"
            ).performClick()

            // Scroll the outer vertical Player by its following heading,
            // not by the nested horizontally scrollable FilterChip.
            compose.onNodeWithText("Equalizer").performScrollTo()
            for (ms in listOf(0, 1000, 2000, 3000, 0)) {
                val chip = compose.onNodeWithTag("crossfade-duration-${ms}ms")
                chip.assertIsDisplayed()
                chip.performClick()
                compose.waitUntil(10_000L) {
                    duration(controller) == ms
                }
                chip.assertIsSelected()
                Log.i("MusicFlowCrossfadeUiQA", "PASS uiCrossfadeSelectedMs=$ms")
            }
            main {
                assertTrue(controller.mediaItemCount == 1)
                assertEquals("crossfade-ui-qa", controller.currentMediaItem?.mediaId)
                assertTrue(controller.isPlaying)
            }
            Log.i("MusicFlowCrossfadeUiQA", "PASS uiDurationControlsRealMediaSession")
        } finally {
            media?.let { controller ->
                main {
                    controller.pause()
                    if (oldItems.isEmpty()) controller.clearMediaItems()
                    else {
                        controller.setMediaItems(
                            oldItems, oldIndex.coerceIn(0, oldItems.lastIndex), oldPosition)
                        controller.prepare()
                    }
                    controller.setPlaybackSpeed(oldSpeed)
                    controller.shuffleModeEnabled = oldShuffle
                    controller.repeatMode = oldRepeat
                    if (oldPlaying && oldItems.isNotEmpty()) controller.play()
                }
                oldDuration?.let { saved ->
                    // A fresh controller is unnecessary: same command as UI.
                    val args = Bundle().apply {
                        putInt(TransitionContract.KEY_OPERATION, TransitionContract.OP_SET)
                        putInt(TransitionContract.KEY_DURATION_MS, saved)
                    }
                    var request: com.google.common.util.concurrent.ListenableFuture<
                        androidx.media3.session.SessionResult>? = null
                    main {
                        request = controller.sendCustomCommand(
                            SessionCommand(TransitionContract.ACTION, Bundle.EMPTY), args)
                    }
                    checkNotNull(request).get(10, TimeUnit.SECONDS)
                }
            }
            main { MediaController.releaseFuture(future) }
            audio.delete()
        }
    }
}
