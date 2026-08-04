package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.domain.ZodiacCalculator
import kotlin.math.floor

/**
 * 古典占星三框架（复用 ZodiacCalculator 本命星盘）：
 * - 希腊（Hellenistic）：十度区间（Decan）主星、行星庙旺落陷、幸运点
 * - 波斯（Persian）：也门点等阿拉伯点（Arabic Parts）
 * - 巴比伦（Babylonian）：依星象生成预兆（omen）
 * 平黄经近似，仅供文化娱乐参考。
 */
object ClassicalAstrology {

    private val STANDARD_ORDER = listOf(
        "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座",
        "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"
    )

    /** 庙（Domicile）：行星 → 守护星座 */
    private val DOMICILE = mapOf(
        "太阳" to listOf("狮子座"), "月亮" to listOf("巨蟹座"),
        "水星" to listOf("双子座", "处女座"), "金星" to listOf("金牛座", "天秤座"),
        "火星" to listOf("白羊座", "天蝎座"), "木星" to listOf("射手座", "双鱼座"),
        "土星" to listOf("摩羯座", "水瓶座")
    )
    /** 旺（Exaltation）：行星 → 擢升星座 */
    private val EXALTATION = mapOf(
        "太阳" to "白羊座", "月亮" to "金牛座", "木星" to "巨蟹座", "土星" to "天秤座",
        "火星" to "摩羯座", "金星" to "双鱼座", "水星" to "处女座"
    )

    /**  Chaldean 十度区间主星：星座 → 三个区间主星 */
    private val DECAN_RULERS = mapOf(
        "白羊座" to listOf("火星", "太阳", "金星"), "金牛座" to listOf("水星", "月亮", "土星"),
        "双子座" to listOf("木星", "火星", "太阳"), "巨蟹座" to listOf("金星", "水星", "月亮"),
        "狮子座" to listOf("土星", "木星", "火星"), "处女座" to listOf("太阳", "金星", "水星"),
        "天秤座" to listOf("月亮", "土星", "木星"), "天蝎座" to listOf("火星", "太阳", "金星"),
        "射手座" to listOf("水星", "月亮", "土星"), "摩羯座" to listOf("木星", "火星", "太阳"),
        "水瓶座" to listOf("金星", "水星", "月亮"), "双鱼座" to listOf("土星", "木星", "火星")
    )

    data class Dignity(val planet: String, val sign: String, val status: String)
    data class GreekResult(
        val sunDecan: String,
        val ascDecan: String,
        val dignities: List<Dignity>,
        val partOfFortune: String
    )
    data class Part(val name: String, val pos: String)
    data class PersianResult(val parts: List<Part>)
    data class BabylonianResult(val omens: List<String>)
    data class ClassicalResult(
        val greek: GreekResult,
        val persian: PersianResult,
        val babylonian: BabylonianResult,
        val verdict: String,
        val note: String
    )

    private fun mod360(v: Double): Double = ((v % 360.0) + 360.0) % 360.0

    private fun lonToPos(lon: Double): String {
        val idx = ((floor(lon / 30.0).toInt()) % 12 + 12) % 12
        return "${STANDARD_ORDER[idx]} ${floor(lon % 30.0).toInt()}°"
    }

    private fun decanText(lon: Double): String {
        val idx = ((floor(lon / 30.0).toInt()) % 12 + 12) % 12
        val sign = STANDARD_ORDER[idx]
        val deg = floor(lon % 30.0).toInt()
        val decan = deg / 10 // 0,1,2
        val ruler = DECAN_RULERS.getValue(sign)[decan]
        return "$sign 第${decan + 1}区(${decan * 10}-${decan * 10 + 10}°)，主星 $ruler"
    }

    fun cast(profile: UserProfile): ClassicalResult {
        val chart = ZodiacCalculator.calculateNatalChart(
            profile.birthYear, profile.birthMonth, profile.birthDay,
            profile.birthHour, profile.birthMinute, profile.locationName
        )
        val map = chart.planets.associateBy { it.name }
        val sun = map.getValue("太阳").longitude
        val moon = map.getValue("月亮").longitude
        val asc = chart.ascendant
        val venus = map.getValue("金星").longitude
        val saturn = map.getValue("土星").longitude

        // 希腊：十度区间 + 庙旺落陷 + 幸运点
        val dignities = chart.planets.filter { it.name in DOMICILE.keys }.map { p ->
            val sign = p.sign
            val status = when {
                DOMICILE[p.name]?.contains(sign) == true -> "庙（守护）"
                EXALTATION[p.name] == sign -> "旺（擢升）"
                DOMICILE[p.name]?.contains(oppositeSign(sign)) == true -> "弱（失势）"
                EXALTATION[p.name] == oppositeSign(sign) -> "陷（落陷）"
                else -> "游（平平）"
            }
            Dignity(p.name, sign, status)
        }
        val poF = mod360(asc + moon - sun)
        val greek = GreekResult(
            sunDecan = decanText(sun),
            ascDecan = decanText(asc),
            dignities = dignities,
            partOfFortune = lonToPos(poF)
        )

        // 波斯：阿拉伯点
        val parts = listOf(
            Part("也门点", lonToPos(mod360(sun + asc - moon))),
            Part("婚姻点", lonToPos(mod360(venus + asc - sun))),
            Part("父亲点", lonToPos(mod360(sun + asc - saturn))),
            Part("母亲点", lonToPos(mod360(moon + asc - venus)))
        )
        val persian = PersianResult(parts)

        // 巴比伦：预兆
        val omens = babylonianOmens(chart, map)
        val babylonian = BabylonianResult(omens)

        val verdict = buildVerdict(greek, persian, babylonian)

        return ClassicalResult(
            greek = greek,
            persian = persian,
            babylonian = babylonian,
            verdict = verdict,
            note = "行星为平黄经近似，十度区间与阿拉伯点为传统技法演示，仅供娱乐参考。"
        )
    }

