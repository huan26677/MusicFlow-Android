package com.example.musicflow.feature.search

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.feature.library.MusicCollection
import com.example.musicflow.feature.library.MusicCollections

/**
 * One stable result snapshot per query, so switching tabs does not trigger
 * extra MediaStore scans or reorder the current playback selection.
 */
internal data class AdvancedSearchResults(
    val songs: List<DeviceSong> = emptyList(),
    val albums: List<MusicCollection> = emptyList(),
    val artists: List<MusicCollection> = emptyList(),
    val genres: List<MusicCollection> = emptyList()
)

internal object AdvancedMusicSearch {
    fun find(songs: List<DeviceSong>, query: String): AdvancedSearchResults {
        if (query.isBlank()) return AdvancedSearchResults()
        return AdvancedSearchResults(
            songs = SongSearch.filter(songs, query),
            albums = MusicCollections.albums(songs).filter {
                SongSearch.matchesText(it.name, query)
            },
            artists = MusicCollections.artists(songs).filter {
                SongSearch.matchesText(it.name, query)
            },
            genres = MusicCollections.genres(songs).filter {
                SongSearch.matchesText(it.name, query)
            }
        )
    }
}
