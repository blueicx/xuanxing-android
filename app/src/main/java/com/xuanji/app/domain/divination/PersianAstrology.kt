package com.xuanji.app.domain.divination

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor

/**
 * 波斯占星（Persian Astrology）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - Jarbakhtar 周期（129 年循环，迦勒底顺序，自 Almuten 起）；
 *  - 法达星盘 Firdaria（120 年时间主星，日/夜盘同序，找出当前周期）；
 *  - Tasyir 定向（1° ≈ 1 年）。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据 ========================

private val CHALDEAN_ORDER = listOf("土星", "木星", "火星", "太阳", "金星", "水星", "月亮")

private val PLANET_MINOR_YEARS = mapOf("土星" to 30, "木星" to 12, "火星" to 15, "太阳" to 19, "金星" to 8, "水星" to 20, "月亮" to 25)

private val PLANET_ATTRIBUTES = mapOf(
    "土星" to ("大凶" to "限制、时间、命运、责任、考验"),
    "木星" to ("大吉" to "幸运、扩张、智慧、财富、宗教"),
    "火星" to ("凶" to "战争、冲突、能量、行动、勇气"),
    "太阳" to ("吉" to "权威、生命、自我、荣耀、父亲"),
    "金星" to ("大吉" to "爱情、美丽、和谐、艺术、母亲"),
    "水星" to ("中性" to "智慧、沟通、商业、学习、旅行"),
    "月亮" to ("中性" to "情感、直觉、母亲、潜意识、家庭")
)

private val PERIOD_INTERPRETATIONS = mapOf(
    "土星" to "这是人生的筑基阶段。土星带来责任与考验，需要建立稳固基础，学习耐心与自律，可能经历挫折但为未来成功做准备。",
    "木星" to "这是人生的扩张阶段。木星带来机遇与幸运，适合学习、旅行、拓展视野，是收获的季节。",
    "火星" to "这是人生的行动阶段。火星带来能量与勇气，适合启动新项目、追求目标，但需控制冲动避免冲突。",
    "太阳" to "这是人生的闪耀阶段。太阳带来自信与荣耀，你将成焦点，适合展现领导力、追求个人成就。",
    "金星" to "这是人生的和谐阶段。金星带来爱与美，适合建立关系、享受生活，是社交与情感发展的黄金时期。",
    "水星" to "这是人生的学习阶段。水星带来智慧与沟通，适合学习新技能、建立人脉，思想活跃。",
    "月亮" to "这是人生的滋养阶段。月亮带来情感与直觉，适合关注家庭、内心世界，休养生息。"
)

private val FIRDARIA_ORDER = listOf("太阳", "月亮", "火星", "水星", "木星", "金星", "土星")
private val FIRDARIA_YEARS = mapOf("太阳" to 10, "月亮" to 9, "火星" to 7, "水星" to 13, "木星" to 12, "金星" to 8, "土星" to 11)

/** 星座守护星（Almuten 简化） */
private val SIGN_RULERS = mapOf(
    0 to "火星", 1 to "金星", 2 to "水星", 3 to "月亮", 4 to "太阳", 5 to "水星",
    6 to "金星", 7 to "火星", 8 to "木星", 9 to "土星", 10 to "土星", 11 to "木星"
)

