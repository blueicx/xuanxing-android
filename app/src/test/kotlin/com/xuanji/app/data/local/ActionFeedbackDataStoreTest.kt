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
}
