package com.xuanji.app.domain

import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.BaziConclusion
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.ConclusionItem
import com.xuanji.app.data.model.Branch
import com.xuanji.app.data.model.BranchRelation
import com.xuanji.app.data.model.DaYun
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.Geju
import com.xuanji.app.data.model.Pillar
import com.xuanji.app.data.model.Stem
import com.xuanji.app.data.model.StrengthResult
import com.xuanji.app.data.model.TenGod
import com.xuanji.app.data.model.TenGodItem
import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.data.model.YongJi
import com.xuanji.app.data.model.ShenShaItem
import com.xuanji.app.data.model.ShenShaMeta
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * 八字推算与分析。
 * 排盘（四柱）→ 定日主 → 分十神 → 看月令判旺衰 → 取用神忌神 → 排大运流年 → 看刑冲合害断应期。
 *
 * 节气时刻基于 20/21 世纪经验表，误差通常在 ±1 天（世纪边界附近 ±2 天），
 * 对命理展示已足够；日柱为精确值。
 */
object BaziCalculator {

    // 二十四节气经验偏移表（自「小寒」起算的分钟数）
    private val ST20 = intArrayOf(
        0, 19168, 42337, 64704, 87090, 109369,
        131767, 154165, 176583, 198928, 221221, 243487,
        265713, 287902, 310093, 332157, 354195, 376145,
        398078, 419901, 441696, 463453, 485193, 506863
    )
    private val ST21 = intArrayOf(
        0, 21208, 42467, 63836, 85337, 107014,
        128867, 150921, 173149, 195551, 218072, 240693,
        263343, 285989, 308563, 331033, 353350, 375494,
        397447, 419210, 440795, 462224, 483532, 504758
    )

    // 十二「节」对应的月支：小寒->丑, 立春->寅, 惊蛰->卯, 清明->辰, 立夏->巳,
    // 芒种->午, 小暑->未, 立秋->申, 白露->酉, 寒露->戌, 立冬->亥, 大雪->子
    private val TERM_BRANCH = mapOf(
        0 to Branch.丑, 2 to Branch.寅, 4 to Branch.卯, 6 to Branch.辰,
        8 to Branch.巳, 10 to Branch.午, 12 to Branch.未, 14 to Branch.申,
        16 to Branch.酉, 18 to Branch.戌, 20 to Branch.亥, 22 to Branch.子
    )

    // —————————————————————————— 排盘 ——————————————————————————

    fun calculate(profile: UserProfile): BaziChart {
        val birth = LocalDateTime.of(
            profile.birthYear, profile.birthMonth, profile.birthDay,
            profile.birthHour, profile.birthMinute
        )

        val yearPillar = calcYearPillar(birth)
        val monthPillar = calcMonthPillar(birth, yearPillar.stem)
        val dayPillar = calcDayPillar(birth)
        val hourPillar = calcHourPillar(birth, dayPillar.stem)

        val dayMaster = dayPillar.stem
        val zodiac = yearPillar.branch.zodiac

        val counts = mutableMapOf(
            Element.WOOD to 0, Element.FIRE to 0, Element.EARTH to 0,
            Element.METAL to 0, Element.WATER to 0
        )
        listOf(yearPillar, monthPillar, dayPillar, hourPillar).forEach { p ->
            counts[p.stem.element] = counts.getValue(p.stem.element) + 1
            counts[p.branch.element] = counts.getValue(p.branch.element) + 1
        }

        val (favorable, unfavorable) = computeFavorable(dayMaster, counts)

        return BaziChart(
            yearPillar = yearPillar,
            monthPillar = monthPillar,
            dayPillar = dayPillar,
            hourPillar = hourPillar,
            dayMaster = dayMaster,
            zodiac = zodiac,
            elementCounts = counts,
            favorableElements = favorable,
            unfavorableElements = unfavorable
        )
    }

    /** 完整分析 */
    fun analyze(profile: UserProfile): BaziFull {
        val chart = calculate(profile)
        val dm = chart.dayMaster

        // 分十神：四柱天干 + 地支藏干
        val tenGods = mutableListOf<TenGodItem>()
        tenGods.add(TenGodItem("年", "天干", chart.yearPillar.stem, tenGod(dm, chart.yearPillar.stem)))
        tenGods.add(TenGodItem("月", "天干", chart.monthPillar.stem, tenGod(dm, chart.monthPillar.stem)))
        tenGods.add(TenGodItem("日", "日主", chart.dayPillar.stem, TenGod.比肩))
        tenGods.add(TenGodItem("时", "天干", chart.hourPillar.stem, tenGod(dm, chart.hourPillar.stem)))
        val branchPairs = listOf(
            "年" to chart.yearPillar.branch,
            "月" to chart.monthPillar.branch,
            "日" to chart.dayPillar.branch,
            "时" to chart.hourPillar.branch
        )
        branchPairs.forEach { (label, b) ->
            b.hidden.forEachIndexed { i, s ->
                val pos = if (i == 0) "本气" else if (i == 1) "中气" else "余气"
                tenGods.add(TenGodItem(label, pos, s, tenGod(dm, s)))
            }
        }

        // 月令旺衰
        val strength = computeStrength(chart, dm)
        // 用神忌神
        val yongJi = computeYongJi(dm, strength.level)
        // 生辰格局（月令取格）
        val geju = computeGeju(chart, dm, strength)
        // 神煞
        val shenSha = computeShenSha(chart, dm)

        // 大运流年
        val genderMale = profile.gender != "女"
        val daYun = computeDaYun(chart, profile, genderMale)

        val thisYear = LocalDate.now().year
        val currentYearPillar = calcYearPillar(LocalDateTime.of(thisYear, 6, 15, 12, 0))
        val futureYears = listOf(
            "明年" to calcYearPillar(LocalDateTime.of(thisYear + 1, 6, 15, 12, 0)),
            "后年" to calcYearPillar(LocalDateTime.of(thisYear + 2, 6, 15, 12, 0)),
            "大后年" to calcYearPillar(LocalDateTime.of(thisYear + 3, 6, 15, 12, 0))
        )

        // 刑冲合害
        val relations = detectRelations(
            listOf(chart.yearPillar.branch, chart.monthPillar.branch, chart.dayPillar.branch, chart.hourPillar.branch)
        )

        // 综合结论（汇总日主 / 旺衰 / 用忌 / 格局 / 十神 / 神煞 / 刑冲）
        val conclusion = computeConclusion(chart, dm, strength, yongJi, geju, tenGods, shenSha, relations)

        val note = "命盘仅供文化娱乐参考。大运起运以出生至最近「节」的日数 ÷3 推算（三日为一岁）。"
        return BaziFull(
            chart, tenGods, strength, yongJi, daYun, currentYearPillar, futureYears,
            relations, geju, shenSha, conclusion, note
        )
    }

    /** 给定日期的日柱（供每日运势使用） */
    fun dayPillarForDate(date: LocalDate): Pillar {
        val jdn = julianDayNumber(date.year, date.monthValue, date.dayOfMonth)
        val idx = ((((jdn + 49) % 60) + 60) % 60).toInt()
        return pillarFromIndex(idx)
    }

    /** 给定日期的日干支序数（0 = 甲子），供需与八字共用日柱口径的模块调用。 */
    fun dayPillarIndexForDate(date: LocalDate): Int {
        val pillar = dayPillarForDate(date)
        return pillarToIndex(pillar)
    }

