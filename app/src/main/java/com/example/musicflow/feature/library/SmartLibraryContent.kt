package com.example.musicflow.feature.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.smart.SmartLibraryRules
import com.example.musicflow.core.smart.SmartPlaylistKind
import com.example.musicflow.core.smart.SmartSong
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import com.example.musicflow.ui.components.OceanFlowCard
import com.example.musicflow.ui.components.OceanFlowSectionTitle

/** Dynamic collections from currently visible offline MediaStore tracks only. */
@Composable
fun SmartLibraryContent(
    songs: List<DeviceSong>,
    favoriteSongUris: Set<String>,
    recentSongUris: List<String>,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    onAddToPlaylist: (DeviceSong) -> Unit,
    onToggleFavorite: (DeviceSong) -> Unit,
    onQueueNext: ((DeviceSong) -> Unit)? = null,
    onQueueEnd: ((DeviceSong) -> Unit)? = null
) {
    var selectedKindName by rememberSaveable { mutableStateOf(SmartPlaylistKind.MIX.name) }
    val snapshots = remember(songs, favoriteSongUris, recentSongUris) {
        SmartLibraryRules.build(
            scanned = songs.map {
                SmartSong(it.uri.toString(), it.title, it.dateAddedSeconds, it.durationMs)
            },
            favoriteKeys = favoriteSongUris,
            recentKeys = recentSongUris
        )
    }
    val byKey = remember(songs) { songs.associateBy { it.uri.toString() } }
    val selected = snapshots.firstOrNull { it.kind.name == selectedKindName }
        ?: snapshots.first()
    val visible = selected.songKeys.mapNotNull(byKey::get)

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("smart-library-content"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OceanFlowCard {
                OceanFlowSectionTitle("Playlist thông minh")
                Text(
                    "Tự cập nhật theo nhạc offline đã quét, yêu thích và lịch sử nghe. " +
                        "Không tạo bản sao playlist hay thay đổi tệp nhạc.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Chỉ tính những bài còn có trong thư viện đang quét; " +
                    "các tệp SAF ngoài MediaStore có thể không xuất hiện.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                snapshots.forEach { playlist ->
                    FilterChip(
                        selected = playlist.kind.name == selectedKindName,
                        onClick = { selectedKindName = playlist.kind.name },
                        label = { Text("${playlist.kind.title} · ${playlist.songKeys.size}") },
                        modifier = Modifier.testTag(
                            "smart-playlist-${playlist.kind.name.lowercase()}"
                        )
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(selected.kind.title, style = MaterialTheme.typography.titleLarge)
                Text(selected.kind.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (visible.isNotEmpty()) {
                    Button(
                        onClick = { onSongSelected(visible, 0) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("smart-play-all")
                    ) { Text("▶ Phát ${visible.size} bài") }
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.EMPTY,
                    title = "Chưa có bài phù hợp",
                    description = when (selected.kind) {
                        SmartPlaylistKind.FAVORITES ->
                            "Hãy yêu thích một bài trong Thư viện để danh sách tự cập nhật."
                        SmartPlaylistKind.RECENT ->
                            "Phát một bài đã quét để danh sách nghe gần đây xuất hiện."
                        SmartPlaylistKind.NEWEST ->
                            "Thiết bị chưa có bài với thời điểm thêm hợp lệ."
                        else -> "Quét thêm nhạc offline hoặc chọn bộ sưu tập khác."
                    }
                )
            }
        } else {
            itemsIndexed(visible, key = { _, song -> song.uri.toString() }) { index, song ->
                SongRow(
                    song = song,
                    onClick = { onSongSelected(visible, index) },
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
