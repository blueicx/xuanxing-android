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
    Action("action")
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
    val text: String
)

interface MysticDialogueEngine {
    fun classify(question: String): MysticIntent
    /** Generate a reply for the supplied input without mutating the context object. */
    fun reply(context: DialogueContext, input: String): DialogueReply
}

/** Compatibility overload for callers that keep the input in [DialogueContext]. */
fun MysticDialogueEngine.reply(context: DialogueContext): DialogueReply =
    reply(context, context.question)

class DefaultMysticDialogueEngine : MysticDialogueEngine {
    override fun classify(question: String): MysticIntent = MysticIntentClassifier.classify(question)

    override fun reply(context: DialogueContext, input: String): DialogueReply {
        val normalizedInput = input.trim().take(200)
        val continuity = MysticDialogueContinuity.resolve(normalizedInput, context.recentTurns)
        val intent = continuity.intent
        val prefix = MysticGuideGenerator.customAnswerPrefix(normalizedInput)
        val text = MysticGuideGenerator.customAnswer(
            context.mode,
            context.topicKey,
            continuity.generationInput,
            context.fortune,
            context.latestTest,
            context.skinId
        )
        return DialogueReply(intent, prefix, prefix + text)
    }

}
