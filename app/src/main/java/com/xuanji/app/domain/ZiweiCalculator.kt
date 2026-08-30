package com.xuanji.app.domain

import com.xuanji.app.domain.calendar.LunisolarDate

/**
 * 紫微斗数排盘系统 —— 三派合一。
 *
 * 支持三大派别：
 *  1. 中州派 (Zhongzhou)：传统口传派，强调左辅右弼不化科，庚干天府化科
 *  2. 北派 (Beipai)：主宫位四化，次星辰，沿用左辅右弼化科，庚干天同化科、天相化忌
 *  3. 闽派/南派 (Minpai)：源自《紫微斗数全书》，庚干"阳武同阴"（太阳化禄、武曲化权、天同化科、太阴化忌）
 *
 * 安星算法基于传统紫微斗数排盘规则（离线确定性，无随机）：
 *  - 年干支（公历近似）
 *  - 五行局（简化按年干；非完整纳音五行局）
 *  - 命宫/身宫（生月、生时）
 *  - 十二宫天干（五虎遁）
 *  - 紫微星（命宫干支五行局 + 生日）
 *  - 天府星（紫微对宫）
 *  - 十四主星（紫微星系逆布 / 天府星系顺布）
 *  - 六吉星、六煞星、四化星
 *  - 各派别的四化表与星曜/宫位解读
 */
object ZiweiCalculator {

    // ===================== 基础数据 =====================
    private val TIAN_GAN = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val DI_ZHI = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val PALACE_NAMES = listOf(
        "命宫", "兄弟", "夫妻", "子女", "财帛", "疾厄",
        "迁移", "交友", "事业", "田宅", "福德", "父母"
    )
    private val ELEMENT_BUREAU_NUM = mapOf("水二局" to 2, "木三局" to 3, "金四局" to 4, "土五局" to 5, "火六局" to 6)

    private val MAIN_STARS = listOf(
        "紫微", "天机", "太阳", "武曲", "天同", "廉贞",   // 紫微星系（逆布）
        "天府", "太阴", "贪狼", "巨门", "天相", "天梁", "七杀", "破军" // 天府星系（顺布）
    )

    private val SCHOOLS = listOf("中州派", "北派", "闽派")

