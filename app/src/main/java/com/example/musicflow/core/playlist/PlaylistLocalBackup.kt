package com.example.musicflow.core.playlist

import com.example.musicflow.core.media.TrackMediaItems
import org.json.JSONArray
import org.json.JSONObject

/** Portable metadata only: never stores audio bytes, credentials or cloud rights. */
data class PlaylistBackupEntry(val name: String, val tracks: List<PlaylistTrack>)

object PlaylistLocalBackup {
    const val MIME = "application/json"
    const val MAX_FILE_BYTES = 2 * 1024 * 1024
    const val MAX_PLAYLISTS = 100
    const val MAX_TRACKS_PER_PLAYLIST = 500
    private const val FORMAT = "musicflow.local.playlists"
    private const val VERSION = 1

    /** Validate entire document BEFORE any database write. Never treat it as a cloud entitlement. */
    fun validate(entries: List<PlaylistBackupEntry>): List<PlaylistBackupEntry> {
        require(entries.size <= MAX_PLAYLISTS) { "Tệp có quá nhiều playlist (tối đa 100)." }
        entries.forEach { entry ->
            require(PlaylistRules.normalizedNameOrNull(entry.name) == entry.name) {
                "Tên playlist trong bản sao lưu không hợp lệ."
            }
            require(entry.tracks.size <= MAX_TRACKS_PER_PLAYLIST) {
                "Playlist vượt quá 500 bài trong bản sao lưu."
            }
            val seenIds = mutableSetOf<String>()
            entry.tracks.forEach { track ->
                require(track.mediaId.isNotBlank() && track.mediaId.length <= 1024 &&
                    seenIds.add(track.mediaId)) { "Mã bài hát trùng hoặc không hợp lệ." }
                require(track.audioUri.length in 1..2048 &&
                    TrackMediaItems.isSafeStoredLocal(track.mediaId, track.audioUri)) {
                    "Chỉ sao lưu đường dẫn âm thanh cục bộ, không chứa URL streaming."
                }
                require(track.title.isNotBlank() && track.title.length <= 180 &&
                    track.artist.length <= 180 && track.addedAtMs >= 0L) {
                    "Thông tin bài hát trong bản sao lưu không hợp lệ."
                }
            }
        }
        return entries
    }

    fun encode(entries: List<PlaylistBackupEntry>): ByteArray {
        validate(entries)
        val root = JSONObject().put("format", FORMAT).put("version", VERSION)
        val playlists = JSONArray()
        entries.forEach { entry ->
            val tracks = JSONArray()
            entry.tracks.forEach { track ->
                tracks.put(JSONObject()
                    .put("mediaId", track.mediaId)
                    .put("audioUri", track.audioUri)
                    .put("title", track.title)
                    .put("artist", track.artist)
                    .put("addedAtMs", track.addedAtMs))
            }
            playlists.put(JSONObject().put("name", entry.name).put("tracks", tracks))
        }
        val bytes = root.put("playlists", playlists).toString(2).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_FILE_BYTES) { "Tệp sao lưu vượt quá 2 MB." }
        return bytes
    }

    fun decode(bytes: ByteArray): List<PlaylistBackupEntry> {
        require(bytes.isNotEmpty() && bytes.size <= MAX_FILE_BYTES) {
            "Tệp sao lưu trống hoặc vượt quá 2 MB."
        }
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        require(root.getString("format") == FORMAT && root.getInt("version") == VERSION) {
            "Không phải bản sao lưu MusicFlow v1 hợp lệ."
        }
        val playlists = root.getJSONArray("playlists")
        require(playlists.length() <= MAX_PLAYLISTS) { "Tệp có quá nhiều playlist." }
        val parsed = (0 until playlists.length()).map { i ->
            val entry = playlists.getJSONObject(i)
            val tracks = entry.getJSONArray("tracks")
            require(tracks.length() <= MAX_TRACKS_PER_PLAYLIST) {
                "Playlist vượt quá 500 bài."
            }
            PlaylistBackupEntry(
                name = entry.getString("name"),
                tracks = (0 until tracks.length()).map { j ->
                    tracks.getJSONObject(j).let { item ->
                        PlaylistTrack(
                            mediaId = item.getString("mediaId"),
                            audioUri = item.getString("audioUri"),
                            title = item.getString("title"),
                            artist = item.getString("artist"),
                            addedAtMs = item.getLong("addedAtMs")
                        )
                    }
                }
            )
        }
        return validate(parsed)
    }
}
