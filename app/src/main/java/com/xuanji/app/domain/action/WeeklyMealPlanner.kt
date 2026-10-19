package com.xuanji.app.domain.action

import com.xuanji.app.domain.DailyAgendaItem
import java.time.LocalDate

data class WeeklyMealLock(val dateKey: String, val slot: MealSlot, val candidateKey: String)
data class PlannedWeeklyMeal(val dateKey: String, val meal: MealSuggestion, val locked: Boolean)
data class WeeklyMealPlan(
    val profileKey: String, val meals: List<PlannedWeeklyMeal>, val locks: List<WeeklyMealLock>,
    val unfilled: List<String>, val problems: List<String>, val referenceBudgetCents: Long,
    val unknownPrices: Int
) {
    fun toAgenda(): List<DailyAgendaItem> = meals.map { entry ->
        DailyAgendaItem(
            id = "weekly-" + StableScore.sha256("$profileKey|${entry.dateKey}|${entry.meal.slot}|${entry.meal.candidateKey}").take(40),
            dateKey = entry.dateKey, time = when (entry.meal.slot) {
                MealSlot.Breakfast -> "08:00"
                MealSlot.Lunch -> "12:00"
                MealSlot.Dinner -> "18:00"
                MealSlot.Drink -> "15:00"
            }, title = entry.meal.title, category = "meal", ingredients = entry.meal.ingredients,
            candidateKey = entry.meal.candidateKey, estimatedPriceCentsPerPortion = entry.meal.estimatedPriceCents,
            // Repeated previews must not manufacture timestamps or overwrite completed saved occurrences.
            createdAt = LocalDate.parse(entry.dateKey).toEpochDay().coerceAtLeast(0) * 86_400_000
        )
    }
}

/** Greedy, reproducible menu composition, not a future fortune or a globally optimal budget solver. */
object WeeklyMealPlanner {
    val slots = listOf(MealSlot.Breakfast, MealSlot.Lunch, MealSlot.Dinner)

    fun plan(
        profileKey: String, start: LocalDate, eligiblePool: List<MealSuggestion>,
        locks: List<WeeklyMealLock> = emptyList(), budgetCents: Int? = null, variation: Int = 0
    ): WeeklyMealPlan {
        require(profileKey.isNotBlank() && (budgetCents == null || budgetCents in 0..10_000_000))
        val cells = (0L..6L).flatMap { offset -> slots.map { start.plusDays(offset).toString() to it } }
        require(locks.map { it.dateKey to it.slot }.distinct().size == locks.size)
        val pool = eligiblePool.filter { it.slot in slots && it.candidateKey.isNotBlank() }
            .distinctBy { it.slot to it.candidateKey }
        val problems = mutableListOf<String>()
        val selectedLocks = locks.mapNotNull { lock ->
            val match = pool.firstOrNull { it.candidateKey == lock.candidateKey && it.slot == lock.slot }
            if (lock.dateKey to lock.slot !in cells || match == null ||
                (budgetCents != null && match.estimatedPriceCents == null)) {
                problems += "锁定餐食不可用或已被当前约束过滤：${lock.dateKey} ${lock.candidateKey}"
                null
            } else PlannedWeeklyMeal(lock.dateKey, match, true)
        }
        val reserved = selectedLocks.sumOf { it.meal.estimatedPriceCents?.toLong() ?: 0 }
        if (budgetCents != null && reserved > budgetCents) problems += "锁定餐食的参考预算已超过整周上限。"
        if (problems.isNotEmpty()) return WeeklyMealPlan(profileKey, emptyList(), locks,
            cells.map { "${it.first} ${it.second}" }, problems, 0, 0)
        val picked = selectedLocks.toMutableList()
        var cost = reserved
        cells.forEach { (date, slot) ->
            if (selectedLocks.any { it.dateKey == date && it.meal.slot == slot }) return@forEach
            val available = pool.filter { candidate -> candidate.slot == slot && (budgetCents == null ||
                candidate.estimatedPriceCents?.let { cost + it <= budgetCents } == true) }
            val chosen = available.sortedWith(compareByDescending<MealSuggestion> { candidate ->
                val used = picked.count { it.meal.candidateKey == candidate.candidateKey }
                val ingredients = picked.flatMap { it.meal.ingredients }.toSet()
                candidate.score * 10 - used * 1000 + candidate.ingredients.count { it in ingredients } * 6
            }.thenBy { StableScore.tieBreak(profileKey, "$date|$variation", it.candidateKey) }.thenBy { it.candidateKey }).firstOrNull()
            if (chosen != null) { picked += PlannedWeeklyMeal(date, chosen, false); cost += chosen.estimatedPriceCents ?: 0 }
        }
        val ordered = picked.sortedWith(compareBy<PlannedWeeklyMeal> { it.dateKey }.thenBy { slots.indexOf(it.meal.slot) })
        return WeeklyMealPlan(profileKey, ordered, locks,
            cells.filter { (date, slot) -> ordered.none { it.dateKey == date && it.meal.slot == slot } }
                .map { "${it.first} ${it.second}" }, emptyList(), cost, ordered.count { it.meal.estimatedPriceCents == null })
    }
}
