package com.example.musicflow.core.online

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.feature.online.OnlineCatalogScreen
import com.example.musicflow.feature.online.OnlineCatalogUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnlineRightsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val rights = OnlineTrackRights(
        licenseCode = "CC-BY-4.0",
        licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
        attribution = "Artist — QA Test Track (CC BY 4.0)",
        sourceUrl = "https://example.com/source",
        rightsConfirmed = true, streamingAllowed = true,
        verifiedAt = "2026-10-29T10:00:00Z"
    )

    private fun render(tracks: List<OnlineTrack>, onPlay: (Int) -> Unit) {
        compose.setContent {
            OnlineCatalogScreen(
                state = OnlineCatalogUiState(
                    endpointInput = "https://example.com",
                    savedEndpoint = "https://example.com",
                    tracks = tracks, isLoaded = true
                ),
                onBack = {},
                onEndpointChange = {},
                onSaveEndpoint = {},
                onClearEndpoint = {},
                onRefresh = {},
                onLoadMore = {},
                onPlayTrack = { _, selected -> onPlay(selected) }
            )
        }
    }

    @Test fun verifiedTrackCanPlayAndShowCredits() {
        var index = -1
        render(listOf(
            OnlineTrack("valid", "Đã cấp quyền", "Artist", null, 1L,
                "https://example.com/stream.mp3", rights)
        )) { index = it }
        compose.onNodeWithText("Đã cấp quyền").performClick()
        compose.runOnIdle { assertEquals(0, index) }
        compose.onNodeWithTag("track-rights-valid").performClick()
        compose.onNodeWithText("Artist — QA Test Track (CC BY 4.0)", substring = true)
            .assertExists()
        compose.onNodeWithText("Đọc giấy phép").assertExists()
    }

    @Test fun unverifiedTrackShowsDetailsButIsNotPlayable() {
        var selected = 0
        render(listOf(
            OnlineTrack("missing", "Chưa có quyền", "Artist", null, 1L,
                "https://example.com/stream.mp3")
        )) { selected++ }
        compose.onNodeWithText("Chưa xác minh quyền phát").assertExists()
        compose.onNodeWithTag("track-rights-missing").performClick()
        compose.onNodeWithText("Giấy phép: Chưa khai báo").assertExists()
        compose.runOnIdle { assertEquals(0, selected) }
    }
}
