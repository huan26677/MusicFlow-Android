package com.example.musicflow.core.ux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListeningJourneyRulesTest {
    @Test fun recentPreviewCanExpandWithoutChangingHistory() {
        assertEquals(4, ListeningJourneyRules.recentVisibleCount(17, false))
        assertEquals(17, ListeningJourneyRules.recentVisibleCount(17, true))
        assertEquals(2, ListeningJourneyRules.recentVisibleCount(2, false))
        assertEquals(0, ListeningJourneyRules.recentVisibleCount(0, true))
    }

    @Test fun queuePositionIsHumanFriendlyAndHandlesEmptyPlayer() {
        assertEquals("Chưa có bài trong hàng đợi",
            ListeningJourneyRules.queuePosition(0, 0, false))
        assertEquals("Bài 1 / 5", ListeningJourneyRules.queuePosition(0, 5, true))
        assertEquals("Bài 3 / 5", ListeningJourneyRules.queuePosition(2, 5, true))
        assertEquals("Bài 5 / 5", ListeningJourneyRules.queuePosition(99, 5, true))
    }

    @Test fun searchSuggestionFindsAnotherCategoryWithoutNewScan() {
        assertEquals("albums", ListeningJourneyRules.suggestedSearchCategory(
            "songs", 0, 2, 3, 0))
        assertEquals("songs", ListeningJourneyRules.suggestedSearchCategory(
            "albums", 5, 0, 0, 0))
        assertNull(ListeningJourneyRules.suggestedSearchCategory("songs", 3, 2, 0, 0))
        assertNull(ListeningJourneyRules.suggestedSearchCategory("songs", 0, 0, 0, 0))
    }

    @Test fun resultGroupNamesAreReadable() {
        assertEquals("Nghệ sĩ", ListeningJourneyRules.searchCategoryLabel("artists"))
        assertEquals("Thể loại", ListeningJourneyRules.searchCategoryLabel("genres"))
    }
}
