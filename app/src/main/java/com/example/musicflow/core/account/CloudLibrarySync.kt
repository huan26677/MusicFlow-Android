package com.example.musicflow.core.account

import android.content.Context
import com.example.musicflow.core.favorites.LocalFavoritesStore
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.media.DeviceMusicScanner
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.playlist.LocalPlaylist
import com.example.musicflow.core.playlist.PlaylistTrack
import com.example.musicflow.core.playlist.RoomPlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class CloudSyncSummary(
    val playlistsSaved: Int,
    val favoritesSaved: Int,
    val playlistsRestored: Int,
    val tracksLinked: Int,
    val favoritesLinked: Int,
    val unmatched: Int,
    val pending: List<CloudPendingMatch> = emptyList(),
    val deviceSongs: List<DeviceSong> = emptyList(),
    val cloudPlaylists: List<CloudRemotePlaylistInfo> = emptyList(),
    val cloudFavorites: List<CloudRemoteFavoriteInfo> = emptyList()
)

data class CloudPendingMatch(
    val playlistId: String?,
    val title: String,
    val artist: String
)

data class CloudRemotePlaylistInfo(val id: String, val name: String)
data class CloudRemoteFavoriteInfo(
    val key: String,
    val title: String,
    val artist: String
)

/**
 * Manual opt-in merge, never deletes local or remote data.
 * Device-specific content:// URIs and private file paths are not sent to Supabase.
 * A cloud track is playable only when its title/artist resolves uniquely on
 * the current device. Cross-device file transfer is not implied.
 */
class CloudLibrarySync(context: Context, private val accountApi: AccountApi) {
    private val app = context.applicationContext
    private val ownerBinding = app.getSharedPreferences(
        "musicflow_cloud_sync_owner_v1", Context.MODE_PRIVATE
    )
    // Mapping never leaves the device: cloud metadata -> chosen local content URI.
    private val relinkStore = app.getSharedPreferences(
        "musicflow_cloud_relink_v2", Context.MODE_PRIVATE
    )
    private val playlists = RoomPlaylistRepository(RecentHistoryDatabase.getInstance(app))
    private val favorites = LocalFavoritesStore.getInstance(app)

    private data class RemotePlaylist(
        val id: String,
        val name: String,
        val created: Long,
        val updated: Long,
        val tracks: List<CloudTrackMeta>,
        val deleted: Boolean
    )

    private data class RemoteFavorite(
        val key: String,
        val title: String,
        val artist: String,
        val deleted: Boolean
    )

