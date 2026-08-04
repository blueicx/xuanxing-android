package com.xuanji.app.domain.test

import kotlin.math.roundToInt

/**
 * MMPI（明尼苏达多相人格测验，通识简化版）
 * 14 个量表：效度 Q/L/F/K + 临床 Hs/D/Hy/Pd/Mf/Pa/Pt/Sc/Ma/Si，共 20 题。
 * 每题作答「是/否」：key=false 表示「是」计分，key=true 表示「否」计分。
 * 原始分做 K 校正（Hs+0.5K、Pd+0.4K、Pt+1K、Sc+1K、Ma+0.2K），
 * 再按 T=50+10*(raw-mean)/sd 换算 T 分（常模 mean/sd 见 NORM），T≥60 判偏高。
 * 全离线、确定性计分，结果仅供自我探索参考，不构成医学诊断。
 */

data class MmpiQuestion(val text: String, val scale: String?, val key: Boolean)

data class ScaleResult(
    val code: String,
    val name: String,
    val raw: Int,
    val kCorrected: Double,
    val tScore: Int,
    val interpretation: String
)

data class MmpiResult(
    val scales: List<ScaleResult>,
    val valid: Boolean,
    val invalidReasons: List<String>,
    val conclusion: String
)

object Mmpi {
    val QUESTIONS: List<MmpiQuestion> = listOf(
        // L 效度 1-2
        MmpiQuestion("我从未在背后说过别人的坏话。", "L", false),
        MmpiQuestion("我每一天都感到快乐而满足。", "L", false),
        // F 效度 3
        MmpiQuestion("我常常觉得自己被人暗中监视或跟踪。", "F", false),
        // K 效度 4
        MmpiQuestion("我对自己非常满意，几乎从不后悔自己的决定。", "K", false),
        // Hs 疑病 5-6
        MmpiQuestion("我时常感到身体莫名疼痛，但检查不出明确原因。", "Hs", false),
        MmpiQuestion("我的胃口一直很好，很少有肠胃不适。", "Hs", true),
        // D 抑郁 7-8
        MmpiQuestion("我常常觉得生活没有希望，心情难以振奋。", "D", false),
        MmpiQuestion("我容易入睡，夜里也很少醒来。", "D", true),
        // Hy 癔病 9
        MmpiQuestion("遇到压力时，我常常会感到头痛或身体不适。", "Hy", false),
        // Pd 精神病态 10
        MmpiQuestion("我常常对规则和权威感到不耐烦。", "Pd", false),
        // Mf 男性化/女性化 11
        MmpiQuestion("我对文学、艺术和音乐有浓厚的兴趣。", "Mf", false),
        // Pa 偏执 12
        MmpiQuestion("我常常觉得别人在背后议论或针对我。", "Pa", false),
        // Pt 精神衰弱 13-15
        MmpiQuestion("我经常反复思考同一件事，很难停止担忧。", "Pt", false),
        MmpiQuestion("做决定时我很少犹豫，能很快定下来。", "Pt", true),
        MmpiQuestion("我有时会无缘无故地紧张或心慌。", "Pt", false),
        // Sc 精神分裂 16-17
        MmpiQuestion("我有时会听到别人听不到的声音或看到奇怪的东西。", "Sc", false),
        MmpiQuestion("我常常觉得自己的念头不受控制地涌现。", "Sc", false),
        // Ma 轻躁狂 18
        MmpiQuestion("我精力充沛，常常忙个不停。", "Ma", false),
        // Si 社会内向 19-20
        MmpiQuestion("我宁愿独处，也不太喜欢参加集体活动。", "Si", false),
        MmpiQuestion("在热闹的场合中我感到很自在。", "Si", true)
    )

    val SCALE_ORDER: List<String> = listOf(
        "Q", "L", "F", "K", "Hs", "D", "Hy", "Pd", "Mf", "Pa", "Pt", "Sc", "Ma", "Si"
    )

    val SCALE_NAMES: Map<String, String> = mapOf(
        "Q" to "疑问", "L" to "说谎", "F" to "诈病（罕见回答）", "K" to "校正",
        "Hs" to "疑病", "D" to "抑郁", "Hy" to "癔病", "Pd" to "精神病态",
        "Mf" to "男性化/女性化", "Pa" to "偏执", "Pt" to "精神衰弱",
        "Sc" to "精神分裂", "Ma" to "轻躁狂", "Si" to "社会内向"
    )

