package com.xuanji.app.domain.divination

/**
 * 欧洲手相学（Chiromancy）确定性计算。
 *
 * 基于用户的 Python 基础实现移植并完善：
 *  - 手掌形状（四元素派）/ 手指比例 / 拇指大小 / 皮肤纹理；
 *  - 生命线 / 智慧线 / 感情线 / 命运线 / 健康线 / 太阳线；
 *  - 综合解读：从各特征解读文本提取关键词，拼成性格总结并附针对性建议。
 * 全部离线、确定性，无随机，仅供文化娱乐参考。
 */

// ======================== 数据 ========================

private val HAND_SHAPES = mapOf(
    "方形" to "务实、稳定、有条理，喜欢结构和规则。",
    "长方形" to "灵活、适应力强，善于沟通和变化。",
    "圆形" to "富有同情心、情感丰富，喜欢社交和艺术。",
    "圆锥形" to "敏感、理想化，有创造力但容易情绪波动。"
)

private val FINGER_LENGTH = mapOf(
    "短" to "中指长度小于手掌宽度，性格果断、直率，喜欢行动而非思考。",
    "中" to "手指与手掌比例协调，性格平衡，既务实又灵活。",
    "长" to "中指长度大于手掌宽度，善于思考、分析，有耐心但可能过于谨慎。"
)

private val THUMB_SIZE = mapOf(
    "大" to "意志坚定、自信，有领导力，不易动摇。",
    "小" to "犹豫不决、依赖性强，容易受他人影响。",
    "中" to "平衡的意志力，能适应环境，但有主见。"
)

private val LIFE_LINE = mapOf(
    "长" to "生命力旺盛，身体健康，耐力持久。",
    "短" to "精力有限，需注意健康，但可能精神能量很强。",
    "链状" to "健康波动大，精力不稳定，容易疲劳。",
    "分叉" to "人生有重大转折，可能向不同方向发展。",
    "双重" to "生命力特别强，有良好的支持系统。"
)

private val HEAD_LINE = mapOf(
    "直" to "逻辑性强，思维直接，注重事实。",
    "弯曲" to "直觉型，创意丰富，善于联想。",
    "长" to "思维深入，喜欢研究复杂问题。",
    "短" to "思维敏捷，但注意力持续时间短。",
    "与生命线结合" to "谨慎、注重安全，容易受家庭影响。",
    "与生命线分离" to "独立、自由，追求个人空间。"
)

private val HEART_LINE = mapOf(
    "长" to "感情丰富，表达自如，善于建立关系。",
    "短" to "情感内敛，独立，不轻易表露。",
    "直" to "理性对待感情，喜欢平等关系。",
    "波状" to "情感波动大，浪漫而多情。",
    "分叉" to "感情之路有选择或分歧。"
)

private val FATE_LINE = mapOf(
    "存在" to "有明确的人生目标，事业心强。",
    "深" to "事业成功，运势稳定。",
    "浅" to "事业方向易变，需努力才能成功。",
    "中断" to "事业有挫折，或转折点。",
    "不存在" to "生活随遇而安，不追求传统成就。"
)

private val HEALTH_LINE = mapOf(
    "存在" to "需关注健康，可能有慢性问题。",
    "不存在" to "身体素质较好，无需过度担忧。",
    "清晰" to "健康意识强，能及早发现问题。"
)

private val SUN_LINE = mapOf(
    "存在" to "有艺术天赋，容易获得认可。",
    "清晰" to "创造力强，人生有亮点。",
    "不存在" to "更注重私人生活，不追求公众赞誉。"
)

private val SKIN_TEXTURE = mapOf(
    "粗糙" to "务实、实际，注重物质世界。",
    "光滑" to "敏感、细腻，注重情感与美感。"
)

/** 特征 → 选项 → 解读 */
private val FEATURE_TABLES: Map<String, Map<String, String>> = mapOf(
    "hand_shape" to HAND_SHAPES,
    "finger_length" to FINGER_LENGTH,
    "thumb" to THUMB_SIZE,
    "life_line" to LIFE_LINE,
    "head_line" to HEAD_LINE,
    "heart_line" to HEART_LINE,
    "fate_line" to FATE_LINE,
    "health_line" to HEALTH_LINE,
    "sun_line" to SUN_LINE,
    "skin_texture" to SKIN_TEXTURE
)

