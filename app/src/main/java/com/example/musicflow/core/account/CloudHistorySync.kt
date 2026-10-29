package com.example.musicflow.core.account

import android.content.Context
import com.example.musicflow.core.history.CloudHistoryRules
import com.example.musicflow.core.history.RecentSong
import com.example.musicflow.core.history.RoomRecentHistoryRepository
import com.example.musicflow.core.history.local.RecentHistoryDatabase
import com.example.musicflow.core.media.DeviceMusicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class CloudHistorySummary(
    val uploaded: Int,
    val restored: Int,
    val notOnDevice: Int,
    val availableOnCloud: Int
)

/**
 * Manual, independent opt-in history sync. Cloud stores title, artist and
 * listening timestamp only. It never stores local audio URI or media ID.
 */
class CloudHistorySync(context: Context, private val api: AccountApi) {
    private val app = context.applicationContext
    private val history = RoomRecentHistoryRepository(
        RecentHistoryDatabase.getInstance(app)
    )
    private val ownerBinding = app.getSharedPreferences(
        "musicflow_cloud_sync_owner_v1", Context.MODE_PRIVATE
    )

    suspend fun sync(): CloudHistorySummary = withContext(Dispatchers.IO) {
        val account = api.currentAccount()
        val owner = UUID.fromString(account.userId).toString()
        val previousOwner = ownerBinding.getString("owner_id", null)
        check(previousOwner == null || previousOwner == owner) {
            "Thư viện trên thiết bị đã gắn với tài khoản khác."
        }

        // Scan before any cloud writes so a missing permission never causes a
        // destructive or partial restore.
        val deviceSongs = DeviceMusicScanner.scan(app)
        if (previousOwner == null) {
            check(ownerBinding.edit().putString("owner_id", owner).commit()) {
                "Không thể lưu tài khoản đã liên kết."
            }
        }

        val onDeviceHistory = history.getRecent()
        var uploaded = 0
        for (item in onDeviceHistory) {
            val title = item.title.trim().take(240)
            val artist = item.artist.trim().take(180)
            if (title.isBlank() || item.playedAtMs <= 0L) continue
            val key = sha256(CloudHistoryRules.key(title, artist))
            api.cloudRequest(
                "POST",
                "/rest/v1/rpc/musicflow_merge_recent_history",
                JSONObject()
                    .put("p_track_key", key)
                    .put("p_title", title)
                    .put("p_artist", artist)
                    .put("p_played_at_ms", item.playedAtMs)
            )
            uploaded++
        }

        val currentMs = System.currentTimeMillis()
        val cutoff = currentMs - 30L * 24 * 60 * 60 * 1000
        val filter = if (account.tier == "premium") "" else
            "&played_at_ms=gte.$cutoff"
        val response = api.cloudRequest(
            "GET",
            "/rest/v1/cloud_recent_history?owner_id=eq.$owner" +
                "&select=title,artist,played_at_ms" +
                filter +
                "&order=played_at_ms.desc&limit=200"
        )
        val cloud = JSONArray(response)
        val restored = mutableListOf<RecentSong>()
        var missing = 0
        for (i in 0 until cloud.length()) {
            val item = cloud.getJSONObject(i)
            val title = item.optString("title")
            val artist = item.optString("artist")
            // A unique title+artist match is required; ambiguous files are
            // intentionally omitted until the user resolves them locally.
            val song = CloudSyncRules.uniqueLocalMatch(
                deviceSongs, title, artist, { it.title }, { it.artist }
            )
            if (song == null) {
                missing++
            } else {
                restored.add(
                    RecentSong(
                        mediaId = song.mediaId ?: "device-${song.id}",
                        audioUri = song.uri.toString(),
                        title = song.title,
                        artist = song.artist,
                        playedAtMs = item.getLong("played_at_ms")
                    )
                )
            }
        }
        CloudHistorySummary(
            uploaded = uploaded,
            restored = history.mergeCloudRecent(restored),
            notOnDevice = missing,
            availableOnCloud = cloud.length()
        )
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
