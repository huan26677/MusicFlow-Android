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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.feature.library.MusicCollection
import com.example.musicflow.feature.search.SearchScreen
import com.example.musicflow.feature.search.SearchUiState
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Focused UX smoke: no media scan, accounts, remote services or saved songs. */
@RunWith(AndroidJUnit4::class)
class MusicFlowSearchUxV3Test {
    @get:Rule val compose = createComposeRule()

    @Test
    fun permissionScreenOffersManualFileAlternative() {
        var permissionRequests = 0
        var openLibrary = 0
        compose.setContent {
            MusicFlowTheme {
                SearchScreen(
                    state = SearchUiState(),
                    hasPermission = false,
                    onQueryChange = {},
                    onRequestPermission = { permissionRequests++ },
                    onRefresh = {},
                    onSongSelected = { _, _ -> },
                    onAddToPlaylist = {},
                    onOpenLibrary = { openLibrary++ }
                )
            }
        }
        compose.onNodeWithText("Cấp quyền").performClick()
        compose.onNodeWithTag("search-open-library").performClick()
        compose.runOnIdle {
            assertEquals(1, permissionRequests)
            assertEquals(1, openLibrary)
        }
    }

    @Test
    fun noScannedMusicGuidesUserToLibrary() {
        var openLibrary = 0
        compose.setContent {
            MusicFlowTheme {
                SearchScreen(
                    state = SearchUiState(isLoaded = true),
                    hasPermission = true,
                    onQueryChange = {},
                    onRequestPermission = {},
                    onRefresh = {},
                    onSongSelected = { _, _ -> },
                    onAddToPlaylist = {},
                    onOpenLibrary = { openLibrary++ }
                )
            }
        }
        compose.onNodeWithText("Không tìm thấy bài hát nào trên thiết bị").assertExists()
        compose.onNodeWithTag("search-open-library").performClick()
        compose.runOnIdle { assertEquals(1, openLibrary) }
    }

    @Test
    fun categoryCountsGuideUserToAlbumWhenNoDirectSongMatches() {
        val song = DeviceSong(
            id = 1L,
            uri = Uri.parse("content://media/external/audio/media/1"),
            title = "Bài thử nghiệm",
            artist = "MusicFlow QA",
            album = "Đà Lạt",
            durationMs = 1500
        )
        var queryAfterClear = "unchanged"
        compose.setContent {
            MusicFlowTheme {
                SearchScreen(
                    state = SearchUiState(
                        query = "da lat",
                        songs = listOf(song),
                        results = emptyList(),
                        albumResults = listOf(
                            MusicCollection("album-qa", "Đà Lạt", listOf(song))
                        ),
                        isLoaded = true
                    ),
                    hasPermission = true,
                    onQueryChange = { queryAfterClear = it },
                    onRequestPermission = {},
                    onRefresh = {},
                    onSongSelected = { _, _ -> },
                    onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithText("Bài hát (0)").assertExists()
        compose.onNodeWithText("Album (1)").assertExists()
        compose.onNodeWithText("Không tìm thấy bài hát phù hợp").assertExists()
        compose.onNodeWithTag("search-category-albums").performClick()
        compose.onNodeWithText("Đà Lạt").assertExists()
        compose.onNodeWithTag("search-category-songs").performClick()
        compose.onNodeWithText("Xóa tìm kiếm").performClick()
        compose.runOnIdle { assertEquals("", queryAfterClear) }
    }

    @Test
    fun searchHeaderRemainsUsableWithLargeTextAndNarrowWidth() {
        var refreshes = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1.8f)
            ) {
                MusicFlowTheme {
                    Box(Modifier.width(310.dp).height(700.dp)) {
                        SearchScreen(
                            state = SearchUiState(),
                            hasPermission = true,
                            onQueryChange = {},
                            onRequestPermission = {},
                            onRefresh = { refreshes++ },
                            onSongSelected = { _, _ -> },
                            onAddToPlaylist = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Tìm kiếm").assertExists()
        // Unloaded library has a single retry button in its status panel.
        compose.onNodeWithText("Quét lại").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }
}
