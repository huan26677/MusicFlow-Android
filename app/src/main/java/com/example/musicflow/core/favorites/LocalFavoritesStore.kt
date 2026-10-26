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