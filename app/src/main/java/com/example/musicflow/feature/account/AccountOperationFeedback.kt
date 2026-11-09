package com.example.musicflow.feature.account

import java.io.IOException
import java.util.Locale

/**
 * User-facing feedback for existing privileged Creator / admin actions.
 * Raw RPC, Storage or session exceptions must never appear in the interface.
 */
object AccountOperationFeedback {
    enum class Action {
        ADMIN,
        DRAFT_SAVE,
        DRAFT_SUBMIT,
        DRAFT_REVIEW,
        FILE_UPLOAD,
        FILE_DOWNLOAD,
        FILE_LIST
    }

    fun failure(cause: Throwable, action: Action): String {
        val detail = cause.message.orEmpty().lowercase(Locale.ROOT)
        return when {
            "jwt" in detail || "session" in detail || "unauthorized" in detail ||
                "401" in detail || "đăng nhập" in detail ->
                "Phiên đăng nhập có thể đã hết hạn. Hãy đăng nhập lại rồi thử."
            cause is SecurityException || "permission denied" in detail ||
                "forbidden" in detail || "403" in detail ->
                "Tài khoản chưa có quyền thực hiện thao tác này."
            action == Action.FILE_UPLOAD &&
                ("6 mb" in detail || "6mb" in detail || "too large" in detail) ->
                "Tệp lớn hơn 6 MB. Hãy chọn tệp nhỏ hơn."
            action == Action.FILE_UPLOAD &&
                ("mime" in detail || "unsupported" in detail || "file type" in detail) ->
                "Định dạng tệp không phù hợp. Hãy chọn bản thu hoặc chứng cứ PDF/JPG/PNG."
            cause is IOException || "timeout" in detail || "network" in detail ||
                "connection" in detail ->
                "Không thể kết nối. Hãy kiểm tra Internet rồi thử lại."
            action == Action.ADMIN ->
                "Chưa thực hiện được thao tác quản trị. Hãy kiểm tra quyền và thử lại."
            action == Action.DRAFT_SAVE ->
                "Chưa lưu được bản nháp. Hãy kiểm tra thông tin và thử lại."
            action == Action.DRAFT_SUBMIT ->
                "Chưa gửi được bản nháp xét duyệt. Hãy thử lại."
            action == Action.DRAFT_REVIEW ->
                "Chưa ghi được quyết định xét duyệt. Hãy thử lại."
            action == Action.FILE_UPLOAD ->
                "Chưa tải lên được tệp. Hãy chọn lại tệp và thử lại."
            action == Action.FILE_DOWNLOAD ->
                "Chưa tải về được tệp. Hãy chọn nơi lưu khác và thử lại."
            else ->
                "Chưa tải được danh sách tệp. Hãy thử làm mới."
        }
    }
}
