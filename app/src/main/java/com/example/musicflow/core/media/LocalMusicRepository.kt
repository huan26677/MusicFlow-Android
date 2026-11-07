package com.example.musicflow.core.media

import android.content.Context

interface LocalMusicRepository {
    suspend fun getSongs(): List<DeviceSong>
    /** Expensive genre metadata is only needed on the Genres tab. */
    suspend fun getSongsWithGenres(): List<DeviceSong> = getSongs()
}

class MediaStoreLocalMusicRepository(
    context: Context
) : LocalMusicRepository {

    private val appContext = context.applicationContext

    override suspend fun getSongs(): List<DeviceSong> {
        return DeviceMusicScanner.scan(appContext, includeGenres = false)
    }

    override suspend fun getSongsWithGenres(): List<DeviceSong> =
        DeviceMusicScanner.scan(appContext, includeGenres = true)
}
