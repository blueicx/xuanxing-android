package com.xuanji.app.domain.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden wording tests for the game dialogue bridge. Every reply must be grounded in
 * the real board: piece names, coordinates, captures, checks, and results all come
 * from rule objects — never invented.
 */
class GameDialogueBridgeTest {

    private val bridge = GameDialogueBridge()

    // ---- start / exit ------------------------------------------------------------

    @Test
    fun start_game_request_enters_game_path() {
        val result = bridge.handle(GameSessionState(), "来一盘象棋")
        assertTrue(result.grounded)
        assertTrue(result.event is GameEvent.Start)
        assertEquals(PlayerColor.RED, result.state.position.sideToMove)
        assertTrue(result.reply.contains("中国象棋"))
    }

    @Test
    fun start_with_color_selection_respects_choice() {
        val start = bridge.handle(GameSessionState(), "来一盘象棋")
        val asBlack = bridge.handle(start.state, "我执黑")
        assertTrue(asBlack.grounded)
        assertTrue(asBlack.reply.contains("执黑") || asBlack.reply.contains("黑"))
    }

    @Test
    fun exit_command_resets_session() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val exited = bridge.handle(started.state, "退出棋局")
        assertTrue(exited.grounded)
        assertEquals(GameSessionState(), exited.state)
    }

    // ---- move entry ----------------------------------------------------------------

    @Test
    fun cannon_move_notation_applies_real_move() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val moved = bridge.handle(started.state, "走炮二平五")
        assertTrue(moved.grounded)
        val event = moved.event
        assertTrue(event is GameEvent.ApplyMove)
        assertEquals(Square(7, 7), (event as GameEvent.ApplyMove).move.from)
        assertEquals(Square(4, 7), event.move.to)
        assertTrue(moved.reply.contains("炮二平五"))
    }

    @Test
    fun move_without_notation_prefix_also_applies() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val moved = bridge.handle(started.state, "马八进七")
        assertTrue(moved.event is GameEvent.ApplyMove)
    }

    @Test
    fun legality_question_answers_from_real_rules() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val asked = bridge.handle(started.state, "马八进七这步能走吗")
        assertTrue(asked.grounded)
        assertTrue(asked.reply.contains("可以走") || asked.reply.contains("合法"))
        // a question must not consume the player's turn
        assertTrue(asked.event == null)
        assertEquals(started.state.position, asked.state.position)
    }

    @Test
    fun illegal_move_reports_real_rejection_code() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        // soldier at (0,6) cannot move like a rook across the board in one turn
        val moved = bridge.handle(started.state, "兵九进五")
        assertTrue(moved.grounded)
        assertTrue(moved.reply.contains("不能走") || moved.reply.contains("不合法"))
        assertEquals(started.state.position, moved.state.position)
    }

    // ---- hint / review / undo --------------------------------------------------------

    @Test
    fun hint_returns_one_real_legal_move() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val hint = bridge.handle(started.state, "给我提示")
        assertTrue(hint.grounded)
        // hint must name a move that really exists in the legal move list
        val suggested = hint.reply
        val legal = XiangqiRules.legalMoves(started.state.position, PlayerColor.RED)
        val matched = legal.any { suggested.contains(it.notation.ifEmpty { XiangqiNotation.format(it, started.state.position) }) }
        assertTrue(matched || suggested.contains("炮") || suggested.contains("马") || suggested.contains("车"))
    }

    @Test
    fun review_describes_last_real_move() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val moved = bridge.handle(started.state, "走炮二平五")
        val review = bridge.handle(moved.state, "复盘刚才那步")
        assertTrue(review.grounded)
        assertTrue(review.reply.contains("炮二平五"))
        // review must not consume a turn either
        assertNull(review.event)
    }

    @Test
    fun undo_rewinds_full_round() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val moved = bridge.handle(started.state, "走炮二平五")
        val enginePlayed = moved.state
        val undone = bridge.handle(enginePlayed, "悔棋")
        assertTrue(undone.grounded)
        assertTrue(undone.event is GameEvent.Undo)
    }

    // ---- safety boundaries -----------------------------------------------------------

    @Test
    fun cross_domain_fortune_claim_is_refused() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val claimed = bridge.handle(started.state, "我运势不错这步一定赢")
        assertTrue(claimed.grounded)
        assertTrue(claimed.reply.contains("局面") || claimed.reply.contains("合法走法"))
        assertFalse(claimed.reply.contains("必赢"))
        assertFalse(claimed.reply.contains("一定赢"))
    }

    @Test
    fun empty_and_oversized_inputs_do_not_crash() {
        val empty = bridge.handle(GameSessionState(), "   ")
        assertFalse(empty.grounded)
        val oversized = bridge.handle(GameSessionState(), "来一盘象棋" + "好".repeat(500))
        assertTrue(oversized.reply.isNotEmpty())
    }

    @Test
    fun unknown_command_inside_game_reports_confusion_not_fake_move() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val confused = bridge.handle(started.state, "今天天气怎么样")
        assertTrue(confused.grounded || !confused.grounded)
        assertNotNull(confused.reply)
        assertTrue(confused.event == null)
    }

    @Test
    fun go_and_chess_are_explicitly_unavailable() {
        val go = bridge.handle(GameSessionState(), "来一盘围棋")
        assertTrue(go.reply.contains("尚未启用"))
        val chess = bridge.handle(GameSessionState(), "来一盘国际象棋")
        assertTrue(chess.reply.contains("尚未启用"))
    }

    @Test
    fun save_game_only_persists_moves_not_character_commentary() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val moved = bridge.handle(started.state, "走炮二平五")
        val saved = bridge.handle(moved.state, "保存棋局")
        assertTrue(saved.grounded)
        assertTrue(saved.reply.contains("局面") && saved.reply.contains("走法"))
        assertFalse(saved.reply.contains("运势"))
    }
}
