package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.DrawnTarot

/** Observations about this exact draw, not additional predictions or invented traditional quotations. */
object DivinationLearning {
    const val VERSION = "draw-observation-v1"
    const val SCOPE = "项目自编学习提示；只观察当前牌面/符文与位置，不是传统原文、医疗诊断、投资建议或未来事件保证。"

    fun tarotDetail(cards: List<DrawnTarot>, dateKey: String, seed: String?): com.xuanji.app.domain.MysticDetailContext {
        require(cards.isNotEmpty())
        return com.xuanji.app.domain.MysticDetailContext("塔罗", "draw", dateKey, "塔罗牌阵与复盘",
            cards.joinToString("\n\n") { "${it.position} · ${it.card.nameCn}（${if (it.reversed) "逆位" else "正位"}）\n${if (it.reversed) it.card.reversed else it.card.upright}" } +
                "\n\n" + tarotRelations(cards).joinToString("\n"),
            "$SCOPE\n抽取：${seed?.let { "确定性种子 $it" } ?: "用户主动随机抽牌；未提供确定性种子"}", SCOPE, VERSION)
    }

    fun tarotRelations(cards: List<DrawnTarot>): List<String> {
        if (cards.isEmpty()) return emptyList()
        return buildList {
            cards.zipWithNext().forEach { (before, after) ->
                add("${before.position} · ${before.card.nameCn} → ${after.position} · ${after.card.nameCn}：先分别阅读两处解释，再写下共同点或冲突；不把先后位置当作必然因果。")
            }
            cards.filter { it.card.suit.isNotBlank() }.groupingBy { it.card.suit }.eachCount().toSortedMap()
                .filterValues { it > 1 }.forEach { (suit, count) ->
                    add("当前同花色 $suit 共 $count 张：可比较这些牌在各位置的不同作用；重复只是牌面统计，不增加预测概率。")
                }
            add("当前逆位 ${cards.count { it.reversed }}/${cards.size}；正逆位仅按本项目牌库解释，不作为结果好坏分数。")
            add("自我复盘：哪一句描述有现实证据？哪一句不符合实际？下一步能用什么小行动验证？")
        }
    }

    fun runePositions(reading: RuneReading): List<String> = reading.draws.map { draw ->
        "${draw.position} · ${draw.result.rune.symbol} ${draw.result.rune.name}（${if (draw.result.reversed) "逆位" else "正位"}）：" +
            if (draw.result.reversed) draw.result.rune.reversed else draw.result.rune.upright
    }
}

data class NumerologyCalculationStep(val label: String, val number: Int, val calculation: String)
