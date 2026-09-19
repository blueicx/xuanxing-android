package com.xuanji.app.ui.components

import com.xuanji.app.domain.action.ActionEvidence
import com.xuanji.app.domain.action.ActionSource
import com.xuanji.app.domain.action.ConfidenceLevel
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.MealSlot
import com.xuanji.app.domain.action.MealSuggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActionCardModelTest {
    @Test
    fun action_card_model_exposes_content_score_confidence_and_why() {
        val plan = DailyActionPlan(
            dateKey = "2026-09-19", profileKey = "p", cityKey = "shanghai",
            meals = listOf(MealSuggestion(MealSlot.Breakfast, "燕麦蓝莓酸奶碗", listOf("燕麦"), "豆乳燕麦碗", listOf("燕麦"), 81, listOf(ActionEvidence(ActionSource.FiveElements, "wood", "五行匹配", 32)))),
            activities = emptyList(), outings = emptyList(), evidence = emptyList(),
            confidence = ConfidenceLevel.High, disclaimer = "边界"
        )
        val cards = DailyActionCardModel.from(plan)
        assertEquals(listOf("吃什么", "做什么", "去哪玩"), cards.map { it.title })
        assertTrue(cards.all { it.why.isNotEmpty() || it.title != "吃什么" })
        assertTrue(cards.all { it.boundary.isNotBlank() })
    }
}
