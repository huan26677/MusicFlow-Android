package com.example.musicflow.core.playback

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlaybackSkipRecoveryTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val context =
        instrumentation.targetContext

    private fun createTestWav(): File {
        val rate = 8000
        val dataSize = rate * 5 * 2

        val bytes = ByteBuffer
            .allocate(44 + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)

        fun text(value: String) {
            bytes.put(value.toByteArray(Charsets.US_ASCII))
        }

        text("RIFF")
        bytes.putInt(36 + dataSize)
        text("WAVE")
        text("fmt ")
        bytes.putInt(16)
        bytes.putShort(1.toShort())
        bytes.putShort(1.toShort())
        bytes.putInt(rate)
        bytes.putInt(rate * 2)
        bytes.putShort(2.toShort())
        bytes.putShort(16.toShort())
        text("data")
        bytes.putInt(dataSize)

        val file = File.createTempFile(
            "musicflow_test_",
            ".wav",
            context.cacheDir
        )

        file.writeBytes(bytes.array())
        return file
    }

    @Test
    fun missingFileCanSkipToValidTrack() {
        val validFile = createTestWav()
        val missingFile = File(
            context.cacheDir,
            "missing_${System.nanoTime()}.wav"
        )

        val errorSignal = CountDownLatch(1)
        val playingSignal = CountDownLatch(1)

        lateinit var player: ExoPlayer

        try {
            instrumentation.runOnMainSync {
                player = ExoPlayer.Builder(context).build()

                player.addListener(object : Player.Listener {

                    override fun onPlayerError(
                        error: PlaybackException
                    ) {
                        Log.i(
                            "MusicFlowTest",
                            "MISSING_FILE_ERROR code=${error.errorCode}"
                        )
                        errorSignal.countDown()
                    }

                    override fun onIsPlayingChanged(
                        isPlaying: Boolean
                    ) {
                        if (
                            isPlaying &&
                            player.currentMediaItem?.mediaId ==
                                "valid"
                        ) {
                            playingSignal.countDown()
                        }
                    }
                })

                player.setMediaItems(
                    listOf(
                        MediaItem.Builder()
                            .setMediaId("missing")
                            .setUri(Uri.fromFile(missingFile))
                            .build(),

                        MediaItem.Builder()
                            .setMediaId("valid")
                            .setUri(Uri.fromFile(validFile))
                            .build()
                    )
                )

                player.prepare()
                player.play()
            }

            assertTrue(
                "Không nhận được lỗi file thiếu",
                errorSignal.await(15, TimeUnit.SECONDS)
            )

            instrumentation.runOnMainSync {
                assertEquals(
                    Player.STATE_IDLE,
                    player.playbackState
                )

                assertTrue(
                    PlaybackSkipRecovery.next(player)
                )

                assertEquals(
                    "valid",
                    player.currentMediaItem?.mediaId
                )
            }

            assertTrue(
                "Không phát được bài hợp lệ sau khi bỏ qua",
                playingSignal.await(15, TimeUnit.SECONDS)
            )

            Log.i(
                "MusicFlowTest",
                "PASS missingFileCanSkipToValidTrack"
            )
        } finally {
            if (::player.isInitialized) {
                instrumentation.runOnMainSync {
                    player.release()
                }
            }

            validFile.delete()
        }
    }

    @Test
    fun noNextTrackDoesNotSkip() {
        lateinit var player: ExoPlayer

        try {
            instrumentation.runOnMainSync {
                player = ExoPlayer.Builder(context).build()

                player.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId("last")
                        .setUri(
                            Uri.parse("file:///not-existing.wav")
                        )
                        .build()
                )

                assertFalse(
                    PlaybackSkipRecovery.next(player)
                )
            }

            Log.i(
                "MusicFlowTest",
                "PASS noNextTrackDoesNotSkip"
            )
        } finally {
            if (::player.isInitialized) {
                instrumentation.runOnMainSync {
                    player.release()
                }
            }
        }
    }
}
