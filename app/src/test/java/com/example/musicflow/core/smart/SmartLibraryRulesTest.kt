package com.example.musicflow.core.smart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartLibraryRulesTest {
    private fun song(id: Int, title: String = "Bài $id", days: Long = 10,
        duration: Long = 185_000L) = SmartSong(
        "content://media/external/audio/media/$id", title, days, duration
    )

    @Test fun choosesOnlyCurrentlyScannedLocalFiles() {
        val a = song(1)
        val b = song(2)
        val remote = SmartSong("https://example.invalid/audio.mp3", "Streaming", 99, 320_000L)
        val keys = SmartLibraryRules.build(listOf(a, b, a, remote),
            setOf(a.key, remote.key, "content://missing"), listOf("content://missing", b.key))
        assertEquals(listOf(a.key), keys[SmartPlaylistKind.FAVORITES.ordinal].songKeys)
        assertEquals(listOf(b.key), keys[SmartPlaylistKind.RECENT.ordinal].songKeys)
        assertTrue(keys.flatMap { it.songKeys }.none { it == remote.key || it == "content://missing" })
    }

    @Test fun recentOrderAndMixPriorityAreStableAndDeduplicated() {
        val a = song(1); val b = song(2); val c = song(3)
        val result = SmartLibraryRules.build(listOf(a, b, c), setOf(b.key, c.key),
            listOf(c.key, a.key, c.key, b.key))
        assertEquals(listOf(c.key, a.key, b.key),
            result[SmartPlaylistKind.RECENT.ordinal].songKeys)
        assertEquals(listOf(c.key, a.key, b.key),
            result[SmartPlaylistKind.MIX.ordinal].songKeys)
    }

    @Test fun newestIgnoresUnknownDateAndUsesDeterministicTieBreaking() {
        val unknown = song(9, "A", days = 0)
        val older = song(1, "B", days = 10)
        val newest = song(2, "C", days = 20)
        val result = SmartLibraryRules.build(listOf(unknown, older, newest), emptySet(), emptyList())
        assertEquals(listOf(newest.key, older.key),
            result[SmartPlaylistKind.NEWEST.ordinal].songKeys)
    }

    @Test fun durationThresholdAndFavoritesAreComputedDynamically() {
        val short = song(1, "Short", duration = 239_999L)
        val long = song(2, "Long", duration = 240_000L)
        val original = SmartLibraryRules.build(listOf(short, long), setOf(short.key), emptyList())
        val changed = SmartLibraryRules.build(listOf(short, long), setOf(long.key), emptyList())
        assertEquals(listOf(long.key), original[SmartPlaylistKind.LONG_LISTEN.ordinal].songKeys)
        assertEquals(listOf(short.key), original[SmartPlaylistKind.FAVORITES.ordinal].songKeys)
        assertEquals(listOf(long.key), changed[SmartPlaylistKind.FAVORITES.ordinal].songKeys)
    }

    @Test fun emptyLibraryIsSafeAndCannotManufacturePlaybackTracks() {
        val results = SmartLibraryRules.build(emptyList(), setOf("content://gone"),
            listOf("content://gone"))
        assertEquals(SmartPlaylistKind.entries.size, results.size)
        assertTrue(results.all { it.songKeys.isEmpty() })
    }

    @Test fun eachListIsCappedAtThirtyTracks() {
        val music = (0..120).map { song(it, days = it.toLong(), duration = 300_000L) }
        val list = SmartLibraryRules.build(music, music.map { it.key }.toSet(),
            music.reversed().map { it.key })
        assertTrue(list.all { it.songKeys.size <= 30 })
        assertEquals(30, list[SmartPlaylistKind.MIX.ordinal].songKeys.size)
    }
}
