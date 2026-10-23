
package com.example.musicflow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri

import java.io.ByteArrayOutputStream

object SessionArtworkReader {

    private const val MAX_DIMENSION = 384
    private const val MAX_BYTES = 256 * 1024

    /**
     * Chỉ gọi trên luồng I/O.
     * Trả về ảnh JPEG đã thu nhỏ hoặc null.
     */
    fun read(
        context: Context,
        uri: Uri
    ): ByteArray? {
        if (uri.scheme != "content" &&
            uri.scheme != "file"
        ) {
            return null
        }

        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(context, uri)

            val embeddedBytes =
                retriever.embeddedPicture ?: return null

            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeByteArray(
                embeddedBytes,
                0,
                embeddedBytes.size,
                bounds
            )

            if (
                bounds.outWidth <= 0 ||
                bounds.outHeight <= 0
            ) {
                return null
            }

            var sampleSize = 1

            while (
                bounds.outWidth / sampleSize > MAX_DIMENSION ||
                bounds.outHeight / sampleSize > MAX_DIMENSION
            ) {
                sampleSize *= 2
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            val bitmap = BitmapFactory.decodeByteArray(
                embeddedBytes,
                0,
                embeddedBytes.size,
                options
            ) ?: return null

            try {
                ByteArrayOutputStream().use { output ->
                    val success = bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        80,
                        output
                    )

                    if (!success) {
                        return null
                    }

                    output.toByteArray().takeIf {
                        it.size <= MAX_BYTES
                    }
                }
            } finally {
                bitmap.recycle()
            }

        } catch (_: Exception) {
            null

        } finally {
            runCatching {
                retriever.release()
            }
        }
    }
}
