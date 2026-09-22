package com.xuanji.app.domain

import com.xuanji.app.data.model.Element
import com.xuanji.app.domain.action.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticCompanionActionRouterTest {
    private val evidence = listOf(ActionEvidence(ActionSource.FiveElements, "wood", "喜用五行：木", 40))
    private val plan = DailyActionPlan(
        dateKey = "2026-09-22", profileKey = "p", cityKey = null,
        meals = listOf(MealSuggestion(MealSlot.Lunch, "菌菇鸡肉饭", listOf("鸡肉", "菌菇"), "豆腐", listOf("菌菇饭"), 88, evidence, 2800, 20)),
        activities = listOf(ActivitySuggestion("整理书桌", 15..20, "午后", null, 80, evidence)),
        outings = listOf(OutingSuggestion("滨水公园", "适合舒展", "本地", 79, evidence)),
        evidence = evidence, confidence = ConfidenceLevel.High, disclaimer = "生活灵感"
    )
    private val profile = LifeProfile(
        profileKey = "p", careerClusters = listOf(CareerCluster("design", "设计策划", 82, listOf("木"))),
        colorPalette = listOf(ColorCluster("green", "青绿色", "#557A64", listOf("木"))),
        environmentProfile = EnvironmentProfile(listOf("滨水"), "舒缓", listOf("木水")),
        regionCandidates = emptyList(), evidence = evidence, confidence = ConfidenceLevel.Medium,
        missingInputs = emptyList(), disclaimer = "匹配示例"
    )

    @Test
    fun routes_grounded_actions_and_evidence() {
        assertTrue(MysticCompanionActionRouter.route("今天吃什么", plan, profile, false) is MysticCompanionAction.ShowTodayMeal)
        assertTrue(MysticCompanionActionRouter.route("今天适合做什么", plan, profile, false) is MysticCompanionAction.ShowTodayActivity)
        assertTrue(MysticCompanionActionRouter.route("今天去哪玩", plan, profile, false) is MysticCompanionAction.ShowTodayOuting)
        assertTrue(MysticCompanionActionRouter.route("适合什么工作", plan, profile, false) is MysticCompanionAction.ShowLifeProfile)
        assertEquals(MysticCompanionAction.ShowEvidence, MysticCompanionActionRouter.route("解释刚才", plan, profile, false))
    }

    @Test
    fun routes_character_games_and_only_resumes_a_real_archive() {
        assertEquals(MysticCompanionAction.OpenGame("poetry_chain"), MysticCompanionActionRouter.route("开始诗句接龙", plan, profile, false))
        assertNull(MysticCompanionActionRouter.route("继续棋局", plan, profile, false))
        assertEquals(MysticCompanionAction.ResumeXiangqi, MysticCompanionActionRouter.route("继续棋局", plan, profile, true))
    }

    @Test
    fun missing_grounded_data_falls_back_to_dialogue() {
        assertNull(MysticCompanionActionRouter.route("今天吃什么", null, null, false))
        assertNull(MysticCompanionActionRouter.route("适合什么工作", null, null, false))
    }
}
