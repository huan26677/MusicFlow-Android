package com.example.musicflow.feature.playlist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.OpenableColumns
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.MediaStoreLocalMusicRepository
import com.example.musicflow.core.playlist.PlaylistQuickAddRules
import com.example.musicflow.feature.library.LibraryUiState
import com.example.musicflow.feature.library.LibraryViewModel
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel

/** One route per selected playlist. No playback and no intermediate playlist picker. */
@Composable
fun PlaylistQuickAddRoute(playlistId: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val playlistModel = rememberPlaylistViewModel()
    val playlistState by playlistModel.playlistsState.collectAsStateWithLifecycle()
    val trackFlow = remember(playlistModel, playlistId) {
        playlistModel.tracksState(playlistId)
    }
    val trackState by trackFlow.collectAsStateWithLifecycle(
        initialValue = PlaylistTracksUiState.Loading
    )
    val addState by playlistModel.addTrackState.collectAsStateWithLifecycle()

    val musicRepository = remember(context) {
        MediaStoreLocalMusicRepository(context.applicationContext)
    }
    val musicModel: LibraryViewModel = viewModel(
        factory = remember(musicRepository) { LibraryViewModel.factory(musicRepository) }
    )
    val libraryState by musicModel.uiState.collectAsStateWithLifecycle()

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else Manifest.permission.READ_EXTERNAL_STORAGE
    var granted by remember(context, permission) {
        mutableStateOf(ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED)
    }
    var fileError by rememberSaveable(playlistId) { mutableStateOf<String?>(null) }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(granted) {
        if (granted) musicModel.refresh()
    }
    LaunchedEffect(playlistId) { playlistModel.resetAddTrackState() }

    val playlistName = (playlistState as? PlaylistsUiState.Ready)
        ?.playlists?.firstOrNull { it.id == playlistId }?.name

    val pickAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = playlistName
            if (name == null) {
                fileError = "Playlist đã thay đổi. Quay lại danh sách để kiểm tra."
            } else if (uri.scheme != "content") {
                fileError = "Tệp đã chọn không có đường dẫn có thể lưu lâu dài."
            } else {
                // SAF documents are accessible after app restart only with a persisted grant.
                // Do not store a dangling playlist item when that grant cannot be saved.
                val persisted = runCatching {
                    resolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    true
                }.getOrElse {
                    resolver.persistedUriPermissions.any {
                        it.uri == uri && it.isReadPermission
                    }
                }
                if (!persisted) {
                    fileError = "Không lưu được quyền đọc tệp. Hãy chọn lại từ bộ chọn tệp."
                } else {
                    val filename = runCatching {
                        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
                            null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                    }.getOrNull().orEmpty().ifBlank { "Tệp âm thanh đã chọn" }
                    fileError = null
                    playlistModel.addDeviceSong(
                        playlistId, name,
                        DeviceSong(
                            id = -(uri.toString().hashCode().toLong() and 0xffffffffL) - 1L,
                            uri = uri,
                            title = filename.take(180),
                            artist = "Tệp âm thanh của bạn",
                            durationMs = 0L,
                            mediaId = uri.toString()
                        )
                    )
                }
            }
        }
    }

    PlaylistQuickAddScreen(
        playlistName = playlistName,
        playlistLoaded = playlistState !is PlaylistsUiState.Loading,
        trackState = trackState,
        libraryState = libraryState,
        hasScanPermission = granted,
        addState = addState,
        fileError = fileError,
        onDone = onDone,
        onRequestScan = {
            if (granted) musicModel.refresh() else requestPermission.launch(permission)
        },
        onPickFile = { pickAudio.launch(arrayOf("audio/*")) },
        onAddSong = { song ->
            if (playlistName != null) {
                fileError = null
                playlistModel.addDeviceSong(playlistId, playlistName, song)
            }
        }
    )
}

