package com.xuanji.app.domain.divination

/**
 * 太乙神数（Tai Yi Shen Shu，简化八宫版）：
 * 以年+月+日确定所落之宫与主神煞，按宫位/神煞给出吉凶解读。
 * 确定性算法，离线可用。
 */
object TaiYi {

    private val PALACES = listOf("乾宫", "坎宫", "艮宫", "震宫", "巽宫", "离宫", "坤宫", "兑宫")
    private val SPIRITS = listOf("太乙", "摄提", "轩辕", "招摇", "天符", "青龙", "咸池", "太阴")

    private val PALACE_MEANING = mapOf(
        "乾宫" to "天行刚健，宜开拓事业，但需防刚愎自用。",
        "坎宫" to "水主险陷，当前宜守不宜攻，韬光养晦。",
        "艮宫" to "山主止，宜静待时机，不可轻举妄动。",
        "震宫" to "雷主动，有变动之机，宜果断行动。",
        "巽宫" to "风主顺，顺势而为，可得助力。",
        "离宫" to "火主明，智慧显现，利文化之事。",
        "坤宫" to "地主厚，以德服人，可获群众支持。",
        "兑宫" to "泽主悦，人际关系和顺，利合作。"
    )

    private val SPIRIT_MEANING = mapOf(
        "太乙" to "吉神，万事可成，贵人相助。",
        "摄提" to "凶神，宜谨慎，防口舌是非。",
        "轩辕" to "吉神，利事业，但需防小人。",
        "招摇" to "凶神，主动荡，不宜远行。",
        "天符" to "中性，按部就班可得利。",
        "青龙" to "大吉，财喜临门，诸事顺利。",
        "咸池" to "凶煞，主桃花劫，感情需谨慎。",
        "太阴" to "吉神，有暗助，适合幕后策划。"
    )

    private val LUCKY = setOf("太乙", "轩辕", "青龙", "太阴")

    data class TaiYiResult(
        val year: Int, val month: Int, val day: Int,
        val palace: String, val spirit: String,
        val palaceMeaning: String, val spiritMeaning: String,
        val isLucky: Boolean,
        val verdict: String
    )

    fun calculate(year: Int, month: Int, day: Int): TaiYiResult {
        val idx = ((year + month * 7 + day * 13) % 8 + 8) % 8
        val palace = PALACES[idx]
        val spirit = SPIRITS[idx]
        val lucky = spirit in LUCKY
        return TaiYiResult(
            year = year, month = month, day = day,
            palace = palace, spirit = spirit,
            palaceMeaning = PALACE_MEANING.getValue(palace),
            spiritMeaning = SPIRIT_MEANING.getValue(spirit),
            isLucky = lucky,
            verdict = buildVerdict(palace, spirit, lucky)
        )
    }

    /** 综合解读：总评 + 事业/财运/感情/健康/行动建议 */
    private fun buildVerdict(palace: String, spirit: String, lucky: Boolean): String {
        val sb = StringBuilder()
        sb.append("本日神煞落$palace，主神为「$spirit」，${if (lucky) "主吉" else "主凶/中性"}。")
        // 事业
        sb.append("事业：").append(
            when {
                spirit in setOf("太乙", "青龙", "轩辕") -> "有贵人助力、职位稳固，宜积极争取，成效显著。"
                spirit in setOf("摄提", "招摇") -> "易有是非与动荡，宜谨言慎行、守成避锋。"
                spirit == "咸池" -> "职场人缘复杂，宜专注本分，防感情用事影响判断。"
                else -> "按部就班即可，稳中有进，不宜冒进。"
            }
        )
        // 财运
        sb.append("财运：").append(
            when (spirit) {
                "青龙" -> "财喜临门，正偏财俱旺，利于进财与投资。"
                "太阴" -> "暗财暗助，宜低调理财，不宜张扬。"
                "摄提" -> "防口舌破财，忌与人合伙借贷。"
                else -> "财运平稳，宜守成储蓄，勿贪意外之财。"
            }
        )
        // 感情
        sb.append("感情：").append(
            when (spirit) {
                "咸池" -> "桃花旺盛但多虚象，需擦亮双眼，谨慎抉择，防情劫。"
                "太乙", "青龙" -> "感情和顺，有喜事之兆，宜坦诚相待。"
                "招摇" -> "感情易生波折，宜多沟通、少猜疑。"
                else -> "感情平稳，用心经营即可。"
            }
        )
        // 健康
        sb.append("健康：").append(
            if (spirit in setOf("摄提", "招摇", "咸池")) "精力有耗，宜留意情绪与作息，防小恙累积。"
            else "身心安泰，保持良好习惯即可。"
        )
        // 建议
        sb.append("建议：").append(
            if (lucky) "吉神当值，宜乘势而为、广结善缘，把握当日机遇。"
            else "凶煞当令，宜以退为进、韬光养晦，静待时机转吉。"
        )
        sb.append("（太乙神数为传统三式之学，结果仅供文化娱乐参考）")
        return sb.toString()
    }
}
