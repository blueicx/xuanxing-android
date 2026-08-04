package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.UserProfile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor

/**
 * 印度吠陀占星（Vedic / Hindu）：
 * - 月亮星座（Rashi）：月亮的恒星黄经（sidereal）所在黄道十二宫。
 * - 二十七宿（Nakshatra）：黄道 360° 均分 27 份，每份 13°20′（13.3333°）。
 * - Vimshottari 大运系统（核心算法）：
 *     1. 计算出生时刻月亮的恒星黄经 → 定位所在宿及其在宿内的已过比例 frac。
 *     2. 该宿主宰星（九星之一，固定循环）决定人生第一个大限（Mahadasha）的主星。
 *     3. 首大限剩余年数 = 主星总年限 × (1 − frac)。
 *     4. 之后按九星固定循环顺序依次切换后续大限，每限取该星完整年限。
 *     5. 大限内子限（Antardasha）：子限星年限 × 主限星年限 ÷ 120（标准 Vimshottari 公式，
 *        保证所有子限时长之和恰等于该大限总年限）。
 *
 * 月亮恒星黄经由「平黄经 + 中心差」近似，并用 Lahiri 岁差（ayanamsa）从黄道回归系转到恒星系。
 * 精度为占星娱乐级（宿边界误差 ≤ 1~2 宿），仅供文化娱乐参考。
 */
object Vedic {

    // ===================== 二十七宿 =====================
    private val NAKSHATRAS = listOf(
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
        "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
        "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
        "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
        "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
    )
    private val NAKSHATRA_CN = listOf(
        "娄宿", "胃宿", "昴宿", "毕宿", "觜宿", "参宿",
        "井宿", "鬼宿", "柳宿", "星宿", "张宿", "翼宿",
        "轸宿", "角宿", "亢宿", "氐宿", "房宿", "心宿",
        "尾宿", "箕宿", "斗宿", "牛宿", "女宿", "虚宿",
        "危宿", "室宿", "壁宿"
    )

    // ===================== 九星（Vimshottari 大限主宰） =====================
    // 名称 / 中文 / 大限年限（年）。九星年限累加 = 120 年。
    data class Graha(val key: String, val cn: String, val years: Int)

    private val GRAHAS = listOf(
        Graha("Ketu", "计都(Rahu's south node)", 7),
        Graha("Venus", "金星", 20),
        Graha("Sun", "太阳", 6),
        Graha("Moon", "月亮", 10),
        Graha("Mars", "火星", 7),
        Graha("Rahu", "罗喉(Rahu's north node)", 18),
        Graha("Jupiter", "木星", 16),
        Graha("Saturn", "土星", 19),
        Graha("Mercury", "水星", 17)
    )

    // 二十七宿的固定主宰星（依 Vimshottari 标准：自 Ashwini 起按上述九星循环）。
    // 即第 i 宿由 GRAHAS[i % 9] 主宰。
    private fun lordOfNakshatra(idx: Int): Graha = GRAHAS[((idx % 9) + 9) % 9]

    // ===================== 月亮恒星黄经 =====================

    /** 距 J2000.0（2000-01-01 12:00 TT）的天数（含小数） */
    private fun daysSinceJ2000(y: Int, m: Int, d: Int, hh: Int, mm: Int): Double {
        val dt = LocalDateTime.of(y, m, d, hh, mm)
        val j2000 = LocalDateTime.of(2000, 1, 1, 12, 0)
        return ChronoUnit.MINUTES.between(j2000, dt) / 1440.0
    }

    /** Lahiri 岁差（度）。2000 年 ≈ 23.85°，年增量 ≈ 0.01397°。 */
    private fun lahiriAyanamsa(y: Int, m: Int): Double {
        val frac = y + (m - 1) / 12.0
        return 23.85 + (frac - 2000.0) * 0.01397
    }

