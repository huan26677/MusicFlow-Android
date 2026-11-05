package com.example.musicflow.core.playback

import android.content.ComponentName
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
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
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.compose.ui.test.assertIsDisplayed
import kotlin.math.roundToInt
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
import kotlin.math.abs

/** Real MainActivity + Media3 MediaSession + two locally generated WAV files. */
@RunWith(AndroidJUnit4::class)
class PlaybackSpeedRuntimeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device by lazy { UiDevice.getInstance(instrumentation) }

    private data class Snapshot(
        val media: List<MediaItem>,
        val index: Int,
        val position: Long,
        val playWhenReady: Boolean,
        val speed: Float,
        val shuffle: Boolean,
        val repeat: Int
    )

    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun wav(prefix: String): File {
        val rate = 8000
        val count = rate * 30
        val data = ByteBuffer.allocate(44 + count * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun ascii(value: String) { data.put(value.toByteArray(Charsets.US_ASCII)) }
        ascii("RIFF")
        data.putInt(36 + count * 2)
        ascii("WAVE")
        ascii("fmt ")
        data.putInt(16)
        data.putShort(1.toShort())
        data.putShort(1.toShort())
        data.putInt(rate)
        data.putInt(rate * 2)
        data.putShort(2.toShort())
        data.putShort(16.toShort())
        ascii("data")
        data.putInt(count * 2)
        return File.createTempFile(prefix, ".wav", context.cacheDir).apply {
            writeBytes(data.array())
        }
    }

    private fun waitState(
        description: String,
        timeoutMs: Long = 12_000L,
        check: () -> Boolean
    ) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            var matched = false
            main { matched = check() }
            if (matched) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for $description")
    }

    private fun speed(media: MediaController): Float {
        var value = Float.NaN
        main { value = media.playbackParameters.speed }
        return value
    }

    private fun expectSpeed(media: MediaController, value: Float) {
        waitState("real MediaSession speed $value") {
            abs(media.playbackParameters.speed - value) < 0.001f
        }
    }

    /** Dispatches a real Android touch to a Compose element's screen center. */
    private fun physicalTapAtChip(value: Float, player: MediaController) {
        val tag = "playback-speed-${value}x"
        val element = compose.onNodeWithTag(tag)
        element.assertIsDisplayed()
        val windowBounds = element.fetchSemanticsNode().boundsInWindow
        val screenLocation = IntArray(2)
        val windowLocation = IntArray(2)
        main {
            val decor = compose.activity.window.decorView
            decor.getLocationOnScreen(screenLocation)
            decor.getLocationInWindow(windowLocation)
        }
        val x = (windowBounds.center.x + screenLocation[0] -
            windowLocation[0]).roundToInt()
        val y = (windowBounds.center.y + screenLocation[1] -
            windowLocation[1]).roundToInt()
        Log.i("MusicFlowSpeedTouchQA",
            "REAL_TAP value=$value bounds=$windowBounds screen=($x,$y) " +
                "screenSize=${device.displayWidth}x${device.displayHeight}")
        // After a font-scale configuration change, the OS can briefly lose
        // the first input event while focus/layout settles. Retry only actual
        // injected touchscreen events, never semantic OnClick or direct
        // Player.setPlaybackSpeed as a fallback.
        for (attempt in 1..3) {
            compose.waitForIdle()
            device.waitForIdle(800L)
            assertTrue("Real screen tap was rejected by device", device.click(x, y))
            try {
                waitState("physical tap $value", 2_000L) {
                    abs(player.playbackParameters.speed - value) < 0.001f
                }
                Log.i("MusicFlowSpeedTouchQA",
                    "REAL_TAP_CONFIRMED value=$value attempt=$attempt")
                return
            } catch (e: AssertionError) {
                Log.w("MusicFlowSpeedTouchQA",
                    "REAL_TAP_RETRY value=$value attempt=$attempt")
                if (attempt == 3) throw e
            }
        }
    }

    private fun withOfflineQueue(test: (MediaController) -> Unit) {
        val a = wav("musicflow_speed_alpha_")
        val b = wav("musicflow_speed_beta_")
        val future = MediaController.Builder(
            context, SessionToken(
                context, ComponentName(context, PlaybackService::class.java)
            )
        ).buildAsync()
        var media: MediaController? = null
        var old: Snapshot? = null
        try {
            media = future.get(20, TimeUnit.SECONDS)
            val player = media
            main {
                old = Snapshot(
                    media = (0 until player.mediaItemCount).map(player::getMediaItemAt),
                    index = player.currentMediaItemIndex,
                    position = player.currentPosition.coerceAtLeast(0L),
                    playWhenReady = player.playWhenReady,
                    speed = player.playbackParameters.speed,
                    shuffle = player.shuffleModeEnabled,
                    repeat = player.repeatMode
                )
                val sources = listOf(a, b).mapIndexed { index, file ->
                    MediaItem.Builder()
                        .setMediaId(if (index == 0) "speed-qa-alpha" else "speed-qa-beta")
                        .setUri(Uri.fromFile(file))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(if (index == 0)
                                    "Speed QA Track Alpha" else "Speed QA Track Beta")
                                .setArtist("MusicFlow QA")
                                .build()
                        ).build()
                }
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.setPlaybackSpeed(1f)
                player.setMediaItems(sources, 0, 0L)
                player.prepare()
                player.play()
            }
            waitState("offline WAV playback") {
                player.isPlaying && player.mediaItemCount == 2 &&
                    player.currentMediaItem?.mediaId == "speed-qa-alpha"
            }
            Log.i("MusicFlowSpeedQA", "PASS offlineMediaSessionReady")
            test(player)
        } finally {
            media?.let { player ->
                old?.let { original ->
                    main {
                        player.pause()
                        if (original.media.isEmpty()) player.clearMediaItems()
                        else {
                            player.setMediaItems(
                                original.media,
                                original.index.coerceIn(0, original.media.lastIndex),
                                original.position
                            )
                            player.prepare()
                        }
                        player.shuffleModeEnabled = original.shuffle
                        player.repeatMode = original.repeat
                        player.setPlaybackSpeed(original.speed)
                        if (original.playWhenReady && original.media.isNotEmpty())
                            player.play()
                    }
                }
            }
            main { MediaController.releaseFuture(future) }
            a.delete()
            b.delete()
        }
    }

    @Test
    fun physicalTouchOnSpeedChipsWorksAcrossHorizontalScroll() =
        withOfflineQueue { player ->
            compose.waitUntil(15_000L) {
                compose.onAllNodesWithContentDescription(
                    "Mở trình phát: Speed QA Track Alpha, MusicFlow QA"
                ).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription(
                "Mở trình phát: Speed QA Track Alpha, MusicFlow QA"
            ).performClick()
            compose.onNodeWithTag("player-advanced-toggle")
                .performScrollTo().performClick()
            // The speed Row scrolls *horizontally* inside the whole Player
            // screen's *vertical* scroll. Calling performScrollTo on the chip
            // reaches the nearest horizontal parent and cannot reveal the row
            // vertically. Scroll a sibling heading after that row instead.
            compose.onNodeWithText("Hẹn giờ tắt nhạc").performScrollTo()
            compose.waitForIdle()

            physicalTapAtChip(0.75f, player)
            expectSpeed(player, 0.75f)
            Log.i("MusicFlowSpeedTouchQA", "PASS physicalTapFirstSpeed")

            for (value in listOf(1f, 1.25f)) {
                physicalTapAtChip(value, player)
                expectSpeed(player, value)
            }
            Log.i("MusicFlowSpeedTouchQA", "PASS physicalTapVisibleMiddleSpeeds")

            // The final chip is beyond the screen width. Swipe the horizontal
            // row with actual input events and then tap the visible 2x chip.
            val screenBounds = compose.onNodeWithTag("playback-speed-0.75x")
                .fetchSemanticsNode().boundsInWindow
            val lastBefore = compose.onNodeWithTag("playback-speed-2.0x")
                .fetchSemanticsNode().boundsInWindow
            Log.i("MusicFlowSpeedTouchQA", "LAST_BEFORE_HORIZONTAL_SWIPE=$lastBefore")
            val y = (screenBounds.center.y).roundToInt()
            val screenWidth = device.displayWidth
            device.swipe(
                (screenWidth * 0.84f).toInt(), y,
                (screenWidth * 0.17f).toInt(), y, 18
            )
            device.waitForIdle(2000L)
            val lastAfter = compose.onNodeWithTag("playback-speed-2.0x")
                .fetchSemanticsNode().boundsInWindow
            Log.i("MusicFlowSpeedTouchQA", "LAST_AFTER_HORIZONTAL_SWIPE=$lastAfter")
            if (lastBefore.right <= lastBefore.left) {
                assertTrue("2x was not revealed by actual horizontal swipe",
                    lastAfter.right > lastAfter.left)
                Log.i("MusicFlowSpeedTouchQA",
                    "PASS horizontalOverflowRevealedByPhysicalSwipe")
            }
            for (value in listOf(1.5f, 2f)) {
                physicalTapAtChip(value, player)
                expectSpeed(player, value)
            }
            Log.i("MusicFlowSpeedTouchQA", "PASS physicalSwipeAndTapLastSpeed")
            Log.i("MusicFlowSpeedTouchQA", "PASS fivePhysicalSpeedTouches")
        }

    @Test
    fun nowPlayingSpeedChipsControlRealMediaSession() = withOfflineQueue { player ->
        compose.waitUntil(15_000L) {
            compose.onAllNodesWithContentDescription(
                "Mở trình phát: Speed QA Track Alpha, MusicFlow QA"
            ).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(
            "Mở trình phát: Speed QA Track Alpha, MusicFlow QA"
        ).performClick()
        compose.onNodeWithTag("player-advanced-toggle")
            .performScrollTo().performClick()

        for (value in listOf(0.75f, 1f, 1.25f, 1.5f, 2f)) {
            compose.onNodeWithTag("playback-speed-${value}x")
                .performScrollTo()
                .assertIsEnabled()
                .performSemanticsAction(SemanticsActions.OnClick)
            expectSpeed(player, value)
            compose.onNodeWithTag("playback-speed-${value}x").assertIsSelected()
        }
        Log.i("MusicFlowSpeedQA", "PASS fiveUiSpeedsReachMediaSession")

        // Pause/resume using production Now Playing buttons, not test-only
        // player calls. The selected speed should stay at 2x.
        compose.onNodeWithText("Tạm dừng").performScrollTo().performClick()
        waitState("paused playback") { !player.isPlaying }
        expectSpeed(player, 2f)
        compose.onNodeWithText("Phát").performScrollTo().performClick()
        waitState("resumed playback") { player.isPlaying }
        expectSpeed(player, 2f)
        Log.i("MusicFlowSpeedQA", "PASS uiPauseResumeKeepsSpeed")

        compose.onNodeWithText("Tiếp").performScrollTo().performClick()
        waitState("next offline audio") {
            player.currentMediaItem?.mediaId == "speed-qa-beta" &&
                player.mediaItemCount == 2 && player.currentMediaItemIndex == 1
        }
        expectSpeed(player, 2f)
        var correctQueue = false
        main {
            correctQueue = player.mediaItemCount == 2 &&
                player.getMediaItemAt(0).mediaId == "speed-qa-alpha" &&
                player.getMediaItemAt(1).mediaId == "speed-qa-beta"
        }
        assertTrue("Speed selection changed offline queue", correctQueue)
        Log.i("MusicFlowSpeedQA", "PASS nextTrackKeepsSpeedAndQueue")

        compose.onNodeWithTag("playback-speed-1.0x")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        expectSpeed(player, 1f)
        Log.i("MusicFlowSpeedQA", "PASS uiSpeedResetToNormal")
    }

    @Test
    fun playbackControllerRejectsInvalidSpeeds() = withOfflineQueue { player ->
        val controller = PlaybackController(context)
        try {
            waitState("separate PlaybackController connected") {
                controller.uiState.value.connected
            }
            main { controller.setPlaybackSpeed(1.25f) }
            expectSpeed(player, 1.25f)
            for (value in listOf(-1f, 0f, 0.49f, 2.01f, Float.NaN,
                Float.POSITIVE_INFINITY)) {
                main { controller.setPlaybackSpeed(value) }
                assertEquals("Invalid speed $value was accepted", 1.25f,
                    speed(player), 0.001f)
            }
            Log.i("MusicFlowSpeedQA", "PASS invalidSpeedsRejected")

            main { controller.setPlaybackSpeed(0.5f) }
            expectSpeed(player, 0.5f)
            main { controller.setPlaybackSpeed(2f) }
            expectSpeed(player, 2f)
            var stillPlaying = false
            main {
                stillPlaying = player.isPlaying &&
                    player.mediaItemCount == 2 &&
                    player.currentMediaItem?.mediaId == "speed-qa-alpha"
            }
            assertTrue("PlaybackController speed changed offline queue", stillPlaying)
            Log.i("MusicFlowSpeedQA", "PASS speedLimitsAndQueueStable")
        } finally {
            main { controller.release() }
        }
    }
}
