package com.xuanji.app.domain.divination

import java.security.MessageDigest

/**
 * 纳迪占星（Nadi Astrology / Nadi Jothidam）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 指纹分类（108 类，以 SHA-256 哈希模拟指纹特征提取）；
 *  - 27 宿 → Nadi 三组（Adi / Madhya / Antya）；
 *  - Nadi Dosha 配对（同组为缺陷，太阳同座可化解）；
 *  - 16 Kandam 章节概览与个性化提示；
 *  - Bhrigu Nandi Nadi（BNN）规则查询（太阳×12 星座 → 生活领域）。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据 ========================

/** 16 Kandam（章节） */
val NADI_KANDAMS: List<String> = listOf(
    "General (一般概览)", "Family & Education (家庭与教育)", "Brothers & Sisters (兄弟姐妹)",
    "Mother & Property (母亲与财产)", "Children (子女)", "Diseases & Debts (疾病与债务)",
    "Marriage (婚姻)", "Life Span (寿命)", "Father & Wealth (父亲与财富)",
    "Profession (职业)", "Second Marriage (再婚)", "Foreign Travel (国外旅行)",
    "Past Life (前世)", "Remedies (补救措施)", "Spiritual (灵性)", "Conclusion (结论)"
)

/** 27 宿名称 */
val NADI_NAKSHATRAS: List<String> = listOf(
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
    "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
    "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
    "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
    "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
)

/** 27 宿 → Nadi 组（0-8 Adi, 9-17 Madhya, 18-26 Antya） */
fun nadiOfNakshatra(index: Int): String =
    when (floorMod(index, 27) / 9) {
        0 -> "Adi"
        1 -> "Madhya"
        else -> "Antya"
    }

/** 12 星座名 */
val RASHI_NAMES = listOf(
    "Aries (Mesha)", "Taurus (Vrishabha)", "Gemini (Mithuna)", "Cancer (Karka)",
    "Leo (Simha)", "Virgo (Kanya)", "Libra (Tula)", "Scorpio (Vrishchika)",
    "Sagittarius (Dhanu)", "Capricorn (Makara)", "Aquarius (Kumbha)", "Pisces (Meena)"
)

/** Nadi Dosha 解读（同组=严重；异组=良好） */
data class NadiDoshaResult(
    val hasDosha: Boolean,
    val isCancelled: Boolean,
    val boyNadi: String?,
    val girlNadi: String?,
    val level: String,
    val text: String,
    val advice: String
)

private fun doshaInterpretation(boy: String, girl: String, cancelled: Boolean): Triple<String, String, String> {
    if (cancelled) {
        return Triple("已化解 (Cancelled)", "虽然双方同属 $boy Nadi，但通过特定的行星位置或补救仪式，Nadi Dosha 已被化解。", "Dosha 已化解，关系可正常发展。")
    }
    if (boy == girl) {
        return when (boy) {
            "Adi" -> Triple("严重 (Severe)", "双方同属 Adi Nadi（起始纳迪），能量过于相似，缺乏互补，婚后可能缺乏活力与成长动力，传统上认为会影响子嗣运势。", "如其他匹配因素评分很高，此 Dosha 可部分抵消；建议婚后进行补救仪式（Parihara）。")
            "Madhya" -> Triple("严重 (Severe)", "双方同属 Madhya Nadi（中间纳迪），能量层次完全相同，婚后易陷入平庸、缺乏互相激励的动力。", "建议婚前详细分析星盘，检查是否有其他强力吉相弥补；可通过咒语（Mantra）与祭祀（Homa）化解。")
            else -> Triple("严重 (Severe)", "双方同属 Antya Nadi（末尾纳迪），能量处于相同的衰退阶段，被认为最不利，可能导致关系能量枯竭，甚至影响健康与寿命。", "传统认为此配对极不吉利需非常谨慎；如必须结合，建议全面补救仪式并寻求资深占星师指导。")
        }
    }
    return when {
        (boy == "Adi" && girl == "Madhya") || (boy == "Madhya" && girl == "Adi") ->
            Triple("良好 (Good)", "Adi（起始）与 Madhya（中间）相结合，能量的互补与平衡：起始注入活力，中间提供稳定支持，是和谐的组合。", "此配对自然和谐，无需特别补救，建议双方保持各自独特性。")
        (boy == "Adi" && girl == "Antya") || (boy == "Antya" && girl == "Adi") ->
            Triple("良好 (Good)", "Adi（起始）与 Antya（末尾）相结合，代表生命周期的完整循环：起始带来新希望，末尾带来智慧与总结，富有深度。", "此配对有精神层面的深度，适合共同追求灵性成长，多进行深层次沟通。")
        else ->
            Triple("良好 (Good)", "Madhya（中间）与 Antya（末尾）相结合，代表从稳定到成熟的过渡：中间提供坚实基础，末尾带来圆满完成，是渐进成长的组合。", "此配对适合长期关系，建议耐心经营，享受共同进步。")
    }
}

