package com.example.musicflow.feature.online

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicflow.core.online.HttpsOnlineCatalogRepository
import com.example.musicflow.core.online.OnlineCatalogRepository
import com.example.musicflow.core.online.OnlineCatalogSettings
import com.example.musicflow.core.online.OnlineTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class OnlineCatalogUiState(
    val endpointInput: String = "",
    val savedEndpoint: String = "",
    val tracks: List<OnlineTrack> = emptyList(),
    val nextCursor: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null
)

class OnlineCatalogViewModel(
    private val settings: OnlineCatalogSettings,
    private val repository: OnlineCatalogRepository
) : ViewModel() {
    private val initialEndpoint = settings.savedEndpoint()
    private val _uiState = MutableStateFlow(
        OnlineCatalogUiState(
            endpointInput = initialEndpoint,
            savedEndpoint = initialEndpoint
        )
    )
    val uiState: StateFlow<OnlineCatalogUiState> = _uiState.asStateFlow()

    private var requestJob: Job? = null

    init {
        if (initialEndpoint.isNotBlank()) refresh()
    }

    fun updateEndpointInput(value: String) {
        _uiState.update { it.copy(endpointInput = value, error = null) }
    }

    fun saveAndReload() {
        try {
            val normalized = settings.saveEndpoint(_uiState.value.endpointInput)
            requestJob?.cancel()
            _uiState.update {
                it.copy(
                    endpointInput = normalized,
                    savedEndpoint = normalized,
                    tracks = emptyList(),
                    nextCursor = null,
                    isLoaded = false,
                    error = null
                )
            }
            refresh()
        } catch (error: Exception) {
            _uiState.update {
                it.copy(error = error.message ?: "Không thể lưu máy chủ.")
            }
        }
    }

    fun clearEndpoint() {
        try {
            settings.clear()
            requestJob?.cancel()
            _uiState.value = OnlineCatalogUiState()
        } catch (error: Exception) {
            _uiState.update {
                it.copy(error = error.message ?: "Không thể xóa máy chủ.")
            }
        }
    }

    fun refresh() = requestPage(append = false)

    fun loadMore() {
        val current = _uiState.value
        if (current.nextCursor != null && !current.isLoadingMore &&
            !current.isLoading
        ) {
            requestPage(append = true)
        }
    }

    private fun requestPage(append: Boolean) {
        val state = _uiState.value
        val endpoint = state.savedEndpoint
        if (endpoint.isBlank()) return
        val cursor = if (append) state.nextCursor ?: return else null
        requestJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = !append,
                isLoadingMore = append,
                isLoaded = if (append) it.isLoaded else false,
                error = null,
                tracks = if (append) it.tracks else emptyList(),
                nextCursor = if (append) it.nextCursor else null
            )
        }

        requestJob = viewModelScope.launch {
            try {
                val page = repository.fetchPage(endpoint, cursor)
                _uiState.update { current ->
                    if (current.savedEndpoint != endpoint) return@update current
                    val combined = if (append) {
                        (current.tracks + page.tracks).distinctBy { it.id }
                    } else {
                        page.tracks
                    }
                    current.copy(
                        tracks = combined,
                        nextCursor = page.nextCursor
                            ?.takeUnless { it == cursor },
                        isLoading = false,
                        isLoadingMore = false,
                        isLoaded = true,
                        error = null
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val explanation = when (error) {
                    is IOException ->
                        "Không thể kết nối máy chủ. Kiểm tra Internet hoặc API."
                    else -> error.message ?: "Không thể tải danh mục online."
                }
                _uiState.update { current ->
                    if (current.savedEndpoint != endpoint) return@update current
                    current.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        isLoaded = true,
                        error = explanation
                    )
                }
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    require(modelClass.isAssignableFrom(OnlineCatalogViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return OnlineCatalogViewModel(
                        OnlineCatalogSettings(context.applicationContext),
                        HttpsOnlineCatalogRepository()
                    ) as T
                }
            }
    }
}
