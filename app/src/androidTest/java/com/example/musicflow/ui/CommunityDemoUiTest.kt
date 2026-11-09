package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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

    @Test fun editExistingCommentMovesFocusToExistingComposer() {
        compose.setContent {
            MusicFlowTheme {
                CommunityRoute("demo-community", "Bài hát mẫu", demo = true, onClose = {})
            }
        }
        // The initial coroutine loading the demo snapshot may still hold
        // the submit button disabled on slower physical API 30 devices.
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Bình luận (2)")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("community-comment-input").performScrollTo()
            .performTextInput("Bình luận trước khi sửa")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("community-comment-submit")
            .performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Bình luận (3)").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Sửa").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("community-comment-input").assertIsFocused()
        compose.onNodeWithText("Lưu chỉnh sửa").assertExists()
    }

    @Test fun demoDoesNotPretendToBeOnlineUser() {
        compose.setContent {
            MusicFlowTheme {
                CommunityRoute("demo-community","Bài hát mẫu",demo=true,onClose={})
            }
        }
        compose.onNodeWithTag("community-demo-label").assertExists()
        compose.onNodeWithText(
            "CHẾ ĐỘ DEMO — dữ liệu mẫu, không gửi lên tài khoản trực tuyến."
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

    @Test fun managedOnlineSourceDoesNotShowTechnicalServerField() {
        compose.setContent {
            MusicFlowTheme {
                OnlineCatalogScreen(
                    state = OnlineCatalogUiState(
                        savedEndpoint = "https://example.invalid",
                        endpointInput = "https://example.invalid",
                        isLoaded = true
                    ),
                    onBack = {}, onEndpointChange = {}, onSaveEndpoint = {},
                    onClearEndpoint = {}, onRefresh = {}, onLoadMore = {},
                    onPlayTrack = { _, _ -> }, isSupabaseManaged = true
                )
            }
        }
        compose.onNodeWithTag("online-manual-endpoint").assertDoesNotExist()
        compose.onNodeWithText("Chưa có bài hát online").assertExists()
    }

    @Test fun manuallyConfiguredSourceStillOffersExistingEditor() {
        compose.setContent {
            MusicFlowTheme {
                OnlineCatalogScreen(
                    state = OnlineCatalogUiState(),
                    onBack = {}, onEndpointChange = {}, onSaveEndpoint = {},
                    onClearEndpoint = {}, onRefresh = {}, onLoadMore = {},
                    onPlayTrack = { _, _ -> }, isSupabaseManaged = false
                )
            }
        }
        compose.onNodeWithTag("online-manual-endpoint").assertExists()
    }

    @Test fun largeTextKeepsAllRatingsAndCommentComposerReachable() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                MusicFlowTheme {
                    CommunityRoute(
                        "demo-community", "Bài hát Demo", demo = true, onClose = {}
                    )
                }
            }
        }
        compose.onNodeWithTag("community-rate-5").performScrollTo().performClick()
        compose.onNodeWithTag("community-comment-input")
            .performScrollTo().performTextInput("Bình luận khi chữ lớn")
        compose.onNodeWithTag("community-comment-submit").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Bình luận (3)").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun largeTextCanReachDemoFromEmptyManagedCatalog() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                MusicFlowTheme {
                    OnlineCatalogScreen(
                        state = OnlineCatalogUiState(
                            savedEndpoint = "https://example.invalid",
                            endpointInput = "https://example.invalid",
                            tracks = emptyList(), isLoaded = true
                        ),
                        onBack = {}, onEndpointChange = {}, onSaveEndpoint = {},
                        onClearEndpoint = {}, onRefresh = {}, onLoadMore = {},
                        onPlayTrack = { _, _ -> }, isSupabaseManaged = true
                    )
                }
            }
        }
        compose.onNodeWithTag("community-open-demo").performScrollTo().performClick()
        compose.onNodeWithTag("community-demo-label").assertExists()
    }
}
