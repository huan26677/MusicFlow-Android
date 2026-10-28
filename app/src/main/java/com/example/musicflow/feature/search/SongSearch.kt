
package com.example.musicflow.feature.search

import com.example.musicflow.core.media.DeviceSong
import java.text.Normalizer
import java.util.Locale

object SongSearch {

    private val accentPattern = Regex("\\p{M}+")
    private val whitespacePattern = Regex("\\s+")

    /**
     * Trả về true nếu tên bài hoặc nghệ sĩ
     * chứa tất cả các từ khóa cần tìm.
     */
    fun matches(
        title: String,
        artist: String,
        query: String
    ): Boolean {
        return matchesText("$title $artist", query)
    }

    /**
     * Search group titles with the same accent-insensitive matching
     * already used by the original song search.
     */
    fun matchesText(text: String, query: String): Boolean {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return false
        val searchableText = normalize(text)
        return normalizedQuery.split(' ').all { keyword ->
            searchableText.contains(keyword)
        }
    }

    /**
     * Lọc danh sách nhạc thiết bị.
     * Giữ nguyên thứ tự của danh sách gốc.
     */
    fun filter(
        songs: List<DeviceSong>,
        query: String
    ): List<DeviceSong> {
        if (query.isBlank()) {
            return emptyList()
        }

        return songs.filter { song ->
            matches(
                title = song.title,
                artist = song.artist,
                query = query
            )
        }
    }

    /**
     * Chuẩn hóa từ khóa tiếng Việt:
     * - Chuyển về chữ thường
     * - Loại bỏ dấu thanh và dấu chữ
     * - Chuyển đ thành d
     * - Gộp khoảng trắng liên tiếp
     */
    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(
            text,
            Normalizer.Form.NFD
        )

        return decomposed
            .replace(accentPattern, "")
            .lowercase(Locale.ROOT)
            .replace('đ', 'd')
            .replace(whitespacePattern, " ")
            .trim()
    }
}
