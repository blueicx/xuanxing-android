package com.xuanji.app.domain

import com.xuanji.app.domain.action.StableScore
import java.time.LocalDate
import java.time.LocalTime

/** Finite explicit schedules, not an implicit background recurring-task generator. */
object AgendaPlanning {
    const val MAX_REPEAT_WEEKS = 12

    fun repeat(source: DailyAgendaItem, startDate: LocalDate, weeks: Int, weekdays: Set<Int>): List<DailyAgendaItem> {
        require(weeks in 1..MAX_REPEAT_WEEKS && weekdays.isNotEmpty() && weekdays.all { it in 1..7 })
        require(DailyLifeCodec.valid(DailyLifeDocument(agenda = listOf(source))))
        return (0L until weeks * 7L).map { startDate.plusDays(it) }
            .filter { it.dayOfWeek.value in weekdays && it.toString() != source.dateKey }
            .map { date -> source.copy(
                id = "repeat-" + StableScore.sha256("${source.id}:$date").take(40),
                dateKey = date.toString(), completed = false
            ) }
    }

    fun freeSlots(
        existing: List<DailyAgendaItem>, date: LocalDate, durationMinutes: Int,
        from: LocalTime = LocalTime.of(8, 0), until: LocalTime = LocalTime.of(20, 0)
    ): List<String> {
        require(durationMinutes in 1..1440 && until > from)
        val first = date.atTime(from)
        val end = date.atTime(until)
        return generateSequence(first) { it.plusMinutes(15) }
            .takeWhile { it.plusMinutes(durationMinutes.toLong()) <= end }
            .filter { time -> AgendaConflictRules.conflicts(existing,
                DailyAgendaItem("", date.toString(), time.toLocalTime().toString(), "空闲时段", "custom", durationMinutes = durationMinutes)
            ).isEmpty() }
            .map { it.toLocalTime().toString() }.toList()
    }
}
