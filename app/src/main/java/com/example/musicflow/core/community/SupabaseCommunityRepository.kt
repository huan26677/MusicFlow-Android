package com.example.musicflow.core.community

import android.content.Context
import com.example.musicflow.BuildConfig
import com.example.musicflow.core.account.AccountApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Community is bound only to the canonical, rights-reviewed Supabase UUID. */
class SupabaseCommunityRepository(context: Context) {
    private val account = AccountApi(context.applicationContext)
    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    suspend fun read(trackId: String, offset: Int = 0): CommunitySnapshot {
        require(CommunityRules.isCloudTrackId(trackId)) { "Bài hát thiếu mã hợp lệ." }
        require(offset in 0..1000 && offset % CommunityRules.PAGE_SIZE == 0)
        check(base.startsWith("https://") && publishableKey.startsWith("sb_publishable_")) {
            "Chưa cấu hình dịch vụ MusicFlow."
        }
        val args = JSONObject().put("p_track",trackId).put("p_offset",offset)
        val identity = account.restore()
        val data = if (identity == null) publicRead(args)
            else account.roleRpc("musicflow_community_read",args)
        return CommunitySnapshot.decode(JSONObject(data),identity != null)
    }

    suspend fun rate(trackId: String, stars: Int) {
        require(CommunityRules.isCloudTrackId(trackId) && CommunityRules.validStars(stars))
        write("musicflow_community_rate",
            JSONObject().put("p_track",trackId).put("p_stars",stars))
    }

    suspend fun create(trackId: String, body: String) {
        require(CommunityRules.isCloudTrackId(trackId))
        write("musicflow_community_comment_create",
            JSONObject().put("p_track",trackId)
                .put("p_body",CommunityRules.cleanComment(body)))
    }

    suspend fun edit(id: String, body: String) {
        write("musicflow_community_comment_edit",
            JSONObject().put("p_comment",id)
                .put("p_body",CommunityRules.cleanComment(body)))
    }

    suspend fun delete(id: String) {
        write("musicflow_community_comment_delete",
            JSONObject().put("p_comment",id))
    }

    suspend fun report(id: String, reason: String) {
        write("musicflow_community_report",
            JSONObject().put("p_comment",id)
                .put("p_reason",CommunityRules.cleanReason(reason)))
    }

    private suspend fun write(name: String, args: JSONObject) {
        // Revalidate session for every mutation; no demo/guest bypass.
        check(account.restore() != null) { "Đăng nhập tài khoản thật để tham gia." }
        account.roleRpc(name,args)
    }

    private suspend fun publicRead(args: JSONObject): String =
        withContext(Dispatchers.IO) {
            val connection = URL("$base/rest/v1/rpc/musicflow_community_read")
                .openConnection() as HttpsURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey",publishableKey)
            connection.setRequestProperty("Accept","application/json")
            connection.setRequestProperty("Content-Type","application/json")
            connection.doOutput = true
            try {
                val bytes = args.toString().toByteArray(Charsets.UTF_8)
                connection.outputStream.use { it.write(bytes) }
                if (connection.responseCode !in 200..299) {
                    error("Không tải được bình luận. Hãy thử lại.")
                }
                val response = connection.inputStream.bufferedReader().use {
                    it.readText().take(128 * 1024 + 1)
                }
                require(response.length <= 128 * 1024) { "Dữ liệu cộng đồng quá lớn." }
                response
            } finally {
                connection.disconnect()
            }
        }
}
