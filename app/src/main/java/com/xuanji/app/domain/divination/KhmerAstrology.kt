package com.xuanji.app.domain.divination

import java.time.LocalDate
import java.lang.Math.floorMod

/**
 * 高棉占星（Khmer Astrology / Chhankitek 农历）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 公历 → 高棉农历（简化：以 4 月 14 日新年为界，高棉历纪元 = 公元 - 638/637）；
 *  - 十二生肖（高棉黄道，含高棉文名）；
 *  - 十纪元 Sak（10 年循环）；
 *  - 星期主星（7 曜）及其元素/特质/吉凶/象征；
 *  - 生肖深度解读（性格/优点/缺点/事业/爱情/幸运色/幸运数字）与每日运势（宜/忌）。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据：生肖与纪元 ========================

/** 高棉十二生肖（索引 0=鼠..11=猪） */
val KHMER_ZODIAC: List<String> = listOf(
    "鼠 (Chhout/ជូត)", "牛 (Chhlorp/ឆ្លូវ)", "虎 (Khal/ខាល)", "兔 (Thos/ថោះ)",
    "龙 (Rorng/រោង)", "蛇 (Masagn/ម្សាញ់)", "马 (Mom/មមី)", "羊 (Mamae/មមែ)",
    "猴 (Saka/សកា)", "鸡 (Roka/រកា)", "狗 (Chlae/ឆ្កែ)", "猪 (Kor/កុរ)"
)

/** 高棉十纪元 Sak（10 年循环） */
private val KHMER_SAK = listOf(
    "ឆស័ក (Chor Sak)", "ឯកស័ក (Ek Sak)", "ទោស័ក (To Sak)", "ត្រីស័ក (Tri Sak)",
    "ចត្វាស័ក (Chatur Sak)", "បញ្ចស័ក (Pach Sak)", "ឆស័ក (Chor Sak)",
    "សប្តស័ក (Sat Sak)", "អដ្ឋស័ក (Ath Sak)", "នវស័ក (Nav Sak)"
)

/** 纪元解读 */
private val SAK_INTERPRETATION = mapOf(
    "ឆស័ក (Chor Sak)" to "丰收之年，宜播种、投资、建立新事业",
    "ឯកស័ក (Ek Sak)" to "领导之年，宜展现才能、争取晋升",
    "ទោស័ក (To Sak)" to "合作之年，宜团队协作、广结善缘",
    "ត្រីស័ក (Tri Sak)" to "智慧之年，宜学习、研究、修行",
    "ចត្វាស័ក (Chatur Sak)" to "稳定之年，宜守成、巩固、享受生活",
    "បញ្ចស័ក (Pach Sak)" to "变革之年，宜创新、旅行、打破常规",
    "សប្តស័ក (Sat Sak)" to "反思之年，宜静养、规划、调整方向",
    "អដ្ឋស័ក (Ath Sak)" to "挑战之年，宜坚持、克服困难、积累实力",
    "នវស័ក (Nav Sak)" to "收获之年，宜庆祝、感恩、享受成果"
)

// ======================== 数据：星期主星 ========================

/** 星期主星（0=周日..6=周六） */
private val WEEKDAY_PLANETS = listOf(
    "太阳 (Suriya/សុរ្យ)", "月亮 (Chantra/ចន្រ្ទ)", "火星 (Angar/អង្គារ)", "水星 (Budh/ពុធ)",
    "木星 (Preah/ព្រហស្បតិ៍)", "金星 (Suk/សុក្រ)", "土星 (Sau/សៅរ៍)"
)

/** 主星属性（元素/特质/吉凶/象征） */
private val PLANET_ATTRIBUTES = mapOf(
    WEEKDAY_PLANETS[0] to PlanetAttr("火", "热情、领导力、自信、创造力", "吉", "国王、权威、父亲"),
    WEEKDAY_PLANETS[1] to PlanetAttr("水", "情感、直觉、包容、适应力", "吉", "母亲、情感、潜意识"),
    WEEKDAY_PLANETS[2] to PlanetAttr("火", "行动力、勇气、竞争、冲动", "凶", "战士、力量、冲突"),
    WEEKDAY_PLANETS[3] to PlanetAttr("地", "智慧、沟通、学习、商业", "吉", "商人、学者、交流"),
    WEEKDAY_PLANETS[4] to PlanetAttr("风", "智慧、扩张、幸运、宗教", "吉", "导师、财富、哲学"),
    WEEKDAY_PLANETS[5] to PlanetAttr("水", "美丽、爱情、艺术、享受", "吉", "爱人、艺术家、奢侈品"),
    WEEKDAY_PLANETS[6] to PlanetAttr("风", "责任、纪律、困难、延迟", "凶", "长者、边界、时间")
)

