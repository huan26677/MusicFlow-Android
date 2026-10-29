package com.example.musicflow.core.playback

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.feature.player.QueueManagerScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QueueManagerScreenTest {
    @get:Rule val compose = createComposeRule()

    private val state = PlayerUiState(
        connected = true,
        hasMedia = true,
        currentIndex = 2,
        queueCount = 4,
        canEditQueue = true,
        queueVersion = 42L,
        queueItems = (1..4).map {
            QueueTrackUi("qa-track-$it", "Bài $it", "Nghệ sĩ")
        }
    )

    @Test
    fun actionsUseCurrentTimelineVersionAndCurrentRowIsProtected() {
        var moved: Triple<Int, Int, Long>? = null
        var removed: Pair<Int, Long>? = null
        compose.setContent {
            QueueManagerScreen(
                state = state,
                onBack = {},
                onMove = { from, to, version ->
                    moved = Triple(from, to, version)
                    true
                },
                onRemove = { index, version ->
                    removed = index to version
                    true
                },
                onClearOthers = { true }
            )
        }
        compose.onNodeWithContentDescription("Xóa Bài 2 khỏi hàng đợi")
            .assertIsNotEnabled()
        compose.onNodeWithContentDescription("Đưa Bài 4 lên").performClick()
        compose.onNodeWithContentDescription("Xóa Bài 3 khỏi hàng đợi").performClick()
        compose.runOnIdle {
            assertEquals(Triple(3, 2, 42L), moved)
            assertEquals(2 to 42L, removed)
        }
    }

    @Test
    fun clearingRequiresConfirmationAndKeepsCurrent() {
        var clearedVersion: Long? = null
        compose.setContent {
            QueueManagerScreen(
                state = state,
                onBack = {},
                onMove = { _, _, _ -> false },
                onRemove = { _, _ -> false },
                onClearOthers = { version ->
                    clearedVersion = version
                    true
                }
            )
        }
        compose.onNodeWithText("Dọn hàng đợi (giữ bài đang phát)").performClick()
        compose.onNodeWithText("Dọn hàng đợi?").assertExists()
        compose.runOnIdle { assertEquals(null, clearedVersion) }
        compose.onNodeWithText("Dọn hàng đợi").performClick()
        compose.runOnIdle { assertEquals(42L, clearedVersion) }
    }
}
