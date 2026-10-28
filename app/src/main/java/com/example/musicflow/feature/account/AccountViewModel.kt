package com.example.musicflow.feature.account

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicflow.core.account.AccountApi
import com.example.musicflow.core.account.AccountIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val loading: Boolean = true,
    val account: AccountIdentity? = null,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val message: String? = null,
    val error: String? = null,
    val configured: Boolean = true
)

class AccountViewModel(private val api: AccountApi) : ViewModel() {
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
        if (_state.value.loading) return
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

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AccountViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return AccountViewModel(AccountApi(context.applicationContext)) as T
                }
            }
    }
}
