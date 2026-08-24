package com.xuanji.app.ui.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.TodayOracle
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun TodayOracleScreen() {
    val today = LocalDate.now()
    val dailyState = produceState<TodayOracle.OracleResult?>(initialValue = null, today) {
        value = AppModule.repository.getOrDrawTodayOracle(today)
    }
    val randomResult = remember { mutableStateOf<TodayOracle.OracleResult?>(null) }
    val reaction = remember { mutableStateOf<TodayOracle.OracleReaction?>(null) }
    val extraToken = remember { mutableStateOf(0) }
    val shown = dailyState.value

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("今日算命", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        FortuneCard {
            SectionTitle("今日灵签 · ${today}")
            Spacer(Modifier.height(8.dp))
            if (shown == null) {
                Text("正在展开今日签……", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(shown.level, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                Text("「${shown.poem}」", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                InfoRow("幸运数字", "${shown.luckyNumber}")
                InfoRow("幸运色", shown.luckyColor)
                InfoRow("宜", shown.good)
                InfoRow("忌", shown.avoid)
                Spacer(Modifier.height(8.dp))
                Text(shown.advice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                TodayObserverBubble(
                    sourceKey = "daily-$today",
                    draw = shown,
                    reaction = TodayOracle.dailyReaction(shown)
                )
            }
        }

        if (shown != null) {
            Button(
                onClick = {
                    val draw = TodayOracle.randomDraw()
                    randomResult.value = draw
                    reaction.value = TodayOracle.manualReaction(draw)
                    extraToken.value += 1
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("手动彩蛋抽一签 · 不改今日")
            }
        }

        randomResult.value?.let { extra ->
            reaction.value?.let { reaction ->
                FortuneCard {
                    SectionTitle("手动彩蛋 · 不改今日签")
                    Spacer(Modifier.height(8.dp))
                    TodayObserverBubble(
                        sourceKey = "manual-${extraToken.value}",
                        draw = extra,
                        reaction = reaction
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(extra.level, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                    Text("「${extra.poem}」", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        SystemExplanation("today")
    }
}

@Composable
private fun TodayObserverBubble(
    sourceKey: String,
    draw: TodayOracle.OracleResult,
    reaction: TodayOracle.OracleReaction
) {
    val selectedChoice = remember(sourceKey) { mutableStateOf<String?>(null) }
    val pending = remember(sourceKey) { mutableStateOf(false) }
    val exchange = remember(sourceKey) {
        mutableStateOf<TodayOracle.OracleExchange?>(null)
    }

    LaunchedEffect(selectedChoice.value, sourceKey) {
        val choiceKey = selectedChoice.value ?: return@LaunchedEffect
        delay(320)
        exchange.value = TodayOracle.observerExchange(draw, choiceKey)
        pending.value = false
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    reaction.roleName.take(1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${reaction.roleName}刚好过来",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    reaction.line,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (selectedChoice.value == null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TodayOracle.observerChoices().forEach { choice ->
                            Surface(
                                onClick = {
                                    selectedChoice.value = choice.key
                                    pending.value = true
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                            ) {
                                Text(
                                    choice.label,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    val askedChoice = TodayOracle.observerChoices()
                        .firstOrNull { it.key == selectedChoice.value }
                    Text(
                        "你 · ${askedChoice?.label.orEmpty()}",
                        modifier = Modifier.padding(top = 10.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                    when (val answer = exchange.value) {
                        null -> Text(
                            if (pending.value) "对方正在接话···" else "",
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> {
                            Text(
                                answer.line,
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "${answer.roleName}离席 · ${answer.exitLine}",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
                            )
                        }
                    }
                }
            }
        }
    }
}
