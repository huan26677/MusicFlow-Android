package com.example.musicflow.core.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueEditRulesTest {
    @Test fun currentTrackCannotBeRemoved() {
        assertFalse(QueueEditRules.canRemove(4, 1, 1))
        assertTrue(QueueEditRules.canRemove(4, 1, 0))
        assertTrue(QueueEditRules.canRemove(4, 1, 3))
    }

    @Test fun invalidOrEmptyQueueCannotBeEdited() {
        assertFalse(QueueEditRules.canRemove(0, 0, 0))
        assertFalse(QueueEditRules.canRemove(2, 2, 0))
        assertFalse(QueueEditRules.canRemove(2, 0, -1))
        assertFalse(QueueEditRules.canClearOthers(1, 0))
    }

    @Test fun futureTracksMoveWithinFutureSegment() {
        assertTrue(QueueEditRules.canMove(5, 1, 3, 2, false))
        assertTrue(QueueEditRules.canMove(5, 1, 2, 3, false))
    }

    @Test fun pastTracksMoveWithinPastSegment() {
        assertTrue(QueueEditRules.canMove(5, 3, 0, 1, false))
        assertTrue(QueueEditRules.canMove(5, 3, 2, 1, false))
    }

    @Test fun currentItemIsFixedAndCannotBeCrossed() {
        assertFalse(QueueEditRules.canMove(5, 2, 2, 3, false))
        assertFalse(QueueEditRules.canMove(5, 2, 1, 2, false))
        assertFalse(QueueEditRules.canMove(5, 2, 3, 2, false))
        assertFalse(QueueEditRules.canMove(5, 2, 1, 3, false))
    }

    @Test fun shuffleDisablesReorder() {
        assertFalse(QueueEditRules.canMove(5, 1, 2, 3, true))
    }

    @Test fun cannotMoveOutOfBoundsOrToSamePlace() {
        assertFalse(QueueEditRules.canMove(5, 1, 3, 3, false))
        assertFalse(QueueEditRules.canMove(5, 1, 4, 5, false))
        assertFalse(QueueEditRules.canMove(5, 1, -1, 0, false))
        assertFalse(QueueEditRules.canMove(5, 1, 4, 2, false))
    }

    @Test fun clearOthersKeepsCurrent() {
        assertTrue(QueueEditRules.canClearOthers(5, 2))
        assertFalse(QueueEditRules.canClearOthers(0, 0))
        assertFalse(QueueEditRules.canClearOthers(2, -1))
    }
}