/** 每日运势（0=周日..6=周六） */
private val WEEKDAY_FORTUNE = listOf(
    DailyFortune("充满活力与自信的一天，适合领导项目和展现自我。", "启动新计划、社交、创造性工作", "独断专行、过度自信"),
    DailyFortune("情感丰富、直觉敏锐的一天，适合处理家庭和情感事务。", "与家人相处、艺术创作、休息", "做重大决策、情绪化"),
    DailyFortune("充满行动力与竞争精神的一天，适合攻坚克难。", "运动、竞争性工作、处理难题", "冲动行事、与人争吵"),
    DailyFortune("思维敏捷、沟通顺畅的一天，适合学习和商务往来。", "谈判、写作、学习新技能", "轻信他人、信息过载"),
    DailyFortune("智慧与幸运的一天，适合追求知识和开展重要事务。", "求学、投资、旅行、宗教活动", "懒惰、浪费机会"),
    DailyFortune("充满美感与享乐的一天，适合社交和艺术活动。", "约会、购物、艺术欣赏、聚会", "过度消费、放纵"),
    DailyFortune("需要责任与纪律的一天，适合规划和清理旧账。", "整理财务、规划未来、休息", "逃避责任、消极悲观")
)

// ======================== 数据：生肖深度解读 ========================

data class ZodiacInfo(
    val personality: String, val strength: String, val weakness: String,
    val career: String, val love: String, val luckyColor: String, val luckyNumber: String
)

private val ZODIAC_INTERPRETATION: Map<String, ZodiacInfo> = mapOf(
    KHMER_ZODIAC[0] to ZodiacInfo("聪明、机智、善于社交、适应力强", "精明、勤劳、富有魅力", "多疑、急功近利、不够坚持", "适合贸易、金融、公关", "浪漫但挑剔，需要安全感", "蓝色、金色", "2, 3"),
    KHMER_ZODIAC[1] to ZodiacInfo("稳重、勤劳、踏实、有耐心", "可靠、坚韧、有条理", "固执、保守、不善变通", "适合农业、建筑、管理", "忠诚但内敛，需要理解", "绿色、黄色", "1, 4"),
    KHMER_ZODIAC[2] to ZodiacInfo("勇敢、自信、独立、有冒险精神", "领导力强、热情、慷慨", "冲动、易怒、不够谨慎", "适合创业、军警、演艺", "热情但占有欲强，需要空间", "橙色、黑色", "3, 7"),
    KHMER_ZODIAC[3] to ZodiacInfo("温和、善良、敏感、追求和平", "细心、优雅、有艺术天赋", "优柔寡断、过于谨慎", "适合艺术、教育、外交", "温柔体贴，需要浪漫", "粉色、白色", "4, 9"),
    KHMER_ZODIAC[4] to ZodiacInfo("自信、强大、有魅力、理想主义", "领导力强、勇敢、慷慨", "自负、急躁、不切实际", "适合政治、管理、创意", "浪漫但主导欲强，需要崇拜", "金色、红色", "1, 6"),
    KHMER_ZODIAC[5] to ZodiacInfo("智慧、神秘、冷静、有洞察力", "深思熟虑、有魅力、果断", "多疑、冷漠、过于算计", "适合研究、金融、咨询", "深情但隐秘，需要信任", "紫色、黑色", "5, 8"),
    KHMER_ZODIAC[6] to ZodiacInfo("活泼、自由、热情、喜欢冒险", "开朗、善于交际、适应力强", "冲动、不够专注、善变", "适合销售、传媒、旅游", "热情但不稳定，需要自由", "红色、黄色", "2, 7"),
    KHMER_ZODIAC[7] to ZodiacInfo("温柔、善良、有艺术气质、和平主义", "体贴、有创造力、有耐心", "依赖、悲观、不够果断", "适合艺术、护理、教育", "浪漫且专一，需要安全感", "绿色、粉色", "3, 8"),
    KHMER_ZODIAC[8] to ZodiacInfo("聪明、灵活、好奇、善于模仿", "机智、多才多艺、幽默", "善变、浮躁、不够诚实", "适合科技、表演、销售", "有趣但不够专一，需要刺激", "白色、蓝色", "4, 9"),
    KHMER_ZODIAC[9] to ZodiacInfo("自信、勤奋、有责任心、追求完美", "诚实、有条理、有领导力", "挑剔、固执、过于直接", "适合管理、会计、公务员", "忠诚但苛刻，需要欣赏", "金色、棕色", "5, 7"),
    KHMER_ZODIAC[10] to ZodiacInfo("忠诚、正直、有正义感、警惕性强", "可靠、勇敢、有同情心", "多疑、固执、焦虑", "适合法律、安保、社工", "忠诚专一，需要信任", "红色、黑色", "3, 4"),
    KHMER_ZODIAC[11] to ZodiacInfo("善良、诚实、宽厚、有耐心", "慷慨、乐观、有同情心", "轻信、懒惰、容易放纵", "适合餐饮、教育、慈善", "温柔体贴，需要呵护", "黄色、灰色", "2, 8")
)

