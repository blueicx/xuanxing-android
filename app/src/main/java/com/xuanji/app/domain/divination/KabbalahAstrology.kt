package com.xuanji.app.domain.divination

import java.time.LocalDate

/**
 * 犹太占星（卡巴拉星象）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 公历 → 希伯来历（简化，以 9 月 15 日为新年分界）；
 *  - 希伯来月份 → 星座（Mazalot）、支派、感官；
 *  - 生命之树（Sefirot）与行星对应；
 *  - 姓名 Gematria（希伯来字母数值）与数根解读。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据 ========================

private val HEBREW_MONTHS = listOf(
    "尼散月 (Nisan)", "以珥月 (Iyar)", "西弯月 (Sivan)", "搭模斯月 (Tammuz)",
    "埃波月 (Av)", "以禄月 (Elul)", "提斯利月 (Tishrei)", "赫什万月 (Cheshvan)",
    "基斯流月 (Kislev)", "提别月 (Tevet)", "细罢特月 (Shevat)", "亚达月 (Adar)"
)

private val MONTH_ZODIAC = mapOf(
    "尼散月 (Nisan)" to ("白羊座 ♈" to "Taleh (טלה)"),
    "以珥月 (Iyar)" to ("金牛座 ♉" to "Shor (שור)"),
    "西弯月 (Sivan)" to ("双子座 ♊" to "Te'omim (תאומים)"),
    "搭模斯月 (Tammuz)" to ("巨蟹座 ♋" to "Sartan (סרטן)"),
    "埃波月 (Av)" to ("狮子座 ♌" to "Aryeh (אריה)"),
    "以禄月 (Elul)" to ("处女座 ♍" to "Betulah (בתולה)"),
    "提斯利月 (Tishrei)" to ("天秤座 ♎" to "Moznayim (מאזניים)"),
    "赫什万月 (Cheshvan)" to ("天蝎座 ♏" to "Akrav (עקרב)"),
    "基斯流月 (Kislev)" to ("射手座 ♐" to "Keshet (קשת)"),
    "提别月 (Tevet)" to ("摩羯座 ♑" to "Gedi (גדי)"),
    "细罢特月 (Shevat)" to ("水瓶座 ♒" to "D'li (דלי)"),
    "亚达月 (Adar)" to ("双鱼座 ♓" to "Dagim (דגים)")
)

private val TRIBE_MONTH = mapOf(
    "尼散月 (Nisan)" to "犹大 (Judah)", "以珥月 (Iyar)" to "以萨迦 (Issachar)", "西弯月 (Sivan)" to "西布伦 (Zebulun)",
    "搭模斯月 (Tammuz)" to "流便 (Reuben)", "埃波月 (Av)" to "西缅 (Simeon)", "以禄月 (Elul)" to "迦得 (Gad)",
    "提斯利月 (Tishrei)" to "以法莲 (Ephraim)", "赫什万月 (Cheshvan)" to "玛拿西 (Manasseh)", "基斯流月 (Kislev)" to "便雅悯 (Benjamin)",
    "提别月 (Tevet)" to "但 (Dan)", "细罢特月 (Shevat)" to "亚设 (Asher)", "亚达月 (Adar)" to "拿弗他利 (Naphtali)"
)

private val SENSE_MONTH = mapOf(
    "尼散月 (Nisan)" to "言语 (Speech)", "以珥月 (Iyar)" to "思想 (Thought)", "西弯月 (Sivan)" to "行动 (Motion)",
    "搭模斯月 (Tammuz)" to "视觉 (Sight)", "埃波月 (Av)" to "听觉 (Hearing)", "以禄月 (Elul)" to "行动 (Action)",
    "提斯利月 (Tishrei)" to "嗅觉 (Smell)", "赫什万月 (Cheshvan)" to "言语 (Speech)", "基斯流月 (Kislev)" to "睡眠 (Sleep)",
    "提别月 (Tevet)" to "愤怒 (Anger)", "细罢特月 (Shevat)" to "味觉 (Taste)", "亚达月 (Adar)" to "欢笑 (Laughter)"
)

/** 卡巴拉深度解读（星座 → 含义/课题） */
private val KABBALAH_INTERPRETATION = mapOf(
    "白羊座 ♈" to ("尼散月是救赎之月，逾越节的羔羊象征从埃及的解放。言语的力量在这个月最为突出，通过讲述出埃及的故事实现灵性转化。犹大支派代表王权与领导力，是这个月的属灵主导。" to "领导力的觉醒、言语的净化、从束缚中解放"),
    "金牛座 ♉" to ("以珥月是数算俄梅珥（Omer）的月份，通过 49 天的品格修炼走向五旬节。公牛象征力量与坚韧，以萨迦支派以 Torah 学习和智慧著称。" to "内在反思、品格修炼、耐心与坚持"),
    "双子座 ♊" to ("西弯月是领受 Torah 的月份，双胞胎象征两块石板——十诫的两面：人与神、人与人之间的关系。西布伦支派与商业和航海相关，代表物质与精神的平衡。" to "律法的领受、关系的平衡、行动中的智慧"),
    "巨蟹座 ♋" to ("螃蟹有坚硬的外壳保护柔软的内在，象征在动荡中守护灵性核心。流便支派代表长子身份与悔改的力量。" to "内在保护、视觉的纯净、悔改与修复"),
    "狮子座 ♌" to ("埃波月是圣殿被毁的月份，但狮子象征神的力量与将来的重建。西缅支派代表倾听与纪律。" to "哀悼与重建、倾听神的声音、力量的正确使用"),
    "处女座 ♍" to ("以禄月是悔改之月，为赎罪日做准备。处女象征灵性的纯净与准备。迦得支派代表军事力量与属灵争战。" to "灵性净化、悔改的准备、属灵争战"),
    "天秤座 ♎" to ("提斯利月是最神圣的月份，包含犹太新年、赎罪日和住棚节。天平象征神圣的审判与怜悯的平衡。以法莲支派代表多结果子。" to "神圣审判、平衡与正义、悔改与修复"),
    "天蝎座 ♏" to ("赫什万月是唯一没有节日的月份，蝎子象征隐藏的力量与转化。玛拿西支派代表遗忘与超越。" to "隐藏力量的转化、超越过去、寂静中的成长"),
    "射手座 ♐" to ("基斯流月包含光明节（Chanukah），弓箭象征马加比的军事胜利与光的奇迹。便雅悯支派代表战士与持久的忠诚。" to "光的胜利、信心的试炼、持久忠诚"),
    "摩羯座 ♑" to ("山羊象征跳跃与攀登，提别月是但支派审判与辩白的月份。愤怒在此月可以被转化为正义的力量。" to "情绪的转化、正义的追求、灵性的攀登"),
    "水瓶座 ♒" to ("细罢特月是树木新年（Tu b'Shevat），水桶象征浇灌与生长。亚设支派代表丰盛与祝福。" to "灵性浇灌、丰盛祝福、与自然的连接"),
    "双鱼座 ♓" to ("亚达月包含普珥节（Purim），鱼象征隐藏的奇迹。拿弗他利支派代表甜美与自由。" to "隐藏奇迹的发现、超越表象的喜乐、灵性自由")
)