    /** 月亮恒星黄经（0-360°，sidereal），含中心差修正。 */
    private fun moonSiderealLongitude(y: Int, m: Int, d: Int, hh: Int, mm: Int): Double {
        val dd = daysSinceJ2000(y, m, d, hh, mm)
        val meanLon = 218.3165 + 13.176396 * dd            // 月亮平黄经（tropical, 度）
        val meanAnom = 134.963 + 13.064993 * dd            // 平近点角（度）
        val trueLonTrop = meanLon + 6.289 * Math.sin(Math.toRadians(meanAnom))  // 中心差修正
        val ayan = lahiriAyanamsa(y, m)
        var sidereal = trueLonTrop - ayan
        sidereal = ((sidereal % 360.0) + 360.0) % 360.0
        return sidereal
    }

    // ===================== 对外数据类 =====================

    data class Rashi(
        val name: String,       // 白羊
        val sanskrit: String,   // Meṣa
        val trait: String
    )

    data class VedicResult(
        val rashi: Rashi,
        val nakshatra: String,
        val nakshatraCn: String,
        val nakshatraIndex: Int
    )

    /** 单个子限（Antardasha） */
    data class Antardasha(
        val graha: Graha,
        val years: Double,
        val startAge: Double,
        val endAge: Double
    )

    /** 单个大限（Mahadasha） */
    data class Mahadasha(
        val index: Int,         // 第几限（1 起）
        val graha: Graha,
        val startAge: Double,
        val endAge: Double,
        val isFirst: Boolean,   // 首限（按比例截断）
        val antardashas: List<Antardasha>
    )

    data class VimshottariResult(
        val moonLongitude: Double,      // 月亮恒星黄经
        val nakshatraIndex: Int,        // 0-26
        val nakshatraName: String,
        val nakshatraCn: String,
        val nakshatraLord: Graha,
        val fracInNakshatra: Double,    // 0-1，已在宿内过的比例
        val firstRemainingYears: Double,
        val mahadashas: List<Mahadasha>,
        val activeIndex: Int            // 当前年龄所处大限下标（-1 表示未进入/已超出）
    )

    // ===================== 黄道十二宫（Rashi）基础数据 =====================

    private val RASHIS = listOf(
        Rashi("白羊", "Meṣa", "热情果决，勇于开拓，喜争第一。"),
        Rashi("金牛", "Vṛṣabha", "沉稳务实，重视感官与安全。"),
        Rashi("双子", "Mithuna", "机敏善变，好奇多才。"),
        Rashi("巨蟹", "Karka", "念旧顾家，情感深沉。"),
        Rashi("狮子", "Siṃha", "自信大方，具领导与表现欲。"),
        Rashi("处女", "Kanyā", "细致严谨，服务与分析法强。"),
        Rashi("天秤", "Tulā", "追求平衡和谐，重关系与美感。"),
        Rashi("天蝎", "Vṛścika", "深沉专注，洞察与意志力强。"),
        Rashi("射手", "Dhanu", "乐观自由，热爱探索与哲思。"),
        Rashi("摩羯", "Makara", "务实坚韧，有野心与责任感。"),
        Rashi("水瓶", "Kumbha", "独立创新，胸怀群体理想。"),
        Rashi("双鱼", "Mīna", "温柔慈悲，富有想象力。")
    )

    private const val NAKSHATRA_SPAN = 360.0 / 27.0   // 13.3333°

    // ===================== 对外 API =====================

    fun calculate(profile: UserProfile): VedicResult {
        val ml = moonSiderealLongitude(
            profile.birthYear, profile.birthMonth, profile.birthDay,
            profile.birthHour, profile.birthMinute
        )
        val rashiIdx = (floor(ml / 30.0).toInt() % 12 + 12) % 12
        val nakshatraIdx = (floor(ml / NAKSHATRA_SPAN).toInt() % 27 + 27) % 27
        return VedicResult(
            rashi = RASHIS[rashiIdx],
            nakshatra = NAKSHATRAS[nakshatraIdx],
            nakshatraCn = NAKSHATRA_CN[nakshatraIdx],
            nakshatraIndex = nakshatraIdx
        )
    }

