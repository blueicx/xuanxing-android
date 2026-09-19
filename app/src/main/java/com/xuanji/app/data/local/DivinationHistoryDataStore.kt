package com.xuanji.app.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuanji.app.domain.divination.DivinationHistoryEntry
import com.xuanji.app.domain.divination.DivinationHistoryStore
import java.security.MessageDigest

/** JSON-backed local history. The profile key is hashed so raw birth data never becomes a preference key. */
class DataStoreDivinationHistoryStore(private val bridge: PreferenceBridge) : DivinationHistoryStore {
    private val gson = Gson()
    private val type = object : TypeToken<List<DivinationHistoryEntry>>() {}.type

    override suspend fun list(profileKey: String, limit: Int): List<DivinationHistoryEntry> {
        val raw = bridge.read(key(profileKey)) ?: return emptyList()
        return runCatching { gson.fromJson<List<DivinationHistoryEntry>>(raw, type).orEmpty() }
            .getOrDefault(emptyList())
            .asReversed()
            .take(limit.coerceAtLeast(0))
    }

    override suspend fun append(profileKey: String, entry: DivinationHistoryEntry) {
        val current = list(profileKey, 100).asReversed().toMutableList()
        current.removeAll { it.id == entry.id }
        current += entry
        bridge.write(key(profileKey), gson.toJson(current.takeLast(100)))
    }

    override suspend fun remove(profileKey: String, id: String) {
        val current = list(profileKey, 100).asReversed().filterNot { it.id == id }
        if (current.isEmpty()) bridge.delete(key(profileKey)) else bridge.write(key(profileKey), gson.toJson(current))
    }

    override suspend fun clear(profileKey: String) = bridge.delete(key(profileKey))

    fun key(profileKey: String): String = KEY_PREFIX + fingerprint(profileKey)

    companion object {
        const val KEY_PREFIX = "divination_history_"
        fun fingerprint(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

fun android.content.Context.divinationHistoryStore(): DataStoreDivinationHistoryStore =
    DataStoreDivinationHistoryStore(DataStorePreferenceBridge(this))
