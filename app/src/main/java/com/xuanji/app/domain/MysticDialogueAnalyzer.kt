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

    private fun casualIntent(q: String): MysticIntent? = when {
        q in setOf("hi", "hello", "yo", "嗨", "哈喽", "哈罗", "你好", "您好", "在吗", "在么", "在不在") ||
            q.length <= 5 && setOf("你好", "您好", "哈喽", "哈罗", "嗨").any(q::startsWith) ||
            setOf("你好呀", "您好呀", "早上好", "早安", "午安", "下午好", "晚上好", "晚安", "最近怎么样", "最近如何").any(q::contains) -> MysticIntent.Greeting
        q == "走了" || setOf("拜拜", "再见", "回见", "睡了", "去忙").any(q::contains) -> MysticIntent.Farewell
        setOf("谢谢", "多谢", "感谢", "辛苦了", "thanks", "thank you").any(q::contains) -> MysticIntent.Thanks
        setOf("你是谁", "你叫什么", "叫什么名字", "什么名字", "介绍一下自己", "你是什么人", "你是神仙吗").any(q::contains) -> MysticIntent.Identity
        setOf("吃了吗", "干嘛呢", "在干嘛", "在忙吗", "无聊", "陪我聊", "陪我说话", "今天心情").any(q::contains) -> MysticIntent.Smalltalk
        else -> null
    }

    private fun topicMatches(q: String): List<TopicMatch> = TOPICS.mapNotNull { topic ->
        val positions = topic.cues.mapNotNull { cue -> q.indexOf(cue).takeIf { it >= 0 } }
        positions.minOrNull()?.let { TopicMatch(topic.key, topic.intent, topic.label, it) }
    }.sortedWith(compareBy<TopicMatch> { it.position }.thenBy { it.key })

    private fun actionIntent(q: String): Pair<MysticIntent, String>? = when {
        setOf("早餐吃什么", "午餐吃什么", "午饭吃什么", "晚餐吃什么", "晚饭吃什么", "今天吃什么", "外卖吃什么").any(q::contains) -> MysticIntent.TodayMeal to "daily_meal"
        setOf("今天做什么", "今天适合做什么", "现在做什么").any(q::contains) -> MysticIntent.TodayActivity to "daily_activity"
        setOf("去哪玩", "去哪里玩", "今天去哪", "今天去哪里").any(q::contains) -> MysticIntent.TodayOuting to "daily_outing"
        setOf("适合什么工作", "适合做什么工作", "什么工作适合我", "什么颜色适合我", "适合什么颜色", "哪个城市适合我", "适合哪个城市", "适合什么地区").any(q::contains) -> MysticIntent.LifeProfile to "life_profile"
        else -> null
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
            TopicDefinition("study", MysticIntent.Study, "学习", setOf("学习", "考试", "复习", "作业", "论文", "背", "题")),
            TopicDefinition("health", MysticIntent.Health, "健康", setOf("健康", "身体", "睡觉", "睡眠", "失眠", "生病", "累")),
            TopicDefinition("why", MysticIntent.Why, "依据", setOf("为什么", "怎么来", "怎么算", "依据", "来源", "多少分")),
            TopicDefinition("care", MysticIntent.Care, "注意事项", setOf("留意", "注意", "风险", "小心", "避免", "坑")),
            TopicDefinition("outcome", MysticIntent.Outcome, "结果", setOf("能不能", "会不会", "可不可以", "行不行", "成不成", "该不该")),
            TopicDefinition("action", MysticIntent.Action, "行动", setOf("什么时候", "几点", "哪天", "现在适合", "今天适合", "怎么做", "怎么办", "如何", "建议", "行动", "开始", "计划", "破解")),
            TopicDefinition("daily", MysticIntent.Daily, "日常", setOf("饿", "吃", "外卖", "天气", "下雨", "热死", "冷", "困"))
        )
    }
}
