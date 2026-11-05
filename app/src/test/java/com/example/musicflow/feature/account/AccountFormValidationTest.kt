package com.example.musicflow.feature.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountFormValidationTest {
    @Test fun emptyFormExplainsMissingFields() {
        val errors = AccountFormValidation.validate("", "", "", AccountAuthMode.SIGN_UP)
        assertEquals("Bạn chưa nhập email.", errors.email)
        assertEquals("Bạn chưa nhập mật khẩu.", errors.password)
        assertEquals("Hãy nhập lại mật khẩu.", errors.confirmPassword)
        assertFalse(errors.isValid)
    }

    @Test fun invalidEmailAndShortPasswordShowInlineErrors() {
        val errors = AccountFormValidation.validate("huan@@gmail", "123", "123", AccountAuthMode.SIGN_UP)
        assertEquals("Địa chỉ email chưa hợp lệ.", errors.email)
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự.", errors.password)
    }

    @Test fun signupRequiresMatchingPassword() {
        val errors = AccountFormValidation.validate(
            "demo@example.com", "secret123", "secret234", AccountAuthMode.SIGN_UP
        )
        assertEquals("Mật khẩu nhập lại chưa khớp.", errors.confirmPassword)
    }

    @Test fun signinDoesNotRequireConfirmation() {
        val errors = AccountFormValidation.validate(
            "demo@example.com", "secret123", "", AccountAuthMode.SIGN_IN
        )
        assertNull(errors.confirmPassword)
        assertTrue(errors.isValid)
    }

    @Test fun signupAcceptsMatchingPasswordAndTrimmedEmail() {
        val errors = AccountFormValidation.validate(
            " demo@example.com ", "secret123", "secret123", AccountAuthMode.SIGN_UP
        )
        assertTrue(errors.isValid)
    }
}
