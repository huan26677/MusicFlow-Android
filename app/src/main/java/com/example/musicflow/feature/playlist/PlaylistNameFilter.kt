package com.example.musicflow.feature.playlist

import com.example.musicflow.core.playlist.LocalPlaylist
import java.text.Normalizer
import java.util.Locale

/** Search playlist names without case/diacritic sensitivity, preserving original order. */
internal fun filterPlaylistNames(
    playlists: List<LocalPlaylist>,
    query: String
): List<LocalPlaylist> {
    val needle = searchablePlaylistText(query.trim())
    if (needle.isEmpty()) return playlists
    return playlists.filter { searchablePlaylistText(it.name).contains(needle) }
}

private fun searchablePlaylistText(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace('đ', 'd')
