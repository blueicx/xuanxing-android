package com.xuanji.app.domain.divination

import java.security.MessageDigest
import kotlin.math.floor

/**
 * 阿拉伯占星（Arabic Astrology）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 阿拉伯点（Lots）：15 个传统点位，昼夜公式 ASC ± (A − B)；
 *  - Jarbakhtar 周期：迦勒底顺序 × 行星小年（合计 129 年），自 Almuten 起；
 *  - Tasyir 定向：黄道度数差 ≈ 年数（1° ≈ 1 年）；
 *  - Abjad 字母数值：姓名 → 数值和/数根/模 12 解读。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据 ========================

private val CHALDEAN_ORDER = listOf("土星", "木星", "火星", "太阳", "金星", "水星", "月亮")

private val PLANET_MINOR_YEARS = mapOf("土星" to 30, "木星" to 12, "火星" to 15, "太阳" to 19, "金星" to 8, "水星" to 20, "月亮" to 25)

private val PLANET_ATTRIBUTES = mapOf(
    "土星" to ("大凶" to "限制、时间、命运、责任"),
    "木星" to ("大吉" to "幸运、扩张、智慧、财富"),
    "火星" to ("凶" to "战争、冲突、能量、行动"),
    "太阳" to ("吉" to "权威、生命、自我、荣耀"),
    "金星" to ("大吉" to "爱情、美丽、和谐、艺术"),
    "水星" to ("中性" to "智慧、沟通、商业、学习"),
    "月亮" to ("中性" to "情感、直觉、母亲、潜意识")
)

private val ABJAD_MAP = mapOf(
    'ا' to 1, 'ب' to 2, 'ج' to 3, 'د' to 4, 'ه' to 5, 'و' to 6, 'ز' to 7, 'ح' to 8, 'ط' to 9,
    'ي' to 10, 'ك' to 20, 'ل' to 30, 'م' to 40, 'ن' to 50, 'س' to 60, 'ع' to 70, 'ف' to 80,
    'ص' to 90, 'ق' to 100, 'ر' to 200, 'ش' to 300, 'ت' to 400, 'ث' to 500, 'خ' to 600,
    'ذ' to 700, 'ض' to 800, 'ظ' to 900, 'غ' to 1000
)

private val LATIN_TO_ARABIC = mapOf(
    'a' to 'ا', 'b' to 'ب', 'c' to 'ج', 'd' to 'د', 'e' to 'ه', 'f' to 'ف', 'g' to 'غ',
    'h' to 'ح', 'i' to 'ي', 'j' to 'ج', 'k' to 'ك', 'l' to 'ل', 'm' to 'م', 'n' to 'ن',
    'o' to 'و', 'p' to 'ب', 'q' to 'ق', 'r' to 'ر', 's' to 'س', 't' to 'ت', 'u' to 'و',
    'v' to 'ف', 'w' to 'و', 'x' to 'خ', 'y' to 'ي', 'z' to 'ز'
)

private val SIGNS = listOf("白羊", "金牛", "双子", "巨蟹", "狮子", "处女", "天秤", "天蝎", "射手", "摩羯", "水瓶", "双鱼")

/** 星座守护星（Almuten 简化） */
private val SIGN_RULERS = mapOf(
    0 to "火星", 1 to "金星", 2 to "水星", 3 to "月亮", 4 to "太阳", 5 to "水星",
    6 to "金星", 7 to "火星", 8 to "木星", 9 to "土星", 10 to "土星", 11 to "木星"
)

/** 阿拉伯点公式：(名称, 点A, 点B)，白天 = ASC + A − B，夜晚 = ASC + B − A */
private val ARABIC_PARTS = listOf(
    "幸运点" to ("月亮" to "太阳"),
    "精神点" to ("太阳" to "月亮"),
    "爱情点" to ("金星" to "土星"),
    "商业点" to ("水星" to "太阳"),
    "激情点" to ("火星" to "金星"),
    "胜利点" to ("木星" to "太阳"),
    "死亡点" to ("土星" to "月亮"),
    "父亲点" to ("太阳" to "土星"),
    "母亲点" to ("月亮" to "金星"),
    "婚姻点" to ("金星" to "月亮"),
    "子女点" to ("木星" to "土星"),
    "朋友点" to ("金星" to "水星"),
    "敌人点" to ("火星" to "水星"),
    "疾病点" to ("土星" to "火星"),
    "财富点" to ("木星" to "月亮")
)

