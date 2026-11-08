package com.example.musicflow.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.account.DemoAccountStore
import com.example.musicflow.core.account.DemoCenterPlan
import com.example.musicflow.core.account.DemoCenterStore
import com.example.musicflow.core.account.DemoPersona
import com.example.musicflow.feature.account.AccountAuthMode
import com.example.musicflow.feature.account.AccountScreen
import com.example.musicflow.feature.account.AccountUiState
import com.example.musicflow.feature.account.DemoCenterScreen
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Mock-only role journeys. No backend RPC, payment, files or privileged credentials. */
@RunWith(AndroidJUnit4::class)
class DemoCenterV1UiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun guestCanOpenDemoCenterWithoutLoggingIn() {
        var opened = 0
        compose.setContent {
            MusicFlowTheme {
                AccountScreen(
                    state = AccountUiState(loading = false, configured = false),
                    themeMode = "system", onThemeModeChange = {},
                    onEmailChange = {}, onPasswordChange = {}, onDisplayNameChange = {},
                    onSignIn = {}, onSignUp = {}, onGoogleSignIn = {},
                    onSaveProfile = {}, onSignOut = {}, onSyncLibrary = {},
                    onSyncHistory = {}, onRelink = { _, _ -> },
                    onDeleteCloudPlaylist = {}, onDeleteCloudFavorite = {},
                    onOpenDemoCenter = { opened++ }
                )
            }
        }
        compose.onNodeWithTag("account-demo-tools-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("account-demo-center-open").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        compose.onNodeWithTag("account-guest").assertExists()
        compose.onNodeWithTag("account-role-tools").assertDoesNotExist()
    }

    @Test fun guestCanChooseLocalDemoAuthWhenSupabaseIsNotConfigured() {
        var switched = 0
        var registered = 0
        compose.setContent {
            var mode by remember { mutableStateOf(false) }
            MusicFlowTheme {
                AccountScreen(
                    state = AccountUiState(loading = false, configured = false,
                        authMode = AccountAuthMode.SIGN_UP, demoMode = mode),
                    themeMode = "system", onThemeModeChange = {},
                    onEmailChange = {}, onPasswordChange = {}, onDisplayNameChange = {},
                    onSignIn = {}, onSignUp = { registered++ }, onGoogleSignIn = {},
                    onSaveProfile = {}, onSignOut = {}, onSyncLibrary = {},
                    onSyncHistory = {}, onRelink = { _, _ -> },
                    onDeleteCloudPlaylist = {}, onDeleteCloudFavorite = {},
                    onDemoModeChange = { mode = it; switched++ }
                )
            }
        }
        compose.onNodeWithTag("account-auth-demo-mode").performScrollTo().performClick()
        compose.onNodeWithTag("account-auth-demo-explanation").assertExists()
        compose.onNodeWithText("Tài khoản Demo được tạo ngay trên máy, không cần xác nhận email.")
            .assertExists()
        compose.onNodeWithTag("account-submit").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("account-submit").performClick()
        compose.runOnIdle {
            assertEquals(1, switched)
            assertEquals(1, registered)
        }
    }

    @Test fun ownerStoryShowsMockDelegationAndAuditWithoutRealRoleGrants() {
        compose.setContent {
            var progress by remember { mutableStateOf(0) }
            MusicFlowTheme {
                DemoCenterScreen(
                    actor = DemoPersona.OWNER, progress = progress, error = null,
                    onSelect = {}, onAdvance = {
                        progress = DemoCenterPlan.advance(DemoPersona.OWNER, progress)
                    }, onReset = { progress = 0 }, onClose = {}, onOpenBilling = {}
                )
            }
        }
        compose.onNodeWithTag("demo-center-warning").assertExists()
        repeat(3) {
            compose.onNodeWithTag("demo-center-advance").performScrollTo().performClick()
        }
        compose.onNodeWithTag("demo-center-finished").assertExists()
        compose.onNodeWithText("Quản trị viên mẫu đã có quyền xét duyệt trong kịch bản; Supabase không đổi.")
            .assertExists()
        compose.onNodeWithTag("demo-center-reset").performScrollTo().performClick()
        compose.onNodeWithTag("demo-center-progress").assertExists()
        compose.onNodeWithTag("demo-center-finished").assertDoesNotExist()
    }

