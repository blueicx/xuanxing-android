package com.xuanji.app.domain

enum class PersonalitySource { Unknown, ExplicitAssessment }

data class GuardedResponse(
    val text: String,
    val domain: SafetyDomain,
    val personalitySource: PersonalitySource,
    val replaced: Boolean
)

/** Single local safety boundary for offline and optional provider text. */
object SafetyResponseGuard {
    private val hiddenPersonalityCues = listOf("从你的聊天看你是", "你骨子里是", "你一定是", "我推断你的性格")

    fun guard(
        input: String,
        draft: String,
        context: DialogueContext,
        personalitySource: PersonalitySource = PersonalitySource.Unknown
    ): GuardedResponse {
        val domain = MysticSafetyGuard.verdictDomainOf(input)
        if (domain != SafetyDomain.None) {
            return GuardedResponse(
                text = MysticSafetyGuard.refusal(context.mode, domain, stableVariant(input)),
                domain = domain,
                personalitySource = personalitySource,
                replaced = true
            )
        }
        val noHiddenInference = if (
            personalitySource == PersonalitySource.Unknown && hiddenPersonalityCues.any(draft::contains)
        ) {
            "我不会仅凭聊天给你贴性格标签；如果你愿意，可以完成一个明确标注来源的测验。"
        } else draft
        val validated = DialogueReplyValidator.validate(noHiddenInference, context)
        val safeText = if (validated is ValidationResult.Accept) noHiddenInference else {
            "我先给你一个可核对的方向：把问题拆成一小步，结合页面上的盘面依据自己判断。"
        }
        val withDisclaimer = MysticSafetyGuard.enforce(
            context.mode,
            input,
            stableVariant(input),
            safeText
        )
        return GuardedResponse(withDisclaimer, MysticSafetyGuard.domainOf(input), personalitySource, safeText != noHiddenInference)
    }

    fun personalityLabel(source: PersonalitySource): String = when (source) {
        PersonalitySource.ExplicitAssessment -> "来自你主动完成的测验"
        PersonalitySource.Unknown -> "尚未进行明确测验"
    }

    private fun stableVariant(input: String): Int = input.trim().hashCode().ushr(1)
}
