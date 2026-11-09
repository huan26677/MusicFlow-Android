package com.example.musicflow.core.community

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityRulesTest {
    @Test fun starsOnlyAcceptOneThroughFive() {
        assertFalse(CommunityRules.validStars(0))
        assertFalse(CommunityRules.validStars(6))
        assertTrue((1..5).all(CommunityRules::validStars))
    }
    @Test fun commentTrimsWhitespaceAndRejectsEmpty() {
        assertEquals("Nhạc hay", CommunityRules.cleanComment("  Nhạc hay  "))
        assertTrue(runCatching { CommunityRules.cleanComment("   ") }.isFailure)
    }
    @Test fun commentRejectsMoreThanFiveHundredCharacters() {
        assertEquals(500,CommunityRules.cleanComment("a".repeat(500)).length)
        assertTrue(runCatching { CommunityRules.cleanComment("a".repeat(501)) }.isFailure)
    }
    @Test fun reportRequiresAtLeastFiveAndAtMostThreeHundredCharacters() {
        assertTrue(runCatching { CommunityRules.cleanReason("spam") }.isFailure)
        assertEquals("Spam lặp lại",CommunityRules.cleanReason("  Spam lặp lại "))
        assertTrue(runCatching { CommunityRules.cleanReason("x".repeat(301)) }.isFailure)
    }
    @Test fun onlineIdsMustBeCanonicalUuidNeverLocalSafUri() {
        assertTrue(CommunityRules.isCloudTrackId("5bed8b88-a111-4d38-8e01-123456789001"))
        assertFalse(CommunityRules.isCloudTrackId("content://media/external/audio/3"))
        assertFalse(CommunityRules.isCloudTrackId("demo-community"))
    }
}
