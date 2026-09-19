package com.xuanji.app.domain

import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.WesternDailyFortune
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositeFortuneSummaryTest {
    @Test
    fun period_summary_only_presents_the_combined_conclusion() {
        val date = LocalDate.of(2026, 9, 19)
        val eastern = EasternDailyFortune(
            dateKey = date.toString(),
            overallScore = 72,
            careerScore = 72,
            wealthScore = 72,
            loveScore = 72,
            healthScore = 72,
            summary = "东方单独结论不应出现在综合总评",
            advice = "保持节奏",
            dayPillarText = "甲子",
            favorableToday = emptyList(),
            luckyColor = "青",
            luckyDirection = "东"
        )
        val western = WesternDailyFortune(
            dateKey = date.toString(),
            sign = "白羊座",
            overallScore = 68,
            careerScore = 68,
            wealthScore = 68,
            loveScore = 68,
            healthScore = 68,
            summary = "西方单独结论不应出现在综合总评",
            luckyNumber = 3,
            luckyColor = "红",
            luckyDirection = "南"
        )

        val summary = CompositeFortuneGenerator.generate(eastern, western, date).periodSummary

        assertTrue(summary.contains("综合"))
        assertFalse(summary.contains("八字以"))
        assertFalse(summary.contains("星盘以"))
        assertFalse(summary.contains(eastern.summary))
        assertFalse(summary.contains(western.summary))
    }
}
