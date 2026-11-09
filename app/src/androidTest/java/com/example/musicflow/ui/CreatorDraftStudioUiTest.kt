package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.feature.account.CreatorDraftStudio
import com.example.musicflow.feature.account.CreatorDraftReviewPanel
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** In-memory responses; never creates uploads or user roles on real Supabase. */
@RunWith(AndroidJUnit4::class)
class CreatorDraftStudioUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun creatorCanStartDraftAndSeeCopyrightFields() {
        rule.setContent {
            MusicFlowTheme {
                CreatorDraftStudio(rpc = { _, _ -> "[]" })
            }
        }
        rule.onNodeWithTag("creator-new-draft").performClick()
        rule.onNodeWithTag("draft-title").performTextInput("Bài nháp QA")
        rule.onNodeWithTag("draft-credit-name").performTextInput("Nghệ sĩ QA")
        rule.onNodeWithTag("draft-add-credit").assertExists()
        rule.onNodeWithTag("draft-streaming-claim").assertExists()
        rule.onNodeWithTag("draft-save").assertExists()
        rule.onNodeWithText("Lưu bản nháp riêng tư").assertExists()
        rule.onNodeWithText("Lưu nháp lên Supabase").assertDoesNotExist()
        rule.onNodeWithTag("draft-submit").assertDoesNotExist()
    }

    @Test fun creatorExplainsLicenseChoicesAndKeepsRightsConfirmationExplicit() {
        rule.setContent {
            MusicFlowTheme { CreatorDraftStudio(rpc = { _, _ -> "[]" }) }
        }
        rule.onNodeWithTag("creator-new-draft").performClick()
        rule.onNodeWithText("Chưa xác minh quyền").assertExists()
        rule.onNodeWithText("Vuốt ngang để xem các loại giấy phép khác.").assertExists()
        rule.onNodeWithTag("draft-streaming-claim").assertExists()
        rule.onNodeWithText("Vuốt ngang để xem thêm vai trò người tham gia.").assertExists()
    }

    @Test fun moderatorReviewScreenDoesNotPublishWithoutDecision() {
        rule.setContent {
            MusicFlowTheme {
                CreatorDraftReviewPanel(rpc = { _, _ -> "[]" })
            }
        }
        rule.onNodeWithTag("draft-review-load").assertExists()
        rule.onNodeWithTag("draft-review-reason").assertDoesNotExist()
    }
}
