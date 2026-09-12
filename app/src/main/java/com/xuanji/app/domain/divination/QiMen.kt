package com.xuanji.app.domain.divination

import com.xuanji.app.domain.BaziCalculator
import java.time.LocalDateTime

/**
 * 奇门遁甲（简化离线推算）：
 * - 阴阳遁：冬至→夏至用阳遁，夏至→冬至用阴遁
 * - 局数：按当前节气 + 上/中/下元（三元各 5 日）查表
 * - 地盘三奇六仪：阳遁从局数宫顺布，阴遁逆布
 * - 天盘九星、八门、八神：以值符值使飞布九宫（洛书盘）
 * 为便于离线确定性计算，采用标准三元局表与飞布规则，未做拆补置闰精校，仅供文化娱乐参考。
 */
object QiMen {

    /** 洛书宫序（顺布）：坎1 坤2 震3 巽4 中5 乾6 兑7 艮8 离9 */
    private val LUO = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

    private val GONG_TRIGRAM = mapOf(
        1 to "坎", 2 to "坤", 3 to "震", 4 to "巽",
        5 to "中(寄坤)", 6 to "乾", 7 to "兑", 8 to "艮", 9 to "离"
    )
    private val DOORS = listOf("休", "死", "伤", "杜", "中", "开", "惊", "生", "景") // 按洛书宫-1
    private val STARS = listOf("蓬", "芮", "冲", "辅", "禽", "心", "柱", "任", "英") // 洛书宫-1（禽寄坤）
    private val GODS = listOf("值符", "螣蛇", "太阴", "六合", "白虎", "玄武", "九地", "九天")
    private val TIANGAN = listOf("戊", "己", "庚", "辛", "壬", "癸", "丁", "丙", "乙") // 三奇六仪

    /** 九星意象（用于六维解读） */
    private val STAR_MEANING = mapOf(
        "蓬" to "主智谋多变，利于出奇制胜",
        "芮" to "主柔顺守成，宜学沟通而防劳倦",
        "冲" to "主威猛果敢，利进取开拓",
        "辅" to "主文昌贵人，利学业声誉",
        "禽" to "主居中调和，宜稳健守成",
        "心" to "主决策权谋，利领导医药",
        "柱" to "主才华声张，宜谨言慎行",
        "任" to "主厚德积福，利不动产与长期经营",
        "英" to "主文明声誉，利扬名而忌虚火"
    )

    /** 八门意象（用于六维解读） */
    private val DOOR_MEANING = mapOf(
        "开" to "主开张启运，利事业启动与公事开展",
        "休" to "主休养生息，利人际往来与休闲调养",
        "生" to "主生机财源，利求财经营",
        "伤" to "主竞争动荡，利破旧攻坚而忌意气用事",
        "杜" to "主闭塞防守，利保密固守而忌贸然出行",
        "景" to "主文书名声，利考试宣传与形象塑造",
        "死" to "主静止收敛，利了结旧事而忌启动新事",
        "惊" to "主惊恐口舌，宜谨慎避险而忌妄动招灾"
    )

