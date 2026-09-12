package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure rule tests. Coordinate convention: file = 0..8 (left to right from Black's view),
 * rank = 0..9 with rank 0 the Black back rank and rank 9 the Red back rank.
 * Red advances by decreasing rank; Black advances by increasing rank.
 */
class XiangqiRulesTest {

    private fun board(
        placements: List<Triple<Square, PlayerColor, PieceKind>>,
        sideToMove: PlayerColor = PlayerColor.RED
    ): BoardPosition {
        var position = BoardPosition.empty(sideToMove)
        placements.forEach { (square, color, kind) ->
            position = position.withPiece(square, Piece(color, kind))
        }
        return position
    }

    private fun at(pos: Pair<Int, Int>, color: PlayerColor, kind: PieceKind) =
        Triple(Square(pos.first, pos.second), color, kind)

    // ---- movement generation --------------------------------------------------

    @Test
    fun initial_red_has_42_legal_moves() {
        // manually verified for this rules implementation: 5 soldiers + 4 rooks + 4 horses
        // + 4 elephants + 2 advisors + 1 general + 24 cannon moves = 44
        assertEquals(44, XiangqiRules.legalMoves(XiangqiBoard.initial(), PlayerColor.RED).size)
    }

    @Test
    fun soldier_moves_forward_before_river_and_sideways_after() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(0 to 6, PlayerColor.RED, PieceKind.SOLDIER), // before river
                at(0 to 4, PlayerColor.RED, PieceKind.SOLDIER) // crossed river
            )
        )
        val legal = XiangqiRules.legalMoves(position, PlayerColor.RED)
        val from = legal.filter { it.from == Square(0, 6) }
        assertTrue(from.any { it.to == Square(0, 5) })
        assertTrue(from.none { it.to == Square(1, 6) }) // no sideways before river
        val crossed = legal.filter { it.from == Square(0, 4) }
        assertTrue(crossed.any { it.to == Square(0, 3) })
        assertTrue(crossed.any { it.to == Square(1, 4) })
    }

    @Test
    fun horse_cannot_jump_over_blocking_leg() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(1 to 2, PlayerColor.RED, PieceKind.HORSE),
                at(2 to 2, PlayerColor.RED, PieceKind.SOLDIER) // leg toward (3,1)/(3,3)
            )
        )
        val legal = XiangqiRules.legalMoves(position, PlayerColor.RED)
        assertTrue(legal.none { it.from == Square(1, 2) && it.to == Square(3, 3) })
        assertTrue(legal.none { it.from == Square(1, 2) && it.to == Square(3, 1) })
        assertTrue(legal.any { it.from == Square(1, 2) && it.to == Square(2, 4) }) // leg (1,4) clear
    }

    @Test
    fun cannon_requires_exactly_one_screen_to_capture() {
        val oneScreen = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(4 to 5, PlayerColor.RED, PieceKind.CANNON),
                at(4 to 6, PlayerColor.BLACK, PieceKind.SOLDIER), // the single screen
                at(4 to 8, PlayerColor.BLACK, PieceKind.SOLDIER) // capture target
            )
        )
        val twoScreens = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(4 to 5, PlayerColor.RED, PieceKind.CANNON),
                at(4 to 6, PlayerColor.BLACK, PieceKind.SOLDIER),
                at(4 to 7, PlayerColor.BLACK, PieceKind.SOLDIER), // second screen
                at(4 to 8, PlayerColor.BLACK, PieceKind.SOLDIER)
            )
        )
        val legalOne = XiangqiRules.legalMoves(oneScreen, PlayerColor.RED)
        val legalTwo = XiangqiRules.legalMoves(twoScreens, PlayerColor.RED)
        assertTrue(legalOne.any { it.from == Square(4, 5) && it.to == Square(4, 8) })
        assertTrue(legalTwo.none { it.from == Square(4, 5) && it.to == Square(4, 8) })
    }

    @Test
    fun cannon_moves_like_rook_when_not_capturing() {
        val position = board(
            listOf(
                at(3 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(5 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(4 to 5, PlayerColor.RED, PieceKind.CANNON),
                at(4 to 8, PlayerColor.BLACK, PieceKind.SOLDIER)
            )
        )
        val legal = XiangqiRules.legalMoves(position, PlayerColor.RED)
        assertTrue(legal.any { it.from == Square(4, 5) && it.to == Square(4, 6) })
        assertTrue(legal.any { it.from == Square(4, 5) && it.to == Square(4, 7) })
        // zero screens between cannon and soldier: capture is illegal
        assertTrue(legal.none { it.from == Square(4, 5) && it.to == Square(4, 8) })
    }

    @Test
    fun elephant_cannot_cross_river() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(2 to 5, PlayerColor.RED, PieceKind.ELEPHANT)
            )
        )
        val legal = XiangqiRules.legalMoves(position, PlayerColor.RED)
        assertTrue(legal.none { it.to.rank < 5 })
        assertTrue(legal.any { it.from == Square(2, 5) && it.to == Square(0, 7) })
        assertTrue(legal.any { it.from == Square(2, 5) && it.to == Square(4, 7) })
    }

    @Test
    fun elephant_moves_exactly_two_diagonal_with_open_eye() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(2 to 7, PlayerColor.RED, PieceKind.ELEPHANT),
                at(1 to 8, PlayerColor.RED, PieceKind.SOLDIER) // eye toward (0,9)
            )
        )
        val legal = XiangqiRules.legalMoves(position, PlayerColor.RED)
        assertTrue(legal.none { it.from == Square(2, 7) && it.to == Square(0, 9) })
        assertTrue(legal.any { it.from == Square(2, 7) && it.to == Square(0, 5) })
        assertTrue(legal.any { it.from == Square(2, 7) && it.to == Square(4, 5) })
    }

    @Test
    fun advisor_and_general_confined_to_palace() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 8, PlayerColor.RED, PieceKind.ADVISOR),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL)
            )
        )
        val legal = XiangqiRules.legalMoves(position, PlayerColor.RED)
        assertTrue(legal.any { it.from == Square(3, 8) && it.to == Square(4, 7) })
        assertTrue(legal.none { it.from == Square(3, 8) && it.to == Square(2, 7) }) // outside palace
        assertTrue(legal.any { it.from == Square(4, 9) && it.to == Square(3, 9) })
        assertTrue(legal.any { it.from == Square(4, 9) && it.to == Square(5, 9) })
        assertTrue(legal.any { it.from == Square(4, 9) && it.to == Square(4, 8) })
        assertTrue(legal.none { it.from == Square(4, 9) && it.to == Square(5, 8) }) // not orthogonal
    }

    @Test
    fun rook_blocked_by_any_interposing_piece() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(8 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(0 to 0, PlayerColor.RED, PieceKind.ROOK),
                at(0 to 5, PlayerColor.BLACK, PieceKind.SOLDIER)
            )
        )
        val legal = XiangqiRules.legalMoves(position, PlayerColor.RED)
        assertTrue(legal.any { it.from == Square(0, 0) && it.to == Square(0, 5) }) // capture
        assertTrue(legal.none { it.from == Square(0, 0) && it.to == Square(0, 6) }) // beyond blocker
    }

    @Test
    fun black_pieces_advance_in_opposite_direction() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(0 to 6, PlayerColor.BLACK, PieceKind.SOLDIER) // crossed river
            ),
            sideToMove = PlayerColor.BLACK
        )
        val legal = XiangqiRules.legalMoves(position, PlayerColor.BLACK)
        assertTrue(legal.any { it.from == Square(0, 6) && it.to == Square(0, 7) })
        assertTrue(legal.none { it.from == Square(0, 6) && it.to == Square(0, 5) })
    }

    @Test
    fun cannot_capture_own_piece() {
        val initial = XiangqiBoard.initial()
        val legal = XiangqiRules.legalMoves(initial, PlayerColor.RED)
        assertTrue(legal.none { initial.pieceAt(it.to)?.color == PlayerColor.RED })
    }

    // ---- apply / reject -------------------------------------------------------

    @Test
    fun apply_rejects_empty_source_and_wrong_turn() {
        val position = XiangqiBoard.initial()
        val emptyFrom = XiangqiRules.apply(position, BoardMove(Square(4, 5), Square(4, 6), "炮五进一"))
        assertEquals("from_empty", (emptyFrom as RuleResult.Rejected).code)

        val blackMove = XiangqiRules.apply(
            position,
            BoardMove(Square(0, 0), Square(0, 1), "车1进1", player = PlayerColor.BLACK)
        )
        assertEquals("wrong_turn", (blackMove as RuleResult.Rejected).code)
    }

    @Test
    fun apply_legal_move_updates_side_to_move_and_position() {
        val position = XiangqiBoard.initial()
        val horseMove = BoardMove(Square(1, 9), Square(2, 7), "马八进七")
        val applied = XiangqiRules.apply(position, horseMove) as RuleResult.Applied
        assertEquals(PlayerColor.BLACK, applied.position.sideToMove)
        assertEquals(null, applied.position.pieceAt(Square(1, 9)))
        assertEquals(PieceKind.HORSE, applied.position.pieceAt(Square(2, 7))?.kind)
    }

    @Test
    fun exposing_general_to_rook_is_rejected_as_self_check() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(4 to 2, PlayerColor.BLACK, PieceKind.ROOK),
                at(4 to 5, PlayerColor.RED, PieceKind.ROOK) // only blocker on file 4
            )
        )
        val result = XiangqiRules.apply(position, BoardMove(Square(4, 5), Square(0, 5), "车五平一"))
        assertEquals("self_check", (result as RuleResult.Rejected).code)
        val stayOnFile = XiangqiRules.apply(position, BoardMove(Square(4, 5), Square(4, 6), "车五进一"))
        assertTrue(stayOnFile is RuleResult.Applied)
    }

    @Test
    fun moving_after_game_over_is_rejected() {
        val checkmated = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 9, PlayerColor.RED, PieceKind.HORSE),
                at(5 to 9, PlayerColor.RED, PieceKind.HORSE),
                at(4 to 8, PlayerColor.BLACK, PieceKind.ROOK),
                at(4 to 0, PlayerColor.BLACK, PieceKind.ROOK),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL)
            ),
            sideToMove = PlayerColor.RED
        )
        assertEquals(GameOutcome.Checkmate(PlayerColor.BLACK), XiangqiRules.outcome(checkmated))
        val anyMove = BoardMove(Square(4, 9), Square(4, 8), "帅五进一")
        assertEquals("game_over", (XiangqiRules.apply(checkmated, anyMove) as RuleResult.Rejected).code)
    }

    // ---- check / checkmate / stalemate ----------------------------------------

    @Test
    fun generals_cannot_face_each_other() {
        val facing = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(4 to 0, PlayerColor.BLACK, PieceKind.GENERAL)
            )
        )
        assertTrue(XiangqiRules.outcome(facing).isIllegalPosition)
    }

    @Test
    fun simple_check_is_detected() {
        val check = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(4 to 2, PlayerColor.BLACK, PieceKind.ROOK)
            )
        )
        assertEquals(GameOutcome.Check(PlayerColor.BLACK), XiangqiRules.outcome(check))
    }

    @Test
    fun checkmate_detected_when_no_legal_reply() {
        val checkmated = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 9, PlayerColor.RED, PieceKind.HORSE),
                at(5 to 9, PlayerColor.RED, PieceKind.HORSE),
                at(4 to 8, PlayerColor.BLACK, PieceKind.ROOK),
                at(4 to 0, PlayerColor.BLACK, PieceKind.ROOK),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL)
            ),
            sideToMove = PlayerColor.RED
        )
        assertEquals(GameOutcome.Checkmate(PlayerColor.BLACK), XiangqiRules.outcome(checkmated))
    }

    @Test
    fun stalemate_is_loss_for_stalemated_side() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(2 to 1, PlayerColor.RED, PieceKind.SOLDIER), // controls (2,0)
                at(4 to 1, PlayerColor.RED, PieceKind.SOLDIER), // controls (4,0)
                at(3 to 2, PlayerColor.RED, PieceKind.SOLDIER), // controls (3,1)
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL)
            ),
            sideToMove = PlayerColor.BLACK
        )
        assertEquals(GameOutcome.Stalemate(PlayerColor.RED), XiangqiRules.outcome(position))
    }

    @Test
    fun normal_position_is_in_progress_and_not_illegal() {
        val position = XiangqiBoard.initial()
        assertEquals(GameOutcome.InProgress, XiangqiRules.outcome(position))
        assertFalse(XiangqiRules.outcome(position).isIllegalPosition)
    }

    @Test
    fun pinned_rook_cannot_leave_the_file() {
        val position = board(
            listOf(
                at(4 to 9, PlayerColor.RED, PieceKind.GENERAL),
                at(3 to 0, PlayerColor.BLACK, PieceKind.GENERAL),
                at(4 to 3, PlayerColor.BLACK, PieceKind.ROOK),
                at(4 to 8, PlayerColor.RED, PieceKind.ROOK)
            )
        )
        val legal = XiangqiRules.legalMoves(position, PlayerColor.RED)
        assertTrue(legal.none { it.from == Square(4, 8) && it.to == Square(0, 8) })
        assertTrue(legal.none { it.from == Square(4, 8) && it.to == Square(5, 8) })
        assertTrue(legal.any { it.from == Square(4, 8) && it.to == Square(4, 5) })
        assertTrue(legal.any { it.from == Square(4, 8) && it.to == Square(4, 3) }) // capture the pinning rook
    }
}
