package com.xuanji.app.domain

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * 西方星座 / 本命星盘推算。
 * - 太阳星座：按出生月日（精确）
 * - 上升 / 天顶(MC)：按出生地经纬度 + 本地恒星时(LST) 精确计算（确定性方法）
 * - 十大行星：开普勒轨道根数（JPL 低精度）推算地心黄经，落座准确
 * - 北交(平均北交点)：长期回归近似
 */
object ZodiacCalculator {

    data class ZodiacInfo(
        val sign: String,        // 摩羯座
        val element: String,     // 土
        val symbol: String,      // ♑
        val dateRange: String,
        val trait: String
    )

    /** 完整星盘（太阳 / 上升 / 月亮） */
    data class WesternDetail(
        val sun: ZodiacInfo,
        val rising: ZodiacInfo,
        val moon: ZodiacInfo,
        val note: String
    )

    // ===================== 本命星盘 =====================

    /** 单颗行星在星盘中的位置 */
    data class PlanetPosition(
        val name: String,        // 太阳
        val symbol: String,      // ☉
        val sign: String,        // 摩羯座
        val degreeInSign: Int,   // 0-29（落入该星座的度数）
        val house: Int,          // 1-12（落入的宫位）
        val longitude: Double    // 0-360（黄经）
    )

    /** 行星之间的相位 */
    data class Aspect(
        val p1: String,          // 太阳
        val p2: String,          // 月亮
        val type: String,        // 合 / 六合 / 刑 / 拱 / 冲
        val orb: Double          // 容许度（度）
    )

    /** 本命星盘 */
    data class NatalChart(
        val ascendant: Double,   // 上升点黄经
        val midheaven: Double,   // 天顶黄经（由 LST 推算）
        val cusps: List<Double>, // 十二宫 Placidus 宫界（顺序 1..12，单位黄经）
        val planets: List<PlanetPosition>,
        val aspects: List<Aspect>
    )

    // ===================== 太阳星座基础数据 =====================

    private data class Def(
        val sign: String,
        val symbol: String,
        val element: String,
        val dateRange: String,
        val trait: String,
        val startMonth: Int,
        val startDay: Int,
        val endMonth: Int,
        val endDay: Int
    )

    private val DEFS = listOf(
        Def("水瓶座", "♒", "风", "1/20 - 2/18",
            "独立自主、思维前卫，重视友情与理想；偶尔显得疏离而固执。", 1, 20, 2, 18),
        Def("双鱼座", "♓", "水", "2/19 - 3/20",
            "温柔浪漫、富有同理心与想象力；易感伤、缺乏边界感。", 2, 19, 3, 20),
        Def("白羊座", "♈", "火", "3/21 - 4/19",
            "热情直率、行动力强、敢为人先；易冲动、欠缺耐心。", 3, 21, 4, 19),
        Def("金牛座", "♉", "土", "4/20 - 5/20",
            "踏实稳重、务实可靠、重视物质与感官；偏固执、抗拒改变。", 4, 20, 5, 20),
        Def("双子座", "♊", "风", "5/21 - 6/21",
            "机敏善变、沟通力强、兴趣广泛；易三分钟热度、不够专注。", 5, 21, 6, 21),
        Def("巨蟹座", "♋", "水", "6/22 - 7/22",
            "顾家念旧、心思细腻、保护欲强；情绪化、敏感多疑。", 6, 22, 7, 22),
        Def("狮子座", "♌", "火", "7/23 - 8/22",
            "自信大方、富有领袖气场与创造力；好面子、略显专制。", 7, 23, 8, 22),
        Def("处女座", "♍", "土", "8/23 - 9/22",
            "严谨细致、追求完美与秩序；易焦虑、过度挑剔。", 8, 23, 9, 22),
        Def("天秤座", "♎", "风", "9/23 - 10/23",
            "优雅平和、追求公平与和谐；优柔寡断、回避冲突。", 9, 23, 10, 23),
        Def("天蝎座", "♏", "水", "10/24 - 11/22",
            "深沉专一、洞察力强、意志坚定；占有欲强、不善表达。", 10, 24, 11, 22),
        Def("射手座", "♐", "火", "11/23 - 12/21",
            "自由乐观、热爱探索与哲思；粗心大意、缺乏耐性。", 11, 23, 12, 21),
        Def("摩羯座", "♑", "土", "12/22 - 1/19",
            "务实坚韧、有责任感与野心；严肃保守、易感压力。", 12, 22, 1, 19)
    )

    /** 标准黄道顺序（白羊起，用于黄经 → 星座映射） */
    private val STANDARD_ORDER = listOf(
        "白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座",
        "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"
    )

    private val BY_NAME = DEFS.associateBy { it.sign }

