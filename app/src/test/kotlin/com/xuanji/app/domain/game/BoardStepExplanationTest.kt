package com.xuanji.app.domain.game

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BoardStepExplanationTest {
    @Test fun endgame_hint_explains_actual_piece_and_rule_checked_target_without_playing() = runBlocking {
        val bridge = GameDialogueBridge()
        val state = bridge.handle(GameSessionState(), "开第 1 关").state
        val suggestion = SmartBoardEngine(state.difficulty).bestMove(state.position, state.position.sideToMove, state.sessionToken) as EngineResult.Move
        val move = suggestion.turn.move
        val response = bridge.handle(state, "提示")
        assertEquals(state, response.state)
        assertTrue(response.reply.contains("第 1 步"))
        assertTrue(response.reply.contains(XiangqiNotation.coordinate(move.to)))
        assertTrue(response.reply.contains("合法走法"))
    }

    @Test fun replay_explains_real_before_and_after_frames() {
        val bridge = GameDialogueBridge()
        val start = bridge.handle(GameSessionState(), "来一盘象棋").state
        val played = bridge.handle(start, "炮二平五").state
        val review = bridge.handle(played, "复盘")
        assertEquals(played, review.state)
        assertTrue(review.reply.contains("第 1 步"))
        assertTrue(review.reply.contains(XiangqiNotation.coordinate(played.history.last().to)))
        assertTrue(review.reply.contains("合法走法"))
    }
}
