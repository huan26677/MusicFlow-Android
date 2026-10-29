package com.example.musicflow.core.history

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudHistoryRulesTest {
    private fun item(id: String, time: Long) =
        RecentSong(id, "content://local/$id", id, "Artist", time)

    @Test fun olderRemoteEntryNeverReplacesNewerLocal() {
        assertEquals(
            300L,
            CloudHistoryRules.merge(
                listOf(item("one", 300L)),
                listOf(item("one", 200L))
            ).single().playedAtMs
        )
    }

    @Test fun newerRemoteEntryCanUpdateTimestamp() {
        assertEquals(
            400L,
            CloudHistoryRules.merge(
                listOf(item("one", 300L)),
                listOf(item("one", 400L))
            ).single().playedAtMs
        )
    }

    @Test fun retainsOnlyTwentyMostRecent() {
        val rows = (1..30).map { item("song-$it", it.toLong()) }
        val combined = CloudHistoryRules.merge(rows.take(15), rows.drop(15))
        assertEquals(20, combined.size)
        assertEquals(30L, combined.first().playedAtMs)
        assertEquals(11L, combined.last().playedAtMs)
    }

    @Test fun normalizationIsStable() {
        assertEquals(
            CloudHistoryRules.key(" My   Song ", "ARTIST"),
            CloudHistoryRules.key("my song", "artist")
        )
    }
}
