package com.example.musicflow.core.media

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Only remembers document references. Never copies, edits or deletes audio files. */
data class PickedMusicRecord(
    val uri: String,
    val title: String,
    val artist: String,
    val addedSeconds: Long,
    val durationMs: Long = 0L
)

data class PickedMusicInspection(
    val available: List<DeviceSong> = emptyList(),
    val unavailable: List<PickedMusicRecord> = emptyList()
)

class LocalPickedMusicStore private constructor(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val prefs = context.applicationContext.getSharedPreferences(
        "musicflow_picked_audio_catalog", Context.MODE_PRIVATE
    )
    private val recordsMutable = MutableStateFlow(readSaved())
    val records = recordsMutable.asStateFlow()

    private fun readSaved(): List<PickedMusicRecord> = runCatching {
        val json = JSONArray(prefs.getString("records", "[]"))
        (0 until minOf(json.length(), MAX_ITEMS)).mapNotNull { index ->
            val obj = json.optJSONObject(index) ?: return@mapNotNull null
            val uri = obj.optString("uri")
            if (!uri.startsWith("content://")) return@mapNotNull null
            PickedMusicRecord(
                uri = uri,
                title = obj.optString("title").ifBlank { "Bài hát đã chọn" },
                artist = obj.optString("artist").ifBlank { "Không rõ nghệ sĩ" },
                addedSeconds = obj.optLong("addedSeconds").coerceAtLeast(0L),
                durationMs = obj.optLong("durationMs").coerceAtLeast(0L)
            )
        }.distinctBy { it.uri }
    }.getOrDefault(emptyList())

    private fun save(list: List<PickedMusicRecord>) {
        val json = JSONArray()
        list.forEach { item ->
            json.put(JSONObject()
                .put("uri", item.uri)
                .put("title", item.title)
                .put("artist", item.artist)
                .put("addedSeconds", item.addedSeconds)
                .put("durationMs", item.durationMs))
        }
        check(prefs.edit().putString("records", json.toString()).commit()) {
            "Không lưu được danh sách nhạc đã chọn."
        }
        recordsMutable.value = list
    }

    /** Save only readable documents with a persistable grant; temporary grants are not reliable. */
    @Synchronized
    fun remember(items: List<PickedMusicRecord>) {
        val persisted = resolver.persistedUriPermissions
            .filter { it.isReadPermission }.map { it.uri.toString() }.toSet()
        val valid = items.filter { item ->
            item.uri in persisted && item.uri.startsWith("content://") &&
                canRead(Uri.parse(item.uri))
        }.distinctBy { it.uri }
        if (valid.isEmpty()) return
        val existing = recordsMutable.value.associateBy { it.uri }
        val incoming = valid.map { it.uri }.toSet()
        val merged = (valid.map { item ->
            existing[item.uri]?.copy(title = item.title, artist = item.artist,
                durationMs = item.durationMs, addedSeconds = item.addedSeconds) ?: item
        } + recordsMutable.value.filter { it.uri !in incoming })
            .take(MAX_ITEMS)
        save(merged)
    }

    @Synchronized
    fun forget(uri: String) {
        // Remove only our local shortcut; other features may still require the grant.
        save(recordsMutable.value.filterNot { it.uri == uri })
    }

    suspend fun inspect(): PickedMusicInspection = withContext(Dispatchers.IO) {
        val granted = resolver.persistedUriPermissions
            .filter { it.isReadPermission }.map { it.uri.toString() }.toSet()
        val found = mutableListOf<DeviceSong>()
        val missing = mutableListOf<PickedMusicRecord>()
        recordsMutable.value.forEach { item ->
            val uri = Uri.parse(item.uri)
            if (item.uri !in granted || !canRead(uri)) {
                missing.add(item)
            } else {
                found.add(DeviceSong(
                    id = -1L - (item.uri.hashCode().toLong() and 0xffffffffL),
                    uri = uri,
                    title = item.title,
                    artist = item.artist,
                    durationMs = item.durationMs,
                    mediaId = item.uri,
                    dateAddedSeconds = item.addedSeconds
                ))
            }
        }
        PickedMusicInspection(found, missing)
    }

    private fun canRead(uri: Uri): Boolean = try {
        resolver.openFileDescriptor(uri, "r")?.use { true } == true
    } catch (_: Exception) {
        false
    }

    companion object {
        private const val MAX_ITEMS = 250
        @Volatile private var instance: LocalPickedMusicStore? = null
        fun getInstance(context: Context): LocalPickedMusicStore =
            instance ?: synchronized(this) {
                instance ?: LocalPickedMusicStore(context.applicationContext).also {
                    instance = it
                }
            }
    }
}