    // ===================== 行星轨道根数（J2000，每儒略世纪变化） =====================
    // 采用 JPL「Keplerian Elements for Approximate Positions of the Major Planets」低精度根数：
    // 先由根数求日心黄道直角坐标，再减地球日心坐标得到地心黄经。
    // 相比旧的「平黄经近似」，落座准确度大幅提升（已对照真实星历验证内/外行星）。
    // 适用范围约 1800–2100 年，误差通常在 1° 以内，足够落座与宫位判定。
    private data class OrbitEl(
        val name: String,
        val symbol: String,
        val a: Double, val aRate: Double,
        val e: Double, val eRate: Double,
        val i: Double, val iRate: Double,
        val l: Double, val lRate: Double,
        val peri: Double, val periRate: Double,
        val node: Double, val nodeRate: Double
    )

    private val EARTH_EL = OrbitEl(
        "地球", "🌍",
        1.00000261, 0.00000562,
        0.01671123, -0.00004392,
        -0.00001531, -0.01294668,
        100.46457166, 35999.37244981,
        102.93768193, 0.32327364,
        0.0, -0.00002832
    )

    private val PLANET_KEPLER = listOf(
        OrbitEl("水星", "☿",
            0.38709927, 0.00000037,
            0.20563593, 0.00001906,
            7.00497902, -0.00594749,
            252.25032350, 149472.67411175,
            77.45779628, 0.21176362,
            48.33076593, -0.12534081),
        OrbitEl("金星", "♀",
            0.72333566, 0.00000390,
            0.00677672, -0.00004107,
            3.39467605, -0.00078890,
            181.97909950, 58517.81538729,
            131.60246718, 0.03244191,
            76.67984255, -0.27769418),
        OrbitEl("火星", "♂",
            1.52371034, 0.00001847,
            0.09339410, 0.00007882,
            1.84969142, -0.00813131,
            -4.55343205, 19140.30268499,
            -23.94362959, 0.44441088,
            49.55953891, -0.29257343),
        OrbitEl("木星", "♃",
            5.20288700, -0.0011607,
            0.04838624, -0.00013253,
            1.30439695, -0.00183714,
            34.39644051, 3034.74612775,
            14.72847983, 0.21252668,
            100.47390909, 0.20469106),
        OrbitEl("土星", "♄",
            9.53667594, -0.00125060,
            0.05386179, -0.00050991,
            2.48599187, -0.00194258,
            49.95424423, 1222.49362201,
            92.59887831, -0.41897216,
            113.66242448, -0.28867794),
        OrbitEl("天王星", "⛢",
            19.18916464, -0.00196176,
            0.04725744, -0.00004397,
            0.77263783, 0.00035372,
            313.23810451, 428.48202785,
            170.95427630, 0.40805281,
            74.01692503, 0.04240589),
        OrbitEl("海王星", "♆",
            30.06992276, 0.00026291,
            0.00859048, 0.00005105,
            1.77004347, 0.00035372,
            -55.12002969, 218.45945325,
            44.96476227, -0.32241464,
            131.78422574, -0.00508664),
        OrbitEl("冥王星", "♇",
            39.48211675, -0.00031596,
            0.24882730, 0.00005170,
            17.14001206, 0.00004818,
            238.92903833, 145.20780515,
            224.06891629, -0.04062942,
            110.30393684, -0.01183482)
    )

    /** 由 J2000 轨道根数求某行星的日心黄道直角坐标 (x, y, z)（单位 AU）。 */
    private fun helioPosition(el: OrbitEl, T: Double): Triple<Double, Double, Double> {
        val a = el.a + el.aRate * T
        val e = el.e + el.eRate * T
        val iRad = Math.toRadians(el.i + el.iRate * T)
        val L = el.l + el.lRate * T
        val peri = el.peri + el.periRate * T
        val node = el.node + el.nodeRate * T
        var M = L - peri
        M = mod360(M)
        if (M > 180.0) M -= 360.0
        val Mr = Math.toRadians(M)
        // 牛顿迭代解开普勒方程 E - e·sinE = M
        var E = Mr
        for (k in 0 until 60) {
            val f = E - e * Math.sin(E) - Mr
            val fp = 1.0 - e * Math.cos(E)
            E -= f / fp
        }
        val nu = 2.0 * Math.atan2(
            Math.sqrt(1.0 + e) * Math.sin(E / 2.0),
            Math.sqrt(1.0 - e) * Math.cos(E / 2.0)
        )
        val r = a * (1.0 - e * Math.cos(E))
        val xo = r * Math.cos(nu)
        val yo = r * Math.sin(nu)
        // 轨道平面内旋转（近点角 ω = peri − node）
        val w = Math.toRadians(peri - node)
        val om = Math.toRadians(node)
        val cosw = Math.cos(w); val sinw = Math.sin(w)
        val cosO = Math.cos(om); val sinO = Math.sin(om)
        val cosi = Math.cos(iRad); val sini = Math.sin(iRad)
        val x2 = cosw * xo - sinw * yo
        val y2 = sinw * xo + cosw * yo
        val x3 = x2
        val y3 = y2 * cosi
        val z3 = y2 * sini
        // 升交点旋转到黄道系
        val xE = cosO * x3 - sinO * y3
        val yE = sinO * x3 + cosO * y3
        val zE = z3
        return Triple(xE, yE, zE)
    }

