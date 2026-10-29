package com.example.musicflow

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeBatchTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun themePersistsAfterRestart() {
        val context = InstrumentationRegistry
            .getInstrumentation().targetContext

        val prefs = context.getSharedPreferences(
            "musicflow_settings", Context.MODE_PRIVATE
        )

        val previous = prefs.getString("theme_mode", null)

        var scenario: ActivityScenario<MainActivity>? = null

        try {
            scenario = ActivityScenario.launch(
                MainActivity::class.java
            )

            composeRule.onNodeWithText("Account")
                .performClick()

            // Actual Account filter chip is labeled "Tối". Scroll because
            // account content can exceed the emulator viewport.
            // There are nested scroll axes: the theme chips scroll horizontally
            // while the Account screen scrolls vertically. First reveal the
            // theme section via the OUTER vertical scroll container.
            composeRule.onNodeWithTag("account-scroll")
                .performScrollToNode(hasText("Tối"))
            composeRule.onNodeWithText("Tối").performClick()
            composeRule.onNodeWithText("Tối")
                .assertIsSelected()

            assertEquals(
                "dark",
                prefs.getString("theme_mode", null)
            )

            scenario.close()
            scenario = null

            scenario = ActivityScenario.launch(
                MainActivity::class.java
            )

            composeRule.onNodeWithText("Account")
                .performClick()

            composeRule.onNodeWithTag("account-scroll")
                .performScrollToNode(hasText("Tối"))
            composeRule.onNodeWithText("Tối").assertIsSelected()

            Log.i(
                "MusicFlowFavoritesQA",
                "PASS themePersistsAfterRestart"
            )
        } finally {
            scenario?.close()

            val editor = prefs.edit()
            if (previous == null) {
                editor.remove("theme_mode")
            } else {
                editor.putString("theme_mode", previous)
            }
            editor.commit()
        }
    }
}
