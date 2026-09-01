package com.xuanji.app.domain.game

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Archive and scoreboard contract: a saved game must come back as the position the rules
 * really produced, a damaged archive must degrade honestly instead of silently, and the
 * persisted record only ever counts settled results.
 */
class GameArchiveTest {

    private val bridge = GameDialogueBridge()

    /** Four real halfmoves played through the square-tap path (no engine needed). */
    private fun playedGame(): GameSessionState {
        var state = reduceGame(
            GameSessionState(),
            GameEvent.Start(GameType.XIANGQI, token = 1L, difficulty = SmartBoardEngine.HARD)
        )
        for (tap in listOf(
            Square(7, 7) to Square(4, 7),
            Square(7, 0) to Square(6, 2),
            Square(8, 9) to Square(8, 8),
            Square(1, 0) to Square(2, 2)
        )) {
            val next = bridge.applySquareMove(state, tap.first, tap.second).state
            assertTrue("tap $tap rejected", next.history.size > state.history.size)
            state = next
        }
        return state
    }

    // ---- codec ---------------------------------------------------------------------

    @Test
    fun archive_carries_only_board_data() {
        val save = GameArchive.saveOf(playedGame(), savedAt = 1234L)
        assertEquals(4, save.moves.size)
        assertEquals(1234L, save.savedAt)
        assertEquals(GameArchive.VERSION, save.version)
        assertTrue(
            "moves must stay plain UCI so the archive can never smuggle commentary",
            save.moves.all { move ->
                move.length == 4 && move.all { char -> char in 'a'..'i' || char in '0'..'9' }
            }
        )
        assertTrue(save.title.isEmpty())
    }

    @Test
    fun save_then_restore_rebuilds_the_same_session() {
        val played = playedGame()
        val restored = GameArchive.restore(GameArchive.saveOf(played), token = 7L)
        assertTrue(restored is GameRestore.Loaded)
        val loaded = restored as GameRestore.Loaded
        assertEquals(0, loaded.dropped)
        assertEquals(played.position, loaded.state.position)
        assertEquals(played.positionLog, loaded.state.positionLog)
        assertEquals(played.history.size, loaded.state.history.size)
        assertEquals(played.difficulty, loaded.state.difficulty)
        assertEquals(played.playerColor, loaded.state.playerColor)
        assertEquals(7L, loaded.state.sessionToken)
    }

    @Test
    fun restored_session_still_replays_captures_and_draw_log() {
        // red rook shuffles then takes the black soldier: the capture must survive the archive
        var state = reduceGame(
            GameSessionState(),
            GameEvent.Start(
                type = GameType.XIANGQI,
                token = 1L,
                position = XiangqiBoard.empty(PlayerColor.RED)
                    .withPiece(Square(3, 9), Piece(PlayerColor.RED, PieceKind.GENERAL))
                    .withPiece(Square(5, 0), Piece(PlayerColor.BLACK, PieceKind.GENERAL))
                    .withPiece(Square(0, 9), Piece(PlayerColor.RED, PieceKind.ROOK))
                    .withPiece(Square(0, 5), Piece(PlayerColor.BLACK, PieceKind.SOLDIER))
            )
        )
        for (tap in listOf(
            Square(0, 9) to Square(0, 8),
            Square(5, 0) to Square(4, 0),
            Square(0, 8) to Square(0, 5)
        )) {
            state = bridge.applySquareMove(state, tap.first, tap.second).state
        }
        assertEquals("卒", state.history.last().captured)
        val loaded = GameArchive.restore(GameArchive.saveOf(state), token = 2L) as GameRestore.Loaded
        assertEquals(0, loaded.dropped)
        assertEquals(state.positionLog, loaded.state.positionLog)
        assertEquals(
            Piece(PlayerColor.RED, PieceKind.ROOK),
            loaded.state.position.pieceAt(Square(0, 5))
        )
        assertEquals("卒", loaded.state.history.last().captured)
    }

    @Test
    fun endgame_archive_keeps_its_puzzle_position_and_solver() {
        val puzzle = EndgameCatalog.ALL.first()
        val opened = bridge.handle(GameSessionState(), "开第一关").state
        val loaded = GameArchive.restore(GameArchive.saveOf(opened), token = 3L) as GameRestore.Loaded
        assertEquals(0, loaded.dropped)
        assertEquals(puzzle.title, loaded.state.title)
        assertEquals(puzzle.position(), loaded.state.startPosition)
        assertEquals(puzzle.solver, loaded.state.playerColor)
    }

    @Test
    fun replay_stops_at_the_first_move_the_rules_reject() {
        val save = GameArchive.saveOf(playedGame())
        val damaged = save.copy(moves = save.moves + listOf("e9e7", "i0i1"))
        val loaded = GameArchive.restore(damaged, token = 4L) as GameRestore.Loaded
        assertEquals(2, loaded.dropped)
        assertEquals(save.moves.size, loaded.state.history.size)
        assertEquals(GameArchive.saveOf(playedGame()).start, loaded.state.positionLog.first())
    }

    @Test
    fun unreadable_or_illegal_archives_are_rejected() {
        val save = GameArchive.saveOf(playedGame())
        assertTrue(
            GameArchive.restore(save.copy(version = GameArchive.VERSION + 1), 1L)
                is GameRestore.Rejected
        )
        assertTrue(GameArchive.restore(save.copy(start = ""), 1L) is GameRestore.Rejected)
        assertTrue(GameArchive.restore(save.copy(start = "nonsense"), 1L) is GameRestore.Rejected)
        val facing = GameArchive.restore(
            save.copy(start = "4k4/9/9/9/9/9/9/9/9/4K4 r", moves = emptyList()),
            token = 1L
        )
        assertTrue(facing is GameRestore.Rejected)
        assertEquals("generals_facing", (facing as GameRestore.Rejected).reason)
    }

