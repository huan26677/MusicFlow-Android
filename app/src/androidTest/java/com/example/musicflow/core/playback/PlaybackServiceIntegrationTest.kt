package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.ContentValues
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat

import androidx.lifecycle.Lifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.musicflow.MainActivity
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.PlaylistMediaItems
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import com.google.common.util.concurrent.ListenableFuture

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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PlaybackServiceIntegrationTest {

    private val instrumentation =
        InstrumentationRegistry.getInstrumentation()

    private val context = instrumentation.targetContext

    private fun createWav(): File {
        val rate = 8000
        val dataSize = rate * 30 * 2

        val bytes = ByteBuffer
            .allocate(44 + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)

        fun writeText(value: String) {
            bytes.put(value.toByteArray(Charsets.US_ASCII))
        }

        writeText("RIFF")
        bytes.putInt(36 + dataSize)
        writeText("WAVE")
        writeText("fmt ")
        bytes.putInt(16)
        bytes.putShort(1.toShort())
        bytes.putShort(1.toShort())
        bytes.putInt(rate)
        bytes.putInt(rate * 2)
        bytes.putShort(2.toShort())
        bytes.putShort(16.toShort())
        writeText("data")
        bytes.putInt(dataSize)

        return File.createTempFile(
            "musicflow_service_",
            ".wav",
            context.cacheDir
        ).also {
            it.writeBytes(bytes.array())
        }
    }

    @Test
    fun playbackServiceIntegration() = runBlocking<Unit> {
        val runId = System.nanoTime().toString()

        val files = (1..3).associateWith {
            createWav()
        }

        val testDatabase = Room.inMemoryDatabaseBuilder(
            context,
            RecentHistoryDatabase::class.java
        ).build()

        val history = RoomRecentHistoryRepository(
            RecentHistoryDatabase.getInstance(context)
        )

        val oldTestIds = history.getRecent()
            .map { it.mediaId }
            .filter { it.startsWith("qa-service-") }

        for (mediaId in oldTestIds) {
            history.remove(mediaId)
        }

        val previousHistory = history.getRecent()

        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null
        var activity: MainActivity? = null

        val firstPlaying = CountDownLatch(1)
        val nextPlaying = CountDownLatch(1)
        val previousPlaying = CountDownLatch(1)

        val phase = AtomicInteger(0)
        val playbackError =
            AtomicReference<String?>(null)

        try {
            val playlists = RoomPlaylistRepository(
                testDatabase
            )

            val playlist = playlists.create(
                "Service Integration Test"
            )!!

            val tracks = (1..3).map { id ->
                PlaylistTrack(
                    mediaId = "qa-service-$runId-$id",
                    audioUri = Uri.fromFile(
                        files.getValue(id)
                    ).toString(),
                    title = "QA Track $id",
                    artist = "MusicFlow Test",
                    addedAtMs = id.toLong()
                )
            }

            for (track in tracks) {
                assertTrue(
                    playlists.addTrack(
                        playlist.id,
                        track
                    )
                )
            }

            assertTrue(
                playlists.moveTrack(
                    playlist.id,
                    tracks[2].mediaId,
                    -1
                )
            )

            assertTrue(
                playlists.moveTrack(
                    playlist.id,
                    tracks[2].mediaId,
                    -1
                )
            )

            val expectedIds = listOf(
                tracks[2].mediaId,
                tracks[0].mediaId,
                tracks[1].mediaId
            )

            val ordered = playlists.getTracks(
                playlist.id
            )

            assertEquals(
                expectedIds,
                ordered.map { it.mediaId }
            )

            val items = PlaylistMediaItems.fromTracks(
                ordered
            )

            Log.i(
                "MusicFlowServiceIT",
                "PASS roomReorderToQueue"
            )

            val intent = Intent(
                context,
                MainActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            activity = instrumentation.startActivitySync(
                intent
            ) as MainActivity

            instrumentation.waitForIdleSync()

            val token = SessionToken(
                context,
                ComponentName(
                    context,
                    PlaybackService::class.java
                )
            )

            val connecting = MediaController.Builder(
                context,
                token
            ).buildAsync()

            future = connecting

            val mediaController = connecting.get(
                20,
                TimeUnit.SECONDS
            )

            controller = mediaController

            Log.i(
                "MusicFlowServiceIT",
                "PASS mediaControllerConnected"
            )

            instrumentation.runOnMainSync {
                mediaController.addListener(
                    object : Player.Listener {

                        override fun onPlayerError(
                            error: PlaybackException
                        ) {
                            playbackError.set(
                                error.errorCodeName
                            )
                        }

                        override fun onEvents(
                            player: Player,
                            events: Player.Events
                        ) {
                            if (!player.isPlaying) return

                            val id = player.currentMediaItem
                                ?.mediaId

                            when (phase.get()) {
                                0 -> {
                                    if (id == expectedIds[0]) {
                                        firstPlaying.countDown()
                                    }
                                }

                                1 -> {
                                    if (id == expectedIds[1]) {
                                        nextPlaying.countDown()
                                    }
                                }

                                2 -> {
                                    if (id == expectedIds[0]) {
                                        previousPlaying.countDown()
                                    }
                                }
                            }
                        }
                    }
                )

                mediaController.shuffleModeEnabled = false
                mediaController.repeatMode =
                    Player.REPEAT_MODE_OFF

                mediaController.setMediaItems(
                    items,
                    0,
                    0L
                )

                assertEquals(
                    3,
                    mediaController.mediaItemCount
                )

                mediaController.prepare()
                mediaController.play()
            }

            assertTrue(
                "Service không phát bài đầu: " +
                    playbackError.get(),
                firstPlaying.await(
                    15,
                    TimeUnit.SECONDS
                )
            )

            Log.i(
                "MusicFlowServiceIT",
                "PASS servicePlaysFirstTrack"
            )

            withTimeout(15_000) {
                while (true) {
                    val raw = context.getSharedPreferences(
                        "musicflow_playback_queue_v1",
                        Context.MODE_PRIVATE
                    ).getString("snapshot", null)

                    val correctlySaved = runCatching {
                        val saved = JSONObject(checkNotNull(raw))
                        val tracks = saved.getJSONArray("items")

                        val ids = (0 until tracks.length()).map {
                            tracks.getJSONObject(it)
                                .getString("mediaId")
                        }

                        ids == expectedIds &&
                            saved.getInt("index") == 0
                    }.getOrDefault(false)

                    if (correctlySaved) break

                    delay(100)
                }
            }

            Log.i(
                "MusicFlowServiceIT",
                "PASS servicePersistsPlaybackQueue"
            )

            withTimeout(15_000) {
                while (
                    history.getRecent().none {
                        it.mediaId == expectedIds[0]
                    }
                ) {
                    delay(100)
                }
            }

            Log.i(
                "MusicFlowServiceIT",
                "PASS serviceWritesRecentlyPlayed"
            )

            ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation
                    .executeShellCommand(
                        "input keyevent KEYCODE_HOME"
                    )
            ).use { stream ->
                while (stream.read() != -1) {
                }
            }

            instrumentation.waitForIdleSync()
            delay(1000)

            var activityIsBackground = false
            var audioStillPlaying = false

            instrumentation.runOnMainSync {
                activityIsBackground =
                    !checkNotNull(activity)
                        .lifecycle
                        .currentState
                        .isAtLeast(Lifecycle.State.STARTED)

                audioStillPlaying =
                    mediaController.isPlaying &&
                    mediaController.currentMediaItem
                        ?.mediaId == expectedIds[0]
            }

            assertTrue(
                "Activity chưa chuyển về nền",
                activityIsBackground
            )

            assertTrue(
                "Nhạc dừng sau khi nhấn Home",
                audioStillPlaying
            )

            Log.i(
                "MusicFlowServiceIT",
                "PASS backgroundPlaybackAfterHome"
            )


            phase.set(1)

            instrumentation.runOnMainSync {
                mediaController.seekToNextMediaItem()
            }

            assertTrue(
                "Next lỗi: ${playbackError.get()}",
                nextPlaying.await(
                    15,
                    TimeUnit.SECONDS
                )
            )

            Log.i(
                "MusicFlowServiceIT",
                "PASS nextThroughMediaSession"
            )

            withTimeout(15_000) {
                while (
                    history.getRecent().none {
                        it.mediaId == expectedIds[1]
                    }
                ) {
                    delay(100)
                }
            }

            Log.i(
                "MusicFlowServiceIT",
                "PASS nextTrackRecordedInHistory"
            )

            phase.set(2)

            instrumentation.runOnMainSync {
                mediaController.seekToPreviousMediaItem()
            }

            assertTrue(
                "Previous lỗi: ${playbackError.get()}",
                previousPlaying.await(
                    15,
                    TimeUnit.SECONDS
                )
            )

            Log.i(
                "MusicFlowServiceIT",
                "PASS previousThroughMediaSession"
            )

            withTimeout(15_000) {
                while (
                    history.getRecent()
                        .firstOrNull()
                        ?.mediaId != expectedIds[0]
                ) {
                    delay(100)
                }
            }

            assertEquals(
                expectedIds,
                playlists.getTracks(playlist.id)
                    .map { it.mediaId }
            )

            Log.i(
                "MusicFlowServiceIT",
                "PASS playlistOrderUnchanged"
            )

            Log.i(
                "MusicFlowServiceIT",
                "PASS playbackServiceIntegration"
            )

        } finally {
            controller?.let { current ->
                instrumentation.runOnMainSync {
                    current.pause()
                    current.stop()
                    current.clearMediaItems()
                }
            }

            future?.let { connecting ->
                instrumentation.runOnMainSync {
                    MediaController.releaseFuture(connecting)
                }
            }

            activity?.let { screen ->
                instrumentation.runOnMainSync {
                    screen.finish()
                }
            }

            try {
                history.clear()

                for (song in previousHistory.asReversed()) {
                    history.record(song)
                }

                assertEquals(
                    previousHistory,
                    history.getRecent()
                )

                Log.i(
                    "MusicFlowServiceIT",
                    "PASS historyRestored"
                )
            } finally {
                testDatabase.close()

                files.values.forEach {
                    it.delete()
                }
            }
        }
    }
    @Test
    fun mvpPickerResultStartsRealPlayback() {
        val resolver = context.contentResolver
        val source = createWav()
        val name = "musicflow_picker_${System.nanoTime()}.wav"

        var audioUri: Uri? = null
        var monitor: Instrumentation.ActivityMonitor? = null
        var activity: MainActivity? = null
        var future: ListenableFuture<MediaController>? = null
        var controller: MediaController? = null

        try {
            val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(
                    context, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand(
                        "pm grant ${context.packageName} $permission"
                    )
                ).use { it.readBytes() }
            }

            assertEquals(
                PackageManager.PERMISSION_GRANTED,
                ContextCompat.checkSelfPermission(context, permission)
            )

            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MusicFlowQA/")
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val uri = checkNotNull(
                resolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    values
                )
            )
            audioUri = uri

            resolver.openOutputStream(uri, "w").use { output ->
                checkNotNull(output)
                source.inputStream().use { it.copyTo(output) }
            }

            resolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                },
                null,
                null
            )

            val pickerResult = Intent().apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            monitor = instrumentation.addMonitor(
                IntentFilter(Intent.ACTION_OPEN_DOCUMENT),
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    pickerResult
                ),
                true
            )

            val activityMonitor = instrumentation.addMonitor(
                MainActivity::class.java.name,
                null,
                false
            )

            try {
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )
                )

                activity = instrumentation.waitForMonitorWithTimeout(
                    activityMonitor,
                    15_000L
                ) as? MainActivity

                assertNotNull("MainActivity không khởi động", activity)
            } finally {
                instrumentation.removeMonitor(activityMonitor)
            }

            fun clickText(text: String) {
                val deadline = SystemClock.uptimeMillis() + 15000

                while (SystemClock.uptimeMillis() < deadline) {
                    val nodes = instrumentation.uiAutomation
                        .rootInActiveWindow
                        ?.findAccessibilityNodeInfosByText(text)
                        .orEmpty()

                    for (node in nodes) {
                        var current: AccessibilityNodeInfo? = node

                        while (current != null && !current.isClickable) {
                            current = current.parent
                        }

                        if (current?.performAction(
                                AccessibilityNodeInfo.ACTION_CLICK
                            ) == true
                        ) return
                    }

                    SystemClock.sleep(200)
                }

                throw AssertionError("Không tìm thấy nút: $text")
            }

            clickText("Mở thư viện thiết bị")
            clickText("Chọn MP3")

            assertTrue(
                "MainActivity chưa mở ACTION_OPEN_DOCUMENT",
                checkNotNull(monitor).hits > 0
            )
            Log.i("MusicFlowMvpE2E", "PASS pickerIntentIntercepted")

        val token = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        future = MediaController.Builder(context, token).buildAsync()
        val media = future.get(20, TimeUnit.SECONDS)
        controller = media

        val deadline = SystemClock.uptimeMillis() + 20000
        var playingCorrectFile = false

        while (
            !playingCorrectFile &&
            SystemClock.uptimeMillis() < deadline
        ) {
            instrumentation.runOnMainSync {
                playingCorrectFile =
                    media.isPlaying &&
                    media.mediaItemCount == 1 &&
                    media.currentMediaItem?.mediaId == uri.toString() &&
                    media.currentMediaItem?.mediaMetadata
                        ?.title?.toString() == name
            }

            if (!playingCorrectFile) SystemClock.sleep(150)
        }

        assertTrue(
            "File được chọn chưa phát đúng qua MediaSession",
            playingCorrectFile
        )
        Log.i("MusicFlowMvpE2E", "PASS pickerResultPlaysThroughService")

    } finally {
        controller?.let { media ->
            instrumentation.runOnMainSync {
                media.pause()
                media.stop()
                media.clearMediaItems()
            }
        }

        future?.let { connecting ->
            instrumentation.runOnMainSync {
                MediaController.releaseFuture(connecting)
            }
        }

        activity?.let { screen ->
            instrumentation.runOnMainSync { screen.finish() }
        }

        monitor?.let { instrumentation.removeMonitor(it) }

        runBlocking {
            audioUri?.let {
                RoomRecentHistoryRepository(
                    RecentHistoryDatabase.getInstance(context)
                ).remove(it.toString())
            }
        }

        audioUri?.let { resolver.delete(it, null, null) }
        source.delete()
    }
}
}

