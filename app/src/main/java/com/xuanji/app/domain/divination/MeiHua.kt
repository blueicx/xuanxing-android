package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.UserProfile
import java.time.LocalDateTime

/**
 * 梅花易数（邵雍先天易学）：
 * - 以年月日时数起卦（先天八卦数：乾1 兑2 离3 震4 巽5 坎6 艮7 坤8）
 * - 上卦 = (年+月+日) 取余 8，下卦 = (年+月+日+时) 取余 8，动爻 = (年+月+日+时) 取余 6
 * - 互卦：本卦二三四爻为下互、三四五爻为上互
 * - 变卦：动爻阴阳互变
 * - 体用：有动爻之卦为「用」，无动爻之卦为「体」；以体用五行生克断吉凶
 * 离线确定性算法，仅供文化娱乐参考。
 */
object MeiHua {

    /** 地支序（子1…亥12），用于年/时起卦 */
    private fun yearBranch(year: Int): Int = (((year - 4) % 12) + 12) % 12 + 1
    private fun hourBranch(hour: Int): Int {
        val h = (hour + 1) % 24
        return h / 2 + 1
    }

    data class GuaView(
        val lower: GuaCommons.Trigram,
        val upper: GuaCommons.Trigram,
        val name: String,
        val binary: String
    )

    data class MeiHuaResult(
        val original: GuaView,
        val changed: GuaView,
        val mutual: GuaView,
        val movingLine: Int,          // 1-6
        val ti: GuaCommons.Trigram,   // 体卦
        val yong: GuaCommons.Trigram, // 用卦
        val tiYongRelation: String,
        val reading: String,
        val question: String
    )

    /** 六爻二进制（自下而上）→ GuaView */
    private fun guaFromBinary(bin: String): GuaView {
        val lowerLines = bin.take(3).map { it == '1' }
        val upperLines = bin.takeLast(3).map { it == '1' }
        val lower = GuaCommons.TRIGRAMS.first { it.lines == lowerLines }
        val upper = GuaCommons.TRIGRAMS.first { it.lines == upperLines }
        return GuaView(lower, upper, GuaCommons.guaName(lower, upper), bin)
    }

    private fun linesOf(lower: GuaCommons.Trigram, upper: GuaCommons.Trigram): List<Boolean> =
        lower.lines + upper.lines

    /** 互卦：本卦六爻(自下而上)取 二三四爻→下互，三四五爻→上互 */
    private fun mutualGua(lines: List<Boolean>): GuaView {
        val lowerLines = listOf(lines[1], lines[2], lines[3])
        val upperLines = listOf(lines[2], lines[3], lines[4])
        val lower = GuaCommons.TRIGRAMS.first { it.lines == lowerLines }
        val upper = GuaCommons.TRIGRAMS.first { it.lines == upperLines }
        return GuaView(lower, upper, GuaCommons.guaName(lower, upper), lowerLines.joinToString("") { if (it) "1" else "0" } + upperLines.joinToString("") { if (it) "1" else "0" })
    }

    /** 主体算法：给定年月日时 + 问题。seed=0 用真实时辰；非 0（重新起卦）随机抽时辰使结果变化 */
    fun cast(year: Int, month: Int, day: Int, hour: Int, question: String, seed: Long = 0L): MeiHuaResult {
        val effHour = if (seed == 0L) hour else (hour + (kotlin.math.abs(seed) % 24).toInt()) % 24
        val yb = yearBranch(year)
        val hb = hourBranch(effHour)
        val sum = yb + month + day + hb
        val up = GuaCommons.trigramByValue(yb + month + day)
        val down = GuaCommons.trigramByValue(sum)
        val moving = if (sum % 6 == 0) 6 else sum % 6

        val origLines = linesOf(down, up)
        val orig = guaFromBinary(origLines.joinToString("") { if (it) "1" else "0" })

        // 变卦
        val chLines = origLines.toMutableList()
        chLines[moving - 1] = !chLines[moving - 1]
        val changed = guaFromBinary(chLines.joinToString("") { if (it) "1" else "0" })

        val mutual = mutualGua(origLines)

        // 体用：动爻落在 1..3 → 下卦为用、上卦为体；4..6 → 上卦为用、下卦为体
        val (ti, yong) = if (moving <= 3) (up to down) else (down to up)
        val rel = GuaCommons.wxRelation(ti.wx, yong.wx)

        val reading = buildReading(ti, yong, moving, rel, question)
        return MeiHuaResult(orig, changed, mutual, moving, ti, yong, rel, reading, question)
    }

    /** 用当前时间起卦 */
    fun castNow(question: String, now: LocalDateTime = LocalDateTime.now(), seed: Long = 0L): MeiHuaResult =
        cast(now.year, now.monthValue, now.dayOfMonth, now.hour, question, seed)

