package com.xuanji.app.domain.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pure-Kotlin alpha-beta search engine. Real game tree search over [XiangqiRules]
 * (depth 2-4 by difficulty) with material + position evaluation. Fully offline and
 * deterministic: identical position + difficulty + token always yields the same move.
 *
 * Search convention: [evaluate] and every node score are from the ROOT side's
 * perspective; maximizing nodes are root-side turns, minimizing nodes are the opponent.
 */
class SmartBoardEngine(private val difficulty: String = NORMAL) : BoardEngine {

    override suspend fun bestMove(position: BoardPosition, color: PlayerColor, token: Long): EngineResult =
        withContext(Dispatchers.Default) {
            // never move on a settled position (mate/stalemate/draw): the reducer refuses
            // such moves with game_over, so answering NoMove keeps the contract honest
            when (XiangqiRules.outcome(position)) {
                is GameOutcome.Checkmate, is GameOutcome.Stalemate, is GameOutcome.Draw ->
                    return@withContext EngineResult.NoMove("game_over")
                else -> Unit
            }
            openingBookMove(position, color)?.let { return@withContext EngineResult.Move(EngineTurn(it)) }
            val legal = XiangqiRules.legalMoves(position, color)
            if (legal.isEmpty()) return@withContext EngineResult.NoMove("no_legal_move")
            val depth = depthOf(difficulty)
            var bestMove: BoardMove? = null
            var bestScore = Int.MIN_VALUE
            var alpha = Int.MIN_VALUE + 1
            val beta = Int.MAX_VALUE
            val ordered = legal.sortedByDescending { moveScoreHint(position, it) }
            for (move in ordered) {
                val applied = XiangqiRules.apply(position, move) as? RuleResult.Applied ?: continue
                val score = alphabeta(applied.position, depth - 1, alpha, beta, color.opponentSide(), color)
                if (score > bestScore) {
                    bestScore = score
                    bestMove = move
                }
                if (bestScore > alpha) alpha = bestScore
            }
            val chosen = bestMove ?: legal.first()
            EngineResult.Move(EngineTurn(chosen))
        }

    private fun alphabeta(
        position: BoardPosition,
        depth: Int,
        alpha: Int,
        beta: Int,
        sideToMove: PlayerColor,
        rootColor: PlayerColor
    ): Int {
        val outcome = XiangqiRules.outcome(position)
        when (outcome) {
            is GameOutcome.Checkmate ->
                return if (outcome.winner == rootColor) MATE_SCORE - (10 - depth) else -(MATE_SCORE - (10 - depth))
            is GameOutcome.Stalemate ->
                // stalemated side loses in xiangqi
                return if (outcome.winner == rootColor) MATE_SCORE - (10 - depth) else -(MATE_SCORE - (10 - depth))
            is GameOutcome.IllegalPosition -> return 0
            is GameOutcome.Draw -> return 0
            GameOutcome.InProgress -> Unit
            is GameOutcome.Check -> Unit
        }
        if (depth == 0) return evaluate(position, rootColor)
        val moves = XiangqiRules.legalMoves(position, sideToMove)
        if (moves.isEmpty()) {
            // no moves while not mated/stalemated cannot happen, but stay safe
            return evaluate(position, rootColor)
        }
        val maximizing = sideToMove == rootColor
        var best = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE
        var a = alpha
        var b = beta
        for (move in moves.sortedByDescending { moveScoreHint(position, it) }.take(18)) {
            val applied = XiangqiRules.apply(position, move) as? RuleResult.Applied ?: continue
            val score = alphabeta(applied.position, depth - 1, a, b, sideToMove.opponentSide(), rootColor)
            if (maximizing) {
                if (score > best) best = score
                if (best > a) a = best
            } else {
                if (score < best) best = score
                if (best < b) b = best
            }
            if (a >= b) break
        }
        return best
    }

    /** Material + crossed-pawn/cannon-position evaluation, always from rootColor's view. */
    private fun evaluate(position: BoardPosition, rootColor: PlayerColor): Int {
        var score = 0
        val opponent = rootColor.opponentSide()
        for (rank in 0..9) for (file in 0..8) {
            val square = Square(file, rank)
            val piece = position.pieceAt(square) ?: continue
            val value = PIECE_VALUE.getValue(piece.kind) + positionBonus(piece, square)
            if (piece.color == rootColor) score += value else score -= value
        }
        return score
    }

    private fun positionBonus(piece: Piece, square: Square): Int = when (piece.kind) {
        PieceKind.SOLDIER -> {
            val crossed = if (piece.color == PlayerColor.RED) square.rank <= 4 else square.rank >= 5
            if (crossed) 12 else 2
        }
        PieceKind.CANNON -> if (square.rank in 3..6) 4 else 0
        PieceKind.HORSE -> if (square.rank in 2..7) 4 else 0
        else -> 0
    }

    /** Cheap move-ordering: real captures first (value read from the board, not the flag). */
    private fun moveScoreHint(position: BoardPosition, move: BoardMove): Int {
        val target = move.from?.let { position.pieceAt(move.to) } ?: return 0
        return PIECE_VALUE.getValue(target.kind) / 10
    }

    private fun openingBookMove(position: BoardPosition, color: PlayerColor): BoardMove? {
        if (color != PlayerColor.RED) return null
        val encoded = XiangqiBoard.encode(position)
        val target = OPENING_BOOK[encoded] ?: return null
        // the book entry is the cannon (7,7) moving to the target file
        val candidate = BoardMove(Square(7, 7), target, "", player = PlayerColor.RED)
        return if (XiangqiRules.legalMoves(position, PlayerColor.RED).any {
                it.from == candidate.from && it.to == candidate.to
            }
        ) candidate else null
    }

    companion object {
        private const val MATE_SCORE = 100000

        const val EASY = "easy"
        const val NORMAL = "normal"
        const val HARD = "hard"

        /** Difficulty vocabulary shared by the dialogue bridge and the board UI. */
        val LEVELS: List<String> = listOf(EASY, NORMAL, HARD)

        fun depthOf(difficulty: String): Int = when (difficulty) {
            HARD -> 4
            NORMAL -> 3
            else -> 2
        }

        fun labelOf(difficulty: String): String = when (difficulty) {
            HARD -> "困难"
            NORMAL -> "普通"
            else -> "轻松"
        }

        /** Map a free-text difficulty request onto a known level; null when unrecognized. */
        fun parseLabel(text: String): String? = when {
            text.contains("困难") || text.contains("高手") || text.contains("最强") -> HARD
            text.contains("简单") || text.contains("轻松") || text.contains("新手") -> EASY
            text.contains("普通") || text.contains("标准") -> NORMAL
            else -> null
        }

        private val PIECE_VALUE = mapOf(
            PieceKind.GENERAL to 10000,
            PieceKind.ROOK to 900,
            PieceKind.CANNON to 450,
            PieceKind.HORSE to 400,
            PieceKind.ADVISOR to 200,
            PieceKind.ELEPHANT to 200,
            PieceKind.SOLDIER to 100
        )

        private fun PlayerColor.opponentSide(): PlayerColor =
            if (this == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED

        /** Minimal opening book: 炮二平五 for the standard opening position (red to move). */
        private val OPENING_BOOK: Map<String, Square> = mapOf(
            "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR r" to Square(4, 7)
        )
    }
}
