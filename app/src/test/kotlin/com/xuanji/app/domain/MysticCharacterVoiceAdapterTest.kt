package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticCharacterVoiceAdapterTest {
    @Test
    fun every_character_routes_the_same_question_to_a_real_specialty() {
        val selections = MysticCharacterId.entries.map { id ->
            MysticCharacterVoiceAdapter.selectSpecialty(id, MysticIntent.Career, "career")
        }

        assertEquals(4, selections.map { it.characterId }.toSet().size)
        assertEquals("bazi", selections.first { it.characterId == MysticCharacterId.ShenYanzhou }.specialtyKey)
        assertEquals("composite", selections.first { it.characterId == MysticCharacterId.MoHeng }.specialtyKey)
        assertEquals("western_astrology", selections.first { it.characterId == MysticCharacterId.EvelynNova }.specialtyKey)
        assertEquals("babylonian_astrology", selections.first { it.characterId == MysticCharacterId.NadirRashid }.specialtyKey)
    }

    @Test
    fun four_character_replies_are_different_but_keep_the_same_grounded_score() {
        val input = "今天工作怎么安排"
        val replies = MysticCharacterId.entries.map { id ->
            DefaultMysticDialogueEngine().reply(
                DialogueContext(
                    mode = "scholar",
                    styleKey = "archive",
                    topicKey = "career",
                    fortune = fortune,
                    question = input,
                    characterId = id,
                    characterName = MysticCharacterCatalog.byId(id).displayName
                ),
                input
            )
        }

        assertEquals(4, replies.map { it.text }.toSet().size)
        replies.forEach { reply ->
            assertTrue(reply.text.contains("70 分"))
            assertFalse(reply.text.contains("保证收益"))
            assertFalse(reply.text.contains("诊断"))
        }
    }

    @Test
    fun explicit_specialty_is_honored_only_when_it_belongs_to_the_character() {
        val valid = MysticCharacterVoiceAdapter.adapt(
            context = context(MysticCharacterId.EvelynNova).copy(characterSpecialtyKey = "tarot"),
            analysis = DialogueAnalysis(MysticIntent.Tarot, "tarot", confidence = 99, needsClarification = false),
            draft = "牌阵只作反思入口。"
        )
        val invalid = MysticCharacterVoiceAdapter.adapt(
            context = context(MysticCharacterId.EvelynNova).copy(characterSpecialtyKey = "bazi"),
            analysis = DialogueAnalysis(MysticIntent.Tarot, "tarot", confidence = 99, needsClarification = false),
            draft = "牌阵只作反思入口。"
        )

        assertEquals("tarot", valid.selection.specialtyKey)
        assertNotEquals("bazi", invalid.selection.specialtyKey)
        assertTrue(valid.text.contains("伊芙琳"))
    }

    @Test
    fun casual_intents_do_not_echo_the_question() {
        MysticCharacterId.entries.forEach { id ->
            val input = "你好"
            val reply = DefaultMysticDialogueEngine().reply(context(id).copy(question = input), input)
            assertFalse(reply.text.contains("你问：「$input」"))
            assertTrue(reply.text.contains(MysticCharacterCatalog.byId(id).displayName))
        }
    }

    @Test
    fun reply_exposes_the_selected_specialty_and_a_reproducible_evidence_trace() {
        val context = context(MysticCharacterId.NadirRashid).copy(
            topicKey = "weather",
            question = "天气预报"
        )
        val first = DefaultMysticDialogueEngine().reply(context, context.question)
        val second = DefaultMysticDialogueEngine().reply(context, context.question)

        assertEquals("place", first.specialtyKey)
        assertEquals(first.evidence, second.evidence)
        assertTrue(first.evidence?.seed?.isNotBlank() == true)
    }

    private fun context(id: MysticCharacterId) = DialogueContext(
        mode = "scholar",
        styleKey = "archive",
        topicKey = "composite",
        fortune = fortune,
        characterId = id,
        characterName = MysticCharacterCatalog.byId(id).displayName
    )

    private companion object {
        val fortune = CompositeDailyFortune(
            dateKey = "2026-09-22",
            overallScore = 72,
            dimensions = listOf(FortuneDimension("career", "事业", 70, "稳步推进")),
            luckyNumber = 6,
            luckyColor = "青",
            luckyDirection = "东南",
            cautions = "别硬顶",
            eastern = EasternDailyFortune(
                dateKey = "2026-09-22", overallScore = 68, careerScore = 70, wealthScore = 65,
                loveScore = 66, healthScore = 69, summary = "东方盘平稳", advice = "稳步推进",
                dayPillarText = "甲子", favorableToday = emptyList<Element>(), luckyColor = "青", luckyDirection = "东南"
            ),
            western = WesternDailyFortune(
                dateKey = "2026-09-22", sign = "处女座", overallScore = 74, careerScore = 73,
                wealthScore = 72, loveScore = 71, healthScore = 75, summary = "西方盘平稳",
                luckyNumber = 6, luckyColor = "青", luckyDirection = "东南", dimensionBasis = emptyMap()
            )
        )
    }
}