    @Test
    fun unknown_difficulty_falls_back_to_normal() {
        val save = GameArchive.saveOf(playedGame()).copy(difficulty = "hell")
        val loaded = GameArchive.restore(save, token = 5L) as GameRestore.Loaded
        assertEquals(SmartBoardEngine.NORMAL, loaded.state.difficulty)
    }

    // ---- bridge commands -------------------------------------------------------------

    @Test
    fun save_command_asks_the_caller_to_write_and_keeps_the_session() {
        val played = playedGame()
        val result = bridge.handle(played, "保存棋局")
        assertEquals(GameDialogueBridge.ArchiveRequest.SAVE, result.archive)
        assertEquals(played, result.state)
        assertTrue(result.reply.contains("4 手"))
        assertTrue(result.grounded)
        assertEquals(
            GameDialogueBridge.ArchiveRequest.SAVE,
            bridge.handle(played, "存档").archive
        )
        assertNull(bridge.handle(GameSessionState(sessionToken = 1L), "保存棋局").archive)
    }

    @Test
    fun resume_command_asks_the_caller_to_read() {
        val idle = GameSessionState()
        val result = bridge.handle(idle, "继续棋局")
        assertEquals(GameDialogueBridge.ArchiveRequest.RESUME, result.archive)
        assertEquals(idle, result.state)
        // a live session with moves does not need a resume, and must not be overwritten
        val played = playedGame()
        val busy = bridge.handle(played, "继续棋局")
        assertNull(busy.archive)
        assertEquals(played, busy.state)
    }

    @Test
    fun resume_with_no_archive_or_a_bad_one_says_so() {
        val empty = GameSessionState()
        val missing = bridge.resumeWith(empty, null)
        assertTrue(missing.reply.contains("没有找到"))
        assertNull(missing.event)
        val bad = bridge.resumeWith(empty, GameArchive.saveOf(playedGame()).copy(start = ""))
        assertTrue(bad.reply.contains("未能通过规则校验"))
        assertEquals(empty, bad.state)
    }

    @Test
    fun resume_with_an_archive_reopens_the_game_at_its_last_frame() {
        val played = playedGame()
        val save = GameArchive.saveOf(played)
        val result = bridge.resumeWith(GameSessionState(), save)
        assertTrue(result.event is GameEvent.Start)
        assertEquals(played.position, result.state.position)
        assertEquals(played.history.size, result.state.history.size)
        assertTrue(result.reply.contains("回到第 ${played.history.size} 手"))
        val resumed = GameDialogueBridge().handle(result.state, "有哪些威胁")
        assertTrue("resumed session must behave like a live one", resumed.grounded)
    }

    @Test
    fun resume_reports_the_halfmoves_it_had_to_drop() {
        val save = GameArchive.saveOf(playedGame())
        val result = bridge.resumeWith(
            GameSessionState(),
            save.copy(moves = save.moves + "e9e7")
        )
        assertTrue(result.reply.contains("1 手未通过规则校验"))
        assertEquals(save.moves.size, result.state.history.size)
    }

    @Test
    fun record_command_reads_the_injected_scoreboard() {
        val scored = GameDialogueBridge(recordOf = { GameRecord(wins = 2, losses = 1, lastResult = "负") })
        val result = scored.handle(GameSessionState(), "战绩")
        assertNull(result.event)
        assertNull(result.archive)
        assertTrue(result.reply.contains("2 胜 1 负 0 和"))
        assertTrue(result.reply.contains("共 3 局"))
        assertTrue(scored.handle(playedGame(), "比分").reply.contains("上一局负"))
    }

    @Test
    fun engine_reply_after_resume_keeps_playing_the_same_session() = runTest {
        // black to move after one red halfmove: archive, restore, then let the engine answer
        val opened = bridge.handle(GameSessionState(), "来一盘象棋，简单").state
        val moved = bridge.handle(opened, "走炮二平五").state
        assertTrue(bridge.shouldAskEngine(moved))
        val loaded = bridge.resumeWith(GameSessionState(), GameArchive.saveOf(moved))
        assertTrue(loaded.awaitEngine)
        val answer = bridge.engineReply(loaded.state)
        assertEquals(2, answer.state.history.size)
        assertEquals(PlayerColor.RED, answer.state.position.sideToMove)
        assertNotNull(answer.state.history.lastOrNull())
    }

    // ---- scoreboard ------------------------------------------------------------------

    @Test
    fun tally_only_counts_settled_results() {
        val record = GameRecord().tally("win").tally(null).tally("draw").tally("loss")
        assertEquals(1, record.wins)
        assertEquals(1, record.draws)
        assertEquals(1, record.losses)
        assertEquals(3, record.games)
        assertEquals("负", record.lastResult)
        assertEquals(GameRecord(1, 1, 1, "负"), record)
    }

    @Test
    fun summary_text_states_the_real_numbers() {
        assertEquals("还没有已结束的棋局。", GameRecord().summaryText())
        assertEquals(
            "战绩 2 胜 0 负 1 和，共 3 局。上一局和。",
            GameRecord(wins = 2, draws = 1, lastResult = "和").summaryText()
        )
    }

    @Test
    fun settled_note_uses_the_same_words_as_the_scoreboard() {
        assertEquals("象棋·胜", GameRecord.settledNote("win"))
        assertEquals("象棋·负", GameRecord.settledNote("loss"))
        assertEquals("象棋·和", GameRecord.settledNote("draw"))
        // 没有结算就没有一行——调用方拿到空串，什么也不会记进长期记忆。
        assertEquals("", GameRecord.settledNote(null))
        assertEquals("", GameRecord.settledNote("resigned"))
    }
}
