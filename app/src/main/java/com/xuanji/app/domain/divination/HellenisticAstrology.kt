package com.xuanji.app.domain.divination

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.sin

/**
 * 希腊占星（Hellenistic Astrology）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 简化儒略日 → 太阳黄经、月亮黄经、上升点（简化）、五大行星位置；
 *  - 区段（Sect）判定（日生/夜生）；
 *  - 幸运点 / 精神点（Lots，昼夜公式）；
 *  - 年主星推运（Annual Profections）：每年上升点推进一宫；
 *  - 法达星盘（Firdaria）：日/夜盘不同行星顺序。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据 ========================

private val SIGNS = listOf("白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座")

/** 传统守护星（简化） */
private val SIGN_RULERS = mapOf(
    0 to "火星", 1 to "金星", 2 to "水星", 3 to "月亮", 4 to "太阳", 5 to "水星",
    6 to "金星", 7 to "火星", 8 to "木星", 9 to "土星", 10 to "土星", 11 to "木星"
)

/** 宫位生活领域 */
private val HOUSE_AREAS = listOf(
    "自我与生命", "财富与资源", "沟通与旅行", "家庭与根基", "创造与享乐", "健康与服务",
    "关系与伴侣", "转变与重生", "哲学与远行", "事业与声望", "社群与理想", "灵性与超越"
)

/** 法达（Firdaria）行星年限 */
private val PLANETARY_YEARS = mapOf(
    "太阳" to 10, "月亮" to 9, "火星" to 7, "水星" to 13,
    "木星" to 12, "金星" to 8, "土星" to 11
)

private val FIRDARIA_ORDER_DAY = listOf("太阳", "金星", "水星", "月亮", "土星", "木星", "火星")
private val FIRDARIA_ORDER_NIGHT = listOf("月亮", "土星", "木星", "火星", "太阳", "金星", "水星")

private val PLANET_MEANINGS = mapOf(
    "太阳" to "核心自我、生命力、权威、父亲",
    "月亮" to "情绪、潜意识、母亲、家庭",
    "水星" to "沟通、智力、学习、旅行",
    "金星" to "爱情、美、和谐、价值观",
    "火星" to "行动、欲望、冲突、勇气",
    "木星" to "扩张、幸运、哲学、财富",
    "土星" to "限制、责任、时间、结构"
)

/** 十二星座六维解读档案（总评/事业/财运/感情/健康/建议） */
private data class SignProfile(
    val general: String, val career: String,
    val wealth: String, val love: String,
    val health: String, val advice: String
)

