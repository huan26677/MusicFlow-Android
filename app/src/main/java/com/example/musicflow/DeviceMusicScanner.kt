package com.example.musicflow

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceSong(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long
)

object DeviceMusicScanner {

    suspend fun scan(context: Context): List<DeviceSong> =
        withContext(Dispatchers.IO) {

            val songs = mutableListOf<DeviceSong>()

            val collection =
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
            )

            // Chỉ lấy file Android phân loại là nhạc.
            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            val sortOrder =
                "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                )

                val titleColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                )

                val filenameColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DISPLAY_NAME
                )

                val artistColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST
                )

                val durationColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.DURATION
                )

                while (cursor.moveToNext()) {

                    val id = cursor.getLong(idColumn)

                    val filename =
                        cursor.getString(filenameColumn)
                            ?: "Bài hát không tên"

                    val title =
                        cursor.getString(titleColumn)
                            ?.takeIf { it.isNotBlank() }
                            ?: filename

                    val artist =
                        cursor.getString(artistColumn)
                            ?.takeIf {
                                it.isNotBlank() &&
                                        it != "<unknown>"
                            }
                            ?: "Không rõ nghệ sĩ"

                    val duration = if (
                        cursor.isNull(durationColumn)
                    ) {
                        0L
                    } else {
                        cursor.getLong(durationColumn)
                    }

                    val songUri =
                        ContentUris.withAppendedId(
                            collection,
                            id
                        )

                    songs.add(
                        DeviceSong(
                            id = id,
                            uri = songUri,
                            title = title,
                            artist = artist,
                            durationMs = duration
                        )
                    )
                }
            }

            songs
        }
}
