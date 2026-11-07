package com.example.musicflow.core.smart

import java.util.Locale

/** Pure metadata projection. Smart lists never create Room rows or copy audio files. */
data class SmartSong(
    val key: String,
    val title: String,
    val dateAddedSeconds: Long,
    val durationMs: Long
)

enum class SmartPlaylistKind(val title: String, val description: String) {
    MIX("Mix của bạn", "Kết hợp bài vừa nghe, yêu thích và nhạc mới."),
    FAVORITES("Yêu thích", "Các bài có dấu trái tim trong thư viện hiện tại."),
    RECENT("Nghe gần đây", "Nhạc đã nghe, theo thứ tự gần nhất trước."),
    NEWEST("Mới thêm", "Nhạc có ngày thêm mới nhất trên thiết bị."),
    LONG_LISTEN("Nghe thật lâu", "Những bài dài ít nhất 4 phút.")
}

data class SmartPlaylistSnapshot(
    val kind: SmartPlaylistKind,
    val songKeys: List<String>
)

object SmartLibraryRules {
    private const val MAX_ITEMS = 30
    private const val MIN_LONG_DURATION_MS = 240_000L

    /**
     * A deterministic, bounded, device-only view. No streaming URLs, missing
     * files or stale history/favorites are fabricated into playback queues.
     * Unknown MediaStore timestamps are excluded only from NEWEST.
     */
    fun build(
        scanned: List<SmartSong>,
        favoriteKeys: Set<String>,
        recentKeys: List<String>
    ): List<SmartPlaylistSnapshot> {
        val library = scanned.asSequence()
            .filter { it.key.startsWith("content://") && it.title.isNotBlank() }
            .distinctBy { it.key }
            .toList()
        val byKey = library.associateBy { it.key }
        val byTitle = compareBy<SmartSong>(
            { it.title.trim().lowercase(Locale.ROOT) }, { it.key }
        )
        val favorites = library.filter { it.key in favoriteKeys }
            .sortedWith(byTitle).take(MAX_ITEMS).map { it.key }
        val recent = recentKeys.asSequence().distinct().filter { it in byKey }
            .take(MAX_ITEMS).toList()
        val newest = library.asSequence()
            .filter { it.dateAddedSeconds > 0L }
            .sortedWith(compareByDescending<SmartSong> { it.dateAddedSeconds }
                .then(byTitle))
            .take(MAX_ITEMS).map { it.key }.toList()
        val longSongs = library.asSequence()
            .filter { it.durationMs >= MIN_LONG_DURATION_MS }
            .sortedWith(byTitle)
            .take(MAX_ITEMS).map { it.key }.toList()
        // Sources often overlap heavily (a recent track may also be favorite
        // and newly added). Fill remaining slots from the scanned library so
        // a large collection still produces up to 30 *distinct* tracks.
        val mix = (recent.take(12) + favorites.take(12) + newest.take(12) +
            longSongs.take(6) + library.sortedWith(byTitle).map { it.key })
            .distinct().take(MAX_ITEMS)
        return listOf(
            SmartPlaylistSnapshot(SmartPlaylistKind.MIX, mix),
            SmartPlaylistSnapshot(SmartPlaylistKind.FAVORITES, favorites),
            SmartPlaylistSnapshot(SmartPlaylistKind.RECENT, recent),
            SmartPlaylistSnapshot(SmartPlaylistKind.NEWEST, newest),
            SmartPlaylistSnapshot(SmartPlaylistKind.LONG_LISTEN, longSongs)
        )
    }
}
