package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XiangqiBoardTest {

    @Test
    fun initial_position_has_32_pieces() {
        assertEquals(32, XiangqiBoard.initial().pieces.count { it != null })
    }

    @Test
    fun initial_position_starts_with_red_and_encodes_stably() {
        val position = XiangqiBoard.initial()
        assertEquals(PlayerColor.RED, position.sideToMove)
        assertEquals(
            "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR r",
            XiangqiBoard.encode(position)
        )
        assertEquals(position, XiangqiBoard.decode(XiangqiBoard.encode(position)))
        assertEquals(XiangqiBoard.encode(position), XiangqiBoard.encode(XiangqiBoard.initial()))
    }

    @Test
    fun piece_lookup_reads_file_and_rank() {
        val position = XiangqiBoard.initial()
        assertEquals(PieceKind.GENERAL, position.pieceAt(Square(4, 9))?.kind)
        assertEquals(PlayerColor.BLACK, position.pieceAt(Square(4, 0))?.color)
        assertEquals(PieceKind.SOLDIER, position.pieceAt(Square(0, 3))?.kind)
        assertEquals(PieceKind.ROOK, position.pieceAt(Square(8, 0))?.kind)
        assertEquals(null, position.pieceAt(Square(4, 5)))
    }

    @Test
    fun repetition_count_tracks_identical_final_positions() {
        assertEquals(3, XiangqiBoard.repetitionCount(listOf("a", "b", "a", "a")))
        assertEquals(1, XiangqiBoard.repetitionCount(listOf("x", "y")))
        assertEquals(0, XiangqiBoard.repetitionCount(emptyList()))
    }

    @Test
    fun position_immutability_keeps_original_untouched() {
        val position = XiangqiBoard.initial()
        val next = position.withPiece(Square(4, 5), Piece(PlayerColor.RED, PieceKind.CANNON))
        assertEquals(null, position.pieceAt(Square(4, 5)))
        assertEquals(PieceKind.CANNON, next.pieceAt(Square(4, 5))?.kind)
        assertTrue(next != position)
    }
}
