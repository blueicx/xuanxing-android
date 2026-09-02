package com.xuanji.app.ui.components

import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.ZodiacCalculator
import com.xuanji.app.domain.elementName

data class ShareCard(
    val eyebrow: String,
    val title: String,
    val badge: String = "",
    val headline: String = "",
    val headlineUnit: String = "",
    val summary: String,
    val rows: List<Pair<String, String>> = emptyList(),
    val accent: Int = 0xFFB69CFF.toInt()
)

object ResultShareCards {
    fun composite(
        cardId: String,
        period: String,
        fortune: CompositeDailyFortune
    ): ShareCard {
        val prefix = periodLabel(period)
        return when (cardId) {
            "luck" -> ShareCard(
                "玄星 · $prefix", "幸运信息",
                badge = "${fortune.luckyNumber}",
                headline = fortune.luckyColor,
                headlineUnit = "幸运色",
                summary = "按当日干支与星座关系推算的轻量行动提示。",
                rows = listOf("数字" to "${fortune.luckyNumber}", "方位" to fortune.luckyDirection)
            )
            "caution" -> ShareCard(
                "玄星 · $prefix", "注意事项",
                summary = fortune.cautions.ifBlank { "当前没有特别提醒。" }
            )
            else -> {
                val dimension = fortune.dimensions.firstOrNull { "dim-${it.key}" == cardId }
                if (dimension != null) {
                    ShareCard(
                        "玄星 · $prefix", dimension.label,
                        badge = "${dimension.score}分",
                        headline = "${dimension.score}",
                        headlineUnit = "分",
                        summary = dimension.interpretation
                    )
                } else {
                    ShareCard(
                        "玄星 · $prefix", "综合总分",
                        badge = "${fortune.overallScore}分",
                        headline = "${fortune.overallScore}",
                        headlineUnit = "分",
                        summary = fortune.dimensions.firstOrNull()?.interpretation.orEmpty(),
                        rows = listOf(
                            "幸运" to listOf("数字${fortune.luckyNumber}", fortune.luckyColor, fortune.luckyDirection)
                                .joinToString(" · "),
                            "注意" to fortune.cautions
                        )
                    )
                }
            }
        }
    }

    fun eastern(
        cardId: String,
        period: String,
        full: BaziFull,
        fortune: EasternDailyFortune,
        hourText: String? = null
    ): ShareCard {
        val prefix = periodLabel(period)
        val chart = full.chart
        val pillars = listOf(chart.yearPillar.display, chart.monthPillar.display, chart.dayPillar.display, chart.hourPillar.display)
        return when (cardId) {
            "hours" -> ShareCard(
                "玄星 · $prefix", "今日吉时",
                headline = hourText ?: full.daYun.firstOrNull()?.pillar?.display.orEmpty(),
                summary = "按五鼠遁与日主喜忌推算，选择更顺的时段安排重要事。",
                rows = listOf("日主" to "${chart.dayMaster.chinese}（${elementName(chart.dayMasterElement)}）")
            )
            "pillars" -> ShareCard(
                "玄星 · $prefix", "八字命盘",
                headline = pillars.joinToString(" "),
                summary = "日主${chart.dayMaster.chinese}（${elementName(chart.dayMasterElement)}），生肖${chart.zodiac}。",
                rows = listOf("年月" to "${pillars[0]} ${pillars[1]}", "日时" to "${pillars[2]} ${pillars[3]}")
            )
            "conclusion" -> ShareCard(
                "玄星 · $prefix", "综合结论",
                summary = full.conclusion.summary,
                rows = full.conclusion.items.take(3).map { it.title to it.headline }
            )
            "geju" -> ShareCard(
                "玄星 · $prefix", "生辰格局",
                badge = full.geju.category,
                headline = full.geju.name,
                summary = full.geju.desc
            )
            "elements" -> ShareCard(
                "玄星 · $prefix", "五行占比",
                summary = "木火土金水的分布反映命局能量结构。",
                rows = full.chart.elementCounts.entries.take(5).map { elementName(it.key) to "${it.value}" }
            )
            "ten-gods" -> ShareCard(
                "玄星 · $prefix", "十神格局",
                summary = "以日干为「我」，推演其余干支与日主的关系。",
                rows = full.tenGods.take(4).map { "${it.position} ${it.stem.chinese}" to it.tenGod.chinese }
            )
            "strength" -> ShareCard(
                "玄星 · $prefix", "日主旺衰",
                badge = full.strength.level,
                headline = "${full.strength.score}",
                headlineUnit = "评分",
                summary = full.strength.desc
            )
            "yongji" -> ShareCard(
                "玄星 · $prefix", "用神忌神",
                headline = full.yongJi.useful.joinToString("、") { elementName(it) },
                headlineUnit = "用神",
                summary = full.yongJi.desc,
                rows = listOf("忌神" to full.yongJi.avoidance.joinToString("、") { elementName(it) })
            )
            "dayun" -> {
                val first = full.daYun.firstOrNull()
                ShareCard(
                    "玄星 · $prefix", "大运",
                    badge = first?.let { "${it.startAge}-${it.endAge}岁" }.orEmpty(),
                    headline = first?.pillar?.display ?: "未排",
                    summary = first?.desc ?: "按出生时辰与节气顺逆排出的行运序列。"
                )
            }
            "relations" -> ShareCard(
                "玄星 · $prefix", "刑冲合害",
                summary = full.relations.joinToString("；") { "${it.type}：${it.desc}" }.ifBlank { "未见明显关系。" },
                rows = full.relations.take(3).map { it.type to it.desc }
            )
            "shensha" -> ShareCard(
                "玄星 · $prefix", "神煞",
                summary = full.shenSha.joinToString("、") { it.name }.ifBlank { "命局未见明显神煞。" },
                rows = full.shenSha.take(3).map { it.name to it.desc }
            )
            else -> ShareCard(
                "玄星 · 东方$prefix", "周期运势",
                badge = "${fortune.overallScore}分",
                headline = "${fortune.overallScore}",
                headlineUnit = "分",
                summary = fortune.summary,
                rows = listOf(
                    "四类" to "事业${fortune.careerScore} · 财运${fortune.wealthScore} · 感情${fortune.loveScore} · 健康${fortune.healthScore}",
                    "提醒" to fortune.advice
                )
            )
        }
    }

