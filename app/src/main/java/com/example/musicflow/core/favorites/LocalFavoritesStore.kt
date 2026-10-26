package com.example.musicflow.core.favorites

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalFavoritesStore private constructor(
    context: Context
) {
    private val prefs = context.applicationContext
        .getSharedPreferences(
            "musicflow_favorites",
            Context.MODE_PRIVATE
        )

    private val _songUris = MutableStateFlow(
        prefs.getStringSet("song_uris", emptySet())
            .orEmpty()
            .toSet()
    )

    val songUris = _songUris.asStateFlow()

    private val _albumIds = MutableStateFlow(
        prefs.getStringSet("album_ids", emptySet())
            .orEmpty()
            .toSet()
    )

    val albumIds = _albumIds.asStateFlow()

    @Synchronized
    fun toggleAlbum(albumId: Long) {
        if (albumId <= 0L) return

        val key = albumId.toString()
        val updated = _albumIds.value.toMutableSet()

        if (!updated.add(key)) {
            updated.remove(key)
        }

        val snapshot = updated.toSet()

        prefs.edit()
            .putStringSet("album_ids", snapshot)
            .apply()

        _albumIds.value = snapshot
    }

    @Synchronized
    fun toggleSong(uri: Uri) {
        val key = uri.toString()
        val updated = _songUris.value.toMutableSet()

        if (!updated.add(key)) {
            updated.remove(key)
        }

        prefs.edit()
            .putStringSet("song_uris", updated.toSet())
            .apply()

        _songUris.value = updated.toSet()
    }

    companion object {
        @Volatile
        private var instance: LocalFavoritesStore? = null

        fun getInstance(context: Context): LocalFavoritesStore =
            instance ?: synchronized(this) {
                instance ?: LocalFavoritesStore(
                    context.applicationContext
                ).also { instance = it }
            }
    }
}