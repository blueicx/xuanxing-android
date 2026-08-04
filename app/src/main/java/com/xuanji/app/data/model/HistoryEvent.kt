package com.xuanji.app.data.model

/**
 * 历史上的今天条目。date 为 "MM-dd" 形式，用于匹配当前月日。
 * year 为可空：历史事件填具体年份（如 "1949"、"公元前221年"）；节气/民俗等年度性条目可留空。
 */
data class HistoryEvent(
    val date: String,
    val title: String,
    val desc: String,
    val tag: String,
    val year: String? = null
)