    /** 本命卦：以生日(年月、日时)起卦，可作为先天命卦参考 */
    fun benming(profile: UserProfile): GuaView {
        val yb = yearBranch(profile.birthYear)
        val hb = hourBranch(profile.birthHour)
        val up = GuaCommons.trigramByValue(yb + profile.birthMonth)
        val down = GuaCommons.trigramByValue(profile.birthDay + hb)
        return guaFromBinary(linesOf(down, up).joinToString("") { if (it) "1" else "0" })
    }

    private fun buildReading(
        ti: GuaCommons.Trigram,
        yong: GuaCommons.Trigram,
        moving: Int,
        rel: String,
        question: String
    ): String {
        val sb = StringBuilder()
        sb.append("体卦为${ti.cn}（${ti.wx}），用卦为${yong.cn}（${yong.wx}），动在${posName(moving)}爻。体用关系：$rel。")

        // 总评 + 六维解读（事业/财运/感情/健康/行动建议）
        val parts = when {
            rel.contains("大吉") -> listOf(
                "外力相生，天时地利，事多顺遂，宜把握机遇大胆进取。",
                "事业有贵人提携，团队协作顺遂，可乘势推进、争取升迁或新项目。",
                "财运走旺，正财偏财皆有进账，利于投资与理财规划。",
                "感情和顺，彼此滋养，单身的易遇良缘，恋爱的宜升温。",
                "身强体健，精力充沛，注意劳逸结合即可。",
                "吉运当前，宜主动出击、多结善缘，勿因犹豫错失良机。")
            rel.contains("吉") -> listOf(
                "体能控用，事可成，宜主动推进、把握节奏。",
                "事业进展顺利，你的能力足以驾驭当前局面，可稳步扩张。",
                "财运尚可，付出有回报，但不宜冒进，量入为出为佳。",
                "感情主动权在握，坦诚沟通可让关系更进一步。",
                "健康状况平稳，保持规律作息，小疾勿拖。",
                "以稳为主、以攻为辅，方向明确则放手去做。")
            rel.contains("比和") -> listOf(
                "体用同心，内外相安，平稳可成，贵在和合。",
                "事业平稳，同侪相助，宜合作共赢、深耕主业。",
                "财运平稳，宜守成储蓄，不宜投机。",
                "感情和谐默契，细水长流，是经营关系的好时机。",
                "身心调和，无大碍，保持好习惯即可。",
                "顺其自然、以和为贵，不争而善胜。")
            rel.contains("小凶") -> listOf(
                "我生则用泄我气，须多付出耐心与力气，事倍而功半。",
                "事业需多投入精力，短期内回报不显，宜沉住气、打基础。",
                "财运有出无入之象，宜节流，避免无谓开销与借贷。",
                "感情上你付出较多，需防单方面迁就，多沟通、求平衡。",
                "精力消耗较大，宜防疲劳累积，注意休养。",
                "此阶段宜养精蓄锐、降低预期，把功夫下在打基础上。")
            rel.contains("凶") -> listOf(
                "用克体则受制，宜守不宜攻，慎防阻碍与是非。",
                "事业阻力较大，宜守成避锋，暂缓扩张或大动作。",
                "财运受阻，忌投资投机，守财为上，防破财。",
                "感情易生摩擦，宜冷静包容，勿因小事争执。",
                "体质偏弱，宜注意防护，及时休整，防病来袭。",
                "以退为进、明哲保身，待时而动，不可强求。")
            else -> listOf(
                "体用平淡，静观其变，谋定而后动。",
                "事业无大起落，宜按部就班、扎实做事。",
                "财运平平，宜务实积累，不贪意外之财。",
                "感情平稳，宜多点用心经营，勿冷落对方。",
                "健康如常，规律生活即可。",
                "不急于求成，观察趋势后再决定方向。")
        }
        val overall = parts[0]
        val work = parts[1]
        val wealth = parts[2]
        val love = parts[3]
        val health = parts[4]
        val advice = parts[5]
        sb.append("总评：$overall")
        sb.append("事业：$work")
        sb.append("财运：$wealth")
        sb.append("感情：$love")
        sb.append("健康：$health")
        sb.append("建议：$advice")
        if (question.isNotBlank()) {
            sb.append("就「$question」而言，${if (rel.contains("吉") || rel.contains("比和")) "趋向有利" else "宜谨慎行事"}。")
        }
        sb.append("（梅花易数为先天易数推演，结果仅供文化娱乐参考）")
        return sb.toString()
    }

    private fun posName(p: Int): String = listOf("初", "二", "三", "四", "五", "上")[p - 1]
}