/** 特征中文名 */
val FEATURE_LABELS: Map<String, String> = mapOf(
    "hand_shape" to "手掌形状", "finger_length" to "手指长度", "thumb" to "拇指大小",
    "life_line" to "生命线", "head_line" to "智慧线", "heart_line" to "感情线",
    "fate_line" to "命运线", "health_line" to "健康线", "sun_line" to "太阳线",
    "skin_texture" to "皮肤纹理"
)

/** 各特征可选值（供 UI 展示） */
val FEATURE_OPTIONS: Map<String, List<String>> = FEATURE_TABLES.mapValues { it.value.keys.toList() }

/** 默认特征 */
val DEFAULT_FEATURES: Map<String, String> = mapOf(
    "hand_shape" to "方形", "finger_length" to "中", "thumb" to "中",
    "life_line" to "长", "head_line" to "直", "heart_line" to "长",
    "fate_line" to "存在", "health_line" to "不存在", "sun_line" to "不存在",
    "skin_texture" to "光滑"
)

// ======================== 结果模型 ========================

data class PalmistryFeature(val key: String, val label: String, val value: String, val interpretation: String)

data class PalmistryReport(
    val features: List<PalmistryFeature>,
    val summary: String
)

// ======================== 核心计算 ========================

object Palmistry {

    /** 生成报告 */
    fun generate(features: Map<String, String>): PalmistryReport {
        val list = FEATURE_LABELS.keys.map { key ->
            val value = features[key] ?: DEFAULT_FEATURES[key] ?: ""
            PalmistryFeature(key, FEATURE_LABELS[key] ?: key, value, FEATURE_TABLES[key]?.get(value) ?: "")
        }
        return PalmistryReport(list, summary(list))
    }

    /** 取单个特征某选项的解读（供 UI 内联展示）。 */
    fun interpretFeature(key: String, option: String): String =
        FEATURE_TABLES[key]?.get(option) ?: ""

    /** 综合解读：抽取关键词 + 针对性建议 */
    private fun summary(features: List<PalmistryFeature>): String {
        val byKey = features.associateBy { it.key }
        val keywords = LinkedHashSet<String>()
        fun scan(key: String, vararg needles: String, tag: String) {
            val text = byKey[key]?.interpretation ?: ""
            if (needles.any { text.contains(it) }) keywords.add(tag)
        }
        scan("hand_shape", "务实", "实际", tag = "务实")
        scan("finger_length", "灵活", "适应", tag = "灵活")
        scan("hand_shape", "同情", "情感", tag = "情感丰富")
        scan("thumb", "领导", "坚定", tag = "有领导力")
        scan("head_line", "创意", "直觉", tag = "创意直觉")
        scan("health_line", "健康", "精力", tag = "健康意识")
        scan("fate_line", "事业", "目标", tag = "事业心强")

        if (keywords.isEmpty()) {
            keywords.add("性格平衡")
            keywords.add("适应力强")
        }

        val sb = StringBuilder("根据您的手掌特征，您是一个")
        val kw = keywords.toList()
        sb.append(if (kw.size > 2) kw.dropLast(1).joinToString("、") + "和" + kw.last() else kw.joinToString("、"))
        sb.append("的人。您的生命充满可能性，建议您结合自身优势，选择适合的发展方向。")

        when (byKey["life_line"]?.value) {
            "短" -> sb.append(" 您可能需要注意劳逸结合，保持规律作息。")
        }
        when (byKey["fate_line"]?.value) {
            "不存在" -> sb.append(" 您可能更倾向于自由职业或多元发展，不必拘泥于传统成功模式。")
        }
        when (byKey["head_line"]?.value) {
            "弯曲" -> sb.append(" 您的直觉和创意是宝贵财富，不妨在工作中多运用这些天赋。")
        }
        return sb.toString()
    }
}
