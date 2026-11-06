package com.example.musicflow.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoBillingRulesTest {
    private val now = 1_700_000_000_000L
    private val day = 86_400_000L

    @Test fun monthlyPlanIs30DaysAndIllustrativePrice() {
        assertEquals(29_000, DemoBillingPlan.MONTH.priceVnd)
        assertEquals(now + 30 * day,
            DemoBillingRules.nextExpiry(now, null, DemoBillingPlan.MONTH))
    }

    @Test fun yearPlanUses365Days() {
        assertEquals(299_000, DemoBillingPlan.YEAR.priceVnd)
        assertEquals(now + 365 * day,
            DemoBillingRules.nextExpiry(now, null, DemoBillingPlan.YEAR))
    }

    @Test fun activePlanRenewalExtendsExistingExpiry() {
        val before = DemoBillingRules.newReceipt(
            now, null, DemoBillingPlan.MONTH, DemoBillingMethod.WALLET, "MF-DEMO-A"
        )
        assertEquals(now + 60 * day,
            DemoBillingRules.nextExpiry(now + day, before, DemoBillingPlan.MONTH))
    }

    @Test fun expiredPlanBeginsAgainFromPresentTime() {
        val before = DemoBillingRules.newReceipt(
            now - 40 * day, null, DemoBillingPlan.MONTH, DemoBillingMethod.WALLET,
            "MF-DEMO-B"
        )
        assertEquals(now + 30 * day,
            DemoBillingRules.nextExpiry(now, before, DemoBillingPlan.MONTH))
    }

    @Test fun cancelledPlanIsNotActiveAndRenewalStartsFromNow() {
        val before = DemoBillingRules.newReceipt(now, null, DemoBillingPlan.MONTH,
            DemoBillingMethod.CARD, "MF-DEMO-C").copy(cancelled = true)
        assertFalse(before.active(now + day))
        assertEquals(now + day + 365 * day,
            DemoBillingRules.nextExpiry(now + day, before, DemoBillingPlan.YEAR))
    }

    @Test fun receiptIsNotBackendPremiumOrAdministrativeAccess() {
        val invoice = DemoBillingRules.newReceipt(now, null,
            DemoBillingPlan.YEAR, DemoBillingMethod.CARD, "MF-DEMO-D")
        assertTrue(invoice.active(now))
        assertFalse(invoice.active(invoice.expiresAtMs))
        assertTrue(invoice.id.startsWith("MF-DEMO-"))
        // No AccountIdentity/AccountTier is changed by these independent rules.
        assertEquals("free", AccountTier.activeTier("free", null))
    }
}
