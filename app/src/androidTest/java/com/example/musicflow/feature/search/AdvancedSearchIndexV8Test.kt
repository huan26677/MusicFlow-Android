package com.example.musicflow.feature.search

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicflow.core.media.DeviceSong
import com.example.musicflow.core.media.LocalMusicRepository
import com.example.musicflow.feature.library.MusicCollections
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdvancedSearchIndexV8Test {
    private fun song(
        id: Int,
        title: String,
        artist: String = "Nghệ sĩ QA",
        album: String = "Album Đường Phố",
        genres: List<String> = listOf("Nhạc Trữ Tình")
    ) = DeviceSong(
        id = id.toLong(), uri = Uri.parse("content://media/external/audio/media/$id"),
        title = title, artist = artist, album = album, albumId = id.toLong() / 10,
        durationMs = 90_000L, genres = genres
    )

    @Test fun precomputedGroupMatchesPreserveAccentInsensitiveAndWordOrder() {
        val songs = listOf(
            song(10, "Đường về nhà", "Đen Vâu", "Hành trình", listOf("Nhạc Indie", "Ballad")),
            song(11, "Nắng trên phố", "Hà Anh", "Đường Phố", listOf("Nhạc Trữ Tình")),
            song(20, "Mưa tháng sáu", "Nghệ Sĩ QA", "Mùa Hạ", listOf("Nhạc Indie"))
        )
        val index = AdvancedMusicSearchIndex(songs)
        for (query in listOf("duong", "DUONG PHO", "indie", "den", "nhac", "ve", "ha")) {
            val expectedAlbums = MusicCollections.albums(songs)
                .filter { SongSearch.matchesText(it.name, query) }
            val expectedArtists = MusicCollections.artists(songs)
                .filter { SongSearch.matchesText(it.name, query) }
            val expectedGenres = MusicCollections.genres(songs)
                .filter { SongSearch.matchesText(it.name, query) }
            val actual = index.find(query)
            assertEquals(SongSearch.filter(songs, query), actual.songs)
            assertEquals(expectedAlbums, actual.albums)
            assertEquals(expectedArtists, actual.artists)
            assertEquals(expectedGenres, actual.genres)
        }
    }

    @Test fun filteringSameCatalogReusesOriginalGroupObjectWithoutRebuilding() {
        val songs = listOf(song(10, "Đường về"), song(11, "Nhà xa"))
        val index = AdvancedMusicSearchIndex(songs)
        val firstAlbum = index.find("album").albums.first()
        val secondAlbum = index.find("album duong").albums.first()
        assertSame(firstAlbum, secondAlbum)
        assertEquals(listOf("Đường về", "Nhà xa"), firstAlbum.tracks.map { it.title })
    }

    @Test fun largeCatalogSearchIsCorrectAndKeepsPlaybackQueueOrder() {
        val songs = (1..1502).map { id ->
            song(id, if (id % 3 == 0) "Đường về $id" else "Mùa nắng $id",
                 artist = "Artist ${id % 12}", album = "Album ${id % 25}",
                 genres = listOf("Genre ${id % 7}"))
        }
        val index = AdvancedMusicSearchIndex(songs)
        for (query in listOf("duong", "mua", "artist 7", "genre 3", "album 10")) {
            val result = index.find(query)
            assertEquals(SongSearch.filter(songs, query), result.songs)
            assertEquals(MusicCollections.albums(songs)
                .filter { SongSearch.matchesText(it.name, query) }, result.albums)
        }
        val queue = index.find("duong").songs
        assertEquals(500, queue.size)
        assertEquals("Đường về 3", queue.first().title)
        assertEquals("Đường về 1500", queue.last().title)
    }

    @Test fun blankQueriesNeverExposeAllCatalogItems() {
        val index = AdvancedMusicSearchIndex(listOf(song(11, "Đường về")))
        assertTrue(index.find("").songs.isEmpty())
        assertTrue(index.find("   ").albums.isEmpty())
        assertTrue(index.find("\n\t").genres.isEmpty())
    }

    @Test fun refreshRevocationAndQueryChangesDoNotReuseStaleCatalog() = runBlocking {
        var catalog = listOf(song(11, "Đường về"))
        var scans = 0
        val repository = object : LocalMusicRepository {
            override suspend fun getSongs(): List<DeviceSong> {
                scans++
                return catalog
            }
        }
        val vm = SearchViewModel(repository, initialQuery = "duong")
        vm.refresh()
        withTimeout(10_000) {
            vm.uiState.first { it.isLoaded && !it.isFiltering && it.results.size == 1 }
        }
        assertEquals(1, scans)
        vm.updateQuery("khong co")
        withTimeout(10_000) {
            vm.uiState.first { it.isLoaded && !it.isFiltering && it.query == "khong co" }
        }
        assertEquals(1, scans)
        vm.clearSongs()
        assertTrue(vm.uiState.value.results.isEmpty())
        assertFalse(vm.uiState.value.isLoaded)
        catalog = listOf(song(22, "Mùa hạ"))
        vm.refresh()
        withTimeout(10_000) {
            vm.uiState.first { it.isLoaded && !it.isFiltering }
        }
        assertEquals(2, scans)
        assertTrue(vm.uiState.value.results.isEmpty())
        vm.updateQuery("mua")
        withTimeout(10_000) {
            vm.uiState.first { it.query == "mua" && !it.isFiltering &&
                it.results.firstOrNull()?.title == "Mùa hạ" }
        }
        assertEquals(2, scans)
    }
}
