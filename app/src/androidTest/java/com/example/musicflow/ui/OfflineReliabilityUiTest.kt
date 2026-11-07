package com.example.musicflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.feature.playlist.PlaylistBackupControls
import com.example.musicflow.feature.playlist.PlaylistTrackOptions
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Local-only UI: never selects a real user's file or creates playlist data. */
@RunWith(AndroidJUnit4::class)
class OfflineReliabilityUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun backupToolsCollapseByDefaultAndExpandOnDemand() {
        compose.setContent { MusicFlowTheme { PlaylistBackupControls() } }
        compose.onNodeWithTag("playlist-backup-toggle").assertExists()
        compose.onNodeWithTag("playlist-backup-import").assertDoesNotExist()
        compose.onNodeWithTag("playlist-backup-toggle").performClick()
        compose.onNodeWithTag("playlist-backup-export").assertExists()
        compose.onNodeWithTag("playlist-backup-import").assertExists()
        compose.onNodeWithTag("playlist-backup-toggle").performClick()
        compose.onNodeWithTag("playlist-backup-import").assertDoesNotExist()
    }

    @Test fun relinkActionIsVisibleAndDoesNotTriggerDeleteAtLargeFonts() {
        var relinks = 0
        var deletes = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.65f)) {
                Box(Modifier.width(320.dp).height(690.dp)) {
                    MusicFlowTheme {
                        PlaylistTrackOptions(
                            mediaId = "qa-missing", title = "Bài hát cần chọn lại",
                            canMoveUp = false, canMoveDown = false,
                            onMoveUp = {}, onMoveDown = {}, onRequestRemove = { deletes++ },
                            onRelink = { relinks++ }
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("playlist-track-options-qa-missing").performClick()
        compose.onNodeWithTag("playlist-track-relink").performClick()
        compose.runOnIdle {
            assertEquals(1, relinks)
            assertEquals(0, deletes)
        }
    }
}
