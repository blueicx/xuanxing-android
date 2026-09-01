package com.xuanji.app.domain.game

/**
 * Chinese xiangqi notation (中文纵线记法) conversion.
 *
 * Red side: files 0..8 map to numerals 9..1 (file 0 = 九, file 8 = 一); black side maps
 * files 0..8 to 1..9 (file 0 = 1). Straight moves of rook/cannon/soldier use step counts
 * (进X/退X); sideways moves use 平 + destination file; diagonal pieces (马/相象/仕士)
 * always record the destination file after 进/退. When two identical pieces share a file,
 * the 前/后 prefix replaces the source file numeral.
 */
object XiangqiNotation {

    // index = file 0..8; value = the numeral used in notation (9..1 for Red, 1..9 for Black)
    private val RED_FILE_NUMERAL = listOf(9, 8, 7, 6, 5, 4, 3, 2, 1)
    private val BLACK_FILE_NUMERAL = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
    private val RED_NUMERALS = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")

    private val PIECE_CHARS = mapOf(
        (PieceKind.ROOK to PlayerColor.RED) to "车",
        (PieceKind.ROOK to PlayerColor.BLACK) to "车",
        (PieceKind.HORSE to PlayerColor.RED) to "马",
        (PieceKind.HORSE to PlayerColor.BLACK) to "马",
        (PieceKind.ELEPHANT to PlayerColor.RED) to "相",
        (PieceKind.ELEPHANT to PlayerColor.BLACK) to "象",
        (PieceKind.ADVISOR to PlayerColor.RED) to "仕",
        (PieceKind.ADVISOR to PlayerColor.BLACK) to "士",
        (PieceKind.GENERAL to PlayerColor.RED) to "帅",
        (PieceKind.GENERAL to PlayerColor.BLACK) to "将",
        (PieceKind.CANNON to PlayerColor.RED) to "炮",
        (PieceKind.CANNON to PlayerColor.BLACK) to "砲",
        (PieceKind.SOLDIER to PlayerColor.RED) to "兵",
        (PieceKind.SOLDIER to PlayerColor.BLACK) to "卒"
    )

    private val RED_PIECE_ALIASES = mapOf(
        "车" to PieceKind.ROOK, "俥" to PieceKind.ROOK,
        "马" to PieceKind.HORSE, "傌" to PieceKind.HORSE,
        "相" to PieceKind.ELEPHANT, "象" to PieceKind.ELEPHANT,
        "仕" to PieceKind.ADVISOR, "士" to PieceKind.ADVISOR,
        "帅" to PieceKind.GENERAL, "将" to PieceKind.GENERAL,
        "炮" to PieceKind.CANNON, "砲" to PieceKind.CANNON,
        "兵" to PieceKind.SOLDIER, "卒" to PieceKind.SOLDIER
    )

    private val BLACK_PIECE_ALIASES = mapOf(
        "车" to PieceKind.ROOK, "俥" to PieceKind.ROOK,
        "马" to PieceKind.HORSE, "傌" to PieceKind.HORSE,
        "象" to PieceKind.ELEPHANT, "相" to PieceKind.ELEPHANT,
        "士" to PieceKind.ADVISOR, "仕" to PieceKind.ADVISOR,
        "将" to PieceKind.GENERAL, "帅" to PieceKind.GENERAL,
        "砲" to PieceKind.CANNON, "炮" to PieceKind.CANNON,
        "卒" to PieceKind.SOLDIER, "兵" to PieceKind.SOLDIER
    )

    private val CHINESE_NUMERAL_INDEX: Map<String, Int> =
        RED_NUMERALS.mapIndexed { i, n -> n to (i + 1) }.toMap()

    private val DIGIT_NUMERAL_INDEX: Map<String, Int> =
        ('1'..'9').mapIndexed { i, c -> c.toString() to (i + 1) }.toMap()

    private val DIAGONAL_KINDS = setOf(PieceKind.HORSE, PieceKind.ELEPHANT, PieceKind.ADVISOR)

    // ---- formatting -------------------------------------------------------------

