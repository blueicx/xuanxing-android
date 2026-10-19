package com.xuanji.app.domain.game

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

class BoardWorkRunner(private val scope: CoroutineScope,
    private val computation: CoroutineDispatcher = Dispatchers.Default,
    private val publication: CoroutineDispatcher = Dispatchers.Main.immediate) {
    private val generation = AtomicLong()
    private var job: Job? = null
    class Ticket(private val dispatcher: CoroutineDispatcher, private val isCurrent: () -> Boolean) {
        suspend fun publish(action: () -> Unit) { withContext(dispatcher) {
            currentCoroutineContext().ensureActive()
            if (isCurrent()) action()
        } }
    }
    fun cancel() { generation.incrementAndGet(); job?.cancel(); job = null }
    fun launch(onFinished: () -> Unit, onError: (Throwable) -> Unit = {}, work: suspend Ticket.() -> Unit) {
        cancel()
        val token = generation.get()
        job = scope.launch(computation) {
            val ticket = Ticket(publication) { generation.get() == token }
            try { ticket.work() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { ticket.publish { onError(error) } }
            finally { withContext(NonCancellable + publication) {
                if (generation.get() == token) onFinished()
            } }
        }
    }
}
