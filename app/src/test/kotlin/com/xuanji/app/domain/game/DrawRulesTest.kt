package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawRulesTest {

    private fun started(): GameSessionState =
        reduceGame(GameSessionState(sessionToken = 8L), GameEvent.Start(GameType.XIANGQI, token = 9L))

    @Test
    fun sixty_quiet_halfmoves_draw_the_game() {
        // Sparse position so 60 legal quiet halfmoves genuinely exist: two free horses
        // shuffling on open files with no captures available.
        val sparse = XiangqiBoard.empty(PlayerColor.RED)
            .withPiece(Square(3, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(5, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
            .withPiece(Square(0, 4), Piece(PlayerColor.RED, PieceKind.HORSE))
            .withPiece(Square(8, 5), Piece(PlayerColor.BLACK, PieceKind.HORSE))
        var state = GameSessionState(sessionToken = 1L, position = sparse)
        val redCycle = listOf(
            Square(0, 4) to Square(1, 6),
            Square(1, 6) to Square(0, 4)
        )
        val blackCycle = listOf(
            Square(8, 5) to Square(7, 3),
            Square(7, 3) to Square(8, 5)
        )
        var appliedPairs = 0
        for (i in 0 until 30) {
            if (state.outcome is GameOutcome.Draw) break
            val (fromR, toR) = redCycle[i % 2]
            val red = XiangqiRules.apply(state.position, BoardMove(fromR, toR, "", player = PlayerColor.RED))
            if (red is RuleResult.Applied) {
                state = reduceGame(state, GameEvent.ApplyMove(state.sessionToken, red.move))
            } else {
                throw AssertionError("red shuffle rejected at pair $appliedPairs: ${red}")
            }
            if (state.outcome is GameOutcome.Draw) break
            val (fromB, toB) = blackCycle[i % 2]
            val black = XiangqiRules.apply(state.position, BoardMove(fromB, toB, "", player = PlayerColor.BLACK))
            if (black is RuleResult.Applied) {
                state = reduceGame(state, GameEvent.EngineReply(state.sessionToken, EngineTurn(black.move)))
                appliedPairs++
            } else {
                throw AssertionError("black shuffle rejected at pair $appliedPairs: ${black}")
            }
        }
        assertTrue(
            "expected Draw after 60 quiet halfmoves; appliedPairs=$appliedPairs outcome=${state.outcome}",
            state.outcome is GameOutcome.Draw
        )
    }

    @Test
    fun draw_game_rejects_further_moves() {
        val drawn = GameSessionState(sessionToken = 1L, outcome = GameOutcome.Draw)
        val after = reduceGame(drawn, GameEvent.ApplyMove(1L, BoardMove(Square(4, 9), Square(4, 8), "", player = PlayerColor.RED)))
        assertEquals(drawn, after)
    }
}
