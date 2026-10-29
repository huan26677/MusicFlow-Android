package com.example.musicflow.core.lyrics

import android.content.ComponentName
import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.playback.PlaybackService
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.player.NowPlayingScreen
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * The real MediaSession supplies playback/seek position; NowPlayingScreen
 * must highlight the correct LRC line and follow seeks backwards too.
 */
@RunWith(AndroidJUnit4::class)
class LyricsScreenSyncTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun wav(): File {
        val samples = 8000 * 30
        val data = ByteBuffer.allocate(44 + samples * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun write(s: String) { data.put(s.toByteArray(Charsets.US_ASCII)) }
        write("RIFF"); data.putInt(36 + samples * 2)
        write("WAVE"); write("fmt "); data.putInt(16)
        data.putShort(1.toShort()); data.putShort(1.toShort())
        data.putInt(8000); data.putInt(16000)
        data.putShort(2.toShort()); data.putShort(16.toShort())
        write("data"); data.putInt(samples * 2)
        return File.createTempFile("musicflow_lrc_player_", ".wav", context.cacheDir)
            .apply { writeBytes(data.array()) }
    }

    @Test
    fun highlightsLrcLineAtRealMediaSessionSeekPositions() {
        val audio = wav()
        val future = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(
                context, PlaybackService::class.java
            ))
        ).buildAsync()
        var media: MediaController? = null
        var oldItems = emptyList<MediaItem>()
        var oldIndex = 0
        var oldPosition = 0L
        var oldPlayWhenReady = false
        var oldShuffle = false
        var oldRepeat = androidx.media3.common.Player.REPEAT_MODE_OFF
        var positionMs by mutableLongStateOf(0L)
        val lines = LrcParser.parse(
            "[00:01.00]First lyric\n[00:03.00]Second lyric\n[00:06.00]Last lyric"
        )
        try {
            media = future.get(20, TimeUnit.SECONDS)
            val controller = media
            main {
                oldItems = (0 until controller.mediaItemCount)
                    .map(controller::getMediaItemAt)
                oldIndex = controller.currentMediaItemIndex
                oldPosition = controller.currentPosition.coerceAtLeast(0)
                oldPlayWhenReady = controller.playWhenReady
                oldShuffle = controller.shuffleModeEnabled
                oldRepeat = controller.repeatMode
                controller.setMediaItem(
                    MediaItem.Builder().setMediaId("lyrics-real-player-qa")
                        .setUri(Uri.fromFile(audio)).build()
                )
                controller.prepare()
                controller.play()
            }
            val end = SystemClock.uptimeMillis() + 10_000
            while (SystemClock.uptimeMillis() < end) {
                var playing = false
                main { playing = controller.isPlaying }
                if (playing) break
                SystemClock.sleep(100)
            }
            var playing = false
            main { playing = controller.isPlaying }
            assertTrue("Offline playback never started", playing)
            main { controller.pause() }

            compose.setContent {
                MusicFlowTheme {
                    NowPlayingScreen(
                        state = PlayerUiState(
                            connected = true,
                            hasMedia = true,
                            title = "Lyrics QA",
                            audioUri = Uri.fromFile(audio),
                            positionMs = positionMs,
                            durationMs = 30_000L,
                            queueCount = 1
                        ),
                        onBack = {}, onPickFiles = {}, onOpenLibrary = {},
                        onPlayPause = {}, onPrevious = {}, onNext = {},
                        onSeek = {}, onToggleShuffle = {}, onCycleRepeat = {},
                        onSetPlaybackSpeed = {}, onSetSleepTimerMinutes = {},
                        onSetEqualizerEnabled = {}, onSetEqualizerBand = { _, _ -> },
                        onSetCrossfadeDuration = {},
                        lyrics = LyricsUiState(
                            trackUri = Uri.fromFile(audio).toString(),
                            lines = lines
                        ),
                        onPickLyrics = {}
                    )
                }
            }
            compose.onNodeWithTag("lyrics-active-line").assertDoesNotExist()

            for ((seek, text) in listOf(
                1200L to "First lyric",
                3300L to "Second lyric",
                6200L to "Last lyric",
                1800L to "First lyric"
            )) {
                main {
                    controller.seekTo(seek)
                    positionMs = controller.currentPosition
                }
                compose.waitForIdle()
                compose.onNodeWithTag("lyrics-active-line")
                    .assertTextEquals(text)
            }
            android.util.Log.i("MusicFlowLyricsQA", "PASS realPlaybackSeekLyricsHighlight")
        } finally {
            media?.let { controller ->
                main {
                    controller.pause()
                    if (oldItems.isEmpty()) controller.clearMediaItems()
                    else {
                        controller.setMediaItems(
                            oldItems, oldIndex.coerceIn(0, oldItems.lastIndex),
                            oldPosition
                        )
                        controller.prepare()
                    }
                    controller.shuffleModeEnabled = oldShuffle
                    controller.repeatMode = oldRepeat
                    if (oldPlayWhenReady && oldItems.isNotEmpty()) controller.play()
                }
            }
            main { MediaController.releaseFuture(future) }
            audio.delete()
        }
    }
}
