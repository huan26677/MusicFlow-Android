package com.example.musicflow.feature.library

import com.example.musicflow.core.media.DeviceSong
import java.util.Locale

enum class LibrarySongSort(val label: String) {
    TITLE("Tên bài"),
    ARTIST("Nghệ sĩ"),
    ALBUM("Album"),
    NEWEST("Mới thêm")
}

/** The same sorted list is used for display and playback-queue order. */
fun sortLibrarySongs(
    songs: List<DeviceSong>,
    sort: LibrarySongSort
): List<DeviceSong> {
    val byTitle = compareBy<DeviceSong>(
        { it.title.trim().lowercase(Locale.ROOT) },
        { it.uri.toString() }
    )
    return when (sort) {
        LibrarySongSort.TITLE -> songs.sortedWith(byTitle)
        LibrarySongSort.ARTIST -> songs.sortedWith(
            compareBy<DeviceSong> { it.artist.trim().lowercase(Locale.ROOT) }
                .then(byTitle)
        )
        LibrarySongSort.ALBUM -> songs.sortedWith(
            compareBy<DeviceSong> { it.album.trim().lowercase(Locale.ROOT) }
                .then(byTitle)
        )
        LibrarySongSort.NEWEST -> songs.sortedWith(
            compareByDescending<DeviceSong> { it.dateAddedSeconds }
                .then(byTitle)
        )
    }
}
