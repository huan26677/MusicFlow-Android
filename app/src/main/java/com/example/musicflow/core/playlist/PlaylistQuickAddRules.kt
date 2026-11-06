package com.example.musicflow.core.playlist

/** Shared duplicate checks for the one-tap playlist editor. */
object PlaylistQuickAddRules {
    fun mediaId(explicitId: String?, deviceId: Long): String =
        explicitId?.takeIf { it.isNotBlank() } ?: "device-$deviceId"

    fun canAdd(
        explicitId: String?,
        deviceId: Long,
        currentIds: Set<String>,
        saving: Boolean
    ): Boolean = !saving && mediaId(explicitId, deviceId) !in currentIds

    fun match(title: String, artist: String, query: String): Boolean =
        query.isBlank() || com.example.musicflow.feature.search.SongSearch.matches(
            title, artist, query
        )
}
