package com.example.musicflow.core.playback

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
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

    @Test
    fun actionButtonsRemainReachableAt320dpAnd200PercentFont() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Box(Modifier.width(320.dp).height(690.dp)) {
                    QueueManagerScreen(
                        state = state, onBack = {},
                        onMove = { _, _, _ -> true },
                        onRemove = { _, _ -> true },
                        onClearOthers = { true }
                    )
                }
            }
        }
        // LazyColumn does not compose offscreen rows until its owner scrolls.
        compose.onNodeWithTag("queue-items-list").performScrollToIndex(3)
        compose.onNodeWithContentDescription("Đưa Bài 4 lên").assertIsDisplayed()
        compose.onNodeWithTag("queue-items-list").performScrollToIndex(2)
        compose.onNodeWithContentDescription("Xóa Bài 3 khỏi hàng đợi").assertIsDisplayed()
    }
}