    /** 六维解读：总评 + 事业/财运/感情/健康/建议，综合希腊、波斯、巴比伦三套古典框架 */
    private fun buildVerdict(
        greek: GreekResult,
        persian: PersianResult,
        babylonian: BabylonianResult
    ): String {
        val sunRuler = greek.sunDecan.substringAfter("主星 ")
        val ascRuler = greek.ascDecan.substringAfter("主星 ")
        val fortune = greek.partOfFortune
        val dignityOf = { name: String -> greek.dignities.firstOrNull { it.planet == name }?.status ?: "游（平平）" }
        val dignified = greek.dignities.count { it.status.startsWith("庙") || it.status.startsWith("旺") }
        val debilitated = greek.dignities.count { it.status.startsWith("弱") || it.status.startsWith("陷") }
        val marriagePart = persian.parts.firstOrNull { it.name == "婚姻点" }?.pos ?: "未知"
        val overall = if (dignified >= debilitated) "命局庙旺之力多于落陷，整体格局稳健向上"
        else "命局落陷稍多，宜以收敛与修持来平衡星力"
        val saturnStatus = dignityOf("土星")
        val moonStatus = dignityOf("月亮")
        val healthNote = if (saturnStatus.startsWith("弱") || saturnStatus.startsWith("陷"))
            "土星「$saturnStatus」，尤须提防筋骨与慢性劳损"
        else "土星「$saturnStatus」，筋骨与压力耐受尚稳"
        val moonNote = if (moonStatus.startsWith("弱") || moonStatus.startsWith("陷"))
            "月亮「$moonStatus」，情绪与睡眠节律易波动，宜早睡养神"
        else "月亮「$moonStatus」，情绪与睡眠节律平顺，保持既有作息即可"
        val sb = StringBuilder()
        sb.append("总评：太阳临${greek.sunDecan}，上升入${greek.ascDecan}，幸运点落于$fortune；$overall，此生福泽的汇聚方向可由幸运点所在星座窥见")
        sb.append("\n事业：太阳十度区间主星为「$sunRuler」，上升区间主星为「$ascRuler」，前者定你建功的方向，后者定你处世的姿态，顺着两股星力择业进取，进退有据")
        sb.append("\n财运：幸运点落于$fortune，是古典占星中最看重的财帛信号；木星${dignityOf("木星")}，其得位与否提示贵人助财的强弱，可据此把握进账节奏")
        sb.append("\n感情：金星${dignityOf("金星")}，主你的爱恋质地；波斯婚姻点落于$marriagePart，感情功课由此而生，宜以金星之德（真诚、和美）经营关系")
        sb.append("\n健康：$healthNote；$moonNote，两星所在星座对应的身体部位宜多加保养")
        sb.append("\n建议：${babylonian.omens.joinToString("；")}；顺着庙旺之星所在的领域进取，避开落陷之星的短板，先稳后进，自可趋吉避凶")
        return sb.toString()
    }

    private fun oppositeSign(sign: String): String {
        val i = STANDARD_ORDER.indexOf(sign)
        return STANDARD_ORDER[(i + 6) % 12]
    }

    private fun babylonianOmens(
        chart: ZodiacCalculator.NatalChart,
        map: Map<String, ZodiacCalculator.PlanetPosition>
    ): List<String> {
        val out = mutableListOf<String>()
        val mars = map.getValue("火星")
        if (mars.sign in listOf("白羊座", "天蝎座", "狮子座")) {
            out.add("荧惑守战：火星临其强位，争战、变动与进取之兆。")
        }
        if (map.getValue("木星").sign == "巨蟹座") {
            out.add("岁星入庙：丰饶、恩泽与声名之兆。")
        }
        if (map.getValue("土星").sign in listOf("摩羯座", "水瓶座")) {
            out.add("镇星得位：秩序、约束与长久之兆。")
        }
        val sunLon = map.getValue("太阳").longitude
        val moonLon = map.getValue("月亮").longitude
        val diff = kotlin.math.abs(sunLon - moonLon)
        val sep = if (diff < 360 - diff) diff else 360 - diff
        if (sep <= 8.0) out.add("日月合朔：新旧更替、大事将定之兆。")
        if (map.getValue("金星").sign in listOf("金牛座", "天秤座")) {
            out.add("太白得位：和合、欢愉与人缘之兆。")
        }
        if (out.isEmpty()) out.add("诸星各守其度，四时不忒，岁时平顺之兆。")
        return out
    }
}
