package com.example.musicflow.core.playlist

import android.net.Uri
import android.util.Log

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playback.PlaybackSkipRecovery

import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PlaylistPlaybackOrderTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val context = instrumentation.targetContext

    private fun createWav(id: Int): File {
        val sampleRate = 8000
        val dataSize = sampleRate * 15 * 2

        val buffer = ByteBuffer
            .allocate(44 + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)

        fun chunk(value: String) {
            buffer.put(
                value.toByteArray(Charsets.US_ASCII)
            )
        }

        chunk("RIFF")
        buffer.putInt(36 + dataSize)
        chunk("WAVE")
        chunk("fmt ")
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2.toShort())
        buffer.putShort(16.toShort())
        chunk("data")
        buffer.putInt(dataSize)

        return File.createTempFile(
            "musicflow_order_${id}_",
            ".wav",
            context.cacheDir
        ).also {
            it.writeBytes(buffer.array())
        }
    }

    @Test
    fun playlistReorderControlsMedia3Playback() =
        runBlocking<Unit> {

            val files = (1..3).associateWith {
                createWav(it)
            }

            val database = Room.inMemoryDatabaseBuilder(
                context,
                RecentHistoryDatabase::class.java
            ).build()

            var player: ExoPlayer? = null

            val firstPlaying = CountDownLatch(1)
            val nextPlaying = CountDownLatch(1)
            val previousPlaying = CountDownLatch(1)

            val playerError =
                AtomicReference<String?>(null)

            try {
                val playlists =
                    RoomPlaylistRepository(database)

                val history =
                    RoomRecentHistoryRepository(database)

                val playlist = playlists.create(
                    "Playlist Playback Order Test"
                )!!

                val original = (1..3).map { id ->
                    PlaylistTrack(
                        mediaId = "device-$id",
                        audioUri = Uri.fromFile(
                            files.getValue(id)
                        ).toString(),
                        title = "Bài $id",
                        artist = "Test",
                        addedAtMs = id.toLong()
                    )
                }

                for (track in original) {
                    assertTrue(
                        playlists.addTrack(
                            playlist.id,
                            track
                        )
                    )
                }

                history.record(
                    RecentSong(
                        mediaId = "existing-history",
                        audioUri = "content://history/test",
                        title = "Lịch sử trước đó",
                        artist = "Test",
                        playedAtMs = 100L
                    )
                )

                val recentBefore = history.getRecent()

                assertTrue(
                    playlists.moveTrack(
                        playlist.id,
                        "device-3",
                        -1
                    )
                )

                assertTrue(
                    playlists.moveTrack(
                        playlist.id,
                        "device-3",
                        -1
                    )
                )

                val ordered = playlists.getTracks(
                    playlist.id
                )

                val expectedIds = listOf(
                    "device-3",
                    "device-1",
                    "device-2"
                )

                assertEquals(
                    expectedIds,
                    ordered.map { it.mediaId }
                )

                val items =
                    PlaylistMediaItems.fromTracks(ordered)

                assertEquals(
                    expectedIds,
                    items.map { it.mediaId }
                )

                assertEquals(
                    ordered.map { it.audioUri },
                    items.map {
                        it.localConfiguration
                            ?.uri
                            ?.toString()
                    }
                )

                assertEquals(
                    ordered.map { it.title },
                    items.map {
                        it.mediaMetadata.title.toString()
                    }
                )

                Log.i(
                    "MusicFlowOrderTest",
                    "PASS roomToMediaItemsOrder"
                )

                var phase = 0

                instrumentation.runOnMainSync {
                    val exo =
                        ExoPlayer.Builder(context).build()

                    player = exo

                    exo.addListener(
                        object : Player.Listener {

                            override fun onPlayerError(
                                error: PlaybackException
                            ) {
                                playerError.set(
                                    error.errorCodeName
                                )
                            }

                            override fun onEvents(
                                current: Player,
                                events: Player.Events
                            ) {
                                if (!current.isPlaying) return

                                val id =
                                    current.currentMediaItem
                                        ?.mediaId

                                when (phase) {
                                    0 -> if (id == "device-3") {
                                        firstPlaying.countDown()
                                    }

                                    1 -> if (id == "device-1") {
                                        nextPlaying.countDown()
                                    }

                                    2 -> if (id == "device-3") {
                                        previousPlaying.countDown()
                                    }
                                }
                            }
                        }
                    )

                    exo.setMediaItems(
                        items,
                        0,
                        0L
                    )

                    assertEquals(
                        3,
                        exo.mediaItemCount
                    )

                    assertEquals(
                        expectedIds,
                        (0 until exo.mediaItemCount)
                            .map {
                                exo.getMediaItemAt(it)
                                    .mediaId
                            }
                    )

                    exo.prepare()
                    exo.play()
                }

                assertTrue(
                    "Không phát được bài đầu: " +
                        playerError.get(),
                    firstPlaying.await(
                        15,
                        TimeUnit.SECONDS
                    )
                )

                Log.i(
                    "MusicFlowOrderTest",
                    "PASS firstTrackAfterReorder"
                )

                instrumentation.runOnMainSync {
                    phase = 1

                    assertTrue(
                        PlaybackSkipRecovery.next(
                            checkNotNull(player)
                        )
                    )
                }

                assertTrue(
                    "Next không phát đúng bài: " +
                        playerError.get(),
                    nextPlaying.await(
                        15,
                        TimeUnit.SECONDS
                    )
                )

                Log.i(
                    "MusicFlowOrderTest",
                    "PASS nextUsesReorderedQueue"
                )

                instrumentation.runOnMainSync {
                    phase = 2
                    checkNotNull(player)
                        .seekToPreviousMediaItem()
                }

                assertTrue(
                    "Previous không phát đúng bài: " +
                        playerError.get(),
                    previousPlaying.await(
                        15,
                        TimeUnit.SECONDS
                    )
                )

                Log.i(
                    "MusicFlowOrderTest",
                    "PASS previousUsesReorderedQueue"
                )

                instrumentation.runOnMainSync {
                    val exo = checkNotNull(player)

                    exo.pause()

                    exo.setMediaItems(items, 1, 0L)

                    assertEquals(
                        "device-1",
                        exo.currentMediaItem?.mediaId
                    )

                    assertEquals(3, exo.mediaItemCount)

                    exo.seekToNextMediaItem()

                    assertEquals(
                        "device-2",
                        exo.currentMediaItem?.mediaId
                    )

                    exo.seekToPreviousMediaItem()

                    assertEquals(
                        "device-1",
                        exo.currentMediaItem?.mediaId
                    )
                }

                Log.i(
                    "MusicFlowOrderTest",
                    "PASS selectedIndexKeepsFullQueue"
                )

                assertEquals(
                    expectedIds,
                    playlists.getTracks(playlist.id)
                        .map { it.mediaId }
                )

                assertEquals(
                    recentBefore,
                    history.getRecent()
                )

                Log.i(
                    "MusicFlowOrderTest",
                    "PASS playlistAndHistoryUnchanged"
                )

                Log.i(
                    "MusicFlowOrderTest",
                    "PASS playlistReorderControlsMedia3Playback"
                )

            } finally {
                player?.let { exo ->
                    instrumentation.runOnMainSync {
                        exo.release()
                    }
                }

                database.close()

                files.values.forEach {
                    it.delete()
                }
            }
        }
}

