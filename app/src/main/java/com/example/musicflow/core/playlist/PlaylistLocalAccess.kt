package com.example.musicflow.core.playlist

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** On-demand check only. This confirms readability, NOT that a media decoder can play it. */
object PlaylistLocalAccess {
    suspend fun check(resolver: ContentResolver, tracks: List<PlaylistTrack>): Set<String> =
        withContext(Dispatchers.IO) {
            tracks.filterNot { track -> readable(resolver, track.audioUri) }
                .mapTo(mutableSetOf()) { it.mediaId }
        }

    fun readable(resolver: ContentResolver, rawUri: String): Boolean = runCatching {
        val uri = Uri.parse(rawUri)
        when (uri.scheme?.lowercase()) {
            "content" -> resolver.openFileDescriptor(uri, "r")?.use { true } == true
            "file" -> uri.path?.let { File(it).isFile && File(it).canRead() } == true
            null -> rawUri.startsWith("/") && !rawUri.startsWith("//") &&
                File(rawUri).isFile && File(rawUri).canRead()
            else -> false
        }
    }.getOrDefault(false)
}
