package com.example.musicflow.core.account

import java.util.Locale

/** Device URIs must never be uploaded. Only these metadata values are synced. */
data class CloudTrackMeta(
    val title: String,
    val artist: String,
    val addedAtMs: Long = 0L
)

data class CloudFavoriteMeta(
    val title: String,
    val artist: String
)

object CloudSyncRules {
    private fun canonical(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    fun key(title: String, artist: String): String =
        canonical(title) + "\u0000" + canonical(artist)

    /**
     * Never interpret a track URI from another device.
     * A track may be linked only if exactly ONE local song matches metadata.
     */
    fun <T> uniqueLocalMatch(
        candidates: List<T>,
        title: String,
        artist: String,
        songTitle: (T) -> String,
        songArtist: (T) -> String
    ): T? {
        val wanted = key(title, artist)
        val matches = candidates.filter {
            key(songTitle(it), songArtist(it)) == wanted
        }
        return matches.singleOrNull()
    }

    fun mergeTracks(
        cloud: List<CloudTrackMeta>,
        local: List<CloudTrackMeta>
    ): List<CloudTrackMeta> {
        val seen = mutableSetOf<String>()
        return (cloud + local).filter { track ->
            track.title.isNotBlank() && seen.add(key(track.title, track.artist))
        }.take(500)
    }
}
