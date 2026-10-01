package com.xuanji.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.action.ActionEvidence
import com.xuanji.app.domain.action.ActionFeedbackKind
import com.xuanji.app.domain.action.ActivitySuggestion
import com.xuanji.app.domain.action.ConfidenceLevel
import com.xuanji.app.domain.action.DailyActionPlan
import com.xuanji.app.domain.action.MealSuggestion
import com.xuanji.app.domain.action.OutingSuggestion

data class DailyActionCardModel(
    val title: String,
    val summary: String,
    val score: Int,
    val confidence: String,
    val why: List<String>,
    val boundary: String,
    val alternatives: List<String> = emptyList(),
    val detailFields: List<DailyActionDetailField> = emptyList()
) {
    companion object {
        fun from(plan: DailyActionPlan): List<DailyActionCardModel> = listOf(
            DailyActionCardModel(
                "吃什么",
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
                }.orEmpty()
            ),
            DailyActionCardModel(
                "做什么",
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
                }.orEmpty()
            ),
            DailyActionCardModel(
                "去哪玩",
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
                }.orEmpty()
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

@Composable
fun DailyActionSection(
    plan: DailyActionPlan?,
    onFeedback: (category: String, candidateKey: String, kind: ActionFeedbackKind) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    if (plan == null) return
    val cards = DailyActionCardModel.from(plan)
    FortuneCard(modifier = modifier, cardId = "daily-action", title = "今日行动") {
        SectionTitle("今日行动")
        Spacer(Modifier.height(6.dp))
        Text(
            "五行 + 星座 + 今日盘面 + ${if (plan.cityKey == null) "季节" else "城市标签"} · ${plan.confidence.label()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        cards.forEachIndexed { index, card ->
            ActionCard(card, onFeedback)
            if (index != cards.lastIndex) Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(plan.disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionCard(
    card: DailyActionCardModel,
    onFeedback: (category: String, candidateKey: String, kind: ActionFeedbackKind) -> Unit
) {
    var detailOpen by rememberSaveable(card.title) { mutableStateOf(false) }
    var feedback by rememberSaveable(card.title) { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxWidth()
            .testTag("daily-action-card-${card.title}")
            .clickable(onClickLabel = "查看${card.title}详细解说") { detailOpen = true }
            .semantics { contentDescription = "${card.title}：${card.summary}，匹配分 ${card.score}，${card.confidence}" }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(card.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("${card.score} · ${card.confidence}", style = MaterialTheme.typography.labelMedium)
        }
        Text(card.summary, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "点开查看详细解说 ›",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        if (detailOpen) {
            FortuneDetailPage(
                title = card.title,
                onDismiss = { detailOpen = false }
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
                Text("匹配依据", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (card.why.isEmpty()) {
                    Text("当前结果没有单独列出的匹配依据。", style = MaterialTheme.typography.bodySmall)
                } else {
                    card.why.forEach { reason ->
                        Text("• $reason", style = MaterialTheme.typography.bodySmall)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        feedback = "已采纳"
                        onFeedback(card.title, card.summary, ActionFeedbackKind.Accepted)
                    }) { Text("已采纳") }
                    TextButton(onClick = {
                        feedback = "标记为不合适"
                        onFeedback(card.title, card.summary, ActionFeedbackKind.NotSuitable)
                    }) { Text("不合适") }
                }
                feedback?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
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