    /** 节气表（按历法顺序，自冬至起共 24 个）：月, 日, 阴阳遁(阳遁/阴遁), 局数 a, 名称 */
    private data class Jq(val m: Int, val d: Int, val yy: String, val a: Int, val name: String)
    private val JIEQI = listOf(
        Jq(12, 22, "阳遁", 1, "冬至"), Jq(1, 5, "阳遁", 2, "小寒"), Jq(1, 20, "阳遁", 3, "大寒"),
        Jq(2, 4, "阳遁", 4, "立春"), Jq(2, 19, "阳遁", 5, "雨水"), Jq(3, 5, "阳遁", 6, "惊蛰"),
        Jq(3, 20, "阳遁", 7, "春分"), Jq(4, 4, "阳遁", 8, "清明"), Jq(4, 20, "阳遁", 9, "谷雨"),
        Jq(5, 5, "阳遁", 1, "立夏"), Jq(5, 21, "阳遁", 2, "小满"), Jq(6, 6, "阳遁", 3, "芒种"),
        Jq(6, 21, "阴遁", 9, "夏至"), Jq(7, 7, "阴遁", 8, "小暑"), Jq(7, 22, "阴遁", 7, "大暑"),
        Jq(8, 7, "阴遁", 6, "立秋"), Jq(8, 23, "阴遁", 5, "处暑"), Jq(9, 7, "阴遁", 4, "白露"),
        Jq(9, 23, "阴遁", 3, "秋分"), Jq(10, 8, "阴遁", 2, "寒露"), Jq(10, 23, "阴遁", 1, "霜降"),
        Jq(11, 7, "阴遁", 9, "立冬"), Jq(11, 22, "阴遁", 8, "小雪"), Jq(12, 7, "阴遁", 7, "大雪")
    )

    data class GongCell(
        val gong: Int,            // 1-9 洛书宫
        val trigram: String,      // 卦
        val door: String,         // 门
        val star: String,         // 星
        val god: String?,         // 神（仅值符系列宫有）
        val tianPan: String,      // 天盘干
        val diPan: String,        // 地盘干
        val isZhiFu: Boolean,
        val isZhiShi: Boolean
    )

    data class QiMenResult(
        val yinYang: String,      // 阳遁/阴遁
        val ju: Int,              // 局数 1-9
        val jieqi: String,
        val sanYuan: String,      // 上元/中元/下元
        val zhiFuStar: String,    // 值符九星
        val zhiShiDoor: String,   // 值使八门
        val cells: List<GongCell>, // 洛书宫 1..9
        val verdict: String       // 六维解读（总评/事业/财运/感情/健康/建议）
    )

    private fun mod9(v: Int): Int { val m = ((v - 1) % 9 + 9) % 9; return m + 1 }

    /** 日干支序数（0-59）：干=ord%10，支=ord%12 */
    private fun dayGanZhi(y: Int, m: Int, d: Int): Int {
        return BaziCalculator.dayPillarIndexForDate(java.time.LocalDate.of(y, m, d))
    }

    private fun hourBranch(hour: Int): Int { val h = (hour + 1) % 24; return h / 2 + 1 }

