package com.xuanji.app.domain.game

/**
 * Stable vocabulary shared by the dialogue engine, the board UI, and future engines.
 * Values here are contract types: they must stay serializable, UI-free, and deterministic.
 */
enum class GameType { XIANGQI, CHESS, GO }

enum class PlayerColor { RED, BLACK, WHITE }

/** file = column 0..8, rank = row 0..9 (rank 0 is the Black back rank, rank 9 the Red back rank). */
data class Square(val file: Int, val rank: Int)

data class BoardMove(
    val from: Square?,
    val to: Square,
    val notation: String,
    val captured: String? = null,
    val player: PlayerColor = PlayerColor.RED
)

/** Optional engine metric. Offline play never populates or displays these values. */
data class EngineEvaluation(val centipawns: Int?, val mateIn: Int?, val depth: Int)

data class EngineTurn(val move: BoardMove, val evaluation: EngineEvaluation? = null)

/**
 * Go (weiqi) is reserved only. No rules, no fake board, no simulated score until a real
 * provider (GTP engine) is wired in; the UI must surface [GoRulesAvailability.unavailable].
 */
data class GoPosition(
    val captures: Map<PlayerColor, Int> = emptyMap(),
    val playable: Boolean = false
)

data class GoPlaceMove(val at: Square, val color: PlayerColor)

data class GoRulesAvailability(val available: Boolean, val reason: String?) {
    companion object {
        const val PROVIDER_NOT_ENABLED = "go_provider_not_enabled"
        fun unavailable(reason: String) = GoRulesAvailability(available = false, reason = reason)
    }
}
