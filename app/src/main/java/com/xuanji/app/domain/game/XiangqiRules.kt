package com.xuanji.app.domain.game

/** Outcome values shared by rules, reducer, dialogue, and UI. */
sealed interface GameOutcome {
    val isIllegalPosition: Boolean

    data object InProgress : GameOutcome {
        override val isIllegalPosition: Boolean = false
    }

    data class Check(val byColor: PlayerColor) : GameOutcome {
        override val isIllegalPosition: Boolean = false
    }

    data class Checkmate(val winner: PlayerColor) : GameOutcome {
        override val isIllegalPosition: Boolean = false
    }

    /** In xiangqi, stalemated side loses. */
    data class Stalemate(val winner: PlayerColor) : GameOutcome {
        override val isIllegalPosition: Boolean = false
    }

    /** Threefold repetition or the 60-halfmove no-capture limit: game drawn. */
    data object Draw : GameOutcome {
        override val isIllegalPosition: Boolean = false
    }

    /** Generals facing each other (双将对脸) or other rule violations. */
    data class IllegalPosition(val reason: String) : GameOutcome {
        override val isIllegalPosition: Boolean = true
    }
}

sealed interface RuleResult {
    data class Applied(val position: BoardPosition, val move: BoardMove) : RuleResult
    data class Rejected(val code: String) : RuleResult
}

interface BoardRules {
    fun legalMoves(position: BoardPosition, color: PlayerColor): List<BoardMove>
    fun apply(position: BoardPosition, move: BoardMove): RuleResult
    fun outcome(position: BoardPosition): GameOutcome
}

object XiangqiRules : BoardRules {

    const val ERR_FROM_EMPTY = "from_empty"
    const val ERR_WRONG_TURN = "wrong_turn"
    const val ERR_ILLEGAL_MOVE = "illegal_move"
    const val ERR_SELF_CHECK = "self_check"
    const val ERR_GAME_OVER = "game_over"

    override fun legalMoves(position: BoardPosition, color: PlayerColor): List<BoardMove> {
        val moves = mutableListOf<BoardMove>()
        for (rank in 0..9) for (file in 0..8) {
            val piece = position.pieceAt(Square(file, rank)) ?: continue
            if (piece.color != color) continue
            moves += pieceMoves(position, piece, Square(file, rank))
        }
        // filter moves that leave own general attacked or expose it to the opposing general
        return moves.filter { move ->
            val from = move.from ?: return@filter false
            val next = position.withPiece(from, null).withPiece(move.to, position.pieceAt(from))
            !isGeneralAttacked(next, color)
        }
    }

    override fun apply(position: BoardPosition, move: BoardMove): RuleResult {
        if (move.from == null) return RuleResult.Rejected(ERR_ILLEGAL_MOVE)
        val piece = position.pieceAt(move.from) ?: return RuleResult.Rejected(ERR_FROM_EMPTY)
        if (piece.color != position.sideToMove) return RuleResult.Rejected(ERR_WRONG_TURN)
        val currentOutcome = outcome(position)
        when (currentOutcome) {
            is GameOutcome.Checkmate, is GameOutcome.Stalemate, is GameOutcome.IllegalPosition ->
                return RuleResult.Rejected(ERR_GAME_OVER)
            GameOutcome.Draw ->
                return RuleResult.Rejected(ERR_GAME_OVER)
            GameOutcome.InProgress, is GameOutcome.Check -> Unit
        }
        if (move.player != piece.color) return RuleResult.Rejected(ERR_WRONG_TURN)
        val candidate = move.copy(player = piece.color, captured = position.pieceAt(move.to)?.let { glyphName(it) })
        if (candidate.to == candidate.from) return RuleResult.Rejected(ERR_ILLEGAL_MOVE)
        if (position.pieceAt(candidate.to)?.color == piece.color) return RuleResult.Rejected(ERR_ILLEGAL_MOVE)
        if (!pseudoLegal(position, piece, candidate)) return RuleResult.Rejected(ERR_ILLEGAL_MOVE)
        val next = position.withPiece(move.from, null).withPiece(move.to, piece)
            .copy(sideToMove = opponentOf(piece.color))
        if (isGeneralAttacked(next, piece.color)) return RuleResult.Rejected(ERR_SELF_CHECK)
        val nextOutcome = outcome(next)
        if (nextOutcome.isIllegalPosition) return RuleResult.Rejected(ERR_ILLEGAL_MOVE)
        return RuleResult.Applied(next, candidate)
    }

