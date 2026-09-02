package com.xuanji.app.domain

import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.Branch
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneInsight
import com.xuanji.app.data.model.Pillar
import com.xuanji.app.data.model.Stem
import com.xuanji.app.data.model.TenGod
import kotlin.math.abs

/**
 * 由周期读数汇出的结构化结果。overallDelta / 各维 delta 直接驱动评分，
 * insights / notes 驱动文案，两者取自同一批信号，做到「说的话」= 「算的分」。
 */
data class EasternReading(
    val headline: String,
    val summary: String,
    val advice: String,
    val insights: List<FortuneInsight>,
    val notes: Map<String, List<String>>,
    val overallDelta: Int,
    val careerDelta: Int,
    val wealthDelta: Int,
    val loveDelta: Int,
    val studyDelta: Int,
    val healthDelta: Int,
    val luckyElement: Element,
    /** 本读数所依据的周期干支；合并读数会把它拼起来 */
    val pillarText: String = ""
)

/**
 * 东方（八字）周期解盘器。
 *
 * 输入是「一段周期的干支」（流日柱 / 流月柱 / 流年柱，也可叠加大运），
 * 输出是一组可追溯的命理信号：五行喜忌、天干十神、天干合冲、
 * 地支六合六冲六害三刑六破、三合局三会方、日主十二长生、
 * 神煞（禄刃文昌天乙桃花驿马将星华盖劫煞贵人空亡）、伏吟反吟、太岁关系。
 *
 * 每个信号带自己的分值与各维度加减，并附一句人话解释；
 * 评分由信号累加，解说由信号展开，同输入必同输出。
 */
object EasternPeriodReader {

    private const val DIM_CAREER = "career"
    private const val DIM_WEALTH = "wealth"
    private const val DIM_LOVE = "love"
    private const val DIM_STUDY = "study"
    private const val DIM_HEALTH = "health"

    private data class Sig(
        val tag: String,
        val title: String,
        val body: String,
        val weight: Int,
        val career: Int = 0,
        val wealth: Int = 0,
        val love: Int = 0,
        val study: Int = 0,
        val health: Int = 0,
        val notes: List<Pair<String, String>> = emptyList()
    )

    private val STEM_ORDER = Stem.values()
    private val BRANCH_ORDER = Branch.values()

    // 天干五合：甲己合土、乙庚合金、丙辛合水、丁壬合木、戊癸合火（干距 5）
    private val HE_STEM_ELEMENTS = mapOf(
        5 to Element.EARTH, 6 to Element.METAL, 7 to Element.WATER,
        8 to Element.WOOD, 9 to Element.FIRE
    )
    private val HE_STEM_NAMES = mapOf(
        5 to "甲己合土", 6 to "乙庚合金", 7 to "丙辛合水", 8 to "丁壬合木", 9 to "戊癸合火"
    )

    private fun pairOf(a: Int, b: Int): Set<Int> = setOf(a, b)

    // 地支六合（含合化所临五行）
    private data class LiuHe(val a: Int, val b: Int, val hua: Element, val name: String)

    private val LIU_HE = listOf(
        LiuHe(0, 1, Element.EARTH, "子丑合土"),
        LiuHe(2, 11, Element.WOOD, "寅亥合木"),
        LiuHe(3, 10, Element.FIRE, "卯戌合火"),
        LiuHe(4, 9, Element.METAL, "辰酉合金"),
        LiuHe(5, 8, Element.WATER, "巳申合水"),
        LiuHe(6, 7, Element.EARTH, "午未合土")
    )

    // 六害
    private val LIU_HAI = listOf(pairOf(0, 7), pairOf(1, 6), pairOf(2, 5), pairOf(3, 4), pairOf(8, 11), pairOf(9, 10))

    // 六破
    private val LIU_PO = listOf(pairOf(0, 9), pairOf(3, 6), pairOf(1, 4), pairOf(7, 10), pairOf(2, 11), pairOf(5, 8))

    // 三刑（两支一组者）：无恩之刑 寅巳申；无礼之刑 子卯；恃势之刑 丑戌未
    private val SAN_XING = listOf(
        pairOf(2, 5), pairOf(5, 8), pairOf(2, 8),
        pairOf(1, 10), pairOf(10, 7), pairOf(1, 7),
        pairOf(0, 3)
    )

    private data class Triad(val branches: List<Int>, val element: Element, val name: String)

    private val SAN_HE = listOf(
        Triad(listOf(8, 0, 4), Element.WATER, "申子辰合水局"),
        Triad(listOf(11, 3, 7), Element.WOOD, "亥卯未合木局"),
        Triad(listOf(2, 6, 10), Element.FIRE, "寅午戌合火局"),
        Triad(listOf(5, 9, 1), Element.METAL, "巳酉丑合金局")
    )

    private val SAN_HUI = listOf(
        Triad(listOf(2, 3, 4), Element.WOOD, "寅卯辰会东方木"),
        Triad(listOf(5, 6, 7), Element.FIRE, "巳午未会南方火"),
        Triad(listOf(8, 9, 10), Element.METAL, "申酉戌会西方金"),
        Triad(listOf(11, 0, 1), Element.WATER, "亥子丑会北方水")
    )

    // 三合局索引（申子辰0 / 亥卯未1 / 寅午戌2 / 巳酉丑3）
    private val SAN_HE_GROUP = IntArray(12) { b ->
        SAN_HE.indexOfFirst { it.branches.contains(b) }
    }

    // 日干神煞表（按天干序号）
    private val LU_BRANCH = intArrayOf(2, 3, 5, 6, 5, 6, 8, 9, 11, 0)
    private val YANG_REN = intArrayOf(3, 4, 6, 7, 6, 7, 9, 10, 0, 1)
    private val WENCHANG = intArrayOf(5, 6, 8, 9, 8, 9, 11, 0, 2, 3)
    private val TIANYI = mapOf(
        0 to listOf(1, 7), 4 to listOf(1, 7), 6 to listOf(1, 7),
        1 to listOf(0, 8), 5 to listOf(0, 8),
        2 to listOf(11, 9), 3 to listOf(11, 9),
        7 to listOf(2, 6),
        8 to listOf(3, 5), 9 to listOf(3, 5)
    )

    // 按三合组取用的神煞（桃花 / 驿马 / 将星 / 华盖 / 劫煞）
    private val TAO_HUA = intArrayOf(9, 0, 3, 6)
    private val YI_MA = intArrayOf(2, 5, 8, 11)
    private val JIANG_XING = intArrayOf(0, 3, 6, 9)
    private val HUA_GAI = intArrayOf(4, 7, 10, 1)
    private val JIE_SHA = intArrayOf(5, 8, 11, 2)

    // 日主十二长生：起点（长生所落地支序号），阳干顺行、阴干逆行
    private val CHANG_SHENG_START = intArrayOf(11, 6, 2, 9, 2, 9, 5, 0, 8, 3)
    private val STAGES = listOf(
        "长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养"
    )
    private val STAGE_WEIGHT = intArrayOf(6, 1, 4, 8, 7, -1, -5, -7, -4, -8, 1, 2)
    private val STAGE_DESC = listOf(
        "如草木初萌，学什么、起手做什么吸收得最快",
        "气息未定，情绪与感情的事常常赶在正事前头上",
        "气象渐成，适合出面、亮相、把姿态端起来",
        "日主得地，说话有分量、办事有抓手，是担事的时候",
        "气盛到顶点，成事快却也最容易过头",
        "气势开始回落，守成比开拓省力",
        "外境琐碎缠身，人是容易被小事磨乏的",
        "气机沉静，不宜扩张，最适合收尾、复盘、了结旧账",
        "财与才入库，宜储蓄整理、把东西收拢，不宜抛头张扬",
        "落到低处，凡事先退一步，转机反而在这里结胎",
        "新机在暗处成形，表面看不见动静，内里已在酝酿",
        "宜调养、宜就学、宜借力，不宜一个人硬扛大任"
    )
    private val STAGE_HEALTH = listOf(
        "精力回升", "留意皮肤与泌尿", "大体康健", "元气充足",
        "防上火与血压起伏", "容易疲劳", "旧疾易犯", "宜静养",
        "脾胃消化偏滞", "抵抗力偏低", "作息宜规律", "将养为佳"
    )

    private val PILLAR_LABELS = listOf("年柱", "月柱", "日柱", "时柱")

