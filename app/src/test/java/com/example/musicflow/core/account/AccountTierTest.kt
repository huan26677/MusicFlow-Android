package com.example.musicflow.core.account

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AccountTierTest {
    private val now = Instant.parse("2026-10-29T00:00:00Z")

    @Test fun missingEntitlementIsFree() {
        assertEquals("free", AccountTier.activeTier(null, null, now))
    }

    @Test fun activePremiumIsPremium() {
        assertEquals(
            "premium",
            AccountTier.activeTier("premium", "2026-11-29T00:00:00Z", now)
        )
    }

    @Test fun expiredPremiumIsFree() {
        assertEquals(
            "free",
            AccountTier.activeTier("premium", "2026-10-28T00:00:00Z", now)
        )
    }

    @Test fun premiumWithoutExpiryIsFree() {
        assertEquals("free", AccountTier.activeTier("premium", null, now))
    }

    @Test fun unknownOrMalformedEntitlementIsFree() {
        assertEquals("free", AccountTier.activeTier("admin", "2030-01-01T00:00:00Z", now))
        assertEquals("free", AccountTier.activeTier("premium", "not-a-date", now))
    }
}
