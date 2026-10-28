package com.example.musicflow.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicflow.core.catalog.HttpsOnlineCatalogRepository
import com.example.musicflow.core.catalog.OnlineCatalogConfiguration
import com.example.musicflow.core.catalog.OnlineCatalogTrack

@Composable
fun OnlineCatalogRoute(
    onBack: () -> Unit,
    onConfigure: () -> Unit
) {
    val context = LocalContext.current
    val configuration = remember(context) {
        OnlineCatalogConfiguration(context.applicationContext)
    }
    val repository = remember(configuration) {
        HttpsOnlineCatalogRepository(configuration)
    }
    val factory = remember(configuration, repository) {
        OnlineCatalogViewModel.factory(configuration, repository)
    }
    val model: OnlineCatalogViewModel = viewModel(factory = factory)
    val uiState by model.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        model.refresh()
    }

    OnlineCatalogScreen(
        state = uiState,
        onBack = onBack,
        onConfigure = onConfigure,
        onRefresh = model::refresh
    )
}

@Composable
fun OnlineCatalogScreen(
    state: OnlineCatalogUiState,
    onBack: () -> Unit,
    onConfigure: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
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
            OutlinedButton(onClick = onBack) {
                Text("Quay lại")
            }
        }

        Text(
            "Danh mục từ máy chủ HTTPS do bạn cấu hình. " +
                "Nhạc trên thiết bị vẫn hoạt động khi offline.",
            style = MaterialTheme.typography.bodyMedium
        )

        when (state) {
            OnlineCatalogUiState.NotConfigured -> {
                Text("Chưa cấu hình máy chủ nhạc online.")
                Button(onClick = onConfigure) {
                    Text("Cấu hình máy chủ")
                }
            }

            OnlineCatalogUiState.Loading -> {
                CircularProgressIndicator()
                Text("Đang tải danh mục...")
            }

            OnlineCatalogUiState.Empty -> {
                Text("Máy chủ chưa có bài hát nào.")
                Button(onClick = onRefresh) {
                    Text("Tải lại")
                }
            }

            is OnlineCatalogUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRefresh) {
                        Text("Thử lại")
                    }
                    OutlinedButton(onClick = onConfigure) {
                        Text("Cài đặt API")
                    }
                }
            }

            is OnlineCatalogUiState.Loaded -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${state.tracks.size} bài hát online")
                    OutlinedButton(onClick = onRefresh) {
                        Text("Làm mới")
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.tracks, key = { it.id }) { track ->
                        OnlineCatalogTrackCard(track)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineCatalogTrackCard(track: OnlineCatalogTrack) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(track.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${track.artist} · ${track.album}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (track.rightsNotice.isNotBlank()) {
                Text(
                    track.rightsNotice,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                "Phát trực tuyến và tải offline sẽ bổ sung ở giai đoạn tiếp theo.",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
