
package com.example.musicflow

import android.content.Intent
import android.media.MediaMetadataRetriever
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
import com.example.musicflow.feature.player.PickedAudioFile
import com.example.musicflow.feature.player.PlayerViewModel
import com.example.musicflow.core.lyrics.LocalLyricsRepository
import com.example.musicflow.core.lyrics.LyricsUiState
import com.example.musicflow.navigation.MusicFlowApp
import com.example.musicflow.ui.theme.MusicFlowTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.jvm.Volatile
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private lateinit var playerViewModel: PlayerViewModel

private val lyricsRepository by lazy {
    LocalLyricsRepository(applicationContext)
}

private val lyricsUiState =
    mutableStateOf(LyricsUiState())

private var lyricsPickerTarget: String? = null

private val pickLyrics = registerForActivityResult(
    ActivityResultContracts.OpenDocument()
) { selectedUri ->
    val trackUri = lyricsPickerTarget
    lyricsPickerTarget = null

    if (selectedUri != null && trackUri != null) {
        lifecycleScope.launch {
            try {
                val lines = withContext(Dispatchers.IO) {
                    lyricsRepository.import(
                        trackUri,
                        selectedUri
                    )
                }

                if (
                    playerViewModel.uiState.value
                        .audioUri?.toString() == trackUri
                ) {
                    lyricsUiState.value = LyricsUiState(
                        trackUri = trackUri,
                        lines = lines
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (
                    playerViewModel.uiState.value
                        .audioUri?.toString() == trackUri
                ) {
                    lyricsUiState.value = LyricsUiState(
                        trackUri = trackUri,
                        error = "Không thể nhập file LRC. " +
                            "Hãy chọn file khác."
                    )
                }
            }
        }
    }
}

    private var pickFilesJob: Job? = null

    @Volatile
    var lastPickedUrisForQa: List<Uri> = emptyList()
        private set


    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->

        if (uris.isNotEmpty()) {

            val selectedUris = uris.distinct()

            lastPickedUrisForQa = selectedUris

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

            // Hủy yêu cầu đọc metadata của lần chọn trước.
            pickFilesJob?.cancel()

            pickFilesJob = lifecycleScope.launch {

                // Vẫn đọc metadata ngoài Main thread.
                val files = withContext(Dispatchers.IO) {
                    selectedUris.map { uri ->
                        PickedAudioFile(
                            uri = uri,
                            title = getFileName(uri),
                            artist = getEmbeddedArtist(uri)
                        )
                    }
                }

                // Chỉ tác vụ chưa bị hủy mới được dùng
                // để cập nhật hàng đợi phát nhạc.
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

    private fun getEmbeddedArtist(uri: Uri): String? {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(this, uri)

            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_ARTIST
            )
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty() &&
                        !it.equals("<unknown>", ignoreCase = true)
                }

        } catch (_: Exception) {
            // Không có metadata hoặc file không hỗ trợ.
            null

        } finally {
            runCatching {
                retriever.release()
            }
        }
    }

    fun openAudioPickerForQa() {
        pickAudio.launch(arrayOf("audio/*"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerViewModel = ViewModelProvider(
            this,
            PlayerViewModel.factory(applicationContext)
        )[PlayerViewModel::class.java]

        lifecycleScope.launch {
            playerViewModel.uiState
                .map { it.audioUri?.toString() }
                .distinctUntilChanged()
                .collectLatest { trackUri ->

                    lyricsUiState.value = LyricsUiState(
                        trackUri = trackUri
                    )

                    if (trackUri == null) {
                        return@collectLatest
                    }

                    try {
                        val lines = withContext(Dispatchers.IO) {
                            lyricsRepository.load(trackUri)
                        }

                        lyricsUiState.value = LyricsUiState(
                            trackUri = trackUri,
                            lines = lines
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        lyricsUiState.value = LyricsUiState(
                            trackUri = trackUri,
                            error = "Không thể đọc lời bài hát đã lưu."
                        )
                    }
                }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    playerViewModel.refreshProgress()
                    delay(500)
                }
            }
        }

        setContent {
            val preferences = remember {
                getSharedPreferences("musicflow_settings", MODE_PRIVATE)
            }

            var themeMode by remember {
                mutableStateOf(
                    preferences.getString("theme_mode", "system")
                        ?.takeIf { it in setOf("system", "dark", "light") }
                        ?: "system"
                )
            }

            val systemDark = isSystemInDarkTheme()

            val useDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            val playerState by playerViewModel.uiState
                .collectAsStateWithLifecycle()

            MusicFlowTheme(darkTheme = useDarkTheme) {
                MusicFlowApp(
                    state = playerState,
                    lyrics = lyricsUiState.value,
                    themeMode = themeMode,
                    onThemeModeChange = { selected ->
                        if (selected in setOf("system", "dark", "light")) {
                            preferences.edit()
                                .putString("theme_mode", selected)
                                .apply()
                            themeMode = selected
                        }
                    },

                    onPickFiles = {
                        pickAudio.launch(arrayOf("audio/*"))
                    },

                    onPlayDeviceSongs = { songs, index ->
                        playerViewModel.playDeviceSongs(
                            songs,
                            index
                        )
                    },

                    onPlayRecentSongs = { songs, index ->
                        playerViewModel.playRecentSongs(
                            songs = songs,
                            selectedIndex = index
                        )
                    },

                    onPlayPlaylistTracks = { tracks, index ->
                        playerViewModel.playPlaylistTracks(
                            tracks = tracks,
                            selectedIndex = index
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
                    },

                    onSetPlaybackSpeed = { speed ->
                        playerViewModel.setPlaybackSpeed(speed)
                    },

                    onSetSleepTimerMinutes = { minutes ->
                        playerViewModel.setSleepTimerMinutes(minutes)
                    },
                    onSetEqualizerEnabled = { enabled ->
                        playerViewModel.setEqualizerEnabled(enabled)
                    },
                    onSetEqualizerBand = { index, level ->
                        playerViewModel.setEqualizerBand(index, level)
                    },
                    onSetCrossfadeDuration = { duration ->
                        playerViewModel.setCrossfadeDuration(duration)
                    },

                    onPickLyrics = {
                        val currentAudioUri =
                            playerViewModel.uiState.value
                                .audioUri?.toString()

                        if (currentAudioUri != null) {
                            lyricsPickerTarget = currentAudioUri

                            pickLyrics.launch(arrayOf("*/*"))
                        }
                    }
                )
            }
        }
    }
}