    /** 计算 Vimshottari 大运系统（含当前年龄所处大限）。 */
    fun vimshottari(profile: UserProfile): VimshottariResult {
        val ml = moonSiderealLongitude(
            profile.birthYear, profile.birthMonth, profile.birthDay,
            profile.birthHour, profile.birthMinute
        )
        val nakshatraIdx = (floor(ml / NAKSHATRA_SPAN).toInt() % 27 + 27) % 27
        val within = ml - nakshatraIdx * NAKSHATRA_SPAN          // 宿内已过的度数
        val frac = (within / NAKSHATRA_SPAN).coerceIn(0.0, 1.0)  // 宿内已过比例 0-1

        val firstLord = lordOfNakshatra(nakshatraIdx)
        val firstRemaining = firstLord.years * (1.0 - frac)      // 首大限剩余年数

        // 当前年龄（用于定位活跃大限）
        val today = LocalDate.now()
        val ageYears = ChronoUnit.DAYS.between(
            LocalDate.of(profile.birthYear, profile.birthMonth, profile.birthDay),
            today
        ) / 365.25

        // 生成 N 个大限（覆盖约 120 年 + 首限）
        val COUNT = 12
        val list = mutableListOf<Mahadasha>()
        var cursor = 0.0                       // 累计年数游标（从出生 0 岁起算）
        var activeIdx = -1
        val startLordPos = nakshatraIdx % 9    // 首限在九星循环中的位置
        for (i in 0 until COUNT) {
            val lordPos = (startLordPos + i) % 9
            val lord = GRAHAS[lordPos]
            // 首限按「剩余未过比例」截断；后续各限取该星完整年限。
            val mainYears = if (i == 0) firstRemaining else lord.years.toDouble()
            val span = mainYears
            val startAge = cursor
            val endAge = cursor + span
            // 子限：标准 Vimshottari 公式 子限 = 子限星年限 × 主限有效年限 ÷ 120，
            // 保证所有子限时长之和恰等于该大限总年限（首限亦按截断后的 mainYears 缩放）。
            // 子限顺序同九星循环，自本限主星起。
            val antars = mutableListOf<Antardasha>()
            var aCursor = startAge
            for (j in 0 until 9) {
                val subLord = GRAHAS[(lordPos + j) % 9]
                val aYears = subLord.years * mainYears / 120.0
                antars.add(Antardasha(subLord, aYears, aCursor, aCursor + aYears))
                aCursor += aYears
            }
            if (activeIdx == -1 && ageYears >= startAge && ageYears < endAge) {
                activeIdx = i
            }
            list.add(Mahadasha(i + 1, lord, startAge, endAge, i == 0, antars))
            cursor = endAge
        }

        return VimshottariResult(
            moonLongitude = ml,
            nakshatraIndex = nakshatraIdx,
            nakshatraName = NAKSHATRAS[nakshatraIdx],
            nakshatraCn = NAKSHATRA_CN[nakshatraIdx],
            nakshatraLord = firstLord,
            fracInNakshatra = frac,
            firstRemainingYears = firstRemaining,
            mahadashas = list,
            activeIndex = activeIdx
        )
    }

    // ===================== 解读（增强版） =====================

    /** 九星（大限主宰）的人格/主题解读 */
    val GRAHA_MEANING: Map<String, String> = mapOf(
        "Ketu" to "计都主灵性觉醒与解脱：此段适合向内探索、放下执念，财务上宜保守，精神成长显著。",
        "Venus" to "金星主关系与富足：此段人缘桃花旺、艺术审美佳，宜经营感情与享受生活，注意节制享乐。",
        "Sun" to "太阳主自我与权威：此段适合争取地位、建立声望，事业上升期，宜主动担当。",
        "Moon" to "月亮主情绪与家庭：此段情感丰沛、直觉敏锐，宜安顿家庭、照顾身心，避免情绪化决策。",
        "Mars" to "火星主行动与竞争：此段精力旺盛、执行力强，宜攻坚克难，注意急躁冲动与口舌。",
        "Rahu" to "罗喉主扩张与变革：此段欲望与机遇并存，宜开拓新领域，警惕投机与执念。",
        "Jupiter" to "木星主智慧与贵人：此段福泽深厚、贵人相助，宜学习进修、行善积德，财禄顺遂。",
        "Saturn" to "土星主责任与磨砺：此段宜踏实守业、积累实力，考验耐心，熬过即有大成。",
        "Mercury" to "水星主沟通与商业：此段思维敏捷、商机涌现，宜谈判、写作、经营，注意多思多虑。"
    )

