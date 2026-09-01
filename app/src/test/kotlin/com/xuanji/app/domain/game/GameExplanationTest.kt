package com.xuanji.app.domain.game

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wording checks for the two explaining commands (「这步为什么不好」/「换个稳一点的走法」) and the
 * difficulty-scaled critique tail every played halfmove now carries.
 *
 * The board premises are the same fixtures proven square by square in [BoardExplanationTest].
 * Where a sentence reports a count, this file recomputes that count from [BoardExplanation] and
 * requires the reply to carry exactly it: the property under test is "the character speaks from
 * the real attack map", not the attack map itself.
 */
class GameExplanationTest {

    private val bridge = GameDialogueBridge()

    private fun board(side: PlayerColor, vararg at: Pair<Square, Piece>): BoardPosition =
        at.fold(XiangqiBoard.empty(side)) { position, (square, piece) -> position.withPiece(square, piece) }

    private fun red(kind: PieceKind) = Piece(PlayerColor.RED, kind)
    private fun black(kind: PieceKind) = Piece(PlayerColor.BLACK, kind)

    private fun session(
        position: BoardPosition,
        difficulty: String = SmartBoardEngine.NORMAL
    ): GameSessionState = reduceGame(
        GameSessionState(),
        GameEvent.Start(GameType.XIANGQI, 1L, position = position, difficulty = difficulty)
    )

    /** A rook guarding the rank is pinned to its general, so the cannon behind it is really free. */
    private fun pinnedPosition(withPin: Boolean): BoardPosition {
        val base = board(
            PlayerColor.RED,
            Square(4, 9) to red(PieceKind.GENERAL),
            Square(4, 5) to red(PieceKind.ROOK),
            Square(6, 5) to red(PieceKind.CANNON),
            Square(3, 0) to black(PieceKind.GENERAL),
            Square(5, 3) to black(PieceKind.HORSE)
        )
        return if (withPin) base.withPiece(Square(4, 0), black(PieceKind.ROOK)) else base
    }

    /** Nothing hangs here, and 俥 (0,5) -> (6,5) walks onto a square the black horse covers. */
    private fun hangingRig(): BoardPosition = board(
        PlayerColor.RED,
        Square(4, 9) to red(PieceKind.GENERAL),
        Square(0, 5) to red(PieceKind.ROOK),
        Square(3, 0) to black(PieceKind.GENERAL),
        Square(5, 3) to black(PieceKind.HORSE)
    )

    /** (0,6) would be the horse's square, (0,7) is not — so the scan must prefer the second. */
    private fun horseWatch(): BoardPosition = board(
        PlayerColor.RED,
        Square(4, 9) to red(PieceKind.GENERAL),
        Square(0, 5) to red(PieceKind.ROOK),
        Square(3, 0) to black(PieceKind.GENERAL),
        Square(2, 7) to black(PieceKind.HORSE)
    )

    // ---- threat report ----------------------------------------------------------------

    @Test
    fun threat_report_says_which_pieces_are_really_free() {
        val state = session(pinnedPosition(withPin = true))
        val facts = BoardExplanation.exposed(state.position, PlayerColor.RED)
        assertEquals(2, facts.size)
        assertEquals(2, facts.count { it.isUndefended })

        val reply = bridge.handle(state, "有哪些威胁").reply
        assertTrue("count must come from the attack map: $reply",
            reply.contains("红方有 2 个子正被攻击"))
        assertTrue(reply.contains("红方俥（第5列第6线）正被黑方車、黑方馬盯住，没人能吃回"))
        assertTrue(reply.contains("红方炮（第7列第6线）正被黑方馬盯住，没人能吃回"))
        assertTrue("both hang, so both must be named free: $reply",
            reply.contains("其中 2 个没人能吃回"))
        assertFalse("nothing is defended here: $reply", reply.contains("都有子护着"))
    }

    @Test
    fun a_defended_piece_is_reported_as_defended() {
        val state = session(pinnedPosition(withPin = false))
        val reply = bridge.handle(state, "有哪些威胁").reply
        assertTrue("the rook guards the cannon once the pin is gone: $reply",
            reply.contains("红方炮（第7列第6线）正被黑方馬盯住，1 个子能吃回"))
        assertTrue(reply.contains("红方俥（第5列第6线）正被黑方馬盯住，没人能吃回"))
        assertTrue("only the bare rook is free: $reply",
            reply.contains("其中 1 个没人能吃回：红方俥（第5列第6线）"))
        assertFalse(reply.contains("其中 2 个"))
    }

