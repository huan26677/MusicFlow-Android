package com.example.musicflow.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicflow.core.catalog.OnlineCatalogConfiguration
import com.example.musicflow.core.catalog.OnlineCatalogRepository
import com.example.musicflow.core.catalog.OnlineCatalogTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OnlineCatalogUiState {
    data object NotConfigured : OnlineCatalogUiState
    data object Loading : OnlineCatalogUiState
    data object Empty : OnlineCatalogUiState
    data class Loaded(val tracks: List<OnlineCatalogTrack>) : OnlineCatalogUiState
    data class Error(val message: String) : OnlineCatalogUiState
}

class OnlineCatalogViewModel(
    private val configuration: OnlineCatalogConfiguration,
    private val repository: OnlineCatalogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<OnlineCatalogUiState>(
        OnlineCatalogUiState.NotConfigured
    )
    val uiState: StateFlow<OnlineCatalogUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        if (configuration.baseUrl().isBlank()) {
            _uiState.value = OnlineCatalogUiState.NotConfigured
            return
        }

        refreshJob = viewModelScope.launch {
            _uiState.value = OnlineCatalogUiState.Loading
            try {
                val tracks = repository.loadTracks()
                _uiState.value = if (tracks.isEmpty()) {
                    OnlineCatalogUiState.Empty
                } else {
                    OnlineCatalogUiState.Loaded(tracks)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = OnlineCatalogUiState.Error(
                    when (error) {
                        is IllegalArgumentException,
                        is IllegalStateException -> error.message
                            ?: "Dữ liệu danh mục không hợp lệ."
                        else -> "Không thể kết nối máy chủ. " +
                            "Kiểm tra mạng và cấu hình HTTPS."
                    }
                )
            }
        }
    }

    companion object {
        fun factory(
            configuration: OnlineCatalogConfiguration,
            repository: OnlineCatalogRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(OnlineCatalogViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return OnlineCatalogViewModel(configuration, repository) as T
            }
        }
    }
}
