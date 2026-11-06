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
    val expiresAt: String?,
    val access: MusicFlowAccess = MusicFlowAccess.NONE
)

/**
 * Encrypts refresh token and short-lived PKCE state. No passwords or admin keys.
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

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(
            cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
    }

    private fun decrypt(value: String): String? {
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
            null
        }
    }

    fun save(refreshToken: String) {
        check(prefs.edit().putString("refresh", encrypt(refreshToken)).commit())
    }

    fun read(): String? {
        val raw = prefs.getString("refresh", null) ?: return null
        return decrypt(raw).also {
            if (it == null) prefs.edit().remove("refresh").apply()
        }
    }

    fun saveGooglePending(pending: GoogleOAuthFlow.Pending) {
        val content = listOf(
            pending.verifier, pending.nonce, pending.createdAtMs.toString()
        ).joinToString("|")
        check(prefs.edit().putString("google_pending", encrypt(content)).commit())
    }

    fun takeGooglePending(): GoogleOAuthFlow.Pending? {
        val raw = prefs.getString("google_pending", null)
        prefs.edit().remove("google_pending").commit()
        val values = raw?.let { decrypt(it) }?.split('|') ?: return null
        if (values.size != 3) return null
        val created = values[2].toLongOrNull() ?: return null
        return GoogleOAuthFlow.Pending(values[0], values[1], created)
    }

    fun clearGooglePending() {
        prefs.edit().remove("google_pending").apply()
    }

    fun clear() {
        prefs.edit().remove("refresh").remove("google_pending").commit()
    }
}

class AccountApi(context: Context) {
    private val creatorStorage = CreatorPrivateStorage(
        context.applicationContext,
        token = { accessToken },
        rpc = { name, args -> roleRpc(name, args) }
    )
    private val store = AccountTokenStore(context)
    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    private var accessToken: String? = null

    val isConfigured: Boolean
        get() = base.startsWith("https://") && publishableKey.startsWith("sb_publishable_")

    suspend fun googleProviderEnabled(): Boolean = withContext(Dispatchers.IO) {
        JSONObject(request("GET", "/auth/v1/settings"))
            .optJSONObject("external")?.optBoolean("google", false) == true
    }

    /**
     * Supabase hosted Auth controls email confirmation on the server.
     * This read-only settings check cannot disable it from Android.
     * null = setting unavailable, true = user must confirm email.
     */
    suspend fun emailConfirmationRequired(): Boolean? = withContext(Dispatchers.IO) {
        val settings = JSONObject(request("GET", "/auth/v1/settings"))
        if (!settings.has("mailer_autoconfirm") ||
            settings.isNull("mailer_autoconfirm")
        ) null else !settings.getBoolean("mailer_autoconfirm")
    }

    fun prepareGoogleSignIn(): String {
        check(isConfigured) { "Supabase chưa được cấu hình." }
        val pending = GoogleOAuthFlow.create()
        store.saveGooglePending(pending)
        return GoogleOAuthFlow.authorizeUrl(base, pending)
    }

    /** If no browser can open, do not leave an active verifier on disk. */
    fun cancelGoogleSignIn() {
        store.clearGooglePending()
    }

    suspend fun finishGoogleSignIn(callbackUrl: String): AccountIdentity =
        withContext(Dispatchers.IO) {
            // One-time PKCE state: even a replayed callback cannot reuse it.
            val pending = store.takeGooglePending()
            val code = GoogleOAuthFlow.validate(callbackUrl, pending)
            val response = JSONObject(request(
                "POST", "/auth/v1/token?grant_type=pkce",
                JSONObject()
                    .put("auth_code", code)
                    .put("code_verifier", pending!!.verifier)
            ))
            check(applySession(response)) { "Không nhận được phiên Google hợp lệ." }
            currentAccount()
        }

    private fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        authenticated: Boolean = false,
        prefer: String? = null,
        allowRefresh: Boolean = true,
        maxResponseBytes: Int = 256 * 1024
    ): String {
        check(isConfigured) { "Chưa cấu hình Supabase cho ứng dụng." }
        val connection = URL(base + path).openConnection() as HttpsURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("apikey", publishableKey)
        connection.setRequestProperty("Accept", "application/json")
        if (prefer != null) connection.setRequestProperty("Prefer", prefer)
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
            if (code == 401 && authenticated && allowRefresh) {
                val refresh = store.read()
                if (refresh != null) {
                    val updated = runCatching {
                        val payload = JSONObject(request(
                            "POST", "/auth/v1/token?grant_type=refresh_token",
                            JSONObject().put("refresh_token", refresh),
                            allowRefresh = false
                        ))
                        applySession(payload)
                    }.getOrDefault(false)
                    if (updated) {
                        return request(
                            method, path, body, authenticated, prefer,
                            allowRefresh = false,
                            maxResponseBytes = maxResponseBytes
                        )
                    }
                }
            }
            if (code !in 200..299) {
                val authErrorCode = if (path.startsWith("/auth/v1/")) {
                    runCatching {
                        connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                            val buffer = CharArray(4096)
                            val length = reader.read(buffer)
                            if (length <= 0) null else {
                                val errorBody = JSONObject(String(buffer, 0, length))
                                val backendCode = errorBody.optString("code").ifBlank {
                                    errorBody.optString("error_code")
                                }
                                backendCode.takeIf { it.isNotBlank() }
                            }
                        }
                    }.getOrNull()
                } else {
                    null
                }
                error(AccountAuthErrorLabels.forResponse(code, authErrorCode))
            }
            return connection.inputStream.use { input ->
                val result = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    require(result.size() + size <= maxResponseBytes) {
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

    /**
     * Internal Data API access for opted-in cloud sync.
     * JWT comes from Auth, never from a caller or an untrusted URI.
     */
    internal fun cloudRequest(
        method: String,
        path: String,
        body: JSONObject? = null,
        prefer: String? = null
    ): String = request(
        method, path, body, authenticated = true, prefer = prefer,
        maxResponseBytes = 4 * 1024 * 1024
    )

    /** Fixed allow-list. Every mutation must also pass server-side role checks. */
    suspend fun roleRpc(name: String, args: JSONObject = JSONObject()): String =
        withContext(Dispatchers.IO) {
            require(name in setOf(
                "musicflow_my_access",
                "musicflow_admin_users",
                "musicflow_manage_role",
                "musicflow_set_admin_permission",
                "musicflow_apply_creator",
                "musicflow_my_creator_requests",
                "musicflow_pending_creators",
                "musicflow_review_creator",
                "musicflow_my_creator_profile",
                "musicflow_save_creator_profile",
                "musicflow_admin_tracks",
                "musicflow_review_track",
                "musicflow_owner_audit",
                "musicflow_creator_draft_save",
                "musicflow_creator_my_drafts",
                "musicflow_creator_submit_draft",
                "musicflow_pending_drafts",
                "musicflow_review_draft",
                "musicflow_creator_upload_reserve",
                "musicflow_creator_upload_confirm",
                "musicflow_creator_upload_list",
                "musicflow_owner_demo_summary"
            )) { "Chức năng chưa được hỗ trợ." }
            cloudRequest("POST", "/rest/v1/rpc/$name", args)
        }

    suspend fun uploadCreatorFile(draftId: String, kind: String, uri: android.net.Uri) =
        creatorStorage.upload(draftId, kind, uri)

    suspend fun downloadCreatorFile(path: String, destination: android.net.Uri) =
        creatorStorage.download(path, destination)

    private fun applySession(reply: JSONObject): Boolean {
        val access = reply.optString("access_token")
        val refresh = reply.optString("refresh_token")
        if (access.isBlank() || refresh.isBlank()) return false
        store.save(refresh)
        accessToken = access
        store.clearGooglePending()
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

    suspend fun currentAccount(requireFreshRoles: Boolean = false): AccountIdentity =
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
            // Fail closed on missing role API / transient outages: NEVER infer
            // privileged roles from email, Premium tier or local metadata.
            val access = if (requireFreshRoles) {
                // Explicit user-triggered refresh must report authorization errors.
                // Only one roles RPC is needed; do not re-query the same endpoint.
                MusicFlowAccess.fromResponse(JSONObject(roleRpc("musicflow_my_access")))
            } else {
                // Sign-in/restore can still show listening tools if roles service is down.
                runCatching {
                    MusicFlowAccess.fromResponse(JSONObject(roleRpc("musicflow_my_access")))
                }.getOrDefault(MusicFlowAccess.NONE)
            }
            AccountIdentity(
                userId = id,
                email = email,
                displayName = name,
                tier = AccountTier.activeTier(
                    entitlement?.optString("tier"),
                    expiry
                ),
                expiresAt = expiry.ifBlank { null },
                access = access
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
