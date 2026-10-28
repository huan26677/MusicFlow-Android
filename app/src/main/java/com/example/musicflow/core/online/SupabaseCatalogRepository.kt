package com.example.musicflow.core.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Public, RLS-filtered catalog. The publishable key is NOT an admin secret. */
class SupabaseCatalogRepository(
    private val projectUrl: String,
    private val publishableKey: String
) : OnlineCatalogRepository {
    override suspend fun fetchPage(
        baseUrl: String,
        cursor: String?
    ): OnlineCatalogPage = withContext(Dispatchers.IO) {
        val offset = cursor?.toIntOrNull() ?: 0
        require(offset in 0..100_000) { "Trang danh mục không hợp lệ." }
        val base = OnlineCatalogEndpoint.normalize(projectUrl)
        val address = URL(
            "$base/rest/v1/music_tracks?select=id,title,artist,album,duration_ms,stream_url" +
                "&is_published=eq.true&rights_confirmed=eq.true" +
                "&order=created_at.desc,id.desc&limit=25&offset=$offset"
        )
        val connection = (address.openConnection() as HttpsURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = false
            setRequestProperty("apikey", publishableKey)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("Supabase HTTP $code. Kiểm tra RLS và API.")
            }
            val raw = connection.inputStream.bufferedReader().use {
                it.readText().take(1024 * 1024 + 1)
            }
            require(raw.length <= 1024 * 1024) { "Dữ liệu quá lớn." }
            val records = JSONArray(raw)
            val tracks = (0 until records.length()).map { index ->
                val item = records.getJSONObject(index)
                val url = item.optString("stream_url").trim()
                OnlineTrack(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    artist = item.optString("artist", "Không rõ nghệ sĩ"),
                    album = item.optString("album").ifBlank { null },
                    durationMs = item.optLong("duration_ms", 0L),
                    streamUrl = url.takeIf {
                        it.startsWith("https://", ignoreCase = true)
                    }
                )
            }
            OnlineCatalogPage(
                tracks,
                if (tracks.size == 25) (offset + 25).toString() else null
            )
        } finally {
            connection.disconnect()
        }
    }
}
