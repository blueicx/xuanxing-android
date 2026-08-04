package com.xuanji.app.domain.divination

/**
 * 姓名学（Nameology / 五格剖象法）：
 * 按姓氏与名字计算天格、人格、地格、外格、总格五格数理，
 * 尾数定五行、数值定吉凶，并给出各格解读。笔画数为确定性模拟（真实应查《康熙字典》）。
 */
object Nameology {

    data class Ge(val name: String, val number: Int, val element: String, val luck: String, val text: String)

    data class NameologyResult(
        val surname: String,
        val givenName: String,
        val geList: List<Ge>,
        val note: String,
        val verdict: String
    )

    // 各格解读（尾数取模）
    private val GE_TEXT = mapOf(
        "天格" to mapOf(1 to "一生平稳，长辈助力", 2 to "早年波折，需自强", 3 to "聪明好学，少年得志", 4 to "踏实稳重，中年发家", 5 to "福禄双全，晚年亨通"),
        "人格" to mapOf(1 to "领导力强，事业有成", 2 to "温和善良，人缘极佳", 3 to "才华横溢，名利双收", 4 to "稳健务实，大器晚成", 5 to "威严庄重，能担大任"),
        "地格" to mapOf(1 to "家庭和睦，子孙满堂", 2 to "持家勤俭，生活安稳", 3 to "夫妻恩爱，家运昌隆", 4 to "家庭责任重，需多包容", 5 to "晚年安逸，享福之命"),
        "外格" to mapOf(1 to "社交广泛，贵人相助", 2 to "人缘平平，但真诚", 3 to "口才出众，朋友众多", 4 to "谨慎交友，防小人", 5 to "威望高，得人敬重"),
        "总格" to mapOf(1 to "一生顺利，福寿双全", 2 to "先苦后甜，终成大事", 3 to "才智卓越，名声远播", 4 to "稳定发展，幸福圆满", 5 to "格局宏大，影响深远")
    )

    private fun strokesOf(name: String): Int = name.sumOf { (it.code % 8) + 1 }

    private fun strokesChar(ch: Char): Int = (ch.code % 8) + 1

    private fun elementOf(num: Int): String = when (num % 10) {
        1, 2 -> "木"
        3, 4 -> "火"
        5, 6 -> "土"
        7, 8 -> "金"
        else -> "水"
    }

    private fun luckOf(num: Int): String = when {
        num >= 30 -> "吉"
        num >= 20 -> "中吉"
        num >= 15 -> "小吉"
        else -> "凶"
    }

    fun analyze(surname: String, givenName: String): NameologyResult {
        val s = surname.trim()
        val g = givenName.trim()

        val tian = if (s.length == 1) strokesOf(s) + 1 else strokesOf(s)
        val ren = if (s.isNotEmpty() && g.isNotEmpty()) {
            strokesChar(s.last()) + strokesChar(g.first())
        } else {
            strokesOf(s + g)
        }
        val di = strokesOf(g)
        val zong = strokesOf(s + g)
        val wai = zong - ren + 1

        fun buildGe(name: String, num: Int): Ge {
            val text = GE_TEXT[name]?.get(num % 5 + 1) ?: "此数理中庸，随遇而安。"
            return Ge(name, num, elementOf(num), luckOf(num), text)
        }

        return NameologyResult(
            surname = s,
            givenName = g,
            geList = listOf(
                buildGe("天格", tian),
                buildGe("人格", ren),
                buildGe("地格", di),
                buildGe("外格", wai),
                buildGe("总格", zong)
            ),
            note = "五格搭配中，人格最为关键。若人格数理为吉，则其他格稍有不足亦可弥补。建议起名时注重人格与总格的协调，避免凶数组合。笔画数为简化模拟，实际取名建议参考《康熙字典》笔画与专业命名师。",
            verdict = buildVerdict(listOf(tian, ren, di, wai, zong))
        )
    }

    /** 综合解读：按五格吉凶数统计给出事业/财运/感情/健康/建议 */
    private fun buildVerdict(nums: List<Int>): String {
        fun lucky(n: Int): String = when { n >= 30 -> "吉"; n >= 20 -> "中吉"; n >= 15 -> "小吉"; else -> "凶" }
        val ren = nums[1]   // 人格
        val luckyCount = nums.count { lucky(it).contains("吉") }
        val sb = StringBuilder()
        sb.append("人格数理为$ren（${lucky(ren)}），为姓名之枢纽。")
        // 事业
        sb.append("事业：").append(
            when {
                lucky(ren).contains("吉") -> "主星得位，事业心强，易得认可与上升空间，宜担纲主事。"
                lucky(ren) == "凶" -> "事业多波折，需加倍努力、稳扎稳打，宜深耕专业积累。"
                else -> "事业平稳，按部就班可有成，宜抓住关键机会突破。"
            }
        )
        // 财运
        sb.append("财运：").append(
            when {
                luckyCount >= 4 -> "五格多为吉数，财运亨通，正偏财俱旺，利于积累。"
                luckyCount >= 2 -> "财运中等偏上，正财稳定，宜理财储蓄、忌投机。"
                else -> "财运有起伏，宜开源节流，谨慎投资，稳中求进。"
            }
        )
        // 感情
        sb.append("感情：").append(
            when (lucky(nums[2]).contains("吉")) { // 地格主家庭婚姻
                true -> "地格为吉，家庭和睦、夫妻恩爱，感情顺遂。"
                else -> "感情上需多包容沟通，用心经营方能长久。"
            }
        )
        // 健康
        sb.append("健康：").append(
            if (luckyCount >= 3) "整体身心安稳，气色佳，保持良好作息即可。"
            else "宜留意劳逸结合，情绪起伏时注意调适。"
        )
        // 建议
        sb.append("建议：").append(
            if (lucky(ren).contains("吉")) "善用名数之吉，坚定自信、顺势而为，前景可期。"
            else "数理虽有不足，但事在人为，以勤补拙、以德养名，终能成事。"
        )
        sb.append("（姓名学五格剖象为传统民俗，结果仅供文化娱乐参考）")
        return sb.toString()
    }
}
