package com.example.musicflow.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.LocalMusicRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryUiState {

    data object Idle : LibraryUiState

    data object Loading : LibraryUiState

    data object Empty : LibraryUiState

    data class Success(
        val songs: List<DeviceSong>
    ) : LibraryUiState

    data class Error(
        val message: String
    ) : LibraryUiState
}

class LibraryViewModel(
    private val repository: LocalMusicRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<LibraryUiState>(LibraryUiState.Idle)

    val uiState: StateFlow<LibraryUiState> =
        _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun refresh() {
        scanJob?.cancel()

        scanJob = viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading

            try {
                val songs = repository.getSongs()

                _uiState.value = if (songs.isEmpty()) {
                    LibraryUiState.Empty
                } else {
                    LibraryUiState.Success(songs)
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: SecurityException) {
                _uiState.value = LibraryUiState.Error(
                    "Ứng dụng không có quyền đọc nhạc."
                )

            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(
                    "Không thể quét nhạc: ${e.javaClass.simpleName}"
                )
            }
        }
    }

    companion object {

        fun factory(
            repository: LocalMusicRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {

                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    require(
                        modelClass.isAssignableFrom(
                            LibraryViewModel::class.java
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return LibraryViewModel(repository) as T
                }
            }
    }
}