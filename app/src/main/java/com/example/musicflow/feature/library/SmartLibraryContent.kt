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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.smart.SmartLibraryRules
import com.example.musicflow.core.smart.SmartPlaylistKind
import com.example.musicflow.core.smart.SmartSong
import com.example.musicflow.feature.search.SongSearch
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import com.example.musicflow.ui.components.OceanFlowCard
import com.example.musicflow.ui.components.OceanFlowSectionTitle

/** Dynamic collections from readable MediaStore and explicitly picked local documents. */
@Composable
fun SmartLibraryContent(
    songs: List<DeviceSong>,
    favoriteSongUris: Set<String>,
    recentSongUris: List<String>,
    onSongSelected: (List<DeviceSong>, Int) -> Unit,
    onAddToPlaylist: (DeviceSong) -> Unit,
    onToggleFavorite: (DeviceSong) -> Unit,
    onQueueNext: ((DeviceSong) -> Unit)? = null,
    onQueueEnd: ((DeviceSong) -> Unit)? = null,
    onBrowseSongs: (() -> Unit)? = null,
    browseActionLabel: String = "Xem mục Bài hát"
) {
    var selectedKindName by rememberSaveable { mutableStateOf(SmartPlaylistKind.MIX.name) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
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
    // Playback must use the same filtered order that the listener can see.
    // Keep the original Smart ordering (recent/favorites/newest) unchanged.
    val visible = selected.songKeys.mapNotNull(byKey::get).filter { song ->
        searchQuery.isBlank() || SongSearch.matches(song.title, song.artist, searchQuery)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("smart-library-content"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OceanFlowCard {
                OceanFlowSectionTitle("Playlist thông minh")
                Text(
                    "Tự cập nhật theo nhạc đã quét, tệp đã chọn, yêu thích và lịch sử nghe. " +
                        "Không tạo bản sao playlist hay thay đổi tệp nhạc.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Chỉ tính bài offline còn truy cập được; " +
                    "tệp chọn thủ công cần quyền đọc đã lưu.",
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
                        onClick = {
                            if (selectedKindName != playlist.kind.name) {
                                selectedKindName = playlist.kind.name
                                searchQuery = ""
                            }
                        },
                        label = { Text("${playlist.kind.title} · ${playlist.songKeys.size}") },
                        modifier = Modifier.testTag(
                            "smart-playlist-${playlist.kind.name.lowercase()}"
                        )
                    )
                }
            }
            Text("Vuốt ngang để xem các bộ sưu tập nhạc khác.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("smart-collections-scroll-hint"))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(selected.kind.title, style = MaterialTheme.typography.titleLarge)
                Text(selected.kind.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(80) },
                    label = { Text("Tìm trong ${selected.kind.title}") },
                    placeholder = { Text("Tên bài hoặc nghệ sĩ, có thể không dấu") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("smart-search-clear")
                                    .semantics { contentDescription = "Xóa từ khóa tìm kiếm" }) {
                                Text("✕")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("smart-search-field")
                )
                Text(
                    "Hiển thị ${visible.size} / ${selected.songKeys.size} bài",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    title = if (searchQuery.isNotBlank()) "Không tìm thấy bài trong bộ sưu tập"
                        else "Chưa có bài phù hợp",
                    description = if (searchQuery.isNotBlank()) {
                        "Thử từ khóa ngắn hơn hoặc xóa bộ lọc. Danh sách gốc vẫn được giữ nguyên."
                    } else when (selected.kind) {
                        SmartPlaylistKind.FAVORITES ->
                            "Hãy yêu thích một bài trong Thư viện để danh sách tự cập nhật."
                        SmartPlaylistKind.RECENT ->
                            "Phát một bài đã quét để danh sách nghe gần đây xuất hiện."
                        SmartPlaylistKind.NEWEST ->
                            "Thiết bị chưa có bài với thời điểm thêm hợp lệ."
                        else -> "Quét thêm nhạc offline hoặc chọn bộ sưu tập khác."
                    },
                    actionLabel = when {
                        searchQuery.isNotBlank() -> "Xóa bộ lọc"
                        onBrowseSongs != null -> browseActionLabel
                        else -> null
                    },
                    onAction = if (searchQuery.isNotBlank()) ({ searchQuery = "" })
                        else onBrowseSongs
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