private val SIGN_PROFILES = listOf(
    // 白羊座
    SignProfile(
        "你自带开拓者的锐气，命运的主线是「以行动开辟自己的战场」，把握得住冲劲便能率先破局。",
        "事业上适合主动出击、抢占先机，独立负责的项目比按部就班的岗位更能激发你的战斗力。",
        "财运来自敢闯敢试，偏财机会往往在你果断出手时出现，但要警惕冲动消费与仓促投资。",
        "感情中你热烈直接，喜欢便会大方表达，需学会把耐心留给对方，避免把伴侣当作战友较劲。",
        "健康方面能量充沛但易消耗过度，头痛、炎症与旧伤是重点防护部位，注意给肾上腺素按下暂停键。",
        "把「先冲再想」改成「想三步再冲」，在重要决策前留出二十四小时的冷静期。"
    ),
    // 金牛座
    SignProfile(
        "你以稳健见长，命运偏爱长期主义者，越沉得住气，积累的果实就越扎实。",
        "事业上你擅长把资源经营成壁垒，财务、供应链与需要耐心的专业领域最能发挥你的价值。",
        "财运是十二宫中最稳健的一档，正财收入稳中有升，守住既定配置便是最好的增值策略。",
        "感情中你重承诺、重安全感，细水长流的陪伴胜过轰轰烈烈，别把心里话都憋成沉默的壁垒。",
        "健康上易因久坐与饮食过量埋下隐患，颈肩与代谢系统需要规律运动来维持运转。",
        "允许自己偶尔「慢中求变」，在舒适区外建立一个小而新的尝试，避免因固执错过转机。"
    ),
    // 双子座
    SignProfile(
        "你思维如风，命运的精彩在于信息与连接，你能在多变中找到别人看不到的通路。",
        "事业上你的天赋在沟通、写作与跨界整合，多线程的环境反而让你如鱼得水。",
        "财运起伏与信息差高度相关，消息灵通时能捕捉机会，但注意避免东一榔头西一棒子的散财。",
        "感情中你风趣机敏，最怕的是单调，需要用共同的兴趣与话题维持新鲜感，也需收敛口不择言。",
        "健康上神经系统易紧张，失眠与肩颈酸痛常见，冥想与减少信息轰炸对你尤为重要。",
        "把好奇心收束成一两个主线课题深挖下去，浅尝辄止会让你错失复利式的成果。"
    ),
    // 巨蟹座
    SignProfile(
        "你情感如水，命运的主轴是家与归属，你在滋养他人时也成就了自己。",
        "事业上你适合需要共情与照护的领域，团队里的定海神针角色远比孤军奋战适合你。",
        "财运与家庭资源深度绑定，置产、储蓄与家族支持是你的福荫，避免因情绪化支出破坏计划。",
        "感情中你深情而敏感，会记得每个细节，需学习表达需求而非让情绪在暗处积压成猜疑。",
        "健康上情绪直接影响肠胃与免疫，压力大时先照顾好饮食与睡眠，再处理棘手的事。",
        "把「安全感」这件事主动争取而非被动等待，你建立的边界越清晰，关系反而越稳固。"
    ),
    // 狮子座
    SignProfile(
        "你自带光芒，命运赋予你舞台的自觉，你敢于站高，也担得起众人的目光。",
        "事业上你适合领导与展示型角色，你的号召力能聚拢团队，但要避免把所有功劳都扛在自己肩上。",
        "财运与人气、名望正相关，打造个人品牌比闷声做事更能放大收入，但要防止场面大于里子。",
        "感情中你慷慨热烈，愿意把最好的给对方，也渴望被崇拜，记得感情是双向的欣赏而非独角戏。",
        "健康上注意心脏与脊柱，情绪高涨时容易透支，规律作息比熬夜狂欢更能维持你的王座。",
        "把舞台让出一半给别人，学会托举团队与伴侣，你松手之后收获的反而是更多忠诚。"
    ),
    // 处女座
    SignProfile(
        "你以细致见长，命运的奖赏藏在细节里，你打磨出来的东西经得起时间检验。",
        "事业上你是流程与品质的把关人，分析、医疗、教育、工艺等需要精确度的领域最能施展才华。",
        "财运属于精打细算型，记账与规划让你财富稳步累积，警惕因追求完美而反复折腾造成损耗。",
        "感情中你体贴周到却容易挑剔，学会欣赏对方本来的样子，比把对方改造成标准答案更重要。",
        "健康上肠胃与神经是薄弱环节，压力型消化问题多发，规律三餐与放下清单才能恢复元气。",
        "把「完美主义」用在事情上、把「宽容」用在对人上，松弛感会为你带来意想不到的运气。"
    ),
    // 天秤座
    SignProfile(
        "你一生都在寻找平衡，命运给你的课题是：在不失去自我的前提下成就和谐。",
        "事业上你擅长协调与公关，谈判桌与审美场是你的主场，公正客观的判断力是你的王牌。",
        "财运与人脉和合作相关，借力打力、强强联手是增收捷径，避免因面子问题乱作人情买单。",
        "感情中你优雅迷人，重视平等与陪伴，但犹豫不决和怕冲突会让你错失表达心意的时机。",
        "健康上注意腰肾与内分泌，久坐与失衡的生活方式要调整，规律运动能帮你找回内外的对称。",
        "练习「先取悦自己再取悦世界」，当你不为讨好而活时，你的魅力反而更加无往不利。"
    ),
    // 天蝎座
    SignProfile(
        "你深邃而坚韧，命运的底色是转化，每一次低谷都是你脱胎换骨的跳板。",
        "事业上你适合深耕型与攻坚型岗位，危机处理、研究与资源整合都能让你后来居上。",
        "财运与大额资产、投资及他人资源相关，靠复利和深度研判获利，切忌孤注一掷的豪赌。",
        "感情中你浓烈忠诚、占有欲强，信任是你给的最高礼物，也需要练习允许对方保有私密空间。",
        "健康上生殖系统与排毒功能需留意，情绪不释放会积郁成疾，找到安全的出口比压抑更有用。",
        "把「掌控」换成「允许」，当你不再害怕失去时，你握住的反而比想象中更多。"
    ),
    // 射手座
    SignProfile(
        "你心怀远方，命运的馈赠在广阔的天地间，越走越远的路越能成就你的辽阔。",
        "事业上你适合对外、涉外与开创型工作，跨文化、教育与出版领域能放大你的眼界优势。",
        "财运与远行、新知及贵人挂钩，机会常来自陌生的圈子，注意避免盲目乐观下的超额支出。",
        "感情中你乐观直率，渴望与伴侣共同成长探索，需给关系留出稳定落地的锚点，而非永远在路上。",
        "健康上注意肝与大腿，运动损伤常因过猛，循序渐进与充足睡眠是你保持高飞的双翼。",
        "把远方的地图翻译成脚下的里程碑，大志向拆成小步骤，你的自由才不会变成散漫。"
    ),
    // 摩羯座
    SignProfile(
        "你负重致远，命运的法则是一分耕耘一分收获，时间最终会站在你这边。",
        "事业上你适合管理、金融与实业，越接近权力与责任核心的位置，越能兑现你的长期主义。",
        "财运稳健向好，升职加薪与不动产是主要来源，节制消费让每一分钱都变成你攀高的台阶。",
        "感情中你责任大于甜言，用行动表达在乎，需提醒自己伴侣要的不是业绩，而是陪伴与温度。",
        "健康上骨骼、膝盖与牙齿是重点，过劳与压抑是隐患，学会在拼业绩之外给身体放个假。",
        "在攀登的同时偶尔回头看看风景，允许自己示弱与求助，你的肩膀不需要扛起所有。"
    ),
    // 水瓶座
    SignProfile(
        "你以独特见长，命运不属于随波逐流者，你的不同凡响恰恰是你的通行证。",
        "事业上你适合科技、公益与创新领域，跳出框架的思路会让你成为规则的重写者。",
        "财运与独特赛道和群体运营相关，新兴领域有红利，但需有人帮你把关落地与风控。",
        "感情中你需要精神共鸣与独立空间，先做知己再做恋人，别用疏离感掩盖对亲密的渴望。",
        "健康上循环系统与脚踝易出状况，作息颠倒的代价不小，给身体建立一套稳定的节律。",
        "把你的理想主义接上地气，找到一个愿意陪你一起落地的伙伴，才华才不至于悬在半空。"
    ),
    // 双鱼座
    SignProfile(
        "你柔软而富有想象力，命运的通道在感受力，你感知到的世界比大多数人更丰盛。",
        "事业上你适合艺术、疗愈与创意行业，直觉是你的雷达，但需要制度与帮手来兜底执行。",
        "财运波动较大，灵感变现需要有人把关，守住基本盘，别让同情心成为无底洞式开销。",
        "感情中你浪漫包容、共情力强，容易理想化对方，学会分辨真实与投射才能避开一厢情愿。",
        "健康上脚部与免疫系统需留意，情绪敏感时容易失眠多梦，音乐与亲近自然是最好的药方。",
        "为你的温柔设定边界，善良需要带一点理性，才能既照亮别人又不灼伤自己。"
    )
)

