package com.xuanji.app.domain.divination

import java.time.LocalDate

/**
 * 吸引力法则（Law of Attraction）评估引擎：
 * 以「目标清晰度 / 情绪状态 / 信念模式 / 行动频率 / 感恩习惯」五维度打分（0-100），
 * 计算加权综合振动频率与等级，给出各维度解读、21 天改进建议、今日肯定语与可视化练习。
 * 肯定语按日期种子确定性选取（无随机，离线可复现）。
 */
object LawOfAttraction {

    data class LoaResult(
        val scores: Map<String, Int>,
        val total: Double,
        val level: String,
        val interpretation: String,
        val verdict: String,
        val advice: List<String>,
        val affirmation: String,
        val visualizationTip: String,
        val date: LocalDate
    )

    // 各维度解读（按得分档）
    private val DIMENSION_INTERPRETATIONS = mapOf(
        "目标清晰度" to mapOf(
            "low" to "你的目标还不够明确。试着写下你真正渴望的事物，越具体越好。",
            "medium" to "你有一定的方向感，但可以更聚焦。每天早晨花 5 分钟清晰观想你的目标。",
            "high" to "你对目标非常清晰，这是吸引力的强大基础。继续保持，并信任宇宙的时机。"
        ),
        "情绪状态" to mapOf(
            "low" to "你常感到焦虑或低落。情绪是振动的核心，先通过冥想、运动或音乐提升频率。",
            "medium" to "情绪有波动，但你能主动调节。多关注生活中微小而美好的事物。",
            "high" to "你拥有积极乐观的情绪基调，这是吸引理想现实的关键。持续保持感恩与喜悦。"
        ),
        "信念模式" to mapOf(
            "low" to "你存在一些限制性信念，如「我不够好」或「我不配拥有」。尝试用肯定语重塑信念。",
            "medium" to "大部分信念积极，但仍有部分怀疑。用证据清单反驳负面信念。",
            "high" to "你坚信自己值得拥有美好，这种信念将加速你的显现。"
        ),
        "行动频率" to mapOf(
            "low" to "你倾向于被动等待。吸引力法则需要你采取启发性的行动，哪怕是小步。",
            "medium" to "你时有行动，但缺乏持续性。建立每日微习惯，保持动能。",
            "high" to "你积极采取受启发的行动，与宇宙共同创造，效果将显著。"
        ),
        "感恩习惯" to mapOf(
            "low" to "你很少停下来感恩。感恩是吸引力法则的加速器，尝试每天写下 3 件感恩的事。",
            "medium" to "你有感恩意识，但未成习惯。建议固定时间进行感恩练习。",
            "high" to "感恩已成为你的生活方式，这极大地提升你的频率，吸引更多丰盛。"
        )
    )

    // 综合评分等级
    private data class Level(val min: Int, val max: Int, val name: String, val desc: String)

    private val LEVELS = listOf(
        Level(90, 100, "极高振动频率", "你正处于强大的吸引状态，宇宙正响应你的频率。继续保持，并留意同步性事件。"),
        Level(70, 89, "良好振动频率", "你走在正确的轨道上，只需微调一些细节，愿望将加速显现。"),
        Level(50, 69, "中等振动频率", "一些阻碍因素在减弱你的吸引力。建议重点关注得分较低的维度，并坚持练习。"),
        Level(30, 49, "低频振动", "你需要大幅调整思想和情绪。请从基础练习开始，如冥想和感恩日记。"),
        Level(0, 29, "低频阻塞", "你可能会感到失望或无助。建议先进行情绪释放练习，并允许自己休息和接纳现状。")
    )

    // 肯定语库（按主题）
    private val AFFIRMATIONS = mapOf(
        "目标" to listOf("我清晰知道我想要什么，宇宙正为我安排。", "我每一天都朝着目标迈进一小步。"),
        "情绪" to listOf("我选择感受喜悦与平静。", "我的情绪是流动的，我允许自己感受并释放。"),
        "信念" to listOf("我值得拥有所有美好。", "我拥有无限的潜能。"),
        "行动" to listOf("我采取启发性的行动，并信任结果。", "我的行动与我的目标一致。"),
        "感恩" to listOf("我感恩现在拥有的一切。", "我吸引更多丰盛进入我的生活。"),
        "通用" to listOf("我值得被爱，值得丰盛。", "宇宙总是支持我。", "我信任生命的流动。")
    )

    private val THEME_MAP = mapOf(
        "目标清晰度" to "目标",
        "情绪状态" to "情绪",
        "信念模式" to "信念",
        "行动频率" to "行动",
        "感恩习惯" to "感恩"
    )

    private val VISUALIZATION_PRACTICES = mapOf(
        "目标清晰度" to "每天早晚闭上眼，用 5 分钟观想自己已经实现目标的具体场景，调动所有感官。",
        "情绪状态" to "闭上眼睛，想象一道金色的光从头顶灌入，充满全身，提升你的振动频率。",
        "信念模式" to "写下你的理想信念，反复阅读并感受其真实性。",
        "行动频率" to "在脑海中预演你将要采取的行动，看到自己顺利完成的画面。",
        "感恩习惯" to "在睡前回想一天中令你感恩的人和事，并感受那份温暖。"
    )

