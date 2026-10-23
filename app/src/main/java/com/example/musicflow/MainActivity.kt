
package com.example.musicflow

import com.example.musicflow.ui.theme.MusicFlowTheme
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var playerViewModel: PlayerViewModel

    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->

        if (uris.isNotEmpty()) {

            val selectedUris = uris.distinct()

            selectedUris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Vẫn có thể thử phát trong phiên hiện tại.
                }
            }

            val files = selectedUris.map { uri ->
                uri to getFileName(uri)
            }

            playerViewModel.playPickedFiles(files)
        }
    }

    private fun getFileName(uri: Uri): String {
        return try {
            var name: String? = null

            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                }
            }

            name ?: "Bài hát không tên"

        } catch (_: Exception) {
            "Bài hát không tên"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerViewModel = ViewModelProvider(
            this,
            PlayerViewModel.factory(applicationContext)
        )[PlayerViewModel::class.java]

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    playerViewModel.refreshProgress()
                    delay(500)
                }
            }
        }

        setContent {
            val playerState by playerViewModel.uiState
                .collectAsStateWithLifecycle()

            MusicFlowTheme {
                MusicFlowApp(
                    state = playerState,

                    onPickFiles = {
                        pickAudio.launch(arrayOf("audio/*"))
                    },

                    onPlayDeviceSongs = { songs, index ->
                        playerViewModel.playDeviceSongs(
                            songs,
                            index
                        )
                    },

                    onPlayPause = {
                        playerViewModel.togglePlayPause()
                    },

                    onPrevious = {
                        playerViewModel.previous()
                    },

                    onNext = {
                        playerViewModel.next()
                    },

                    onSeek = { position ->
                        playerViewModel.seekTo(position)
                    }
                )
            }
        }
    }
}

@Composable
fun PlayerTestScreen(
    state: PlayerUiState,
    onPickFiles: () -> Unit,
    onOpenLibrary: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember {
        mutableStateOf(false)
    }

    var sliderValue by remember {
        mutableFloatStateOf(0f)
    }

    val maximum = state.durationMs
        .coerceAtLeast(1L)
        .toFloat()

    val displayedPosition = if (isDragging) {
        sliderValue
    } else {
        state.positionMs.toFloat()
    }.coerceIn(0f, maximum)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            "MusicFlow",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(12.dp))

        Text(state.status)

        Spacer(Modifier.height(24.dp))

        Text(
            state.title,
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Bài ${state.currentIndex} / ${state.queueCount}"
        )

        Spacer(Modifier.height(28.dp))

        Button(
            enabled = state.connected,
            onClick = onPickFiles
        ) {
            Text("Chọn nhiều file MP3")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = state.connected,
            onClick = onOpenLibrary
        ) {
            Text("Quét và xem nhạc thiết bị")
        }

        Spacer(Modifier.height(24.dp))

        Slider(
            value = displayedPosition,
            onValueChange = { value ->
                isDragging = true
                sliderValue = value
            },
            onValueChangeFinished = {
                onSeek(sliderValue.toLong())
                isDragging = false
            },
            valueRange = 0f..maximum,
            enabled = state.hasMedia && state.durationMs > 0L
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatMusicTime(displayedPosition.toLong()))

            Text(
                if (state.durationMs > 0L) {
                    formatMusicTime(state.durationMs)
                } else {
                    "--:--"
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Button(
                enabled = state.canPrevious,
                onClick = onPrevious
            ) {
                Text("Trước")
            }

            Button(
                enabled = state.hasMedia,
                onClick = onPlayPause
            ) {
                Text(
                    if (state.isPlaying) {
                        "Tạm dừng"
                    } else {
                        "Phát"
                    }
                )
            }

            Button(
                enabled = state.canNext,
                onClick = onNext
            ) {
                Text("Tiếp")
            }
        }
    }
}

private fun formatMusicTime(timeMs: Long): String {
    val totalSeconds = timeMs.coerceAtLeast(0L) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
