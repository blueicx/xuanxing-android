package com.xuanji.app.domain

import com.xuanji.app.data.model.UserProfile
import java.time.LocalDate
import java.time.LocalDateTime

object ReadingExploration {
    fun calculate(profile: UserProfile, date: LocalDate, period: String): com.xuanji.app.data.model.CompositeDailyFortune {
        val bazi = BaziCalculator.analyze(profile)
        val natal = ZodiacCalculator.calculateNatalChart(profile.birthYear, profile.birthMonth, profile.birthDay,
            profile.birthHour, profile.birthMinute, profile.locationName, profile.locationLat, profile.locationLng)
        val zodiac = ZodiacCalculator.detailFromChart(natal).sun
        val age = date.year - profile.birthYear
        val dayun = bazi.daYun.firstOrNull { age in it.startAge..it.endAge }?.pillar
            ?: bazi.daYun.lastOrNull()?.takeIf { age >= it.startAge }?.pillar
        return when (period) {
            "day" -> CompositeFortuneGenerator.generate(EasternFortuneGenerator.generate(bazi.chart, date, "day", dayun),
                WesternFortuneGenerator.generate(zodiac, date, "day", natal), date)
            "week" -> CompositeFortuneGenerator.generateWeekly(bazi.chart, zodiac, date, dayun, natal)
            "month" -> CompositeFortuneGenerator.generateMonthly(bazi.chart, zodiac, date, dayun, natal)
            "year" -> CompositeFortuneGenerator.generateYearly(bazi.chart, zodiac, date, dayun, natal)
            else -> error("unsupported_reading_period")
        }
    }
    fun periodCode(label: String): String? = when (label) {
        "day", "今日", "今天", "日" -> "day"
        "week", "本周", "周" -> "week"
        "month", "本月", "月" -> "month"
        "year", "今年", "本年", "年" -> "year"
        else -> null
    }
    fun adjacent(date: LocalDate, period: String, direction: Int): LocalDate {
        require(direction in setOf(-1, 0, 1))
        val step = direction.toLong()
        return when (period) {
            "day" -> date.plusDays(step)
            "week" -> date.plusWeeks(step)
            "month" -> date.plusMonths(step)
            "year" -> date.plusYears(step)
            else -> error("unsupported_reading_period")
        }
    }
    data class SensitivitySample(val time: LocalDateTime, val chart: String)
    fun baziSensitivity(profile: UserProfile): List<SensitivitySample> {
        val original = LocalDateTime.of(profile.birthYear, profile.birthMonth, profile.birthDay, profile.birthHour, profile.birthMinute)
        return listOf(-60L, 0L, 60L).map { minutes ->
            val date = original.plusMinutes(minutes)
            val changed = profile.copy(birthYear = date.year, birthMonth = date.monthValue, birthDay = date.dayOfMonth,
                birthHour = date.hour, birthMinute = date.minute)
            SensitivitySample(date, BaziCalculator.calculate(changed).display)
        }
    }
}
