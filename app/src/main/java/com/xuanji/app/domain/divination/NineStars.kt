package com.xuanji.app.domain.divination

import java.time.LocalDate

/**
 * 九星気学（日本版八字）确定性计算。
 * 规则：
 *  - 公历年份以立春（约 2/4）为年度分界；生日在立春前按前一年计算。
 *  - 取 (计算年份 % 9) 的余数，按固定映射得到一白水～九紫火九颗本命星。
 *  - 每星各有五行与本位方位；神社属性以主祭神之五行为主、方位为辅。
 *  - 推荐：生我（该元素生我）·大吉、同我（同元素）·吉；克我（该元素克我）·回避。
 * 全部为离线确定性算法，无随机，仅供文化娱乐参考。
 */

data class NineStar(
    val number: Int,      // 1..9 星序
    val name: String,     // 一白水
    val element: String,  // 水
    val direction: String,// 北
    val summary: String
)

data class Shrine(
    val name: String,
    val deity: String,    // 主祭神
    val element: String,  // 五行（以祭神为主判定）
    val direction: String // 方位
)

data class ShrineMatch(
    val shrine: Shrine,
    val relation: String, // 生我·大吉 / 同我·吉 / 平 / 克我·回避
    val priority: Int     // 排序用，越大越优先
)

data class NineStarResult(
    val birthYear: Int,
    val adjustedYear: Int, // 经立春校正后的计算年份
    val star: NineStar,
    val matches: List<ShrineMatch>, // 已排序：大吉 > 吉 > 平 > 回避
    val verdict: String    // 六维解读（总评/事业/财运/感情/健康/建议）
)

object NineStars {

    // 余数(year%9) -> 本命星（映射以给定规则为准）
    private val STARS = mapOf(
        0 to NineStar(2, "二黒土", "土", "西南", "沉稳包容、承载万物，主诚信与务实，亦主田宅根基。"),
        1 to NineStar(1, "一白水", "水", "北", "聪明灵动、善谋变，主智慧与人际流通，亦主人缘桃花。"),
        2 to NineStar(9, "九紫火", "火", "南", "热情聪慧、具感染力，主名声与文昌，亦主礼仪文明。"),
        3 to NineStar(8, "八白土", "土", "东北", "厚实稳健、积善成山，主财富与积蓄，亦主诚信持久。"),
        4 to NineStar(7, "七赤金", "金", "西", "伶俐善言、口才出众，主交际与口福，亦主变动机敏。"),
        5 to NineStar(6, "六白金", "金", "西北", "刚健果决、领导有方，主权威与功名，亦主贵气。"),
        6 to NineStar(5, "五黄土", "土", "中", "居中统御、厚重核心，主中枢与稳定，宜静不宜动。"),
        7 to NineStar(4, "四緑木", "木", "东南", "柔和仁慈、生机勃发，主文昌与成长，亦主仁德。"),
        8 to NineStar(3, "三碧木", "木", "东", "直率进取、朝气蓬勃，主行动与开拓，亦主竞争。")
    )

    // 内置神社库（祭神为主、方位为辅判五行；文化示意，非考据定论）
    private val SHRINES = listOf(
        Shrine("伊勢神宮", "天照大御神", "火", "东南"),
        Shrine("出雲大社", "大国主大神", "土", "西"),
        Shrine("伏見稲荷大社", "稲荷大神", "木", "南"),
        Shrine("厳島神社", "宗像三女神", "水", "西"),
        Shrine("熱田神宮", "草薙剣", "金", "南"),
        Shrine("春日大社", "武甕槌命", "木", "南"),
        Shrine("浅草寺", "聖観音", "水", "东"),
        Shrine("日光東照宮", "徳川家康", "金", "北"),
        Shrine("熊野那智大社", "熊野牟須美大神", "水", "南"),
        Shrine("諏訪大社", "建御名方神", "水", "中"),
        Shrine("松尾大社", "大山咋神", "木", "南"),
        Shrine("大神神社", "大物主神", "土", "中")
    )

    // 五行生：a 生 b
    private val GENERATES = setOf(
        "木" to "火", "火" to "土", "土" to "金", "金" to "水", "水" to "木"
    )
    // 五行克：a 克 b
    private val CONTROLS = setOf(
        "木" to "土", "土" to "水", "水" to "火", "火" to "金", "金" to "木"
    )