    // 旧档案兼容坐标；新版档案保存省/市/县区中心坐标。
    private val CITY_COORDS = mapOf(
        "宜兴" to Pair(31.36, 119.82),
        "无锡" to Pair(31.49, 120.31),
        "苏州" to Pair(31.30, 120.62),
        "南京" to Pair(32.06, 118.80),
        "上海" to Pair(31.23, 121.47),
        "杭州" to Pair(30.27, 120.16),
        "北京" to Pair(39.90, 116.41),
        "天津" to Pair(39.13, 117.20),
        "广州" to Pair(23.13, 113.26),
        "深圳" to Pair(22.54, 114.06),
        "香港" to Pair(22.32, 114.17),
        "成都" to Pair(30.57, 104.07),
        "重庆" to Pair(29.56, 106.55),
        "武汉" to Pair(30.59, 114.31),
        "西安" to Pair(34.27, 108.95),
        "台北" to Pair(25.03, 121.57)
    )

    private const val TZ_OFFSET_HOURS = 8.0   // 北京时间(UTC+8)，无夏令时

    // ===================== 对外 API =====================

    fun calculate(month: Int, day: Int): ZodiacInfo = infoOf(sunDef(month, day))

    /** 计算太阳 / 上升 / 月亮星座（精确上升，需出生地） */
    fun calculateDetail(
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
        locationName: String,
        locationLat: Double? = null,
        locationLng: Double? = null
    ): WesternDetail = detailFromChart(
        calculateNatalChart(year, month, day, hour, minute, locationName, locationLat, locationLng)
    )

    /** 兼容旧调用（无地点时按北京推算） */
    fun calculateDetail(year: Int, month: Int, day: Int, hour: Int, minute: Int): WesternDetail =
        calculateDetail(year, month, day, hour, minute, "北京")

    /** 由星盘推导太阳 / 上升 / 月亮星座 */
    fun detailFromChart(chart: NatalChart): WesternDetail {
        val sun = chart.planets.first { it.name == "太阳" }
        val moon = chart.planets.first { it.name == "月亮" }
        val ascIdx = floor(chart.ascendant / 30.0).toInt() % 12
        return WesternDetail(
            sun = infoOf(BY_NAME.getValue(sun.sign)),
            rising = infoOf(BY_NAME.getValue(STANDARD_ORDER[ascIdx])),
            moon = infoOf(BY_NAME.getValue(moon.sign)),
            note = "太阳 / 上升 / 天顶为按出生地经纬度的精确推算；" +
                "十大行星采用 JPL 开普勒轨道根数推算地心黄经（约 1800–2100 年适用，误差通常 1° 内）；北交为平均交点近似，仅供娱乐参考。"
        )
    }

    /**
     * 计算本命星盘：
     * - 上升 / 天顶：本地恒星时(LST) + 出生地经纬度（精确）
     * - 十大行星：开普勒地心黄经 + Placidus 分宫制落宫（真月亮）
     * - 北交：平均北交点长期回归近似
     */
    fun calculateNatalChart(
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
        locationName: String,
        locationLat: Double? = null,
        locationLng: Double? = null
    ): NatalChart {
        val legacy = locationToCoords(locationName)
        val lat = locationLat ?: legacy.first
        val lon = locationLng ?: legacy.second
        val utHours = (hour + minute / 60.0) - TZ_OFFSET_HOURS
        val jd = julianDate(year, month, day, utHours)
        val d = jd - 2451545.0
        val T = d / 36525.0

        // 本地恒星时（RAMC）
        val gmst = mod360(280.46061837 + 360.98564736629 * d)
        val lst = mod360(gmst + lon)
        val ramcR = Math.toRadians(lst)
        val epsR = Math.toRadians(23.4392911)

        // 天顶(MC) 与 上升点(ASC)
        val mc = mod360(Math.toDegrees(atan2(sin(ramcR), cos(ramcR) * cos(epsR))))
        val asc = mod360(Math.toDegrees(atan2(
            cos(ramcR),
            -(sin(ramcR) * cos(epsR) + tan(Math.toRadians(lat)) * sin(epsR))
        )))

        // 地球日心坐标（用于地心换算）
        val earth = helioPosition(EARTH_EL, T)
        // 太阳地心黄经 = 地球日心黄经 + 180°
        val sunLon = mod360(Math.toDegrees(atan2(earth.second, earth.first)) + 180.0)
        // 月亮：真月亮黄经（含平近点角与主要周期项，精度约 0.1°）
        val moonLon = trueMoon(jd)

        // Placidus 十二宫宫界（按出生地纬度与 RAMC 推算）
        val cusps = placidusCusps(lst, epsR, Math.toRadians(lat))

        val planets = mutableListOf<PlanetPosition>()
        planets.add(planetAt(sunLon, cusps, "太阳", "☉"))
        planets.add(planetAt(moonLon, cusps, "月亮", "☽"))
        // 其余行星：日心坐标减地球日心坐标得地心黄经（开普勒轨道根数）
        for (el in PLANET_KEPLER) {
            val p = helioPosition(el, T)
            val gx = p.first - earth.first
            val gy = p.second - earth.second
            val geoLon = mod360(Math.toDegrees(atan2(gy, gx)))
            planets.add(planetAt(geoLon, cusps, el.name, el.symbol))
        }
        // 北交（平均北交点）：J2000 约 125.04°，逆行 ~19.361°/年（按日计）
        val nnLon = mod360(125.04 - 19.361 * (d / 365.25))
        planets.add(planetAt(nnLon, cusps, "北交", "☊"))

        return NatalChart(
            ascendant = asc,
            midheaven = mc,
            cusps = cusps,
            planets = planets,
            aspects = buildAspects(planets)
        )
    }

