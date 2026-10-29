package com.example.musicflow.core.online

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * Metadata only. Streaming/download authorization is intentionally NOT
 * implemented in the catalog foundation milestone.
 */
data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val streamUrl: String? = null,
    val rights: OnlineTrackRights? = null
)

data class OnlineCatalogPage(
    val tracks: List<OnlineTrack>,
    val nextCursor: String?
)

interface OnlineCatalogRepository {
    suspend fun fetchPage(baseUrl: String, cursor: String? = null): OnlineCatalogPage
}

/**
 * User-provided base URL of a licensed catalog backend.
 * Requests are HTTPS-only and never include stored credentials.
 */
object OnlineCatalogEndpoint {
    fun normalize(input: String): String {
        val raw = input.trim()
        require(raw.isNotEmpty() && raw.length <= 500) {
            "Địa chỉ máy chủ phải có từ 1 đến 500 ký tự."
        }
        val uri = try {
            URI(raw)
        } catch (_: Exception) {
            throw IllegalArgumentException("Địa chỉ máy chủ không hợp lệ.")
        }
        require(
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.rawQuery == null &&
                uri.rawFragment == null
        ) {
            "Chỉ chấp nhận URL HTTPS, không chứa tài khoản, query hoặc fragment."
        }
        return uri.toASCIIString().trimEnd('/')
    }
}

class OnlineCatalogSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "musicflow_online_catalog_v1",
        Context.MODE_PRIVATE
    )

    fun savedEndpoint(): String =
        preferences.getString("baseUrl", null).orEmpty()

    fun saveEndpoint(input: String): String {
        val normalized = OnlineCatalogEndpoint.normalize(input)
        check(preferences.edit().putString("baseUrl", normalized).commit()) {
            "Không thể lưu địa chỉ máy chủ."
        }
        return normalized
    }

    fun clear() {
        check(preferences.edit().remove("baseUrl").commit()) {
            "Không thể xóa địa chỉ máy chủ."
        }
    }
}

class HttpsOnlineCatalogRepository : OnlineCatalogRepository {
    override suspend fun fetchPage(
        baseUrl: String,
        cursor: String?
    ): OnlineCatalogPage = withContext(Dispatchers.IO) {
        val base = OnlineCatalogEndpoint.normalize(baseUrl)
        require(cursor == null || cursor.length in 1..256) {
            "Mã trang tiếp theo không hợp lệ."
        }
        val query = cursor?.let {
            "?cursor=" + URLEncoder.encode(it, "UTF-8")
        }.orEmpty()
        val address = URL("$base/v1/catalog/tracks$query")
        val connection = (address.openConnection() as HttpsURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-store")
        }
        try {
            coroutineContext.ensureActive()
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException(
                    "Máy chủ trả về HTTP $status. Kiểm tra địa chỉ/API."
                )
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    coroutineContext.ensureActive()
                    val count = input.read(buffer)
                    if (count == -1) break
                    if (output.size() + count > 1024 * 1024) {
                        throw IllegalStateException("Trang dữ liệu vượt quá 1 MB.")
                    }
                    output.write(buffer, 0, count)
                }
            }
            coroutineContext.ensureActive()
            decodePage(output.toByteArray().toString(Charsets.UTF_8))
        } finally {
            connection.disconnect()
        }
    }

    private fun decodePage(raw: String): OnlineCatalogPage {
        val json = try {
            JSONObject(raw)
        } catch (_: Exception) {
            throw IllegalArgumentException("Máy chủ không trả về JSON hợp lệ.")
        }
        val items = json.optJSONArray("items")
            ?: throw IllegalArgumentException("JSON thiếu danh sách items.")
        require(items.length() <= 100) {
            "Một trang chỉ được chứa tối đa 100 bài hát."
        }
        val results = mutableListOf<OnlineTrack>()
        val seenIds = mutableSetOf<String>()
        for (index in 0 until items.length()) {
            val entry = items.optJSONObject(index)
                ?: throw IllegalArgumentException("Bản ghi $index không hợp lệ.")
            val id = entry.optString("id").trim().take(200)
            val title = entry.optString("title").trim().take(240)
            val artist = entry.optString("artist").trim().take(180)
            require(id.isNotEmpty() && title.isNotEmpty()) {
                "Bài hát cần có id và title."
            }
            if (seenIds.add(id)) {
                results.add(
                    OnlineTrack(
                        id = id,
                        title = title,
                        artist = artist.ifBlank { "Không rõ nghệ sĩ" },
                        album = if (entry.isNull("album")) {
                            null
                        } else {
                            entry.optString("album")
                                .trim().take(180).ifBlank { null }
                        },
                        durationMs = entry.optLong("durationMs", 0L)
                            .coerceIn(0L, 24 * 60 * 60 * 1000L),
                        streamUrl = if (entry.isNull("streamUrl")) null
                            else entry.optString("streamUrl").trim().takeIf { it.isNotBlank() },
                        rights = OnlineRightsJson.fromExternal(entry)
                    )
                )
            }
        }
        val next = if (json.isNull("nextCursor")) {
            null
        } else {
            json.optString("nextCursor")
                .trim().takeIf { it.isNotEmpty() }
        }
        require(next == null || next.length <= 256) {
            "Mã phân trang vượt giới hạn."
        }
        return OnlineCatalogPage(results, next)
    }
}
