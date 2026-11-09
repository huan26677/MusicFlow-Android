package com.example.musicflow.feature.account

import java.util.Locale

/** Keep HTTP/RPC error details away from the account screen. No auth rules change. */
object AccountFriendlyErrors {
    fun authentication(failure: Throwable, register: Boolean): String {
        val message = failure.message.orEmpty().lowercase(Locale.ROOT)
        return when {
            "email not confirmed" in message || "chưa xác nhận email" in message ->
                "Hãy mở email xác nhận tài khoản, sau đó đăng nhập lại."
            "invalid login credentials" in message || "sai mật khẩu" in message ||
                "incorrect password" in message ->
                "Email hoặc mật khẩu chưa đúng. Hãy kiểm tra và thử lại."
            "already registered" in message || "đã tồn tại" in message ->
                "Email này đã có tài khoản. Hãy chuyển sang Đăng nhập."
            "rate limit" in message || "too many requests" in message ->
                "Bạn đã thử nhiều lần. Hãy đợi một lúc rồi thử lại."
            "timeout" in message || "network" in message ||
                "connection" in message || "unable to resolve host" in message ->
                "Không thể kết nối. Hãy kiểm tra Internet rồi thử lại."
            "password" in message && register ->
                "Mật khẩu chưa đáp ứng yêu cầu. Hãy kiểm tra và thử lại."
            register -> "Chưa tạo được tài khoản. Hãy kiểm tra thông tin rồi thử lại."
            else -> "Chưa đăng nhập được. Hãy kiểm tra thông tin rồi thử lại."
        }
    }
}
