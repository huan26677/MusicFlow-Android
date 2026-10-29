package com.example.musicflow.core.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CloudSyncRulesTest {
    private data class Song(val title: String, val artist: String)

    @Test fun matchesOnlyOneLocalSong() {
        val songs = listOf(Song("Bài hát", "Nghệ sĩ"), Song("Bài khác", "Nghệ sĩ"))
        assertEquals(
            songs.first(),
            CloudSyncRules.uniqueLocalMatch(
                songs, " Bài hát ", "NGHỆ SĨ", { it.title }, { it.artist }
            )
        )
    }

    @Test fun rejectsAmbiguousOrMissingSongs() {
        val songs = listOf(Song("One", "Artist"), Song(" one ", "artist"))
        assertNull(
            CloudSyncRules.uniqueLocalMatch(
                songs, "One", "Artist", { it.title }, { it.artist }
            )
        )
        assertNull(
            CloudSyncRules.uniqueLocalMatch(
                songs, "Not found", "Artist", { it.title }, { it.artist }
            )
        )
    }

    @Test fun cloudMergeKeepsBothAndNeverDuplicates() {
        val older = CloudTrackMeta("Track A", "Artist")
        val next = CloudTrackMeta("Track B", "Artist")
        val merged = CloudSyncRules.mergeTracks(
            listOf(older), listOf(older.copy(title = " TRACK A "), next)
        )
        assertEquals(listOf(older, next), merged)
    }

    @Test fun cloudMergeSkipsBlankMetadata() {
        assertEquals(
            emptyList<CloudTrackMeta>(),
            CloudSyncRules.mergeTracks(emptyList(), listOf(CloudTrackMeta("", "Artist")))
        )
    }
}
