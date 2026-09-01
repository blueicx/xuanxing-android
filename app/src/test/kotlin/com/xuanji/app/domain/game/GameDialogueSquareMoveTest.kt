package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Grid tap-to-move path: squares come from the UI, rules stay the single gatekeeper. */
class GameDialogueSquareMoveTest {

    private val bridge = GameDialogueBridge()

    private fun started(): GameSessionState =
        bridge.handle(GameSessionState(), "来一盘象棋").state

    @Test
    fun square_move_applies_real_move_with_formatted_notation() {
        val state = started()
        val result = bridge.applySquareMove(state, from = Square(7, 7), to = Square(4, 7))
        assertTrue(result.event is GameEvent.ApplyMove)
        assertTrue(result.reply.contains("炮二平五"))
        assertEquals(PlayerColor.BLACK, result.state.position.sideToMove)
        assertEquals(1, result.state.history.size)
    }

    @Test
    fun square_move_rejects_illegal_target_without_touching_state() {
        val state = started()
        // red general cannot move like a rook
        val result = bridge.applySquareMove(state, from = Square(4, 9), to = Square(4, 5))
        assertNull(result.event)
        assertEquals(state, result.state)
        assertTrue(result.reply.contains("不合法") || result.reply.contains("不能走"))
    }

    @Test
    fun square_move_rejects_empty_source() {
        val state = started()
        val result = bridge.applySquareMove(state, from = Square(4, 5), to = Square(4, 6))
        assertNull(result.event)
        assertEquals(state, result.state)
        assertTrue(result.reply.contains("没有棋子"))
    }

    @Test
    fun square_move_respects_turn_order() {
        val state = started()
        // tapping a black piece on red's turn is refused with the real mover named
        val result = bridge.applySquareMove(state, from = Square(0, 0), to = Square(0, 1))
        assertNull(result.event)
        assertTrue(result.reply.contains("红方"))
    }

    @Test
    fun same_file_two_rooks_format_without_front_rear_when_file_differs() {
        // custom position with two red rooks on file 0 (front rank 2, rear rank 5)
        val position = XiangqiBoard.empty()
            .withPiece(Square(3, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(5, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
            .withPiece(Square(0, 5), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(0, 2), Piece(PlayerColor.RED, PieceKind.ROOK))
        val state = GameSessionState(sessionToken = 3L, position = position)
        // tapping the front rook (rank 2) sideways to file 3 must be the FRONT rook's move
        val result = bridge.applySquareMove(state, from = Square(0, 2), to = Square(3, 2))
        assertTrue(result.reply.contains("前车平六"))
    }
}
