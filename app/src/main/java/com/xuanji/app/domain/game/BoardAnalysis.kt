package com.xuanji.app.domain.game

/**
 * Pure board-analysis helpers shared by dialogue grounding and the UI: attack maps and
 * threats for the current position. All answers derive from [XiangqiRules] on the real
 * position — nothing here invents board facts.
 */
object BoardAnalysis {

    /** Chinese side + glyph naming kept domain-side to avoid a dependency on the UI layer. */
    fun pieceName(piece: Piece): String {
        val side = if (piece.color == PlayerColor.RED) "红方" else "黑方"
        val glyph = when (piece.kind) {
            PieceKind.GENERAL -> if (piece.color == PlayerColor.RED) "帥" else "將"
            PieceKind.ADVISOR -> if (piece.color == PlayerColor.RED) "仕" else "士"
            PieceKind.ELEPHANT -> if (piece.color == PlayerColor.RED) "相" else "象"
            PieceKind.HORSE -> if (piece.color == PlayerColor.RED) "傌" else "馬"
            PieceKind.ROOK -> if (piece.color == PlayerColor.RED) "俥" else "車"
            PieceKind.CANNON -> if (piece.color == PlayerColor.RED) "炮" else "砲"
            PieceKind.SOLDIER -> if (piece.color == PlayerColor.RED) "兵" else "卒"
        }
        return side + glyph
    }

    data class Threat(
        val attacked: Square,
        val attackedPiece: String,
        val attacker: Square,
        val attackerPiece: String
    )

    /**
     * For every piece of [defender] that an enemy move could capture right now, report
     * the pairing. Result is deterministic (board scan order).
     */
    fun threatsAgainst(position: BoardPosition, defender: PlayerColor): List<Threat> {
        val attacker = if (defender == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
        val enemyMoves = XiangqiRules.legalMoves(position, attacker)
        val threats = mutableListOf<Threat>()
        for (rank in 0..9) for (file in 0..8) {
            val square = Square(file, rank)
            val piece = position.pieceAt(square) ?: continue
            if (piece.color != defender) continue
            val incoming = enemyMoves.firstOrNull { it.to == square } ?: continue
            val attackerSquare = incoming.from ?: continue
            val attackerPiece = position.pieceAt(attackerSquare) ?: continue
            threats += Threat(
                attacked = square,
                attackedPiece = pieceName(piece),
                attacker = attackerSquare,
                attackerPiece = pieceName(attackerPiece)
            )
        }
        return threats
    }
}

/**
 * Fixed endgame puzzles (残局). Each entry is a position + the side to solve it; the
 * puzzle is verified by test to be winnable through [XiangqiRules]. Positions reuse the
 * stable FEN-like encoding so they can also come from the miniprogram later.
 */
object EndgameCatalog {

    data class Puzzle(
        val id: String,
        val title: String,
        val encoding: String,
        val solver: PlayerColor
    ) {
        fun position(): BoardPosition = XiangqiBoard.decode(encoding)
    }

    val ALL: List<Puzzle> = listOf(
        Puzzle(
            id = "mating_rook",
            title = "单车必胜残局（红先胜）",
            // red: general (4,9), rook (5,1); black: lone general (3,0) — files differ, no facing generals
            encoding = "3k5/5R3/9/9/9/9/9/9/9/4K4 r",
            solver = PlayerColor.RED
        ),
        Puzzle(
            id = "rook_vs_advisors",
            title = "车破双士（红先胜）",
            // red: general (3,9), rook (0,5); black: general (4,0), advisors (3,0),(5,0) — files differ
            encoding = "3aka3/9/9/9/9/R8/9/9/9/3K5 r",
            solver = PlayerColor.RED
        ),
        Puzzle(
            id = "cannon_mate_setup",
            title = "空头炮杀势（红先胜）",
            // red: general (3,9), advisor (4,9), cannon (4,4); black: lone general (4,0)
            encoding = "4k4/9/9/9/4C4/9/9/9/9/3KA4 r",
            solver = PlayerColor.RED
        )
    )

    fun byId(id: String): Puzzle? = ALL.firstOrNull { it.id == id }
}
