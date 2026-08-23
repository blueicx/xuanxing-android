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
    val body: String,
    val evidence: List<String> = emptyList(),
    val followUps: List<MysticFollowUp> = emptyList()
)

data class MysticFollowUp(
    val key: String,
    val question: String,
    val answer: String
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

        val followUps = listOf(
            MysticFollowUp(
                key = "why",
                question = "这个数怎么来？",
                answer = whyAnswer(scholar, label, focus.score, eastScore, westScore)
            ),
            MysticFollowUp(
                key = "action",
                question = "现在怎么做？",
                answer = actionAnswer(scholar, high.label, low.label, fortune.luckyColor, fortune.luckyDirection)
            ),
            MysticFollowUp(
                key = "care",
                question = "要留意什么？",
                answer = careAnswer(scholar, low.label, fortune.cautions)
            ),
            MysticFollowUp(
                key = "focus",
                question = "${label}怎么破？",
                answer = topicAnswer(
                    scholar,
                    topicKey,
                    label,
                    focus.score,
                    high.label,
                    low.label,
                    test?.testName.orEmpty()
                )
            )
        )

        return MysticGuide(
            mode = mode,
            topicKey = topicKey,
            roleName = if (scholar) "玄学家" else "半仙",
            signature = if (scholar) "只讲盘面依据 · 仅供娱乐参考" else "浮夸但讲逻辑 · 仅供娱乐参考",
            headline = headline,
            body = body,
            evidence = facts,
            followUps = followUps
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

    private fun whyAnswer(
        scholar: Boolean,
        label: String,
        focusScore: Int,
        eastScore: Int,
        westScore: Int
    ): String {
        val gap = kotlin.math.abs(eastScore - westScore)
        val stronger = if (eastScore >= westScore) "东方盘" else "西方盘"
        val weaker = if (eastScore >= westScore) "西方盘" else "东方盘"
        return if (scholar) {
            "${label}的 $focusScore 分来自两侧交叉核对：东方 $eastScore 分，西方 $westScore 分。" +
                if (gap >= 20) {
                    "${stronger}更给力，${weaker}偏保守；不必硬选一边，先让稳的那边带路。"
                } else {
                    "两边口径接近，说明这个判断比较稳，可以放心当作今天的参照。"
                }
        } else {
            "别看只是一个 $focusScore，背后可是东方 $eastScore 分、西方 $westScore 分在开会！" +
                if (gap >= 20) {
                    "${stronger}嗓门最大，${weaker}在旁边泼温水；先听强的，也别把弱的锁门外。"
                } else {
                    "两边意见罕见一致，这信号可信度直接拉满！"
                }
        }
    }

    private fun actionAnswer(scholar: Boolean, highLabel: String, lowLabel: String, color: String, direction: String): String =
        if (scholar) {
            "先给「$lowLabel」十分钟的照看，再做一件能让「$highLabel」落地的小事。" +
                "今天可用「$color」和「$direction」当状态开关：换颜色、调座位或出门方向，都是提醒自己切换节奏。"
        } else {
            "给「$lowLabel」递杯仙气水，再让「$highLabel」冲锋！" +
                "记得带上「$color」，往「$direction」挪一挪；这不是魔法命令，是给你换个心理档位。"
        }

    private fun careAnswer(scholar: Boolean, lowLabel: String, cautions: String): String {
        val cleanCaution = cautions.trim().ifBlank { "保持规律，别把日程塞太满" }
        return if (scholar) {
            "盘面提醒的重点是「$lowLabel」：$cleanCaution。" +
                "这些是倾向描述，不是判决；如果状态持续不舒服，请优先休息或寻求专业帮助。"
        } else {
            "天界小黑板写的是「$lowLabel」：$cleanCaution！" +
                "半仙只负责敲锣，不负责吓人；真不舒服就去休息，别硬撑成苦瓜。"
        }
    }

    private fun topicAnswer(
        scholar: Boolean,
        topicKey: String,
        label: String,
        focusScore: Int,
        highLabel: String,
        lowLabel: String,
        testName: String
    ): String {
        val strong = focusScore >= 65
        val mid = focusScore >= 35 && !strong
        val opener = when {
            strong -> "「$label」有空间"
            mid -> "「$label」适合小步走"
            else -> "「$label」要先减负"
        }
        return if (scholar) {
            val tail = when (topicKey) {
                "composite" -> "把注意力放在「$highLabel」，同时给「$lowLabel」留缓冲。"
                "career" -> "挑一件最重要的事推进，沟通时把需求说清楚，比同时开五个头更有力。"
                "love" -> "少一点猜测，多一点具体表达；关系里的安全感的来源之一是把话说开。"
                "wealth" -> "先守住必要支出，再考虑尝试；金额越小，决策越清醒。"
                "study" -> "把目标切成二十五分钟的小段，先完成一次回顾，再谈突破。"
                "health" -> "优先睡眠、饮食和活动量；身体信号值得被认真对待。"
                "test" -> "可以把「${testName.ifBlank { "最近测试" }}」当自我观察材料，与命盘互相参照，不单独下结论。"
                else -> "结合「$highLabel」推进，同时照看「$lowLabel」。"
            }
            "$opener。$tail"
        } else {
            val tail = when (topicKey) {
                "composite" -> "「$highLabel」举火把，「$lowLabel」坐轿子，路线已经很清楚啦！"
                "career" -> "主打一招，别十八般武艺同时抡；把关键话说漂亮，胜过加班到冒烟。"
                "love" -> "直球可以扔，阴阳怪气快收起来；具体说想要什么，才不会被误会的云雾罩住。"
                "wealth" -> "钱包系好绳，小额定投快乐可以，大额冲动先冷冻三天。"
                "study" -> "番茄钟启动！先把最烦的那块啃一小口，成就感会自动续杯。"
                "health" -> "仙体也要保养：早点躺，好好吃，动一动，别和沙发签订永久契约。"
                "test" -> "「${testName.ifBlank { "最近测试" }}」只是镜子，不是审判书；拿来认识自己刚刚好。"
                else -> "让「$highLabel」打头阵，别把「$lowLabel」丢在后山。"
            }
            "$opener！$tail"
        }
    }
}
