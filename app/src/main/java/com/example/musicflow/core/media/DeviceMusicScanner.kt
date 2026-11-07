package com.example.musicflow.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    val genres: List<String> = emptyList(),
    /** MediaStore DATE_ADDED is seconds since Unix epoch; 0 for manually chosen items. */
    val dateAddedSeconds: Long = 0L
)

object DeviceMusicScanner {

    suspend fun scan(
        context: Context,
        includeGenres: Boolean = true
    ): List<DeviceSong> =
        withContext(Dispatchers.IO) {

            val songs = mutableListOf<DeviceSong>()

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
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DATE_ADDED
            )

            // Some providers mark actual songs as IS_MUSIC=0. Permit long,
            // recognized audio from Music/ while excluding short notifications,
            // calls and recordings in unrelated folders. Other files remain
            // accessible explicitly through the document picker.
            val recognizedAudioTypes = listOf(
                "audio/mpeg", "audio/mp4", "audio/flac", "audio/x-flac",
                "audio/wav", "audio/x-wav", "audio/ogg", "audio/opus",
                "audio/aac", "audio/x-m4a"
            )
            val fallbackTypes = recognizedAudioTypes.joinToString(",") { "'$it'" }
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR (" +
                "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE 'Music/%' AND " +
                "${MediaStore.Audio.Media.DURATION} >= 30000 AND " +
                "${MediaStore.Audio.Media.MIME_TYPE} IN ($fallbackTypes))"

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
                val dateAddedColumn = cursor.getColumnIndex(
                    MediaStore.Audio.Media.DATE_ADDED
                )

                while (cursor.moveToNext()) {

                    if (songs.size % 64 == 0) currentCoroutineContext().ensureActive()
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
                            dateAddedSeconds = if (
                                dateAddedColumn < 0 || cursor.isNull(dateAddedColumn)
                            ) 0L else cursor.getLong(dateAddedColumn).coerceAtLeast(0L)
                        )
                    )
                }
            }

            // Genre providers need one additional query per genre.
            // Defer these expensive queries until the listener opens Thể loại.
            if (!includeGenres || songs.isEmpty()) {
                songs
            } else {
                val bySong = loadGenres(context, songs.map { it.id }.toHashSet())
                songs.map { song -> song.copy(genres = bySong[song.id].orEmpty()) }
            }
        }

    /**
     * Genre metadata is optional on some Android MediaStore providers.
     * An unavailable genre table must never prevent loading the song library.
     */
    private suspend fun loadGenres(
        context: Context,
        scannedSongIds: Set<Long>
    ): Map<Long, List<String>> {
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
                    currentCoroutineContext().ensureActive()
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
                                val songId = members.getLong(songIdIndex)
                                if (songId in scannedSongIds) {
                                    result.getOrPut(songId) { linkedSetOf() }.add(name)
                                }
                            }
                        }
                    } catch (cancel: CancellationException) {
                        throw cancel
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
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Exception) {
            Log.w("MusicFlowLibrary", "Genres unavailable", error)
            emptyMap()
        }
    }
}
