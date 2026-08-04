package com.xuanji.app.domain.divination

import java.time.LocalDate

/**
 * 日本阴阳道 · 本命星与属星（当年星）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 出生年地支（生肖）→ 本命星（北斗七星：贪狼/巨门/禄存/文曲/廉贞/武曲/破军）；
 *  - 立春调年：1 月出生按前一年，2 月简化按前一年（真实立春在 2/3–2/5，此处为简化规则）；
 *  - 九曜属星（当年星）：以虚岁对九曜（日月火水木金土罗睺计都）取模。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据映射 ========================

/** 十二地支 → 本命星（北斗七星） */
private val EARTHLY_BRANCH_TO_HONMEI = mapOf(
    "子" to "贪狼星", // 鼠
    "丑" to "巨门星", // 牛
    "寅" to "禄存星", // 虎
    "卯" to "文曲星", // 兔
    "辰" to "廉贞星", // 龙
    "巳" to "武曲星", // 蛇
    "午" to "破军星", // 马
    "未" to "武曲星", // 羊
    "申" to "廉贞星", // 猴
    "酉" to "文曲星", // 鸡
    "戌" to "禄存星", // 狗
    "亥" to "巨门星"  // 猪
)

/** 北斗七星 → 星君全称 */
private val BIG_DIPPER_STARS = mapOf(
    "贪狼星" to "天枢（贪狼星君）",
    "巨门星" to "天璇（巨门星君）",
    "禄存星" to "天玑（禄存星君）",
    "文曲星" to "天权（文曲星君）",
    "廉贞星" to "玉衡（廉贞星君）",
    "武曲星" to "开阳（武曲星君）",
    "破军星" to "瑶光（破军星君）"
)

/** 北斗七星 → 本命星解读（性格/运势基调） */
private val HONMEI_MEANING = mapOf(
    "贪狼星" to "主欲望与桃花：聪明机智、善交际，具领袖魅力与开拓精神；宜进取创新，但需节制贪求、沉淀心性，方成大事。",
    "巨门星" to "主口才与思辨：观察入微、能言善道，利分析研究；唯易惹口舌是非，宜以诚待人、谨言慎行。",
    "禄存星" to "主财禄与稳定：沉稳持重、福禄绵长，善守成经营；宜务实积累，忌好高骛远，稳中自有富贵。",
    "文曲星" to "主文采与才艺：聪慧好学、气质儒雅，利学业考试与文艺创作；宜发挥才智，忌恃才傲物。",
    "廉贞星" to "主威严与责任：刚正果断、敢于担当，能掌权责；宜刚柔并济、以德服人，避免过于刚烈。",
    "武曲星" to "主武勇与财富：果敢务实、执行力强，利事业与财运；宜脚踏实地、持之以恒，切忌半途而废。",
    "破军星" to "主变革与开创：敢闯敢拼、善破旧立新，多变动亦多机遇；宜稳中求变，戒骄戒躁，方能守成。"
)

/** 九曜（用于属星/当年星） */
private val NAVAGRAHA = listOf("日", "月", "火", "水", "木", "金", "土", "罗睺", "计都")

/** 生肖中文名 */
private val ZODIAC_NAMES = mapOf(
    "子" to "鼠", "丑" to "牛", "寅" to "虎", "卯" to "兔",
    "辰" to "龙", "巳" to "蛇", "午" to "马", "未" to "羊",
    "申" to "猴", "酉" to "鸡", "戌" to "狗", "亥" to "猪"
)

private val DI_ZHI = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

/** 九曜意象（用于展示属星含义） */
private val NAVAGRAHA_MEANING = mapOf(
    "日" to "太阳：光明权威，主名声与事业，宜积极进取。",
    "月" to "月亮：温和滋养，主人缘与情绪，宜守成养望。",
    "火" to "火星：行动果决，主竞争与开拓，注意冲动。",
    "水" to "水星：聪慧善言，主学习与交流，利才艺口才。",
    "木" to "木星：仁厚广博，主贵人财富，利扩张与教育。",
    "金" to "金星：优雅富足，主审美人际，利婚恋与协作。",
    "土" to "土星：稳重担当，主责任积累，宜务实守业。",
    "罗睺" to "罗睺：隐伏多变，主突变与执念，宜谨慎抉择。",
    "计都" to "计都：业力收束，主纠葛与转化，宜静心沉淀。"
)

