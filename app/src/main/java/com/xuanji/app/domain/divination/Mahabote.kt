package com.xuanji.app.domain.divination

import java.time.LocalDate

/**
 * 缅甸黄道带（玛哈图法 Mahabote）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 出生日星期 → 主星（周日太阳…周六土星；周三上午水星 / 周三下午罗睺，按出生时刻 12:00 分界）；
 *  - 主星对应缅甸占星八种动物象征、方位与元素；
 *  - 玛哈图七宫方阵：出生主星坐镇 Binga（第一宫），其余按缅甸星期顺序顺时针排列；
 *  - 两人主星兼容性：同星极佳 / 相生良好 / 相克一般。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据映射 ========================

/** 星期主星（0=周日..6=周六） */
private val WEEKDAY_PLANETS = listOf("太阳", "月亮", "火星", "水星", "木星", "金星", "土星")

/** 周三下午主星为罗睺 */
private const val WEDNESDAY_PM_PLANET = "罗睺"

/** 主星 → 动物象征（缅甸黄道带 8 种动物） */
private val PLANET_ANIMAL = mapOf(
    "太阳" to "狮子",
    "月亮" to "老虎",
    "火星" to "大象（有牙）",
    "水星" to "大象（无牙）", // 周三上午
    "罗睺" to "大象（无牙）", // 周三下午
    "木星" to "老鼠",
    "金星" to "天竺鼠",
    "土星" to "龙"
)

/** 主星 → 方位 */
private val PLANET_DIRECTION = mapOf(
    "太阳" to "东",
    "月亮" to "北",
    "火星" to "南",
    "水星" to "东",
    "罗睺" to "西",
    "木星" to "北",
    "金星" to "南",
    "土星" to "西"
)

/** 主星 → 元素 */
private val PLANET_ELEMENT = mapOf(
    "太阳" to "火",
    "月亮" to "水",
    "火星" to "火",
    "水星" to "地",
    "罗睺" to "地",
    "木星" to "风",
    "金星" to "水",
    "土星" to "风"
)

/** 玛哈图七宫 */
private val MAHABOTE_HOUSES = listOf("Binga", "Ahtun", "Yaza", "Adipati", "Marana", "Thike", "Puti")

/** 宫位中文含义 */
private val HOUSE_MEANINGS = mapOf(
    "Binga" to "领袖宫（自我、地位）",
    "Ahtun" to "财富宫（金钱、资源）",
    "Yaza" to "权力宫（成就、权威）",
    "Adipati" to "智慧宫（知识、决策）",
    "Marana" to "障碍宫（挑战、死亡）",
    "Thike" to "人际宫（伙伴、敌人）",
    "Puti" to "业力宫（因果、解脱）"
)

/** 六维解读标签（总评/事业/财运/感情/健康/建议） */
private val DIM_LABELS = listOf("总评", "事业", "财运", "感情", "健康", "建议")

/** 将六个维度的文本拼成带「」标签的多行解读 */
private fun joinReading(lines: List<String>): String =
    DIM_LABELS.mapIndexed { i, label -> "「$label」${lines[i]}" }.joinToString("\n")

