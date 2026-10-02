package com.xuanji.app.ui.composite

import com.xuanji.app.ui.lifetimeChartTabs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositePresentationContractTest {
    @Test
    fun composite_headline_describes_combined_reading_without_split_scores() {
        val headline = compositeHeadlineLine("2026-09-19")

        assertTrue(headline.contains("合参"))
        assertFalse(headline.contains("东方八字"))
        assertFalse(headline.contains("西方星盘"))
    }

    @Test
    fun daily_action_is_only_visible_for_the_daily_period() {
        assertTrue(shouldShowDailyAction("day"))
        assertFalse(shouldShowDailyAction("week"))
        assertFalse(shouldShowDailyAction("month"))
        assertFalse(shouldShowDailyAction("year"))
    }

    @Test
    fun period_presentation_keeps_each_cycle_date_and_legacy_title_mapping() {
        val expected = mapOf(
            "day" to "今日",
            "week" to "本周",
            "month" to "本月",
            "year" to "本年"
        )
        expected.forEach { (period, label) ->
            val presentation = compositePeriodPresentation(period, "2026-10-02")
            assertEquals(label, presentation.label)
            assertEquals("2026-10-02 · 八字与星盘合参", presentation.subtitle)
            assertEquals(if (period == "day") "今天的总趋势" else "${label}的总趋势", presentation.title)
            assertEquals(period == "day", presentation.showDailyAction)
        }
    }

    @Test
    fun lifetime_chart_tabs_keep_overview_separate_from_both_natal_charts() {
        assertEquals(
            listOf("overview" to "人生画像", "eastern" to "东方本命", "western" to "西方本命"),
            lifetimeChartTabs().map { it.id to it.label }
        )
    }
}
