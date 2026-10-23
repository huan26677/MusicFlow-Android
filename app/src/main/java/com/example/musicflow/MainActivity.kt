
package com.example.musicflow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.musicflow.ui.theme.MusicFlowTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    // Vẫn thử phát trong phiên hiện tại.
                }
            }

            lifecycleScope.launch {

                // Truy vấn tên file ngoài luồng giao diện.
                val files = withContext(Dispatchers.IO) {
                    selectedUris.map { uri ->
                        uri to getFileName(uri)
                    }
                }

                // Trở lại Main thread trước khi gọi bộ phát.
                playerViewModel.playPickedFiles(files)
            }
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
                    },

                    onToggleShuffle = {
                        playerViewModel.toggleShuffle()
                    },

                    onCycleRepeat = {
                        playerViewModel.cycleRepeat()
                    }
                )
            }
        }
    }
}

