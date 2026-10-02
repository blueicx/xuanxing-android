package com.xuanji.app.domain

/** A snapshot of the exact, already-rendered result a user is asking about. */
data class MysticDetailContext(
    val mode: String,
    val period: String,
    val dateKey: String,
    val title: String,
    val result: String,
    val evidence: String,
    val sourceStatus: String,
    val algorithmVersion: String
) {
    /** Bounded, display-faithful prompt; it intentionally carries no birth-profile payload. */
    fun toQuestion(): String = buildString {
        appendLine("请解释我当前打开的运势详情，并以这份已显示结果为准，不要重新排盘。")
        appendLine("体系：${mode.cleanField(80)}；周期：${period.cleanField(80)}；日期：${dateKey.cleanField(80)}")
        appendLine("板块：${title.cleanField(160)}")
        appendLine("已显示结果：${result.cleanField(1_600)}")
        appendLine("依据：${evidence.cleanField(1_600).ifBlank { "暂无" }}")
        appendLine("来源状态：${sourceStatus.cleanField(160).ifBlank { "未提供" }}")
        algorithmVersion.cleanField(80).takeIf(String::isNotBlank)?.let { appendLine("算法版本：$it") }
    }.trim().take(MAX_QUESTION_LENGTH)

    private fun String.cleanField(limit: Int): String =
        replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(limit)

    companion object {
        const val MAX_QUESTION_LENGTH = 4_000
    }
}
