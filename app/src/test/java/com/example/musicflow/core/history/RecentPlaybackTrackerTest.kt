
package com.example.musicflow.core.history

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentPlaybackTrackerTest {

    @Test
    fun doesNotRecordBeforePlaybackStarts() {
        val tracker = RecentPlaybackTracker()

        assertFalse(
            tracker.shouldRecord(
                isPlaying = false,
                mediaId = "device-1",
                audioUri = "content://media/audio/1"
            )
        )

        assertTrue(
            tracker.shouldRecord(
                isPlaying = true,
                mediaId = "device-1",
                audioUri = "content://media/audio/1"
            )
        )
    }

    @Test
    fun pauseAndResumeDoNotRecordAgain() {
        val tracker = RecentPlaybackTracker()

        tracker.shouldRecord(
            true, "device-1", "content://media/audio/1"
        )

        assertFalse(
            tracker.shouldRecord(
                false, "device-1", "content://media/audio/1"
            )
        )

        assertFalse(
            tracker.shouldRecord(
                true, "device-1", "content://media/audio/1"
            )
        )
    }

    @Test
    fun artworkUpdateDoesNotRecordAgain() {
        val tracker = RecentPlaybackTracker()

        assertTrue(
            tracker.shouldRecord(
                true, "device-1", "content://media/audio/1"
            )
        )

        // Metadata thay đổi, nhưng ID và URI vẫn giữ nguyên.
        assertFalse(
            tracker.shouldRecord(
                true, "device-1", "content://media/audio/1"
            )
        )
    }

    @Test
    fun nextAndPreviousSongsAreRecorded() {
        val tracker = RecentPlaybackTracker()

        assertTrue(
            tracker.shouldRecord(
                true, "device-1", "content://media/audio/1"
            )
        )

        assertTrue(
            tracker.shouldRecord(
                true, "device-2", "content://media/audio/2"
            )
        )

        assertTrue(
            tracker.shouldRecord(
                true, "device-1", "content://media/audio/1"
            )
        )
    }

    @Test
    fun missingIdentityIsIgnored() {
        val tracker = RecentPlaybackTracker()

        assertFalse(tracker.shouldRecord(true, "", "uri"))
        assertFalse(tracker.shouldRecord(true, "id", ""))
    }
}
