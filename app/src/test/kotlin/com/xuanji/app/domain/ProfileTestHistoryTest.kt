package com.xuanji.app.domain

import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.data.local.ProfileTestHistoryStore
import com.xuanji.app.data.model.TestRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProfileTestHistoryTest {
    @Test fun scoresStayProfileScopedAndComparisonUsesOnlySameInstrumentVersion() = runBlocking {
        val data = mutableMapOf<String, String>()
        val store = ProfileTestHistoryStore(object : PreferenceBridge {
            override suspend fun read(key: String) = data[key]
            override suspend fun write(key: String, value: String) { data[key] = value }
            override suspend fun delete(key: String) { data.remove(key) }
        })
        val first = TestRecord("自编大五", "性格", "A", "摘要", "2026-10-19 10:00", rawScores = mapOf("O" to 25))
        store.add("one", first)
        store.add("one", first.copy(date = "2026-10-19 11:00", rawScores = mapOf("O" to 30)))
        val restored = store.load("one")
        assertEquals(2, restored.size)
        assertTrue(store.load("two").isEmpty())
        assertEquals(mapOf("O" to 5), TestHistoryComparison.differences(restored[1], restored[0]))
        assertTrue(TestHistoryComparison.differences(restored[1], restored[0].copy(scoringVersion = "other")).isEmpty())
        store.clear("one")
        assertTrue(store.load("one").isEmpty())
    }
    @Test fun corruptHistoryIsProtectedRatherThanSilentlyReset() = runBlocking {
        val data = mutableMapOf<String, String>()
        val store = ProfileTestHistoryStore(object : PreferenceBridge {
            override suspend fun read(key: String) = data[key]
            override suspend fun write(key: String, value: String) { data[key] = value }
            override suspend fun delete(key: String) { data.remove(key) }
        })
        store.add("one", TestRecord("测试", "趣味", "A", "摘要", "2026-10-19 10:00"))
        data[data.keys.single()] = "broken"
        assertTrue(runCatching { store.add("one", TestRecord("测试", "趣味", "B", "摘要", "2026-10-19 11:00")) }.isFailure)
        assertEquals("broken", data.values.single())
    }
    @Test fun lateCompletedResultCannotReappearAfterExplicitClear() = runBlocking {
        val data = mutableMapOf<String, String>()
        val store = ProfileTestHistoryStore(object : PreferenceBridge {
            override suspend fun read(key: String) = data[key]
            override suspend fun write(key: String, value: String) { data[key] = value }
            override suspend fun delete(key: String) { data.remove(key) }
        })
        val ticket = ProfileTestHistoryStore.epoch("one")
        store.clear("one")
        assertTrue(runCatching { store.add("one", TestRecord("测试", "趣味", "A", "摘要", "2026-10-19 10:00"), ticket) }.isFailure)
        assertTrue(store.load("one").isEmpty())
    }
    @Test fun completeDataEraseAlsoFencesPendingQuestionnaireAndHistoryWrites() = runBlocking {
        val data = mutableMapOf<String, String>()
        val bridge = object : PreferenceBridge {
            override suspend fun read(key: String) = data[key]
            override suspend fun write(key: String, value: String) { data[key] = value }
            override suspend fun delete(key: String) { data.remove(key) }
        }
        val draftTicket = com.xuanji.app.data.local.QuestionnaireDraftStore.epoch("one")
        val resultTicket = ProfileTestHistoryStore.epoch("one")
        com.xuanji.app.data.local.LocalDataEraseFence.erase { data.clear() }
        val spec = com.xuanji.app.domain.test.QuestionnaireSpec("bigfive", listOf(setOf("1")))
        assertTrue(runCatching { com.xuanji.app.data.local.QuestionnaireDraftStore(bridge).save("one", spec,
            com.xuanji.app.domain.test.QuestionnaireDraft(1, pick = "1"), draftTicket) }.isFailure)
        assertTrue(runCatching { ProfileTestHistoryStore(bridge).add("one", TestRecord("测试", "趣味", "A", "摘要", "2026-10-19 10:00"), resultTicket) }.isFailure)
        assertTrue(data.isEmpty())
    }
}
