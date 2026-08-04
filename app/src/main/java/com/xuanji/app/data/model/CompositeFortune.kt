package com.xuanji.app.data.model

/**
 * 综合运势：融合东方八字与西方星座的每日运势。
 * 所有评分 0-100，按日期确定性生成（离线可用）。
 */
data class CompositeDailyFortune(
    val dateKey: String,                       // yyyy-MM-dd
    val overallScore: Int,                      // 综合分
    val dimensions: List<FortuneDimension>,     // 桃花运/情感/事业/学习/财富/健康
    val luckyNumber: Int,                       // 幸运数字
    val luckyColor: String,                     // 幸运色
    val luckyDirection: String,                 // 吉利方位
    val cautions: String,                       // 注意事项
    val eastern: EasternDailyFortune,           // 东方来源（用于标签展示）
    val western: WesternDailyFortune            // 西方来源
)

/** 单个运势维度 */
data class FortuneDimension(
    val key: String,        // peach / emotion / career / study / wealth / health
    val label: String,      // 桃花运
    val score: Int,         // 0-100
    val interpretation: String
)
