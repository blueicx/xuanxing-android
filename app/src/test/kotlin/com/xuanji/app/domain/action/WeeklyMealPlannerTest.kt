package com.xuanji.app.domain.action

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeeklyMealPlannerTest {
    private val start = LocalDate.parse("2026-10-19")
    private fun meal(slot: MealSlot, id: String, price: Int = 1000) = MealSuggestion(slot, id, listOf("番茄", id), "", emptyList(), 80, emptyList(), price, 15, id)
    private val pool = listOf(MealSlot.Breakfast, MealSlot.Lunch, MealSlot.Dinner).flatMap { slot -> (1..7).map { meal(slot, "${slot.name}-$it") } }

    @Test fun week_is_deterministic_diverse_and_retains_real_ingredients_and_prices() {
        val first = WeeklyMealPlanner.plan("p", start, pool)
        assertEquals(first, WeeklyMealPlanner.plan("p", start, pool.reversed()))
        assertEquals(21, first.meals.size)
        assertEquals(21000L, first.referenceBudgetCents)
        assertTrue(first.meals.all { it.meal in pool && it.meal.ingredients.first() == "番茄" })
        assertEquals(21, first.meals.map { it.meal.candidateKey }.distinct().size)
        assertTrue(first.unfilled.isEmpty())
    }

    @Test fun locks_are_preserved_only_when_eligible_and_budget_is_never_silently_exceeded() {
        val locked = WeeklyMealLock(start.toString(), MealSlot.Lunch, "Lunch-7")
        val plan = WeeklyMealPlanner.plan("p", start, pool, listOf(locked), budgetCents = 22000)
        assertEquals("Lunch-7", plan.meals.single { it.dateKey == start.toString() && it.meal.slot == MealSlot.Lunch }.meal.candidateKey)
        assertEquals(listOf(locked), plan.locks)
        val missing = WeeklyMealPlanner.plan("p", start, pool.filterNot { it.candidateKey == "Lunch-7" }, listOf(locked))
        assertTrue(missing.problems.any { it.contains("锁定") })
        val small = WeeklyMealPlanner.plan("p", start, pool, budgetCents = 1500)
        assertTrue(small.referenceBudgetCents <= 1500)
        assertTrue(small.unfilled.isNotEmpty())
    }

    @Test fun empty_pool_and_unknown_costs_are_honest_and_drinks_are_not_mislabeled_as_meals() {
        assertEquals(21, WeeklyMealPlanner.plan("p", start, emptyList()).unfilled.size)
        val plan = WeeklyMealPlanner.plan("p", start, pool.map { it.copy(estimatedPriceCents = null) } + meal(MealSlot.Drink, "drink"))
        assertEquals(21, plan.unknownPrices)
        assertTrue(plan.meals.none { it.meal.slot == MealSlot.Drink })
        assertTrue(WeeklyMealPlanner.plan("p", start, pool.map { it.copy(estimatedPriceCents = null) }, budgetCents = 20000).meals.isEmpty())
    }

    @Test fun converted_agenda_is_idempotent_and_retains_price_sources() {
        val plan = WeeklyMealPlanner.plan("p", start, pool)
        val items = plan.toAgenda()
        assertEquals(items, plan.toAgenda())
        assertTrue(items.all { it.estimatedPriceCentsPerPortion == 1000 && it.ingredients.size == 2 && it.category == "meal" })
        assertEquals(listOf("08:00", "12:00", "18:00"), items.take(3).map { it.time })
    }
}