    // 各派别四化表
    private val HUA_TABLES = mapOf(
        "中州派" to mapOf(
            "甲" to mapOf("化禄" to "廉贞", "化权" to "破军", "化科" to "武曲", "化忌" to "太阳"),
            "乙" to mapOf("化禄" to "天机", "化权" to "天梁", "化科" to "紫微", "化忌" to "太阴"),
            "丙" to mapOf("化禄" to "天同", "化权" to "天机", "化科" to "文昌", "化忌" to "廉贞"),
            "丁" to mapOf("化禄" to "太阴", "化权" to "天同", "化科" to "天机", "化忌" to "巨门"),
            "戊" to mapOf("化禄" to "贪狼", "化权" to "太阴", "化科" to "右弼", "化忌" to "天机"),
            "己" to mapOf("化禄" to "武曲", "化权" to "贪狼", "化科" to "天梁", "化忌" to "文曲"),
            "庚" to mapOf("化禄" to "太阳", "化权" to "武曲", "化科" to "天府", "化忌" to "天同"),
            "辛" to mapOf("化禄" to "巨门", "化权" to "太阳", "化科" to "文曲", "化忌" to "文昌"),
            "壬" to mapOf("化禄" to "天梁", "化权" to "紫微", "化科" to "左辅", "化忌" to "武曲"),
            "癸" to mapOf("化禄" to "破军", "化权" to "巨门", "化科" to "太阴", "化忌" to "贪狼")
        ),
        "北派" to mapOf(
            "甲" to mapOf("化禄" to "廉贞", "化权" to "破军", "化科" to "武曲", "化忌" to "太阳"),
            "乙" to mapOf("化禄" to "天机", "化权" to "天梁", "化科" to "紫微", "化忌" to "太阴"),
            "丙" to mapOf("化禄" to "天同", "化权" to "天机", "化科" to "文昌", "化忌" to "廉贞"),
            "丁" to mapOf("化禄" to "太阴", "化权" to "天同", "化科" to "天机", "化忌" to "巨门"),
            "戊" to mapOf("化禄" to "贪狼", "化权" to "太阴", "化科" to "右弼", "化忌" to "天机"),
            "己" to mapOf("化禄" to "武曲", "化权" to "贪狼", "化科" to "天梁", "化忌" to "文曲"),
            "庚" to mapOf("化禄" to "太阳", "化权" to "武曲", "化科" to "天同", "化忌" to "天相"),
            "辛" to mapOf("化禄" to "巨门", "化权" to "太阳", "化科" to "文曲", "化忌" to "文昌"),
            "壬" to mapOf("化禄" to "天梁", "化权" to "紫微", "化科" to "左辅", "化忌" to "武曲"),
            "癸" to mapOf("化禄" to "破军", "化权" to "巨门", "化科" to "太阴", "化忌" to "贪狼")
        ),
        "闽派" to mapOf(
            "甲" to mapOf("化禄" to "廉贞", "化权" to "破军", "化科" to "武曲", "化忌" to "太阳"),
            "乙" to mapOf("化禄" to "天机", "化权" to "天梁", "化科" to "紫微", "化忌" to "太阴"),
            "丙" to mapOf("化禄" to "天同", "化权" to "天机", "化科" to "文昌", "化忌" to "廉贞"),
            "丁" to mapOf("化禄" to "太阴", "化权" to "天同", "化科" to "天机", "化忌" to "巨门"),
            "戊" to mapOf("化禄" to "贪狼", "化权" to "太阴", "化科" to "右弼", "化忌" to "天机"),
            "己" to mapOf("化禄" to "武曲", "化权" to "贪狼", "化科" to "天梁", "化忌" to "文曲"),
            "庚" to mapOf("化禄" to "太阳", "化权" to "武曲", "化科" to "天同", "化忌" to "太阴"),
            "辛" to mapOf("化禄" to "巨门", "化权" to "太阳", "化科" to "文曲", "化忌" to "文昌"),
            "壬" to mapOf("化禄" to "天梁", "化权" to "紫微", "化科" to "左辅", "化忌" to "武曲"),
            "癸" to mapOf("化禄" to "破军", "化权" to "巨门", "化科" to "太阴", "化忌" to "贪狼")
        )
    )

