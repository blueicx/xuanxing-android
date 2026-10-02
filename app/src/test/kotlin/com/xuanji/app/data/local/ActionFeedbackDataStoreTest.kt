package com.xuanji.app.data.local

import com.xuanji.app.domain.action.ActionFeedback
import com.xuanji.app.domain.action.ActionFeedbackKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionFeedbackDataStoreTest {
    private class FakeBridge : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun feedback_does_not_cross_profile_boundaries() = runBlocking {
        val store = ActionFeedbackDataStore(FakeBridge())
        store.append("a", ActionFeedback("1", "2026-09-19", "meal", "oat-berry", ActionFeedbackKind.Accepted, createdAtEpochMs = 1L))
        assertEquals(1, store.list("a").size)
        assertTrue(store.list("b").isEmpty())
    }

    @Test
    fun removing_replacement_restores_only_that_candidate_feedback() = runBlocking {
        val store = ActionFeedbackDataStore(FakeBridge())
        val replacement = ActionFeedback("replace-a", "2026-09-19", "meal", "oat-berry", ActionFeedbackKind.Replaced, createdAtEpochMs = 1L)
        val accepted = ActionFeedback("accept-b", "2026-09-19", "meal", "lotus-porridge", ActionFeedbackKind.Accepted, createdAtEpochMs = 2L)
        store.append("a", replacement)
        store.append("a", accepted)

        val removed = store.remove("a", replacement.id)

        assertEquals(replacement, removed)
        assertEquals(listOf(accepted), store.list("a"))
        assertTrue(store.remove("a", "missing") == null)
    }

    @Test
    fun clear_removes_feedback_for_only_the_selected_profile() = runBlocking {
        val store = ActionFeedbackDataStore(FakeBridge())
        store.append("a", ActionFeedback("a1", "2026-09-19", "meal", "oat-berry", ActionFeedbackKind.Accepted))
        store.append("b", ActionFeedback("b1", "2026-09-19", "meal", "oat-berry", ActionFeedbackKind.Accepted))

        store.clear("a")

        assertTrue(store.list("a").isEmpty())
        assertEquals(1, store.list("b").size)
    }
}
