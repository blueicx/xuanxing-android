package com.xuanji.app.domain

import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.Pillar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 东方（八字）每日运势生成器。
 * 基于「今日日干支」与用户「日主 / 喜用神」的关系，得到稳定的每日评分。
 * 同一天结果固定，跨天自然变化。
 */
object EasternFortuneGenerator {

    fun generate(chart: BaziChart, date: LocalDate): EasternDailyFortune {
        val dayPillar = BaziCalculator.dayPillarForDate(date)
        val fav = chart.favorableElements.toSet()
        val unfav = chart.unfavorableElements.toSet()

        fun contribution(e: Element): Int = when {
            e in fav -> 14
            e in unfav -> -10
            else -> 3
        }
        val base = 50 + contribution(dayPillar.stem.element) + contribution(dayPillar.branch.element)

        val seed = run {
            var s = date.toEpochDay() xor chart.display.hashCode().toLong()
            if (s == 0L) s = date.toEpochDay()
            s
        }
        val rnd = Random(seed)
        val jitter = rnd.nextInt(21) - 10
        val overall = (base + jitter).coerceIn(15, 96)

        fun catScore(): Int = (overall + rnd.nextInt(25) - 12).coerceIn(10, 98)
        val career = catScore()
        val wealth = catScore()
        val love = catScore()
        val health = catScore()

        val favElem = chart.favorableElements.firstOrNull() ?: Element.EARTH
        val luckyColor = elementColor(favElem)
        val luckyDirection = elementDirection(favElem)

        val summary = buildSummary(overall, dayPillar, chart)
        val advice = "今日日干为「${dayPillar.display}」，五行喜 " +
            chart.favorableElements.joinToString("、") { elementName(it) } +
            "。幸运色：$luckyColor；吉利方位：$luckyDirection。宜循序渐进，忌冒进躁动。"

        return EasternDailyFortune(
            dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            overallScore = overall,
            careerScore = career,
            wealthScore = wealth,
            loveScore = love,
            healthScore = health,
            summary = summary,
            advice = advice,
            dayPillarText = dayPillar.display,
            favorableToday = chart.favorableElements,
            luckyColor = luckyColor,
            luckyDirection = luckyDirection
        )
    }

    private fun buildSummary(overall: Int, dayPillar: Pillar, chart: BaziChart): String {
        val band = when {
            overall >= 80 -> "气场通达，诸事顺遂，宜把握良机"
            overall >= 65 -> "运势上扬，主动出击多有所得"
            overall >= 50 -> "平稳和顺，按部就班即可"
            overall >= 35 -> "宜守宜稳，蓄势待发为佳"
            else -> "宜低调内敛，凡事慎行多思"
        }
        return "今日干支「${dayPillar.display}」，日主${elementName(chart.dayMasterElement)}。整体$band。"
    }
}
