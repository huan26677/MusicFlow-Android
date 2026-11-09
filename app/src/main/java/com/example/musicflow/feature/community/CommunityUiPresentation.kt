package com.example.musicflow.feature.community

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Presentation only. Never converts Demo text into real timestamps. */
object CommunityUiPresentation {
    fun dateLabel(raw: String, zone: ZoneId = ZoneId.systemDefault()): String {
        if (raw == "Vừa xong" || raw == "Dữ liệu mẫu") return raw
        return runCatching {
            DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
                .withZone(zone).format(Instant.parse(raw))
        }.getOrElse { "Không rõ thời gian" }
    }

    fun errorLabel(failure: Throwable, fallback: String): String {
        val text = failure.message.orEmpty()
        return when {
            text.contains("Đăng nhập", ignoreCase = true) ->
                "Hãy đăng nhập tài khoản trực tuyến ở mục Tài khoản rồi thử lại."
            text.contains("500 ký tự") || text.contains("300 ký tự") ->
                text
            text.contains("quá nhanh", ignoreCase = true) ->
                "Bạn vừa gửi bình luận. Hãy thử lại sau ít giây."
            text.contains("timeout", ignoreCase = true) ||
                text.contains("network", ignoreCase = true) ||
                text.contains("connection", ignoreCase = true) ->
                "Không thể kết nối. Hãy kiểm tra Internet và thử lại."
            else -> fallback
        }
    }
}
