package com.xuanji.app.domain

import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.ZodiacCalculator.NatalChart
import com.xuanji.app.domain.ZodiacCalculator.SkySnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 西方（占星）周期运势生成器。
 *
 * 日 / 周 / 月 / 年 全部走真实星历：
 *  - 日：当天北京正午的星空与本命盘比相位；
 *  - 周：本周（锚周一）七天逐日采样；
 *  - 月：当月整月逐日采样；
 *  - 年：当年每四天一次、全年约 90 个采样点。
 * 评分由相位力度累加（慢行星累加为背景、个人行星取均值），解说由同一批相位展开。
 * 无随机：同一本命盘、同一日期、同一周期，输出完全相同。
 */
object WesternFortuneGenerator {

    private val DIM_LABELS = linkedMapOf(
        "career" to "事业运",
        "wealth" to "财运",
        "love" to "感情运",
        "study" to "学业运",
        "health" to "健康运"
    )

    /** 占星传统色系（按星座），比五行色系更贴合西方语境 */
    private val SIGN_COLORS = listOf(
        "铁锈红", "苔绿", "鹅黄", "月白", "金橙", "雾蓝灰",
        "藕粉", "墨黑", "茄紫", "岩棕", "电光蓝", "海蓝"
    )

    fun generate(info: ZodiacCalculator.ZodiacInfo, date: LocalDate): WesternDailyFortune =
        generate(info, date, "day", null)

    fun generate(
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        period: String,
        natal: NatalChart?
    ): WesternDailyFortune {
        val dates = WesternTransitReader.sampleDates(date, period)
        val label = periodLabel(period)
        val reading = WesternTransitReader.read(natal, info.sign, dates, label)
        val span = (dates.last().toEpochDay() - dates.first().toEpochDay()).toInt() + 1
        return assemble(info, date, period, reading, span, label)
    }

    fun generateWeekly(
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        natal: NatalChart? = null
    ): WesternDailyFortune = generate(info, date, "week", natal)

    fun generateMonthly(
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        natal: NatalChart? = null
    ): WesternDailyFortune = generate(info, date, "month", natal)

    fun generateYearly(
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        natal: NatalChart? = null
    ): WesternDailyFortune = generate(info, date, "year", natal)

    private fun periodLabel(period: String): String = when (period) {
        "week" -> "本周"
        "month" -> "本月"
        "year" -> "本年"
        else -> "今日"
    }

    private fun assemble(
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        period: String,
        reading: WesternReading,
        windowDays: Int,
        periodText: String
    ): WesternDailyFortune {
        val overall = (50 + reading.overallDelta).coerceIn(12, 97)
        fun score(delta: Int): Int = (overall + delta).coerceIn(6, 99)

        val luckySignIdx = WesternTransitReader.signIndexOf(reading.luckySign)
        val luckyColor = SIGN_COLORS[luckySignIdx]
        val luckyDirection = elementDirection(reading.luckyElement)
        // 幸运数取自真实天象：当值太阳度数与主导相位力度的组合（1-9）
        val sky: SkySnapshot = ZodiacCalculator.skyAt(date)
        val sunDegree = (sky.lon("太阳") % 30.0).toInt()
        val leadWeight = reading.insights.firstOrNull()?.weight ?: 0
        val luckyNumber = (((sunDegree + leadWeight + luckySignIdx * 3).let { ((it % 9) + 9) % 9 }) + 1)

        val dims = DIM_LABELS.entries.map { (key, label) ->
            val delta = when (key) {
                "career" -> reading.careerDelta
                "wealth" -> reading.wealthDelta
                "love" -> reading.loveDelta
                "study" -> reading.studyDelta
                else -> reading.healthDelta
            }
            val s = score(delta)
            FortuneDimension(
                key = key,
                label = label,
                score = s,
                interpretation = interpret(label, s, reading.notes[key], windowDays, periodText)
            )
        }

        return WesternDailyFortune(
            dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            sign = info.sign,
            overallScore = overall,
            careerScore = score(reading.careerDelta),
            wealthScore = score(reading.wealthDelta),
            loveScore = score(reading.loveDelta),
            healthScore = score(reading.healthDelta),
            summary = reading.summary,
            luckyNumber = luckyNumber,
            luckyColor = luckyColor,
            luckyDirection = luckyDirection,
            period = period,
            insights = reading.insights,
            dimensionNotes = dims,
            dimensionBasis = reading.notes
        )
    }

    private fun interpret(
        label: String,
        score: Int,
        rawNotes: List<String>?,
        windowDays: Int,
        periodText: String
    ): String {
        val basis = (rawNotes ?: emptyList()).distinct().take(2)
        val trend = when {
            score >= 82 -> "${label}得到天象明显的助力"
            score >= 68 -> "${label}偏顺，推一下就动"
            score >= 52 -> "${label}中性，取决于你怎么安排"
            score >= 38 -> "${label}受阻，宜收不宜放"
            else -> "${label}处于低谷，先止损再谈发展"
        }
        val body = if (basis.isEmpty()) {
            "$trend。${periodText}这 ${windowDays} 天内没有行运行星精准触及这一领域对应的本命点位，${label}处于自然状态。"
        } else {
            "$trend。依据是：${basis.joinToString("；")}。"
        }
        return body + when (label) {
            "事业运" -> if (score >= 68) "该出面的场合出面，成果会被记住。" else "${periodText}少承诺、多交付，把口径留在纸上。"
            "财运" -> if (score >= 68) "可以主动谈价与收款，扩张也划算。" else "不宜加杠杆、不宜替人垫钱，先看清条款。"
            "感情运" -> if (score >= 68) "关系里的气氛是打开的，想说的话适合现在说。" else "别把一时的感受当结论，先把日常过稳。"
            "学业运" -> if (score >= 68) "吸收快、表达准，把难的科目排前面。" else "读不进去就换动手的事，硬坐只会耗掉心情。"
            else -> if (score >= 68) "体力与情绪承载得住，可加大工作量。" else "睡眠与饮食先管住，身体在替你记账。"
        }
    }
}