    /** 兼容旧调用（无地点时按北京推算） */
    fun calculateNatalChart(
        year: Int, month: Int, day: Int, hour: Int, minute: Int
    ): NatalChart = calculateNatalChart(year, month, day, hour, minute, "北京")

    // ===================== 内部工具 =====================

    private fun planetAt(lon: Double, cusps: List<Double>, name: String, symbol: String): PlanetPosition {
        val sign = STANDARD_ORDER[((floor(lon / 30.0).toInt()) % 12 + 12) % 12]
        val degreeInSign = floor(lon % 30.0).toInt()
        val house = houseOf(lon, cusps)
        return PlanetPosition(name, symbol, sign, degreeInSign, house, lon)
    }

    /** 真月亮黄经（Meeus 低精度月历，含平近点角与主要周期项，精度约 0.1°）。 */
    private fun trueMoon(jd: Double): Double {
        val T = (jd - 2451545.0) / 36525.0
        val r = Math::toRadians
        val Lp = 218.3164477 + 481267.88123421 * T - 0.0015786 * T * T + T * T * T / 538841 - T * T * T * T / 65194000
        val D = 297.8501921 + 445267.1114034 * T - 0.0018819 * T * T + T * T * T / 545868 - T * T * T * T / 113065000
        val M = 357.5291092 + 35999.0502909 * T - 0.0001536 * T * T + T * T * T / 24490000
        val Mp = 134.9633964 + 477198.8675055 * T + 0.0087414 * T * T + T * T * T / 69699 - T * T * T * T / 14712000
        val F = 93.2720950 + 483202.0175233 * T - 0.0036539 * T * T - T * T * T / 3526000 + T * T * T * T / 863310000
        val L = Lp
            + 6.288774 * sin(r(Mp))
            + 1.274027 * sin(r(2 * D - Mp))
            + 0.658314 * sin(r(2 * D))
            + 0.213618 * sin(r(2 * Mp))
            + 0.185116 * sin(r(2 * M))
            - 0.114332 * sin(r(2 * F))
            + 0.058793 * sin(r(2 * D - 2 * Mp))
            + 0.057066 * sin(r(2 * D - M - Mp))
            + 0.053322 * sin(r(2 * D + Mp))
            + 0.045758 * sin(r(2 * D - M))
            + 0.040923 * sin(r(Mp - M))
            - 0.034720 * sin(r(D))
            - 0.030383 * sin(r(2 * F - Mp))
            + 0.014216 * sin(r(2 * D + 2 * Mp))
        return mod360(L)
    }

    /** 给定赤经(RA, 度)反查其所在黄道的黄经（epsR 为黄赤交角，弧度）。 */
    private fun raToLam(ra: Double, epsR: Double): Double {
        val y = sin(Math.toRadians(ra))
        val x = cos(Math.toRadians(ra)) * cos(epsR)
        return mod360(Math.toDegrees(atan2(y, x)))
    }

    /** Placidus 单对宫界：从参考角点(refRA)逆时针取 1/3、2/3 半弧处的黄经。 */
    private fun placidusPair(refRA: Double, upper: Boolean, epsR: Double, latR: Double): Pair<Double, Double> {
        var ad = 0.0
        for (k in 0 until 80) {
            val sa = if (upper) 90.0 + ad else 90.0 - ad
            val lam1 = raToLam(refRA + sa / 3.0, epsR)
            val decl1 = asin((sin(Math.toRadians(lam1)) * sin(epsR)).coerceIn(-1.0, 1.0))
            val newAd = asin((tan(latR) * tan(decl1)).coerceIn(-1.0, 1.0))
            if (abs(Math.toDegrees(newAd) - ad) < 1e-9) break
            ad = Math.toDegrees(newAd)
        }
        val sa = if (upper) 90.0 + ad else 90.0 - ad
        return raToLam(refRA + sa / 3.0, epsR) to raToLam(refRA + 2 * sa / 3.0, epsR)
    }