    /** 五维度评估（goal/emotion/belief/action/gratitude 取值 0-100） */
    fun evaluate(
        goal: Int, emotion: Int, belief: Int, action: Int, gratitude: Int,
        date: LocalDate = LocalDate.now()
    ): LoaResult {
        val clamp = { v: Int -> v.coerceIn(0, 100) }
        val scores = linkedMapOf(
            "目标清晰度" to clamp(goal),
            "情绪状态" to clamp(emotion),
            "信念模式" to clamp(belief),
            "行动频率" to clamp(action),
            "感恩习惯" to clamp(gratitude)
        )
        val total = scores.values.sum().toDouble() / 5.0

        val level = LEVELS.first { total.toInt() in it.min..it.max }

        // 详细建议
        val advice = mutableListOf<String>()
        scores.forEach { (dim, score) ->
            val band = when {
                score < 40 -> "low"
                score < 70 -> "medium"
                else -> "high"
            }
            advice.add("${dim}（得分 $score）：${DIMENSION_INTERPRETATIONS.getValue(dim).getValue(band)}")
        }
        advice.add(
            if (total < 70) "💡 整体建议：请从得分最低的维度开始改进，坚持 21 天，你会感受到显著变化。"
            else "🌟 你已掌握吸引力法则的核心，保持这种状态，并持续调整到更高频率。"
        )

        // 最低分维度 → 主题
        val minDim = scores.minByOrNull { it.value }?.key ?: "目标清晰度"
        val theme = THEME_MAP[minDim] ?: "通用"
        val pool = AFFIRMATIONS[theme] ?: AFFIRMATIONS.getValue("通用")

        // 确定性选取：按日期种子（年*372 + 月*31 + 日 + 主题 hash）
        val seed = (date.year * 372 + date.monthValue * 31 + date.dayOfMonth) + theme.hashCode()
        val affirmation = pool[((seed % pool.size) + pool.size) % pool.size]

        val visualizationTip = VISUALIZATION_PRACTICES[minDim]
            ?: "闭上眼睛，想象你理想生活的全景，感受其中的喜悦与和平。"

        val verdict = buildVerdict(scores, total, level.name, level.desc, minDim)

        return LoaResult(
            scores = scores,
            total = Math.round(total * 10.0) / 10.0,
            level = level.name,
            interpretation = level.desc,
            verdict = verdict,
            advice = advice,
            affirmation = affirmation,
            visualizationTip = visualizationTip,
            date = date
        )
    }

    /** 按维度得分给出事业一句话 */
    private fun careerLine(goal: Int, action: Int): String = when {
        goal >= 70 && action >= 70 -> "目标与行动同频，事业显化的动能强劲，宜乘势推进手头的计划"
        goal >= 70 -> "愿景清晰但行动偏缓，把大目标拆成每日微行动，能明显提速"
        action >= 70 -> "行动力在线但方向略散，先锚定一个明确目标再发力"
        else -> "事业面的磁场偏弱，宜先厘清真正想要的方向，再以小步行动累积动能"
    }

    /** 按信念与感恩得分给出财运一句话 */
    private fun wealthLine(belief: Int, gratitude: Int): String = when {
        belief >= 70 && gratitude >= 70 -> "对丰盛的信念与感恩的习惯都充足，财富磁场畅通，正财与机遇会如期而至"
        belief < 40 -> "「不配得」式的限制性信念是财路的主要卡点，先重塑信念再谈进账"
        gratitude < 40 -> "感恩不足让丰盛难以驻留，从每晚写下三件感恩小事开始滋养财气"
        else -> "财富能量中等，提升对已有之物的感谢，能放大现有的进账与机会"
    }

    /** 按情绪得分给出感情一句话 */
    private fun loveLine(emotion: Int): String = when {
        emotion >= 70 -> "情绪高频而稳定，自然散发吸引力，亲密关系宜保持流动与真诚"
        emotion >= 40 -> "情绪有起落但可自我调节，感情中多表达真实感受，别把不安藏在心里"
        else -> "低频情绪易让关系蒙尘，先照顾好自己，再谈与他人靠近"
    }

    /** 按情绪得分给出健康一句话 */
    private fun healthLine(emotion: Int): String = when {
        emotion >= 70 -> "情绪平和滋养身体，保持作息与运动的节奏即可"
        emotion >= 40 -> "情绪波动会先反映在睡眠与肠胃，宜以冥想或散步及时释放"
        else -> "长期低频情绪易耗损身心，先做情绪释放练习，允许自己休息与接纳"
    }

    /** 六维解读：总评 + 事业/财运/感情/健康/建议，贴合振动频率与五维度主题 */
    private fun buildVerdict(
        scores: Map<String, Int>,
        total: Double,
        levelName: String,
        levelDesc: String,
        minDim: String
    ): String {
        val goal = scores["目标清晰度"] ?: 50
        val emotion = scores["情绪状态"] ?: 50
        val belief = scores["信念模式"] ?: 50
        val action = scores["行动频率"] ?: 50
        val gratitude = scores["感恩习惯"] ?: 50
        val totalRound = Math.round(total * 10.0) / 10.0
        val sb = StringBuilder()
        sb.append("总评：你的综合振动频率为 $totalRound 分，属「$levelName」，$levelDesc")
        sb.append("\n事业：目标清晰度 $goal 分、行动频率 $action 分，${careerLine(goal, action)}")
        sb.append("\n财运：信念模式 $belief 分、感恩习惯 $gratitude 分，${wealthLine(belief, gratitude)}")
        sb.append("\n感情：情绪状态 $emotion 分，${loveLine(emotion)}")
        sb.append("\n健康：情绪是身体的先导，${healthLine(emotion)}")
        sb.append("\n建议：优先改善得分最低的「$minDim」，配合今日肯定语与可视化练习，坚持 21 天后再测一次频率")
        return sb.toString()
    }
}