    /** 27 宿补充释义（每宿一句，用于深度解读） */
    val NAKSHATRA_MEANING: Map<String, String> = mapOf(
        "Ashwini" to "药神双马宿：主疗愈与速度，行动敏捷，宜开创与济世。",
        "Bharani" to "孕藏宿：主孕育与蜕变，情感深沉，能承载重负。",
        "Krittika" to "火炼宿：主净化与锋芒，刚烈果敢，宜磨砺成才。",
        "Rohini" to "红牛宿：主丰饶与美感，魅力动人，重感官享受。",
        "Mrigashira" to "鹿首宿：主寻觅与好奇，温柔善变，宜探索新知。",
        "Ardra" to "泪珠宿：主风暴与重生，情感强烈，历经磨砺愈强。",
        "Punarvasu" to "重光宿：主复原与希望，乐观豁达，屡仆屡起。",
        "Pushya" to "乳牛宿：主滋养与供养，福德深厚，宜敬长行善。",
        "Ashlesha" to "灵蛇宿：主穿透与智慧，洞察人心，善谋略机变。",
        "Magha" to "王座宿：主宗族与传承，尊贵自重，有祖荫之福。",
        "Purva Phalguni" to "前红宿：主享乐与创造，热情浪漫，重美与伴侣。",
        "Uttara Phalguni" to "后红宿：主稳固与结盟，重承诺契约，善成人之美。",
        "Hasta" to "掌中宿：主技艺与巧思，手巧心细，宜精进专业。",
        "Chitra" to "明珠宿：主美感与营造，才华横溢，能工巧匠之才。",
        "Swati" to "风旗宿：主独立与调和，柔韧善变，能独当一面。",
        "Vishakha" to "双树宿：主目标与成败，志向高远，成败系于专注。",
        "Anuradha" to "随喜宿：主忠诚与友谊，重信守诺，善结善缘。",
        "Jyeshtha" to "长老宿：主权威与守护，胆识过人，宜护持一方。",
        "Mula" to "根须宿：主深挖与转化，直指根本，宜破旧立新。",
        "Purva Ashadha" to "前胜宿：主宣示与征服，辩才无碍，宜扬名立志。",
        "Uttara Ashadha" to "后胜宿：主恒久与成就，坚韧不拔，终成大器。",
        "Shravana" to "听闻宿：主学习与传承，敏而好学，宜通识博闻。",
        "Dhanishta" to "鼓音宿：主富足与韵律，财禄声名俱佳，宜音乐艺术。",
        "Shatabhisha" to "百医宿：主疗愈与秘学，慧眼独具，宜研究玄理。",
        "Purva Bhadrapada" to "前祥宿：主燃烧与净化，热忱刚烈，宜修行觉悟。",
        "Uttara Bhadrapada" to "后祥宿：主深海与智慧，慈悲沉静，宜深潜学问。",
        "Revati" to "丰足宿：主圆满与守护，富足和顺，善利群生。"
    )