    /** 给定日期的流年柱（立春为界，供年度运势使用） */
    fun yearPillarForDate(date: LocalDate): Pillar =
        calcYearPillar(LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, 12, 0))

    /**
     * 给定日期的流月柱：以十二「节」为界定月支，再由年干五虎遁推月干。
     * 与排盘走同一套节气表，故流月干支与本命月柱口径一致。
     */
    fun monthPillarForDate(date: LocalDate): Pillar {
        val noon = LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, 12, 0)
        return calcMonthPillar(noon, calcYearPillar(noon).stem)
    }

    // --- 年柱（立春为界） ---
    private fun calcYearPillar(birth: LocalDateTime): Pillar {
        val lichun = getTerm(birth.year, 2) // 立春
        val solarYear = if (!birth.isBefore(lichun)) birth.year else birth.year - 1
        val stemIdx = (((solarYear - 4) % 10) + 10) % 10
        val branchIdx = (((solarYear - 4) % 12) + 12) % 12
        return Pillar(Stem.values()[stemIdx], Branch.values()[branchIdx])
    }

    // --- 月柱（十二节为界） ---
    private fun calcMonthPillar(birth: LocalDateTime, yearStem: Stem): Pillar {
        val candidates = mutableListOf<Pair<LocalDateTime, Branch>>()
        for (y in (birth.year - 1)..(birth.year + 1)) {
            TERM_BRANCH.forEach { (termIdx, branch) ->
                candidates.add(getTerm(y, termIdx) to branch)
            }
        }
        val branch = candidates
            .filter { !birth.isBefore(it.first) }
            .maxByOrNull { it.first }
            ?.second ?: Branch.子

        val firstMonthStem = firstYinMonthStem(yearStem) // 寅月天干
        val orderFromYin = branchOrderFromYin(branch)
        val stemIdx = (((firstMonthStem.ordinal + orderFromYin) % 10) + 10) % 10
        return Pillar(Stem.values()[stemIdx], branch)
    }

    // 五虎遁：年干 -> 寅月天干
    private fun firstYinMonthStem(yearStem: Stem): Stem = when (yearStem) {
        Stem.甲, Stem.己 -> Stem.丙
        Stem.乙, Stem.庚 -> Stem.戊
        Stem.丙, Stem.辛 -> Stem.庚
        Stem.丁, Stem.壬 -> Stem.壬
        Stem.戊, Stem.癸 -> Stem.甲
    }

    // 地支在「寅..丑」顺序中的位置
    private fun branchOrderFromYin(branch: Branch): Int {
        val order = listOf(
            Branch.寅, Branch.卯, Branch.辰, Branch.巳, Branch.午, Branch.未,
            Branch.申, Branch.酉, Branch.戌, Branch.亥, Branch.子, Branch.丑
        )
        return order.indexOf(branch)
    }

    // --- 日柱（精确儒略日；采用子初换日：23:00 起归入次日） ---
    private fun calcDayPillar(birth: LocalDateTime): Pillar {
        val civilDate = LocalDate.of(birth.year, birth.month, birth.dayOfMonth)
        val baziDate = if (birth.hour == 23) civilDate.plusDays(1) else civilDate
        return dayPillarForDate(baziDate)
    }

    // --- 时柱 ---
    private fun calcHourPillar(birth: LocalDateTime, dayStem: Stem): Pillar {
        val hour = birth.hour
        val branchIdx = ((hour + 1) / 2) % 12
        val branch = Branch.values()[branchIdx]
        // 五鼠遁：日干 -> 子时天干
        val ziStem = when (dayStem) {
            Stem.甲, Stem.己 -> Stem.甲
            Stem.乙, Stem.庚 -> Stem.丙
            Stem.丙, Stem.辛 -> Stem.戊
            Stem.丁, Stem.壬 -> Stem.庚
            Stem.戊, Stem.癸 -> Stem.壬
        }
        val stemIdx = (((ziStem.ordinal + branchIdx) % 10) + 10) % 10
        return Pillar(Stem.values()[stemIdx], branch)
    }

    // 0 = 甲子
    private fun pillarFromIndex(idx: Int): Pillar =
        Pillar(Stem.values()[idx % 10], Branch.values()[idx % 12])

    private fun pillarToIndex(p: Pillar): Int {
        for (i in 0..59) {
            if (Stem.values()[i % 10] == p.stem && Branch.values()[i % 12] == p.branch) return i
        }
        return 0
    }

    private fun julianDayNumber(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return (
            day +
                (153 * m + 2) / 5 +
                365L * y +
                y / 4 -
                y / 100 +
                y / 400 -
                32045
            ).toLong()
    }

    // 计算某年第 n 个节气（0=小寒 ... 23=冬至）的 LocalDateTime（近似北京时间）
    private fun getTerm(year: Int, n: Int): LocalDateTime {
        val table = if (year >= 2000) ST21 else ST20
        val baseYear = if (year >= 2000) 2000 else 1900
        val base = LocalDateTime.of(baseYear, 1, 6, 2, 5) // 小寒基准时刻
        val minutes = table[n] + ((year - baseYear) * 525948.76).toLong()
        return base.plusMinutes(minutes)
    }

    // —————————————————————————— 十神 ——————————————————————————

    /** 以日主为参照，求 other 天干对应的十神 */
    fun tenGod(dm: Stem, other: Stem): TenGod {
        val sameElement = dm.element == other.element
        val samePolarity = dm.isYang == other.isYang
        return when {
            sameElement && samePolarity -> TenGod.比肩
            sameElement && !samePolarity -> TenGod.劫财
            produces(other.element, dm.element) && samePolarity -> TenGod.偏印
            produces(other.element, dm.element) && !samePolarity -> TenGod.正印
            produces(dm.element, other.element) && samePolarity -> TenGod.食神
            produces(dm.element, other.element) && !samePolarity -> TenGod.伤官
            controls(dm.element, other.element) && samePolarity -> TenGod.偏财
            controls(dm.element, other.element) && !samePolarity -> TenGod.正财
            controls(other.element, dm.element) && samePolarity -> TenGod.七杀
            controls(other.element, dm.element) && !samePolarity -> TenGod.正官
            else -> TenGod.比肩
        }
    }

    // —————————————————————————— 旺衰 ——————————————————————————

    private fun computeStrength(chart: BaziChart, dm: Stem): StrengthResult {
        val branches = listOf(chart.yearPillar.branch, chart.monthPillar.branch, chart.dayPillar.branch, chart.hourPillar.branch)
        val stems = listOf(chart.yearPillar.stem, chart.monthPillar.stem, chart.dayPillar.stem, chart.hourPillar.stem)

        var score = 0.0
        score += branchWeight(branches[1], dm) * 4.0   // 月令权重最高
        for (i in listOf(0, 2, 3)) score += branchWeight(branches[i], dm) * 1.5
        for (i in 0..3) if (i != 2) score += stemWeight(stems[i], dm) * 1.0 // 日主自身不计

        val rounded = score.roundToInt()
        val level = when {
            score >= 6 -> "身强"
            score <= 1 -> "身弱"
            else -> "中和"
        }
        val desc = when (level) {
            "身强" -> "日主得令、得地或得势，气势偏旺。宜用克、泄、耗来平衡命局。"
            "身弱" -> "日主失令、失地或失势，气势偏弱。宜用生、扶来助身任财官。"
            else -> "日主中和，不偏不倚，顺势而为即可，吉凶随运而转。"
        }
        return StrengthResult(level, rounded, desc)
    }

    private fun branchWeight(b: Branch, dm: Stem): Double = elementWeight(b.element, dm)
    private fun stemWeight(s: Stem, dm: Stem): Double = elementWeight(s.element, dm)

    private fun elementWeight(e: Element, dm: Stem): Double = when {
        e == dm.element -> 1.0                  // 同我
        produces(e, dm.element) -> 0.8          // 生我
        produces(dm.element, e) -> -0.5         // 我生
        controls(dm.element, e) -> -0.3         // 我克
        controls(e, dm.element) -> -0.8         // 克我
        else -> 0.0
    }

    // —————————————————————————— 用神忌神 ——————————————————————————

    private fun computeYongJi(dm: Stem, level: String): YongJi {
        val supports = mutableListOf<Element>() // 生我 + 同我
        val drains = mutableListOf<Element>()   // 我生 + 我克 + 克我
        Element.values().forEach { e ->
            if (e == dm.element || produces(e, dm.element)) supports.add(e)
            else drains.add(e)
        }
        return when (level) {
            "身强" -> YongJi(
                drains, supports,
                "日主强，喜克、泄、耗（财、官杀、食伤）为用神；忌印比（生扶）过旺。"
            )
            "身弱" -> YongJi(
                supports, drains,
                "日主弱，喜生、扶（印、比劫）为用神；忌财、官杀、食伤耗身。"
            )
            else -> YongJi(
                supports, drains,
                "日主中和，用神随岁运流转；宜扶抑并用，不可偏颇。"
            )
        }
    }

    // 喜用神：日主强则用「克泄耗」，弱则用「生扶」（供每日运势兼容）
    private fun computeFavorable(
        dayMaster: Stem,
        counts: Map<Element, Int>
    ): Pair<List<Element>, List<Element>> {
        val dmElem = dayMaster.element
        val sameCount = counts.getValue(dmElem)
        val strong = sameCount >= 3

        val supports = mutableListOf<Element>()
        val opposes = mutableListOf<Element>()
        Element.values().forEach { e ->
            if (e == dmElem || produces(e, dmElem)) supports.add(e)
            else opposes.add(e)
        }
        return if (strong) opposes.toList() to supports.toList()
        else supports.toList() to opposes.toList()
    }

    // —————————————————————————— 大运流年 ——————————————————————————

    private fun computeDaYun(chart: BaziChart, profile: UserProfile, genderMale: Boolean): List<DaYun> {
        val birth = LocalDateTime.of(
            profile.birthYear, profile.birthMonth, profile.birthDay,
            profile.birthHour, profile.birthMinute
        )
        val yStem = chart.yearPillar.stem
        // 阳男阴女顺排；阴男阳女逆排
        val forward = (yStem.isYang == genderMale)

        val (_, days) = if (forward) nextTerm(birth) else prevTerm(birth)
        val startAge = days / 3.0

        val mIdx = pillarToIndex(chart.monthPillar)
        val dm = chart.dayMaster
        val list = mutableListOf<DaYun>()
        for (i in 0..7) {
            val idx = if (forward) (mIdx + (i + 1)) % 60 else ((mIdx - (i + 1)) % 60 + 60) % 60
            val pillar = pillarFromIndex(idx)
            val sa = (startAge + i * 10).roundToInt()
            val tg = tenGod(dm, pillar.stem)
            val desc = "大运「${pillar.display}」，天干十神为${tg.chinese}（${tg.category}）。"
            list.add(DaYun(i + 1, sa, sa + 9, pillar, desc))
        }
        return list
    }

    /** 出生后的下一个「节」 */
    private fun nextTerm(birth: LocalDateTime): Pair<LocalDateTime, Double> {
        var best: LocalDateTime? = null
        for (y in (birth.year - 1)..(birth.year + 1)) {
            for (n in 0..22 step 2) {
                val t = getTerm(y, n)
                if (!t.isBefore(birth) && (best == null || t.isBefore(best))) best = t
            }
        }
        val dt = best ?: getTerm(birth.year, 2)
        val days = ChronoUnit.MINUTES.between(birth, dt) / 1440.0
        return dt to days
    }

    /** 出生前的上一个「节」 */
    private fun prevTerm(birth: LocalDateTime): Pair<LocalDateTime, Double> {
        var best: LocalDateTime? = null
        for (y in (birth.year - 1)..(birth.year + 1)) {
            for (n in 0..22 step 2) {
                val t = getTerm(y, n)
                if (t.isBefore(birth) && (best == null || t.isAfter(best))) best = t
            }
        }
        val dt = best ?: getTerm(birth.year - 1, 22)
        val days = ChronoUnit.MINUTES.between(dt, birth) / 1440.0
        return dt to days
    }

    // —————————————————————————— 刑冲合害 ——————————————————————————

    private fun detectRelations(bs: List<Branch>): List<BranchRelation> {
        val set = bs.toSet()
        val result = mutableListOf<BranchRelation>()

        val sixHe = listOf(
            setOf(Branch.子, Branch.丑), setOf(Branch.寅, Branch.亥),
            setOf(Branch.卯, Branch.戌), setOf(Branch.辰, Branch.酉),
            setOf(Branch.巳, Branch.申), setOf(Branch.午, Branch.未)
        )
        val sixChong = listOf(
            setOf(Branch.子, Branch.午), setOf(Branch.丑, Branch.未),
            setOf(Branch.寅, Branch.申), setOf(Branch.卯, Branch.酉),
            setOf(Branch.辰, Branch.戌), setOf(Branch.巳, Branch.亥)
        )
        val liuHai = listOf(
            setOf(Branch.子, Branch.未), setOf(Branch.丑, Branch.午),
            setOf(Branch.寅, Branch.巳), setOf(Branch.卯, Branch.辰),
            setOf(Branch.申, Branch.亥), setOf(Branch.酉, Branch.戌)
        )
        val sanHe = listOf(
            setOf(Branch.申, Branch.子, Branch.辰), setOf(Branch.亥, Branch.卯, Branch.未),
            setOf(Branch.寅, Branch.午, Branch.戌), setOf(Branch.巳, Branch.酉, Branch.丑)
        )
        val sanHui = listOf(
            setOf(Branch.寅, Branch.卯, Branch.辰), setOf(Branch.巳, Branch.午, Branch.未),
            setOf(Branch.申, Branch.酉, Branch.戌), setOf(Branch.亥, Branch.子, Branch.丑)
        )
        // 三刑
        val sanXing = listOf(
            setOf(Branch.寅, Branch.巳, Branch.申),
            setOf(Branch.丑, Branch.戌, Branch.未),
            setOf(Branch.子, Branch.卯),
            setOf(Branch.辰, Branch.午, Branch.酉, Branch.亥)
        )

        sixHe.forEach { if (set.containsAll(it)) result.add(BranchRelation("六合", it.toList(), "地支${join(it)}相合，主和顺、人缘助益。")) }
        sixChong.forEach { if (set.containsAll(it)) result.add(BranchRelation("六冲", it.toList(), "地支${join(it)}相冲，主变动、冲克，需防起伏。")) }
        liuHai.forEach { if (set.containsAll(it)) result.add(BranchRelation("六害", it.toList(), "地支${join(it)}相害，主暗中不睦、小人。")) }
        sanHe.forEach { if (set.containsAll(it)) result.add(BranchRelation("三合", it.toList(), "地支${join(it)}三合，主成局得助、气场汇聚。")) }
        sanHui.forEach { if (set.containsAll(it)) result.add(BranchRelation("三会", it.toList(), "地支${join(it)}三会，主一方之气旺盛。")) }
        sanXing.forEach { if (set.containsAll(it)) result.add(BranchRelation("三刑", it.toList(), "地支${join(it)}相刑，主刑伤、纠葛，宜化解。")) }
        return result
    }

    // —————————————————————————— 生辰格局 ——————————————————————————

    private val LU_BRANCH = mapOf<Stem, Branch>(
        Stem.甲 to Branch.寅, Stem.乙 to Branch.卯, Stem.丙 to Branch.巳, Stem.丁 to Branch.午,
        Stem.戊 to Branch.巳, Stem.己 to Branch.午, Stem.庚 to Branch.申, Stem.辛 to Branch.酉,
        Stem.壬 to Branch.亥, Stem.癸 to Branch.子
    )
    private val YANG_REN_BRANCH = mapOf<Stem, Branch>(
        Stem.甲 to Branch.卯, Stem.乙 to Branch.辰, Stem.丙 to Branch.午, Stem.丁 to Branch.未,
        Stem.戊 to Branch.午, Stem.己 to Branch.未, Stem.庚 to Branch.酉, Stem.辛 to Branch.戌,
        Stem.壬 to Branch.子, Stem.癸 to Branch.丑
    )

    private fun computeGeju(chart: BaziChart, dm: Stem, strength: StrengthResult): Geju {
        val mb = chart.monthPillar.branch
        val mainQi = mb.hidden.first()
        val tg = tenGod(dm, mainQi)
        val note = "（日主${strength.level}）"
        return when (tg) {
            TenGod.正官 -> Geju("正官格", "吉格", "月令本气为正官，主品行端正、守法负责，宜行正道、掌权位。$note")
            TenGod.七杀 -> Geju("七杀格", "凶格转吉", "月令本气为七杀，主果敢决断、能担大任；性烈须制化方成大事。$note")
            TenGod.正印 -> Geju("正印格", "吉格", "月令本气为正印，主聪慧仁慈、得长辈荫庇，宜求学修养、积德进业。$note")
            TenGod.偏印 -> Geju("偏印格", "中格", "月令本气为偏印，主悟性高、善钻研冷门，宜术业专攻，然略显孤僻。$note")
            TenGod.食神 -> Geju("食神格", "吉格", "月令本气为食神，主心地宽厚、才艺通达，宜以才智生财、安享其成。$note")
            TenGod.伤官 -> Geju("伤官格", "中格", "月令本气为伤官，主聪明外露、不拘礼法，宜艺术创见，须防口舌是非。$note")
            TenGod.正财 -> Geju("正财格", "吉格", "月令本气为正财，主勤劳务实、稳积财富，宜踏实经营、细水长流。$note")
            TenGod.偏财 -> Geju("偏财格", "吉格", "月令本气为偏财，主慷慨善交、偏得外财，宜投资交际、广结善缘。$note")
            TenGod.比肩, TenGod.劫财 -> {
                val lu = LU_BRANCH[dm]
                val ren = YANG_REN_BRANCH[dm]
                when {
                    mb == lu -> Geju("建禄格", "特殊格", "月令为日主之禄，身旺得地、自主心强，宜白手兴家、自立门户。$note")
                    mb == ren -> Geju("月刃格", "特殊格", "月令为日主羊刃，性刚果敢、勇毅过人，然易逞强招凶，须有制化方安。$note")
                    else -> Geju("比劫格", "中格", "月令本气为比劫，主同辈朋助、自给自足，然财星易分，须防争竞耗财。$note")
                }
            }
        }
    }

    // —————————————————————————— 神煞 ——————————————————————————

    private val TIANYI = mapOf<Stem, List<Branch>>(
        Stem.甲 to listOf(Branch.丑, Branch.未), Stem.戊 to listOf(Branch.丑, Branch.未), Stem.庚 to listOf(Branch.丑, Branch.未),
        Stem.乙 to listOf(Branch.子, Branch.申), Stem.己 to listOf(Branch.子, Branch.申),
        Stem.丙 to listOf(Branch.亥, Branch.酉), Stem.丁 to listOf(Branch.亥, Branch.酉),
        Stem.壬 to listOf(Branch.卯, Branch.巳), Stem.癸 to listOf(Branch.卯, Branch.巳),
        Stem.辛 to listOf(Branch.寅, Branch.午)
    )
    private val TAIJI = mapOf<Stem, List<Branch>>(
        Stem.甲 to listOf(Branch.子, Branch.午), Stem.乙 to listOf(Branch.子, Branch.午),
        Stem.丙 to listOf(Branch.卯, Branch.酉), Stem.丁 to listOf(Branch.卯, Branch.酉),
        Stem.戊 to listOf(Branch.辰, Branch.戌, Branch.丑, Branch.未), Stem.己 to listOf(Branch.辰, Branch.戌, Branch.丑, Branch.未),
        Stem.庚 to listOf(Branch.寅, Branch.亥), Stem.辛 to listOf(Branch.寅, Branch.亥),
        Stem.壬 to listOf(Branch.巳, Branch.申), Stem.癸 to listOf(Branch.巳, Branch.申)
    )
    private val WENCHANG = mapOf<Stem, List<Branch>>(
        Stem.甲 to listOf(Branch.巳, Branch.午), Stem.乙 to listOf(Branch.巳, Branch.午),
        Stem.丙 to listOf(Branch.申), Stem.戊 to listOf(Branch.申),
        Stem.丁 to listOf(Branch.酉), Stem.己 to listOf(Branch.酉),
        Stem.庚 to listOf(Branch.亥), Stem.辛 to listOf(Branch.子),
        Stem.壬 to listOf(Branch.寅), Stem.癸 to listOf(Branch.卯)
    )

    private data class TriadRule(
        val name: String, val nature: String, val desc: String,
        val groups: List<Pair<List<Branch>, Branch>>
    )
    private val TRIAD_RULES = listOf(
        TriadRule("桃花（咸池）", "中",
            "桃花临命，主容貌气质佳、人缘风流、异性缘旺；过旺则易陷情欲纠葛。",
            listOf(listOf(Branch.寅, Branch.午, Branch.戌) to Branch.卯,
                listOf(Branch.申, Branch.子, Branch.辰) to Branch.酉,
                listOf(Branch.亥, Branch.卯, Branch.未) to Branch.子,
                listOf(Branch.巳, Branch.酉, Branch.丑) to Branch.午)),
        TriadRule("驿马", "中",
            "驿马星动，主走动迁徙、外出发展，利远方求财求学；静守则难展其能。",
            listOf(listOf(Branch.申, Branch.子, Branch.辰) to Branch.寅,
                listOf(Branch.寅, Branch.午, Branch.戌) to Branch.申,
                listOf(Branch.巳, Branch.酉, Branch.丑) to Branch.亥,
                listOf(Branch.亥, Branch.卯, Branch.未) to Branch.巳)),
        TriadRule("华盖", "中",
            "华盖临命，主聪颖孤高、喜玄学艺术，性带清冷；宜修心养性，孤芳自赏。",
            listOf(listOf(Branch.寅, Branch.午, Branch.戌) to Branch.戌,
                listOf(Branch.申, Branch.子, Branch.辰) to Branch.辰,
                listOf(Branch.巳, Branch.酉, Branch.丑) to Branch.丑,
                listOf(Branch.亥, Branch.卯, Branch.未) to Branch.未)),
        TriadRule("将星", "吉",
            "将星照命，主具领导统御之才，处事果决、能掌局面，宜任职带班。",
            listOf(listOf(Branch.寅, Branch.午, Branch.戌) to Branch.午,
                listOf(Branch.申, Branch.子, Branch.辰) to Branch.子,
                listOf(Branch.巳, Branch.酉, Branch.丑) to Branch.酉,
                listOf(Branch.亥, Branch.卯, Branch.未) to Branch.卯)),
        TriadRule("亡神", "凶",
            "亡神入命，主心思深沉、喜怒不形于色；过旺则易惹官非暗损，须防小人。",
            listOf(listOf(Branch.寅, Branch.午, Branch.戌) to Branch.巳,
                listOf(Branch.申, Branch.子, Branch.辰) to Branch.亥,
                listOf(Branch.巳, Branch.酉, Branch.丑) to Branch.申,
                listOf(Branch.亥, Branch.卯, Branch.未) to Branch.寅)),
        TriadRule("劫煞", "凶",
            "劫煞临身，主性急刚暴、易遭突发劫夺；须修平和、远争斗以避其凶。",
            listOf(listOf(Branch.寅, Branch.午, Branch.戌) to Branch.亥,
                listOf(Branch.申, Branch.子, Branch.辰) to Branch.巳,
                listOf(Branch.巳, Branch.酉, Branch.丑) to Branch.寅,
                listOf(Branch.亥, Branch.卯, Branch.未) to Branch.申))
    )

    private fun computeShenSha(chart: BaziChart, dm: Stem): List<ShenShaItem> {
        val branches = listOf(
            chart.yearPillar.branch, chart.monthPillar.branch,
            chart.dayPillar.branch, chart.hourPillar.branch
        )
        val dayB = chart.dayPillar.branch
        val yearB = chart.yearPillar.branch
        val out = mutableListOf<ShenShaItem>()

        TIANYI[dm]?.let { tgts ->
            branches.firstOrNull { it in tgts }?.let { b ->
                out.add(ShenShaItem("天乙贵人", "吉", b, "天乙贵人照命，出入得助、危难有救，主贵人扶持、人缘和善。"))
            }
        }
        TAIJI[dm]?.let { tgts ->
            branches.firstOrNull { it in tgts }?.let { b ->
                out.add(ShenShaItem("太极贵人", "吉", b, "太极贵人临身，悟性高超、好学尚德，主近道缘、逢凶化吉。"))
            }
        }
        WENCHANG[dm]?.let { tgts ->
            branches.firstOrNull { it in tgts }?.let { b ->
                out.add(ShenShaItem("文昌贵人", "吉", b, "文昌入命，聪明好学、利文章才艺，主才思敏捷、笔耕有成。"))
            }
        }
        LU_BRANCH[dm]?.let { lb ->
            if (lb in branches) out.add(ShenShaItem("禄神", "吉", lb, "禄神临支，主衣禄丰足、安稳有靠，一生少缺衣食、多享现成。"))
        }
        YANG_REN_BRANCH[dm]?.let { rb ->
            if (rb in branches) out.add(ShenShaItem("羊刃", "凶", rb, "羊刃在命，性刚果决、勇猛过人；制化得宜则为权，失制则易招伤灾。"))
        }
        TRIAD_RULES.forEach { rule ->
            rule.groups.forEach { (group, target) ->
                if ((dayB in group || yearB in group) && target in branches) {
                    out.add(ShenShaItem(rule.name, rule.nature, target, rule.desc))
                }
            }
        }
        return out.distinctBy { it.name }
    }

    /** 神煞图鉴（静态知识库，供「神煞图鉴」板块展示） */
    val SHEN_SHA_ATLAS: List<ShenShaMeta> = listOf(
        ShenShaMeta("天乙贵人", "\uD83C\uDF20", "吉", "最尊贵之星，主贵人扶持、遇难呈祥、人缘佳。"),
        ShenShaMeta("太极贵人", "☯️", "吉", "主悟性高、近道缘、逢凶化吉、好尚德行。"),
        ShenShaMeta("文昌贵人", "\uD83D\uDCDA", "吉", "主聪明好学、利文章才艺科举。"),
        ShenShaMeta("天德贵人", "\uD83D\uDEE1️", "吉", "月德之阳，主逢凶化吉、慈祥仁厚，一生之福星。"),
        ShenShaMeta("月德贵人", "\uD83C\uDF15", "吉", "阴德之星，主性情温良、遇难呈祥、宜积善。"),
        ShenShaMeta("禄神", "\uD83D\uDC8E", "吉", "主衣禄丰足、安稳有靠、少缺衣食。"),
        ShenShaMeta("文昌", "\uD83C\uDFEB", "吉", "主聪明好学、文章显达，利仕途。"),
        ShenShaMeta("国印贵人", "\uD83D\uDD16", "吉", "主掌印信权威、诚信稳重，利公职名望。"),
        ShenShaMeta("羊刃", "\uD83D\uDD3D", "凶", "性刚果决、勇猛；制化得宜为权，失制招伤。"),
        ShenShaMeta("桃花（咸池）", "\uD83C\uDF38", "中", "主容貌气质佳、异性缘旺；过旺易陷情欲。"),
        ShenShaMeta("驿马", "\uD83D\uDC0E", "中", "主走动迁徙、外出发展，利远方求财求学。"),
        ShenShaMeta("华盖", "\uD83C\uDFAD", "中", "主聪颖孤高、喜玄学艺术，性带清冷。"),
        ShenShaMeta("将星", "⭐", "吉", "主领导统御之才，处事果决、能掌局面。"),
        ShenShaMeta("亡神", "\uD83D\uDC80", "凶", "主心思深沉；过旺易惹官非暗损，须防小人。"),
        ShenShaMeta("劫煞", "⚡", "凶", "主性急刚暴、易遭突发劫夺，须远争斗。"),
        ShenShaMeta("红鸾", "\uD83D\uDC95", "吉", "喜庆之星，主姻缘桃花、婚恋喜庆。"),
        ShenShaMeta("天喜", "\uD83C\uDF89", "吉", "喜事之星，主添丁嫁娶、欢愉顺遂。")
    )

    private fun join(branches: Set<Branch>): String = branches.joinToString("") { it.chinese }

    // ==================== 综合结论 ====================

    /** 五行 → 代表行业 */
    private val INDUSTRY: Map<Element, List<String>> = mapOf(
        Element.WOOD to listOf("教育培训", "文化出版", "文创设计", "中医药", "园林农林", "纺织服装", "家具木作", "环保绿能"),
        Element.FIRE to listOf("互联网 / IT", "电子半导体", "能源电力", "广告传媒", "餐饮美食", "美容时尚", "演艺主播", "照明光电"),
        Element.EARTH to listOf("房地产", "建筑工程", "农业畜牧", "陶瓷建材", "仓储物流", "保险", "咨询顾问", "人力资源"),
        Element.METAL to listOf("金融投资", "机械制造", "汽车工业", "五金机电", "法律司法", "军警安防", "珠宝黄金", "精密仪器"),
        Element.WATER to listOf("国际贸易", "航运物流", "旅游酒店", "水产渔业", "公关销售", "心理咨询", "流媒体", "酒水饮料")
    )

    /** 五行 → 脏腑与养生（描述串） */
    private val HEALTH: Map<Element, String> = mapOf(
        Element.WOOD to "肝胆、筋络与双眼",
        Element.FIRE to "心脏、小肠与血脉",
        Element.EARTH to "脾胃、肌肉与消化",
        Element.METAL to "肺、大肠与呼吸皮肤",
        Element.WATER to "肾、膀胱、骨与泌尿"
    )

    /**
     * 五行 → 身体部位 key（用于在人体图上高亮）。
     * key 与 HealthBodyAtlas 中的标注保持一致。
     * 相邻部位合并为词（如「肝胆」「脾胃」「心血脉」「肾膀胱」），减少标签数量。
     */
    val HEALTH_PARTS: Map<Element, List<String>> = mapOf(
        Element.WOOD to listOf("肝胆", "眼", "筋"),
        Element.FIRE to listOf("心血脉", "小肠"),
        Element.EARTH to listOf("脾胃", "消化", "肌肉"),
        Element.METAL to listOf("肺", "皮肤"),
        Element.WATER to listOf("肾膀胱", "泌尿", "骨")
    )

    /** 日主天干 → 性格底色 */
    private val DM_TRAIT: Map<Stem, String> = mapOf(
        Stem.甲 to "如参天大树，正直向上、有担当，讲原则、不轻易低头；缺点是略显固执，不够圆融。",
        Stem.乙 to "如藤蔓花草，柔韧灵活、善于借力，人缘与适应力极佳；缺点是易随环境摇摆、缺少主心骨。",
        Stem.丙 to "如太阳当空，热情外放、光明磊落，天生具感染力；缺点是急躁张扬，耐性不足。",
        Stem.丁 to "如灯烛之火，细腻温暖、洞察人心，擅长在小处发光；缺点是敏感多虑、情绪起伏。",
        Stem.戊 to "如高山厚土，稳重可靠、承载力强，是众人依靠；缺点是保守笨重、变通较慢。",
        Stem.己 to "如田园之土，包容务实、擅长经营细节；缺点是心思偏多，容易内耗。",
        Stem.庚 to "如刀剑之金，果决刚毅、执行力强、讲义气；缺点是锋芒外露、言语易伤人。",
        Stem.辛 to "如珠玉之金，精致敏锐、审美出众、爱惜羽毛；缺点是心高气傲、受不得委屈。",
        Stem.壬 to "如江河大水，胸襟开阔、机变多谋、社交广；缺点是心思难定、容易漂泊。",
        Stem.癸 to "如雨露之水，内敛聪慧、直觉极强、慈悲柔和；缺点是易忧思、边界感弱。"
    )

    private fun elementCn(e: Element): String = when (e) {
        Element.WOOD -> "木"; Element.FIRE -> "火"; Element.EARTH -> "土"
        Element.METAL -> "金"; Element.WATER -> "水"
    }

    private fun dirOf(e: Element): String = when (e) {
        Element.WOOD -> "东方"; Element.FIRE -> "南方"; Element.EARTH -> "中宫 / 西南"
        Element.METAL -> "西方"; Element.WATER -> "北方"
    }

    private fun colorOf(e: Element): String = when (e) {
        Element.WOOD -> "青绿"; Element.FIRE -> "红紫"; Element.EARTH -> "黄棕"
        Element.METAL -> "白金银"; Element.WATER -> "黑蓝"
    }

    private fun numOf(e: Element): String = when (e) {
        Element.WOOD -> "3、8"; Element.FIRE -> "2、7"; Element.EARTH -> "5、0"
        Element.METAL -> "4、9"; Element.WATER -> "1、6"
    }

    /**
     * 综合命盘全部要素，生成分维度的结论论述。
     * 逻辑全部由命盘数据推导（确定性，无随机）。
     */
    private fun computeConclusion(
        chart: BaziChart,
        dm: Stem,
        strength: StrengthResult,
        yongJi: YongJi,
        geju: Geju,
        tenGods: List<TenGodItem>,
        shenSha: List<ShenShaItem>,
        relations: List<BranchRelation>
    ): BaziConclusion {
        val counts = chart.elementCounts
        var totalRaw = 0
        Element.values().forEach { totalRaw += (counts[it] ?: 0) }
        val total = totalRaw.coerceAtLeast(1)

        fun pct(e: Element): Int = ((counts[e] ?: 0) * 100.0 / total).roundToInt()

        val strongest = Element.values().maxByOrNull { counts[it] ?: 0 } ?: dm.element
        val missing = Element.values().filter { (counts[it] ?: 0) == 0 }
        val useful = yongJi.useful
        val primaryUse = useful.firstOrNull() ?: dm.element

        // 十神统计
        fun cnt(vararg gods: TenGod): Int = tenGods.count { it.tenGod in gods }
        val guanSha = cnt(TenGod.正官, TenGod.七杀)
        val yin = cnt(TenGod.正印, TenGod.偏印)
        val cai = cnt(TenGod.正财, TenGod.偏财)
        val shiShang = cnt(TenGod.食神, TenGod.伤官)
        val biJie = cnt(TenGod.比肩, TenGod.劫财)
        val isStrong = strength.level.contains("强")
        val isWeak = strength.level.contains("弱")

        val shenShaNames = shenSha.map { it.name }.toSet()
        fun has(n: String) = shenShaNames.any { it.contains(n) }

        // ---------- 总述 ----------
        val distText = Element.values().joinToString("、") { "${elementCn(it)}${pct(it)}%" }
        val summary = buildString {
            append("日主${dm.chinese}${elementCn(dm.element)}，生于${chart.monthPillar.branch.chinese}月，取「${geju.name}」。")
            append("全局五行占比 $distText，")
            append(if (missing.isEmpty()) "五行俱全，气脉流通；" else "缺${missing.joinToString("") { elementCn(it) }}，需后天补益；")
            append("${strongest.let { elementCn(it) }}气最盛。")
            append("日主判为「${strength.level}」（评分 ${strength.score}），")
            append("宜以${useful.joinToString("、") { elementCn(it) }}为用，忌${yongJi.avoidance.joinToString("、") { elementCn(it) }}。")
            append("综观此造：")
            append(
                when {
                    isStrong && cai + guanSha >= 4 -> "身旺而财官俱现，属能扛事、能成事的命局，中年后格局渐开。"
                    isStrong && shiShang >= 3 -> "身旺食伤吐秀，才华外显，适合靠专业技能与创意立身。"
                    isStrong -> "身旺有余而泄耗不足，宜主动求变、走出去消耗多余的自我能量。"
                    isWeak && yin + biJie >= 4 -> "身弱而印比有根，虽起步吃力，但贵人与后援不缺，宜稳步积累。"
                    isWeak -> "身弱财官旺，事多担子重，忌逞强硬拼，宜借势合作、择良木而栖。"
                    else -> "五行流通、日主中和，属进退有度的稳健命局，一生少大起大落。"
                }
            )
        }

        val items = mutableListOf<ConclusionItem>()

        // ---------- 1 性格底色 ----------
        val domGod = listOf(
            "官杀" to guanSha, "印星" to yin, "财星" to cai, "食伤" to shiShang, "比劫" to biJie
        ).maxByOrNull { it.second }
        val domText = when (domGod?.first) {
            "官杀" -> "官杀为主，自律守规、责任心重，在意外界评价，适合体制内或有明确规则的组织。"
            "印星" -> "印星为主，重学习与内省，心地仁厚、依赖感强，适合研究、教育、专业积累型路线。"
            "财星" -> "财星为主，务实重结果、对机会敏感，天生带商业嗅觉，行动导向明显。"
            "食伤" -> "食伤为主，表达欲与创造力旺盛，不喜束缚，适合靠作品、口才、技艺发声。"
            else -> "比劫为主，主见强、行动独立，重朋友义气，凡事习惯亲力亲为。"
        }
        items.add(
            ConclusionItem(
                title = "性格底色", icon = "\uD83E\uDDED",
                headline = "${dm.chinese}${elementCn(dm.element)}日主 · ${domGod?.first ?: "均衡"}主导",
                body = (DM_TRAIT[dm] ?: "") + "\n\n" + domText + "\n\n" +
                    if (relations.isNotEmpty())
                        "命局带${relations.joinToString("、") { it.type }}，内心张力较大，情绪与人际上容易经历拉扯，但也因此比常人更早成熟。"
                    else "四支相处平和，无明显刑冲，性情较为安定，处事不易走极端。",
                tags = listOfNotNull(
                    strength.level,
                    geju.name,
                    if (has("华盖")) "清高艺术" else null,
                    if (has("将星")) "领导气场" else null
                )
            )
        )

        // ---------- 2 事业与适合行业 ----------
        val industries = useful.flatMap { INDUSTRY[it] ?: emptyList() }.distinct().take(8)
        val careerBody = buildString {
            append("用神取${useful.joinToString("、") { elementCn(it) }}，")
            append("事业上宜往${useful.joinToString(" / ") { dirOf(it) }}发展，从事与${useful.joinToString("、") { elementCn(it) }}属性相关的行业最能借力。\n\n")
            append(
                when {
                    guanSha >= 3 && yin >= 2 -> "官印相生，走公职、国企、大型组织的管理路线最为顺畅，靠资历与信誉累积上位。"
                    guanSha >= 3 -> "官杀偏重，压力型驱动明显，适合有考核、有竞争的岗位；但须防长期紧绷，学会分权。"
                    shiShang >= 3 && cai >= 2 -> "食伤生财，靠才华变现是你的主路——技术、内容、设计、咨询等「作品换钱」的模式最合适。"
                    shiShang >= 3 -> "食伤旺而财弱，才华有余而落地不足，建议把创意收敛到一个能持续产出的方向上。"
                    cai >= 3 && isStrong -> "身旺任财，适合自主经营、销售、投资等直接与钱打交道的领域，越主动收获越大。"
                    cai >= 3 && isWeak -> "财多身弱，机会看得见却抓不稳，忌盲目扩张与借贷，宜依附平台、做深一件事。"
                    biJie >= 4 -> "比劫成群，合伙需格外谨慎，宜先小人后君子把股权与分工写清楚，独立发展反而更稳。"
                    else -> "五行流通，职业选择面较宽，关键在于选定后长期深耕，忌频繁转换赛道。"
                }
            )
            if (has("驿马")) append("\n\n命带驿马，外出、异地、跨区域发展的机会显著优于守在原地。")
            if (has("文昌")) append("\n\n文昌入命，考试、资格证、学术与写作类的路径对你格外有利。")
        }
        items.add(
            ConclusionItem(
                title = "事业与适合行业", icon = "\uD83D\uDCBC",
                headline = "宜走${useful.joinToString("、") { elementCn(it) }}路线 · ${dirOf(primaryUse)}得力",
                body = careerBody,
                tags = industries
            )
        )

        // ---------- 3 财运 ----------
        val wealthBody = buildString {
            append("命中财星共 $cai 处，")
            append(
                when {
                    cai == 0 -> "八字无明财，正常的工薪与专业收入反而更稳；求偏财、赌性投机对你极为不利，宜以技能换钱。"
                    isStrong && cai >= 3 -> "身旺财旺，是典型的能赚也守得住的配置，中年后财源渐宽，可适度做实业与资产配置。"
                    isStrong && cai in 1..2 -> "身旺财轻，赚钱能力强于财库，重点不在「赚多少」而在「留多少」，建议强制储蓄与定投。"
                    isWeak && cai >= 3 -> "财多身弱，是「钱追着人跑但接不住」的格局，切忌高杠杆、担保与合伙垫资，稳字第一。"
                    else -> "财星适中，属于随事业稳步增长的类型，不会暴富但少有大破财。"
                }
            )
            append("\n\n")
            append(if (Element.values().any { it in useful && INDUSTRY.containsKey(it) })
                "求财方位以${useful.joinToString("、") { dirOf(it) }}为佳，" else "")
            append("忌神${yongJi.avoidance.joinToString("、") { elementCn(it) }}对应的领域（如${yongJi.avoidance.firstOrNull()?.let { INDUSTRY[it]?.take(3)?.joinToString("、") } ?: "投机博彩"}）宜谨慎涉入。")
            if (has("禄神")) append("\n\n命带禄神，衣食有靠，一生少受缺钱之困。")
            if (has("劫煞") || biJie >= 4) append("\n\n比劫/劫煞显现，须防因朋友、合伙、借贷破财，钱账要清。")
        }
        items.add(
            ConclusionItem(
                title = "财运", icon = "\uD83D\uDCB0",
                headline = when {
                    cai == 0 -> "正财立身，忌投机"
                    isStrong && cai >= 3 -> "身旺任财，聚财力强"
                    isWeak && cai >= 3 -> "财多身弱，稳守为上"
                    else -> "财随业进，稳步增长"
                },
                body = wealthBody,
                tags = listOf("求财方位 ${dirOf(primaryUse)}", "幸运数 ${numOf(primaryUse)}")
            )
        )

        // ---------- 4 感情婚姻 ----------
        val dayBranch = chart.dayPillar.branch
        val spouseElement = dayBranch.element
        val loveBody = buildString {
            append("日支${dayBranch.chinese}（${elementCn(spouseElement)}）为配偶宫，")
            append(
                when {
                    produces(spouseElement, dm.element) -> "配偶宫生扶日主，另一半多为你的助力，婚后运势常有提升。"
                    controls(spouseElement, dm.element) -> "配偶宫克制日主，感情中易感到被管束或压力，需要主动沟通边界。"
                    spouseElement == dm.element -> "配偶宫与日主同气，两人性格相近、志趣相投，但也容易因太像而互不相让。"
                    controls(dm.element, spouseElement) -> "日主克配偶宫，你在关系中偏主导，须留意不要把伴侣管得太紧。"
                    else -> "日主生配偶宫，你付出较多，属于疼人的一方，但要避免单方面消耗。"
                }
            )
            append("\n\n")
            append(
                when {
                    guanSha >= 4 -> "官杀繁杂，感情来得多也乱，容易同时面对多个选择；宜晚婚，先立业再谈定。"
                    guanSha == 0 && cai == 0 -> "官财俱不显，缘分启动偏慢，多靠介绍或长期相处日久生情，不必急。"
                    shiShang >= 3 && guanSha >= 1 -> "食伤见官，个性强、不愿将就，感情中最忌硬碰硬，学会退半步就能海阔天空。"
                    cai >= 3 -> "财星旺相，异性缘与相处机会不缺，关键在于收心定性。"
                    else -> "感情配置较为平和，只要不刻意回避，中段大运遇合的机会自然出现。"
                }
            )
            if (has("桃花")) append("\n\n命带桃花（咸池），魅力与异性缘突出，是加分项，但也要留意分寸与专一。")
            if (has("红鸾") || has("天喜")) append("\n\n红鸾天喜临命，主喜庆姻缘，逢流年引动时婚恋喜事概率高。")
            val hasChong = relations.any { it.type.contains("冲") }
            if (hasChong) append("\n\n命局有冲，感情容易经历分合波折，宜择性情稳定者，避免闪婚闪离。")
        }
        items.add(
            ConclusionItem(
                title = "感情婚姻", icon = "\uD83D\uDC9E",
                headline = "配偶宫 ${dayBranch.chinese}（${elementCn(spouseElement)}）· " + when {
                    guanSha >= 4 -> "宜晚婚定心"
                    has("桃花") -> "缘分丰沛"
                    else -> "细水长流"
                },
                body = loveBody,
                tags = listOfNotNull(
                    if (has("桃花")) "桃花" else null,
                    if (has("红鸾")) "红鸾" else null,
                    if (has("天喜")) "天喜" else null,
                    "配偶宫${dayBranch.chinese}"
                )
            )
        )

        // ---------- 5 健康 ----------
        val over = Element.values().filter { pct(it) >= 35 }
        val healthBody = buildString {
            if (missing.isNotEmpty()) {
                append("五行缺${missing.joinToString("") { elementCn(it) }}，对应")
                append(missing.joinToString("、") { HEALTH[it] ?: "" })
                append("相对薄弱，是需要长期留意的部位。\n\n")
            }
            if (over.isNotEmpty()) {
                append("${over.joinToString("、") { elementCn(it) }}过旺（占比 ${over.joinToString("、") { "${pct(it)}%" }}），")
                append("易在${over.joinToString("、") { HEALTH[it] ?: "" }}上出现「太过」之症（如炎症、亢进、代谢紊乱），宜疏泄不宜再补。\n\n")
            }
            append("调养方向：多接触${useful.joinToString("、") { dirOf(it) }}的环境与${useful.joinToString("、") { colorOf(it) }}色系，")
            append(
                when (primaryUse) {
                    Element.WOOD -> "作息宜早睡养肝，多做拉伸与户外散步。"
                    Element.FIRE -> "注意心血管与情绪管理，忌熬夜与过度亢奋。"
                    Element.EARTH -> "重在规律饮食、护脾胃，忌暴饮暴食与思虑过度。"
                    Element.METAL -> "多做有氧与呼吸训练，注意肺与皮肤保养。"
                    Element.WATER -> "保证饮水与休息，护腰肾，忌久坐与过度劳累。"
                }
            )
            if (has("羊刃")) append("\n\n命带羊刃，须格外留意刀器、金属、交通与运动损伤。")
        }
        // 把「缺失」和「过旺」的五行拆出对应的人体部位 key，给 UI 的人体图使用
        val highlightParts = (missing + over).distinct().flatMap { e ->
            HEALTH_PARTS[e].orEmpty()
        }.distinct()

        items.add(
            ConclusionItem(
                title = "健康", icon = "\uD83C\uDF3F",
                headline = if (missing.isEmpty() && over.isEmpty()) "五行流通，体质均衡"
                else "留意 " + (missing + over).distinct().joinToString("、") { elementCn(it) } + " 相关脏腑",
                body = healthBody,
                tags = (missing + over).distinct().map { HEALTH[it] ?: elementCn(it) },
                highlightParts = highlightParts
            )
        )

        // ---------- 6 人际与贵人 ----------
        val socialBody = buildString {
            append(
                when {
                    has("天乙贵人") -> "命带天乙贵人，是神煞中最尊之星，人生关键节点常有长辈或前辈出手相助，遇难多能呈祥。"
                    yin >= 3 -> "印星厚重，长辈缘与师长缘极佳，善于从他人经验中获益。"
                    biJie >= 4 -> "比劫成群，同辈朋友多、江湖气重，但也容易因义气而吃亏。"
                    else -> "人际关系需靠主动经营，贵人多出现在你专业能力被看见之后。"
                }
            )
            append("\n\n")
            append("与你气场相合的人：五行属${useful.joinToString("、") { elementCn(it) }}者（如生肖/性格偏${useful.joinToString("、") { elementCn(it) }}之人）；")
            append("需要保持距离的：过度带${yongJi.avoidance.joinToString("、") { elementCn(it) }}气者，长期相处易被消耗。")
            if (has("亡神")) append("\n\n命带亡神，心思偏深，也易招暗损，涉及承诺与合约务必留书面凭据。")
        }
        items.add(
            ConclusionItem(
                title = "人际与贵人", icon = "\uD83E\uDD1D",
                headline = if (has("天乙贵人")) "天乙照命，贵人不缺" else "贵人随实力而来",
                body = socialBody,
                tags = shenSha.filter { it.nature == "吉" }.map { it.name }.take(5)
            )
        )

        // ---------- 7 开运建议 ----------
        val luckyBody = buildString {
            append("· 方位：常往 ${useful.joinToString(" / ") { dirOf(it) }} 走动、办公或居住；避${yongJi.avoidance.joinToString(" / ") { dirOf(it) }}久留。\n")
            append("· 颜色：多用 ${useful.joinToString("、") { colorOf(it) }}；少用 ${yongJi.avoidance.joinToString("、") { colorOf(it) }}。\n")
            append("· 数字：${useful.joinToString("、") { numOf(it) }}。\n")
            append("· 行为：")
            append(
                when {
                    isStrong -> "身旺宜「泄」与「耗」——多输出、多助人、多做能落地成果的事，忌闭门自恃。"
                    isWeak -> "身弱宜「扶」与「帮」——补足学习、找对平台、借助团队，忌单打独斗硬扛。"
                    else -> "中和之命宜「守势中求进」——保持节奏，遇机会敢试，遇诱惑能止。"
                }
            )
            append("\n· 时机：")
            append(
                if (missing.isNotEmpty())
                    "流年行至${missing.joinToString("、") { elementCn(it) }}运时，是补齐短板、开新局的关键窗口。"
                else "五行俱全，重在把握用神${useful.joinToString("、") { elementCn(it) }}当旺之年发力。"
            )
        }
        items.add(
            ConclusionItem(
                title = "开运建议", icon = "✨",
                headline = "以${useful.joinToString("、") { elementCn(it) }}为用 · ${if (isStrong) "宜泄宜耗" else if (isWeak) "宜扶宜帮" else "守中求进"}",
                body = luckyBody,
                tags = listOf(dirOf(primaryUse), colorOf(primaryUse), numOf(primaryUse))
            )
        )

        return BaziConclusion(summary, items)
    }
}
