package com.example.musicflow.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.feature.account.AccountRolePanel
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** All RPC responses are in-memory fixtures; no server accounts are changed. */
@RunWith(AndroidJUnit4::class)
class CreatorApplicationReviewUiTest {
    @get:Rule val compose = createComposeRule()

    private val qa = AccountIdentity(
        userId = "00000000-0000-4000-8000-000000000045",
        email = "review@example.invalid", displayName = "QA", tier = "free", expiresAt = null
    )
    private val pending = """[{"id":"00000000-0000-4000-8000-000000000099",
        "email":"creator@example.invalid","requested_role":"artist",
        "requested_name":"Demo Artist","note":"Sample metadata only"}]"""

    @Test fun pendingArtistRequestIsShownAndCannotBeResubmitted() {
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(qa, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" -> """{"roles":[],"permissions":[]}"""
                        "musicflow_my_creator_requests" -> """[{
                            "requested_role":"artist","requested_name":"Demo Artist",
                            "status":"pending"}]"""
                        else -> error("Unexpected application RPC: $name")
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-pending-hint").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-stage-name").performTextInput("Another Artist")
        compose.onNodeWithTag("creator-request").assertIsNotEnabled()
        compose.onNodeWithTag("creator-request-status").assertExists()
        compose.onNodeWithTag("creator-review-approve").assertDoesNotExist()
    }

    @Test fun adminDecisionWaitsForExplicitConfirmationAndCanBeCancelled() {
        var reviews = 0
        var lastId = ""
        var accepted = false
        var reason = ""
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(qa, onClose = {}) { name, args ->
                    when (name) {
                        "musicflow_my_access" ->
                            """{"roles":["platform_admin"],"permissions":["review_creators"]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_pending_creators" -> pending
                        "musicflow_review_creator" -> {
                            reviews++
                            lastId = args.optString("p_application")
                            accepted = args.optBoolean("p_approve")
                            reason = args.optString("p_reason")
                            "true"
                        }
                        else -> error("Unexpected application RPC: $name")
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-review-reason").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-review-reason")
            .performScrollTo().performTextInput("Hồ sơ thử nghiệm đầy đủ")
        compose.onNodeWithTag("creator-review-approve").assertDoesNotExist()
        compose.onNodeWithTag("creator-load-requests").assertExists()
        // The reviewer list is loaded only through a dedicated admin RPC.
        compose.onNodeWithTag("creator-review-load-pending").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-review-approve").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-review-approve").performScrollTo().performClick()
        compose.onNodeWithTag("creator-review-confirm").assertExists()
        assertEquals(0, reviews)
        compose.onNodeWithTag("creator-review-cancel").performClick()
        assertEquals(0, reviews)
        compose.onNodeWithTag("creator-review-approve").performScrollTo().performClick()
        compose.onNodeWithTag("creator-review-confirm").performClick()
        compose.waitUntil(10_000) { reviews == 1 }
        compose.waitForIdle()
        assertEquals("00000000-0000-4000-8000-000000000099", lastId)
        assertTrue(accepted)
        assertEquals("Hồ sơ thử nghiệm đầy đủ", reason)
        compose.onNodeWithTag("creator-review-approve").assertDoesNotExist()
    }

    @Test fun serverRefusalPreservesPendingReview() {
        var reviews = 0
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(qa, onClose = {}) { name, args ->
                    when (name) {
                        "musicflow_my_access" ->
                            """{"roles":["platform_admin"],"permissions":["review_creators"]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_pending_creators" -> pending
                        "musicflow_review_creator" -> {
                            reviews++
                            assertFalse(args.optBoolean("p_approve"))
                            "false" // No confirmation from the mocked server.
                        }
                        else -> error("Unexpected application RPC: $name")
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-review-reason").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-review-reason")
            .performScrollTo().performTextInput("Chưa đủ tài liệu thử nghiệm")
        compose.onNodeWithTag("creator-review-load-pending").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-review-reject").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-review-reject").performScrollTo().performClick()
        compose.onNodeWithTag("creator-review-confirm").performClick()
        compose.waitUntil(10_000) { reviews == 1 }
        compose.waitForIdle()
        compose.onNodeWithTag("creator-review-reject").assertExists()
    }
}
