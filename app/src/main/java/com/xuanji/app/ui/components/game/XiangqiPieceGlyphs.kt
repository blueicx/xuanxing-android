package com.xuanji.app.ui.components.game

import com.xuanji.app.domain.game.Piece
import com.xuanji.app.domain.game.PieceKind
import com.xuanji.app.domain.game.PlayerColor

/**
 * Traditional Chinese characters for xiangqi pieces, kept separate from board layout so
 * the board never owns rule decisions and the glyphs stay theme-able.
 */
object XiangqiPieceGlyphs {

    fun glyph(piece: Piece): String = when (piece.kind) {
        PieceKind.GENERAL -> if (piece.color == PlayerColor.RED) "帥" else "將"
        PieceKind.ADVISOR -> if (piece.color == PlayerColor.RED) "仕" else "士"
        PieceKind.ELEPHANT -> if (piece.color == PlayerColor.RED) "相" else "象"
        PieceKind.HORSE -> if (piece.color == PlayerColor.RED) "傌" else "馬"
        PieceKind.ROOK -> if (piece.color == PlayerColor.RED) "俥" else "車"
        PieceKind.CANNON -> if (piece.color == PlayerColor.RED) "炮" else "砲"
        PieceKind.SOLDIER -> if (piece.color == PlayerColor.RED) "兵" else "卒"
    }

    /** Content description for TalkBack, e.g. "红方 车". */
    fun description(piece: Piece): String =
        (if (piece.color == PlayerColor.RED) "红方" else "黑方") + " " + glyph(piece)
}
