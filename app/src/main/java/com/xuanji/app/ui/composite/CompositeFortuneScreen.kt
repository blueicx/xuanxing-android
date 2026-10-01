package com.xuanji.app.ui.composite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.local.actionFeedbackStore
import com.xuanji.app.di.AppModule
import com.xuanji.app.ui.components.CardLayouts
import com.xuanji.app.ui.components.CardMeta
import com.xuanji.app.ui.components.DailyActionSection
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.FortuneDetailPage
import com.xuanji.app.ui.components.FortunePageIntro
import com.xuanji.app.ui.components.FortuneInsightList
import com.xuanji.app.ui.components.FortunePageWidth
import com.xuanji.app.ui.components.FortuneProse
import com.xuanji.app.ui.components.FortuneSurfaceTokens
import com.xuanji.app.ui.components.FortuneStickyHeader
import com.xuanji.app.ui.components.TodayFortuneMode
import com.xuanji.app.ui.components.LocalCardLayout
import com.xuanji.app.ui.components.MysticFloatingGuide
import com.xuanji.app.ui.components.ResultShareCards
import com.xuanji.app.ui.components.RestoreCardsBar
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.ScoreRing
import com.xuanji.app.ui.components.ShareCard
import com.xuanji.app.ui.components.rememberCardLayoutController
import com.xuanji.app.ui.viewmodel.CompositeFortuneViewModel
import com.xuanji.app.ui.viewmodel.CompositeUiState
import com.xuanji.app.ui.viewmodel.ActionViewModel
import com.xuanji.app.ui.xuanjiViewModel
import com.xuanji.app.domain.action.ActionFeedback
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

/**
 * 综合运势页：把八字与星盘合参为一份可执行的结论。
 *
 * 版式约定（三个运势页共用）：
 *  - 顶部一条「置顶栏」固定不动，写清楚当前看的是哪一段周期和合参结论，
 *    周期切换器放在这里，滚动时不会消失；
 *  - 下面的正文用一条 ScrollState 贯穿滚动，不再出现「卡片里套滚动」的双滚动条；
 *  - 正文限宽居中，大屏不会把行拉得太长；底部留出自适应的收尾间距。
 */
