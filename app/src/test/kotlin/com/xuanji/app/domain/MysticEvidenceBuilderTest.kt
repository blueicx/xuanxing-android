package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticEvidenceBuilderTest {
    @Test
    fun trace_is_deterministic_and_marks_missing_realtime_weather() {
        val context = DialogueContext(
            profileKey = "bazi|demo",
            dateKey = "2026-09-22",
            mode = "scholar",
            styleKey = "archive",
            topicKey = "weather",
            fortune = fortune,
            question = "今天下雨吗",
            characterId = MysticCharacterId.NadirRashid
        )
        val analysis = DialogueAnalysis(MysticIntent.Weather, "weather", confidence = 92, needsClarification = false)
        val specialty = MysticCharacterVoiceAdapter.selectSpecialty(MysticCharacterId.NadirRashid, analysis.intent, "weather")
        val first = MysticEvidenceBuilder.forDialogue(context, analysis, specialty)
        val second = MysticEvidenceBuilder.forDialogue(context, analysis, specialty)

        assertEquals(first, second)
        assertTrue(first.items.any { it.status == MysticEvidenceStatus.NotEnabled && it.key == "weather" })
        assertTrue(first.missingInputs.contains("手动授权的天气数据"))
        assertEquals(100, first.weights.values.sum())
    }

    @Test
    fun fortune_trace_separates_computed_systems() {
        val trace = MysticEvidenceBuilder.forFortune(fortune, "profile")
        assertEquals(listOf("eastern", "western", "composite"), trace.systems)
        assertTrue(trace.items.all { it.status == MysticEvidenceStatus.Computed })
        assertEquals(100, trace.confidence)
    }

    private companion object {
        val fortune = CompositeDailyFortune(
            dateKey = "2026-09-22", overallScore = 72,
            dimensions = listOf(FortuneDimension("career", "事业", 70, "稳步推进")),
            luckyNumber = 6, luckyColor = "青", luckyDirection = "东南", cautions = "别硬顶",
            eastern = EasternDailyFortune(
                dateKey = "2026-09-22", overallScore = 68, careerScore = 70, wealthScore = 65,
                loveScore = 66, healthScore = 69, summary = "东方盘平稳", advice = "稳步推进",
                dayPillarText = "甲子", favorableToday = emptyList<Element>(), luckyColor = "青", luckyDirection = "东南"
            ),
            western = WesternDailyFortune(
                dateKey = "2026-09-22", sign = "处女座", overallScore = 74, careerScore = 73,
                wealthScore = 72, loveScore = 71, healthScore = 75, summary = "西方盘平稳",
                luckyNumber = 6, luckyColor = "青", luckyDirection = "东南", dimensionBasis = emptyMap()
            )
        )
    }
}