    private fun palace(bi: Int): String = when (bi) {
        0 -> "年支（长辈、祖业、外在环境）"
        1 -> "月支（事业门户、上司、名分）"
        2 -> "日支（配偶宫、自身、家宅）"
        else -> "时支（子女、下属、晚景与出路）"
    }

    private fun sexagenaryIndex(p: Pillar): Int {
        val s = p.stem.ordinal
        val b = p.branch.ordinal
        for (i in 0 until 60) if (i % 10 == s && i % 12 == b) return i
        return 0
    }

    /** 该柱所在旬的空亡二支（甲子旬空戌亥等） */
    private fun xunKong(p: Pillar): List<Int> {
        val start = sexagenaryIndex(p) % 12
        return listOf((start + 10) % 12, (start + 11) % 12)
    }

    // ==================================================== 主入口

    /**
     * @param period 该周期论断所用的干支（流日柱 / 流月柱 / 流年柱）
     * @param label  周期称谓：今日 / 本周 / 本月 / 本年
     * @param dayun  当前所行大运（可空）
     * @param taiSui 是否按「太岁」论（只有流年为真）
     */
    fun read(
        chart: BaziChart,
        period: Pillar,
        label: String,
        dayun: Pillar? = null,
        taiSui: Boolean = false
    ): EasternReading = readRaw(chart, period, label, dayun, taiSui)

    private fun readRaw(
        chart: BaziChart,
        period: Pillar,
        label: String,
        dayun: Pillar?,
        taiSui: Boolean
    ): EasternReading {
        val dm = chart.dayMaster
        val pillarList = listOf(
            chart.yearPillar, chart.monthPillar, chart.dayPillar, chart.hourPillar
        )
        val sigs = mutableListOf<Sig>()
        sigs += elementSignals(chart, period, label)
        sigs += stemGodSignal(dm, chart, period, label)
        sigs += stemInteractionSignals(dm, chart, pillarList, period, label)
        sigs += branchInteractionSignals(chart, pillarList, period, label)
        sigs += groupingSignals(chart, pillarList, period, label)
        sigs += stageSignal(dm, period, label)
        sigs += shenShaSignals(chart, dm, pillarList, period, label)
        sigs += voidSignal(chart, period, label)
        sigs += repeatSignals(pillarList, period, label)
        if (taiSui) sigs += taiSuiSignal(chart, period, label)
        dayun?.let { sigs += dayunSignal(dm, chart, it, period, label) }

        return buildReading(label, period, chart, dm, sigs)
    }

    /**
     * 把多份读数合成一份：倍数 >=100 的是「逐日读数」（内部取均值），
     * 倍数 <100 的是「背景周期」（流月为势、流年为底，按倍数叠加）。
     * 例：周评 = 7 份日评(100) + 1 份流月(60) + 1 份流年(40)。
     */
    fun merge(parts: List<Pair<EasternReading, Int>>, label: String): EasternReading {
        parts.singleOrNull()?.takeIf { it.second >= 100 }?.let { return it.first }
        val daily = parts.filter { it.second >= 100 }
        val background = parts.filter { it.second < 100 }
        val dayCount = daily.size.coerceAtLeast(1)

        fun avg(sel: EasternReading.() -> Int): Int =
            daily.sumOf { sel(it.first) } / dayCount +
                background.sumOf { (r, w) -> sel(r) * w / 100 }

        val lucky = LinkedHashMap<Element, Int>()
        parts.forEach { (r, w) -> lucky[r.luckyElement] = (lucky[r.luckyElement] ?: 0) + w }

        // 同名信号（同 tag + 同 title）合并为一条，并标注在本周期出现的频次
        val grouped = LinkedHashMap<String, MutableList<Pair<FortuneInsight, Boolean>>>()
        parts.forEach { (r, w) ->
            val isDaily = w >= 100
            r.insights.forEach {
                grouped.getOrPut("${it.tag}#=${it.title}") { mutableListOf() }.add(it to isDaily)
            }
        }
        val insights = grouped.entries.map { (key, occ) ->
            val first = occ.first().first
            val dailyHits = occ.count { it.second }
            val note = if (dayCount > 1 && dailyHits > 1) "本周期内共出现 $dailyHits / $dayCount 日。" else ""
            FortuneInsight(
                tag = key.substringBefore("#="),
                title = key.substringAfter("#="),
                body = first.body + note,
                weight = first.weight
            )
        }
        // 显著性 = 单次分值 × 出现天数：只出现一天的孤signal 不会盖过贯穿全周期的主调
        val significance = grouped.entries.map { (key, occ) ->
            val hits = occ.size
            val weight = abs(occ.first().first.weight)
            key to (if (occ.count { it.second } > 1) weight * hits else weight)
        }.toMap()
        val picked = insights.sortedByDescending { significance["${it.tag}#=${it.title}"] ?: 0 }.take(8)

        val notes = LinkedHashMap<String, MutableList<String>>()
        parts.forEach { (r, _) ->
            r.notes.forEach { (k, list) ->
                val bucket = notes.getOrPut(k) { mutableListOf() }
                list.forEach { if (!bucket.contains(it)) bucket.add(it) }
            }
        }

        val lead = picked.firstOrNull()
        val headline = if (lead == null) {
            "${label}干支与命局未见激烈作用，以常理应对即可"
        } else {
            "${label}以「${lead.title}」为主调，${if (lead.weight >= 0) "顺势可取" else "此处最须留心"}"
        }
        val pillarText = (daily.map { it.first.pillarText } + background.map { it.first.pillarText })
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("、")
        val summary = buildString {
            if (pillarText.isNotBlank()) append("${label}干支依序为「$pillarText」。")
            lead?.let { append(it.body) }
            val second = picked.drop(1).firstOrNull { abs(it.weight) >= 5 }
            if (second != null) append(second.body)
        }
        val bandText = when {
            avg { overallDelta } >= 18 -> "这一段是明显可以借力的时候"
            avg { overallDelta } >= 8 -> "整体偏顺，主动一步就有回应"
            avg { overallDelta } >= -7 -> "平平常常，靠的是常规功夫"
            avg { overallDelta } >= -17 -> "外境阻力实有，宜减事、宜缓口"
            else -> "气机逆乱，凡是退一步，先保住根本"
        }
        val advice = buildString {
            val keys = picked.filter { abs(it.weight) >= 5 }.take(3)
            if (keys.isEmpty()) {
                append("${label}按常规安排即可，不必刻意加减")
            } else {
                keys.forEachIndexed { i, s -> append(if (i == 0) "${label}在「${s.title}」这一处" else "、「${s.title}」") }
            }
            append("——$bandText。")
        }

        return EasternReading(
            headline = headline,
            summary = summary,
            advice = advice,
            insights = picked,
            notes = notes.mapValues { it.value.take(3) },
            overallDelta = avg { overallDelta },
            careerDelta = avg { careerDelta },
            wealthDelta = avg { wealthDelta },
            loveDelta = avg { loveDelta },
            studyDelta = avg { studyDelta },
            healthDelta = avg { healthDelta },
            luckyElement = lucky.maxByOrNull { it.value }?.key ?: Element.EARTH,
            pillarText = pillarText
        )
    }

    // ==================================================== 汇总成文

