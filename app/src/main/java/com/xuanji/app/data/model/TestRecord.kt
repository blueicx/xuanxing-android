package com.xuanji.app.data.model

/**
 * 一次测试的结果记录。category: 职业 / 性格 / 趣味，用于分组展示。
 */
data class TestRecord(
    val testName: String,   // 如 "MBTI 职业性格测试"
    val category: String,   // 职业 / 性格 / 趣味
    val resultCode: String, // 如 "INTJ"、"大五·高开放"、"格兰芬多"
    val resultName: String, // 一句话结果/解读摘要
    val date: String        // "yyyy-MM-dd HH:mm"
)
