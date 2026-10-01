package com.xuanji.app.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class EasternLunarDateFormatterTest {
    @Test
    fun formats_lunar_new_year_using_the_table_calendar() {
        assertEquals("农历正月初一", formatEasternLunarDate("2024-02-10"))
    }

    @Test
    fun includes_leap_month_marker() {
        assertEquals("农历闰二月初一", formatEasternLunarDate("2023-03-22"))
    }

    @Test
    fun reports_out_of_range_dates_without_inventing_lunar_values() {
        assertEquals("公历1899-12-31（农历日期超出历表）", formatEasternLunarDate("1899-12-31"))
    }
}