    // ---- the critique tail on a played move -------------------------------------------

    @Test
    fun a_normal_game_move_explains_the_hang_it_created() {
        val state = session(hangingRig())
        val moved = bridge.applySquareMove(state, Square(0, 5), Square(6, 5))
        assertTrue(moved.state.history.size == 1)
        assertTrue(
            "the played move must name both facts: ${moved.reply}",
            moved.reply.contains("这手：落点第7列第6线被黑方馬盯住；红方俥（第7列第6线）没人能吃回，属于白送。")
        )
        assertFalse("no extra tally at normal: ${moved.reply}", moved.reply.contains("有子护着"))
    }

    @Test
    fun the_easy_level_stays_quiet_and_hard_adds_only_facts() {
        val easy = bridge.applySquareMove(
            session(hangingRig(), SmartBoardEngine.EASY), Square(0, 5), Square(6, 5)
        )
        assertFalse("easy plays without a critique: ${easy.reply}", easy.reply.contains("这手："))

        val hard = bridge.applySquareMove(
            session(hangingRig(), SmartBoardEngine.HARD), Square(0, 5), Square(6, 5)
        )
        assertTrue(hard.reply.contains("落点第7列第6线被黑方馬盯住"))
        assertFalse(
            "every attacked red piece hangs here, so no guarded tally may appear: ${hard.reply}",
            hard.reply.contains("但有子护着")
        )
    }

    // ---- 「这步为什么不好」 -------------------------------------------------------------

    @Test
    fun why_names_the_square_walked_into_and_the_piece_left_free() {
        val moved = bridge.applySquareMove(session(hangingRig()), Square(0, 5), Square(6, 5))
        val why = bridge.handle(moved.state, "这步为什么不好")
        assertTrue(why.grounded)
        assertNull("a judgement must never touch the board", why.event)
        assertEquals("asking why leaves the session untouched", moved.state, why.state)
        assertTrue("reply: ${why.reply}", why.reply.contains("走到第7列第6线后被黑方馬盯住"))
        assertTrue("reply: ${why.reply}",
            why.reply.contains("这手让红方俥（第7列第6线）没人能吃回，属于白送"))
        assertTrue("reply: ${why.reply}",
            why.reply.contains("走完后己方 1 个子被盯住，其中 1 个没人能吃回。"))
    }

