package com.example.musicflow.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ArtworkCache {

    private const val MAX_DIMENSION = 512

    // Tối đa khoảng 12 MB cho bitmap đã giải mã.
    private val images =
        object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
            override fun sizeOf(
                key: String,
                value: Bitmap
            ): Int = value.byteCount
        }

    // Ghi nhớ file không có ảnh nhúng.
    private val withoutArtwork =
        LruCache<String, Boolean>(200)

    fun load(context: Context, uri: Uri): Bitmap? {
        val key = uri.toString()

        images.get(key)?.let { return it }

        if (withoutArtwork.get(key) == true) {
            return null
        }

        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, uri)

            val bytes = retriever.embeddedPicture

            if (bytes == null) {
                withoutArtwork.put(key, true)
                return null
            }

            // Đọc kích thước ảnh mà chưa giải mã bitmap.
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                bounds
            )

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                withoutArtwork.put(key, true)
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
                bytes,
                0,
                bytes.size,
                options
            )

            if (bitmap != null) {
                images.put(key, bitmap)
            } else {
                withoutArtwork.put(key, true)
            }

            return bitmap

        } catch (e: CancellationException) {
            throw e

        } catch (_: Exception) {
            // File không hỗ trợ, mất quyền đọc, dữ liệu lỗi...
            // Giao diện sẽ dùng ảnh mặc định.
            return null

        } finally {
            runCatching { retriever.release() }
        }
    }
}

@Composable
fun SongArtwork(
    uri: Uri?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext

    // Tạo trạng thái riêng cho mỗi URI.
    // Khi đổi bài, ảnh cũ không còn được giữ trên UI.
    key(uri?.toString()) {

        val bitmap by produceState<Bitmap?>(
            initialValue = null,
            uri
        ) {
            value = uri?.let { source ->
                withContext(Dispatchers.IO) {
                    ArtworkCache.load(context, source)
                }
            }
        }

        Box(
            modifier = modifier.background(
                MaterialTheme.colorScheme.primaryContainer
            ),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {

                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Ảnh bìa bài hát",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

            } else {

                Text(
                    text = "♫",
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