    override fun outcome(position: BoardPosition): GameOutcome {
        val side = position.sideToMove
        // generals facing each other is an illegal position regardless of turn
        val redGeneral = findGeneral(position, PlayerColor.RED)
        val blackGeneral = findGeneral(position, PlayerColor.BLACK)
        if (redGeneral == null || blackGeneral == null) {
            return GameOutcome.IllegalPosition("general_missing")
        }
        if (redGeneral.file == blackGeneral.file &&
            !fileBlocked(position, redGeneral.file, redGeneral.rank, blackGeneral.rank)
        ) {
            return GameOutcome.IllegalPosition("generals_facing")
        }
        val inCheck = isGeneralAttacked(position, side)
        val hasMoves = legalMoves(position, side).isNotEmpty()
        return when {
            inCheck && !hasMoves -> GameOutcome.Checkmate(opponentOf(side))
            !inCheck && !hasMoves -> GameOutcome.Stalemate(opponentOf(side))
            inCheck -> GameOutcome.Check(opponentOf(side))
            else -> GameOutcome.InProgress
        }
    }

    // ---- attack detection ------------------------------------------------------

    private fun isGeneralAttacked(position: BoardPosition, color: PlayerColor): Boolean {
        val general = findGeneral(position, color) ?: return true
        val opponent = opponentOf(color)
        for (rank in 0..9) for (file in 0..8) {
            val piece = position.pieceAt(Square(file, rank)) ?: continue
            if (piece.color != opponent) continue
            val attack = BoardMove(Square(file, rank), general, "", player = opponent)
            if (pseudoLegal(position, piece, attack, allowKingCapture = true)) return true
        }
        // flying general rule: facing generals count as an attack too
        if (general.file == findGeneral(position, opponent)?.file) {
            val other = findGeneral(position, opponent)!!
            if (!fileBlocked(position, general.file, general.rank, other.rank)) return true
        }
        return false
    }

    private fun findGeneral(position: BoardPosition, color: PlayerColor): Square? {
        val rankRange = if (color == PlayerColor.RED) 7..9 else 0..2
        for (rank in rankRange) for (file in 3..5) {
            val piece = position.pieceAt(Square(file, rank))
            if (piece != null && piece.color == color && piece.kind == PieceKind.GENERAL) {
                return Square(file, rank)
            }
        }
        return null
    }

    // ---- pseudo-legal movement -------------------------------------------------

    /**
     * Moving onto the enemy GENERAL is not a legal board move in xiangqi (the game ends
     * in mate before it can happen); attack detection ([isGeneralAttacked]) still needs
     * "can this piece strike the general" semantics, so it opts in via [allowKingCapture].
     */
    private fun pseudoLegal(
        position: BoardPosition,
        piece: Piece,
        move: BoardMove,
        allowKingCapture: Boolean = false
    ): Boolean {
        val from = move.from ?: return false
        val to = move.to
        if (to.file !in 0..8 || to.rank !in 0..9) return false
        if (from == to) return false
        val target = position.pieceAt(to)
        if (target != null && target.color == piece.color) return false
        if (!allowKingCapture && target != null && target.kind == PieceKind.GENERAL && target.color != piece.color) {
            return false
        }
        return when (piece.kind) {
            PieceKind.GENERAL -> generalMove(from, to, piece.color)
            PieceKind.ADVISOR -> advisorMove(from, to, piece.color)
            PieceKind.ELEPHANT -> elephantMove(position, from, to, piece.color)
            PieceKind.HORSE -> horseMove(position, from, to)
            PieceKind.ROOK -> rookMove(position, from, to)
            PieceKind.CANNON -> cannonMove(position, from, to)
            PieceKind.SOLDIER -> soldierMove(from, to, piece.color)
        }
    }

