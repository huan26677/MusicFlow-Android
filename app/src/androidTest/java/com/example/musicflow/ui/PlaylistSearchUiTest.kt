package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import com.example.musicflow.feature.playlist.PlaylistListRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Creates and removes only one isolated QA playlist on the test-owned emulator. */
@RunWith(AndroidJUnit4::class)
class PlaylistSearchUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun accentInsensitiveSearchOpensCorrectPlaylistAndClearsEmptyState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = RoomPlaylistRepository(RecentHistoryDatabase.getInstance(context))
        val name = "Nhạc thư giãn QA ${System.nanoTime()}"
        val playlist = runBlocking { repository.create(name) }
        assertNotNull(playlist)
        val ownedPlaylist = checkNotNull(playlist)
        var selectedId: String? = null

        try {
            compose.setContent {
                PlaylistListRoute(
                    onBack = {},
                    onOpenPlaylist = { selectedId = it }
                )
            }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("playlist-search")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("playlist-search").performTextReplacement("nhac thu")
            compose.onNodeWithText(name).assertExists().performClick()
            compose.runOnIdle { assertEquals(ownedPlaylist.id, selectedId) }

            compose.onNodeWithTag("playlist-search")
                .performTextReplacement("khong co playlist")
            compose.onNodeWithText("Không tìm thấy playlist").assertExists()
            compose.onNodeWithText("Xóa tìm kiếm").performClick()
            compose.onNodeWithText(name).assertExists()
        } finally {
            runBlocking { repository.deletePlaylist(ownedPlaylist.id) }
        }
    }
}
