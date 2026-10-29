package com.example.musicflow.core.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueInsertRulesTest {
    @Test fun nextInsertsAfterCurrentNotAtEnd() {
        assertEquals(2, QueueInsertRules.insertionIndex(5, 1, QueuePlacement.NEXT, false))
    }

    @Test fun nextAfterLastAppends() {
        assertEquals(5, QueueInsertRules.insertionIndex(5, 4, QueuePlacement.NEXT, false))
    }

    @Test fun endAlwaysAppendsEvenUnderShuffle() {
        assertEquals(5, QueueInsertRules.insertionIndex(5, 1, QueuePlacement.END, true))
    }

    @Test fun nextUnderShuffleIsRejectedRatherThanMisleading() {
        assertNull(QueueInsertRules.insertionIndex(5, 1, QueuePlacement.NEXT, true))
    }

    @Test fun nextUnderRepeatOneIsRejectedRatherThanMisleading() {
        assertNull(QueueInsertRules.insertionIndex(5, 1, QueuePlacement.NEXT, false, true))
        assertEquals(5, QueueInsertRules.insertionIndex(5, 1, QueuePlacement.END, false, true))
    }

    @Test fun emptyQueueIsRejected() {
        assertNull(QueueInsertRules.insertionIndex(0, 0, QueuePlacement.END, false))
    }

    @Test fun invalidIndexIsRejected() {
        assertNull(QueueInsertRules.insertionIndex(5, -1, QueuePlacement.NEXT, false))
        assertNull(QueueInsertRules.insertionIndex(5, 5, QueuePlacement.END, false))
    }

    @Test fun oneTrackQueueCanGrow() {
        assertEquals(1, QueueInsertRules.insertionIndex(1, 0, QueuePlacement.NEXT, false))
    }
}
