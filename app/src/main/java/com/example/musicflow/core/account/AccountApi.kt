package com.example.musicflow.core.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.musicflow.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection
import java.security.KeyStore

data class AccountIdentity(
    val userId: String,
    val email: String,
    val displayName: String,
    val tier: String,
    val expiresAt: String?
)

/**
 * Encrypts only the rotating refresh token. Passwords and service keys
 * are NEVER persisted. Guest mode doesn't create an Auth user.
 */
internal class AccountTokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "musicflow_account_session_v1", Context.MODE_PRIVATE
    )
    private val alias = "musicflow_refresh_token_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun save(refreshToken: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encoded = Base64.encodeToString(
            cipher.iv + cipher.doFinal(refreshToken.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
        check(prefs.edit().putString("refresh", encoded).commit())
    }

    fun read(): String? {
        val value = prefs.getString("refresh", null) ?: return null
        return try {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            require(bytes.size > 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE, key(),
                GCMParameterSpec(128, bytes.copyOfRange(0, 12))
            )
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun clear() {
        prefs.edit().remove("refresh").commit()
    }
}

class AccountApi(context: Context) {
    private val store = AccountTokenStore(context)
    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private var accessToken: String? = null

    val isConfigured: Boolean
        get() = base.startsWith("https://") && publishableKey.startsWith("sb_publishable_")

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = false
    ): String {
        check(isConfigured) { "Chưa cấu hình Supabase cho ứng dụng." }
        val connection = URL(base + path).openConnection() as HttpsURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("apikey", publishableKey)
        connection.setRequestProperty("Accept", "application/json")
        if (authenticated) {
            val token = accessToken ?: error("Phiên đăng nhập đã hết hạn.")
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        if (body != null) {
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
        }
        try {
            if (body != null) {
                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                val label = when (code) {
                    400, 401 -> "Email, mật khẩu hoặc phiên đăng nhập không hợp lệ."
                    403 -> "Không có quyền truy cập tài nguyên này."
                    422 -> "Email hoặc mật khẩu không hợp lệ hoặc đã được đăng ký."
                    429 -> "Thao tác quá nhanh. Hãy thử lại sau."
                    else -> "Máy chủ trả về lỗi HTTP $code."
                }
                error(label)
            }
            return connection.inputStream.use { input ->
                val result = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    require(result.size() + size <= 256 * 1024) {
                        "Phản hồi máy chủ quá lớn."
                    }
                    result.write(buffer, 0, size)
                }
                result.toString("UTF-8")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun applySession(reply: JSONObject): Boolean {
        val access = reply.optString("access_token")
        val refresh = reply.optString("refresh_token")
        if (access.isBlank() || refresh.isBlank()) return false
        store.save(refresh)
        accessToken = access
        return true
    }

    suspend fun signUp(email: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val response = JSONObject(request(
                "POST", "/auth/v1/signup",
                JSONObject().put("email", email).put("password", password)
            ))
            applySession(response)
        }

    suspend fun signIn(email: String, password: String): AccountIdentity =
        withContext(Dispatchers.IO) {
            val response = JSONObject(request(
                "POST", "/auth/v1/token?grant_type=password",
                JSONObject().put("email", email).put("password", password)
            ))
            check(applySession(response)) { "Không nhận được phiên đăng nhập." }
            currentAccount()
        }

    suspend fun restore(): AccountIdentity? = withContext(Dispatchers.IO) {
        val refresh = store.read() ?: return@withContext null
        try {
            val response = JSONObject(request(
                "POST", "/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refresh)
            ))
            check(applySession(response)) { "Không thể khôi phục phiên." }
            currentAccount()
        } catch (error: Exception) {
            // Keep token on transient network errors; clear on invalid token
            // when server explicitly rejects refresh.
            if (error.message?.contains("không hợp lệ", true) == true) {
                store.clear()
            }
            accessToken = null
            throw error
        }
    }

    suspend fun currentAccount(): AccountIdentity =
        withContext(Dispatchers.IO) {
            val user = JSONObject(request("GET", "/auth/v1/user", authenticated = true))
            val id = UUID.fromString(user.getString("id")).toString()
            val email = user.optString("email").trim()
            val profilePath = "/rest/v1/account_profiles?id=eq.$id&select=display_name"
            var profiles = JSONArray(request("GET", profilePath, authenticated = true))
            if (profiles.length() == 0) {
                request(
                    "POST", "/rest/v1/account_profiles",
                    JSONObject().put("id", id).put("display_name", ""),
                    authenticated = true
                )
                profiles = JSONArray(request("GET", profilePath, authenticated = true))
            }
            val name = profiles.optJSONObject(0)?.optString("display_name").orEmpty()
            val entitlements = JSONArray(request(
                "GET",
                "/rest/v1/account_entitlements?user_id=eq.$id&select=tier,expires_at",
                authenticated = true
            ))
            val entitlement = entitlements.optJSONObject(0)
            val expiry = entitlement?.optString("expires_at").orEmpty()
            AccountIdentity(
                userId = id,
                email = email,
                displayName = name,
                tier = AccountTier.activeTier(
                    entitlement?.optString("tier"),
                    expiry
                ),
                expiresAt = expiry.ifBlank { null }
            )
        }

    suspend fun changeDisplayName(value: String): AccountIdentity =
        withContext(Dispatchers.IO) {
            require(value.length <= 80) { "Tên hiển thị không quá 80 ký tự." }
            val account = currentAccount()
            request(
                "PATCH",
                "/rest/v1/account_profiles?id=eq.${account.userId}",
                JSONObject().put("display_name", value.trim()),
                authenticated = true
            )
            currentAccount()
        }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            if (accessToken != null) {
                request("POST", "/auth/v1/logout", authenticated = true)
            }
        } catch (_: Exception) {
            // Local sign-out always succeeds even if network is unavailable.
        } finally {
            accessToken = null
            store.clear()
        }
    }
}
