package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticDialogueAnalyzerTest {
    @Test
    fun multi_topic_input_keeps_one_primary_topic_and_one_clarifier() {
        val result = DefaultMysticDialogueAnalyzer().analyze(
            "工作和感情都想问，先看哪个？",
            testDialogueContext()
        )

        assertEquals(MysticIntent.Career, result.intent)
        assertEquals("career", result.topicKey)
        assertEquals("love", result.entities["secondary_topic"])
        assertTrue(result.needsClarification)
    }

    @Test
    fun explicit_follow_up_inherits_previous_topic_and_entity() {
        val result = DefaultMysticDialogueAnalyzer().analyze(
            "那这个呢？",
            testDialogueContext(
                recentTurns = listOf(
                    MysticTurn("最近工作压力很大", "先把工作拆开", "career")
                )
            )
        )

        assertEquals(MysticIntent.Career, result.intent)
        assertEquals("career", result.topicKey)
        assertEquals("工作", result.entities["topic_label"])
        assertTrue(result.confidence >= 60)
    }

    private fun testDialogueContext(recentTurns: List<MysticTurn> = emptyList()) = DialogueContext(
        mode = "scholar",
        styleKey = "archive",
        topicKey = "composite",
        fortune = CompositeDailyFortune(
            dateKey = "2026-09-12",
            overallScore = 72,
            dimensions = listOf(FortuneDimension("career", "事业", 70, "稳步推进")),
            luckyNumber = 6,
            luckyColor = "青",
            luckyDirection = "东南",
            cautions = "别硬顶",
            eastern = EasternDailyFortune(
                "2026-09-12", 68, 70, 65, 66, 69, "东方盘平稳", "稳步推进", "甲子",
                emptyList<Element>(), "青", "东南"
            ),
            western = WesternDailyFortune(
                "2026-09-12", "处女座", 74, 73, 72, 71, 75, "西方盘平稳", 6, "青", "东南",
                dimensionBasis = emptyMap()
            ),
            period = "day",
            periodSummary = "平稳推进",
            insights = emptyList()
        ),
        recentTurns = recentTurns
    )
}
