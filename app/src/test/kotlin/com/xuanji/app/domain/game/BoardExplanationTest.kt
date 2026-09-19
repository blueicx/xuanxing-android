package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every board premise here is built square by square and then re-asserted, because the
 * wording the dialogue layer builds on top of these lists reads as a fact about the game:
 * "no recapture" has to mean the rules really found none.
 */
class BoardExplanationTest {

    private fun board(side: PlayerColor, vararg at: Pair<Square, Piece>): BoardPosition =
        at.fold(XiangqiBoard.empty(side)) { position, (square, piece) -> position.withPiece(square, piece) }

    private fun red(kind: PieceKind) = Piece(PlayerColor.RED, kind)
    private fun black(kind: PieceKind) = Piece(PlayerColor.BLACK, kind)

    @Test
    fun aRookOnAnOpenFileIsThreatenedAndNothingTakesItBack() {
        val position = board(
            PlayerColor.RED,
            Square(4, 9) to red(PieceKind.GENERAL),
            Square(0, 5) to red(PieceKind.ROOK),
            Square(3, 0) to black(PieceKind.GENERAL),
            Square(0, 1) to black(PieceKind.ROOK)
        )
        // premise: the file really is clear between the two rooks
        for (rank in 2..4) assertNull(position.pieceAt(Square(0, rank)))

        val exposed = BoardExplanation.exposed(position, PlayerColor.RED)
        assertEquals(1, exposed.size)
        val hanging = exposed.first()
        assertEquals(Square(0, 5), hanging.square)
        assertEquals("红方俥", hanging.piece)
        assertEquals(listOf(Square(0, 1)), hanging.attackers)
        assertTrue("only the bare general is left, so nothing can recapture", hanging.recapturers.isEmpty())
        assertTrue(hanging.isUndefended)
    }

    @Test
    fun everyAttackerOnTheSameSquareIsNamed() {
        val position = board(
            PlayerColor.RED,
            Square(4, 9) to red(PieceKind.GENERAL),
            Square(2, 0) to red(PieceKind.ROOK),
            Square(5, 0) to black(PieceKind.GENERAL),
            Square(0, 0) to black(PieceKind.ROOK),
            Square(4, 0) to black(PieceKind.ROOK)
        )
        assertNull(position.pieceAt(Square(1, 0)))
        assertNull(position.pieceAt(Square(3, 0)))

        assertEquals(
            listOf(Square(0, 0), Square(4, 0)),
            BoardExplanation.attackersOf(position, Square(2, 0), PlayerColor.RED)
        )
    }

    /** A pinned defender only looks like protection until the rules get a vote. */
    @Test
    fun aPinnedProtectorDoesNotMakeAPieceSafe() {
        val cannon = Square(6, 5)
        val pinnedRook = Square(4, 5)
        val blackRook = Square(4, 0)
        val shared = listOf(
            Square(4, 9) to red(PieceKind.GENERAL),
            pinnedRook to red(PieceKind.ROOK),
            cannon to red(PieceKind.CANNON),
            Square(3, 0) to black(PieceKind.GENERAL),
            Square(5, 3) to black(PieceKind.HORSE)
        )
        // premises: the horse's leg and the rank between the two red pieces are clear
        assertNull(board(PlayerColor.RED, *(shared + (blackRook to black(PieceKind.ROOK))).toTypedArray())
            .pieceAt(Square(5, 4)))
        assertNull(board(PlayerColor.RED, *(shared + (blackRook to black(PieceKind.ROOK))).toTypedArray())
            .pieceAt(Square(5, 5)))

        val pinned = board(PlayerColor.RED, *(shared + (blackRook to black(PieceKind.ROOK))).toTypedArray())
        val cannonOnPinned = BoardExplanation.exposed(pinned, PlayerColor.RED).first { it.square == cannon }
        assertEquals(listOf(Square(5, 3)), cannonOnPinned.attackers)
        assertTrue(
            "the rook guards the rank but cannot leave the file without exposing its general",
            cannonOnPinned.recapturers.isEmpty()
        )
        assertTrue(cannonOnPinned.isUndefended)

        // control: remove the pinning rook and the very same guard now counts
        val unpinned = board(PlayerColor.RED, *shared.toTypedArray())
        val cannonOnFree = BoardExplanation.exposed(unpinned, PlayerColor.RED).first { it.square == cannon }
        assertEquals(listOf(pinnedRook), cannonOnFree.recapturers)
        assertFalse(cannonOnFree.isUndefended)
    }

    @Test
    fun aCheckedGeneralIsNeverReportedAsHanging() {
        val position = board(
            PlayerColor.RED,
            Square(4, 9) to red(PieceKind.GENERAL),
            Square(3, 0) to black(PieceKind.GENERAL),
            Square(4, 6) to black(PieceKind.ROOK)
        )
        assertTrue(XiangqiRules.outcome(position) is GameOutcome.Check)
        assertTrue(
            "rules refuse piece moves that capture a general, so no attacker is ever named on it",
            BoardExplanation.exposed(position, PlayerColor.RED).none { it.square == Square(4, 9) }
        )
    }