/** 生命之树（Sefirot） */
private val SEFIROT = listOf(
    "Kether (王冠)" to ("无" to "神圣源头"),
    "Chokhmah (智慧)" to ("无" to "原初智慧"),
    "Binah (理解)" to ("土星 ♄" to "理解与限制"),
    "Chesed (仁慈)" to ("木星 ♃" to "仁慈与扩张"),
    "Geburah (严厉)" to ("火星 ♂" to "力量与审判"),
    "Tiphareth (美丽)" to ("太阳 ☉" to "美丽与平衡"),
    "Netzach (胜利)" to ("金星 ♀" to "胜利与永恒"),
    "Hod (荣耀)" to ("水星 ☿" to "荣耀与智慧"),
    "Yesod (根基)" to ("月亮 ☽" to "根基与潜意识"),
    "Malkuth (王国)" to ("地球" to "物质世界")
)

/** 星座 → 六维解读（总评/事业/财运/感情/健康/建议），贴合卡巴拉与生命之树主题 */
private val KABBALAH_VERDICT = mapOf(
    "白羊座 ♈" to listOf(
        "总评：尼散月为救赎之月，逾越节羔羊昭示从束缚走向解放，此命如初春之焰，先破后立。",
        "事业：犹大支派的王权气质赋予开拓勇气，宜担纲领队、开创新局，言语能力是开路利器。",
        "财运：财由「说」而来，谈判、演说、传播皆利，忌冲动投资，火候到了再出手。",
        "感情：言语是这个月的力量，甜言与狠话同样有力，学会用话语建造而非拆毁关系。",
        "健康：注意头部与血气之涌，怒火宜疏不宜压，用表达代替发作。",
        "建议：借逾越节的转化之力，切断旧束缚，以洁净的言语重新立约。"
    ),
    "金牛座 ♉" to listOf(
        "总评：以珥月是数算俄梅珥的品格修炼之月，此命以耐心与坚韧为底色，一步一修行。",
        "事业：以萨迦支派的智慧型学识利于深耕专业，财会、研究、实务皆宜，贵在持久。",
        "财运：正财稳健如公牛犁地，宜储蓄与长期积累，勿为小利折损根本。",
        "感情：关系宜细水长流，49 天的修炼之道同样适用于经营，慢即是深。",
        "健康：注意咽喉与颈椎，劳逸有度，身体是修行的第一座圣殿。",
        "建议：把品格修炼当作每日功课，耐心数算恩典，五旬节的丰收自会到来。"
    ),
    "双子座 ♊" to listOf(
        "总评：西弯月领受 Torah，双子双板象征天人之约，此命在交流与平衡中成就自我。",
        "事业：西布伦支派的海商基因利于商贸、传媒与信息业，知识就是流通的货币。",
        "财运：财从沟通与人脉而来，宜多元布局，但须防浅尝辄止、财随嘴散。",
        "感情：两块石板提醒关系的对等与契约精神，说出口的承诺就要兑现。",
        "健康：注意神经与呼吸，思虑过繁则气浮，定时放空有益清明。",
        "建议：以律法精神立身——对自己守约，对他人守信，双翼齐飞方能高翔。"
    ),
    "巨蟹座 ♋" to listOf(
        "总评：搭模斯月的蟹有坚壳护软心，此命外刚内柔，以守护与滋养为天命。",
        "事业：流便支派的长子责任赋予担当，餐饮、护理、房地产皆利，家即是根基。",
        "财运：财随口碑与家业而聚，宜置业安家，情绪化消费是需守的关口。",
        "感情：温柔而护短，铠甲只对家人卸下，也别忘了让对方看见你的柔软。",
        "健康：注意肠胃与情绪潮汐，心结宜开解，郁结最伤中焦。",
        "建议：悔改与修复是本月课题，守住灵性内核，壳再硬也要记得透气。"
    ),
    "狮子座 ♌" to listOf(
        "总评：埃波月圣殿被毁亦将重建，狮子之命在废墟中升起，哀悼之后必有复兴。",
        "事业：西缅支派的纪律加上狮子王气，宜掌帅印、治大任，越挫越勇。",
        "财运：财随声威与作品而来，宜以实力立信，忌挥霍撑场面。",
        "感情：倾听是狮子的修行，先听后说，王者之爱要能低下头来。",
        "健康：注意心脏与血压，情绪的圣殿亦需保养，大怒伤肝更伤心。",
        "建议：效法西缅的倾听与纪律，力量的正确使用，比力量本身更重要。"
    ),
    "处女座 ♍" to listOf(
        "总评：以禄月为悔改之月，处女之命以洁净与预备为功课，精益求精是道途。",
        "事业：迦得支派的争战之志利于攻坚克难，医疗、科研、精工行业皆宜。",
        "财运：细账算得清，财便守得住，正财稳健，忌过度挑剔误了时机。",
        "感情：完美主义的刀要收起来，以接纳之心相处，爱不是检查清单。",
        "健康：注意肠道与神经紧绷，悔改之月宜放下苛责，与己和解。",
        "建议：属灵争战先从内心打起，洁净心念，赎罪日的恩典必临到你。"
    ),
    "天秤座 ♎" to listOf(
        "总评：提斯利月为至圣之月，天平象征审判与怜悯的平衡，此命以公义与和谐立世。",
        "事业：以法莲支派的多结果子利于外交、法律与协调之位，平衡各方即是大才。",
        "财运：财从合作与公道而来，宜合伙经营，忌因求全而损及己利。",
        "感情：关系讲究秤平，付出与接受对等，天平失衡时先沟通再调整。",
        "健康：注意肾脏与腰背，久坐与反复权衡最耗气力，需动静结合。",
        "建议：在悔改与修复中校正生命的天平，让正义与怜悯并行不悖。"
    ),
    "天蝎座 ♏" to listOf(
        "总评：赫什万月无节庆，恰似蝎藏于石，此命在寂静中酝酿最深的力量。",
        "事业：玛拿西支派的遗忘与超越利于转型再生，心理、金融、侦查之业皆宜。",
        "财运：善掌暗财与资源整合，宜长线潜伏，忌疑心误判、错失良机。",
        "感情：深情而多疑，信任这门功课需反复练习，猜忌是关系最大的毒。",
        "健康：注意生殖泌尿与情绪淤积，秘密太多也伤身，学会倾诉。",
        "建议：让隐藏的力量在寂静中转化，遗忘该忘的，才能超越重来。"
    ),
    "射手座 ♐" to listOf(
        "总评：基斯流月的光明节燃灯八夜，此命如箭在弦，以信念与远见射向远方。",
        "事业：便雅悯支派的战士之志利于教育、出版、外务，眼光放远则路自宽。",
        "财运：财随机遇与见识而来，宜开拓新域，赌性宜收，信实是持久的财。",
        "感情：赤诚坦荡、慕爱自由，记得给承诺留出位置，热情才不流于浮光。",
        "健康：注意肝火与运动损伤，箭要瞄准也要回鞘，劳逸需平衡。",
        "建议：效法马加比以信心点灯，黑暗中也要相信光的奇迹就在坚持之后。"
    ),
    "摩羯座 ♑" to listOf(
        "总评：提别月的山羊善于攀登，此命以攀登与转化见长，愤怒亦可炼成正义之火。",
        "事业：但支派的审判与辩白之才利于管理、法务与基建，越走越高。",
        "财运：财由爬坡而来，宜长线规划与积攒，中年之后愈显丰厚。",
        "感情：情感内敛如山石，宜以行动表心意，硬汉也要学会说柔软的话。",
        "健康：注意骨骼关节与过度承压，山峰要登，膝盖更要养。",
        "建议：把愤怒转化为正义的追求，情绪升华为攀登的阶梯。"
    ),
    "水瓶座 ♒" to listOf(
        "总评：细罢特月的树木新年，水桶浇灌生长，此命以革新与博爱滋养众生。",
        "事业：亚设支派的丰盛之福利于科技、公益与创意产业，先浇灌，后收成。",
        "财运：财随创新与网络而丰，宜做长线生态，忌孤芳自赏断了财路。",
        "感情：重精神共鸣而轻形式，给彼此生长空间，关系如树愈久愈深。",
        "健康：注意循环与作息，白天耗电太多，夜里要记得充电。",
        "建议：在灵性上浇灌、在人群中结果，连接自然与同道，丰盛自来。"
    ),
    "双鱼座 ♓" to listOf(
        "总评：亚达月的普珥节藏着隐藏的奇迹，此命以灵感与慈悲为舟，渡人亦渡己。",
        "事业：拿弗他利支派的甜美与自由利于艺术、疗愈与慈善，直觉即天赋。",
        "财运：财如潮水有涨落，宜设堤防与底仓，勿被情绪浪潮卷走。",
        "感情：浪漫多情而易迷失，爱要流动也要有界，先爱己再爱人。",
        "健康：注意足部与睡眠，共情过度会透支，独处回血是必修课。",
        "建议：学会在表象之下看见隐藏的奇迹，以喜乐战胜愁苦，灵性自得自由。"
    )
)

