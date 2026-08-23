package com.xuanji.app.domain

import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.TestRecord
import com.xuanji.app.domain.divination.TodayOracle
import kotlin.math.roundToInt
import java.time.LocalDate

data class MysticGuide(
    val mode: String,
    val topicKey: String,
    val roleName: String,
    val signature: String,
    val headline: String,
    val body: String
)

/**
 * 双面灵语：玄学家负责基于现有算法结果做心理按摩，半仙负责浮夸调侃。
 * 不使用随机数；同一个人、同一天、同一问题、同一模式必然得到同一回答。
 */
object MysticGuideGenerator {
    private val topics = linkedMapOf(
        "composite" to "综合",
        "career" to "事业",
        "love" to "感情",
        "wealth" to "财富",
        "study" to "学习",
        "health" to "健康",
        "test" to "测试"
    )

    fun topicLabels(): List<Pair<String, String>> = topics.map { it.key to it.value }

    fun generate(
        mode: String,
        topicKey: String,
        bazi: BaziFull,
        fortune: CompositeDailyFortune,
        test: TestRecord? = null,
        divinationSummary: String? = null
    ): MysticGuide {
        val label = topics[topicKey] ?: "综合"
        val focus = if (topicKey == "test") {
            fortune.dimensions.maxByOrNull { it.score } ?: fortune.dimensions.first()
        } else {
            fortune.dimensions.firstOrNull { it.key == topicKey }
                ?: fortune.dimensions.firstOrNull { it.key == "emotion" && topicKey == "love" }
                ?: fortune.dimensions.first()
        }
        val eastScore = when (topicKey) {
            "career" -> fortune.eastern.careerScore
            "wealth" -> fortune.eastern.wealthScore
            "love", "test" -> fortune.eastern.loveScore
            "health" -> fortune.eastern.healthScore
            "study" -> ((fortune.eastern.careerScore + fortune.eastern.overallScore) / 2.0).roundToInt()
            else -> fortune.eastern.overallScore
        }
        val westScore = when (topicKey) {
            "career" -> fortune.western.careerScore
            "wealth" -> fortune.western.wealthScore
            "love", "test" -> fortune.western.loveScore
            "health" -> fortune.western.healthScore
            "study" -> ((fortune.western.careerScore + fortune.western.overallScore) / 2.0).roundToInt()
            else -> fortune.western.overallScore
        }
        val high = fortune.dimensions.maxByOrNull { it.score } ?: focus
        val low = fortune.dimensions.minByOrNull { it.score } ?: focus
        val oracle = TodayOracle.generate(LocalDate.now())
        val facts = buildList {
            add(
                "东方盘：${bazi.chart.dayMaster.chinese}（${elementName(bazi.chart.dayMasterElement)}），" +
                    "${bazi.strength.level}；今日${fortune.eastern.dayPillarText}，喜${bazi.yongJi.useful.joinToString("、") { elementName(it) }}。"
            )
            add("西方盘：太阳${fortune.western.sign}，今日整体 ${fortune.western.overallScore} 分。")
            add("综合盘：${fortune.overallScore} 分；最强是${high.label} ${high.score}，最需照看是${low.label} ${low.score}。")
            add("今日灵签：${oracle.level}·「${oracle.poem}」；宜${oracle.good}，忌${oracle.avoid}。")
            if (!divinationSummary.isNullOrBlank()) add("占卜参照：${divinationSummary.trim()}")
            if (test != null) add("最近测试：${test.testName} → ${test.resultCode}（${test.resultName}）")
        }
        val scholar = mode != "half"
        val headline = if (scholar) {
            scholarHeadline(focus.score, label)
        } else {
            halfHeadline(focus.score, label)
        }
        val body = if (scholar) {
            "我把「${label}」放回完整命盘看：综合 ${focus.score} 分，东方 $eastScore 分，西方 $westScore 分。" +
                bandSentence(focus.score) +
                if (focus.score >= low.score && low.score < 55) {
                    "真正想被照顾的是「${low.label}」，今天给它一个十分钟的小承诺就够了。"
                } else {
                    "你不需要立刻变成另一个人，只要让已有的稳定继续发生。"
                }
        } else {
            "天界吐槽频道已锁定「${label}」：综合 ${focus.score} 分，东方 $eastScore 分，西方 $westScore 分！" +
                halfBandSentence(focus.score) +
                if (high.score >= 65) "「${high.label}」简直在冒仙气，别端着了，赶紧去接住这波排面！" else "连半仙都看不下去啦，先别硬冲，留点力气明天封神！"
        }

        return MysticGuide(
            mode = mode,
            topicKey = topicKey,
            roleName = if (scholar) "玄学家" else "半仙",
            signature = if (scholar) "心理按摩 · 确定性解读" else "天界吐槽办事处 · 浮夸但讲逻辑",
            headline = headline,
            body = "$body\n\n${facts.joinToString("\n")}"
        )
    }

    private fun scholarHeadline(score: Int, label: String): String = when {
        score >= 80 -> "${label}有势能，你可以安心接住"
        score >= 65 -> "${label}方向清楚，节奏可以温柔些"
        score >= 50 -> "${label}正在蓄力，不必逼它开花"
        score >= 35 -> "${label}需要小步确认，而不是大步证明"
        else -> "先把${label}安顿好，再安排世界"
    }

    private fun halfHeadline(score: Int, label: String): String = when {
        score >= 80 -> "不得了！${label}直接踩着祥云起飞"
        score >= 65 -> "${label}火力在线，神仙都要侧目"
        score >= 50 -> "${label}稳如老君炉，别慌"
        score >= 35 -> "${label}有点闹脾气，得哄"
        else -> " ${label}暂时躲进云里充电了"
    }

    private fun bandSentence(score: Int): String = when {
        score >= 80 -> "现在的关键不是怀疑机会，而是把注意力放在能让你稳定发挥的选择上。"
        score >= 65 -> "推进是合适的，只是把期待拆成几个可完成的小节点，会更轻松。"
        score >= 50 -> "平稳不代表平淡，它给你空间整理节奏、修补细节。"
        score >= 35 -> "低分不是否定，而是身体和情绪在提醒你收缩战线。"
        else -> "此刻最有效的行动是休息、求助和把任务缩小到不会吓跑自己的程度。"
    }

    private fun halfBandSentence(score: Int): String = when {
        score >= 80 -> "这分数都快溢出八卦炉了，好运追着你跑，记得留个门！"
        score >= 65 -> "运势小火苗烧得很旺，适合把计划端上桌，别让它干等！"
        score >= 50 -> "不惊不喜，像一碗温吞仙汤，喝完照样能走路带风。"
        score >= 35 -> "星星在天上挤眉弄眼：今天别硬闯，绕个路更灵光！"
        else -> "云层信号有点差，宜躺平回血，不宜跟命运掰手腕！"
    }
}
