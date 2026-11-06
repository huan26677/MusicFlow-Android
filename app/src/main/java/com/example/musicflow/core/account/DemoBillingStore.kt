package com.example.musicflow.core.account

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

/** Prices are illustrative ONLY; no payment SDK, bank, card details, network or entitlement. */
enum class DemoBillingPlan(val label: String, val priceVnd: Int, val durationDays: Long) {
    MONTH("30 ngày", 29_000, 30),
    YEAR("365 ngày", 299_000, 365)
}

enum class DemoBillingMethod(val label: String) {
    WALLET("Ví điện tử giả lập"), CARD("Thẻ ngân hàng giả lập")
}

data class DemoBillingReceipt(
    val id: String,
    val plan: DemoBillingPlan,
    val method: DemoBillingMethod,
    val purchasedAtMs: Long,
    val expiresAtMs: Long,
    val cancelled: Boolean = false
) {
    fun active(nowMs: Long): Boolean = !cancelled && expiresAtMs > nowMs
}

sealed interface DemoBillingResult {
    data class Success(val receipt: DemoBillingReceipt) : DemoBillingResult
    data object Declined : DemoBillingResult
}

/** Stateless rules used by the store and unit tests: simulated access is presentation-only. */
object DemoBillingRules {
    fun nextExpiry(nowMs: Long, previous: DemoBillingReceipt?, plan: DemoBillingPlan): Long {
        val baseline = if (previous?.active(nowMs) == true) {
            maxOf(nowMs, previous.expiresAtMs)
        } else nowMs
        return Math.addExact(baseline, Math.multiplyExact(plan.durationDays, 86_400_000L))
    }

    fun newReceipt(
        nowMs: Long,
        previous: DemoBillingReceipt?,
        plan: DemoBillingPlan,
        method: DemoBillingMethod,
        id: String
    ) = DemoBillingReceipt(id, plan, method, nowMs, nextExpiry(nowMs, previous, plan))
}

/**
 * One-device, per-account-scope DEMO receipts. A guest is a separate scope;
 * signing in to a real or Demo account never imports a guest purchase. The
 * actual AccountIdentity.tier, Supabase entitlements and role RPCs are untouched.
 */
class DemoBillingStore(
    context: Context,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "musicflow_demo_billing_v1_not_real", Context.MODE_PRIVATE
    )

    private fun prefix(scope: String): String {
        require(scope.isNotBlank())
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(scope.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) } + "."
    }

    @Synchronized
    fun receipt(scope: String): DemoBillingReceipt? {
        val key = prefix(scope)
        val id = prefs.getString(key + "id", null) ?: return null
        val plan = runCatching {
            DemoBillingPlan.valueOf(prefs.getString(key + "plan", "").orEmpty())
        }.getOrNull() ?: return null
        val method = runCatching {
            DemoBillingMethod.valueOf(prefs.getString(key + "method", "").orEmpty())
        }.getOrNull() ?: return null
        val created = prefs.getLong(key + "created", 0L)
        val expires = prefs.getLong(key + "expires", 0L)
        if (created <= 0 || expires <= created || id.isBlank()) return null
        return DemoBillingReceipt(id, plan, method, created, expires,
            cancelled = prefs.getBoolean(key + "cancelled", false))
    }

    @Synchronized
    fun simulatePayment(
        scope: String, plan: DemoBillingPlan, method: DemoBillingMethod,
        simulateDecline: Boolean
    ): DemoBillingResult {
        // Failed simulations MUST NOT mutate even an existing active receipt.
        if (simulateDecline) return DemoBillingResult.Declined
        val timestamp = now()
        val newRecord = DemoBillingRules.newReceipt(
            timestamp, receipt(scope), plan, method,
            id = "MF-DEMO-" + UUID.randomUUID().toString().take(8).uppercase()
        )
        val key = prefix(scope)
        check(prefs.edit()
            .putString(key + "id", newRecord.id)
            .putString(key + "plan", plan.name)
            .putString(key + "method", method.name)
            .putLong(key + "created", newRecord.purchasedAtMs)
            .putLong(key + "expires", newRecord.expiresAtMs)
            .putBoolean(key + "cancelled", false)
            .commit()
        ) { "Không lưu được giao dịch mô phỏng trên thiết bị." }
        return DemoBillingResult.Success(newRecord)
    }

    @Synchronized
    fun cancel(scope: String) {
        val prior = receipt(scope) ?: return
        if (prior.cancelled) return
        val key = prefix(scope)
        check(prefs.edit().putBoolean(key + "cancelled", true).commit()) {
            "Không thể hủy gói mô phỏng."
        }
    }

    /** Only clears simulated billing records for the current scope. */
    @Synchronized
    fun reset(scope: String) {
        val key = prefix(scope)
        check(prefs.edit().remove(key + "id").remove(key + "plan")
            .remove(key + "method").remove(key + "created")
            .remove(key + "expires").remove(key + "cancelled").commit())
    }
}