    /** 常模：mean/sd（本简化版按题量推算的参考值） */
    private val NORM: Map<String, Pair<Double, Double>> = mapOf(
        "Q" to (0.0 to 1.0),
        "L" to (1.0 to 0.7),
        "F" to (0.15 to 0.35),
        "K" to (0.4 to 0.5),
        "Hs" to (0.6 to 0.6),
        "D" to (0.6 to 0.6),
        "Hy" to (0.3 to 0.46),
        "Pd" to (0.3 to 0.46),
        "Mf" to (0.5 to 0.5),
        "Pa" to (0.2 to 0.4),
        "Pt" to (1.0 to 0.9),
        "Sc" to (0.5 to 0.6),
        "Ma" to (0.4 to 0.5),
        "Si" to (0.9 to 0.6)
    )

    /** K 校正系数：key=量表，value=乘 K 原始分的系数 */
    private val K_FACTOR: Map<String, Double> = mapOf(
        "Hs" to 0.5, "Pd" to 0.4, "Pt" to 1.0, "Sc" to 1.0, "Ma" to 0.2
    )

    private val CLINICAL_CODES = listOf("Hs", "D", "Hy", "Pd", "Mf", "Pa", "Pt", "Sc", "Ma", "Si")

    private fun computeRaw(answers: List<Boolean>, scale: String): Int {
        var sum = 0
        QUESTIONS.forEachIndexed { i, q ->
            if (q.scale == scale) {
                val ans = answers.getOrNull(i) ?: return@forEachIndexed
                // key=false →「是」计分；key=true →「否」计分
                if (ans == !q.key) sum++
            }
        }
        return sum
    }

    private val VALIDITY_TEXT: Map<String, String> = mapOf(
        "Q" to "Q（疑问）量表反映漏答或无法作答的题目数量。本次测评所有题目均已作答，Q 分正常，结果完整可靠。",
        "L" to "L（说谎）量表反映个体是否刻意塑造完美形象。L 分偏高时，可能提示自我美化、不愿承认缺点；L 分正常时，说明作答较为坦诚。",
        "F" to "F（诈病/罕见回答）量表反映作答的真实性与一致性。F 分偏高时，可能提示作答随意、误解题意，或情绪困扰较重；F 分正常时，说明回答真实可信。",
        "K" to "K（校正）量表反映个体的防御倾向与自我评价。K 分偏高时，提示防御性较强、可能不愿表露内心；K 分偏低时，提示自我批评倾向明显。"
    )

    private val CLINICAL_TEXT: Map<String, Pair<String, String>> = mapOf(
        "Hs" to (
            "你的 Hs（疑病）量表偏高，提示你较多关注身体感受，容易把压力与不适归结为躯体症状。建议在排除器质性疾病后，留意情绪因素对身体的影响。" to
                "你的 Hs（疑病）量表得分正常，说明你较少被身体不适困扰，对健康状况抱有较为平和的心态。"),
        "D" to (
            "你的 D（抑郁）量表偏高，近期可能情绪低落、兴趣减退，容易感到疲惫与悲观。建议适度调整作息、增加活动与倾诉，必要时寻求专业帮助。" to
                "你的 D（抑郁）量表得分正常，说明你的情绪基调较为平稳，不易陷入长期的悲观与低落。"),
        "Hy" to (
            "你的 Hy（癔病）量表偏高，提示你在压力下可能以躯体不适或回避的方式应对，情绪表达较为内隐。学会直接表达感受有助于减轻身心负担。" to
                "你的 Hy（癔病）量表得分正常，说明你应对压力的方式较为直接，较少把情绪问题转化为身体不适。"),
        "Pd" to (
            "你的 Pd（精神病态）量表偏高，提示你较反感规则与权威，行为可能更随性、更具反叛倾向。保持原则性与自律，有助于人际关系与长期发展。" to
                "你的 Pd（精神病态）量表得分正常，说明你尊重规则、注重社会规范，行为方式较为成熟稳健。"),
        "Mf" to (
            "你的 Mf（男性化/女性化）量表偏高，提示你对艺术、审美与情感细腻的事物有较多兴趣，兴趣结构较为多元、包容。" to
                "你的 Mf（男性化/女性化）量表得分正常，说明你的兴趣与行为方式与传统性别角色较为一致，自我认同清晰。"),
        "Pa" to (
            "你的 Pa（偏执）量表偏高，提示你较为敏感多疑，容易把他人的言行解读为针对自己。适度降低戒备、多核实事实，可减少误解。" to
                "你的 Pa（偏执）量表得分正常，说明你对他人的信任度较高，较少怀疑他人动机。"),
        "Pt" to (
            "你的 Pt（精神衰弱）量表偏高，提示你容易担忧、反复思虑，常为小事焦虑，伴有紧张与不安全感。练习正念、合理设定目标有助于缓解。" to
                "你的 Pt（精神衰弱）量表得分正常，说明你思虑适度，做决定较为果断，较少被焦虑困扰。"),
        "Sc" to (
            "你的 Sc（精神分裂）量表偏高，提示你的思维与感受可能与周围人有些不同，易感孤独或被误解。若明显影响生活，建议寻求专业评估。" to
                "你的 Sc（精神分裂）量表得分正常，说明你的思维清晰连贯，感知与大多数人保持一致。"),
        "Ma" to (
            "你的 Ma（轻躁狂）量表偏高，提示你精力旺盛、思维活跃、行动力强，但也可能因此难以安静、冲动易怒。注意节奏与休息。" to
                "你的 Ma（轻躁狂）量表得分正常，说明你的精力水平适中，行动有度，不会过度亢奋。"),
        "Si" to (
            "你的 Si（社会内向）量表偏高，提示你偏内向，喜欢独处，在社交中较为拘谨。内向本身是健康的特质，保持适合的社交节奏即可。" to
                "你的 Si（社会内向）量表得分正常，说明你在社交中较为自在，乐于与人往来。")
    )

