package com.example.musicflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun LibraryRoute(
    onBack: () -> Unit,
    onPickManual: () -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit
) {
    val context = LocalContext.current

    val repository = remember {
        MediaStoreLocalMusicRepository(context.applicationContext)
    }

    val factory = remember(repository) {
        LibraryViewModel.factory(repository)
    }

    val libraryViewModel: LibraryViewModel =
        viewModel(factory = factory)

    val uiState by libraryViewModel.uiState
        .collectAsStateWithLifecycle()

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
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
        val granted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            libraryViewModel.refresh()
        }
    }

    LibraryScreen(
        uiState = uiState,
        hasPermission = hasPermission,
        onBack = onBack,
        onPickManual = onPickManual,
        onRefresh = {
            if (hasPermission) {
                libraryViewModel.refresh()
            } else {
                permissionLauncher.launch(permission)
            }
        },
        onSongSelected = onSongSelected
    )
}

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    hasPermission: Boolean,
    onBack: () -> Unit,
    onPickManual: () -> Unit,
    onRefresh: () -> Unit,
    onSongSelected: (List<DeviceSong>, Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nhạc thiết bị",
                    style = MaterialTheme.typography.headlineSmall
                )

                TextButton(onClick = onBack) {
                    Text("Quay lại")
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRefresh) {
                    Text("Quét lại")
                }

                Button(onClick = onPickManual) {
                    Text("Chọn MP3")
                }
            }

            if (!hasPermission) {
                Text(
                    "MusicFlow cần quyền đọc âm thanh " +
                            "để quét thư viện thiết bị."
                )

                Button(onClick = onRefresh) {
                    Text("Cấp quyền")
                }

                Text(
                    "Nếu đã từ chối quyền nhiều lần, " +
                            "hãy kiểm tra quyền ứng dụng trong Cài đặt.",
                    style = MaterialTheme.typography.bodySmall
                )

            } else {
                when (uiState) {

                    LibraryUiState.Idle -> {
                        Text("Chưa quét thư viện.")
                    }

                    LibraryUiState.Loading -> {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Đang quét nhạc...")
                    }

                    LibraryUiState.Empty -> {
                        Text(
                            "Không tìm thấy bài hát nào " +
                                    "được Android phân loại là nhạc."
                        )
                    }

                    is LibraryUiState.Error -> {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    is LibraryUiState.Success -> {
                        Text("${uiState.songs.size} bài hát")

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement =
                                Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.songs,
                                key = { _, song -> song.id }
                            ) { index, song ->

                                TextButton(
                                    onClick = {
                                        onSongSelected(
                                            uiState.songs,
                                            index
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SongArtwork(
                                            uri = song.uri,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(song.title)

                                            Text(
                                                text = song.artist,
                                                style = MaterialTheme.typography.bodySmall
                                            )
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