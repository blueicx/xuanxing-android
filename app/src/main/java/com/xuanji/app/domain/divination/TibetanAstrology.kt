package com.xuanji.app.domain.divination

import java.time.LocalDate

/**
 * 西藏占星（藏历时轮占算）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 儒略日（JDN）为天文计算基础；
 *  - 月亮星宿 Nakshatra（27 宿）用「儒略日 / 恒星月 27.321582」简化模型；
 *  - 太阴日 Tithi（藏历日期，1..30）简化计算；
 *  - 五要素：星曜日（gza'）· 太阴日（tshe）· 月宿（skar）· 结合期（sbyor-ba）· 运动期（byed-pa）；
 *  - 元素组合：星曜日元素 × 月宿元素 → 运势解读（4×4 全组合，确定性）。
 * 全部离线、无随机，仅供文化娱乐参考。
 */

// ======================== 数据：27 月宿 ========================

/** 27 星宿（印度-藏历体系，中文常用译名） */
val TIBETAN_NAKSHATRAS: List<String> = listOf(
    "昴宿", "毕宿", "觜宿", "参宿", "井宿", "鬼宿", "柳宿", "星宿", "张宿",
    "翼宿", "轸宿", "角宿", "亢宿", "氐宿", "房宿", "心宿", "尾宿", "箕宿",
    "斗宿", "牛宿", "女宿", "虚宿", "危宿", "室宿", "壁宿", "奎宿", "娄宿"
)

/** 月宿元素（4 元素循环：地 水 火 风） */
private val NAKSHATRA_ELEMENT_CYCLE = listOf("地", "水", "火", "风")

// ======================== 数据：星曜日 ========================

/** 星期索引（0=太阳日..6=土星日）→ (藏历星期名, 元素) */
private val WEEKDAY_TABLE = listOf(
    Triple("太阳日", "日曜", "火"),
    Triple("月亮日", "月曜", "水"),
    Triple("火星日", "火曜", "火"),
    Triple("水星日", "水曜", "水"),
    Triple("木星日", "木曜", "木"),
    Triple("金星日", "金曜", "金"),
    Triple("土星日", "土曜", "土")
)

// ======================== 数据：结合期 / 运动期 ========================

/** 结合期（sbyor-ba）12 态（简化命名） */
private val CONJUNCTION_NAMES = listOf(
    "会合", "离分", "增长", "圆满", "平顺", "逆乱",
    "和谐", "冲突", "收获", "损耗", "兴起", "收束"
)

/** 运动期（byed-pa）8 态（简化命名） */
private val MOTION_NAMES = listOf(
    "开端", "发展", "旺盛", "转折", "回落", "停滞", "变革", "收束"
)

// ======================== 数据：元素组合解读（4×4） ========================

/** 星曜元素 × 月宿元素 → 解读（覆盖全部 16 种组合） */
private val ELEMENT_COMBINATION_INTERPRETATION: Map<Pair<String, String>, String> = mapOf(
    ("地" to "地") to "今日宜打基础、处理稳定的事务，按部就班易有收获。",
    ("地" to "水") to "今日是种植、建设的好时机，务实推进可让成果落地。",
    ("地" to "火") to "今日先稳固根基再图发展，厚积薄发最为稳妥。",
    ("地" to "风") to "今日宜把计划落到实地，借沟通与筹备把想法做实。",
    ("水" to "地") to "今日适合处理财务、培育项目，耐心经营自有回报。",
    ("水" to "水") to "今日宜增进人际关系、进行疗愈或庆祝活动，情感流动顺畅。",
    ("水" to "火") to "今日情绪与行动并存，宜以柔克刚、顺势而为。",
    ("水" to "风") to "今日宜交流学习，灵感充沛，利于创意表达与协作。",
    ("火" to "地") to "今日宜将热情转化为实际行动，脚踏实地见成效。",
    ("火" to "水") to "今日激情遇水宜先冷静沉淀，再谋定后动。",
    ("火" to "火") to "今日宜行动、竞争，但需注意控制情绪，避免急躁。",
    ("火" to "风") to "今日宜果断出击、大胆表达，适合开拓新局面。",
    ("风" to "地") to "今日宜把想法落实为具体步骤，谨防空谈误事。",
    ("风" to "水") to "今日宜合作沟通，关系和谐，适合洽谈与结盟。",
    ("风" to "火") to "今日宜先谋后动，把言论化为行动，事半功倍。",
    ("风" to "风") to "今日宜沟通、学习、旅行，思维活跃利见闻增长。"
)

private const val DEFAULT_INTERPRETATION = "今日是平稳的一天，宜静不宜动。"

/** 各元素基准分（确定性评分用） */
private val ELEMENT_BASE_SCORE = mapOf("地" to 55, "水" to 60, "火" to 58, "风" to 62, "木" to 57, "金" to 59)

