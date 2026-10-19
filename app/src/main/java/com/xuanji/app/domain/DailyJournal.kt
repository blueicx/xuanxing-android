package com.xuanji.app.domain

import java.time.LocalDate

/** User-authored text only. Never populated from generated replies, inferred mood or scores. */
data class DailyJournalEntry(
    val dateKey: String, val text: String = "", val mood: String = "",
    val actualExecution: String = "", val updatedAt: Long = 0
)
data class DailyJournalReview(
    val recordedDays: Int, val moods: Map<String, Int>, val totalAgenda: Int, val completedAgenda: Int
)
object DailyJournalRules {
    const val MAX_ENTRIES = 366
    val moods = listOf("平静", "愉快", "低落", "疲惫", "紧张", "复杂")
    fun valid(entry: DailyJournalEntry): Boolean = runCatching {
        LocalDate.parse(entry.dateKey).toString() == entry.dateKey && entry.text.length <= 4000 &&
            entry.actualExecution.length <= 1000 && (entry.mood.isBlank() || entry.mood in moods) && entry.updatedAt >= 0
    }.getOrDefault(false)

    fun review(document: DailyLifeDocument, from: LocalDate, through: LocalDate): DailyJournalReview {
        require(!through.isBefore(from))
        fun included(key: String) = LocalDate.parse(key).let { !it.isBefore(from) && !it.isAfter(through) }
        val entries = document.journal.filter { included(it.dateKey) }
        val agenda = document.agenda.filter { included(it.dateKey) }
        return DailyJournalReview(entries.size, entries.filter { it.mood.isNotBlank() }.groupingBy { it.mood }.eachCount().toSortedMap(),
            agenda.size, agenda.count { it.completed })
    }
}
