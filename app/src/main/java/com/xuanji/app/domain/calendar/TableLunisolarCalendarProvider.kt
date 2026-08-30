package com.xuanji.app.domain.calendar

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Table-driven conversion based on the public 1900..2100 lunar year-length table.
 * The table encodes leap-month position and month lengths; it does not invent dates
 * outside the published range and therefore returns Unsupported/Failure explicitly.
 */
object TableLunisolarCalendarProvider : LunisolarCalendarProvider {
    override val supportedYears: IntRange = 1900..2100
    private val baseDate = LocalDate.of(1900, 1, 31)

    // 1900..2100, sourced from the Hong Kong Observatory conversion-table convention.
    private val lunarInfo = intArrayOf(
        0x04bd8,0x04ae0,0x0a570,0x054d5,0x0d260,0x0d950,0x16554,0x056a0,0x09ad0,0x055d2,
        0x04ae0,0x0a5b6,0x0a4d0,0x0d250,0x1d255,0x0b540,0x0d6a0,0x0ada2,0x095b0,0x14977,
        0x04970,0x0a4b0,0x0b4b5,0x06a50,0x06d40,0x1ab54,0x02b60,0x09570,0x052f2,0x04970,
        0x06566,0x0d4a0,0x0ea50,0x06e95,0x05ad0,0x02b60,0x186e3,0x092e0,0x1c8d7,0x0c950,
        0x0d4a0,0x1d8a6,0x0b550,0x056a0,0x1a5b4,0x025d0,0x092d0,0x0d2b2,0x0a950,0x0b557,
        0x06ca0,0x0b550,0x15355,0x04da0,0x0a5b0,0x14573,0x052b0,0x0a9a8,0x0e950,0x06aa0,
        0x0aea6,0x0ab50,0x04b60,0x0aae4,0x0a570,0x05260,0x0f263,0x0d950,0x05b57,0x056a0,
        0x096d0,0x04dd5,0x04ad0,0x0a4d0,0x0d4d4,0x0d250,0x0d558,0x0b540,0x0b6a0,0x195a6,
        0x095b0,0x049b0,0x0a974,0x0a4b0,0x0b27a,0x06a50,0x06d40,0x0af46,0x0ab60,0x09570,
        0x04af5,0x04970,0x064b0,0x074a3,0x0ea50,0x06b58,0x055c0,0x0ab60,0x096d5,0x092e0,
        0x0c960,0x0d954,0x0d4a0,0x0da50,0x07552,0x056a0,0x0abb7,0x025d0,0x092d0,0x0cab5,
        0x0a950,0x0b4a0,0x0baa4,0x0ad50,0x055d9,0x04ba0,0x0a5b0,0x15176,0x052b0,0x0a930,
        0x07954,0x06aa0,0x0ad50,0x05b52,0x04b60,0x0a6e6,0x0a4e0,0x0d260,0x0ea65,0x0d530,
        0x05aa0,0x076a3,0x096d0,0x04afb,0x04ad0,0x0a4d0,0x1d0b6,0x0d250,0x0d520,0x0dd45,
        0x0b5a0,0x056d0,0x055b2,0x049b0,0x0a577,0x0a4b0,0x0aa50,0x1b255,0x06d20,0x0ada0,
        0x14b63,0x09370,0x049f8,0x04970,0x064b0,0x168a6,0x0ea50,0x06b20,0x1a6c4,0x0aae0,
        0x0a2e0,0x0d2e3,0x0c960,0x0d557,0x0d4a0,0x0da50,0x05d55,0x056a0,0x0a6d0,0x055d4,
        0x052d0,0x0a9b8,0x0a950,0x0b4a0,0x0b6a6,0x0ad50,0x055a0,0x0aba4,0x0a5b0,0x052b0,
        0x0b273,0x06930,0x07337,0x06aa0,0x0ad50,0x14b55,0x04b60,0x0a570,0x054e4,0x0d160,
        0x0e968,0x0d520,0x0daa0,0x16aa6,0x056d0,0x04ae0,0x0a9d4,0x0a2d0,0x0d150,0x0f252,
        0x0d520
    )

    private fun info(year: Int): Int = lunarInfo[year - 1900]
    fun leapMonth(year: Int): Int = info(year) and 0xF
    fun leapDays(year: Int): Int = if (leapMonth(year) == 0) 0 else if (info(year) and 0x10000 != 0) 30 else 29
    fun monthDays(year: Int, month: Int): Int = if (info(year) and (0x10000 shr month) != 0) 30 else 29
    fun yearDays(year: Int): Int = (1..12).sumOf { monthDays(year, it) } + leapDays(year)

    override fun solarToLunar(date: LocalDate): LunarConversionResult {
        if (date.isBefore(baseDate)) return LunarConversionResult.Unsupported("公历早于 1900-01-31")
        var offset = ChronoUnit.DAYS.between(baseDate, date).toInt()
        var year = 1900
        while (year < 2101 && offset >= yearDays(year)) { offset -= yearDays(year); year++ }
        if (year !in supportedYears) return LunarConversionResult.Unsupported("公历超出 2100 年历表")
        val leap = leapMonth(year)
        var month = 1
        var isLeap = false
        while (month <= 12) {
            val days = if (isLeap) leapDays(year) else monthDays(year, month)
            if (offset < days) break
            offset -= days
            if (leap > 0 && month == leap && !isLeap) isLeap = true else { if (isLeap) isLeap = false; month++ }
        }
        return LunarConversionResult.Success(LunisolarDate(year, month, offset + 1, isLeap))
    }

    override fun lunarToSolar(date: LunisolarDate): Result<LocalDate> = runCatching {
        require(date.year in supportedYears) { "农历年份必须在 $supportedYears" }
        val leap = leapMonth(date.year)
        require(!date.isLeapMonth || leap == date.month) { "${date.year} 年没有闰${date.month}月" }
        val max = if (date.isLeapMonth) leapDays(date.year) else monthDays(date.year, date.month)
        require(date.day in 1..max) { "该农历月只有 $max 天" }
        var offset = (1900 until date.year).sumOf { yearDays(it) }
        for (month in 1 until date.month) {
            offset += monthDays(date.year, month)
            if (leap == month) offset += leapDays(date.year)
        }
        if (date.isLeapMonth) offset += monthDays(date.year, date.month)
        baseDate.plusDays((offset + date.day - 1).toLong())
    }
}