    @Test fun switchingPersonaKeepsSeparateLocalProgressAndPremiumLinksToMockBilling() {
        var billingOpens = 0
        compose.setContent {
            var actor by remember { mutableStateOf(DemoPersona.FREE) }
            var guestProgress by remember { mutableStateOf(0) }
            var freeProgress by remember { mutableStateOf(0) }
            var premiumProgress by remember { mutableStateOf(0) }
            MusicFlowTheme {
                DemoCenterScreen(
                    actor = actor,
                    progress = when (actor) {
                        DemoPersona.GUEST -> guestProgress
                        DemoPersona.PREMIUM -> premiumProgress
                        else -> freeProgress
                    },
                    error = null,
                    onSelect = { actor = it },
                    onAdvance = {
                        when (actor) {
                            DemoPersona.GUEST -> guestProgress++
                            DemoPersona.PREMIUM -> premiumProgress++
                            else -> freeProgress++
                        }
                    }, onReset = {}, onClose = {},
                    onOpenBilling = { billingOpens++ }
                )
            }
        }
        compose.onNodeWithTag("demo-center-advance").performScrollTo().performClick()
        compose.onNodeWithTag("demo-center-actor-picker").performScrollTo().performClick()
        compose.onNodeWithTag("demo-center-actor-premium").performClick()
        compose.onNodeWithTag("demo-center-open-billing").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, billingOpens) }
        compose.onNodeWithTag("demo-center-actor-picker").performScrollTo().performClick()
        compose.onNodeWithTag("demo-center-actor-free").performClick()
        compose.onNodeWithTag("demo-center-outcome-0").assertExists()
    }

    @Test fun demoCenterHasAccessibleControlsAtLargeFontOnNarrowScreen() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.65f)) {
                Box(Modifier.width(320.dp).height(690.dp)) {
                    MusicFlowTheme {
                        DemoCenterScreen(
                            actor = DemoPersona.ARTIST, progress = 0, error = null,
                            onSelect = {}, onAdvance = {}, onReset = {},
                            onClose = {}, onOpenBilling = {}
                        )
                    }
                }
            }
        }
        compose.onNodeWithTag("demo-center-advance").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("demo-center-reset").performScrollTo().assertIsEnabled()
    }

    @Test fun demoProgressAndActorSelectionPersistButCannotGrantAnyRealAuthority() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = DemoCenterStore(context)
        try {
            first.select(DemoPersona.MODERATOR)
            first.progressTo(DemoPersona.MODERATOR, 1)
            first.progressTo(DemoPersona.OWNER, 200)
            val second = DemoCenterStore(context)
            assertEquals(DemoPersona.MODERATOR, second.selected())
            assertEquals(1, second.progress(DemoPersona.MODERATOR))
            assertEquals(DemoCenterPlan.steps(DemoPersona.OWNER).size,
                second.progress(DemoPersona.OWNER))
            assertEquals(0, second.progress(DemoPersona.DEVELOPER))
            // Simulated completion doesn't create JWTs, permission objects or
            // server roles; the store only has actor + step operations.
            assertFalse(second.selected().name.contains("JWT"))
        } finally {
            DemoPersona.entries.forEach { first.reset(it) }
            first.select(DemoPersona.GUEST)
        }
    }

    @Test fun localAccountCanRegisterRestoreSignOutAndSignInWithNoPrivileges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences(
            "musicflow_local_demo_account_v1", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = DemoAccountStore(context)
        try {
            val identity = store.register("musicflow_demo_${System.nanoTime()}@example.invalid",
                "qa-only-pass-123")
            assertTrue(identity.userId.startsWith("demo-local:"))
            assertTrue(identity.access.roles.isEmpty())
            assertTrue(identity.access.permissions.isEmpty())
            assertEquals("free", identity.tier)
            assertEquals(identity, store.restore())
            store.signOut()
            assertNull(store.restore())
            val returned = store.signIn(identity.email, "qa-only-pass-123")
            assertEquals(identity.userId, returned.userId)
            assertEquals("free", returned.tier)
        } finally {
            store.signOut()
            prefs.edit().clear().commit()
        }
    }
}
