package com.example.musicflow.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.core.account.AccountTier
import com.example.musicflow.core.account.DemoBillingMethod
import com.example.musicflow.core.account.DemoBillingPlan
import com.example.musicflow.core.account.DemoBillingReceipt
import com.example.musicflow.core.account.DemoBillingResult
import com.example.musicflow.core.account.DemoBillingStore
import com.example.musicflow.core.account.MusicFlowAccess
import com.example.musicflow.feature.account.AccountScreen
import com.example.musicflow.feature.account.AccountUiState
import com.example.musicflow.feature.account.DemoBillingScreen
import com.example.musicflow.ui.theme.MusicFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Fully isolated real local-persistence tests. No payment endpoint, card numbers or Supabase. */
@RunWith(AndroidJUnit4::class)
class DemoBillingUiTest {
    @get:Rule val compose = createComposeRule()
    private val now = 1_800_000_000_000L

    @Test fun guestCanOpenCheckoutWithoutSigningIn() {
        var openings = 0
        compose.setContent {
            MusicFlowTheme {
                AccountScreen(
                    state = AccountUiState(loading = false), themeMode = "system",
                    onThemeModeChange = {}, onEmailChange = {}, onPasswordChange = {},
                    onDisplayNameChange = {}, onSignIn = {}, onSignUp = {},
                    onGoogleSignIn = {}, onSaveProfile = {}, onSignOut = {},
                    onSyncLibrary = {}, onSyncHistory = {}, onRelink = { _, _ -> },
                    onDeleteCloudPlaylist = {}, onDeleteCloudFavorite = {},
                    onOpenDemoBilling = { openings++ }
                )
            }
        }
        compose.onNodeWithTag("account-demo-tools-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("account-demo-billing-open").performScrollTo().performClick()
        assertEquals(1, openings)
        compose.onNodeWithTag("account-guest").assertExists()
        compose.onNodeWithTag("account-home-tier").assertDoesNotExist()
    }

    private fun mount(
        store: DemoBillingStore,
        scope: String
    ) {
        compose.setContent {
            var receipt by remember { mutableStateOf(store.receipt(scope)) }
            var feedback by remember { mutableStateOf<String?>(null) }
            MusicFlowTheme {
                DemoBillingScreen(
                    receipt = receipt, feedback = feedback, nowMs = now,
                    onPurchase = { plan, method, decline ->
                        val result = store.simulatePayment(scope, plan, method, decline)
                        receipt = store.receipt(scope)
                        feedback = when (result) {
                            DemoBillingResult.Declined -> "Thanh toán thất bại (mô phỏng)"
                            is DemoBillingResult.Success -> "Thành công (mô phỏng)"
                        }
                    },
                    onCancel = {
                        store.cancel(scope)
                        receipt = store.receipt(scope)
                    },
                    onReset = {
                        store.reset(scope)
                        receipt = null
                    },
                    onClose = {}
                )
            }
        }
    }

    @Test fun annualCardCheckoutNeedsConfirmationThenShowsFakeReceipt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DemoBillingStore(context) { now }
        val scope = "qa-annual-${System.nanoTime()}"
        try {
            mount(store, scope)
            compose.onNodeWithTag("demo-billing-disclaimer").assertExists()
            compose.onNodeWithTag("demo-billing-plan-year").performScrollTo().performClick()
            compose.onNodeWithTag("demo-billing-method-card").performScrollTo().performClick()
            compose.onNodeWithTag("demo-billing-checkout").performScrollTo().performClick()
            compose.onNodeWithText("Xác nhận thanh toán MÔ PHỎNG").assertExists()
            assertNull(store.receipt(scope))
            compose.onNodeWithTag("demo-billing-confirm").performClick()
            compose.onNodeWithTag("demo-billing-receipt").assertExists()
            compose.onNodeWithTag("demo-billing-status").assertExists()
            compose.runOnIdle {
                val persisted = checkNotNull(store.receipt(scope))
                assertEquals(DemoBillingPlan.YEAR, persisted.plan)
                assertEquals(DemoBillingMethod.CARD, persisted.method)
                assertEquals(now + 365L * 86_400_000L, persisted.expiresAtMs)
                assertTrue(persisted.active(now))
            }
        } finally { store.reset(scope) }
    }