    // 十四主星详细解读
    private val STAR_INFO = mapOf(
        "紫微" to mapOf(
            "属性" to "北斗主星，己土，阴土", "性格" to "尊贵、威严、包容，有领导力和解厄制化之力",
            "优点" to "领导力强、有魄力、包容大度、能化解危机", "缺点" to "孤高自傲、独断专行、若独坐无辅佐易成孤君",
            "事业" to "适合管理、政治、高层决策", "爱情" to "择偶标准高，需能匹配其地位与格局"
        ),
        "天机" to mapOf(
            "属性" to "南斗主星，乙木，阴木", "性格" to "聪明、机智、善变、谋略过人",
            "优点" to "思维敏捷、足智多谋、善于策划", "缺点" to "心性不定、易生疑虑、缺乏耐心",
            "事业" to "适合军师、策划、智囊、科技行业", "爱情" to "重精神交流，需有共同语言"
        ),
        "太阳" to mapOf(
            "属性" to "中天主星，丙火，阳火", "性格" to "光明磊落、热情慷慨、光明正大",
            "优点" to "博爱、热情、有领导力、光明磊落", "缺点" to "过于直接、易招是非、光芒太盛",
            "事业" to "适合教育、公益、传媒、政治", "爱情" to "热情主动，需伴侣能承受其光芒"
        ),
        "武曲" to mapOf(
            "属性" to "北斗主星，辛金，阴金", "性格" to "刚毅果决、务实稳健、财帛之星",
            "优点" to "果断、务实、理财能力强、坚韧不拔", "缺点" to "固执、缺乏情趣、过于现实",
            "事业" to "适合金融、军警、工程、管理", "爱情" to "务实专一，不喜浪漫但责任强"
        ),
        "天同" to mapOf(
            "属性" to "南斗主星，壬水，阳水", "性格" to "温和、善良、乐观、享福之星",
            "优点" to "温和善良、乐观知足、人缘好", "缺点" to "懒散、缺乏进取心、依赖性强",
            "事业" to "适合休闲、艺术、教育、服务行业", "爱情" to "温柔体贴，追求和谐"
        ),
        "廉贞" to mapOf(
            "属性" to "北斗主星，丁火，阴火", "性格" to "聪慧、机敏、有魅力、司法令之星",
            "优点" to "聪明机智、反应快、有魅力、善于交际", "缺点" to "好胜心强、易惹是非、感情纠葛多",
            "事业" to "适合法律、军警、科技、复杂系统管理", "爱情" to "感情丰富但多变，易有桃花"
        ),
        "天府" to mapOf(
            "属性" to "南斗主星，戊土，阳土", "性格" to "宽厚、稳重、富足、财库之星",
            "优点" to "宽厚稳重、理财有道、善于积累", "缺点" to "保守、缺乏冒险精神、过于安逸",
            "事业" to "适合金融、地产、管理、仓储", "爱情" to "稳重专一，能提供物质保障"
        ),
        "太阴" to mapOf(
            "属性" to "南斗主星，癸水，阴水", "性格" to "温柔、内敛、细腻、阴柔之美",
            "优点" to "温柔细腻、善解人意、有艺术气质", "缺点" to "多愁善感、优柔寡断、缺乏魄力",
            "事业" to "适合艺术、文化、护理、女性行业", "爱情" to "深情内敛，追求灵魂共鸣"
        ),
        "贪狼" to mapOf(
            "属性" to "北斗主星，甲木，阳木", "性格" to "欲望强烈、多才多艺、桃花之星",
            "优点" to "多才多艺、善于交际、有魅力、应变力强", "缺点" to "欲望过强、贪多求快、易沉迷",
            "事业" to "适合娱乐、外交、贸易、公关", "爱情" to "桃花旺盛，追求激情与新鲜感"
        ),
        "巨门" to mapOf(
            "属性" to "南斗主星，癸水，阴水", "性格" to "口才犀利、深思熟虑、是非之星",
            "优点" to "口才好、思维缜密、善于辩论", "缺点" to "口舌是非多、多疑、过于计较",
            "事业" to "适合法律、外交、媒体、咨询", "爱情" to "言语犀利，需注意沟通方式"
        ),
        "天相" to mapOf(
            "属性" to "南斗主星，壬水，阳水", "性格" to "温和、稳重、辅佐之才、印星",
            "优点" to "温和稳重、善于辅佐、有协调能力", "缺点" to "缺乏主见、过于依赖、优柔寡断",
            "事业" to "适合秘书、助理、协调、管理", "爱情" to "温和体贴，是好的伴侣"
        ),
        "天梁" to mapOf(
            "属性" to "南斗主星，戊土，阳土", "性格" to "正直、慈悲、长寿之星、荫星",
            "优点" to "正直慈悲、有领导力、善于调解、长寿", "缺点" to "固执、好为人师、过于理想化",
            "事业" to "适合医疗、教育、慈善、宗教", "爱情" to "稳重专一，有责任心"
        ),
        "七杀" to mapOf(
            "属性" to "南斗主星，庚金，阳金", "性格" to "勇猛果决、开创力强、将星",
            "优点" to "果断勇敢、开创力强、有魄力、执行力强", "缺点" to "冲动、好斗、缺乏耐心、易得罪人",
            "事业" to "适合军警、创业、外科医生、竞技", "爱情" to "激烈专一，占有欲强"
        ),
        "破军" to mapOf(
            "属性" to "北斗主星，癸水，阴水", "性格" to "破坏与重建、变革之星、先锋",
            "优点" to "勇于变革、执行力强、敢于突破、有领导力", "缺点" to "冲动、不顾后果、人际关系紧张",
            "事业" to "适合创业、改革、军警、工程", "爱情" to "轰轰烈烈，但不稳定"
        )
    )

