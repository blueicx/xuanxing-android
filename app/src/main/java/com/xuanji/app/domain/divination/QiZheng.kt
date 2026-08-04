package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.UserProfile
import com.xuanji.app.domain.ZodiacCalculator
import kotlin.math.floor
import java.time.LocalDateTime

/**
 * 七政四余（中式星命）：
 * - 七政：日、月、水、金、火、木、土 七曜，落黄道十二宫
 * - 四余：罗睺（月北交）、计都（月南交）、月孛、紫气
 * 日月五星复用 ZodiacCalculator 的本命星盘（平黄经近似）；四余中罗睺取北交，
 * 计都取对冲，月孛/紫气为简化慢速均值近似。离线确定性推算，仅供文化娱乐参考。
 */
object QiZheng {

    /** 黄道十二宫（自下而上，白羊起） */
    private val PALACES = listOf(
        "白羊宫", "金牛宫", "双子宫", "巨蟹宫", "狮子宫", "处女宫",
        "天秤宫", "天蝎宫", "射手宫", "摩羯宫", "水瓶宫", "双鱼宫"
    )

    data class Body(
        val name: String,
        val symbol: String,
        val longitude: Double,
        val palace: String,
        val degreeInPalace: Int,
        val kind: String // 七政 / 四余
    )

    data class QiZhengResult(
        val bodies: List<Body>,
        val verdict: String,
        val note: String
    )

    private fun mod360(v: Double): Double = ((v % 360.0) + 360.0) % 360.0

    fun cast(profile: UserProfile): QiZhengResult {
        val chart = ZodiacCalculator.calculateNatalChart(
            profile.birthYear, profile.birthMonth, profile.birthDay,
            profile.birthHour, profile.birthMinute, profile.locationName
        )
        val dt = LocalDateTime.of(profile.birthYear, profile.birthMonth, profile.birthDay, profile.birthHour, profile.birthMinute)
        val j2000 = LocalDateTime.of(2000, 1, 1, 12, 0)
        val d = java.time.temporal.ChronoUnit.MINUTES.between(j2000, dt) / 1440.0

        val map = chart.planets.associateBy { it.name }
        val seven = listOf("太阳", "月亮", "水星", "金星", "火星", "木星", "土星")
        val symbols = mapOf(
            "太阳" to "☉", "月亮" to "☽", "水星" to "☿", "金星" to "♀",
            "火星" to "♂", "木星" to "♃", "土星" to "♄"
        )
        val bodies = mutableListOf<Body>()
        seven.forEach { n ->
            map[n]?.let { p ->
                bodies.add(toBody(n, symbols.getValue(n), p.longitude, "七政"))
            }
        }

        // 四余
        val rahu = map["北交"]?.longitude ?: mod360(125.04 - 19.361 * d / 365.25)
        bodies.add(toBody("罗睺", "☊", rahu, "四余"))
        bodies.add(toBody("计都", "☋", mod360(rahu + 180.0), "四余"))
        bodies.add(toBody("月孛", "✶", mod360(250.0 + 0.0539 * d), "四余")) // 简化均值近似
        bodies.add(toBody("紫气", "✺", mod360(110.0 + 0.0243 * d), "四余")) // 简化均值近似

        return QiZhengResult(
            bodies = bodies,
            verdict = buildVerdict(bodies),
            note = "日月五星为按出生地的平黄经近似（误差数度至十余度）；罗睺取北交、计都对冲，月孛/紫气为慢速均值近似。仅供娱乐参考。"
        )
    }

    /** 按七政四余落宫给出六维解读（总评/事业/财运/感情/健康/建议） */
    private fun buildVerdict(bodies: List<Body>): String {
        fun palaceOf(name: String): String? = bodies.firstOrNull { it.name == name }?.palace
        val sun = palaceOf("太阳")
        val moon = palaceOf("月亮")
        val jupiter = palaceOf("木星")
        val saturn = palaceOf("土星")
        val mars = palaceOf("火星")
        val venus = palaceOf("金星")

        val sb = StringBuilder()
        // 总评
        sb.append("总评：七政四余星盘中，日躔${sun ?: "—"}、月宿${moon ?: "—"}，命身根基由此而定；五行得位则气机顺畅，失位则宜调和。")
        // 事业（太阳/火星/土星）
        sb.append("事业：").append(
            when {
                sun == "白羊宫" || sun == "狮子宫" || sun == "摩羯宫" -> "太阳得势，事业心强、目标坚定，宜担纲主事、稳步晋升。"
                mars == "天蝎宫" || mars == "白羊宫" -> "火星入强位，执行力与开创力俱佳，宜开拓新局，但防急躁。"
                saturn == "摩羯宫" -> "土星得位，稳重可靠，宜长期深耕专业，中年后地位稳固。"
                else -> "事业平稳，宜按部就班、积累实力，把握星曜顺行之机再图进取。"
            }
        )
        // 财运（木星/金星）
        sb.append("财运：").append(
            when {
                jupiter == "双鱼宫" || jupiter == "射手宫" -> "木星入庙，财库丰盈，正偏财皆宜，但防铺张。"
                venus == "金牛宫" || venus == "天秤宫" -> "金星得位，财运顺遂，利于人脉与资源变现。"
                else -> "财运平稳，宜守成理财、量入为出，财随德聚。"
            }
        )
        // 感情（金星/月亮）
        sb.append("感情：").append(
            when {
                venus == "天秤宫" || venus == "双鱼宫" -> "金星入旺位，感情浪漫和顺，桃花带吉，单身者易遇良缘。"
                moon == "巨蟹宫" -> "月入旺位，情感细腻、重家庭，关系温暖而长久。"
                else -> "感情平稳，宜真诚经营、多些陪伴，细水长流。"
            }
        )
        // 健康
        sb.append("健康：").append(
            when (mars) {
                "白羊宫" -> "火气偏旺，宜注意劳逸结合，防急躁与上火。"
                "天蝎宫" -> "精力强但易积郁，宜留意情绪疏导与休养。"
                else -> "身心总体安泰，保持规律作息即可。"
            }
        )
        // 建议
        sb.append("建议：以日、月二曜为纲，顺星之吉凶趋避；吉星得势则乘势而为，凶曜受制则守正待时，观其行、修其德，命途自宽。")
        sb.append("（七政四余为传统中式星命，结果仅供文化娱乐参考）")
        return sb.toString()
    }

    private fun toBody(name: String, symbol: String, longitude: Double, kind: String): Body {
        val idx = ((floor(longitude / 30.0).toInt()) % 12 + 12) % 12
        return Body(name, symbol, longitude, PALACES[idx], floor(longitude % 30.0).toInt(), kind)
    }
}