    private fun buildReading(
        label: String,
        period: Pillar,
        chart: BaziChart,
        dm: Stem,
        sigs: List<Sig>
    ): EasternReading {
        val ordered = sigs.sortedByDescending { abs(it.weight) }
        val overallDelta = sigs.sumOf { it.weight }.coerceIn(-32, 32)
        val lucky = luckyElement(chart, period, sigs)

        val notes = LinkedHashMap<String, MutableList<String>>()
        sigs.forEach { s ->
            s.notes.forEach { (k, text) ->
                val bucket = notes.getOrPut(k) { mutableListOf() }
                if (bucket.size < 3 && !bucket.contains(text)) bucket.add(text)
            }
        }

        val insights = ordered.filter { it.weight != 0 }.map {
            FortuneInsight("东方·${it.tag}", it.title, it.body, it.weight)
        }.take(6)

        val lead = ordered.firstOrNull { abs(it.weight) >= 5 }
        val headline = if (lead == null) {
            "${period.display}，${label}气机平和，与命局未起激烈作用"
        } else {
            "$label${period.display}，${lead.title}"
        }

        val bandText = when {
            overallDelta >= 18 -> "这一段是明显可以借力的时候"
            overallDelta >= 8 -> "整体偏顺，主动一步就有回应"
            overallDelta >= -7 -> "平平常常，靠的是常规功夫"
            overallDelta >= -17 -> "外境阻力实有，宜减事、宜缓口"
            else -> "气机逆乱，凡是退一步，先保住根本"
        }
        val summary = buildString {
            append("${label}干支「${period.display}」，天干${period.stem.chinese}（${elementName(period.stem.element)}）")
            append("、地支${period.branch.chinese}（${elementName(period.branch.element)}），")
            append("对日主${dm.chinese}而言")
            append(lead?.let { "${it.title}为主调" } ?: "无显著作用")
            append("。$bandText。")
            ordered.drop(1).filter { abs(it.weight) >= 6 }.take(2).forEach { append(" ${it.body}") }
        }
        val advice = buildString {
            ordered.filter { abs(it.weight) >= 6 }.take(3).forEachIndexed { i, s ->
                append(if (i == 0) "$label${s.title}" else "、${s.title}")
            }
            if (isEmpty()) append("${label}按常规安排即可，不必刻意加减")
            append(if (overallDelta >= 8) "——趁势推进，别停在原地等更好的时机。" else "——先收后放，把该了结的了结。")
        }

        return EasternReading(
            headline = headline,
            summary = summary,
            advice = advice,
            insights = insights,
            notes = notes,
            overallDelta = overallDelta,
            careerDelta = sigs.sumOf { it.career }.coerceIn(-18, 18),
            wealthDelta = sigs.sumOf { it.wealth }.coerceIn(-18, 18),
            loveDelta = sigs.sumOf { it.love }.coerceIn(-18, 18),
            studyDelta = sigs.sumOf { it.study }.coerceIn(-18, 18),
            healthDelta = sigs.sumOf { it.health }.coerceIn(-18, 18),
            luckyElement = lucky,
            pillarText = period.display
        )
    }

    private fun luckyElement(chart: BaziChart, period: Pillar, sigs: List<Sig>): Element {
        val fav = chart.favorableElements
        val cand = listOf(period.stem.element, period.branch.element).firstOrNull { it in fav }
        if (cand != null) return cand
        return fav.firstOrNull() ?: Element.EARTH
    }

    // ==================================================== 信号一：五行喜忌

    private fun elementSignals(chart: BaziChart, period: Pillar, label: String): List<Sig> {
        val fav = chart.favorableElements.toSet()
        val unfav = chart.unfavorableElements.toSet()
        val out = mutableListOf<Sig>()
        val pairs = listOf(
            "天干" to period.stem.element,
            "地支" to period.branch.element
        )
        pairs.forEach { (pos, e) ->
            val cn = elementName(e)
            when {
                e in fav -> out += Sig(
                    tag = "五行喜忌",
                    title = "${pos}见${cn}为喜用",
                    body = "$label${pos}走$cn，正落在你命局喜用的五行上：这类日子不必勉强，事情本身就在往你这边倾斜，宜把主力时间花在真正要紧的那件事上。",
                    weight = 7, career = 2, wealth = 3, love = 1, health = 2,
                    notes = listOf(
                        DIM_CAREER to "${pos}见${cn}（喜用），办事有人接",
                        DIM_WEALTH to "${pos}见${cn}（喜用），进财路径顺"
                    )
                )
                e in unfav -> out += Sig(
                    tag = "五行喜忌",
                    title = "${pos}见${cn}为忌神",
                    body = "$label${pos}走$cn，是你命局里的忌神：同样一句话，平日说说无妨，这几天说出来就容易被接歪。宜减事、宜缓口、宜把决定往后压一压。",
                    weight = -7, career = -2, wealth = -3, love = -1, health = -1,
                    notes = listOf(
                        DIM_CAREER to "${pos}见${cn}（忌神），易生枝节",
                        DIM_WEALTH to "${pos}见${cn}（忌神），钱财上宜保守"
                    )
                )
                else -> out += Sig(
                    tag = "五行喜忌",
                    title = "${pos}见${cn}不喜不忌",
                    body = "$label${pos}走$cn，与你命局喜忌无涉，这一项不加分也不减分，全看自己怎么安排。",
                    weight = 1
                )
            }
        }
        return out
    }

    // ==================================================== 信号二：天干十神

    private data class GodScore(
        val weight: Int,
        val career: Int,
        val wealth: Int,
        val love: Int,
        val study: Int,
        val health: Int
    )

    private data class God(
        val label: String,
        val fav: GodScore,
        val favBody: String,
        val unfav: GodScore,
        val unfavBody: String,
        val careerNote: String,
        val wealthNote: String
    )

    private fun g(w: Int, c: Int = 0, m: Int = 0, l: Int = 0, s: Int = 0, h: Int = 0) =
        GodScore(w, c, m, l, s, h)