    @Test fun failedCheckoutNeverChangesPriorPaidDemoReceipt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DemoBillingStore(context) { now }
        val scope = "qa-declined-${System.nanoTime()}"
        val first = (store.simulatePayment(scope, DemoBillingPlan.MONTH,
            DemoBillingMethod.WALLET, simulateDecline = false) as DemoBillingResult.Success).receipt
        try {
            mount(store, scope)
            compose.onNodeWithTag("demo-billing-result-declined").performScrollTo().performClick()
            compose.onNodeWithTag("demo-billing-checkout").performScrollTo().performClick()
            compose.onNodeWithTag("demo-billing-confirm").performClick()
            compose.onNodeWithText("Thanh toán thất bại (mô phỏng)").assertExists()
            compose.runOnIdle { assertEquals(first, store.receipt(scope)) }
        } finally { store.reset(scope) }
    }

    @Test fun cancellingRequiresConfirmationAndResetClearsOnlyDemoRecord() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DemoBillingStore(context) { now }
        val scope = "qa-cancel-${System.nanoTime()}"
        store.simulatePayment(scope, DemoBillingPlan.MONTH, DemoBillingMethod.WALLET, false)
        try {
            mount(store, scope)
            compose.onNodeWithTag("demo-billing-cancel").performScrollTo().performClick()
            compose.onNodeWithText("Hủy gói Premium mô phỏng?").assertExists()
            compose.onNodeWithText("Giữ gói demo").performClick()
            assertTrue(checkNotNull(store.receipt(scope)).active(now))
            compose.onNodeWithTag("demo-billing-cancel").performScrollTo().performClick()
            compose.onNodeWithTag("demo-billing-confirm-cancel").performClick()
            compose.runOnIdle {
                assertFalse(checkNotNull(store.receipt(scope)).active(now))
            }
            compose.onNodeWithTag("demo-billing-reset").performScrollTo().performClick()
            compose.onNodeWithTag("demo-billing-empty").assertExists()
            assertNull(store.receipt(scope))
        } finally { store.reset(scope) }
    }

    @Test fun cancellingCheckoutBeforeConfirmCreatesNoRecord() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DemoBillingStore(context) { now }
        val scope = "qa-abort-${System.nanoTime()}"
        try {
            mount(store, scope)
            compose.onNodeWithTag("demo-billing-checkout").performScrollTo().performClick()
            compose.onNodeWithTag("demo-billing-abort").performClick()
            compose.onNodeWithTag("demo-billing-empty").assertExists()
            assertNull(store.receipt(scope))
        } finally { store.reset(scope) }
    }

    @Test fun receiptsPersistOnDeviceButNeverCrossAccountScopesOrGrantAccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val at = DemoBillingStore(context) { now }
        val userA = "demo-local:${System.nanoTime()}"
        val userB = "supabase-qa:${System.nanoTime()}"
        try {
            val success = at.simulatePayment(userA, DemoBillingPlan.MONTH,
                DemoBillingMethod.WALLET, false)
            assertTrue(success is DemoBillingResult.Success)
            val readFromNewStore = DemoBillingStore(context) { now }
            assertEquals(at.receipt(userA), readFromNewStore.receipt(userA))
            assertNull(readFromNewStore.receipt(userB))
            assertNull(readFromNewStore.receipt("guest-offline-demo"))
            val identity = AccountIdentity(userA, "demo@example.invalid", "Demo QA",
                "free", null, MusicFlowAccess.NONE)
            assertEquals("free", identity.tier)
            assertTrue(identity.access.roles.isEmpty())
            assertEquals("free", AccountTier.activeTier(identity.tier, identity.expiresAt))
        } finally {
            at.reset(userA)
            at.reset(userB)
        }
    }
}
