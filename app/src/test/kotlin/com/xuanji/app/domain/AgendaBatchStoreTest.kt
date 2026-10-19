package com.xuanji.app.domain

import com.xuanji.app.data.local.DailyLifeStore
import com.xuanji.app.data.local.PreferenceBridge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AgendaBatchStoreTest {
    private class Memory : PreferenceBridge {
        val data = mutableMapOf<String, String>()
        override suspend fun read(key: String) = data[key]
        override suspend fun write(key: String, value: String) { data[key] = value }
        override suspend fun delete(key: String) { data.remove(key) }
    }
    private val original = DailyAgendaItem("source", "2026-10-18", "09:00", "阅读", "custom", durationMinutes = 30)
    private fun copies() = AgendaPlanning.repeat(original, LocalDate.parse("2026-10-19"), 1, setOf(1, 2))

    @Test fun batch_is_idempotent_restorable_and_profile_isolated() = runBlocking {
        val memory = Memory(); val store = DailyLifeStore(memory)
        assertEquals(2, store.addAgendaBatch("a", copies()).copied.size)
        assertTrue(store.addAgendaBatch("a", copies()).copied.isEmpty())
        assertEquals(2, DailyLifeStore(memory).load("a").agenda.size)
        assertTrue(store.load("b").agenda.isEmpty())
        store.setCompleted("a", copies().first().id, true)
        store.addAgendaBatch("a", copies())
        assertTrue(store.load("a").agenda.first { it.id == copies().first().id }.completed)
    }

    @Test fun one_collision_blocks_whole_batch_without_writes() = runBlocking {
        val memory = Memory(); val store = DailyLifeStore(memory)
        store.upsertAgenda("a", original.copy(id = "busy", dateKey = "2026-10-20", time = "09:15"))
        val before = memory.data.toMap()
        val result = store.addAgendaBatch("a", copies())
        assertTrue(result.copied.isEmpty())
        assertEquals("busy", result.conflicts.single().id)
        assertEquals(before, memory.data)
    }

    @Test fun capacity_corruption_duplicate_ids_and_invalid_records_never_partially_write() = runBlocking {
        val memory = Memory(); val store = DailyLifeStore(memory)
        val many = (1..500).map { original.copy(id = "a$it", time = "") }
        store.replace("a", DailyLifeDocument(agenda = many))
        val before = memory.data.toMap()
        assertTrue(runCatching { store.addAgendaBatch("a", copies()) }.isFailure)
        assertEquals(before, memory.data)
        assertTrue(runCatching { store.addAgendaBatch("b", copies() + copies()) }.isFailure)
        assertTrue(store.load("b").agenda.isEmpty())
        memory.write(store.storageKey("broken"), "broken")
        assertTrue(runCatching { store.addAgendaBatch("broken", copies()) }.isFailure)
        assertEquals("broken", memory.read(store.storageKey("broken")))
    }

    @Test fun shifting_a_day_is_atomic_preserves_ids_and_skips_completed_items() = runBlocking {
        val memory = Memory(); val store = DailyLifeStore(memory)
        store.upsertAgenda("a", original)
        store.upsertAgenda("a", original.copy(id = "done", time = "11:00", completed = true))
        store.upsertAgenda("a", original.copy(id = "busy", dateKey = "2026-10-19", time = "09:15"))
        val before = memory.data.toMap()
        assertTrue(store.moveAgendaDate("a", original.dateKey, "2026-10-19").conflicts.isNotEmpty())
        assertEquals(before, memory.data)
        assertEquals(1, store.moveAgendaDate("a", original.dateKey, "2026-10-20").copied.size)
        val restored = DailyLifeStore(memory).load("a").agenda
        assertEquals("2026-10-20", restored.single { it.id == "source" }.dateKey)
        assertEquals("2026-10-18", restored.single { it.id == "done" }.dateKey)
        assertTrue(restored.single { it.id == "done" }.completed)
    }
}
