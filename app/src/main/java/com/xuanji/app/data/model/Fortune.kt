package com.xuanji.app.data.model

/** 东方（八字）每日运势 */
data class EasternDailyFortune(
    val dateKey: String,            // yyyy-MM-dd
    val overallScore: Int,          // 0-100
    val careerScore: Int,
    val wealthScore: Int,
    val loveScore: Int,
    val healthScore: Int,
    val summary: String,
    val advice: String,
    val dayPillarText: String,       // 今日干支
    val favorableToday: List<Element>,
    val luckyColor: String,
    val luckyDirection: String,
    val period: String = "day"       // day / week / month
)

/** 西方（星座）每日运势 */
data class WesternDailyFortune(
    val dateKey: String,
    val sign: String,               // 摩羯座
    val overallScore: Int,
    val careerScore: Int,
    val wealthScore: Int,
    val loveScore: Int,
    val healthScore: Int,
    val summary: String,
    val luckyNumber: Int,
    val luckyColor: String,
    val luckyDirection: String,
    val period: String = "day"       // day / week / month
)
