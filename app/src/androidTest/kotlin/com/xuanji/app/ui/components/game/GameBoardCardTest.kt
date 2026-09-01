package com.xuanji.app.ui.components.game

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xuanji.app.domain.game.BoardMove
import com.xuanji.app.domain.game.BoardPosition
import com.xuanji.app.domain.game.PlayerColor
import com.xuanji.app.domain.game.RuleResult
import com.xuanji.app.domain.game.SmartBoardEngine
import com.xuanji.app.domain.game.Square
import com.xuanji.app.domain.game.XiangqiBoard
import com.xuanji.app.domain.game.XiangqiNotation
import com.xuanji.app.domain.game.XiangqiRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real board composable through the taps a player makes. Every expectation is a
 * legal-move fact from [XiangqiRules], so the UI cannot start offering a move the rules reject.
 *
 * The card is mounted at 60% width: the board is aspect-ratio locked, so a full width card
 * pushes the lower control rows off short screens and a tap would land on the wrong node.
 */
@RunWith(AndroidJUnit4::class)
class GameBoardCardTest {

    @get:Rule
    val compose = createComposeRule()

    private val initial get() = XiangqiBoard.initial()

    private fun squareTag(file: Int, rank: Int) = "square-$file-$rank"

    private fun showCard(
        position: BoardPosition = initial,
        history: List<BoardMove> = emptyList(),
        viewPly: Int? = null,
        difficulty: String = SmartBoardEngine.NORMAL,
        thinking: Boolean = false,
        canRedo: Boolean = false,
        onSquareTap: (Pair<Square, Square>) -> Unit = {},
        onStep: (Int) -> Unit = {},
        onDifficultyChange: (String) -> Unit = {}
    ) {
        compose.setContent {
            GameBoardCard(
                position = position,
                modifier = Modifier.fillMaxWidth(0.6f),
                history = history,
                viewPly = viewPly,
                difficulty = difficulty,
                thinking = thinking,
                canRedo = canRedo,
                onSquareTap = onSquareTap,
                onStep = onStep,
                onDifficultyChange = onDifficultyChange
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun tappingAPieceThenALegalTargetReportsThatPair() {
        var tapped: Pair<Square, Square>? = null
        showCard(onSquareTap = { tapped = it })

        compose.onNodeWithTag(squareTag(7, 7)).performClick()
        compose.onNodeWithTag(squareTag(4, 7)).performClick()

        assertEquals(Square(7, 7) to Square(4, 7), tapped)
    }

    @Test
    fun selectingThenTappingAnUnrelatedSquareCancelsInsteadOfMoving() {
        var tapped: Pair<Square, Square>? = null
        showCard(onSquareTap = { tapped = it })

        compose.onNodeWithTag(squareTag(7, 7)).performClick()
        compose.onNodeWithTag(squareTag(1, 8)).performClick()
        compose.onNodeWithTag(squareTag(4, 7)).performClick()

        assertNull("a cancelled selection must never emit a move", tapped)
    }

    @Test
    fun opponentPiecesAreNotSelectableOnYourTurn() {
        var tapped: Pair<Square, Square>? = null
        showCard(onSquareTap = { tapped = it })

        compose.onNodeWithTag(squareTag(0, 0)).performClick()

        assertNull(tapped)
        assertTrue(
            "nothing may be highlighted while no own piece is selected",
            compose.onAllNodesWithContentDescription("可落子格").fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun selectingACannonHighlightsItsTargetsAndNamesThePiece() {
        showCard()

        compose.onNodeWithTag(squareTag(7, 7)).performClick()

        assertTrue(
            compose.onAllNodesWithContentDescription("可落子格").fetchSemanticsNodes().isNotEmpty()
        )
        // both red cannons carry this label, so the count is the assertion
        assertEquals(2, compose.onAllNodesWithContentDescription("红方 炮").fetchSemanticsNodes().size)
    }

    @Test
    fun selectingAHorseHighlightsExactlyItsTwoLegalSquares() {
        showCard()

        // one of the four jumps is blocked by the elephant standing on the horse's leg
        compose.onNodeWithTag(squareTag(1, 9)).performClick()

        assertEquals(2, compose.onAllNodesWithContentDescription("可落子格").fetchSemanticsNodes().size)
    }

    @Test
    fun theChosenDifficultyIsReportedBack() {
        var picked: String? = null
        showCard(onDifficultyChange = { picked = it })

        compose.onNodeWithTag("difficulty-" + SmartBoardEngine.HARD).performClick()

        assertEquals(SmartBoardEngine.HARD, picked)
    }

    @Test
    fun thinkingBlocksInputAndShowsTheEngineStatus() {
        var tapped: Pair<Square, Square>? = null
        var picked: String? = null
        showCard(thinking = true, onSquareTap = { tapped = it }, onDifficultyChange = { picked = it })

        compose.onNodeWithText("引擎推演中···").assertExists()
        compose.onNodeWithTag(squareTag(7, 7)).performClick()
        compose.onNodeWithTag(squareTag(4, 7)).performClick()
        compose.onNodeWithTag("difficulty-" + SmartBoardEngine.HARD).performClick()

        assertNull(tapped)
        assertNull(picked)
    }

    @Test
    fun aReviewFrameIsLabelledAndIgnoresTaps() {
        var tapped: Pair<Square, Square>? = null
        var stepped: Int? = null
        val moves = openingHistory()
        showCard(
            position = replay(moves.take(1)),
            history = moves,
            viewPly = 1,
            onSquareTap = { tapped = it },
            onStep = { stepped = it }
        )

        compose.onNodeWithText("回放第 1/2 手").assertExists()
        compose.onNodeWithTag(squareTag(7, 7)).performClick()
        compose.onNodeWithTag(squareTag(4, 7)).performClick()
        assertNull("a review frame must not commit moves", tapped)

        compose.onNodeWithContentDescription("回到当前局面").performClick()
        assertEquals(2, stepped)
    }

    @Test
    fun undoAndRedoAreDisabledOnAFreshSession() {
        showCard(canRedo = false)

        compose.onNodeWithContentDescription("撤销上一手").assertIsNotEnabled()
        compose.onNodeWithContentDescription("恢复刚悔掉的走法").assertIsNotEnabled()
    }

    @Test
    fun undoAndRedoOpenUpOnceTheSessionHasHistory() {
        val moves = openingHistory()
        showCard(position = replay(moves), history = moves, canRedo = true)

        compose.onNodeWithContentDescription("撤销上一手").assertIsEnabled()
        compose.onNodeWithContentDescription("恢复刚悔掉的走法").assertIsEnabled()
    }

    @Test
    fun theCaptureTrayStaysHiddenWithoutRealCaptures() {
        showCard()

        compose.onNodeWithContentDescription("双方吃子记录").assertDoesNotExist()
    }

    @Test
    fun theCaptureTrayReportsOnlyWhatHistoryActuallyCaptured() {
        val (position, history) = captureHistory()
        showCard(position = position, history = history)

        compose.onNodeWithContentDescription("双方吃子记录").assertExists()
        compose.onNodeWithText("红吃: 卒").assertExists()
    }

    /** Replays [moves] through the rules so a test mounts the board that ply actually reaches. */
    private fun replay(moves: List<BoardMove>): BoardPosition {
        var position = initial
        for (move in moves) {
            position = (XiangqiRules.apply(position, move) as RuleResult.Applied).position
        }
        return position
    }

    /** Coordinates only: the app's own formatter writes the Chinese record. */
    private fun move(from: Square, to: Square, color: PlayerColor, at: BoardPosition): RuleResult.Applied {
        val candidate = BoardMove(from, to, "", player = color)
        return XiangqiRules.apply(
            at,
            candidate.copy(notation = XiangqiNotation.format(candidate, at))
        ) as RuleResult.Applied
    }

    private fun openingHistory(): List<BoardMove> {
        val first = move(Square(7, 7), Square(4, 7), PlayerColor.RED, initial)
        val second = move(Square(7, 0), Square(6, 2), PlayerColor.BLACK, first.position)
        return listOf(first.move, second.move)
    }

    /** 兵 advances, 卒 advances, then 兵 takes 卒: the third halfmove really captures. */
    private fun captureHistory(): Pair<BoardPosition, List<BoardMove>> {
        val one = move(Square(0, 6), Square(0, 5), PlayerColor.RED, initial)
        val two = move(Square(0, 3), Square(0, 4), PlayerColor.BLACK, one.position)
        val three = move(Square(0, 5), Square(0, 4), PlayerColor.RED, two.position)
        assertEquals("卒", three.move.captured)
        return three.position to listOf(one.move, two.move, three.move)
    }
}
