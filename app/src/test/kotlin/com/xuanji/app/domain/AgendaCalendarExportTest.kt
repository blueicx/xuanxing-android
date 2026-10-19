package com.xuanji.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AgendaCalendarExportTest {
    private val date = LocalDate.parse("2026-10-18")
    private val now = Instant.parse("2026-10-18T00:00:00Z")
    private val item = DailyAgendaItem("private-id", date.toString(), "23:30", "阅读", "custom", durationMinutes = 90, note = "私人备注")
    private fun export(items: List<DailyAgendaItem>, notes: Boolean = false) = AgendaCalendarExport.encode(items, date, date, notes, now)

    @Test fun timed_and_all_day_records_preserve_local_time_and_unknown_end() {
        val calendar = export(listOf(item, item.copy(id = "all-day", time = ""), item.copy(id = "unknown", time = "09:00", durationMinutes = 0)))
        assertTrue(calendar.contains("DTSTART:20261018T233000\r\nDTEND:20261019T010000"))
        assertTrue(calendar.contains("DTSTART;VALUE=DATE:20261018"))
        val unknown = calendar.split("BEGIN:VEVENT").first { it.contains("DTSTART:20261018T090000") }
        assertFalse(unknown.contains("DTEND"))
        assertFalse(calendar.contains("私人备注"))
        assertFalse(calendar.contains("private-id"))
    }

    @Test fun text_cannot_inject_calendar_fields_and_long_utf8_lines_are_folded() {
        val calendar = export(listOf(item.copy(title = "中文".repeat(70) + ",;\\\r\nEND:VEVENT", note = "第一行\n第二行")), true)
        assertEquals(1, calendar.split("\r\n").count { it == "END:VEVENT" })
        assertTrue(calendar.split("\r\n").all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        assertFalse(calendar.contains('\uFFFD'))
        val unfolded = calendar.replace("\r\n ", "")
        assertTrue(unfolded.contains("\\,\\;\\\\\\nEND:VEVENT"))
        assertTrue(unfolded.contains("DESCRIPTION:第一行\\n第二行"))
    }

    @Test fun range_selection_is_explicit_and_deterministic() {
        assertEquals(export(listOf(item)), export(listOf(item.copy(id = "other", dateKey = "2026-10-19"), item)))
        val calendar = export(listOf(item.copy(dateKey = "2026-10-19")), false)
        assertFalse(calendar.contains("BEGIN:VEVENT"))
        assertTrue(runCatching { AgendaCalendarExport.encode(listOf(item), date, date.minusDays(1), false, now) }.isFailure)
        assertEquals(export(listOf(item)), export(listOf(item)))
    }
}
