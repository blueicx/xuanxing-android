package com.xuanji.app.domain

import com.xuanji.app.domain.action.*
import org.junit.Assert.*
import org.junit.Test

class TemporaryActionConditionsTest {
    @Test fun evidence_follows_selected_candidate_and_marks_temporary_filters_separately() {
        fun meal(id: String) = MealSuggestion(MealSlot.Lunch, id, listOf("米饭"), "", emptyList(), 70,
            listOf(ActionEvidence(ActionSource.FiveElements, id, "$id 五行匹配", 32)), candidateKey = id)
        val plan = DailyActionPlan("2026-10-15", "test", null, listOf(meal("first"), meal("second")), emptyList(), emptyList(), emptyList(), ConfidenceLevel.High, "离线参考")
        val lines = MysticReferenceResolver.evidenceLines(plan, "meal", "second", TemporaryActionConditions(maxMinutes = 15), listOf("盘面依据"))
        assertEquals("当前候选：second", lines.first())
        assertTrue(lines.any { it.contains("second 五行匹配") })
        assertTrue(lines.any { it.contains("15 分钟") && it.contains("不计匹配分") })
        assertTrue(lines.contains("盘面依据"))
        assertFalse(lines.any { it.contains("first 五行匹配") })
    }
    @Test fun parses_explicit_minutes_ingredients_budget_and_scenario() {
        assertEquals(15, TemporaryActionConditions.parse("我只有十五分钟").maxMinutes)
        assertEquals(setOf("豆腐"), TemporaryActionConditions.parse("今天不要豆腐").excludedIngredients)
        assertTrue(TemporaryActionConditions.parse("换个室内的").indoorOnly == true)
        assertEquals(2000, TemporaryActionConditions.parse("预算20元以内").maxBudgetCents)
        assertEquals(ActionScenario.Work, TemporaryActionConditions.parse("今天上班").scenario)
    }
    @Test fun explicit_constraints_merge_without_changing_saved_preferences() {
        val saved = FoodPreference(vegetarian = true, excludedIngredients = setOf("花生"))
        val conditions = TemporaryActionConditions.parse("不要豆腐，预算20元以内")
        val merged = conditions.apply(saved)
        assertTrue(merged.vegetarian)
        assertEquals(setOf("花生", "豆腐"), merged.excludedIngredients)
        assertEquals(setOf("花生"), saved.excludedIngredients)
        assertNull(saved.maxMealBudgetCents)
    }

    @Test fun short_on_time_scenario_limits_both_meal_preparation_and_activity_without_relaxing_preferences() {
        val conditions = TemporaryActionConditions(scenario = ActionScenario.ShortOnTime)
        assertEquals(15, conditions.apply(FoodPreference()).maxPrepMinutes)
        assertEquals(10, conditions.apply(FoodPreference(maxPrepMinutes = 10)).maxPrepMinutes)
        assertEquals(15, conditions.apply(ActionConstraints()).maxActivityMinutes)
    }
    @Test fun invalid_amounts_are_not_silently_interpreted_as_conditions() {
        assertNull(TemporaryActionConditions.parse("十五分钟之前的那句话").maxMinutes)
        assertNull(TemporaryActionConditions.parse("预算-10元").maxBudgetCents)
        assertEquals(2, MysticReferenceResolver.requestedIndex("第二个呢"))
        assertNull(MysticReferenceResolver.requestedIndex("为什么"))
    }

    @Test fun referenced_candidate_is_the_same_in_the_reply_and_action_card_without_mutating_ranking() {
        fun meal(id: String) = MealSuggestion(MealSlot.Lunch, id, listOf("米饭"), "", emptyList(), 70, emptyList(), candidateKey = id)
        val plan = DailyActionPlan("2026-10-13", "test", null, listOf(meal("first"), meal("second")), emptyList(), emptyList(), emptyList(), ConfidenceLevel.High, "离线参考")
        val selected = MysticReferenceResolver.selectPlan(plan, "meal", "second")!!
        assertEquals("second", selected.meals.first().candidateKey)
        assertEquals("second", MysticReferenceResolver.fromPlan(selected, "meal").first().id)
        assertEquals(listOf("first", "second"), plan.meals.map { it.candidateKey })
        assertNull(MysticReferenceResolver.selectPlan(plan, "meal", "missing"))
        assertNull(MysticReferenceResolver.selectPlan(plan, "unknown", "second"))
    }
}
