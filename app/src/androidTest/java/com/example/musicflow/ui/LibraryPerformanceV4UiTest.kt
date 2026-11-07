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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibrarySongSearchIndex
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Deterministic, local-only fixture data; no device songs or external accounts. */
@RunWith(AndroidJUnit4::class)
class LibraryPerformanceV4UiTest {
    @get:Rule val compose = createComposeRule()

    private fun song(id: Int, title: String, artist: String = "Nghệ sĩ QA") = DeviceSong(
        id = id.toLong(), uri = Uri.parse("content://media/external/audio/media/$id"),
        title = title, artist = artist, durationMs = 240_000L
    )

    @Test fun preparedIndexMatchesVietnameseAndPreservesLargeCatalogOrder() {
        val catalog = (1..1500).map { song(it, "Bài hát QA $it") } +
            listOf(song(2001, "Đường về A", "Sơn Tùng"),
                song(2002, "Đường về B", "Nguyễn Phong"))
        val original = catalog.map { it.uri }
        val index = LibrarySongSearchIndex(catalog)
        assertEquals(listOf("Đường về A", "Đường về B"),
            index.filter(" DUONG   VE ").map { it.title })
        assertEquals(listOf("Đường về A"), index.filter("SON TUNG").map { it.title })
        assertTrue(index.filter("khongco").isEmpty())
        assertEquals(original, index.filter("").map { it.uri })
        assertEquals(1502, catalog.size)
    }

    @Test fun songSearchPlaysOnlyFilteredQueueAndPreservesIndex() {
        val tracks = listOf(song(1, "Đường về A"), song(2, "Lối nhỏ"),
            song(3, "Đường về B"))
        var playback = emptyList<DeviceSong>()
        var selected = -1
        compose.setContent {
            MusicFlowTheme {
                LibraryScreen(
                    uiState = LibraryUiState.Success(tracks), hasPermission = true,
                    onBack = {}, onPickManual = {}, onRefresh = {},
                    onSongSelected = { queue, index -> playback = queue; selected = index },
                    onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithTag("library-search-field").performTextInput("duong ve")
        compose.onNodeWithText("Hiển thị 2 / 3 bài").assertExists()
        compose.onNodeWithTag("library-songs-list").performScrollToIndex(5)
        compose.onNodeWithText("Đường về B").performClick()
        compose.runOnIdle {
            assertEquals(listOf("Đường về A", "Đường về B"), playback.map { it.title })
            assertEquals(1, selected)
        }
    }

    @Test fun noResultsHaveClearActionAndSearchDoesNotDeleteMusic() {
        val tracks = listOf(song(1, "Một"), song(2, "Hai"))
        compose.setContent {
            MusicFlowTheme {
                LibraryScreen(
                    uiState = LibraryUiState.Success(tracks), hasPermission = true,
                    onBack = {}, onPickManual = {}, onRefresh = {},
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithTag("library-search-field").performTextInput("khongtontai")
        compose.onNodeWithText("Hiển thị 0 / 2 bài").assertExists()
        compose.onNodeWithTag("library-search-clear").performClick()
        compose.onNodeWithText("Hiển thị 2 / 2 bài").assertExists()
        assertEquals(2, tracks.size)
    }

    @Test fun pickedMusicSearchIsAccentInsensitiveAndQueueMatchesFilter() {
        val chosen = listOf(song(3, "Đường xa"), song(4, "Mùa xuân"))
        var playback = emptyList<DeviceSong>()
        compose.setContent {
            MusicFlowTheme {
                LibraryScreen(
                    uiState = LibraryUiState.Empty, hasPermission = false,
                    pickedSongs = chosen, onBack = {}, onPickManual = {}, onRefresh = {},
                    onSongSelected = { queue, _ -> playback = queue }, onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithTag("library-category-picked").performClick()
        compose.onNodeWithTag("picked-search-field").performTextInput("duong")
        compose.onNodeWithText("Hiển thị 1 / 2 bài").assertExists()
        compose.onNodeWithText("Đường xa").performClick()
        compose.runOnIdle { assertEquals(listOf("Đường xa"), playback.map { it.title }) }
    }

    @Test fun deniedLibraryPermissionOffersFriendlyOfflineAlternative() {
        var pickerOpens = 0
        compose.setContent {
            MusicFlowTheme {
                LibraryScreen(
                    uiState = LibraryUiState.Idle, hasPermission = false,
                    onBack = {}, onPickManual = { pickerOpens++ }, onRefresh = {},
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithText("Chỉ cần cấp quyền nếu muốn quét cả máy").assertExists()
        compose.onNodeWithTag("library-permission-open-picked").performClick()
        compose.onNodeWithText("Chưa có nhạc đã chọn").assertExists()
        compose.runOnIdle { assertEquals(0, pickerOpens) }
    }

    @Test fun genreTabRequestsMetadataOnlyAfterItIsSelected() {
        var genreRequests = 0
        compose.setContent {
            MusicFlowTheme {
                LibraryScreen(
                    uiState = LibraryUiState.Success(listOf(song(1, "Bài 1"))),
                    hasPermission = true,
                    onBack = {}, onPickManual = {}, onRefresh = {},
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {},
                    onGenresSelected = { genreRequests++ }
                )
            }
        }
        compose.runOnIdle { assertEquals(0, genreRequests) }
        compose.onNodeWithTag("library-category-genres").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, genreRequests) }
    }

    @Test fun largeFontSearchAndSortRemainReachableInOneScrollableList() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.65f)) {
                Box(Modifier.width(320.dp).height(740.dp)) {
                    MusicFlowTheme {
                        LibraryScreen(
                            uiState = LibraryUiState.Success(listOf(song(1, "Một bài"))),
                            hasPermission = true, onBack = {}, onPickManual = {},
                            onRefresh = {}, onSongSelected = { _, _ -> }, onAddToPlaylist = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("library-songs-list").performScrollToIndex(3)
        compose.onNodeWithTag("library-sort-button").performClick()
        compose.onNodeWithTag("library-sort-newest").performClick()
        compose.onNodeWithTag("library-songs-list").performScrollToIndex(1)
        compose.onNodeWithTag("library-search-field").performTextInput("mot")
        compose.onNodeWithTag("library-songs-list").performScrollToIndex(2)
        compose.onNodeWithText("Hiển thị 1 / 1 bài").assertExists()
    }
}
