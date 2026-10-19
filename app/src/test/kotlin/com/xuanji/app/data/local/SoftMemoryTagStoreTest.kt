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
    fun corrupt_payload_is_preserved_until_explicit_clear() = runTest {
        val bridge = FakePreferenceBridge()
        val store = SoftMemoryTagStore(bridge)
        bridge.values[store.memoryKey("profile-a")] = "broken"

        assertTrue(store.read("profile-a").unreadable)
        val tag = SoftMemoryTag("love", "topic", "最近常聊", "感情", SoftMemorySource.Inferred, "2026-09-12", "2026-09-12")
        assertTrue(store.upsertInferred("profile-a", tag).unreadable)
        assertEquals("broken", bridge.values[store.memoryKey("profile-a")])
        store.clear("profile-a")
        assertEquals(listOf(tag), store.upsertInferred("profile-a", tag).tags)
    }

    @Test fun edited_and_revoked_tags_are_not_replaced_by_future_detection() = runTest {
        val store = SoftMemoryTagStore(FakePreferenceBridge())
        val original = SoftMemoryTag("work", "topic", "最近常聊", "工作", SoftMemorySource.Inferred, "2026-10-16", "2026-10-16")
        store.upsertInferred("a", original)
        val edited = original.copy(value = "学习", expiresAtDate = "2026-10-23")
        store.edit("a", edited)
        store.upsertInferred("a", original)
        assertEquals("学习", store.read("a").tags.single().value)
        assertEquals(SoftMemorySource.UserConfirmed, store.read("a").tags.single().source)
        assertEquals("2026-10-23", store.read("a").tags.single().expiresAtDate)
        store.revoke("a", "work")
        store.upsertInferred("a", original)
        assertTrue(store.read("a").tags.single().revoked)
        assertTrue(store.read("b").tags.isEmpty())
    }
}