/** State-driven UI so the full journey can be tested without network or MediaStore writes. */
@Composable
internal fun PlaylistQuickAddScreen(
    playlistName: String?,
    playlistLoaded: Boolean,
    trackState: PlaylistTracksUiState,
    libraryState: LibraryUiState,
    hasScanPermission: Boolean,
    addState: AddTrackUiState,
    fileError: String?,
    onDone: () -> Unit,
    onRequestScan: () -> Unit,
    onPickFile: () -> Unit,
    onAddSong: (DeviceSong) -> Unit
) {
    var query by rememberSaveable(playlistName) { mutableStateOf("") }
    val busy = addState is AddTrackUiState.Saving
    val trackIds = (trackState as? PlaylistTracksUiState.Ready)
        ?.tracks?.map { it.mediaId }?.toSet().orEmpty()
    val tracksReady = trackState is PlaylistTracksUiState.Ready

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Thêm bài vào playlist", style = MaterialTheme.typography.titleLarge)
            TextButton(
                onClick = onDone,
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("quick-add-done")
            ) { Text("Xong") }
        }

        when {
            !playlistLoaded -> MusicFlowStatusPanel(
                MusicFlowStatusKind.LOADING, "Đang mở playlist...",
                "MusicFlow đang đọc danh sách phát trên thiết bị."
            )
            playlistName == null -> MusicFlowStatusPanel(
                MusicFlowStatusKind.ERROR, "Playlist không còn tồn tại",
                "Danh sách phát đã bị xóa hoặc không đọc được.",
                actionLabel = "Trở về playlist", onAction = onDone
            )
            else -> {
                Text("Đang thêm vào: $playlistName", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("quick-add-target"))
                Text("Chạm Thêm cạnh bài hát. Bạn có thể thêm nhiều bài liên tiếp, " +
                    "sau đó bấm Xong. Thao tác này không phát nhạc hay di chuyển tệp.",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = onPickFile,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("quick-add-saf-file")
                ) { Text("Chọn tệp âm thanh riêng (không cần quyền quét)") }

                if (fileError != null) {
                    Text(fileError, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("quick-add-file-error"))
                }
                when (addState) {
                    is AddTrackUiState.Success -> Text(
                        "Đã thêm bài vào ${addState.playlistName}.",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("quick-add-success")
                    )
                    is AddTrackUiState.Error -> Text(addState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("quick-add-error"))
                    AddTrackUiState.Saving -> Text("Đang lưu bài vào playlist...")
                    AddTrackUiState.Idle -> Unit
                }

                if (!hasScanPermission) {
                    MusicFlowStatusPanel(
                        MusicFlowStatusKind.PERMISSION, "Tìm nhạc trong thư viện thiết bị",
                        "Chỉ cần cấp quyền nếu muốn duyệt toàn bộ nhạc. " +
                            "Bạn vẫn có thể dùng Chọn tệp âm thanh riêng phía trên.",
                        actionLabel = "Cho phép quét nhạc", onAction = onRequestScan
                    )
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(100) },
                        label = { Text("Tìm bài hát hoặc nghệ sĩ") },
                        placeholder = { Text("Có thể nhập không dấu") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                TextButton(onClick = { query = "" },
                                    modifier = Modifier.testTag("quick-add-clear-search")) {
                                    Text("Xóa")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("quick-add-query")
                    )

                    when (libraryState) {
                        LibraryUiState.Idle -> MusicFlowStatusPanel(
                            MusicFlowStatusKind.INFO, "Chưa quét nhạc",
                            "Quét thư viện để tìm bài, hoặc chọn tệp riêng ở trên.",
                            actionLabel = "Quét thư viện", onAction = onRequestScan
                        )
                        LibraryUiState.Loading -> MusicFlowStatusPanel(
                            MusicFlowStatusKind.LOADING, "Đang tìm nhạc trên thiết bị...",
                            "Không thay đổi playlist trong lúc quét."
                        )
                        LibraryUiState.Empty -> MusicFlowStatusPanel(
                            MusicFlowStatusKind.EMPTY, "Không có nhạc đã quét",
                            "Chọn tệp âm thanh riêng, hoặc quét lại thư viện.",
                            actionLabel = "Quét lại", onAction = onRequestScan
                        )
                        is LibraryUiState.Error -> MusicFlowStatusPanel(
                            MusicFlowStatusKind.ERROR, "Không đọc được nhạc",
                            libraryState.message,
                            actionLabel = "Thử lại", onAction = onRequestScan
                        )
                        is LibraryUiState.Success -> {
                            if (!tracksReady) {
                                MusicFlowStatusPanel(
                                    MusicFlowStatusKind.LOADING, "Đang kiểm tra bài đã có...",
                                    "Đảm bảo không thêm trùng vào playlist."
                                )
                            } else {
                                val visible = remember(libraryState.songs, query) {
                                    libraryState.songs.filter {
                                        PlaylistQuickAddRules.match(it.title, it.artist, query)
                                    }
                                }
                                Text("${visible.size} bài phù hợp · ${trackIds.size} bài đã có",
                                    style = MaterialTheme.typography.bodySmall)
                                if (visible.isEmpty()) {
                                    MusicFlowStatusPanel(
                                        MusicFlowStatusKind.EMPTY, "Không có bài phù hợp",
                                        "Thử từ khóa ngắn hơn, không dấu hoặc xóa từ khóa.",
                                        actionLabel = "Xóa tìm kiếm", onAction = { query = "" }
                                    )
                                } else {
                                    HorizontalDivider()
                                    LazyColumn(
                                        modifier = Modifier.weight(1f)
                                            .testTag("quick-add-song-list"),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(visible, key = { it.uri.toString() }) { song ->
                                            val key = PlaylistQuickAddRules.mediaId(song.mediaId, song.id)
                                            val exists = key in trackIds
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .heightIn(min = 64.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(song.title, maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis)
                                                    Text(song.artist, style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis)
                                                }
                                                Button(
                                                    enabled = PlaylistQuickAddRules.canAdd(
                                                        song.mediaId, song.id, trackIds, busy
                                                    ),
                                                    onClick = { onAddSong(song) },
                                                    modifier = Modifier.heightIn(min = 48.dp)
                                                        .testTag("quick-add-item-$key")
                                                ) { Text(if (exists) "Đã có" else "Thêm") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
