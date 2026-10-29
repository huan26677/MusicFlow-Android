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
import com.example.musicflow.core.online.OnlineTrack
import com.example.musicflow.core.media.TrackMediaItems
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                "Chỉ có thể chọn phát bài có URL HTTPS hợp lệ; " +
                "quyền phát nhạc phải được xác nhận từ nguồn cung cấp.",
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

        if (state.error != null) {
            Text(
                state.error,
                color = MaterialTheme.colorScheme.error
            )
        }

        HorizontalDivider()

        when {
            state.savedEndpoint.isBlank() -> {
                Text(
                    "Chưa cấu hình nguồn nhạc online. " +
                        "Hãy nhập URL HTTPS của máy chủ có quyền cung cấp nội dung.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            state.isLoading -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Đang tải danh mục từ máy chủ...")
            }
            else -> {
                if (state.error != null) {
                    TextButton(onClick = onRefresh) { Text("Thử lại") }
                }
                if (state.isLoaded && state.tracks.isEmpty() &&
                    state.error == null
                ) {
                    Text("Máy chủ chưa trả về bài hát nào.")
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
                                track.streamUrl?.let(TrackMediaItems::isSafeHttpsMediaUrl) == true
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
}