    private val GODS: Map<TenGod, God> = mapOf(
        TenGod.正官 to God(
            label = "正官",
            fav = g(8, 6, 1, 1, 0, 1),
            favBody = "官星为喜用而临{%L}：名分、考核、上级视线一起落下来，这是被人看见的一段。公开场合、正式汇报、需要签字认领的事，反而宜往前站。",
            unfav = g(-7, -5, 0, -1, 0, -2),
            unfavBody = "官星为忌而临{%L}：责任压得比能力快，容易被人拿规矩与进度来量你。宜把话说清楚、把边界画明白，别默认对方懂你的意思。",
            careerNote = "官星临期，事业宫被点亮，考核、汇报、面诊一类正式场合加分",
            wealthNote = "官印之路通，钱是走正道慢慢来的，忌抄近路"
        ),
        TenGod.七杀 to God(
            label = "七杀",
            fav = g(7, 5, 0, -1, 0, -2),
            favBody = "七杀为喜而临{%L}：压力与机会同来，越是有硬任务的时候你越有劲。适合主动接难的那件事，但作息一旦垮了，身体最先替你结账。",
            unfav = g(-8, -4, -1, -2, 0, -4),
            unfavBody = "七杀为忌而临{%L}：外境带杀气，容易被催、被压、被临时改要求。硬碰硬最吃亏，宜借力、宜留痕、宜把冲突挡在正式流程里。健康上注意睡眠与肩颈。",
            careerNote = "七杀临期，事业有冲劲也有人对着干，宜正面对决、忌背后较劲",
            wealthNote = "财在压力下才动，冲动决策容易把钱撒在情绪上"
        ),
        TenGod.正印 to God(
            label = "正印",
            fav = g(8, 2, -1, 1, 6, 3),
            favBody = "正印为喜而临{%L}：文书、学习、长辈与贵人的助力都在。这几天适合考试、签约、写材料、求一句推荐；遇到卡住的事，去找一个懂行的人问，比自己去试省一半力气。",
            unfav = g(-6, -1, -2, 0, -2, 2),
            unfavBody = "正印为忌而临{%L}：帮忙的人太多反而没了主张，容易陷在资料、情绪和「再想想」里。宜少问几个人，定一个标准，先做起来。",
            careerNote = "印星临期，靠资历、文书、背书吃饭的事顺",
            wealthNote = "印主名誉不主财，这几天赚名声比赚钱容易"
        ),
        TenGod.偏印 to God(
            label = "偏印",
            fav = g(5, 1, -1, -1, 5, 1),
            favBody = "偏印为喜而临{%L}：思路向内收拢，灵感多、观察细，适合研究、复盘、学一门冷门的手艺。不适合当众拍板——你想得深，但说出来的话容易省掉关键环节。",
            unfav = g(-6, -2, -2, -2, -1, -1),
            unfavBody = "偏印为忌而临{%L}：多疑、钻牛角、把人往坏处想是这几天最大的内耗。别在夜里做关于人的判断；困了就去睡，明早再看同一件事，感觉会不一样。",
            careerNote = "偏印临期，宜幕后谋划，不宜台前谈判",
            wealthNote = "偏财不显，冷门的路子反而可能有机会"
        ),
        TenGod.食神 to God(
            label = "食神",
            fav = g(7, 1, 5, 2, 3, 3),
            favBody = "食神为喜而临{%L}：这是最舒服的一种运气——口福、手艺、表达都能直接换成好处。适合见人、宴请、把作品拿出来给人看；情绪松弛，做事有节奏。",
            unfav = g(-4, -1, -1, 0, 0, -2),
            unfavBody = "食神为忌而临{%L}：享受过了头，时间与钱都从「无所谓」的地方漏掉。嘴上松一寸，腰上和账上紧一尺；日程需要人为设一道闸。",
            careerNote = "食神临期，靠表达、内容、手艺推进的事顺",
            wealthNote = "食神生财，钱是从做出来的东西上自然长出来的"
        ),
        TenGod.伤官 to God(
            label = "伤官",
            fav = g(6, -1, 2, 1, 5, 0),
            favBody = "伤官为喜而临{%L}：才华外露，嘴也快。适合创作、提案、把不合时宜的实话讲出来——但同样的话，这几天有七分机会被当成锋利。先说事、后说人。",
            unfav = g(-8, -6, -2, -4, 1, -1),
            unfavBody = "伤官为忌而临{%L}：忍不住要纠正别人，尤其是上位者。这几天的一句气话，可能要花几周去收拾。宜把不同意见写成文字、留一夜再发；感情上最忌讲赢。",
            careerNote = "伤官临期，与上位者、规则之间易起摩擦，宜做事不宜争执",
            wealthNote = "财从手艺上来，不从争执上来"
        ),
        TenGod.正财 to God(
            label = "正财",
            fav = g(8, 2, 7, 2, 0, 1),
            favBody = "正财为喜而临{%L}：按部就班的进账、实处的事情最稳。适合谈价、收尾款、核对数字、把散的口子收拢。感情上，这几天你说的话是有分量的，宜明确。",
            unfav = g(-6, -1, -5, -1, 0, -1),
            unfavBody = "正财为忌而临{%L}：钱的事容易卡在细节或被拖着，也可能因为过度谨慎错过该付的那一笔。合同数字、期限、经手人，三样都要亲眼再确认一次。",
            careerNote = "财星临期，务实推进的活最容易出成绩",
            wealthNote = "正财到位，是这几周期真正的进账窗口（或漏口，看喜忌）"
        ),
        TenGod.偏财 to God(
            label = "偏财",
            fav = g(6, 1, 6, 3, 0, 0),
            favBody = "偏财为喜而临{%L}：意外之得与人际应酬一起增多，钱在走动里、在人脉里。适合谈合作、跑动、把关系盘活。但偏财来得快也去得快，切忌贪多。",
            unfav = g(-7, -1, -5, -3, 0, -1),
            unfavBody = "偏财为忌而临{%L}：最容易在「顺手一把」上出事——临时加注、替人垫付、听了一个好消息就动钱。这几天凡是需要你先掏钱的提议，一律按暂停处理。",
            careerNote = "偏财临期，跑动型、资源型的事有戏，坐办公室的活反而拖",
            wealthNote = "钱财在动中，也在险中；见好就收是唯一护身符"
        ),
        TenGod.比肩 to God(
            label = "比肩",
            fav = g(4, 1, -1, -1, 1, 1),
            favBody = "比肩为喜而临{%L}：同行、朋友、同事能搭把手。适合结伴做事、找人分担。但比肩终归是分财之神，好处是大家有份，别指望独得。",
            unfav = g(-5, -2, -4, -3, 0, 0),
            unfavBody = "比肩为忌而临{%L}：有人来分你的时间、你的资源、甚至你的名分。合伙、AA、共享署名这类事要说在前头；逞强替人扛，最后是自己买单。",
            careerNote = "比劫临期，同级别之间会有竞争，宜守自己那摊",
            wealthNote = "分财之象，钱不宜借出、账不宜含糊"
        ),
        TenGod.劫财 to God(
            label = "劫财",
            fav = g(3, 2, -2, -1, 0, 0),
            favBody = "劫财为喜而临{%L}：行动力与抢占意识在线，适合去抢一个位置、争一个名额。但赢的姿态要收着点，别把对手变成日后的敌人。",
            unfav = g(-7, -2, -6, -4, 0, -1),
            unfavBody = "劫财为忌而临{%L}：破财之象最重的一段——冲动下单、临时改约、替人担保、被人带节奏，钱从这些地方走。凡是「现在就决定」的要求，先拒绝一半。",
            careerNote = "劫财临期，防抢功，防被推着做决定",
            wealthNote = "劫财见财，最忌合伙含糊与冲动消费"
        )
    )
    private fun stemGodSignal(dm: Stem, chart: BaziChart, period: Pillar, label: String): List<Sig> {
        val god = GODS[BaziCalculator.tenGod(dm, period.stem)] ?: return emptyList()
        val isFav = period.stem.element in chart.favorableElements.toSet()
        val score = if (isFav) god.fav else god.unfav
        val body = (if (isFav) god.favBody else god.unfavBody).replace("{%L}", label)
        val verdict = if (isFav) "取其为喜用之长" else "见其为忌之短"
        return listOf(
            Sig(
                tag = "十神",
                title = "${period.stem.chinese}是日主${dm.chinese}之${god.label}（$verdict）",
                body = body,
                weight = score.weight,
                career = score.career,
                wealth = score.wealth,
                love = score.love,
                study = score.study,
                health = score.health,
                notes = listOf(
                    DIM_CAREER to "${period.stem.chinese}（${god.label}）临${label}：${god.careerNote}",
                    DIM_WEALTH to "${period.stem.chinese}（${god.label}）临${label}：${god.wealthNote}"
                )
            )
        )
    }

    // ==================================================== 信号三：天干合冲

    private fun stemInteractionSignals(
        dm: Stem,
        chart: BaziChart,
        pillars: List<Pillar>,
        period: Pillar,
        label: String
    ): List<Sig> {
        val si = period.stem.ordinal
        val di = dm.ordinal
        val fav = chart.favorableElements.toSet()
        val out = mutableListOf<Sig>()
        val heGap = if (si > di) si - di else di - si

        if (heGap == 5) {
            val key = if (si > di) si else di
            val hua = HE_STEM_ELEMENTS[key] ?: Element.EARTH
            val name = HE_STEM_NAMES[key] ?: "天干五合"
            val helpful = hua in fav
            out += Sig(
                tag = "天干合",
                title = "${period.stem.chinese}合日主${dm.chinese}（$name）",
                body = "${label}天干与你的日干成合，主「被牵住」：这一段你很难只替自己考虑，不是有人有事黏上来，就是你自己舍不得松手。" +
                    "合化之气落${elementName(hua)}，" +
                    if (helpful) "正是你喜用的五行——关系在这里是助力，宜借人成事，宜把该谈的承诺谈定。"
                    else "于你未必有利——人情会盖过本分，宜先分清楚哪一句是情分、哪一句是职责。",
                weight = if (helpful) 5 else -3,
                career = -1, wealth = if (helpful) 2 else -2, love = 4, health = 0,
                notes = listOf(DIM_LOVE to "$name 合入日主：感情与人际同时被拉近，${label}最容易被「人」影响判断")
            )
        } else if (heGap == 6) {
            out += Sig(
                tag = "天干冲",
                title = "${period.stem.chinese}冲日主${dm.chinese}",
                body = "${label}天干与日主当面相冲，主「一开口就分岔」：你想往东，事往西，明面上的分歧这几天格外多。" +
                    "凡是要签、要付、要公开表态，先把对面那人的话完整听完再作声，能省掉八成的返工。",
                weight = -6, career = -3, wealth = -2, love = -3, health = -1,
                notes = listOf(DIM_CAREER to "天干冲身：外界要求与自身节奏对不上，宜先对齐再动手")
            )
        }

        // 合动命局其他柱（年 / 月 / 时）：引动对应宫位的人与事
        listOf(0, 1, 3).forEach { i ->
            val other = pillars[i].stem.ordinal
            val gap = if (si > other) si - other else other - si
            if (gap == 5) {
                val hua = HE_STEM_ELEMENTS[if (si > other) si else other] ?: Element.EARTH
                out += Sig(
                    tag = "天干合",
                    title = "${period.stem.chinese}合${pillars[i].stem.chinese}（动${PILLAR_LABELS[i]}）",
                    body = "${label}天干不去合日主，反倒来合你${PILLAR_LABELS[i]}之干——${palaceForStem(i)}。" +
                        "这周期的真正着力点不在你自己身上，而在这一段关系里：主动去谈、去问候、去把话带到，比一个人在原地盘算有用得多。" +
                        "合化${elementName(hua)}气。",
                    weight = 2, career = if (i == 1) 2 else 0, love = 2,
                    notes = listOf(DIM_LOVE to "合动${PILLAR_LABELS[i]}：${palaceForStem(i)}")
                )
            } else if (abs(si - other) == 6) {
                out += Sig(
                    tag = "天干冲",
                    title = "${period.stem.chinese}冲${pillars[i].stem.chinese}（动${PILLAR_LABELS[i]}）",
                    body = "${label}之干来冲你${PILLAR_LABELS[i]}之干，${palaceForStem(i)}先起波澜。" +
                        "这几日那头的安排、口径、态度容易变，你这边宜留余地、别把话讲死，也不要假定对方还会照上次的说法办。",
                    weight = -4, career = if (i == 1) -3 else -1, love = -1,
                    notes = listOf(DIM_CAREER to "冲${PILLAR_LABELS[i]}之干：${palaceForStem(i)}，外部口径易变")
                )
            }
        }
        return out
    }

