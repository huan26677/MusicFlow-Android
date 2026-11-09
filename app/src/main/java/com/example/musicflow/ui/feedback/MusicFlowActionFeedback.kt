package com.example.musicflow.ui.feedback

import java.io.IOException
import java.util.Locale

/**
 * Presentation only: do not display raw exception text containing URLs, tokens,
 * provider details or implementation identifiers. No business rule is changed.
 */
object MusicFlowActionFeedback {
    enum class FileAction { EXPORT, IMPORT, CHECK }
    enum class CloudAction { LIBRARY, HISTORY, ITEM }

    fun fileFailure(error: Throwable, action: FileAction): String {
        val detail = error.message.orEmpty().lowercase(Locale.ROOT)
        return when {
            "2 mb" in detail || "2mb" in detail ->
                "Tệp sao lưu vượt giới hạn 2 MB. Hãy chọn tệp nhỏ hơn."
            "quá nhiều playlist" in detail || "tối đa 100" in detail ->
                "Tệp có quá nhiều danh sách phát (tối đa 100)."
            "500 bài" in detail ->
                "Một danh sách phát vượt giới hạn 500 bài."
            "không ghi được tệp" in detail ->
                "Không lưu được tệp. Hãy chọn thư mục khác và thử lại."
            "không đọc được tệp" in detail ->
                "Không đọc được tệp đã chọn. Hãy chọn lại tệp."
            error is SecurityException || "permission denied" in detail ->
                "Không có quyền truy cập tệp. Hãy chọn lại tệp hoặc cấp quyền đọc."
            action == FileAction.IMPORT &&
                (error is IllegalArgumentException ||
                    "json" in detail || "format" in detail ||
                    "sao lưu musicflow" in detail || "phiên bản" in detail) ->
                "Tệp không đúng định dạng sao lưu MusicFlow. Hãy chọn tệp .json hợp lệ."
            error is IOException ->
                "Không đọc hoặc ghi được tệp. Kiểm tra bộ nhớ rồi thử lại."
            action == FileAction.EXPORT ->
                "Không thể sao lưu danh sách phát. Hãy thử lại hoặc chọn nơi lưu khác."
            action == FileAction.IMPORT ->
                "Không thể khôi phục danh sách phát. Hãy chọn lại tệp sao lưu."
            else ->
                "Không kiểm tra được tệp nhạc. Hãy chọn lại tệp hoặc thử lại."
        }
    }

    fun cloudFailure(error: Throwable, action: CloudAction): String {
        val detail = error.message.orEmpty().lowercase(Locale.ROOT)
        return when {
            "unauthorized" in detail || "jwt" in detail ||
                "session" in detail || "đăng nhập" in detail ->
                "Phiên đăng nhập có thể đã hết hạn. Hãy đăng nhập lại rồi thử."
            error is SecurityException || "permission denied" in detail ||
                "forbidden" in detail ->
                "Tài khoản chưa có quyền thực hiện thao tác này."
            error is IOException || "timeout" in detail ||
                "network" in detail || "connection" in detail ->
                "Không thể kết nối đám mây. Hãy kiểm tra Internet rồi thử lại."
            action == CloudAction.LIBRARY ->
                "Chưa đồng bộ được thư viện. Kiểm tra kết nối và quyền truy cập rồi thử lại."
            action == CloudAction.HISTORY ->
                "Chưa đồng bộ được lịch sử nghe. Kiểm tra kết nối rồi thử lại."
            else ->
                "Chưa cập nhật được bản sao trên đám mây. Hãy thử lại."
        }
    }
}
