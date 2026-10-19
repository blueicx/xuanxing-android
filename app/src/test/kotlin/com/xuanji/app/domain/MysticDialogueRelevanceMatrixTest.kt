package com.xuanji.app.domain

import com.google.gson.Gson
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.WesternDailyFortune
import com.xuanji.app.domain.action.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MysticDialogueRelevanceMatrixTest {
    private val analyzer = DefaultMysticDialogueAnalyzer()
    private val engine = DefaultMysticDialogueEngine()

    @Test
    fun actual_analysis_routing_and_offline_replies_match_the_curated_four_character_matrix() {
        val envelope = javaClass.classLoader!!.getResourceAsStream("dialogue_relevance_cases.json")!!.bufferedReader().use {
            Gson().fromJson(it, RelevanceEnvelope::class.java)
        }
        assertTrue("fixture should contain at least 200 authored inputs", envelope.cases.size >= 200)
        assertEquals(4, envelope.characters.distinct().size)
        var replyCount = 0

        envelope.cases.forEach { case ->
            envelope.characters.forEach { characterKey ->
                val character = MysticCharacterId.entries.single { it.key == characterKey }
                val profile = MysticCharacterCatalog.byId(character)
                val context = dialogueContext(character).let { base ->
                    case.recentTurnKind?.let { kind ->
                        base.copy(recentTurns = listOf(MysticTurn("之前的话题", "本机测试上下文", kind)))
                    } ?: base
                }
                val analysis = analyzer.analyze(case.input, context)
                val label = "${case.id}/${character.key}: ${case.input}"
                assertEquals(label, case.expectedIntent, analysis.intent.name)
                case.needsClarification?.let { assertEquals(label, it, analysis.needsClarification) }

                val action = MysticCompanionActionRouter.route(
                    input = case.input,
                    dailyActionPlan = actionPlan(),
                    lifeProfile = lifeProfile(),
                    hasXiangqiArchive = false
                )
                assertEquals(label, case.expectedAction, actionLabel(action))

                val first = engine.reply(context, case.input)
                val second = engine.reply(context, case.input)
                assertEquals(label, analysis.intent.name, first.intent.name)
                assertEquals(label, first, second)
                assertTrue("$label produced empty offline text", first.text.isNotBlank())
                val expectedContent = when (case.expectedAction) {
                    "meal" -> "菌菇鸡肉饭"
                    "activity" -> "整理书桌"
                    "outing" -> "滨水公园"
                    "life_profile" -> "设计策划"
                    else -> null
                }
                expectedContent?.let {
                    assertTrue("$label reply did not use the routed real fixture: ${first.text}", first.text.contains(it))
                    assertTrue("$label redirected instead of answering", !first.text.contains("请打开综合页"))
                }
                assertTrue(
                    "$label selected a specialty outside ${profile.displayName}",
                    profile.specialties.any { it.key == first.specialtyKey }
                )
                when (case.expectedSafety) {
                    "Health" -> assertTrue(label, first.text.contains("不构成医疗建议"))
                    "Finance" -> assertTrue(label, first.text.contains("不构成投资建议"))
                }
                if (analysis.intent == MysticIntent.Greeting || analysis.intent == MysticIntent.Smalltalk) {
                    assertTrue("$label echoed an unnatural question prefix", !first.text.contains("你问：「"))
                }
                if (analysis.intent == MysticIntent.Greeting && case.id.startsWith("greeting_")) {
                    assertTrue("$label lost the selected character voice", first.text.contains(profile.displayName))
                }
                replyCount++
            }
        }
        println("Dialogue relevance matrix: $replyCount real offline replies, ${envelope.cases.size} inputs × ${envelope.characters.size} characters")
    }

    @Test
    fun action_shortcuts_without_grounded_data_stay_in_dialogue() {
        listOf("今天吃什么", "今天适合做什么", "今天去哪玩", "适合什么工作").forEach { input ->
            assertEquals(null, MysticCompanionActionRouter.route(input, null, null, false))
        }
    }

    private fun actionLabel(action: MysticCompanionAction?): String = when (action) {
        null -> "none"
        MysticCompanionAction.OpenConversation -> "open_conversation"
        is MysticCompanionAction.ShowTodayMeal -> "meal"
        is MysticCompanionAction.ShowTodayActivity -> "activity"
        is MysticCompanionAction.ShowTodayOuting -> "outing"
        is MysticCompanionAction.ShowLifeProfile -> "life_profile"
        MysticCompanionAction.ShowEvidence -> "evidence"
        is MysticCompanionAction.OpenGame -> "open_game:${action.gameId}"
        MysticCompanionAction.ResumeXiangqi -> "resume_xiangqi"
    }

    private fun dialogueContext(characterId: MysticCharacterId) = DialogueContext(
        profileKey = "synthetic-relevance-profile",
        dateKey = "2026-10-03",
        mode = MysticCharacterCatalog.byId(characterId).dialogueMode,
        styleKey = "archive",
        topicKey = "composite",
        fortune = fixtureFortune(),
        dailyActionPlan = actionPlan(),
        lifeProfile = lifeProfile(),
        characterId = characterId,
        characterName = MysticCharacterCatalog.byId(characterId).displayName
    )

    private fun actionPlan() = DailyActionPlan(
        dateKey = "2026-10-03", profileKey = "synthetic-relevance-profile", cityKey = null,
        meals = listOf(MealSuggestion(MealSlot.Lunch, "菌菇鸡肉饭", listOf("鸡肉", "菌菇"), "豆腐", listOf("菌菇饭"), 88, emptyList(), 2800, 20)),
        activities = listOf(ActivitySuggestion("整理书桌", 15..20, "午后", null, 80, emptyList())),
        outings = listOf(OutingSuggestion("滨水公园", "适合舒展", "本地", 79, emptyList())),
        evidence = emptyList(), confidence = ConfidenceLevel.High, disclaimer = "生活灵感"
    )

    private fun lifeProfile() = LifeProfile(
        profileKey = "synthetic-relevance-profile",
        careerClusters = listOf(CareerCluster("design", "设计策划", 82, emptyList())),
        colorPalette = listOf(ColorCluster("green", "青绿色", "#557A64", emptyList())),
        environmentProfile = EnvironmentProfile(emptyList(), "舒缓", emptyList()),
        regionCandidates = emptyList(), evidence = emptyList(), confidence = ConfidenceLevel.Medium,
        missingInputs = emptyList(), disclaimer = "匹配示例"
    )

    private fun fixtureFortune() = CompositeDailyFortune(
        dateKey = "2026-10-03", overallScore = 72,
        dimensions = listOf(FortuneDimension("career", "事业", 70, "稳步推进")),
        luckyNumber = 6, luckyColor = "青", luckyDirection = "东南", cautions = "别硬顶",
        eastern = EasternDailyFortune("2026-10-03", 68, 70, 65, 66, 69, "东方盘平稳", "稳步推进", "甲子", emptyList<Element>(), "青", "东南"),
        western = WesternDailyFortune("2026-10-03", "处女座", 74, 73, 72, 71, 75, "西方盘平稳", 6, "青", "东南", dimensionBasis = emptyMap()),
        period = "day", periodSummary = "平稳推进", insights = emptyList()
    )

    private data class RelevanceEnvelope(val characters: List<String>, val cases: List<RelevanceCase>)
    private data class RelevanceCase(
        val id: String,
        val input: String,
        val expectedIntent: String,
        val expectedAction: String,
        val expectedSafety: String? = null,
        val recentTurnKind: String? = null,
        val needsClarification: Boolean? = null
    )
}
