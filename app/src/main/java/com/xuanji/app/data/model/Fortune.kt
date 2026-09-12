package com.xuanji.app.data.model

/**
 * 一条「解说依据」：来自体系本身的可追溯信号。
 * tag 标明体系与门类（东方·刑冲 / 东方·十神 / 东方·神煞 / 西方·行运 等），
 * weight 是它对总分的实际加减，做到「说的话」与「算的分」同源。
 */
data class FortuneInsight(
    val tag: String,
    val title: String,
    val body: String,
    val weight: Int
)

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
    val period: String = "day",      // day / week / month / year
    val periodPillarText: String = "",   // 该周期论断所用的干支（日柱/流月/流年）
    val insights: List<FortuneInsight> = emptyList(),
    val dimensionNotes: List<FortuneDimension> = emptyList(),
    /** 每维度的取证短语（career/wealth/love/study/health → 命理事由），供综合页融合 */
    val dimensionBasis: Map<String, List<String>> = emptyMap()
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
    val period: String = "day",      // day / week / month / year
    val insights: List<FortuneInsight> = emptyList(),
    val dimensionNotes: List<FortuneDimension> = emptyList(),
    /** 每维度的取证短语（天象理由），供综合页融合 */
    val dimensionBasis: Map<String, List<String>> = emptyMap()
)
