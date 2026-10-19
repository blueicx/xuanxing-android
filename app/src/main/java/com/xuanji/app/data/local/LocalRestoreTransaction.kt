package com.xuanji.app.data.local

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** No profile/chat data in the log. The recovery journal stays private in DataStore. */
class LocalRestoreTransaction(private val bridge: PreferenceBridge) {
    suspend fun commit(before: Map<String, String?>, changes: Map<String, String?>): Boolean = mutex.withLock {
        require(before.keys.containsAll(changes.keys) && before.keys.all(::allowedKey))
        if (!recoverUnlocked()) return@withLock false
        if (before.any { (key, value) -> bridge.read(key) != value }) return@withLock false
        val journal = JsonObject().apply {
            addProperty("version", 1)
            addProperty("atomic", bridge.atomicBatch)
            add("before", JsonObject().apply { before.forEach { (key, value) -> addProperty(key, value) } })
        }.toString()
        require(journal.toByteArray().size <= MAX_JOURNAL_BYTES)
        bridge.write(JOURNAL_KEY, journal)
        try {
            val committed = bridge.compareAndWriteBatch(before, changes + mapOf(
                LOG_KEY to log("completed", changes.size), JOURNAL_KEY to null
            ))
            if (!committed) bridge.delete(JOURNAL_KEY) // CAS rejected: target data was not touched.
            committed
        } catch (error: Throwable) {
            withContext(NonCancellable) { recoverUnlocked() }
            if (error is kotlinx.coroutines.CancellationException) throw error
            false
        }
    }

    suspend fun recover(): Boolean = mutex.withLock { recoverUnlocked() }

    private suspend fun recoverUnlocked(): Boolean {
        val raw = bridge.read(JOURNAL_KEY) ?: return true
        return runCatching {
            require(raw.toByteArray().size <= MAX_JOURNAL_BYTES)
            val root = JsonParser.parseString(raw).asJsonObject
            require(root.get("version").asInt == 1)
            val before = root.getAsJsonObject("before").entrySet().associate { (key, value) ->
                require(allowedKey(key))
                key to if (value.isJsonNull) null else value.asString
            }
            require(before.size <= 32)
            // A production DataStore commit is atomic: a remaining journal means commit did not finish.
            val rollback = if (root.get("atomic")?.asBoolean == true) emptyMap() else before
            check(bridge.compareAndWriteBatch(emptyMap(), rollback + mapOf(
                LOG_KEY to log("rolled_back", before.size), JOURNAL_KEY to null
            )))
            true
        }.getOrDefault(false)
    }

    private fun log(status: String, count: Int): String = JsonObject().apply {
        addProperty("status", status); addProperty("sections", count); addProperty("at", System.currentTimeMillis())
    }.toString()

    companion object {
        const val JOURNAL_KEY = "local_restore_pending_v1"
        const val LOG_KEY = "local_restore_log_v1"
        private const val MAX_JOURNAL_BYTES = 8_000_000
        private val mutex = Mutex()
        private val allowedPrefixes = listOf("reading_favorites_", "action_feedback_", "companion_game_", "game_save_", "food_preference_", "activity_preference_", "daily_life_")
        private fun allowedKey(key: String) = key.length <= 200 && allowedPrefixes.any { key.startsWith(it) }
    }
}

/** Runs ordinary store codecs against an overlay, with no writes to the user's data. */
internal class RestoreStagingBridge(private val base: PreferenceBridge) : PreferenceBridge {
    val before = linkedMapOf<String, String?>()
    val changes = linkedMapOf<String, String?>()
    override suspend fun read(key: String): String? {
        if (changes.containsKey(key)) return changes[key]
        if (!before.containsKey(key)) before[key] = base.read(key)
        return before[key]
    }
    override suspend fun write(key: String, value: String) { read(key); changes[key] = value }
    override suspend fun delete(key: String) { read(key); changes[key] = null }
}