    fun cast(now: LocalDateTime = LocalDateTime.now(), seed: Long = 0L): QiMenResult {
        // seed 为 0 时用真实当前时间（按问事时辰起局）；非 0 时（重新起局）用随
        // 机抽取的「时辰」让每次起局结果明显不同，避免「点了没反应」的观感。
        val effNow = if (seed == 0L) now else run {
            val s = kotlin.math.abs(seed)
            now.plusDays((s ushr 7) % 240).withHour(((s and 0x1FL).toInt()) % 24)
        }
        val (yinYang, a, jieqi, sanYuan, ju) = determineJu(effNow)
        val yang = yinYang == "阳遁"

        // 地盘三奇六仪：阳遁 戊自局数宫顺布，阴遁逆布
        val diPan = IntArray(10) { -1 } // 索引洛书宫 1..9
        for (k in 1..9) {
            val idx = if (yang) (k - ju) else (ju - k)
            diPan[k] = ((idx % 9) + 9) % 9
        }

        // 时干支 → 旬首（三奇六仪 index 0-5）
        val dgz = dayGanZhi(effNow.year, effNow.monthValue, effNow.dayOfMonth)
        val shiGZ = ((dgz + hourBranch(effNow.hour) - 1) % 60 + 60) % 60
        val shiGan = shiGZ % 10   // 0甲1乙2丙3丁4戊5己6庚7辛8壬9癸
        val xun = shiGZ / 10       // 0甲子..5甲寅
        val shouGanIdx = xun       // 戊0己1庚2辛3壬4癸5
        // 时干 → 三奇六仪 index（甲→戊0）
        val shiGanSange = when (shiGan) {
            0, 4 -> 0; 5 -> 1; 6 -> 2; 7 -> 3; 8 -> 4; 9 -> 5; 3 -> 6; 2 -> 7; 1 -> 8
            else -> 0
        }

        // 值符落宫 = 时干三奇六仪 在地盘的宫
        val zhiFuGong = (1..9).first { diPan[it] == shiGanSange }
        // 值使门 index = 同值符宫的 门
        val zhiShiDoorIdx = (LUO.indexOf(zhiFuGong)) // 0-8 in LUO order; door uses 洛书宫-1

        // 天盘九星 / 八门 / 八神 飞布
        val tianPan = IntArray(10) { -1 }
        val starAt = IntArray(10) { -1 }
        val doorAt = IntArray(10) { -1 }
        val godAt = arrayOfNulls<String>(10)

        val step = if (yang) 1 else -1
        val gfPos = LUO.indexOf(zhiFuGong)
        // 九星飞布（蓬..英），从值符宫起顺/逆
        for (i in 0..8) {
            val gong = LUO[(gfPos + step * i + 9) % 9]
            starAt[gong] = i // 蓬在值符宫
            tianPan[gong] = ((shiGanSange + step * i) % 9 + 9) % 9
        }
        // 八门飞布（值使门在值符宫，顺/逆，不入中宫5→寄2）
        for (i in 0..8) {
            var gong = LUO[(gfPos + step * i + 9) % 9]
            if (gong == 5) gong = 2
            doorAt[gong] = (zhiShiDoorIdx + step * i + 9) % 9
        }
        // 八神飞布（值符神在值符宫，顺/逆，不入中宫5→寄2）
        for (i in 0..7) {
            var gong = LUO[(gfPos + step * i + 9) % 9]
            if (gong == 5) gong = 2
            godAt[gong] = GODS[i]
        }

        val cells = (1..9).map { k ->
            val doorIdx = if (k == 5) 1 else doorAt[k]   // 中宫门寄坤(死)
            val starIdx = if (k == 5) 1 else starAt[k]    // 中宫星寄坤(芮/禽)
            GongCell(
                gong = k,
                trigram = GONG_TRIGRAM.getValue(k),
                door = DOORS[doorIdx],
                star = STARS[starIdx],
                god = godAt[k],
                tianPan = TIANGAN[tianPan[k]],
                diPan = TIANGAN[diPan[k]],
                isZhiFu = (k == zhiFuGong),
                isZhiShi = (k == LUO[(gfPos) % 9] && doorAt[k] == zhiShiDoorIdx)
            )
        }

        return QiMenResult(
            yinYang = yinYang,
            ju = ju,
            jieqi = jieqi,
            sanYuan = sanYuan,
            zhiFuStar = STARS[starAt[zhiFuGong]],
            zhiShiDoor = DOORS.getOrElse(zhiShiDoorIdx) { DOORS[0] },
            cells = cells,
            verdict = buildVerdict(yinYang, ju, STARS[starAt[zhiFuGong]], DOORS.getOrElse(zhiShiDoorIdx) { DOORS[0] }, cells)
        )
    }