    @Test
    fun critiqueReportsWhereTheRookLandedAndWhatItLeftBehind() {
        val before = board(
            PlayerColor.RED,
            Square(4, 9) to red(PieceKind.GENERAL),
            Square(0, 5) to red(PieceKind.ROOK),
            Square(3, 0) to black(PieceKind.GENERAL),
            Square(5, 3) to black(PieceKind.HORSE)
        )
        assertNull(before.pieceAt(Square(1, 5)))
        assertNull(before.pieceAt(Square(5, 4)))
        assertTrue(BoardExplanation.exposed(before, PlayerColor.RED).isEmpty())

        val move = BoardMove(Square(0, 5), Square(6, 5), "", player = PlayerColor.RED)
        val applied = XiangqiRules.apply(before, move) as RuleResult.Applied

        val critique = BoardExplanation.critique(before, applied.position, applied.move)
        assertEquals(listOf(Square(5, 3)), BoardExplanation.attackersOf(applied.position, Square(6, 5), PlayerColor.RED))
        assertTrue("the rook walked onto a square the horse covers", critique.landedUnderFire)
        assertEquals(listOf(Square(6, 5)), critique.newlyUndefended.map { it.square })
    }

    @Test
    fun safestSkipsTheFirstSquareThatWalksIntoTheHorse() {
        val position = board(
            PlayerColor.RED,
            Square(4, 9) to red(PieceKind.GENERAL),
            Square(0, 5) to red(PieceKind.ROOK),
            Square(3, 0) to black(PieceKind.GENERAL),
            Square(2, 7) to black(PieceKind.HORSE)
        )
        // premises: the horse's two legs and both candidate squares are clear
        assertNull(position.pieceAt(Square(1, 7)))
        assertNull(position.pieceAt(Square(1, 5)))
        assertNull(position.pieceAt(Square(0, 6)))
        assertNull(position.pieceAt(Square(0, 7)))
        // (0,6) is the horse's; (0,7) is not
        assertEquals(listOf(Square(2, 7)), BoardExplanation.attackersOf(position, Square(0, 6), PlayerColor.RED))
        assertTrue(BoardExplanation.attackersOf(position, Square(0, 7), PlayerColor.RED).isEmpty())

        val safer = BoardExplanation.safest(position, PlayerColor.RED, SmartBoardEngine.NORMAL)!!
        assertEquals(Square(0, 5), safer.move.from)
        assertEquals("the scan's first move hangs a piece, so it must not win", Square(0, 7), safer.move.to)
        assertEquals(0, safer.attackedBefore)
        assertEquals(0, safer.attackedAfter)
        assertFalse("nothing was attacked to begin with, so nothing improved", safer.improves)
        assertTrue(safer.undefendedAfter.isEmpty())
        assertEquals(safer, BoardExplanation.safest(position, PlayerColor.RED, SmartBoardEngine.NORMAL))
    }

    @Test
    fun safestOpensUpWhenTheScanIsDeepEnoughToSeeTheCannon() {
        val initial = XiangqiBoard.initial()
        // each black cannon already aims at a red horse over red's own cannon on files 1 and 7
        val threats = BoardAnalysis.threatsAgainst(initial, PlayerColor.RED)
        assertEquals(2, threats.size)
        assertEquals(
            listOf(Square(1, 9), Square(7, 9)),
            threats.map { it.attacked }
        )

        val shallow = BoardExplanation.safest(initial, PlayerColor.RED, SmartBoardEngine.EASY)!!
        // rank 6 is the first rank holding red pieces, so a file-0 soldier push leads the scan
        assertEquals(BoardMove(Square(0, 6), Square(0, 5), "", player = PlayerColor.RED), shallow.move)
        assertEquals(2, shallow.attackedBefore)
        assertFalse("6 candidates never reach a cannon move", shallow.improves)

        val deeper = BoardExplanation.safest(initial, PlayerColor.RED, SmartBoardEngine.NORMAL)!!
        assertEquals(2, deeper.attackedBefore)
        assertEquals(1, deeper.attackedAfter)
        assertTrue(deeper.improves)
        assertTrue("no red piece is free to take after the winning move", deeper.undefendedAfter.isEmpty())
        assertEquals(deeper, BoardExplanation.safest(initial, PlayerColor.RED, SmartBoardEngine.NORMAL))

        assertEquals(6, BoardExplanation.candidateLimit(SmartBoardEngine.EASY))
        assertEquals(14, BoardExplanation.candidateLimit(SmartBoardEngine.NORMAL))
        assertEquals(40, BoardExplanation.candidateLimit(SmartBoardEngine.HARD))
    }

    @Test
    fun safestGivesUpWhenTheSideHasNoLegalMove() {
        val position = board(
            PlayerColor.RED,
            Square(4, 9) to red(PieceKind.GENERAL),
            Square(3, 0) to black(PieceKind.GENERAL),
            Square(4, 6) to black(PieceKind.ROOK),
            Square(3, 7) to black(PieceKind.ROOK),
            Square(5, 5) to black(PieceKind.ROOK)
        )
        assertTrue(XiangqiRules.legalMoves(position, PlayerColor.RED).isEmpty())
        assertNull(BoardExplanation.safest(position, PlayerColor.RED, SmartBoardEngine.HARD))
    }
}
