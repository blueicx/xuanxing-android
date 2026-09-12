package com.xuanji.app.domain.game

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Engine wiring for the dialogue bridge: the search engine must actually answer, the
 * engine reply must come back through the rules, and every new command (difficulty,
 * threats, spectate, redo, endgames, draw wording) must stay grounded in real board data.
 */
class GameAutoReplyTest {

    private val bridge = GameDialogueBridge()

    private fun started(token: Long = 1L): GameSessionState =
        bridge.handle(GameSessionState(sessionToken = token - 1), "来一盘象棋，简单").state

    private fun session(position: BoardPosition, token: Long = 1L): GameSessionState =
        reduceGame(
            GameSessionState(sessionToken = token - 1),
            GameEvent.Start(GameType.XIANGQI, token, position = position)
        )

    /** Quiet shuffle position: two rooks on open files, generals on different files. */
    private fun shufflePosition(): BoardPosition = XiangqiBoard.empty(PlayerColor.RED)
        .withPiece(Square(3, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
        .withPiece(Square(0, 7), Piece(PlayerColor.RED, PieceKind.ROOK))
        .withPiece(Square(5, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
        .withPiece(Square(8, 2), Piece(PlayerColor.BLACK, PieceKind.ROOK))

    private val shuffleCycle = listOf(
        Square(0, 7) to Square(0, 6),
        Square(8, 2) to Square(8, 3),
        Square(0, 6) to Square(0, 7),
        Square(8, 3) to Square(8, 2)
    )

    private fun applySquare(state: GameSessionState, from: Square, to: Square): GameSessionState {
        val result = bridge.applySquareMove(state, from, to)
        assertTrue("move $from->$to rejected: ${result.reply}", result.state != state)
        return result.state
    }

    /** Shuffle until the draw rule fires; returns the state right before that happens. */
    private fun shuffleFor(halfmoves: Int): GameSessionState {
        var state = session(shufflePosition())
        for (index in 0 until halfmoves) {
            val (from, to) = shuffleCycle[index % shuffleCycle.size]
            state = applySquare(state, from, to)
            if (state.outcome is GameOutcome.Draw) break
        }
        return state
    }

    // ---- automatic engine reply ------------------------------------------------------

    @Test
    fun player_move_marks_engine_turn_pending() {
        val moved = bridge.handle(started(), "走炮二平五")
        assertTrue(moved.awaitEngine)
        assertEquals(PlayerColor.BLACK, moved.state.position.sideToMove)
    }

    @Test
    fun engine_reply_lands_a_rules_verified_move_and_returns_the_turn() = runTest {
        val moved = bridge.handle(started(), "走炮二平五").state
        val reply = bridge.engineReply(moved)
        assertTrue(reply.grounded)
        assertTrue(reply.event is GameEvent.EngineReply)
        assertEquals(2, reply.state.history.size)
        assertEquals(PlayerColor.RED, reply.state.position.sideToMove)
        assertFalse("engine must not hand the turn straight back to itself", reply.awaitEngine)
        assertTrue("reply must be first person commentary: ${reply.reply}", reply.reply.startsWith("我走「"))
        val engineMove = reply.state.history.last()
        assertEquals(PlayerColor.BLACK, engineMove.player)
        assertTrue(
            "engine move must exist in the real legal move list",
            XiangqiRules.legalMoves(moved.position, PlayerColor.BLACK).any {
                it.from == engineMove.from && it.to == engineMove.to
            }
        )
    }

    @Test
    fun engine_reply_is_deterministic_for_the_same_position() = runTest {
        val moved = bridge.handle(started(), "走炮二平五").state
        val first = bridge.engineReply(moved)
        val second = bridge.engineReply(moved)
        assertEquals(first.state.position, second.state.position)
        assertEquals(first.reply, second.reply)
    }

    @Test
    fun engine_reply_is_skipped_when_it_is_the_players_turn() = runTest {
        val fresh = started()
        val reply = bridge.engineReply(fresh)
        assertFalse(reply.grounded)
        assertNull(reply.event)
        assertEquals(fresh, reply.state)
    }

    @Test
    fun injected_engine_is_the_one_that_answers() = runTest {
        val offlineBridge = GameDialogueBridge(OfflineBoardEngine("easy"))
        val moved = offlineBridge.handle(started(), "走炮二平五").state
        val reply = offlineBridge.engineReply(moved)
        assertEquals(2, reply.state.history.size)
        assertEquals(PlayerColor.BLACK, reply.state.history.last().player)
    }

    @Test
    fun black_games_open_with_the_engine_move() = runTest {
        val opened = bridge.handle(GameSessionState(), "来一盘象棋，我执黑，简单")
        assertEquals(PlayerColor.BLACK, opened.state.playerColor)
        assertTrue(opened.awaitEngine)
        assertEquals(PlayerColor.RED, opened.state.position.sideToMove)
        val reply = bridge.engineReply(opened.state)
        assertEquals(PlayerColor.RED, reply.state.history.last().player)
        assertEquals(PlayerColor.BLACK, reply.state.position.sideToMove)
    }

    @Test
    fun color_change_before_the_first_move_switches_sides() = runTest {
        val fresh = started()
        val flipped = bridge.handle(fresh, "我执黑")
        assertEquals(PlayerColor.BLACK, flipped.state.playerColor)
        assertTrue(flipped.awaitEngine)
        // mid-game the bridge refuses a silent color swap
        val moved = bridge.handle(fresh, "走炮二平五").state
        val late = bridge.handle(moved, "我执黑")
        assertEquals(moved, late.state)
        assertTrue(late.reply.contains("中途不换色"))
    }

    // ---- spectate / difficulty -------------------------------------------------------

    @Test
    fun spectate_hands_both_sides_to_the_engine() {
        val watching = bridge.handle(started(), "这局我观战")
        assertEquals(PlayerColor.WHITE, watching.state.playerColor)
        assertTrue(watching.awaitEngine)
        assertTrue(bridge.shouldAskEngine(watching.state))
        assertNull(bridge.settledResult(watching.state))
    }

    @Test
    fun difficulty_command_updates_the_session() {
        val harder = bridge.handle(started(), "难度困难")
        assertEquals(SmartBoardEngine.HARD, harder.state.difficulty)
        assertTrue(harder.reply.contains("困难"))
        assertTrue(harder.reply.contains("4 层"))
        val unknown = bridge.handle(started(), "难度地狱")
        assertEquals(SmartBoardEngine.EASY, unknown.state.difficulty)
    }

    @Test
    fun session_side_effects_respect_the_token() {
        val state = started()
        val stale = state.sessionToken + 99
        assertEquals(state, reduceGame(state, GameEvent.Redo(stale)))
        assertEquals(state, reduceGame(state, GameEvent.SetDifficulty(stale, SmartBoardEngine.HARD)))
        assertEquals(state, reduceGame(state, GameEvent.SetColor(stale, PlayerColor.WHITE)))
    }

    // ---- undo / redo -----------------------------------------------------------------

    @Test
    fun redo_replays_the_undone_round_move_by_move() = runTest {
        val moved = bridge.handle(started(), "走炮二平五").state
        val answered = bridge.engineReply(moved).state
        val undone = bridge.handle(answered, "悔棋").state
        assertEquals(0, undone.history.size)
        assertEquals(PlayerColor.RED, undone.position.sideToMove)
        val first = bridge.handle(undone, "重做这一手")
        assertEquals(1, first.state.history.size)
        assertEquals(moved.position, first.state.position)
        assertTrue(first.reply.contains("已重做"))
        val second = bridge.handle(first.state, "重做这一手")
        assertEquals(answered.position, second.state.position)
    }

    @Test
    fun redo_without_pending_moves_reports_instead_of_faking() {
        val result = bridge.handle(started(), "重做这一手")
        assertTrue(result.reply.contains("没有可重做"))
        assertNull(result.event)
    }

    // ---- grounded threat report --------------------------------------------------------

    @Test
    fun threat_report_names_the_real_attacked_piece() {
        val attacked = session(
            shufflePosition()
                .withPiece(Square(0, 7), null)
                .withPiece(Square(0, 6), Piece(PlayerColor.RED, PieceKind.ROOK))
                .withPiece(Square(8, 2), null)
                .withPiece(Square(0, 2), Piece(PlayerColor.BLACK, PieceKind.ROOK))
        )
        val asked = bridge.handle(attacked, "我有哪些子被威胁")
        assertTrue(asked.grounded)
        assertTrue("reply must name the threat: ${asked.reply}", asked.reply.contains("正被"))
        assertTrue(asked.reply.contains("俥"))
        assertTrue(asked.reply.contains("第1列"))
        assertNull(asked.event)
    }

    @Test
    fun threat_count_comes_from_the_real_attack_map() {
        val state = started()
        val expected = BoardAnalysis.threatsAgainst(state.position, PlayerColor.RED).size
        assertTrue("the opening position has cannon fire on red soldiers", expected > 0)
        val result = bridge.handle(state, "有哪些威胁")
        assertTrue(result.reply.contains("$expected 个子正被攻击"))
    }

    @Test
    fun quiet_position_reports_no_threats() {
        val result = bridge.handle(session(shufflePosition()), "有哪些威胁")
        assertTrue(result.reply.contains("没有正被攻击"))
    }

    // ---- endgames ----------------------------------------------------------------------

    @Test
    fun endgame_catalog_is_listed_and_loadable() {
        val list = bridge.handle(GameSessionState(), "残局")
        assertTrue(list.reply.contains("单车必胜"))
        assertTrue(list.reply.contains("${EndgameCatalog.ALL.size} 关"))
        val puzzle = EndgameCatalog.ALL.first()
        val loaded = bridge.handle(GameSessionState(), "开第一关")
        assertEquals(puzzle.title, loaded.state.title)
        assertEquals(puzzle.position(), loaded.state.position)
        assertEquals(puzzle.solver, loaded.state.playerColor)
        assertTrue(loaded.reply.contains(puzzle.title))
    }

    @Test
    fun endgame_out_of_range_is_not_loaded() {
        val result = bridge.handle(GameSessionState(), "开第${EndgameCatalog.ALL.size + 5}关")
        assertTrue(result.reply.isNotEmpty())
        assertEquals("", result.state.title)
    }

    // ---- draw wording --------------------------------------------------------------------

    @Test
    fun threefold_repetition_draws_with_the_real_reason() {
        val drawn = shuffleFor(12)
        assertTrue(drawn.outcome is GameOutcome.Draw)
        assertEquals("repetition", drawn.drawReason())
    }

    @Test
    fun draw_reply_text_comes_from_the_draw_reason() {
        val beforeDraw = shuffleFor(7)
        assertFalse(beforeDraw.outcome is GameOutcome.Draw)
        val drawing = bridge.applySquareMove(beforeDraw, Square(8, 3), Square(8, 2))
        assertEquals(GameOutcome.Draw, drawing.state.outcome)
        assertTrue(drawing.reply.contains("和棋（双方不变作和）"))
        assertEquals("draw", bridge.settledResult(drawing.state))
    }

    @Test
    fun engine_does_not_move_on_a_drawn_position() = runTest {
        val drawn = GameSessionState(sessionToken = 1L, outcome = GameOutcome.Draw, playerColor = PlayerColor.BLACK)
        assertFalse(bridge.shouldAskEngine(drawn))
        assertEquals("", bridge.engineReply(drawn).reply)
    }

    // ---- settled result (drives the scoreboard) ------------------------------------------

    @Test
    fun settled_result_is_relative_to_the_human_color() {
        // red rook drops to rank 0 for mate; the black general is trapped on (3,0)
        val position = XiangqiBoard.empty(PlayerColor.RED)
            .withPiece(Square(3, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
            .withPiece(Square(5, 0), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(4, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
            .withPiece(Square(4, 1), Piece(PlayerColor.RED, PieceKind.ROOK))
            .withPiece(Square(2, 1), Piece(PlayerColor.RED, PieceKind.SOLDIER))
        val mate = BoardMove(Square(4, 1), Square(4, 0), "车五进一", player = PlayerColor.RED)
        val asRed = reduceGame(
            session(position),
            GameEvent.ApplyMove(1L, mate)
        )
        assertTrue(asRed.outcome is GameOutcome.Checkmate)
        assertEquals("win", bridge.settledResult(asRed))
        assertEquals("loss", bridge.settledResult(asRed.copy(playerColor = PlayerColor.BLACK)))
        assertNull(bridge.settledResult(asRed.copy(playerColor = PlayerColor.WHITE)))
    }
}