/** 法达主星 → 五维分述（事业/财运/感情/健康/建议），供六维解读拼接 */
private val PLANET_DIMENSIONS = mapOf(
    "太阳" to listOf(
        "事业上你正走入聚光灯下，宜展现领导力与决断力，主动争取关键岗位。",
        "财运随名望水涨船高，正财为主，授权与荣誉带来的增益可期。",
        "感情中需放下自我中心，多给对方光芒与关注，关系方能相得益彰。",
        "健康注意心脏、血压与精力透支，劳逸结合，忌过度燃烧自我。",
        "以太阳般的坦然与威严立身，先立名后图利，行事光明正大。"
    ),
    "月亮" to listOf(
        "事业宜围绕家庭、服务与群众需求展开，以柔克刚，稳中有进。",
        "财运如潮水涨落，宜设应急之蓄，忌投机冒进，细水长流方久。",
        "感情渴望滋养与安全感，多陪伴与倾听，亲密关系将愈加深厚。",
        "健康留意情绪与消化系统，安顿内心、规律作息是最佳良方。",
        "顺应月亮的节律生活，重视直觉信号，为情绪留出安放的空间。"
    ),
    "火星" to listOf(
        "事业上冲劲十足，宜启动新项目、攻克难关，但需以谋略驾驭勇武。",
        "财运波动较大，速得速失，宜设定止损，勿在情绪中做重大投资。",
        "感情中热情外露易起摩擦，学会降温沟通，火气过后再谈是非。",
        "健康防炎症、外伤与过劳，运动宜适度，忌透支体力硬撑。",
        "将火星的能量导向建设性目标，冲动时先数到十，谋定而后动。"
    ),
    "水星" to listOf(
        "事业利于学习、沟通与商贸，多走动多交流，信息即是财富。",
        "财运来自脑力与信息差，合约文书务必审慎，签约前反复核对。",
        "感情以言语传情，坦诚而温柔的对话胜过一切猜测与沉默。",
        "健康注意神经紧张与睡眠，少思多息，脑力劳动者尤须放松。",
        "保持好奇与开放，持续学习新技能，人脉与见闻将打开新局。"
    ),
    "木星" to listOf(
        "事业迎来扩张良机，宜远行、进修与开拓新市场，格局越做越大。",
        "财运亨通，正财偏财皆有进益，宜广施善缘，财随德聚。",
        "感情和乐融融，单身者易遇良缘，有伴者共同成长、彼此成全。",
        "健康整体向好，注意饮食过盛与体重，富贵之余不忘运动。",
        "趁木星之势大胆向前，同时保持谦逊，好运偏爱有准备之人。"
    ),
    "金星" to listOf(
        "事业利于艺术、美学与人脉经营，以柔性和谐之道化解竞争。",
        "财运顺遂，偏财与人缘之财可观，宜投资审美与社交资本。",
        "感情甜蜜升温，是表白、复合与加深羁绊的黄金时期。",
        "健康身心舒畅，注意咽喉与肾脏保养，愉悦是最好的药引。",
        "以金星之美经营生活与关系，善待他人，亦善待自己的心意。"
    ),
    "土星" to listOf(
        "事业处于筑基期，责任加重，宜沉潜积累，慢即是快。",
        "财运偏紧，宜节俭储蓄、削减负债，稳扎稳打以渡此关。",
        "感情需以责任与承诺维系，少些浪漫幻想，多些实际担当。",
        "健康注意骨骼、牙齿与慢性疲劳，定期体检，作息规律至上。",
        "土星是时间之神，熬过此段的磨砺，你将获得最扎实的根基。"
    )
)

// ======================== 结果模型 ========================

data class PersianChartData(
    val ascendant: Double, val sun: Double, val moon: Double,
    val mercury: Double, val venus: Double, val mars: Double,
    val jupiter: Double, val saturn: Double, val isDiurnal: Boolean
)

data class PersianJarbakhtarPeriod(val planet: String, val startAge: Int, val endAge: Int, val years: Int, val omen: String, val symbol: String, val text: String)

data class FirdariaCurrent(val age: Double, val planet: String, val remaining: Double, val elapsed: Double, val totalYears: Int, val omen: String, val symbol: String, val text: String)

data class PersianFirdariaPeriod(val planet: String, val startAge: Int, val endAge: Int, val years: Int, val omen: String)

// ======================== 核心计算 ========================

object PersianAstrology {

    private fun norm(d: Double): Double = ((d % 360) + 360) % 360

    /** Jarbakhtar 周期（129 年循环） */
    fun jarbakhtar(c: PersianChartData): List<PersianJarbakhtarPeriod> {
        val ascIdx = floor(c.ascendant / 30).toInt() % 12
        val almuten = SIGN_RULERS[ascIdx] ?: "土星"
        val startIdx = CHALDEAN_ORDER.indexOf(almuten).let { if (it < 0) 0 else it }
        val ordered = CHALDEAN_ORDER.drop(startIdx) + CHALDEAN_ORDER.take(startIdx)
        val result = mutableListOf<PersianJarbakhtarPeriod>()
        var cumulative = 0
        ordered.forEach { planet ->
            val years = PLANET_MINOR_YEARS[planet] ?: 0
            val (omen, symbol) = PLANET_ATTRIBUTES[planet] ?: ("未知" to "")
            val base = PERIOD_INTERPRETATIONS[planet] ?: ""
            result.add(PersianJarbakhtarPeriod(planet, cumulative, cumulative + years - 1, years, omen, symbol, "【Jarbakhtar 周期】从${cumulative}岁到${cumulative + years - 1}岁：$base 此时期${symbol}方面的议题将变得重要。"))
            cumulative += years
        }
        return result
    }

