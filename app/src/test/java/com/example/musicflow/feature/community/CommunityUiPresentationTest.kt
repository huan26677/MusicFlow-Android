package com.example.musicflow.feature.community

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.ZoneId

class CommunityUiPresentationTest {
    @Test fun serverDatesAreShownInVietnameseOrderAndLocalZone() {
        assertEquals("07:30 · 09/11/2026",
            CommunityUiPresentation.dateLabel(
                "2026-11-09T00:30:00Z", ZoneId.of("Asia/Ho_Chi_Minh")
            ))
    }

    @Test fun demoTimeAndMalformedInputStayReadable() {
        assertEquals("Vừa xong", CommunityUiPresentation.dateLabel("Vừa xong"))
        assertEquals("Dữ liệu mẫu", CommunityUiPresentation.dateLabel("Dữ liệu mẫu"))
        assertEquals("Không rõ thời gian",
            CommunityUiPresentation.dateLabel("not-an-iso-date"))
    }

    @Test fun errorsNeverExposeRawBackendDetails() {
        val message = CommunityUiPresentation.errorLabel(
            IllegalStateException("permission denied: musicflow_community_rate RPC"),
            "Không thể lưu đánh giá. Hãy thử lại."
        )
        assertEquals("Không thể lưu đánh giá. Hãy thử lại.", message)
        assertFalse(message.contains("RPC"))
    }

    @Test fun loginAndConnectionErrorsHaveActions() {
        assertEquals(
            "Hãy đăng nhập tài khoản trực tuyến ở mục Tài khoản rồi thử lại.",
            CommunityUiPresentation.errorLabel(
                IllegalStateException("Đăng nhập để đánh giá"), "Thử lại"
            )
        )
        assertEquals(
            "Không thể kết nối. Hãy kiểm tra Internet và thử lại.",
            CommunityUiPresentation.errorLabel(
                IllegalStateException("connection reset"), "Thử lại"
            )
        )
    }
}
