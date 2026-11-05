package com.example.musicflow.ui

import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.lyrics.LyricsUiState
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibrarySongSort
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.feature.library.sortLibrarySongs
import com.example.musicflow.feature.player.NowPlayingScreen
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** New controls only; never installs songs, starts audio or touches cloud accounts. */
@RunWith(AndroidJUnit4::class)
class MusicFlowPlayerLibraryUxV3Test {
    @get:Rule val compose = createComposeRule()

    @Test
    fun advancedPlayerControlsStartCollapsedAndExpandOnDemand() {
        compose.setContent {
            MusicFlowTheme {
                NowPlayingScreen(
                    state = PlayerUiState(),
                    onBack = {},
                    onPickFiles = {},
                    onOpenLibrary = {},
                    onPlayPause = {},
                    onPrevious = {},
                    onNext = {},
                    onSeek = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onSetPlaybackSpeed = {},
                    onSetSleepTimerMinutes = {},
                    onSetEqualizerEnabled = {},
                    onSetEqualizerBand = { _, _ -> },
                    onSetCrossfadeDuration = {},
                    lyrics = LyricsUiState(),
                    onPickLyrics = {}
                )
            }
        }
        compose.onNodeWithTag("player-advanced-toggle").assertExists()
        compose.onNodeWithTag("playback-speed-1.0x").assertDoesNotExist()
        compose.onNodeWithTag("crossfade-duration-0ms").assertDoesNotExist()

        compose.onNodeWithTag("player-advanced-toggle")
            .performScrollTo().performClick()
        compose.onNodeWithTag("playback-speed-1.0x").assertExists()
        compose.onNodeWithTag("crossfade-duration-0ms").assertExists()
        compose.onNodeWithText("Hẹn giờ tắt nhạc").assertExists()

        compose.onNodeWithTag("player-advanced-toggle")
            .performScrollTo().performClick()
        compose.onNodeWithTag("playback-speed-1.0x").assertDoesNotExist()
        compose.onNodeWithText("Lời bài hát").assertExists()
    }

    @Test
    fun sortingUsesActualMediaStoreDatesAndKeepsDeterministicOrders() {
        val songs = fixtures()
        assertEquals(
            listOf("Alpha Song", "Bravo Song", "Charlie Song"),
            sortLibrarySongs(songs, LibrarySongSort.TITLE).map { it.title }
        )
        assertEquals(
            listOf("Bravo Song", "Charlie Song", "Alpha Song"),
            sortLibrarySongs(songs, LibrarySongSort.ARTIST).map { it.title }
        )
        assertEquals(
            listOf("Charlie Song", "Alpha Song", "Bravo Song"),
            sortLibrarySongs(songs, LibrarySongSort.ALBUM).map { it.title }
        )
        assertEquals(
            listOf("Bravo Song", "Charlie Song", "Alpha Song"),
            sortLibrarySongs(songs, LibrarySongSort.NEWEST).map { it.title }
        )
    }

    @Test
    fun librarySortMenuChangesPlaybackOrderWithoutEditingSongs() {
        val songs = fixtures()
        var selectedTitle = ""
        var selectedIndex = -1
        var selectedQueue = emptyList<String>()
        compose.setContent {
            MusicFlowTheme {
                LibraryScreen(
                    uiState = LibraryUiState.Success(songs),
                    hasPermission = true,
                    onBack = {},
                    onPickManual = {},
                    onRefresh = {},
                    onSongSelected = { items, index ->
                        selectedTitle = items[index].title
                        selectedIndex = index
                        selectedQueue = items.map { it.title }
                    },
                    onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithTag("library-sort-button").performClick()
        compose.onNodeWithTag("library-sort-newest").performClick()
        compose.onNodeWithText("Sắp xếp: Mới thêm").assertExists()
        compose.onNodeWithText("Bravo Song").performClick()
        compose.runOnIdle {
            assertEquals("Bravo Song", selectedTitle)
            assertEquals(0, selectedIndex)
            assertEquals(
                listOf("Bravo Song", "Charlie Song", "Alpha Song"),
                selectedQueue
            )
            assertEquals(3, songs.size)
        }
    }

    private fun fixtures() = listOf(
        DeviceSong(
            id = 1, uri = Uri.parse("content://media/external/audio/media/1"),
            title = "Alpha Song", artist = "Zulu", album = "Album B",
            durationMs = 1000, dateAddedSeconds = 50
        ),
        DeviceSong(
            id = 2, uri = Uri.parse("content://media/external/audio/media/2"),
            title = "Bravo Song", artist = "Adam", album = "Album C",
            durationMs = 1000, dateAddedSeconds = 300
        ),
        DeviceSong(
            id = 3, uri = Uri.parse("content://media/external/audio/media/3"),
            title = "Charlie Song", artist = "Mira", album = "Album A",
            durationMs = 1000, dateAddedSeconds = 150
        )
    )
}
