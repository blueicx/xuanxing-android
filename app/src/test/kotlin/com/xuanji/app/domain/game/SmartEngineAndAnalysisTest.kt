package com.xuanji.app.domain.game

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartBoardEngineTest {

    @Test
    fun smart_engine_is_deterministic_per_difficulty() = runBlocking {
        val position = XiangqiBoard.initial()
        val first = SmartBoardEngine("hard").bestMove(position, PlayerColor.RED, 7L)
        val second = SmartBoardEngine("hard").bestMove(position, PlayerColor.RED, 7L)
        assertEquals(first, second)
    }

    @Test
    fun opening_book_plays_central_cannon_on_start_position() = runBlocking {
        val result = SmartBoardEngine("easy").bestMove(XiangqiBoard.initial(), PlayerColor.RED, 1L)
        val move = (result as EngineResult.Move).turn.move
        assertEquals(Square(4, 7), move.to) // 炮二平五
    }

    @Test
    fun smart_engine_moves_are_always_legal() = runBlocking {
        val position = XiangqiBoard.initial()
        val result = SmartBoardEngine("normal").bestMove(position, PlayerColor.BLACK, 3L)
        val move = (result as EngineResult.Move).turn.move
        assertTrue(XiangqiRules.legalMoves(position, PlayerColor.BLACK).contains(move))
    }

    @Test
    fun smart_engine_captures_free_hanging_piece() = runBlocking {
        // black rook hangs on (0,5), attacked by red rook on the same file with a clear path
        val position = XiangqiBoard.empty(PlayerColor.RED)
            .withPiece(Square(4, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(3, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
            .withPiece(Square(0, 1), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(0, 5), Piece(PlayerColor.BLACK, PieceKind.ROOK))
        val result = SmartBoardEngine("normal").bestMove(position, PlayerColor.RED, 1L)
        val move = (result as EngineResult.Move).turn.move
        // the engine must capture the hanging rook (material is dominant in evaluation)
        assertEquals(Square(0, 5), move.to)
    }

    @Test
    fun smart_engine_finds_mate_in_one_at_hard_depth() = runBlocking {
        val position = XiangqiBoard.empty(PlayerColor.RED)
            .withPiece(Square(3, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
            .withPiece(Square(5, 0), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(4, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(4, 1), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(2, 1), Piece(PlayerColor.RED, PieceKind.SOLDIER))
        // verify the mate-in-1 exists through the rules module first
        val mateMove = BoardMove(Square(4, 1), Square(4, 0), "", player = PlayerColor.RED)
        val applied = XiangqiRules.apply(position, mateMove) as RuleResult.Applied
        assertEquals(GameOutcome.Checkmate(PlayerColor.RED), XiangqiRules.outcome(applied.position))
        // engine must deliver mate or stalemate (both end the game immediately, red wins)
        val result = SmartBoardEngine("hard").bestMove(position, PlayerColor.RED, 1L)
        val move = (result as EngineResult.Move).turn.move
        val after = XiangqiRules.apply(position, move) as RuleResult.Applied
        val outcome = XiangqiRules.outcome(after.position)
        assertTrue(
            "engine move $move must end the game immediately; got $outcome",
            outcome is GameOutcome.Checkmate || outcome is GameOutcome.Stalemate
        )
    }
}

class BoardAnalysisTest {

    @Test
    fun hanging_rook_is_reported_as_threat() {
        val position = XiangqiBoard.empty(PlayerColor.RED)
            .withPiece(Square(4, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(3, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
            .withPiece(Square(0, 5), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(8, 2), Piece(PlayerColor.BLACK, PieceKind.HORSE))
        // red rook steps into the black horse's jump range at (7,4)
        val hung = position.withPiece(Square(7, 4), Piece(PlayerColor.RED, PieceKind.ROOK))
        val threats = BoardAnalysis.threatsAgainst(hung, PlayerColor.RED)
        assertTrue(
            "expected the rook at (7,4) to be threatened by the horse at (8,2); got: $threats",
            threats.any { it.attacked == Square(7, 4) && it.attacker == Square(8, 2) }
        )
    }

    @Test
    fun opening_shows_real_cannon_threat_on_red_horse() {
        // In the opening the black cannon (7,2) strikes the red horse (7,9) over the
        // pawn screen at (7,6) — a genuine, rule-derived threat that must be reported.
        val threats = BoardAnalysis.threatsAgainst(XiangqiBoard.initial(), PlayerColor.RED)
        assertTrue(
            "expected the (7,2)->(7,9) cannon-vs-horse threat; got: $threats",
            threats.any { it.attacked == Square(7, 9) && it.attacker == Square(7, 2) }
        )
    }
}

class EndgameCatalogTest {

    @Test
    fun all_puzzles_are_legal_positions_with_expected_side() {
        EndgameCatalog.ALL.forEach { puzzle ->
            val position = puzzle.position()
            assertEquals(puzzle.solver, position.sideToMove)
            assertTrue(
                "puzzle ${puzzle.id} has no legal moves",
                XiangqiRules.legalMoves(position, puzzle.solver).isNotEmpty()
            )
        }
    }

    @Test
    fun mating_rook_puzzle_reaches_checkmate_when_engine_plays_both_sides() = runBlocking {
        val puzzle = EndgameCatalog.byId("mating_rook")
        assertNotNull(puzzle)
        var position = puzzle!!.position()
        var plies = 0
        while (plies < 40) {
            val outcome = XiangqiRules.outcome(position)
            if (outcome is GameOutcome.Checkmate && outcome.winner == PlayerColor.RED) {
                return@runBlocking
            }
            if (outcome is GameOutcome.Stalemate && outcome.winner == PlayerColor.RED) {
                // stalemate is a WIN for red in xiangqi: puzzle solved either way
                return@runBlocking
            }
            if (outcome !is GameOutcome.InProgress && outcome !is GameOutcome.Check) {
                throw AssertionError("puzzle ended without red win at ply $plies: $outcome")
            }
            val side = position.sideToMove
            // red (rook side) searches deep; the lone black general plays shallow
            val engine = if (side == PlayerColor.RED) SmartBoardEngine("hard") else SmartBoardEngine("easy")
            val result = engine.bestMove(position, side, plies.toLong())
            if (result !is EngineResult.Move) {
                throw AssertionError("engine returned $result at ply $plies; outcome=${XiangqiRules.outcome(position)}")
            }
            val applied = XiangqiRules.apply(position, result.turn.move)
            if (applied !is RuleResult.Applied) {
                throw AssertionError(
                    "engine move ${result.turn.move} rejected at ply $plies (${applied}); " +
                        "outcome before move=${XiangqiRules.outcome(position)}"
                )
            }
            position = applied.position
            plies++
        }
        throw AssertionError("mating_rook puzzle not solved within 40 plies; last outcome=${XiangqiRules.outcome(position)}")
    }
}
