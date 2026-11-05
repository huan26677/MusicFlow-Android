package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.feature.account.AccountRolePanel
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Fake, in-memory RPC: never changes the hosted Supabase database. */
@RunWith(AndroidJUnit4::class)
class MusicFlowRolesUiTest {
    @get:Rule val compose = createComposeRule()

    private val listener = AccountIdentity(
        userId = "00000000-0000-4000-8000-000000000001",
        email = "qa@example.invalid", displayName = "QA", tier = "premium",
        expiresAt = null
    )

    @Test fun premiumDoesNotExposeAdministrationOrDeveloper() {
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(listener, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" -> """{"roles":[],"permissions":[]}"""
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Vai trò: Người nghe")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("admin-search-user").assertDoesNotExist()
        compose.onNodeWithTag("developer-quick-access").assertDoesNotExist()
        compose.onNodeWithTag("creator-request").assertExists()
    }

    @Test fun scopedPlatformAdminCannotAccessOwnerGrants() {
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(listener, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" ->
                            """{"roles":["platform_admin"],"permissions":["manage_users"]}"""
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("admin-search-user")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("admin-search-user").assertExists()
        compose.onNodeWithTag("admin-reason").assertDoesNotExist()
        compose.onNodeWithTag("developer-quick-access").assertDoesNotExist()
    }

    @Test fun developerHasDebugShortcutWithoutAdminPowers() {
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(listener, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" ->
                            """{"roles":["developer"],"permissions":[]}"""
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("developer-quick-access")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("developer-quick-access").assertExists()
        compose.onNodeWithTag("admin-search-user").assertDoesNotExist()
    }

    @Test fun creatorRequestUsesServerRpcAndNeverSelfAssignsRole() {
        var called = false
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(listener, onClose = {}) { name, args ->
                    when (name) {
                        "musicflow_my_access" -> """{"roles":[],"permissions":[]}"""
                        "musicflow_apply_creator" -> {
                            called = args.optString("p_name") == "Nhạc sĩ QA"
                            "\"00000000-0000-4000-8000-000000000002\""
                        }
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-stage-name")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-stage-name").performTextInput("Nhạc sĩ QA")
        compose.onNodeWithTag("creator-request").performScrollTo().performClick()
        compose.waitUntil(10_000) { called }
        assertTrue(called)
    }
}