private val GEMATRIA_MAP = mapOf(
    'א' to 1, 'ב' to 2, 'ג' to 3, 'ד' to 4, 'ה' to 5, 'ו' to 6, 'ז' to 7, 'ח' to 8, 'ט' to 9,
    'י' to 10, 'כ' to 20, 'ל' to 30, 'מ' to 40, 'נ' to 50, 'ס' to 60, 'ע' to 70, 'פ' to 80,
    'צ' to 90, 'ק' to 100, 'ר' to 200, 'ש' to 300, 'ת' to 400
)

private val GEMATRIA_ROOT_MEANING = mapOf(
    1 to "与 Kether（王冠）相应，代表神圣源头与至高意识",
    2 to "与 Chokhmah（智慧）相应，代表原初智慧与洞察",
    3 to "与 Binah（理解）相应，代表理解与辨识力",
    4 to "与 Chesed（仁慈）相应，代表仁慈与扩张",
    5 to "与 Geburah（严厉）相应，代表力量与审判",
    6 to "与 Tiphareth（美丽）相应，代表平衡与和谐",
    7 to "与 Netzach（胜利）相应，代表胜利与永恒",
    8 to "与 Hod（荣耀）相应，代表荣耀与智慧",
    9 to "与 Yesod（根基）相应，代表根基与潜意识",
    10 to "与 Malkuth（王国）相应，代表物质世界的实现"
)

