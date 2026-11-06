package com.example.musicflow.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.lyrics.LyricsUiState
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.home.HomeHistoryUiState
import com.example.musicflow.feature.home.HomeScreen
import com.example.musicflow.feature.library.MusicCollection
import com.example.musicflow.feature.player.NowPlayingScreen
import com.example.musicflow.feature.playlist.PlaylistEmptyState
import com.example.musicflow.feature.search.SearchScreen
import com.example.musicflow.feature.search.SearchUiState
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Listener UX only. No login, actual audio playback, provider changes or DB writes. */
@RunWith(AndroidJUnit4::class)
class ListeningJourneysV1UiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeShowsShortHistoryThenAllSongsWithOriginalPlaybackIndex() {
        val songs = (1..6).map { number ->
            RecentSong(
                mediaId = "qa-$number",
                audioUri = "content://media/external/audio/media/$number",
                title = "Bài $number", artist = "Ca sĩ $number", playedAtMs = number.toLong()
            )
        }
        var queueCount = 0
        var selectedIndex = -1
        compose.setContent {
            MusicFlowTheme {
                HomeScreen(
                    state = PlayerUiState(),
                    historyState = HomeHistoryUiState.Success(songs),
                    onOpenLibrary = {}, onOpenSearch = {}, onOpenPlaylists = {},
                    onOpenPlayer = {},
                    onRecentSongSelected = { selection, index ->
                        queueCount = selection.size
                        selectedIndex = index
                    },
                    actionError = null, onDismissActionError = {},
                    onRemoveRecentSong = {}, onClearHistory = {}
                )
            }
        }
        compose.onNodeWithTag("history-song-qa-1").assertExists()
        compose.onNodeWithTag("history-song-qa-5").assertDoesNotExist()
        compose.onNodeWithTag("home-recent-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("history-song-qa-6").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(6, queueCount)
            assertEquals(5, selectedIndex)
        }
        compose.onNodeWithTag("home-recent-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("history-song-qa-6").assertDoesNotExist()
    }

    @Test fun searchOffersDirectJumpToMatchingAlbumWithoutRescanning() {
        val track = DeviceSong(
            id = 22L,
            uri = Uri.parse("content://media/external/audio/media/22"),
            title = "Bài nhạc không trùng từ khóa", artist = "QA Artist",
            album = "Album Đà Lạt", durationMs = 10_000
        )
        var requestedRefresh = 0
        compose.setContent {
            MusicFlowTheme {
                SearchScreen(
                    state = SearchUiState(
                        query = "da lat", songs = listOf(track), results = emptyList(),
                        albumResults = listOf(MusicCollection("album-22", "Album Đà Lạt", listOf(track))),
                        isLoaded = true
                    ),
                    hasPermission = true,
                    onQueryChange = {}, onRequestPermission = {},
                    onRefresh = { requestedRefresh++ },
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithTag("search-related-suggestion").assertExists().performClick()
        compose.onNodeWithText("Album Đà Lạt").assertExists()
        compose.onNodeWithTag("search-related-suggestion").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, requestedRefresh) }
    }

    @Test fun emptyPlayerHasImmediateOfflineActions() {
        var libraryOpens = 0
        var pickerOpens = 0
        player(
            state = PlayerUiState(),
            onOpenLibrary = { libraryOpens++ },
            onPickFiles = { pickerOpens++ }
        )
        compose.onNodeWithTag("player-empty-guide").assertExists()
        compose.onNodeWithTag("player-empty-library").performClick()
        compose.onNodeWithTag("player-empty-files").performClick()
        compose.onNodeWithText("Chưa có bài trong hàng đợi").assertExists()
        compose.runOnIdle {
            assertEquals(1, libraryOpens)
            assertEquals(1, pickerOpens)
        }
    }

    @Test fun playerShowsFirstQueuePositionAndAccessibleRepeatAtLargeText() {
        var repeats = 0
        var shuffles = 0
        compose.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(current.density, fontScale = 1.7f)
            ) {
                Box(Modifier.width(320.dp).height(690.dp)) {
                    MusicFlowTheme {
                        playerContent(
                            state = PlayerUiState(
                                hasMedia = true, connected = true,
                                currentIndex = 0, queueCount = 3, title = "Bài đang phát"
                            ),
                            onOpenLibrary = {}, onPickFiles = {},
                            onRepeat = { repeats++ }, onShuffle = { shuffles++ }
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Bài 1 / 3").performScrollTo().assertExists()
        compose.onNodeWithTag("player-empty-guide").assertDoesNotExist()
        compose.onNodeWithTag("player-shuffle-action").performScrollTo().performClick()
        compose.onNodeWithTag("player-repeat-action").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, shuffles)
            assertEquals(1, repeats)
        }
    }

    @Test fun emptyPlaylistGuidesDirectlyToLocalLibrary() {
        var opens = 0
        compose.setContent {
            MusicFlowTheme { PlaylistEmptyState(onFindSongs = { opens++ }) }
        }
        compose.onNodeWithText("Tìm bài hát để thêm").performClick()
        compose.runOnIdle { assertEquals(1, opens) }
    }

    private fun player(
        state: PlayerUiState,
        onOpenLibrary: () -> Unit,
        onPickFiles: () -> Unit
    ) {
        compose.setContent {
            MusicFlowTheme {
                playerContent(state, onOpenLibrary, onPickFiles, {}, {})
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun playerContent(
        state: PlayerUiState,
        onOpenLibrary: () -> Unit,
        onPickFiles: () -> Unit,
        onRepeat: () -> Unit,
        onShuffle: () -> Unit
    ) {
        NowPlayingScreen(
            state = state, onBack = {}, onPickFiles = onPickFiles,
            onOpenLibrary = onOpenLibrary, onPlayPause = {}, onPrevious = {},
            onNext = {}, onSeek = {}, onToggleShuffle = onShuffle,
            onCycleRepeat = onRepeat, onSetPlaybackSpeed = {},
            onSetSleepTimerMinutes = {}, onSetEqualizerEnabled = {},
            onSetEqualizerBand = { _, _ -> }, onSetCrossfadeDuration = {},
            lyrics = LyricsUiState(), onPickLyrics = {}
        )
    }
}