    /** 计算十二宫 Placidus 宫界（顺序 1..12，单位黄经）。 */
    private fun placidusCusps(ramc: Double, epsR: Double, latR: Double): List<Double> {
        val asc = raToLam(ramc + 90.0, epsR)
        val ic = raToLam(ramc + 180.0, epsR)
        val dsc = raToLam(ramc + 270.0, epsR)
        val mc = raToLam(ramc, epsR)
        val (c11, c12) = placidusPair(ramc, true, epsR, latR)
        val (c2, c3) = placidusPair(ramc + 90.0, false, epsR, latR)
        val (c5, c6) = placidusPair(ramc + 180.0, false, epsR, latR)
        val (c8, c9) = placidusPair(ramc + 270.0, true, epsR, latR)
        return listOf(asc, c2, c3, ic, c5, c6, dsc, c8, c9, mc, c11, c12)
    }

    /** 由黄经与十二宫界求落宫（1..12）。 */
    private fun houseOf(lon: Double, cusps: List<Double>): Int {
        val l = mod360(lon)
        for (h in 0 until 12) {
            val a = mod360(cusps[h])
            val b = mod360(cusps[(h + 1) % 12])
            if (a <= b) {
                if (l >= a && l < b) return h + 1
            } else {
                if (l >= a || l < b) return h + 1
            }
        }
        return 1
    }

    private fun buildAspects(planets: List<PlanetPosition>): List<Aspect> {
        val out = mutableListOf<Aspect>()
        for (i in planets.indices) {
            for (j in i + 1 until planets.size) {
                val a = planets[i]
                val b = planets[j]
                val diff = abs(a.longitude - b.longitude)
                val sep = if (diff < 360.0 - diff) diff else 360.0 - diff
                val (type, orb) = when {
                    sep <= 8.0 -> "合" to sep
                    abs(sep - 60.0) <= 5.0 -> "六合" to abs(sep - 60.0)
                    abs(sep - 90.0) <= 6.0 -> "刑" to abs(sep - 90.0)
                    abs(sep - 120.0) <= 6.0 -> "拱" to abs(sep - 120.0)
                    sep >= 172.0 -> "冲" to (180.0 - sep)
                    else -> null to 0.0
                }
                if (type != null) out.add(Aspect(a.name, b.name, type, orb))
            }
        }
        return out.sortedByDescending { it.orb }
    }

    private fun mod360(v: Double): Double = ((v % 360.0) + 360.0) % 360.0

    /** 出生地名称 → (纬度, 经度)。未命中返回北京。 */
    private fun locationToCoords(name: String): Pair<Double, Double> {
        for ((key, coord) in CITY_COORDS) {
            if (name.contains(key)) return coord
        }
        return CITY_COORDS.getValue("北京")
    }

    /** 儒略日（含 UT 小数日）。 */
    private fun julianDate(y: Int, m: Int, d: Int, utHours: Double): Double {
        var yy = y
        var mm = m
        if (mm <= 2) { yy -= 1; mm += 12 }
        val a = yy / 100
        val b = 2 - a + a / 4
        return floor(365.25 * (yy + 4716)) + floor(30.6001 * (mm + 1)) + d + b - 1524.5 + utHours / 24.0
    }

    private fun sunDef(month: Int, day: Int): Def {
        return DEFS.first { def ->
            if (def.startMonth <= def.endMonth) {
                (month == def.startMonth && day >= def.startDay) ||
                    (month == def.endMonth && day <= def.endDay) ||
                    (month > def.startMonth && month < def.endMonth)
            } else {
                (month == def.startMonth && day >= def.startDay) ||
                    (month == def.endMonth && day <= def.endDay) ||
                    month > def.startMonth || month < def.endMonth
            }
        }
    }

    private fun infoOf(def: Def): ZodiacInfo = ZodiacInfo(
        sign = def.sign,
        element = def.element,
        symbol = def.symbol,
        dateRange = def.dateRange,
        trait = def.trait
    )

    // ===================== 综合解读（确定性，无随机） =====================

    data class WesternConclusionItem(
        val title: String,
        val icon: String,
        val headline: String,
        val body: String,
        val tags: List<String> = emptyList()
    )

    data class WesternConclusion(
        val summary: String,
        val items: List<WesternConclusionItem>
    )

    /** 星座中文名 → 四象元素 */
    private val SIGN_ELEMENT = mapOf(
        "白羊座" to "火", "狮子座" to "火", "射手座" to "火",
        "金牛座" to "土", "处女座" to "土", "摩羯座" to "土",
        "双子座" to "风", "天秤座" to "风", "水瓶座" to "风",
        "巨蟹座" to "水", "天蝎座" to "水", "双鱼座" to "水"
    )

