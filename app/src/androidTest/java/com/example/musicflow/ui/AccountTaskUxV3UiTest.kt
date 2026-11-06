package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.feature.account.AccountRolePanel
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** No live credentials, backend changes or licensed media operations. */
@RunWith(AndroidJUnit4::class)
class AccountTaskUxV3UiTest {
    @get:Rule val compose = createComposeRule()

    private val identity = AccountIdentity(
        userId = "00000000-0000-4000-8000-000000000063",
        email = "persona@example.invalid", displayName = "QA", tier = "free", expiresAt = null
    )

    @Test fun ownerStartsWithUserWorkAndOpensOverviewOnlyWhenAsked() {
        var overviewQueries = 0
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(identity, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" -> """{"roles":["owner"],"permissions":[]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_owner_demo_summary" -> { overviewQueries++; "{}" }
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("admin-search-user").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("workspace-next-step").assertExists()
        compose.onNodeWithTag("owner-overview-toggle").assertExists()
        compose.onNodeWithTag("owner-demo-dashboard").assertDoesNotExist()
        assertEquals(0, overviewQueries)
        compose.onNodeWithTag("owner-overview-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("owner-demo-dashboard").assertExists()
        compose.waitUntil(10_000) { overviewQueries == 1 }
        compose.onNodeWithTag("owner-overview-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("owner-demo-dashboard").assertDoesNotExist()
    }

    @Test fun creatorStudioIsOptInAndShowsUsefulEmptyState() {
        var draftQueries = 0
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(identity, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" -> """{"roles":["artist"],"permissions":[]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_creator_my_drafts" -> { draftQueries++; "[]" }
                        else -> "{}"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-studio-toggle").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-new-draft").assertDoesNotExist()
        assertEquals(0, draftQueries)
        compose.onNodeWithTag("creator-studio-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("creator-new-draft").assertExists()
        compose.onNodeWithTag("creator-drafts-start-hint").assertExists()
        compose.onNodeWithTag("creator-drafts-load").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-drafts-empty").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, draftQueries)
    }

    @Test fun emptyModerationQueueOnlyAppearsAfterSuccessfulLoad() {
        var queries = 0
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(identity, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" ->
                            """{"roles":["platform_admin"],"permissions":["review_creators"]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_pending_creators" -> { queries++; "[]" }
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-review-load-pending")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-review-empty").assertDoesNotExist()
        compose.onNodeWithTag("creator-review-load-pending").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-review-empty").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, queries)
    }

    @Test fun failedReloadClearsCachedAdminSearchResult() {
        var searches = 0
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(identity, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" ->
                            """{"roles":["platform_admin"],"permissions":["manage_users"]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_admin_users" -> {
                            searches++
                            if (searches == 1)
                                """[{"id":"00000000-0000-4000-8000-000000000008",
                                "email":"qa@example.invalid","tier":"free"}]"""
                            else error("QA network disconnected")
                        }
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("admin-users-load").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("admin-users-load").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("admin-user-item").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("admin-users-load").performScrollTo().performClick()
        compose.waitUntil(10_000) { searches == 2 }
        compose.waitForIdle()
        compose.onNodeWithTag("admin-user-item").assertDoesNotExist()
        compose.onNodeWithTag("admin-users-empty").assertDoesNotExist()
    }

    @Test fun actorNavigationIsAvailableAtLargeFontScale() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 1.65f)) {
                MusicFlowTheme {
                    AccountRolePanel(identity, onClose = {}) { name, _ ->
                        when (name) {
                            "musicflow_my_access" -> """{"roles":["owner"],"permissions":[]}"""
                            "musicflow_my_creator_requests" -> "[]"
                            else -> "[]"
                        }
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("role-workspace-management")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("role-workspace-moderation").performScrollTo().performClick()
        compose.onNodeWithTag("creator-review-load-pending").assertExists()
        compose.onNodeWithTag("role-workspace-creator").performScrollTo().performClick()
        compose.onNodeWithTag("creator-stage-name").assertExists()
    }
}
