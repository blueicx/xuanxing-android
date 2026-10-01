package com.xuanji.app.ui.components

import com.xuanji.app.domain.action.ActionEvidence
import com.xuanji.app.domain.action.ActionSource
import com.xuanji.app.domain.action.ActivitySuggestion
import com.xuanji.app.domain.action.ConfidenceLevel
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.MealSlot
import com.xuanji.app.domain.action.MealSuggestion
import com.xuanji.app.domain.action.OutingSuggestion
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

    @Test
    fun detail_fields_keep_the_specific_source_data_for_all_three_action_types() {
        val evidence = listOf(ActionEvidence(ActionSource.DailyFortune, "day", "今日盘面提示", 9))
        val plan = DailyActionPlan(
            dateKey = "2026-09-30",
            profileKey = "p",
            cityKey = null,
            meals = listOf(
                MealSuggestion(
                    MealSlot.Lunch,
                    "菌菇鸡肉饭",
                    listOf("鸡肉", "香菇"),
                    "豆腐可替代鸡肉",
                    listOf("鸡肉饭"),
                    82,
                    evidence,
                    estimatedPriceCents = 2400,
                    prepMinutes = 20
                )
            ),
            activities = listOf(
                ActivitySuggestion(
                    title = "公园慢走",
                    durationMinutes = 15..25,
                    bestPeriod = "午后",
                    avoid = "高温时段",
                    score = 76,
                    evidence = evidence
                )
            ),
            outings = listOf(
                OutingSuggestion(
                    placeType = "湖边步道",
                    reason = "环境开阔，适合短时散步",
                    cityLabel = "杭州 · 西湖",
                    score = 71,
                    evidence = evidence,
                    indoor = false
                )
            ),
            evidence = evidence,
            confidence = ConfidenceLevel.Medium,
            disclaimer = "仅供日常灵感"
        )

        val cards = DailyActionCardModel.from(plan)
        val mealFields = cards[0].detailFields.associate { it.label to it.value }
        val activityFields = cards[1].detailFields.associate { it.label to it.value }
        val outingFields = cards[2].detailFields.associate { it.label to it.value }

        assertEquals("鸡肉、香菇", mealFields["主要食材"])
        assertEquals("豆腐可替代鸡肉", mealFields["替代食材"])
        assertEquals("约 ¥24.0", mealFields["预算参考"])
        assertEquals("20 分钟", mealFields["准备时间"])
        assertEquals("15–25 分钟", activityFields["建议时长"])
        assertEquals("午后", activityFields["适合时段"])
        assertEquals("杭州 · 西湖", outingFields["地区示例"])
        assertEquals("环境开阔，适合短时散步", outingFields["建议理由"])
    }
}
