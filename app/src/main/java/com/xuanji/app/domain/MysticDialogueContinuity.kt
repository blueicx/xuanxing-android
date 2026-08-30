package com.xuanji.app.domain

/** 将省略式追问明确绑定到上一回合主题，避免“继续”被当成无关闲聊。 */
data class DialogueContinuity(
    val intent: MysticIntent,
    val generationInput: String,
    val bridged: Boolean
)

object MysticDialogueContinuity {
    private val followUpPhrases = setOf(
        "继续", "然后呢", "这个呢", "那呢", "怎么办", "怎么做", "还有呢", "再说说", "具体呢"
    )

    fun resolve(input: String, recentTurns: List<MysticTurn>): DialogueContinuity {
        val normalized = input.trim().take(200)
        val current = MysticIntentClassifier.classify(normalized)
        val previous = recentTurns.lastOrNull() ?: return DialogueContinuity(current, normalized, false)
        val previousIntent = intentFromValue(previous.kind) ?: return DialogueContinuity(current, normalized, false)
        val canBridge = isFollowUp(normalized) && current in setOf(MysticIntent.Chat, MysticIntent.Daily, MysticIntent.Action)
        if (!canBridge) return DialogueContinuity(current, normalized, false)

        val hint = when (previousIntent) {
            MysticIntent.Fortune -> "运势"
            MysticIntent.Mood -> "情绪"
            MysticIntent.Love -> "感情"
            MysticIntent.Wealth -> "财富"
            MysticIntent.Career -> "工作"
            MysticIntent.Study -> "学习"
            MysticIntent.Health -> "健康"
            MysticIntent.Care -> "注意事项"
            MysticIntent.Outcome -> "结果"
            MysticIntent.Action -> "行动建议"
            MysticIntent.Daily -> "今天"
            else -> return DialogueContinuity(current, normalized, false)
        }
        val generated = "$hint；$normalized"
        return DialogueContinuity(MysticIntentClassifier.classify(generated), generated, true)
    }

    private fun isFollowUp(input: String): Boolean {
        val normalized = input.lowercase().trimEnd('.', ',', '，', '。', '!', '！', '?', '？', '~', '～')
        return normalized in followUpPhrases || normalized.startsWith("那") && normalized.length <= 8
    }

    private fun intentFromValue(value: String): MysticIntent? = MysticIntent.entries.firstOrNull { it.value == value }
}