/** BNN 领域（太阳×12 星座） */
private val BNN_DOMAINS = listOf(
    "Career (职业)", "Health (健康)", "Wealth (财富)", "Marriage (婚姻)",
    "Children (子女)", "Education (教育)", "Travel (旅行)", "Spirituality (灵性)",
    "Family (家庭)", "Social Status (社会地位)", "Enemies (敌人)", "Death (死亡)"
)

/** 六维解读标签（总评/事业/财运/感情/健康/建议） */
private val DIM_LABELS = listOf("总评", "事业", "财运", "感情", "健康", "建议")

/** 将六个维度的文本拼成带「」标签的多行解读 */
private fun joinReading(lines: List<String>): String =
    DIM_LABELS.mapIndexed { i, label -> "「$label」${lines[i]}" }.joinToString("\n")

/** 指纹原型（108 类按 (cls-1)%12 归入 12 组），每组六维解读 */
private val FINGERPRINT_READINGS: List<List<String>> = listOf(
    listOf(
        "指纹纹理刚直清晰，命格如开山者，天生的领导者与正义守护者，一生以担当立身。",
        "适合管理、军警、司法等需要威信与决断的领域，职位越高越能施展。",
        "财从名望与责任中来，掌权则财聚，惟不喜蝇营狗苟之财，正财最稳。",
        "爱得坦荡而具保护欲，是可靠的依靠，惟需放下强势、多听心声。",
        "精力旺盛但易积劳，注意脊背与关节，宜定期放松筋骨。",
        "以柔济刚、以德服人，把领导力用于成就他人，福报更深。"
    ),
    listOf(
        "指纹曲线流畅如画，命带艺术灵光，对美有独特感知，一生以审美立足。",
        "适合艺术、设计、演艺、文创，灵感处即是机遇，宜持续输出作品。",
        "才艺可生财，作品即财富，惟理财观念淡薄，宜请专人打理。",
        "浪漫多情、魅力外放，桃花常在，惟须守住内心、专情到底。",
        "感官敏锐易疲惫，注意咽喉与情绪，宜亲近自然滋养灵感。",
        "把才华当事业经营，亦要学着务实，艺术与生活两不误。"
    ),
    listOf(
        "指纹朴实匀称，命格务实稳健，擅长把琐事料理得井井有条，是可靠的基石型人物。",
        "适合技术、农业、工程与后勤，越是实处越见功力，宜深耕专业。",
        "财以勤劳换取，稳中有升，忌投机与贪快，积少自成多。",
        "爱得实在、不善浪漫，以行动表达真心，需多学甜言蜜语。",
        "体魄尚健，惟易忽视小恙，宜定期体检、防微杜渐。",
        "在踏实之上添一点远见，偶作突破，人生更上层楼。"
    ),
    listOf(
        "指纹细密幽深，命格带灵性追求，对生命意义充满好奇，是灵魂的探索者。",
        "适合哲学、心理、玄学、教育与公益，精神价值重于物质回报。",
        "视钱财为身外之物，财来随缘，惟需保障基本生活之资。",
        "重精神契合，寻灵魂伴侣，惟理想化易与现实落差，宜接地气。",
        "身心相连，情绪波动影响气脉，宜静坐、瑜伽以养神。",
        "在入世与出世间找到平衡，把灵性智慧化为日常行持。"
    ),
    listOf(
        "指纹纹路灵动，命格能言善道，具说服与感染之力，是天然的传播者。",
        "适合销售、传媒、演讲、外交，口才所至、财缘所聚。",
        "财从口中来，谈判、演说皆可生财，惟防言多必失。",
        "妙语连珠、风趣迷人，易获青睐，惟需以真诚为底色。",
        "用嗓过度易伤咽喉，宜护嗓润肺，注意声带保养。",
        "言出必践、以诚立信，巧言之外更须巧行。"
    ),
    listOf(
        "指纹纹理缜密，命格心思细密、长于钻研，于数据与细节中见真章。",
        "适合科研、会计、法务、情报分析，专注力是最大本钱。",
        "精于算计、善于理财，财以智聚，惟防钻牛角尖。",
        "爱得理性克制，不善甜言，惟行动可靠，需学习表达情感。",
        "久坐深思易劳神，注意颈椎与眼睛，宜劳逸结合。",
        "把细致用于大局，莫困于琐碎，放眼看远处更从容。"
    ),
    listOf(
        "指纹纹理独特多变，命格创造力丰沛，能于平凡中掘出不凡，是点石成金之才。",
        "适合创意、发明、编剧、产品设计，独特视角即核心竞争力。",
        "创意变现能力强，点子即金矿，惟需防天马行空不落地。",
        "心思灵动、情趣盎然，惟善变让人难测，宜给关系定心丸。",
        "神经活跃易失眠，宜规律作息，以静制动养精神。",
        "把灵感落成产品，把奇思做进现实，创意终成资产。"
    ),
    listOf(
        "指纹纹络圆和，命格重情重义、人缘广阔，一生以朋友与家人为最大财富。",
        "适合服务、社交、人力资源、社群运营，得人心者得天下。",
        "财随人脉而来，众人拾柴火焰高，惟须防义气破财。",
        "待人以诚、情深义重，是值得托付之人，惟易心软吃亏。",
        "情绪牵动脏腑，易因他人之事劳心，宜学会量力而行。",
        "帮人亦有度，先安顿好自己，方能长久照亮他人。"
    ),
    listOf(
        "指纹刚劲深峻，命格意志如铁、越挫越勇，逆境是淬炼你的熔炉，终有大成。",
        "适合攻坚克难的事业，愈是险阻愈见你本色，宜深耕专业。",
        "财从苦斗中来，先难后获，忌急功近利，守得云开见月明。",
        "爱得执着而深沉，惟刚强易折，需学会柔软与低头。",
        "耐受力强却易硬扛，注意慢性劳损与旧伤，宜及时就医。",
        "刚柔并济、张弛有度，懂得求助亦是强者之道。"
    ),
    listOf(
        "指纹纹理纤细微妙，命格天生敏锐，直觉与洞察力俱佳，如鹰眼观世。",
        "适合研判、医疗、投资、侦探等重直觉的领域，预感常是机遇。",
        "凭敏锐嗅觉捕捉时机，先人一步，惟需理性验证直觉。",
        "一眼看透人心，重深度连接，惟敏锐过头易生猜忌。",
        "神经过敏易透支，注意睡眠与精神压力，宜多静养。",
        "信任直觉而不盲从，以理性为舵，以灵觉为帆。"
    ),
    listOf(
        "指纹圆润通透，命格宽厚仁和、富有涵养，一生以德服人，福泽自来。",
        "适合教育、医疗、公益与咨询，以德望与耐心成就专业。",
        "正财稳健，乐善好施反积福德，财缘细水长流。",
        "温和包容、善解人意，是治愈系伴侣，惟易委屈求全。",
        "气性平和、底子良好，注意脾胃养护，饮食宜温。",
        "守住善良也要守住界限，温和之中须有原则。"
    ),
    listOf(
        "指纹纹理灵动多变，命格多才多艺、适应力超群，是百变的万金油之才。",
        "适合跨界与多领域发展，一专多能，机会多多益善。",
        "财路多元、东边不亮西边亮，惟需防广种薄收。",
        "风趣多变、人缘极佳，惟需专情与定力，别让桃花乱心。",
        "精力分散易疲乏，注意劳逸结合，养好元气。",
        "择一而精、以专带博，把多才炼成绝活，必成大器。"
    )
)

