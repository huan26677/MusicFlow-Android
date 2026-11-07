package com.example.musicflow.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.home.HomeHistoryUiState
import com.example.musicflow.feature.home.HomeScreen
import com.example.musicflow.feature.library.SongRow
import com.example.musicflow.feature.player.MiniPlayer
import com.example.musicflow.feature.search.SearchScreen
import com.example.musicflow.feature.search.SearchUiState
import com.example.musicflow.ui.components.OceanFlowArtworkStage
import com.example.musicflow.ui.components.OceanFlowHeader
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Visual changes must preserve usable semantics and interactions in both modes. */
@RunWith(AndroidJUnit4::class)
class OceanFlowVisualUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun brandHeaderRendersInDarkAndLightWithoutChangingText() {
        compose.setContent {
            var dark by remember { mutableStateOf(true) }
            MusicFlowTheme(darkTheme = dark) {
                androidx.compose.foundation.layout.Column {
                    OceanFlowHeader(
                        eyebrow = "Ocean Flow", title = "Âm nhạc của bạn",
                        subtitle = "Nghe offline thật thoải mái"
                    )
                    androidx.compose.material3.Button(onClick = { dark = !dark }) {
                        androidx.compose.material3.Text("Đổi giao diện")
                    }
                }
            }
        }
        compose.onNodeWithTag("ocean-flow-header").assertExists()
        compose.onNodeWithText("Âm nhạc của bạn").assertExists()
        compose.onNodeWithText("OCEAN FLOW").assertExists()
        compose.onNodeWithText("Đổi giao diện").performClick()
        compose.onNodeWithText("Âm nhạc của bạn").assertExists()
    }

    @Test fun playerArtworkStageShowsSafeOfflinePlaceholder() {
        compose.setContent {
            MusicFlowTheme(darkTheme = true) {
                OceanFlowArtworkStage(artworkUri = null)
            }
        }
        compose.onNodeWithTag("ocean-flow-artwork-stage").assertExists()
    }

    @Test fun homeHeroPreservesLibrarySearchPlaylistActionsAtLargeFont() {
        var library = 0
        var search = 0
        var playlists = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.65f)) {
                Box(Modifier.width(320.dp).height(690.dp)) {
                    MusicFlowTheme {
                        HomeScreen(
                            state = PlayerUiState(),
                            historyState = HomeHistoryUiState.Empty,
                            onOpenLibrary = { library++ },
                            onOpenSearch = { search++ },
                            onOpenPlaylists = { playlists++ },
                            onOpenPlayer = {}, onRecentSongSelected = { _, _ -> },
                            actionError = null, onDismissActionError = {},
                            onRemoveRecentSong = {}, onClearHistory = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("ocean-flow-header").assertExists()
        compose.onNodeWithTag("home-library-action").performScrollTo().performClick()
        compose.onNodeWithTag("home-search-action").performScrollTo().performClick()
        compose.onNodeWithTag("home-playlist-action").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, library)
            assertEquals(1, search)
            assertEquals(1, playlists)
        }
    }

    @Test fun floatingMiniPlayerKeepsPlayAndOpenActions() {
        var opened = 0
        var paused = 0
        compose.setContent {
            MusicFlowTheme(darkTheme = true) {
                MiniPlayer(
                    state = PlayerUiState(
                        hasMedia = true, isPlaying = true,
                        title = "Giai điệu QA", artist = "Nghệ sĩ QA",
                        durationMs = 120_000L, positionMs = 15_000L
                    ),
                    onOpenPlayer = { opened++ }, onPlayPause = { paused++ }
                )
            }
        }
        compose.onNodeWithContentDescription("Tạm dừng nhạc").performClick()
        compose.onNodeWithContentDescription("Mở trình phát: Giai điệu QA, Nghệ sĩ QA")
            .performClick()
        compose.runOnIdle {
            assertEquals(1, paused)
            assertEquals(1, opened)
        }
    }

    @Test fun themedLibrarySongRowStillKeepsActionsSeparate() {
        val song = DeviceSong(
            id = 42L,
            uri = Uri.parse("content://media/external/audio/media/42"),
            title = "Bài nhạc Ocean QA", artist = "Nghệ sĩ Ocean",
            durationMs = 65_000L
        )
        var played = 0
        var added = 0
        var favored = 0
        compose.setContent {
            MusicFlowTheme {
                SongRow(song = song, onClick = { played++ },
                    onAddToPlaylist = { added++ },
                    onToggleFavorite = { favored++ })
            }
        }
        compose.onNodeWithText("Bài nhạc Ocean QA").performClick()
        compose.onNodeWithContentDescription("Thêm Bài nhạc Ocean QA vào playlist").performClick()
        compose.onNodeWithContentDescription("Yêu thích Bài nhạc Ocean QA").performClick()
        compose.runOnIdle {
            assertEquals(1, played)
            assertEquals(1, added)
            assertEquals(1, favored)
        }
    }

    @Test fun searchStillOffersItsInputWithNewOceanHeader() {
        compose.setContent {
            MusicFlowTheme(darkTheme = false) {
                SearchScreen(
                    state = SearchUiState(query = "", isLoaded = true),
                    hasPermission = true, onQueryChange = {},
                    onRequestPermission = {}, onRefresh = {},
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithTag("ocean-flow-header").assertExists()
        compose.onNodeWithText("Tìm kiếm").assertExists()
        compose.onNodeWithTag("search-query").assertExists()
    }
}
