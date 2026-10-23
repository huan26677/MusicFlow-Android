
package com.example.musicflow.feature.search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.MediaStoreLocalMusicRepository
import com.example.musicflow.feature.library.SongRow

@Composable
fun SearchRoute(
    onSongSelected: (List<DeviceSong>, Int) -> Unit
) {
    val context = LocalContext.current

    val repository = remember(context) {
        MediaStoreLocalMusicRepository(
            context.applicationContext
        )
    }

    val factory = remember(repository) {
        SearchViewModel.factory(repository)
    }

    val searchViewModel: SearchViewModel =
        viewModel(factory = factory)

    val state by searchViewModel.uiState
        .collectAsStateWithLifecycle()

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember(context, permission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            searchViewModel.refresh()
        } else {
            searchViewModel.clearSongs()
        }
    }

    SearchScreen(
        state = state,
        hasPermission = hasPermission,
        onQueryChange = searchViewModel::updateQuery,
        onRequestPermission = {
            permissionLauncher.launch(permission)
        },
        onRefresh = {
            if (hasPermission) {
                searchViewModel.refresh()
            } else {
                permissionLauncher.launch(permission)
            }
        },
        onSongSelected = onSongSelected
    )
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    hasPermission: Boolean,
    onQueryChange: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tìm kiếm",
                style = MaterialTheme.typography.headlineSmall
            )

            if (hasPermission) {
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.isLoading
                ) {
                    Text("Quét lại")
                }
            }
        }

        if (!hasPermission) {
            Text(
                text = "MusicFlow cần quyền đọc âm thanh " +
                        "để tìm bài hát trên thiết bị."
            )

            Button(onClick = onRequestPermission) {
                Text("Cấp quyền")
            }

            Text(
                text = "Nếu đã từ chối nhiều lần, " +
                        "hãy kiểm tra quyền ứng dụng trong Cài đặt.",
                style = MaterialTheme.typography.bodySmall
            )

            return@Column
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tên bài hát hoặc nghệ sĩ") },
            placeholder = { Text("Ví dụ: duong ve nha") },
            singleLine = true,
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") }
                    ) {
                        Text("×")
                    }
                }
            }
        )

        when {
            state.isLoading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Đang quét nhạc trên thiết bị...")
            }

            state.error != null -> {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error
                )

                Button(onClick = onRefresh) {
                    Text("Thử lại")
                }
            }

            !state.isLoaded -> {
                Text("Chưa tải thư viện nhạc.")
            }

            state.songs.isEmpty() -> {
                Text("Không tìm thấy bài hát nào trên thiết bị.")
            }

            state.query.isBlank() -> {
                Text(
                    text = "Nhập tên bài hát hoặc nghệ sĩ để tìm kiếm.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            state.results.isEmpty() -> {
                Text("Không tìm thấy bài hát phù hợp.")
            }

            else -> {
                Text(
                    text = "${state.results.size} kết quả",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = state.results,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        SongRow(
                            song = song,
                            onClick = {
                                onSongSelected(
                                    state.results,
                                    index
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