private val PART_INTERPRETATION = mapOf(
    "幸运点" to "阿拉伯占星中最重要的点，代表个人的福报、机遇和整体运势走向。",
    "精神点" to "代表个人的精神追求、内在驱动力和人生使命。",
    "爱情点" to "揭示个人的爱情模式、吸引力和亲密关系的发展。",
    "商业点" to "反映个人的商业头脑、财务管理和贸易能力。",
    "激情点" to "代表个人的热情、行动力和竞争意识。",
    "胜利点" to "预示个人在竞争、挑战中获得成功的潜力。",
    "死亡点" to "象征重大的转变、结束和重生，而非字面上的死亡。",
    "父亲点" to "反映与父亲的关系、父系传承和权威形象。",
    "母亲点" to "反映与母亲的关系、母系传承和养育能力。",
    "婚姻点" to "揭示婚姻的质量、伴侣的特质和关系的稳定性。",
    "子女点" to "反映与子女的关系、创造力和传承。",
    "朋友点" to "揭示社交圈、友谊的质量和人际网络。",
    "敌人点" to "反映潜在的冲突、挑战和需要警惕的人际关系。",
    "疾病点" to "揭示健康方面的潜在风险和需要注意的身体信号。",
    "财富点" to "反映财富积累的能力、财运和物质生活。"
)

// ======================== 结果模型 ========================

data class ArabicPartResult(val name: String, val degree: Double, val sign: String, val degInSign: Double, val text: String)

data class ArabicJarbakhtarPeriod(val planet: String, val startYear: Int, val endYear: Int, val years: Int, val omen: String, val symbol: String, val text: String)

data class TasyirResult(val significator: String, val promissor: String, val arc: Double, val years: Double, val months: Double, val text: String)

data class AbjadResult(val original: String, val arabic: String, val total: Int, val digitRoot: Int, val mod12: Int, val text: String)

/** 星盘数据（出生时刻黄经） */
data class ArabicChartData(
    val ascendant: Double, val sun: Double, val moon: Double,
    val mercury: Double, val venus: Double, val mars: Double,
    val jupiter: Double, val saturn: Double, val isDiurnal: Boolean
)

// ======================== 核心计算 ========================

object ArabicAstrology {

    private fun norm(d: Double): Double = ((d % 360) + 360) % 360

    /** 阿拉伯点 */
    fun arabicParts(c: ArabicChartData): List<ArabicPartResult> {
        val pos = mapOf(
            "ASC" to c.ascendant, "太阳" to c.sun, "月亮" to c.moon, "水星" to c.mercury,
            "金星" to c.venus, "火星" to c.mars, "木星" to c.jupiter, "土星" to c.saturn
        )
        return ARABIC_PARTS.map { (name, pair) ->
            val (aName, bName) = pair
            val a = pos[aName] ?: 0.0
            val b = pos[bName] ?: 0.0
            val raw = if (c.isDiurnal) c.ascendant + a - b else c.ascendant + b - a
            val deg = norm(raw)
            val sign = SIGNS[floor(deg / 30).toInt() % 12]
            val base = PART_INTERPRETATION[name] ?: "此点反映个人在该领域的潜能与发展。"
            ArabicPartResult(name, deg, sign, deg % 30, "$base 位于 ${sign}座 ${"%.1f".format(deg % 30)}°。")
        }
    }

    /** Jarbakhtar 周期（自 Almuten 起，迦勒底顺序） */
    fun jarbakhtar(c: ArabicChartData, birthYear: Int): List<ArabicJarbakhtarPeriod> {
        val ascIdx = floor(c.ascendant / 30).toInt() % 12
        val almuten = SIGN_RULERS[ascIdx] ?: "土星"
        val startIdx = CHALDEAN_ORDER.indexOf(almuten).let { if (it < 0) 0 else it }
        val ordered = CHALDEAN_ORDER.drop(startIdx) + CHALDEAN_ORDER.take(startIdx)
        val result = mutableListOf<ArabicJarbakhtarPeriod>()
        var year = birthYear
        ordered.forEach { planet ->
            val years = PLANET_MINOR_YEARS[planet] ?: 0
            val (omen, symbol) = PLANET_ATTRIBUTES[planet] ?: ("未知" to "")
            val base = "$planet 周期从 $year 年开始，持续 $years 年。"
            val extra = when (omen) {
                "大吉" -> "这是人生中的黄金时期，$symbol 方面的机遇将大量涌现。"
                "吉" -> "这一时期有利于 $symbol 方面的发展。"
                "凶" -> "这一时期需要特别关注 $symbol 方面的挑战。"
                "大凶" -> "这是需要谨慎和忍耐的时期，$symbol 方面可能面临考验。"
                else -> "这一时期在 $symbol 方面呈现平衡发展的态势。"
            }
            result.add(ArabicJarbakhtarPeriod(planet, year, year + years - 1, years, omen, symbol, base + extra))
            year += years
        }
        return result
    }

