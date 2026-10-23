
package com.example.musicflow.feature.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playlist.LocalPlaylist
import com.example.musicflow.core.playlist.PlaylistRules
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.RoomPlaylistRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlaylistsUiState {
    data object Loading : PlaylistsUiState

    data class Ready(
        val playlists: List<LocalPlaylist>
    ) : PlaylistsUiState

    data class Error(
        val message: String
    ) : PlaylistsUiState
}

sealed interface PlaylistTracksUiState {
    data object Loading : PlaylistTracksUiState

    data class Ready(
        val tracks: List<PlaylistTrack>
    ) : PlaylistTracksUiState

    data class Error(
        val message: String
    ) : PlaylistTracksUiState
}

sealed interface AddTrackUiState {
    data object Idle : AddTrackUiState
    data object Saving : AddTrackUiState

    data class Success(
        val playlistName: String
    ) : AddTrackUiState

    data class Error(
        val message: String
    ) : AddTrackUiState
}

class PlaylistViewModel(
    private val repository: RoomPlaylistRepository
) : ViewModel() {

    val playlistsState: StateFlow<PlaylistsUiState> =
        repository.observePlaylists()
            .map<List<LocalPlaylist>, PlaylistsUiState> {
                PlaylistsUiState.Ready(it)
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(
                    PlaylistsUiState.Error(
                        "Không thể tải danh sách phát."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PlaylistsUiState.Loading
            )

    fun tracksState(
        playlistId: String
    ): Flow<PlaylistTracksUiState> =
        repository.observeTracks(playlistId)
            .map<List<PlaylistTrack>, PlaylistTracksUiState> {
                PlaylistTracksUiState.Ready(it)
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(
                    PlaylistTracksUiState.Error(
                        "Không thể tải bài hát trong playlist."
                    )
                )
            }

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError = _actionError.asStateFlow()

    private val _addTrackState =
        MutableStateFlow<AddTrackUiState>(AddTrackUiState.Idle)

    val addTrackState = _addTrackState.asStateFlow()

    fun addDeviceSong(
        playlistId: String,
        playlistName: String,
        song: DeviceSong
    ) {
        if (_addTrackState.value == AddTrackUiState.Saving) {
            return
        }

        _addTrackState.value = AddTrackUiState.Saving

        viewModelScope.launch {
            try {
                val track = PlaylistTrack(
                    mediaId = "device-${song.id}",
                    audioUri = song.uri.toString(),
                    title = song.title,
                    artist = song.artist,
                    addedAtMs = System.currentTimeMillis()
                )

                val added = repository.addTrack(
                    playlistId = playlistId,
                    track = track
                )

                _addTrackState.value = if (added) {
                    AddTrackUiState.Success(playlistName)
                } else {
                    AddTrackUiState.Error(
                        "Bài hát đã có trong playlist " +
                            "hoặc playlist không còn tồn tại."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _addTrackState.value = AddTrackUiState.Error(
                    "Không thể thêm bài hát vào playlist."
                )
            }
        }
    }

    fun resetAddTrackState() {
        if (_addTrackState.value != AddTrackUiState.Saving) {
            _addTrackState.value = AddTrackUiState.Idle
        }
    }

    fun create(rawName: String) {
        if (PlaylistRules.normalizedNameOrNull(rawName) == null) {
            _actionError.value = "Tên playlist không hợp lệ."
            return
        }

        viewModelScope.launch {
            try {
                if (repository.create(rawName) == null) {
                    _actionError.value = "Không thể tạo playlist."
                } else {
                    _actionError.value = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _actionError.value = "Không thể tạo playlist."
            }
        }
    }

    fun rename(id: String, rawName: String) {
        if (PlaylistRules.normalizedNameOrNull(rawName) == null) {
            _actionError.value = "Tên playlist không hợp lệ."
            return
        }

        viewModelScope.launch {
            try {
                val success = repository.rename(id, rawName)
                _actionError.value = if (success) {
                    null
                } else {
                    "Không thể đổi tên playlist."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _actionError.value = "Không thể đổi tên playlist."
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                val success = repository.deletePlaylist(id)
                _actionError.value = if (success) {
                    null
                } else {
                    "Không thể xóa playlist."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _actionError.value = "Không thể xóa playlist."
            }
        }
    }

    fun removeTrack(
        playlistId: String,
        mediaId: String
    ) {
        if (playlistId.isBlank() || mediaId.isBlank()) {
            _actionError.value = "Bài hát không hợp lệ."
            return
        }

        viewModelScope.launch {
            try {
                val removed = repository.removeTrack(
                    playlistId = playlistId,
                    mediaId = mediaId
                )

                _actionError.value = if (removed) {
                    null
                } else {
                    "Không thể xóa bài hát khỏi playlist."
                }

            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _actionError.value =
                    "Không thể xóa bài hát khỏi playlist."
            }
        }
    }

    fun dismissError() {
        _actionError.value = null
    }

    companion object {
        fun factory(
            repository: RoomPlaylistRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    require(
                        modelClass.isAssignableFrom(
                            PlaylistViewModel::class.java
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return PlaylistViewModel(repository) as T
                }
            }
    }
}