/** 运动期（byed-pa）→ 事业维度解读 */
private val MOTION_CAREER = mapOf(
    "开端" to "事业正处起势，宜大胆启动新计划、开创新局面。",
    "发展" to "事业稳步上扬，宜趁势推进、扩大既有成果。",
    "旺盛" to "事业进入高峰，宜乘胜追击，但勿骄勿满。",
    "转折" to "事业将临变局，宜审时度势、灵活调整方向。",
    "回落" to "事业动能稍减，宜收缩整理、蓄力以待。",
    "停滞" to "事业暂陷胶着，宜查漏补缺、静待转机。",
    "变革" to "事业亟需革新，宜破旧立新、敢于调整思路。",
    "收束" to "事业告一段落，宜收尾总结、再谋新篇。"
)

/** 结合期（sbyor-ba）→ 财运维度解读 */
private val CONJUNCTION_WEALTH = mapOf(
    "会合" to "财运与人脉聚合，宜合作共赢、合力聚财。",
    "离分" to "财缘易散，宜守财防漏，勿做分散冒险投资。",
    "增长" to "财运呈增长之势，宜把握机会、稳健增值。",
    "圆满" to "财运圆满顺遂，宜知足感恩，勿贪多求全。",
    "平顺" to "财运平稳无波，宜按部就班、量入为出。",
    "逆乱" to "财运易生反复，宜谨慎决策、防突生变故。",
    "和谐" to "财运和谐顺畅，宜合作生财、和气致祥。",
    "冲突" to "财上易起争执，宜账目分明、防因财失和。",
    "收获" to "前劳得报、财运有实收，宜及时落袋为安。",
    "损耗" to "财有损耗之象，宜节流防耗，忌冲动消费。",
    "兴起" to "财运开始转旺，宜把握新机、顺势而起。",
    "收束" to "财运进入收束期，宜整理理财、储备冬粮。"
)

/** 元素 → 健康维度解读 */
private val ELEMENT_HEALTH = mapOf(
    "地" to "健康：地主脾胃，宜饮食规律、少食生冷，注意消化。",
    "水" to "健康：水主肾与泌尿，宜保暖防寒湿，注意作息与水液代谢。",
    "火" to "健康：火气偏旺，宜注意心火、血压与上火之症，多饮水平心静气。",
    "风" to "健康：风主呼吸与神经，宜防风邪，注意呼吸道与睡眠。",
    "木" to "健康：木主肝胆，宜疏肝理气、少熬夜，戒怒养神。",
    "金" to "健康：金主肺与皮肤，宜防燥润肺、注意皮肤过敏。"
)

// ======================== 核心计算 ========================

object TibetanAstrology {

    /**
     * 儒略日数（JDN，格里历），天文计算基础。
     * 与 Python 版本算法一致。
     */
    fun julianDay(d: LocalDate): Long {
        val a = (14 - d.monthValue) / 12
        val y = d.year + 4800 - a
        val m = d.monthValue + 12 * a - 3
        val jdn = d.dayOfMonth + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        return jdn.toLong()
    }

    /** 月亮所在星宿（0..26）。简化模型：以恒星月 27.321582 天为周期。 */
    fun moonNakshatra(jd: Long): Int {
        val lunarDay = (jd - 2451545.0) / 27.321582
        val mod = ((lunarDay % 27.0) + 27.0) % 27.0 // 防负
        return mod.toInt()
    }

    /** 太阴日 Tithi（藏历日期，1..30）。简化模型。 */
    fun lunarDay(jd: Long): Int {
        val lunarDay = (jd - 2451550.1) / 1.0
        val mod = ((lunarDay % 30.0) + 30.0) % 30.0 // 防负
        return mod.toInt() + 1
    }

    /** 五要素：星曜日·太阴日·月宿·结合期·运动期 */
    fun fiveElements(date: LocalDate): TibetanFiveElements {
        val jd = julianDay(date)
        // dayOfWeek.value: Mon=1..Sun=7 → 0=太阳日(周日)..6=土星日(周六)
        val weekdayIndex = date.dayOfWeek.value % 7
        val (weekdayName, _, weekdayElement) = WEEKDAY_TABLE[weekdayIndex]

        val nakshatraIndex = moonNakshatra(jd)
        val nakshatraElement = NAKSHATRA_ELEMENT_CYCLE[nakshatraIndex % NAKSHATRA_ELEMENT_CYCLE.size]

        val tithi = lunarDay(jd)
        val conjunction = ((tithi + nakshatraIndex) % 12 + 12) % 12
        val motion = ((tithi * 2 + nakshatraIndex) % 8 + 8) % 8

        return TibetanFiveElements(
            weekdayIndex = weekdayIndex,
            weekdayName = weekdayName,
            weekdayElement = weekdayElement,
            lunarDate = tithi,
            nakshatraIndex = nakshatraIndex,
            nakshatraName = TIBETAN_NAKSHATRAS[nakshatraIndex],
            nakshatraElement = nakshatraElement,
            conjunction = conjunction,
            conjunctionName = CONJUNCTION_NAMES[conjunction],
            motion = motion,
            motionName = MOTION_NAMES[motion]
        )
    }

