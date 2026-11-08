package com.example.musicflow.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.LocalMusicRepository
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.feature.library.SongRow
import com.example.musicflow.feature.search.OfflineSearchRepository
import com.example.musicflow.feature.search.SearchScreen
import com.example.musicflow.feature.search.SearchUiState
import com.example.musicflow.ui.theme.MusicFlowTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Scoped UI + repository tests: no account, billing or real device file changed. */
@RunWith(AndroidJUnit4::class)
class AccessibilityOfflineSearchV5Test {
    @get:Rule val compose = createComposeRule()

    private fun song(id: Int, title: String, uri: String =
        "content://media/external/audio/media/$id"
    ) = DeviceSong(
        id = id.toLong(), uri = Uri.parse(uri), title = title,
        artist = "Nghệ sĩ QA", durationMs = 40_000L
    )

    @Test fun offlineSearchRepositoryMergesDeduplicatesAndRespectsRevocation() {
        val first = song(1, "Nhạc trên máy")
        val picked = song(2, "Đường xa", "content://com.android.providers.downloads.documents/document/2")
        val duplicate = picked.copy(title = "Đường xa bản chọn")
        var mayScan = false
        var scans = 0
        val fake = object : LocalMusicRepository {
            override suspend fun getSongs(): List<DeviceSong> {
                scans++
                return listOf(first, picked)
            }
        }
        val repository = OfflineSearchRepository(
            fake, mayScanDevice = { mayScan },
            readablePickedSongs = { listOf(duplicate) }
        )
        assertEquals(listOf(duplicate.uri), runBlocking { repository.getSongs() }.map { it.uri })
        assertEquals(0, scans)
        mayScan = true
        val merged = runBlocking { repository.getSongs() }
        assertEquals(listOf(first.uri, picked.uri), merged.map { it.uri })
        assertEquals("Đường xa", merged.last().title)
        assertEquals(1, scans)
        mayScan = false
        assertEquals(listOf(duplicate.uri), runBlocking { repository.getSongs() }.map { it.uri })
        assertEquals(1, scans)
    }

    @Test fun unavailableMediaStoreFallsBackToReadablePickedDocuments() {
        val picked = song(3, "Tệp của tôi", "content://documents/3")
        val repository = OfflineSearchRepository(
            object : LocalMusicRepository {
                override suspend fun getSongs(): List<DeviceSong> {
                    throw SecurityException("permission revoked")
                }
            },
            mayScanDevice = { true },
            readablePickedSongs = { listOf(picked) }
        )
        assertEquals(listOf(picked.uri), runBlocking { repository.getSongs() }.map { it.uri })
    }

    @Test fun noPermissionStillSearchesAndPlaysOnlySelectedSafResults() {
        val songs = listOf(
            song(3, "Đường xa", "content://documents/music3"),
            song(4, "Mùa xuân", "content://documents/music4")
        )
        var queue = emptyList<DeviceSong>()
        var selected = -1
        compose.setContent {
            MusicFlowTheme {
                SearchScreen(
                    state = SearchUiState(
                        query = "duong", songs = songs, results = songs.take(1),
                        isLoaded = true
                    ),
                    hasPermission = false, onQueryChange = {}, onRefresh = {},
                    onRequestPermission = {}, onAddToPlaylist = {},
                    onSongSelected = { items, index -> queue = items; selected = index }
                )
            }
        }
        compose.onNodeWithTag("search-saf-without-permission").assertExists()
        compose.onNodeWithTag("search-query").assertExists()
        compose.onNodeWithText("Đường xa").performClick()
        compose.runOnIdle {
            assertEquals(listOf("Đường xa"), queue.map { it.title })
            assertEquals(0, selected)
        }
    }

    @Test fun missingPermissionShowsTwoDistinctOptInPaths() {
        var grants = 0
        var library = 0
        compose.setContent {
            MusicFlowTheme {
                SearchScreen(
                    state = SearchUiState(isLoaded = true), hasPermission = false,
                    onQueryChange = {}, onRefresh = {},
                    onRequestPermission = { grants++ },
                    onOpenLibrary = { library++ },
                    onSongSelected = { _, _ -> }, onAddToPlaylist = {}
                )
            }
        }
        compose.onNodeWithText("Chưa có nhạc đã chọn").assertExists()
        compose.onNodeWithTag("search-request-device-scan").performClick()
        compose.onNodeWithTag("search-open-library").performClick()
        compose.runOnIdle {
            assertEquals(1, grants)
            assertEquals(1, library)
        }
    }

    @Test fun compactSongRowMakesFavoritesAndPlaylistEasyToReach() {
        var favorites = 0
        var added = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                MusicFlowTheme {
                    Box(Modifier.width(310.dp).height(170.dp)) {
                        SongRow(
                            song = song(7, "Bài hát có tiêu đề dài"),
                            onClick = {},
                            onToggleFavorite = { favorites++ },
                            onAddToPlaylist = { added++ }
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Tùy chọn cho Bài hát có tiêu đề dài")
            .performClick()
        compose.onNodeWithText("Yêu thích").performClick()
        compose.onNodeWithTag("song-compact-actions").performClick()
        compose.onNodeWithText("Thêm vào playlist").performClick()
        compose.runOnIdle {
            assertEquals(1, favorites)
            assertEquals(1, added)
        }
    }

    @Test fun narrowLibraryDoesNotForcePermissionAndShowsBothActions() {
        var scan = 0
        var picks = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.65f)) {
                MusicFlowTheme {
                    Box(Modifier.width(320.dp).height(800.dp)) {
                        LibraryScreen(
                            uiState = LibraryUiState.Idle, hasPermission = false,
                            onBack = {}, onPickManual = { picks++ },
                            onRefresh = { scan++ },
                            onSongSelected = { _, _ -> }, onAddToPlaylist = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("library-scan-action").assertExists().performClick()
        compose.onNodeWithTag("library-pick-action").assertExists().performClick()
        compose.runOnIdle {
            assertEquals(1, scan)
            assertEquals(1, picks)
        }
    }
}
