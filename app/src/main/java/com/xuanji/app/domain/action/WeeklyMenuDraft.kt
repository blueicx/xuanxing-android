package com.xuanji.app.domain.action

import java.time.LocalDate

/** User-saved planning parameters only. Restored locks must be checked against the current eligible pool. */
data class WeeklyMenuDraft(
    val startDate: String, val budgetYuan: String = "", val variation: Int = 0,
    val locks: List<WeeklyMealLock> = emptyList()
) {
    fun valid(): Boolean = runCatching {
        val start = LocalDate.parse(startDate)
        require(start.toString() == startDate && variation in 0..1_000_000)
        require(budgetYuan.isEmpty() || budgetYuan.matches(Regex("[0-9]{1,6}")) && budgetYuan.toInt() in 0..100000)
        require(locks.size <= 21 && locks.map { it.dateKey to it.slot }.distinct().size == locks.size)
        require(locks.all { LocalDate.parse(it.dateKey) in start..start.plusDays(6) && it.slot in WeeklyMealPlanner.slots &&
            it.candidateKey.isNotBlank() && it.candidateKey.length <= 128 })
        true
    }.getOrDefault(false)
}
