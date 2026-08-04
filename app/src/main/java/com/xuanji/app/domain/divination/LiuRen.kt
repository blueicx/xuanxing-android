package com.xuanji.app.domain.divination

/**
 * 大六壬（Da Liu Ren，简化四课三传版）：
 * 以年月日时组合生成四课（上神/下神）与三传（初/中/末传），
 * 依上神与下神是否相克判顺逆。确定性算法，离线可用。
 */
object LiuRen {

    private val DI_ZHI = listOf(
        "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"
    )

    data class Ke(val upper: String, val lower: String)

    data class LiuRenResult(
        val year: Int, val month: Int, val day: Int, val hour: Int,
        val kes: List<Ke>,
        val first: String, val second: String, val third: String,
        val hasKe: Boolean,
        val verdict: String
    )

    fun calculate(year: Int, month: Int, day: Int, hour: Int): LiuRenResult {
        val seed = (year + month * 2 + day * 3 + hour * 5) % 12
        val idx = { n: Int -> ((seed + n) % 12 + 12) % 12 }

        val kes = listOf(
            Ke(DI_ZHI[idx(0)], DI_ZHI[idx(2)]),
            Ke(DI_ZHI[idx(4)], DI_ZHI[idx(6)]),
            Ke(DI_ZHI[idx(8)], DI_ZHI[idx(10)]),
            Ke(DI_ZHI[idx(1)], DI_ZHI[idx(3)])
        )
        val first = DI_ZHI[idx(5)]
        val second = DI_ZHI[idx(9)]
        val third = DI_ZHI[idx(11)]

        val hasKe = kes[0].upper != kes[0].lower
        val verdict = buildVerdict(hasKe, first, second, third)

        return LiuRenResult(
            year = year, month = month, day = day, hour = hour,
            kes = kes, first = first, second = second, third = third,
            hasKe = hasKe, verdict = verdict
        )
    }

    /** 综合解读：总评 + 事业/财运/感情/健康/行动建议 */
    private fun buildVerdict(hasKe: Boolean, first: String, second: String, third: String): String {
        val sb = StringBuilder()
        if (hasKe) {
            sb.append("上神与下神相克，主变动，事情需经历先难后易的过程，最终可成。")
            sb.append("三传初传$first、中传$second、末传$third，事体由急而缓、由变而定。")
            sb.append("事业：虽有波折，但贵在坚持，先难后易，中期有转机，宜沉住气推进。")
            sb.append("财运：财来有反复，需耐心经营，忌急躁贪快，稳扎稳打方有所得。")
            sb.append("感情：易有磨合与小争执，真诚沟通可化干戈为玉帛，感情反因共同经历而加深。")
            sb.append("健康：留意因操劳与情绪引起的失衡，宜劳逸结合、调畅情志。")
            sb.append("建议：事缓则圆，以静制动，先谋定而后动，切莫因一时受挫而自乱阵脚。")
        } else {
            sb.append("上下比和，主平稳，事情顺遂可期。")
            sb.append("三传初传$first、中传$second、末传$third，事体一以贯之、波澜不惊。")
            sb.append("事业：局面稳定，宜按部就班、守成扩展，是打牢根基的好时机。")
            sb.append("财运：财运平稳向好，正财为主，宜储蓄理财、不宜投机。")
            sb.append("感情：和顺融洽，彼此理解，单身者易遇性情相投之人。")
            sb.append("健康：身心安定，无大碍，保持良好作息即可。")
            sb.append("建议：顺势而为、稳中求进，珍惜当下的平和，为长远做积累。")
        }
        sb.append("（大六壬为传统三式之学，结果仅供文化娱乐参考）")
        return sb.toString()
    }
}
