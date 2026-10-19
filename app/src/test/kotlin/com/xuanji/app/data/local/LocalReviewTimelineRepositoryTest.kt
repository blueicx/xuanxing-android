package com.xuanji.app.data.local

import com.xuanji.app.domain.LocalReviewSource
import com.xuanji.app.domain.ReadingFavorite
import com.xuanji.app.domain.action.ActionFeedback
import com.xuanji.app.domain.action.ActionFeedbackKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalReviewTimelineRepositoryTest {
    private class MemoryBridge : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun merges_the_two_current_profile_sources_and_keeps_other_profile_records_out() = runTest {
        val bridge = MemoryBridge()
        val favorites = ReadingFavoriteStore(bridge)
        val feedback = ActionFeedbackDataStore(bridge)
        val repository = LocalReviewTimelineRepository(favorites, feedback)
        favorites.upsert("favorite-a", favorite("fa", 10L))
        favorites.upsert("favorite-b", favorite("fb", 20L))
        feedback.append("action-a", feedback("aa", ActionFeedbackKind.Accepted, 30L))
        feedback.append("action-b", feedback("ab", ActionFeedbackKind.NotSuitable, 40L))

        val snapshot = repository.load("favorite-a", "action-a")

        assertEquals(listOf("feedback:aa", "favorite:fa"), snapshot.items.map { it.id })
        assertTrue(snapshot.feedbackAvailable)
        assertEquals(2, repository.load("favorite-a", "action-a").items.size)
    }

    @Test
    fun filters_real_sources_and_does_not_invent_entries_when_stores_are_missing() = runTest {
        val bridge = MemoryBridge()
        val favorites = ReadingFavoriteStore(bridge)
        val feedback = ActionFeedbackDataStore(bridge)
        val repository = LocalReviewTimelineRepository(favorites, feedback)
        favorites.upsert("favorite-a", favorite("fa", 10L))
        feedback.append("action-a", feedback("aa", ActionFeedbackKind.Replaced, 30L))

        assertEquals(listOf(LocalReviewSource.Favorite), repository.load("favorite-a", "action-a", com.xuanji.app.domain.LocalReviewFilter.Favorites).items.map { it.source })
        assertEquals(listOf(LocalReviewSource.ReplacedAction), repository.load("favorite-a", "action-a", com.xuanji.app.domain.LocalReviewFilter.ActionFeedback).items.map { it.source })
        val empty = repository.load(null, null)
        assertTrue(empty.items.isEmpty())
        assertFalse(empty.favoriteState == com.xuanji.app.domain.ReadingFavoriteDataState.Ready)
    }

    private fun favorite(id: String, at: Long) = ReadingFavorite(
        id = id, type = "fortune", title = "快照$id", body = "保存时结果", createdAt = at
    )

    private fun feedback(id: String, kind: ActionFeedbackKind, at: Long) = ActionFeedback(
        id = id, dateKey = "2026-10-03", category = "meal", candidateKey = "candidate-$id", kind = kind,
        createdAtEpochMs = at
    )
}