    private fun palaceForStem(i: Int): String = when (i) {
        0 -> "长辈、祖上与外在环境那一层被你牵动"
        1 -> "事业门户、上司与名分那一层被你牵动"
        else -> "子女、下属与日后出路那一层被你牵动"
    }

    // ==================================================== 信号四：地支作用

    /** 周期地支与命局四支的六合、六冲、六害、三刑、六破、自刑。 */
    private fun branchInteractionSignals(
        chart: BaziChart,
        pillars: List<Pillar>,
        period: Pillar,
        label: String
    ): List<Sig> {
        val pb = period.branch.ordinal
        val out = mutableListOf<Sig>()

        fun palaceWord(i: Int): String = when (i) {
            0 -> "长辈、故土、外在环境这一层先动"
            1 -> "事业门户、上司与名分这一层先动"
            2 -> "自身与家宅、夫妻这一层先动"
            else -> "子女、下属与日后出路这一层先动"
        }

        for (i in pillars.indices) {
            val b = pillars[i].branch.ordinal
            val disp = pillars[i].branch.chinese

            if (i != 2 && b == pb) {
                out += Sig(
                    tag = "地支复见",
                    title = "期支${period.branch.chinese}复见${PILLAR_LABELS[i]}之$disp",
                    body = "${label}的地支与你${PILLAR_LABELS[i]}同一个字，命理叫「伏吟」。" +
                        "同一股气重叠下来，主重复、停滞、旧事重提：${palaceWord(i)}。" +
                        "这一段最省力的做法不是开新局，而是把那一层里没办完的旧事，挑一件真正办到底。",
                    weight = -4, career = -1, health = -1,
                    notes = listOf(DIM_CAREER to "${PILLAR_LABELS[i]}伏吟于期支：${palaceWord(i)}，宜了结旧事")
                )
            }

            val he = LIU_HE.firstOrNull { it.a == pb && it.b == b || it.a == b && it.b == pb }
            if (he != null) {
                val hua = he.hua
                val helpful = hua in chart.favorableElements.toSet()
                out += Sig(
                    tag = "地支六合",
                    title = "${period.branch.chinese}合$disp（${he.name}）",
                    body = "${label}地支与你${PILLAR_LABELS[i]}之${disp}成六合，${he.name}，合化出${elementName(hua)}。" +
                        "合是「系住」：${palaceWord(i)}，你会不由自主对那一边多花心思，事情也走得慢而稳。" +
                        if (helpful) "化气正是你的喜用，这份牵绊于你有利，宜依靠、宜结盟、宜把关系坐实。"
                        else "化气于你偏忌，这份牵绊更像纠缠，宜先谈清条件，再谈感情。",
                    weight = if (helpful) 6 else 2, career = 2, love = 4, wealth = if (helpful) 2 else 0, health = 1,
                    notes = listOf(DIM_LOVE to "${he.name}合动${PILLAR_LABELS[i]}：${palaceWord(i)}，关系比道理管用")
                )
                continue
            }

            if (abs(pb - b) == 6) {
                val qg = if (pb == 6 || b == 6) "火" else if (pb == 0 || b == 0) "水" else "土"
                out += Sig(
                    tag = "地支六冲",
                    title = "${period.branch.chinese}冲$disp（${PILLAR_LABELS[i]}）",
                    body = "${label}地支来冲你${PILLAR_LABELS[i]}之$disp，${qg}水相激，气机在这里翻转。" +
                        "${palaceWord(i)}。冲不是坏字，但一定是「动」字：这段那处必有一件事要重来、要改口、要换人、要挪地方。" +
                        "宜主动把可变的那部分先改掉，别等着被通知；重要决定尽量避开对冲最紧的那几天。",
                    weight = -8, career = -3, wealth = -2, love = -2, health = -2,
                    notes = listOf(
                        DIM_CAREER to "${period.branch.chinese}冲$disp：${palaceWord(i)}，主变动，宜先动再谈",
                        DIM_HEALTH to "冲则气散，${label}留意作息与出行安全"
                    )
                )
                continue
            }

            if (pairOf(pb, b) in LIU_HAI) {
                out += Sig(
                    tag = "地支六害",
                    title = "${period.branch.chinese}害$disp（${PILLAR_LABELS[i]}）",
                    body = "${label}地支与你${PILLAR_LABELS[i]}之${disp}相害。害是「暗伤」：不当面翻脸，却在背后感情分、耗信任。" +
                        "${palaceWord(i)}。${label}不宜传话、不宜替人担保、不宜把私下的玩笑说给第三个人听。",
                    weight = -4, career = -1, love = -3, wealth = -1,
                    notes = listOf(DIM_LOVE to "六害入${PILLAR_LABELS[i]}：嘴上不说、心里记账，宜当面把话说开")
                )
                continue
            }

            if (pairOf(pb, b) in SAN_XING) {
                out += Sig(
                    tag = "地支三刑",
                    title = "${period.branch.chinese}刑$disp（${PILLAR_LABELS[i]}）",
                    body = "${label}地支与${PILLAR_LABELS[i]}之${disp}构成三刑。刑主「规矩与情面打架」：容易碰到争执、投诉、验查、文书出错、旧账翻起。" +
                        "${palaceWord(i)}。这一段凡涉合同、报销、体检、证照，一律按最严的标准自查一遍，别走捷径。",
                    weight = -5, career = -3, wealth = -1, love = -1, health = -2,
                    notes = listOf(DIM_CAREER to "三刑见${PILLAR_LABELS[i]}：${label}程序、文书、口径务必自清")
                )
                continue
            }

            if (pairOf(pb, b) in LIU_PO) {
                out += Sig(
                    tag = "地支六破",
                    title = "${period.branch.chinese}破$disp（${PILLAR_LABELS[i]}）",
                    body = "${label}地支与${PILLAR_LABELS[i]}之${disp}相破，破是「小磕小碰」：计划不会全毁，但会在细节上出岔，东西易损、时间易误。" +
                        "${palaceWord(i)}。留出冗余，是对冲这个字最实在的办法。",
                    weight = -2, career = -1, wealth = -1, health = -1,
                    notes = listOf(DIM_WEALTH to "六破：小破耗多，${label}物品与开支宜清点")
                )
            }
        }

        val ziXing = setOf(4, 6, 9, 11)
        if (pb in ziXing && pillars.any { it.branch.ordinal == pb }) {
            out += Sig(
                tag = "地支自刑",
                title = "${period.branch.chinese}见${period.branch.chinese}为自刑",
                body = "${label}地支与命局同字且落辰午酉亥，这四组最怕自刑——麻烦多半是自己跟自己较劲：反复回想、临时改主意、夜里翻旧账。" +
                    "外部并无大碍，是内耗把力气抽干的。${label}给自己定一个「到此为止」的时点，过点即停。",
                weight = -4, love = -2, health = -2, study = -1,
                notes = listOf(DIM_HEALTH to "自刑：心结耗神，${label}宜外动（运动、出门）不宜内想")
            )
        }
        return out
    }

    // ==================================================== 信号五：三合三会