    suspend fun sync(): CloudSyncSummary = withContext(Dispatchers.IO) {
        val identity = accountApi.currentAccount()
        val owner = UUID.fromString(identity.userId).toString()
        // Scan before any writes. No media permission -> report an error, not a
        // silent empty-library upload.
        val onDevice = DeviceMusicScanner.scan(app)
        val existingOwner = ownerBinding.getString("owner_id", null)
        check(existingOwner == null || existingOwner == owner) {
            "Thư viện local này đã được gắn với tài khoản khác. " +
                "Không đồng bộ chéo để tránh lộ Playlist riêng."
        }
        if (existingOwner == null) {
            check(ownerBinding.edit().putString("owner_id", owner).commit()) {
                "Không thể gắn thư viện với tài khoản."
            }
        }
        val originalPlaylists = playlists.observePlaylists().first()
        val beforeCloud = loadPlaylists(owner)
        val previousFavorites = loadFavorites(owner)
        val deletedPlaylistIds = beforeCloud.filter { it.deleted }.map { it.id }.toSet()

        var savedPlaylists = 0
        originalPlaylists.take(100).forEach { playlist ->
            if (playlist.id in deletedPlaylistIds) return@forEach
            val localTracks = playlists.getTracks(playlist.id).map {
                CloudTrackMeta(it.title.take(240), it.artist.take(180), it.addedAtMs)
            }
            accountApi.cloudRequest(
                "POST", "/rest/v1/rpc/musicflow_merge_cloud_playlist",
                JSONObject().put("p_playlist_id", playlist.id)
                    .put("p_name", playlist.name)
                    .put("p_created_at_ms", playlist.createdAtMs)
                    .put("p_updated_at_ms", playlist.updatedAtMs)
                    .put("p_tracks", tracksJson(localTracks.take(500)))
            )
            savedPlaylists++
        }

        val localByUri = onDevice.associateBy { it.uri.toString() }
        val favoriteDetails = favorites.songDetails.value
        val savedKeys = previousFavorites.map {
            CloudSyncRules.key(it.title, it.artist)
        }.toMutableSet()
        var savedFavorites = 0
        favorites.songUris.value.forEach { uri ->
            val info = favoriteDetails[uri]
            val song = localByUri[uri]
            val title = (info?.title ?: song?.title)?.take(240).orEmpty()
            val artist = (info?.artist ?: song?.artist)?.take(180).orEmpty()
            if (title.isBlank()) return@forEach
            val key = CloudSyncRules.key(title, artist)
            if (!savedKeys.add(key)) return@forEach
            accountApi.cloudRequest(
                "POST", "/rest/v1/cloud_favorites",
                JSONObject().put("owner_id", owner)
                    .put("favorite_key", sha256(key))
                    .put("title", title)
                    .put("artist", artist),
                prefer = "resolution=ignore-duplicates"
            )
            savedFavorites++
        }

        val remote = loadPlaylists(owner)
        val cloudFavorites = loadFavorites(owner)
        val existingIds = originalPlaylists.map { it.id }.toSet()
        var restoredPlaylists = 0
        var linkedTracks = 0
        var unmatched = 0
        val pending = mutableListOf<CloudPendingMatch>()
        remote.forEach { cloud ->
            if (cloud.deleted) return@forEach
            val matched = cloud.tracks.mapNotNull { track ->
                val match = match(owner, onDevice, track.title, track.artist)
                if (match == null) {
                    unmatched++
                    if (pending.size < 60) {
                        pending.add(CloudPendingMatch(cloud.id, track.title, track.artist))
                    }
                    null
                } else {
                    PlaylistTrack(
                        mediaId = match.mediaId ?: "device-${match.id}",
                        audioUri = match.uri.toString(),
                        title = match.title,
                        artist = match.artist,
                        addedAtMs = track.addedAtMs
                    )
                }
            }
            val playlist = LocalPlaylist(
                id = cloud.id,
                name = cloud.name,
                createdAtMs = cloud.created,
                updatedAtMs = cloud.updated
            )
            linkedTracks += playlists.mergeCloudPlaylist(playlist, matched)
            if (cloud.id !in existingIds) restoredPlaylists++
        }
        var linkedFavorites = 0
        cloudFavorites.forEach { favorite ->
            if (favorite.deleted) return@forEach
            val match = match(owner, onDevice, favorite.title, favorite.artist)
            if (match == null) {
                unmatched++
                if (pending.size < 60) {
                    pending.add(CloudPendingMatch(null, favorite.title, favorite.artist))
                }
            } else if (favorites.addSongIfAbsent(match)) {
                linkedFavorites++
            }
        }
        CloudSyncSummary(
            playlistsSaved = savedPlaylists,
            favoritesSaved = savedFavorites,
            playlistsRestored = restoredPlaylists,
            tracksLinked = linkedTracks,
            favoritesLinked = linkedFavorites,
            unmatched = unmatched,
            pending = pending.distinct(),
            deviceSongs = onDevice,
            cloudPlaylists = remote.filterNot { it.deleted }.map {
                CloudRemotePlaylistInfo(it.id, it.name)
            },
            cloudFavorites = cloudFavorites.filterNot { it.deleted }.map {
                CloudRemoteFavoriteInfo(it.key, it.title, it.artist)
            }
        )
    }

    private fun mappingKey(owner: String, title: String, artist: String): String =
        owner + "_" + sha256(CloudSyncRules.key(title, artist))

    private fun match(
        owner: String, items: List<DeviceSong>, title: String, artist: String
    ): DeviceSong? {
        val saved = relinkStore.getString(mappingKey(owner, title, artist), null)
        val mapped = saved?.let { uri ->
            items.singleOrNull { it.uri.toString() == uri }
        }
        return mapped ?: CloudSyncRules.uniqueLocalMatch(
            items, title, artist, { it.title }, { it.artist }
        )
    }