/** 指纹类别 → 六维初步指示（108 类按 (cls-1)%12 归组） */
private fun fingerprintIndication(cls: Int): String {
    val idx = ((cls - 1) % 12 + 12) % 12
    val parts = FINGERPRINT_READINGS[idx]
    return joinReading(parts)
}

// ======================== 核心计算 ========================

object NadiAstrology {

    /** 指纹字符串 → 108 类（SHA-256 哈希模拟） */
    fun classifyFingerprint(fingerprint: String): Int {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(fingerprint.trim().toByteArray(Charsets.UTF_8))
        // 取前 8 字节为大端整数
        var hash = 0L
        for (i in 0 until 8) {
            hash = (hash shl 8) or (digest[i].toLong() and 0xFF)
        }
        return (Math.floorMod(hash, 108L)).toInt() + 1
    }

    /** 个人纳迪档案 */
    fun profile(
        fingerprint: String,
        gender: String = "male",
        moonNakshatra: Int? = null,
        sunSign: Int? = null,
        jupiterSign: Int? = null,
        saturnSign: Int? = null
    ): NadiProfile {
        val cls = classifyFingerprint(fingerprint)
        val thumb = if (gender == "male") "右手拇指" else "左手拇指"
        return NadiProfile(
            gender = if (gender == "male") "男性" else "女性",
            thumb = thumb,
            fingerprintClass = cls,
            fingerprintIndication = fingerprintIndication(cls),
            moonNakshatra = if (moonNakshatra != null) NADI_NAKSHATRAS[floorMod(moonNakshatra, 27)] else null,
            nadi = moonNakshatra?.let { nadiOfNakshatra(it) },
            sunSign = sunSign?.let { RASHI_NAMES[floorMod(it, 12)] },
            jupiterSign = jupiterSign?.let { RASHI_NAMES[floorMod(it, 12)] },
            saturnSign = saturnSign?.let { RASHI_NAMES[floorMod(it, 12)] }
        )
    }

