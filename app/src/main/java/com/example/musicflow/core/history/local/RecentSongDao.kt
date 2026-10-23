
package com.example.musicflow.core.history.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSongDao {

    @Query(
        "SELECT * FROM recent_songs " +
                "ORDER BY position ASC"
    )
    suspend fun getAll(): List<RecentSongEntity>

    @Query(
        "SELECT * FROM recent_songs " +
                "ORDER BY position ASC"
    )
    fun observeAll(): Flow<List<RecentSongEntity>>

    @Insert
    suspend fun insertAll(
        songs: List<RecentSongEntity>
    )

    @Query("DELETE FROM recent_songs WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: String)

    @Query("DELETE FROM recent_songs")
    suspend fun deleteAll()
}
