package com.xuanji.app.data.local

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.xuanji.app.data.model.TestRecord
import com.xuanji.app.domain.action.StableScore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProfileTestHistoryStore(private val bridge: PreferenceBridge) {
    suspend fun load(owner: String): List<TestRecord> = decode(bridge.read(key(owner)), owner)
    suspend fun add(owner: String, record: TestRecord, expectedEpoch: Long? = null) = LocalDataEraseFence.writes.withLock { mutex.withLock mutation@{
        check(expectedEpoch == null || expectedEpoch == epoch(owner)) { "test_history_write_expired" }
        require(valid(record))
        val before = load(owner)
        if (record in before) return@mutation
        check(before.size < 100) { "test_history_capacity" }
        bridge.write(key(owner), Gson().toJson(mapOf("version" to 1, "owner" to StableScore.sha256(owner), "records" to (listOf(record) + before))))
    } }
    suspend fun clear(owner: String) = mutex.withLock {
        bridge.delete(key(owner))
        epochs.compute(StableScore.sha256(owner)) { _, value -> (value ?: 0) + 1 }
    }
    companion object {
        private val mutex = Mutex()
        private val epochs = java.util.concurrent.ConcurrentHashMap<String, Long>()
        private val globalEpoch = java.util.concurrent.atomic.AtomicLong(0)
        fun epoch(owner: String): Long = (epochs[StableScore.sha256(owner)] ?: 0L) + globalEpoch.get()
        internal fun invalidateAll() { globalEpoch.incrementAndGet() }
        fun key(owner: String): String { require(owner.isNotBlank()); return "profile_test_history_" + StableScore.sha256(owner) }
        private fun valid(record: TestRecord): Boolean = record.testName.length in 1..100 && record.category.length in 1..40 &&
            record.resultCode.length in 1..500 && record.resultName.length <= 500 && record.date.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) &&
            record.scoringVersion.length in 1..100 && record.rawScores.orEmpty().size <= 40 && record.rawScores.orEmpty().all { it.key.length in 1..60 && it.value in -1000..1000 }
        fun decode(raw: String?, owner: String): List<TestRecord> {
            if (raw == null) return emptyList()
            require(raw.length <= 512_000)
            val json = JsonParser.parseString(raw).asJsonObject
            require(json["version"].asInt == 1 && json["owner"].asString == StableScore.sha256(owner))
            val array = json["records"].asJsonArray
            require(array.size() <= 100)
            return array.map { Gson().fromJson(it, TestRecord::class.java).also { record -> require(valid(record)) } }
        }
    }
}
