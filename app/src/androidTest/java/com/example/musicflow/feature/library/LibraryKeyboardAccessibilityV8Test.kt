package com.example.musicflow.feature.library

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Hardware-key / accessibility semantics contracts on a disposable emulator.
 * These do not claim manual TalkBack or physical Bluetooth keyboard testing.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class LibraryKeyboardAccessibilityV8Test {
    @get:Rule val compose = createComposeRule()
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun song(id: Int, albumId: Long = 11L) = DeviceSong(
        id = id.toLong(),
        uri = Uri.parse("content://media/external/audio/media/$id"),
        title = "Keyboard song $id",
        artist = "Artist QA",
        album = "Album QA",
        albumId = albumId,
        durationMs = 90_000L
    )

    private fun assertExpanded(tag: String, expected: Boolean) {
        compose.onNodeWithTag(tag).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                if (expected) "Đang mở" else "Đang thu gọn"
            )
        )
    }

    @Test fun keyboardEnterCanExpandAndCollapseArtistWithoutTouch() {
        val group = MusicCollection("artist-qa", "Artist QA", listOf(song(1), song(2)))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MusicFlowTheme {
                    Column(Modifier.width(320.dp).height(640.dp)) {
                        MusicCollectionsContent(
                            title = "nghệ sĩ", collections = listOf(group),
                            favoriteSongUris = emptySet(),
                            onToggleFavorite = {}, onAddToPlaylist = {},
                            onSongSelected = { _, _ -> }
                        )
                    }
                }
            }
        }
        val tag = "collection-header-artist-qa"
        // A real hardware key exits Android touch-mode before focus is requested.
        device.pressDPadDown()
        compose.waitForIdle()
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        assertExpanded(tag, false)
        device.pressEnter()
        compose.waitForIdle()
        assertExpanded(tag, true)
        device.pressEnter()
        compose.waitForIdle()
        assertExpanded(tag, false)
    }

    @Test fun dpadMovesFocusBetweenCollectionHeaders() {
        val groups = listOf(
            MusicCollection("first", "First", listOf(song(1))),
            MusicCollection("second", "Second", listOf(song(2)))
        )
        compose.setContent {
            MusicFlowTheme {
                Column(Modifier.width(360.dp).height(570.dp)) {
                    MusicCollectionsContent(
                        title = "nghệ sĩ", collections = groups,
                        favoriteSongUris = emptySet(), onToggleFavorite = {},
                        onAddToPlaylist = {}, onSongSelected = { _, _ -> }
                    )
                }
            }
        }
        device.pressDPadDown()
        compose.waitForIdle()
        compose.onNodeWithTag("collection-header-first")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        device.pressDPadDown()
        compose.waitForIdle()
        compose.onNodeWithTag("collection-header-second").assertIsFocused()
    }

    @Test fun albumHeaderReceivesKeyboardEnterWithNoUnexpectedPlayback() {
        var played = 0
        compose.setContent {
            MusicFlowTheme {
                Column(Modifier.width(360.dp).height(750.dp)) {
                    LibraryScreen(
                        uiState = LibraryUiState.Success(listOf(song(1), song(2))),
                        hasPermission = true, initialShowAlbums = true,
                        onBack = {}, onPickManual = {}, onRefresh = {},
                        onSongSelected = { _, _ -> played++ },
                        onAddToPlaylist = {}
                    )
                }
            }
        }
        val tag = "album-header-11"
        compose.onNodeWithTag(tag).performScrollTo()
        device.pressDPadDown()
        compose.waitForIdle()
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        device.pressEnter()
        compose.waitForIdle()
        assertExpanded(tag, true)
        device.pressEnter()
        compose.waitForIdle()
        assertExpanded(tag, false)
        compose.runOnIdle { assertEquals(0, played) }
    }
}
