package com.example.musicflow.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleOAuthFlowTest {
    private val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    private val pending = GoogleOAuthFlow.Pending(verifier, "random_nonce_123456", 1000)

    @Test fun challengeMatchesPkceKnownVector() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            GoogleOAuthFlow.challenge(verifier)
        )
    }

    @Test fun authorizeUsesSupabaseAndCodeChallenge() {
        val url = GoogleOAuthFlow.authorizeUrl(
            "https://example.supabase.co", pending
        )
        assertTrue(url.startsWith("https://example.supabase.co/auth/v1/authorize?"))
        assertTrue(url.contains("provider=google"))
        assertTrue(url.contains("code_challenge_method=s256"))
        assertTrue(url.contains("flow%3Drandom_nonce_123456"))
        assertFalse(url.contains("code_verifier"))
        assertFalse(url.contains(verifier))
    }

    @Test fun acceptsValidOneTimeCallback() {
        assertEquals(
            "one-time-code",
            GoogleOAuthFlow.validate(
                "musicflow://auth/callback?flow=random_nonce_123456&code=one-time-code",
                pending, 1001
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWrongNonce() {
        GoogleOAuthFlow.validate(
            "musicflow://auth/callback?flow=attacker&code=code", pending, 1002
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsExpiredRequest() {
        GoogleOAuthFlow.validate(
            "musicflow://auth/callback?flow=random_nonce_123456&code=code",
            pending, 700_001
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnexpectedOrigin() {
        GoogleOAuthFlow.validate(
            "https://attacker.example/callback?flow=random_nonce_123456&code=code",
            pending, 1003
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateCode() {
        GoogleOAuthFlow.validate(
            "musicflow://auth/callback?flow=random_nonce_123456&code=a&code=b",
            pending, 1003
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateFlow() {
        GoogleOAuthFlow.validate(
            "musicflow://auth/callback?flow=random_nonce_123456&flow=random_nonce_123456&code=x",
            pending, 1003
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCallbackWithPort() {
        GoogleOAuthFlow.validate(
            "musicflow://auth:443/callback?flow=random_nonce_123456&code=x",
            pending, 1003
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedCallbackQuery() {
        GoogleOAuthFlow.validate(
            "musicflow://auth/callback?flow=random_nonce_123456&code=" + "x".repeat(5000),
            pending, 1003
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOAuthErrorEvenWithCode() {
        GoogleOAuthFlow.validate(
            "musicflow://auth/callback?flow=random_nonce_123456&code=x&error=access_denied",
            pending, 1003
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCallbackWithFragment() {
        GoogleOAuthFlow.validate(
            "musicflow://auth/callback?flow=random_nonce_123456&code=x#access_token=abc",
            pending, 1003
        )
    }
}
