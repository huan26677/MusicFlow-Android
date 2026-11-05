package com.example.musicflow

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Validates the real Account screen on an emulator without creating a user. */
@RunWith(AndroidJUnit4::class)
class AccountAuthUiTest {
    @get:Rule val rule = createEmptyComposeRule()

    @Test
    fun signupTabInlineErrorsGoogleDisabledAndGuestNavigation() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithText("Account").performClick()
            rule.onNodeWithTag("account-signup-tab").performClick()
            rule.onNodeWithTag("account-confirm-password").assertExists()
            rule.onNodeWithTag("account-submit").performClick()
            rule.onNodeWithText("Bạn chưa nhập email.").assertExists()
            rule.onNodeWithText("Bạn chưa nhập mật khẩu.").assertExists()
            rule.onNodeWithTag("account-scroll")
                .performScrollToNode(hasTestTag("account-google-disabled"))
            rule.onNodeWithTag("account-google-disabled").assertIsNotEnabled()
            rule.onNodeWithTag("account-scroll")
                .performScrollToNode(hasTestTag("account-guest"))
            rule.onNodeWithTag("account-guest").performClick()
            rule.onNodeWithText("Home").assertExists()
        }
    }
}
