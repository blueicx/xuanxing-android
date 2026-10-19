package com.xuanji.app.domain.game

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class GameHintCancellationTest {
    @Test fun hint_search_inherits_the_callers_cancellation_instead_of_finishing_an_obsolete_tree() = runBlocking {
        val job = Job()
        var nodes = 0
        val bridge = GameDialogueBridge(SmartBoardEngine(SmartBoardEngine.HARD) { nodes++; job.cancel() })
        val state = bridge.handle(GameSessionState(), "开第 1 关").state
        try {
            withContext(job) { bridge.handle(state, "提示", currentCoroutineContext()) }
            fail("Cancelled hints cannot return a new reply")
        } catch (_: CancellationException) { assertEquals(1, nodes) }
    }
}
