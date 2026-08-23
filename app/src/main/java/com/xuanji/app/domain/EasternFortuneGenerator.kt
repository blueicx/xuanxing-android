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

    // --- 五行生克关系评分（与小程序 easternFortune.js 完全对齐） ---

    private val shengMap = mapOf(
        Element.WOOD to Element.FIRE, Element.FIRE to Element.EARTH,
        Element.EARTH to Element.METAL, Element.METAL to Element.WATER,
        Element.WATER to Element.WOOD
    )
    private val keMap = mapOf(
        Element.WOOD to Element.EARTH, Element.EARTH to Element.WATER,
        Element.WATER to Element.FIRE, Element.FIRE to Element.METAL,
        Element.METAL to Element.WOOD
    )

    private fun relationScore(me: Element, other: Element): Int = when {
        me == other -> 8          // 同我：比劫
        shengMap[other] == me -> 12   // 生我：印星
        shengMap[me] == other -> 2    // 我生：食伤
        keMap[me] == other -> 5       // 我克：财星
        keMap[other] == me -> -10     // 克我：官杀
        else -> 0
    }

    private fun relationName(score: Int) = when (score) {
        12 -> "印星(生扶)"
        8 -> "比劫(同气)"
        5 -> "财星(可得)"
        2 -> "食伤(泄秀)"
        -10 -> "官杀(受制)"
        else -> "平"
    }

    /** 计算流月干支（简化节气法） */
    private fun monthPillarForDate(date: LocalDate): Pair<String, Pair<Element, Element>> {
        // 用日柱推算近似月柱：取当月15日的月支
        val midDate = LocalDate.of(date.year, date.month, 15)
        val pillar = BaziCalculator.dayPillarForDate(midDate)
        return pillar.display to (pillar.stem.element to pillar.branch.element)
    }

    /** 东方每周运势 — 用本周中位日的日柱做五行分析 */
    fun generateWeekly(chart: BaziChart, date: LocalDate): EasternDailyFortune {
        val midDate = date.plusDays(3)
        val midPillar = BaziCalculator.dayPillarForDate(midDate)
        return buildByRelation(chart, date, midPillar.display, midPillar.stem.element, midPillar.branch.element, "本周", "week")
    }

    /** 东方每月运势 — 用流月干支做五行分析 */
    fun generateMonthly(chart: BaziChart, date: LocalDate): EasternDailyFortune {
        val (gzText, elements) = monthPillarForDate(date)
        return buildByRelation(chart, date, gzText, elements.first, elements.second, "本月", "month")
    }

    private fun buildByRelation(
        chart: BaziChart,
        date: LocalDate,
        pillarGz: String,
        stemElement: Element,
        branchElement: Element,
        periodLabel: String,
        periodTag: String
    ): EasternDailyFortune {
        val me = chart.dayMasterElement
        val fav = chart.favorableElements
        val unfav = chart.unfavorableElements

        val stemRel = relationScore(me, stemElement)
        val branchRel = relationScore(me, branchElement)
        var overall = 50 + stemRel + branchRel

        val periodElems = setOf(stemElement, branchElement)
        overall += fav.count { it in periodElems } * 6
        overall += unfav.count { it in periodElems } * (-5)

        overall = overall.coerceIn(15, 96)

        fun cat(base: Int, bonus: Int) = (base + bonus).coerceIn(10, 98)
        val career = cat(overall, if (stemRel >= 0) 3 else -3)
        val wealth = cat(overall, if (branchRel >= 0) 3 else -3)
        val love = cat(overall, if (stemRel > 5 && branchRel > 0) 4 else (if (stemRel < 0) -4 else 0))
        val health = cat(overall, if (branchRel > 5) 4 else (if (branchRel < 0) -4 else 0))

        val band = when {
            overall >= 80 -> "气场通达，诸事顺遂，宜把握良机"
            overall >= 65 -> "运势上扬，主动出击多有所得"
            overall >= 50 -> "平稳和顺，按部就班即可"
            overall >= 35 -> "宜守宜稳，蓄势待发为佳"
            else -> "宜低调内敛，凡事慎行多思"
        }
        val summary = "${periodLabel}干支「$pillarGz」，日主「${elementName(me)}」。天干${relationName(stemRel)}、地支${relationName(branchRel)}。整体$band。"

        val favElem = fav.firstOrNull() ?: Element.EARTH
        val luckyColor = elementColor(favElem)
        val luckyDirection = elementDirection(favElem)

        val advice = "${periodLabel}流期「$pillarGz」，与日主${elementName(me)}形成" +
            "${relationName(stemRel)}($stemRel)及${relationName(branchRel)}($branchRel)。" +
            "五行喜 ${fav.joinToString("、") { elementName(it) }}。" +
            "幸运色：$luckyColor；吉利方位：$luckyDirection。"

        return EasternDailyFortune(
            dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            overallScore = overall,
            careerScore = career,
            wealthScore = wealth,
            loveScore = love,
            healthScore = health,
            summary = summary,
            advice = advice,
            dayPillarText = pillarGz,
            favorableToday = fav,
            luckyColor = luckyColor,
            luckyDirection = luckyDirection,
            period = periodTag
        )
    }
}