    /** 各本命星 → 六维解读（离线确定性） */
    private val STAR_VERDICT = mapOf(
        1 to "总评：一白水星当令，智慧灵动、人缘桃花皆旺，运势柔中有进。\n事业：利策划、创意、人际流通之事，宜以智取胜、广结善缘。\n财运：财随人脉而来，偏财机缘多，宜灵活布局、见好就收。\n感情：桃花星临，单身者异性缘佳，已婚者须把握分寸、以专一为上。\n健康：水主肾与泌尿，宜防寒湿，注意睡眠与腰部保养。\n建议：宜多向北、向水方位发展，谦和灵动，忌投机取巧。",
        2 to "总评：二黒土性主沉稳诚信，宜守不宜攻，稳中自有福泽。\n事业：利田宅、地产、务实管理之业，按部就班自有成就。\n财运：财宜积累、宜置业，长线投资可期，忌贪快冒进。\n感情：感情重实不重虚，以真心相待，平淡之中见真情。\n健康：土主脾胃，注意饮食规律，忌忧思过度伤脾。\n建议：宜向西南方取气，持重守正、待时而动。",
        3 to "总评：三碧木气主行动开拓，朝气蓬勃，宜冲宜闯、动中求成。\n事业：利开创、竞争、竞技之业，行动力就是生产力。\n财运：财从进取中来，宜主动出击，但防冲动破耗。\n感情：直率热情，宜多些细腻体贴，避免口角争执。\n健康：木主肝胆，少熬夜、多疏解压力，忌动怒伤身。\n建议：宜向东、向木方位开拓，锋芒略敛、稳中求快。",
        4 to "总评：四緑文昌星临，柔和仁慈、生机勃发，利学业功名。\n事业：利文教、传媒、设计之业，才华可期、步步高升。\n财运：文昌生财，宜凭才艺与学识变现，忌急功近利。\n感情：仁厚温和、人缘佳，感情宜多交流、共同成长。\n健康：木气舒畅，注意作息与用眼，宜多亲近自然。\n建议：宜向东南方取文昌之气，静心修学、厚积薄发。",
        5 to "总评：五黄居中统御、力量厚重，宜静不宜动、以稳为吉。\n事业：利中枢统筹、核心要务，宜居中调度、不宜临阵冒进。\n财运：财宜守不宜攻，防意外破耗，理财以稳健为先。\n感情：重心在责任与稳定，宜多陪伴，忌冷淡疏远。\n健康：中宫土气，注意脾胃与整体调理，宜静养安神。\n建议：宜静守本宫、以不动应万变，大事缓办、化险为夷。",
        6 to "总评：六白金性刚健果决、主权威功名，宜掌大任、谋大事。\n事业：利领导、管理、武职军警，宜担重任、果断决策。\n财运：财随权贵而来，正财可期，宜以实力谋财、取之有道。\n感情：威严有余、柔情不足，宜多放下身段表达关爱。\n健康：金主肺与筋骨，注意呼吸道与关节保养。\n建议：宜向西北方取气，刚柔并济、以德服人。",
        7 to "总评：七赤金主交际口才，伶俐善言，人脉生财之象。\n事业：利销售、演艺、中介之业，口才就是生产力。\n财运：财从口来、变通中得，宜抓住人际商机，防破耗。\n感情：魅力四射、桃花不断，宜专一守情，忌暧昧误人。\n健康：金性主肺，注意呼吸道与声带保养。\n建议：宜向西取金气，善用口才但忌巧言令色、防口舌是非。",
        8 to "总评：八白当运，厚实稳健、主财富积蓄，正是发财旺运之星。\n事业：利实业、金融、地产之业，根基扎实、步步为营。\n财运：本命财星，正财大旺，宜置业储蓄、长线布局。\n感情：稳重专一，能给足安全感，宜主动表达柔情。\n健康：土主脾胃，注意饮食有节，防积食与湿气。\n建议：宜向东北方取气，务实积累，财不外露、守中得富。",
        9 to "总评：九紫火主名声文明，热情聪慧、具感染力，声名鹊起之年。\n事业：利传媒、演艺、文化之业，宜打造个人品牌、扬名立万。\n财运：名到财来，利品牌与流量变现，忌虚火浮躁。\n感情：热烈浪漫、魅力十足，宜真诚专一，忌三分钟热度。\n健康：火主心脏与眼目，注意血压与用眼，忌熬夜上火。\n建议：宜向南取火气，趁势扬名，修内敛之德以配其位。"
    )

    // 立春近似日（公历每年约 2/4，足够本命星分界使用）
    private fun startOfSpring(year: Int): LocalDate = LocalDate.of(year, 2, 4)

    fun calculate(birth: LocalDate): NineStarResult {
        val year = birth.year
        val adjusted = if (birth.isBefore(startOfSpring(year))) year - 1 else year
        val r = ((adjusted % 9) + 9) % 9
        val star = STARS[r] ?: STARS[0]!!
        val matches = matchShrines(star.element, star.direction)
        val verdict = STAR_VERDICT[star.number] ?: "总评：本命星气内敛，宜守正而行。\n事业：宜结合自身五行取长补短，稳中求进。\n财运：宜量入为出，长线布局。\n感情：宜真诚相待，细水长流。\n健康：注意作息规律、动静结合。\n建议：顺星而为，趋吉避凶，以平常心处世。"
        return NineStarResult(year, adjusted, star, matches, verdict)
    }

    private fun relation(userWx: String, shrineWx: String): Pair<String, Int> {
        return when {
            shrineWx == userWx -> "同我·吉" to 2
            GENERATES.contains(shrineWx to userWx) -> "生我·大吉" to 3
            CONTROLS.contains(shrineWx to userWx) -> "克我·回避" to 0
            else -> "平" to 1
        }
    }

    private fun matchShrines(userWx: String, userDir: String): List<ShrineMatch> {
        return SHRINES.map { s ->
            val (rel, base) = relation(userWx, s.element)
            // 神社方位与本命星本位方位相同则加分（方位为辅）
            val bonus = if (s.direction == userDir) 1 else 0
            ShrineMatch(s, rel, base + bonus)
        }.sortedWith(
            compareByDescending<ShrineMatch> { it.priority }
                .thenBy { it.shrine.name }
        )
    }
}