@Composable
fun CompositeFortuneScreen(
    viewModel: CompositeFortuneViewModel = xuanjiViewModel {
        CompositeFortuneViewModel(AppModule.repository)
    },
    onTodayModeChange: ((TodayFortuneMode) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionViewModel = xuanjiViewModel { ActionViewModel(AppModule.actionRepository) }
    val actionState by actionViewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is CompositeUiState.Loading -> CenterMessage("正在综合推算…")
        is CompositeUiState.Empty -> CenterMessage("尚未设置出生信息，请先在「我的」中填写生日。")
        is CompositeUiState.Ready -> CompositeContent(
            bazi = s.bazi,
            fortune = s.fortune,
            period = s.period,
            onPeriodChange = viewModel::setPeriod,
            dailyAction = actionState.todayPlan,
            onTodayModeChange = onTodayModeChange
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
    bazi: BaziFull,
    fortune: CompositeDailyFortune,
    period: String,
    onPeriodChange: (String) -> Unit,
    dailyAction: com.xuanji.app.domain.action.DailyActionPlan?,
    onTodayModeChange: ((TodayFortuneMode) -> Unit)?
) {
    val controller = rememberCardLayoutController("composite", bazi.chart.display)
    val cards = fortuneCards(fortune, period)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val feedbackStore = remember(context) { context.actionFeedbackStore() }

    CompositionLocalProvider(LocalCardLayout provides controller) {
        MysticFloatingGuide(bazi, fortune) { scrollState ->
            Column(Modifier.fillMaxSize()) {
                FortuneStickyHeader(
                    period = period,
                    onPeriodChange = onPeriodChange,
                    dateLabel = "公历 · ${fortune.dateKey}",
                    todayMode = onTodayModeChange?.let { TodayFortuneMode.Composite },
                    onTodayModeChange = onTodayModeChange
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FortunePageWidth {
                        Column(
                            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FortunePageIntro(
                                title = if (period == "day") "今天的总趋势" else "${periodLabel(period)}的总趋势",
                                subtitle = "${fortune.dateKey} · 八字与星盘合参",
                                modeLabel = "综合"
                            )
                            DailyOutlookHero(fortune, period)
                            SummaryBlock(fortune)
                            CompositeDimensionGrid(fortune.dimensions)
                            if (shouldShowDailyAction(period)) {
                                DailyActionSection(
                                    plan = dailyAction,
                                    onFeedback = { category, candidateKey, kind ->
                                        dailyAction?.let { plan ->
                                            scope.launch {
                                                feedbackStore.append(
                                                    plan.profileKey,
                                                    ActionFeedback(
                                                        id = "${plan.dateKey}|$category|$candidateKey|${kind.name}",
                                                        dateKey = plan.dateKey,
                                                        category = category,
                                                        candidateKey = candidateKey,
                                                        kind = kind
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            CardLayouts.ordered(cards, controller.state).forEach { card ->
                                when (card.id) {
                                    "luck" -> LuckCard(card.shareCard, fortune)
                                    "caution" -> CautionCard(card.shareCard, fortune.cautions)
                                    "evidence" -> EvidenceCard(card.shareCard, fortune)
                                    "dimensions" -> Unit
                                    else -> card.content()
                                }
                            }
                            if (controller.state.hiddenCount > 0) {
                                RestoreCardsBar(controller)
                            }
                            FooterNote()
                        }
                    }
                }
            }
        }
    }
}

/** 置顶栏副标题：只强调合参结果，不重复东方/西方分栏。 */
private fun CompositeDailyFortune.headlineLine(): String = compositeHeadlineLine(dateKey)

internal fun compositeHeadlineLine(dateKey: String): String = "$dateKey · 八字与星盘合参"

/** 今日行动是日程建议，只在今日盘面展示，不延伸到周/月/年周期。 */
internal fun shouldShowDailyAction(period: String): Boolean = period == "day"

/** 趋势主视觉：只突出综合分、短结论和少量维度，不承载长篇解说。 */
@Composable
private fun DailyOutlookHero(fortune: CompositeDailyFortune, period: String) {
    val summary = fortune.periodSummary.trim()
    val sentenceEnd = summary.indexOfFirst { it in "。！？" }
    val shortConclusion = when {
        summary.isBlank() -> "综合趋势已结合八字与星盘计算。"
        sentenceEnd in 1..54 -> summary.substring(0, sentenceEnd + 1)
        summary.length > 54 -> summary.take(54).trimEnd() + "…"
        else -> summary
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(FortuneSurfaceTokens.HERO_SHAPE)
            .background(FortuneSurfaceTokens.HERO_GRADIENT)
            .padding(15.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(fortune.overallScore, diameter = 68.dp, caption = "综合总分")
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${periodLabel(period)}综合指数", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE5D6F4))
                    Text("沿用旧版综合结论", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(shortConclusion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f), maxLines = 2)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                fortune.dimensions.take(3).forEach { dimension ->
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF72598C).copy(alpha = 0.48f)) {
                        Text(
                            "${dimension.label} · ${dimension.score}",
                            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE8D8FF)
                        )
                    }
                }
            }
        }
    }
}

/** 本周期总评：原始完整结论进入详情；首屏只呈现短预览与幸运信息。 */
@Composable
private fun SummaryBlock(fortune: CompositeDailyFortune) {
    FortuneCard(
        cardId = "summary",
        title = "本周期总评",
        previewContent = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("综合结论", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD9C27E), fontWeight = FontWeight.Bold)
                Text(fortune.periodSummary, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SummaryMetaChip("幸运数字", "${fortune.luckyNumber}")
                    SummaryMetaChip("幸运色", fortune.luckyColor)
                    SummaryMetaChip("吉利方位", fortune.luckyDirection)
                }
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(fortune.overallScore)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoLine("幸运数字", "${fortune.luckyNumber}")
                InfoLine("幸运色", fortune.luckyColor)
                InfoLine("吉利方位", fortune.luckyDirection)
            }
        }
        Spacer(Modifier.height(12.dp))
        FortuneProse(fortune.periodSummary)
    }
}

@Composable
private fun SummaryMetaChip(label: String, value: String) {
    Surface(
        shape = FortuneSurfaceTokens.COMPACT_SHAPE,
        color = FortuneSurfaceTokens.COMPACT_SURFACE,
        border = BorderStroke(FortuneSurfaceTokens.CARD_STROKE_WIDTH, FortuneSurfaceTokens.COMPACT_STROKE)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CompositeDimensionGrid(dimensions: List<FortuneDimension>) {
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("六维运势") {
            Text("点击查看解说", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        dimensions.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { dimension ->
                    Surface(
                        modifier = Modifier.weight(1f).clickable { selectedKey = dimension.key },
                        shape = FortuneSurfaceTokens.COMPACT_SHAPE,
                        color = FortuneSurfaceTokens.COMPACT_SURFACE,
                        border = BorderStroke(FortuneSurfaceTokens.CARD_STROKE_WIDTH, FortuneSurfaceTokens.COMPACT_STROKE),
                        tonalElevation = 0.dp
                    ) {
                        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(dimension.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("${dimension.score}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { dimension.score / 100f },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFD8BF7D),
                                trackColor = Color(0xFF4C3B60)
                            )
                            Text(dimension.interpretation, style = MaterialTheme.typography.labelSmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("查看详细解说 ›", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    selectedKey?.let { key ->
        dimensions.firstOrNull { it.key == key }?.let { dimension ->
            FortuneDetailPage(title = dimension.label, onDismiss = { selectedKey = null }) {
                Text("当前周期评分 · ${dimension.score} 分", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                FortuneProse(dimension.interpretation)
                Text("本项属于综合运势的趋势参考，不替代现实中的专业判断。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

private fun fortuneCards(fortune: CompositeDailyFortune, period: String): List<CardMeta> = listOf(
    CardMeta("luck", "幸运信息", ResultShareCards.composite("luck", period, fortune)) {},
    CardMeta("evidence", "评分依据", ResultShareCards.composite("evidence", period, fortune)) {},
    CardMeta("caution", "注意事项", ResultShareCards.composite("caution", period, fortune)) {}
)

@Composable
private fun LuckCard(shareCard: ShareCard?, fortune: CompositeDailyFortune) {
    FortuneCard(cardId = "luck", title = "幸运信息", shareCard = shareCard) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LuckChip("幸运数字", fortune.luckyNumber.toString(), Modifier.weight(1f))
            LuckChip("幸运色", fortune.luckyColor, Modifier.weight(1f))
            LuckChip("吉利方位", fortune.luckyDirection, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "幸运数取自喜用神「${fortune.luckyDirection}」的河图生成数，颜色与方位沿用八字喜用。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 评分依据：把这一周期真正参与加减分的信号摊开给用户提供证据 */
@Composable
private fun EvidenceCard(shareCard: ShareCard?, fortune: CompositeDailyFortune) {
    FortuneCard(cardId = "evidence", title = "评分依据", shareCard = shareCard) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "下面每一条都是${periodLabel(fortune.period)}加减分的实际理由，不是事后配的文案。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FortuneInsightList(
                fortune.insights,
                emptyText = "这一周期八字与星盘都没有查到足以改变分数的信号，分数由命局常态与基础天象给出。"
            )
        }
    }
}

@Composable
private fun CautionCard(shareCard: ShareCard?, cautions: String) {
    FortuneCard(cardId = "caution", title = "注意事项", shareCard = shareCard) {
        FortuneProse(cautions)
    }
}

@Composable
private fun LuckChip(title: String, value: String, modifier: Modifier = Modifier) {
    FortuneCard(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** 页面收尾说明 */
@Composable
private fun FooterNote() {
    Text(
        "本页运势由出生信息按日期本地确定性推算（离线可用），融合东方八字与西方星盘，仅供娱乐参考。",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
    )
}

private fun periodLabel(period: String): String = when (period) {
    "week" -> "本周"
    "month" -> "本月"
    "year" -> "本年"
    else -> "今日"
}
