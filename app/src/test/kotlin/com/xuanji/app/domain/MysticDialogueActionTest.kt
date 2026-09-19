package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.action.ActionEvidence
import com.xuanji.app.domain.action.ActionSource
import com.xuanji.app.domain.action.ActivitySuggestion
import com.xuanji.app.domain.action.ConfidenceLevel
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.LifeProfile
import com.xuanji.app.domain.action.MealSlot
import com.xuanji.app.domain.action.MealSuggestion
import com.xuanji.app.domain.action.RegionCandidate
import com.xuanji.app.domain.action.CareerCluster
import com.xuanji.app.domain.action.ColorCluster
import com.xuanji.app.domain.action.EnvironmentProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticDialogueActionTest {
    private val engine = DefaultMysticDialogueEngine()
    private val plan = DailyActionPlan(
        dateKey = "2026-09-19", profileKey = "p", cityKey = "shanghai",
        meals = listOf(MealSuggestion(MealSlot.Breakfast, "燕麦蓝莓酸奶碗", listOf("燕麦", "蓝莓"), "豆乳燕麦碗", listOf("燕麦", "蓝莓"), 81, evidence())),
        activities = listOf(ActivitySuggestion("在绿地慢走 30 分钟", 25..40, "傍晚", "避免冲刺", 77, evidence())),
        outings = emptyList(), evidence = evidence(), confidence = ConfidenceLevel.High, disclaimer = "边界"
    )
    private val profile = LifeProfile(
        "p",
        listOf(CareerCluster("creative", "创意表达与内容", 82, listOf("五行"))),
        listOf(ColorCluster("jade", "玉青", "#7BB8A4", listOf("星座"))),
        EnvironmentProfile(listOf("green"), "适合变化", listOf("五行")),
        listOf(RegionCandidate("葡萄牙", "里斯本", listOf("waterfront"), listOf("签证"), 73)),
        evidence(), ConfidenceLevel.High, emptyList(), "边界"
    )

    @Test
    fun classifier_distinguishes_action_topics_before_generic_daily_words() {
        assertEquals(MysticIntent.TodayMeal, MysticIntentClassifier.classify("今天早餐吃什么"))
        assertEquals(MysticIntent.TodayActivity, MysticIntentClassifier.classify("今天做什么"))
        assertEquals(MysticIntent.TodayOuting, MysticIntentClassifier.classify("今天去哪玩"))
        assertEquals(MysticIntent.LifeProfile, MysticIntentClassifier.classify("什么工作适合我"))
    }

    @Test
    fun meal_question_uses_the_same_title_score_and_evidence_as_the_card() {
        val reply = engine.reply(context(), "今天早餐吃什么")
        assertTrue(reply.text.contains("燕麦蓝莓酸奶碗"))
        assertTrue(reply.text.contains("81"))
        assertTrue(reply.groundedFacts.contains("五行依据"))
        assertFalse(reply.text.contains("你问：「"))
    }

    @Test
    fun profile_question_uses_same_profile_and_keeps_boundary_language() {
        val reply = engine.reply(context(), "哪个城市适合我")
        assertTrue(reply.text.contains("创意表达与内容"))
        assertTrue(reply.text.contains("匹配示例"))
        assertFalse(reply.text.contains("最适合你移民"))
    }

    private fun evidence() = listOf(ActionEvidence(ActionSource.FiveElements, "wood", "五行依据", 32))

    private fun context() = DialogueContext(
        profileKey = "p", dateKey = "2026-09-19", mode = "scholar", styleKey = "archive", topicKey = "composite",
        fortune = CompositeDailyFortune(
            "2026-09-19", 72, listOf(FortuneDimension("career", "事业", 70, "平稳")), 6, "青", "东南", "慢一点",
            EasternDailyFortune("2026-09-19", 70, 70, 65, 66, 68, "平稳", "慢一点", "甲子", listOf(Element.WATER), "青", "东南"),
            WesternDailyFortune("2026-09-19", "双鱼座", 74, 73, 72, 71, 75, "平稳", 6, "青", "东南")
        ),
        dailyActionPlan = plan,
        lifeProfile = profile
    )
}
