package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 长期记忆的编解码契约：只有用户自己说过/选过/落定的结果能进来；
 * 读不出来就承认读不出来，挤掉了就承认挤掉了。
 */
class MysticRecollectionTest {

    private fun memoryOf(vararg entries: RecollectionEntry): ConversationMemory =
        entries.fold(ConversationMemory()) { acc, entry -> RecollectionCodec.append(acc, entry) }

    private fun inputs(count: Int): List<RecollectionEntry> = (1..count).map { index ->
        RecollectionEntry("2026-08-01", RecollectionKind.USER_INPUT, "记录${index.toString().padStart(2, '0')}")
    }

    // ---- 红线：可存的 kind 由类型决定 ------------------------------------------------

    @Test
    fun generated_copy_has_no_kind_it_could_be_stored_under() {
        assertEquals(
            listOf("user_input", "user_choice", "settled_game_result"),
            RecollectionKind.values().map { it.wire }
        )
        listOf("generated", "assistant", "guide", "opening", "rhythm", "game", "guest", "ask", "handoff")
            .forEach { assertTrue("$it must stay unstorable", RecollectionKind.fromWire(it) == null) }
    }

    @Test
    fun an_entry_needs_something_the_user_actually_said() {
        assertNull(RecollectionCodec.entryOf("2026-09-01", RecollectionKind.USER_INPUT, "   "))
        assertNull(RecollectionCodec.entryOf("2026-09-01", RecollectionKind.USER_INPUT, "\u200B\uFEFF"))
        assertTrue(ConversationMemory().isEmpty)
        assertEquals(ConversationMemory(), RecollectionCodec.append(ConversationMemory(),
            RecollectionEntry("2026-09-01", RecollectionKind.USER_INPUT, "")))
    }

    // ---- 文本裁剪：只裁不改 ----------------------------------------------------------

    @Test
    fun text_is_normalized_not_reworded() {
        assertEquals("今天 加班 很累", RecollectionCodec.cleanText("今天\u00A0\u00A0加班\n\n很累"))
        assertEquals("你好", RecollectionCodec.cleanText("你\u200B\uFEFF好"))
        val long = "啊".repeat(RecollectionCodec.MAX_TEXT_CODEPOINTS + 5)
        val cleaned = RecollectionCodec.cleanText(long)
        assertEquals(RecollectionCodec.MAX_TEXT_CODEPOINTS + 1, cleaned.length)
        assertTrue(cleaned.endsWith("…"))
    }

    @Test
    fun only_iso_shaped_date_keys_survive() {
        assertEquals("2026-09-01", RecollectionCodec.cleanDateKey("2026-09-01"))
        assertEquals("", RecollectionCodec.cleanDateKey("昨天"))
        assertEquals("", RecollectionCodec.cleanDateKey("2026-9-1"))
        assertEquals("", RecollectionCodec.entryOf("昨天", RecollectionKind.USER_INPUT, "换工作")!!.dateKey)
    }

    // ---- 溢出与去重 ------------------------------------------------------------------

    @Test
    fun the_cap_drops_the_oldest_and_says_so() {
        val memory = RecollectionCodec.reduced(inputs(25))
        assertEquals(RecollectionCodec.MAX_ENTRIES, memory.entries.size)
        assertEquals(5, memory.dropped)
        assertEquals("记录06", memory.entries.first().text)
        assertEquals("记录25", memory.entries.last().text)
    }

    @Test
    fun appending_one_at_a_time_reaches_the_same_place() {
        val memory = inputs(25).fold(ConversationMemory()) { acc, entry -> RecollectionCodec.append(acc, entry) }
        assertEquals(RecollectionCodec.reduced(inputs(25)), memory)
    }

    @Test
    fun the_same_words_are_not_kept_twice_and_nothing_is_lost_for_it() {
        val entry = RecollectionEntry("2026-08-30", RecollectionKind.USER_INPUT, "想换工作", "career")
        val memory = memoryOf(entry, entry)
        assertEquals(1, memory.entries.size)
        assertEquals(0, memory.dropped)
        // same words, different kind is still two records
        assertEquals(2, memoryOf(entry, entry.copy(kind = RecollectionKind.USER_CHOICE)).entries.size)
    }

    // ---- 编解码 ----------------------------------------------------------------------

    @Test
    fun encode_decode_round_trips_every_field() {
        val memory = memoryOf(
            RecollectionEntry("2026-08-30", RecollectionKind.USER_INPUT, "这周要不要换工作", "Career"),
            RecollectionEntry("2026-08-31", RecollectionKind.USER_CHOICE, "先做最小一步", "composite"),
            RecollectionEntry("2026-09-01", RecollectionKind.SETTLED_GAME_RESULT, "象棋·胜", "game")
        )
        val json = RecollectionCodec.encode(memory)
        assertEquals(memory, RecollectionCodec.decode(json))
        assertEquals(json, RecollectionCodec.encode(RecollectionCodec.decode(json)!!))
        // the eviction count is part of the record, not a per-process memory
        assertEquals(memory.dropped, RecollectionCodec.decode(json)!!.dropped)
    }

