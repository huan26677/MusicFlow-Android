package com.example.musicflow.core.playback

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.ContextCompat
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.MediaStoreLocalMusicRepository
import com.example.musicflow.core.media.TrackMediaItems
import com.example.musicflow.feature.library.LibraryRoute
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.feature.library.MusicCollections
import com.example.musicflow.feature.player.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * Tests actual published MediaStore Audio rows with real Library UI and
 * Media3 playback. Synthetic WAV fixtures are the only media used.
 */
@RunWith(AndroidJUnit4::class)
class AdvancedLibraryRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val resolver = context.contentResolver
    private val repository = MediaStoreLocalMusicRepository(context)

    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    private fun grantAudioPermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(
                context, permission
            ) != PackageManager.PERMISSION_GRANTED) {
            ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand(
                    "pm grant ${context.packageName} $permission"
                )
            ).use { it.readBytes() }
        }
        assertEquals(PackageManager.PERMISSION_GRANTED,
            ContextCompat.checkSelfPermission(context, permission))
        Log.i("MusicFlowLibraryQA", "PASS runtimeAudioReadPermission")
    }

    private fun wav(): ByteArray {
        val samples = 8000 * 28
        val out = ByteBuffer.allocate(44 + samples * 2)
            .order(ByteOrder.LITTLE_ENDIAN)
        fun chunk(s: String) { out.put(s.toByteArray(Charsets.US_ASCII)) }
        chunk("RIFF"); out.putInt(36 + samples * 2)
        chunk("WAVE"); chunk("fmt "); out.putInt(16)
        out.putShort(1.toShort()); out.putShort(1.toShort())
        out.putInt(8000); out.putInt(16000)
        out.putShort(2.toShort()); out.putShort(16.toShort())
        chunk("data"); out.putInt(samples * 2)
        return out.array()
    }

    private fun publishWav(
        name: String, artist: String, album: String,
        folder: String = "MusicFlowLibraryQA"
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$name.wav")
            put(MediaStore.Audio.Media.TITLE, name)
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.ALBUM, album)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/$folder/")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
        )) { "Unable to insert real MediaStore WAV" }
        try {
            checkNotNull(resolver.openOutputStream(uri, "w")).use {
                it.write(wav())
            }
            assertEquals(1, resolver.update(
                uri, ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }, null, null
            ))
            return uri
        } catch (failure: Throwable) {
            resolver.delete(uri, null, null)
            throw failure
        }
    }

    private fun loadPublished(uris: List<Uri>): List<DeviceSong> = runBlocking {
        val uriSet = uris.toSet()
        withTimeout(20_000L) {
            while (true) {
                val songs = repository.getSongs()
                    .filter { it.uri in uriSet }
                if (songs.size == uriSet.size) {
                    assertEquals(uris.toSet(), songs.map { it.uri }.toSet())
                    assertTrue(songs.all { it.id > 0 && it.durationMs > 0 })
                    songs.forEach {
                        Log.i("MusicFlowLibraryQA",
                            "SCANNED title=${it.title} album=${it.album} " +
                                "albumId=${it.albumId} artist=${it.artist} " +
                                "artistId=${it.artistId} duration=${it.durationMs}")
                    }
                    Log.i("MusicFlowLibraryQA", "PASS realMediaStoreScanner")
                    return@withTimeout songs
                }
                delay(200L)
            }
            @Suppress("UNREACHABLE_CODE")
            emptyList()
        }
    }

    private fun awaitUi(text: String) {
        compose.waitUntil(20_000L) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun withLibrary(
        run: (List<DeviceSong>, PlayerViewModel, PlaybackController,
            MediaController) -> Unit
    ) {
        grantAudioPermission()
        val stamp = System.nanoTime().toString()
        val artist = "MusicFlow Library Artist $stamp"
        val album = "MusicFlow Library Album $stamp"
        val uris = mutableListOf<Uri>()
        val prefs = context.getSharedPreferences(
            "musicflow_playback_queue_v1", Context.MODE_PRIVATE
        )
        val originalSnapshot = prefs.getString("snapshot", null)
        var controller: PlaybackController? = null
        var media: MediaController? = null
        var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
        var oldQueue = emptyList<MediaItem>()
        var oldIndex = 0
        var oldPosition = 0L
        var oldPlaying = false
        var oldSpeed = 1f
        var oldShuffle = false
        var oldRepeat = Player.REPEAT_MODE_OFF
        try {
            uris += publishWav("MF Library Alpha $stamp", artist, album)
            uris += publishWav("MF Library Beta $stamp", artist, album)
            uris += publishWav(
                "MF Library Gamma $stamp", artist, "MusicFlow Library Second $stamp",
                folder = "MusicFlowLibraryQASecond"
            )
            val songs = loadPublished(uris)
            main { controller = PlaybackController(context) }
            future = MediaController.Builder(
                context, SessionToken(context, ComponentName(
                    context, PlaybackService::class.java
                ))
            ).buildAsync()
            media = checkNotNull(future).get(20, TimeUnit.SECONDS)
            val player = checkNotNull(media)
            main {
                oldQueue = (0 until player.mediaItemCount).map(player::getMediaItemAt)
                oldIndex = player.currentMediaItemIndex
                oldPosition = player.currentPosition.coerceAtLeast(0L)
                oldPlaying = player.playWhenReady
                oldSpeed = player.playbackParameters.speed
                oldShuffle = player.shuffleModeEnabled
                oldRepeat = player.repeatMode
                player.pause()
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.setPlaybackSpeed(1f)
            }
            compose.waitUntil(15_000L) {
                checkNotNull(controller).uiState.value.connected
            }
            run(songs, PlayerViewModel(checkNotNull(controller)),
                checkNotNull(controller), player)
        } finally {
            media?.let { player ->
                main {
                    player.pause()
                    if (oldQueue.isEmpty()) player.clearMediaItems()
                    else {
                        player.setMediaItems(
                            oldQueue, oldIndex.coerceIn(0, oldQueue.lastIndex),
                            oldPosition
                        )
                        player.prepare()
                    }
                    player.shuffleModeEnabled = oldShuffle
                    player.repeatMode = oldRepeat
                    player.setPlaybackSpeed(oldSpeed)
                    if (oldPlaying && oldQueue.isNotEmpty()) player.play()
                }
            }
            main {
                controller?.release()
                future?.let(MediaController::releaseFuture)
            }
            prefs.edit().apply {
                if (originalSnapshot == null) remove("snapshot")
                else putString("snapshot", originalSnapshot)
            }.commit()
            uris.forEach { uri -> resolver.delete(uri, null, null) }
        }
    }

    @Test
    fun realMediaStoreLibraryRouteScansAndStartsPlayback() = withLibrary {
        songs, viewModel, _, player ->
        compose.setContent {
            LibraryRoute(
                onBack = {}, onPickManual = {},
                onSongSelected = viewModel::playDeviceSongs
            )
        }
        songs.forEach { awaitUi(it.title) }
        Log.i("MusicFlowLibraryQA", "PASS realLibraryRouteDisplaysMediaStoreSongs")
        compose.onNodeWithText(songs.first().title).performClick()
        compose.waitUntil(20_000L) {
            var playing = false
            main {
                playing = player.isPlaying &&
                    player.currentMediaItem?.mediaId ==
                        TrackMediaItems.fromDevice(songs.first()).mediaId
            }
            playing
        }
        main {
            assertTrue("MediaStore song must use a content URI",
                player.currentMediaItem?.localConfiguration?.uri?.scheme == "content")
        }
        Log.i("MusicFlowLibraryQA", "PASS realLibraryRouteToMediaSession")
    }

    @Test
    fun mediaStoreAlbumsArtistsAndGenresOpenGroupsAndPlay() = withLibrary {
        songs, viewModel, _, player ->
        val albumGroups = MusicCollections.albums(songs)
        val artistGroups = MusicCollections.artists(songs)
        val genreGroups = MusicCollections.genres(songs)
        assertEquals("Two distinct real MediaStore album directories", 2,
            albumGroups.size)
        assertTrue(artistGroups.isNotEmpty())
        assertTrue(genreGroups.isNotEmpty())
        Log.i("MusicFlowLibraryQA", "PASS distinctRealAlbumGroups")

        compose.setContent {
            LibraryScreen(
                uiState = LibraryUiState.Success(songs),
                hasPermission = true,
                onBack = {}, onPickManual = {}, onRefresh = {},
                onSongSelected = viewModel::playDeviceSongs,
                onAddToPlaylist = {}
            )
        }
        compose.onNodeWithText("Album").performClick()
        awaitUi("2 album")
        val album = albumGroups.first()
        awaitUi(album.name)
        compose.onAllNodesWithText(album.name).onFirst().performClick()
        awaitUi("Phát toàn bộ album")
        compose.onNodeWithText("Phát toàn bộ album").performClick()
        compose.waitUntil(15_000L) {
            var selected = false
            main {
                selected = player.isPlaying &&
                    player.mediaItemCount == album.tracks.size &&
                    player.currentMediaItem?.mediaId ==
                        TrackMediaItems.fromDevice(album.tracks.first()).mediaId
            }
            selected
        }
        Log.i("MusicFlowLibraryQA", "PASS realAlbumGroupPlayback")

        compose.onNodeWithTag("library-category-artists")
            .performScrollTo().performClick()
        compose.onNodeWithTag("music-collections-list").assertExists()
        val artist = artistGroups.first()
        compose.onNodeWithTag("music-collections-list")
            .performScrollToKey("header:${artist.key}")
        compose.onNodeWithTag("collection-header-${artist.key}").performClick()
        compose.onNodeWithTag("music-collections-list")
            .performScrollToKey("play:${artist.key}")
        compose.onNodeWithTag("collection-play-all").performClick()
        compose.waitUntil(15_000L) {
            var selected = false
            main {
                selected = player.isPlaying &&
                    player.mediaItemCount == artist.tracks.size &&
                    player.currentMediaItem?.mediaId ==
                        TrackMediaItems.fromDevice(artist.tracks.first()).mediaId
            }
            selected
        }
        Log.i("MusicFlowLibraryQA", "PASS realArtistGroupPlayback")

        compose.onNodeWithTag("library-category-genres")
            .performScrollTo().performClick()
        compose.onNodeWithTag("music-collections-list").assertExists()
        val genre = genreGroups.first()
        compose.onNodeWithTag("music-collections-list")
            .performScrollToKey("header:${genre.key}")
        compose.onNodeWithTag("collection-header-${genre.key}").performClick()
        compose.onNodeWithTag("music-collections-list")
            .performScrollToKey("play:${genre.key}")
        compose.onNodeWithTag("collection-play-all").assertExists()
        Log.i("MusicFlowLibraryQA", "PASS realGenreGroupDisplay")
    }
}
