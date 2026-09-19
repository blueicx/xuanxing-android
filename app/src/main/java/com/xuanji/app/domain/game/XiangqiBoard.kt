package com.xuanji.app.domain.game

/**
 * Immutable xiangqi board. Coordinate convention (frozen in the 2026-09-01 design spec):
 *  - file = 0..8, left to right; Red numerals map file 0..8 to 九八七六五四三二一.
 *  - rank = 0..9; rank 0 is the Black back rank, rank 9 the Red back rank.
 *  - Red advances by decreasing rank, Black by increasing rank.
 */
data class Piece(val color: PlayerColor, val kind: PieceKind)

enum class PieceKind {
    GENERAL, ADVISOR, ELEPHANT, HORSE, ROOK, CANNON, SOLDIER
}

data class BoardPosition(
    val pieces: List<Piece?>,
    val sideToMove: PlayerColor
) {
    fun pieceAt(square: Square): Piece? {
        val index = square.rank * 9 + square.file
        if (square.file !in 0..8 || square.rank !in 0..9) return null
        return pieces.getOrNull(index)
    }

    fun withPiece(square: Square, piece: Piece?): BoardPosition {
        val index = square.rank * 9 + square.file
        val next = pieces.toMutableList()
        next[index] = piece
        return copy(pieces = next)
    }

    companion object {
        fun empty(sideToMove: PlayerColor = PlayerColor.RED): BoardPosition =
            BoardPosition(List(90) { null }, sideToMove)
    }
}

object XiangqiBoard {

    fun initial(): BoardPosition {
        var position = BoardPosition.empty(PlayerColor.RED)
        val backRank = listOf(
            PieceKind.ROOK, PieceKind.HORSE, PieceKind.ELEPHANT, PieceKind.ADVISOR, PieceKind.GENERAL,
            PieceKind.ADVISOR, PieceKind.ELEPHANT, PieceKind.HORSE, PieceKind.ROOK
        )
        for (file in 0..8) {
            position = position.withPiece(Square(file, 0), Piece(PlayerColor.BLACK, backRank[file]))
            position = position.withPiece(Square(file, 9), Piece(PlayerColor.RED, backRank[file]))
        }
        for (file in listOf(1, 7)) {
            position = position.withPiece(Square(file, 2), Piece(PlayerColor.BLACK, PieceKind.CANNON))
            position = position.withPiece(Square(file, 7), Piece(PlayerColor.RED, PieceKind.CANNON))
        }
        for (file in listOf(0, 2, 4, 6, 8)) {
            position = position.withPiece(Square(file, 3), Piece(PlayerColor.BLACK, PieceKind.SOLDIER))
            position = position.withPiece(Square(file, 6), Piece(PlayerColor.RED, PieceKind.SOLDIER))
        }
        return position
    }

    fun empty(sideToMove: PlayerColor = PlayerColor.RED): BoardPosition = BoardPosition.empty(sideToMove)

    /** FEN-like stable encoding; uppercase = Red, lowercase = Black, red side to move appended as " r"/" b". */
    fun encode(position: BoardPosition): String {
        val rows = mutableListOf<String>()
        for (rank in 0..9) {
            val builder = StringBuilder()
            var emptyRun = 0
            for (file in 0..8) {
                val piece = position.pieceAt(Square(file, rank))
                if (piece == null) {
                    emptyRun++
                } else {
                    if (emptyRun > 0) builder.append(emptyRun.toString())
                    emptyRun = 0
                    builder.append(glyph(piece))
                }
            }
            if (emptyRun > 0) builder.append(emptyRun.toString())
            rows.add(builder.toString())
        }
        val suffix = if (position.sideToMove == PlayerColor.RED) "r" else "b"
        return rows.joinToString("/") + " " + suffix
    }

    fun decode(encoded: String): BoardPosition {
        val parts = encoded.trim().split(" ")
        val rows = parts[0].split("/")
        var position = BoardPosition.empty(if (parts.getOrNull(1) == "b") PlayerColor.BLACK else PlayerColor.RED)
        rows.forEachIndexed { rank, row ->
            var file = 0
            row.forEach { char ->
                if (char.isDigit()) {
                    repeat(char - '0') { position = position.withPiece(Square(file, rank), null); file++ }
                } else {
                    position = position.withPiece(Square(file, rank), pieceFromGlyph(char))
                    file++
                }
            }
        }
        return position
    }

    /** Number of occurrences of the final position encoding in the history (repetition bookkeeping). */
    fun repetitionCount(history: List<String>): Int {
        val last = history.lastOrNull() ?: return 0
        return history.count { it == last }
    }

    fun glyph(piece: Piece): Char = when (piece.kind) {
        PieceKind.GENERAL -> if (piece.color == PlayerColor.RED) 'K' else 'k'
        PieceKind.ADVISOR -> if (piece.color == PlayerColor.RED) 'A' else 'a'
        PieceKind.ELEPHANT -> if (piece.color == PlayerColor.RED) 'B' else 'b'
        PieceKind.HORSE -> if (piece.color == PlayerColor.RED) 'N' else 'n'
        PieceKind.ROOK -> if (piece.color == PlayerColor.RED) 'R' else 'r'
        PieceKind.CANNON -> if (piece.color == PlayerColor.RED) 'C' else 'c'
        PieceKind.SOLDIER -> if (piece.color == PlayerColor.RED) 'P' else 'p'
    }

    fun pieceFromGlyph(char: Char): Piece {
        val isRed = char.isUpperCase()
        val color = if (isRed) PlayerColor.RED else PlayerColor.BLACK
        val kind = when (char.lowercaseChar()) {
            'k' -> PieceKind.GENERAL
            'a' -> PieceKind.ADVISOR
            'b' -> PieceKind.ELEPHANT
            'n' -> PieceKind.HORSE
            'r' -> PieceKind.ROOK
            'c' -> PieceKind.CANNON
            'p' -> PieceKind.SOLDIER
            else -> throw IllegalArgumentException("unknown glyph: $char")
        }
        return Piece(color, kind)
    }
}

/** Convenience mapping used by tests and the engine seam. */