/** 主星 → 六维详细解说（用于档案卡） */
private val PLANET_MEANING = mapOf(
    "太阳" to joinReading(listOf(
        "光明自信、领导力强，是天生的引领者，一生运势多有上升之势。",
        "适合担当决策与领衔角色，越是高挑战的舞台越能成就声望，宜主动争取权责。",
        "财从名来，掌权之后财路自然宽阔，惟须注意排场开支，莫让面子大于里子。",
        "热情主动、占有欲强，容易被仰慕者围绕，需学会给对方留出呼吸的空间。",
        "火气偏旺，注意心脏、血压与眼目之劳，避免长期透支精力。",
        "以沉稳驾驭光芒，多借他人之力而非事事亲为，则大业可成。"
    )),
    "月亮" to joinReading(listOf(
        "温和感性、善解人意，一生以情谊滋养生命，人缘与家庭是最大的福气。",
        "适合服务、照料、创意与传播类工作，与人为善的相处之道让合作格外顺遂。",
        "财来如潮汐，起伏随情绪而动，宜建立稳定的储蓄习惯，忌情绪化消费。",
        "细腻体贴、极重安全感，是温柔而长情的伴侣，惟易因多思而患得患失。",
        "情绪与肠胃互相牵动，注意消化系统与睡眠，宜以静养调和身心。",
        "把敏感化为共情力，情绪波动时先安顿自己再作决定，则诸事顺遂。"
    )),
    "火星" to joinReading(listOf(
        "勇敢果决、行动力超群，一生以热血开疆拓土，越战越勇是其本色。",
        "适合竞争、开创与攻坚性行业，危机时刻最能展现价值，宜趁年轻多立战功。",
        "财从拼搏中来，主动出击常有进账，但冲动投资易破财，务必三思而后行。",
        "热烈直接、敢爱敢恨，相处讲究坦荡真诚，需克制脾气与争强好胜之心。",
        "精力充沛但易上火受伤，注意意外磕碰、炎症与血压，运动要量力而行。",
        "把冲劲装进计划里，先瞄准再出手，锋芒内敛方能长久。"
    )),
    "水星" to joinReading(listOf(
        "聪慧善言、思维敏捷，一生靠头脑与沟通安身立命，是天然的谋士与商人。",
        "适合商业、传媒、教育与写作，信息灵通处即是机遇所在，宜多走动多结缘。",
        "正财偏财皆从「点子」中来，善于捕捉信息差，但需防小聪明误事、投机失手。",
        "妙语连珠、风趣幽默，能说会道易得青睐，需少说空话、多以真心相待。",
        "思虑过度易神经紧张，注意呼吸道与用脑疲劳，宜给大脑定期「关机」。",
        "以诚信为本、以专注为桨，把聪明用在深耕一处，成就自然不请自来。"
    )),
    "罗睺" to joinReading(listOf(
        "隐伏多变、执念深重，人生多奇遇与转折，静水深流中暗藏非凡潜力。",
        "适合幕后、研究、玄学与新兴产业，越是不循常规的赛道越能一鸣惊人。",
        "财路诡谲、大起大落，忌投机与担保，宜以退为进，守住本金再图扩张。",
        "爱得深沉而多疑，易陷入执念，需学会信任与放手，莫让猜忌侵蚀情缘。",
        "神经与压力是薄弱环节，注意失眠、头痛与无名不适，宜静坐调息。",
        "以静制动、以柔克刚，把执念转化为深耕的定力，则凶中亦有吉。"
    )),
    "木星" to joinReading(listOf(
        "仁厚广博、福气深厚，一生贵人环绕，是公认的福星与良师益友。",
        "适合教育、法律、金融与宗教文化，德望所至则财位自开，宜广结善缘。",
        "财运宽裕稳健，常有意外之喜，乐善好施反而越散越有，忌守财吝啬。",
        "宽厚包容、重情重义，是值得托付的伴侣，惟需避免大包大揽而忽略对方感受。",
        "精力尚佳但易发福，注意肝胆与代谢，饮食有节、多行善养生。",
        "善用福泽而不骄纵，广布恩德而不居功，则福报绵延不绝。"
    )),
    "金星" to joinReading(listOf(
        "优雅圆融、人缘绝佳，一生与美、爱与和谐相伴，自带吸睛光环。",
        "适合艺术、时尚、社交与服务业，以审美与亲和力立足，越精致越出彩。",
        "财随人脉与审美而来，品味可生财，但享乐消费不节制则财来财去。",
        "浪漫多情、魅力四射，桃花旺盛，需守住真心，莫在莺燕之间迷失方向。",
        "注重保养、底子尚好，留意咽喉与内分泌，甜食美酒要适可而止。",
        "把魅力用在长情的经营上，内外兼修、张弛有度，则幸福长久。"
    )),
    "土星" to joinReading(listOf(
        "稳重担当、坚韧务实，人生先苦后甜，守成积累终有大成。",
        "适合管理、工程、公职与需要耐力的领域，越久越显其价值，宜深耕不辍。",
        "财来迟但稳，宜置产置业、长期投资，忌急功近利，耐得住才有厚报。",
        "爱得克制而深沉，不善言辞却重承诺，需学习主动表达与适时示弱。",
        "注意骨骼关节、牙齿与肠胃，劳碌易积劳成疾，宜规律作息、适度运动。",
        "以持重御繁难，懂得借力与休息，熬过低谷自见云开月明。"
    ))
)

/** 兼容性：主星友好关系（简化） */
private val FRIENDLY_PLANETS = mapOf(
    "太阳" to setOf("月亮", "火星", "木星"),
    "月亮" to setOf("太阳", "水星", "金星"),
    "火星" to setOf("太阳", "木星", "土星"),
    "水星" to setOf("月亮", "金星", "土星"),
    "木星" to setOf("太阳", "月亮", "火星"),
    "金星" to setOf("月亮", "水星", "土星"),
    "土星" to setOf("火星", "水星", "金星"),
    "罗睺" to setOf("土星", "水星")
)

