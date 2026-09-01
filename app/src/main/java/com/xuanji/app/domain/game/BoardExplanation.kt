package com.xuanji.app.domain.game

/**
 * Explanation facts derived only from [XiangqiRules] on a real position: which pieces
 * actually hang (attacked, and nobody can legally take the attacker back), what a played
 * move exposed, and which legal move leaves fewer of my pieces attacked.
 *
 * No engine evaluation reaches this file, and no number produced here is a strength
 * rating: the counts are "how many pieces are attacked", never a score.
 */
object BoardExplanation {

    /** One of [mine]'s pieces under attack, with the recapture facts that decide whether it is free. */
    data class Hanging(
        val square: Square,
        val piece: String,
        val attackers: List<Square>,
        val recapturers: List<Square>
    ) {
        /** True when taking this piece costs the opponent nothing they can be made to pay for. */
        val isUndefended: Boolean get() = recapturers.isEmpty()
    }

    /** What the last halfmove really did to the mover's own safety. */
    data class Critique(
        val landedUnderFire: Boolean,
        val newlyUndefended: List<Hanging>
    )

    /**
     * The calmest move found by scanning legal moves, with attacked-piece counts on both frames
     * and the recapture-checked facts of the position it lands in.
     */
    data class Safer(
        val move: BoardMove,
        val attackedBefore: Int,
        val attackedAfter: Int,
        val undefendedAfter: List<Hanging>
    ) {
        val improves: Boolean get() = attackedAfter < attackedBefore
    }

    fun exposed(position: BoardPosition, mine: PlayerColor): List<Hanging> {
        val result = mutableListOf<Hanging>()
        for (rank in 0..9) for (file in 0..8) {
            val square = Square(file, rank)
            val piece = position.pieceAt(square) ?: continue
            if (piece.color != mine) continue
            val attackers = attackersOf(position, square, mine)
            if (attackers.isEmpty()) continue
            result += Hanging(
                square = square,
                piece = BoardAnalysis.pieceName(piece),
                attackers = attackers,
                recapturers = recapturersOf(position, square, mine, attackers)
            )
        }
        return result
    }

    /**
     * Squares [mine]'s enemies could legally land on — an occupied one of mine means a capture,
     * an empty one means "walking here would be covered". The rules refuse to generate a move
     * capturing a general, so a general never appears as attackable.
     */
    fun attackersOf(position: BoardPosition, square: Square, mine: PlayerColor): List<Square> =
        XiangqiRules.legalMoves(position, opponentOf(mine))
            .filter { it.to == square }
            .mapNotNull { it.from }
            .distinct()

    fun critique(before: BoardPosition, after: BoardPosition, move: BoardMove): Critique {
        val mover = move.player
        val alreadyUndefended = exposed(before, mover).filter { it.isUndefended }.map { it.square }.toSet()
        return Critique(
            landedUnderFire = attackersOf(after, move.to, mover).isNotEmpty(),
            newlyUndefended = exposed(after, mover).filter { it.isUndefended && it.square !in alreadyUndefended }
        )
    }

    /**
     * Best of the first [candidateLimit] legal moves in board scan order, ranked by how many
     * of [mover]'s pieces are attacked afterwards. Ties keep the earlier move, so the answer
     * is deterministic without consulting any random or hashed value.
     */
    fun safest(position: BoardPosition, mover: PlayerColor, difficulty: String): Safer? {
        val candidates = XiangqiRules.legalMoves(position, mover).take(candidateLimit(difficulty))
        if (candidates.isEmpty()) return null
        val attackedBefore = BoardAnalysis.threatsAgainst(position, mover).size
        // XiangqiRules.apply refuses a move whose side is not the one to move
        val probe = position.copy(sideToMove = mover)
        var best: Triple<BoardPosition, BoardMove, Int>? = null
        for (move in candidates) {
            val applied = XiangqiRules.apply(probe, move.copy(player = mover)) as? RuleResult.Applied
                ?: continue
            val attackedAfter = BoardAnalysis.threatsAgainst(applied.position, mover).size
            val current = best
            if (current == null || attackedAfter < current.third) {
                best = Triple(applied.position, applied.move, attackedAfter)
            }
        }
        val (after, winningMove, attackedAfter) = best ?: return null
        return Safer(
            move = winningMove,
            attackedBefore = attackedBefore,
            attackedAfter = attackedAfter,
            undefendedAfter = exposed(after, mover).filter { it.isUndefended }
        )
    }

    fun candidateLimit(difficulty: String): Int = when (difficulty) {
        SmartBoardEngine.HARD -> 40
        SmartBoardEngine.NORMAL -> 14
        else -> 6
    }

    /**
     * A recapture is real only if it is legal *after* the capture, so the attacker is moved
     * onto the square first and [XiangqiRules.legalMoves] decides the rest — that filter is
     * also what keeps a pinned defender from counting as protection.
     */
    private fun recapturersOf(
        position: BoardPosition,
        square: Square,
        mine: PlayerColor,
        attackers: List<Square>
    ): List<Square> {
        val result = mutableListOf<Square>()
        for (attacker in attackers) {
            val piece = position.pieceAt(attacker) ?: continue
            val taken = position.withPiece(attacker, null).withPiece(square, piece)
            val back = XiangqiRules.legalMoves(taken, mine).firstOrNull { it.to == square }?.from
            if (back != null && back !in result) result += back
        }
        return result
    }

    private fun opponentOf(color: PlayerColor): PlayerColor =
        if (color == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
}
