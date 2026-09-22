package com.xuanji.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.MysticCompanionAction

@Composable
fun MysticCompanionActionCard(
    action: MysticCompanionAction,
    evidenceLines: List<String> = emptyList(),
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (action) {
        is MysticCompanionAction.ShowTodayMeal -> "今天吃什么"
        is MysticCompanionAction.ShowTodayActivity -> "今天做什么"
        is MysticCompanionAction.ShowTodayOuting -> "今天去哪玩"
        is MysticCompanionAction.ShowLifeProfile -> "人生画像匹配"
        MysticCompanionAction.ShowEvidence -> "这条回答的依据"
        else -> return
    }
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            when (action) {
                is MysticCompanionAction.ShowTodayMeal -> {
                    val meal = action.plan.meals.firstOrNull()
                    if (meal != null) {
                        Text(meal.title, style = MaterialTheme.typography.titleMedium)
                        Text("食材：${meal.ingredients.joinToString("、")}")
                        Text("替代：${meal.substitute} · 约 ${meal.prepMinutes ?: 0} 分钟 · ${meal.estimatedPriceCents?.let { "约 ¥${it / 100.0}" } ?: "预算未设"}")
                        Text("外卖搜索：${meal.deliveryKeywords.joinToString("、")}")
                        Text("依据：${meal.evidence.take(3).joinToString("；") { it.label }}", style = MaterialTheme.typography.labelSmall)
                    } else Text("当前偏好过滤掉了全部候选，请调整饮食偏好。")
                }
                is MysticCompanionAction.ShowTodayActivity -> action.plan.activities.firstOrNull()?.let { item ->
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text("建议：${item.bestPeriod} · ${item.durationMinutes.first}-${item.durationMinutes.last} 分钟")
                    Text("依据：${item.evidence.take(3).joinToString("；") { it.label }}", style = MaterialTheme.typography.labelSmall)
                }
                is MysticCompanionAction.ShowTodayOuting -> action.plan.outings.firstOrNull()?.let { item ->
                    Text("${item.cityLabel} · ${item.placeType}", style = MaterialTheme.typography.titleMedium)
                    Text(item.reason)
                    Text("依据：${item.evidence.take(3).joinToString("；") { it.label }}", style = MaterialTheme.typography.labelSmall)
                }
                is MysticCompanionAction.ShowLifeProfile -> {
                    action.profile.careerClusters.firstOrNull()?.let { Text("职业方向：${it.label} · ${it.score} 分") }
                    action.profile.colorPalette.firstOrNull()?.let { Text("色彩灵感：${it.label} · ${it.hex}") }
                    action.profile.regionCandidates.firstOrNull()?.let { Text("地区示例：${it.country} · ${it.city}") }
                    Text("仅作匹配示例，不替代职业、移民或旅行决策。", style = MaterialTheme.typography.labelSmall)
                }
                MysticCompanionAction.ShowEvidence -> {
                    evidenceLines.take(12).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (evidenceLines.isEmpty()) Text("当前没有可展开的计算依据。")
                }
                else -> Unit
            }
            OutlinedButton(onClick = onDismiss) { Text("收起这张卡") }
        }
    }
}
