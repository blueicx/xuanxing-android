package com.xuanji.app.domain.divination

import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

/**
 * 巴比伦占星（Babylonian Astrology）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 简化儒略日 → 太阳黄经（春分起点）、月亮位置（System B 锯齿函数）、五大行星（System A/B）；
 *  - 巴比伦黄道十二宫（楔形文字名）与行星神祇、吉凶象征；
 *  - 月亮黄纬（交点周期正弦近似）解读；
 *  - 黄道位置对应人生领域。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据 ========================

/** 巴比伦黄道十二宫（楔形名 + 现代对应） */
val BABYLONIAN_ZODIAC: List<String> = listOf(
    "𒀯𒊩𒌆𒀭𒈾 (The Hired Man / 白羊座)",
    "𒀯𒈥𒄘𒃻 (The Great Twins / 金牛座)",
    "𒀯𒋛𒁉𒍣𒀭𒈾 (The Crab / 双子座)",
    "𒀯𒌨𒈤 (The Lion / 巨蟹座)",
    "𒀯𒀭𒈾𒀀 (The Furrow / 狮子座)",
    "𒀯𒀭𒈾𒀀 (The Scales / 处女座)",
    "𒀯𒄈𒋰 (The Scorpion / 天秤座)",
    "𒀯𒉺𒇻𒊬 (The Archer / 天蝎座)",
    "𒀯𒈗𒋰𒄩 (The Sea-Goat / 射手座)",
    "𒀯𒄩𒋛𒀀 (The Water-Pourer / 摩羯座)",
    "𒀯𒍠𒀭𒈾 (The Tails / 水瓶座)",
    "𒀯𒆲𒋰𒄩 (The Great One / 双鱼座)"
)

/** 现代星座简称（解读用） */
private val MODERN_SIGNS = listOf("白羊", "金牛", "双子", "巨蟹", "狮子", "处女", "天秤", "天蝎", "射手", "摩羯", "水瓶", "双鱼")

/** 巴比伦七曜 */
private val BABYLONIAN_PLANETS = listOf(
    "𒀭𒋀𒆠 (Sin) 月亮 (Moon)",
    "𒀭𒌓 (Shamash) 太阳 (Sun)",
    "𒀭𒈹 (Ishtar) 金星 (Venus)",
    "𒀭𒄊𒀕𒃲 (Marduk) 木星 (Jupiter)",
    "𒀭𒊩𒌆𒄈𒋰 (Nergal) 火星 (Mars)",
    "𒀭𒉽 (Nabu) 水星 (Mercury)",
    "𒀭𒆠𒋻 (Ninurta) 土星 (Saturn)"
)

/** 行星（中文键）→ 吉凶/象征 */
private val PLANET_OMEN = mapOf(
    "月亮" to ("中性" to "女性、生育、直觉"),
    "太阳" to ("吉" to "男性、权威、生命"),
    "金星" to ("大吉" to "爱情、美丽、丰饶"),
    "木星" to ("大吉" to "幸运、扩张、智慧"),
    "火星" to ("凶" to "战争、冲突、疾病"),
    "水星" to ("中性" to "智慧、沟通、商业"),
    "土星" to ("凶" to "限制、时间、命运")
)

/** 黄道位置 → 人生领域 */
private val ZODIAC_DOMAIN = listOf(
    "自我与身份", "财富与资源", "沟通与旅行", "家庭与根基", "创造与享乐",
    "健康与服务", "关系与伙伴", "转变与重生", "哲学与远行", "事业与声望",
    "社群与理想", "灵性与超越"
)

// ======================== 结果模型 ========================

data class BabylonianReading(
    val signIndex: Int,
    val signName: String,        // 巴比伦名
    val modernSign: String,
    val degreeInSign: Double,
    val domain: String
)

data class BabylonianPlanetPos(
    val name: String,            // 中文名
    val fullName: String,        // 楔形+神祇名
    val longitude: Double,
    val signName: String,
    val degreeInSign: Double,
    val omen: String,
    val symbol: String
)

data class BabylonianResult(
    val date: LocalDate,
    val jd: Long,
    val useSystemA: Boolean,
    val sun: BabylonianReading,
    val moon: BabylonianReading,
    val lunarLatitude: Double,
    val lunarLatitudeMeaning: String,
    val planets: List<BabylonianPlanetPos>,
    val verdict: String
)

// ======================== 核心计算 ========================

object BabylonianAstrology {

    /** 公历 → 简化儒略日（整数） */
    fun julianDay(y: Int, m: Int, d: Int): Long {
        var yy = y
        var mm = m
        if (mm <= 2) { yy -= 1; mm += 12 }
        val a = yy / 100
        val b = 2 - a + a / 4
        return (365.25 * (yy + 4716)).toLong() + (30.6001 * (mm + 1)).toLong() + d + b - 1524
    }

    /** 太阳黄经（春分 0° 起，简化） */
    fun sunLongitude(jd: Long): Double {
        val daysSinceEquinox = (jd - 2451623.5) % 365.2422
        return ((daysSinceEquinox / 365.2422) * 360 + 360) % 360
    }