/** 本命星 → 总评（六维解读用） */
private val HONMEI_OVERALL = mapOf(
    "贪狼星" to "贪狼主欲望与桃花，一生人缘通达、机遇频生，贵在节制贪求以成大器。",
    "巨门星" to "巨门主口才思辨，利以智谋立身，唯须谨言慎行、以诚待人。",
    "禄存星" to "禄存主财禄稳定、福泽绵长，宜守成经营、稳中求富。",
    "文曲星" to "文曲主文采才艺，利学业功名，宜以才学立世、忌恃才傲物。",
    "廉贞星" to "廉贞主威严责任，刚正敢当，宜刚柔并济、以德服人。",
    "武曲星" to "武曲主武勇财富，果敢务实，利事业财运，忌半途而废。",
    "破军星" to "破军主变革开创，敢破敢立，多变动亦多机遇，宜稳中求变。"
)

/** 本命星 → 感情维度解读 */
private val HONMEI_LOVE = mapOf(
    "贪狼星" to "感情：桃花旺而缘多，宜专一守心，情缘自顺。",
    "巨门星" to "感情：爱在心口难开，宜主动表达，忌口舌误情。",
    "禄存星" to "感情：情感稳定专一，宜以行动表爱，细水长流。",
    "文曲星" to "感情：才情吸引异性，宜以真诚相处，忌恃才挑剔。",
    "廉贞星" to "感情：爱得刚烈，宜多些温柔体谅，以柔化刚。",
    "武曲星" to "感情：重情重诺，宜多留时间陪伴，勿以忙碌疏远。",
    "破军星" to "感情：爱憎分明、敢爱敢恨，宜平心静气、勿急勿躁。"
)

/** 本命星 → 健康维度解读 */
private val HONMEI_HEALTH = mapOf(
    "贪狼星" to "健康：木性生发，注意肝胆与作息，宜疏肝解郁。",
    "巨门星" to "健康：土性主脾胃，注意消化系统，宜饮食有节。",
    "禄存星" to "健康：土性主脾胃，福泽之人更须防积滞，宜少食多动。",
    "文曲星" to "健康：水性主肾，注意肾水与泌尿，宜保暖防寒。",
    "廉贞星" to "健康：火性主心，注意心火与血压，宜平心静气。",
    "武曲星" to "健康：金性主肺，注意呼吸道与筋骨，宜润肺养身。",
    "破军星" to "健康：变动之星，注意劳逸结合，防意外磕碰。"
)

/** 属星（九曜）→ 事业维度解读 */
private val ZOKUSEI_CAREER = mapOf(
    "日" to "事业：当年星值日，主名声与事业，宜积极进取、树立权威。",
    "月" to "事业：当年星值月，宜守成养望、以稳健积累为主。",
    "火" to "事业：当年星值火，行动果决，利竞争开拓，忌冲动误事。",
    "水" to "事业：当年星值水，利学习交流与信息行业，以智取胜。",
    "木" to "事业：当年星值木，仁厚广博，利扩张合作与教育事业。",
    "金" to "事业：当年星值金，利商务金融、审美创意之业，宜协作共赢。",
    "土" to "事业：当年星值土，宜务实守业、深耕积累，根基自固。",
    "罗睺" to "事业：当年星值罗睺，易有突变与执念，宜谨慎抉择、顺势而变。",
    "计都" to "事业：当年星值计都，主收束与转化，宜整理旧务、静心沉淀。"
)

/** 属星（九曜）→ 财运维度解读 */
private val ZOKUSEI_WEALTH = mapOf(
    "日" to "财运：阳光之财，名正言顺，正财可期。",
    "月" to "财运：财来温缓，宜细水长流、积少成多。",
    "火" to "财运：财如烈火，来得快也去得快，宜见好就收。",
    "水" to "财运：财路灵活多变，宜多元经营、防财来财去。",
    "木" to "财运：仁德生财，利长期投资与人脉之财。",
    "金" to "财运：金玉之财，宜储蓄与贵重资产，忌奢靡耗散。",
    "土" to "财运：厚土生财，宜置业积财、稳中增值。",
    "罗睺" to "财运：财有暗变，宜守财防漏，忌高风险投机。",
    "计都" to "财运：财气收束，宜理清旧账、量入为出。"
)

// ======================== 结果模型 ========================

data class OnmyodoHonmei(
    val birthYear: Int,
    val adjustedYear: Int,   // 经立春调整后的计算年份
    val branch: String,      // 地支
    val zodiac: String,      // 生肖
    val star: String,        // 本命星（贪狼星）
    val starFull: String,    // 星君全称（天枢（贪狼星君））
    val meaning: String      // 本命星解读
)

