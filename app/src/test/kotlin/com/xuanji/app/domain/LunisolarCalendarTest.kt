package com.xuanji.app.domain

import com.xuanji.app.domain.calendar.LunarConversionResult
import com.xuanji.app.domain.calendar.LunisolarDate
import com.xuanji.app.domain.calendar.TableLunisolarCalendarProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LunisolarCalendarTest {
    @Test fun known_leap_month_samples_round_trip() {
        val provider = TableLunisolarCalendarProvider
        val lunar = (provider.solarToLunar(LocalDate.of(2020, 5, 23)) as LunarConversionResult.Success).date
        assertEquals(LunisolarDate(2020, 4, 1, true), lunar)
        assertEquals(LocalDate.of(2020, 5, 23), provider.lunarToSolar(lunar).getOrThrow())
        assertEquals(LocalDate.of(2023, 3, 22), provider.lunarToSolar(LunisolarDate(2023, 2, 1, true)).getOrThrow())
        assertEquals(LunisolarDate(2024, 1, 1, false), (provider.solarToLunar(LocalDate.of(2024, 2, 10)) as LunarConversionResult.Success).date)
    }

    @Test fun unsupported_dates_are_explicit() {
        assertTrue(TableLunisolarCalendarProvider.solarToLunar(LocalDate.of(1899, 12, 31)) is LunarConversionResult.Unsupported)
    }
}