    /** 月亮星座 → 六维解读（按下标取：0总评 1事业 2财运 3感情 4健康 5建议） */
    private val RASHI_READING: Map<String, List<String>> = mapOf(
        "白羊" to listOf(
            "性情热情果决、勇于争先，一生以行动开创局面，是天生的先锋型人格。",
            "适合开创、竞争与高挑战行业，越是风口浪尖越能脱颖而出，宜把握先机。",
            "财从胆识来，敢闯敢拼常有进账，惟冲动投资易失，须设止损线。",
            "爱得热烈直接、毫不掩饰，惟急脾气易伤人心，宜慢一点、柔一点。",
            "能量充沛但易亢奋透支，注意头部、血压与炎症，运动宜留余力。",
            "先谋后动、以柔济刚，把冲劲导入长期目标，则前程似锦。"
        ),
        "金牛" to listOf(
            "性情沉稳务实、重感官与安全感，一生脚踏实地，福自稳中来。",
            "适合金融、地产、餐饮与技艺类行业，耐得住寂寞方能守得住繁华。",
            "财缘深厚、善于积累，正财稳健，是典型的小富即安的聚财之格。",
            "爱得忠诚持久、慢热深情，重陪伴与实在，忌固执己见。",
            "底子好但易发福，注意喉咙、甲状腺与代谢，饮食宜清淡有节。",
            "善用坚持之德，莫因安逸错过时机，偶作突破则境界更宽。"
        ),
        "双子" to listOf(
            "性情机敏善变、好奇多才，一生以灵活应变立足，信息即力量。",
            "适合传媒、贸易、教育、科技，多线并进而能游刃有余。",
            "财路多元、来去皆快，善抓信息差，惟须防三心二意损财。",
            "风趣健谈、桃花不缺，惟定力不足易生飘忽，宜专情守一。",
            "思虑过甚易神经衰弱，注意呼吸道与手肘，宜动静结合。",
            "把聪明聚焦于深耕，减少浅尝辄止，方能厚积薄发。"
        ),
        "巨蟹" to listOf(
            "念旧顾家、情感深沉，一生以家为根，直觉与共情力超群。",
            "适合照护、餐饮、房产与文化行业，以亲和力与耐心取胜。",
            "财宜守不宜博，家中理财有方，忌在外担保借贷。",
            "温柔细腻、极重情义，惟敏感多思、易患得患失，需安全感滋养。",
            "情绪与肠胃互为表里，注意胃部、乳腺与睡眠，宜少忧多安。",
            "以家为锚、以柔克刚，学会放下过度敏感，运势自稳。"
        ),
        "狮子" to listOf(
            "自信大方、颇具领导与表现欲，一生自带光环，是舞台的宠儿。",
            "适合管理、演艺、创意与公共行业，声望与地位是最大追求。",
            "财随名望而至，贵气十足，惟排场开支大，宜量入为出。",
            "浪漫热烈、慷慨护短，惟好面子、爱主导，须给伴侣舞台。",
            "心火偏旺，注意心脏、脊椎与眼目，宜早睡养肝。",
            "以谦逊驾驭光芒，多成人之美，王者之姿更得人心。"
        ),
        "处女" to listOf(
            "细致严谨、服务与分析法强，一生以匠心立身，于细微处见真章。",
            "适合医疗、会计、科研与技术，追求完美则专业度出众。",
            "精打细算、善于规划，财不露白，惟过俭易失大方之友。",
            "爱得认真而挑剔，重细节、讲实际，惟易因苛求伤情。",
            "思虑过细易焦虑，注意肠胃与神经系统，宜学会放松。",
            "放过自己与伴侣的「不完美」，大处着眼，小处从容。"
        ),
        "天秤" to listOf(
            "追求平衡和谐，重关系与美感，一生以人缘与品味行世。",
            "适合法务、外交、艺术、公关，协调斡旋是独门功夫。",
            "财从合作与审美来，人脉即财路，惟优柔寡断易失良机。",
            "浪漫优雅、善解人意，是理想的伴侣，惟怕做决定、怕冲突。",
            "肾与内分泌易受情绪影响，注意腰背与皮肤，宜规律作息。",
            "学会取舍与决断，守住自己的立场，平衡中不失自我。"
        ),
        "天蝎" to listOf(
            "深沉专注、洞察与意志力惊人，一生不鸣则已、一鸣惊人。",
            "适合研究、金融、侦探、玄秘领域，愈深挖愈见其能。",
            "聚财于暗处，擅长掌控资源，惟执念于财亦易为财所困。",
            "爱得炽烈而占有欲强，重深度连接，惟多疑易毁深情。",
            "生殖系统与泌尿须留意，情绪郁结易伤身，宜疏导宣泄。",
            "把洞察用于自我成长而非掌控他人，通透即是福。"
        ),
        "射手" to listOf(
            "乐观自由、热爱探索与哲思，一生心怀远方，福缘随眼界而开。",
            "适合教育、出版、旅行、国际贸易，走得越远天地越宽。",
            "财从见多识广中来，正财稳、偏财随缘，忌眼高手低。",
            "坦率潇洒、重精神共鸣，惟自由至上，需伴侣懂得放养。",
            "肝火与髋腿是薄弱处，易因劳顿而伤，宜劳逸结合。",
            "脚踏实地地追梦，让理想落地为计划，福泽自来。"
        ),
        "摩羯" to listOf(
            "务实坚韧、有野心与责任感，一生先苦后甜，是耐力型的长跑者。",
            "适合管理、工程、公职与实业的长期耕耘，资历即是资本。",
            "财来迟而稳，宜置业置产、长期投资，忌投机取巧。",
            "爱得克制而深沉，重承诺、不善表达，需学习柔情表达。",
            "注意骨骼、牙齿与膝盖，积劳易成疾，宜防过劳。",
            "张弛有度、学会借力，不必事事硬扛，则功业可期。"
        ),
        "水瓶" to listOf(
            "独立创新、胸怀群体理想，一生不走寻常路，思想即力量。",
            "适合科技、网络、公益与未来产业，越新颖越能发挥。",
            "财路别具一格，常凭独特眼光获利，惟需稳守现金流。",
            "重精神契合与自由空间，爱得理性疏离，需暖意调和。",
            "循环系统与小腿踝部易有状况，作息颠倒是大忌。",
            "让理想照进现实，多些人间烟火气，福气更接地气。"
        ),
        "双鱼" to listOf(
            "温柔慈悲、想象力丰富，一生以善感为天赋，是梦与美的化身。",
            "适合艺术、医疗、公益、灵性行业，以共情与灵感立身。",
            "财来如梦、宜疏不宜堵，善布施反得福，忌涉险投机。",
            "浪漫多情、极重感觉，惟界限模糊易受伤，需明辨真心。",
            "脚部与免疫系统易敏感，情绪影响极大，宜多亲近自然。",
            "以清醒守护善良，梦想与现实并举，则福慧双修。"
        )
    )

