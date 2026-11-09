package com.example.musicflow.feature.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AccountFriendlyErrorsTest {
    @Test fun incorrectPasswordHasFriendlyNextStep() {
        assertEquals("Email hoặc mật khẩu chưa đúng. Hãy kiểm tra và thử lại.",
            AccountFriendlyErrors.authentication(
                IllegalStateException("Invalid login credentials"), false
            ))
    }
    @Test fun emailConfirmationHasClearNextStep() {
        assertEquals("Hãy mở email xác nhận tài khoản, sau đó đăng nhập lại.",
            AccountFriendlyErrors.authentication(
                IllegalStateException("Email not confirmed"), false
            ))
    }
    @Test fun registeredEmailExplainsHowToProceed() {
        assertEquals("Email này đã có tài khoản. Hãy chuyển sang Đăng nhập.",
            AccountFriendlyErrors.authentication(
                IllegalStateException("User already registered"), true
            ))
    }
    @Test fun rawBackendDetailsAreNeverExposedForUnexpectedErrors() {
        val value = AccountFriendlyErrors.authentication(
            IllegalStateException("HTTP 500 /rest/v1/rpc/auth_internal"), false
        )
        assertEquals("Chưa đăng nhập được. Hãy kiểm tra thông tin rồi thử lại.", value)
        assertFalse(value.contains("HTTP"))
    }
}
