package com.xuanji.app.domain.calendar

/** Chinese lunisolar date. Month 1..12; isLeapMonth marks the inserted month. */
data class LunisolarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean = false
) {
    init {
        require(month in 1..12) { "农历月份必须为 1..12" }
        require(day in 1..30) { "农历日期必须为 1..30" }
    }
}

sealed class LunarConversionResult {
    data class Success(val date: LunisolarDate) : LunarConversionResult()
    data class Unsupported(val message: String) : LunarConversionResult()
}
