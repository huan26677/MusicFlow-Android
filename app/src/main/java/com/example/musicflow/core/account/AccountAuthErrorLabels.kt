package com.example.musicflow.core.account

import java.util.Locale

/**
 * User-readable messages for the trusted Supabase Auth error codes.
 *
 * Never echo the server's raw error message: it may include addresses or other
 * private information. A client cannot bypass a hosted project's SMTP limits.
 */
internal object AccountAuthErrorLabels {
    fun forResponse(status: Int, authErrorCode: String?): String {
        val code = authErrorCode?.trim()?.lowercase(Locale.ROOT)
        return when (code) {
            "over_email_send_rate_limit", "email_rate_limit_exceeded" ->
                "Supabase đã hết hạn mức gửi email xác nhận. Đăng ký chưa thành công. " +
                    "Hãy thử lại sau khi hạn mức được làm mới hoặc cấu hình SMTP riêng."
            "email_address_not_authorized" ->
                "Dịch vụ email mặc định của Supabase chưa cho phép gửi đến địa chỉ này. " +
                    "Cần dùng email thành viên dự án hoặc cấu hình SMTP riêng."
            "email_address_invalid" ->
                "Supabase từ chối địa chỉ email này. Hãy kiểm tra lại địa chỉ và tên miền email."
            "email_not_confirmed" ->
                "Email chưa được xác nhận. Hãy mở email xác nhận trước khi đăng nhập."
            "invalid_credentials" ->
                "Email hoặc mật khẩu không đúng."
            "user_already_exists", "email_exists" ->
                "Email này đã có tài khoản. Hãy đăng nhập thay vì đăng ký lại."
            "weak_password" ->
                "Mật khẩu chưa đạt yêu cầu bảo mật của máy chủ. Hãy chọn mật khẩu mạnh hơn."
            "signup_disabled" ->
                "Máy chủ hiện không cho phép đăng ký tài khoản mới."
            else -> when (status) {
                400, 401 -> "Email, mật khẩu hoặc phiên đăng nhập không hợp lệ."
                403 -> "Không có quyền truy cập tài nguyên này."
                422 -> "Email hoặc mật khẩu không hợp lệ hoặc đã được đăng ký."
                429 -> "Máy chủ đang giới hạn yêu cầu. Hãy thử lại sau."
                else -> "Máy chủ trả về lỗi HTTP $status."
            }
        }
    }
}
