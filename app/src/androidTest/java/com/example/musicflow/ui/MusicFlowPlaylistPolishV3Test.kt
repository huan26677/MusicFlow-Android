package com.example.musicflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.feature.playlist.PlaylistTrackOptions
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Focused, isolated UI tests: no Room writes, audio or Supabase access. */
@RunWith(AndroidJUnit4::class)
class MusicFlowPlaylistPolishV3Test {
    @get:Rule val compose = createComposeRule()

    @Test
    fun trackMenuWorksAtLargeFontWithoutImmediateDeletion() {
        var movedUp = 0
        var movedDown = 0
        var deletionRequested = 0
        compose.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(current.density, 1.8f)
            ) {
                MusicFlowTheme {
                    Box(Modifier.width(310.dp).height(700.dp)) {
                        PlaylistTrackOptions(
                            mediaId = "qa-2",
                            title = "Bài hát có tên rất dài để thử",
                            canMoveUp = true,
                            canMoveDown = true,
                            onMoveUp = { movedUp++ },
                            onMoveDown = { movedDown++ },
                            onRequestRemove = { deletionRequested++ }
                        )
                    }
                }
            }
        }
        val trigger = compose.onNodeWithTag("playlist-track-options-qa-2")
        trigger.performClick()
        compose.onNodeWithTag("playlist-track-move-up").performClick()
        trigger.performClick()
        compose.onNodeWithTag("playlist-track-move-down").performClick()
        compose.runOnIdle {
            assertEquals(1, movedUp)
            assertEquals(1, movedDown)
            assertEquals(0, deletionRequested)
        }
        trigger.performClick()
        compose.onNodeWithTag("playlist-track-request-delete").performClick()
        compose.runOnIdle { assertEquals(1, deletionRequested) }
    }

    @Test
    fun firstTrackDisablesMovingUpButAllowsMovingDown() {
        var movedUp = 0
        var movedDown = 0
        compose.setContent {
            MusicFlowTheme {
                PlaylistTrackOptions(
                    mediaId = "qa-first",
                    title = "Đầu danh sách",
                    canMoveUp = false,
                    canMoveDown = true,
                    onMoveUp = { movedUp++ },
                    onMoveDown = { movedDown++ },
                    onRequestRemove = {}
                )
            }
        }
        compose.onNodeWithTag("playlist-track-options-qa-first").performClick()
        compose.onNodeWithTag("playlist-track-move-up").assertIsNotEnabled()
        compose.onNodeWithTag("playlist-track-move-down").performClick()
        compose.runOnIdle {
            assertEquals(0, movedUp)
            assertEquals(1, movedDown)
        }
    }
}