    fun format(move: BoardMove, position: BoardPosition): String {
        val from = move.from ?: return move.notation
        val to = move.to
        val piece = position.pieceAt(from) ?: return move.notation
        val color = piece.color

        val siblings = sameFilePieces(position, piece.kind, color, from.file)
        val prefix = prefixFor(color, from.rank, siblings.map { it.rank })
        val fileNumText = fileNumeral(from.file, color)
        val verb: String
        val valueText: String
        when {
            to.file == from.file -> {
                verb = if (isAdvance(color, to.rank - from.rank)) "进" else "退"
                valueText = numberText(kotlin.math.abs(to.rank - from.rank), color)
            }
            to.rank == from.rank -> {
                verb = "平"
                valueText = fileNumeral(to.file, color)
            }
            piece.kind in DIAGONAL_KINDS -> {
                verb = if (isAdvance(color, to.rank - from.rank)) "进" else "退"
                valueText = fileNumeral(to.file, color)
            }
            else -> {
                // rank change across files for a straight mover cannot happen legally; fall back
                verb = if (isAdvance(color, to.rank - from.rank)) "进" else "退"
                valueText = fileNumeral(to.file, color)
            }
        }
        // with a 前/后 prefix the source file numeral is omitted by convention
        return if (prefix.isNotEmpty()) {
            "$prefix${pieceChar(piece.kind, color)}$verb$valueText"
        } else {
            "${pieceChar(piece.kind, color)}$fileNumText$verb$valueText"
        }
    }

    // ---- parsing ----------------------------------------------------------------

    fun parse(text: String, position: BoardPosition): BoardMove =
        parseOrNull(text, position)
            ?: throw IllegalArgumentException("unparseable xiangqi notation: $text")

    fun parseOrNull(raw: String, position: BoardPosition): BoardMove? {
        val text = normalize(raw)
        if (text.isEmpty() || text.length !in 3..5) return null

        var body = text
        var prefix: String? = null
        if (body.startsWith("前") || body.startsWith("后")) {
            prefix = body.take(1)
            body = body.drop(1)
        }
        if (body.length < 3) return null
        val pieceCharText = body.take(1)
        val tail = body.drop(1)

        val isBlack = tail.any { it in '0'..'9' }
        val color = if (isBlack) PlayerColor.BLACK else PlayerColor.RED
        val kind = (if (isBlack) BLACK_PIECE_ALIASES else RED_PIECE_ALIASES)[pieceCharText] ?: return null

        val from: Square
        val verb: String
        val toNumeral: Int
        if (prefix != null && tail.length == 2) {
            // 前车平六 form: piece + verb + value
            verb = tail.take(1)
            toNumeral = numeralOf(tail.drop(1)) ?: return null
            from = choosePrefixedPiece(position, kind, color, prefix, verb, toNumeral) ?: return null
        } else {
            if (tail.length < 2) return null
            val fromNumeral = numeralOf(tail.take(1)) ?: return null
            verb = tail[1].toString()
            if (verb !in setOf("进", "退", "平")) return null
            toNumeral = numeralOf(tail.drop(2)) ?: return null
            val fromFile = fileOf(fromNumeral, color)
            val onFile = sameFilePieces(position, kind, color, fromFile)
            if (onFile.size != 1) return null
            from = onFile.first()
        }
        if (verb !in setOf("进", "退", "平")) return null
        val to = resolveTarget(from, verb, toNumeral, color, position, kind) ?: return null
        return BoardMove(from, to, raw.trim(), player = color)
    }

    private fun choosePrefixedPiece(
        position: BoardPosition,
        kind: PieceKind,
        color: PlayerColor,
        prefix: String,
        verb: String,
        toNumeral: Int
    ): Square? {
        val grouped = groupedSameFilePieces(position, kind, color)
        val candidates = grouped.mapNotNull { (_, squares) ->
            if (squares.size < 2) return@mapNotNull null
            if (prefix == "前") squares.first() else squares.last()
        }
        return candidates.firstOrNull { from ->
            resolveTarget(from, verb, toNumeral, color, position, kind) != null
        }
    }

    private fun normalize(raw: String): String = raw.trim()
        .replace("。", "")
        .replace("，", "")
        .replace(",", "")
        .replace(" ", "")
        .replace(".", "")

    private fun numeralOf(text: String): Int? =
        CHINESE_NUMERAL_INDEX[text.take(1)] ?: DIGIT_NUMERAL_INDEX[text.take(1)]

    private fun fileOf(numeral: Int, color: PlayerColor): Int =
        if (color == PlayerColor.RED) RED_FILE_NUMERAL.indexOf(numeral)
        else BLACK_FILE_NUMERAL.indexOf(numeral)

