package com.example.musicflow.feature.library

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.ui.theme.MusicFlowTheme
import com.example.musicflow.ui.theme.ReadabilityContrast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose lazy-item/semantic tests, deterministic fixtures, no device music changes. */
@RunWith(AndroidJUnit4::class)
class LibraryAccessibilityV7Test {
    @get:Rule val compose = createComposeRule()

    private fun song(number: Int, artist: String = "Nghệ sĩ QA") = DeviceSong(
        id = number.toLong(),
        uri = Uri.parse("content://media/external/audio/media/$number"),
        title = "Track $number",
        artist = artist,
        album = "Album QA",
        albumId = 42L,
        durationMs = 120_000L
    )

    @Test fun collapsedCollectionsDoNotCreateHundredsOfSongRows() {
        val group = MusicCollection("artist-qa", "Nghệ sĩ QA", (1..600).map(::song))
        val collapsed = collectionRows(listOf(group), null)
        assertEquals(1, collapsed.size)
        assertTrue(collapsed.single() is MusicCollectionRow.Header)
        val expanded = collectionRows(listOf(group), group.key)
        assertEquals(602, expanded.size)
        assertEquals(600, (expanded.last() as MusicCollectionRow.Track).index + 1)
        assertEquals(602, expanded.map { it.key }.distinct().size)
    }

    @Test fun albumRowsPreservePlaybackIndicesAndDoNotCreateDuplicateKeys() {
        val songs = (1..600).map(::song)
        val group = listOf(42L to songs, 99L to listOf(song(1001)))
        assertEquals(2, albumRows(group, null).size)
        val rows = albumRows(group, 42L)
        assertEquals(603, rows.size)
        assertEquals(600, (rows[601] as AlbumRow.Track).index + 1)
        assertEquals(rows.size, rows.map { it.key }.distinct().size)
    }

    @Test fun artistTracksStayVirtualizedAndKeepCorrectPlaybackQueueAt200Percent() {
        val songs = (1..600).map(::song)
        val group = MusicCollection("artist-qa", "Nghệ sĩ QA", songs)
        var queue = emptyList<DeviceSong>()
        var selectedIndex = -1
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MusicFlowTheme {
                    Column(Modifier.width(320.dp).height(720.dp)) {
                        MusicCollectionsContent(
                            title = "nghệ sĩ", collections = listOf(group),
                            favoriteSongUris = emptySet(),
                            onToggleFavorite = {},
                            onAddToPlaylist = {},
                            onSongSelected = { list, index ->
                                queue = list
                                selectedIndex = index
                            }
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("collection-header-artist-qa")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Đang thu gọn"))
            .performClick()
        compose.onNodeWithTag("collection-header-artist-qa")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Đang mở"))
        compose.onNodeWithText("Track 500").assertDoesNotExist()
        compose.onNodeWithTag("music-collections-list").performScrollToIndex(501)
        compose.onNodeWithText("Track 500").performClick()
        compose.runOnIdle {
            assertEquals(600, queue.size)
            assertEquals(499, selectedIndex)
        }
    }

    @Test fun albumTracksStayVirtualizedInRealLibraryAt200Percent() {
        val songs = (1..600).map(::song)
        var queueSize = 0
        var selectedIndex = -1
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MusicFlowTheme {
                    Column(Modifier.width(320.dp).height(820.dp)) {
                        LibraryScreen(
                            uiState = LibraryUiState.Success(songs), hasPermission = true,
                            onBack = {}, onPickManual = {}, onRefresh = {},
                            onSongSelected = { queue, index ->
                                queueSize = queue.size
                                selectedIndex = index
                            },
                            onAddToPlaylist = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("library-category-albums").performScrollTo().performClick()
        compose.onNodeWithTag("album-header-42").performClick()
        compose.onNodeWithText("Track 500").assertDoesNotExist()
        compose.onNodeWithTag("library-albums-list").performScrollToIndex(501)
        compose.onNodeWithText("Track 500").performClick()
        compose.runOnIdle {
            assertEquals(600, queueSize)
            assertEquals(499, selectedIndex)
        }
    }

    private fun assertThemeContrast(dark: Boolean) {
        lateinit var scheme: ColorScheme
        compose.setContent {
            MusicFlowTheme(darkTheme = dark) {
                scheme = MaterialTheme.colorScheme
            }
        }
        compose.runOnIdle {
            val pairs: Map<String, Pair<Color, Color>> = mapOf(
                "primary action" to (scheme.onPrimary to scheme.primary),
                "secondary action" to (scheme.onSecondary to scheme.secondary),
                "tertiary action" to (scheme.onTertiary to scheme.tertiary),
                "body text" to (scheme.onSurface to scheme.surface),
                "subtle text" to (scheme.onSurfaceVariant to scheme.surfaceContainerLow),
                "primary container" to (scheme.onPrimaryContainer to scheme.primaryContainer)
            )
            pairs.forEach { (label, colors) ->
                val ratio = ReadabilityContrast.ratio(colors.first, colors.second)
                assertTrue("$label must pass WCAG AA 4.5:1; actual ratio=$ratio", ratio >= 4.5)
            }
        }
    }

    @Test fun lightThemePrimaryTextTokensMeetWcagAa() = assertThemeContrast(false)

    @Test fun darkThemePrimaryTextTokensMeetWcagAa() = assertThemeContrast(true)
}
