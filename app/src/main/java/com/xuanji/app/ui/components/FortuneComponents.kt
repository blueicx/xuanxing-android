package com.xuanji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xuanji.app.data.model.BaziChart
import com.xuanji.app.data.model.Element
import com.xuanji.app.domain.elementColorCompose
import com.xuanji.app.domain.elementName

@Composable
fun FortuneCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
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
    val options = listOf("day" to "日", "week" to "周", "month" to "月")
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
