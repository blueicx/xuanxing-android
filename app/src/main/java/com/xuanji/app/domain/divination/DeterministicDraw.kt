package com.xuanji.app.domain.divination

import com.xuanji.app.data.model.DrawnTarot
import com.xuanji.app.data.model.TarotCard
import java.security.MessageDigest

/** Stable, platform-independent draw helper. Never uses kotlin.random.Random. */
object DeterministicDraw {
    fun seed(query: DivinationQuery, salt: String = "draw"): String =
        sha256("${query.canonicalKey()}|$salt")

    fun order(size: Int, query: DivinationQuery, salt: String = "draw"): List<Int> {
        if (size <= 0) return emptyList()
        return (0 until size).sortedBy { sha256("${seed(query, salt)}|$it") }
    }

    fun boolean(query: DivinationQuery, salt: String): Boolean =
        (sha256("${seed(query)}|$salt").first().code and 1) == 1

    fun bounded(query: DivinationQuery, salt: String, bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        val hex = sha256("${seed(query)}|$salt").take(8)
        return (hex.toLong(16) % bound).toInt()
    }

    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

data class DeterministicTarotReading(
    val query: DivinationQuery,
    val seed: String,
    val cards: List<DrawnTarot>,
    val explanation: String = "牌序由日期、问题、档案摘要和牌阵派生；同样输入会得到同样结果。"
)

/** Pure reader; the Android repository only supplies the 78-card deck. */
object DeterministicTarot {
    fun read(deck: List<TarotCard>, query: DivinationQuery): DeterministicTarotReading {
        val count = when (query.spread.lowercase()) {
            "three", "三张", "past-present-future" -> 3
            "five", "五张", "cross" -> 5
            else -> 1
        }.coerceAtMost(deck.size)
        val positions = when (count) {
            3 -> listOf("过去", "现在", "未来")
            5 -> listOf("主题", "阻力", "资源", "行动", "走向")
            else -> listOf("指引")
        }
        val indices = DeterministicDraw.order(deck.size, query, "tarot").take(count)
        val cards = indices.mapIndexed { index, deckIndex ->
            DrawnTarot(
                card = deck[deckIndex],
                reversed = DeterministicDraw.boolean(query, "tarot-reversed-$deckIndex"),
                position = positions.getOrElse(index) { "位置 ${index + 1}" }
            )
        }
        return DeterministicTarotReading(query, DeterministicDraw.seed(query, "tarot"), cards)
    }
}