/** 拉丁字母（含近似）→ 希伯来字母，用于转写后查 Gematria 数值 */
private val LATIN_TO_HEBREW = mapOf(
    'a' to 'א', 'b' to 'ב', 'c' to 'כ', 'd' to 'ד', 'e' to 'ה', 'f' to 'פ',
    'g' to 'ג', 'h' to 'ה', 'i' to 'י', 'j' to 'י', 'k' to 'כ', 'l' to 'ל',
    'm' to 'מ', 'n' to 'נ', 'o' to 'ע', 'p' to 'פ', 'q' to 'ק', 'r' to 'ר',
    's' to 'ס', 't' to 'ת', 'u' to 'ו', 'v' to 'ב', 'w' to 'ו', 'x' to 'צ',
    'y' to 'י', 'z' to 'ז'
)

/**
 * 汉字/任意字符 → 拉丁字母（确定性映射，供 Gematria 计算）：
 * 拉丁字母原样保留（小写化）；汉字按 Unicode 码点对 26 取模映射到 a-z。
 */
private fun toLatin(name: String): String = name.map { ch ->
    when {
        ch in 'a'..'z' || ch in 'A'..'Z' -> ch.lowercaseChar()
        else -> ('a' + ((ch.code % 26 + 26) % 26)).toChar()
    }
}.joinToString("")

