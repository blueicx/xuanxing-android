package com.xuanji.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticGuideGeneratorTest {

    @Test
    fun customAnswerPrefix_returnsEmptyForBlankQuestion() {
        assertEquals("", MysticGuideGenerator.customAnswerPrefix("   "))
    }

    // ---- 本机长期记忆的召回句 --------------------------------------------------------

    private val topicsOnly = RecallFacts(
        dates = listOf("2026-08-30", "2026-09-01"),
        userTopics = listOf("career", "wealth")
    )

    @Test
    fun the_scholar_quotes_the_record_and_adds_no_verdict() {
        assertEquals(
            "8月30日、9月1日你聊过「事业、财富」。这几句是你自己留在本机的，我不加判词。",
            MysticGuideGenerator.recallLine("scholar", "archive", topicsOnly)
        )
    }

    @Test
    fun the_herald_keeps_the_same_facts_in_his_own_mouth() {
        val line = MysticGuideGenerator.recallLine("half", "herald", topicsOnly)
        assertTrue(line.contains("8月30日、9月1日你聊过「事业、财富」"))
        assertTrue(line.endsWith("——都是你自己压在本机上的话，我可没现编。"))
    }

    @Test
    fun settled_games_are_recalled_because_the_rules_settled_them() {
        val facts = RecallFacts(dates = listOf("2026-09-01"), results = listOf("象棋·胜", "象棋·负"))
        val line = MysticGuideGenerator.recallLine("half", "alley", facts)
        assertTrue(line.contains("棋本上记着象棋·胜、象棋·负"))
    }

    @Test
    fun an_empty_record_gets_no_line_at_all() {
        listOf("scholar" to "archive", "scholar" to "harbor", "half" to "herald", "half" to "intern")
            .forEach { (mode, style) ->
                assertEquals("", MysticGuideGenerator.recallLine(mode, style, RecallFacts()))
            }
    }

    @Test
    fun only_the_two_families_get_a_recall_line() {
        assertEquals("", MysticGuideGenerator.recallLine("scholar", "herald", topicsOnly))
        assertEquals("", MysticGuideGenerator.recallLine("half", "archive", topicsOnly))
        assertEquals("", MysticGuideGenerator.recallLine("", "archive", topicsOnly))
    }

    @Test
    fun a_topic_key_the_guide_does_not_know_is_not_called_composite() {
        val line = MysticGuideGenerator.recallLine("scholar", "compass", RecallFacts(userTopics = listOf("mood")))
        assertEquals("", line)
        assertFalse(line.contains("综合"))
    }

    @Test
    fun a_cleaned_count_is_said_even_when_it_is_all_there_is() {
        assertEquals(
            "更早的 7 条已清理，本机只留最近 20 条。",
            MysticGuideGenerator.recallLine("scholar", "harbor", RecallFacts(dropped = 7))
        )
        assertTrue(
            MysticGuideGenerator.recallLine("half", "intern", RecallFacts(dropped = 7))
                .startsWith("更早的 7 条已经清了")
        )
    }

    @Test
    fun cleaning_never_eats_what_is_still_there() {
        val line = MysticGuideGenerator.recallLine("scholar", "archive", topicsOnly.copy(dropped = 3))
        assertTrue(line.contains("事业"))
        assertTrue(line.endsWith("更早的 3 条已清理，本机只留最近 20 条。"))
    }

    @Test
    fun an_unreadable_record_is_admitted_in_both_voices() {
        val broken = RecallFacts(unreadable = true)
        assertEquals("本机那几页记录读不出来，这次不引旧话。", MysticGuideGenerator.recallLine("scholar", "compass", broken))
        assertEquals("摊子上那本旧账我翻不开，这次不装记得。", MysticGuideGenerator.recallLine("half", "herald", broken))
    }

    @Test
    fun a_bad_date_card_is_never_narrated_as_a_day() {
        val line = MysticGuideGenerator.recallLine(
            "scholar", "archive",
            RecallFacts(dates = listOf("昨天", "2026-13-40"), userTopics = listOf("love"))
        )
        assertTrue(line.startsWith("你聊过「感情」"))
    }

    @Test
    fun the_stored_record_is_the_only_thing_the_line_can_say() {
        val memory = listOf(
            RecollectionEntry("2026-08-30", RecollectionKind.USER_INPUT, "这周要不要换工作", "career"),
            RecollectionEntry("2026-08-31", RecollectionKind.SETTLED_GAME_RESULT, "象棋·胜", "game")
        ).fold(ConversationMemory()) { acc, entry -> RecollectionCodec.append(acc, entry) }
        val line = MysticGuideGenerator.recallLine(
            "scholar", "archive",
            RecollectionCodec.factsOf(RecollectionCodec.encode(memory))
        )
        assertTrue(line.contains("8月30日"))
        assertTrue(line.contains("事业"))
        assertTrue(line.contains("象棋·胜"))
        // 原话只在本机列表里出现，不进召回文案
        assertFalse(line.contains("换工作"))
    }

    @Test
    fun the_recall_line_is_stable_and_never_states_a_rating() {
        val shapes = listOf(
            RecallFacts(), RecallFacts(unreadable = true), RecallFacts(dropped = 19), topicsOnly,
            topicsOnly.copy(dropped = 4, results = listOf("象棋·和"))
        )
        val rating = Regex("胜率|等级分|Elo|评分|棋力|[0-9]+\\.[0-9]+")
        shapes.forEach { facts ->
            listOf("scholar" to "archive", "scholar" to "harbor", "scholar" to "compass",
                "half" to "herald", "half" to "alley", "half" to "intern").forEach { (mode, style) ->
                val line = MysticGuideGenerator.recallLine(mode, style, facts)
                assertEquals(line, MysticGuideGenerator.recallLine(mode, style, facts))
                assertFalse("<$line> must not rate anything", rating.containsMatchIn(line))
            }
        }
    }
}
