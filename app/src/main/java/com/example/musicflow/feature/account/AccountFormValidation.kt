package com.example.musicflow.feature.account

enum class AccountAuthMode { SIGN_IN, SIGN_UP }

data class AccountFormErrors(
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null
) {
    val isValid: Boolean get() = email == null && password == null && confirmPassword == null
}

/** Local form validation. Supabase still validates all credentials on the server. */
object AccountFormValidation {
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun validate(
        email: String,
        password: String,
        confirmPassword: String,
        mode: AccountAuthMode
    ): AccountFormErrors = AccountFormErrors(
        email = when {
            email.isBlank() -> "Bạn chưa nhập email."
            !emailPattern.matches(email.trim()) -> "Địa chỉ email chưa hợp lệ."
            else -> null
        },
        password = when {
            password.isEmpty() -> "Bạn chưa nhập mật khẩu."
            password.length < 6 -> "Mật khẩu phải có ít nhất 6 ký tự."
            else -> null
        },
        confirmPassword = if (mode == AccountAuthMode.SIGN_UP) {
            when {
                confirmPassword.isEmpty() -> "Hãy nhập lại mật khẩu."
                confirmPassword != password -> "Mật khẩu nhập lại chưa khớp."
                else -> null
            }
        } else null
    )
}