    private val PALACE_MEANING = mapOf(
        "命宫" to "十二宫的核心，代表先天性格、才能、智慧、人生格局",
        "兄弟" to "兄弟姐妹关系、手足缘分、朋友助力",
        "夫妻" to "婚姻状况、配偶性格、感情缘分",
        "子女" to "子女缘分、生育能力、子女性格",
        "财帛" to "财运状况、理财能力、财富来源",
        "疾厄" to "身体健康、疾病倾向、体质强弱",
        "迁移" to "外出运势、人际关系、社会活动",
        "交友" to "朋友关系、社交圈、下属缘分（古称奴仆宫）",
        "事业" to "事业发展、职业方向、社会地位（古称官禄宫）",
        "田宅" to "不动产、家庭环境、居住品质",
        "福德" to "精神享受、福气、内心世界",
        "父母" to "父母缘分、长辈关系、遗传体质"
    )

    private val SCHOOL_INFO = mapOf(
        "中州派" to "洛阳口传派，祖师白玉蝉与吴景鸾，每代只收一徒；王亭之（陆斌兆传授）为代表。强调十四主星、四化星与三方四正；左辅右弼不化科，庚干天府化科。",
        "北派" to "由'十八飞星策天紫微斗数'演变而来，主宫位四化、次星辰，侧重四化逻辑推理；沿用《全集》左辅右弼化科，庚干天同化科、天相化忌。",
        "闽派" to "源自《紫微斗数全书》，传说陈希夷所作；南派重星曜组合，排盘用全星系；庚干'阳武同阴'：太阳化禄、武曲化权、天同化科、太阴化忌。"
    )

    private val HUA_MEANING = mapOf(
        "化禄" to "吉化，主财运、福气、缘分、扩张性，任何一星化禄其吉利作用倍增",
        "化权" to "吉化，主权力、权威、执行力、强化性，增强星曜的力量与决断力",
        "化科" to "吉化，主名声、考试运、贵人相助、文昌，提升知名度与人际关系",
        "化忌" to "凶化，主阻碍、损失、压力、是非，星曜最凶的转化，需谨慎应对"
    )

    // ===================== 数据模型 =====================
    data class Palace(
        val name: String,
        val branch: String,
        val gan: String,
        val mainStars: List<String>,
        val luckyStars: List<String>,
        val badStars: List<String>,
        val hua: List<String>,
        val isLife: Boolean,
        val isBody: Boolean
    )

    data class ZiweiChart(
        val school: String,
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val gender: String,
        val yearGan: String,
        val yearZhi: String,
        val bureau: String,
        val lifePalace: String,
        val bodyPalace: String,
        val palaces: List<Palace>,
        val fourTrans: List<String>,
        val schoolInfo: String,
        val note: String
    )

