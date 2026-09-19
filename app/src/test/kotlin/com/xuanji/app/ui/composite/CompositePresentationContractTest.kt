package com.xuanji.app.ui.composite

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
}
