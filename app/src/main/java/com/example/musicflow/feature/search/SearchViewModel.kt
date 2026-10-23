
package com.example.musicflow.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.LocalMusicRepository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchUiState(
    val query: String = "",
    val songs: List<DeviceSong> = emptyList(),
    val results: List<DeviceSong> = emptyList(),
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null
)

class SearchViewModel(
    private val repository: LocalMusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())

    val uiState: StateFlow<SearchUiState> =
        _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var filterJob: Job? = null

    fun refresh() {
        scanJob?.cancel()
        filterJob?.cancel()

        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    songs = emptyList(),
                    results = emptyList(),
                    isLoading = true,
                    isLoaded = false,
                    error = null
                )
            }

            try {
                // Repository đã quét MediaStore trên Dispatchers.IO.
                val songs = repository.getSongs()

                _uiState.update {
                    it.copy(
                        songs = songs,
                        isLoading = false,
                        isLoaded = true,
                        error = null
                    )
                }

                // Giữ lại từ khóa nếu đã nhập trước khi quét xong.
                scheduleFilter(
                    query = _uiState.value.query,
                    songs = songs
                )

            } catch (e: CancellationException) {
                throw e

            } catch (_: SecurityException) {
                showError("Ứng dụng không có quyền đọc âm thanh.")

            } catch (e: Exception) {
                showError(
                    "Không thể quét nhạc: " +
                            e.javaClass.simpleName
                )
            }
        }
    }

    fun updateQuery(query: String) {
        filterJob?.cancel()

        _uiState.update {
            it.copy(
                query = query,
                results = emptyList()
            )
        }

        if (!query.isBlank() && _uiState.value.isLoaded) {
            scheduleFilter(
                query = query,
                songs = _uiState.value.songs
            )
        }
    }

    private fun scheduleFilter(
        query: String,
        songs: List<DeviceSong>
    ) {
        filterJob?.cancel()

        if (query.isBlank()) {
            return
        }

        filterJob = viewModelScope.launch {
            delay(150)

            val results = withContext(Dispatchers.Default) {
                SongSearch.filter(songs, query)
            }

            // Bỏ kết quả cũ nếu đã nhập từ khóa mới
            // hoặc danh sách nhạc đã được quét lại.
            _uiState.update { current ->
                if (
                    current.query == query &&
                    current.isLoaded &&
                    current.songs === songs
                ) {
                    current.copy(results = results)
                } else {
                    current
                }
            }
        }
    }

    fun clearSongs() {
        scanJob?.cancel()
        filterJob?.cancel()

        _uiState.update {
            SearchUiState(query = it.query)
        }
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(
                songs = emptyList(),
                results = emptyList(),
                isLoading = false,
                isLoaded = false,
                error = message
            )
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
                            SearchViewModel::class.java
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return SearchViewModel(repository) as T
                }
            }
    }
}