    private fun interpretation(code: String, tScore: Int): String {
        VALIDITY_TEXT[code]?.let { return it }
        val (high, low) = CLINICAL_TEXT[code]!!
        return if (tScore >= 60) high else low
    }

    /** 计分：answers 为每题「是/否」的选择（true=是，false=否） */
    fun calculate(answers: List<Boolean>): MmpiResult {
        val kRaw = computeRaw(answers, "K")
        val scales = SCALE_ORDER.map { code ->
            val raw = if (code == "Q") (QUESTIONS.size - answers.size).coerceAtLeast(0) else computeRaw(answers, code)
            val kCorrected = K_FACTOR[code]?.let { raw + it * kRaw } ?: raw.toDouble()
            val (mean, sd) = NORM[code]!!
            val t = (50 + 10 * (kCorrected - mean) / sd).roundToInt()
            ScaleResult(code, SCALE_NAMES[code]!!, raw, kCorrected, t, interpretation(code, t))
        }
        val q = scales.first { it.code == "Q" }
        val invalidReasons = mutableListOf<String>()
        if (q.raw > 3) invalidReasons.add("漏答题数较多（${q.raw} 题），结果可能不准确。")
        val valid = invalidReasons.isEmpty()
        val conclusion = buildConclusion(scales, valid, invalidReasons)
        return MmpiResult(scales, valid, invalidReasons, conclusion)
    }

    private fun buildConclusion(scales: List<ScaleResult>, valid: Boolean, invalidReasons: List<String>): String {
        if (!valid) return invalidReasons.joinToString("")
        val t = scales.associate { it.code to it.tScore }
        val sb = StringBuilder()
        if (t["L"]!! >= 65) sb.append("L 量表偏高，可能有自我美化倾向；")
        if (t["F"]!! >= 70) sb.append("F 量表偏高，作答可能存在不真实或随意成分；")
        if (t["K"]!! >= 65) sb.append("K 量表偏高，防御性较强，结果可能有所保留；")
        val elevated = scales
            .filter { it.code in CLINICAL_CODES && it.tScore >= 60 }
            .sortedByDescending { it.tScore }
            .take(2)
        if (elevated.isNotEmpty()) {
            sb.append("临床量表中，${elevated[0].name}（T=${elevated[0].tScore}）偏高")
            if (elevated.size > 1) sb.append("，${elevated[1].name}（T=${elevated[1].tScore}）也偏高")
            sb.append("。建议结合日常生活状态综合看待，若持续困扰请寻求专业心理帮助。")
        } else {
            sb.append("各临床量表均在正常范围（T<60），说明你的心理健康状况总体平稳。")
        }
        return sb.toString()
    }
}
