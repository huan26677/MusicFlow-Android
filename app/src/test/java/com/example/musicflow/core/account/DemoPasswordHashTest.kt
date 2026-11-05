package com.example.musicflow.core.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoPasswordHashTest {
    private val salt = ByteArray(16) { it.toByte() }

    @Test fun verifiesCorrectPasswordWithoutSavingPlaintext() {
        val stored = DemoPasswordHash.derive("demo-secret-123".toCharArray(), salt)
        assertTrue(DemoPasswordHash.verify("demo-secret-123".toCharArray(), salt, stored))
        assertFalse(stored.contentEquals("demo-secret-123".toByteArray()))
    }

    @Test fun rejectsIncorrectPassword() {
        val stored = DemoPasswordHash.derive("demo-secret-123".toCharArray(), salt)
        assertFalse(DemoPasswordHash.verify("wrong-secret".toCharArray(), salt, stored))
    }

    @Test fun saltChangesStoredHash() {
        val stored = DemoPasswordHash.derive("demo-secret-123".toCharArray(), salt)
        val otherSalt = ByteArray(16) { (it + 3).toByte() }
        assertFalse(stored.contentEquals(
            DemoPasswordHash.derive("demo-secret-123".toCharArray(), otherSalt)
        ))
    }

    @Test fun rejectsInvalidHashLength() {
        assertFalse(DemoPasswordHash.verify("demo-secret-123".toCharArray(), salt, byteArrayOf()))
    }
}
