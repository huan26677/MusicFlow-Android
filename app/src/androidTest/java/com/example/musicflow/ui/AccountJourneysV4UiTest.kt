package com.example.musicflow.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.feature.account.AccountRolePanel
import com.example.musicflow.feature.account.CreatorDraftReviewPanel
import com.example.musicflow.feature.account.CreatorDraftStudio
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** One in-memory integrated QA suite for the completed account-flow redesign. */
@RunWith(AndroidJUnit4::class)
class AccountJourneysV4UiTest {
    @get:Rule val compose = createComposeRule()

    private val qa = AccountIdentity(
        userId = "00000000-0000-4000-8000-000000000072",
        email = "account-v4@example.invalid", displayName = "QA", tier = "free", expiresAt = null
    )

    @Test fun ownerActionsAppearOnlyAfterSelectingAnAccountAndUseConfirmedSnapshot() {
        var grants = 0
        var grantedTarget = ""
        var grantedReason = ""
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(qa, onClose = {}) { name, args ->
                    when (name) {
                        "musicflow_my_access" -> """{"roles":["owner"],"permissions":[]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_admin_users" -> """[{
                            "id":"00000000-0000-4000-8000-000000000073",
                            "email":"selected@example.invalid","tier":"free"
                        }]"""
                        "musicflow_manage_role" -> {
                            grants++
                            grantedTarget = args.optString("p_user_id")
                            grantedReason = args.optString("p_reason")
                            """{"roles":["owner"],"permissions":[]}"""
                        }
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("admin-users-load").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("admin-reason").assertDoesNotExist()
        compose.onNodeWithTag("admin-users-load").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("admin-user-item").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("admin-user-item").performScrollTo().performClick()
        compose.onNodeWithTag("owner-selected-user").assertExists()
        compose.onNodeWithTag("admin-reason").performScrollTo()
            .performTextInput("QA cấp quyền đã kiểm tra")
        compose.onNodeWithText("Cấp vai trò").performScrollTo().performClick()
        assertEquals(0, grants)
        compose.onNodeWithTag("owner-role-confirm").assertExists()
        compose.onNodeWithTag("owner-role-confirm").performClick()
        compose.waitUntil(10_000) { grants == 1 }
        assertEquals("00000000-0000-4000-8000-000000000073", grantedTarget)
        assertEquals("QA cấp quyền đã kiểm tra", grantedReason)
        compose.onNodeWithTag("admin-reason").assertDoesNotExist()
    }

    @Test fun oneReviewerAreaAtATimeAndNoUnrelatedPanels() {
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(qa, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" -> """{"roles":["owner"],"permissions":[]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("role-workspace-moderation")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("role-workspace-moderation").performClick()
        compose.onNodeWithTag("review-area-applications").assertExists()
        compose.onNodeWithTag("review-area-drafts").assertExists()
        compose.onNodeWithTag("review-area-catalog").assertExists()
        compose.onNodeWithTag("creator-review-load-pending").assertExists()
        compose.onNodeWithTag("draft-review-load").assertDoesNotExist()
        compose.onNodeWithTag("review-area-drafts").performClick()
        compose.onNodeWithTag("draft-review-load").assertExists()
        compose.onNodeWithTag("creator-review-load-pending").assertDoesNotExist()
        compose.onNodeWithTag("creator-review-reason").assertDoesNotExist()
        compose.onNodeWithTag("review-area-catalog").performClick()
        compose.onNodeWithTag("creator-review-reason").assertExists()
        compose.onNodeWithText("Tải danh mục cần kiểm duyệt").assertExists()
        compose.onNodeWithTag("draft-review-load").assertDoesNotExist()
    }

    @Test fun catalogMutationNeedsConfirmationAndServerAcknowledgment() {
        var calls = 0
        var requestedPublish = true
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(qa, onClose = {}) { name, args ->
                    when (name) {
                        "musicflow_my_access" ->
                            """{"roles":["platform_admin"],"permissions":["moderate_catalog"]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_admin_tracks" -> """[{
                            "id":"00000000-0000-4000-8000-000000000080",
                            "title":"Bài demo có giấy phép","license_code":"UNVERIFIED"
                        }]"""
                        "musicflow_review_track" -> {
                            calls++
                            requestedPublish = args.optBoolean("p_publish")
                            "true"
                        }
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-review-reason").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-review-reason").performScrollTo()
            .performTextInput("QA yêu cầu gỡ phát")
        compose.onNodeWithText("Tải danh mục cần kiểm duyệt")
            .performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("catalog-confirm-unpublish")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("catalog-confirm-unpublish").performScrollTo().performClick()
        compose.onNodeWithTag("catalog-action-cancel").performClick()
        assertEquals(0, calls)
        compose.onNodeWithTag("catalog-confirm-unpublish").performScrollTo().performClick()
        compose.onNodeWithTag("catalog-action-confirm").performClick()
        compose.waitUntil(10_000) { calls == 1 }
        compose.waitForIdle()
        assertFalse(requestedPublish)
        compose.onNodeWithTag("catalog-confirm-unpublish").assertDoesNotExist()
    }

    @Test fun creatorProfileOpensWithSingleLoadAndClosesAfterSave() {
        var profileLoads = 0
        var saves = 0
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(qa, onClose = {}) { name, _ ->
                    when (name) {
                        "musicflow_my_access" -> """{"roles":["artist"],"permissions":[]}"""
                        "musicflow_my_creator_requests" -> "[]"
                        "musicflow_my_creator_profile" -> {
                            profileLoads++
                            """{"stage_name":"Nghệ sĩ QA","bio":"Hồ sơ demo"}"""
                        }
                        "musicflow_save_creator_profile" -> { saves++; "true" }
                        else -> "[]"
                    }
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-profile-toggle")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-stage-name").assertDoesNotExist()
        compose.onNodeWithTag("creator-profile-toggle").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-profile-save").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, profileLoads)
        compose.onNodeWithTag("creator-profile-save").performScrollTo().performClick()
        compose.waitUntil(10_000) { saves == 1 }
        compose.waitForIdle()
        compose.onNodeWithTag("creator-profile-save").assertDoesNotExist()
    }

    @Test fun studioOptionalAlbumIsHiddenAndUnsavedEditsOfferKeepOrDiscard() {
        compose.setContent {
            // The real Creator Studio lives inside AccountRolePanel's scrollable area.
            MusicFlowTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    CreatorDraftStudio(rpc = { _, _ -> "[]" })
                }
            }
        }
        compose.onNodeWithTag("creator-new-draft").performClick()
        compose.onNodeWithTag("draft-album").assertDoesNotExist()
        compose.onNodeWithTag("draft-album-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("draft-album").assertExists()
        compose.onNodeWithTag("draft-title").performScrollTo()
            .performTextInput("Nhạc QA chưa lưu")
        compose.onNodeWithTag("draft-title").assertTextContains("Nhạc QA chưa lưu")
        compose.onNodeWithTag("creator-editor-close").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("creator-editor-keep").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("creator-editor-keep").assertExists().performClick()
        compose.onNodeWithTag("draft-title").assertExists()
        compose.onNodeWithTag("creator-editor-close").performScrollTo().performClick()
        compose.onNodeWithTag("creator-editor-discard").performClick()
        compose.onNodeWithTag("draft-title").assertDoesNotExist()
    }

    @Test fun draftReviewLoadsLightweightSummaryThenRequiresOpeningAndAcknowledgment() {
        var decisions = 0
        compose.setContent {
            MusicFlowTheme {
                CreatorDraftReviewPanel(rpc = { name, _ ->
                    when (name) {
                        "musicflow_pending_drafts" -> """[{
                            "id":"00000000-0000-4000-8000-000000000098",
                            "title":"Tác phẩm cần duyệt","artist":"Nghệ sĩ QA",
                            "creator_email":"author@example.invalid",
                            "rights_holder":"Nghệ sĩ QA","license_code":"UNVERIFIED",
                            "rights_reference":"QA reference", "credits": []
                        }]"""
                        "musicflow_creator_upload_list" -> "[]"
                        "musicflow_review_draft" -> {
                            decisions++
                            if (decisions == 1) "false" else "true"
                        }
                        else -> error("Unexpected test RPC: $name")
                    }
                })
            }
        }
        compose.onNodeWithTag("draft-review-load").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("draft-review-open-item")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("draft-review-approve").assertDoesNotExist()
        compose.onNodeWithTag("draft-review-reason").performTextInput("Đã kiểm tra QA")
        compose.onNodeWithTag("draft-review-open-item").performClick()
        compose.onNodeWithTag("draft-review-approve").assertExists().performClick()
        assertEquals(0, decisions)
        compose.onNodeWithTag("draft-review-confirm").performClick()
        compose.waitUntil(10_000) { decisions == 1 }
        compose.waitForIdle()
        compose.onNodeWithTag("draft-review-open-item").assertExists()
        compose.onNodeWithTag("draft-review-approve").performClick()
        compose.onNodeWithTag("draft-review-confirm").performClick()
        compose.waitUntil(10_000) { decisions == 2 }
        compose.waitForIdle()
        compose.onNodeWithTag("draft-review-open-item").assertDoesNotExist()
    }

    @Test fun androidBackClosesOwnerOverviewInsteadOfLosingRolePage() {
        var closePage = 0
        compose.setContent {
            MusicFlowTheme {
                AccountRolePanel(qa, onClose = { closePage++ }) { name, _ ->
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
            compose.onAllNodesWithTag("owner-overview-toggle")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("owner-overview-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("owner-demo-dashboard").assertExists()
        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithTag("owner-demo-dashboard").assertDoesNotExist()
        compose.onNodeWithTag("role-workspace-management").assertExists()
        assertEquals(0, closePage)
    }
}
