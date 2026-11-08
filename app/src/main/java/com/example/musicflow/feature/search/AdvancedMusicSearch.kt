package com.example.musicflow.feature.search

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.feature.library.LibrarySongSearchIndex
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

/**
 * One immutable index per scanned catalog. Grouping and accent normalization
 * are performed once at refresh, not on every character typed.
 *
 * Never cache globally: the index must be discarded if MediaStore permission
 * changes or SAF documents become unavailable.
 */
internal class AdvancedMusicSearchIndex(private val songs: List<DeviceSong>) {
    private data class PreparedGroup(
        val collection: MusicCollection,
        val normalizedName: String
    )

    private val songIndex = LibrarySongSearchIndex(songs)
    private val albums = MusicCollections.albums(songs).map(::prepare)
    private val artists = MusicCollections.artists(songs).map(::prepare)
    private val genres = MusicCollections.genres(songs).map(::prepare)

    private fun prepare(group: MusicCollection) =
        PreparedGroup(group, SongSearch.normalizeForSearch(group.name))

    fun find(query: String): AdvancedSearchResults {
        val normalized = SongSearch.normalizeForSearch(query)
        if (normalized.isEmpty()) return AdvancedSearchResults()
        val terms = normalized.split(' ').filter { it.isNotBlank() }
        fun filter(groups: List<PreparedGroup>) =
            groups.filter { group ->
                terms.all { group.normalizedName.contains(it) }
            }.map { it.collection }

        return AdvancedSearchResults(
            songs = songIndex.filter(query),
            albums = filter(albums),
            artists = filter(artists),
            genres = filter(genres)
        )
    }
}

/** Compatibility helper for existing callers; prefer a retained index. */
internal object AdvancedMusicSearch {
    fun find(songs: List<DeviceSong>, query: String): AdvancedSearchResults =
        AdvancedMusicSearchIndex(songs).find(query)
}