    private val ELEMENT_NATURE = mapOf(
        "火" to "行动与热情，向外释放能量，敢冲敢闯但易急躁。",
        "土" to "务实与稳定，重视安全感与结果，可靠但偏固执。",
        "风" to "思维与交流，善沟通与变通，理性但易飘忽不定。",
        "水" to "情感与直觉，敏感共情、富有想象，深沉但易情绪化。"
    )

    private val ELEMENT_TRAIT = mapOf(
        "火" to "行动派", "土" to "务实派", "风" to "思考派", "水" to "感受派"
    )

    /**
     * 综合太阳 / 上升 / 月亮三要素与星盘数据，生成多段论述。
     * 全部由命盘数据推导（确定性，无随机）。
     */
    fun computeConclusion(detail: WesternDetail, chart: NatalChart): WesternConclusion {
        val sun = detail.sun
        val rising = detail.rising
        val moon = detail.moon

        // 行星四象分布
        val elemCount = mutableMapOf("火" to 0, "土" to 0, "风" to 0, "水" to 0)
        chart.planets.forEach { p ->
            val e = SIGN_ELEMENT[p.sign]
            if (e != null) elemCount[e] = elemCount.getValue(e) + 1
        }
        val dominantElem = elemCount.maxByOrNull { it.value }?.key ?: "风"

        val summary = buildString {
            append("太阳${sun.sign}（${sun.element}象）奠定你的核心自我与人生方向，")
            append("上升${rising.sign}（${rising.element}象）塑造他人眼中的第一印象与应对姿态，")
            append("月亮${moon.sign}（${moon.element}象）掌管内在情绪与安全感来源。")
            append("三者之中，你的星盘能量偏重于「${dominantElem}象」")
            append("（火${elemCount["火"]} · 土${elemCount["土"]} · 风${elemCount["风"]} · 水${elemCount["水"]}），")
            append("整体呈现出${ELEMENT_TRAIT[dominantElem]}的基调。")
        }

        val items = mutableListOf<WesternConclusionItem>()

        // 1 太阳星座
        items.add(
            WesternConclusionItem(
                title = "太阳星座 · 核心自我",
                icon = sun.symbol,
                headline = "${sun.sign}（${sun.element}象）",
                body = "太阳代表你最本质的生命力与人生目标。${sun.trait}\n\n" +
                    "在关系中你倾向于以${sun.element}象的方式表达自我——${ELEMENT_NATURE[sun.element]}",
                tags = listOf(sun.sign, "${sun.element}象", "人生主线")
            )
        )

        // 2 上升星座
        items.add(
            WesternConclusionItem(
                title = "上升星座 · 外在面具",
                icon = rising.symbol,
                headline = "${rising.sign}（${rising.element}象）",
                body = "上升点（ASC）是出生时东方地平线升起的星座，决定了你给人的第一印象与本能的处世方式。" +
                    "你外在呈现${rising.sign}的特质：${rising.trait}\n\n" +
                    "当他人初识你时，往往先感受到${rising.element}象的能量——${ELEMENT_NATURE[rising.element]}",
                tags = listOf(rising.sign, "${rising.element}象", "第一印象")
            )
        )

        // 3 月亮星座
        items.add(
            WesternConclusionItem(
                title = "月亮星座 · 内在情绪",
                icon = moon.symbol,
                headline = "${moon.sign}（${moon.element}象）",
                body = "月亮掌管情绪、直觉与安全感需求。${moon.trait}\n\n" +
                    "在内省与亲密关系中，你以${moon.element}象的方式处理感受——${ELEMENT_NATURE[moon.element]}",
                tags = listOf(moon.sign, "${moon.element}象", "情绪底色")
            )
        )

        // 4 三元整合
        val trioSame = (sun.element == rising.element) && (rising.element == moon.element)
        val trioMix = (sun.element != rising.element) || (rising.element != moon.element)
        val trioBody = buildString {
            append("太阳、上升、月亮分别落在")
            val parts = mutableListOf<String>()
            if (sun.element != rising.element || sun.element != moon.element) parts.add("${sun.sign}")
            parts.add("${sun.element}象的核心")
            append("${sun.sign}（${sun.element}）、${rising.sign}（${rising.element}）、${moon.sign}（${moon.element}）三处。\n\n")
            if (trioSame) {
                append("三者同属${sun.element}象，内外高度一致——你表里如一，能量聚焦、目标清晰，但也可能因过于单一而少了几分层次变化。")
            } else if (trioMix) {
                append("三者元素各异，说明你的内在（月亮）、外在（上升）与本质（太阳）呈现出不同面向：")
                append("对外你可能更像${rising.sign}（${rising.element}象），独处时却流露出${moon.sign}（${moon.element}象）的情绪底色，而真正驱动你的是${sun.sign}（${sun.element}象）。")
                append("这种层次感让你在不同场合灵活切换，但也需要花些心力整合内在的张力。")
            } else {
                append("你的太阳与上升同象，外在表现与本质一致，增添了可信度与稳定感。")
            }
        }
        items.add(
            WesternConclusionItem(
                title = "三元整合 · 太阳 × 上升 × 月亮",
                icon = "🌗",
                headline = if (trioSame) "内外一致" else "多面层次",
                body = trioBody,
                tags = listOf(sun.sign, rising.sign, moon.sign)
            )
        )

        // 5 元素平衡
        val weakElem = elemCount.filter { it.value == 0 }.keys.toList()
        val elemBody = buildString {
            append("星盘十大星体分布于四象：${elemCount.entries.joinToString(" · ") { "${it.key}象 ${it.value}" }}。\n\n")
            append("能量最旺的是「${dominantElem}象」，你天然擅长${ELEMENT_TRAIT[dominantElem]}式的表达；")
            if (weakElem.isNotEmpty()) {
                append("而${weakElem.joinToString("、")}象偏弱，意味着相关面向（${weakElem.joinToString("、") { ELEMENT_NATURE[it]!!.take(6) }}…）需要后天有意识地补足——例如多接触对应元素的活动与人事物。")
            } else {
                append("四象俱全，能量分布较为均衡，适应面宽。")
            }
        }
        items.add(
            WesternConclusionItem(
                title = "四象元素平衡",
                icon = "♅",
                headline = "主导：${dominantElem}象",
                body = elemBody,
                tags = elemCount.entries.sortedByDescending { it.value }.take(3).map { "${it.key}${it.value}" }
            )
        )

        // 6 主要相位
        if (chart.aspects.isNotEmpty()) {
            val top = chart.aspects.take(3)
            val aspectBody = "星体间的主要相位揭示内在的动力结构：\n\n" +
                top.joinToString("\n") { a ->
                    val desc = when (a.type) {
                        "合" -> "能量叠加、相互强化"
                        "六合" -> "和谐顺畅、轻松助力"
                        "拱" -> "天赋流畅、天然合拍"
                        "刑" -> "内在张力、需主动化解"
                        "冲" -> "两极拉扯、对立中求衡"
                        else -> "特殊关联"
                    }
                    "· ${a.p1} ${a.type} ${a.p2}（${"%.1f".format(a.orb)}°）：$desc"
                } + "\n\n（相位由行星地心黄经推算，仅供娱乐参考。）"
            items.add(
                WesternConclusionItem(
                    title = "主要相位 · 内在动力",
                    icon = "⚭",
                    headline = "关键相位 ${top.size} 组",
                    body = aspectBody,
                    tags = top.map { "${it.p1}${it.type}${it.p2}" }
                )
            )
        }

        // 7 综合建议
        val adviceBody = buildString {
            append("· 发挥你的主色调：以${dominantElem}象的${ELEMENT_TRAIT[dominantElem]}之长立身，把它用在事业与关系的核心处。\n")
            append("· 整合表里：对外借${rising.sign}的圆融打开局面，对内倾听${moon.sign}的真实需求，别让面具盖过本心。\n")
            if (weakElem.isNotEmpty()) {
                append("· 补足短板：刻意经营${weakElem.joinToString("、")}象对应的面向，平衡星盘能量。\n")
            }
            append("· 借势而为：太阳${sun.sign}给你长期方向，上升${rising.sign}给你当下抓手，二者配合最省力。")
        }
        items.add(
            WesternConclusionItem(
                title = "综合建议",
                icon = "✨",
                headline = "主${dominantElem}象 · 表里整合",
                body = adviceBody,
                tags = listOf("发挥${dominantElem}象", "太阳${sun.sign}", "上升${rising.sign}")
            )
        )

        return WesternConclusion(summary, items)
    }

