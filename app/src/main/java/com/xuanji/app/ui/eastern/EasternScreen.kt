package com.xuanji.app.ui.eastern

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.data.model.BaziConclusion
import com.xuanji.app.data.model.BaziFull
import com.xuanji.app.data.model.Branch
import com.xuanji.app.data.model.BranchRelation
import com.xuanji.app.data.model.DaYun
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.EasternDailyFortune
import com.xuanji.app.data.model.Geju
import com.xuanji.app.data.model.ShenShaItem
import com.xuanji.app.data.model.ShenShaMeta
import com.xuanji.app.data.model.TenGod
import com.xuanji.app.data.model.TenGodItem
import com.xuanji.app.data.model.YongJi
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.BaziCalculator
import com.xuanji.app.domain.HourGuide
import com.xuanji.app.domain.elementName
import com.xuanji.app.ui.components.ElementBalance
import com.xuanji.app.ui.components.CardLayouts
import com.xuanji.app.ui.components.CardMeta
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.HealthBodyAtlas
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.LocalCardLayout
import com.xuanji.app.ui.components.MysticFloatingGuide
import com.xuanji.app.ui.components.PeriodToggleRow
import com.xuanji.app.ui.components.PillarCard
import com.xuanji.app.ui.components.ResultShare
import com.xuanji.app.ui.components.ResultShareCards
import com.xuanji.app.ui.components.RestoreCardsBar
import com.xuanji.app.ui.components.ScoreRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.ShareButton
import com.xuanji.app.ui.components.rememberCardLayoutController
import com.xuanji.app.ui.components.WuxingRatioList
import com.xuanji.app.ui.components.WuxingWheel
import com.xuanji.app.ui.viewmodel.EasternUiState
import com.xuanji.app.ui.viewmodel.EasternViewModel
import com.xuanji.app.ui.xuanjiViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EasternScreen() {
    val viewModel = xuanjiViewModel { EasternViewModel(AppModule.repository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val readyState = uiState as? EasternUiState.Ready
    val controller = rememberCardLayoutController(
        "eastern",
        readyState?.full?.chart?.display ?: "guest"
    )

    val content: @Composable (ScrollState) -> Unit = when (val s = uiState) {
        is EasternUiState.Loading -> {
            {
                EasternMessage("正在推算命盘…")
            }
        }
        is EasternUiState.Empty -> {
            {
                EasternMessage("尚未设置出生信息，请先到「我的」填写。")
            }
        }
        is EasternUiState.Ready -> {
            {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(it)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val cards = easternCards(
                        full = s.full,
                        hourGuides = s.hourGuides,
                        fortune = s.fortune,
                        period = s.period,
                        onPeriodChange = viewModel::setPeriod
                    )
                    CardLayouts.ordered(cards, controller.state).forEach { card ->
                        if (card.id == "fortune") {
                            PeriodToggleRow(s.period, viewModel::setPeriod)
                        }
                        card.content()
                    }
                    if (controller.state.hiddenCount > 0) {
                        RestoreCardsBar(controller)
                    }
                    Text(
                        s.full.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val composite = (uiState as? EasternUiState.Ready)?.composite
    val readyBazi = (uiState as? EasternUiState.Ready)?.full
    if (readyBazi != null && composite != null) {
        CompositionLocalProvider(LocalCardLayout provides controller) {
            MysticFloatingGuide(readyBazi, composite) { scrollState -> content(scrollState) }
        }
    } else {
        content(rememberScrollState())
    }
}

private fun easternCards(
    full: BaziFull,
    hourGuides: List<HourGuide>,
    fortune: EasternDailyFortune,
    period: String,
    onPeriodChange: (String) -> Unit
): List<CardMeta> {
    fun share(cardId: String) = ResultShareCards.eastern(
        cardId, period, full, fortune, hourGuides.firstOrNull()?.timeText
    )
    return listOf(
        CardMeta("hours", "今日吉时", share("hours")) { HourGuideSection(hourGuides, share("hours")) },
        CardMeta("pillars", "八字命盘", share("pillars")) { PillarSection(full.chart, share("pillars")) },
        CardMeta("conclusion", "综合结论", share("conclusion")) { ConclusionSection(full.conclusion, share("conclusion")) },
        CardMeta("geju", "生辰格局", share("geju")) { GejuSection(full.geju, share("geju")) },
        CardMeta("elements", "五行占比", share("elements")) { FiveElementsSection(full.chart.elementCounts, share("elements")) },
        CardMeta("ten-gods", "十神格局", share("ten-gods")) { TenGodSection(full.tenGods, share("ten-gods")) },
        CardMeta("ten-god-ratio", "十神占比", share("ten-god-ratio")) { TenGodProportionSection(full.tenGods, share("ten-god-ratio")) },
        CardMeta("strength", "日主旺衰", share("strength")) { StrengthSection(full.strength, share("strength")) },
        CardMeta("yongji", "用神忌神", share("yongji")) { YongJiSection(full.yongJi, share("yongji")) },
        CardMeta("dayun", "大运流年", share("dayun")) { DaYunSection(full.daYun, share("dayun")) },
        CardMeta("relations", "刑冲合害", share("relations")) { RelationsSection(full.relations, share("relations")) },
        CardMeta("shensha", "神煞", share("shensha")) { ShenShaSection(full.shenSha, share("shensha")) },
        CardMeta("atlas", "神煞图鉴", share("shensha")) { ShenShaAtlasSection(full.shenSha, share("shensha")) },
        CardMeta("fortune", "周期运势", share("fortune")) {
            TodayFortuneSection(
                fortune = fortune,
                favorable = full.chart.favorableElements,
                period = period,
                onPeriodChange = onPeriodChange,
                shareCard = share("fortune")
            )
        }
    )
}

@Composable
private fun EasternMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "五行八字",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PillarSection(
    chart: com.xuanji.app.data.model.BaziChart,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "pillars", title = "八字命盘（四柱）", shareCard = shareCard) {
        Spacer(Modifier.height(12.dp))
        PillarCard(chart)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                InfoRow("日主", "${chart.dayMaster.chinese}（${elementName(chart.dayMasterElement)}）")
            }
            Column(Modifier.weight(1f)) {
                InfoRow("生肖", chart.zodiac)
            }
        }
    }
}

@Composable
private fun FiveElementsSection(
    counts: Map<Element, Int>,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "elements", title = "五行占比 · 相生相克", shareCard = shareCard) {
        Spacer(Modifier.height(4.dp))
        Text(
            "圆环外侧绿色箭头为相生（木→火→土→金→水→木），内部红色虚线为相克（木克土、土克水、水克火、火克金、金克木）；节点大小与百分比代表该五行在命局中的占比。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        WuxingWheel(counts)
        Spacer(Modifier.height(4.dp))
        WuxingRatioList(counts)
    }
}

@Composable
private fun ConclusionSection(
    c: BaziConclusion,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "conclusion", title = "综合结论", shareCard = shareCard) {
        Spacer(Modifier.height(8.dp))
        Text(
            c.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(14.dp))
        c.items.forEach { item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.icon, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    item.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.title == "健康" && item.highlightParts.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HealthBodyAtlas(item.highlightParts)
                }
                // 健康项的人体图已经按部位拆开显示了 chip，不再用 TagFlow 重复合并字符串
                if (item.title != "健康" && item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    TagFlow(item.tags)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** 简单标签流（每行 3 个，避免引入 FlowRow 实验 API） */
@Composable
private fun TagFlow(tags: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        tags.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { t ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            t,
                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TenGodSection(
    items: List<TenGodItem>,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    val byPillar = items.groupBy { it.pillarLabel }
    val order = listOf("年", "月", "日", "时")
    FortuneCard(cardId = "ten-gods", title = "十神格局", shareCard = shareCard) {
        Spacer(Modifier.height(8.dp))
        Text(
            "以日干为「我」，推演其余干支与日主的关系。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        order.forEach { label ->
            val list = byPillar[label] ?: return@forEach
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    "${label}柱",
                    Modifier.width(40.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    list.forEach { item ->
                        Text(
                            "${item.position} ${item.stem.chinese} → ${item.tenGod.chinese}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun StrengthSection(
    strength: com.xuanji.app.data.model.StrengthResult,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "strength", title = "日主旺衰", shareCard = shareCard) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                strength.level,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "评分 ${strength.score}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(strength.desc, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun YongJiSection(
    yongJi: YongJi,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "yongji", title = "用神 · 忌神", shareCard = shareCard) {
        Spacer(Modifier.height(8.dp))
        InfoRow("用神", yongJi.useful.joinToString("、") { elementName(it) })
        InfoRow("忌神", yongJi.avoidance.joinToString("、") { elementName(it) })
        Spacer(Modifier.height(6.dp))
        Text(yongJi.desc, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DaYunSection(
    daYun: List<DaYun>,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "dayun", title = "大运 · 流年", shareCard = shareCard) {
        Spacer(Modifier.height(8.dp))
        daYun.forEach { dy ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${dy.index}运", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Text(dy.pillar.display, style = MaterialTheme.typography.titleMedium)
                Text("${dy.startAge}-${dy.endAge}岁", style = MaterialTheme.typography.bodyMedium)
            }
            Text(dy.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun RelationsSection(
    relations: List<BranchRelation>,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "relations", title = "刑冲合害", shareCard = shareCard) {
        Spacer(Modifier.height(8.dp))
        relations.forEach { r ->
            Text(
                "${r.type}：${r.desc}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun HourGuideSection(
    guides: List<HourGuide>,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    val ordered = Branch.values().mapNotNull { branch -> guides.firstOrNull { it.pillar.branch == branch } }
    var selectedBranch by remember(guides) { mutableStateOf(ordered.firstOrNull()?.pillar?.branch) }
    val selected = ordered.firstOrNull { it.pillar.branch == selectedBranch } ?: ordered.firstOrNull()
    val outline = MaterialTheme.colorScheme.outline

    FortuneCard(cardId = "hours", title = "今日吉时 · 五行择时", shareCard = shareCard) {
        Spacer(Modifier.height(6.dp))
        Text(
            "按五鼠遁推十二时辰干支，以日主生克与喜忌修正排序；点击圆盘查看时辰详情。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(ordered) {
                        detectTapGestures { offset ->
                            val dx = offset.x - size.width / 2f
                            val dy = offset.y - size.height / 2f
                            val bearing = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0 + 360.0) % 360.0
                            val index = (((bearing + 15.0) / 30.0).toInt() % 12 + 12) % 12
                            selectedBranch = Branch.values()[index]
                        }
                    }
            ) {
                val w = size.width
                val cx = w / 2f
                val cy = size.height / 2f
                val ringR = w * 0.34f
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = ringR + w * 0.10f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = outline.copy(alpha = 0.20f),
                    radius = ringR + w * 0.10f,
                    center = Offset(cx, cy),
                    style = Stroke(1.dp.toPx())
                )

                val paint = Paint().apply {
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    textSize = w * 0.052f
                    color = android.graphics.Color.WHITE
                }

                ordered.forEachIndexed { i, guide ->
                    val rad = Math.toRadians(-90.0 + i * 30.0)
                    val center = Offset(
                        cx + ringR * cos(rad).toFloat(),
                        cy + ringR * sin(rad).toFloat()
                    )
                    val isSelected = guide.pillar.branch == selectedBranch
                    val nodeR = if (isSelected) w * 0.075f else w * 0.058f
                    val color = hourScoreColor(guide.score)

                    drawLine(
                        color = outline.copy(alpha = 0.12f),
                        start = Offset(cx, cy),
                        end = center,
                        strokeWidth = 1.dp.toPx()
                    )
                    if (guide.isCurrent) {
                        drawCircle(
                            color = Color(0xFFE9D8A6),
                            radius = nodeR + 4.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
                        )
                    }
                    drawCircle(color = color.copy(alpha = 0.18f), radius = nodeR, center = center)
                    drawCircle(color = color, radius = nodeR * 0.68f, center = center)
                    drawContext.canvas.nativeCanvas.drawText(
                        guide.pillar.branch.chinese,
                        center.x,
                        center.y + paint.textSize * 0.35f,
                        paint
                    )
                }

                val hubPaint = Paint().apply {
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    textSize = w * 0.072f
                    color = android.graphics.Color.argb(255, 233, 216, 166)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "${selected?.level ?: ""} ${selected?.score ?: ""}",
                    cx,
                    cy + hubPaint.textSize * 0.35f,
                    hubPaint
                )
            }
        }

        if (selected != null) {
            Spacer(Modifier.height(12.dp))
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${selected.pillar.display}时", style = MaterialTheme.typography.titleMedium)
                        Text(selected.timeText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(
                        "${selected.level} · ${selected.relationSummary}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = hourScoreColor(selected.score)
                    )
                    Text(selected.advice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun hourScoreColor(score: Int): Color = when {
    score >= 70 -> Color(0xFF66BB6A)
    score >= 55 -> Color(0xFFA5D6A7)
    score >= 40 -> Color(0xFFC8BEE8)
    else -> Color(0xFFFF8A80)
}

@Composable
private fun TodayFortuneSection(
    fortune: EasternDailyFortune,
    favorable: List<Element>,
    period: String = "day",
    onPeriodChange: (String) -> Unit,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "fortune", title = "周期运势", shareCard = shareCard) {
        val periodTitle = when (period) {
            "week" -> "本周"
            "month" -> "本月"
            else -> "今日"
        }
        Text("${periodTitle}运势 · ${fortune.dateKey}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        PeriodToggleRow(period, onPeriodChange)
        Spacer(Modifier.height(8.dp))
        Text(fortune.summary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        ScoreRow("综合", fortune.overallScore)
        ScoreRow("事业", fortune.careerScore)
        ScoreRow("财运", fortune.wealthScore)
        ScoreRow("感情", fortune.loveScore)
        ScoreRow("健康", fortune.healthScore)
        Spacer(Modifier.height(8.dp))
        InfoRow("今日干支", fortune.dayPillarText)
        InfoRow("喜用神", favorable.joinToString("、") { elementName(it) })
        InfoRow("幸运色", fortune.luckyColor)
        InfoRow("吉利方位", fortune.luckyDirection)
        Spacer(Modifier.height(8.dp))
        Text(
            fortune.advice,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NatureChip(text: String) {
    val (containerColor, contentColor) = when {
        text.contains("凶") -> Color(0xFF3A1A1A) to Color(0xFFFF8A80)
        text.contains("吉") -> Color(0xFF16331F) to Color(0xFFA5D6A7)
        text.contains("中") -> Color(0xFF2E2748) to Color(0xFFC8BEE8)
        else -> Color(0xFF3A2F12) to Color(0xFFE9D8A6) // 格局类
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun GejuSection(
    geju: Geju,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "geju", title = "生辰格局", shareCard = shareCard) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                geju.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            NatureChip(geju.category)
        }
        Spacer(Modifier.height(6.dp))
        Text(geju.desc, style = MaterialTheme.typography.bodyMedium)
    }
}

private val TEN_GOD_COLORS = mapOf<TenGod, Color>(
    TenGod.比肩 to Color(0xFF8D6E63), TenGod.劫财 to Color(0xFFB08C7E),
    TenGod.正印 to Color(0xFF5FB87A), TenGod.偏印 to Color(0xFF8FD9A8),
    TenGod.食神 to Color(0xFFE0594E), TenGod.伤官 to Color(0xFFEFA79F),
    TenGod.正财 to Color(0xFFC9A227), TenGod.偏财 to Color(0xFFE4C766),
    TenGod.正官 to Color(0xFF4A90D9), TenGod.七杀 to Color(0xFF8FB8E8)
)

@Composable
private fun TenGodProportionSection(
    items: List<TenGodItem>,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    val order = listOf(
        TenGod.正官, TenGod.七杀, TenGod.正印, TenGod.偏印,
        TenGod.正财, TenGod.偏财, TenGod.食神, TenGod.伤官,
        TenGod.比肩, TenGod.劫财
    )
    val counts = order.associateWith { tg -> items.count { it.tenGod == tg } }
    val total = counts.values.sum().coerceAtLeast(1)
    FortuneCard(cardId = "ten-god-ratio", title = "十神占比", shareCard = shareCard) {
        Spacer(Modifier.height(6.dp))
        Text(
            "天干与地支藏干共推演出下列十神分布（按出现次数计）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            order.forEach { tg ->
                val c = counts[tg] ?: 0
                if (c > 0) Box(
                    Modifier
                        .weight(c.toFloat())
                        .fillMaxWidth()
                        .background(TEN_GOD_COLORS.getValue(tg))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "共 $total 处",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        val groups = listOf(
            "克我" to listOf(TenGod.正官, TenGod.七杀),
            "生我" to listOf(TenGod.正印, TenGod.偏印),
            "我克" to listOf(TenGod.正财, TenGod.偏财),
            "我生" to listOf(TenGod.食神, TenGod.伤官),
            "同我" to listOf(TenGod.比肩, TenGod.劫财)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.forEach { (cat, list) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        cat,
                        Modifier.width(40.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    list.forEach { tg ->
                        val c = counts[tg] ?: 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(TEN_GOD_COLORS.getValue(tg))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${tg.chinese} $c",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShenShaSection(
    items: List<ShenShaItem>,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    FortuneCard(cardId = "shensha", title = "神煞（命中所带）", shareCard = shareCard) {
        Spacer(Modifier.height(6.dp))
        if (items.isEmpty()) {
            Text(
                "未检出明显神煞。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    s.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                NatureChip(s.nature)
                                s.branch?.let {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "落${it.chinese}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                s.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShenShaAtlasSection(
    present: List<ShenShaItem>,
    shareCard: com.xuanji.app.ui.components.ShareCard
) {
    val presentNames = present.map { it.name }.toSet()
    val presentBranch = present.associate { it.name to it.branch }
    val atlas = BaziCalculator.SHEN_SHA_ATLAS
    FortuneCard(cardId = "atlas", title = "神煞图鉴", shareCard = shareCard) {
        Spacer(Modifier.height(4.dp))
        Text(
            "常见神煞一览；命中带者以高亮标注（✓）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        atlas.chunked(2).forEach { rowItems ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { meta ->
                    AtlasCell(
                        meta = meta,
                        has = presentNames.contains(meta.name),
                        branch = presentBranch[meta.name]?.chinese,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AtlasCell(meta: ShenShaMeta, has: Boolean, branch: String?, modifier: Modifier = Modifier) {
    val border = if (has) androidx.compose.foundation.BorderStroke(
        1.5.dp,
        MaterialTheme.colorScheme.primary
    ) else androidx.compose.foundation.BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    )
    val container = if (has) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = border,
        modifier = modifier
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(meta.icon, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(6.dp))
                Text(meta.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (has) {
                    Spacer(Modifier.width(4.dp))
                    Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NatureChip(meta.nature)
                if (has && branch != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "落$branch",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                meta.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
