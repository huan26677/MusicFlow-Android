package com.example.musicflow.core.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class OnlineCatalogTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val rightsNotice: String
)

/**
 * No API keys, stream URLs or credentials are stored by the catalog foundation.
 * The owner must configure an HTTPS API that serves authorized content.
 */
class OnlineCatalogConfiguration(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "musicflow_online_catalog_v1", Context.MODE_PRIVATE
    )

    fun baseUrl(): String = preferences.getString("base_url", null).orEmpty()

    fun saveBaseUrl(value: String) {
        val sanitized = normalizeBaseUrl(value)
        check(preferences.edit().putString("base_url", sanitized).commit()) {
            "Không thể lưu cấu hình máy chủ."
        }
    }

    companion object {
        fun normalizeBaseUrl(raw: String): String {
            val value = raw.trim()
            if (value.isEmpty()) return ""
            require(value.length <= 255) { "Địa chỉ máy chủ quá dài." }
            val uri = try {
                URI(value)
            } catch (_: Exception) {
                throw IllegalArgumentException("Địa chỉ HTTPS không hợp lệ.")
            }
            require(
                uri.scheme.equals("https", ignoreCase = true) &&
                    !uri.host.isNullOrBlank() &&
                    uri.userInfo == null &&
                    uri.rawQuery == null &&
                    uri.rawFragment == null
            ) { "Chỉ chấp nhận URL HTTPS không có tài khoản, query hoặc fragment." }
            require(uri.port in -1..65535 && uri.port != 0) {
                "Cổng HTTPS không hợp lệ."
            }
            return value.trimEnd('/') + "/"
        }
    }
}

interface OnlineCatalogRepository {
    suspend fun loadTracks(): List<OnlineCatalogTrack>
}

/**
 * Contract: GET <baseUrl>/v1/tracks?limit=30
 * Response: {"items":[{"id":"...","title":"...","artist":"...",
 *  "album":"...","durationMs":1234,"rightsNotice":"..."}]}
 */
class HttpsOnlineCatalogRepository(
    private val configuration: OnlineCatalogConfiguration
) : OnlineCatalogRepository {

    override suspend fun loadTracks(): List<OnlineCatalogTrack> =
        withContext(Dispatchers.IO) {
            val base = OnlineCatalogConfiguration.normalizeBaseUrl(
                configuration.baseUrl()
            )
            require(base.isNotEmpty()) {
                "Chưa cấu hình máy chủ nhạc online."
            }

            val url = URL(base + "v1/tracks?limit=30")
            val connection = url.openConnection() as HttpsURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")

                val status = connection.responseCode
                if (status != 200) {
                    throw IllegalStateException(
                        "Máy chủ trả về HTTP $status. Hãy kiểm tra API."
                    )
                }

                val mime = connection.contentType.orEmpty()
                require(mime.substringBefore(';').trim()
                    .equals("application/json", ignoreCase = true)
                ) { "Máy chủ không trả về JSON." }

                val bytes = ByteArrayOutputStream()
                connection.inputStream.use { stream ->
                    val chunk = ByteArray(8192)
                    while (true) {
                        val count = stream.read(chunk)
                        if (count < 0) break
                        require(bytes.size() + count <= 512 * 1024) {
                            "Danh mục vượt giới hạn 512 KB."
                        }
                        bytes.write(chunk, 0, count)
                    }
                }

                parseCatalog(bytes.toString(Charsets.UTF_8.name()))
            } finally {
                connection.disconnect()
            }
        }

    companion object {
        fun parseCatalog(content: String): List<OnlineCatalogTrack> {
            val items = JSONObject(content).getJSONArray("items")
            require(items.length() <= 100) {
                "Máy chủ trả về quá nhiều bài hát."
            }
            val seen = hashSetOf<String>()
            return buildList {
                for (index in 0 until items.length()) {
                    val item = items.getJSONObject(index)
                    val id = item.optString("id").trim()
                    val title = item.optString("title").trim()
                    if (id.isBlank() || title.isBlank() || !seen.add(id)) {
                        continue
                    }
                    add(
                        OnlineCatalogTrack(
                            id = id.take(128),
                            title = title.take(200),
                            artist = item.optString("artist")
                                .ifBlank { "Không rõ nghệ sĩ" }.take(200),
                            album = item.optString("album")
                                .ifBlank { "Không rõ album" }.take(200),
                            durationMs = item.optLong("durationMs", 0L)
                                .coerceIn(0L, 24L * 60L * 60L * 1000L),
                            rightsNotice = item.optString("rightsNotice")
                                .take(240)
                        )
                    )
                }
            }
        }
    }
}
