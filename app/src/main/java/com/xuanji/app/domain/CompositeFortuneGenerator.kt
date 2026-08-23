package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.data.model.BaziChart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.round
import kotlin.random.Random

/**
 * 综合运势生成器：将东方（八字）与西方（星座）两套每日运势融合，
 * 产出综合分、六个维度、幸运数字/色/方位与注意事项。
 * 同样基于日期种子，保证同一天结果稳定、离线可用。
 */
object CompositeFortuneGenerator {

    fun generate(
        eastern: EasternDailyFortune,
        western: WesternDailyFortune,
        date: LocalDate
    ): CompositeDailyFortune {
        // seed 注入命盘指纹：八字四柱字符串 + 西方星座，确保不同生日当天综合分不同
        val easternFingerprint = eastern.dayPillarText.hashCode().toLong() xor
            eastern.overallScore.toLong().shl(8)
        val westernFingerprint = (western.sign.hashCode().toLong() shl 16) xor
            western.overallScore.toLong()
        val seed = run {
            var s = date.toEpochDay() xor
                (easternFingerprint * 31L + westernFingerprint)
            if (s == 0L) s = date.toEpochDay()
            s
        }
        val rnd = Random(seed)

        fun blend(a: Int, b: Int): Int = round((a + b) / 2.0).toInt()
        fun jitter(range: Int): Int = rnd.nextInt(range) - range / 2

        // 共享维度：东西方直接融合
        val career = (blend(eastern.careerScore, western.careerScore) + jitter(6)).coerceIn(10, 98)
        val wealth = (blend(eastern.wealthScore, western.wealthScore) + jitter(6)).coerceIn(10, 98)
        val love   = (blend(eastern.loveScore, western.loveScore) + jitter(6)).coerceIn(10, 98)

        // 情感：以 love 为基，附加扰动形成不同解读
        val emotion = (love + jitter(8)).coerceIn(10, 98)
        // 桃花运：在 love 基础上叠加命主喜用神旺衰信号（用八字评分高低映射）
        val peach = (blend(eastern.loveScore, western.loveScore) + rnd.nextInt(10)).coerceIn(10, 98)
        // 学习：八字主文昌/印星≈career；星座无直接项，以 career 综合推断
        val study = (blend(eastern.careerScore, western.careerScore) + jitter(10)).coerceIn(10, 98)
        // 健康：八字 health 与星座 health 融合
        val health = (blend(eastern.healthScore, western.healthScore) + jitter(6)).coerceIn(10, 98)

        val overall = round(
            (career + wealth + love + emotion + peach + study + health) / 7.0
        ).toInt().coerceIn(15, 98)

        fun band(s: Int): String = when {
            s >= 80 -> "上佳"
            s >= 65 -> "良好"
            s >= 50 -> "平稳"
            s >= 35 -> "偏弱"
            else -> "低迷"
        }
        fun interp(label: String, s: Int): String = when {
            s >= 65 -> "$label${band(s)}，宜积极把握，顺势而为。"
            s >= 50 -> "$label${band(s)}，按部就班即可，细节决定成效。"
            s >= 35 -> "$label${band(s)}，宜守不宜攻，稳妥为先。"
            else -> "$label${band(s)}，宜低调蓄力，谨慎行事。"
        }

        val dimensions = listOf(
            FortuneDimension("peach", "桃花运", peach, interp("桃花运", peach)),
            FortuneDimension("emotion", "情感", emotion, interp("情感", emotion)),
            FortuneDimension("career", "事业", career, interp("事业", career)),
            FortuneDimension("study", "学习", study, interp("学习", study)),
            FortuneDimension("wealth", "财富", wealth, interp("财富", wealth)),
            FortuneDimension("health", "健康", health, interp("健康", health))
        )

        // 幸运数字：综合东西方的种子派生（1-9）
        val luckyNumber = 1 + (seed.ushr(3).toInt().let { if (it < 0) -it else it } % 9)
        // 幸运色：以八字喜用神色为主（更个性化），西方色为辅
        val luckyColor = eastern.luckyColor
        val luckyDirection = eastern.luckyDirection

        // 注意事项：取最低分项提示
        val lowDim = dimensions.minByOrNull { it.score }
        val cautions = buildString {
            append("综合运势${band(overall)}。")
            if (lowDim != null && lowDim.score < 55) {
                append("需留意「${lowDim.label}」(${lowDim.score}分)：${lowDim.interpretation}")
            } else {
                append("各项较为均衡，保持节奏即可。")
            }
            append("\n东方侧重${eastern.dayPillarText}干支喜忌，西方侧重${western.sign}每日能量。")
        }

        return CompositeDailyFortune(
            dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            overallScore = overall,
            dimensions = dimensions,
            luckyNumber = luckyNumber,
            luckyColor = luckyColor,
            luckyDirection = luckyDirection,
            cautions = cautions,
            eastern = eastern,
            western = western,
            period = "day"
        )
    }

