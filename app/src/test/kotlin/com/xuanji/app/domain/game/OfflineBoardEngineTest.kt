package com.xuanji.app.domain.game

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineBoardEngineTest {

    @Test
    fun offline_engine_returns_only_legal_deterministic_move() = runTest {
        val position = XiangqiBoard.initial()
        val first = OfflineBoardEngine("easy").bestMove(position, PlayerColor.RED, 3L)
        val second = OfflineBoardEngine("easy").bestMove(position, PlayerColor.RED, 3L)
        assertEquals(first, second)
        assertTrue(first is EngineResult.Move)
        val move = (first as EngineResult.Move).turn.move
        assertTrue(XiangqiRules.legalMoves(position, PlayerColor.RED).contains(move))
        // offline play never exposes engine metrics
        assertNull(first.turn.evaluation)
    }

    @Test
    fun offline_engine_is_deterministic_per_difficulty() = runTest {
        val position = XiangqiBoard.initial()
        val easy = OfflineBoardEngine("easy").bestMove(position, PlayerColor.BLACK, 7L)
        val hard = OfflineBoardEngine("hard").bestMove(position, PlayerColor.BLACK, 7L)
        assertEquals(easy, OfflineBoardEngine("easy").bestMove(position, PlayerColor.BLACK, 7L))
        // different difficulties may pick differently, but both must stay legal
        val legal = XiangqiRules.legalMoves(position, PlayerColor.BLACK)
        if (hard is EngineResult.Move) assertTrue(legal.contains(hard.turn.move))
    }

    @Test
    fun offline_engine_reports_no_move_on_finished_position() = runTest {
        val checkmated = XiangqiBoard.empty(PlayerColor.RED)
            .withPiece(Square(3, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(3, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
        // build a terminal position: red to move, already checkmated
        val position = XiangqiBoard.empty(PlayerColor.RED)
            .withPiece(Square(4, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(3, 9), Piece(PlayerColor.RED, PieceKind.HORSE))
            .withPiece(Square(5, 9), Piece(PlayerColor.RED, PieceKind.HORSE))
            .withPiece(Square(4, 8), Piece(PlayerColor.BLACK, PieceKind.ROOK))
            .withPiece(Square(4, 0), Piece(PlayerColor.BLACK, PieceKind.ROOK))
            .withPiece(Square(3, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
        assertEquals(GameOutcome.Checkmate(PlayerColor.BLACK), XiangqiRules.outcome(position))
        val result = OfflineBoardEngine().bestMove(position, PlayerColor.RED, 1L)
        assertTrue(result is EngineResult.NoMove)
    }

    @Test
    fun offline_engine_falls_back_when_asked_to_move_for_opponent() = runTest {
        // engine asked to move while it is the other side's turn still returns a legal move
        // for the requested color computed from the same position (engine owns no turn state).
        val position = XiangqiBoard.initial()
        val result = OfflineBoardEngine().bestMove(position, PlayerColor.BLACK, 2L)
        assertTrue(result is EngineResult.Move)
        val move = (result as EngineResult.Move).turn.move
        assertTrue(XiangqiRules.legalMoves(position, PlayerColor.BLACK).contains(move))
    }
}
