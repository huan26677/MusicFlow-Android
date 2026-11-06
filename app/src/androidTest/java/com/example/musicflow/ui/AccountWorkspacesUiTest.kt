package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.feature.account.AccountRolePanel
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actor workspaces use a fake RPC: never grant or revoke Supabase privileges in UI QA. */
@RunWith(AndroidJUnit4::class)
class AccountWorkspacesUiTest {
    @get:Rule val compose = createComposeRule()

    private val identity = AccountIdentity(
        userId = "00000000-0000-4000-8000-000000000042",
        email = "workspaces@example.invalid", displayName = "QA", tier = "premium",
        expiresAt = null
    )

    @Test fun ownerStartsInManagementAndCanSwitchToReviewOrCreator() {
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(identity, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" -> """{"roles":["owner"],"permissions":[]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_owner_demo_summary" -> "{}"
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("admin-search-user").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("role-workspace-management").assertExists()
        compose.onNodeWithTag("role-workspace-moderation").assertExists()
        compose.onNodeWithTag("creator-stage-name").assertDoesNotExist()
        compose.onNodeWithTag("role-workspace-moderation").performScrollTo().performClick()
        compose.onNodeWithTag("creator-review-load-pending").assertExists()
        compose.onNodeWithTag("admin-search-user").assertDoesNotExist()
        compose.onNodeWithTag("role-workspace-creator").performScrollTo().performClick()
        compose.onNodeWithTag("creator-stage-name").assertExists()
        compose.onNodeWithTag("creator-review-load-pending").assertDoesNotExist()
    }

    @Test fun reviewOnlyAdminNeverSeesUserManagement() {
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(identity, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" ->
                            """{"roles":["platform_admin"],"permissions":["review_creators"]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-review-load-pending")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("role-workspace-management").assertDoesNotExist()
        compose.onNodeWithTag("role-workspace-moderation").assertExists()
        compose.onNodeWithTag("admin-search-user").assertDoesNotExist()
    }

    @Test fun revokedAdminRoleReturnsToNonPrivilegedWorkspace() {
        var accessChecks = 0
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(identity, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" -> {
                            accessChecks++
                            if (accessChecks == 1) {
                                """{"roles":["platform_admin"],"permissions":["manage_users"]}"""
                            } else """{"roles":[],"permissions":[]}"""
                        }
                        "musicflow_my_creator_requests" -> "[]"
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("admin-search-user").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("role-access-refresh").performScrollTo().performClick()
        compose.waitUntil(10_000) { accessChecks >= 2 }
        compose.waitForIdle()
        compose.onNodeWithTag("role-workspace-management").assertDoesNotExist()
        compose.onNodeWithTag("admin-search-user").assertDoesNotExist()
        compose.onNodeWithTag("role-workspace-creator").assertExists()
        compose.onNodeWithTag("creator-stage-name").assertExists()
        assertTrue(accessChecks >= 2)
    }
}