    private fun epochWeek(date: LocalDate): Long = Math.floorDiv(date.toEpochDay(), 7L)

    private fun epochMonth(date: LocalDate): Long = date.year.toLong() * 12L + (date.monthValue - 1)

    /** 综合每周运势 — 逻辑融合（与小程序 compositeFortune.js 对齐） */
    fun generateWeekly(chart: BaziChart, info: ZodiacCalculator.ZodiacInfo, date: LocalDate): CompositeDailyFortune {
        val easternW = EasternFortuneGenerator.generateWeekly(chart, date)
        val westernW = WesternFortuneGenerator.generateWeekly(info, date)
        return buildLogical(easternW, westernW, date, "week")
    }

    /** 综合每月运势 */
    fun generateMonthly(chart: BaziChart, info: ZodiacCalculator.ZodiacInfo, date: LocalDate): CompositeDailyFortune {
        val easternM = EasternFortuneGenerator.generateMonthly(chart, date)
        val westernM = WesternFortuneGenerator.generateMonthly(info, date)
        return buildLogical(easternM, westernM, date, "month")
    }

    /** 确定性融合：直接取两套体系均值，无随机扰动（与小程序对齐） */
    private fun buildLogical(
        eastern: EasternDailyFortune,
        western: WesternDailyFortune,
        date: LocalDate,
        periodTag: String
    ): CompositeDailyFortune {
        fun blend(a: Int, b: Int) = Math.round((a + b) / 2.0).toInt()

        val career = blend(eastern.careerScore, western.careerScore).coerceIn(10, 98)
        val wealth = blend(eastern.wealthScore, western.wealthScore).coerceIn(10, 98)
        val love = blend(eastern.loveScore, western.loveScore).coerceIn(10, 98)
        // 情感以爱情为基础，偏移由东方感情分决定
        val emotionShift = if (eastern.loveScore > western.loveScore) 3 else -3
        val emotion = (love + emotionShift).coerceIn(10, 98)
        // 桃花运：两系感情分加权
        val peach = Math.round(eastern.loveScore * 0.6 + western.loveScore * 0.4).toInt().coerceIn(10, 98)
        // 学习：事业分的延伸
        val studyShift = if (eastern.careerScore >= 60) 2 else -2
        val study = (blend(eastern.careerScore, western.careerScore) + studyShift).coerceIn(10, 98)
        val health = blend(eastern.healthScore, western.healthScore).coerceIn(10, 98)

        val overall = round((career + wealth + love + emotion + peach + study + health) / 7.0)
            .toInt().coerceIn(15, 98)

        fun band(s: Int): String = when {
            s >= 80 -> "上佳"; s >= 65 -> "良好"; s >= 50 -> "平稳"; s >= 35 -> "偏弱"; else -> "低迷"
        }
        fun interp(label: String, s: Int): String = when {
            s >= 65 -> "$label${band(s)}，宜积极把握，顺势而为。"
            s >= 50 -> "$label${band(s)}，按部就班即可，细节决定成效。"
            s >= 35 -> "$label${band(s)}，宜守不宜攻，稳妥为先。"
            else -> "$label${band(s)}，宜低调蓄力，谨慎行事。"
        }

        val dimensions = listOf(
            FortuneDimension("peach", "桃花运", peach, interp("桃花运", peach)),
            FortuneDimension("emotion", "情感", emotion, interp("情感", emotion)),
            FortuneDimension("career", "事业", career, interp("事业", career)),
            FortuneDimension("study", "学习", study, interp("学习", study)),
            FortuneDimension("wealth", "财富", wealth, interp("财富", wealth)),
            FortuneDimension("health", "健康", health, interp("健康", health))
        )

        val luckyColor = eastern.luckyColor
        val luckyDirection = eastern.luckyDirection
        val luckyNumber = 1 + ((eastern.overallScore + western.overallScore) % 9)

        val lowDim = dimensions.minByOrNull { it.score }
        val cautions = buildString {
            append("综合运势${band(overall)}。")
            if (lowDim != null && lowDim.score < 55) {
                append("需留意「${lowDim.label}」(${lowDim.score}分)：${lowDim.interpretation}")
            } else {
                append("各项较为均衡，保持节奏即可。")
            }
            val periodLabel = if (periodTag == "week") "本周" else "本月"
            append("\n东方${periodLabel}流期${eastern.dayPillarText}，西方${periodLabel}${western.sign}相位。")
        }

        return CompositeDailyFortune(
            dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            overallScore = overall,
            dimensions = dimensions,
            luckyNumber = luckyNumber,
            luckyColor = luckyColor,
            luckyDirection = luckyDirection,
            cautions = cautions,
            eastern = eastern,
            western = western,
            period = periodTag
        )
    }
}
