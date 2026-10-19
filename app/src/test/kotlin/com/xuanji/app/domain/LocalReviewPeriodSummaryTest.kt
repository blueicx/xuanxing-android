package com.xuanji.app.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalReviewPeriodSummaryTest {
    @Test
    fun iso_week_summary_includes_the_cross_year_week_and_counts_only_explicit_actions() {
        val items = listOf(
            item("favorite:f", LocalReviewSource.Favorite, "2026-12-28"),
            item("feedback:a", LocalReviewSource.AcceptedAction, "2027-01-02"),
            item("feedback:n", LocalReviewSource.NotSuitableAction, "2027-01-03"),
            item("feedback:r", LocalReviewSource.ReplacedAction, "2027-01-04")
        )

        val summary = LocalReviewTimeline.summarize(
            items, LocalReviewPeriod.Week, LocalDate.parse("2027-01-03"), ZoneId.of("UTC")
        )

        assertEquals(1, summary.favorites)
        assertEquals(1, summary.acceptedActions)
        assertEquals(1, summary.notSuitableActions)
        assertEquals(0, summary.replacedActions)
        assertEquals(3, summary.total)
        assertTrue(summary.label.contains("本周"))
    }

    @Test
    fun month_summary_respects_calendar_month_and_returns_zero_counts_for_empty_data() {
        val today = LocalDate.parse("2026-10-11")
        val items = listOf(
            item("favorite:inside", LocalReviewSource.Favorite, "2026-10-01"),
            item("feedback:inside", LocalReviewSource.AcceptedAction, "2026-10-10"),
            item("favorite:outside", LocalReviewSource.Favorite, "2026-09-30")
        )

        val summary = LocalReviewTimeline.summarize(items, LocalReviewPeriod.Month, today, ZoneId.of("UTC"))
        val empty = LocalReviewTimeline.summarize(emptyList(), LocalReviewPeriod.Month, today, ZoneId.of("UTC"))

        assertEquals(1, summary.favorites)
        assertEquals(1, summary.acceptedActions)
        assertEquals(2, summary.total)
        assertEquals(0, empty.total)
    }

    private fun item(id: String, source: LocalReviewSource, date: String) = LocalReviewItem(
        id = id,
        source = source,
        title = id,
        summary = "用户主动记录",
        createdAt = LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )
}