    /** 综合运势：五要素 + 组合解读 + 确定性评分（0..100） */
    fun dailyReading(date: LocalDate): TibetanDailyReading {
        val e = fiveElements(date)
        val interpretation = ELEMENT_COMBINATION_INTERPRETATION[e.weekdayElement to e.nakshatraElement]
            ?: DEFAULT_INTERPRETATION

        // 确定性评分：基准分 + 元素关系调整 + 太阴日吉数加成，clamp 30..95
        val base = ELEMENT_BASE_SCORE[e.weekdayElement] ?: 55
        val sameElement = e.weekdayElement == e.nakshatraElement
        val harmonious = isHarmonious(e.weekdayElement, e.nakshatraElement)
        var score = base + if (sameElement) 15 else if (harmonious) 8 else 0
        if (e.lunarDate in listOf(1, 5, 9, 15, 20, 30)) score += 5
        score = score.coerceIn(30, 95)
        val band = when {
            score >= 80 -> "大吉"
            score >= 65 -> "吉"
            score >= 50 -> "中平"
            else -> "小挫"
        }
        return TibetanDailyReading(
            date = date,
            elements = e,
            interpretation = interpretation,
            score = score,
            band = band,
            verdict = buildVerdict(e, interpretation, score, band)
        )
    }

    /** 六维解读：总评/事业/财运/感情/健康/建议（按五要素确定性生成） */
    private fun buildVerdict(
        e: TibetanFiveElements,
        interpretation: String,
        score: Int,
        band: String
    ): String {
        val zong = "总评：今日${e.weekdayElement}（${e.weekdayName}）与${e.nakshatraElement}（${e.nakshatraName}）交会，$interpretation 综合指数 ${score} 分，属「$band」，宜顺势而行、量力而为。"
        val career = "事业：${MOTION_CAREER[e.motionName] ?: "事业宜稳中求进、步步为营。"}"
        val wealth = "财运：${CONJUNCTION_WEALTH[e.conjunctionName] ?: "财运宜守正经营、量入为出。"}"
        val love = when (e.conjunctionName) {
            "和谐", "会合", "圆满" -> "感情：情缘和顺，宜多陪伴沟通，关系有望升温。"
            "冲突", "逆乱", "损耗" -> "感情：易生口角，宜多体谅包容，退一步海阔天空。"
            else -> "感情：平淡之中见真味，宜真诚相待、细水长流。"
        }
        val health = ELEMENT_HEALTH[e.weekdayElement] ?: "健康：宜规律作息、注意饮食起居。"
        val advice = when {
            score >= 80 -> "建议：今日气场极佳，重要之事宜趁势推进，但亦须留有余地。"
            score >= 65 -> "建议：运势向好，宜主动进取，把握人际与事业良机。"
            score >= 50 -> "建议：运势平稳，宜按部就班、不冒进不蹉跎。"
            else -> "建议：今日宜守不宜攻，重要决策可缓一缓，静待转机。"
        }
        return listOf(zong, career, wealth, love, health, advice).joinToString("\n")
    }

    /** 元素相合判定（相邻互补：地→水→火→风→地 为顺） */
    private fun isHarmonious(a: String, b: String): Boolean {
        val cycle = listOf("地", "水", "火", "风")
        val ia = cycle.indexOf(a)
        val ib = cycle.indexOf(b)
        if (ia < 0 || ib < 0) return false
        val diff = ((ib - ia) % 4 + 4) % 4
        return diff == 1 || diff == 3
    }

    /** 元素的性格底色（本命五要素解读用），地/水/火/风 */
    fun elementNature(element: String): String = when (element) {
        "地" -> "地性之人：沉稳踏实、重基础与积累，做事有耐心，适合长期稳定的经营与储蓄，宜静守深耕。"
        "水" -> "水性之人：聪慧灵动、善人际往来，情感细腻，直觉敏锐，适合交流、疗愈与创意类事务。"
        "火" -> "火性之人：热情果敢、行动力强，富有感染力，适合开拓竞争，但需注意收敛急躁与情绪。"
        "风" -> "风性之人：思维活跃、口才出众，求知欲强，适合学习、传播与出行，贵在专注与落实。"
        else -> ""
    }
}

// ======================== 数据类 ========================

data class TibetanFiveElements(
    val weekdayIndex: Int,       // 0=太阳日..6=土星日
    val weekdayName: String,     // 太阳日 / 月亮日 ...
    val weekdayElement: String,  // 星曜日元素
    val lunarDate: Int,          // 太阴日 1..30
    val nakshatraIndex: Int,     // 月宿 0..26
    val nakshatraName: String,   // 昴宿 ...
    val nakshatraElement: String,// 月宿元素 地/水/火/风
    val conjunction: Int,        // 结合期 0..11
    val conjunctionName: String,
    val motion: Int,             // 运动期 0..7
    val motionName: String
)

data class TibetanDailyReading(
    val date: LocalDate,
    val elements: TibetanFiveElements,
    val interpretation: String,
    val score: Int,              // 0..100
    val band: String,            // 大吉 / 吉 / 中平 / 小挫
    val verdict: String          // 六维解读（总评/事业/财运/感情/健康/建议）
)
