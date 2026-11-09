package com.example.musicflow.feature.online

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OnlineCatalogFeedbackTest {
    @Test fun invalidManualEndpointGivesHttpsGuidance() {
        assertEquals(
            "Địa chỉ nguồn nhạc chưa hợp lệ. Hãy dùng địa chỉ HTTPS được cấp quyền.",
            OnlineCatalogFeedback.failure(
                IllegalArgumentException("invalid endpoint"), OnlineCatalogFeedback.Action.CONNECT
            )
        )
    }

    @Test fun networkFailureSuggestsRetryWithoutServerDetails() {
        assertEquals(
            "Chưa kết nối được nhạc online. Hãy kiểm tra Internet rồi thử lại.",
            OnlineCatalogFeedback.failure(
                IOException("connection reset"), OnlineCatalogFeedback.Action.LOAD
            )
        )
    }

    @Test fun unexpectedErrorsDoNotEchoPrivateUrlsOrRpc() {
        val privateMessage = "HTTP 500 RPC https://private.invalid/?token=secret"
        OnlineCatalogFeedback.Action.values().forEach { action ->
            val feedback = OnlineCatalogFeedback.failure(
                IllegalStateException(privateMessage), action
            )
            assertFalse(feedback.contains("RPC"))
            assertFalse(feedback.contains("secret"))
        }
    }
}
