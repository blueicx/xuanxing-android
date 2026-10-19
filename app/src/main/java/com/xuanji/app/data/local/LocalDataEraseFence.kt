package com.xuanji.app.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A pending private-answer write must not resurrect data after explicit complete erase. */
object LocalDataEraseFence {
    internal val writes = Mutex()
    suspend fun erase(block: suspend () -> Unit) = writes.withLock {
        block()
        QuestionnaireDraftStore.invalidateAll()
        ProfileTestHistoryStore.invalidateAll()
    }
}
