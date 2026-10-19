package com.xuanji.app.domain

import com.xuanji.app.domain.action.ActionFeedback
import com.xuanji.app.domain.action.ActionFeedbackKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalReviewTimelineTest {
    @Test
    fun merges_only_explicit_favorites_and_action_feedback_in_newest_first_order() {
        val favorites = listOf(
            favorite("f-old", 100L),
            favorite("f-new", 400L)
        )
        val feedback = listOf(
            feedback("accepted", ActionFeedbackKind.Accepted, 300L),
            feedback("replaced", ActionFeedbackKind.Replaced, 200L)
        )

        val items = LocalReviewTimeline.build(favorites, feedback)

        assertEquals(
            listOf("favorite:f-new", "feedback:accepted", "feedback:replaced", "favorite:f-old"),
            items.map { it.id }
        )
        assertTrue(items.none { it.summary.contains("聊天") || it.summary.contains("人格") })
    }

    @Test
    fun filters_by_source_and_returns_empty_for_no_explicit_records() {
        val favorite = favorite("f", 100L)
        val accepted = feedback("accepted", ActionFeedbackKind.Accepted, 200L)

        assertEquals(listOf("favorite:f"), LocalReviewTimeline.build(listOf(favorite), listOf(accepted), LocalReviewFilter.Favorites).map { it.id })
        assertEquals(listOf("feedback:accepted"), LocalReviewTimeline.build(listOf(favorite), listOf(accepted), LocalReviewFilter.ActionFeedback).map { it.id })
        assertTrue(LocalReviewTimeline.build(emptyList(), emptyList()).isEmpty())
    }

    private fun favorite(id: String, at: Long) = ReadingFavorite(
        id = id, type = "fortune", title = "收藏$id", body = "用户主动收藏的快照", createdAt = at
    )

    private fun feedback(id: String, kind: ActionFeedbackKind, at: Long) = ActionFeedback(
        id = id, dateKey = "2026-10-03", category = "meal", candidateKey = "rice-$id", kind = kind,
        createdAtEpochMs = at
    )
}
