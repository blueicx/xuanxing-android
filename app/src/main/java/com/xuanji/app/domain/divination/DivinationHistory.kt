package com.xuanji.app.domain.divination

/** Local, user-initiated history entry. The question is stored only as a short preview. */
data class DivinationHistoryEntry(
    val id: String,
    val system: String,
    val dateKey: String,
    val spread: String,
    val summary: String,
    val algorithmVersion: String,
    val createdAtEpochMs: Long
)

interface DivinationHistoryStore {
    suspend fun list(profileKey: String, limit: Int = 30): List<DivinationHistoryEntry>
    suspend fun append(profileKey: String, entry: DivinationHistoryEntry)
    suspend fun remove(profileKey: String, id: String)
    suspend fun clear(profileKey: String)
}

/** JVM-friendly store used by tests and as a safe fallback when persistence is unavailable. */
class InMemoryDivinationHistoryStore : DivinationHistoryStore {
    private val values = linkedMapOf<String, MutableList<DivinationHistoryEntry>>()

    override suspend fun list(profileKey: String, limit: Int): List<DivinationHistoryEntry> =
        values[profileKey].orEmpty().asReversed().take(limit.coerceAtLeast(0))

    override suspend fun append(profileKey: String, entry: DivinationHistoryEntry) {
        val entries = values.getOrPut(profileKey) { mutableListOf() }
        entries.removeAll { it.id == entry.id }
        entries += entry
        while (entries.size > 100) entries.removeAt(0)
    }

    override suspend fun remove(profileKey: String, id: String) {
        values[profileKey]?.removeAll { it.id == id }
    }

    override suspend fun clear(profileKey: String) {
        values.remove(profileKey)
    }
}
