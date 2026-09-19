package com.xuanji.app.domain.action

import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.Branch
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.Pillar
import com.xuanji.app.data.model.Stem
import com.xuanji.app.data.model.WesternDailyFortune
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActionPlannerTest {
    @Test
    fun score_uses_fixed_weights_and_never_randomizes_the_main_result() {
        val first = StableScore.combine(100, 80, 60, 40)
        val second = StableScore.combine(100, 80, 60, 40)
        assertEquals(78, first)
        assertEquals(first, second)
        val ordered = StableScore.orderTies(
            listOf(Candidate("a", 70), Candidate("b", 70)),
            "profile",
            "2026-09-19"
        )
        assertEquals(2, ordered.size)
        assertTrue(ordered.first().key == "a" || ordered.first().key == "b")
        assertEquals(ordered.map { it.key }, StableScore.orderTies(
            listOf(Candidate("a", 70), Candidate("b", 70)),
            "profile",
            "2026-09-19"
        ).map { it.key })
    }

    @Test
    fun catalog_contains_concrete_food_and_city_examples() {
        assertTrue(ActionCatalog.meals.size >= 8)
        assertTrue(ActionCatalog.meals.all { it.ingredients.isNotEmpty() && it.substitute.isNotBlank() })
        assertTrue(ActionCatalog.meals.all { it.deliveryKeywords.isNotEmpty() })
        assertTrue(CityProfileCatalog.profiles.size >= 8)
    }

    @Test
    fun plan_is_specific_and_exposes_both_required_sources() {
        val plan = DailyActionPlanner().plan(input(preference = FoodPreference(vegetarian = true)))
        assertTrue(plan.meals.all { it.ingredients.isNotEmpty() && it.substitute.isNotBlank() })
        assertTrue(plan.meals.all { it.deliveryKeywords.isNotEmpty() })
        assertTrue(plan.meals.flatMap { it.evidence }.any { it.source == ActionSource.FiveElements })
        assertTrue(plan.meals.flatMap { it.evidence }.any { it.source == ActionSource.Zodiac })
    }

    @Test
    fun excluded_ingredients_filter_every_meal_and_report_degradation() {
        val plan = DailyActionPlanner().plan(input(preference = FoodPreference(excludedIngredients = setOf("花生", "酒"))))
        assertTrue(plan.meals.none { it.ingredients.any { ingredient -> ingredient.contains("花生") } })
        assertFalse(plan.disclaimer.contains("无法计算"))
    }

    @Test
    fun budget_and_preparation_constraints_are_applied_before_ranking() {
        val plan = DailyActionPlanner().plan(
            input(
                preference = FoodPreference(maxMealBudgetCents = 3000, maxPrepMinutes = 10),
                constraints = ActionConstraints(maxMealBudgetCents = 3000, maxPrepMinutes = 10, energy = EnergyLevel.Low)
            )
        )
        assertTrue(plan.meals.all { (it.estimatedPriceCents ?: 0) <= 3000 })
        assertTrue(plan.meals.all { (it.prepMinutes ?: 0) <= 10 })
        assertTrue(plan.activities.all { it.durationMinutes.first <= 30 })
    }

    private fun input(
        preference: FoodPreference = FoodPreference(),
        city: CityProfile? = CityProfileCatalog.defaultCity,
        date: LocalDate = LocalDate.of(2026, 9, 19),
        constraints: ActionConstraints = ActionConstraints()
    ) = DailyActionInput(
        profileKey = "profile-a",
        date = date,
        chart = BaziChart(
            yearPillar = Pillar(Stem.甲, Branch.子),
            monthPillar = Pillar(Stem.丙, Branch.寅),
            dayPillar = Pillar(Stem.戊, Branch.辰),
            hourPillar = Pillar(Stem.壬, Branch.申),
            dayMaster = Stem.戊,
            zodiac = "鼠",
            elementCounts = mapOf(Element.WOOD to 2, Element.FIRE to 1, Element.EARTH to 3, Element.METAL to 1, Element.WATER to 1),
            favorableElements = listOf(Element.WATER, Element.WOOD),
            unfavorableElements = listOf(Element.FIRE)
        ),
        zodiacKey = "双鱼座",
        zodiacElement = Element.WATER,
        fortune = CompositeDailyFortune(
            dateKey = date.toString(), overallScore = 72,
            dimensions = listOf(
                FortuneDimension("career", "事业", 70, "稳定"),
                FortuneDimension("emotion", "情感", 76, "有回应"),
                FortuneDimension("health", "健康", 68, "注意节奏"),
                FortuneDimension("study", "学习", 74, "适合整理")
            ), luckyNumber = 6, luckyColor = "青", luckyDirection = "东南", cautions = "慢一点",
            eastern = EasternDailyFortune(date.toString(), 70, 70, 65, 66, 68, "平稳", "慢一点", "甲子", listOf(Element.WATER), "青", "东南"),
            western = WesternDailyFortune(date.toString(), "双鱼座", 74, 73, 72, 71, 75, "平稳", 6, "青", "东南")
        ),
        city = city,
        preference = preference,
        constraints = constraints
    )
}