    fun western(
        cardId: String,
        period: String,
        fortune: WesternDailyFortune,
        detail: ZodiacCalculator.WesternDetail,
        chart: ZodiacCalculator.NatalChart,
        interp: ZodiacCalculator.ChartInterpretation
    ): ShareCard {
        val prefix = periodLabel(period)
        return when (cardId) {
            "wheel", "natal" -> ShareCard(
                "玄星 · $prefix", if (cardId == "wheel") "圆盘星盘" else "本命星盘",
                headline = detail.sun.sign,
                headlineUnit = "太阳星座",
                summary = detail.sun.trait,
                rows = listOf("上升" to detail.rising.sign, "月亮" to detail.moon.sign)
            )
            "axes" -> ShareCard(
                "玄星 · $prefix", "四轴解析",
                summary = interp.axes.joinToString(" · ") { "${it.key} ${it.sign}" },
                rows = interp.axes.take(2).map { it.key to "${it.name} ${it.degree}°" }
            )
            "planets" -> ShareCard(
                "玄星 · $prefix", "十大行星",
                summary = chart.planets.joinToString(" · ") { "${it.name}${it.sign}" },
                rows = chart.planets.take(3).map { it.name to "${it.sign} 第${it.house}宫" }
            )
            "planet-meaning" -> ShareCard(
                "玄星 · $prefix", "行星含义",
                summary = interp.planets.take(3).joinToString("；") { "${it.name}${it.sign}${it.text}" },
                rows = interp.planets.take(2).map { it.name to "${it.sign} 第${it.house}宫" }
            )
            "aspects" -> ShareCard(
                "玄星 · $prefix", "主要相位",
                summary = chart.aspects.take(4).joinToString("；") { "${it.p1} ${it.type} ${it.p2}" }
                    .ifBlank { "未检测到主要相位。" }
            )
            "aspect-meaning" -> ShareCard(
                "玄星 · $prefix", "相位解读",
                summary = interp.aspects.take(3).joinToString("；") { "${it.p1} ${it.type} ${it.p2}：${it.text}" }
                    .ifBlank { "未检测到主要相位。" }
            )
            "conclusion" -> {
                val conclusion = ZodiacCalculator.computeConclusion(detail, chart)
                ShareCard(
                    "玄星 · $prefix", "综合解读",
                    summary = conclusion.summary,
                    rows = conclusion.items.take(3).map { it.title to it.headline }
                )
            }
            else -> ShareCard(
                "玄星 · 星座$prefix", "周期运势",
                badge = "${fortune.overallScore}分",
                headline = "${fortune.overallScore}",
                headlineUnit = "分",
                summary = fortune.summary,
                rows = listOf("幸运" to "数字${fortune.luckyNumber} · ${fortune.luckyColor} · ${fortune.luckyDirection}")
            )
        }
    }

    private fun periodLabel(period: String): String = when (period) {
        "week" -> "本周"
        "month" -> "本月"
        "year" -> "本年"
        else -> "今日"
    }
}
