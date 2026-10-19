package com.xuanji.app.domain

import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.data.local.QuestionnaireDraftStore
import com.xuanji.app.domain.test.QuestionnaireDraft
import com.xuanji.app.domain.test.QuestionnaireSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class QuestionnaireDraftTest {
    private val spec = QuestionnaireSpec("synthetic", listOf(setOf("A", "B"), setOf("1", "2")))
    private class Memory : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String) = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }
    @Test fun progressSurvivesNewStoreButDoesNotCrossProfileOrQuestionnaire() = runBlocking {
        val memory = Memory()
        QuestionnaireDraftStore(memory).save("one", spec, QuestionnaireDraft(2, listOf("B"), "2"))
        assertEquals(listOf("B"), QuestionnaireDraftStore(memory).load("one", spec).answers)
        assertEquals("2", QuestionnaireDraftStore(memory).load("one", spec).pick)
        assertTrue(QuestionnaireDraftStore(memory).load("two", spec).answers.isEmpty())
        assertTrue(QuestionnaireDraftStore(memory).load("one", spec.copy(id = "other")).answers.isEmpty())
    }
    @Test fun missingInvalidAndChangedQuestionBanksCannotBeScored() {
        assertFalse(spec.complete(QuestionnaireDraft(3, listOf("A"))))
        assertFalse(spec.complete(QuestionnaireDraft(3, listOf("A", "A"))))
        assertTrue(spec.complete(QuestionnaireDraft(3, listOf("B", "2"))))
        assertNotEquals(spec.fingerprint, spec.copy(options = listOf(setOf("B"), setOf("1", "2"))).fingerprint)
    }
    @Test fun corruptFutureAndForeignDocumentsAreNotOverwritten() = runBlocking {
        val memory = Memory()
        val store = QuestionnaireDraftStore(memory)
        store.save("one", spec, QuestionnaireDraft(1))
        val key = memory.values.keys.single()
        for (raw in listOf("broken", memory.values.getValue(key).replace("\"version\":1", "\"version\":99"))) {
            memory.values[key] = raw
            assertFalse(store.load("one", spec).readable)
            assertTrue(runCatching { store.save("one", spec, QuestionnaireDraft()) }.isFailure)
            assertEquals(raw, memory.values[key])
        }
        store.clear("one", spec)
        assertTrue(store.load("one", spec).readable)
    }
    @Test fun restartAndEditsDoNotSilentlyLoseLaterAnswers() = runBlocking {
        val memory = Memory(); val store = QuestionnaireDraftStore(memory)
        store.save("one", spec, QuestionnaireDraft(3, listOf("B", "2")))
        store.save("one", spec, store.load("one", spec).copy(answers = listOf("A", "2")))
        assertEquals(listOf("A", "2"), store.load("one", spec).answers)
        store.clear("one", spec)
        assertEquals(0, store.load("one", spec).index)
    }
    @Test fun companionReferenceRequiresExplicitProfileBoundConsent() = runBlocking {
        val memory = Memory()
        val store = com.xuanji.app.data.local.TestReferenceConsentStore(memory)
        val record = com.xuanji.app.data.model.TestRecord("自编", "趣味", "A", "摘要", "2026-10-19 12:00")
        assertNull(store.authorized("one", listOf(record)))
        store.grant("one", record)
        assertEquals(record, store.authorized("one", listOf(record)))
        assertNull(store.authorized("two", listOf(record)))
        assertNull(store.authorized("one", listOf(record.copy(resultCode = "B"))))
        assertNull(store.authorized("one", listOf(record.copy(rawScores = mapOf("O" to 40)))))
        store.revoke("one")
        assertNull(store.authorized("one", listOf(record)))
    }
    @Test fun clearingProfileDraftsInvalidatesOldAutosaveTicket() = runBlocking {
        val memory = Memory(); val store = QuestionnaireDraftStore(memory)
        val realSpec = QuestionnaireSpec("bigfive", listOf(setOf("A", "B")))
        val ticket = QuestionnaireDraftStore.epoch("one")
        store.save("one", realSpec, QuestionnaireDraft(1, pick = "A"), ticket)
        store.clearProfile("one")
        assertTrue(runCatching { store.save("one", realSpec, QuestionnaireDraft(1, pick = "B"), ticket) }.isFailure)
        assertTrue(store.load("one", realSpec).answers.isEmpty())
    }
}
