package com.example.musicflow.core.account

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.musicflow.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Uses only the signed-in person's JWT and the publishable client key. */
internal class CreatorPrivateStorage(
    private val context: Context,
    private val token: () -> String?,
    private val rpc: suspend (String, JSONObject) -> String
) {
    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    suspend fun upload(draftId: String, kind: String, uri: Uri) = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val declaredSize = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.SIZE), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
        if (declaredSize != null && declaredSize > CreatorPrivateFileRules.MAX_BYTES) {
            error("Tệp vượt 6 MB. Hãy chọn tệp nhỏ hơn.")
        }
        val bytes = ByteArrayOutputStream().use { buffer ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                val chunk = ByteArray(8192)
                while (true) {
                    val count = input.read(chunk)
                    if (count == -1) break
                    check(buffer.size() + count <= CreatorPrivateFileRules.MAX_BYTES) {
                        "Tệp vượt 6 MB. Hãy chọn tệp nhỏ hơn."
                    }
                    buffer.write(chunk, 0, count)
                }
            } ?: error("Không mở được tệp đã chọn.")
            buffer.toByteArray()
        }
        check(CreatorPrivateFileRules.allowed(kind, mime, bytes.size.toLong())) {
            "Chỉ nhận audio được hỗ trợ hoặc PDF/JPG/PNG, tối đa 6 MB."
        }
        // Let normal RPC refresh a short-lived session before transferring bytes.
        rpc("musicflow_my_access", JSONObject())
        val reservation = JSONObject(rpc(
            "musicflow_creator_upload_reserve",
            JSONObject().put("p_draft_id", draftId).put("p_kind", kind)
                .put("p_mime_type", mime).put("p_byte_count", bytes.size)
        ))
        check(reservation.getString("bucket") == CreatorPrivateFileRules.BUCKET)
        val path = reservation.getString("path")
        check(CreatorPrivateFileRules.validServerPath(path)) { "Đường dẫn lưu tệp không hợp lệ." }
        val tokenValue = token() ?: error("Phiên đăng nhập đã hết hạn.")
        val connection = URL(
            "$base/storage/v1/object/${CreatorPrivateFileRules.BUCKET}/$path"
        ).openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.setRequestProperty("apikey", key)
            connection.setRequestProperty("Authorization", "Bearer $tokenValue")
            connection.setRequestProperty("Content-Type", mime)
            connection.setRequestProperty("x-upsert", "false")
            connection.outputStream.use { it.write(bytes) }
            val code = connection.responseCode
            // Upload may succeed while confirmation fails (network switch).
            check(code in 200..299 || code == 409) {
                "Không tải lên được tệp (HTTP $code). Kiểm tra kết nối và thử lại."
            }
        } finally {
            connection.disconnect()
        }
        rpc(
            "musicflow_creator_upload_confirm",
            JSONObject().put("p_id", reservation.getString("id"))
        )
        Unit
    }

    suspend fun download(path: String, destination: Uri) = withContext(Dispatchers.IO) {
        check(CreatorPrivateFileRules.validServerPath(path)) { "Tệp không hợp lệ." }
        rpc("musicflow_my_access", JSONObject())
        val tokenValue = token() ?: error("Phiên đăng nhập đã hết hạn.")
        val connection = URL(
            "$base/storage/v1/object/authenticated/${CreatorPrivateFileRules.BUCKET}/$path"
        ).openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", key)
            connection.setRequestProperty("Authorization", "Bearer $tokenValue")
            check(connection.responseCode in 200..299) {
                "Không tải được tệp riêng tư. Hãy kiểm tra quyền và phiên đăng nhập."
            }
            val buffer = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val chunk = ByteArray(8192)
                while (true) {
                    val n = input.read(chunk)
                    if (n == -1) break
                    check(buffer.size() + n <= CreatorPrivateFileRules.MAX_BYTES) {
                        "Tệp vượt giới hạn bản demo."
                    }
                    buffer.write(chunk, 0, n)
                }
            }
            context.contentResolver.openOutputStream(destination, "w")?.use {
                it.write(buffer.toByteArray())
            } ?: error("Không ghi được tệp đến nơi đã chọn.")
        } finally {
            connection.disconnect()
        }
    }
}

