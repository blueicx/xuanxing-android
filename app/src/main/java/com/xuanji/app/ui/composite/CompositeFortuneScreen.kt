package com.xuanji.app.ui.composite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.di.AppModule
import com.xuanji.app.ui.components.CardLayoutController
import com.xuanji.app.ui.components.CardLayouts
import com.xuanji.app.ui.components.CardControls
import com.xuanji.app.ui.components.CardMeta
import com.xuanji.app.ui.components.LocalCardLayout
import com.xuanji.app.ui.components.MysticFloatingGuide
import com.xuanji.app.ui.components.PeriodToggleRow
import com.xuanji.app.ui.components.ResultShareCards
import com.xuanji.app.ui.components.RestoreCardsBar
import com.xuanji.app.ui.components.ShareCard
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.cardDragReorder
import com.xuanji.app.ui.components.rememberCardLayoutController
import com.xuanji.app.ui.viewmodel.CompositeFortuneViewModel
import com.xuanji.app.ui.viewmodel.CompositeUiState
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun CompositeFortuneScreen(
    viewModel: CompositeFortuneViewModel = xuanjiViewModel { CompositeFortuneViewModel(AppModule.repository) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val s = state) {
        is CompositeUiState.Loading -> CenterMessage("正在综合推算…")
        is CompositeUiState.Empty -> CenterMessage("尚未设置出生信息，请先在「我的」中填写生日。")
        is CompositeUiState.Ready -> CompositeContent(
            bazi = s.bazi,
            fortune = s.fortune,
            period = s.period,
            onPeriodChange = viewModel::setPeriod
        )
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CompositeContent(
    bazi: com.xuanji.app.data.model.BaziFull,
    fortune: CompositeDailyFortune,
    period: String,
    onPeriodChange: (String) -> Unit
) {
    val controller = rememberCardLayoutController("composite", bazi.chart.display)
    val cards = fortuneCards(fortune, period)

    CompositionLocalProvider(LocalCardLayout provides controller) {
        MysticFloatingGuide(bazi, fortune) { scrollState ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var periodInserted = false
                CardLayouts.ordered(cards, controller.state).forEach { card ->
                    if (!periodInserted && (card.id.startsWith("dim-") || card.id == "caution")) {
                        PeriodToggleRow(period, onPeriodChange)
                        Text("${periodLabel(period)}维度", style = MaterialTheme.typography.titleSmall)
                        periodInserted = true
                    }
                    when (card.id) {
                        "overall" -> OverallCard(controller, card.shareCard, fortune, period)
                        "luck" -> LuckCard(card.shareCard, fortune)
                        "caution" -> CautionCard(card.shareCard, fortune.cautions)
                        else -> card.content()
                    }
                }

                if (controller.state.hiddenCount > 0) {
                    RestoreCardsBar(controller)
                }

                Text(
                    "本页运势由出生信息按日期本地确定性推算（离线可用），融合东方八字与西方星座，仅供娱乐参考。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun fortuneCards(fortune: CompositeDailyFortune, period: String): List<CardMeta> = listOf(
    CardMeta("overall", "综合总分", ResultShareCards.composite("overall", period, fortune)) {},
    CardMeta("luck", "幸运信息", ResultShareCards.composite("luck", period, fortune)) {},
    *fortune.dimensions.map { dim ->
        val dimensionShare = ResultShareCards.composite("dim-${dim.key}", period, fortune)
        CardMeta(
            id = "dim-${dim.key}",
            title = dim.label,
            shareCard = dimensionShare,
            content = { DimensionCard(dim, dimensionShare) }
        )
    }.toTypedArray(),
    CardMeta("caution", "注意事项", ResultShareCards.composite("caution", period, fortune)) {}
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OverallCard(
    controller: CardLayoutController,
    shareCard: ShareCard?,
    fortune: CompositeDailyFortune,
    period: String
) {
    Card(
        Modifier
            .fillMaxWidth()
            .cardDragReorder(
                enabled = controller.editingCardId == "overall",
                cardId = "overall",
                controller = controller
            )
            .combinedClickable(
                onClickLabel = "查看综合运势",
                onLongClickLabel = "编辑综合卡片",
                onClick = {},
                onLongClick = { controller.startEdit("overall") }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (controller.editingCardId == "overall") {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shareCard != null) {
                if (controller.editingCardId == "overall") {
                    CardControls(
                        title = "综合总分",
                        cardId = "overall",
                        controller = controller,
                        shareCard = shareCard
                    )
                } else {
                    Text(
                        "综合总分",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${periodLabel(period)}综合运势",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${fortune.overallScore}",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = scoreColor(fortune.overallScore)
                )
                Text("分", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(scoreEmoji(fortune.overallScore), style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                "融合东方八字与西方星座",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun LuckCard(shareCard: ShareCard?, fortune: CompositeDailyFortune) {
    FortuneCard(
        cardId = "luck",
        title = "幸运信息",
        shareCard = shareCard
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LuckChip("幸运数字", fortune.luckyNumber.toString(), Modifier.weight(1f))
            LuckChip("幸运色", fortune.luckyColor, Modifier.weight(1f))
            LuckChip("吉利方位", fortune.luckyDirection, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CautionCard(shareCard: ShareCard?, cautions: String) {
    FortuneCard(
        cardId = "caution",
        title = "注意事项",
        shareCard = shareCard
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("注意事项", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(cautions, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LuckChip(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun periodLabel(period: String): String = when (period) {
    "week" -> "本周"
    "month" -> "本月"
    else -> "今日"
}

@Composable
private fun DimensionCard(
    dim: FortuneDimension,
    shareCard: ShareCard?
) {
    FortuneCard(
        cardId = "dim-${dim.key}",
        title = dim.label,
        shareCard = shareCard
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(dim.label, style = MaterialTheme.typography.titleSmall)
                Text("${dim.score}分", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = scoreColor(dim.score))
            }
            LinearProgressIndicator(
                progress = { dim.score / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = scoreColor(dim.score),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(dim.interpretation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF1B5E20)
    score >= 65 -> Color(0xFF2E7D32)
    score >= 50 -> Color(0xFF1565C0)
    score >= 35 -> Color(0xFFEF6C00)
    else -> Color(0xFFC62828)
}

private fun scoreEmoji(score: Int): String = when {
    score >= 80 -> "🌟"
    score >= 65 -> "😊"
    score >= 50 -> "🙂"
    score >= 35 -> "💪"
    else -> "🍀"
}
