package com.example.musicflow.core.playback

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.MediaStoreLocalMusicRepository
import com.example.musicflow.core.media.TrackMediaItems
import com.example.musicflow.feature.player.PlayerViewModel
import com.example.musicflow.feature.search.SearchRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/** Searches published real MediaStore WAVs through SearchRoute's real ViewModel. */
@RunWith(AndroidJUnit4::class)
class AdvancedSearchRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val repository = MediaStoreLocalMusicRepository(context)
    private val prefs = context.getSharedPreferences(
        "musicflow_playback_queue_v1", Context.MODE_PRIVATE
    )

    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun grantAudioRead() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, permission) !=
            PackageManager.PERMISSION_GRANTED) {
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
        Log.i("MusicFlowSearchQA", "PASS realMediaStoreAudioPermission")
    }

    private fun wav(): ByteArray {
        val sampleCount = 8000 * 35
        val data = ByteBuffer.allocate(44 + sampleCount * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun mark(value: String) { data.put(value.toByteArray(Charsets.US_ASCII)) }
        mark("RIFF"); data.putInt(36 + sampleCount * 2)
        mark("WAVE"); mark("fmt "); data.putInt(16)
        data.putShort(1.toShort()); data.putShort(1.toShort())
        data.putInt(8000); data.putInt(16000)
        data.putShort(2.toShort()); data.putShort(16.toShort())
        mark("data"); data.putInt(sampleCount * 2)
        return data.array()
    }

    private fun insertAudio(title: String, albumDir: String): Uri {
        val row = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.wav")
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/$albumDir/")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(
            resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, row)
        )
        try {
            checkNotNull(resolver.openOutputStream(uri, "w")).use {
                it.write(wav())
            }
            assertEquals(1, resolver.update(uri, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }, null, null))
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
        return uri
    }

    private fun loadMediaStore(uris: List<Uri>): List<DeviceSong> = runBlocking {
        val expected = uris.toSet()
        withTimeout(20_000L) {
            while (true) {
                val actual = repository.getSongs().filter { it.uri in expected }
                if (actual.size == expected.size) {
                    assertEquals(expected, actual.map { it.uri }.toSet())
                    assertTrue(actual.all { it.id > 0 && it.durationMs > 0 })
                    actual.forEach {
                        Log.i("MusicFlowSearchQA",
                            "SCANNED title=${it.title} album=${it.album} " +
                                "artist=${it.artist} genres=${it.genres}")
                    }
                    Log.i("MusicFlowSearchQA", "PASS publishedWavsScanned")
                    return@withTimeout actual
                }
                delay(200L)
            }
            @Suppress("UNREACHABLE_CODE")
            emptyList()
        }
    }

    private data class SavedPlayer(
        val items: List<MediaItem>,
        val index: Int,
        val positionMs: Long,
        val playWhenReady: Boolean,
        val speed: Float,
        val shuffle: Boolean,
        val repeat: Int
    )

    private fun withSearch(run: (
        songs: List<DeviceSong>,
        vm: PlayerViewModel,
        player: MediaController,
        albumName: String
    ) -> Unit) {
        grantAudioRead()
        val stamp = System.nanoTime().toString()
        val firstAlbum = "MFlowSearchAlbumA_$stamp"
        val secondAlbum = "MFlowSearchAlbumB_$stamp"
        val uris = mutableListOf<Uri>()
        val oldSnapshot = prefs.getString("snapshot", null)
        var api: PlaybackController? = null
        var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
        var media: MediaController? = null
        var original: SavedPlayer? = null
        try {
            uris += insertAudio("MFlow Tình Yêu $stamp", firstAlbum)
            uris += insertAudio("MFlow Đường Về $stamp", firstAlbum)
            uris += insertAudio("MFlow Khúc Nhạc $stamp", secondAlbum)
            val songs = loadMediaStore(uris)
            assertTrue("MediaStore did not preserve the accented title",
                songs.any { it.title.contains("Tình Yêu") })
            assertEquals(2, songs.map { it.albumId }.distinct().size)
            Log.i("MusicFlowSearchQA", "PASS realAccentTitleAndTwoAlbums")

            main { api = PlaybackController(context) }
            future = MediaController.Builder(
                context, SessionToken(context, ComponentName(
                    context, PlaybackService::class.java
                ))
            ).buildAsync()
            media = checkNotNull(future).get(20, TimeUnit.SECONDS)
            val player = checkNotNull(media)
            main {
                original = SavedPlayer(
                    (0 until player.mediaItemCount).map(player::getMediaItemAt),
                    player.currentMediaItemIndex,
                    player.currentPosition.coerceAtLeast(0L),
                    player.playWhenReady,
                    player.playbackParameters.speed,
                    player.shuffleModeEnabled,
                    player.repeatMode
                )
                player.pause()
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.setPlaybackSpeed(1f)
            }
            compose.waitUntil(15_000L) {
                checkNotNull(api).uiState.value.connected
            }
            run(songs, PlayerViewModel(checkNotNull(api)), player, firstAlbum)
        } finally {
            media?.let { player ->
                original?.let { saved ->
                    main {
                        player.pause()
                        if (saved.items.isEmpty()) player.clearMediaItems()
                        else {
                            player.setMediaItems(
                                saved.items, saved.index.coerceIn(0, saved.items.lastIndex),
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
                future?.let(MediaController::releaseFuture)
            }
            prefs.edit().apply {
                if (oldSnapshot == null) remove("snapshot") else putString(
                    "snapshot", oldSnapshot
                )
            }.commit()
            uris.forEach { resolver.delete(it, null, null) }
        }
    }

    private fun awaitText(value: String) {
        compose.waitUntil(20_000L) {
            compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun changeQuery(value: String) {
        compose.onNode(hasSetTextAction()).performTextReplacement(value)
    }

    private fun awaitMedia(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            var passed = false
            main { passed = condition() }
            if (passed) return
            SystemClock.sleep(100L)
        }
        throw AssertionError("MediaSession did not reach $description")
    }

    @Test
    fun searchRouteAccentInsensitivePlaybackAndQueueActions() = withSearch {
        songs, vm, player, _ ->
        val love = songs.single { it.title.contains("Tình Yêu") }
        val road = songs.single { it.title.contains("Đường Về") }
        val tune = songs.single { it.title.contains("Khúc Nhạc") }
        var remembered = ""
        compose.setContent {
            SearchRoute(
                initialQuery = "tinh yeu",
                onQueryRemembered = { remembered = it },
                onSongSelected = vm::playDeviceSongs,
                onQueueNext = { song -> vm.queueDeviceSong(song, QueuePlacement.NEXT) },
                onQueueEnd = { song -> vm.queueDeviceSong(song, QueuePlacement.END) }
            )
        }
        awaitText(love.title)
        Log.i("MusicFlowSearchQA", "PASS unaccentedQueryFindsAccentedMediaStoreTitle")
        compose.onNodeWithText(love.title).performClick()
        awaitMedia("SearchRoute real WAV playback") {
            player.isPlaying && player.mediaItemCount == 1 &&
                player.currentMediaItem?.mediaId ==
                    TrackMediaItems.fromDevice(love).mediaId
        }
        Log.i("MusicFlowSearchQA", "PASS searchRouteMediaSessionPlayback")

        changeQuery("duong ve")
        awaitText(road.title)
        compose.onNodeWithContentDescription("Hàng đợi cho ${road.title}")
            .performClick()
        compose.onNodeWithText("Thêm vào hàng đợi").performClick()
        val first = TrackMediaItems.fromDevice(love).mediaId
        val second = TrackMediaItems.fromDevice(road).mediaId
        awaitMedia("SearchRoute appended WAV") {
            player.isPlaying && player.mediaItemCount == 2 &&
                player.currentMediaItem?.mediaId == first &&
                player.getMediaItemAt(1).mediaId == second
        }
        Log.i("MusicFlowSearchQA", "PASS searchAppendOfflineMediaStoreQueue")

        changeQuery("khuc nhac")
        awaitText(tune.title)
        compose.onNodeWithContentDescription("Hàng đợi cho ${tune.title}")
            .performClick()
        compose.onNodeWithText("Phát tiếp").performClick()
        val inserted = TrackMediaItems.fromDevice(tune).mediaId
        awaitMedia("SearchRoute NEXT inserted directly after current") {
            player.isPlaying && player.mediaItemCount == 3 &&
                player.currentMediaItemIndex == 0 &&
                (0 until 3).map(player::getMediaItemAt).map { it.mediaId } ==
                    listOf(first, inserted, second)
        }
        assertEquals("khuc nhac", remembered)
        Log.i("MusicFlowSearchQA", "PASS searchPlayNextPreservesOrder")
        compose.waitUntil(10_000L) {
            val raw = prefs.getString("snapshot", null) ?: return@waitUntil false
            runCatching {
                val json = JSONObject(raw)
                val tracks = json.getJSONArray("items")
                (0 until tracks.length()).map {
                    tracks.getJSONObject(it).getString("mediaId")
                } == listOf(first, inserted, second)
            }.getOrDefault(false)
        }
        Log.i("MusicFlowSearchQA", "PASS searchQueueSnapshotPersisted")
        main { vm.next() }
        awaitMedia("inserted Search result plays after Next") {
            player.isPlaying && player.currentMediaItemIndex == 1 &&
                player.currentMediaItem?.mediaId == inserted
        }
        Log.i("MusicFlowSearchQA", "PASS searchInsertedTrackActuallyPlays")
    }

    @Test
    fun searchRouteAlbumArtistAndGenreFilterTabsUseRealScanner() = withSearch {
        songs, vm, player, album ->
        compose.setContent {
            SearchRoute(
                initialQuery = album,
                onQueryRemembered = {},
                onSongSelected = vm::playDeviceSongs
            )
        }
        compose.onNodeWithTag("search-category-albums").performClick()
        awaitText(album)
        // The same album name also appears as EditableText in SearchRoute's
        // query field. The last matching node is the result-group row.
        compose.onAllNodesWithText(album).onLast().performClick()
        awaitText("Phát toàn bộ")
        compose.onNodeWithText("Phát toàn bộ").performClick()
        val expectedAlbum = songs.filter { it.album == album }
        awaitMedia("album results play exact MediaStore group") {
            player.isPlaying && player.mediaItemCount == expectedAlbum.size &&
                player.currentMediaItem?.mediaId ==
                    TrackMediaItems.fromDevice(expectedAlbum.first()).mediaId
        }
        Log.i("MusicFlowSearchQA", "PASS searchAlbumGroupPlayback")

        changeQuery("khong ro nghe si")
        compose.onNodeWithTag("search-category-artists").performClick()
        awaitText("Không rõ nghệ sĩ")
        compose.onNodeWithText("Không rõ nghệ sĩ").performClick()
        awaitText("Phát toàn bộ")
        Log.i("MusicFlowSearchQA", "PASS searchArtistFallbackGroup")

        changeQuery("khong ro the loai")
        compose.onNodeWithTag("search-category-genres").performClick()
        awaitText("Không rõ thể loại")
        compose.onNodeWithText("Không rõ thể loại").performClick()
        awaitText("Phát toàn bộ")
        Log.i("MusicFlowSearchQA", "PASS searchGenreFallbackGroup")

        compose.onNodeWithTag("search-category-songs").performClick()
        changeQuery("musicflow-no-match-999999")
        awaitText("Không tìm thấy bài hát phù hợp")
        Log.i("MusicFlowSearchQA", "PASS searchNoResultsState")
    }
}
