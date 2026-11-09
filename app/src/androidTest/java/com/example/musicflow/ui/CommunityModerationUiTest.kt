package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.feature.community.CommunityModerationPanel
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommunityModerationUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun moderationRequiresReasonAndConfirm() {
        var decisions = 0
        compose.setContent {
            MusicFlowTheme {
                CommunityModerationPanel { name, _ ->
                    when (name) {
                        "musicflow_community_reports" ->
                            """[{"id":"1","comment_id":"a","reason":"Nội dung spam","body":"Spam test","track_title":"Bài hát"}]"""
                        "musicflow_community_moderate" -> { decisions++; "true" }
                        else -> error("Unexpected RPC")
                    }
                }
            }
        }
        compose.onNodeWithTag("community-moderation-load").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Spam test").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Ẩn bình luận").performClick()
        compose.onNodeWithTag("community-moderation-confirm").assertExists()
        assertEquals(0,decisions)
        compose.onNodeWithTag("community-moderation-reason").performTextInput("Spam liên tục")
        compose.onNodeWithTag("community-moderation-confirm").performClick()
        compose.waitUntil(10_000) { decisions == 1 }
    }

    @Test fun failedPermissionDoesNotShowReports() {
        compose.setContent {
            MusicFlowTheme {
                CommunityModerationPanel { _, _ -> error("Access denied") }
            }
        }
        compose.onNodeWithTag("community-moderation-load").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Không thể tải báo cáo.",substring=true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
