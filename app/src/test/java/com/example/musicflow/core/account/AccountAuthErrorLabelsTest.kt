package com.example.musicflow.core.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountAuthErrorLabelsTest {
    @Test fun identifiesEmailSendQuotaInsteadOfGenericTooManyRequests() {
        val message = AccountAuthErrorLabels.forResponse(429, "over_email_send_rate_limit")
        assertTrue(message.contains("hạn mức gửi email"))
        assertTrue(message.contains("SMTP"))
    }

    @Test fun handlesLegacyEmailQuotaCode() {
        assertTrue(
            AccountAuthErrorLabels.forResponse(429, "email_rate_limit_exceeded")
                .contains("hạn mức gửi email")
        )
    }

    @Test fun describesUnapprovedRecipientForHostedEmail() {
        assertTrue(
            AccountAuthErrorLabels.forResponse(422, "email_address_not_authorized")
                .contains("email thành viên dự án")
        )
    }

    @Test fun tellsUserAboutRejectedEmailAndConfirmation() {
        assertTrue(
            AccountAuthErrorLabels.forResponse(400, "email_address_invalid")
                .contains("từ chối địa chỉ email")
        )
        assertTrue(
            AccountAuthErrorLabels.forResponse(400, "email_not_confirmed")
                .contains("chưa được xác nhận")
        )
    }

    @Test fun retainsGenericRateLimitFallbackWithoutLeakingOtherDetails() {
        val message = AccountAuthErrorLabels.forResponse(429, "unknown_future_code")
        assertTrue(message.contains("giới hạn yêu cầu"))
        assertFalse(message.contains("SMTP"))
    }

    @Test fun doesNotEchoUnknownServerErrorData() {
        val input = "Attacker's address user@example.invalid"
        val message = AccountAuthErrorLabels.forResponse(400, input)
        assertFalse(message.contains(input))
    }

    @Test fun distinguishesOtherCommonAuthProblems() {
        assertTrue(
            AccountAuthErrorLabels.forResponse(400, "invalid_credentials")
                .contains("không đúng")
        )
        assertTrue(
            AccountAuthErrorLabels.forResponse(422, "weak_password")
                .contains("mật khẩu mạnh hơn")
        )
        assertTrue(
            AccountAuthErrorLabels.forResponse(400, "signup_disabled")
                .contains("không cho phép đăng ký")
        )
    }
}
