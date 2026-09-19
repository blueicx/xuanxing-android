package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
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

    // ---- 双面角色的称谓 --------------------------------------------------------------

    private fun fortuneAt(dateKey: String, score: Int, luckyNumber: Int) = CompositeDailyFortune(
        dateKey = dateKey,
        overallScore = score,
        dimensions = listOf(FortuneDimension("career", "事业", score, "稳步推进")),
        luckyNumber = luckyNumber,
        luckyColor = "青",
        luckyDirection = "东南",
        cautions = "别硬顶",
        eastern = EasternDailyFortune(
            dateKey = dateKey, overallScore = score, careerScore = score, wealthScore = score,
            loveScore = score, healthScore = score, summary = "东方盘平稳", advice = "稳步推进",
            dayPillarText = "甲子", favorableToday = emptyList(), luckyColor = "青", luckyDirection = "东南"
        ),
        western = WesternDailyFortune(
            dateKey = dateKey, sign = "处女座", overallScore = score, careerScore = score,
            wealthScore = score, loveScore = score, healthScore = score, summary = "西方盘平稳",
            luckyNumber = luckyNumber, luckyColor = "青", luckyDirection = "东南"
        )
    )

    @Test
    fun personaName_labels_the_two_modes_and_never_the_umbrella() {
        assertEquals("玄学家", MysticGuideGenerator.personaName("scholar"))
        assertEquals("半仙", MysticGuideGenerator.personaName("half"))
        // 未识别的模式按学者处理，与 mysticSkins / styleKeyFor 的判断一致
        assertEquals("玄学家", MysticGuideGenerator.personaName(""))
        listOf("慈翁", "魔师", "玄师").forEach { banned ->
            listOf("scholar", "half", "", "whatever").forEach { mode ->
                val name = MysticGuideGenerator.personaName(mode)
                assertFalse("<$name> must not be a mode label", name.contains(banned))
            }
        }
    }

    @Test
    fun identity_answers_name_the_persona_exactly_once() {
        val topics = listOf("composite", "career", "love", "wealth", "study", "health", "test")
        val seen = mutableSetOf<String>()
        listOf(
            "2026-08-29" to 72, "2026-08-30" to 55, "2026-09-01" to 61,
            "2026-09-02" to 78, "2026-09-03" to 44
        ).forEachIndexed { index, (dateKey, score) ->
            topics.forEach { topic ->
                val fortune = fortuneAt(dateKey, score, 3 + index)
                listOf("scholar", "half").forEach { mode ->
                    val name = MysticGuideGenerator.personaName(mode)
                    val other = MysticGuideGenerator.personaName(if (mode == "half") "scholar" else "half")
                    val answer = MysticGuideGenerator.customAnswer(mode, topic, "你是谁？", fortune)
                    seen += "$mode/${MysticGuideGenerator.styleKeyFor(mode, topic, fortune)}"
                    listOf("慈翁", "魔师").forEach { retired ->
                        assertFalse("<$answer> still uses $retired", answer.contains(retired))
                    }
                    assertEquals("<$answer> must not borrow $other", 0, occurrences(answer, other))
                    assertEquals("<$answer> must name itself once", 1, occurrences(answer, name))
                }
            }
        }
        assertEquals(
            setOf("scholar/archive", "scholar/harbor", "scholar/compass",
                "half/herald", "half/alley", "half/intern"),
            seen
        )
    }

    private fun occurrences(text: String, token: String): Int =
        Regex(Regex.escape(token)).findAll(text).count()
}
