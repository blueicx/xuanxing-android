package com.xuanji.app.data.local

import com.xuanji.app.domain.RecollectionEntry
import com.xuanji.app.domain.RecollectionKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 用内存假桥把「存进去了」「清掉了」「没碰别人的键」真的跑一遍：
 * 这些结论只能来自一次真实的读回，不能来自编解码本身。
 */
class ConversationMemoryStoreTest {

    private class FakePreferenceBridge : PreferenceBridge {
        val values = LinkedHashMap<String, String>()
        val deleted = ArrayList<String>()

        override suspend fun read(key: String): String? = values[key]

        override suspend fun write(key: String, value: String) {
            values[key] = value
        }

        override suspend fun delete(key: String) {
            deleted += key
            values.remove(key)
        }
    }

    private val keyless = ConversationMemoryStore(FakePreferenceBridge())

    private fun input(text: String, intent: String = "career", date: String = "2026-08-30") =
        RecollectionEntry(date, RecollectionKind.USER_INPUT, text, intent)

    // ---- 键：一个命盘一个键，且不与任何已有功能同前缀 --------------------------------

    @Test
    fun a_key_binds_one_profile_and_nothing_else() {
        assertEquals(
            "talk_memory_104c4491498717b382d124bbe81ae4e87648fbab08ecba7e24135ead5fc97554",
            keyless.memoryKey("profile-a")
        )
        assertTrue(keyless.memoryKey("profile-a") != keyless.memoryKey("profile-b"))
    }

    @Test
    fun the_digest_is_utf8_not_the_platform_default() {
        assertEquals(
            "9abea51c67eeceb101ad2537567433c7e365b35524ac7103c41a553929f034e3",
            ConversationMemoryStore.fingerprint("命盘A")
        )
    }

    @Test
    fun the_talk_key_does_not_borrow_another_names_prefix() {
        val key = keyless.memoryKey("profile-a")
        listOf("game_save_", "game_record_", "mystic_visit_", "card_layout_", "user_profile").forEach {
            assertFalse("<$key> must not start with $it", key.startsWith(it))
        }
    }

    // ---- 读写 ----------------------------------------------------------------------

    @Test
    fun what_was_remembered_comes_back_from_the_bridge() = runTest {
        val bridge = FakePreferenceBridge()
        val memory = ConversationMemoryStore(bridge)
        assertTrue(memory.facts("profile-a").isEmpty)

        memory.remember("profile-a", input("这周要不要换工作"))
        memory.remember("profile-a", input("先做最小一步", "composite", "2026-08-31"))

        assertEquals(listOf(keyless.memoryKey("profile-a")), bridge.values.keys.toList())
        val stored = memory.load("profile-a")!!
        assertEquals(listOf("这周要不要换工作", "先做最小一步"), stored.entries.map { it.text })
        val facts = memory.facts("profile-a")
        assertEquals(listOf("career", "composite"), facts.userTopics)
        assertEquals("2026-08-31", facts.lastDate)
    }

    @Test
    fun profiles_do_not_see_each_other() = runTest {
        val memory = ConversationMemoryStore(FakePreferenceBridge())
        memory.remember("profile-a", input("想换工作"))
        assertEquals(listOf("career"), memory.facts("profile-a").userTopics)
        assertTrue(memory.facts("profile-b").isEmpty)
        assertTrue(memory.load("profile-b")!!.isEmpty)
    }

    // ---- 清除：只删自己那一个键 ------------------------------------------------------

    @Test
    fun clearing_removes_exactly_the_one_key_it_owns() = runTest {
        val bridge = FakePreferenceBridge()
        val memory = ConversationMemoryStore(bridge)
        val fingerprint = ConversationMemoryStore.fingerprint("profile-a")
        val untouched = listOf(
            "game_save_$fingerprint",
            "game_record_$fingerprint",
            "mystic_visit_$fingerprint",
            keyless.memoryKey("profile-b")
        )
        untouched.forEach { bridge.values[it] = "别人的记录" }

        memory.remember("profile-a", input("想换工作"))
        memory.clear("profile-a")

        assertEquals(listOf(keyless.memoryKey("profile-a")), bridge.deleted)
        assertEquals(untouched, untouched.filter { bridge.values.containsKey(it) })
        assertTrue(memory.facts("profile-a").isEmpty)
        assertFalse(memory.facts("profile-a").unreadable)
    }

    // ---- 诚实降级 ------------------------------------------------------------------

    @Test
    fun a_corrupt_blob_is_admitted_until_something_new_replaces_it() = runTest {
        val bridge = FakePreferenceBridge()
        val memory = ConversationMemoryStore(bridge)
        bridge.values[keyless.memoryKey("profile-a")] = "半条被写坏的记录"

        assertTrue(memory.facts("profile-a").unreadable)
        assertNull(memory.load("profile-a"))

        // 空内容不该顺手把现场抹掉
        memory.remember("profile-a", input("   "))
        assertTrue(memory.facts("profile-a").unreadable)

        memory.remember("profile-a", input("想换工作"))
        assertFalse(memory.facts("profile-a").unreadable)
        assertEquals(listOf("career"), memory.facts("profile-a").userTopics)
    }

    @Test
    fun the_local_cap_and_its_count_survive_a_real_write() = runTest {
        val memory = ConversationMemoryStore(FakePreferenceBridge())
        repeat(25) { index ->
            memory.remember("profile-a", input("记录${index.toString().padStart(2, '0')}"))
        }
        val stored = memory.load("profile-a")!!
        assertEquals(20, stored.entries.size)
        assertEquals(5, stored.dropped)
        assertEquals(5, memory.facts("profile-a").dropped)
    }

    @Test
    fun nothing_is_written_when_the_user_said_nothing() = runTest {
        val bridge = FakePreferenceBridge()
        val memory = ConversationMemoryStore(bridge)
        memory.remember("profile-a", input("   "))
        memory.remember("profile-a", RecollectionEntry("2026-08-30", RecollectionKind.USER_CHOICE, "   "))
        assertTrue(bridge.values.isEmpty())
    }
}
