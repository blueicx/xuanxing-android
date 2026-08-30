package com.xuanji.app.domain

import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneInsight
import com.xuanji.app.domain.ZodiacCalculator.SkySnapshot
import com.xuanji.app.domain.ZodiacCalculator.NatalChart
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.math.min

/** 西方周期读数的结构化结果，形状与东方读数对齐，便于综合页融合。 */
data class WesternReading(
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
    val luckySign: String
)

/**
 * 西方（回归占星）行运解盘器。
 *
 * 真实星历（[ZodiacCalculator.skyAt]）× 本命盘 = 行运相位。
 * 每个相位按「容许度越紧越有力」计分，并展开成一段落在具体人生领域的话：
 * 行星本性 × 被触发的本命行星 × 所落宫位 × 相位机制。
 *
 * 逆行、月相、行运合上升/天顶一并计入，全部可追溯。
 */
object WesternTransitReader {

    const val DIM_CAREER = "career"
    const val DIM_WEALTH = "wealth"
    const val DIM_LOVE = "love"
    const val DIM_STUDY = "study"
    const val DIM_HEALTH = "health"

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
        val notes: List<Pair<String, String>> = emptyList(),
        val days: Int = 1
    )

    /** 行星性格：nature=它是什么力，area=它管哪块人事 */
    private data class PChar(val nature: String, val area: String, val malefic: Boolean)

    private val PCHARS: Map<String, PChar> = mapOf(
        "太阳" to PChar("太阳是「我要成为谁」的那股力，要的是被看见、被承认", "自我、意志、上位者与你的事业主轴", false),
        "月亮" to PChar("月亮是你的情绪体与身体节拍，要的是安全与被接住", "情绪、家宅、母亲与身体的实际需求", false),
        "水星" to PChar("水星是念头与话，要的是把东西说清楚、传出去", "沟通、文书、学习、短途与交易细节", false),
        "金星" to PChar("金星是趋近与喜好，要的是美、关系与值得", "感情、审美、合作、进财与享受", false),
        "火星" to PChar("火星是欲望与攻击性，要的是立刻、现在、我说了算", "行动、竞争、性、冲突与体力输出", true),
        "木星" to PChar("木星是扩张，它不挑方向，只会把你已有的东西放大", "机会、钱、法律、远方与信念", false),
        "土星" to PChar("土星是结构与时间，它只认代价与责任，也只在承担里给东西", "职位、规矩、长期、衰老与真实的成就", true),
        "天王星" to PChar("天王星是断裂与自由，它不允许任何东西继续靠惯性运转", "突变、独立、科技、群体与例外", true),
        "海王星" to PChar("海王星是溶解与想象，它把边界模糊掉——好的坏的都糊", "灵感、共情、欺骗、嗜好与超越", true),
        "冥王星" to PChar("冥王星是权力的真相，它逼你面对「我到底在控制什么」", "生死、深度、共有资源、清算与重生", true),
        "北交" to PChar("北交是这一世要走去的方向，它不给舒适，只给方向感", "成长课题、人生走向与必须练的能力", false)
    )

    /** 本命行星 / 轴点被触发时对应的人生领域 */
    private val NATAL_AREA: Map<String, String> = mapOf(
        "太阳" to "你的自我与事业主轴",
        "月亮" to "你的情绪、家宅与身体",
        "水星" to "你的表达、思路与文书往来",
        "金星" to "你的感情、合作与钱财",
        "火星" to "你的行动力、脾气与体力",
        "木星" to "你的机会、格局与进财方式",
        "土星" to "你的责任、位置与长期积累",
        "天王星" to "你的独立性与生活方式",
        "海王星" to "你的感受力、想象与防线",
        "冥王星" to "你的控制欲与深层恐惧",
        "北交" to "你这辈子要走去的方向",
        "上升" to "你这个人本身、你的门面与身体",
        "天顶" to "你的事业定位与公众形象"
    )

    /** 宫位：事情发生在哪个场地 */
    private val HOUSE_TXT = listOf(
        "", "第一宫（自身形象、身体、你给人的第一印象）", "第二宫（钱财、进账、价值观）",
        "第三宫（沟通、学习、兄弟邻里、短途）", "第四宫（家宅、根基、内心归属、晚年）",
        "第五宫（恋爱、子女、创作、赌性与玩乐）", "第六宫（日常工作、服务、身体与健康细节）",
        "第七宫（伴侣、合伙、合同与公开的对手）", "第八宫（共有资源、债务、深度与转化）",
        "第九宫（远方、高学、法律、信念与传播）", "第十宫（事业、名位、上司与公众评价）",
        "第十一宫（朋友圈、社群、愿景与来自群体的机会）", "第十二宫（幕后、休养、旧账与看不见的力量）"
    )

    /** 相位机制 */
    private data class AChar(val label: String, val mechanic: String, val base: Int)

    private val ASPECTS = listOf(
        AChar("合", "两股力拧成一股，同频共振，会把彼此都推到极端", 0),
        AChar("六合", "机会是真的，但它是半开的门，你伸手才有", 4),
        AChar("刑", "两种需求互相拉扯，非要谈判不可——摩擦来自内部，不全是外面", -6),
        AChar("拱", "省力。这股力天生配合，你几乎不用努力就能顺走", 6),
        AChar("冲", "对面把你推到对立位上，事情会以「别人/外境的要求」的形式出现", -8)
    )

    private val ANGLE = mapOf("合" to 0.0, "六合" to 60.0, "刑" to 90.0, "拱" to 120.0, "冲" to 180.0)

    /** 容许度：走得快的行星给宽，走得慢的给窄（外行星一年才动 1°） */
    private val ORB: Map<String, Double> = mapOf(
        "月亮" to 7.0, "太阳" to 5.0, "水星" to 4.5, "金星" to 4.5, "火星" to 4.5,
        "木星" to 3.0, "土星" to 2.8, "天王星" to 2.0, "海王星" to 1.8, "冥王星" to 1.6,
        "北交" to 2.0
    )

    /** 行运行星落在哪一维上着力最重 */
    private val DIMS: Map<String, Map<String, Int>> = mapOf(
        "太阳" to mapOf(DIM_CAREER to 3, DIM_HEALTH to 2),
        "月亮" to mapOf(DIM_LOVE to 3, DIM_HEALTH to 2),
        "水星" to mapOf(DIM_STUDY to 3, DIM_CAREER to 2),
        "金星" to mapOf(DIM_LOVE to 3, DIM_WEALTH to 2),
        "火星" to mapOf(DIM_CAREER to 2, DIM_HEALTH to 3),
        "木星" to mapOf(DIM_WEALTH to 3, DIM_CAREER to 2),
        "土星" to mapOf(DIM_CAREER to 3, DIM_HEALTH to 2),
        "天王星" to mapOf(DIM_CAREER to 2, DIM_STUDY to 2),
        "海王星" to mapOf(DIM_STUDY to 2, DIM_LOVE to 2),
        "冥王星" to mapOf(DIM_CAREER to 2, DIM_WEALTH to 2),
        "北交" to mapOf(DIM_CAREER to 2, DIM_STUDY to 2)
    )

    private val SIGNS = listOf(
        "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座",
        "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"
    )
    /** 占星四元素 → 五行映射，只为把「幸运色/方位」接到既有的色系上 */
    private val SIGN_TO_WU_XING = listOf(
        Element.FIRE, Element.EARTH, Element.WOOD, Element.WATER, Element.FIRE, Element.EARTH,
        Element.WOOD, Element.WATER, Element.FIRE, Element.EARTH, Element.METAL, Element.WATER
    )

    /** 换座观察名单：太阳、月亮的换座由月相与节气覆盖，不在这里重复 */
    private val INGRESS_WATCH = listOf("水星", "金星", "火星", "木星", "土星", "天王星", "海王星", "冥王星")

    fun signIndexOf(name: String): Int = SIGNS.indexOf(name.removeSuffix("座") + "座").let { if (it < 0) 0 else it }
    private fun signOf(lon: Double): String = SIGNS[(((floor(lon / 30.0).toInt()) % 12) + 12) % 12]
    private fun degreeOf(lon: Double): Int = floor(lon % 30.0).toInt()

    // ==================================================== 主入口

    /**
     * @param natal 本命盘；为空时退化到「太阳星座中点」，只论日盘行星与本命太阳的相位
     * @param dates 本周期实际抽样的日期（日=1 天，周=7 天，月/年=整段窗口）
     */
    fun read(
        natal: NatalChart?,
        sunSign: String,
        dates: List<LocalDate>,
        label: String
    ): WesternReading {
        require(dates.isNotEmpty())
        val targets = natalTargets(natal, sunSign)
        val collected = LinkedHashMap<String, MutableList<Pair<Sig, LocalDate>>>()
        var bestLunation: Pair<Int, Hit>? = null        // 本窗口内最精确的一次朔/望
        // 整段窗口只解一次星历（前面多带一天，专门用来发现换座发生在窗口起点当天）
        val probe = (listOf(dates.first().minusDays(1)) + dates).map { it to ZodiacCalculator.skyAt(it) }
        val skies = probe.drop(1)
        val spanDays = (dates.last().toEpochDay() - dates.first().toEpochDay()).toInt() + 1

        skies.forEach { (date, sky) ->
            val sep = sky.separation("太阳", "月亮")
            val newMoonErr = min(sep, 360.0 - sep)
            val fullMoonErr = abs(newMoonErr - 180.0)
            val exactness = ceil(100.0 * (1.0 - min(newMoonErr, fullMoonErr) / 7.0)).toInt()
            if (newMoonErr < 7.0 || fullMoonErr < 7.0) {
                val hit = lunationHit(newMoonErr < fullMoonErr, exactness, date, label)
                if (bestLunation == null || exactness > bestLunation!!.first) bestLunation = exactness to hit
            }
            skySnapshotSignals(sky, label).forEach {
                collected.getOrPut(it.key) { mutableListOf() }.add(it.sig to date)
            }
            transitSignals(sky, targets, date, label, natal != null).forEach {
                collected.getOrPut(it.key) { mutableListOf() }.add(it.sig to date)
            }
        }
        ingressSignals(probe, label, spanDays).forEach { (key, sig, date) ->
            collected.getOrPut(key) { mutableListOf() }.add(sig to date)
        }
        bestLunation?.let { (_, hit) ->
            collected.getOrPut(hit.key) { mutableListOf() }.add(hit.sig to dates.first())
        }

        return assemble(collected, label, dates.size, spanDays, skies[skies.size / 2].second, hasNatal = natal != null)
    }

    /** 采样日期序列：日 / 周（锚周一）/ 月（整月）/ 年（整年每 step 天） */
    fun sampleDates(date: LocalDate, period: String): List<LocalDate> = when (period) {
        "week" -> {
            val monday = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            (0L..6L).map { monday.plusDays(it) }
        }
        "month" -> (1..date.lengthOfMonth()).map { date.withDayOfMonth(it) }
        "year" -> {
            val first = LocalDate.of(date.year, 1, 1)
            val last = LocalDate.of(date.year, 12, 31)
            val total = last.toEpochDay() - first.toEpochDay() + 1
            val step = 4L
            (0 until ceil(total / step.toDouble()).toInt()).map { first.plusDays((it * step).toLong()) }
                .filter { !it.isAfter(last) }
        }
        else -> listOf(date)
    }

    // ==================================================== 内部：命中记录

    private data class Hit(val key: String, val sig: Sig)

    private data class NatalTarget(val name: String, val longitude: Double, val house: Int)

    /**
     * 本命受体。没有精确出生资料时，退到「太阳星座中点」，
     * 并在解说里如实说明这是粗略版。
     */
    private fun natalTargets(natal: NatalChart?, sunSign: String): List<NatalTarget> {
        if (natal == null) {
            return listOf(NatalTarget("太阳", signIndexOf(sunSign) * 30.0 + 15.0, 0))
        }
        val out = natal.planets.mapTo(mutableListOf()) { NatalTarget(it.name, it.longitude, it.house) }
        out += NatalTarget("上升", natal.ascendant, 1)
        out += NatalTarget("天顶", natal.midheaven, 10)
        return out
    }

    /** 行运行星 × 本命受体：按容许度决定力度，越紧越有力。 */
    private fun transitSignals(
        sky: SkySnapshot,
        targets: List<NatalTarget>,
        date: LocalDate,
        label: String,
        hasNatal: Boolean
    ): List<Hit> {
        val out = mutableListOf<Hit>()
        PCHARS.keys.forEach { tp ->
            val tLon = sky.lon(tp)
            val maxOrb = ORB.getValue(tp)
            targets.forEach { tg ->
                val diff = abs(tLon - tg.longitude) % 360.0
                val sep = if (diff > 180.0) 360.0 - diff else diff
                ASPECTS.forEach { asp ->
                    val err = abs(sep - ANGLE.getValue(asp.label))
                    if (err > maxOrb) return@forEach
                    val exactness = (1.0 - err / maxOrb).coerceIn(0.25, 1.0)
                    out += aspectHit(tp, tg, asp.label, exactness, sky, date, label, hasNatal)
                }
            }
        }
        return out
    }

    private fun aspectHit(
        tp: String,
        tg: NatalTarget,
        aspect: String,
        exactness: Double,
        sky: SkySnapshot,
        date: LocalDate,
        label: String,
        hasNatal: Boolean
    ): Hit {
        val pc = PCHARS.getValue(tp)
        val conj = aspect == "合"
        val base = when {
            conj -> if (pc.malefic) -5 else 6
            else -> aspBase(aspect)
        }
        // 逆行中的行星力度打折但不消失；外行星本就长期施力
        val w = (base * exactness).toInt()
        val tSign = signOf(sky.lon(tp))
        val retro = tp in sky.retrograde
        val area = NATAL_AREA.getValue(tg.name)
        val houseTxt = if (hasNatal && tg.house in 1..12) "，落在${tg.name}所在的${HOUSE_TXT[tg.house]}" else ""
        val where = if (hasNatal) "$tSign${degreeOf(sky.lon(tp))}°" else tSign

        val title = "行运${tp}${symbolOf(tp)}${aspect}本命${tg.name}${symbolOf(tg.name)}"
        val body = buildString {
            append("${label}行运${tp}走到$where（${if (retro) "逆行中，" else ""}${signOf2(tSign)}）")
            if (hasNatal) {
                append("，与你的本命${tg.name}（${signOf(natalLon(tg, sky))}${degreeOf(natalLon(tg, sky))}°）构成${aspect}")
            } else {
                append("，与你的太阳星座构成${aspect}")
            }
            append("。${pc.nature}，${aspectMechanic(aspect)}。")
            append("被触发的是${area}$houseTxt。")
            append(adviceFor(tp, aspect, pc.malefic))
        }
        val dims = DIMS.getValue(tp)
        val sign0 = if (aspect == "拱" || aspect == "六合" || (conj && !pc.malefic)) 1 else if (aspect == "刑" || aspect == "冲" || (conj && pc.malefic)) -1 else 0
        val dir = if (sign0 == 0) (if (w >= 0) 1 else -1) else sign0
        val notes = mutableListOf<Pair<String, String>>()
        dims.keys.forEach { d ->
            if (dims.getValue(d) >= 2) {
                notes += d to "行运$tp${aspect}本命${tg.name}：${briefFor(tp, aspect)}"
            }
        }
        return Hit(
            key = "$tp|$tg.name|$aspect",
            sig = Sig(
                tag = "行运$aspect",
                title = title,
                body = body,
                weight = w,
                career = scaled(dims[DIM_CAREER], dir, exactness),
                wealth = scaled(dims[DIM_WEALTH], dir, exactness),
                love = scaled(dims[DIM_LOVE], dir, exactness),
                study = scaled(dims[DIM_STUDY], dir, exactness),
                health = scaled(dims[DIM_HEALTH], dir, exactness),
                notes = notes,
                days = 1
            )
        )
    }

    private fun natalLon(tg: NatalTarget, sky: SkySnapshot): Double = tg.longitude

    private fun aspBase(a: String): Int = ASPECTS.first { it.label == a }.base

    private fun scaled(v: Int?, dir: Int, exactness: Double): Int =
        if (v == null) 0 else (v * dir * exactness).toInt()

    private fun signOf2(sign: String): String = sign.removeSuffix("座") + "座"

    private fun symbolOf(name: String): String = when (name) {
        "太阳" -> "☉"; "月亮" -> "☽"; "水星" -> "☿"; "金星" -> "♀"; "火星" -> "♂"
        "木星" -> "♃"; "土星" -> "♄"; "天王星" -> "⛢"; "海王星" -> "♆"; "冥王星" -> "♇"
        "北交" -> "☊"; "上升" -> "ASC"; "天顶" -> "MC"; else -> ""
    }

    // ==================================================== 内部：天空本身的状态

    /** 逆行：与本命无关、但确实改变了这一段气候的客观状态。 */
    private fun skySnapshotSignals(sky: SkySnapshot, label: String): List<Hit> {
        val out = mutableListOf<Hit>()
        sky.retrograde.forEach { name ->
            val pc = PCHARS.getValue(name)
            out += Hit(
                key = "$name|逆行",
                sig = Sig(
                    tag = "逆行",
                    title = "${name}${symbolOf(name)}逆行",
                    body = "$label${name}在${signOf(sky.lon(name))}逆行。${pc.nature}——逆行不是「倒霉」，是这股力改成往里走：" +
                        adviceFor(name, "逆", pc.malefic),
                    weight = if (name == "水星") -4 else if (name == "金星") -2 else -3,
                    career = if (name == "水星") -3 else -1,
                    study = if (name == "水星") -3 else 0,
                    wealth = if (name == "金星" || name == "水星") -2 else 0,
                    health = if (name == "火星") -2 else 0,
                    notes = listOf(DIM_CAREER to "${name}逆行：${briefFor(name, "逆")}"),
                    days = 1
                )
            )
        }
        return out
    }

    /**
     * 换座：直接在已经解好的采样空位上比较相邻两次的星座归属，零额外星历开销。
     * 顺行入新宫与逆行退回旧宫分开表述，因为二者在实际体感上完全不是一回事。
     * 窗口越长，只看越慢的行星——年度解读里水星换十二次座没有任何信息量。
     */
    private fun ingressSignals(
        probe: List<Pair<LocalDate, SkySnapshot>>,
        label: String,
        spanDays: Int
    ): List<Triple<String, Sig, LocalDate>> {
        val watch = when {
            spanDays > 100 -> listOf("木星", "土星", "天王星", "海王星", "冥王星")
            spanDays > 31 -> listOf("火星", "木星", "土星", "天王星", "海王星", "冥王星")
            else -> INGRESS_WATCH
        }
        val out = mutableListOf<Triple<String, Sig, LocalDate>>()
        if (probe.size < 2) return out
        watch.forEach { name ->
            val pc = PCHARS.getValue(name)
            for (i in 1 until probe.size) {
                val from = signOf(probe[i - 1].second.lon(name))
                val to = signOf(probe[i].second.lon(name))
                if (from == to) continue
                val date = probe[i].first
                val forward = ((signIndexOf(to) - signIndexOf(from) + 12) % 12) <= 6
                val field = HOUSE_TXT[houseOfSign(to)].substringAfter("（").substringBefore("）")
                val phrase = if (forward) {
                    "${name}${symbolOf(name)}顺行入$to。${pc.area}换了一个场地，接下来这股力会持续落在「$field」上，而不是过去那一处"
                } else {
                    "${name}${symbolOf(name)}逆行退回$to。旧场地、「$field」里没处理完的那件事会被重新摆回台面，这是补做，不是重新开始"
                }
                out += Triple(
                    "$name|换座|$to|$date",
                    Sig(
                        tag = "换座",
                        title = "$date ${name}入${to.removeSuffix("座")}",
                        body = "$label$phrase。换座前后两三天体感最明显：老问题忽然失去力气，新麻烦忽然找上门。" +
                            adviceFor(name, if (forward) "拱" else "逆", pc.malefic),
                        weight = if (forward) (if (pc.malefic) -2 else 2) else -1,
                        career = if (forward) (if (pc.malefic) -1 else 1) else -1,
                        study = if (forward) 0 else 1,
                        notes = listOf(
                            DIM_CAREER to "${name}入${to.removeSuffix("座")}：主战场转向$field"
                        ),
                        days = 1
                    ),
                    date
                )
            }
        }
        return out
    }

    /** 某星座大致对应的人生场地（无本命宫位时的替代说法） */
    private fun houseOfSign(sign: String): Int = signIndexOf(sign) + 1

    private fun lunationHit(isNew: Boolean, exactness: Int, date: LocalDate, label: String): Hit {
        return Hit(
            key = "月相|${if (isNew) "朔" else "望"}",
            sig = Sig(
                tag = "月相",
                title = if (isNew) "朔日（新月）$date" else "望日（满月）$date",
                body = if (isNew) {
                    "${label}前后恰逢朔日（${date}），日月同宫，是天然的起点：这套天象利于立意、开局、定周期目标，" +
                        "不利于收尾与结算。新月许的愿要小、要具体，最好是一件一个月内能验证的事。"
                } else {
                    "${label}前后恰逢望日（${date}），日月相对，是天然的兑现与了结：做没做成的事在这一段会露出结果，" +
                    "关系里藏着的话也容易被翻出来。满月宜放不宜起——该结束的别拖到下一个月相周期。"
                },
                weight = if (isNew) 2 else 1,
                love = if (isNew) 0 else 2,
                career = if (isNew) 2 else 0,
                notes = listOf(DIM_CAREER to if (isNew) "朔日：利于开局与立志" else "望日：利于兑现与结束")
            )
        )
    }

    // ==================================================== 内部：文案零件

    private fun aspectMechanic(a: String): String = when (a) {
        "合" -> "合相把两股力并成一股东西，你不辨认它，就会以为那就是自己"
        "六合" -> "六合给的是门，不是路，得你先走过去推一下"
        "刑" -> "刑给的是非做不可却不想做的事，拖着它就会以内部摩擦的形式反复出现"
        "拱" -> "拱给的是天赋与省力，代价是你容易当成理所当然，用到过期"
        "冲" -> "冲给的是对面来的要求，你要么谈判，要么被它推着走"
        else -> "这是一股需要具体辨认的作用力"
    }

    private val EASE = mapOf(
        "太阳" to "这段时间适合露面、递方案、找能拍板的人当面说，成果会被看见。",
        "月亮" to "情绪是稳的，家里和身体的事可以一并处理掉，也是谈心的好时机。",
        "水星" to "写作、谈判、考试、发布都事半功倍，把最费嘴的事排在这几天。",
        "金星" to "关系与钱都偏软，想约的人约，该要的钱要，谈价有加分。",
        "火星" to "体力与胆量都在点上，把最难啃的一块直接啃掉，别浪费在琐事上。",
        "木星" to "这是可以放大的一刻：加预算、加人手、扩范围，往大了想。",
        "土星" to "土星给顺的方式是积累兑现：把长期该补的课补上，它会以职位与口碑的形式还你。",
        "天王星" to "适合破例与换代：新工具、新路子、新合作，此刻试错成本最低。",
        "海王星" to "想象力与共情力在线，做内容、做设计、做疗愈的事最合适。",
        "冥王星" to "有力量做深层清理：改结构、断依赖、把长期失控的地方拿回手里。",
        "北交" to "这一步虽不舒服但方向对，往那儿走的每一格都算数。"
    )

    private val HARD = mapOf(
        "太阳" to "不宜硬要名分与承认，把姿态放低一档，事情反而过得去；重要面谈尽量别排在这几天。",
        "月亮" to "情绪会先于事实说话，家里与身体的需求别压着；先照顾状态，再谈判断。",
        "水星" to "话会被听歪，文件会有错漏。一切落纸、复述一遍、留记录，签约能延就延。",
        "金星" to "别把暧昧当承诺，也别在这几天做大额消费与情感决定；钱要一分一分算。",
        "火星" to "火气会把小事顶成大事。把强度放到体力输出上，别放到争执上；注意磕碰与车速。",
        "木星" to "最容易高估自己的一刻：预算、承诺、规模一律先减两成再签字。",
        "土星" to "压力来自真实的欠缺，不是错觉。别逃，把缺的那块补上，同时允许自己慢。",
        "天王星" to "突发会打乱计划，别在震惊时做决定；先给系统、合同与出行留一份备份。",
        "海王星" to "边界易糊：模糊的条款、说不清的关系、过度的同情都会耗你。要证据，不要感觉。",
        "冥王星" to "有权力斗争的味道。别逼对方认输，也别交出底牌；把注意力收回到自己的事上。",
        "北交" to "这段会把你推向不熟的方向，抗拒最耗力气，先接一小步。"
    )

    private val BRIEF = mapOf(
        "太阳" to "自我与上位者的课题被激活",
        "月亮" to "情绪与家宅需求上浮",
        "水星" to "沟通文书是主战场",
        "金星" to "感情与钱同时被点名",
        "火星" to "行动力与冲突一起升高",
        "木星" to "机会与开销同步放大",
        "土星" to "责任压下来，也在沉淀",
        "天王星" to "变动随时可能落地",
        "海王星" to "感受放大，判断易糊",
        "冥王星" to "深层与控制权被触及",
        "北交" to "成长方向被推了一步"
    )

    private fun adviceFor(tp: String, aspect: String, malefic: Boolean): String {
        val hard = aspect == "刑" || aspect == "冲" || aspect == "逆" || (aspect == "合" && malefic)
        return if (hard) HARD[tp] ?: "" else EASE[tp] ?: ""
    }

    private fun briefFor(tp: String, aspect: String): String = "${BRIEF[tp] ?: ""}（${aspect}）"

    private val FAST_MOVERS = setOf("月亮", "太阳", "水星", "金星", "火星")

    /** 从命中 key 里取出行星名。key 形状统一为：行星|…  （朔望例外，它不属于任何一颗行星） */
    private fun planetOf(key: String): String = key.substringBefore("|")
    private fun isRetroKey(key: String): Boolean = key.endsWith("|逆行")
    private fun isIngressKey(key: String): Boolean = key.contains("|换座|")
    private fun isLunationKey(key: String): Boolean = key.startsWith("月相")

    private fun isBackdrop(key: String): Boolean {
        if (isLunationKey(key)) return false
        val p = planetOf(key)
        return p !in FAST_MOVERS
    }

    // ==================================================== 汇总成文

    private fun aggregate(hits: List<Triple<String, Sig, Boolean>>, sel: (Sig) -> Int): Int {
        val fast = hits.filter { !it.third }.map { sel(it.second) }
        val slow = hits.filter { it.third }.map { sel(it.second) }
        val f = if (fast.isEmpty()) 0 else fast.average().toInt()
        return (f + slow.sum()).coerceIn(-18, 18)
    }

    /** 星象摘要：只报客观事实，作为总评第一句 */
    private fun skyDigest(hits: List<Triple<String, Sig, Boolean>>): String {
        val retro = hits.filter { isRetroKey(it.first) }.map { planetOf(it.first) }
        val ingress = hits.filter { isIngressKey(it.first) }.map { planetOf(it.first) }
        val lunation = hits.firstOrNull { isLunationKey(it.first) }
        val strong = hits.filter {
            abs(it.second.weight) >= 5 && !isRetroKey(it.first) &&
                !isIngressKey(it.first) && !isLunationKey(it.first)
        }
        val parts = mutableListOf<String>()
        if (retro.isNotEmpty()) parts += "${retro.joinToString("、")}处于逆行"
        if (ingress.isNotEmpty()) parts += "${ingress.joinToString("、")}换座"
        lunation?.let { parts += it.second.title }
        parts += if (strong.isEmpty()) "天空相对平静，无显著行运相位"
        else "另有 ${strong.size} 组行运相位与本命盘发生作用"
        return parts.joinToString("；")
    }

    private fun assemble(
        collected: LinkedHashMap<String, MutableList<Pair<Sig, LocalDate>>>,
        label: String,
        samples: Int,
        spanDays: Int,
        sky: SkySnapshot,
        hasNatal: Boolean
    ): WesternReading {
        val hits = collected.entries.map { (key, occ) ->
            val best = occ.maxByOrNull { abs(it.first.weight) }!!.first
            val days = occ.map { it.second }.distinct().size.coerceAtLeast(1)
            // 逆行这类持续状态按「占了几分之几天」折算，避免长窗口把它算成几十次
            val w = if (isRetroKey(key)) {
                (best.weight.toDouble() * days / samples).toInt().coerceAtLeast(-6)
            } else best.weight
            Triple(key, best.copy(weight = w, days = days), isBackdrop(key))
        }

        // 总分：外行星等慢速作用累加为背景，个人行星取均值，再放宽到 ±32
        val fastW = hits.filter { !it.third }.map { it.second.weight }
        val slowW = hits.filter { it.third }.map { it.second.weight }
        val overallDelta = ((if (fastW.isEmpty()) 0 else fastW.average().toInt()) + slowW.sum()).coerceIn(-32, 32)
        val careerDelta = aggregate(hits) { it.career }
        val wealthDelta = aggregate(hits) { it.wealth }
        val loveDelta = aggregate(hits) { it.love }
        val studyDelta = aggregate(hits) { it.study }
        val healthDelta = aggregate(hits) { it.health }

        val ranked = hits.sortedByDescending {
            abs(it.second.weight) * (if (it.third) it.second.days.coerceAtMost(samples) else 1)
        }
        val insights = ranked.filter { it.second.weight != 0 }.take(6).map {
            FortuneInsight("西方·${it.second.tag}", it.second.title, it.second.body, it.second.weight)
        }
        val noteMap = LinkedHashMap<String, MutableList<String>>()
        ranked.forEach { h ->
            h.second.notes.forEach { (k, text) ->
                val b = noteMap.getOrPut(k) { mutableListOf() }
                if (b.size < 3 && !b.contains(text)) b.add(text)
            }
        }

        val lead = ranked.firstOrNull { abs(it.second.weight) >= 4 }
        val headline = if (lead == null) {
            "${label}天空未见紧要相位，按日常节奏推进即可"
        } else {
            "${label}天象以「${lead.second.title}」为主调"
        }
        val bandText = when {
            overallDelta >= 16 -> "这是一段外面在帮你的时候"
            overallDelta >= 7 -> "整体偏顺，主动一步就有反馈"
            overallDelta >= -6 -> "中性天象，靠的是你自己的安排"
            overallDelta >= -15 -> "外境确有摩擦，宜减事、宜慢半拍"
            else -> "天象拧着你走，先保根本，别开新局"
        }
        val summary = buildString {
            append("${label}实算 ${spanDays} 天真实星历：${skyDigest(hits)}。")
            if (!hasNatal) append("这里只比到太阳星座一层，给你一个大方向；补上精确出生时间与地点后会细到本命行星与宫位。")
            append("$bandText。")
            lead?.let { append(it.second.body) }
            ranked.drop(1).firstOrNull { abs(it.second.weight) >= 5 }?.let { append(it.second.body) }
        }
        val advice = buildString {
            val tops = ranked.filter { abs(it.second.weight) >= 4 }.take(3)
            if (tops.isEmpty()) {
                append("${label}不必刻意加减，正常安排即可。")
            } else {
                tops.forEachIndexed { i, h ->
                    append(if (i == 0) "${label}最要紧的是「${h.second.title}」" else "，其次是「${h.second.title}」")
                }
                append("。")
                append(adviceFrom(planetOf(tops.first().first), tops.first().second.weight >= 0))
            }
        }
        val luckySign = ranked.firstOrNull { it.second.weight > 0 }
            ?.let { signOfSky(it.first, sky) } ?: signOf(sky.lon("太阳"))

        return WesternReading(
            headline = headline,
            summary = summary,
            advice = advice,
            insights = insights,
            notes = noteMap,
            overallDelta = overallDelta,
            careerDelta = careerDelta,
            wealthDelta = wealthDelta,
            loveDelta = loveDelta,
            studyDelta = studyDelta,
            healthDelta = healthDelta,
            luckyElement = SIGN_TO_WU_XING[signIndexOf(luckySign)],
            luckySign = luckySign
        )
    }

    private fun signOfSky(key: String, sky: SkySnapshot): String {
        val parts = key.split("|")
        return if (parts.size > 2 && parts[1] == "换座") parts[2] else signOf(sky.lon(planetOf(key)))
    }

    private fun adviceFrom(tp: String, positive: Boolean): String =
        if (positive) EASE[tp] ?: "" else HARD[tp] ?: ""
}
