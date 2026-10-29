package com.example.musicflow.core.playback

import android.content.Context
import android.net.Uri
import android.util.Log

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.PlaylistMediaItems
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.RoomPlaylistRepository

import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlaybackQueuePersistenceTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val context = instrumentation.targetContext

    @Test
    fun legacyOnlineSnapshotCannotAutoResumeWithoutRights() {
        val store = PlaybackQueueStore(context, testPreferencesName())
        var source: ExoPlayer? = null
        var recovered: ExoPlayer? = null
        try {
            instrumentation.runOnMainSync {
                val original = ExoPlayer.Builder(context).build()
                source = original
                original.setMediaItems(
                    listOf(
                        MediaItem.Builder().setMediaId("local-1")
                            .setUri("content://media/external/audio/media/1").build(),
                        MediaItem.Builder().setMediaId("online:old-2")
                            .setUri("https://example.com/expired.mp3").build(),
                        MediaItem.Builder().setMediaId("local-3")
                            .setUri("content://media/external/audio/media/3").build()
                    ),
                    1,
                    8000L
                )
                store.saveFromPlayer(original)
                original.release()
                source = null

                val restored = ExoPlayer.Builder(context).build()
                recovered = restored
                assertTrue(store.restoreInto(restored))
                assertEquals(2, restored.mediaItemCount)
                assertEquals("local-1", restored.getMediaItemAt(0).mediaId)
                assertEquals("local-3", restored.getMediaItemAt(1).mediaId)
                assertEquals(1, restored.currentMediaItemIndex)
                assertEquals(0L, restored.currentPosition)
                assertFalse(restored.playWhenReady)
            }
        } finally {
            instrumentation.runOnMainSync {
                source?.release()
                recovered?.release()
                store.clear()
            }
        }
    }

    @Test
    fun downloadedOfflineTrackWithOldOnlineIdRestoresNormally() {
        val store = PlaybackQueueStore(context, testPreferencesName())
        val file = createWav()
        var original: ExoPlayer? = null
        var restored: ExoPlayer? = null
        try {
            instrumentation.runOnMainSync {
                val first = ExoPlayer.Builder(context).build()
                original = first
                first.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId("online:downloaded-before")
                        .setUri(Uri.fromFile(file))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Nhạc offline đã tải")
                                .build()
                        )
                        .build()
                )
                first.seekTo(4_200L)
                store.saveFromPlayer(first)
                first.release()
                original = null

                val second = ExoPlayer.Builder(context).build()
                restored = second
                assertTrue(store.restoreInto(second))
                assertEquals("online:downloaded-before", second.currentMediaItem?.mediaId)
                assertEquals(Uri.fromFile(file), second.currentMediaItem?.localConfiguration?.uri)
                assertEquals(4_200L, second.currentPosition)
                assertFalse(second.playWhenReady)
            }
        } finally {
            instrumentation.runOnMainSync {
                original?.release()
                restored?.release()
                store.clear()
            }
            file.delete()
        }
    }

    private fun testPreferencesName() =
        "musicflow_queue_qa_${System.nanoTime()}"

    private fun createWav(): File {
        val rate = 8000
        val dataSize = rate * 20 * 2

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

        return File.createTempFile(
            "musicflow_queue_",
            ".wav",
            context.cacheDir
        ).also {
            it.writeBytes(bytes.array())
        }
    }

    @Test
    fun savedQueueRestoresPausedAndResumesPlayback() {
        val name = testPreferencesName()
        val files = List(3) { createWav() }

        val store = PlaybackQueueStore(context, name)
        val playingSignal = CountDownLatch(1)

        var player: ExoPlayer? = null

        try {
            val items = files.mapIndexed { index, file ->
                MediaItem.Builder()
                    .setMediaId("qa-queue-${index + 1}")
                    .setUri(Uri.fromFile(file))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("QA Song ${index + 1}")
                            .setArtist("MusicFlow QA")
                            .build()
                    )
                    .build()
            }

            instrumentation.runOnMainSync {
                val original =
                    ExoPlayer.Builder(context).build()

                player = original

                original.setMediaItems(
                    items,
                    1,
                    4_200L
                )

                original.shuffleModeEnabled = true
                original.repeatMode =
                    Player.REPEAT_MODE_ALL

                store.saveFromPlayer(original)

                original.release()
                player = null

                val restored =
                    ExoPlayer.Builder(context).build()

                player = restored

                assertTrue(
                    PlaybackQueueStore(
                        context,
                        name
                    ).restoreInto(restored)
                )

                assertEquals(
                    3,
                    restored.mediaItemCount
                )

                assertEquals(
                    listOf(
                        "qa-queue-1",
                        "qa-queue-2",
                        "qa-queue-3"
                    ),
                    (0 until restored.mediaItemCount)
                        .map {
                            restored.getMediaItemAt(it)
                                .mediaId
                        }
                )

                assertEquals(
                    1,
                    restored.currentMediaItemIndex
                )

                assertEquals(
                    4_200L,
                    restored.currentPosition
                )

                assertTrue(
                    restored.shuffleModeEnabled
                )

                assertEquals(
                    Player.REPEAT_MODE_ALL,
                    restored.repeatMode
                )

                assertFalse(restored.isPlaying)
                assertFalse(restored.playWhenReady)

                assertEquals(
                    Player.STATE_IDLE,
                    restored.playbackState
                )

                assertEquals(
                    "QA Song 2",
                    restored.currentMediaItem
                        ?.mediaMetadata?.title?.toString()
                )

                Log.i(
                    "MusicFlowQueueTest",
                    "PASS savedQueueRestoresPaused"
                )

                restored.addListener(
                    object : Player.Listener {
                        override fun onEvents(
                            current: Player,
                            events: Player.Events
                        ) {
                            if (
                                current.isPlaying &&
                                current.currentMediaItem
                                    ?.mediaId == "qa-queue-2"
                            ) {
                                playingSignal.countDown()
                            }
                        }
                    }
                )

                restored.prepare()
                restored.play()
            }

            assertTrue(
                "Không tiếp tục được bài đã lưu",
                playingSignal.await(
                    15,
                    TimeUnit.SECONDS
                )
            )

            instrumentation.runOnMainSync {
                val current = checkNotNull(player)

                assertTrue(
                    "Vị trí phát đã bị đặt lại về 0",
                    current.currentPosition >= 3_000L
                )

                assertEquals(
                    "qa-queue-2",
                    current.currentMediaItem?.mediaId
                )
            }

            Log.i(
                "MusicFlowQueueTest",
                "PASS playbackResumesFromSavedPosition"
            )
        } finally {
            player?.let { current ->
                instrumentation.runOnMainSync {
                    current.release()
                }
            }

            store.clear()
            context.deleteSharedPreferences(name)

            files.forEach {
                it.delete()
            }
        }
    }

    @Test
    fun reorderedRoomPlaylistKeepsOrderInSnapshot() =
        runBlocking<Unit> {

            val name = testPreferencesName()
            val store = PlaybackQueueStore(context, name)

            val database = Room.inMemoryDatabaseBuilder(
                context,
                RecentHistoryDatabase::class.java
            ).build()

            var player: ExoPlayer? = null

            try {
                val repository =
                    RoomPlaylistRepository(database)

                val playlist =
                    repository.create("Queue QA")!!

                for (id in 1..3) {
                    assertTrue(
                        repository.addTrack(
                            playlist.id,
                            PlaylistTrack(
                                mediaId = "device-$id",
                                audioUri =
                                    "file:///qa_song_$id.wav",
                                title = "Bài $id",
                                artist = "QA",
                                addedAtMs = id.toLong()
                            )
                        )
                    )
                }

                assertTrue(
                    repository.moveTrack(
                        playlist.id,
                        "device-3",
                        -1
                    )
                )

                assertTrue(
                    repository.moveTrack(
                        playlist.id,
                        "device-3",
                        -1
                    )
                )

                val items = PlaylistMediaItems.fromTracks(
                    repository.getTracks(playlist.id)
                )

                val expected = listOf(
                    "device-3",
                    "device-1",
                    "device-2"
                )

                instrumentation.runOnMainSync {
                    val original =
                        ExoPlayer.Builder(context).build()

                    player = original

                    original.setMediaItems(
                        items,
                        2,
                        0L
                    )

                    store.saveFromPlayer(original)

                    original.release()
                    player = null

                    val restored =
                        ExoPlayer.Builder(context).build()

                    player = restored

                    assertTrue(
                        PlaybackQueueStore(
                            context,
                            name
                        ).restoreInto(restored)
                    )

                    assertEquals(
                        expected,
                        (0 until restored.mediaItemCount)
                            .map {
                                restored.getMediaItemAt(it)
                                    .mediaId
                            }
                    )

                    assertEquals(
                        "device-2",
                        restored.currentMediaItem?.mediaId
                    )

                    assertEquals(
                        2,
                        restored.currentMediaItemIndex
                    )

                    assertFalse(restored.isPlaying)
                }

                Log.i(
                    "MusicFlowQueueTest",
                    "PASS reorderedRoomQueueRestored"
                )
            } finally {
                player?.let { current ->
                    instrumentation.runOnMainSync {
                        current.release()
                    }
                }

                database.close()
                store.clear()
                context.deleteSharedPreferences(name)
            }
        }


    @Test
    fun corruptedSnapshotDoesNotCrashPlayer() {
        val name = testPreferencesName()

        val preferences = context
            .getSharedPreferences(
                name,
                Context.MODE_PRIVATE
            )

        preferences.edit()
            .putString("snapshot", "{invalid-json")
            .commit()

        var player: ExoPlayer? = null

        try {
            instrumentation.runOnMainSync {
                val exo =
                    ExoPlayer.Builder(context).build()

                player = exo

                assertFalse(
                    PlaybackQueueStore(
                        context,
                        name
                    ).restoreInto(exo)
                )

                assertEquals(0, exo.mediaItemCount)
                assertFalse(exo.isPlaying)
            }

            Log.i(
                "MusicFlowQueueTest",
                "PASS corruptedSnapshotIgnored"
            )
        } finally {
            player?.let { current ->
                instrumentation.runOnMainSync {
                    current.release()
                }
            }

            context.deleteSharedPreferences(name)
        }
    }
}

