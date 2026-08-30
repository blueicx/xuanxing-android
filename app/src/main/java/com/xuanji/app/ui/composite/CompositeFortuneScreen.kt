package com.xuanji.app.ui.composite

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.CompositeDailyFortune
import com.xuanji.app.di.AppModule
import com.xuanji.app.ui.components.CardLayouts
import com.xuanji.app.ui.components.CardMeta
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.FortuneDimensionDetail
import com.xuanji.app.ui.components.FortuneInsightList
import com.xuanji.app.ui.components.FortunePageWidth
import com.xuanji.app.ui.components.FortuneProse
import com.xuanji.app.ui.components.FortuneStickyHeader
import com.xuanji.app.ui.components.LocalCardLayout
import com.xuanji.app.ui.components.MysticFloatingGuide
import com.xuanji.app.ui.components.ResultShareCards
import com.xuanji.app.ui.components.RestoreCardsBar
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.ScoreRing
import com.xuanji.app.ui.components.ShareCard
import com.xuanji.app.ui.components.rememberCardLayoutController
import com.xuanji.app.ui.components.scoreColor
import com.xuanji.app.ui.viewmodel.CompositeFortuneViewModel
import com.xuanji.app.ui.viewmodel.CompositeUiState
import com.xuanji.app.ui.xuanjiViewModel

/**
 * 综合运势页：八字与星盘两套体系的对照结果。
 *
 * 版式约定（三个运势页共用）：
 *  - 顶部一条「置顶栏」固定不动，写清楚当前看的是哪一段周期、两套体系各判多少分，
 *    周期切换器放在这里，滚动时不会消失；
 *  - 下面的正文用一条 ScrollState 贯穿滚动，不再出现「卡片里套滚动」的双滚动条；
 *  - 正文限宽居中，大屏不会把行拉得太长；底部留出自适应的收尾间距。
 */
@Composable
fun CompositeFortuneScreen(
    viewModel: CompositeFortuneViewModel = xuanjiViewModel {
        CompositeFortuneViewModel(AppModule.repository)
    }
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
    bazi: BaziFull,
    fortune: CompositeDailyFortune,
    period: String,
    onPeriodChange: (String) -> Unit
) {
    val controller = rememberCardLayoutController("composite", bazi.chart.display)
    val cards = fortuneCards(fortune, period)

    CompositionLocalProvider(LocalCardLayout provides controller) {
        MysticFloatingGuide(bazi, fortune) { scrollState ->
            Column(Modifier.fillMaxSize()) {
                FortuneStickyHeader(
                    period = period,
                    onPeriodChange = onPeriodChange,
                    headline = "${periodLabel(period)}综合 ${fortune.overallScore} 分",
                    subtitle = fortune.headlineLine()
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
                            SummaryBlock(fortune)
                            SystemSplitCard(fortune, period)
                            CardLayouts.ordered(cards, controller.state).forEach { card ->
                                when (card.id) {
                                    "luck" -> LuckCard(card.shareCard, fortune)
                                    "caution" -> CautionCard(card.shareCard, fortune.cautions)
                                    "evidence" -> EvidenceCard(card.shareCard, fortune)
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

/** 置顶栏副标题：两套体系各自判了多少分 */
private fun CompositeDailyFortune.headlineLine(): String =
    "东方八字 ${eastern.overallScore} 分 · 西方星盘 ${western.overallScore} 分 · $dateKey"

/** 本周期总评：融合两套体系立论的长段落 */
@Composable
private fun SummaryBlock(fortune: CompositeDailyFortune) {
    FortuneCard(cardId = "summary", title = "本周期总评") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(fortune.overallScore)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
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
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

/** 两套体系并排：各自的结论 + 各自的取证 */
@Composable
private fun SystemSplitCard(fortune: CompositeDailyFortune, period: String) {
    val share: ShareCard? = ResultShareCards.composite("systems", period, fortune)
    FortuneCard(cardId = "systems", title = "两套体系怎么说的", shareCard = share) {
        var tab by rememberSaveable(fortune.dateKey, period) { mutableStateOf("east") }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SystemTab("八字（东方）", tab == "east") { tab = "east" }
            SystemTab("星盘（西方）", tab == "west") { tab = "west" }
        }
        Spacer(Modifier.height(12.dp))
        if (tab == "east") {
            val e = fortune.eastern
            FortuneProse(e.summary, "论断依据：${e.periodPillarText.ifBlank { e.dayPillarText }}")
            Spacer(Modifier.height(10.dp))
            SectionTitle("东方信号")
            FortuneInsightList(e.insights)
            Spacer(Modifier.height(12.dp))
            SectionTitle("东方分项详批")
            FortuneDimensionDetail(e.dimensionNotes)
        } else {
            val w = fortune.western
            FortuneProse(w.summary, "论断依据：${w.sign}当值行运")
            Spacer(Modifier.height(10.dp))
            SectionTitle("西方信号")
            FortuneInsightList(w.insights)
            Spacer(Modifier.height(12.dp))
            SectionTitle("西方分项详批")
            FortuneDimensionDetail(w.dimensionNotes)
        }
    }
}

@Composable
private fun RowScope.SystemTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun fortuneCards(fortune: CompositeDailyFortune, period: String): List<CardMeta> = listOf(
    CardMeta("luck", "幸运信息", ResultShareCards.composite("luck", period, fortune)) {},
    CardMeta(
        id = "dimensions",
        title = "六维详批",
        shareCard = ResultShareCards.composite("dimensions", period, fortune)
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle("${periodLabel(period)}分项详批")
            Spacer(Modifier.height(4.dp))
            FortuneDimensionDetail(fortune.dimensions)
        }
    },
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
