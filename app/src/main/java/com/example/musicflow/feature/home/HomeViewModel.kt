
package com.example.musicflow.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.history.RoomRecentHistoryRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