    @Test
    fun an_eviction_history_survives_the_round_trip() {
        val memory = inputs(25).fold(ConversationMemory()) { acc, entry -> RecollectionCodec.append(acc, entry) }
        val restored = RecollectionCodec.decode(RecollectionCodec.encode(memory))!!
        assertEquals(5, restored.dropped)
        assertEquals(memory, restored)
    }

    @Test
    fun corrupt_blobs_come_back_null_instead_of_throwing() {
        listOf(
            "", "   ", "not json", "{", "}", "[]", "null", "5",
            "{\"version\":2,\"entries\":[]}",
            "{\"entries\":5}",
            "{\"entries\":{}}"
        ).forEach { assertTrue("must reject <$it>", RecollectionCodec.decode(it) == null) }
    }

    @Test
    fun unreadable_entries_are_counted_not_silently_dropped() {
        val json = """
            {"version":1,"dropped":0,"entries":[
              "一个字符串不是记录",
              {"dateKey":"2026-08-30","kind":"user_input","text":"想换工作","intent":"career"},
              {"dateKey":"2026-08-30","kind":"assistant_reply","text":"这是角色自己写的"},
              {"dateKey":"2026-08-31","kind":"user_input","text":"   "},
              {"kind":"user_choice","text":"选A"}
            ]}
        """.trimIndent()
        val memory = RecollectionCodec.decode(json)!!
        assertEquals(2, memory.entries.size)
        assertEquals(3, memory.dropped)
        assertEquals("", memory.entries[1].dateKey)
        assertFalse(memory.entries.any { it.text.contains("角色自己写") })
    }

    // ---- 召回事实 --------------------------------------------------------------------

    @Test
    fun facts_only_report_what_can_be_counted() {
        val memory = memoryOf(
            RecollectionEntry("2026-08-30", RecollectionKind.USER_INPUT, "这周要不要换工作", "career"),
            RecollectionEntry("2026-08-31", RecollectionKind.USER_CHOICE, "先做最小一步", "composite"),
            RecollectionEntry("2026-09-01", RecollectionKind.SETTLED_GAME_RESULT, "象棋·胜", "game"),
            RecollectionEntry("2026-09-01", RecollectionKind.SETTLED_GAME_RESULT, "象棋·负", "game")
        )
        val facts = RecollectionCodec.facts(memory)
        assertEquals(listOf("2026-08-30", "2026-08-31", "2026-09-01"), facts.dates)
        assertEquals(listOf("career", "composite"), facts.userTopics)
        assertEquals(listOf("象棋·胜", "象棋·负"), facts.results)
        assertEquals("2026-09-01", facts.lastDate)
        assertEquals(0, facts.dropped)
        assertFalse(facts.unreadable)
        assertFalse(facts.isEmpty)
    }

    @Test
    fun never_read_and_broken_read_are_different_answers() {
        assertTrue(RecollectionCodec.factsOf(null).isEmpty)
        assertTrue(RecollectionCodec.factsOf("").isEmpty)
        assertFalse(RecollectionCodec.factsOf("").unreadable)
        val broken = RecollectionCodec.factsOf("garbage")
        assertTrue(broken.unreadable)
        assertTrue(broken.dates.isEmpty())
        // 读不出来是有内容要交代的，不能当成「什么都没说过」
        assertFalse(broken.isEmpty)
    }

    @Test
    fun entries_without_a_date_stay_out_of_the_date_list() {
        val facts = RecollectionCodec.facts(
            memoryOf(RecollectionEntry("昨天", RecollectionKind.USER_INPUT, "想换工作", "career"))
        )
        assertTrue(facts.dates.isEmpty())
        assertEquals(listOf("career"), facts.userTopics)
    }

    // ---- 召回问句 --------------------------------------------------------------------

    @Test
    fun a_question_about_the_past_recalls() {
        listOf(
            "上次我们聊了什么",
            "之前说过啥来着",
            "你还记得我说过什么吗",
            "我说过什么",
            "前天你给的那句话是啥来着"
        ).forEach { assertTrue("<$it> should read as a recall question", RecollectionCodec.rememberOf(it)) }
    }

    @Test
    fun a_statement_about_the_past_does_not() {
        listOf(
            "",
            "   ",
            "今天运势怎么样",
            "上次我说过想换工作",
            "退出棋局",
            "给我提示",
            "有什么想问的",
            "这句话什么意思",
            "这步怎么走"
        ).forEach { assertFalse("<$it> should not read as a recall question", RecollectionCodec.rememberOf(it)) }
    }
}
