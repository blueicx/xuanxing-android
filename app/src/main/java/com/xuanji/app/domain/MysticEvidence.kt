package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune

enum class MysticEvidenceStatus(val label: String) {
    Computed("计算结果"),
    UserProvided("用户主动输入"),
    CulturalReference("文化参考"),
    NotEnabled("未启用能力")
}

data class MysticEvidenceItem(
    val key: String,
    val label: String,
    val status: MysticEvidenceStatus,
    val detail: String
)

data class MysticEvidenceTrace(
    val algorithmVersion: String,
    val seed: String,
    val inputs: List<String>,
    val systems: List<String>,
    val weights: Map<String, Int>,
    val items: List<MysticEvidenceItem>,
    val confidence: Int,
    val missingInputs: List<String>
)

/** Builds a user-readable audit trail without inventing unavailable inputs. */
object MysticEvidenceBuilder {
    const val ALGORITHM_VERSION = "x3-offline-2026.09"

    fun forDialogue(
        context: DialogueContext,
        analysis: DialogueAnalysis,
        specialty: MysticSpecialtySelection
    ): MysticEvidenceTrace {
        val fortune = context.fortune
        val inputs = buildList {
            context.profileKey.takeIf { it.isNotBlank() }?.let { add("档案：${fingerprint(it)}") }
            context.dateKey.takeIf { it.isNotBlank() }?.let { add("日期：$it") }
            add("问题意图：${analysis.intent.value}")
            add("综合分：${fortune.overallScore}")
            if (context.latestTest != null) add("主动完成测验：${context.latestTest.testName}")
        }
        val systems = listOf("composite_fortune", specialty.specialtyKey).distinct()
        val items = buildList {
            add(MysticEvidenceItem("fortune", "今日综合盘面", MysticEvidenceStatus.Computed, "综合 ${fortune.overallScore} 分；日期 ${fortune.dateKey}"))
            add(
                MysticEvidenceItem(
                    "specialty",
                    specialty.specialtyLabel,
                    when (specialty.availability) {
                        MysticCharacterAvailability.Available -> MysticEvidenceStatus.Computed
                        MysticCharacterAvailability.CulturalReference -> MysticEvidenceStatus.CulturalReference
                        MysticCharacterAvailability.NotEnabled -> MysticEvidenceStatus.NotEnabled
                    },
                    specialty.sourceLabel
                )
            )
            if (context.memoryNotes.isNotEmpty()) {
                add(MysticEvidenceItem("memory", "现场手记", MysticEvidenceStatus.UserProvided, "仅引用 ${context.memoryNotes.size} 条用户主动内容"))
            }
            if (analysis.intent == MysticIntent.Weather && context.weatherSummary.isNullOrBlank()) {
                add(MysticEvidenceItem("weather", "实时天气", MysticEvidenceStatus.NotEnabled, "未主动授权或未返回天气数据，不作猜测"))
            }
        }
        val missing = buildList {
            if (context.profileKey.isBlank()) add("出生档案标识")
            if (context.dateKey.isBlank()) add("日期")
            if (analysis.intent == MysticIntent.Personality && context.latestTest == null) add("主动完成的心理测验")
            if (analysis.intent == MysticIntent.Weather && context.weatherSummary.isNullOrBlank()) add("手动授权的天气数据")
        }
        return MysticEvidenceTrace(
            algorithmVersion = ALGORITHM_VERSION,
            seed = seedOf(context, analysis, fortune),
            inputs = inputs,
            systems = systems,
            weights = mapOf("fortune" to 70, "specialty" to 30),
            items = items,
            confidence = analysis.confidence.coerceIn(0, 100),
            missingInputs = missing
        )
    }

    fun forFortune(fortune: CompositeDailyFortune, profileKey: String = ""): MysticEvidenceTrace {
        val systems = listOf("eastern", "western", "composite")
        return MysticEvidenceTrace(
            algorithmVersion = ALGORITHM_VERSION,
            seed = seedOf(profileKey, fortune.dateKey, fortune.overallScore, fortune.luckyNumber),
            inputs = listOfNotNull(
                profileKey.takeIf { it.isNotBlank() }?.let { "档案：${fingerprint(it)}" },
                "日期：${fortune.dateKey}",
                "东方分：${fortune.eastern.overallScore}",
                "西方分：${fortune.western.overallScore}"
            ),
            systems = systems,
            weights = mapOf("eastern" to 50, "western" to 50),
            items = listOf(
                MysticEvidenceItem("eastern", "东方盘", MysticEvidenceStatus.Computed, "${fortune.eastern.overallScore} 分"),
                MysticEvidenceItem("western", "西方盘", MysticEvidenceStatus.Computed, "${fortune.western.overallScore} 分"),
                MysticEvidenceItem("composite", "综合结论", MysticEvidenceStatus.Computed, "${fortune.overallScore} 分")
            ),
            confidence = 100,
            missingInputs = emptyList()
        )
    }

    private fun seedOf(context: DialogueContext, analysis: DialogueAnalysis, fortune: CompositeDailyFortune): String =
        seedOf(context.profileKey, context.dateKey.ifBlank { fortune.dateKey }, fortune.overallScore, fortune.luckyNumber, context.characterId?.key.orEmpty(), analysis.intent.value)

    private fun seedOf(vararg parts: Any?): String {
        var hash = 5381L
        parts.joinToString("|").forEach { hash = (hash * 33L + it.code) % 2_147_483_647L }
        return hash.toString()
    }

    private fun fingerprint(value: String): String {
        var hash = 5381L
        value.forEach { hash = (hash * 33L + it.code) % 2_147_483_647L }
        return hash.toString(16)
    }
}
