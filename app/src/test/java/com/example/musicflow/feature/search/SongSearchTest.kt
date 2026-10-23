
package com.example.musicflow.feature.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSearchTest {

    @Test
    fun searchBySongTitle() {
        assertTrue(
            SongSearch.matches(
                title = "Nàng Thơ",
                artist = "Hoàng Dũng",
                query = "nang tho"
            )
        )
    }

    @Test
    fun searchByArtist() {
        assertTrue(
            SongSearch.matches(
                title = "Một Bước Yêu Vạn Dặm Đau",
                artist = "Mr. Siro",
                query = "mr. siro"
            )
        )
    }

    @Test
    fun ignoreUppercase() {
        assertTrue(
            SongSearch.matches(
                title = "Đi Về Nhà",
                artist = "Đen Vâu",
                query = "DI VE NHA"
            )
        )
    }

    @Test
    fun normalizeVietnameseLetterD() {
        assertTrue(
            SongSearch.matches(
                title = "Đường Về Nhà",
                artist = "Đen Vâu",
                query = "duong"
            )
        )
    }

    @Test
    fun searchMultipleWordsInDifferentOrder() {
        assertTrue(
            SongSearch.matches(
                title = "Trời Hôm Nay",
                artist = "Đen Vâu",
                query = "vau troi"
            )
        )
    }

    @Test
    fun ignoreExtraSpaces() {
        assertTrue(
            SongSearch.matches(
                title = "Hạ Còn Vương Nắng",
                artist = "DatKaa",
                query = "  ha   con   nang  "
            )
        )
    }

    @Test
    fun emptyQueryDoesNotMatch() {
        assertFalse(
            SongSearch.matches(
                title = "Nàng Thơ",
                artist = "Hoàng Dũng",
                query = "   "
            )
        )
    }

    @Test
    fun unrelatedQueryDoesNotMatch() {
        assertFalse(
            SongSearch.matches(
                title = "Nàng Thơ",
                artist = "Hoàng Dũng",
                query = "mua dong"
            )
        )
    }
}
