package com.example.musicflow.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playlist.PlaylistQuickAddRules
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.feature.playlist.AddTrackUiState
import com.example.musicflow.feature.playlist.PlaylistQuickAddScreen
import com.example.musicflow.feature.playlist.PlaylistTracksUiState
import com.example.musicflow.feature.playlist.PlaylistViewModel
import com.example.musicflow.ui.theme.MusicFlowTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** User-side preview plus one real local Room write; no audio, Auth or cloud operations. */
@RunWith(AndroidJUnit4::class)
class PlaylistQuickAddUiTest {
    @get:Rule val compose = createComposeRule()

    private val songA = DeviceSong(
        id = 1, uri = Uri.parse("content://media/external/audio/media/1"),
        title = "Đường về nhà", artist = "Nghệ sĩ QA", durationMs = 150_000
    )
    private val songB = DeviceSong(
        id = 2, uri = Uri.parse("content://media/external/audio/media/2"),
        title = "Bài thử thứ hai", artist = "MusicFlow QA", durationMs = 60_000
    )

    @Test fun firstUseDoesNotDemandScanPermissionOrOpenPickerAutomatically() {
        var scanRequests = 0
        var filePicks = 0
        compose.setContent {
            MusicFlowTheme {
                PlaylistQuickAddScreen(
                    playlistName = "Nhạc của tôi", playlistLoaded = true,
                    trackState = PlaylistTracksUiState.Ready(emptyList()),
                    libraryState = LibraryUiState.Idle, hasScanPermission = false,
                    addState = AddTrackUiState.Idle, fileError = null,
                    onDone = {}, onRequestScan = { scanRequests++ },
                    onPickFile = { filePicks++ }, onAddSong = {}
                )
            }
        }
        compose.onNodeWithTag("quick-add-target").assertExists()
        compose.onNodeWithText("Cho phép quét nhạc").assertExists()
        compose.onNodeWithTag("quick-add-saf-file").performClick()
        compose.runOnIdle {
            assertEquals(0, scanRequests)
            assertEquals(1, filePicks)
        }
        compose.onNodeWithText("Cho phép quét nhạc").performClick()
        compose.runOnIdle { assertEquals(1, scanRequests) }
    }

    @Test fun searchWithoutAccentsAddsInOneTapAndMarksAlreadyStoredTracks() {
        var pressed = 0
        var lastId = ""
        compose.setContent {
            var tracks by remember { mutableStateOf(emptyList<PlaylistTrack>()) }
            MusicFlowTheme {
                PlaylistQuickAddScreen(
                    playlistName = "Đi học", playlistLoaded = true,
                    trackState = PlaylistTracksUiState.Ready(tracks),
                    libraryState = LibraryUiState.Success(listOf(songA, songB)),
                    hasScanPermission = true, addState = AddTrackUiState.Idle,
                    fileError = null, onDone = {}, onRequestScan = {},
                    onPickFile = {},
                    onAddSong = { song ->
                        pressed++
                        lastId = PlaylistQuickAddRules.mediaId(song.mediaId, song.id)
                        tracks = tracks + PlaylistTrack(
                            mediaId = lastId, audioUri = song.uri.toString(),
                            title = song.title, artist = song.artist, addedAtMs = 1L
                        )
                    }
                )
            }
        }
        compose.onNodeWithTag("quick-add-query").performTextInput("duong")
        compose.onNodeWithText("Đường về nhà").assertExists()
        compose.onNodeWithText("Bài thử thứ hai").assertDoesNotExist()
        compose.onNodeWithTag("quick-add-item-device-1").performClick()
        compose.runOnIdle {
            assertEquals(1, pressed)
            assertEquals("device-1", lastId)
        }
        compose.onNodeWithTag("quick-add-item-device-1").assertIsNotEnabled()
        compose.onNodeWithText("Đã có").assertExists()
        compose.onNodeWithTag("quick-add-clear-search").performClick()
        compose.onNodeWithTag("quick-add-item-device-2").performClick()
        compose.runOnIdle { assertEquals(2, pressed) }
    }