// ======================== 结果模型 ========================

data class PlanetAttr(val element: String, val trait: String, val luck: String, val symbol: String)
data class DailyFortune(val text: String, val favorable: String, val avoid: String)

/** 六维解读标签（总评/事业/财运/感情/健康/建议） */
private val DIM_LABELS = listOf("总评", "事业", "财运", "感情", "健康", "建议")

/** 将六个维度的文本拼成带「」标签的多行解读 */
private fun joinReading(lines: List<String>): String =
    DIM_LABELS.mapIndexed { i, label -> "「$label」${lines[i]}" }.joinToString("\n")

/** 生肖 → 六维深度解读（贴合高棉生肖性格与主题） */
private val ZODIAC_READING: Map<String, List<String>> = mapOf(
    KHMER_ZODIAC[0] to listOf(
        "聪明机智、善于社交，一生以灵巧应变立足，机运多藏于人际往来之中。",
        "适合贸易、金融、公关，反应快、路子活，宜主动出击抓住商机。",
        "正财偏财皆有门道，善钻营能生财，惟需防急功近利、见利忘义。",
        "浪漫但挑剔，需要安全感，宜多给真心少给考验，感情方能稳固。",
        "思虑多则耗神，注意神经与睡眠，宜劳逸结合、少熬夜。",
        "把精明用于正道，持之以恒，戒掉三分钟热度则前程可期。"
    ),
    KHMER_ZODIAC[1] to listOf(
        "稳重勤劳、踏实有耐心，一生以勤补拙、以稳取胜，是值得信赖的基石之才。",
        "适合农业、建筑、管理，功夫下得深，职位与资历皆稳步上升。",
        "财以勤俭积累，细水长流，忌投机，置产置业最合命格。",
        "忠诚但内敛，爱得含蓄深沉，需学会表达心意，莫让沉默伤人。",
        "体质耐劳，惟易积劳，注意腰颈与脾胃，宜规律作息。",
        "在踏实之外添一点变通，适时拥抱新事物，运势更上层楼。"
    ),
    KHMER_ZODIAC[2] to listOf(
        "勇敢自信、富有冒险精神，一生以胆识开道，是天然的开拓者与守护者。",
        "适合创业、军警、演艺，愈有挑战愈能激发斗志，宜大胆闯荡。",
        "财从胆识中来，敢拼敢抢常有斩获，惟冲动投资易失，须设底线。",
        "热情但占有欲强，爱得浓烈而霸道，需给对方留足自由空间。",
        "火气偏旺，注意血压、炎症与意外磕碰，运动宜量力而行。",
        "把勇猛用对地方，谋定而后动，戒除急躁则大业可成。"
    ),
    KHMER_ZODIAC[3] to listOf(
        "温和善良、追求和平，一生以柔待人、以和为贵，人缘是最大的福气。",
        "适合艺术、教育、外交，细腻审美与耐心是安身立命之本。",
        "财路平缓，宜守不宜攻，凭专业技能稳步生财，忌大进大出。",
        "温柔体贴、需要浪漫，惟优柔寡断易误良缘，宜勇敢把握。",
        "情绪敏感易伤神，注意脾胃与皮肤，宜以静养调和身心。",
        "修炼决断力，遇事当断则断，温柔而不软弱，人生自顺。"
    ),
    KHMER_ZODIAC[4] to listOf(
        "自信强大、理想主义，一生自带王者气场，目标远大、敢为人先。",
        "适合政治、管理、创意，领导才能出众，宜执掌一方大任。",
        "财从地位来，掌权则财聚，惟排场开支大，宜量入为出。",
        "浪漫但主导欲强，需要被崇拜，须学会欣赏伴侣、放下身段。",
        "心气高则易焦虑，注意心脏与神经，宜减压放松、宽以待己。",
        "理想须落地，多听逆耳忠言，谦和务实方能让基业长青。"
    ),
    KHMER_ZODIAC[5] to listOf(
        "智慧神秘、冷静有洞察力，一生深藏不露，思虑周详、看人极准。",
        "适合研究、金融、咨询，谋定而后动，深谋远虑是最大优势。",
        "理财天赋过人，长于布局，惟疑心重易错失良机，宜当机立断。",
        "深情但隐秘，爱在心头口难开，需学会信任与坦诚相待。",
        "忧思过甚易郁结，注意肝胆与神经，宜多亲近自然纾解。",
        "信任他人亦是放过自己，把聪明用在阳光处，福慧双收。"
    ),
    KHMER_ZODIAC[6] to listOf(
        "活泼自由、热情好动，一生闲不住、爱折腾，在变化中寻觅精彩。",
        "适合销售、传媒、旅游，走动越多财路越宽，宜多闯多试。",
        "财来快去也快，东边不亮西边亮，惟需防冲动消费、广种薄收。",
        "热情但不稳定，需要自由，须以责任感拴住飘忽的心。",
        "精力旺盛但易透支，注意劳逸结合，防小病拖成大病。",
        "选定方向深耕，把好动化为行动力，专注方能成大事。"
    ),
    KHMER_ZODIAC[7] to listOf(
        "温柔善良、有艺术气质，一生与美善为伴，是和平的使者也。",
        "适合艺术、护理、教育，以爱心与创造力立足，越温柔越有力。",
        "财从才艺与人缘来，细水长流，惟依赖心重，宜学独立理财。",
        "浪漫专一、需要安全感，是贴心的伴侣，惟悲观易伤感情。",
        "心思细腻易多愁，注意情绪与睡眠，宜培养乐观心境。",
        "少一点依赖、多一点自信，温柔亦要有主见，人生更从容。"
    ),
    KHMER_ZODIAC[8] to listOf(
        "聪明灵活、善于模仿，一生以机敏取胜，学啥像啥、多才多艺。",
        "适合科技、表演、销售，适应力强、转型快，是时代的弄潮儿。",
        "财路多样、来钱门道多，惟心浮气躁易半途而废，须专注。",
        "有趣但不够专一，需要新鲜感，宜以诚意滋养长情。",
        "神经过敏易疲劳，注意用脑过度与颈椎，宜动静结合。",
        "择一事而深耕，把聪明淬炼成专长，成就自然非凡。"
    ),
    KHMER_ZODIAC[9] to listOf(
        "自信勤奋、追求完美，一生以高标准自律，是可靠的中坚力量。",
        "适合管理、会计、公务员，条理分明、责任心强，宜挑大梁。",
        "理财稳健、精打细算，财库渐丰，惟过俭易失大方气度。",
        "忠诚但苛刻，对人对己皆严，需学会包容与欣赏不完美。",
        "操劳易紧张，注意肩颈与消化，宜学会放松、劳逸结合。",
        "追求完美是美德，但也要放过自己，张弛有度则福气更长。"
    ),
    KHMER_ZODIAC[10] to listOf(
        "忠诚正直、有正义感，一生以信义立身，是值得托付的守护者。",
        "适合法律、安保、社工，一身正气最受信任，宜守正道。",
        "财以正取，不义之财不取，正财稳健，惟防固守成见。",
        "忠诚专一、重情重诺，惟多疑焦虑易生隔阂，宜多信任。",
        "思虑重易失眠，注意肠胃与神经，宜运动排解焦虑。",
        "放下过度的警觉，学会接纳不同，宽厚待人则福泽自至。"
    ),
    KHMER_ZODIAC[11] to listOf(
        "善良诚实、宽厚有耐心，一生以厚道积福，是众人眼中的福气之人。",
        "适合餐饮、教育、慈善，以诚待人、以德服人，口碑即财富。",
        "财运随善缘而来，乐善好施反聚福，惟须防轻信破财。",
        "温柔体贴、需要呵护，是暖心伴侣，惟易因心软受委屈。",
        "易因安逸发福，注意代谢与血糖，宜管住嘴、迈开腿。",
        "善良要有锋芒，学会识人辨事，宽厚而不糊涂，福气更久。"
    )
)

