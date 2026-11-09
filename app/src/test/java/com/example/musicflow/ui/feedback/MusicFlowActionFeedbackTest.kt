package com.example.musicflow.ui.feedback

import com.example.musicflow.ui.feedback.MusicFlowActionFeedback.CloudAction
import com.example.musicflow.ui.feedback.MusicFlowActionFeedback.FileAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

class MusicFlowActionFeedbackTest {
    @Test fun fileSizeAndPlaylistLimitsRemainUnderstandable() {
        assertEquals("Tệp sao lưu vượt giới hạn 2 MB. Hãy chọn tệp nhỏ hơn.",
            MusicFlowActionFeedback.fileFailure(
                IllegalArgumentException("Tệp sao lưu vượt quá 2 MB."), FileAction.IMPORT))
        assertEquals("Một danh sách phát vượt giới hạn 500 bài.",
            MusicFlowActionFeedback.fileFailure(
                IllegalArgumentException("Playlist vượt quá 500 bài."), FileAction.IMPORT))
    }

    @Test fun corruptBackupExplainsWhichFileToSelect() {
        assertEquals("Tệp không đúng định dạng sao lưu MusicFlow. Hãy chọn tệp .json hợp lệ.",
            MusicFlowActionFeedback.fileFailure(
                IllegalArgumentException("Malformed JSON"), FileAction.IMPORT))
    }

    @Test fun genericExceptionsNeverEchoPrivateBackendDetails() {
        val sensitive = "HTTP 500 https://private.example/api?token=secret"
        for (action in FileAction.values()) {
            val label = MusicFlowActionFeedback.fileFailure(
                IllegalStateException(sensitive), action)
            assertFalse(label.contains("HTTP"))
            assertFalse(label.contains("secret"))
        }
        for (action in CloudAction.values()) {
            val label = MusicFlowActionFeedback.cloudFailure(
                IllegalStateException(sensitive), action)
            assertFalse(label.contains("HTTP"))
            assertFalse(label.contains("secret"))
        }
    }

    @Test fun cloudConnectionAndExpiredSessionProvideNextSteps() {
        assertEquals("Không thể kết nối đám mây. Hãy kiểm tra Internet rồi thử lại.",
            MusicFlowActionFeedback.cloudFailure(
                IOException("connection reset"), CloudAction.LIBRARY))
        assertEquals("Phiên đăng nhập có thể đã hết hạn. Hãy đăng nhập lại rồi thử.",
            MusicFlowActionFeedback.cloudFailure(
                IllegalStateException("JWT expired"), CloudAction.ITEM))
    }

    @Test fun localFilePermissionGivesSelectionHint() {
        assertEquals("Không có quyền truy cập tệp. Hãy chọn lại tệp hoặc cấp quyền đọc.",
            MusicFlowActionFeedback.fileFailure(
                SecurityException("permission denied"), FileAction.CHECK))
    }
}
