package com.xuanji.app.domain

data class MysticGuide(
    val mode: String,
    val topicKey: String,
    val roleName: String,
    val styleKey: String,
    val styleName: String,
    val styleIntro: String,
    val signature: String,
    val arrival: String,
    val headline: String,
    val body: String,
    val evidence: List<String> = emptyList(),
    val followUps: List<MysticFollowUp> = emptyList()
)

data class MysticFollowUp(val key: String, val question: String, val answer: String)
data class MysticInteractionOption(val label: String, val feedback: String)
data class MysticInteraction(val title: String, val description: String, val options: List<MysticInteractionOption>)
data class MysticOpeningOption(val key: String, val label: String, val response: String)
data class MysticOpeningCheckin(val prompt: String, val options: List<MysticOpeningOption>)
data class MysticRhythmOption(val key: String, val label: String, val response: String)
data class MysticRhythmCheckin(val prompt: String, val options: List<MysticRhythmOption>)
data class MysticGuestCameo(val roleName: String, val line: String)
data class MysticGuestChoice(val key: String, val label: String)
data class MysticGuestExit(val roleName: String, val line: String)
data class MysticClarifierOption(val key: String, val label: String, val answer: String)
data class MysticClarifier(val title: String, val options: List<MysticClarifierOption>)

data class MysticSkin(
    val id: String,
    val label: String,
    val detail: String,
    val back: Long,
    val garment: Long,
    val trim: Long,
    val voiceLabel: String,
    val voiceIntro: String,
    val gameLead: String,
    val reactionTail: String,
    val visualStyleId: String = "jiangnan_scholar"
)

/** Cross-day visit notes are kept separate from the generated dialogue. */
data class MysticVisitMemory(
    val lastDateKey: String,
    val lastTopicKey: String,
    val lastAction: String
)
