package com.example.musicflow.core.community

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoCommunityStoreTest {
    @Test fun demoSeedIsClearlyFlaggedAndDoesNotUseCloudId() {
        val snapshot = DemoCommunityStore().read()
        assertTrue(snapshot.demo)
        assertTrue(snapshot.authenticated)
        assertEquals(2,snapshot.commentCount)
        assertEquals(2,snapshot.ratingCount)
    }
    @Test fun changingRatingUpdatesOnlyOneDemoRating() {
        val demo = DemoCommunityStore()
        demo.rate(2)
        demo.rate(5)
        assertEquals(3,demo.read().ratingCount)
        assertEquals(5,demo.read().myStars)
    }
    @Test fun myCommentCanBeAddedEditedAndDeleted() {
        val demo = DemoCommunityStore()
        demo.create("Mình nghe thử")
        val mine = demo.read().comments.first()
        assertTrue(mine.mine)
        assertEquals(3,demo.read().commentCount)
        demo.edit(mine.id,"Chỉnh sửa")
        assertEquals("Chỉnh sửa",demo.read().comments.first().body)
        demo.delete(mine.id)
        assertEquals(2,demo.read().commentCount)
    }
    @Test fun cannotEditOrDeleteSomeoneElsesComment() {
        val demo = DemoCommunityStore()
        assertTrue(runCatching { demo.edit("demo-1","Không được") }.isFailure)
        assertTrue(runCatching { demo.delete("demo-1") }.isFailure)
    }
    @Test fun cannotReportSelfOrUseInvalidReason() {
        val demo = DemoCommunityStore()
        demo.create("Của tôi")
        val id = demo.read().comments.first().id
        assertTrue(runCatching { demo.report(id,"spam rác") }.isFailure)
        assertTrue(runCatching { demo.report("demo-1","spam") }.isFailure)
        demo.report("demo-1","Bình luận spam quá nhiều")
    }
    @Test fun demoUsesPageSizeAndFreshInstanceResets() {
        val demo = DemoCommunityStore()
        repeat(30) { demo.create("Comment $it") }
        assertEquals(20,demo.read().comments.size)
        assertEquals(12,demo.read(20).comments.size)
        assertFalse(DemoCommunityStore().read().myStars > 0)
    }
}
