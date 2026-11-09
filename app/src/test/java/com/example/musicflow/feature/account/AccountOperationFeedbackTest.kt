package com.example.musicflow.feature.account

import com.example.musicflow.feature.account.AccountOperationFeedback.Action
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AccountOperationFeedbackTest {
    @Test fun unknownErrorsDoNotExposeRpcOrPrivateServerDetails() {
        val failure = IllegalStateException(
            "HTTP 500 musicflow_admin_grant https://private.invalid?token=secret"
        )
        Action.values().forEach { action ->
            val label = AccountOperationFeedback.failure(failure, action)
            assertFalse(label.contains("musicflow_admin_grant"))
            assertFalse(label.contains("secret"))
            assertFalse(label.contains("HTTP"))
        }
    }

    @Test fun expiredLoginAndDeniedPermissionsRemainActionable() {
        assertEquals(
            "Phiên đăng nhập có thể đã hết hạn. Hãy đăng nhập lại rồi thử.",
            AccountOperationFeedback.failure(
                IllegalStateException("JWT expired"), Action.DRAFT_SUBMIT
            )
        )
        assertEquals(
            "Tài khoản chưa có quyền thực hiện thao tác này.",
            AccountOperationFeedback.failure(
                SecurityException("permission denied"), Action.ADMIN
            )
        )
    }

    @Test fun privateAttachmentsKeepSizeAndFormatGuidance() {
        assertEquals(
            "Tệp lớn hơn 6 MB. Hãy chọn tệp nhỏ hơn.",
            AccountOperationFeedback.failure(
                IllegalArgumentException("File exceeds 6 MB"), Action.FILE_UPLOAD
            )
        )
        assertEquals(
            "Định dạng tệp không phù hợp. Hãy chọn bản thu hoặc chứng cứ PDF/JPG/PNG.",
            AccountOperationFeedback.failure(
                IllegalArgumentException("unsupported MIME"), Action.FILE_UPLOAD
            )
        )
    }

    @Test fun networkAndFileDownloadErrorsTellUserNextStep() {
        assertEquals(
            "Không thể kết nối. Hãy kiểm tra Internet rồi thử lại.",
            AccountOperationFeedback.failure(
                IOException("connection reset"), Action.DRAFT_SAVE
            )
        )
        assertEquals(
            "Chưa tải về được tệp. Hãy chọn nơi lưu khác và thử lại.",
            AccountOperationFeedback.failure(
                IllegalStateException("internal storage error"), Action.FILE_DOWNLOAD
            )
        )
    }
}