    private fun generalMove(from: Square, to: Square, color: PlayerColor): Boolean {
        val palaceFiles = 3..5
        val palaceRanks = if (color == PlayerColor.RED) 7..9 else 0..2
        if (from.file !in palaceFiles || from.rank !in palaceRanks) return false
        if (to.file !in palaceFiles || to.rank !in palaceRanks) return false
        val df = kotlin.math.abs(to.file - from.file)
        val dr = kotlin.math.abs(to.rank - from.rank)
        return (df == 1 && dr == 0) || (df == 0 && dr == 1)
    }

    private fun advisorMove(from: Square, to: Square, color: PlayerColor): Boolean {
        val palaceFiles = 3..5
        val palaceRanks = if (color == PlayerColor.RED) 7..9 else 0..2
        if (from.file !in palaceFiles || from.rank !in palaceRanks) return false
        if (to.file !in palaceFiles || to.rank !in palaceRanks) return false
        return kotlin.math.abs(to.file - from.file) == 1 && kotlin.math.abs(to.rank - from.rank) == 1
    }

    private fun elephantMove(position: BoardPosition, from: Square, to: Square, color: PlayerColor): Boolean {
        val homeRanks = if (color == PlayerColor.RED) 5..9 else 0..4
        if (from.rank !in homeRanks || to.rank !in homeRanks) return false
        val df = to.file - from.file
        val dr = to.rank - from.rank
        if (kotlin.math.abs(df) != 2 || kotlin.math.abs(dr) != 2) return false
        val eye = Square(from.file + df / 2, from.rank + dr / 2)
        return position.pieceAt(eye) == null
    }

    private fun horseMove(position: BoardPosition, from: Square, to: Square): Boolean {
        val df = to.file - from.file
        val dr = to.rank - from.rank
        val (leg, ok) = when {
            kotlin.math.abs(df) == 1 && kotlin.math.abs(dr) == 2 -> Square(from.file, from.rank + dr / 2) to true
            kotlin.math.abs(df) == 2 && kotlin.math.abs(dr) == 1 -> Square(from.file + df / 2, from.rank) to true
            else -> null to false
        }
        if (!ok || leg == null) return false
        return position.pieceAt(leg) == null
    }

    private fun rookMove(position: BoardPosition, from: Square, to: Square): Boolean {
        if (from.file != to.file && from.rank != to.rank) return false
        return pathClearOrthogonal(position, from, to)
    }

    private fun cannonMove(position: BoardPosition, from: Square, to: Square): Boolean {
        if (from.file != to.file && from.rank != to.rank) return false
        val target = position.pieceAt(to)
        val screens = countScreens(position, from, to)
        return if (target == null) screens == 0 else screens == 1
    }

    private fun soldierMove(from: Square, to: Square, color: PlayerColor): Boolean {
        val df = to.file - from.file
        val dr = to.rank - from.rank
        val forward = if (color == PlayerColor.RED) -1 else 1
        val crossedRiver = if (color == PlayerColor.RED) from.rank <= 4 else from.rank >= 5
        return when {
            df == 0 && dr == forward -> true
            crossedRiver && kotlin.math.abs(df) == 1 && dr == 0 -> true
            else -> false
        }
    }

    // ---- path helpers ----------------------------------------------------------

    private fun pathClearOrthogonal(position: BoardPosition, from: Square, to: Square): Boolean {
        if (from.file == to.file) {
            val step = if (to.rank > from.rank) 1 else -1
            var rank = from.rank + step
            while (rank != to.rank) {
                if (position.pieceAt(Square(from.file, rank)) != null) return false
                rank += step
            }
        } else {
            val step = if (to.file > from.file) 1 else -1
            var file = from.file + step
            while (file != to.file) {
                if (position.pieceAt(Square(file, from.rank)) != null) return false
                file += step
            }
        }
        return true
    }

    private fun countScreens(position: BoardPosition, from: Square, to: Square): Int {
        var count = 0
        if (from.file == to.file) {
            val step = if (to.rank > from.rank) 1 else -1
            var rank = from.rank + step
            while (rank != to.rank) {
                if (position.pieceAt(Square(from.file, rank)) != null) count++
                rank += step
            }
        } else {
            val step = if (to.file > from.file) 1 else -1
            var file = from.file + step
            while (file != to.file) {
                if (position.pieceAt(Square(file, from.rank)) != null) count++
                file += step
            }
        }
        return count
    }

