package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.WesternDailyFortune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlinx.coroutines.runBlocking

class MysticDialogueEngineTest {

    private val fortune = CompositeDailyFortune(
        dateKey = "2026-08-29",
        overallScore = 72,
        dimensions = listOf(
            FortuneDimension(
                key = "career",
                label = "事业",
                score = 70,
                interpretation = "稳步推进"
            )
        ),
        luckyNumber = 6,
        luckyColor = "青",
        luckyDirection = "东南",
        cautions = "别硬顶",
        eastern = EasternDailyFortune(
            dateKey = "2026-08-29",
            overallScore = 68,
            careerScore = 70,
            wealthScore = 65,
            loveScore = 66,
            healthScore = 69,
            summary = "东方盘平稳",
            advice = "稳步推进",
            dayPillarText = "甲子",
            favorableToday = emptyList<Element>(),
            luckyColor = "青",
            luckyDirection = "东南"
        ),
        western = WesternDailyFortune(
            dateKey = "2026-08-29",
            sign = "处女座",
            overallScore = 74,
            careerScore = 73,
            wealthScore = 72,
            loveScore = 71,
            healthScore = 75,
            summary = "西方盘平稳",
            luckyNumber = 6,
            luckyColor = "青",
            luckyDirection = "东南",
            dimensionBasis = emptyMap()
        ),
        period = "day",
        periodSummary = "平稳推进",
        insights = emptyList()
    )

    @Test
    fun classify_maps_greeting_to_greeting() {
        val engine = DefaultMysticDialogueEngine()

        assertEquals(MysticIntent.Greeting, engine.classify("你好呀"))
        assertEquals(MysticIntent.Farewell, engine.classify("再见，拜拜"))
        assertEquals(MysticIntent.Thanks, engine.classify("谢谢你"))
        assertEquals(MysticIntent.Identity, engine.classify("你是谁？"))
        assertEquals(MysticIntent.Smalltalk, engine.classify("无聊，陪我聊聊"))
    }

    @Test
    fun reply_for_chat_has_no_forced_question_prefix() {
        val engine = DefaultMysticDialogueEngine()

        val reply = engine.reply(
            DialogueContext(
                mode = "scholar",
                styleKey = "archive",
                topicKey = "composite",
                question = "随便聊聊",
                fortune = fortune
            ),
            "随便聊聊"
        )

        assertEquals("smalltalk", reply.intent.value)
        assertNotEquals(true, reply.text.startsWith("你问：「"))
    }

    @Test
    fun reply_is_deterministic_for_same_input() {
        val engine = DefaultMysticDialogueEngine()
        val context = DialogueContext(
            mode = "half",
            styleKey = "alley",
            topicKey = "wealth",
            question = "我最近财运怎么样",
            fortune = fortune
        )

        assertEquals(engine.reply(context), engine.reply(context))
    }

    @Test
    fun sensitive_topics_stay_as_guidance_not_conclusions() {
        val engine = DefaultMysticDialogueEngine()
        val health = engine.reply(contextFor("我最近睡眠不好，健康怎么样"))
        val wealth = engine.reply(contextFor("要不要投资赚钱"))
        assertEquals(MysticIntent.Health, health.intent)
        assertEquals(MysticIntent.Wealth, wealth.intent)
        assertEquals(false, health.text.contains("诊断"))
        assertEquals(false, wealth.text.contains("保证赚钱"))
    }

    @Test
    fun golden_matrix_handles_variants_and_bounds_input() {
        val engine = DefaultMysticDialogueEngine()
        val cases = mapOf(
            "  你好！！！  " to MysticIntent.Greeting,
            "早安" to MysticIntent.Greeting,
            "再见" to MysticIntent.Farewell,
            "谢谢" to MysticIntent.Thanks,
            "你是谁？" to MysticIntent.Identity,
            "无聊，陪我聊聊" to MysticIntent.Smalltalk,
            "我最近很焦虑" to MysticIntent.Mood,
            "投资要注意什么" to MysticIntent.Wealth,
            "" to MysticIntent.Chat
        )
        cases.forEach { (input, expected) -> assertEquals(expected, engine.classify(input)) }
        assertEquals(MysticIntent.Chat, engine.classify("x".repeat(400)))
    }

    @Test
    fun offline_provider_keeps_the_same_deterministic_reply() = runBlocking {
        val provider = OfflineDialogueProvider()
        val context = contextFor("你好")
        val first = provider.complete(DialogueRequest(context, "你好", 1L))
        val second = provider.complete(DialogueRequest(context, "你好", 1L))
        assertEquals(first, second)
    }

    @Test
    fun greeting_thanks_and_smalltalk_are_natural_without_echoing_question() {
        val engine = DefaultMysticDialogueEngine()
        listOf("你好", "谢谢", "随便聊聊", "晚安").forEach { input ->
            val reply = engine.reply(contextFor(input), input)
            assertEquals(false, reply.text.contains("你问：「$input」"))
        }
    }

    @Test
    fun input_is_normalized_to_two_hundred_characters_before_generation() {
        val engine = DefaultMysticDialogueEngine()
        val longInput = "x".repeat(500)
        val reply = engine.reply(contextFor(longInput), longInput)
        assertEquals(MysticIntent.Chat, reply.intent)
        assertEquals(false, reply.text.contains(longInput))
    }

    @Test
    fun health_and_finance_never_make_diagnostic_or_profit_guarantees() {
        val engine = DefaultMysticDialogueEngine()
        val health = engine.reply(contextFor("我是不是抑郁症，需要吃什么药"), "我是不是抑郁症，需要吃什么药")
        val finance = engine.reply(contextFor("告诉我买哪只股票一定赚钱"), "告诉我买哪只股票一定赚钱")
        assertEquals(false, health.text.contains("你患有"))
        assertEquals(false, health.text.contains("处方"))
        assertEquals(false, finance.text.contains("保证收益"))
        assertEquals(false, finance.text.contains("推荐买"))
        assertEquals(false, finance.text.contains("保证收益"))
    }

    private fun contextFor(question: String) = DialogueContext(
        mode = "scholar",
        styleKey = "archive",
        topicKey = "composite",
        question = question,
        fortune = fortune
    )
}
