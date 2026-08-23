package com.xuanji.app.domain

import com.xuanji.app.data.model.Branch
import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.Pillar
import com.xuanji.app.data.model.Stem
import java.time.LocalDate
import java.time.LocalTime

data class HourGuide(
    val pillar: Pillar,
    val timeText: String,
    val score: Int,
    val level: String,
    val relationSummary: String,
    val advice: String,
    val isCurrent: Boolean = false
)

/** 十二时辰择吉：五鼠遁定时干，再按日主五行生克与喜忌排序。 */
object HourGuideGenerator {
    fun generate(chart: BaziChart, date: LocalDate): List<HourGuide> {
        val dayPillar = BaziCalculator.dayPillarForDate(date)
        val ziStem = when (dayPillar.stem) {
            Stem.甲, Stem.己 -> Stem.甲
            Stem.乙, Stem.庚 -> Stem.丙
            Stem.丙, Stem.辛 -> Stem.戊
            Stem.丁, Stem.壬 -> Stem.庚
            Stem.戊, Stem.癸 -> Stem.壬
        }
        val currentZhi = currentBranchIndex()

        return Branch.values().mapIndexed { index, branch ->
            val stem = Stem.values()[(ziStem.ordinal + index) % 10]
            val pillar = Pillar(stem, branch)
            val stemRel = relationScore(chart.dayMasterElement, stem.element)
            val branchRel = relationScore(chart.dayMasterElement, branch.element)
            val periodElements = setOf(stem.element, branch.element)
            var raw = 50 + stemRel + branchRel
            raw += chart.favorableElements.count { it in periodElements } * 4
            raw += chart.unfavorableElements.count { it in periodElements } * -3

            val score = raw.coerceIn(20, 96)
            HourGuide(
                pillar = pillar,
                timeText = timeRange(index),
                score = score,
                level = when {
                    score >= 70 -> "大吉"
                    score >= 55 -> "吉"
                    score >= 40 -> "平"
                    else -> "宜慎"
                },
                relationSummary = "${BaziCalculator.tenGod(chart.dayMaster, stem).chinese} · ${elementName(branch.element)}",
                advice = adviceFor(score),
                isCurrent = index == currentZhi
            )
        }.sortedWith(
            compareByDescending<HourGuide> { it.score }
                .thenBy { Branch.values().indexOf(it.pillar.branch) }
        )
    }

    private fun relationScore(me: Element, other: Element): Int = when {
        me == other -> 8
        produces(other, me) -> 12
        produces(me, other) -> 2
        controls(me, other) -> 5
        controls(other, me) -> -10
        else -> 0
    }

    private fun adviceFor(score: Int) = when {
        score >= 70 -> "气场相扶，宜安排关键行动。"
        score >= 55 -> "能量顺畅，适合推进计划。"
        score >= 40 -> "平稳过渡，按节奏做事即可。"
        else -> "与日主相制，宜避重就轻。"
    }

    private fun timeRange(index: Int): String = if (index == 0) {
        "23:00-00:59"
    } else {
        val start = index * 2 - 1
        "%02d:00-%02d:59".format(start, start + 2)
    }

    private fun currentBranchIndex(): Int {
        val hour = LocalTime.now().hour
        return if (hour == 23 || hour < 1) 0 else ((hour + 1) / 2) % 12
    }
}
