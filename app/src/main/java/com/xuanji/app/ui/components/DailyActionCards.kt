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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.action.ActionEvidence
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
    val boundary: String
) {
    companion object {
        fun from(plan: DailyActionPlan): List<DailyActionCardModel> = listOf(
            DailyActionCardModel(
                "吃什么",
                plan.meals.firstOrNull()?.title ?: "当前偏好没有可用目录候选",
                plan.meals.firstOrNull()?.score ?: 0,
                confidenceLabel(plan.confidence),
                plan.meals.firstOrNull()?.evidence?.map { it.label }.orEmpty(),
                "生活方式灵感，不是医疗、营养或过敏建议。"
            ),
            DailyActionCardModel(
                "做什么",
                plan.activities.firstOrNull()?.title ?: "先做一件低压力的小事",
                plan.activities.firstOrNull()?.score ?: 0,
                confidenceLabel(plan.confidence),
                plan.activities.firstOrNull()?.evidence?.map { it.label }.orEmpty(),
                "按你的身体和时间调整，不把分数当成硬性要求。"
            ),
            DailyActionCardModel(
                "去哪玩",
                plan.outings.firstOrNull()?.cityLabel ?: "按环境类型选择附近空间",
                plan.outings.firstOrNull()?.score ?: 0,
                confidenceLabel(plan.confidence),
                plan.outings.firstOrNull()?.evidence?.map { it.label }.orEmpty(),
                "不是旅行安全、签证、收入或迁居建议。"
            )
        )

        private fun confidenceLabel(confidence: ConfidenceLevel): String = when (confidence) {
            ConfidenceLevel.High -> "输入完整度高"
            ConfidenceLevel.Medium -> "输入完整度中"
            ConfidenceLevel.Low -> "输入较少"
        }
    }
}

@Composable
fun DailyActionSection(
    plan: DailyActionPlan?,
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
            ActionCard(card)
            if (index != cards.lastIndex) Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(plan.disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionCard(card: DailyActionCardModel) {
    var expanded by rememberSaveable(card.title) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "展开${card.title}依据") { expanded = !expanded }
            .semantics { contentDescription = "${card.title}：${card.summary}，匹配分 ${card.score}，${card.confidence}" }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(card.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("${card.score} · ${card.confidence}", style = MaterialTheme.typography.labelMedium)
        }
        Text(card.summary, style = MaterialTheme.typography.bodyLarge)
        Text(if (expanded) "收起依据" else "为什么？", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        if (expanded) {
            card.why.take(4).forEach { reason ->
                Text("• $reason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("边界：${card.boundary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
