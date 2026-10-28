package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.util.Log

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.musicflow.MainActivity
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.PlaylistMediaItems
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import com.google.common.util.concurrent.ListenableFuture

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class PlaybackProcessRestartTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val context = instrumentation.targetContext

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val queuePrefs = context.getSharedPreferences(
        "musicflow_playback_queue_v1",
        Context.MODE_PRIVATE
    )

    private val qaPrefs = context.getSharedPreferences(
        "musicflow_restart_qa_v1",
        Context.MODE_PRIVATE
    )

    private val ids = listOf(
        "qa-restart-track-1",
        "qa-restart-track-2"
    )

    private fun enabled(phase: String): Boolean =
        InstrumentationRegistry.getArguments()
            .getString("musicflow_restart_phase") == phase

    private fun file(index: Int): File =
        File(
            context.filesDir,
            "musicflow_restart_qa_$index.wav"
        )

    private fun writeWav(target: File) {
        val sampleRate = 8000
        val dataSize = sampleRate * 60 * 2

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

        target.writeBytes(buffer.array())
    }

    private fun openApp() {
        val intent = Intent(
            context,
            MainActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        instrumentation.startActivitySync(intent)
        instrumentation.waitForIdleSync()
    }

    private fun connect():
        Pair<ListenableFuture<MediaController>, MediaController> {

        val token = SessionToken(
            context,
            ComponentName(
                context,
                PlaybackService::class.java
            )
        )

        val future = MediaController.Builder(
            context,
            token
        ).buildAsync()

        return future to future.get(
            20,
            TimeUnit.SECONDS
        )
    }

    private fun release(
        future: ListenableFuture<MediaController>?
    ) {
        if (future == null) return

        instrumentation.runOnMainSync {
            MediaController.releaseFuture(future)
        }
    }

    @Test
    fun prepareBeforeRestart() = runBlocking<Unit> {
        if (!enabled("prepare")) return@runBlocking

        assertFalse(
            "Lần QA trước chưa được dọn dẹp",
            qaPrefs.getBoolean("armed", false)
        )

        val original = queuePrefs.getString(
            "snapshot",
            null
        )

        val backup = qaPrefs.edit()
            .putBoolean("armed", true)
            .putBoolean(
                "originalPresent",
                original != null
            )

        if (original != null) {
            backup.putString(
                "originalSnapshot",
                original
            )
        }

        assertTrue(
            "Không sao lưu được snapshot",
            backup.commit()
        )

        for (index in 1..2) {
            writeWav(file(index))
        }

        val playlists = RoomPlaylistRepository(
            RecentHistoryDatabase.getInstance(context)
        )

        val playlist = checkNotNull(
            playlists.create("Restart Playlist QA")
        )

        assertTrue(
            qaPrefs.edit()
                .putString("playlistId", playlist.id)
                .commit()
        )

        for (index in 2 downTo 1) {
            assertTrue(
                playlists.addTrack(
                    playlist.id,
                    PlaylistTrack(
                        mediaId = ids[index - 1],
                        audioUri = Uri.fromFile(file(index)).toString(),
                        title = "Restart QA $index",
                        artist = "MusicFlow QA",
                        addedAtMs = index.toLong()
                    )
                )
            )
        }

        assertTrue(playlists.moveTrack(playlist.id, ids[0], -1))

        val orderedTracks = playlists.getTracks(playlist.id)
        assertEquals(ids, orderedTracks.map { it.mediaId })

        Log.i("MusicFlowRestartIT", "PASS playlistPrepared")

        var future: ListenableFuture<MediaController>? = null

        try {
            openApp()

            val (connection, controller) = connect()
            future = connection

            val items = PlaylistMediaItems.fromTracks(orderedTracks)

            val playing = CountDownLatch(1)

            instrumentation.runOnMainSync {
                controller.addListener(
                    object : Player.Listener {
                        override fun onEvents(
                            player: Player,
                            events: Player.Events
                        ) {
                            if (
                                player.isPlaying &&
                                player.currentMediaItem
                                    ?.mediaId == ids[1]
                            ) {
                                playing.countDown()
                            }
                        }
                    }
                )

                controller.shuffleModeEnabled = false
                controller.repeatMode =
                    Player.REPEAT_MODE_OFF

                controller.setMediaItems(
                    items,
                    1,
                    7_500L
                )

                controller.prepare()
                controller.play()
            }

            assertTrue(
                "Service chưa phát bài QA",
                playing.await(
                    15,
                    TimeUnit.SECONDS
                )
            )

            withTimeout(15_000) {
                while (true) {
                    val saved = runCatching {
                        JSONObject(
                            checkNotNull(
                                queuePrefs.getString(
                                    "snapshot",
                                    null
                                )
                            )
                        )
                    }.getOrNull()

                    val correct = saved?.let {
                        val array = it.getJSONArray("items")

                        val savedIds =
                            (0 until array.length()).map { n ->
                                array.getJSONObject(n)
                                    .getString("mediaId")
                            }

                        savedIds == ids &&
                            it.getInt("index") == 1 &&
                            it.getLong("positionMs") >= 7_000L
                    } ?: false

                    if (correct) break

                    delay(100)
                }
            }

            delay(1000)

val history = RoomRecentHistoryRepository(
    RecentHistoryDatabase.getInstance(context)
)

withTimeout(15_000L) {
    while (true) {
        val entries = history.getRecent()
            .filter { it.mediaId in ids }

        val correct =
            entries.size == 1 &&
                entries[0].mediaId == ids[1] &&
                entries[0].audioUri ==
                    Uri.fromFile(file(2)).toString() &&
                entries[0].title == "Restart QA 2"

        if (correct) break
        delay(100)
    }
}

Log.i(
    "MusicFlowRestartIT",
    "PASS historyPreparedBeforeRestart"
)

            Log.i(
                "MusicFlowRestartIT",
                "PREPARE_PROCESS_PID=${Process.myPid()}"
            )

            Log.i(
                "MusicFlowRestartIT",
                "PHASE_PREPARE_PASS"
            )
        } finally {
            release(future)
        }
    }


    @Test
    fun verifyAfterRestart() = runBlocking<Unit> {
        if (!enabled("verify")) return@runBlocking

        assertTrue(
            "Không có dữ liệu từ pha chuẩn bị",
            qaPrefs.getBoolean("armed", false)
        )

        Log.i(
            "MusicFlowRestartIT",
            "VERIFY_PROCESS_PID=${Process.myPid()}"
        )

        var future: ListenableFuture<MediaController>? = null

        try {
            openApp()

            val (connection, controller) = connect()
            future = connection

            val playlistId = checkNotNull(
                qaPrefs.getString("playlistId", null)
            )

            val tracks = RoomPlaylistRepository(
                RecentHistoryDatabase.getInstance(context)
            ).getTracks(playlistId)

            assertEquals(ids, tracks.map { it.mediaId })

            Log.i("MusicFlowRestartIT", "PASS playlistPersistedAfterRestart")

val recoveredHistory = RoomRecentHistoryRepository(
    RecentHistoryDatabase.getInstance(context)
).getRecent().filter { it.mediaId in ids }

assertEquals(
    listOf(ids[1]),
    recoveredHistory.map { it.mediaId }
)

assertEquals(
    Uri.fromFile(file(2)).toString(),
    recoveredHistory.single().audioUri
)

assertEquals(
    "Restart QA 2",
    recoveredHistory.single().title
)

Log.i(
    "MusicFlowRestartIT",
    "PASS historyPersistedAfterRestart"
)

            instrumentation.runOnMainSync {
                assertEquals(
                    2,
                    controller.mediaItemCount
                )

                val restoredIds =
                    (0 until controller.mediaItemCount)
                        .map {
                            controller.getMediaItemAt(it)
                                .mediaId
                        }

                assertEquals(ids, restoredIds)

                assertEquals(
                    1,
                    controller.currentMediaItemIndex
                )

                assertEquals(
                    ids[1],
                    controller.currentMediaItem?.mediaId
                )

                assertEquals(
                    "Restart QA 2",
                    controller.currentMediaItem
                        ?.mediaMetadata
                        ?.title
                        ?.toString()
                )

                assertTrue(
                    "Vị trí phát bị mất",
                    controller.currentPosition >= 7_000L
                )

                assertFalse(
                    "Không được tự phát sau restart",
                    controller.isPlaying
                )

                assertFalse(
                    "playWhenReady phải bằng false",
                    controller.playWhenReady
                )

                assertEquals(
                    Player.STATE_IDLE,
                    controller.playbackState
                )
            }

            Log.i(
                "MusicFlowRestartIT",
                "PHASE_VERIFY_PAUSED_PASS"
            )

// 1. Xác nhận History hiển thị sau restart.
composeRule.waitUntil(15_000L) {
    composeRule.onAllNodesWithText("Nghe gần đây")
        .fetchSemanticsNodes()
        .isNotEmpty()
}

composeRule.onNodeWithText("Nghe gần đây")
    .performScrollTo()

val historyTag = "history-song-${ids[1]}"

composeRule.waitUntil(15_000L) {
    composeRule.onAllNodesWithTag(historyTag)
        .fetchSemanticsNodes()
        .isNotEmpty()
}

composeRule.onNodeWithTag(historyTag)
    .performScrollTo()
    .assertExists()

Log.i(
    "MusicFlowRestartIT",
    "PASS historyUiAfterRestart"
)

// 2. Mở Playlist từ Home sau restart.
composeRule.onNodeWithText("Danh sách phát của tôi")
    .performScrollTo()
    .performClick()

val playlistTag = "playlist-$playlistId"

composeRule.waitUntil(15_000L) {
    composeRule.onAllNodesWithTag(playlistTag)
        .fetchSemanticsNodes()
        .isNotEmpty()
}

composeRule.onNodeWithTag(playlistTag)
    .performClick()

// 3. Xác nhận cả hai bài hát có trên giao diện.
val firstTrackTag = "playlist-track-${ids[0]}"
val secondTrackTag = "playlist-track-${ids[1]}"

composeRule.waitUntil(15_000L) {
    composeRule.onAllNodesWithTag(firstTrackTag)
        .fetchSemanticsNodes()
        .isNotEmpty() &&
    composeRule.onAllNodesWithTag(secondTrackTag)
        .fetchSemanticsNodes()
        .isNotEmpty()
}

composeRule.onNodeWithText("2 bài hát")
    .assertExists()

composeRule.onNodeWithTag(firstTrackTag)
    .assertExists()

composeRule.onNodeWithTag(secondTrackTag)
    .assertExists()

Log.i(
    "MusicFlowRestartIT",
    "PASS playlistUiAfterRestart"
)

// 4. Chỉ xem giao diện không được tự phát nhạc.
instrumentation.runOnMainSync {
    assertFalse(controller.isPlaying)
    assertFalse(controller.playWhenReady)
}

Log.i(
    "MusicFlowRestartIT",
    "PASS remainsPausedAfterUiNavigation"
)

            val resumed = CountDownLatch(1)

            instrumentation.runOnMainSync {
                controller.addListener(
                    object : Player.Listener {
                        override fun onEvents(
                            player: Player,
                            events: Player.Events
                        ) {
                            if (
                                player.isPlaying &&
                                player.currentMediaItem
                                    ?.mediaId == ids[1]
                            ) {
                                resumed.countDown()
                            }
                        }
                    }
                )
            }

            instrumentation.runOnMainSync {
                controller.prepare()
                controller.play()
            }

            assertTrue(
                "Không tiếp tục phát được sau restart",
                resumed.await(
                    15,
                    TimeUnit.SECONDS
                )
            )

            instrumentation.runOnMainSync {
                assertTrue(
                    "Phát lại sai vị trí",
                    controller.currentPosition >= 6_500L
                )
            }

            Log.i(
                "MusicFlowRestartIT",
                "PHASE_VERIFY_PASS"
            )
        } finally {
            release(future)
        }
    }


    @Test
    fun cleanupAfterRestart() = runBlocking<Unit> {
        if (!enabled("cleanup")) return@runBlocking

        if (qaPrefs.getBoolean("armed", false)) {
            val editor = queuePrefs.edit()

            if (
                qaPrefs.getBoolean(
                    "originalPresent",
                    false
                )
            ) {
                editor.putString(
                    "snapshot",
                    qaPrefs.getString(
                        "originalSnapshot",
                        null
                    )
                )
            } else {
                editor.remove("snapshot")
            }

            assertTrue(
                "Không khôi phục được snapshot gốc",
                editor.commit()
            )

            val history = RoomRecentHistoryRepository(
                RecentHistoryDatabase.getInstance(
                    context
                )
            )

            for (id in ids) {
                history.remove(id)
            }

assertTrue(
    history.getRecent().none { it.mediaId in ids }
)

Log.i(
    "MusicFlowRestartIT",
    "PASS historyCleanedAfterRestart"
)
        }

        for (index in 1..2) {
            file(index).delete()
        }

        qaPrefs.getString("playlistId", null)?.let { playlistId ->
            val playlists = RoomPlaylistRepository(
                RecentHistoryDatabase.getInstance(context)
            )
            playlists.deletePlaylist(playlistId)
            assertNull(playlists.getPlaylist(playlistId))
        }

        qaPrefs.edit().clear().commit()

        Log.i(
            "MusicFlowRestartIT",
            "PHASE_CLEANUP_PASS"
        )
    }
}