    /** Nadi Dosha 配对 */
    fun nadiDosha(
        boyNakshatra: Int?,
        girlNakshatra: Int?,
        boySunSign: Int?,
        girlSunSign: Int?
    ): NadiDoshaResult {
        if (boyNakshatra == null || girlNakshatra == null) {
            return NadiDoshaResult(false, false, null, null, "无法计算", "缺少月亮星宿信息，无法计算 Nadi Dosha。", "请提供双方的月亮星宿索引。")
        }
        val boy = nadiOfNakshatra(boyNakshatra)
        val girl = nadiOfNakshatra(girlNakshatra)
        var cancelled = false
        if (boy == girl && boySunSign != null && girlSunSign != null && boySunSign == girlSunSign) {
            cancelled = true
        }
        val (level, text, advice) = doshaInterpretation(boy, girl, cancelled)
        return NadiDoshaResult(
            hasDosha = boy == girl && !cancelled,
            isCancelled = cancelled,
            boyNadi = boy,
            girlNadi = girl,
            level = level,
            text = text,
            advice = advice
        )
    }

    /** BNN 规则查询（太阳在星座 → 生活领域） */
    fun bnnRule(planet: String, sign: Int): String {
        if (planet == "Sun") return BNN_DOMAINS[floorMod(sign, 12)]
        return "未知领域"
    }

    /** 16 Kandam 概览（供展示） */
    fun kandamOverview(): List<String> = NADI_KANDAMS

    /** Reads a licensed leaf corpus when configured; otherwise callers can pass the explicit simulation provider. */
    fun reading(query: NadiQuery, provider: NadiCorpusProvider = OfflineNadiSimulationProvider): NadiReading =
        provider.lookup(query)

    /** 宿名 → 索引（0-26），找不到返回 null */
    fun moonNakshatraIndexFromName(name: String): Int? =
        NADI_NAKSHATRAS.indexOf(name).let { if (it < 0) null else it }

    /** 指纹 → 确定性虚拟月亮宿（0-26）。无对方出生资料时，以指纹哈希推一个稳定星宿用于 Dosha 演示。 */
    fun nakshatraFromFingerprint(fingerprint: String): Int {
        val cls = classifyFingerprint(fingerprint)
        return floorMod(cls * 7 - 1, 27)
    }
}

data class NadiProfile(
    val gender: String,
    val thumb: String,
    val fingerprintClass: Int,
    val fingerprintIndication: String,
    val moonNakshatra: String?,
    val nadi: String?,
    val sunSign: String?,
    val jupiterSign: String?,
    val saturnSign: String?
)

private fun floorMod(x: Int, n: Int): Int = Math.floorMod(x, n)
