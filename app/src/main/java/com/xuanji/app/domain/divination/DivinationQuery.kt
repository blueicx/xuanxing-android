package com.xuanji.app.domain.divination

/**
 * 用户主动发起的一次占卜请求。它只包含用于计算的最小输入，
 * 不保存原始姓名或聊天全文，方便在本地历史中做可撤回的摘要。
 */
data class DivinationQuery(
    val system: String,
    val profileKey: String,
    val dateKey: String,
    val question: String = "",
    val spread: String = "single",
    val algorithmVersion: String = "x3-v2"
) {
    fun normalizedQuestion(): String = question.trim().replace(Regex("\\s+"), " ").take(200)

    fun canonicalKey(): String = listOf(
        algorithmVersion,
        system.trim().lowercase(),
        profileKey.trim(),
        dateKey.trim(),
        spread.trim().lowercase(),
        normalizedQuestion().lowercase()
    ).joinToString("|")
}
