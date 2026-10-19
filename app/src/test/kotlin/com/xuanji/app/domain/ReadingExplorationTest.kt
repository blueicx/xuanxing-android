package com.xuanji.app.domain

import com.xuanji.app.data.model.UserProfile
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ReadingExplorationTest {
    @Test fun adjacentPeriodsUseCalendarArithmeticNotFixedDays() {
        assertEquals(LocalDate.of(2024, 2, 29), ReadingExploration.adjacent(LocalDate.of(2024, 1, 31), "month", 1))
        assertEquals(LocalDate.of(2023, 2, 28), ReadingExploration.adjacent(LocalDate.of(2024, 2, 29), "year", -1))
        assertEquals("week", ReadingExploration.periodCode("本周"))
        assertNull(ReadingExploration.periodCode("一生"))
    }
    @Test fun sensitivityActuallyRecalculatesCrossMidnightWithoutChangingOriginal() {
        val profile = UserProfile(1990, 1, 1, 0, 30, "合成资料")
        val result = ReadingExploration.baziSensitivity(profile)
        assertEquals("1989-12-31T23:30", result.first().time.toString())
        assertEquals(BaziCalculator.calculate(profile).display, result[1].chart)
        assertEquals("1990-01-01T01:30", result.last().time.toString())
        assertEquals(1, profile.birthDay)
        assertEquals(0, profile.birthHour)
    }
}