    private fun groupingSignals(
        chart: BaziChart,
        pillars: List<Pillar>,
        period: Pillar,
        label: String
    ): List<Sig> {
        val out = mutableListOf<Sig>()
        val fav = chart.favorableElements.toSet()
        val unfav = chart.unfavorableElements.toSet()
        val pb = period.branch.ordinal
        val present = pillars.map { it.branch.ordinal }
        fun verdict(e: Element): Pair<Int, String> = when {
            e in fav -> 1 to "所化之气正是你的喜用，这股局于你为助"
            e in unfav -> -1 to "所化之气落在你的忌神上，这股局于你为耗"
            else -> 0 to "此气与你喜忌无涉，局成不成，看你自己接不接得住"
        }

        SAN_HE.forEach { triad ->
            if (!triad.branches.contains(pb)) return@forEach
            val others = triad.branches.filter { it != pb }
            val hit = others.filter { present.contains(it) }
            val cn = elementName(triad.element)
            val (sign, judge) = verdict(triad.element)
            val 中支 = triad.branches[1]

            if (hit.size == 2) {
                out += Sig(
                    tag = "三合局",
                    title = "${triad.name}全（${label}引动）",
                    body = "${label}地支把命局里的另两支一并凑成${triad.name}，三合局成，化气为$cn。$judge。" +
                        "局成的意思是「成团成势」：${label}身边容易聚起同一立场的人，同一股劲，事情要么一起顺，要么一起翻。" +
                        "宜借力结盟、宜合伙办事、宜把散着的资源并到一处；不宜单干，不宜此刻讲条件。",
                    weight = if (sign >= 0) 8 else -8,
                    career = if (sign >= 0) 4 else -3, wealth = if (sign >= 0) 4 else -3, love = if (sign >= 0) 2 else -1,
                    notes = listOf(DIM_CAREER to "${triad.name}成局：${cn}气作主，${if (sign >= 0) "宜结盟借力" else "宜防被团体裹挟"}")
                )
            } else if (hit.size == 1 && triad.branches.contains(中支)) {
                out += Sig(
                    tag = "三合半局",
                    title = "${label}与命局成${triad.name}之半合",
                    body = "${label}地支与命局中的${BRANCH_ORDER[hit[0]].chinese}凑成半合${triad.name}，合到${elementName(triad.element)}——" +
                        "半合是「梯子搭了一半」：有势，但要靠人补齐第三支才成事。$judge。" +
                        "${label}找那个差一点的人、差一句话的场合，往往就把局面合上了。",
                    weight = if (sign >= 0) 5 else -5,
                    career = if (sign >= 0) 2 else -2, wealth = if (sign >= 0) 2 else -2, love = if (sign >= 0) 1 else -1,
                    notes = listOf(DIM_WEALTH to "半合${cn}局：局面可用但未合拢，${label}宜找补全的人")
                )
            }
        }

        SAN_HUI.forEach { trio ->
            if (!trio.branches.contains(pb)) return@forEach
            val hit = trio.branches.filter { it != pb && present.contains(it) }
            if (hit.size >= 2) {
                val cn = elementName(trio.element)
                val (sign, judge) = verdict(trio.element)
                out += Sig(
                    tag = "三会方",
                    title = "${trio.name}成方",
                    body = "${label}地支与命局连成一片${trio.name}，会方是同类一气聚成一方，力大而粗，不挑对象。" +
                        "${cn}气一方压倒性占优，你很难在这个周期里维持中立姿态。$judge。" +
                        "这类周期适合做需要气势的事：公开表态、集中投入、大范围推进；不适合精细谈判与需要两头讨好的场合。",
                    weight = if (sign >= 0) 6 else -6,
                    career = if (sign >= 0) 3 else -2, wealth = if (sign >= 0) 3 else -2, health = if (sign >= 0) 1 else -1,
                    notes = listOf(DIM_CAREER to "三会${cn}方：${label}一股气压倒，宜顺势大声，不宜低声谈价")
                )
            }
        }
        return out
    }

    // ==================================================== 信号六：日主十二长生

    private fun stageSignal(dm: Stem, period: Pillar, label: String): List<Sig> {
        val start = CHANG_SHENG_START[dm.ordinal]
        val idx = if (dm.isYang) {
            (period.branch.ordinal - start + 12) % 12
        } else {
            (start - period.branch.ordinal + 12) % 12
        }
        val name = STAGES[idx]
        val w = STAGE_WEIGHT[idx]
        val extra = when (name) {
            "临官" -> "此支同时是你的禄地，禄到则身旺自足，不必等人给。"
            "帝旺" -> "此支又近羊刃，旺到极处易折，成事快，翻脸也快。"
            "墓" -> "墓为库，财与才都在库里，锁着不动则有，要用必得开库。"
            "绝" -> "绝地是退无可退，反过来自有生机。"
            else -> ""
        }
        return listOf(
            Sig(
                tag = "日主十二长生",
                title = "${dm.chinese}日主临${period.branch.chinese}为「$name」",
                body = "把${period.branch.chinese}摆在你日干${dm.chinese}的十二长生上，这一周期你自身的气数落在「$name」：${STAGE_DESC[idx]}。$extra" +
                    "体感上就是：同一件事，这周期你办起来的省力程度和上周期完全不一样。",
                weight = w,
                career = if (w >= 5) 3 else if (w <= -5) -2 else 0,
                wealth = if (w >= 5) 2 else if (w <= -5) -2 else 0,
                study = if (name == "长生" || name == "养") 3 else if (name == "病") -1 else 0,
                health = if (w >= 5) 2 else if (w <= -5) -3 else -1,
                notes = listOf(
                    DIM_CAREER to "日主临$name：${STAGE_DESC[idx]}",
                    DIM_HEALTH to "${name}之地，${STAGE_HEALTH[idx]}"
                )
            )
        )
    }

    // ==================================================== 信号七：神煞

