package com.xuanji.app.domain

import java.text.Normalizer

/** Structured, deterministic interpretation of a user message before copy generation. */
data class DialogueAnalysis(
    val intent: MysticIntent,
    val topicKey: String?,
    val entities: Map<String, String> = emptyMap(),
    val confidence: Int,
    val needsClarification: Boolean
)

interface MysticDialogueAnalyzer {
    fun analyze(input: String, context: DialogueContext): DialogueAnalysis

    /** Compatibility entry point for callers that only need the legacy intent. */
    fun classify(input: String): MysticIntent
}

class DefaultMysticDialogueAnalyzer : MysticDialogueAnalyzer {
    override fun classify(input: String): MysticIntent = analyzeInternal(input, emptyList()).intent

    override fun analyze(input: String, context: DialogueContext): DialogueAnalysis =
        analyzeInternal(input, context.recentTurns)

    private fun analyzeInternal(input: String, recentTurns: List<MysticTurn>): DialogueAnalysis {
        val normalized = normalize(input)
        if (normalized.isEmpty()) {
            return DialogueAnalysis(MysticIntent.Chat, null, confidence = 0, needsClarification = true)
        }

        if (isGame(normalized)) {
            return DialogueAnalysis(MysticIntent.Game, null, confidence = 98, needsClarification = false)
        }

        val casual = casualIntent(normalized)
        if (casual != null) {
            return DialogueAnalysis(casual, null, confidence = 98, needsClarification = false)
        }

        val action = actionIntent(normalized)
        if (action != null) {
            return DialogueAnalysis(action.first, action.second, confidence = 96, needsClarification = false)
        }

        val topics = topicMatches(normalized)
        if (topics.isNotEmpty()) {
            val primary = topics.first()
            val entities = buildMap {
                put("topic_label", primary.label)
                if (topics.size > 1) put("secondary_topic", topics[1].key)
                put("normalized", normalized)
            }
            return DialogueAnalysis(
                intent = primary.intent,
                topicKey = primary.key,
                entities = entities,
                confidence = if (topics.size > 1) 78 else 92,
                needsClarification = topics.size > 1
            )
        }

        val previous = previousTopic(recentTurns)
        if (previous != null && isFollowUp(normalized)) {
            return DialogueAnalysis(
                intent = previous.first,
                topicKey = previous.second,
                entities = mapOf("topic_label" to previous.third, "normalized" to normalized),
                confidence = 76,
                needsClarification = false
            )
        }

        val fallback = MysticIntentClassifier.classify(normalized)
        return DialogueAnalysis(
            intent = fallback,
            topicKey = fallback.topicKeyOrNull(),
            entities = mapOf("normalized" to normalized),
            confidence = if (fallback == MysticIntent.Chat) 40 else 58,
            needsClarification = fallback == MysticIntent.Chat
        )
    }

    private fun normalize(input: String): String = Normalizer.normalize(input, Normalizer.Form.NFKC)
        .trim()
        .lowercase()
        .trimEnd('.', ',', '，', '。', '!', '！', '?', '？', '~', '～', '；', ';')
        .replace(Regex("\\s+"), " ")

    private fun isGame(q: String): Boolean = MysticIntentClassifier.classify(q) == MysticIntent.Game

    private fun casualIntent(q: String): MysticIntent? = MysticIntentClassifier.classify(q).takeIf {
        it in setOf(MysticIntent.Greeting, MysticIntent.Farewell, MysticIntent.Thanks, MysticIntent.Identity, MysticIntent.Smalltalk)
    }

    private fun topicMatches(q: String): List<TopicMatch> = TOPICS.mapNotNull { topic ->
        val positions = topic.cues.mapNotNull { cue -> q.indexOf(cue).takeIf { it >= 0 } }
        positions.minOrNull()?.let { TopicMatch(topic.key, topic.intent, topic.label, it) }
    }.sortedWith(compareBy<TopicMatch> { it.position }.thenBy { it.key })

    private fun actionIntent(q: String): Pair<MysticIntent, String>? {
        val intent = MysticIntentClassifier.classify(q)
        val topic = when (intent) {
            MysticIntent.Health -> "health"
            MysticIntent.TodayMeal -> "daily_meal"
            MysticIntent.TodayActivity -> "daily_activity"
            MysticIntent.TodayOuting -> "daily_outing"
            MysticIntent.LifeProfile -> "life_profile"
            MysticIntent.Tarot -> "tarot"
            MysticIntent.Rune -> "rune"
            MysticIntent.Numerology -> "numerology"
            MysticIntent.Weather -> "weather"
            MysticIntent.Place -> "place"
            MysticIntent.Personality -> "personality"
            else -> null
        }
        return topic?.let { intent to it }
    }

