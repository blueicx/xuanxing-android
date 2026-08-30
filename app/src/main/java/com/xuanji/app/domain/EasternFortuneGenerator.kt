package com.xuanji.app.domain

import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.Pillar
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 东方（八字）周期运势生成器。
 *
 * 日 / 周 / 月 / 年 走同一条解盘管线（[EasternPeriodReader]），
 * 差别只在「用哪一组干支论断」与「怎么加权」：
 *
 *  - 日：论当日流日柱，叠当前大运。
 *  - 周：本周锚定周一，逐日读七日流日柱取平均，再按权重加进流月（六分）与流年（四分）背景。
 *  - 月：论该日所在节气的流月柱，加流年（五分）背景。
 *  - 年：论流年柱并按太岁立论，加当下流月（二分）背景。
 *
 * 评分与解说出自同一批信号：分数由信号累加，文字由信号展开。
 * 同一命盘、同一日、同一周期，输出必完全相同。
 */
object EasternFortuneGenerator {

    private val DIM_LABELS = linkedMapOf(
        "career" to "事业运",
        "wealth" to "财运",
        "love" to "感情运",
        "study" to "学业运",
        "health" to "健康运"
    )

    /** 兼容旧调用：不传 period 时即今日流日。 */
    fun generate(chart: BaziChart, date: LocalDate): EasternDailyFortune =
        generate(chart, date, "day")

    fun generate(
        chart: BaziChart,
        date: LocalDate,
        period: String,
        dayun: Pillar? = null
    ): EasternDailyFortune {
        val (reading, periodKey) = when (period) {
            "week" -> readWeek(chart, date, dayun) to "week"
            "month" -> readMonth(chart, date, dayun) to "month"
            "year" -> readYear(chart, date, dayun) to "year"
            else -> EasternPeriodReader.read(
                chart, BaziCalculator.dayPillarForDate(date), "今日", dayun
            ) to "day"
        }
        return assemble(chart, date, periodKey, reading)
    }

    fun generateWeekly(chart: BaziChart, date: LocalDate, dayun: Pillar? = null): EasternDailyFortune =
        generate(chart, date, "week", dayun)

    fun generateMonthly(chart: BaziChart, date: LocalDate, dayun: Pillar? = null): EasternDailyFortune =
        generate(chart, date, "month", dayun)

    fun generateYearly(chart: BaziChart, date: LocalDate, dayun: Pillar? = null): EasternDailyFortune =
        generate(chart, date, "year", dayun)

    // ==================================================== 四种周期的论断取数

    /** 周：以周一为锚，七日逐日读数取平均，流月为势、流年为底。 */
    private fun readWeek(chart: BaziChart, date: LocalDate, dayun: Pillar?): EasternReading {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val days = (0L..6L).map { monday.plusDays(it) }
        val parts = mutableListOf<Pair<EasternReading, Int>>()
        days.forEach { d ->
            val label = WEEK_NAMES[d.dayOfWeek.value - 1]
            parts += EasternPeriodReader.read(chart, BaziCalculator.dayPillarForDate(d), label, dayun) to 100
        }
        parts += EasternPeriodReader.read(chart, BaziCalculator.monthPillarForDate(monday), "本周所值流月", dayun) to 60
        parts += EasternPeriodReader.read(chart, BaziCalculator.yearPillarForDate(monday), "本周所值流年", dayun) to 40
        return EasternPeriodReader.merge(parts, "本周")
    }

    /** 月：流月柱为主，流年为底。 */
    private fun readMonth(chart: BaziChart, date: LocalDate, dayun: Pillar?): EasternReading {
        val parts = listOf(
            EasternPeriodReader.read(chart, BaziCalculator.monthPillarForDate(date), "本月", dayun) to 100,
            EasternPeriodReader.read(chart, BaziCalculator.yearPillarForDate(date), "本年大势", dayun) to 50
        )
        return EasternPeriodReader.merge(parts, "本月")
    }

