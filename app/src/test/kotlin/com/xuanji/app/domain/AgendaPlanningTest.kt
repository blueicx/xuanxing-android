package com.xuanji.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AgendaPlanningTest {
    private fun item(id: String = "a", date: String = "2026-10-18", time: String = "12:00", duration: Int = 60) =
        DailyAgendaItem(id, date, time, "午餐", "meal", durationMinutes = duration)

    @Test fun overlapping_intervals_conflict_but_adjacent_intervals_do_not() {
        val existing = listOf(item())
        assertEquals(1, AgendaConflictRules.conflicts(existing, item("b", time = "12:30")).size)
        assertTrue(AgendaConflictRules.conflicts(existing, item("b", time = "13:00")).isEmpty())
        assertTrue(AgendaConflictRules.conflicts(existing, item("b", time = "11:00")).isEmpty())
    }

    @Test fun midnight_and_unknown_duration_points_are_handled_without_inventing_duration() {
        val night = item(time = "23:30", duration = 90)
        assertEquals(listOf(night), AgendaConflictRules.conflicts(listOf(night), item("b", "2026-10-19", "00:15", 15)))
        assertEquals(1, AgendaConflictRules.conflicts(listOf(item()), item("b", time = "12:30", duration = 0)).size)
        assertTrue(AgendaConflictRules.conflicts(listOf(item()), item("b", time = "13:00", duration = 0)).isEmpty())
        assertTrue(AgendaConflictRules.conflicts(listOf(item()), item("b", time = "", duration = 60)).isEmpty())
        assertTrue(AgendaConflictRules.conflicts(listOf(item().copy(completed = true)), item("b")).isEmpty())
    }

    @Test fun finite_repeat_is_deterministic_excludes_original_and_rejects_invalid_ranges() {
        val original = item()
        val first = AgendaPlanning.repeat(original, LocalDate.parse("2026-10-18"), 2, setOf(1, 7))
        assertEquals(listOf("2026-10-19", "2026-10-25", "2026-10-26"), first.map { it.dateKey })
        assertEquals(first, AgendaPlanning.repeat(original, LocalDate.parse("2026-10-18"), 2, setOf(1, 7)))
        assertEquals(first.size, first.map { it.id }.distinct().size)
        assertTrue(first.all { !it.completed && it.durationMinutes == 60 })
        assertTrue(runCatching { AgendaPlanning.repeat(original, LocalDate.now(), 53, setOf(1)) }.isFailure)
    }

    @Test fun free_slots_consider_cross_midnight_busy_intervals() {
        val slots = AgendaPlanning.freeSlots(listOf(item(time = "23:00", duration = 120)),
            LocalDate.parse("2026-10-19"), 30, LocalTime.MIDNIGHT, LocalTime.of(2, 0))
        assertEquals(listOf("01:00", "01:15", "01:30"), slots)
        val occupied = AgendaPlanning.freeSlots(listOf(item("slot-probe", time = "08:00", duration = 60)),
            LocalDate.parse("2026-10-18"), 30, LocalTime.of(8, 0), LocalTime.of(9, 0))
        assertTrue(occupied.isEmpty())
    }

    @Test fun old_documents_migrate_to_unknown_duration_and_invalid_duration_is_rejected() {
        val root = com.google.gson.JsonParser.parseString(DailyLifeCodec.encode("p", DailyLifeDocument(agenda = listOf(item())))).asJsonObject
        root.addProperty("version", 3)
        root.getAsJsonObject("document").getAsJsonArray("agenda").single().asJsonObject.remove("durationMinutes")
        val migrated = DailyLifeCodec.decode(root.toString(), "p")
        assertTrue(migrated.readable)
        assertEquals(0, migrated.agenda.single().durationMinutes)
        assertFalse(DailyLifeCodec.valid(DailyLifeDocument(agenda = listOf(item(duration = -1)))))
        assertFalse(DailyLifeCodec.valid(DailyLifeDocument(agenda = listOf(item(duration = 1441)))))
    }

    @Test fun backup_preserves_duration_without_exporting_unselected_notes() {
        val original = item().copy(note = "私人备注")
        val json = LocalDataBackupCodec.encode("p", LocalBackupSelection(agenda = true), LocalBackupPayload(agenda = listOf(original)), 1)
        val restored = LocalDataBackupCodec.decode(json, "p").backup!!.payload.agenda.single()
        assertEquals(60, restored.durationMinutes)
        assertEquals("", restored.note)
        assertEquals(8, LocalDataBackupCodec.VERSION)
    }

    @Test fun old_v4_backup_missing_duration_is_verified_then_migrated_without_guessing() {
        val original = item()
        val json = LocalDataBackupCodec.encode("p", LocalBackupSelection(agenda = true), LocalBackupPayload(agenda = listOf(original)), 1)
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        root.addProperty("version", 4)
        root.getAsJsonObject("payload").getAsJsonArray("agenda").single().asJsonObject.remove("durationMinutes")
        root.remove("checksum")
        root.addProperty("checksum", com.xuanji.app.domain.action.StableScore.sha256(root.toString()))
        val restored = LocalDataBackupCodec.decode(root.toString(), "p").backup!!.payload.agenda.single()
        assertEquals(0, restored.durationMinutes)
        assertEquals(original.portions, restored.portions)
    }
}
