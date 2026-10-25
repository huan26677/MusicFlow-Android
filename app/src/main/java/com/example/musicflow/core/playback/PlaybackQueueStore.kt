package com.example.musicflow.core.playback

import android.content.Context
import android.net.Uri
import android.util.Log

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player

import org.json.JSONArray
import org.json.JSONObject

class PlaybackQueueStore(
    context: Context,
    preferencesName: String = "musicflow_playback_queue_v1"
) {
    private val preferences = context.applicationContext
        .getSharedPreferences(
            preferencesName,
            Context.MODE_PRIVATE
        )

    fun saveFromPlayer(player: Player) {
        val count = player.mediaItemCount

        if (count == 0) {
            clear()
            return
        }

        val tracks = JSONArray()

        for (index in 0 until count) {
            val item = player.getMediaItemAt(index)

            val uri = item.localConfiguration
                ?.uri
                ?.toString()
                ?.takeIf { it.isNotBlank() }

            if (uri == null) {
                Log.w(
                    "MusicFlowQueue",
                    "Queue contains an item without a URI"
                )
                clear()
                return
            }

            tracks.put(
                JSONObject()
                    .put("mediaId", item.mediaId)
                    .put("uri", uri)
                    .put(
                        "title",
                        item.mediaMetadata.title
                            ?.toString().orEmpty()
                    )
                    .put(
                        "artist",
                        item.mediaMetadata.artist
                            ?.toString().orEmpty()
                    )
            )
        }

        val savedPosition = if (
            player.playbackState == Player.STATE_ENDED
        ) {
            0L
        } else {
            player.currentPosition.coerceAtLeast(0L)
        }

        val snapshot = JSONObject()
            .put("version", 1)
            .put(
                "index",
                player.currentMediaItemIndex
                    .coerceIn(0, count - 1)
            )
            .put("positionMs", savedPosition)
            .put(
                "shuffle",
                player.shuffleModeEnabled
            )
            .put("repeat", player.repeatMode)
            .put("items", tracks)

        preferences.edit()
            .putString("snapshot", snapshot.toString())
            .apply()
    }

    fun restoreInto(player: Player): Boolean {
        if (player.mediaItemCount != 0) return false

        val raw = preferences.getString(
            "snapshot",
            null
        ) ?: return false

        return try {
            val snapshot = JSONObject(raw)

            require(snapshot.getInt("version") == 1)

            val tracks = snapshot.getJSONArray("items")
            require(tracks.length() > 0)

            val index = snapshot.getInt("index")
            require(index in 0 until tracks.length())

            val items = (0 until tracks.length()).map {
                val saved = tracks.getJSONObject(it)

                val uri = saved.getString("uri")
                require(uri.isNotBlank())

                MediaItem.Builder()
                    .setMediaId(
                        saved.getString("mediaId")
                    )
                    .setUri(Uri.parse(uri))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(
                                saved.optString("title")
                            )
                            .setArtist(
                                saved.optString("artist")
                            )
                            .build()
                    )
                    .build()
            }

            val positionMs = snapshot
                .getLong("positionMs")
                .coerceAtLeast(0L)

            val repeatMode = snapshot
                .getInt("repeat")
                .takeIf {
                    it == Player.REPEAT_MODE_OFF ||
                    it == Player.REPEAT_MODE_ONE ||
                    it == Player.REPEAT_MODE_ALL
                } ?: Player.REPEAT_MODE_OFF

            player.pause()

            player.setMediaItems(
                items,
                index,
                positionMs
            )

            player.setShuffleModeEnabled(
                snapshot.optBoolean("shuffle", false)
            )

            player.setRepeatMode(repeatMode)

            Log.i(
                "MusicFlowQueue",
                "RESTORED count=${items.size} " +
                    "index=$index positionMs=$positionMs"
            )

            true
        } catch (e: Exception) {
            Log.w(
                "MusicFlowQueue",
                "Cannot restore saved queue",
                e
            )
            false
        }
    }

    fun clear() {
        preferences.edit()
            .remove("snapshot")
            .apply()
    }
}
