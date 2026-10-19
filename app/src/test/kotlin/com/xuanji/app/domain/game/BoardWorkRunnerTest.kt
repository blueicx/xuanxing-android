package com.xuanji.app.domain.game

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class BoardWorkRunnerTest {
    @Test fun even_noncooperative_old_work_cannot_publish_after_cancel_or_replace() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val runner = BoardWorkRunner(scope, Dispatchers.Unconfined, Dispatchers.Unconfined)
        val release = CompletableDeferred<Unit>()
        val outputs = mutableListOf<String>()
        var finished = 0
        runner.launch(onFinished = { finished++ }) {
            withContext(NonCancellable) { release.await(); publish { outputs += "old" } }
        }
        runner.cancel()
        runner.launch(onFinished = { finished++ }) { publish { outputs += "new" } }
        release.complete(Unit)
        scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        assertEquals(listOf("new"), outputs)
        assertEquals(1, finished)
        scope.cancel()
    }

    @Test fun failure_resets_current_spinner_without_publishing_partial_result() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val runner = BoardWorkRunner(scope, Dispatchers.Unconfined, Dispatchers.Unconfined)
        var finished = false
        var error: Throwable? = null
        runner.launch(onFinished = { finished = true }, onError = { error = it }) { throw IllegalStateException("synthetic failure") }
        scope.coroutineContext[Job]!!.children.toList().forEach { it.join() }
        assertTrue(finished)
        assertTrue(error is IllegalStateException)
        scope.cancel()
    }
}