    // ===================== 排盘引擎 =====================
    fun calculate(
        year: Int, month: Int, day: Int, hour: Int, minute: Int,
        gender: String = "male", school: String = "中州派"
    ): ZiweiChart {
        val s = if (school in SCHOOLS) school else "中州派"
        val hourIdx = ((hour + 1) / 2) % 12          // 子=0 … 亥=11
        val gan = ((year - 4) % 10 + 10) % 10
        val zhi = ((year - 4) % 12 + 12) % 12
        val yearGan = TIAN_GAN[gan]
        val yearZhi = DI_ZHI[zhi]

        // 五行局（按年干）
        val bureau = elementBureau(yearGan)

        // 命宫：正月从寅宫起，顺数生月，再顺数生时
        val monthStart = 2
        val monthPos = (monthStart + month - 1) % 12
        val lifePos = (monthPos + hourIdx) % 12

        // 身宫：顺数生月，再逆数生时
        val bodyPos = ((monthPos - hourIdx) % 12 + 12) % 12

        // 十二宫天干（五虎遁，从寅宫起）
        val palaceGan = palaceGanList(yearGan)

        // 紫微星位置（简化：命宫干支五行局 + 生日逆布）
        val ziweiPos = ziweiPosition(bureau, day)

        // 天府星位置（紫微与天府斜对）
        val tianfuPos = tianfuPosition(ziweiPos)

        // 十四主星
        val mainStars = placeMainStars(ziweiPos, tianfuPos)

        // 六吉星 / 六煞星
        val lucky = placeLuckyStars(month, hourIdx, yearGan)
        val bad = placeBadStars(yearGan, yearZhi, hourIdx)

        // 四化星（按派别）
        val huaTable = HUA_TABLES[s]!!.getValue(yearGan)

        // 构建十二宫
        val palaces = (0..11).map { i ->
            val branchPos = (lifePos + i) % 12
            Palace(
                name = PALACE_NAMES[i],
                branch = DI_ZHI[branchPos],
                gan = palaceGan[branchPos],
                mainStars = MAIN_STARS.filter { mainStars[it] == branchPos },
                luckyStars = lucky.filter { it.value == branchPos }.map { it.key },
                badStars = bad.filter { it.value == branchPos }.map { it.key },
                hua = huaTable.filterValues { it in MAIN_STARS && mainStars[it] == branchPos }
                    .map { (hua, star) -> "$star$hua" },
                isLife = i == 0,
                isBody = branchPos == bodyPos
            )
        }

        // 四化摘要（按派别）
        val fourTrans = huaTable.map { (hua, star) ->
            val pos = mainStars[star] ?: -1
            val palaceName = if (pos in 0..11) PALACE_NAMES[(pos - lifePos + 12) % 12] else "未知"
            "$star$hua（${HUA_MEANING[hua]}；落${palaceName}宫）"
        }

        return ZiweiChart(
            school = s,
            year = year, month = month, day = day, hour = hour,
            gender = gender,
            yearGan = yearGan, yearZhi = yearZhi,
            bureau = bureau,
            lifePalace = DI_ZHI[lifePos],
            bodyPalace = DI_ZHI[bodyPos],
            palaces = palaces,
            fourTrans = fourTrans,
            schoolInfo = SCHOOL_INFO[s] ?: "",
            note = "本报告以公历近似输入，未含农历换算、闰月与完整纳音五行局，不能替代传统紫微斗数排盘；各派别四化规则亦有差异，仅供文化研究与自我探索参考。"
        )
    }

    /** Canonical Ziwei entry point when a verified lunar date is available. */
    fun calculate(
        lunarDate: LunisolarDate,
        hour: Int,
        minute: Int,
        gender: String = "male",
        school: String = "中州派"
    ): ZiweiChart {
        val chart = calculate(lunarDate.year, lunarDate.month, lunarDate.day, hour, minute, gender, school)
        val leapLabel = if (lunarDate.isLeapMonth) "闰" else ""
        return chart.copy(
            note = "本命盘使用农历${lunarDate.year}年${leapLabel}${lunarDate.month}月${lunarDate.day}日；闰月已显式保留。紫微斗数各派在闰月安置与起限规则上仍有差异，请以所选流派排盘规则复核。"
        )
    }

    private fun elementBureau(gan: String): String = when (gan) {
        "甲", "乙" -> "金四局"
        "丙", "丁" -> "水二局"
        "戊", "己" -> "火六局"
        "庚", "辛" -> "木三局"
        else -> "土五局"
    }

    private fun palaceGanList(yearGan: String): List<String> {
        val startGan = mapOf(
            "甲" to "丙", "乙" to "戊", "丙" to "庚", "丁" to "壬", "戊" to "甲",
            "己" to "丙", "庚" to "戊", "辛" to "庚", "壬" to "壬", "癸" to "甲"
        )[yearGan] ?: "丙"
        val startIdx = TIAN_GAN.indexOf(startGan)
        return (0 until 12).map { i -> TIAN_GAN[(startIdx + i) % 10] }
    }

    private fun ziweiPosition(bureau: String, day: Int): Int {
        val num = ELEMENT_BUREAU_NUM[bureau] ?: 4
        val quotient = (day + num - 1) / num
        val base = 2  // 寅宫
        return ((base - (quotient - 1) * 2) % 12 + 12) % 12
    }

