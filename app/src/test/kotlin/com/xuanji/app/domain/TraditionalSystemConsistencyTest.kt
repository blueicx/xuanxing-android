package com.xuanji.app.domain

import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.domain.divination.HellenisticAstrology
import com.xuanji.app.domain.divination.Ifa
import com.xuanji.app.domain.divination.MayaTzolkin
import com.xuanji.app.domain.divination.QiMen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class TraditionalSystemConsistencyTest {

    @Test
    fun ziwei_life_and_body_markers_follow_their_actual_branch_positions() {
        val chart = ZiweiCalculator.calculate(1990, 4, 18, 9, 30)

        val life = chart.palaces.single { it.isLife }
        val body = chart.palaces.single { it.isBody }
        assertEquals("命宫", life.name)
        assertEquals(chart.lifePalace, life.branch)
        assertEquals(chart.bodyPalace, body.branch)
    }

    @Test
    fun ziwei_civil_date_input_is_explicitly_marked_as_an_approximation() {
        val chart = ZiweiCalculator.calculate(1990, 4, 18, 9, 30)

        assertTrue(chart.note.contains("公历近似"))
    }

    @Test
    fun bazi_uses_zi_chu_as_the_day_boundary() {
        val lateZi = UserProfile(2000, 1, 1, 23, 30, "北京")
        val followingDay = UserProfile(2000, 1, 2, 0, 30, "北京")

        assertEquals(
            BaziCalculator.calculate(followingDay).dayPillar,
            BaziCalculator.calculate(lateZi).dayPillar
        )
    }

    @Test
    fun qimen_day_cycle_matches_the_shared_bazi_day_cycle() {
        val date = LocalDate.of(2000, 1, 7)
        val expected = pillarIndex(BaziCalculator.dayPillarForDate(date))
        val method = QiMen::class.java.getDeclaredMethod(
            "dayGanZhi",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        ).apply { isAccessible = true }

        val actual = method.invoke(QiMen, date.year, date.monthValue, date.dayOfMonth) as Int
        assertEquals(expected, actual)
    }

    @Test
    fun maya_gmt_correlation_maps_2012_end_of_baktun_to_4_ajaw_3_kankin() {
        val result = MayaTzolkin.forDate(LocalDate.of(2012, 12, 21))

        assertEquals("13.0.0.0.0", result.longCount.label)
        assertEquals(4, result.tzolkin.number)
        assertEquals("Ajaw", result.tzolkin.name)
        assertEquals("Kankin", result.haab.month)
        assertEquals(3, result.haab.day)
    }

    @Test
    fun hellenistic_ascendant_changes_with_sidereal_time_not_solar_longitude() {
        val date = LocalDate.of(2000, 1, 1)
        val midnight = HellenisticAstrology.chart(date, 0.0)
        val noon = HellenisticAstrology.chart(date, 12.0)

        assertTrue(angularDistance(ascendant(midnight), ascendant(noon)) > 100.0)
    }

    @Test
    fun western_detail_does_not_claim_jpl_validity_beyond_2050() {
        val detail = ZodiacCalculator.calculateDetail(2000, 1, 1, 12, 0, "北京")

        assertTrue(detail.note.contains("1800–2050"))
    }

    @Test
    fun natal_chart_respects_the_supplied_birth_time_zone_offset() {
        val chinaTime = ZodiacCalculator.calculateNatalChart(
            2000, 1, 1, 12, 0, "自定义地点", 39.90, 116.41, 8.0
        )
        val utcTime = ZodiacCalculator.calculateNatalChart(
            2000, 1, 1, 12, 0, "自定义地点", 39.90, 116.41, 0.0
        )

        assertTrue(angularDistance(chinaTime.ascendant, utcTime.ascendant) > 80.0)
    }

    @Test
    fun ifa_catalogue_exposes_sixteen_unique_main_odu_names() {
        assertEquals(16, Ifa.oduTable().size)
        assertEquals(16, Ifa.oduTable().map { it.first }.distinct().size)
    }

    private fun pillarIndex(pillar: com.xuanji.app.data.model.Pillar): Int =
        (0 until 60).first { index ->
            index % 10 == pillar.stem.ordinal && index % 12 == pillar.branch.ordinal
        }

    private fun signIndex(sign: String): Int = listOf(
        "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座",
        "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"
    ).indexOf(sign)

    private fun ascendant(chart: com.xuanji.app.domain.divination.HellenisticChart): Double =
        chart.ascDeg + signIndex(chart.ascSign) * 30.0

    private fun angularDistance(a: Double, b: Double): Double {
        val diff = abs((a - b) % 360.0)
        return if (diff > 180.0) 360.0 - diff else diff
    }
}