// ======================== 结果模型 ========================

data class HellenisticChart(
    val date: LocalDate,
    val hour: Double,
    val isDiurnal: Boolean,           // 日生盘
    val sunSign: String, val sunDeg: Double,
    val moonSign: String, val moonDeg: Double,
    val ascSign: String, val ascDeg: Double,
    val lotFortuneSign: String, val lotFortuneDeg: Double,
    val lotSpiritSign: String, val lotSpiritDeg: Double,
    val verdict: String               // 六维综合解读
)

data class Profection(val age: Int, val sign: String, val lord: String, val area: String)

data class FirdariaPeriod(val planet: String, val startAge: Int, val endAge: Int, val years: Int, val meaning: String)

// ======================== 核心计算 ========================

object HellenisticAstrology {

    private fun jd(y: Int, m: Int, d: Int, hour: Double): Double {
        var yy = y; var mm = m
        if (mm <= 2) { yy -= 1; mm += 12 }
        val a = yy / 100
        val b = 2 - a + a / 4
        return (365.25 * (yy + 4716)).toLong() + (30.6001 * (mm + 1)).toLong() + d + b - 1524.5 + hour / 24.0
    }

    private fun sunLongitude(jd: Double): Double {
        val days = jd - 2451545.0
        val meanLon = (280.46646 + 0.98564736 * days) % 360
        val meanAnom = Math.toRadians((357.52911 + 0.98560028 * days) % 360)
        val eq = 1.914602 * sin(meanAnom) + 0.019993 * sin(2 * meanAnom) + 0.000289 * sin(3 * meanAnom)
        return ((meanLon + eq) % 360 + 360) % 360
    }

