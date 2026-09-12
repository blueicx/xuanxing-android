package com.xuanji.app.domain.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Terminal or in-progress evaluation of a session's current position. */
sealed interface GameRequest {
    data object Idle : GameRequest
    data class Thinking(val token: Long, val reason: String) : GameRequest
}

sealed interface EngineResult {
    /** The engine produced a legal, rules-verified reply. Offline play leaves evaluation null. */
    data class Move(val turn: EngineTurn) : EngineResult

    /** No legal move exists (checkmate/stalemate) or the engine declined; the caller falls back to rules. */
    data class NoMove(val reason: String) : EngineResult
}

interface BoardEngine {
    suspend fun bestMove(position: BoardPosition, color: PlayerColor, token: Long): EngineResult
}

/**
 * Deterministic offline fallback. Picks a legal move by stable hashing of the encoded
 * position + color + difficulty; identical inputs always produce identical moves.
 * It never reports evaluations, win rates, or strength levels.
 */
class OfflineBoardEngine(private val difficulty: String = "easy") : BoardEngine {

    override suspend fun bestMove(position: BoardPosition, color: PlayerColor, token: Long): EngineResult =
        withContext(Dispatchers.Default) {
            val legal = XiangqiRules.legalMoves(position, color)
            if (legal.isEmpty()) return@withContext EngineResult.NoMove("no_legal_move")
            // prefer captures and checks slightly so offline play feels alive, but stay deterministic
            val scored = legal.mapIndexed { index, move ->
                val captureBonus = if (move.captured != null) 1 else 0
                val next = XiangqiRules.apply(position, move)
                val checkBonus = if (next is RuleResult.Applied &&
                    XiangqiRules.outcome(next.position) is GameOutcome.Check
                ) 1 else 0
                Triple(index, move, captureBonus + checkBonus)
            }
            val bestScore = scored.maxOf { it.third }
            val preferStrong = difficulty != "easy"
            val pool = if (preferStrong) {
                val strong = scored.filter { it.third == bestScore }
                if (strong.isEmpty()) scored else strong
            } else scored
            val seed = stableHash(XiangqiBoard.encode(position) + "|" + color.name + "|" + difficulty + "|" + token)
            EngineResult.Move(EngineTurn(pool[(seed % pool.size).toInt()].second))
        }

    companion object {
        /** FNV-1a style stable hash; no randomness anywhere. */
        fun stableHash(value: String): Long {
            // FNV-1a 64-bit offset basis (unsigned 14695981039346656037 in signed form)
            var hash = -3750763034362895579L
            for (char in value) {
                hash = hash xor char.code.toLong()
                hash *= 0x100000001b3L
            }
            return hash ushr 1 // keep non-negative
        }
    }
}