    /** 兜底六维解读（理论上不会命中） */
    private val DEFAULT_RASHI_READING = listOf(
        "性格平和，一生运势稳中有升，宜顺势而为、知足常乐。",
        "宜立足本职、稳中求进，凭专业与勤勉打开局面。",
        "财路平缓稳健，宜储蓄理财、量入为出，忌冒进。",
        "感情温和细水长流，宜以真诚与陪伴滋养情缘。",
        "身心大体安稳，宜规律作息、常运动、少忧思。",
        "把握当下、广结善缘，修身养性则福气自来。"
    )

    /**
     * 生成六维综合解读：总评（月亮星座 + 本命宿）、事业、财运、感情、健康、建议，
     * 并附当前大限行运主题。确定性文本拼接，无随机。
     */
    fun interpretation(profile: UserProfile): String {
        val res = calculate(profile)
        val vim = vimshottari(profile)
        val lines = RASHI_READING[res.rashi.name] ?: DEFAULT_RASHI_READING
        val sb = StringBuilder()
        sb.append("「总评」月亮落${res.rashi.name}座（${res.rashi.sanskrit}），${lines[0]} 本命宿${res.nakshatraCn}（${res.nakshatra}）：${NAKSHATRA_MEANING[res.nakshatra] ?: ""}\n")
        sb.append("「事业」${lines[1]}\n")
        sb.append("「财运」${lines[2]}\n")
        sb.append("「感情」${lines[3]}\n")
        sb.append("「健康」${lines[4]}\n")
        sb.append("「建议」${lines[5]}")
        if (vim.activeIndex >= 0) {
            val active = vim.mahadashas[vim.activeIndex]
            sb.append("\n\n「行运」当前处于${active.graha.cn}大限（${"%.0f".format(active.startAge)}–${"%.0f".format(active.endAge)}岁）：${GRAHA_MEANING[active.graha.key] ?: ""}")
        }
        return sb.toString()
    }
}
