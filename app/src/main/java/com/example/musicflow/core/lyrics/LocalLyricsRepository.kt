package com.example.musicflow.core.lyrics

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class LyricsUiState(
    val trackUri: String? = null,
    val lines: List<LyricLine> = emptyList(),
    val error: String? = null
)

object LrcParser {
    private val timestamp = Regex(
        """\[(\d{1,3}):([0-5]\d)(?:[.:](\d{1,3}))?\]"""
    )

    private val offsetTag = Regex(
        """(?i)^\[offset:([+-]?\d+)\]$"""
    )

    fun parse(content: String): List<LyricLine> {
        val lines = content.removePrefix("\uFEFF")
            .lineSequence()
            .take(5000)
            .toList()

        val offset = lines.firstNotNullOfOrNull {
            offsetTag.matchEntire(it.trim())
                ?.groupValues?.get(1)?.toLongOrNull()
        } ?: 0L

        return lines.flatMap { raw ->
            val matches = timestamp.findAll(raw).toList()
            val text = timestamp.replace(raw, "")
                .trim()
                .take(400)

            if (matches.isEmpty() || text.isBlank()) {
                emptyList()
            } else {
                matches.map { match ->
                    val minutes =
                        match.groupValues[1].toLong()

                    val seconds =
                        match.groupValues[2].toLong()

                    val fraction = match.groupValues[3]
                        .takeIf { it.isNotEmpty() }
                        ?.padEnd(3, '0')
                        ?.toLongOrNull() ?: 0L

                    LyricLine(
                        timeMs = (
                            minutes * 60_000L +
                            seconds * 1000L +
                            fraction + offset
                        ).coerceAtLeast(0L),
                        text = text
                    )
                }
            }
        }.sortedBy { it.timeMs }
    }
}

class LocalLyricsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    private val preferences =
        appContext.getSharedPreferences(
            "musicflow_local_lyrics_v1",
            Context.MODE_PRIVATE
        )

    private fun key(audioUri: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(audioUri.toByteArray(Charsets.UTF_8))

        return "track_" + bytes.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    private fun read(uri: Uri): List<LyricLine> {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)

        val input = resolver.openInputStream(uri)
            ?: error("Không thể mở file lời bài hát")

        input.use { stream ->
            while (true) {
                val count = stream.read(buffer)

                if (count < 0) break

                if (output.size() + count > 256 * 1024) {
                    error("File LRC vượt giới hạn 256 KB")
                }

                output.write(buffer, 0, count)
            }
        }

        val content = output.toByteArray()
            .toString(Charsets.UTF_8)

        val lyrics = LrcParser.parse(content)

        require(lyrics.isNotEmpty()) {
            "File không chứa lời bài hát có timestamp"
        }

        return lyrics
    }

    fun load(audioUri: String): List<LyricLine> {
        val saved = preferences.getString(
            key(audioUri), null
        ) ?: return emptyList()

        return read(Uri.parse(saved))
    }

    fun import(
        audioUri: String,
        lyricsUri: Uri
    ): List<LyricLine> {
        // OpenDocument cấp quyền có thể lưu lâu dài.
        resolver.takePersistableUriPermission(
            lyricsUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        // Chỉ ghi nhớ file sau khi đọc và parse thành công.
        val lines = read(lyricsUri)

        check(
            preferences.edit()
                .putString(
                    key(audioUri),
                    lyricsUri.toString()
                )
                .commit()
        ) {
            "Không thể lưu liên kết lời bài hát"
        }

        return lines
    }
}