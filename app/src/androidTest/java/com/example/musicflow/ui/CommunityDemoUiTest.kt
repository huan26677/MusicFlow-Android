package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.feature.community.CommunityRoute
import com.example.musicflow.feature.online.OnlineCatalogScreen
import com.example.musicflow.feature.online.OnlineCatalogUiState
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommunityDemoUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun demoRatingAndCommentActionsStayLocal() {
        compose.setContent {
            MusicFlowTheme {
                CommunityRoute("demo-community","Bài hát mẫu",demo=true,onClose={})
            }
        }
        compose.onNodeWithTag("community-demo-label").assertExists()
        compose.onNodeWithTag("community-rate-4").performClick()
        compose.onNodeWithTag("community-comment-input")
            .performScrollTo().performTextInput("Một bình luận Demo mới")
        compose.onNodeWithTag("community-comment-submit").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Bình luận (3)").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Một bình luận Demo mới").assertExists()
    }

    @Test fun demoDoesNotPretendToBeOnlineUser() {
        compose.setContent {
            MusicFlowTheme {
                CommunityRoute("demo-community","Bài hát mẫu",demo=true,onClose={})
            }
        }
        compose.onNodeWithTag("community-demo-label").assertExists()
        compose.onNodeWithText(
            "CHẾ ĐỘ DEMO — dữ liệu mẫu, không gửi lên Supabase."
        ).assertExists()
    }

    @Test fun emptyOnlineCatalogOffersDemoWithoutPublishedTracks() {
        compose.setContent {
            MusicFlowTheme {
                OnlineCatalogScreen(
                    state = OnlineCatalogUiState(
                        savedEndpoint = "https://example.invalid",
                        endpointInput = "https://example.invalid",
                        tracks = emptyList(),
                        isLoaded = true
                    ),
                    onBack = {},
                    onEndpointChange = {},
                    onSaveEndpoint = {},
                    onClearEndpoint = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onPlayTrack = { _, _ -> },
                    isSupabaseManaged = true
                )
            }
        }
        compose.onNodeWithTag("community-open-demo").performClick()
        compose.onNodeWithTag("community-demo-label").assertExists()
        compose.onNodeWithTag("community-rating-summary").assertExists()
    }
}
