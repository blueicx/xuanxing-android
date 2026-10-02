package com.xuanji.app.domain

import com.xuanji.app.data.model.CompositeDailyFortune

enum class MysticEvidenceStatus(val label: String) {
    Computed("计算结果"),
    UserProvided("用户主动输入"),
    LocallySaved("本机存档"),
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
        val gameIntent = analysis.intent == MysticIntent.Game
        val selectedCharacterId = context.characterId
            ?: MysticCharacterCatalog.all.firstOrNull { it.displayName == context.characterName }?.id
        val gameContext = context.gameContext?.takeIf {
            analysis.intent == MysticIntent.Game && it.characterId == selectedCharacterId
        }
        val inputs = buildList {
            context.profileKey.takeIf { it.isNotBlank() }?.let { add("档案：${fingerprint(it)}") }
            add("问题意图：${analysis.intent.value}")
            if (!gameIntent) context.dateKey.takeIf { it.isNotBlank() }?.let { add("日期：$it") }
            if (!gameIntent) add("综合分：${fortune.overallScore}")
            if (!gameIntent && context.latestTest != null) add("主动完成测验：${context.latestTest.testName}")
            if (!gameIntent) context.detailContext?.let { detail ->
                add("详情周期：${detail.period}")
                add("详情板块：${detail.title}")
                detail.dateKey.takeIf(String::isNotBlank)?.let { add("详情日期：$it") }
                detail.algorithmVersion.takeIf(String::isNotBlank)?.let { add("详情算法：$it") }
            }
            gameContext?.let { add("角色游戏存档：${it.gameTitle} · ${it.checkpoint}") }
        }
        val systems = buildList {
            if (gameIntent) {
                if (gameContext != null) add("local_game_archive")
            } else {
                add("composite_fortune")
                add(specialty.specialtyKey)
                context.detailContext?.mode?.takeIf(String::isNotBlank)?.let(::add)
            }
        }.distinct()
        val items = buildList {
            if (!gameIntent) add(MysticEvidenceItem("fortune", "今日综合盘面", MysticEvidenceStatus.Computed, "综合 ${fortune.overallScore} 分；日期 ${fortune.dateKey}"))
            if (!gameIntent) context.detailContext?.let { detail ->
                val status = when {
                    detail.sourceStatus.contains("未启用") ||
                        detail.sourceStatus.contains("缺失") ||
                        detail.sourceStatus.contains("不足") -> MysticEvidenceStatus.NotEnabled
                    detail.sourceStatus.contains("文化参考") -> MysticEvidenceStatus.CulturalReference
                    detail.sourceStatus.contains("用户") -> MysticEvidenceStatus.UserProvided
                    else -> MysticEvidenceStatus.Computed
                }
                add(MysticEvidenceItem("selected_detail", detail.title.ifBlank { "当前详情" }, status, detail.result.ifBlank { "未提供已显示结果" }))
                add(MysticEvidenceItem("selected_detail_basis", "详情依据", status, detail.evidence.ifBlank { "暂无可用依据；不补造来源" }))
                add(MysticEvidenceItem("selected_detail_source", "结果来源", status, detail.sourceStatus.ifBlank { "未提供" }))
            }
            if (!gameIntent) {
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
            }
            if (!gameIntent && context.memoryNotes.isNotEmpty()) {
                add(MysticEvidenceItem("memory", "现场手记", MysticEvidenceStatus.UserProvided, "仅引用 ${context.memoryNotes.size} 条用户主动内容"))
            }
            if (!gameIntent && analysis.intent == MysticIntent.Weather && context.weatherSummary.isNullOrBlank()) {
                add(MysticEvidenceItem("weather", "实时天气", MysticEvidenceStatus.NotEnabled, "未主动授权或未返回天气数据，不作猜测"))
            }
            if (analysis.intent == MysticIntent.Game) {
                if (gameContext == null) {
                    add(MysticEvidenceItem("game_progress", "当前角色游戏存档", MysticEvidenceStatus.NotEnabled, "没有读到本机有效存档；不补造游戏进度"))
                } else {
                    add(MysticEvidenceItem("game_progress", gameContext.gameTitle, MysticEvidenceStatus.LocallySaved, gameContext.checkpoint))
                }
            }
        }
        val missing = buildList {
            if (context.profileKey.isBlank()) add(if (gameIntent) "本机游戏存档档案" else "出生档案标识")
            if (!gameIntent && context.dateKey.isBlank()) add("日期")
            if (analysis.intent == MysticIntent.Personality && context.latestTest == null) add("主动完成的心理测验")
            if (analysis.intent == MysticIntent.Weather && context.weatherSummary.isNullOrBlank()) add("手动授权的天气数据")
            if (gameIntent && gameContext == null) add("当前角色的本地游戏存档")
        }
        return MysticEvidenceTrace(
            algorithmVersion = ALGORITHM_VERSION,
            seed = if (gameIntent) {
                seedOf(
                    context.profileKey,
                    "local_game_archive",
                    selectedCharacterId?.key.orEmpty(),
                    gameContext?.gameId.orEmpty(),
                    gameContext?.checkpoint.orEmpty()
                )
            } else {
                seedOf(context.copy(characterId = selectedCharacterId, gameContext = gameContext), analysis, fortune)
            },
            inputs = inputs,
            systems = systems,
            weights = if (gameIntent) {
                if (gameContext == null) emptyMap() else mapOf("local_game_archive" to 100)
            } else mapOf("fortune" to 70, "specialty" to 30),
            items = items,
            confidence = if (gameIntent) {
                if (gameContext == null) 0 else 100
            } else analysis.confidence.coerceIn(0, 100),
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
        seedOf(
            context.profileKey,
            context.dateKey.ifBlank { fortune.dateKey },
            fortune.overallScore,
            fortune.luckyNumber,
            context.characterId?.key.orEmpty(),
            analysis.intent.value,
            context.gameContext?.gameId.orEmpty(),
            context.gameContext?.checkpoint.orEmpty()
        )

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