    @Test fun deletedPlaylistNeverShowsAddButton() {
        var attempts = 0
        var finished = 0
        compose.setContent {
            MusicFlowTheme {
                PlaylistQuickAddScreen(
                    playlistName = null, playlistLoaded = true,
                    trackState = PlaylistTracksUiState.Ready(emptyList()),
                    libraryState = LibraryUiState.Success(listOf(songA)),
                    hasScanPermission = true, addState = AddTrackUiState.Idle,
                    fileError = null, onDone = { finished++ },
                    onRequestScan = {}, onPickFile = {},
                    onAddSong = { attempts++ }
                )
            }
        }
        compose.onNodeWithText("Playlist không còn tồn tại").assertExists()
        compose.onNodeWithTag("quick-add-item-device-1").assertDoesNotExist()
        compose.onNodeWithTag("quick-add-done").performClick()
        compose.runOnIdle {
            assertEquals(0, attempts)
            assertEquals(1, finished)
        }
    }

    @Test fun noConcurrentAddWhenSavingEvenOnLargeText() {
        var attempts = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.65f)) {
                Box(Modifier.width(315.dp).height(720.dp)) {
                    MusicFlowTheme {
                        PlaylistQuickAddScreen(
                            playlistName = "Playlist QA", playlistLoaded = true,
                            trackState = PlaylistTracksUiState.Ready(emptyList()),
                            libraryState = LibraryUiState.Success(listOf(songA)),
                            hasScanPermission = true,
                            addState = AddTrackUiState.Saving, fileError = null,
                            onDone = {}, onRequestScan = {}, onPickFile = {},
                            onAddSong = { attempts++ }
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("quick-add-item-device-1").assertIsNotEnabled()
        compose.onNodeWithTag("quick-add-done").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, attempts) }
    }

    @Test fun oneTapFromUiPersistsExactlyOneTrackLocallyAndNoDuplicate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = RoomPlaylistRepository(RecentHistoryDatabase.getInstance(context))
        val owned = runBlocking { repository.create("Direct add QA ${System.nanoTime()}") }
        assertNotNull(owned)
        val playlist = checkNotNull(owned)
        val vm = PlaylistViewModel(repository)
        try {
            compose.setContent {
                val tracks by repository.observeTracks(playlist.id)
                    .collectAsState(initial = emptyList())
                val result by vm.addTrackState.collectAsState()
                MusicFlowTheme {
                    PlaylistQuickAddScreen(
                        playlistName = playlist.name, playlistLoaded = true,
                        trackState = PlaylistTracksUiState.Ready(tracks),
                        libraryState = LibraryUiState.Success(listOf(songA, songB)),
                        hasScanPermission = true, addState = result, fileError = null,
                        onDone = {}, onRequestScan = {}, onPickFile = {},
                        onAddSong = { vm.addDeviceSong(playlist.id, playlist.name, it) }
                    )
                }
            }
            compose.onNodeWithTag("quick-add-item-device-1").performClick()
            compose.waitUntil(15_000) {
                runBlocking { repository.getTracks(playlist.id).size == 1 }
            }
            compose.waitUntil(10_000) {
                compose.onNodeWithTag("quick-add-item-device-1")
                    .fetchSemanticsNode().config.contains(
                        androidx.compose.ui.semantics.SemanticsProperties.Disabled
                    )
            }
            compose.onNodeWithTag("quick-add-item-device-1").assertIsNotEnabled()
            assertEquals("device-1", runBlocking { repository.getTracks(playlist.id) }.single().mediaId)
            assertFalse(vm.addTrackState.value is AddTrackUiState.Error)
            assertTrue(runBlocking { repository.getTracks(playlist.id) }.single().audioUri
                .startsWith("content://"))
        } finally {
            runBlocking { repository.deletePlaylist(playlist.id) }
        }
    }
}