    // ===================== 星盘文字解析（确定性，无随机） =====================

    data class PlanetMeaning(
        val symbol: String,
        val name: String,
        val sign: String,
        val house: Int,
        val text: String
    )

    data class AxisMeaning(
        val key: String,       // ASC / DSC / MC / IC
        val name: String,      // 上升点
        val sign: String,
        val degree: Int,
        val text: String
    )

    data class AspectMeaning(
        val p1: String,
        val p2: String,
        val type: String,
        val orb: Double,
        val text: String
    )

    data class ChartInterpretation(
        val planets: List<PlanetMeaning>,
        val axes: List<AxisMeaning>,
        val aspects: List<AspectMeaning>
    )

    /** 行星核心含义关键词 */
    private val PLANET_KEYWORD = mapOf(
        "太阳" to "代表自我、意志与生命力的核心",
        "月亮" to "掌管情绪、直觉与内在安全感",
        "水星" to "主导思维、沟通与学习方式",
        "金星" to "关联爱、美感与价值取舍",
        "火星" to "驱动行动力、欲望与竞争力",
        "木星" to "象征扩张、幸运与信念成长",
        "土星" to "代表限制、责任与成熟的考验",
        "天王星" to "带来变革、独立与突发变数",
        "海王星" to "指向灵性、想象与边界消融",
        "冥王星" to "关乎转化、权力与深层重生",
        "北交" to "提示今生灵魂成长的方向与课题"
    )

