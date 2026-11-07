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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.home.HomeHistoryUiState
import com.example.musicflow.feature.home.HomeScreen
import com.example.musicflow.feature.library.SmartLibraryContent
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Smart lists use only QA songs supplied by the test, not the user's library. */
@RunWith(AndroidJUnit4::class)
class SmartLibraryUiTest {
    @get:Rule val compose = createComposeRule()

    private fun song(id: Long, title: String, day: Long = 10L) = DeviceSong(
        id = id, uri = Uri.parse("content://media/external/audio/media/$id"),
        title = title, artist = "Nghệ sĩ QA", durationMs = 280_000L,
        dateAddedSeconds = day
    )

    @Test fun mixPlaysCorrectQueueAndIndexWithoutWritingPlaylist() {
        val songs = listOf(song(1, "Một", day = 1), song(2, "Hai", day = 2))
        var selected = -1
        var queue = emptyList<DeviceSong>()
        compose.setContent {
            MusicFlowTheme {
                SmartLibraryContent(
                    songs = songs, favoriteSongUris = setOf(songs[0].uri.toString()),
                    recentSongUris = listOf(songs[1].uri.toString()),
                    onSongSelected = { list, index -> queue = list; selected = index },
                    onAddToPlaylist = {}, onToggleFavorite = {}
                )
            }
        }
        compose.onNodeWithTag("smart-library-content").assertExists()
        compose.onNodeWithTag("smart-play-all").performClick()
        compose.runOnIdle {
            assertEquals(listOf("Hai", "Một"), queue.map { it.title })
            assertEquals(0, selected)
        }
        compose.onNodeWithText("Một").performClick()
        compose.runOnIdle { assertEquals(1, selected) }
    }

    @Test fun changingFavoriteRecomputesSmartCollectionImmediately() {
        val songs = listOf(song(1, "Một"), song(2, "Hai"))
        compose.setContent {
            var favorite by remember { mutableStateOf(setOf(songs[0].uri.toString())) }
            MusicFlowTheme {
                SmartLibraryContent(
                    songs = songs, favoriteSongUris = favorite,
                    recentSongUris = emptyList(),
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {},
                    onToggleFavorite = { item ->
                        val key = item.uri.toString()
                        favorite = if (key in favorite) favorite - key else favorite + key
                    }
                )
            }
        }
        compose.onNodeWithTag("smart-playlist-favorites").performScrollTo().performClick()
        compose.onNodeWithText("Một").assertExists()
        compose.onNodeWithText("Hai").assertDoesNotExist()
        compose.onNodeWithTag("smart-play-all").assertExists()
        compose.onNodeWithContentDescription("Bỏ yêu thích Một").performClick()
        compose.onNodeWithTag("smart-play-all").assertDoesNotExist()
        compose.onNodeWithText("Chưa có bài phù hợp").assertExists()
    }