    private fun previousTopic(recentTurns: List<MysticTurn>): Triple<MysticIntent, String, String>? {
        val previous = recentTurns.asReversed().firstNotNullOfOrNull { turn ->
            val intent = MysticIntent.entries.firstOrNull { it.value == turn.kind } ?: return@firstNotNullOfOrNull null
            val key = intent.topicKeyOrNull() ?: return@firstNotNullOfOrNull null
            Triple(intent, key, topicLabel(key))
        }
        return previous
    }

    private fun isFollowUp(input: String): Boolean {
        val normalized = input.trimEnd('.', ',', '，', '。', '!', '！', '?', '？', '~', '～')
        return normalized in FOLLOW_UPS || (normalized.startsWith("那") && normalized.length <= 8)
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
        MysticIntent.TodayMeal -> "daily"
        MysticIntent.TodayActivity -> "action"
        MysticIntent.TodayOuting -> "action"
        MysticIntent.LifeProfile -> "career"
        MysticIntent.Daily -> "daily"
        MysticIntent.Tarot -> "tarot"
        MysticIntent.Rune -> "rune"
        MysticIntent.Numerology -> "numerology"
        MysticIntent.Weather -> "weather"
        MysticIntent.Place -> "place"
        MysticIntent.Personality -> "personality"
        else -> null
    }

    private data class TopicDefinition(
        val key: String,
        val intent: MysticIntent,
        val label: String,
        val cues: Set<String>
    )

    private data class TopicMatch(
        val key: String,
        val intent: MysticIntent,
        val label: String,
        val position: Int
    )

    private fun topicLabel(key: String): String = TOPICS.firstOrNull { it.key == key }?.label ?: key

    private companion object {
        val FOLLOW_UPS = setOf("继续", "然后呢", "这个呢", "那呢", "怎么办", "怎么做", "还有呢", "再说说", "具体呢")

        val TOPICS = listOf(
            TopicDefinition("fortune", MysticIntent.Fortune, "运势", setOf("运势", "运气", "占卜", "算命", "算一卦", "起卦", "盘面", "综合分", "今天运")),
            TopicDefinition("mood", MysticIntent.Mood, "情绪", setOf("焦虑", "压力", "害怕", "担心", "难过", "崩溃", "很累", "内耗")),
            TopicDefinition("love", MysticIntent.Love, "感情", setOf("感情", "恋爱", "对象", "复合", "暗恋", "表白", "桃花", "分手", "他", "她")),
            TopicDefinition("wealth", MysticIntent.Wealth, "财富", setOf("财", "钱", "赚钱", "投资", "生意", "消费", "钱包")),
            TopicDefinition("career", MysticIntent.Career, "工作", setOf("工作", "上班", "事业", "老板", "同事", "面试", "升职", "跳槽")),
            TopicDefinition("study", MysticIntent.Study, "学习", setOf("学习", "考试", "复习", "作业", "论文", "背单词", "背诵", "做题", "题目", "错题", "习题", "考题")),
            TopicDefinition("health", MysticIntent.Health, "健康", setOf("健康", "身体", "睡觉", "睡眠", "睡不好", "睡不着", "失眠", "生病", "累")),
            TopicDefinition("why", MysticIntent.Why, "依据", setOf("为什么", "怎么来", "怎么算", "依据", "来源", "多少分")),
            TopicDefinition("care", MysticIntent.Care, "注意事项", setOf("留意", "注意", "风险", "小心", "避免", "坑")),
            TopicDefinition("outcome", MysticIntent.Outcome, "结果", setOf("能不能", "会不会", "可不可以", "行不行", "成不成", "该不该")),
            TopicDefinition("action", MysticIntent.Action, "行动", setOf("什么时候", "几点", "哪天", "现在适合", "今天适合", "怎么做", "怎么办", "如何", "建议", "行动", "开始", "计划", "破解")),
            TopicDefinition("daily", MysticIntent.Daily, "日常", setOf("饿", "吃", "外卖", "天气", "下雨", "热死", "冷", "困"))
        )
    }
}