    /** 十二宫位主题 */
    private val HOUSE_THEME = mapOf(
        1 to "自我形象、外在气场与人生起点",
        2 to "财富、自我价值与物质资源",
        3 to "沟通表达、手足与短途交流",
        4 to "家庭、根源与内心归宿",
        5 to "创作、恋爱与子女缘分",
        6 to "工作、健康与日常秩序",
        7 to "伴侣、合作与一对一关系",
        8 to "深层转化、共享资源与隐秘力量",
        9 to "探索、高等教育与远行视野",
        10 to "事业成就、社会形象与公众地位",
        11 to "群体、理想与朋友网络",
        12 to "潜意识、灵性与独处内省"
    )

    /** 星座气质短词（用于文字润色） */
    private val SIGN_FLAVOR = mapOf(
        "白羊座" to "火热直接", "金牛座" to "稳健务实", "双子座" to "灵活善变",
        "巨蟹座" to "细腻念旧", "狮子座" to "自信张扬", "处女座" to "严谨细致",
        "天秤座" to "温和协调", "天蝎座" to "深沉执着", "射手座" to "乐观开阔",
        "摩羯座" to "坚韧克制", "水瓶座" to "独立先锋", "双鱼座" to "浪漫敏感"
    )

    private fun axisInfo(longitude: Double): Pair<String, Int> {
        val sign = STANDARD_ORDER[((floor(longitude / 30.0).toInt()) % 12 + 12) % 12]
        val degree = floor(longitude % 30.0).toInt()
        return sign to degree
    }

    private fun aspectDesc(type: String): String = when (type) {
        "合" -> "能量叠加、相互强化，相关特质被放大"
        "六合" -> "和谐顺畅，带来轻松的助力与机会"
        "拱" -> "天赋流畅，天然合拍、资源易得"
        "刑" -> "内在张力，需主动经营与化解"
        "冲" -> "两极拉扯，在张力中寻求平衡与互补"
        else -> "形成特殊关联"
    }

    /** 由本命星盘生成可读文字解析（行星落座落宫 / 四轴 / 相位）。 */
    fun interpretChart(chart: NatalChart): ChartInterpretation {
        val planetMeanings = chart.planets.map { p ->
            val keyword = PLANET_KEYWORD[p.name] ?: "影响你的某方面能量"
            val flavor = SIGN_FLAVOR[p.sign] ?: ""
            val element = SIGN_ELEMENT[p.sign] ?: ""
            val houseTheme = HOUSE_THEME[p.house] ?: "相关生命领域"
            val text = "$keyword。它落入${flavor}的${p.sign}（${element}象），染上该星座的气质；" +
                "并落于第${p.house}宫——$houseTheme，意味着你在该生命领域会以这颗星的方式运作。"
            PlanetMeaning(p.symbol, p.name, p.sign, p.house, text)
        }

        val axes = listOf(
            buildAxis("ASC", "上升点", chart.ascendant,
                "是你出生时东方地平线升起的星座，决定第一印象、外在面具与身体气场"),
            buildAxis("DSC", "下降点", (chart.ascendant + 180.0) % 360.0,
                "与上升相对，映射你对伴侣与合作者的需求，以及你在关系中吸引的特质"),
            buildAxis("MC", "天顶", chart.midheaven,
                "象征事业、社会成就、公众形象与人生目标所在"),
            buildAxis("IC", "天底", (chart.midheaven + 180.0) % 360.0,
                "与天顶相对，代表家庭根源、内在安全感与私密归宿")
        )

        val aspectMeanings = chart.aspects.map { a ->
            AspectMeaning(a.p1, a.p2, a.type, a.orb,
                "${a.p1} ${a.type} ${a.p2}（${"%.1f".format(a.orb)}°）：${aspectDesc(a.type)}。")
        }

        return ChartInterpretation(planetMeanings, axes, aspectMeanings)
    }

    private fun buildAxis(key: String, name: String, longitude: Double, role: String): AxisMeaning {
        val (sign, degree) = axisInfo(longitude)
        val flavor = SIGN_FLAVOR[sign] ?: ""
        val element = SIGN_ELEMENT[sign] ?: ""
        val text = "${name}落在${flavor}的${sign}（${element}象，约 ${degree}°）。它$role。"
        return AxisMeaning(key, name, sign, degree, text)
    }
}
