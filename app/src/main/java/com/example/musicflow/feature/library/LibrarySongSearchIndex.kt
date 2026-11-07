package com.example.musicflow.feature.library

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.feature.search.SongSearch

/**
 * Immutable, order-preserving lookup for large offline libraries.
 * Normalization happens ONCE per catalog/sort change, not for every
 * keystroke. The same filtered list is passed to the playback queue.
 */
class LibrarySongSearchIndex(private val songs: List<DeviceSong>) {
    private val prepared = songs.map { song ->
        SongSearch.normalizeForSearch("${song.title} ${song.artist}")
    }

    fun filter(query: String): List<DeviceSong> {
        val normalized = SongSearch.normalizeForSearch(query)
        if (normalized.isBlank()) return songs
        val terms = normalized.split(' ').filter { it.isNotBlank() }
        return songs.filterIndexed { index, _ ->
            terms.all { prepared[index].contains(it) }
        }
    }
}