    /** 年：流年柱按太岁立论为主，当下所值流月为近景。 */
    private fun readYear(chart: BaziChart, date: LocalDate, dayun: Pillar?): EasternReading {
        val parts = listOf(
            EasternPeriodReader.read(
                chart, BaziCalculator.yearPillarForDate(date), "本年", dayun, taiSui = true
            ) to 100,
            EasternPeriodReader.read(chart, BaziCalculator.monthPillarForDate(date), "当下流月", dayun) to 20
        )
        return EasternPeriodReader.merge(parts, "本年")
    }

    /** 固定中文称谓，避免设备 locale 改变解盘语气 */
    private val WEEK_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    // ==================================================== 汇总为运势对象

    private fun assemble(
        chart: BaziChart,
        date: LocalDate,
        periodKey: String,
        reading: EasternReading
    ): EasternDailyFortune {
        val overall = (50 + reading.overallDelta).coerceIn(12, 97)
        fun score(delta: Int): Int = (overall + delta).coerceIn(6, 99)

        val lucky = reading.luckyElement
        val notes = reading.notes
        val dims = DIM_LABELS.entries.map { (key, label) ->
            val delta = when (key) {
                "career" -> reading.careerDelta
                "wealth" -> reading.wealthDelta
                "love" -> reading.loveDelta
                "study" -> reading.studyDelta
                else -> reading.healthDelta
            }
            FortuneDimension(
                key = key,
                label = label,
                score = score(delta),
                interpretation = interpret(key, label, score(delta), notes[key], reading)
            )
        }

        return EasternDailyFortune(
            dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            overallScore = overall,
            careerScore = score(reading.careerDelta),
            wealthScore = score(reading.wealthDelta),
            loveScore = score(reading.loveDelta),
            healthScore = score(reading.healthDelta),
            summary = reading.summary,
            advice = reading.advice,
            dayPillarText = BaziCalculator.dayPillarForDate(date).display,
            favorableToday = chart.favorableElements,
            luckyColor = elementColor(lucky),
            luckyDirection = elementDirection(lucky),
            period = periodKey,
            periodPillarText = reading.pillarText,
            insights = reading.insights,
            dimensionNotes = dims,
            dimensionBasis = notes
        )
    }

    /**
     * 维度解说：先给这一维的实际得分差，再列命局依据，最后一句怎么办。
     * 全部取自信号，不套模板。
     */
    private fun interpret(
        key: String,
        label: String,
        score: Int,
        rawNotes: List<String>?,
        reading: EasternReading
    ): String {
        val basis = (rawNotes ?: emptyList()).distinct().take(2)
        val trend = when {
            score >= 82 -> "${label}在这段周期里明显得力"
            score >= 68 -> "${label}偏顺，推一步动一步"
            score >= 52 -> "${label}持平，全凭日常功夫"
            score >= 38 -> "${label}受阻，宜减不宜加"
            else -> "${label}处在低谷，先守住别丢东西"
        }
        val text = if (basis.isEmpty()) {
            "$trend。这段周期${periodWord(reading)}在${label}上没有引发特别的刑冲合会，按常规节奏推进即可。"
        } else {
            "$trend。依据是：${basis.joinToString("；")}。"
        }
        return text + when (key) {
            "career" -> if (score >= 68) "该争的位置这周期去争，晚了就得再等一轮。" else "这周期少开口多落纸，把口径留痕比表现自己有用。"
            "wealth" -> if (score >= 68) "进财的路子会自己冒出来，把该要的钱当面要清。" else "不宜垫资、不宜借出、不宜在这段周期里做大额决策。"
            "love" -> if (score >= 68) "关系里的气氛是软的，想说的话挑这周期说。" else "话到嘴边减三分，别把情绪的定量说成结论。"
            "study" -> if (score >= 68) "记东西、出文字的效率高于平时，把最难的放前面。" else "读不进去就别硬读，换成动手的事反而有收获。"
            else -> if (score >= 68) "精神头足，可以承担更大工作量。" else "作息与饮食先管住，旧毛病这周期容易回头。"
        }
    }

    private fun periodWord(reading: EasternReading): String =
        if (reading.pillarText.isBlank()) "干支" else "「${reading.pillarText}」"
}
