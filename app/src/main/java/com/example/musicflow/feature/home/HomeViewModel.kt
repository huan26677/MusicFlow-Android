
package com.example.musicflow.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.history.RoomRecentHistoryRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeHistoryUiState {
    data object Loading : HomeHistoryUiState
    data object Empty : HomeHistoryUiState

    data class Success(
        val songs: List<RecentSong>
    ) : HomeHistoryUiState

    data class Error(
        val message: String
    ) : HomeHistoryUiState
}

class HomeViewModel(
    private val repository: RoomRecentHistoryRepository
) : ViewModel() {

    val historyState: StateFlow<HomeHistoryUiState> =
        repository.observeRecent()
            .map<List<RecentSong>, HomeHistoryUiState> { songs ->
                if (songs.isEmpty()) {
                    HomeHistoryUiState.Empty
                } else {
                    HomeHistoryUiState.Success(songs)
                }
            }
            .catch { error ->
                if (error is CancellationException) {
                    throw error
                }

                emit(
                    HomeHistoryUiState.Error(
                        "Không thể đọc lịch sử nghe nhạc."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeHistoryUiState.Loading
            )

    private val _actionError = MutableStateFlow<String?>(null)

    val actionError = _actionError.asStateFlow()

    fun removeRecentSong(mediaId: String) {
        if (mediaId.isBlank()) return

        viewModelScope.launch {
            try {
                repository.remove(mediaId)
                _actionError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _actionError.value =
                    "Không thể xóa bài hát khỏi lịch sử."
            }
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            try {
                repository.clear()
                _actionError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _actionError.value =
                    "Không thể xóa lịch sử nghe nhạc."
            }
        }
    }

    fun dismissActionError() {
        _actionError.value = null
    }

    companion object {

        fun factory(
            repository: RoomRecentHistoryRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {

                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    require(
                        modelClass.isAssignableFrom(
                            HomeViewModel::class.java
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return HomeViewModel(repository) as T
                }
            }
    }
}
