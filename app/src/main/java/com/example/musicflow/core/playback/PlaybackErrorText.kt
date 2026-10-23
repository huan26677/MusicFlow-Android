
package com.example.musicflow.core.playback

import androidx.media3.common.PlaybackException

object PlaybackErrorText {

    fun fromCode(errorCode: Int): String =
        when (errorCode) {

            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "Không tìm thấy file nhạc. " +
                        "File có thể đã bị xóa hoặc di chuyển."

            PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                "MusicFlow không còn quyền đọc file nhạc này. " +
                        "Hãy kiểm tra quyền truy cập tệp."

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
                "Định dạng file nhạc không được hỗ trợ."

            else ->
                "Không thể phát bài hát này. " +
                        "Bạn có thể thử lại hoặc chọn bài khác."
        }
}
