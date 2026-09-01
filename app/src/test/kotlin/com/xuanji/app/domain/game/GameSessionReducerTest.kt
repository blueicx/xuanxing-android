package com.xuanji.app.domain.game

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionReducerTest {

    private fun redHorseForward() = BoardMove(Square(1, 9), Square(2, 7), "马八进七", player = PlayerColor.RED)

    private fun started(token: Long = 9L) = reduceGame(GameSessionState(sessionToken = 8L), GameEvent.Start(GameType.XIANGQI, token))

    private fun afterTwoMoves(): GameSessionState {
        val started = started()
        val applied = reduceGame(started, GameEvent.ApplyMove(started.sessionToken, redHorseForward()))
        // simulate engine reply by applying a black move directly
        val blackReply = XiangqiRules.legalMoves(applied.position, PlayerColor.BLACK).first()
        return reduceGame(applied, GameEvent.ApplyMove(applied.sessionToken, blackReply.copy(player = PlayerColor.BLACK)))
    }

    @Test
    fun start_resets_to_initial_position_with_new_token() {
        val stale = GameSessionState(sessionToken = 8L, history = listOf(redHorseForward()))
        val started = reduceGame(stale, GameEvent.Start(GameType.XIANGQI, token = 9L))
        assertEquals(9L, started.sessionToken)
        assertEquals(XiangqiBoard.initial(), started.position)
        assertTrue(started.history.isEmpty())
        assertTrue(started.redo.isEmpty())
        assertEquals(GameOutcome.InProgress, started.outcome)
    }

    @Test
    fun context_change_drops_old_engine_reply() {
        val changed = started(9L)
        val stale = reduceGame(changed, GameEvent.EngineReply(8L, EngineTurn(redHorseForward())))
        assertEquals(changed, stale)
    }

    @Test
    fun apply_move_requires_matching_token_and_updates_history() {
        val state = started(9L)
        val rejected = reduceGame(state, GameEvent.ApplyMove(4L, redHorseForward()))
        assertEquals(state, rejected)
        val applied = reduceGame(state, GameEvent.ApplyMove(9L, redHorseForward()))
        assertEquals(1, applied.history.size)
        assertEquals(PlayerColor.BLACK, applied.position.sideToMove)
    }

    @Test
    fun undo_restores_previous_position_without_losing_redo() {
        val state = afterTwoMoves()
        assertEquals(2, state.history.size)
        // one undo rewinds a full round: both the player's move and the engine reply
        val undone = reduceGame(state, GameEvent.Undo(state.sessionToken))
        assertEquals(0, undone.history.size)
        assertEquals(2, undone.redo.size)
        assertEquals(PlayerColor.RED, undone.position.sideToMove)
        assertEquals(XiangqiBoard.initial(), undone.position)
    }

    @Test
    fun undo_on_empty_history_is_noop() {
        val state = started(9L)
        assertEquals(state, reduceGame(state, GameEvent.Undo(state.sessionToken)))
    }

    @Test
    fun cancel_clears_pending_request_only_for_matching_token() {
        val state = started(9L).let { s ->
            s.copy(request = GameRequest.Thinking(s.sessionToken, "hint"))
        }
        val mismatched = reduceGame(state, GameEvent.Cancel(4L))
        assertTrue(mismatched.request is GameRequest.Thinking)
        val cancelled = reduceGame(state, GameEvent.Cancel(9L))
        assertEquals(GameRequest.Idle, cancelled.request)
    }

    @Test
    fun exit_resets_to_fresh_state() {
        val state = afterTwoMoves()
        val exited = reduceGame(state, GameEvent.Exit)
        assertEquals(GameSessionState(), exited)
        assertTrue(exited.history.isEmpty())
        assertNull(exited.request as? GameRequest.Thinking)
    }

    @Test
    fun illegal_move_keeps_position_unchanged() {
        val state = started(9L)
        val illegal = BoardMove(Square(4, 5), Square(4, 6), "炮五进一", player = PlayerColor.RED)
        val next = reduceGame(state, GameEvent.ApplyMove(state.sessionToken, illegal))
        assertEquals(state, next)
    }

    @Test
    fun outcome_updates_after_checkmate_move() {
        // black general (3,0); red rook (4,1) will deliver the check by dropping to (4,0);
        // rook (5,0) seals rank 0 to the right, soldier (2,1) seals (2,0) to the left.
        val position = XiangqiBoard.empty(PlayerColor.RED)
            .withPiece(Square(3, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
            .withPiece(Square(5, 0), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(4, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(4, 1), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(2, 1), Piece(PlayerColor.RED, PieceKind.SOLDIER))
        val mateMove = BoardMove(Square(4, 1), Square(4, 0), "车五进一", player = PlayerColor.RED)
        val applied = XiangqiRules.apply(position, mateMove)
        assertTrue(applied is RuleResult.Applied)
        val next = (applied as RuleResult.Applied).position
        assertEquals(GameOutcome.Checkmate(PlayerColor.RED), XiangqiRules.outcome(next))
        // reducer reflects terminal outcome
        val state = GameSessionState(sessionToken = 1L, position = position)
        val moved = reduceGame(state, GameEvent.ApplyMove(1L, mateMove))
        assertEquals(GameOutcome.Checkmate(PlayerColor.RED), moved.outcome)
    }
}
