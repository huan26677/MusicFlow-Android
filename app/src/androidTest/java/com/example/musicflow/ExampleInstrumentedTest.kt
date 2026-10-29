package com.example.musicflow

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

import android.content.ComponentName
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playback.PlaybackController
import com.example.musicflow.core.playback.PlaybackService
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.feature.player.NowPlayingScreen
import com.example.musicflow.feature.player.PlayerViewModel
import com.example.musicflow.feature.search.SearchScreen
import com.example.musicflow.feature.search.SearchUiState
import com.example.musicflow.feature.search.SongSearch
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import android.content.ContentUris
import android.content.ContentValues
import android.provider.MediaStore
import com.example.musicflow.core.media.DeviceMusicScanner

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.musicflow", appContext.packageName)
    }

    private fun createQaWav(): File {
        val sampleRate = 8000
        val dataSize = sampleRate * 20 * 2

        val bytes = ByteBuffer.allocate(44 + dataSize)
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

        val context = InstrumentationRegistry
            .getInstrumentation().targetContext

        return File.createTempFile(
            "musicflow_mvp_",
            ".wav",
            context.cacheDir
        ).also {
            it.writeBytes(bytes.array())
        }
    }

@Test
fun mvpLibrarySearchToRealPlayer() {
    val context = InstrumentationRegistry
        .getInstrumentation().targetContext

    val files = listOf(createQaWav(), createQaWav())
    val baseId = System.currentTimeMillis() % 1_000_000_000L +
        1_000_000_000L

    val songs = files.mapIndexed { index, file ->
        DeviceSong(
            id = baseId + index,
            uri = Uri.fromFile(file),
            title = if (index == 0) "MVP Alpha" else "MVP Beta",
            artist = "MusicFlow QA",
            durationMs = 20_000L
        )
    }

    val controller = PlaybackController(context)
    val playerViewModel = PlayerViewModel(controller)

    val history = RoomRecentHistoryRepository(
        RecentHistoryDatabase.getInstance(context)
    )

    var page by mutableStateOf("library")
    var previousClicks = 0
    var searchState by mutableStateOf(
        SearchUiState(
            songs = songs,
            isLoaded = true
        )
    )

    try {
        composeRule.setContent {
            val playerState by controller.uiState.collectAsState()

            when (page) {
                "library" -> LibraryScreen(
                    uiState = LibraryUiState.Success(songs),
                    hasPermission = true,
                    onBack = {},
                    onPickManual = {},
                    onRefresh = {},
                    onSongSelected = { selected, index ->
                        playerViewModel.playDeviceSongs(
                            selected, index
                        )
                        page = "player"
                    },
                    onAddToPlaylist = {}
                )

                "search" -> SearchScreen(
                    state = searchState,
                    hasPermission = true,
                    onQueryChange = { query ->
                        searchState = searchState.copy(
                            query = query,
                            results = SongSearch.filter(
                                songs, query
                            ),
                            isFiltering = false
                        )
                    },
                    onRequestPermission = {},
                    onRefresh = {},
                    onSongSelected = { selected, index ->
                        playerViewModel.playDeviceSongs(
                            selected, index
                        )
                        page = "player"
                    },
                    onAddToPlaylist = {}
                )

                else -> NowPlayingScreen(
                    state = playerState,
                    onBack = { page = "search" },
                    onPickFiles = {},
                    onOpenLibrary = { page = "library" },
                    onPlayPause = playerViewModel::togglePlayPause,
                    onPrevious = {
                        previousClicks++
                        playerViewModel.previous()
                    },
                    onNext = playerViewModel::next,
                    onSeek = playerViewModel::seekTo,
                    onToggleShuffle =
                        playerViewModel::toggleShuffle,
                    onCycleRepeat =
                        playerViewModel::cycleRepeat,
                    onSetPlaybackSpeed =
                        playerViewModel::setPlaybackSpeed,
                    onSetSleepTimerMinutes =
                        playerViewModel::setSleepTimerMinutes,
                    onSetEqualizerEnabled =
                        playerViewModel::setEqualizerEnabled,
                    onSetEqualizerBand =
                        playerViewModel::setEqualizerBand,
                    onSetCrossfadeDuration =
                        playerViewModel::setCrossfadeDuration,
                    lyrics = com.example.musicflow.core.lyrics.LyricsUiState(),
                    onPickLyrics = {}
                )
            }
        }

        // Library: chọn bài thứ hai và kiểm tra Service phát thật.
        composeRule.onNodeWithText("MVP Beta").performClick()

        composeRule.waitUntil(20_000) {
            val state = controller.uiState.value
            state.isPlaying &&
                state.title == "MVP Beta" &&
                state.currentIndex == 2 &&
                state.queueCount == 2
        }

        Log.i("MusicFlowMvpE2E", "PASS libraryUiToMediaSession")

        // Nút Previous / Next trên giao diện Now Playing.
        composeRule.onNodeWithText("Trước").performScrollTo().performClick()
        assertEquals("Previous button must invoke the real callback", 1, previousClicks)

        composeRule.waitUntil(20_000) {
            val state = controller.uiState.value
            state.isPlaying &&
                state.title == "MVP Alpha" &&
                state.currentIndex == 1
        }

        composeRule.onNodeWithText("Tiếp").performScrollTo().performClick()

        composeRule.waitUntil(20_000) {
            val state = controller.uiState.value
            state.isPlaying &&
                state.title == "MVP Beta" &&
                state.currentIndex == 2
        }

        Log.i("MusicFlowMvpE2E", "PASS nextPreviousFromUi")

        // Pause / Play phải thay đổi trạng thái phát thật.
        composeRule.onNodeWithText("Tạm dừng").performScrollTo().performClick()

        composeRule.waitUntil(20_000) {
            !controller.uiState.value.isPlaying
        }

        composeRule.onNodeWithText("Phát").performScrollTo().performClick()

        composeRule.waitUntil(20_000) {
            controller.uiState.value.isPlaying
        }

        Log.i("MusicFlowMvpE2E", "PASS pauseResumeFromUi")

        // Đi đến Search và tìm bài Alpha.
        composeRule.onNodeWithText("‹ Trở về").performScrollTo().performClick()

        composeRule.onNode(hasSetTextAction())
            .performTextInput("Alpha")

        composeRule.onNodeWithText("MVP Alpha").performClick()

        composeRule.waitUntil(20_000) {
            val state = controller.uiState.value
            state.isPlaying &&
                state.title == "MVP Alpha" &&
                state.currentIndex == 1 &&
                state.queueCount == 1
        }

        Log.i("MusicFlowMvpE2E", "PASS searchUiToMediaSession")

        // PlaybackService thực sự ghi lịch sử vào Room.
        runBlocking {
            withTimeout(15_000) {
                while (
                    history.getRecent().none {
                        it.mediaId == "device-${songs[0].id}"
                    }
                ) {
                    delay(100)
                }
            }
        }

        Log.i("MusicFlowMvpE2E", "PASS uiPlaybackWritesHistory")
    } finally {
        var cleanupFuture: ListenableFuture<MediaController>? = null

        try {
            val token = SessionToken(
                context,
                ComponentName(
                    context,
                    PlaybackService::class.java
                )
            )

            cleanupFuture = MediaController.Builder(
                context, token
            ).buildAsync()

            val media = cleanupFuture.get(10, TimeUnit.SECONDS)

            composeRule.runOnUiThread {
                media.pause()
                media.stop()
                media.clearMediaItems()
            }
        } finally {
            composeRule.runOnUiThread {
                cleanupFuture?.let {
                    MediaController.releaseFuture(it)
                }
                controller.release()
            }

            runBlocking {
                songs.forEach {
                    history.remove("device-${it.id}")
                }
            }

            files.forEach { it.delete() }
        }
    }
}
@Test
fun mvpRealMediaStoreToPlayer() {
    val context = InstrumentationRegistry
        .getInstrumentation().targetContext
    val resolver = context.contentResolver
    val source = createQaWav()

    val values = ContentValues().apply {
        put(
            MediaStore.Audio.Media.DISPLAY_NAME,
            "musicflow_qa_${System.nanoTime()}.wav"
        )
        put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
        put(
            MediaStore.Audio.Media.RELATIVE_PATH,
            "Music/MusicFlowQA/"
        )
        put(MediaStore.Audio.Media.IS_MUSIC, 1)
        put(MediaStore.Audio.Media.IS_PENDING, 1)
    }

    var uri: Uri? = null
    var playback: PlaybackController? = null

    try {
        val mediaUri = checkNotNull(
            resolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                values
            )
        )
        uri = mediaUri

        resolver.openOutputStream(mediaUri, "w").use { output ->
            checkNotNull(output)
            source.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        val publish = ContentValues().apply {
            put(MediaStore.Audio.Media.IS_PENDING, 0)
        }
        assertEquals(1, resolver.update(mediaUri, publish, null, null))

        var scanned: DeviceSong? = null

        runBlocking {
            withTimeout(15000) {
                while (scanned == null) {
                    scanned = DeviceMusicScanner.scan(context)
                        .firstOrNull { it.uri == mediaUri }
                    if (scanned == null) delay(200)
                }
            }
        }

        val song = checkNotNull(scanned)
        assertEquals(ContentUris.parseId(mediaUri), song.id)

        Log.i("MusicFlowMvpE2E", "PASS realMediaStoreScan")

        val controller = PlaybackController(context)
        playback = controller
        val vm = PlayerViewModel(controller)

        composeRule.setContent {
            LibraryScreen(
                uiState = LibraryUiState.Success(listOf(song)),
                hasPermission = true,
                onBack = {},
                onPickManual = {},
                onRefresh = {},
                onSongSelected = { songs, index ->
                    vm.playDeviceSongs(songs, index)
                },
                onAddToPlaylist = {}
            )
        }

        composeRule.onNodeWithText(song.title).performClick()

        composeRule.waitUntil(20_000) {
            val state = controller.uiState.value
            state.isPlaying &&
                state.title == song.title &&
                state.queueCount == 1 &&
                state.currentIndex == 1
        }

        Log.i("MusicFlowMvpE2E", "PASS realMediaStoreUiPlayback")
    } finally {
        try {
            if (playback != null) {
                val token = SessionToken(
                    context,
                    ComponentName(context, PlaybackService::class.java)
                )
                val future = MediaController.Builder(
                    context, token
                ).buildAsync()

                try {
                    val media = future.get(15, TimeUnit.SECONDS)
                    composeRule.runOnUiThread {
                        media.pause()
                        media.stop()
                        media.clearMediaItems()
                    }
                } finally {
                    composeRule.runOnUiThread {
                        MediaController.releaseFuture(future)
                    }
                }
            }
        } finally {
            composeRule.runOnUiThread {
                playback?.release()
            }
            uri?.let { resolver.delete(it, null, null) }
            source.delete()
        }
    }
}
}
