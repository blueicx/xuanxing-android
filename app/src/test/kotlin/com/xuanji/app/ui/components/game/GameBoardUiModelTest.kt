package com.xuanji.app.ui.components.game

import com.xuanji.app.domain.game.Piece
import com.xuanji.app.domain.game.PieceKind
import com.xuanji.app.domain.game.PlayerColor
import com.xuanji.app.domain.game.Square
import com.xuanji.app.domain.game.XiangqiBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure Kotlin mapping tests: the board renders exactly what this model exposes. */
class GameBoardUiModelTest {

    @Test
    fun from_initial_position_exposes_32_pieces_and_no_targets() {
        val model = GameBoardUiModel.from(XiangqiBoard.initial(), null)
        assertEquals(32, model.pieces.size)
        assertTrue(model.legalTargets.isEmpty())
        assertNull(model.outcomeText)
        assertEquals(PlayerColor.RED, model.sideToMove)
    }

    @Test
    fun selected_own_piece_exposes_exactly_its_legal_targets() {
        val position = XiangqiBoard.initial()
        val model = GameBoardUiModel.from(position, Square(1, 9)) // red horse on file 1
        assertEquals(2, model.legalTargets.size)
        assertTrue(Square(2, 7) in model.legalTargets)
        assertTrue(Square(0, 7) in model.legalTargets)
        // selecting an enemy piece yields no targets (not your side)
        val enemy = GameBoardUiModel.from(position, Square(1, 2))
        assertTrue(enemy.legalTargets.isEmpty())
    }

    @Test
    fun checkmate_position_maps_to_terminal_text() {
        val position = XiangqiBoard.empty(PlayerColor.RED)
            .withPiece(Square(3, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
            .withPiece(Square(5, 0), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(4, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(4, 1), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(2, 1), Piece(PlayerColor.RED, PieceKind.SOLDIER))
        val checked = GameBoardUiModel.from(position, null)
        assertNull(checked.outcomeText) // before mating move: in progress (red to move)
        val after = position.withPiece(Square(4, 1), null).withPiece(
            Square(4, 0),
            Piece(PlayerColor.RED, PieceKind.ROOK)
        ).copy(sideToMove = PlayerColor.BLACK)
        val mated = GameBoardUiModel.from(after, null)
        assertEquals("绝杀，红方胜", mated.outcomeText)
    }

    @Test
    fun empty_squares_map_to_null_pieces() {
        val model = GameBoardUiModel.from(XiangqiBoard.initial(), null)
        assertNull(model.pieces[Square(4, 5)])
        assertFalse(model.pieces.containsKey(Square(4, 4)))
    }
}