    /** 六维解读：按九宫八门、值符值使落位生成（离线确定性） */
    private fun buildVerdict(
        yinYang: String,
        ju: Int,
        zhiFuStar: String,
        zhiShiDoor: String,
        cells: List<GongCell>
    ): String {
        val fu = cells.first { it.isZhiFu }
        val kai = cells.first { it.door == "开" }
        val sheng = cells.first { it.door == "生" }
        val xiu = cells.first { it.door == "休" }
        val shang = cells.firstOrNull { it.door == "伤" }
        val si = cells.firstOrNull { it.door == "死" }
        val rui = cells.firstOrNull { it.star == "芮" }
        val jiMen = cells.filter { it.door in listOf("开", "休", "生") }
        val jiDesc = jiMen.joinToString("、") { "${it.trigram}宫${it.door}门" }
        val starM = STAR_MEANING[zhiFuStar] ?: "主事之星"
        val doorM = DOOR_MEANING[zhiShiDoor] ?: "行事之门"

        val zong = "总评：本局为${yinYang}·${ju}局，值符${zhiFuStar}星${starM}，值使${zhiShiDoor}门${doorM}；吉门落宫：$jiDesc，全局动静以此为枢，可谋可动，唯须因门取势。"
        val career = "事业：以值符与开门为用神，值符落${fu.trigram}宫掌事机，开门落${kai.trigram}宫，${DOOR_MEANING["开"]}；宜借${zhiFuStar}星之力推进正业，遇阻则按门向择吉而动。"
        val wealth = "财运：生门落${sheng.trigram}宫为财库之钥，${DOOR_MEANING["生"]}；${if (yinYang == "阳遁") "阳遁主升发，求财宜主动出击、拓展财源。" else "阴遁主收藏，求财宜稳健守成、防漏财。"}"
        val love = "感情：休门落${xiu.trigram}宫主人缘桃花，${DOOR_MEANING["休"]}；${if (jiMen.any { it.door == "休" }) "休门得位，人际亲和，利于相识、谈婚与修补感情。" else "休门不显，感情宜多花心思经营，勿以忙碌冷落对方。"}"
        val health = "健康：天芮星落${rui?.trigram ?: "中宫"}主疾厄之察，${shang?.let { "伤门落${it.trigram}宫防磕碰" } ?: "伤门未显"}，${si?.let { "死门落${it.trigram}宫宜收敛静养" } ?: "死门未显"}；宜劳逸结合、防微杜渐。"
        val advice = "建议：${if (jiMen.isNotEmpty()) "三吉门（开休生）皆有所落，凡事可趁吉而动，择吉门方位推进，问事宜快不宜拖。" else "吉门不显，宜守静蓄力、缓进待时，重大决策不妨改日再定。"}值符在${fu.trigram}宫，谋事可向此方位寻求支持与助力。"
        return listOf(zong, career, wealth, love, health, advice).joinToString("\n")
    }

    /** 确定阴阳遁、局数、节气、三元 */
    private fun determineJu(now: LocalDateTime): Quadruple {
        val today = java.time.LocalDate.of(now.year, now.monthValue, now.dayOfMonth)
        // 当前节气（按历法顺序自冬至起；处理年初跨年回绕）
        var chosen: Jq? = null
        var chosenDate: java.time.LocalDate? = null
        for (jq in JIEQI) {
            val jd = java.time.LocalDate.of(now.year, jq.m, jq.d)
            if (!jd.isAfter(today)) { chosen = jq; chosenDate = jd }
        }
        if (chosen == null) {
            // 早于当年小寒：当前节气为上一年冬至
            chosen = JIEQI[0]
            chosenDate = java.time.LocalDate.of(now.year - 1, JIEQI[0].m, JIEQI[0].d)
        }
        val jq = chosen!!
        val offset = (today.toEpochDay() - chosenDate!!.toEpochDay()).toInt() // 0-based 节气内第几天
        val sanYuan = when {
            offset < 5 -> "上元"
            offset < 10 -> "中元"
            else -> "下元"
        }
        val ju = when (sanYuan) {
            "上元" -> jq.a
            "中元" -> if (jq.yy == "阳遁") mod9(jq.a + 6) else mod9(jq.a - 6)
            else -> if (jq.yy == "阳遁") mod9(jq.a + 3) else mod9(jq.a - 3)
        }
        return Quadruple(jq.yy, jq.a, jq.name, sanYuan, ju)
    }

    private data class Quadruple(
        val yinYang: String, val baseA: Int, val jieqi: String,
        val sanYuan: String, val ju: Int
    )
}
