package com.example.musicflow.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.playback.PlayerUiState
import com.example.musicflow.feature.home.HomeHistoryUiState
import com.example.musicflow.feature.home.HomeScreen
import com.example.musicflow.feature.library.LibraryScreen
import com.example.musicflow.feature.library.LibraryUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Focused UX smoke; never signs in or changes an existing device's data. */
@RunWith(AndroidJUnit4::class)
class MusicFlowUxFoundationV3Test {
    @get:Rule val compose = createComposeRule()

    @Test
    fun primaryHomeActionsAndOneTimeGuideCanBeDismissed() {
        var openedLibrary = 0
        var openedSearch = 0
        var openedPlaylists = 0
        compose.setContent {
            var showGuide by remember { mutableStateOf(true) }
            HomeScreen(
                state = PlayerUiState(),
                historyState = HomeHistoryUiState.Empty,
                onOpenLibrary = { openedLibrary++ },
                onOpenSearch = { openedSearch++ },
                onOpenPlaylists = { openedPlaylists++ },
                onOpenPlayer = {},
                onRecentSongSelected = { _, _ -> },
                actionError = null,
                onDismissActionError = {},
                onRemoveRecentSong = {},
                onClearHistory = {},
                showFirstUseGuide = showGuide,
                onDismissFirstUseGuide = { showGuide = false }
            )
        }
        compose.onNodeWithTag("home-library-action").performClick()
        compose.onNodeWithTag("home-search-action").performClick()
        compose.onNodeWithTag("home-playlist-action").performClick()
        compose.onNodeWithTag("home-first-use-guide").assertExists()
        compose.onNodeWithTag("home-guide-dismiss").performScrollTo().performClick()
        compose.onNodeWithTag("home-first-use-guide").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, openedLibrary)
            assertEquals(1, openedSearch)
            assertEquals(1, openedPlaylists)
        }
    }

    @Test
    fun libraryPickerUsesGenericAudioLabelAndKeepsAction() {
        var opened = 0
        compose.setContent {
            LibraryScreen(
                uiState = LibraryUiState.Empty,
                hasPermission = true,
                onBack = {},
                onPickManual = { opened++ },
                onRefresh = {},
                onSongSelected = { _, _ -> },
                onAddToPlaylist = {}
            )
        }
        compose.onNodeWithText("Chọn tệp nhạc").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }
}
