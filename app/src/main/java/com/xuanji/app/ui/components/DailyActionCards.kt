package com.xuanji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.action.ActionEvidence
import com.xuanji.app.domain.action.ActionSource
import com.xuanji.app.domain.action.ActionFeedbackKind
import com.xuanji.app.domain.action.ActionScenario
import com.xuanji.app.domain.action.ActivitySuggestion
import com.xuanji.app.domain.action.ConfidenceLevel
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.MealSuggestion
import com.xuanji.app.domain.action.OutingSuggestion
import com.xuanji.app.domain.MysticDetailContext
import com.xuanji.app.domain.MysticEvidenceBuilder

data class DailyActionCardModel(
    val title: String,
    val category: String,
    val candidateKey: String?,
    val summary: String,
    val score: Int,
    val confidence: String,
    val why: List<String>,
    val boundary: String,
    val alternatives: List<String> = emptyList(),
    val detailFields: List<DailyActionDetailField> = emptyList(),
    val evidenceDetails: List<DailyActionEvidenceDetail> = emptyList(),
    val estimatedPriceCentsPerPortion: Int? = null
) {
    companion object {
        fun from(plan: DailyActionPlan): List<DailyActionCardModel> = listOf(
            DailyActionCardModel(
                title = "吃什么",
                category = "meal",
                candidateKey = plan.meals.firstOrNull()?.candidateKey?.takeIf(String::isNotBlank),
                plan.meals.firstOrNull()?.let { meal ->
                    buildString {
                        append(meal.title)
                        if (meal.ingredients.isNotEmpty()) append(" · ${meal.ingredients.joinToString("、")}")
                        meal.estimatedPriceCents?.let { append(" · 约 ¥${it / 100.0}") }
                        meal.prepMinutes?.let { append(" · ${it}分钟准备") }
                    }
                } ?: "当前偏好没有可用目录候选",
                plan.meals.firstOrNull()?.score ?: 0,
                confidenceLabel(plan.confidence),
                plan.meals.firstOrNull()?.evidence?.map { it.label }.orEmpty(),
                "生活方式灵感，不是医疗、营养或过敏建议。",
                plan.meals.drop(1).map { it.title },
                plan.meals.firstOrNull()?.let { meal ->
                    buildList {
                        add(DailyActionDetailField("菜名", meal.title))
                        add(DailyActionDetailField("做法条件", "${meal.difficulty.label} · ${meal.requiredEquipment.joinToString("、") { it.label }.ifBlank { "无需加热设备" }} · ${if (meal.lunchboxSuitable) "便当形式；储运需自行核对" else "非便当目录"}"))
                        if (meal.steps.isNotEmpty()) add(DailyActionDetailField("简明步骤", meal.steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")))
                        meal.ingredients.takeIf { it.isNotEmpty() }?.let {
                            add(DailyActionDetailField("主要食材", it.joinToString("、")))
                        }
                        meal.substitute.takeIf { it.isNotBlank() }?.let {
                            add(DailyActionDetailField("替代食材", it))
                        }
                        add(
                            DailyActionDetailField(
                                "预算参考",
                                meal.estimatedPriceCents?.let { "约 ¥${it / 100.0}" } ?: "目录未提供价格估算"
                            )
                        )
                        add(
                            DailyActionDetailField(
                                "准备时间",
                                meal.prepMinutes?.let { "$it 分钟" } ?: "目录未提供准备时间"
                            )
                        )
                        meal.deliveryKeywords.takeIf { it.isNotEmpty() }?.let {
                            add(DailyActionDetailField("可搜索关键词", it.joinToString("、")))
                        }
                    }
                }.orEmpty(),
                plan.meals.firstOrNull()?.evidence.orEmpty().map(ActionEvidence::toDailyActionEvidenceDetail),
                estimatedPriceCentsPerPortion = plan.meals.firstOrNull()?.estimatedPriceCents
            ),
            DailyActionCardModel(
                title = "做什么",
                category = "activity",
                candidateKey = plan.activities.firstOrNull()?.candidateKey?.takeIf(String::isNotBlank),
                plan.activities.firstOrNull()?.title ?: "先做一件低压力的小事",
                plan.activities.firstOrNull()?.score ?: 0,
                confidenceLabel(plan.confidence),
                plan.activities.firstOrNull()?.evidence?.map { it.label }.orEmpty(),
                "按你的身体和时间调整，不把分数当成硬性要求。",
                plan.activities.drop(1).map { it.title },
                plan.activities.firstOrNull()?.let { activity ->
                    buildList {
                        add(DailyActionDetailField("活动建议", activity.title))
                        add(DailyActionDetailField("建议时长", "${activity.durationMinutes.first}–${activity.durationMinutes.last} 分钟"))
                        add(DailyActionDetailField("适合时段", activity.bestPeriod))
                        add(DailyActionDetailField("注意事项", activity.avoid ?: "当前规则未标注特别避开事项"))
                    }
                }.orEmpty(),
                plan.activities.firstOrNull()?.evidence.orEmpty().map(ActionEvidence::toDailyActionEvidenceDetail)
            ),
            DailyActionCardModel(
                title = "去哪玩",
                category = "outing",
                candidateKey = plan.outings.firstOrNull()?.candidateKey?.takeIf(String::isNotBlank),
                plan.outings.firstOrNull()?.cityLabel ?: "按环境类型选择附近空间",
                plan.outings.firstOrNull()?.score ?: 0,
                confidenceLabel(plan.confidence),
                plan.outings.firstOrNull()?.evidence?.map { it.label }.orEmpty(),
                "不是旅行安全、签证、收入或迁居建议。",
                plan.outings.drop(1).map { "${it.cityLabel} · ${it.placeType}" },
                plan.outings.firstOrNull()?.let { outing ->
                    buildList {
                        add(DailyActionDetailField("地区示例", outing.cityLabel))
                        add(DailyActionDetailField("场所类型", outing.placeType))
                        add(DailyActionDetailField("建议理由", outing.reason))
                        outing.indoor?.let {
                            add(DailyActionDetailField("场景", if (it) "室内" else "室外"))
                        }
                    }
                }.orEmpty(),
                plan.outings.firstOrNull()?.evidence.orEmpty().map(ActionEvidence::toDailyActionEvidenceDetail)
            )
        )

        private fun confidenceLabel(confidence: ConfidenceLevel): String = when (confidence) {
            ConfidenceLevel.High -> "输入完整度高"
            ConfidenceLevel.Medium -> "输入完整度中"
            ConfidenceLevel.Low -> "输入较少"
        }
    }
}

data class DailyActionDetailField(val label: String, val value: String)

data class DailyActionEvidenceDetail(
    val sourceLabel: String,
    val signalLabel: String,
    val contribution: Int
)

private fun ActionEvidence.toDailyActionEvidenceDetail() = DailyActionEvidenceDetail(
    sourceLabel = source.label(),
    signalLabel = label,
    contribution = contribution
)

private fun ActionSource.label(): String = when (this) {
    ActionSource.FiveElements -> "五行"
    ActionSource.Zodiac -> "星座"
    ActionSource.DailyFortune -> "今日运势"
    ActionSource.Season -> "季节"
    ActionSource.CityProfile -> "城市标签"
    ActionSource.Preference -> "饮食偏好过滤"
}

@Composable
fun DailyActionSection(
    plan: DailyActionPlan?,
    modifier: Modifier = Modifier,
    onFeedback: (category: String, candidateKey: String, kind: ActionFeedbackKind) -> Unit = { _, _, _ -> },
    onUndoReplacement: (category: String, candidateKey: String) -> Unit = { _, _ -> },
    onScenarioChange: ((ActionScenario) -> Unit)? = null
) {
    if (plan == null) return
    val cards = DailyActionCardModel.from(plan)
    var selectedDetailTitle by rememberSaveable(plan.profileKey, plan.dateKey) { mutableStateOf<String?>(null) }
    var feedbackStatus by rememberSaveable(plan.profileKey, plan.dateKey) { mutableStateOf<String?>(null) }
    var scenarioOpen by rememberSaveable(plan.profileKey, plan.dateKey) { mutableStateOf(false) }
    FortuneCard(modifier = modifier, cardId = "daily-action", title = "今日行动") {
        Text(
            "五行 + 星座 + 今日盘面 + ${if (plan.cityKey == null) "季节" else "城市标签"} · ${plan.confidence.label()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        if (onScenarioChange != null) {
            TextButton(onClick = { scenarioOpen = true }, modifier = Modifier.testTag("daily-action-scenario")) {
                Text("今日情境：${scenarioLabel(plan.constraints.scenario)}")
            }
        }
        cards.forEachIndexed { index, card ->
            ActionCard(
                card = card,
                onOpenDetail = {
                    selectedDetailTitle = card.title
                    feedbackStatus = null
                }
            )
            if (index != cards.lastIndex) Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(plan.disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (scenarioOpen && onScenarioChange != null) {
        AlertDialog(
            onDismissRequest = { scenarioOpen = false },
            title = { Text("选择今日情境") },
            text = {
                Column {
                    Text("仅用于本次行动筛选，不自动写入长期偏好。")
                    ActionScenario.entries.forEach { scenario ->
                        TextButton(onClick = { scenarioOpen = false; onScenarioChange(scenario) }) {
                            Text(scenarioLabel(scenario))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { scenarioOpen = false }) { Text("取消") } }
        )
    }

    selectedDetailTitle?.let { title ->
        cards.firstOrNull { it.title == title }?.let { card ->
            val replacementToUndo = plan.appliedFeedback.firstOrNull {
                it.dateKey == plan.dateKey &&
                    (it.category == card.category || it.category == card.title) &&
                    it.kind == ActionFeedbackKind.Replaced
            }
            DailyActionDetailPage(
                card = card,
                dateKey = plan.dateKey,
                relatedCards = cards.filterNot { it.title == card.title },
                feedbackStatus = feedbackStatus,
                undoCandidateKey = replacementToUndo?.candidateKey,
                onDismiss = { selectedDetailTitle = null },
                onRestore = { selectedDetailTitle = card.title },
                onOpenRelated = { related ->
                    selectedDetailTitle = related.title
                    feedbackStatus = null
                },
                onFeedback = { kind ->
                    feedbackStatus = when (kind) {
                        ActionFeedbackKind.Accepted -> "已记录：后续同类候选会提高排序，最多 +5 分。"
                        ActionFeedbackKind.NotSuitable -> "已记录：后续同类候选会降低排序，最多 −8 分。"
                        ActionFeedbackKind.Replaced -> "已换一个候选；这次排除可撤销。"
                    }
                    card.candidateKey?.let { onFeedback(card.category, it, kind) }
                },
                onUndoReplacement = {
                    replacementToUndo?.let { previous ->
                        feedbackStatus = "已撤销上次换选，原候选重新参与排序。"
                        onUndoReplacement(card.category, previous.candidateKey)
                    }
                }
            )
        }
    }
}

private fun scenarioLabel(scenario: ActionScenario): String = when (scenario) {
    ActionScenario.Any -> "不限"
    ActionScenario.Home -> "在家"
    ActionScenario.Work -> "上班"
    ActionScenario.Weekend -> "周末"
    ActionScenario.ShortOnTime -> "时间紧"
}

@Composable
private fun ActionCard(card: DailyActionCardModel, onOpenDetail: () -> Unit) {
    val accent = when (card.title) {
        "吃什么" -> ActionAccent(
            icon = "🍲",
            background = FortuneSurfaceTokens.MEAL_GRADIENT,
            stroke = FortuneSurfaceTokens.MEAL_STROKE,
            content = Color(0xFFE7C58A),
            shape = FortuneSurfaceTokens.MEAL_SHAPE
        )
        "做什么" -> ActionAccent(
            icon = "✦",
            background = FortuneSurfaceTokens.ACTION_GRADIENT,
            stroke = FortuneSurfaceTokens.ACTIVITY_STROKE,
            content = Color(0xFF8FD2C5),
            shape = FortuneSurfaceTokens.ACTION_SHAPE
        )
        else -> ActionAccent(
            icon = "⌖",
            background = FortuneSurfaceTokens.ACTION_GRADIENT,
            stroke = FortuneSurfaceTokens.OUTING_STROKE,
            content = Color(0xFFE8BB70),
            shape = FortuneSurfaceTokens.ACTION_SHAPE
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(accent.shape)
            .background(accent.background)
            .border(FortuneSurfaceTokens.CARD_STROKE_WIDTH, accent.stroke, accent.shape)
            .testTag("daily-action-card-${card.title}")
            .clickable(onClickLabel = "查看${card.title}详细解说", onClick = onOpenDetail)
            .semantics { contentDescription = "${card.title}：${card.summary}，匹配分 ${card.score}，${card.confidence}" }
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(accent.icon, style = MaterialTheme.typography.titleSmall)
                    Text(card.title, fontWeight = FontWeight.Bold, color = accent.content)
                }
                Text("${card.score} · ${card.confidence}", style = MaterialTheme.typography.labelMedium, color = accent.content.copy(alpha = 0.84f))
            }
            Text(card.summary, style = MaterialTheme.typography.bodyMedium)
            Text("点开查看详细解说 ›", style = MaterialTheme.typography.labelSmall, color = accent.content)
        }
    }
}

private data class ActionAccent(
    val icon: String,
    val background: Brush,
    val stroke: Color,
    val content: Color,
    val shape: RoundedCornerShape
)

@Composable
private fun DailyActionDetailPage(
    card: DailyActionCardModel,
    dateKey: String,
    relatedCards: List<DailyActionCardModel>,
    feedbackStatus: String?,
    undoCandidateKey: String?,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onOpenRelated: (DailyActionCardModel) -> Unit,
    onFeedback: (ActionFeedbackKind) -> Unit,
    onUndoReplacement: () -> Unit
) {
    val askMystic = LocalMysticDetailQuestion.current
    val detailContext = MysticDetailContext(
        mode = "综合 · 五行 + 星座",
        period = "今日",
        dateKey = dateKey,
        title = card.title,
        result = buildString {
            append(card.summary)
            card.detailFields.forEach { append("\n${it.label}：${it.value}") }
            append("\n匹配评分：${card.score} · ${card.confidence}")
        },
        evidence = (card.evidenceDetails.map { "${it.sourceLabel}：${it.signalLabel} · ${it.contributionText()}" }
            .ifEmpty { card.why }).joinToString("；"),
        sourceStatus = "本地今日行动候选计算 · ${card.confidence}",
        algorithmVersion = MysticEvidenceBuilder.ALGORITHM_VERSION
    )
    FortuneDetailPage(
        title = card.title,
        onDismiss = onDismiss,
        detailContext = detailContext,
        favoriteType = "action",
        contentIncludesEvidence = true,
        onAskMystic = askMystic?.let { submitDetail ->
            { selectedDetail ->
                onDismiss()
                submitDetail(selectedDetail, onRestore)
            }
        }
    ) {
        Text(
            text = card.summary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "匹配分 ${card.score} · ${card.confidence}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        card.detailFields.forEach { field ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    field.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(field.value, style = MaterialTheme.typography.bodyLarge)
            }
        }
        AddToAgendaAction(
            title = card.detailFields.firstOrNull { it.label in setOf("菜名", "活动建议", "场所类型") }?.value ?: card.summary,
            category = card.category,
            dateKey = dateKey,
            ingredients = card.detailFields.firstOrNull { it.label == "主要食材" }?.value?.split("、").orEmpty(),
            candidateKey = card.candidateKey.orEmpty(),
            estimatedPriceCentsPerPortion = card.estimatedPriceCentsPerPortion
        )
        Text("匹配依据", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (card.why.isEmpty()) {
            Text("当前结果没有单独列出的匹配依据。", style = MaterialTheme.typography.bodySmall)
        } else if (card.evidenceDetails.isEmpty()) {
            card.why.forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            card.evidenceDetails.forEach { evidence ->
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "${evidence.sourceLabel} · ${evidence.signalLabel}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        evidence.contributionText(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (card.alternatives.isNotEmpty()) {
            Text("其他候选", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            card.alternatives.forEach { alternative ->
                Text("• $alternative", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("使用边界", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(card.boundary, style = MaterialTheme.typography.bodySmall)
        if (relatedCards.isNotEmpty()) {
            Text("相关行动", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            relatedCards.forEach { related ->
                TextButton(onClick = { onOpenRelated(related) }) {
                    Text("查看${related.title}详情 ›")
                }
            }
        }
        if (card.candidateKey != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onFeedback(ActionFeedbackKind.Accepted) }) { Text("采纳") }
                TextButton(onClick = { onFeedback(ActionFeedbackKind.NotSuitable) }) { Text("不合适") }
                TextButton(onClick = { onFeedback(ActionFeedbackKind.Replaced) }) { Text("换一个") }
            }
        }
        if (undoCandidateKey != null) {
            TextButton(onClick = onUndoReplacement) { Text("撤销上次换选") }
        }
        feedbackStatus?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

private fun DailyActionEvidenceDetail.contributionText(): String = if (contribution == 0) {
    "用于偏好过滤，不计入评分"
} else {
    "加权参考 ${if (contribution > 0) "+" else ""}$contribution 分"
}

private fun ConfidenceLevel.label(): String = when (this) {
    ConfidenceLevel.High -> "高置信度"
    ConfidenceLevel.Medium -> "中置信度"
    ConfidenceLevel.Low -> "低置信度"
}

@Suppress("UNUSED_PARAMETER")
private fun unusedSuggestionTypes(meal: MealSuggestion, activity: ActivitySuggestion, outing: OutingSuggestion, evidence: ActionEvidence) {
    // 类型在模型层保持显式，UI 仅消费 DailyActionCardModel。
}
