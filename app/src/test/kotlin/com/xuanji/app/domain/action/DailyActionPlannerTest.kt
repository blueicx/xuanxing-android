package com.xuanji.app.domain.action

import org.junit.Assert.assertEquals
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
}
