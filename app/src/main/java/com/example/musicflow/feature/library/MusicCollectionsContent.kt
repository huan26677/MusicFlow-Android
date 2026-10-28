package com.example.musicflow.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.media.DeviceSong
import java.util.Locale

data class MusicCollection(
    val key: String,
    val name: String,
    val tracks: List<DeviceSong>
)

internal object MusicCollections {
    fun albums(songs: List<DeviceSong>): List<MusicCollection> =
        songs.groupBy { song ->
            song.albumId?.let { "album-id:$it" }
                ?: "album-name:${song.album.trim().lowercase(Locale.ROOT)}:" +
                    song.artist.trim().lowercase(Locale.ROOT)
        }.map { (key, tracks) ->
            MusicCollection(
                key = key,
                name = tracks.first().album,
                tracks = tracks
            )
        }.sortedWith(compareBy({ it.name.lowercase(Locale.ROOT) }, { it.key }))

    fun artists(songs: List<DeviceSong>): List<MusicCollection> =
        songs.groupBy { song ->
            song.artistId?.let { "artist-id:$it" }
                ?: "artist-name:${song.artist.trim().lowercase(Locale.ROOT)}"
        }.map { (key, tracks) ->
            MusicCollection(
                key = key,
                name = tracks.first().artist,
                tracks = tracks
            )
        }.sortedWith(compareBy({ it.name.lowercase(Locale.ROOT) }, { it.key }))

    fun genres(songs: List<DeviceSong>): List<MusicCollection> {
        val groups = linkedMapOf<String, MutableList<DeviceSong>>()
        val labels = linkedMapOf<String, String>()

        songs.forEach { song ->
            val names = song.genres
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf("Không rõ thể loại") }
            names
                .distinctBy { it.lowercase(Locale.ROOT) }
                .forEach { name ->
                    val key = "genre:${name.lowercase(Locale.ROOT)}"
                    labels.putIfAbsent(key, name)
                    groups.getOrPut(key) { mutableListOf() }.add(song)
                }
        }

        return groups.map { (key, tracks) ->
            MusicCollection(key, labels.getValue(key), tracks)
        }.sortedWith(compareBy({ it.name.lowercase(Locale.ROOT) }, { it.key }))
    }
}

@Composable
internal fun ColumnScope.MusicCollectionsContent(
    title: String,
    collections: List<MusicCollection>,
    favoriteSongUris: Set<String>,
    onToggleFavorite: (DeviceSong) -> Unit,
    onAddToPlaylist: (DeviceSong) -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit
) {
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    Text("${collections.size} $title")

    if (collections.isEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Chưa có $title. Hãy quét lại thư viện nếu vừa thêm nhạc.",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    LazyColumn(modifier = Modifier.weight(1f)) {
        items(collections, key = { it.key }) { group ->
            Column {
                ListItem(
                    modifier = Modifier.fillMaxWidth().clickable {
                        expandedKey = if (expandedKey == group.key) {
                            null
                        } else {
                            group.key
                        }
                    },
                    headlineContent = { Text(group.name) },
                    supportingContent = {
                        Text("${group.tracks.size} bài hát")
                    }
                )
                if (expandedKey == group.key) {
                    TextButton(
                        onClick = { onSongSelected(group.tracks, 0) },
                        enabled = group.tracks.isNotEmpty()
                    ) {
                        Text("Phát toàn bộ")
                    }
                    group.tracks.forEachIndexed { index, song ->
                        SongRow(
                            song = song,
                            onClick = {
                                onSongSelected(group.tracks, index)
                            },
                            onAddToPlaylist = {
                                onAddToPlaylist(song)
                            },
                            isFavorite = song.uri.toString() in favoriteSongUris,
                            onToggleFavorite = {
                                onToggleFavorite(song)
                            }
                        )
                    }
                }
            }
        }
    }
}
