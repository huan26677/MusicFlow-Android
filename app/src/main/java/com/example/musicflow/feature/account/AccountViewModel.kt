package com.example.musicflow.feature.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicflow.core.account.AccountApi
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.core.account.AccountOAuthRedirects
import com.example.musicflow.core.account.CloudLibrarySync
import com.example.musicflow.core.account.CloudHistorySync
import com.example.musicflow.core.account.CloudSyncSummary
import com.example.musicflow.core.account.CloudPendingMatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class AccountUiState(
    val loading: Boolean = true,
    val account: AccountIdentity? = null,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val message: String? = null,
    val error: String? = null,
    val configured: Boolean = true,
    val googleAvailable: Boolean? = null,
    val syncing: Boolean = false,
    val syncSummary: CloudSyncSummary? = null
)

class AccountViewModel(
    private val api: AccountApi,
    private val cloudSync: CloudLibrarySync,
    private val cloudHistory: CloudHistorySync
) : ViewModel() {
    private val _state = MutableStateFlow(
        AccountUiState(configured = api.isConfigured)
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val account = api.restore()
                _state.update {
                    it.copy(
                        loading = false, account = account,
                        displayName = account?.displayName.orEmpty()
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update {
                    it.copy(loading = false, account = null,
                        message = "Chưa thể khôi phục phiên. Kiểm tra Internet để đăng nhập.")
                }
            }
            AccountOAuthRedirects.latest.filterNotNull().collect { callback ->
                AccountOAuthRedirects.consume(callback)
                _state.update { it.copy(loading = true, error = null, message = null) }
                try {
                    val identity = api.finishGoogleSignIn(callback)
                    _state.update {
                        it.copy(
                            loading = false,
                            account = identity,
                            password = "",
                            displayName = identity.displayName,
                            message = "Đã đăng nhập bằng Google."
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _state.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: "Không đăng nhập Google được."
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            val available = try {
                api.googleProviderEnabled()
            } catch (_: Exception) {
                null
            }
            _state.update { it.copy(googleAvailable = available) }
        }
    }

    fun beginGoogleSignIn(): String? {
        if (_state.value.loading || _state.value.googleAvailable != true) return null
        return try {
            api.prepareGoogleSignIn()
        } catch (error: Exception) {
            _state.update { it.copy(error = error.message ?: "Không mở được Google.") }
            null
        }
    }

    fun googleLaunchFailed() {
        _state.update {
            it.copy(error = "Không tìm thấy trình duyệt để đăng nhập Google.")
        }
    }

    fun emailChanged(value: String) =
        _state.update { it.copy(email = value, error = null) }

    fun passwordChanged(value: String) =
        _state.update { it.copy(password = value, error = null) }

    fun displayNameChanged(value: String) =
        _state.update { it.copy(displayName = value, error = null) }

    fun signIn() = authenticate(register = false)
    fun signUp() = authenticate(register = true)

    private fun authenticate(register: Boolean) {
        if (_state.value.loading) return
        val email = _state.value.email.trim()
        val password = _state.value.password
        if (!email.contains('@') || password.length < 6) {
            _state.update {
                it.copy(error = "Nhập email hợp lệ và mật khẩu từ 6 ký tự.")
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = null) }
            try {
                val account = if (register) {
                    if (api.signUp(email, password)) api.currentAccount() else null
                } else api.signIn(email, password)
                _state.update {
                    it.copy(
                        loading = false,
                        account = account,
                        password = "",
                        displayName = account?.displayName.orEmpty(),
                        message = if (account == null) {
                            "Đã gửi yêu cầu đăng ký. Hãy kiểm tra email để xác nhận rồi đăng nhập."
                        } else "Đã đăng nhập thành công."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, password = "",
                        error = error.message ?: "Không thể xác thực tài khoản.")
                }
            }
        }
    }

    fun saveProfile() {
        if (_state.value.loading || _state.value.account == null) return
        val name = _state.value.displayName
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = null) }
            try {
                val identity = api.changeDisplayName(name)
                _state.update { it.copy(
                    loading = false, account = identity,
                    displayName = identity.displayName,
                    message = "Đã lưu hồ sơ."
                ) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { it.copy(
                    loading = false, error = error.message ?: "Không lưu được hồ sơ."
                ) }
            }
        }
    }

    fun signOut() {
        if (_state.value.loading || _state.value.syncing) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            api.signOut()
            _state.update {
                AccountUiState(
                    loading = false, configured = api.isConfigured,
                    message = "Đã đăng xuất. Nhạc trên thiết bị vẫn được giữ nguyên."
                )
            }
        }
    }

    fun syncLibrary() {
        if (_state.value.loading || _state.value.syncing ||
            _state.value.account == null
        ) return
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, error = null, message = null) }
            try {
                val result = cloudSync.sync()
                _state.update {
                    it.copy(
                        syncing = false,
                        syncSummary = result,
                        message = "Đồng bộ xong: lưu ${result.playlistsSaved} playlist, " +
                            "${result.favoritesSaved} yêu thích; " +
                            "khôi phục ${result.playlistsRestored} playlist, " +
                            "liên kết ${result.tracksLinked} bài, " +
                            "${result.favoritesLinked} yêu thích. " +
                            "${result.unmatched} mục chưa tìm thấy trên thiết bị."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        syncing = false,
                        error = "Đồng bộ chưa hoàn tất: " +
                            (error.message ?: "Kiểm tra Internet và quyền thư viện.")
                    )
                }
            }
        }
    }

    fun relink(pending: CloudPendingMatch, uri: String) {
        if (_state.value.syncing) return
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, error = null) }
            try {
                cloudSync.linkManually(pending, uri)
                _state.update {
                    it.copy(syncing = false,
                        syncSummary = it.syncSummary?.copy(
                            pending = it.syncSummary.pending - pending
                        ),
                        message = "Đã liên kết bài. Nhấn Đồng bộ thư viện để cập nhật.")
                }
            } catch (error: Exception) {
                _state.update { it.copy(syncing = false, error = error.message) }
            }
        }
    }

    fun deleteRemotePlaylist(id: String) {
        if (_state.value.syncing) return
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, error = null) }
            try {
                cloudSync.trashCloudPlaylist(id)
                _state.update {
                    it.copy(syncing = false, syncSummary = null,
                        message = "Đã đánh dấu xóa bản sao cloud. Playlist trên máy vẫn giữ nguyên.")
                }
            } catch (error: Exception) {
                _state.update { it.copy(syncing = false, error = error.message) }
            }
        }
    }

    fun deleteRemoteFavorite(key: String) {
        if (_state.value.syncing) return
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, error = null) }
            try {
                cloudSync.trashCloudFavorite(key)
                _state.update {
                    it.copy(syncing = false, syncSummary = null,
                        message = "Đã đánh dấu xóa yêu thích trên cloud. Bản local vẫn còn.")
                }
            } catch (error: Exception) {
                _state.update { it.copy(syncing = false, error = error.message) }
            }
        }
    }

    fun syncHistory() {
        if (_state.value.loading || _state.value.syncing ||
            _state.value.account == null) return
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, error = null, message = null) }
            try {
                val result = cloudHistory.sync()
                _state.update {
                    it.copy(syncing = false,
                        message = "Lịch sử: gửi ${result.uploaded} bài, " +
                            "khôi phục ${result.restored}; " +
                            "${result.notOnDevice} bài chưa có trên thiết bị.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(syncing = false,
                        error = "Không đồng bộ được lịch sử: " +
                            (error.message ?: "Kiểm tra kết nối và quyền thư viện."))
                }
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AccountViewModel::class.java))
                    val api = AccountApi(context.applicationContext)
                    val model = AccountViewModel(
                        api, CloudLibrarySync(context.applicationContext, api),
                        CloudHistorySync(context.applicationContext, api)
                    )
                    @Suppress("UNCHECKED_CAST")
                    return model as T
                }
            }
    }
}
