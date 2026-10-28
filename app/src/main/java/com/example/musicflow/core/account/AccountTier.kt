package com.example.musicflow.core.account

import java.time.Instant

/**
 * Display-only tier derived from Supabase's RLS-protected entitlement.
 * Paid-resource authorization must also be enforced by the backend.
 */
object AccountTier {
    fun activeTier(
        tier: String?,
        expiresAt: String?,
        now: Instant = Instant.now()
    ): String {
        if (tier != "premium" || expiresAt.isNullOrBlank()) return "free"
        val expiry = try {
            Instant.parse(expiresAt)
        } catch (_: Exception) {
            return "free"
        }
        return if (expiry.isAfter(now)) "premium" else "free"
    }
}
