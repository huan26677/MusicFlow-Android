package com.example.musicflow.feature.online

import java.io.IOException

/** Messages for the existing online catalog; no playback or licensing policy changes. */
object OnlineCatalogFeedback {
    enum class Action { CONNECT, DISCONNECT, LOAD }

    fun failure(cause: Throwable, action: Action): String = when {
        action == Action.CONNECT && cause is IllegalArgumentException ->
            "Địa chỉ nguồn nhạc chưa hợp lệ. Hãy dùng địa chỉ HTTPS được cấp quyền."
        cause is IOException ->
            "Chưa kết nối được nhạc online. Hãy kiểm tra Internet rồi thử lại."
        action == Action.CONNECT ->
            "Chưa lưu được nguồn nhạc. Hãy kiểm tra địa chỉ rồi thử lại."
        action == Action.DISCONNECT ->
            "Chưa ngắt được nguồn nhạc. Hãy thử lại."
        else ->
            "Chưa tải được danh mục nhạc online. Hãy chọn Tải lại sau."
    }
}