    /** 法达星盘：当前周期 */
    fun firdariaCurrent(c: PersianChartData, birthDate: LocalDate): FirdariaCurrent {
        val age = ChronoUnit.DAYS.between(birthDate, LocalDate.now()) / 365.2422
        val ageInCycle = age % 120
        var cumulative = 0.0
        var current = FIRDARIA_ORDER.last()
        var remaining = 0.0
        var elapsed = 0.0
        for (planet in FIRDARIA_ORDER) {
            val years = FIRDARIA_YEARS[planet] ?: 0
            if (ageInCycle < cumulative + years) {
                current = planet
                remaining = cumulative + years - ageInCycle
                elapsed = ageInCycle - cumulative
                break
            }
            cumulative += years
        }
        val (omen, symbol) = PLANET_ATTRIBUTES[current] ?: ("未知" to "")
        val text = PERIOD_INTERPRETATIONS[current] ?: ""
        return FirdariaCurrent(age, current, remaining, elapsed, FIRDARIA_YEARS[current] ?: 0, omen, symbol, text)
    }

    /** 法达完整 120 年周期 */
    fun firdariaFull(): List<PersianFirdariaPeriod> {
        val result = mutableListOf<PersianFirdariaPeriod>()
        var cumulative = 0
        FIRDARIA_ORDER.forEach { planet ->
            val years = FIRDARIA_YEARS[planet] ?: 0
            val (omen, _) = PLANET_ATTRIBUTES[planet] ?: ("未知" to "")
            result.add(PersianFirdariaPeriod(planet, cumulative, cumulative + years - 1, years, omen))
            cumulative += years
        }
        return result
    }

    /** Tasyir 定向 */
    fun tasyir(c: PersianChartData, sig: String, pro: String): Triple<Double, Double, String> {
        val pos = mapOf(
            "太阳" to c.sun, "月亮" to c.moon, "水星" to c.mercury, "金星" to c.venus,
            "火星" to c.mars, "木星" to c.jupiter, "土星" to c.saturn
        )
        val a = pos[sig] ?: 0.0
        val b = pos[pro] ?: 0.0
        val arc = norm(b - a)
        val timing = when {
            arc < 30 -> "近期（1-30年内）"
            arc < 60 -> "中期（30-60年内）"
            arc < 90 -> "中远期（60-90年内）"
            else -> "远期（90年以上）"
        }
        val text = "从${sig}到${pro}的定向角度为 ${"%.1f".format(arc)}°，对应$timing。预示着约 ${"%.1f".format(arc)} 年后与${pro}相关领域将迎来重要发展。"
        return Triple(arc, (arc % 1) * 12, text)
    }

    /** 六维解读：总评 + 事业/财运/感情/健康/行动建议，贴合波斯法达/Jarbakhtar 主题 */
    fun buildVerdict(c: PersianChartData, birthDate: LocalDate): String {
        val fd = firdariaCurrent(c, birthDate)
        val dims = PLANET_DIMENSIONS[fd.planet] ?: PLANET_DIMENSIONS.getValue("月亮")
        val jb = jarbakhtar(c)
        val ageInt = fd.age.toInt()
        val jbCurrent = jb.firstOrNull { ageInt >= it.startAge && ageInt <= it.endAge } ?: jb.first()
        val sb = StringBuilder()
        sb.append("总评：法达主星行至「${fd.planet}」（剩余约 ${"%.1f".format(fd.remaining)} 年），此阶段由该星神掌理运势主轴；Jarbakhtar 同步行至「${jbCurrent.planet}」区间（${jbCurrent.startAge}-${jbCurrent.endAge}岁），双轨并观可判当前十年之基调。")
        sb.append("事业：${dims[0]}")
        sb.append("财运：${dims[1]}")
        sb.append("感情：${dims[2]}")
        sb.append("健康：${dims[3]}")
        sb.append("建议：${dims[4]}")
        sb.append("（波斯占星承袭中古星占传统，结果仅供文化娱乐参考）")
        return sb.toString()
    }
}
