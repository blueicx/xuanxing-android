package com.xuanji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.Element
import com.xuanji.app.data.model.FortuneDimension
import com.xuanji.app.data.model.FortuneInsight
import com.xuanji.app.domain.elementColorCompose
import com.xuanji.app.domain.elementName
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FortuneCard(
    modifier: Modifier = Modifier,
    cardId: String? = null,
    title: String? = null,
    shareCard: ShareCard? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val layout = LocalCardLayout.current
    val editable = layout != null && cardId != null && title != null
    val editing = editable && layout?.editingCardId == cardId
    val cardModifier = if (editable) {
        modifier
            .fillMaxWidth()
            .cardDragReorder(
                enabled = editing,
                cardId = cardId!!,
                controller = layout!!
            )
            .combinedClickable(
                onClickLabel = "查看卡片",
                onLongClickLabel = "编辑卡片",
                onClick = {},
                onLongClick = { layout.startEdit(cardId) }
            )
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (editing) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (layout != null && cardId != null && title != null) {
                CardControls(
                    title = title,
                    cardId = cardId,
                    controller = layout,
                    shareCard = shareCard
                )
                Spacer(Modifier.height(8.dp))
            } else if (shareCard != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                    ShareButton(sharedCard = shareCard)
                }
                Spacer(Modifier.height(4.dp))
            }
            content()
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    SectionTitle(text) {}
}

@Composable
fun SectionTitle(
    text: String,
    trailing: @Composable RowScope.() -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
    )
}

@Composable
fun PeriodToggleRow(
    currentPeriod: String,
    onSelect: (String) -> Unit
) {
    val options = listOf("day" to "日", "week" to "周", "month" to "月", "year" to "年")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (period, label) ->
            val selected = period == currentPeriod
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onSelect(period) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScoreRow(label: String, score: Int) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$score",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("$label：", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = MaterialTheme.colorScheme.onSurface)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun ElementBalance(counts: Map<Element, Int>) {
    val max = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val order = listOf(
        Element.WOOD, Element.FIRE, Element.EARTH, Element.METAL, Element.WATER
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        order.forEach { e ->
            val v = counts[e] ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    elementName(e),
                    modifier = Modifier.width(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(v.toFloat() / max)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(elementColorCompose(e))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (v == 0) "缺" else "$v",
                    modifier = Modifier.width(20.dp),
                    color = if (v == 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun PillarCard(chart: BaziChart) {
    pillarRow(chart)
}

@Composable
private fun pillarRow(chart: BaziChart) {
    val pillars = listOf(
        "年" to chart.yearPillar,
        "月" to chart.monthPillar,
        "日" to chart.dayPillar,
        "时" to chart.hourPillar
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        pillars.forEach { (label, p) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    p.stem.chinese,
                    style = MaterialTheme.typography.titleLarge,
                    color = elementColorCompose(p.stem.element),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    p.branch.chinese,
                    style = MaterialTheme.typography.titleLarge,
                    color = elementColorCompose(p.branch.element),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 页面内容宽度：手机满宽，平板/折叠屏展开时限宽居中，
 * 避免正文行宽过长读不动。所有运势页共用同一个约束。
 */
@Composable
fun FortunePageWidth(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) { content() }
    }
}

/**
 * 置顶周期栏：贴在状态栏下方，不随页面滚动。
 * 里面放日/周/月/年切换，让「当前看的是哪一段周期」始终可见。
 */
@Composable
fun FortuneStickyHeader(
    period: String,
    onPeriodChange: (String) -> Unit,
    headline: String,
    subtitle: String,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FortunePageWidth {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        headline,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(Modifier.width(8.dp))
                trailing()
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    PeriodToggleRow(period, onPeriodChange)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

/**
 * 评分依据列表：每一条都是解盘器真正用到的信号，
 * 带体系标签（东方/西方）与实际加减分，做到「说的话」与「算的分」同源。
 */
@Composable
fun FortuneInsightList(
    insights: List<FortuneInsight>,
    emptyText: String = "这一周期两套体系都没有查到值得单列的信号。"
) {
    if (insights.isEmpty()) {
        Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        insights.forEach { ins ->
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (ins.weight >= 0) Color(0xFF16331F) else Color(0xFF3A1A1A),
                    contentColor = if (ins.weight >= 0) Color(0xFFA5D6A7) else Color(0xFFFF8A80)
                ) {
                    Text(
                        (if (ins.weight >= 0) "+" else "") + ins.weight,
                        Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ins.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            ins.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        ins.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 维度解说：分数条 + 解盘文字（东方/西方/综合三页共用） */
@Composable
fun FortuneDimensionDetail(
    dims: List<FortuneDimension>,
    comparisonOf: (FortuneDimension) -> String? = { null }
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        dims.forEach { d ->
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        d.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${d.score} 分",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scoreColor(d.score),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { d.score / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = scoreColor(d.score),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    d.interpretation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val cmp = comparisonOf(d)
                if (!cmp.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        cmp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

/** 长段落解说块：段落式总评，字距行距按移动端阅读习惯排 */
@Composable
fun FortuneProse(text: String, title: String? = null) {
    if (text.isBlank()) return
    Column(Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun scoreColor(score: Int): Color = when {
    score >= 82 -> Color(0xFF69D68B)
    score >= 68 -> Color(0xFFA8D08A)
    score >= 52 -> Color(0xFFE0C066)
    score >= 38 -> Color(0xFFE8996A)
    else -> Color(0xFFE27676)
}

/**
 * 总分环：把分数画成一段圆弧，配色与下面的分数条同一套规则，
 * 页面顶部扫一眼就知道这一周期整体是吉是凶。
 */
@Composable
fun ScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 78.dp,
    caption: String = "总分"
) {
    val value = score.coerceIn(0, 100)
    val color = scoreColor(value)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier
            .size(diameter)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = size.minDimension / 10f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val ring = Stroke(width = stroke, cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = ring
            )
            drawArc(
                color = color,
                startAngle = -220f,
                sweepAngle = 260f * value / 100f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = ring
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$value",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