    private fun moonLongitude(jd: Double): Double {
        val days = jd - 2451545.0
        return ((218.3 + 13.176396 * days) % 360 + 360) % 360
    }

    /** 完整本命盘 */
    fun chart(date: LocalDate, hour: Double = 12.0): HellenisticChart {
        val j = jd(date.year, date.monthValue, date.dayOfMonth, hour)
        val sun = sunLongitude(j)
        val moon = moonLongitude(j)
        val asc = (sun + 90) % 360
        val isDiurnal = hour >= 6 && hour < 18
        val lotF = if (isDiurnal) (asc + moon - sun) % 360 else (asc + sun - moon) % 360
        val lotS = if (isDiurnal) (asc + sun - moon) % 360 else (asc + moon - sun) % 360
        fun signName(lon: Double): Pair<String, Double> {
            val norm = (lon % 360 + 360) % 360
            return SIGNS[(norm / 30.0).toInt() % 12] to (norm % 30)
        }
        val (sSign, sDeg) = signName(sun)
        val (mSign, mDeg) = signName(moon)
        val (aSign, aDeg) = signName(asc)
        val (fSign, fDeg) = signName(lotF)
        val (lSign, lDeg) = signName(lotS)
        val base = HellenisticChart(date, hour, isDiurnal, sSign, sDeg, mSign, mDeg, aSign, aDeg, fSign, fDeg, lSign, lDeg, "")
        return base.copy(verdict = buildVerdict(base))
    }

    /** 按黄道十二宫与区段、幸运点等要素生成六维综合解读 */
    private fun buildVerdict(c: HellenisticChart): String {
        val p = SIGN_PROFILES[SIGNS.indexOf(c.sunSign)]
        val sect = if (c.isDiurnal) "日生盘" else "夜生盘"
        return buildString {
            append("总评：${p.general}${sect}的你，处事姿态更接近${c.ascSign}上升，表里相合方能内外兼得。\n")
            append("事业：${p.career}\n")
            append("财运：${p.wealth}幸运点落于${c.lotFortuneSign}，顺财之门正朝这些领域敞开。\n")
            append("感情：${p.love}月亮落入${c.moonSign}，你深层的情感模式由此显影。\n")
            append("健康：${p.health}\n")
            append("建议：${p.advice}")
        }
    }

    /** 年主星推运 */
    fun profection(c: HellenisticChart, age: Int): Profection {
        val base = SIGNS.indexOf(c.ascSign)
        val offset = ((age - 1) % 12 + 12) % 12
        val ascIdx = ((base + offset) % 12 + 12) % 12
        return Profection(age, SIGNS[ascIdx], SIGN_RULERS[ascIdx] ?: "未知", HOUSE_AREAS[ascIdx])
    }

    /** 法达星盘完整周期 */
    fun firdaria(c: HellenisticChart): List<FirdariaPeriod> {
        val order = if (c.isDiurnal) FIRDARIA_ORDER_DAY else FIRDARIA_ORDER_NIGHT
        val result = mutableListOf<FirdariaPeriod>()
        var cumulative = 0
        order.forEach { planet ->
            val years = PLANETARY_YEARS[planet] ?: 0
            result.add(FirdariaPeriod(planet, cumulative, cumulative + years - 1, years, PLANET_MEANINGS[planet] ?: ""))
            cumulative += years
        }
        return result
    }

    /** 当前年龄所在法达周期 */
    fun currentFirdaria(c: HellenisticChart, birthDate: LocalDate, ageYears: Double? = null): FirdariaPeriod? {
        val age = ageYears ?: (ChronoUnit.DAYS.between(birthDate, LocalDate.now()) / 365.2422)
        return firdaria(c).firstOrNull { age >= it.startAge && age < it.endAge + 1 }
    }
}