// ======================== 结果模型 ========================

data class MahaboteProfile(
    val birthDate: LocalDate,
    val weekday: String,        // 周日..周六
    val planet: String,         // 主星
    val isWednesdayPm: Boolean, // 周三下午（罗睺）
    val animal: String,         // 动物象征
    val direction: String,      // 方位
    val element: String,        // 元素
    val meaning: String         // 主星解说
)

data class MahaboteHouse(
    val house: String,     // Binga...
    val meaning: String,   // 领袖宫（自我、地位）
    val planet: String,    // 主星
    val animal: String,
    val direction: String,
    val element: String
)

data class MahaboteCompatibility(
    val myPlanet: String,
    val otherPlanet: String,
    val level: String,     // 极佳 / 良好 / 一般
    val desc: String
)

// ======================== 核心计算 ========================

object Mahabote {

    /** 出生日星期索引（0=周日..6=周六） */
    fun weekdayIndex(date: LocalDate): Int = date.dayOfWeek.value % 7 // ISO: Mon=1..Sun=7 → 0=Sun..6=Sat

    /** 是否周三下午（出生时刻 >= 12:00；无时刻则视为上午） */
    fun isWednesdayPm(date: LocalDate, birthHour: Int?): Boolean =
        weekdayIndex(date) == 3 && (birthHour != null && birthHour >= 12)

    /** 出生日主星 */
    fun rulingPlanet(date: LocalDate, birthHour: Int?): String {
        val idx = weekdayIndex(date)
        return when {
            idx == 6 -> "土星"
            idx == 3 -> if (isWednesdayPm(date, birthHour)) WEDNESDAY_PM_PLANET else "水星"
            else -> WEEKDAY_PLANETS[idx]
        }
    }

    /** 个人档案 */
    fun profile(date: LocalDate, birthHour: Int?): MahaboteProfile {
        val planet = rulingPlanet(date, birthHour)
        val weekday = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[weekdayIndex(date)]
        return MahaboteProfile(
            birthDate = date,
            weekday = weekday,
            planet = planet,
            isWednesdayPm = isWednesdayPm(date, birthHour),
            animal = PLANET_ANIMAL[planet] ?: "未知",
            direction = PLANET_DIRECTION[planet] ?: "未知",
            element = PLANET_ELEMENT[planet] ?: "未知",
            meaning = PLANET_MEANING[planet] ?: ""
        )
    }

    /** 玛哈图七宫方阵：出生主星坐镇 Binga，其余按星期顺序顺时针 */
    fun houseSquare(date: LocalDate, birthHour: Int?): List<MahaboteHouse> {
        val planet = rulingPlanet(date, birthHour)
        // 罗睺在水星位（周三位）参与方阵排布
        val lookup = if (planet == "罗睺") "水星" else planet
        val birthIdx = WEEKDAY_PLANETS.indexOf(lookup).let { if (it < 0) 0 else it }
        val isWedPm = isWednesdayPm(date, birthHour)
        return MAHABOTE_HOUSES.mapIndexed { h, house ->
            val lordIdx = (birthIdx + h) % 7
            var lord = WEEKDAY_PLANETS[lordIdx]
            if (lord == "水星" && isWedPm) lord = "罗睺"
            MahaboteHouse(
                house = house,
                meaning = HOUSE_MEANINGS[house] ?: "未知",
                planet = lord,
                animal = PLANET_ANIMAL[lord] ?: "未知",
                direction = PLANET_DIRECTION[lord] ?: "未知",
                element = PLANET_ELEMENT[lord] ?: "未知"
            )
        }
    }

    /** 两人主星兼容性 */
    fun compatibility(myDate: LocalDate, myHour: Int?, otherDate: LocalDate, otherHour: Int?): MahaboteCompatibility {
        val my = rulingPlanet(myDate, myHour)
        val other = rulingPlanet(otherDate, otherHour)
        val (level, desc) = when {
            my == other -> "极佳" to "主星相同，灵魂契合"
            other in (FRIENDLY_PLANETS[my] ?: emptySet()) -> "良好" to "主星相生，和谐共处"
            else -> "一般" to "主星相克，需互相包容"
        }
        return MahaboteCompatibility(my, other, level, desc)
    }
}