    suspend fun linkManually(pending: CloudPendingMatch, uri: String): Boolean =
        withContext(Dispatchers.IO) {
            val owner = UUID.fromString(accountApi.currentAccount().userId).toString()
            check(ownerBinding.getString("owner_id", null) == owner) {
                "Tài khoản chưa được liên kết với thư viện này."
            }
            val song = DeviceMusicScanner.scan(app).singleOrNull {
                it.uri.toString() == uri
            } ?: error("Bài đã chọn không còn tồn tại trên thiết bị.")
            if (pending.playlistId == null) {
                favorites.addSongIfAbsent(song)
            } else {
                val playlist = playlists.getPlaylist(pending.playlistId)
                    ?: error("Không tìm thấy Playlist local.")
                playlists.mergeCloudPlaylist(
                    playlist,
                    listOf(
                        PlaylistTrack(
                            mediaId = song.mediaId ?: "device-${song.id}",
                            audioUri = song.uri.toString(),
                            title = song.title,
                            artist = song.artist,
                            addedAtMs = System.currentTimeMillis()
                        )
                    )
                )
            }
            check(relinkStore.edit().putString(
                mappingKey(owner, pending.title, pending.artist), uri
            ).commit()) { "Không lưu được liên kết bài hát." }
            true
        }

    suspend fun trashCloudPlaylist(id: String): Boolean = withContext(Dispatchers.IO) {
        val owner = accountApi.currentAccount().userId
        check(ownerBinding.getString("owner_id", null) == owner)
        val uuid = UUID.fromString(id).toString()
        accountApi.cloudRequest(
            "POST", "/rest/v1/rpc/musicflow_trash_cloud_playlist",
            JSONObject().put("p_playlist_id", uuid)
        ).trim() == "true"
    }

    suspend fun trashCloudFavorite(key: String): Boolean = withContext(Dispatchers.IO) {
        val owner = accountApi.currentAccount().userId
        check(ownerBinding.getString("owner_id", null) == owner)
        require(key.matches(Regex("[a-f0-9]{64}")))
        accountApi.cloudRequest(
            "POST", "/rest/v1/rpc/musicflow_trash_cloud_favorite",
            JSONObject().put("p_favorite_key", key)
        ).trim() == "true"
    }

    private fun tracksJson(tracks: List<CloudTrackMeta>): JSONArray =
        JSONArray().apply {
            tracks.forEach { track ->
                put(JSONObject().put("title", track.title)
                    .put("artist", track.artist)
                    .put("added_at_ms", track.addedAtMs))
            }
        }

    private fun loadPlaylists(owner: String): List<RemotePlaylist> {
        val entries = mutableListOf<JSONObject>()
        var offset = 0
        while (offset < 200) {
            val raw = accountApi.cloudRequest(
                "GET",
                "/rest/v1/cloud_playlists?owner_id=eq.$owner&select=playlist_id,name,created_at_ms,updated_at_ms,tracks,deleted_at_ms&order=created_at_ms.asc&limit=20&offset=$offset"
            )
            val page = JSONArray(raw)
            for (i in 0 until page.length()) entries.add(page.getJSONObject(i))
            if (page.length() < 20) break
            offset += 20
        }
        return entries.map { entry ->
            val id = UUID.fromString(entry.getString("playlist_id")).toString()
            val contents = entry.getJSONArray("tracks")
            val tracks = (0 until contents.length()).mapNotNull { position ->
                val item = contents.optJSONObject(position) ?: return@mapNotNull null
                val title = item.optString("title").trim().take(240)
                if (title.isBlank()) return@mapNotNull null
                CloudTrackMeta(
                    title, item.optString("artist").trim().take(180),
                    item.optLong("added_at_ms", 0L).coerceAtLeast(0)
                )
            }
            RemotePlaylist(
                id, entry.getString("name"),
                entry.getLong("created_at_ms"), entry.getLong("updated_at_ms"),
                tracks, !entry.isNull("deleted_at_ms")
            )
        }
    }

    private fun loadFavorites(owner: String): List<RemoteFavorite> {
        val results = mutableListOf<RemoteFavorite>()
        var offset = 0
        while (offset < 2500) {
            val raw = accountApi.cloudRequest(
                "GET",
                "/rest/v1/cloud_favorites?owner_id=eq.$owner&select=favorite_key,title,artist,deleted_at_ms&order=favorite_key.asc&limit=200&offset=$offset"
            )
            val items = JSONArray(raw)
            for (i in 0 until items.length()) {
                val entry = items.getJSONObject(i)
                results.add(RemoteFavorite(
                    entry.getString("favorite_key"), entry.getString("title"),
                    entry.optString("artist"), !entry.isNull("deleted_at_ms")
                ))
            }
            if (items.length() < 200) break
            offset += 200
        }
        return results
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