    /** System B 锯齿函数 */
    private fun zigzag(x: Double, maxV: Double, minV: Double, period: Double): Double {
        val normalized = ((x % period) + period) % period / period
        return if (normalized < 0.5) minV + (maxV - minV) * normalized * 2
        else maxV - (maxV - minV) * (normalized - 0.5) * 2
    }

    /** 月亮黄经（System B 锯齿 + 基线） */
    fun moonLongitude(jd: Long): Double {
        val synodic = 29.53059
        val days = (jd - 2451550.1) % synodic
        val base = (days / synodic) * 360
        val speed = zigzag(days, 14.5, 11.5, 248.0)
        return ((base + speed * 0.5) + 360) % 360
    }

    /** 五大行星位置（简化为按平均速度推进） */
    private val PLANET_SPEED = mapOf(
        "金星" to 0.9856, "木星" to 0.0831, "火星" to 0.5240,
        "水星" to 1.3834, "土星" to 0.0335
    )

    /** 月亮黄纬（交点周期正弦近似） */
    fun lunarLatitude(jd: Long): Double {
        val days = jd - 2451550.1
        val nodePhase = (days / 6793.5) % 1
        return 5.145 * sin(2 * PI * nodePhase)
    }

    /** 完整结果 */
    fun calculate(date: LocalDate, useSystemA: Boolean = true): BabylonianResult {
        val jd = julianDay(date.year, date.monthValue, date.dayOfMonth)
        val sun = sunLongitude(jd)
        val moon = moonLongitude(jd)
        val lat = lunarLatitude(jd)

        fun reading(lon: Double): BabylonianReading {
            val idx = floor(lon / 30.0).toInt() % 12
            return BabylonianReading(idx, BABYLONIAN_ZODIAC[idx], MODERN_SIGNS[idx], lon % 30, ZODIAC_DOMAIN[idx])
        }

        val planets = PLANET_SPEED.map { (name, speed) ->
            var pos = ((jd - 2451545.0) * speed + 360) % 360
            // System A：土星阶梯修正
            if (useSystemA && name == "土星") {
                pos = if (pos in 130.0..330.0) pos + 0.1 else pos - 0.05
            }
            pos = ((pos % 360) + 360) % 360
            val idx = floor(pos / 30.0).toInt() % 12
            val (omen, symbol) = PLANET_OMEN[name] ?: ("未知" to "")
            BabylonianPlanetPos(name, BABYLONIAN_PLANETS.first { it.contains(name) }, pos, BABYLONIAN_ZODIAC[idx], pos % 30, omen, symbol)
        }

        val latMeaning = when {
            abs(lat) > 4 -> "月亮处于高黄纬，在巴比伦占星中标志重要的天文事件"
            abs(lat) > 2 -> "月亮黄纬适中，预示着平稳的时期"
            else -> "月亮接近黄道，是进行重要决策的时机"
        }

        val sunReading = reading(sun)
        val moonReading = reading(moon)
        val verdict = buildVerdict(sunReading, moonReading, planets, latMeaning)

        return BabylonianResult(date, jd, useSystemA, sunReading, moonReading, lat, latMeaning, planets, verdict)
    }

    /** 六维解读：总评 + 事业/财运/感情/健康/行动建议，贴合巴比伦行星神祇主题 */
    private fun buildVerdict(
        sun: BabylonianReading,
        moon: BabylonianReading,
        planets: List<BabylonianPlanetPos>,
        latMeaning: String
    ): String {
        fun pos(name: String): BabylonianPlanetPos = planets.first { it.name == name }
        val mars = pos("火星")
        val jupiter = pos("木星")
        val venus = pos("金星")
        val saturn = pos("土星")
        val sb = StringBuilder()
        sb.append("总评：太阳落于${sun.signName}（${sun.modernSign}），此生围绕「${sun.domain}」展开核心课题；月居${moon.modernSign}，对应直觉与潜意识的底色。诸神各就其位，整体格局吉凶相参，以守正持中为要。")
        sb.append("事业：太阳落于「${sun.domain}」，事业主轴与此领域息息相关；火星（Nergal，战神）行于${mars.signName}，提示竞争与冲劲并存，宜以勇毅破局而不宜锋芒过露。")
        sb.append("财运：木星（Marduk，智慧与扩张之神）行于${jupiter.signName}，主正财渐旺与远见之财；金星所在${venus.signName}，暗示人脉与审美带来的增益。")
        sb.append("感情：金星（Ishtar，爱与丰饶女神）行于${venus.signName}，感情质地由此着色，宜以真诚与体贴经营；月亮主导情绪依恋的深浅，${latMeaning}。")
        sb.append("健康：土星（Ninurta，节制与命运之神）行于${saturn.signName}，提示劳损与压力所在，宜注意作息节制；情绪节律随月相起伏，张弛有度方得安稳。")
        sb.append("建议：顺应吉星（金星、木星）的祝福，避开凶星（火星、土星）的锋芒；每逢日月会合等天象窗口顺势而动，信守承诺、依天道而行，自可趋吉避凶。")
        sb.append("（巴比伦占星为古代泥板文明的文化遗产，结果仅供文化娱乐参考）")
        return sb.toString()
    }
}
