package com.xuanji.app.domain

import com.xuanji.app.data.local.CompanionGameProgressStore
import com.xuanji.app.data.local.PreferenceBridge
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.game.CompanionGameProgressState
import com.xuanji.app.domain.game.GameSave
import com.xuanji.app.domain.game.SilkRoadState
import com.xuanji.app.domain.game.StarMapState
import com.xuanji.app.domain.game.PoetryChainState
import com.xuanji.app.domain.game.XiangqiBoard
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticGameContextTest {
    private class MemoryBridge : PreferenceBridge {
        private val values = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = values[key]
        override suspend fun write(key: String, value: String) { values[key] = value }
        override suspend fun delete(key: String) { values.remove(key) }
    }

    @Test
    fun one_context_factory_maps_all_four_characters_to_their_real_game_ids() {
        val savedContexts = listOf(
            MysticGameContextFactory.fromSaved(
                MysticCharacterId.ShenYanzhou,
                CompanionGameProgressState.Poetry(PoetryChainState(promptIndex = 1, selected = "灯影落江中", score = 1)),
                null
            ),
            MysticGameContextFactory.fromSaved(
                MysticCharacterId.MoHeng,
                null,
                GameSave(start = XiangqiBoard.encode(XiangqiBoard.initial()), title = "残局练习")
            ),
            MysticGameContextFactory.fromSaved(
                MysticCharacterId.EvelynNova,
                CompanionGameProgressState.StarMap(StarMapState(current = "天津", path = listOf("织女", "天津"))),
                null
            ),
            MysticGameContextFactory.fromSaved(
                MysticCharacterId.NadirRashid,
                CompanionGameProgressState.SilkRoad(
                    SilkRoadState(current = "敦煌", daysLeft = 6, budgetLeft = 9, path = listOf("长安", "敦煌"))
                ),
                null
            )
        ).map { requireNotNull(it) }

        assertEquals(
            listOf(
                MysticCharacterId.ShenYanzhou to "poetry_chain",
                MysticCharacterId.MoHeng to "xiangqi",
                MysticCharacterId.EvelynNova to "star_map_observation",
                MysticCharacterId.NadirRashid to "silkroad_route"
            ),
            savedContexts.map { it.characterId to it.gameId }
        )
        assertTrue(savedContexts.all { it.checkpoint.isNotBlank() && it.checkpoint.length <= 240 })
    }

    @Test
    fun dialogue_reply_quotes_the_saved_silkroad_checkpoint_and_evidence() {
        val context = gameContext(
            question = "我还剩几天和多少补给？",
            gameContext = MysticGameContextFactory.fromSaved(
                MysticCharacterId.NadirRashid,
                CompanionGameProgressState.SilkRoad(
                    SilkRoadState(current = "敦煌", daysLeft = 6, budgetLeft = 9, path = listOf("长安", "敦煌"))
                ),
                null
            )
        )

        val reply = DefaultMysticDialogueEngine().reply(context, context.question)

        assertEquals(MysticIntent.Game, reply.intent)
        assertTrue(reply.text.contains("敦煌"))
        assertTrue(reply.text.contains("6 天"))
        assertTrue(reply.text.contains("9 点资源"))
        assertTrue(reply.evidence?.items?.any {
            it.key == "game_progress" && it.status == MysticEvidenceStatus.LocallySaved && it.detail.contains("9 点资源")
        } == true)
        assertTrue(reply.evidence?.items?.none { it.key == "fortune" } == true)
        assertEquals(mapOf("local_game_archive" to 100), reply.evidence?.weights)
        assertEquals(listOf("local_game_archive"), reply.evidence?.systems)
        assertTrue(reply.evidence?.items?.none { it.key == "specialty" } == true)
    }

    @Test
    fun next_stop_question_lists_only_real_adjacent_silkroad_choices_and_costs() {
        val saved = MysticGameContextFactory.fromSaved(
            MysticCharacterId.NadirRashid,
            CompanionGameProgressState.SilkRoad(
                SilkRoadState(current = "敦煌", daysLeft = 6, budgetLeft = 9, path = listOf("长安", "敦煌"))
            ),
            null
        )
        val context = gameContext(MysticCharacterId.NadirRashid, "下一站怎么选？", saved)
        val reply = DefaultMysticDialogueEngine().reply(context, context.question)

        assertTrue(reply.text.contains("可选下一站"))
        assertTrue(reply.text.contains("喀什（3天·4资源）"))
        assertTrue(reply.text.contains("长安（0天·0资源）"))
    }

    @Test
    fun missing_or_wrong_character_save_is_reported_without_inventing_progress() {
        val wrongCharacterState = CompanionGameProgressState.SilkRoad(
            SilkRoadState(current = "敦煌", daysLeft = 6, budgetLeft = 9)
        )
        assertNull(MysticGameContextFactory.fromSaved(MysticCharacterId.ShenYanzhou, wrongCharacterState, null))

        val reply = DefaultMysticDialogueEngine().reply(
            gameContext(MysticCharacterId.NadirRashid, "游戏进度", gameContext = null),
            "游戏进度"
        )

        assertEquals(MysticIntent.Game, reply.intent)
        assertTrue(reply.text.contains("没有读到"))
        assertFalse(reply.text.contains("剩余 6 天"))
        assertTrue(reply.evidence?.items?.any { it.key == "game_progress" && it.status == MysticEvidenceStatus.NotEnabled } == true)
        assertTrue(reply.evidence?.systems.isNullOrEmpty())
        assertEquals(listOf("game_progress"), reply.evidence?.items?.map { it.key })
    }

    @Test
    fun resume_requires_an_active_matching_save_and_never_silently_starts_a_new_game() {
        val saved = requireNotNull(
            MysticGameContextFactory.fromSaved(
                MysticCharacterId.NadirRashid,
                CompanionGameProgressState.SilkRoad(
                    SilkRoadState(current = "敦煌", daysLeft = 6, budgetLeft = 9, path = listOf("长安", "敦煌"))
                ),
                null
            )
        )

        assertTrue(saved.isResumableFor(MysticCharacterId.NadirRashid))
        assertFalse(saved.isResumableFor(MysticCharacterId.ShenYanzhou))
        val ended = saved.copy(status = MysticGameProgressStatus.Completed)
        assertFalse(ended.isResumableFor(MysticCharacterId.NadirRashid))
        assertTrue(MysticGameContextAnswer.reply(MysticCharacterId.NadirRashid, null, "继续丝路").contains("不会替你新开一局"))
        assertTrue(MysticGameContextAnswer.reply(MysticCharacterId.NadirRashid, ended, "继续丝路").contains("这一局已完成"))
    }

    @Test
    fun malformed_silkroad_save_is_not_used_as_a_real_checkpoint() {
        val malformed = CompanionGameProgressState.SilkRoad(
            SilkRoadState(current = "不存在的城市", daysLeft = 999, budgetLeft = -4, path = listOf("长安", "不存在的城市"))
        )

        assertNull(MysticGameContextFactory.fromSaved(MysticCharacterId.NadirRashid, malformed, null))
        val impossibleResources = CompanionGameProgressState.SilkRoad(
            SilkRoadState(current = "敦煌", daysLeft = 8, budgetLeft = 12, path = listOf("长安", "敦煌"))
        )
        assertNull(MysticGameContextFactory.fromSaved(MysticCharacterId.NadirRashid, impossibleResources, null))
        val alteredTarget = CompanionGameProgressState.SilkRoad(
            SilkRoadState(
                current = "喀什", target = "喀什", daysLeft = 3, budgetLeft = 5,
                path = listOf("长安", "敦煌", "喀什"), completed = true
            )
        )
        assertNull(MysticGameContextFactory.fromSaved(MysticCharacterId.NadirRashid, alteredTarget, null))
        val invalidPoetrySelection = CompanionGameProgressState.Poetry(
            PoetryChainState(promptIndex = 1, selected = "编造选项", score = 0)
        )
        assertNull(MysticGameContextFactory.fromSaved(MysticCharacterId.ShenYanzhou, invalidPoetrySelection, null))
    }

    @Test
    fun a_saved_game_checkpoint_is_not_attached_to_an_unrelated_fortune_question() {
        val unrelated = MysticGameContextFactory.fromSaved(
            MysticCharacterId.NadirRashid,
            CompanionGameProgressState.SilkRoad(SilkRoadState(current = "敦煌", daysLeft = 6, budgetLeft = 9, path = listOf("长安", "敦煌"))),
            null
        )
        val context = gameContext(MysticCharacterId.NadirRashid, "今天运势怎么样", unrelated)
        val analysis = DefaultMysticDialogueAnalyzer().analyze(context.question, context)
        val specialty = MysticCharacterVoiceAdapter.selectSpecialty(
            MysticCharacterId.NadirRashid,
            analysis.intent,
            analysis.topicKey ?: context.topicKey
        )
        val trace = MysticEvidenceBuilder.forDialogue(context, analysis, specialty)
        val withoutGame = MysticEvidenceBuilder.forDialogue(context.copy(gameContext = null), analysis, specialty)

        assertTrue(trace.items.none { it.key == "game_progress" })
        assertEquals(withoutGame.seed, trace.seed)
    }

    @Test
    fun clearing_one_profile_game_progress_does_not_expose_or_clear_another_profile() = runTest {
        val store = CompanionGameProgressStore(MemoryBridge()) { 12L }
        val alice = CompanionGameProgressState.SilkRoad(
            SilkRoadState(current = "敦煌", daysLeft = 6, budgetLeft = 9, path = listOf("长安", "敦煌"))
        )
        val bob = CompanionGameProgressState.SilkRoad(
            SilkRoadState(current = "喀什", daysLeft = 3, budgetLeft = 5, path = listOf("长安", "敦煌", "喀什"))
        )
        store.save("alice", MysticCharacterId.NadirRashid, alice)
        store.save("bob", MysticCharacterId.NadirRashid, bob)

        val bobBefore = MysticGameContextFactory.fromSaved(
            MysticCharacterId.NadirRashid,
            store.load("bob", MysticCharacterId.NadirRashid, "silkroad_route"),
            null
        )
        store.clear("alice", MysticCharacterId.NadirRashid, "silkroad_route")
        val aliceAfter = store.load("alice", MysticCharacterId.NadirRashid, "silkroad_route")
        val bobAfter = store.load("bob", MysticCharacterId.NadirRashid, "silkroad_route")

        assertNull(aliceAfter)
        assertEquals(bobBefore, MysticGameContextFactory.fromSaved(MysticCharacterId.NadirRashid, bobAfter, null))
        assertTrue(bobBefore?.checkpoint?.contains("3 天") == true)
    }

    private fun gameContext(
        characterId: MysticCharacterId = MysticCharacterId.NadirRashid,
        question: String,
        gameContext: MysticGameContext?
    ) = DialogueContext(
        profileKey = "synthetic-profile",
        dateKey = "2026-10-02",
        mode = "scholar",
        styleKey = "archive",
        topicKey = "composite",
        fortune = fortune(),
        question = question,
        characterId = characterId,
        gameContext = gameContext
    )

    private fun fortune() = CompositeDailyFortune(
        dateKey = "2026-10-02",
        overallScore = 72,
        dimensions = emptyList(),
        luckyNumber = 6,
        luckyColor = "青",
        luckyDirection = "东南",
        cautions = "慢一点",
        eastern = EasternDailyFortune("2026-10-02", 68, 70, 65, 66, 69, "平稳", "慢一点", "甲子", emptyList<Element>(), "青", "东南"),
        western = WesternDailyFortune("2026-10-02", "处女座", 74, 73, 72, 71, 75, "平稳", 6, "青", "东南")
    )
}
