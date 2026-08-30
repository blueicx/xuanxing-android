package com.xuanji.app.domain

import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.FortuneInsight
import com.xuanji.app.data.model.Pillar
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.ZodiacCalculator.NatalChart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 综合运势：把八字与占星两套体系放在一张桌上对账。
 *
 * 融合原则（无随机）：
 *  - 每个维度按「主属体系」加权：事业、学习、健康更信八字；财富两边平分；情感更看行运；
 *  - 加权之外只接受真实信号修正，且只在桃花运／情感两个维度上生效，其余维度不因文案改分；
 *  - 两套体系分歧大时如实写成分歧并给分工建议，不抹平成一句和事老的话；
 *  - 所有解说取自两套体系自己的取证（dimensionBasis），没有信号就承认没有信号。
 */
object CompositeFortuneGenerator {

    /** eastShare：这一维度里更信八字的比重 */
    private data class Dim(
        val key: String,
        val label: String,
        val eastKey: String,
        val westKey: String,
        val eastShare: Int
    )

    private val DIMS = listOf(
        Dim("career", "事业", "career", "career", 55),
        Dim("wealth", "财富", "wealth", "wealth", 50),
        Dim("peach", "桃花运", "love", "love", 50),
        Dim("emotion", "情感", "love", "love", 45),
        Dim("study", "学习", "study", "study", 60),
        Dim("health", "健康", "health", "health", 55)
    )

    private data class Blend(val dim: Dim, val east: Int, val west: Int, val score: Int)

    fun generate(
        eastern: EasternDailyFortune,
        western: WesternDailyFortune,
        date: LocalDate,
        period: String = "day"
    ): CompositeDailyFortune {
        val label = periodLabel(period)
        val eNotes = eastern.dimensionNotes.associateBy { it.key }
        val wNotes = western.dimensionNotes.associateBy { it.key }
        val insights = eastern.insights + western.insights
        val adjust = insightAdjust(insights)

        val blends = DIMS.map { dim ->
            val e = eNotes[dim.eastKey]?.score ?: eastern.overallScore
            val w = wNotes[dim.westKey]?.score ?: western.overallScore
            val raw = (e * dim.eastShare + w * (100 - dim.eastShare)) / 100.0
            Blend(dim, e, w, (raw + (adjust[dim.key] ?: 0)).roundToInt().coerceIn(8, 98))
        }
        val dims = blends.map { b ->
            FortuneDimension(b.dim.key, b.dim.label, b.score, fuse(b, b.score, eastern, western, label))
        }
        val dimMean = blends.map { it.score }.average()
        val overall = ((dimMean * 4 + eastern.overallScore + western.overallScore) / 6)
            .roundToInt().coerceIn(12, 97)

        return CompositeDailyFortune(
            dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            overallScore = overall,
            dimensions = dims,
            luckyNumber = luckyNumber(eastern, western, overall),
            luckyColor = eastern.luckyColor,
            luckyDirection = eastern.luckyDirection,
            cautions = cautions(blends, insights, eastern, western, overall, label),
            eastern = eastern,
            western = western,
            period = period,
            periodSummary = periodSummary(blends, eastern, western, overall, label),
            insights = insights.filter { it.weight != 0 }
                .sortedByDescending { abs(it.weight) }
                .take(8)
        )
    }

    fun generateWeekly(
        chart: BaziChart,
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        dayun: Pillar? = null,
        natal: NatalChart? = null
    ): CompositeDailyFortune = build(
        EasternFortuneGenerator.generateWeekly(chart, date, dayun),
        WesternFortuneGenerator.generateWeekly(info, date, natal),
        date, "week"
    )

    fun generateMonthly(
        chart: BaziChart,
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        dayun: Pillar? = null,
        natal: NatalChart? = null
    ): CompositeDailyFortune = build(
        EasternFortuneGenerator.generateMonthly(chart, date, dayun),
        WesternFortuneGenerator.generateMonthly(info, date, natal),
        date, "month"
    )

