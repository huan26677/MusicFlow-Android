package com.example.musicflow.feature.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicflow.BuildConfig
import com.example.musicflow.feature.community.CommunityRoute

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
    var communityTrack by remember { mutableStateOf<OnlineTrack?>(null) }
    var communityDemo by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val statusScroll = rememberScrollState()
    val scrollStatus = state.savedEndpoint.isBlank() ||
        (state.tracks.isEmpty() && (state.isLoaded || state.error != null))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (scrollStatus) Modifier.verticalScroll(statusScroll) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
            val title: @Composable () -> Unit = {
                Text("Khám phá online", style = MaterialTheme.typography.headlineSmall)
            }
            val back: @Composable () -> Unit = {
                TextButton(onClick = onBack) { Text("Quay lại") }
            }
            if (compact) {
                Column { title(); back() }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) { title(); back() }
            }
        }

        Text(
            if (isSupabaseManaged) {
                "Khám phá các bài hát đã được xác nhận quyền phát. " +
                    "Danh mục sẽ xuất hiện khi có nhạc phù hợp."
            } else {
                "Chỉ kết nối nguồn nhạc bạn quản lý hoặc được phép sử dụng. " +
                    "MusicFlow không tự tải nhạc online."
            },
            style = MaterialTheme.typography.bodyMedium
        )
        // The configured server URL is not actionable by everyday listeners.
        // Keep manual source configuration intact for coursework/custom catalog.
        if (!isSupabaseManaged) {
            OutlinedTextField(
                value = state.endpointInput,
                onValueChange = onEndpointChange,
                label = { Text("Địa chỉ nguồn nhạc (HTTPS)") },
                placeholder = { Text("https://music.example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("online-manual-endpoint")
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 340.dp || LocalDensity.current.fontScale >= 1.5f
            val actions: @Composable () -> Unit = {
                if (!isSupabaseManaged) {
                    Button(
                        onClick = onSaveEndpoint,
                        enabled = !state.isLoading && !state.isLoadingMore &&
                            state.endpointInput.isNotBlank(),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Lưu và kết nối") }
                }
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = state.savedEndpoint.isNotEmpty() &&
                        !state.isLoading && !state.isLoadingMore,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Tải lại") }
                if (!isSupabaseManaged && state.savedEndpoint.isNotEmpty()) {
                    TextButton(onClick = onClearEndpoint,
                        modifier = Modifier.heightIn(min = 48.dp)) { Text("Ngắt") }
                }
            }
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { actions() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
            }
        }

        HorizontalDivider()

        when {
            state.savedEndpoint.isBlank() -> {
                MusicFlowStatusPanel(
                    kind = if (state.error != null) MusicFlowStatusKind.ERROR
                        else MusicFlowStatusKind.INFO,
                    title = if (state.error != null) "Chưa kết nối được nhạc online"
                        else if (isSupabaseManaged) "Nguồn nhạc online chưa sẵn sàng"
                        else "Chưa kết nối nguồn nhạc online",
                    description = if (isSupabaseManaged) {
                        "Danh mục hiện chưa sẵn sàng. Bạn vẫn có thể nghe nhạc " +
                            "trên điện thoại và thử lại sau."
                    } else state.error ?: "Nhập địa chỉ HTTPS của nguồn nhạc " +
                        "được cấp quyền, sau đó chọn Kết nối.",
                    actionLabel = if (state.error == null && !isSupabaseManaged)
                        "Kết nối" else null,
                    onAction = if (state.error == null && !isSupabaseManaged &&
                        state.endpointInput.isNotBlank()) onSaveEndpoint else null
                )
                OutlinedButton(
                    onClick = { communityDemo = true },
                    modifier = Modifier.testTag("community-open-demo")
                ) { Text("Xem thử đánh giá và bình luận (Demo)") }
            }
            state.isLoading -> {
                MusicFlowStatusPanel(
                    kind = MusicFlowStatusKind.LOADING,
                    title = "Đang tải danh mục từ máy chủ...",
                    description = "Danh mục sẽ xuất hiện khi tải xong."
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
                            "Bạn vẫn có thể nghe nhạc offline, hoặc chọn Tải lại."
                    )
                    OutlinedButton(
                        onClick = { communityDemo = true },
                        modifier = Modifier.testTag("community-open-demo")
                    ) { Text("Xem thử đánh giá và bình luận (Demo)") }
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
                                        if (isSupabaseManaged) {
                                            TextButton(
                                                onClick = { communityTrack = track },
                                                modifier = Modifier.testTag("track-community-${track.id}")
                                            ) { Text("Đánh giá & bình luận") }
                                        }
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
    communityTrack?.let { track ->
        CommunityRoute(
            trackId = track.id, title = track.title, demo = false,
            onClose = { communityTrack = null }
        )
    }
    if (communityDemo) {
        CommunityRoute(
            trackId = "demo-community",title = "Bài hát mẫu MusicFlow",
            demo = true,onClose = { communityDemo = false }
        )
    }
}
