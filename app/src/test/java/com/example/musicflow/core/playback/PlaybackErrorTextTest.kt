
package com.example.musicflow.core.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorTextTest {

    @Test
    fun missingFileHasVietnameseMessage() {
        assertEquals(
            "Không tìm thấy file nhạc. " +
                    "File có thể đã bị xóa hoặc di chuyển.",
            PlaybackErrorText.fromCode(
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
            )
        )
    }

    @Test
    fun missingPermissionHasVietnameseMessage() {
        assertEquals(
            "MusicFlow không còn quyền đọc file nhạc này. " +
                    "Hãy kiểm tra quyền truy cập tệp.",
            PlaybackErrorText.fromCode(
                PlaybackException.ERROR_CODE_IO_NO_PERMISSION
            )
        )
    }

    @Test
    fun unknownErrorHasFallbackMessage() {
        assertEquals(
            "Không thể phát bài hát này. " +
                    "Bạn có thể thử lại hoặc chọn bài khác.",
            PlaybackErrorText.fromCode(-9999)
        )
    }
}