    fun generateYearly(
        chart: BaziChart,
        info: ZodiacCalculator.ZodiacInfo,
        date: LocalDate,
        dayun: Pillar? = null,
        natal: NatalChart? = null
    ): CompositeDailyFortune = build(
        EasternFortuneGenerator.generateYearly(chart, date, dayun),
        WesternFortuneGenerator.generateYearly(info, date, natal),
        date, "year"
    )

    private fun build(
        e: EasternDailyFortune,
        w: WesternDailyFortune,
        date: LocalDate,
        period: String
    ): CompositeDailyFortune = generate(e, w, date, period)

    private fun periodLabel(period: String): String = when (period) {
        "week" -> "本周"
        "month" -> "本月"
        "year" -> "本年"
        else -> "今日"
    }

    private fun basisOf(fortune: EasternDailyFortune, key: String): List<String> =
        fortune.dimensionBasis[key] ?: emptyList()

    private fun basisOf(fortune: WesternDailyFortune, key: String): List<String> =
        fortune.dimensionBasis[key] ?: emptyList()

    /**
     * 信号修正：只有明确命中感情领域的真实信号才动分，取值锁死在 -6..+5。
     * 不参与事业／财富／学习／健康，避免「换个文案就换个分」。
     */
    private fun insightAdjust(insights: List<FortuneInsight>): Map<String, Int> {
        var peach = 0
        var emotion = 0
        insights.forEach { ins ->
            val text = ins.tag + ins.title + ins.body
            val good = ins.weight >= 0
            when {
                text.contains("桃花") -> peach += if (good) 2 else -3
                text.contains("五宫") -> peach += if (good) 2 else -2
                text.contains("金星") -> peach += if (good) 1 else -2
            }
            when {
                text.contains("空亡") -> emotion -= 2
                text.contains("自刑") -> emotion -= 2
                text.contains("羊刃") -> emotion -= 2
                text.contains("反吟") -> emotion -= 2
                text.contains("伏吟") -> emotion -= 1
                text.contains("合日主") -> emotion += 2
                text.contains("月亮") -> emotion += if (good) 2 else -2
                text.contains("海王星") -> emotion += if (good) 1 else -1
            }
        }
        return mapOf(
            "peach" to peach.coerceIn(-6, 5),
            "emotion" to emotion.coerceIn(-6, 5)
        )
    }

    /** 维度解说：报分、报两边取证、最后一句怎么办 */
    private fun fuse(
        b: Blend,
        score: Int,
        eastern: EasternDailyFortune,
        western: WesternDailyFortune,
        label: String
    ): String {
        val eb = basisOf(eastern, b.dim.eastKey).distinct().take(2)
        val wb = basisOf(western, b.dim.westKey).distinct().take(2)
        val trend = when {
            score >= 82 -> "${label}${b.dim.label}两边得力，是可以押注的地方"
            score >= 68 -> "${label}${b.dim.label}偏顺，推进有回应"
            score >= 52 -> "${label}${b.dim.label}持平，结果取决于安排"
            score >= 38 -> "${label}${b.dim.label}受阻，宜减不宜加"
            else -> "${label}${b.dim.label}在低谷，先守住别丢东西"
        }
        val pillar = eastern.periodPillarText.ifBlank { eastern.dayPillarText }
        val eastText = if (eb.isEmpty()) {
            "八字「$pillar」在${b.dim.label}上未起刑冲合会，无加分也无减分"
        } else {
            "八字：${eb.joinToString("；")}"
        }
        val westText = if (wb.isEmpty()) {
            "星盘${western.sign}这一周期无行星精准触及${b.dim.label}对应点位"
        } else {
            "星盘：${wb.joinToString("；")}"
        }
        val gap = b.east - b.west
        val verdict = when {
            gap >= 15 -> "两套体系口径不一：八字说动、星盘说卡。多半是机会在而节奏不在，按星盘的时间表落地，按八字的窗口出手。"
            gap <= -15 -> "两套体系口径不一：星盘说动、八字说卡。多半是外部环境顺而自身接不住，先处理八字点出的忌神问题再谈扩张。"
            gap >= 6 -> "两边都算，八字权重更高（${b.dim.eastShare}），人事与时机是这一维的主要变量。"
            gap <= -6 -> "两边都算，星盘权重更高（${100 - b.dim.eastShare}），环境与节奏是这一维的主要变量。"
            else -> "两套体系各占${b.dim.eastShare}比${100 - b.dim.eastShare}且结论一致，可执行度高。"
        }
        return "$trend。$eastText；$westText。$verdict"
    }

