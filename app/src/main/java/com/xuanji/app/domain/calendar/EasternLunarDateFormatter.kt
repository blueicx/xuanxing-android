package com.xuanji.app.domain.calendar

import java.time.LocalDate

/** Formats an ISO solar date with the project's table-backed Chinese lunisolar calendar. */
fun formatEasternLunarDate(dateKey: String): String {
    val solarDate = runCatching { LocalDate.parse(dateKey) }.getOrNull()
        ?: return "日期暂不可用"

    return when (val result = TableLunisolarCalendarProvider.solarToLunar(solarDate)) {
        is LunarConversionResult.Success -> {
            val lunar = result.date
            val monthName = lunarMonthNames[lunar.month - 1]
            val dayName = lunarDayNames[lunar.day - 1]
            "农历${if (lunar.isLeapMonth) "闰" else ""}${monthName}${dayName}"
        }
        is LunarConversionResult.Unsupported -> "公历$dateKey（农历日期超出历表）"
    }
}

private val lunarMonthNames = listOf(
    "正月", "二月", "三月", "四月", "五月", "六月",
    "七月", "八月", "九月", "十月", "冬月", "腊月"
)

private val lunarDayNames = listOf(
    "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
)
