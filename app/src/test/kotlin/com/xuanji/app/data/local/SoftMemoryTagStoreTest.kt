package com.xuanji.app.data.local

import com.xuanji.app.domain.SoftMemorySource
import com.xuanji.app.domain.SoftMemoryTag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftMemoryTagStoreTest {
    private class FakePreferenceBridge : PreferenceBridge {
        val values = LinkedHashMap<String, String>()
        val deleted = mutableListOf<String>()

        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { deleted += key; values.remove(key) }
    }

    @Test
    fun inferred_tags_round_trip_and_revoke_without_touching_other_keys() = runTest {
        val bridge = FakePreferenceBridge()
        val store = SoftMemoryTagStore(bridge)
        bridge.values["talk_memory_keep"] = "other"
        val tag = SoftMemoryTag("work", "topic", "最近常聊", "工作", SoftMemorySource.Inferred, "2026-09-12", "2026-09-12")

        store.upsertInferred("profile-a", tag)
        assertEquals(listOf(tag), store.read("profile-a").tags)

        val revoked = store.revoke("profile-a", "work")
        assertTrue(revoked.tags.single().revoked)
        store.clear("profile-a")
        assertEquals(listOf(store.memoryKey("profile-a")), bridge.deleted)
        assertEquals("other", bridge.values["talk_memory_keep"])
    }

    @Test
    fun corrupt_payload_is_reported_without_blocking_a_new_write() = runTest {
        val bridge = FakePreferenceBridge()
        val store = SoftMemoryTagStore(bridge)
        bridge.values[store.memoryKey("profile-a")] = "broken"

        assertTrue(store.read("profile-a").unreadable)
        val tag = SoftMemoryTag("love", "topic", "最近常聊", "感情", SoftMemorySource.Inferred, "2026-09-12", "2026-09-12")
        assertEquals(listOf(tag), store.upsertInferred("profile-a", tag).tags)
        assertTrue(!store.read("profile-a").unreadable)
    }
}
