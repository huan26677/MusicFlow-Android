package com.example.musicflow.feature.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.material3.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import com.example.musicflow.core.online.OnlineTrack
import com.example.musicflow.core.online.OnlineRightsPolicy
import com.example.musicflow.core.media.TrackMediaItems
import com.example.musicflow.ui.components.MusicFlowStatusKind
import com.example.musicflow.ui.components.MusicFlowStatusPanel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicflow.BuildConfig

@Composable
fun OnlineCatalogRoute(
    onBack: () -> Unit,
    onPlayTrack: (List<OnlineTrack>, Int) -> Unit
) {
    val context = LocalContext.current
    val factory = remember(context) {
        OnlineCatalogViewModel.factory(context.applicationContext)
    }
    val model: OnlineCatalogViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()

    OnlineCatalogScreen(
        state = state,
        onBack = onBack,
        onEndpointChange = model::updateEndpointInput,
        onSaveEndpoint = model::saveAndReload,
        onClearEndpoint = model::clearEndpoint,
        onRefresh = model::refresh,
        onLoadMore = model::loadMore
        ,onPlayTrack = onPlayTrack,
        isSupabaseManaged = BuildConfig.SUPABASE_URL.isNotBlank()
    )
}

@Composable
fun OnlineCatalogScreen(
    state: OnlineCatalogUiState,
    onBack: () -> Unit,
    onEndpointChange: (String) -> Unit,
    onSaveEndpoint: () -> Unit,
    onClearEndpoint: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onPlayTrack: (List<OnlineTrack>, Int) -> Unit,
    isSupabaseManaged: Boolean = false
) {
    var rightsDetail by remember { mutableStateOf<OnlineTrack?>(null) }
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Khám phá online",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onBack) { Text("Quay lại") }
        }

        Text(
            "Kết nối API danh mục nhạc do bạn quản lý hoặc được cấp phép. " +
                "Chỉ phát các bài có URL HTTPS, thông tin quyền phát và ghi công " +
                "được nhà cung cấp xác nhận. Không tự cho phép tải nhạc.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = state.endpointInput,
            onValueChange = onEndpointChange,
            label = { Text("Địa chỉ gốc API (HTTPS)") },
            placeholder = { Text("https://music.example.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            readOnly = isSupabaseManaged
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isSupabaseManaged) {
                Button(
                    onClick = onSaveEndpoint,
                    enabled = !state.isLoading && !state.isLoadingMore &&
                        state.endpointInput.isNotBlank()
                ) {
                    Text("Lưu và kết nối")
                }
            }
            OutlinedButton(
                onClick = onRefresh,
                enabled = state.savedEndpoint.isNotEmpty() &&
                    !state.isLoading && !state.isLoadingMore
            ) {
                Text("Tải lại")
            }
            if (!isSupabaseManaged && state.savedEndpoint.isNotEmpty()) {
                TextButton(onClick = onClearEndpoint) {
                    Text("Ngắt")
                }
            }
        }

        HorizontalDivider()

        when {
            state.savedEndpoint.isBlank() -> {
                MusicFlowStatusPanel(
                    kind = if (state.error != null) MusicFlowStatusKind.ERROR
                        else MusicFlowStatusKind.INFO,
                    title = if (state.error != null) "Cấu hình nguồn nhạc chưa hợp lệ"
                        else "Chưa kết nối nguồn nhạc online",
                    description = state.error ?: "Nhập URL HTTPS của máy chủ có quyền " +
                        "cung cấp nội dung và lưu kết nối để tải danh mục.",
                    actionLabel = if (state.error == null && !isSupabaseManaged)
                        "Kết nối" else null,
                    onAction = if (state.error == null && !isSupabaseManaged &&
                        state.endpointInput.isNotBlank()) onSaveEndpoint else null
                )
            }
            state.isLoading -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.LOADING,
                    title = "Đang tải danh mục từ máy chủ...",
                    description = "MusicFlow đang kết nối nguồn nhạc được cấu hình."
                )
            }
            else -> {
                if (state.error != null) {
                    MusicFlowStatusPanel(
                        kind = MusicFlowStatusKind.ERROR,
                        title = "Không thể tải danh mục online",
                        description = state.error,
                        actionLabel = "Thử lại",
                        onAction = onRefresh
                    )
                }
                if (state.isLoaded && state.tracks.isEmpty() &&
                    state.error == null
                ) {
                    MusicFlowStatusPanel(
                        kind = MusicFlowStatusKind.EMPTY,
                        title = "Chưa có bài hát online",
                        description = "Máy chủ chưa trả về bài hát nào có thể hiển thị. " +
                            "Danh mục sẽ xuất hiện khi có nội dung hợp lệ. " +
                            "Sử dụng nút Tải lại ở phía trên để kiểm tra lần nữa."
                    )
                }

                if (state.tracks.isNotEmpty()) {
                    Text(
                        "${state.tracks.size} bài hát đã tải",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.tracks.size) { index ->
                            val track = state.tracks[index]
                            val canPlay = track.id.isNotBlank() &&
                                track.streamUrl?.let(TrackMediaItems::isSafeHttpsMediaUrl) == true &&
                                OnlineRightsPolicy.canStream(track.rights)
                            ListItem(
                                modifier = Modifier.clickable(
                                    enabled = canPlay
                                ) { onPlayTrack(state.tracks, index) },
                                headlineContent = {
                                    Text(
                                        track.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        Text(
                                            buildString {
                                                append(track.artist)
                                                track.album?.let {
                                                    append(" · ")
                                                    append(it)
                                                }
                                            },
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            if (canPlay) "Có quyền phát trực tuyến"
                                            else OnlineRightsPolicy.statusLabel(
                                                OnlineRightsPolicy.streamingStatus(track.rights)
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (canPlay) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = { rightsDetail = track },
                                            modifier = Modifier.testTag("track-rights-${track.id}")
                                        ) { Text("Nguồn và giấy phép") }
                                    }
                                },
                                trailingContent = {
                                    if (canPlay) {
                                        Text("▶")
                                    } else if (track.durationMs > 0L) {
                                        val minutes = track.durationMs / 60_000
                                        val seconds = (track.durationMs / 1000) % 60
                                        Text("%d:%02d".format(minutes, seconds))
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                        if (state.nextCursor != null) {
                            item(key = "load-more") {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(12.dp)
                                    )
                                } else {
                                    TextButton(onClick = onLoadMore) {
                                        Text("Tải thêm")
                                    }
                                }
                            }
                        }
                    }
                } else if (!state.isLoaded && state.error == null) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    rightsDetail?.let { track ->
        val rights = track.rights
        AlertDialog(
            onDismissRequest = { rightsDetail = null },
            title = { Text("Nguồn và giấy phép") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium)
                    Text("Nghệ sĩ: ${track.artist}")
                    Text("Giấy phép: ${rights?.licenseCode ?: "Chưa khai báo"}")
                    Text("Ghi công: ${rights?.attribution ?: "Chưa có thông tin"}")
                    Text(OnlineRightsPolicy.statusLabel(
                        OnlineRightsPolicy.streamingStatus(rights)
                    ))
                    Text(
                        if (rights?.downloadAllowed == true &&
                            OnlineRightsPolicy.canStream(rights)) {
                            "Nguồn khai báo cho phép tải; MusicFlow chưa hỗ trợ tải nhạc online."
                        } else "Không cho phép tải xuống trong MusicFlow."
                    )
                    rights?.sourceUrl?.takeIf(OnlineRightsPolicy::isSafeHttpsLink)?.let { url ->
                        TextButton(onClick = { uriHandler.openUri(url) }) {
                            Text("Mở nguồn gốc bài hát")
                        }
                    }
                    rights?.licenseUrl?.takeIf(OnlineRightsPolicy::isSafeHttpsLink)?.let { url ->
                        TextButton(onClick = { uriHandler.openUri(url) }) {
                            Text("Đọc giấy phép")
                        }
                    }
                    Text("Thông tin do nhà cung cấp khai báo; không thay thế việc " +
                        "kiểm tra quyền sử dụng với chủ sở hữu.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { rightsDetail = null }) { Text("Đóng") }
            }
        )
    }
}
