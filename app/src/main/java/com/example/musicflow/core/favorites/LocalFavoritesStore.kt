package com.example.musicflow.core.favorites

import android.content.Context
import android.net.Uri
import com.example.musicflow.core.media.DeviceSong
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FavoriteSongDetails(
    val title: String,
    val artist: String
)

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

    private fun loadSongDetails(): Map<String, FavoriteSongDetails> {
        val json = runCatching {
            JSONObject(prefs.getString("song_details", "{}") ?: "{}")
        }.getOrElse { JSONObject() }

        return json.keys().asSequence().mapNotNull { uri ->
            val item = json.optJSONObject(uri)
                ?: return@mapNotNull null

            uri to FavoriteSongDetails(
                title = item.optString("title").ifBlank {
                    "Bài hát yêu thích"
                },
                artist = item.optString("artist").ifBlank {
                    "Không rõ nghệ sĩ"
                }
            )
        }.toMap()
    }

    private val _songDetails = MutableStateFlow(loadSongDetails())

    val songDetails = _songDetails.asStateFlow()

    private val _albumIds = MutableStateFlow(
        prefs.getStringSet("album_ids", emptySet())
            .orEmpty()
            .toSet()
    )

    val albumIds = _albumIds.asStateFlow()

    /** Idempotent merge for cloud sync; never toggles an existing favorite off. */
    @Synchronized
    fun addSongIfAbsent(song: DeviceSong): Boolean {
        val key = song.uri.toString()
        if (key in _songUris.value) return false
        val uris = _songUris.value + key
        val details = _songDetails.value + (
            key to FavoriteSongDetails(song.title, song.artist)
        )
        val json = JSONObject()
        details.forEach { (uri, item) ->
            json.put(uri, JSONObject().put("title", item.title).put("artist", item.artist))
        }
        check(prefs.edit()
            .putStringSet("song_uris", uris)
            .putString("song_details", json.toString())
            .commit()) { "Không lưu được bài hát yêu thích." }
        _songDetails.value = details
        _songUris.value = uris
        return true
    }

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
    fun toggleSong(
        uri: Uri,
        title: String? = null,
        artist: String? = null
    ) {
        val key = uri.toString()
        val updated = _songUris.value.toMutableSet()
        val details = _songDetails.value.toMutableMap()

        if (updated.add(key)) {
            if (!title.isNullOrBlank()) {
                details[key] = FavoriteSongDetails(
                    title = title.trim(),
                    artist = artist?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: "Không rõ nghệ sĩ"
                )
            }
        } else {
            updated.remove(key)
            details.remove(key)
        }

        val json = JSONObject()

        details.forEach { (savedUri, info) ->
            json.put(
                savedUri,
                JSONObject()
                    .put("title", info.title)
                    .put("artist", info.artist)
            )
        }

        prefs.edit()
            .putStringSet("song_uris", updated.toSet())
            .putString("song_details", json.toString())
            .apply()

        _songDetails.value = details.toMap()
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
