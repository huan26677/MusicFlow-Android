package com.example.musicflow

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Validates the real Account screen without creating a user or calling live signup. */
@RunWith(AndroidJUnit4::class)
class AccountAuthUiTest {
    @get:Rule val rule = createEmptyComposeRule()

    @Test
    fun signupTabInlineErrorsGoogleDisabledAndGuestNavigation() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithText("Tài khoản").performClick()
            // On a physical phone the startup session check may be slower than
            // Compose rendering. Wait until Demo mode is actually selectable.
            rule.waitUntil(15_000) {
                runCatching {
                    rule.onNodeWithTag("account-auth-demo-mode").assertIsEnabled()
                }.isSuccess
            }
            // Offline report builds may deliberately have no Supabase Auth.
            // Local demo still exercises the real form and its validation.
            rule.onNodeWithTag("account-auth-demo-mode")
                .performScrollTo().performClick()
            rule.waitUntil(10_000) {
                runCatching {
                    rule.onNodeWithTag("account-auth-demo-mode").assertIsSelected()
                }.isSuccess
            }
            rule.onNodeWithTag("account-signup-tab")
                .performScrollTo().performClick()
            rule.onNodeWithTag("account-confirm-password").assertExists()
            // This button is below the fold on Android 16: performClick on a
            // composed but offscreen node does not reliably trigger onClick.
            rule.onNodeWithTag("account-submit")
                .performScrollTo().assertIsEnabled().performClick()
            rule.onNodeWithText("Bạn chưa nhập email.", useUnmergedTree = true)
                .assertExists()
            rule.onNodeWithText("Bạn chưa nhập mật khẩu.", useUnmergedTree = true)
                .assertExists()
            // There is intentionally no dead-end Google button in the demo.
            rule.onAllNodesWithTag("account-google-disabled").assertCountEquals(0)
            rule.onNodeWithTag("account-guest").performScrollTo().performClick()
            rule.onNodeWithText("Trang chủ").assertExists()
        }
    }
}
