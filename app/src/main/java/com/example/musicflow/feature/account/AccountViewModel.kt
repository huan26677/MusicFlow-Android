package com.example.musicflow.feature.account

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicflow.core.account.AccountApi
import com.example.musicflow.core.account.AccountIdentity
import com.example.musicflow.core.account.MusicFlowAccess
import com.example.musicflow.core.account.DemoAccountStore
import com.example.musicflow.core.account.AccountOAuthRedirects
import com.example.musicflow.core.account.CloudLibrarySync
import com.example.musicflow.core.account.CloudHistorySync
import com.example.musicflow.core.account.CloudSyncSummary
import com.example.musicflow.core.account.CloudPendingMatch
import org.json.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AccountUiState(
    val loading: Boolean = true,
    val account: AccountIdentity? = null,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val authMode: AccountAuthMode = AccountAuthMode.SIGN_IN,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val displayName: String = "",
    val message: String? = null,
    val error: String? = null,
    val configured: Boolean = true,
    val demoMode: Boolean = false,
    val demoSession: Boolean = false,
    val googleAvailable: Boolean? = null,
    val emailConfirmationRequired: Boolean? = null,
    val syncing: Boolean = false,
    val syncSummary: CloudSyncSummary? = null
)

class AccountViewModel(
    private val api: AccountApi,
    private val cloudSync: CloudLibrarySync,
    private val cloudHistory: CloudHistorySync,
    private val demoStore: DemoAccountStore
) : ViewModel() {
    private val _state = MutableStateFlow(
        // Supabase online is the default. The offline-only Demo mode is opt-in.
        AccountUiState(configured = api.isConfigured, demoMode = false)
    )
    val state = _state.asStateFlow()

    /** Uses the existing authenticated AccountApi session; RPCs recheck roles in SQL. */
    suspend fun roleRpc(name: String, body: JSONObject): String = api.roleRpc(name, body)
    suspend fun uploadCreatorFile(draftId: String, kind: String, uri: Uri) =
        api.uploadCreatorFile(draftId, kind, uri)
    suspend fun downloadCreatorFile(path: String, destination: Uri) =
        api.downloadCreatorFile(path, destination)

    init {
        viewModelScope.launch {
            try {
                // Real server sessions take precedence. A previously explicitly
                // signed-in LOCAL demo account may resume offline, but its tier
                // and roles can never be interpreted as Supabase privileges.
                val remote = api.restore()
                val localDemo = if (remote == null) demoStore.restore() else null
                val account = remote ?: localDemo
                _state.update {
                    it.copy(
                        loading = false, account = account,
                        demoSession = localDemo != null,
                        demoMode = localDemo != null,
                        displayName = account?.displayName.orEmpty()
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // An offline demo login is still usable when Supabase cannot
                // be reached; no fabricated JWT or elevated access is supplied.
                val localDemo = runCatching { demoStore.restore() }.getOrNull()
                _state.update {
                    it.copy(loading = false, account = localDemo,
                        demoMode = localDemo != null,
                        demoSession = localDemo != null,
                        displayName = localDemo?.displayName.orEmpty(),
                        message = if (localDemo == null) {
                            "Chưa thể khôi phục phiên. Kiểm tra Internet để đăng nhập."
                        } else "Đã mở tài khoản Demo lưu trên thiết bị. Không có quyền cloud."
                    )
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
            val confirmation = try {
                api.emailConfirmationRequired()
            } catch (_: Exception) {
                null
            }
            _state.update {
                it.copy(
                    googleAvailable = available,
                    emailConfirmationRequired = confirmation
                )
            }
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
        api.cancelGoogleSignIn()
        _state.update {
            it.copy(error = "Không tìm thấy trình duyệt để đăng nhập Google.")
        }
    }

    fun emailChanged(value: String) =
        _state.update { it.copy(email = value, emailError = null, error = null) }

    fun passwordChanged(value: String) =
        _state.update {
            it.copy(
                password = value, passwordError = null,
                confirmPasswordError = null, error = null
            )
        }

    fun confirmPasswordChanged(value: String) =
        _state.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, error = null)
        }

    fun authModeChanged(mode: AccountAuthMode) {
        if (_state.value.loading || _state.value.account != null) return
        _state.update {
            it.copy(
                authMode = mode, password = "", confirmPassword = "",
                emailError = null, passwordError = null,
                confirmPasswordError = null, error = null, message = null
            )
        }
    }

    fun displayNameChanged(value: String) =
        _state.update { it.copy(displayName = value, error = null) }

    fun demoModeChanged(value: Boolean) {
        if (_state.value.loading || _state.value.account != null) return
        _state.update {
            it.copy(
                demoMode = value, password = "", confirmPassword = "",
                passwordError = null, confirmPasswordError = null,
                message = null, error = null
            )
        }
    }

    fun signIn() = authenticate(register = false)
    fun signUp() = authenticate(register = true)

    private fun authenticate(register: Boolean) {
        if (_state.value.loading) return
        val email = _state.value.email.trim()
        val password = _state.value.password
        val errors = AccountFormValidation.validate(
            email, password, _state.value.confirmPassword,
            if (register) AccountAuthMode.SIGN_UP else AccountAuthMode.SIGN_IN
        )
        if (!errors.isValid) {
            _state.update {
                it.copy(
                    emailError = errors.email,
                    passwordError = errors.password,
                    confirmPasswordError = errors.confirmPassword,
                    error = null, message = null
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = null) }
            try {
                if (_state.value.demoMode) {
                    val localAccount = withContext(Dispatchers.Default) {
                        if (register) demoStore.register(email, password)
                        else demoStore.signIn(email, password)
                    }
                    _state.update {
                        it.copy(
                            loading = false,
                            account = localAccount,
                            demoSession = true,
                            password = "",
                            confirmPassword = "",
                            displayName = localAccount.displayName,
                            message = if (register) {
                                "Đã đăng ký tài khoản Demo trên thiết bị này. Không gửi email và không đồng bộ Supabase."
                            } else {
                                "Đã đăng nhập Demo. Tài khoản chỉ nằm trên thiết bị này."
                            }
                        )
                    }
                    return@launch
                }
                val account = if (register) {
                    if (api.signUp(email, password)) api.currentAccount() else null
                } else api.signIn(email, password)
                _state.update {
                    it.copy(
                        loading = false,
                        account = account,
                        password = "",
                        confirmPassword = "",
                        displayName = account?.displayName.orEmpty(),
                        message = if (account == null) {
                            "Đã gửi yêu cầu đăng ký. Hãy kiểm tra email để xác nhận rồi đăng nhập."
                        } else if (register) {
                            "Đã tạo tài khoản và đăng nhập thành công."
                        } else "Đã đăng nhập thành công."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, password = "", confirmPassword = "",
                        error = AccountFriendlyErrors.authentication(error, register))
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
                val identity = if (_state.value.demoSession) {
                    withContext(Dispatchers.IO) { demoStore.changeDisplayName(name) }
                } else {
                    api.changeDisplayName(name)
                }
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

    /** Recheck tier and roles against Supabase, never trust cached privilege badges. */
    fun refreshAccount() {
        val previous = _state.value.account ?: return
        if (_state.value.loading || _state.value.syncing || _state.value.demoSession) return
        // Hide stale privileges/entitlement immediately while awaiting a fresh response.
        _state.update {
            it.copy(
                loading = true, error = null, message = null,
                account = previous.copy(tier = "free", expiresAt = null, access = MusicFlowAccess.NONE)
            )
        }
        viewModelScope.launch {
            try {
                val latest = api.currentAccount(requireFreshRoles = true)
                check(latest.userId == previous.userId) { "Phiên tài khoản đã thay đổi." }
                _state.update {
                    it.copy(
                        loading = false, account = latest,
                        displayName = latest.displayName,
                        message = "Đã cập nhật gói thành viên và quyền truy cập."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "Chưa xác minh được gói và quyền. Kiểm tra kết nối hoặc đăng nhập lại; các công cụ đặc quyền đã được ẩn."
                    )
                }
            }
        }
    }

    fun signOut() {
        if (_state.value.loading || _state.value.syncing) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            if (_state.value.demoSession) {
                withContext(Dispatchers.IO) { demoStore.signOut() }
            } else {
                api.signOut()
            }
            _state.update {
                AccountUiState(
                    loading = false, configured = api.isConfigured, demoMode = false,
                    message = "Đã đăng xuất. Nhạc trên thiết bị vẫn được giữ nguyên."
                )
            }
        }
    }

    fun syncLibrary() {
        if (_state.value.loading || _state.value.syncing ||
            _state.value.account == null || _state.value.demoSession
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
        if (_state.value.syncing || _state.value.demoSession) return
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
        if (_state.value.syncing || _state.value.demoSession) return
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
        if (_state.value.syncing || _state.value.demoSession) return
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
            _state.value.account == null || _state.value.demoSession) return
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
                        CloudHistorySync(context.applicationContext, api),
                        DemoAccountStore(context.applicationContext)
                    )
                    @Suppress("UNCHECKED_CAST")
                    return model as T
                }
            }
    }
}
