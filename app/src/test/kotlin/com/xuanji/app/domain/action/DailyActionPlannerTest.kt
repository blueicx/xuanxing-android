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
    @Test fun practical_filters_apply_to_daily_pool_and_substitutes_without_changing_cultural_scores() {
        val planner = DailyActionPlanner()
        val baseline = planner.mealPool(input()).associateBy { it.candidateKey }
        val restricted = planner.mealPool(input(preference = FoodPreference(
            availableEquipment = emptySet(), maxCookingDifficulty = CookingDifficulty.Easy, lunchboxOnly = true
        )))
        assertTrue(restricted.isNotEmpty())
        restricted.forEach { meal ->
            assertTrue(meal.requiredEquipment.isEmpty())
            assertEquals(CookingDifficulty.Easy, meal.difficulty)
            assertTrue(meal.lunchboxSuitable)
            assertEquals(baseline.getValue(meal.candidateKey).score, meal.score)
            meal.substituteCandidateKey?.let { key -> assertTrue(restricted.any { it.candidateKey == key }) }
        }
    }

    @Test fun pantry_priority_is_explicit_excludes_expired_or_zero_stock_and_preserves_score() {
        val planner = DailyActionPlanner()
        val base = input()
        val original = planner.mealPool(base).associateBy { it.candidateKey }
        val stock = listOf(
            com.xuanji.app.domain.PantryItem("番茄", quantity = "1", expiresOn = base.date.toString()),
            com.xuanji.app.domain.PantryItem("鸡蛋", quantity = "0"),
            com.xuanji.app.domain.PantryItem("面条", expiresOn = base.date.minusDays(1).toString())
        )
        assertEquals(planner.mealPool(base), planner.mealPool(base.copy(pantry = stock)))
        val ranked = planner.mealPool(base.copy(preference = FoodPreference(prioritizePantry = true), pantry = stock))
        assertTrue(ranked.first().ingredients.contains("番茄"))
        ranked.forEach { meal ->
            assertEquals(original.getValue(meal.candidateKey).score, meal.score)
            assertTrue(meal.evidence.filter { it.key == "pantry-priority" }.all { it.contribution == 0 && !it.label.contains("鸡蛋") && !it.label.contains("面条") })
        }
    }

    @Test fun weekly_pool_uses_identical_filters_scores_and_substitutes_as_daily() {
        val planner = DailyActionPlanner()
        val input = input(preference = FoodPreference(vegetarian = true, excludedIngredients = setOf("豆腐"), allergens = setOf("鸡蛋"), maxPrepMinutes = 20))
        val pool = planner.mealPool(input)
        assertTrue(pool.isNotEmpty())
        assertTrue(pool.none { it.ingredients.any { ingredient -> ingredient.contains("豆腐") || ingredient.contains("鸡蛋") } })
        assertTrue(pool.all { it.prepMinutes!! <= 20 })
        pool.forEach { assertEquals(it.score, it.evidence.sumOf(ActionEvidence::contribution)) }
        planner.plan(input).meals.forEach { daily -> assertEquals(daily, pool.first { it.slot == daily.slot }) }
        val week = WeeklyMealPlanner.plan(input.profileKey, input.date, pool)
        assertTrue(week.meals.all { it.meal in pool })
    }

    @Test
    fun expanded_catalog_has_unique_complete_records_in_each_slot() {
        assertEquals(64, ActionCatalog.meals.size)
        MealSlot.entries.forEach { slot -> assertEquals(16, ActionCatalog.meals.count { it.slot == slot }) }
        assertEquals(64, ActionCatalog.meals.map { it.key }.toSet().size)
        assertTrue(ActionCatalog.meals.all { it.steps.isNotEmpty() && it.steps.all(String::isNotBlank) })
        assertEquals(32, ActionCatalog.activities.size)
        assertEquals(16, ActionCatalog.outings.size)
        assertEquals(24, CityProfileCatalog.profiles.size)
        assertTrue(ActionCatalog.meals.map { it.prepMinutes }.toSet().size > 4)
        assertTrue(ActionCatalog.meals.map { it.estimatedPriceCents }.toSet().size > 4)
    }

    @Test
    fun temporary_scenario_limits_short_actions_and_home_has_no_outings() {
        val home = DailyActionPlanner().plan(input(constraints = ActionConstraints(scenario = ActionScenario.Home)))
        assertTrue(home.activities.all { it.indoor })
        assertTrue(home.outings.isEmpty())
        val short = DailyActionPlanner().plan(input(constraints = ActionConstraints(scenario = ActionScenario.ShortOnTime)))
        assertTrue(short.activities.isNotEmpty())
        assertTrue(short.activities.all { it.durationMinutes.last <= 15 })
    }

    @Test
    fun indoor_only_and_duration_limit_apply_to_displayed_activity() {
        val plan = DailyActionPlanner().plan(input(constraints = ActionConstraints(indoorOnly = true, maxActivityMinutes = 30)))
        assertTrue(plan.activities.isNotEmpty())
        assertTrue(plan.activities.all { suggestion ->
            val candidate = ActionCatalog.activities.single { it.key == suggestion.candidateKey }
            candidate.key != "green-walk" && candidate.key != "water-rest" && suggestion.durationMinutes.last <= 30
        })
    }

    @Test
    fun evidence_contributions_sum_to_actual_candidate_score() {
        val plan = DailyActionPlanner().plan(input())
        plan.meals.forEach { assertEquals(it.score, it.evidence.sumOf(ActionEvidence::contribution)) }
        plan.activities.forEach { assertEquals(it.score, it.evidence.sumOf(ActionEvidence::contribution)) }
        plan.outings.forEach { assertEquals(it.score, it.evidence.sumOf(ActionEvidence::contribution)) }
        assertEquals(4, plan.evidence.count { it.contribution > 0 })
    }

    @Test
    fun substitutes_are_selected_from_eligible_catalog_not_unfiltered_names() {
        val excluded = setOf("豆腐", "豆乳", "豆浆", "花生")
        val plan = DailyActionPlanner().plan(input(preference = FoodPreference(vegetarian = true, excludedIngredients = excluded)))
        plan.meals.forEach { meal ->
            if (meal.substitute != "当前约束下无同餐次替代候选") {
                val candidate = ActionCatalog.meals.single { it.title == meal.substitute }
                assertTrue(candidate.vegetarian)
                assertTrue(candidate.ingredients.none { ingredient -> excluded.any(ingredient::contains) })
                assertEquals(meal.slot, candidate.slot)
                assertTrue(meal.title != meal.substitute)
            }
        }
    }

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

    @Test
    fun explicitly_saved_activity_preferences_filter_actions_and_outings_without_randomness() {
        val preference = ActivityPreference(EnergyLevel.Low, indoorOnly = true, maxDurationMinutes = 30)
        val plan = DailyActionPlanner().plan(input(constraints = preference.toConstraints()))

        assertTrue(plan.activities.isNotEmpty())
        assertTrue(plan.activities.all { it.durationMinutes.first <= 30 })
        assertTrue(plan.outings.isNotEmpty())
        assertTrue(plan.outings.all { it.indoor == true })
        assertEquals(preference.toConstraints(), plan.constraints)
    }

    @Test
    fun replacing_top_meal_excludes_it_for_today_and_preserves_base_scores() {
        val planner = DailyActionPlanner()
        val baseline = planner.plan(input())
        val replaced = baseline.meals.first()
        val next = planner.plan(
            input(
                feedback = listOf(
                    ActionFeedback(
                        "replace-${replaced.candidateKey}",
                        "2026-09-19",
                        "meal",
                        replaced.candidateKey,
                        ActionFeedbackKind.Replaced
                    )
                )
            )
        )

        assertFalse(next.meals.any { it.candidateKey == replaced.candidateKey })
        val baseScores = baseline.meals.associate { it.candidateKey to it.score }
        assertTrue(next.meals.filter { it.candidateKey in baseScores }
            .all { it.score == baseScores[it.candidateKey] })
        assertTrue(next.appliedFeedback.any { it.kind == ActionFeedbackKind.Replaced })
    }

    @Test
    fun feedback_cannot_reintroduce_meals_removed_by_hard_constraints() {
        val plan = DailyActionPlanner().plan(
            input(
                preference = FoodPreference(vegetarian = true),
                feedback = (1..20).map { index ->
                    ActionFeedback(
                        "accepted-$index",
                        "2026-09-19",
                        "meal",
                        "halal-lamb-rice",
                        ActionFeedbackKind.Accepted
                    )
                }
            )
        )

        assertTrue(plan.meals.none { it.candidateKey == "halal-lamb-rice" })
    }

    private fun input(
        preference: FoodPreference = FoodPreference(),
        city: CityProfile? = CityProfileCatalog.defaultCity,
        date: LocalDate = LocalDate.of(2026, 9, 19),
        constraints: ActionConstraints = ActionConstraints(),
        feedback: List<ActionFeedback> = emptyList()
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
        constraints = constraints,
        feedback = feedback
    )
}
