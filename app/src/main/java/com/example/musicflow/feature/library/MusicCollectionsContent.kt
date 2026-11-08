package com.example.musicflow.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
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

/**
 * Each expanded track is an independent lazy item. Before v7 all tracks in a
 * selected album/artist/genre were composed eagerly inside ONE LazyColumn item.
 */
internal sealed interface MusicCollectionRow {
    val key: String

    data class Header(val group: MusicCollection) : MusicCollectionRow {
        override val key: String = "header:${group.key}"
    }

    data class PlayAll(val group: MusicCollection) : MusicCollectionRow {
        override val key: String = "play:${group.key}"
    }

    data class Track(val group: MusicCollection, val index: Int, val song: DeviceSong)
        : MusicCollectionRow {
        // Keep keys unique even when the same URI occurs in multiple genres.
        override val key: String = "track:${group.key}:$index"
    }
}

internal fun collectionRows(
    collections: List<MusicCollection>,
    expandedKey: String?
): List<MusicCollectionRow> = buildList {
    collections.forEach { group ->
        add(MusicCollectionRow.Header(group))
        if (group.key == expandedKey) {
            if (group.tracks.isNotEmpty()) add(MusicCollectionRow.PlayAll(group))
            group.tracks.forEachIndexed { index, song ->
                add(MusicCollectionRow.Track(group, index, song))
            }
        }
    }
}

@Composable
internal fun ColumnScope.MusicCollectionsContent(
    title: String,
    collections: List<MusicCollection>,
    favoriteSongUris: Set<String>,
    onToggleFavorite: (DeviceSong) -> Unit,
    onAddToPlaylist: (DeviceSong) -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    onQueueNext: ((DeviceSong) -> Unit)? = null,
    onQueueEnd: ((DeviceSong) -> Unit)? = null
) {
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val rows = remember(collections, expandedKey) {
        collectionRows(collections, expandedKey)
    }
    Text("${collections.size} $title")

    if (collections.isEmpty()) {
        Spacer(Modifier.height(8.dp))
        MusicFlowStatusPanel(
            kind = MusicFlowStatusKind.EMPTY,
            title = "Chưa có $title",
            description = "Hãy thử từ khóa khác hoặc quét lại thư viện nếu vừa thêm nhạc."
        )
    }

    LazyColumn(modifier = Modifier.weight(1f).testTag("music-collections-list")) {
        items(rows, key = { it.key }) { row ->
            when (row) {
                is MusicCollectionRow.Header -> {
                    val group = row.group
                    val expanded = expandedKey == group.key
                    var keyboardFocused by remember(group.key) { mutableStateOf(false) }
                    ListItem(
                        modifier = Modifier.fillMaxWidth()
                            .testTag("collection-header-${group.key}")
                            .onFocusChanged { keyboardFocused = it.isFocused }
                            .clickable(role = Role.Button) {
                                expandedKey = if (expanded) null else group.key
                            }
                            .border(
                                width = if (keyboardFocused) 3.dp else 0.dp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .semantics {
                                stateDescription = if (expanded) "Đang mở" else "Đang thu gọn"
                            },
                        headlineContent = { Text(group.name) },
                        supportingContent = {
                            Text("${group.tracks.size} bài hát")
                        }
                    )
                }
                is MusicCollectionRow.PlayAll -> {
                    TextButton(
                        onClick = { onSongSelected(row.group.tracks, 0) },
                        modifier = Modifier.heightIn(min = 48.dp)
                            .testTag("collection-play-all")
                    ) { Text("Phát toàn bộ") }
                }
                is MusicCollectionRow.Track -> {
                    val song = row.song
                    SongRow(
                        song = song,
                        onClick = { onSongSelected(row.group.tracks, row.index) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        isFavorite = song.uri.toString() in favoriteSongUris,
                        onToggleFavorite = { onToggleFavorite(song) },
                        onQueueNext = onQueueNext?.let { action -> { action(song) } },
                        onQueueEnd = onQueueEnd?.let { action -> { action(song) } }
                    )
                }
            }
        }
    }
}
