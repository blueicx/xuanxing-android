package com.xuanji.app.domain

import com.xuanji.app.domain.action.StableScore
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** User-selected local appointments only; floating times intentionally carry no inferred timezone. */
object AgendaCalendarExport {
    private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd", java.util.Locale.ROOT)
    private val timeFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", java.util.Locale.ROOT)

    fun encode(
        items: List<DailyAgendaItem>, from: LocalDate, through: LocalDate,
        includeNotes: Boolean = false, generatedAt: Instant
    ): String {
        require(through >= from)
        require(DailyLifeCodec.valid(DailyLifeDocument(agenda = items)))
        val stamp = timeFormat.withZone(ZoneOffset.UTC).format(generatedAt) + "Z"
        val lines = mutableListOf("BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Xuanji//Local Agenda//ZH", "CALSCALE:GREGORIAN")
        items.filter { LocalDate.parse(it.dateKey) in from..through }
            .sortedWith(compareBy<DailyAgendaItem> { it.dateKey }.thenBy { it.time }.thenBy { it.id })
            .forEach { item ->
                val date = LocalDate.parse(item.dateKey)
                lines += "BEGIN:VEVENT"
                lines += "UID:${StableScore.sha256(item.id)}@xuanji.local"
                lines += "DTSTAMP:$stamp"
                if (item.time.isBlank()) {
                    lines += "DTSTART;VALUE=DATE:${dateFormat.format(date)}"
                    lines += "DTEND;VALUE=DATE:${dateFormat.format(date.plusDays(1))}"
                } else {
                    val start = date.atTime(LocalTime.parse(item.time))
                    lines += "DTSTART:${timeFormat.format(start)}"
                    if (item.durationMinutes > 0) lines += "DTEND:${timeFormat.format(start.plusMinutes(item.durationMinutes.toLong()))}"
                }
                lines += "SUMMARY:${escape(item.title)}"
                if (includeNotes && item.note.isNotBlank()) lines += "DESCRIPTION:${escape(item.note)}"
                // Planned entries should not silently reserve the receiving calendar's availability.
                lines += "TRANSP:TRANSPARENT"
                lines += "END:VEVENT"
            }
        lines += "END:VCALENDAR"
        return lines.joinToString("\r\n", postfix = "\r\n") { fold(it) }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\")
        .replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\\n")
        .replace(";", "\\;").replace(",", "\\,")

    /** RFC 5545 folding counts UTF-8 octets and never splits a code point. */
    private fun fold(value: String): String = buildString {
        var bytes = 0
        var index = 0
        while (index < value.length) {
            val point = value.codePointAt(index)
            val part = String(Character.toChars(point))
            val size = part.toByteArray(Charsets.UTF_8).size
            if (bytes + size > 75) { append("\r\n "); bytes = 1 }
            append(part); bytes += size; index += Character.charCount(point)
        }
    }
}
