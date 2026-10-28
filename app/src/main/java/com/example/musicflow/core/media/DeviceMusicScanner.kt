package com.example.musicflow.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceSong(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val albumId: Long? = null,
    val album: String = "Không rõ album",
    val mediaId: String? = null,
    val artistId: Long? = null,
    val genres: List<String> = emptyList()
)

object DeviceMusicScanner {

    suspend fun scan(context: Context): List<DeviceSong> =
        withContext(Dispatchers.IO) {

            val songs = mutableListOf<DeviceSong>()
            val genresBySongId = loadGenres(context)

            val collection =
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST_ID
            )

            // Chỉ lấy file Android phân loại là nhạc.
            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            val sortOrder =
                "${MediaStore.Audio.Media.TITLE} ASC"

            val cursor = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            ) ?: throw IllegalStateException(
                "MediaStore không trả về dữ liệu"
            )

            cursor.use { cursor ->

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

                val albumIdColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ALBUM_ID
                )
                val albumColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ALBUM
                )
                val artistIdColumn = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST_ID
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

                    val albumId = if (cursor.isNull(albumIdColumn)) {
                        null
                    } else {
                        cursor.getLong(albumIdColumn).takeIf { it > 0L }
                    }

                    val artistId = if (cursor.isNull(artistIdColumn)) {
                        null
                    } else {
                        cursor.getLong(artistIdColumn).takeIf { it > 0L }
                    }

                    val albumName = cursor.getString(albumColumn)
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty() &&
                                !it.equals("<unknown>", ignoreCase = true)
                        }
                        ?: "Không rõ album"

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
                            durationMs = duration,
                            albumId = albumId,
                            album = albumName,
                            artistId = artistId,
                            genres = genresBySongId[id].orEmpty()
                        )
                    )
                }
            }

            songs
        }

    /**
     * Genre metadata is optional on some Android MediaStore providers.
     * An unavailable genre table must never prevent loading the song library.
     */
    private fun loadGenres(context: Context): Map<Long, List<String>> {
        return try {
            val result = mutableMapOf<Long, MutableSet<String>>()
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Audio.Genres._ID,
                    MediaStore.Audio.Genres.NAME
                ),
                null,
                null,
                null
            )?.use { genres ->
                val idIndex = genres.getColumnIndexOrThrow(
                    MediaStore.Audio.Genres._ID
                )
                val nameIndex = genres.getColumnIndexOrThrow(
                    MediaStore.Audio.Genres.NAME
                )

                while (genres.moveToNext()) {
                    val genreId = genres.getLong(idIndex)
                    val name = genres.getString(nameIndex)
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank() &&
                                !it.equals("<unknown>", ignoreCase = true)
                        } ?: continue

                    try {
                        context.contentResolver.query(
                            MediaStore.Audio.Genres.Members.getContentUri(
                                "external", genreId
                            ),
                            arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                            null,
                            null,
                            null
                        )?.use { members ->
                            val songIdIndex = members.getColumnIndexOrThrow(
                                MediaStore.Audio.Genres.Members.AUDIO_ID
                            )
                            while (members.moveToNext()) {
                                result.getOrPut(
                                    members.getLong(songIdIndex)
                                ) { linkedSetOf() }.add(name)
                            }
                        }
                    } catch (error: Exception) {
                        Log.w(
                            "MusicFlowLibrary",
                            "Skipping inaccessible genre ID $genreId",
                            error
                        )
                    }
                }
            }
            result.mapValues { (_, names) -> names.toList() }
        } catch (error: Exception) {
            Log.w("MusicFlowLibrary", "Genres unavailable", error)
            emptyMap()
        }
    }
}