data class KhmerProfile(
    val birthDate: LocalDate,
    val khmerYear: Int,
    val khmerMonth: Int,
    val khmerDay: Int,
    val isLeapMonth: Boolean,
    val zodiac: String,        // 生肖（含高棉文）
    val zodiacIndex: Int,
    val sak: String,           // 纪元
    val sakIndex: Int,
    val sakMeaning: String,
    val weekday: String,       // 周日..
    val planet: String,        // 主星
    val planetAttr: PlanetAttr,
    val zodiacInfo: ZodiacInfo,
    val dailyFortune: DailyFortune,
    val verdict: String        // 六维综合解读
)

// ======================== 核心计算 ========================

object KhmerAstrology {

    /** 公历 → 高棉农历（简化：以 4 月 14 日新年为界；高棉历纪元 = 公元 - 638/637） */
    fun gregorianToKhmerLunar(dt: LocalDate): KhmerLunarDate {
        val khmerYear = if (dt.monthValue < 4 || (dt.monthValue == 4 && dt.dayOfMonth < 14)) {
            dt.year - 638
        } else {
            dt.year - 637
        }
        // 高棉历月序简化映射
        var khmerMonth = (dt.monthValue + 1) % 12
        if (khmerMonth == 0) khmerMonth = 12
        return KhmerLunarDate(khmerYear, khmerMonth, dt.dayOfMonth, false)
    }

