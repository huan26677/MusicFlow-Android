package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.musicflow.MainActivity
import com.google.common.util.concurrent.ListenableFuture

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlaybackRuntimeAutoSkipTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val context = instrumentation.targetContext

    private fun createWav(): File {
        val sampleRate = 8000
        val dataSize = sampleRate * 20 * 2

        val bytes = ByteBuffer
            .allocate(44 + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)

        fun write(value: String) {
            bytes.put(value.toByteArray(Charsets.US_ASCII))
        }

        write("RIFF")
        bytes.putInt(36 + dataSize)
        write("WAVE")
        write("fmt ")
        bytes.putInt(16)
        bytes.putShort(1.toShort())
        bytes.putShort(1.toShort())
        bytes.putInt(sampleRate)
        bytes.putInt(sampleRate * 2)
        bytes.putShort(2.toShort())
        bytes.putShort(16.toShort())
        write("data")
        bytes.putInt(dataSize)

        return File.createTempFile(
            "qa_runtime_valid_",
            ".wav",
            context.cacheDir
        ).also {
            it.writeBytes(bytes.array())
        }
    }

    private fun withController(
        action: (MediaController) -> Unit
    ) {
        var activity: MainActivity? = null
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null

        try {
            activity = instrumentation.startActivitySync(
                Intent(
                    context,
                    MainActivity::class.java
                ).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            ) as MainActivity

            instrumentation.waitForIdleSync()

            val token = SessionToken(
                context,
                ComponentName(
                    context,
                    PlaybackService::class.java
                )
            )

            val connection = MediaController.Builder(
                context,
                token
            ).buildAsync()

            future = connection

            val connected = connection.get(
                20,
                TimeUnit.SECONDS
            )

            controller = connected
            action(connected)
        } finally {
            controller?.let { current ->
                instrumentation.runOnMainSync {
                    current.pause()
                    current.stop()
                    current.clearMediaItems()
                }
            }

            future?.let { connection ->
                instrumentation.runOnMainSync {
                    MediaController.releaseFuture(connection)
                }
            }

            activity?.let { screen ->
                instrumentation.runOnMainSync {
                    screen.finish()
                }
            }
        }
    }


    private fun runScenario(
        missingCount: Int,
        includeValid: Boolean,
        marker: String
    ) {
        val runId = System.nanoTime()

        val missingItems =
            (1..missingCount).map { index ->
                val missing = File(
                    context.cacheDir,
                    "qa_missing_${runId}_$index.wav"
                )

                // Không tạo file: lỗi xảy ra lúc Play.
                MediaItem.Builder()
                    .setMediaId("missing-$index")
                    .setUri(Uri.fromFile(missing))
                    .build()
            }

        val validFile = if (includeValid) {
            createWav()
        } else {
            null
        }

        try {
            val items = missingItems +
                listOfNotNull(
                    validFile?.let {
                        MediaItem.Builder()
                            .setMediaId("valid")
                            .setUri(Uri.fromFile(it))
                            .build()
                    }
                )

            withController { controller ->
                val complete = CountDownLatch(1)

                instrumentation.runOnMainSync {
                    controller.addListener(
                        object : Player.Listener {
                            override fun onEvents(
                                player: Player,
                                events: Player.Events
                            ) {
                                if (includeValid) {
                                    if (
                                        player.isPlaying &&
                                        player.currentMediaItem
                                            ?.mediaId == "valid"
                                    ) {
                                        complete.countDown()
                                    }
                                } else {
                                    if (
                                        player.currentMediaItemIndex ==
                                            missingCount - 1 &&
                                        !player.playWhenReady &&
                                        player.playbackState ==
                                            Player.STATE_IDLE
                                    ) {
                                        complete.countDown()
                                    }
                                }
                            }
                        }
                    )

                    // Cố tình bật Repeat ALL để xác nhận
                    // Auto-Skip không quay vòng vô hạn.
                    controller.shuffleModeEnabled = false
                    controller.repeatMode =
                        Player.REPEAT_MODE_ALL

                    controller.setMediaItems(
                        items,
                        0,
                        0L
                    )

                    controller.prepare()
                    controller.play()
                }

                assertTrue(
                    "Runtime Auto-Skip không hoàn tất",
                    complete.await(
                        20,
                        TimeUnit.SECONDS
                    )
                )

                instrumentation.runOnMainSync {
                    assertEquals(
                        items.size,
                        controller.mediaItemCount
                    )

                    if (includeValid) {
                        assertEquals(
                            "valid",
                            controller.currentMediaItem
                                ?.mediaId
                        )

                        assertTrue(controller.isPlaying)
                    } else {
                        assertEquals(
                            "missing-$missingCount",
                            controller.currentMediaItem
                                ?.mediaId
                        )

                        assertFalse(controller.isPlaying)
                        assertFalse(controller.playWhenReady)

                        assertEquals(
                            Player.STATE_IDLE,
                            controller.playbackState
                        )
                    }
                }

                if (!includeValid) {
                    // Kiểm tra trạng thái vẫn ổn định,
                    // không tự chạy lại bài đầu.
                    Thread.sleep(800)

                    instrumentation.runOnMainSync {
                        assertEquals(
                            missingCount - 1,
                            controller.currentMediaItemIndex
                        )

                        assertFalse(controller.isPlaying)
                        assertFalse(controller.playWhenReady)
                    }
                }
            }

            Log.i(
                "MusicFlowAutoSkipTest",
                "PASS $marker"
            )
        } finally {
            validFile?.delete()
        }
    }


    @Test
    fun singleMissingTrackAutoSkips() {
        runScenario(
            missingCount = 1,
            includeValid = true,
            marker = "singleMissingAutoSkipped"
        )
    }

    @Test
    fun consecutiveMissingTracksAutoSkip() {
        runScenario(
            missingCount = 2,
            includeValid = true,
            marker = "consecutiveMissingAutoSkipped"
        )
    }

    @Test
    fun allMissingTracksStopWithoutLoop() {
        runScenario(
            missingCount = 3,
            includeValid = false,
            marker = "allMissingStopsWithoutLoop"
        )
    }

    @Test
    fun pausedPlayerDoesNotAutoSkip() {
        var player: ExoPlayer? = null

        try {
            instrumentation.runOnMainSync {
                val exo =
                    ExoPlayer.Builder(context).build()

                player = exo

                exo.setMediaItems(
                    listOf(
                        MediaItem.Builder()
                            .setMediaId("first")
                            .setUri(
                                Uri.parse(
                                    "file:///missing1.wav"
                                )
                            )
                            .build(),

                        MediaItem.Builder()
                            .setMediaId("second")
                            .setUri(
                                Uri.parse(
                                    "file:///missing2.wav"
                                )
                            )
                            .build()
                    )
                )

                exo.pause()

                val recovery = PlaybackRuntimeAutoSkip(
                    exo
                ) { task ->
                    task()
                }

                recovery.onPlayerError(
                    PlaybackException(
                        "Simulated missing file",
                        null,
                        PlaybackException
                            .ERROR_CODE_IO_FILE_NOT_FOUND
                    )
                )

                assertEquals(
                    0,
                    exo.currentMediaItemIndex
                )

                assertFalse(exo.playWhenReady)
                assertFalse(exo.isPlaying)

                recovery.dispose()
            }

            Log.i(
                "MusicFlowAutoSkipTest",
                "PASS pausedPlayerDoesNotAutoSkip"
            )
        } finally {
            player?.let {
                instrumentation.runOnMainSync {
                    it.release()
                }
            }
        }
    }
}

