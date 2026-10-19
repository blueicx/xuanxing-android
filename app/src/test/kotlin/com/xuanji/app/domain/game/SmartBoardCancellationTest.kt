package com.xuanji.app.domain.game

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SmartBoardCancellationTest {
    @Test fun normal_dense_board_has_a_deterministic_node_budget_and_returns_a_legal_move() = runBlocking {
        val position = XiangqiBoard.initial().copy(sideToMove = PlayerColor.BLACK)
        var nodes = 0
        val result = SmartBoardEngine(SmartBoardEngine.NORMAL) { nodes++ }.bestMove(position, PlayerColor.BLACK, 9)
        assertTrue("Dense positions must not exhaust the full obsolete tree", nodes <= 256)
        assertTrue(XiangqiRules.apply(position, (result as EngineResult.Move).turn.move) is RuleResult.Applied)
        assertEquals(result, SmartBoardEngine(SmartBoardEngine.NORMAL).bestMove(position, PlayerColor.BLACK, 9))
    }
    @Test fun actual_search_reports_nodes_without_changing_the_move() = runBlocking {
        val position = EndgameCatalog.ALL.first().position()
        var nodes = 0
        val measured = SmartBoardEngine(SmartBoardEngine.HARD) { nodes++ }.bestMove(position, position.sideToMove, 7)
        val normal = SmartBoardEngine(SmartBoardEngine.HARD).bestMove(position, position.sideToMove, 7)
        assertEquals(normal, measured)
        assertTrue("Counters must describe the actual search, not an opening-book shortcut", nodes > 0)
    }

    @Test fun cancellation_inside_search_stops_before_another_node_is_evaluated() = runBlocking {
        val searchJob = Job()
        var nodes = 0
        try {
            withContext(searchJob) {
                val position = EndgameCatalog.ALL.first().position()
                SmartBoardEngine(SmartBoardEngine.HARD) { nodes++; searchJob.cancel() }
                    .bestMove(position, position.sideToMove, 8)
            }
            fail("Cancelled computation cannot publish a move")
        } catch (_: CancellationException) {
            assertEquals("CPU search must cooperate with cancellation, not finish the obsolete tree", 1, nodes)
        }
    }
}
