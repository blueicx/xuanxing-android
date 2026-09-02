package com.xuanji.app.domain.game

/**
 * Reserved Go (weiqi) session adapter. Without a real GTP provider (e.g. KataGo) every
 * query returns an explicit unavailable result — the UI shows "尚未启用" and never a
 * simulated board. When a provider is wired later it plugs into the same BoardEngine
 * response contract (token-bound, cancellable, rules-verified).
 */
class GoSessionAdapter {

    private val rulesAvailability = GoContracts.availability()

    fun availability(): GoRulesAvailability = rulesAvailability

    suspend fun respond(
        position: GoPosition,
        move: GoPlaceMove,
        token: Long
    ): EngineResult {
        // No GTP provider: never fabricate a stone, a capture, or a score.
        return EngineResult.NoMove(GoRulesAvailability.PROVIDER_NOT_ENABLED)
    }
}

/**
 * Minimal chess position value object reserving the UCI seam. It does not implement
 * chess rules; it only marks which side moves so future Stockfish integration can
 * reuse the BoardEngine contract without touching the xiangqi code paths.
 */
data class ChessPosition(
    val sideToMove: PlayerColor = PlayerColor.WHITE,
    val uci: String = START_FEN
) {
    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun start(): ChessPosition = ChessPosition()
    }
}

/**
 * Reserved chess engine adapter. Without a real UCI chess engine every query returns
 * an explicit unavailable result. Chess coordinates must never be confused with
 * xiangqi squares: the future adapter converts through XiangqiNotation-compatible
 * uci helpers only after its own board validation.
 */
class ChessEngineAdapter {

    suspend fun respond(position: ChessPosition, color: PlayerColor, token: Long): EngineResult {
        return EngineResult.NoMove("chess_provider_not_enabled")
    }
}
