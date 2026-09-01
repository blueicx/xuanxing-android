package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the board facts the instrumented GameBoardCardTest asserts against. That test needs a
 * device to run, so any premise it gets wrong has to fail here on the JVM first.
 */
class BoardUiFixtureTest {

    private val initial get() = XiangqiBoard.initial()

    /** Builds a move the same way the app would, letting [XiangqiNotation] write its record. */
    private fun applied(from: Square, to: Square, color: PlayerColor, at: BoardPosition): RuleResult.Applied {
        val candidate = BoardMove(from, to, "", player = color)
        val recorded = candidate.copy(notation = XiangqiNotation.format(candidate, at))
        return XiangqiRules.apply(at, recorded) as RuleResult.Applied
    }

    @Test
    fun openingSequenceIsLegal() {
        val first = applied(Square(7, 7), Square(4, 7), PlayerColor.RED, initial)
        assertNull(first.move.captured)
        val second = applied(Square(7, 0), Square(6, 2), PlayerColor.BLACK, first.position)
        assertNull(second.move.captured)
        assertEquals(GameOutcome.InProgress, XiangqiRules.outcome(second.position))
    }

    @Test
    fun captureSequenceReallyCaptures() {
        val one = applied(Square(0, 6), Square(0, 5), PlayerColor.RED, initial)
        val two = applied(Square(0, 3), Square(0, 4), PlayerColor.BLACK, one.position)
        val three = applied(Square(0, 5), Square(0, 4), PlayerColor.RED, two.position)
        assertEquals("卒", three.move.captured)
    }

    @Test
    fun squareFactsBehindTheUiAssertions() {
        val redMoves = XiangqiRules.legalMoves(initial, PlayerColor.RED)

        val cannonTargets = redMoves.filter { it.from == Square(7, 7) }.map { it.to }.toSet()
        assertEquals(12, cannonTargets.size)
        assertTrue(Square(4, 7) in cannonTargets)

        // the horse is blocked on one leg by its own elephant, so exactly two rings light up
        val horseTargets = redMoves.filter { it.from == Square(1, 9) }.map { it.to }.toSet()
        assertEquals(setOf(Square(0, 7), Square(2, 7)), horseTargets)
        assertTrue(horseTargets.all { initial.pieceAt(it) == null })

        // an empty square outside the selected piece's target set cancels the selection
        assertNull(initial.pieceAt(Square(1, 8)))
        assertTrue(Square(1, 8) !in cannonTargets)

        assertEquals(PlayerColor.BLACK, initial.pieceAt(Square(0, 0))?.color)
        assertEquals(
            2,
            (0..9).flatMap { rank -> (0..8).map { file -> Square(file, rank) } }
                .mapNotNull { initial.pieceAt(it) }
                .count { it.color == PlayerColor.RED && it.kind == PieceKind.CANNON }
        )
    }
}