    @Test fun emptySmartListIsSafeAtLargeFontAndNoPlayAction() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.65f)) {
                Box(Modifier.width(320.dp).height(690.dp)) {
                    MusicFlowTheme(darkTheme = true) {
                        SmartLibraryContent(songs = emptyList(), favoriteSongUris = emptySet(),
                            recentSongUris = listOf("content://no-file"),
                            onSongSelected = { _, _ -> error("Never play empty smart playlist") },
                            onAddToPlaylist = {}, onToggleFavorite = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("smart-library-content").assertExists()
        compose.onNodeWithTag("smart-play-all").assertDoesNotExist()
        compose.onNodeWithText("Chưa có bài phù hợp").assertExists()
    }

    @Test fun homeEntryForSmartLibraryIsClickableWithLargeFont() {
        var opened = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.65f)) {
                Box(Modifier.width(320.dp).height(690.dp)) {
                    MusicFlowTheme {
                        HomeScreen(state = PlayerUiState(),
                            historyState = HomeHistoryUiState.Empty,
                            onOpenLibrary = {}, onOpenSearch = {},
                            onOpenPlaylists = {}, onOpenPlayer = {},
                            onRecentSongSelected = { _, _ -> },
                            actionError = null, onDismissActionError = {},
                            onRemoveRecentSong = {}, onClearHistory = {},
                            onOpenSmartLibrary = { opened++ })
                    }
                }
            }
        }
        compose.onNodeWithTag("home-smart-library-action").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun smartLibraryRouteShowsSmartCollectionsWhenMediaStoreIsEmpty() {
        compose.setContent {
            MusicFlowTheme {
                LibraryScreen(
                    uiState = LibraryUiState.Empty, hasPermission = true,
                    initialShowSmart = true,
                    onBack = {}, onPickManual = {}, onRefresh = {},
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithTag("smart-library-content").assertExists()
        compose.onNodeWithTag("smart-playlist-mix").assertExists()
    }

    @Test fun smartSearchIgnoresVietnameseAccentsAndPlaysOnlyVisibleQueue() {
        val songs = listOf(song(1, "Đường về"), song(2, "Nhớ em"),
            song(3, "Bình yên").copy(artist = "Sơn Tùng"))
        var queue = emptyList<DeviceSong>()
        var index = -1
        compose.setContent {
            MusicFlowTheme {
                SmartLibraryContent(songs, emptySet(), emptyList(),
                    onSongSelected = { items, selected -> queue = items; index = selected },
                    onAddToPlaylist = {}, onToggleFavorite = {})
            }
        }
        compose.onNodeWithTag("smart-search-field").performTextInput("duong")
        compose.onNodeWithText("Hiển thị 1 / 3 bài").assertExists()
        compose.onNodeWithTag("smart-play-all").performClick()
        compose.runOnIdle {
            assertEquals(listOf("Đường về"), queue.map { it.title })
            assertEquals(0, index)
        }
        compose.onNodeWithTag("smart-search-clear").performClick()
        compose.onNodeWithTag("smart-search-field").performTextInput("son tung")
        compose.onNodeWithTag("smart-play-all").performClick()
        compose.runOnIdle { assertEquals(listOf("Bình yên"), queue.map { it.title }) }
    }

    @Test fun smartSearchNoResultsCanClearFilterWithoutLosingCollection() {
        val songs = listOf(song(1, "Một"), song(2, "Hai"))
        compose.setContent {
            MusicFlowTheme {
                SmartLibraryContent(songs, emptySet(), emptyList(),
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {},
                    onToggleFavorite = {})
            }
        }
        compose.onNodeWithTag("smart-search-field").performTextInput("khongtontai")
        compose.onNodeWithText("Không tìm thấy bài trong bộ sưu tập").assertExists()
        compose.onNodeWithTag("smart-play-all").assertDoesNotExist()
        compose.onNodeWithText("Xóa bộ lọc").performClick()
        compose.onNodeWithText("Hiển thị 2 / 2 bài").assertExists()
        compose.onNodeWithTag("smart-play-all").assertExists()
    }

    @Test fun changingSmartCollectionClearsOldSearch() {
        val songs = listOf(song(1, "Một"), song(2, "Hai"))
        compose.setContent {
            MusicFlowTheme {
                SmartLibraryContent(songs, emptySet(), emptyList(),
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {},
                    onToggleFavorite = {})
            }
        }
        compose.onNodeWithTag("smart-search-field").performTextInput("mot")
        // NEWEST uses the same two songs, but a previous MIX query must not hide them.
        compose.onNodeWithTag("smart-playlist-newest").performScrollTo().performClick()
        compose.onNodeWithText("Hiển thị 2 / 2 bài").assertExists()
    }

    @Test fun emptySmartCollectionCanOpenOrdinaryLibrary() {
        compose.setContent {
            MusicFlowTheme {
                LibraryScreen(uiState = LibraryUiState.Empty, hasPermission = true,
                    initialShowSmart = true, onBack = {}, onPickManual = {}, onRefresh = {},
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {})
            }
        }
        // On smaller devices the empty-state action is below the fold.
        compose.onNodeWithTag("smart-library-content").performScrollToIndex(3)
        compose.onNodeWithText("Xem mục Bài hát").performClick()
        compose.onNodeWithText("Thư viện chưa có nhạc").assertExists()
    }
}
