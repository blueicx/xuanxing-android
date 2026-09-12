package com.xuanji.app.ui.history

/** 供历史页折叠卡片复用的短文案规则，保持纯 Kotlin 便于单测。 */
object HistoryCopy {
    fun summary(text: String): String {
        val normalized = text.trim()
        if (normalized.isEmpty()) return ""
        val end = normalized.indexOfFirst { it == '。' || it == '！' || it == '？' }
        return if (end >= 0) normalized.substring(0, end + 1) else normalized
    }
}
