package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.feature.account.CreatorPrivateAttachmentPanel
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic documents only: no authenticated Supabase account or file mutation. */
@RunWith(AndroidJUnit4::class)
class CreatorPrivateAttachmentUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun approvedCreatorCanChooseTwoPrivateUploads() {
        rule.setContent {
            MusicFlowTheme {
                CreatorPrivateAttachmentPanel(
                    draftId = "123e4567-e89b-12d3-a456-426614174001",
                    editable = true,
                    rpc = { _, _ -> "[]" },
                    upload = { _, _, _ -> },
                    download = { _, _ -> }
                )
            }
        }
        rule.onNodeWithTag("creator-audio-upload").assertExists()
        rule.onNodeWithTag("creator-evidence-upload").assertExists()
    }

    @Test fun reviewerCannotSeeUploadButtons() {
        rule.setContent {
            MusicFlowTheme {
                CreatorPrivateAttachmentPanel(
                    draftId = "123e4567-e89b-12d3-a456-426614174001",
                    editable = false,
                    rpc = { _, _ -> "[]" },
                    upload = { _, _, _ -> },
                    download = { _, _ -> }
                )
            }
        }
        rule.onNodeWithTag("creator-audio-upload").assertDoesNotExist()
        rule.onNodeWithTag("creator-evidence-upload").assertDoesNotExist()
        rule.onNodeWithTag("creator-attachment-refresh").assertExists()
    }
}
