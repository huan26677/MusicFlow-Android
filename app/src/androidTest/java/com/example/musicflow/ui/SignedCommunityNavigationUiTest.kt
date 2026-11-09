package com.example.musicflow.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the installed app's REAL MainActivity, unlike createComposeRule()
 * which depends on debug-only androidx.activity.ComponentActivity registration.
 * Safe on both signed QA Release and Debug; no login or public data writes.
 */
@RunWith(AndroidJUnit4::class)
class SignedCommunityNavigationUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun guestReachesCommunityDemoFromRealHomeNavigation() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Khám phá nhạc online")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Khám phá nhạc online").performScrollTo().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("Khám phá online").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Khám phá online").assertExists()
        compose.waitUntil(25_000) {
            compose.onAllNodesWithTag("community-open-demo").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("community-open-demo").performClick()
        compose.onNodeWithTag("community-demo-label").assertExists()
        compose.onNodeWithTag("community-rating-summary").assertExists()
    }
}
