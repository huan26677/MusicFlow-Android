package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.feature.account.OwnerDemoDashboard
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only, in-memory RPC mocks. No real Owner password or DB mutations. */
@RunWith(AndroidJUnit4::class)
class OwnerDemoDashboardUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun ownerSeesCountsAndDemoNextSteps() {
        rule.setContent {
            MusicFlowTheme {
                OwnerDemoDashboard { _, _ ->
                    """{"owner_count":1,"user_count":3,"platform_admin_count":0,
                       "moderator_count":0,"artist_count":0,"author_count":0,
                       "developer_count":0,"creator_requests_pending":0,
                       "drafts_pending":0,"drafts_metadata_approved":0,
                       "creator_files_uploaded":0}"""
                }
            }
        }
        rule.waitUntil(10_000) {
            rule.onAllNodesWithTag("owner-demo-users").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("owner-demo-owner-count").assertExists()
        rule.onNodeWithTag("owner-demo-staff").assertExists()
        rule.onNodeWithTag("owner-demo-refresh").assertExists()
    }

    @Test fun serverDenialNeverShowsOwnerCounts() {
        rule.setContent {
            MusicFlowTheme {
                OwnerDemoDashboard { _, _ -> error("403 forbidden") }
            }
        }
        rule.waitUntil(10_000) {
            rule.onAllNodesWithTag("owner-demo-refresh").fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
        rule.onNodeWithTag("owner-demo-users").assertDoesNotExist()
        rule.onNodeWithTag("owner-demo-refresh").assertExists()
    }
}