    /** Tasyir 定向 */
    fun tasyir(c: ArabicChartData, sig: String, pro: String): TasyirResult {
        val pos = mapOf(
            "ASC" to c.ascendant, "太阳" to c.sun, "月亮" to c.moon, "水星" to c.mercury,
            "金星" to c.venus, "火星" to c.mars, "木星" to c.jupiter, "土星" to c.saturn
        )
        val a = pos[sig] ?: 0.0
        val b = pos[pro] ?: 0.0
        val arc = norm(b - a)
        val months = (arc % 1) * 12
        val timing = when {
            arc < 30 -> "近期（1-30年内）"
            arc < 60 -> "中期（30-60年内）"
            else -> "远期（60年以上）"
        }
        val text = "从 $sig 到 $pro 的定向角度为 ${"%.1f".format(arc)}°，对应$timing。这预示着在约 ${"%.1f".format(arc)} 年后，与 $pro 相关领域将迎来重要发展。"
        return TasyirResult(sig, pro, arc, arc, months, text)
    }

    /** 姓名 → 阿拉伯字母 */
    fun toArabic(name: String): String {
        if (name.any { it in '\u0600'..'\u06FF' }) return name
        val sb = StringBuilder()
        for (ch in name.lowercase()) LATIN_TO_ARABIC[ch]?.let { sb.append(it) }
        return sb.toString()
    }

    /** Abjad 数值 */
    fun abjad(name: String): AbjadResult {
        val arabic = toArabic(name)
        val total = arabic.sumOf { ABJAD_MAP[it] ?: 0 }
        var root = total
        while (root >= 10) root = root.toString().sumOf { it - '0' }
        val mod12 = total % 12
        val scope = when {
            total < 100 -> "基础层面"
            total < 500 -> "个人层面"
            total < 1000 -> "社会层面"
            else -> "宇宙层面"
        }
        val rootMeanings = mapOf(
            1 to "领导力、独立、开创", 2 to "合作、平衡、外交", 3 to "创造、表达、社交",
            4 to "稳定、务实、秩序", 5 to "自由、冒险、变革", 6 to "责任、服务、和谐",
            7 to "智慧、内省、灵性", 8 to "力量、财富、权威", 9 to "慈悲、奉献、完成"
        )
        val sign = SIGNS[mod12 % 12]
        val text = "总数值为 $total，属于$scope。数根为 $root，象征${rootMeanings[root] ?: "未知"}。模12为 $mod12，对应${sign}座的能量特质。"
        return AbjadResult(name, arabic, total, root, mod12, text)
    }

    /** 六维解读：总评 + 事业/财运/感情/健康/行动建议，贴合阿拉伯点（Lots）主题 */
    fun buildVerdict(c: ArabicChartData): String {
        val parts = arabicParts(c)
        fun part(name: String): ArabicPartResult = parts.first { it.name == name }
        val fortune = part("幸运点")
        val spirit = part("精神点")
        val business = part("商业点")
        val victory = part("胜利点")
        val wealth = part("财富点")
        val love = part("爱情点")
        val marriage = part("婚姻点")
        val sickness = part("疾病点")
        val sb = StringBuilder()
        sb.append("总评：幸运点落${fortune.sign}座，与精神点（${spirit.sign}座）彼此呼应，整体福泽可期；吉星顺位则诸事多有贵人襄助，可依天时而动。")
        sb.append("事业：商业点居${business.sign}座，主谋略与贸易之才；胜利点落${victory.sign}座，竞争场合宜主动布局、以智取胜，声望随实力渐长。")
        sb.append("财运：幸运点为福报之源，财富点落${wealth.sign}座，正财偏财皆有通路，宜以技艺与人脉双线积累，忌贪快求横财。")
        sb.append("感情：爱情点落${love.sign}座，吸引力与魅力由此而生；婚姻点居${marriage.sign}座，关系宜以坦诚与包容筑基，忌以猜疑磨损情分。")
        sb.append("健康：疾病点落${sickness.sign}座，提示该星座对应的部位与情绪易有波动，宜定期调养、劳逸结合，顺应天时作息。")
        sb.append("建议：择时而行是阿拉伯占星的精髓，重要决策可对照幸运点与胜利点方位顺势推进；持吉星般的慷慨与智慧，谦逊守正，福运自聚。")
        sb.append("（阿拉伯占星源自中古星占传统，结果仅供文化娱乐参考）")
        return sb.toString()
    }
}