    private fun tianfuPosition(ziweiPos: Int): Int {
        return if (ziweiPos < 6) (6 - ziweiPos) % 12 else (18 - ziweiPos) % 12
    }

    private fun placeMainStars(ziweiPos: Int, tianfuPos: Int): Map<String, Int> {
        val stars = HashMap<String, Int>()
        // 紫微星系逆布
        val ziweiOffsets = mapOf(
            "紫微" to 0, "天机" to 2, "太阳" to 4, "武曲" to 5, "天同" to 7, "廉贞" to 10
        )
        ziweiOffsets.forEach { (star, off) -> stars[star] = ((ziweiPos - off) % 12 + 12) % 12 }
        // 天府星系顺布
        val tianfuOffsets = mapOf(
            "天府" to 0, "太阴" to 1, "贪狼" to 2, "巨门" to 3, "天相" to 4, "天梁" to 5, "七杀" to 6, "破军" to 8
        )
        tianfuOffsets.forEach { (star, off) -> stars[star] = (tianfuPos + off) % 12 }
        return stars
    }

    private fun placeLuckyStars(month: Int, hourIdx: Int, yearGan: String): Map<String, Int> {
        val stars = HashMap<String, Int>()
        stars["左辅"] = (4 + month - 1) % 12          // 辰=4，顺行
        stars["右弼"] = ((10 - (month - 1)) % 12 + 12) % 12 // 戌=10，逆行
        stars["文昌"] = (4 + hourIdx) % 12
        stars["文曲"] = ((10 - hourIdx) % 12 + 12) % 12
        val kuiMap = mapOf("甲" to "丑", "乙" to "子", "丙" to "亥", "丁" to "亥", "戊" to "丑",
            "己" to "子", "庚" to "丑", "辛" to "午", "壬" to "卯", "癸" to "辰")
        val yueMap = mapOf("甲" to "未", "乙" to "申", "丙" to "酉", "丁" to "酉", "戊" to "未",
            "己" to "申", "庚" to "未", "辛" to "寅", "壬" to "巳", "癸" to "戌")
        stars["天魁"] = DI_ZHI.indexOf(kuiMap[yearGan] ?: "丑")
        stars["天钺"] = DI_ZHI.indexOf(yueMap[yearGan] ?: "未")
        return stars
    }

    private fun placeBadStars(yearGan: String, yearZhi: String, hourIdx: Int): Map<String, Int> {
        val stars = HashMap<String, Int>()
        val luMap = mapOf("甲" to "寅", "乙" to "卯", "丙" to "巳", "丁" to "午", "戊" to "巳",
            "己" to "午", "庚" to "申", "辛" to "酉", "壬" to "亥", "癸" to "子")
        val luIdx = DI_ZHI.indexOf(luMap[yearGan] ?: "寅")
        stars["擎羊"] = (luIdx + 1) % 12
        stars["陀罗"] = ((luIdx - 1) % 12 + 12) % 12

        val huoMap = mapOf("寅" to "寅", "午" to "寅", "戌" to "寅", "申" to "子", "子" to "子", "辰" to "子",
            "巳" to "丑", "酉" to "丑", "丑" to "丑", "亥" to "卯", "卯" to "卯", "未" to "卯")
        val lingMap = mapOf("寅" to "戌", "午" to "戌", "戌" to "戌", "申" to "辰", "子" to "辰", "辰" to "辰",
            "巳" to "亥", "酉" to "亥", "丑" to "亥", "亥" to "巳", "卯" to "巳", "未" to "巳")
        stars["火星"] = DI_ZHI.indexOf(huoMap[yearZhi] ?: "寅")
        stars["铃星"] = DI_ZHI.indexOf(lingMap[yearZhi] ?: "戌")

        stars["地空"] = (hourIdx + 6) % 12
        stars["地劫"] = hourIdx % 12
        return stars
    }

    // ===================== 解读 =====================
    fun starInfo(star: String): Map<String, String>? = STAR_INFO[star]
    fun palaceMeaning(name: String): String? = PALACE_MEANING[name]
}
