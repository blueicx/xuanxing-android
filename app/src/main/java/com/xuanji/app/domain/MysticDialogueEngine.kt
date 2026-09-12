package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.TestRecord

enum class MysticIntent(val value: String) {
    Greeting("greeting"),
    Farewell("farewell"),
    Thanks("thanks"),
    Identity("identity"),
    Smalltalk("smalltalk"),
    Daily("daily"),
    Chat("chat"),
    Fortune("fortune"),
    Mood("mood"),
    Love("love"),
    Wealth("wealth"),
    Career("career"),
    Study("study"),
    Health("health"),
    Why("why"),
    Care("care"),
    Outcome("outcome"),
    Action("action"),
    Game("game")
}

data class DialogueContext(
    val profileKey: String = "",
    val dateKey: String = "",
    val mode: String,
    val styleKey: String,
    val topicKey: String,
    val fortune: CompositeDailyFortune,
    val latestTest: TestRecord? = null,
    val recentTurns: List<MysticTurn> = emptyList(),
    val memoryNotes: List<MysticMemoryNote> = emptyList(),
    val skinId: String = "",
    val question: String = ""
)

/** Minimal, UI-independent turn record used when the dialogue engine is called off-screen. */
data class MysticTurn(
    val question: String,
    val answer: String,
    val kind: String = "ask"
)

/** User-authored memory only; generated facts are intentionally not persisted here. */
data class MysticMemoryNote(
    val id: String,
    val text: String
)

data class DialogueReply(
    val intent: MysticIntent,
    val prefix: String,
    val text: String,
    val clarifiers: List<String> = emptyList(),
    val source: ReplySource = ReplySource.Offline,
    val groundedFacts: List<String> = emptyList()
)

enum class ReplySource { Offline, OnlineFallback, OnlineValidated }

interface MysticDialogueEngine {
    fun classify(question: String): MysticIntent
    /** Generate a reply for the supplied input without mutating the context object. */
    fun reply(context: DialogueContext, input: String): DialogueReply
}

/** Compatibility overload for callers that keep the input in [DialogueContext]. */
fun MysticDialogueEngine.reply(context: DialogueContext): DialogueReply =
    reply(context, context.question)

class DefaultMysticDialogueEngine : MysticDialogueEngine {
    private val analyzer: MysticDialogueAnalyzer = DefaultMysticDialogueAnalyzer()

    override fun classify(question: String): MysticIntent = analyzer.classify(question)

    override fun reply(context: DialogueContext, input: String): DialogueReply {
        val normalizedInput = input.trim().take(200)
        val analysis = analyzer.analyze(normalizedInput, context)
        val continuity = MysticDialogueContinuity.resolve(normalizedInput, context.recentTurns)
        val intent = analysis.intent
        val topicKey = analysis.topicKey ?: continuity.intent.topicKeyOrNull() ?: context.topicKey
        val prefix = MysticGuideGenerator.customAnswerPrefix(normalizedInput)
        val text = MysticGuideGenerator.customAnswer(
            context.mode,
            topicKey,
            continuity.generationInput,
            context.fortune,
            context.latestTest,
            context.skinId
        )
        return DialogueReply(
            intent = intent,
            prefix = prefix,
            text = prefix + text,
            clarifiers = clarifiersFor(analysis)
        )
    }

}

private fun MysticIntent.topicKeyOrNull(): String? = when (this) {
    MysticIntent.Fortune -> "fortune"
    MysticIntent.Mood -> "mood"
    MysticIntent.Love -> "love"
    MysticIntent.Wealth -> "wealth"
    MysticIntent.Career -> "career"
    MysticIntent.Study -> "study"
    MysticIntent.Health -> "health"
    MysticIntent.Why -> "why"
    MysticIntent.Care -> "care"
    MysticIntent.Outcome -> "outcome"
    MysticIntent.Action -> "action"
    MysticIntent.Daily -> "daily"
    else -> null
}

fun clarifiersFor(analysis: DialogueAnalysis): List<String> = buildList {
    analysis.entities["secondary_topic"]?.let { secondary ->
        add("先看${analysis.entities["topic_label"] ?: analysis.topicKey.orEmpty()}")
        add("再看${topicLabelFor(secondary)}")
    }
    if (isEmpty() && analysis.needsClarification) {
        add("先看今天运势")
        add("先说说最近状态")
    }
}.take(2)

private fun topicLabelFor(key: String): String = mapOf(
    "fortune" to "运势",
    "mood" to "情绪",
    "love" to "感情",
    "wealth" to "财富",
    "career" to "工作",
    "study" to "学习",
    "health" to "健康",
    "why" to "依据",
    "care" to "注意事项",
    "outcome" to "结果",
    "action" to "行动",
    "daily" to "日常"
)[key] ?: key