    private fun shenShaSignals(
        chart: BaziChart,
        dm: Stem,
        pillars: List<Pillar>,
        period: Pillar,
        label: String
    ): List<Sig> {
        val out = mutableListOf<Sig>()
        val pb = period.branch.ordinal
        val homeIdx = pillars.indexOfFirst { it.branch.ordinal == pb }
        val home = if (homeIdx >= 0) "，且正临你${PILLAR_LABELS[homeIdx]}" else ""
        val group = SAN_HE_GROUP[chart.dayPillar.branch.ordinal]
            .takeIf { it >= 0 } ?: SAN_HE_GROUP[chart.yearPillar.branch.ordinal]

        if (pb == LU_BRANCH[dm.ordinal]) {
            out += Sig(
                tag = "神煞·禄",
                title = "禄到${period.branch.chinese}$home",
                body = "${dm.chinese}日主的禄在${BRANCH_ORDER[LU_BRANCH[dm.ordinal]].chinese}，${label}走到这里，等于你自己有饭吃、有位置站。" +
                    "禄主「稳到的进账与实权」：不求暴利，但实在，谈薪、谈价、谈条件这周期最有底气。宜接活、宜签长期、宜要名分。",
                weight = 6, career = 3, wealth = 4, health = 1,
                notes = listOf(DIM_WEALTH to "禄神临期：${label}进得踏实钱，宜开口要该得的那一份")
            )
        }

        if (pb == YANG_REN[dm.ordinal]) {
            val chongged = pillars.any { abs(it.branch.ordinal - pb) == 6 }
            out += Sig(
                tag = "神煞·羊刃",
                title = "羊刃到${period.branch.chinese}$home",
                body = "${label}带你的羊刃。刃是刀，也是过量的自己：精力与胆量同时上头，决断快、话也硬，最容易在得意那一句上出事。" +
                    if (chongged) "更要命的是命局里还有人来冲这把刃——这是典型的见血之象，${label}忌刀器、忌快车、忌动手术以外的冒险。"
                    else "单刃不冲还好，能把劲用在工作量上就是生产力；用在争执上就是赔偿。",
                weight = -6, career = 2, wealth = -3, love = -2, health = -4,
                notes = listOf(
                    DIM_HEALTH to "羊刃：${label}忌血光、忌过劳、忌硬碰",
                    DIM_LOVE to "羊刃主刚，${label}说话留三分余地"
                )
            )
        }

        if (pb == WENCHANG[dm.ordinal]) {
            out += Sig(
                tag = "神煞·文昌",
                title = "文昌到${period.branch.chinese}$home",
                body = "${label}踩着你的文昌。文昌主文字、考试、表达与名声：这周期写得出、说得清、记得住，交出去的东西质量高于平时。" +
                    "宜考试、宜投稿、宜汇报、宜把复杂的事整理成一张纸。${label}别浪费在闲聊上。",
                weight = 5, study = 5, career = 2,
                notes = listOf(DIM_STUDY to "文昌临期：${label}是出文字与成绩的时候，宜先办最烧脑那件")
            )
        }

        if (TIANYI[dm.ordinal]?.contains(pb) == true) {
            out += Sig(
                tag = "神煞·天乙贵人",
                title = "天乙贵人到${period.branch.chinese}$home",
                body = "${label}的地支是你日干${dm.chinese}的天乙贵人。贵人不替你干活，贵人在关键处替你说话：这周期开口求人有回应，卡住的事有人伸手。" +
                    "宜求人、宜引荐、宜赴约、宜把难办的话说给能拍板的人听；不宜自己闷头扛。",
                weight = 5, career = 3, love = 2, wealth = 1,
                notes = listOf(DIM_CAREER to "天乙贵人临期：${label}主动求人是效率最高的一条路")
            )
        }

        if (group >= 0 && pb == TAO_HUA[group]) {
            val marriage = homeIdx == 2
            out += Sig(
                tag = "神煞·桃花",
                title = "桃花到${period.branch.chinese}" + if (marriage) "（且入配偶宫）" else "",
                body = "${BRANCH_ORDER[chart.dayPillar.branch.ordinal].chinese}（或年支）出生的人，桃花在${BRANCH_ORDER[TAO_HUA[group]].chinese}，${label}正当时。" +
                    "桃花是「被看见」：这周期你出门有人多看一眼，话也说得比平日活。" +
                    if (marriage) "更要紧的是它落进了你的配偶宫——感情的事不会只停在气氛上，会有实质进展或是实质摩擦，两者都比平日浓。"
                    else "桃花不在配偶宫时，好处在人际与人气，感情上易来易去，别把气氛当承诺。",
                weight = 4, love = 5, career = 1,
                notes = listOf(DIM_LOVE to if (marriage) "桃花入日支：${label}感情有实质事件，宜当面定分晓"
                    else "桃花在旁宫：${label}人缘旺但易误读，看清楚再动心")
            )
        }

        if (group >= 0 && pb == YI_MA[group]) {
            out += Sig(
                tag = "神煞·驿马",
                title = "驿马到${period.branch.chinese}$home",
                body = "${label}带驿马。驿马主动：出差、搬家、跑客户、换岗、出国、至少也是换个地方吃饭。马不能按住，按住就生闷气；" +
                    "这周期宜把要跑的行程排在一起一口气办完，越动越顺，越歇越躁。",
                weight = 3, career = 3, wealth = 1, health = -1,
                notes = listOf(DIM_CAREER to "驿马临期：${label}宜动不宜守，路上的事比桌前的事成得快")
            )
        }

        if (group >= 0 && pb == JIANG_XING[group]) {
            out += Sig(
                tag = "神煞·将星",
                title = "将星到${period.branch.chinese}$home",
                body = "${label}遇将星，这是「有人看你眼色」的位置：责任会自己找上你，你也得接。宜带队、宜拍板、宜定规矩；" +
                    "不宜把该你拿的主意推给别人，推出去这周期就白过了。",
                weight = 3, career = 4,
                notes = listOf(DIM_CAREER to "将星临期：${label}该你说话，别缩")
            )
        }

        if (group >= 0 && pb == HUA_GAI[group]) {
            out += Sig(
                tag = "神煞·华盖",
                title = "华盖到${period.branch.chinese}$home",
                body = "${label}遇华盖。华盖是孤高之星：主研究、主艺术、主玄学，也主一个人待着。这周期你对热闹的兴致自然下降，对沉进去做一件事的兴致上升。" +
                    "宜读书、宜钻研、宜独处写东西；不宜硬挤应酬，去了也只是消耗。副作用是容易想太多、说话太直。",
                weight = 1, study = 3, love = -2, career = -1,
                notes = listOf(DIM_STUDY to "华盖临期：${label}适合深耕不适合社交，把孤处当资源用")
            )
        }

        if (group >= 0 && pb == JIE_SHA[group]) {
            out += Sig(
                tag = "神煞·劫煞",
                title = "劫煞到${period.branch.chinese}$home",
                body = "${label}的地支是你三合局的劫煞。劫主「夺」：劫财、劫物、劫时，最典型是钱与时间同时在半路上被人截一口。" +
                    "这周期忌大额转账、忌代付、忌把贵重东西交人保管、忌把关键时间节点压在别人的承诺上。" +
                    "凡是要你先出钱、先出力、后讲话的，一律延后。",
                weight = -3, wealth = -4, career = -1, health = -1,
                notes = listOf(DIM_WEALTH to "劫煞临期：${label}防人分走你那一口，钱与物不空手过夜的账")
            )
        }
        return out
    }

    // ==================================================== 信号八：空亡

    private fun voidSignal(chart: BaziChart, period: Pillar, label: String): List<Sig> {
        val kong = xunKong(chart.dayPillar)
        if (!kong.contains(period.branch.ordinal)) return emptyList()
        val cn = period.branch.chinese
        val hitPalace = listOf(
            chart.yearPillar, chart.monthPillar, chart.hourPillar
        ).mapIndexed { i, p -> if (p.branch.ordinal == period.branch.ordinal) PILLAR_LABELS[i] else null }
            .filterNotNull()
        return listOf(
            Sig(
                tag = "旬空空亡",
                title = "${period.display}落空亡（$cn 空）",
                body = "以日柱${chart.dayPillar.display}起旬，空在${BRANCH_ORDER[kong[0]].chinese}${BRANCH_ORDER[kong[1]].chinese}，" +
                    "${label}的地支${cn}正在空里。空亡不是凶，是「不实」：话说得响，落地会轻；答应得好，到时候会滑。" +
                    if (hitPalace.isNotEmpty()) "更具体的是，$cn 在你命局里本就在${hitPalace.joinToString("、")}，那一句「空」是直接落在这层人事上的——${label}关于这处的一切口头结论，都要拿书面钉一遍。"
                    else "${label}凡属口头承诺、意向、初步共识，一律当作尚未发生处理。",
                weight = -3, career = -1, wealth = -2, love = -1,
                notes = listOf(
                    DIM_CAREER to "空亡：${label}宜确认、宜落笔、宜复述，不宜只拿一句口头话",
                    DIM_LOVE to "空亡在$cn：${label}感情上的承诺易飘，听三分信三分"
                )
            )
        )
    }

    // ==================================================== 信号九：伏吟反吟

    private fun repeatSignals(pillars: List<Pillar>, period: Pillar, label: String): List<Sig> {
        val out = mutableListOf<Sig>()
        val ps = period.stem.ordinal
        val pb = period.branch.ordinal
        val same = pillars.withIndex().filter { (i, p) -> i != 2 && p.stem.ordinal == ps && p.branch.ordinal == pb }
        if (same.isNotEmpty()) {
            out += Sig(
                tag = "伏吟",
                title = "${label}干支与${same.joinToString("、") { PILLAR_LABELS[it.index] }}同字",
                body = "干支与命局同字叫伏吟，古诀说「反吟伏吟，哭泣淋淋」，其实核心只是一个「滞」字：${label}里做的事看着在做，实际在原地，" +
                    "同一类麻烦还会再来一次。它来第二次，是逼你把第一次那件没做完的收尾。" +
                    "宜复盘、宜补漏、宜回同一个地方找同一个人，不宜开新的战场。",
                weight = -5, career = -2, wealth = -1, love = -1, health = -1,
                notes = listOf(DIM_CAREER to "伏吟：${label}主重复停滞，先把旧账了结再开新事")
            )
        }
        val fan = pillars.withIndex().filter { (i, p) ->
            i != 2 && abs(p.stem.ordinal - ps) == 4 && abs(p.branch.ordinal - pb) == 6
        }
        if (fan.isNotEmpty()) {
            out += Sig(
                tag = "反吟",
                title = "${label}与${fan.joinToString("、") { PILLAR_LABELS[it.index] }}天克地冲",
                body = "${label}干支跟你的${fan.joinToString("、") { PILLAR_LABELS[it.index] }}天干相克、地支相冲，这叫反吟，是周期里最硬的一击：" +
                    "两头都不肯让，事情容易被整段推翻。这一段忌签长约、忌大投入、忌摊牌、忌远行赶时间。" +
                    "躲不掉的就只办一件事：把损失算清楚，然后认。其余都往后推。",
                weight = -8, career = -4, wealth = -3, love = -2, health = -3,
                notes = listOf(
                    DIM_CAREER to "反吟天克地冲：${label}不宜决裂式决定，宜先减损",
                    DIM_HEALTH to "天克地冲：${label}作息与出行从保守，注意旧伤"
                )
            )
        }
        return out
    }

    // ==================================================== 信号十：太岁（仅流年）

