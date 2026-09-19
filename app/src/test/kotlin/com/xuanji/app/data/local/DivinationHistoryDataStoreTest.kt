package com.xuanji.app.data.local

import com.xuanji.app.domain.divination.DivinationHistoryEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DivinationHistoryDataStoreTest {
    private class FakeBridge : PreferenceBridge {
        val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun history_is_profile_namespaced_and_clearable() = runBlocking {
        val bridge = FakeBridge()
        val store = DataStoreDivinationHistoryStore(bridge)
        val entry = DivinationHistoryEntry("1", "tarot", "2026-09-19", "three", "牌面摘要", "x3-v2", 1L)
        store.append("profile-a", entry)
        assertEquals(listOf(entry), store.list("profile-a"))
        assertTrue(store.list("profile-b").isEmpty())
        store.clear("profile-a")
        assertTrue(store.list("profile-a").isEmpty())
    }
}
