package com.xuanji.app.domain

sealed interface ValidationResult {
    data object Accept : ValidationResult
    data class Reject(val reason: String) : ValidationResult
}

/** Local guard for optional provider text. It never lets a provider invent app facts or conclusions. */
object DialogueReplyValidator {
    private val SCORE_PATTERN = Regex("(?<!\\d)(\\d{1,3})\\s*分")
    private val MEMORY_CUES = listOf("我记得你", "你之前说过", "你一直都", "长期记忆显示")

    fun validate(text: String, context: DialogueContext): ValidationResult {
        val reply = text.trim()
        if (reply.isEmpty()) return ValidationResult.Reject("empty_reply")
        if (reply.length > 1600) return ValidationResult.Reject("reply_too_long")

        val allowedScores = buildSet {
            add(context.fortune.overallScore)
            context.fortune.dimensions.forEach { add(it.score) }
            add(context.fortune.eastern.overallScore)
            add(context.fortune.eastern.careerScore)
            add(context.fortune.eastern.wealthScore)
            add(context.fortune.eastern.loveScore)
            add(context.fortune.eastern.healthScore)
            add(context.fortune.western.overallScore)
            add(context.fortune.western.careerScore)
            add(context.fortune.western.wealthScore)
            add(context.fortune.western.loveScore)
            add(context.fortune.western.healthScore)
        }
        if (SCORE_PATTERN.findAll(reply).any { it.groupValues[1].toInt() !in allowedScores }) {
            return ValidationResult.Reject("ungrounded_fact")
        }

        if (MysticSafetyGuard.FORBIDDEN.any(reply::contains) ||
            MEMORY_CUES.any(reply::contains) ||
            reply.contains("保证收益") || reply.contains("稳赚")) {
            return ValidationResult.Reject(if (MEMORY_CUES.any(reply::contains)) "ungrounded_fact" else "safety_boundary")
        }
        return ValidationResult.Accept
    }
}