    /** 高棉历年份 → 生肖索引（基准：1362 年 = 龙年 index 4；用 floorMod 防负） */
    fun zodiacIndex(khmerYear: Int): Int = floorMod(khmerYear - 1362 + 4, 12)

    /** 高棉历年份 → 纪元 Sak 索引（10 年循环） */
    fun sakIndex(khmerYear: Int): Int = floorMod(khmerYear, 10)

    /** 完整档案 */
    fun profile(date: LocalDate): KhmerProfile {
        val lunar = gregorianToKhmerLunar(date)
        val zi = zodiacIndex(lunar.year)
        val si = sakIndex(lunar.year)
        val zodiac = KHMER_ZODIAC[zi]
        val sak = KHMER_SAK[si]
        val weekdayIdx = date.dayOfWeek.value % 7 // ISO Mon=1..Sun=7 → 0=Sun..6=Sat
        val planet = WEEKDAY_PLANETS[weekdayIdx]
        val reading = ZODIAC_READING[zodiac]
            ?: listOf("", "", "", "", "", "")
        return KhmerProfile(
            birthDate = date,
            khmerYear = lunar.year,
            khmerMonth = lunar.month,
            khmerDay = lunar.day,
            isLeapMonth = lunar.isLeapMonth,
            zodiac = zodiac,
            zodiacIndex = zi,
            sak = sak,
            sakIndex = si,
            sakMeaning = SAK_INTERPRETATION[sak] ?: "",
            weekday = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[weekdayIdx],
            planet = planet,
            planetAttr = PLANET_ATTRIBUTES[planet] ?: PlanetAttr("", "", "", ""),
            zodiacInfo = ZODIAC_INTERPRETATION[zodiac] ?: ZodiacInfo("", "", "", "", "", "", ""),
            dailyFortune = WEEKDAY_FORTUNE[weekdayIdx],
            verdict = joinReading(reading)
        )
    }

    /** 12 生肖简表（供展示） */
    fun zodiacTable(): List<Pair<String, String>> =
        KHMER_ZODIAC.map { z -> z to (ZODIAC_INTERPRETATION[z]?.personality ?: "") }
}

data class KhmerLunarDate(val year: Int, val month: Int, val day: Int, val isLeapMonth: Boolean)