    private fun taiSuiSignal(chart: BaziChart, period: Pillar, label: String): List<Sig> {
        val ming = chart.yearPillar.branch.ordinal
        val sui = period.branch.ordinal
        val out = mutableListOf<Sig>()
        val mingCn = chart.yearPillar.branch.chinese
        val suiCn = period.branch.chinese

        if (ming == sui) {
            out += Sig(
                tag = "太岁·值",
                title = "值太岁（本命年，${suiCn}年）",
                body = "${label}是${suiCn}年，与你生年${mingCn}同字，叫伏太岁、也叫本命年。太岁当令，你这一年的气是「自己跟自己顶」：" +
                    "事不由外人搅，多由自己起，旧问题会用新形式再演一遍。" +
                    "本年宜立一个足够大的主线（健康、手艺、身份），让太岁的力气有去处；最忌同时开三条战线、忌以「讨个吉利」为名的冲动消费与跳槽。" +
                    "生日前后、${suiCn}月与对冲的${BRANCH_ORDER[(sui + 6) % 12].chinese}月是本年最紧的两个口。",
                weight = -5, career = -1, health = -2, love = -1,
                notes = listOf(DIM_CAREER to "值太岁：本年主「自己转」，宜定单一主线，忌多线开战")
            )
        }
        if (abs(ming - sui) == 6) {
            out += Sig(
                tag = "太岁·冲",
                title = "冲太岁（${suiCn}冲${mingCn}）",
                body = "${label}太岁在$suiCn，正冲你生年支$mingCn，这是六年一轮的「正面受冲」。冲太岁的变动不在你身上，在你与外部世界的关系上：" +
                    "居所、组织、关系、身份，四样里本年至少动一样，被动挪不如自己先挪。" +
                    "本年宜主动改变量：搬家、换岗、结束一段名存实亡的合作、把拖了两年的证照手续办掉。" +
                    "忌的是同时变动两样以上，也忌用情绪决定去留。对冲之月（${BRANCH_ORDER[(ming) % 12].chinese}、${BRANCH_ORDER[(sui + 6) % 12].chinese}月）不作重大决定。",
                weight = -8, career = -3, wealth = -2, love = -2, health = -2,
                notes = listOf(DIM_CAREER to "冲太岁：本年必有结构性变动，宜自选一项先动，忌两头同动")
            )
        }
        if (pairOf(ming, sui) in LIU_HAI) {
            out += Sig(
                tag = "太岁·害",
                title = "害太岁（${suiCn}害${mingCn}）",
                body = "${label}太岁与生年支相害。害太岁的难处不在明处，在人情：本年容易碰到口头上帮你、实际误你的事，或者是你好心办的事最后成了你的责任。" +
                    "本年立字据、留记录、把责任边界写清，是唯一的解法；不宜替人担保，不宜掺进是非的中间层。",
                weight = -5, career = -2, love = -3, wealth = -1,
                notes = listOf(DIM_LOVE to "害太岁：本年防人情暗耗，帮忙先划边界")
            )
        }
        if ((pairOf(ming, sui) in SAN_XING) || (ming == sui && sui in setOf(4, 6, 9, 11))) {
            out += Sig(
                tag = "太岁·刑",
                title = "刑太岁（${suiCn}刑${mingCn}）",
                body = "${label}太岁与生年支构成刑。刑主「规矩、口舌、身体」三件事：本年容易与制度、合同、审核、体检正面相逢。" +
                    "宜主动把这三年拖着的检查、补办、对账、清算一次做干净，做完了刑气就散了；" +
                    "忌侥幸心理，忌在票据、证照、时间点上省小钱吃大罚。",
                weight = -5, career = -3, health = -2, wealth = -1,
                notes = listOf(DIM_HEALTH to "刑太岁：本年主动做一次全面体检，旧患宜早处理")
            )
        }
        if (pairOf(ming, sui) in LIU_PO) {
            out += Sig(
                tag = "太岁·破",
                title = "破太岁（${suiCn}破${mingCn}）",
                body = "${label}太岁与生年支相破。破是「坏在细处」：本年不算大凶，但东西容易坏、计划容易漏、约定容易改日期。" +
                    "宜预留缓冲，宜买该买的保修，宜把重要日程放在自己可控的位置。破太岁最省事的过法，是承认「今年事情就是会多一点」。",
                weight = -3, wealth = -2, career = -1, health = -1,
                notes = listOf(DIM_WEALTH to "破太岁：本年维修与替换开支偏多，宜留冗余预算")
            )
        }
        return out
    }

    // ==================================================== 信号十一：大运背景

    private fun dayunSignal(
        dm: Stem,
        chart: BaziChart,
        dayun: Pillar,
        period: Pillar,
        label: String
    ): List<Sig> {
        val out = mutableListOf<Sig>()
        val fav = chart.favorableElements.toSet()
        val unfav = chart.unfavorableElements.toSet()
        val de = dayun.stem.element
        val isFav = de in fav || dayun.branch.element in fav
        val god = runCatching { BaziCalculator.tenGod(dm, dayun.stem).chinese }.getOrDefault("")
        out += Sig(
            tag = "大运背景",
            title = "现行${dayun.display}大运" + if (god.isNotEmpty()) "（$god 当令）" else "",
            body = "十年看大运。你当下行${dayun.display}运，${dayun.stem.chinese}属${elementName(de)}、${dayun.branch.chinese}藏${elementName(dayun.branch.element)}，" +
                if (isFav) "于你命局为喜用，这十年的底色本就偏顺，${label}只要不太过火，都能被大环境接住。"
                else if (de in unfav) "于你命局为忌，这十年做事天然要多费一手，${label}更不宜硬顶；把目标调低半档，反而过得去。"
                else "与你喜忌无大涉，这十年靠的是自己的选择，不是运气。",
            weight = if (isFav) 4 else if (de in unfav) -4 else 0,
            career = if (isFav) 2 else -1, wealth = if (isFav) 1 else -1,
            notes = listOf(DIM_CAREER to "大运${dayun.display}为${if (isFav) "喜用" else "忌"}：${label}是在这条底色上跑的短线")
        )

        val ds = dayun.stem.ordinal
        val db = dayun.branch.ordinal
        val ps = period.stem.ordinal
        val pb = period.branch.ordinal
        if (ds == ps && db == pb) {
            out += Sig(
                tag = "运岁并临",
                title = "${label}干支与大运同字",
                body = "${label}干支与所行大运完全相同，叫「岁运并临」：同一股气叠加三倍，好的极好、坏的极坏，总之不会平静。" +
                    "这周期一切都被放大，宜把重大决定提前或推后，把不可逆的动作拿掉，只留下能重复的事。",
                weight = if (isFav) 4 else -6, career = if (isFav) 2 else -2, health = if (isFav) 0 else -2,
                notes = listOf(DIM_CAREER to "岁运并临：${label}这股气不中庸，宜避免不可逆操作")
            )
        } else if (abs(ds - ps) == 4 && abs(db - pb) == 6) {
            out += Sig(
                tag = "运岁天克地冲",
                title = "${label}克冲所行大运${dayun.display}",
                body = "${label}与你正在走的大运天干相克、地支相冲，叫「运岁天克地冲」——短线与十年底色顶上了。" +
                    "体感就是：明明该顺的事，处处别扭。这周期不宜与体制、上级、长期安排正面对撞，宜等这阵过去再谈。",
                weight = -5, career = -3, wealth = -1, health = -2,
                notes = listOf(DIM_CAREER to "运岁相冲：${label}与十年大势相左，宜蛰伏不宜改道")
            )
        } else if (abs(ds - ps) == 5) {
            out += Sig(
                tag = "运岁天干合",
                title = "${label}之干合大运${dayun.display}之干",
                body = "${label}天干来合你大运之干，主「十年主线被短线牵住」：这周期自然会在大运的主题上多花力气，也容易被那件长期的事占满时间。" +
                    "宜顺势处理主线，忌同时开支线。",
                weight = 3, career = 2,
                notes = listOf(DIM_CAREER to "运岁干合：${label}宜为大运那件事推进一格")
            )
        } else if (abs(db - pb) == 6) {
            out += Sig(
                tag = "运岁地支冲",
                title = "${label}之支冲大运${dayun.display}之支",
                body = "${label}地支冲你所行大运的地支，十年根基被动了一角：居住环境、岗位、长期合作，本周期会显出变化的苗头。" +
                    "主动整比被动改好，至少可以把变的那部分限定在一个范围内。",
                weight = -4, career = -2, wealth = -1, health = -1,
                notes = listOf(DIM_CAREER to "运岁支冲：${label}动的是长线，宜限定变量")
            )
        }
        return out
    }
}