    @Test
    fun why_admits_when_the_rules_show_nothing_wrong() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val moved = bridge.handle(started.state, "走炮二平五")
        val why = bridge.handle(moved.state, "刚才那步走错了吗")
        assertTrue(why.grounded)
        assertTrue("reply: ${why.reply}", why.reply.contains("没有把子送到对方嘴里"))
        assertTrue("reply: ${why.reply}", why.reply.contains("按当前规则看不出问题"))
        // the absolute tally it quotes has to be the tally the rules really find
        val facts = BoardExplanation.exposed(moved.state.position, PlayerColor.RED)
        val free = facts.count { it.isUndefended }
        val note = when {
            facts.isEmpty() -> "走完后己方没有子被盯住。"
            free == 0 -> "走完后己方 ${facts.size} 个子被盯住，但都有子护着。"
            else -> "走完后己方 ${facts.size} 个子被盯住，其中 $free 个没人能吃回。"
        }
        assertTrue("reply must quote the real tally ($note): ${why.reply}", why.reply.contains(note))
    }

    @Test
    fun why_answers_for_the_humans_own_last_move_first() {
        // spectating hands both sides to the engine: the judgement still reads one real halfmove
        val state = session(hangingRig()).copy(playerColor = PlayerColor.WHITE)
        val moved = bridge.applySquareMove(state, Square(0, 5), Square(6, 5))
        val why = bridge.handle(moved.state, "上一步为什么不好")
        assertTrue("reply: ${why.reply}", why.reply.contains("属于白送"))
    }

    @Test
    fun why_without_a_move_says_so() {
        val empty = bridge.handle(session(hangingRig()), "这步为什么不好")
        assertTrue(empty.grounded)
        assertEquals("还没有走法可以评价。", empty.reply)
    }

    // ---- 「换个稳一点的走法」 ------------------------------------------------------------

    @Test
    fun safer_quotes_the_move_the_scan_actually_picked() {
        val position = horseWatch()
        val state = session(position)
        val result = bridge.handle(state, "换个稳一点的走法")
        assertTrue(result.grounded)
        assertNull(result.event)
        assertEquals("asking for an alternative must not move anything", state, result.state)

        val expected = XiangqiNotation.format(
            BoardMove(Square(0, 5), Square(0, 7), "", player = PlayerColor.RED), position
        )
        assertTrue("reply must quote the calm square ($expected): ${result.reply}",
            result.reply.contains("更稳的一手「$expected」"))
        assertTrue("reply: ${result.reply}",
            result.reply.contains("己方被盯住的子 0 → 0 个，之后没有白送的子。"))
        assertTrue("the disclaimer is part of the contract: ${result.reply}",
            result.reply.contains("非强度评级"))
        val named = Regex("更稳的一手「(.+?)」").find(result.reply)!!.groupValues[1]
        assertTrue(
            "the quoted move has to exist on this board",
            XiangqiRules.legalMoves(position, PlayerColor.RED).any {
                XiangqiNotation.format(it, position) == named
            }
        )
    }

    @Test
    fun safer_reports_the_hang_that_survives_the_alternative() {
        val state = session(pinnedPosition(withPin = true))
        val scan = BoardExplanation.safest(state.position, PlayerColor.RED, SmartBoardEngine.NORMAL)!!
        val result = bridge.handle(state, "有没有更稳的走法")
        assertTrue("reply: ${result.reply}",
            result.reply.contains("己方被盯住的子 ${scan.attackedBefore} → ${scan.attackedAfter} 个"))
        val free = scan.undefendedAfter.size
        assertTrue("reply: ${result.reply}",
            result.reply.contains(
                if (free == 0) "之后没有白送的子" else "之后仍有 $free 个没人能吃回"
            )
        )
    }

    @Test
    fun safer_waits_for_the_humans_turn() {
        val moved = bridge.applySquareMove(session(hangingRig()), Square(0, 5), Square(6, 5))
        val result = bridge.handle(moved.state, "换个稳一点的走法")
        assertFalse("not the human's turn, so no alternative may be offered: ${result.reply}",
            result.reply.contains("更稳的一手"))
        assertNull(result.event)
    }

    @Test
    fun safer_stops_when_the_board_is_settled() {
        val mated = board(
            PlayerColor.RED,
            Square(4, 9) to red(PieceKind.GENERAL),
            Square(3, 0) to black(PieceKind.GENERAL),
            Square(4, 6) to black(PieceKind.ROOK),
            Square(3, 7) to black(PieceKind.ROOK),
            Square(5, 5) to black(PieceKind.ROOK)
        )
        assertTrue(XiangqiRules.legalMoves(mated, PlayerColor.RED).isEmpty())
        val settled = XiangqiRules.outcome(mated)
        assertTrue("expected a settled mate, got $settled", settled !is GameOutcome.InProgress &&
            settled !is GameOutcome.Check)
        val state = session(mated).copy(outcome = settled)
        val result = bridge.handle(state, "换个稳一点的走法")
        assertEquals("棋局已经结束，没有可换的走法。可以「复盘」或「退出棋局」。", result.reply)
    }

    // ---- classification boundaries ------------------------------------------------------

    @Test
    fun judgement_words_do_not_swallow_moves_or_other_commands() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        val move = bridge.handle(started.state, "走炮二平五")
        assertTrue("a plain move must still move", move.event is GameEvent.ApplyMove)

        val threats = bridge.handle(started.state, "有哪些威胁")
        assertNull(threats.event)
        assertTrue("威胁 keeps its precedence: ${threats.reply}",
            threats.reply.contains("正被攻击") || threats.reply.contains("没有正被攻击"))

        val hint = bridge.handle(started.state, "给我提示")
        assertTrue("hint still answers: ${hint.reply}", hint.reply.contains("建议「"))
    }

    @Test
    fun a_lucky_claim_is_still_refused_not_critiqued() {
        val started = bridge.handle(GameSessionState(), "来一盘象棋")
        // 「不错」 carries 「错」: it used to read as a request to judge the last move
        val claimed = bridge.handle(started.state, "我运势不错这步一定赢")
        assertTrue("reply: ${claimed.reply}",
            claimed.reply.contains("局面") || claimed.reply.contains("合法走法"))
        assertFalse("a refusal must not turn into commentary: ${claimed.reply}",
            claimed.reply.contains("没有把子送到对方嘴里"))
        assertNull(claimed.event)

        val probe = bridge.handle(started.state, "马八进七这步能走吗")
        assertTrue("reply: ${probe.reply}", probe.reply.contains("可以走") || probe.reply.contains("合法"))
        assertNull("a legality probe must not consume the turn", probe.event)
    }

    // ---- honesty gates -------------------------------------------------------------------

    @Test
    fun no_in_game_reply_states_a_rating_or_a_probability() {
        val inputs = listOf(
            "有哪些威胁", "我有哪些子被威胁", "这步为什么不好", "刚才那步走错了吗",
            "换个稳一点的走法", "给我提示", "复盘", "悔棋", "重做",
            "难度换成困难", "观战", "执黑", "退出棋局", "保存棋局", "继续棋局",
            "战绩", "残局", "来一盘象棋", "今天天气怎么样", "随便看看"
        )
        val banned = Regex("胜率|等级分|Elo|评分|棋力|[0-9]+\\.[0-9]+%?")
        for (level in SmartBoardEngine.LEVELS) {
            val quiet = board(
                PlayerColor.RED,
                Square(3, 9) to red(PieceKind.GENERAL),
                Square(0, 7) to red(PieceKind.ROOK),
                Square(5, 0) to black(PieceKind.GENERAL),
                Square(8, 2) to black(PieceKind.ROOK)
            )
            val played = bridge.applySquareMove(session(quiet, level), Square(0, 7), Square(0, 6))
            // hand the turn back to red through the same rules the reducer uses, no search needed
            val state = reduceGame(
                played.state,
                GameEvent.ApplyMove(1L, BoardMove(Square(8, 2), Square(8, 3), "", player = PlayerColor.BLACK))
            )
            assertEquals(2, state.history.size)
            assertEquals(PlayerColor.RED, state.position.sideToMove)
            val replies = mutableListOf(
                played.reply,
                bridge.handle(state, "来一盘象棋，$level").reply
            )
            for (input in inputs) replies += bridge.handle(state, input).reply
            for (reply in replies) {
                assertTrue("[$level] reply must stay rating-free: $reply", banned.find(reply) == null)
            }
        }
    }

    @Test
    fun engine_commentary_stays_first_person_and_rating_free() = runTest {
        val banned = Regex("胜率|等级分|Elo|评分|棋力|[0-9]+\\.[0-9]+%?")
        for (level in SmartBoardEngine.LEVELS) {
            val state = session(hangingRig(), level)
            val moved = bridge.applySquareMove(state, Square(0, 5), Square(6, 5))
            assertTrue(moved.awaitEngine)
            val reply = bridge.engineReply(moved.state)
            assertTrue("[$level] ${reply.reply}", reply.reply.startsWith("我走「"))
            assertTrue("[$level] ${reply.reply}", banned.find(reply.reply) == null)
        }
    }

    @Test
    fun two_bridges_explain_the_same_board_identically() {
        val other = GameDialogueBridge()
        val inputs = listOf(
            "有哪些威胁" to pinnedPosition(withPin = true),
            "这步为什么不好" to hangingRig(),
            "换个稳一点的走法" to horseWatch()
        )
        for ((input, position) in inputs) {
            val first = session(position)
            val moved = if (input == "这步为什么不好") {
                bridge.applySquareMove(first, Square(0, 5), Square(6, 5)).state
            } else {
                first
            }
            val mine = bridge.handle(moved, input).reply
            val twice = bridge.handle(moved, input).reply
            val theirs = other.handle(moved, input).reply
            assertEquals("explanations must be deterministic", mine, twice)
            assertEquals("explanations must not depend on the bridge instance", mine, theirs)
        }
    }
}
