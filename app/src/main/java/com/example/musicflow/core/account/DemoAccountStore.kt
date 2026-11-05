package com.example.musicflow.core.account

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * A separate, ONE-DEVICE demo login for the school/report APK.
 *
 * Never grants a Supabase access token, cloud identity, admin role or Premium.
 * Passwords are not stored; only a salted, slow password hash is persisted.
 */
internal object DemoPasswordHash {
    private const val ITERATIONS = 120_000
    fun derive(password: CharArray, salt: ByteArray): ByteArray {
        require(salt.size == 16)
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: CharArray, salt: ByteArray, expected: ByteArray): Boolean =
        expected.size == 32 &&
            MessageDigest.isEqual(derive(password, salt), expected)
}

class DemoAccountStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "musicflow_local_demo_account_v1", Context.MODE_PRIVATE
    )

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    private fun identity(): AccountIdentity {
        val userId = prefs.getString("id", null)
            ?: error("Không tìm thấy mã tài khoản Demo.")
        val email = prefs.getString("email", null)
            ?: error("Không tìm thấy email Demo.")
        return AccountIdentity(
            userId = "demo-local:$userId",
            email = email,
            displayName = prefs.getString("name", "").orEmpty(),
            tier = "free",
            expiresAt = null
        )
    }

    fun exists(): Boolean = prefs.contains("id")

    fun restore(): AccountIdentity? =
        if (prefs.getBoolean("signed_in", false) && exists()) identity() else null

    @Synchronized
    fun register(email: String, password: String): AccountIdentity {
        require(!exists()) {
            "Đã có tài khoản Demo trên thiết bị này. Hãy chọn Đăng nhập Demo."
        }
        val normalized = email.trim().lowercase(Locale.ROOT)
        require(normalized.length <= 254 && normalized.contains('@')) {
            "Hãy nhập địa chỉ email Demo hợp lệ."
        }
        require(password.length in 6..256) { "Mật khẩu Demo phải từ 6 đến 256 ký tự." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hashed = DemoPasswordHash.derive(password.toCharArray(), salt)
        check(prefs.edit()
            .putString("id", UUID.randomUUID().toString())
            .putString("email", normalized)
            .putString("name", "")
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hashed, Base64.NO_WRAP))
            .putBoolean("signed_in", true)
            .commit()
        ) { "Không lưu được tài khoản Demo trên thiết bị." }
        return identity()
    }

    @Synchronized
    fun signIn(email: String, password: String): AccountIdentity {
        require(exists()) { "Chưa có tài khoản Demo. Hãy chọn Đăng ký Demo." }
        val storedEmail = prefs.getString("email", "").orEmpty()
        val salt = prefs.getString("salt", null)
        val hash = prefs.getString("hash", null)
        val correct = salt != null && hash != null &&
            runCatching {
                DemoPasswordHash.verify(password.toCharArray(), decode(salt), decode(hash))
            }.getOrDefault(false)
        require(storedEmail.equals(email.trim(), ignoreCase = true) && correct) {
            "Email hoặc mật khẩu Demo không đúng."
        }
        check(prefs.edit().putBoolean("signed_in", true).commit())
        return identity()
    }

    fun changeDisplayName(value: String): AccountIdentity {
        require(prefs.getBoolean("signed_in", false) && exists())
        require(value.length <= 80) { "Tên hiển thị không quá 80 ký tự." }
        check(prefs.edit().putString("name", value.trim()).commit())
        return identity()
    }

    fun signOut() {
        check(prefs.edit().putBoolean("signed_in", false).commit())
    }
}
