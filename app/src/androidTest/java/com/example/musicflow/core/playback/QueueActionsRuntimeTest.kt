package com.example.musicflow.core.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.TrackMediaItems
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.feature.player.PlayerViewModel
import com.example.musicflow.feature.search.SearchScreen
import com.example.musicflow.feature.search.SearchUiState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Actual Library/Search composables, ViewModel and MediaSession queue on API 30. */
@RunWith(AndroidJUnit4::class)
class QueueActionsRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val preferences = context.getSharedPreferences(
        "musicflow_playback_queue_v1", Context.MODE_PRIVATE
    )

    private data class Original(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long,
        val playWhenReady: Boolean,
        val shuffle: Boolean,
        val repeat: Int,
        val speed: Float
    )

    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun wav(prefix: String): File {
        val sampleRate = 8000
        val samples = 60 * sampleRate
        val buffer = ByteBuffer.allocate(44 + samples * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun text(s: String) { buffer.put(s.toByteArray(Charsets.US_ASCII)) }
        text("RIFF"); buffer.putInt(36 + samples * 2)
        text("WAVE"); text("fmt "); buffer.putInt(16)
        buffer.putShort(1.toShort()); buffer.putShort(1.toShort())
        buffer.putInt(sampleRate); buffer.putInt(sampleRate * 2)
        buffer.putShort(2.toShort()); buffer.putShort(16.toShort())
        text("data"); buffer.putInt(samples * 2)
        return File.createTempFile(prefix, ".wav", context.cacheDir)
            .apply { writeBytes(buffer.array()) }
    }

    private fun waitFor(label: String, check: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 15_000L
        while (SystemClock.uptimeMillis() < end) {
            var matched = false
            main { matched = check() }
            if (matched) return
            SystemClock.sleep(100L)
        }
        throw AssertionError("Timed out: $label")
    }

    private fun queueIds(media: MediaController): List<String> {
        var ids = emptyList<String>()
        main { ids = (0 until media.mediaItemCount).map { media.getMediaItemAt(it).mediaId } }
        return ids
    }

    private fun assertPlayingWithoutReset(
        media: MediaController, currentId: String, positionBefore: Long
    ) {
        main {
            assertTrue("Queue edit paused the current offline WAV", media.isPlaying)
            assertEquals(currentId, media.currentMediaItem?.mediaId)
            assertTrue("Queue edit reset playback position",
                media.currentPosition >= positionBefore - 250L)
        }
    }

    private fun withQueue(test: (
        media: MediaController, api: PlaybackController, vm: PlayerViewModel,
        songs: List<DeviceSong>
    ) -> Unit) {
        val files = (0..3).map { wav("musicflow_queue_action_${it}_") }
        val baseId = 1_500_000_000L + (System.nanoTime() % 100_000_000L)
        val songs = files.mapIndexed { i, file ->
            DeviceSong(
                id = baseId + i,
                uri = Uri.fromFile(file),
                title = listOf("Queue Alpha", "Queue Omega",
                    "Queue Next", "Queue End")[i],
                artist = "MusicFlow QA",
                durationMs = 60_000L
            )
        }
        val future = MediaController.Builder(
            context, SessionToken(context, ComponentName(
                context, PlaybackService::class.java
            ))
        ).buildAsync()
        var media: MediaController? = null
        var api: PlaybackController? = null
        var original: Original? = null
        val savedSnapshot = preferences.getString("snapshot", null)
        try {
            main { api = PlaybackController(context) }
            media = future.get(20, TimeUnit.SECONDS)
            val player = media
            val controller = checkNotNull(api)
            main {
                original = Original(
                    (0 until player.mediaItemCount).map(player::getMediaItemAt),
                    player.currentMediaItemIndex,
                    player.currentPosition.coerceAtLeast(0L),
                    player.playWhenReady,
                    player.shuffleModeEnabled,
                    player.repeatMode,
                    player.playbackParameters.speed
                )
                player.pause()
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.setPlaybackSpeed(1f)
                player.setMediaItems(songs.take(2).map(TrackMediaItems::fromDevice),
                    0, 5_000L)
                player.prepare()
                player.play()
            }
            val initialIds = songs.take(2).map { TrackMediaItems.fromDevice(it).mediaId }
            waitFor("queue API connected and two offline WAVs playing") {
                player.isPlaying && player.currentMediaItemIndex == 0 &&
                    controller.uiState.value.connected &&
                    controller.uiState.value.queueItems.map { it.mediaId } == initialIds
            }
            Log.i("MusicFlowQueueActionsQA", "PASS offlineQueueReady")
            test(player, controller, PlayerViewModel(controller), songs)
        } finally {
            media?.let { player ->
                original?.let { saved ->
                    main {
                        player.pause()
                        if (saved.items.isEmpty()) player.clearMediaItems()
                        else {
                            player.setMediaItems(
                                saved.items,
                                saved.index.coerceIn(0, saved.items.lastIndex),
                                saved.positionMs
                            )
                            player.prepare()
                        }
                        player.shuffleModeEnabled = saved.shuffle
                        player.repeatMode = saved.repeat
                        player.setPlaybackSpeed(saved.speed)
                        if (saved.playWhenReady && saved.items.isNotEmpty()) player.play()
                    }
                }
            }
            main {
                api?.release()
                MediaController.releaseFuture(future)
            }
            preferences.edit().apply {
                if (savedSnapshot == null) remove("snapshot")
                else putString("snapshot", savedSnapshot)
            }.commit()
            files.forEach(File::delete)
        }
    }

    @Test
    fun libraryPlayNextThenSearchAppendPreservesPlaybackAndSnapshot() =
        withQueue { media, api, vm, songs ->
            var page by mutableStateOf("library")
            var searchState by mutableStateOf(
                SearchUiState(
                    query = "Queue End",
                    songs = songs,
                    results = listOf(songs[3]),
                    isLoaded = true
                )
            )
            val nextSent = AtomicBoolean(false)
            val endSent = AtomicBoolean(false)
            compose.setContent {
                val playing by api.uiState.collectAsState()
                when (page) {
                    "library" -> LibraryScreen(
                        uiState = LibraryUiState.Success(listOf(songs[2])),
                        hasPermission = true,
                        onBack = {}, onPickManual = {}, onRefresh = {},
                        onSongSelected = { _, _ -> },
                        onAddToPlaylist = {},
                        onQueueNext = if (playing.connected && playing.hasMedia &&
                            playing.canEditQueue && !playing.shuffleEnabled &&
                            playing.repeatMode != Player.REPEAT_MODE_ONE
                        ) {
                            { song -> nextSent.set(vm.queueDeviceSong(song, QueuePlacement.NEXT)) }
                        } else null,
                        onQueueEnd = { song -> vm.queueDeviceSong(song, QueuePlacement.END) }
                    )
                    else -> SearchScreen(
                        state = searchState,
                        hasPermission = true,
                        onQueryChange = { query -> searchState = searchState.copy(
                            query = query,
                            results = songs.filter { it.title.contains(query, true) }
                        ) },
                        onRequestPermission = {}, onRefresh = {},
                        onSongSelected = { _, _ -> }, onAddToPlaylist = {},
                        onQueueNext = { song -> vm.queueDeviceSong(song, QueuePlacement.NEXT) },
                        onQueueEnd = { song ->
                            endSent.set(vm.queueDeviceSong(song, QueuePlacement.END))
                        }
                    )
                }
            }
            var before = 0L
            main { before = media.currentPosition }

            compose.onNodeWithContentDescription("Hàng đợi cho Queue Next").performClick()
            compose.onNodeWithText("Phát tiếp").performClick()
            assertTrue("Library Play Next callback refused", nextSent.get())
            val expectedNext = listOf(songs[0], songs[2], songs[1])
                .map { TrackMediaItems.fromDevice(it).mediaId }
            waitFor("Library NEXT inserted immediately behind playing WAV") {
                queueIdsUnsafe(media) == expectedNext && media.currentMediaItemIndex == 0
            }
            assertPlayingWithoutReset(media, expectedNext[0], before)
            Log.i("MusicFlowQueueActionsQA", "PASS libraryPlayNextOrder")

            compose.runOnIdle { page = "search" }
            compose.onNodeWithContentDescription("Hàng đợi cho Queue End").performClick()
            compose.onNodeWithText("Thêm vào hàng đợi").performClick()
            assertTrue("Search Queue End callback refused", endSent.get())
            val expectedEnd = listOf(songs[0], songs[2], songs[1], songs[3])
                .map { TrackMediaItems.fromDevice(it).mediaId }
            waitFor("Search END appended without changing current audio") {
                queueIdsUnsafe(media) == expectedEnd && media.currentMediaItemIndex == 0
            }
            assertPlayingWithoutReset(media, expectedEnd[0], before)
            Log.i("MusicFlowQueueActionsQA", "PASS searchAppendAtTail")

            // The playback service must store the resulting order, not only
            // temporarily expose it via a MediaController UI list.
            waitFor("MediaSession persisted queue order") {
                val raw = preferences.getString("snapshot", null) ?: return@waitFor false
                runCatching {
                    val json = JSONObject(raw)
                    val entries = json.getJSONArray("items")
                    entries.length() == 4 &&
                        (0 until entries.length()).map {
                            entries.getJSONObject(it).getString("mediaId")
                        } == expectedEnd &&
                        json.getInt("index") == 0
                }.getOrDefault(false)
            }
            Log.i("MusicFlowQueueActionsQA", "PASS queuePersistedInCorrectOrder")

            // The new NEXT item must actually become the next playable
            // MediaSession track, rather than merely appearing in UI state.
            main { vm.next() }
            waitFor("Playback Next reaches inserted local WAV") {
                media.currentMediaItemIndex == 1 &&
                    media.currentMediaItem?.mediaId == expectedEnd[1] &&
                    media.mediaItemCount == 4 && media.isPlaying
            }
            Log.i("MusicFlowQueueActionsQA", "PASS insertedNextActuallyPlays")
        }

    private fun queueIdsUnsafe(media: MediaController) =
        (0 until media.mediaItemCount).map { media.getMediaItemAt(it).mediaId }

    @Test
    fun nextRejectsShuffleRepeatOneAndEmptyQueueWithoutInterruptingAudio() =
        withQueue { media, api, vm, songs ->
            val candidate = TrackMediaItems.fromDevice(songs[2])
            val initialIds = queueIds(media)
            var before = 0L
            main {
                before = media.currentPosition
                media.shuffleModeEnabled = true
            }
            waitFor("queue controller has received shuffle enabled") {
                api.uiState.value.shuffleEnabled
            }
            main {
                assertFalse(vm.queueDeviceSong(songs[2], QueuePlacement.NEXT))
                assertEquals(initialIds, queueIdsUnsafe(media))
                media.shuffleModeEnabled = false
                media.repeatMode = Player.REPEAT_MODE_ONE
            }
            waitFor("queue controller has received repeat one") {
                api.uiState.value.repeatMode == Player.REPEAT_MODE_ONE &&
                    !api.uiState.value.shuffleEnabled
            }
            main {
                assertFalse(vm.queueDeviceSong(songs[2], QueuePlacement.NEXT))
                assertEquals(initialIds, queueIdsUnsafe(media))
                media.repeatMode = Player.REPEAT_MODE_OFF
                // Do not accept a malformed item without a playable location.
                assertFalse(api.enqueue(MediaItem.Builder()
                    .setMediaId("invalid-no-uri").build(), QueuePlacement.END))
                assertEquals(initialIds, queueIdsUnsafe(media))
            }
            assertPlayingWithoutReset(media, initialIds[0], before)
            Log.i("MusicFlowQueueActionsQA", "PASS shuffleRepeatAndInvalidRejected")

            main {
                media.pause()
                media.clearMediaItems()
            }
            waitFor("queue controller has received empty timeline") {
                !api.uiState.value.hasMedia && api.uiState.value.queueItems.isEmpty()
            }
            main {
                assertFalse(api.enqueue(candidate, QueuePlacement.END))
                assertFalse(api.enqueue(candidate, QueuePlacement.NEXT))
                assertEquals(0, media.mediaItemCount)
            }
            Log.i("MusicFlowQueueActionsQA", "PASS emptyQueueDoesNotAutoPlay")
        }
}