    /** 本周期总评：两套体系各自立论 + 最强最弱 + 最大分歧的落地办法 */
    private fun periodSummary(
        blends: List<Blend>,
        eastern: EasternDailyFortune,
        western: WesternDailyFortune,
        overall: Int,
        label: String
    ): String {
        val pillar = eastern.periodPillarText.ifBlank { eastern.dayPillarText }
        val best = blends.maxByOrNull { it.score }
        val worst = blends.minByOrNull { it.score }
        val diverging = blends.maxByOrNull { abs(it.east - it.west) }
        return buildString {
            append("${label}综合 $overall 分。")
            append("八字以「$pillar」立论判${eastern.overallScore}分：${eastern.summary}")
            append(" 星盘以${western.sign}当值相位立论判${western.overallScore}分：${western.summary}")
            if (best != null && best.score >= 68) {
                append(" 全周期最能借力的维度是${best.dim.label}（${best.score}分），有限的心力先花在这上面。")
            }
            if (worst != null && worst.score < 52) {
                append(" 最容易吃亏的是${worst.dim.label}（${worst.score}分），这一维宜委托、宜延后、宜降低预期。")
            }
            if (diverging != null && abs(diverging.east - diverging.west) >= 15) {
                append(" 分歧最大的是${diverging.dim.label}：八字${diverging.east}分、星盘${diverging.west}分，说明外部条件与你的准备度不匹配，别当纯机会也别当纯阻碍处理。")
            }
        }
    }

    private fun cautions(
        blends: List<Blend>,
        insights: List<FortuneInsight>,
        eastern: EasternDailyFortune,
        western: WesternDailyFortune,
        overall: Int,
        label: String
    ): String {
        val worst = blends.minByOrNull { it.score }
        val negatives = insights.filter { it.weight < 0 }
            .sortedBy { it.weight }
            .take(3)
        return buildString {
            append("${label}综合 $overall 分。")
            if (worst != null) append("最弱一项：${worst.dim.label} ${worst.score} 分。")
            if (negatives.isEmpty()) {
                val pillar = eastern.periodPillarText.ifBlank { eastern.dayPillarText }
                append("八字「$pillar」与星盘${western.sign}这一周期都没有查到实质凶象，不需要额外规避，按常规节奏推进。")
            } else {
                negatives.forEach { append("${it.tag}·${it.title}；") }
                append("以上是被两套体系同时点名的减分项，优先处理排在最前的那条。")
            }
            append("\n${eastern.advice}")
        }
    }

    /** 幸运数：取喜用神河图生成数，阴阳由两套体系合分的奇偶决定 */
    private fun luckyNumber(eastern: EasternDailyFortune, western: WesternDailyFortune, overall: Int): Int {
        val (sheng, cheng) = hetu(directionToElement(eastern.luckyDirection))
        val yang = (overall + western.overallScore) % 2 == 0
        val n = if (yang) sheng else cheng
        return if (n > 9) 9 else n
    }

    /** 河图数：天一生水地六成之，地二生火天七成之，天三生木地八成之，地四生金天九成之，天五生土地十成之 */
    private fun hetu(e: Element): Pair<Int, Int> = when (e) {
        Element.WATER -> 1 to 6
        Element.FIRE -> 2 to 7
        Element.WOOD -> 3 to 8
        Element.METAL -> 4 to 9
        Element.EARTH -> 5 to 10
    }

    private fun directionToElement(dir: String): Element = when {
        dir.contains("东") -> Element.WOOD
        dir.contains("南") -> Element.FIRE
        dir.contains("西") -> Element.METAL
        dir.contains("北") -> Element.WATER
        else -> Element.EARTH
    }
}