    private fun resolveTarget(
        from: Square,
        verb: String,
        numeral: Int,
        color: PlayerColor,
        position: BoardPosition,
        kind: PieceKind
    ): Square? {
        val legal = XiangqiRules.legalMoves(position, color).filter { it.from == from }
        return when (verb) {
            "平" -> {
                val toFile = fileOf(numeral, color)
                legal.firstOrNull { it.to == Square(toFile, from.rank) }?.to
            }
            "进", "退" -> {
                val wantAdvance = verb == "进"
                if (kind in DIAGONAL_KINDS) {
                    // numeral is the destination file; direction disambiguates the target rank
                    val toFile = fileOf(numeral, color)
                    legal.firstOrNull {
                        it.to.file == toFile && isAdvance(color, it.to.rank - from.rank) == wantAdvance
                    }?.to
                } else {
                    val sign = advanceSign(color) * (if (wantAdvance) 1 else -1)
                    legal.firstOrNull { it.to == Square(from.file, from.rank + sign * numeral) }?.to
                }
            }
            else -> null
        }
    }

    /** Human-readable square name for dialogue: 1-based column (file) and rank. */
    fun coordinate(square: Square): String =
        "第${square.file + 1}列第${square.rank + 1}线"

    // ---- UCI --------------------------------------------------------------------

    /** uci uses file letters a..i (file 0 = a) and row digits where Black back rank = '0', Red = '9'. */
    fun toUci(from: Square, to: Square): String =
        "${'a' + from.file}${rowChar(from.rank)}${'a' + to.file}${rowChar(to.rank)}"

    fun fromUci(text: String): Pair<Square, Square> =
        fromUciOrNull(text) ?: throw IllegalArgumentException("unparseable uci move: $text")

    fun fromUciOrNull(text: String): Pair<Square, Square>? {
        if (text.length != 4) return null
        val fromFile = text[0] - 'a'
        val toFile = text[2] - 'a'
        val fromRank = rowRank(text[1]) ?: return null
        val toRank = rowRank(text[3]) ?: return null
        if (fromFile !in 0..8 || toFile !in 0..8) return null
        return Square(fromFile, fromRank) to Square(toFile, toRank)
    }

    private fun rowChar(rank: Int): Char = ('9' - rank)

    private fun rowRank(char: Char): Int? {
        if (char !in '0'..'9') return null
        return '9' - char
    }

    // ---- helpers ----------------------------------------------------------------

    private fun fileNumeral(file: Int, color: PlayerColor): String {
        val numeral = if (color == PlayerColor.RED) RED_FILE_NUMERAL[file] else BLACK_FILE_NUMERAL[file]
        return if (color == PlayerColor.RED) RED_NUMERALS[numeral - 1] else numeral.toString()
    }

    private fun numberText(steps: Int, color: PlayerColor): String =
        if (color == PlayerColor.RED) RED_NUMERALS[steps - 1] else steps.toString()

    private fun pieceChar(kind: PieceKind, color: PlayerColor): String =
        PIECE_CHARS.getValue(kind to color)

    private fun advanceSign(color: PlayerColor): Int = if (color == PlayerColor.RED) -1 else 1

    private fun isAdvance(color: PlayerColor, deltaRank: Int): Boolean =
        (color == PlayerColor.RED && deltaRank < 0) || (color == PlayerColor.BLACK && deltaRank > 0)

    private fun sameFilePieces(
        position: BoardPosition,
        kind: PieceKind,
        color: PlayerColor,
        file: Int
    ): List<Square> {
        val found = mutableListOf<Square>()
        for (rank in 0..9) {
            val piece = position.pieceAt(Square(file, rank))
            if (piece != null && piece.color == color && piece.kind == kind) found += Square(file, rank)
        }
        return found
    }

    /** All same-kind pieces of one color grouped by file; each group sorted enemy-ward first. */
    private fun groupedSameFilePieces(
        position: BoardPosition,
        kind: PieceKind,
        color: PlayerColor
    ): List<Pair<Int, List<Square>>> {
        val byFile = mutableMapOf<Int, List<Square>>()
        for (file in 0..8) {
            val squares = sameFilePieces(position, kind, color, file)
            if (squares.isNotEmpty()) {
                byFile[file] = if (color == PlayerColor.RED) {
                    squares.sortedBy { it.rank }
                } else {
                    squares.sortedByDescending { it.rank }
                }
            }
        }
        return byFile.entries.map { it.key to it.value }
    }

    /**
     * 前 = the piece nearest the enemy (smallest rank for Red, largest for Black).
     * Returns "" when fewer than two identical pieces share the file.
     */
    private fun prefixFor(color: PlayerColor, rank: Int, siblingRanks: List<Int>): String {
        if (siblingRanks.size < 2) return ""
        val isFront = if (color == PlayerColor.RED) {
            siblingRanks.filter { it != rank }.all { it > rank }
        } else {
            siblingRanks.filter { it != rank }.all { it < rank }
        }
        return if (isFront) "前" else "后"
    }
}