// ======================== 结果模型 ========================

data class KabbalahResult(
    val date: LocalDate,
    val hebrewYear: Int,
    val monthName: String,
    val hebrewDay: Int,
    val zodiacSign: String,
    val hebrewZodiacName: String,
    val tribe: String,
    val sense: String,
    val kabbalahMeaning: String,
    val spiritualTheme: String,
    val gematria: GematriaResult?,
    val verdict: String
)

data class GematriaResult(
    val total: Int,
    val digitRoot: Int,
    val text: String,
    val transliteration: String,   // 转写后的拉丁字母序列（汉字自动转写）
    val hebrewSequence: String     // 对应的希伯来字母序列
)

// ======================== 核心计算 ========================

object KabbalahAstrology {

    /** 公历 → 希伯来历（简化） */
    fun gregorianToHebrew(dt: LocalDate): Triple<Int, Int, Int> {
        val year = dt.year; val month = dt.monthValue
        val hebrewYear = 5784 + (year - 2024)
        val hebrewMonth = if (month < 9 || (month == 9 && dt.dayOfMonth < 15)) {
            when (month) {
                1 -> 11; 2 -> 12; 3 -> 1; 4 -> 2; 5 -> 3; 6 -> 4; 7 -> 5; 8 -> 6
                else -> 7
            }
        } else {
            when (month) { 9 -> 7; 10 -> 8; 11 -> 9; else -> 10 }
        }
        return Triple(hebrewYear, hebrewMonth, dt.dayOfMonth)
    }

    /** 完整档案 */
    fun calculate(date: LocalDate, name: String? = null): KabbalahResult {
        val (hYear, hMonth, hDay) = gregorianToHebrew(date)
        val monthName = HEBREW_MONTHS[(hMonth - 1).coerceIn(0, 11)]
        val (zodiac, hebrewName) = MONTH_ZODIAC[monthName] ?: ("未知" to "")
        val (meaning, theme) = KABBALAH_INTERPRETATION[zodiac] ?: ("" to "")
        val gematria = name?.takeIf { it.isNotBlank() }?.let { gematria(it) }
        val dims = KABBALAH_VERDICT[zodiac] ?: emptyList()
        val verdict = if (dims.size >= 6) dims.joinToString("") + "（卡巴拉星象为犹太神秘主义传统，结果仅供文化娱乐参考）" else ""
        return KabbalahResult(
            date, hYear, monthName, hDay, zodiac, hebrewName,
            TRIBE_MONTH[monthName] ?: "未知", SENSE_MONTH[monthName] ?: "未知",
            meaning, theme, gematria, verdict
        )
    }

    /** Gematria（支持汉字自动转写为拉丁字母，再映射希伯来字母求值） */
    fun gematria(name: String): GematriaResult {
        // 1. 汉字/任意字符 → 拉丁字母（确定性转写）
        val latin = toLatin(name)
        // 2. 拉丁字母 → 希伯来字母序列
        val hebrewSeq = latin.map { LATIN_TO_HEBREW[it] ?: 'א' }.joinToString("")
        // 3. 希伯来字母求 Gematria 数值
        val total = hebrewSeq.sumOf { GEMATRIA_MAP[it] ?: 0 }
        var root = total
        while (root >= 10) root = root.toString().sumOf { it - '0' }
        val text = GEMATRIA_ROOT_MEANING[root] ?: "与生命之树的某一层面相应"
        return GematriaResult(total, root, text, latin, hebrewSeq)
    }

    /** 生命之树对应 */
    fun sefirot(): List<Triple<String, String, String>> = SEFIROT.map { (n, p) -> Triple(n, p.first, p.second) }

    /** 12 月份对应一览 */
    fun monthTable(): List<Triple<String, String, String>> =
        HEBREW_MONTHS.map { m ->
            val z = MONTH_ZODIAC[m]?.first ?: "未知"
            Triple(m, z, "${TRIBE_MONTH[m] ?: ""} / ${SENSE_MONTH[m] ?: ""}")
        }
}