    private fun fileBlocked(
        position: BoardPosition,
        file: Int,
        rankA: Int,
        rankB: Int
    ): Boolean {
        val step = if (rankB > rankA) 1 else -1
        var rank = rankA + step
        while (rank != rankB) {
            if (position.pieceAt(Square(file, rank)) != null) return true
            rank += step
        }
        return false
    }

    private fun pieceMoves(position: BoardPosition, piece: Piece, from: Square): List<BoardMove> {
        val moves = mutableListOf<BoardMove>()
        val targets = when (piece.kind) {
            PieceKind.GENERAL, PieceKind.ADVISOR -> palaceTargets(from, piece.color)
            PieceKind.ELEPHANT -> listOf(
                Square(from.file - 2, from.rank - 2), Square(from.file + 2, from.rank - 2),
                Square(from.file - 2, from.rank + 2), Square(from.file + 2, from.rank + 2)
            )
            PieceKind.HORSE -> listOf(
                Square(from.file - 1, from.rank - 2), Square(from.file + 1, from.rank - 2),
                Square(from.file - 2, from.rank - 1), Square(from.file + 2, from.rank - 1),
                Square(from.file - 2, from.rank + 1), Square(from.file + 2, from.rank + 1),
                Square(from.file - 1, from.rank + 2), Square(from.file + 1, from.rank + 2)
            )
            PieceKind.ROOK, PieceKind.CANNON -> lineTargets(position, from)
            PieceKind.SOLDIER -> soldierTargets(from, piece.color)
        }
        targets.forEach { to ->
            val move = BoardMove(from, to, "", player = piece.color)
            if (pseudoLegal(position, piece, move)) moves += move
        }
        return moves
    }

    private fun palaceTargets(from: Square, color: PlayerColor): List<Square> {
        val palaceFiles = 3..5
        val palaceRanks = if (color == PlayerColor.RED) 7..9 else 0..2
        val candidates = mutableListOf<Square>()
        for (df in -1..1) for (dr in -1..1) {
            if (df == 0 && dr == 0) continue
            candidates += Square(from.file + df, from.rank + dr)
        }
        // advisor only diagonal, general only orthogonal — pseudoLegal re-filters
        return candidates.filter { it.file in palaceFiles && it.rank in palaceRanks }
    }

    private fun lineTargets(position: BoardPosition, from: Square): List<Square> {
        val targets = mutableListOf<Square>()
        for (direction in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
            var file = from.file + direction.first
            var rank = from.rank + direction.second
            while (file in 0..8 && rank in 0..9) {
                targets += Square(file, rank)
                file += direction.first
                rank += direction.second
            }
        }
        return targets
    }

    private fun soldierTargets(from: Square, color: PlayerColor): List<Square> {
        val forward = if (color == PlayerColor.RED) -1 else 1
        val crossedRiver = if (color == PlayerColor.RED) from.rank <= 4 else from.rank >= 5
        val candidates = mutableListOf(Square(from.file, from.rank + forward))
        if (crossedRiver) {
            candidates += Square(from.file - 1, from.rank)
            candidates += Square(from.file + 1, from.rank)
        }
        return candidates
    }

    private fun glyphName(piece: Piece): String = when (piece.kind) {
        PieceKind.GENERAL -> if (piece.color == PlayerColor.RED) "帅" else "将"
        PieceKind.ADVISOR -> "士"
        PieceKind.ELEPHANT -> "象"
        PieceKind.HORSE -> "马"
        PieceKind.ROOK -> "车"
        PieceKind.CANNON -> "炮"
        PieceKind.SOLDIER -> if (piece.color == PlayerColor.RED) "兵" else "卒"
    }

    fun pieceName(piece: Piece): String = glyphName(piece)

    private fun opponentOf(color: PlayerColor): PlayerColor =
        if (color == PlayerColor.RED) PlayerColor.BLACK else PlayerColor.RED
}
