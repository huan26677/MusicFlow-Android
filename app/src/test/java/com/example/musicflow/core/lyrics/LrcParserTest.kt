package com.example.musicflow.core.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test fun bomMetadataAndMillisecondFractions() {
        val lines = LrcParser.parse(
            "﻿[ar:MusicFlow QA]\n[00:01.2]First\n[00:02.34]Second\n[00:03.567]Third"
        )
        assertEquals(listOf(1200L, 2340L, 3567L), lines.map { it.timeMs })
        assertEquals(listOf("First", "Second", "Third"), lines.map { it.text })
    }

    @Test fun multipleTimestampsAndUnsortedLinesAreSorted() {
        val lines = LrcParser.parse(
            "[00:03.00]Later\n[00:01.00][00:02.00]Repeated"
        )
        assertEquals(listOf(1000L, 2000L, 3000L), lines.map { it.timeMs })
        assertEquals(listOf("Repeated", "Repeated", "Later"), lines.map { it.text })
    }

    @Test fun offsetAndNegativeClamp() {
        val lines = LrcParser.parse(
            "[offset:-1500]\n[00:01.00]Early\n[00:04.00]Later"
        )
        assertEquals(listOf(0L, 2500L), lines.map { it.timeMs })
    }

    @Test fun invalidAndBlankTimestampsIgnored() {
        assertTrue(LrcParser.parse(
            "[ar:Artist]\n[00:70.000]Invalid\n[00:01.00]\nPlain text"
        ).isEmpty())
    }

    @Test fun longTextTruncatedToFourHundredCharacters() {
        val result = LrcParser.parse("[00:01.00]${"x".repeat(600)}")
        assertEquals(1, result.size)
        assertEquals(400, result.single().text.length)
    }

    @Test fun timelineFollowsSeekAndBeforeFirstTimestamp() {
        val lyrics = LrcParser.parse(
            "[00:01.00]A\n[00:02.00]B\n[00:05.00]C"
        )
        assertEquals(-1, LyricsTimeline.activeIndex(lyrics, 999))
        assertEquals(0, LyricsTimeline.activeIndex(lyrics, 1000))
        assertEquals(1, LyricsTimeline.activeIndex(lyrics, 2000))
        assertEquals(2, LyricsTimeline.activeIndex(lyrics, 6200))
        assertEquals(0, LyricsTimeline.activeIndex(lyrics, 1100))
        assertEquals(-1, LyricsTimeline.activeIndex(emptyList(), 2000))
    }
}
