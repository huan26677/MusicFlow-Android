package com.example.musicflow.core.community

/**
 * In-memory, disposable demo. Never calls Supabase, uses no account token and
 * cannot create a public community rating or moderator permission.
 */
class DemoCommunityStore {
    private var stars = 0
    private var comments = listOf(
        CommunityComment("demo-1", "Giai điệu dễ nghe, mình rất thích!",
            "Người nghe mẫu A", false, "Dữ liệu mẫu"),
        CommunityComment("demo-2", "Phần hòa âm khá thú vị.", "Người nghe mẫu B",
            false, "Dữ liệu mẫu")
    )
    private var nextId = 3

    fun read(offset: Int = 0) = CommunitySnapshot(
        averageStars = if (stars == 0) 4.5 else (4.5 * 2 + stars) / 3,
        ratingCount = if (stars == 0) 2 else 3,
        myStars = stars,
        commentCount = comments.size,
        comments = comments.drop(offset).take(CommunityRules.PAGE_SIZE),
        authenticated = true,
        demo = true
    )

    fun rate(value: Int) {
        require(CommunityRules.validStars(value))
        stars = value
    }

    fun create(body: String) {
        val clean = CommunityRules.cleanComment(body)
        comments = listOf(
            CommunityComment("demo-${nextId++}",clean,"Bạn (Demo)",true,"Vừa xong")
        ) + comments
    }

    fun edit(id: String, body: String) {
        val clean = CommunityRules.cleanComment(body)
        require(comments.any { it.id == id && it.mine }) { "Không phải bình luận của bạn." }
        comments = comments.map { if (it.id == id) it.copy(body = clean) else it }
    }

    fun delete(id: String) {
        require(comments.any { it.id == id && it.mine }) { "Không phải bình luận của bạn." }
        comments = comments.filterNot { it.id == id }
    }

    fun report(id: String, reason: String) {
        CommunityRules.cleanReason(reason)
        require(comments.any { it.id == id && !it.mine }) {
            "Không thể báo cáo bình luận này."
        }
    }
}
