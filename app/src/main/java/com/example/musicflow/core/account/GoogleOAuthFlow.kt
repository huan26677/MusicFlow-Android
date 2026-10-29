package com.example.musicflow.core.account

import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Pure PKCE + callback validation: no Google tokens or client secret in the app. */
object GoogleOAuthFlow {
    const val REDIRECT_URI = "musicflow://auth/callback"
    private const val MAX_AGE_MS = 10 * 60 * 1000L
    private val random = SecureRandom()

    data class Pending(
        val verifier: String,
        val nonce: String,
        val createdAtMs: Long
    )

    fun create(nowMs: Long = System.currentTimeMillis()): Pending =
        Pending(randomBase64(32), randomBase64(24), nowMs)

    private fun randomBase64(size: Int): String {
        val value = ByteArray(size)
        random.nextBytes(value)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value)
    }

    fun challenge(verifier: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256")
                .digest(verifier.toByteArray(Charsets.US_ASCII))
        )

    fun authorizeUrl(projectUrl: String, pending: Pending): String {
        val base = URI(projectUrl.trimEnd('/'))
        require(base.scheme == "https" && !base.host.isNullOrBlank())
        require(pending.verifier.length in 43..128 && pending.nonce.isNotBlank())
        val redirect = "$REDIRECT_URI?flow=${pending.nonce}"
        return projectUrl.trimEnd('/') + "/auth/v1/authorize?" +
            "provider=google" +
            "&redirect_to=" + encode(redirect) +
            "&code_challenge=" + encode(challenge(pending.verifier)) +
            "&code_challenge_method=s256"
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    fun isCallback(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "musicflow" && uri.host == "auth" &&
            uri.path == "/callback" && uri.fragment == null &&
            uri.userInfo == null
    }.getOrDefault(false)

    /** Require matching one-time flow nonce, fresh PKCE verifier and a code. */
    fun validate(
        url: String,
        pending: Pending?,
        nowMs: Long = System.currentTimeMillis()
    ): String {
        require(isCallback(url)) { "Đường dẫn đăng nhập Google không hợp lệ." }
        require(pending != null && nowMs >= pending.createdAtMs &&
            nowMs - pending.createdAtMs <= MAX_AGE_MS
        ) { "Phiên đăng nhập Google đã hết hạn. Hãy thử lại." }
        val values = URI(url).rawQuery.orEmpty()
            .split('&')
            .mapNotNull { item ->
                val split = item.indexOf('=')
                if (split <= 0) null else {
                    val key = java.net.URLDecoder.decode(item.substring(0, split), "UTF-8")
                    val value = java.net.URLDecoder.decode(item.substring(split + 1), "UTF-8")
                    key to value
                }
            }
        val flow = values.filter { it.first == "flow" }.map { it.second }
        require(flow.size == 1 && MessageDigest.isEqual(
            flow.single().toByteArray(Charsets.UTF_8),
            pending.nonce.toByteArray(Charsets.UTF_8)
        )) { "Xác thực Google không khớp phiên yêu cầu." }
        if (values.any { it.first == "error" }) {
            throw IllegalArgumentException("Đăng nhập Google đã bị hủy hoặc từ chối.")
        }
        val code = values.filter { it.first == "code" }.map { it.second }
        require(code.size == 1 && code.single().isNotBlank() &&
            code.single().length <= 2048
        ) { "Google không trả về mã đăng nhập hợp lệ." }
        return code.single()
    }
}
