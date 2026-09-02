package com.xuanji.app.domain.calendar

import java.time.LocalDate

interface LunisolarCalendarProvider {
    val supportedYears: IntRange
    fun solarToLunar(date: LocalDate): LunarConversionResult
    fun lunarToSolar(date: LunisolarDate): Result<LocalDate>
}