data class OnmyodoZokusei(
    val age: Int,            // 虚岁
    val star: String,        // 属星（当年星）
    val index: Int,          // 九曜索引 0..8
    val meaning: String
)

data class OnmyodoProfile(
    val honmei: OnmyodoHonmei,
    val zokusei: OnmyodoZokusei,
    val verdict: String   // 六维解读（总评/事业/财运/感情/健康/建议）
)

// ======================== 核心计算 ========================

object Onmyodo {

    /** 立春调年：1 月出生算前一年；2 月简化按前一年（真实需算节气） */
    fun adjustedYear(birthYear: Int, birthMonth: Int?): Int {
        if (birthMonth == null) return birthYear
        return if (birthMonth == 1 || birthMonth == 2) birthYear - 1 else birthYear
    }

    /** 根据年份取地支（基准：2020 年 = 庚子年 = 子） */
    fun earthlyBranch(year: Int): String {
        val baseYear = 2020
        val diff = year - baseYear
        val idx = ((0 + diff) % 12 + 12) % 12 // 防负
        return DI_ZHI[idx]
    }

    /** 本命星信息 */
    fun honmei(birthYear: Int, birthMonth: Int? = null): OnmyodoHonmei {
        val adjusted = adjustedYear(birthYear, birthMonth)
        val branch = earthlyBranch(adjusted)
        val zodiac = ZODIAC_NAMES[branch] ?: "未知"
        val star = EARTHLY_BRANCH_TO_HONMEI[branch] ?: "未知"
        val starFull = BIG_DIPPER_STARS[star] ?: star
        val meaning = HONMEI_MEANING[star] ?: ""
        return OnmyodoHonmei(
            birthYear = birthYear,
            adjustedYear = adjusted,
            branch = branch,
            zodiac = zodiac,
            star = star,
            starFull = starFull,
            meaning = meaning
        )
    }

    /** 属星（当年星）：以虚岁推算，虚岁 = 当前年 - 出生年 + 1 */
    fun zokusei(birthYear: Int, age: Int? = null): OnmyodoZokusei {
        val currentYear = LocalDate.now().year
        val realAge = age ?: (currentYear - birthYear + 1)
        val idx = ((realAge - 1) % 9 + 9) % 9 // 防负
        val star = NAVAGRAHA[idx]
        return OnmyodoZokusei(
            age = realAge,
            star = star,
            index = idx,
            meaning = NAVAGRAHA_MEANING[star] ?: ""
        )
    }

    /** 完整档案 */
    fun profile(birthYear: Int, birthMonth: Int? = null, age: Int? = null): OnmyodoProfile {
        val h = honmei(birthYear, birthMonth)
        val z = zokusei(birthYear, age)
        return OnmyodoProfile(h, z, buildVerdict(h.star, z.star))
    }

    /** 六维解读：总评/事业/财运/感情/健康/建议（按本命星×属星确定性生成） */
    private fun buildVerdict(honmeiStar: String, zokuseiStar: String): String {
        val zong = "总评：本命${honmeiStar}（${HONMEI_OVERALL[honmeiStar] ?: "星性内敛"}）" +
            "，今年属星为「$zokuseiStar」，本命主其基、属星主其势，本末兼修则运势通达。"
        val career = ZOKUSEI_CAREER[zokuseiStar] ?: "事业：宜结合本命星性，稳中求进、扬长避短。"
        val wealth = ZOKUSEI_WEALTH[zokuseiStar] ?: "财运：宜量入为出，长线布局、忌贪快冒进。"
        val love = HONMEI_LOVE[honmeiStar] ?: "感情：宜真诚相待、多些陪伴，细水长流。"
        val health = HONMEI_HEALTH[honmeiStar] ?: "健康：宜规律作息、动静结合，防微杜渐。"
        val advice = "建议：本命「$honmeiStar」取其长，当年「$zokuseiStar」避其短；大事谋定后动，平日顺星而为，自可趋吉避凶。"
        return listOf(zong, career, wealth, love, health, advice).joinToString("\n")
    }

    /** 十二生肖 → 本命星 对照表（供展示） */
    fun honmeiTable(): List<Triple<String, String, String>> =
        DI_ZHI.map { dz ->
            val zodiac = ZODIAC_NAMES[dz] ?: "?"
            val star = EARTHLY_BRANCH_TO_HONMEI[dz] ?: "?"
            val full = BIG_DIPPER_STARS[star] ?: star
            Triple("$dz（$zodiac）", star, full)
        }
}
