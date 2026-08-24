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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.TodayOracle
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun TodayOracleScreen() {
    val today = LocalDate.now()
    val dailyState = produceState<TodayOracle.OracleResult?>(initialValue = null, today) {
        value = AppModule.repository.getOrDrawTodayOracle(today)
    }
    val randomResult = remember { mutableStateOf<TodayOracle.OracleResult?>(null) }
    val reaction = remember { mutableStateOf<TodayOracle.OracleReaction?>(null) }
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

                TodayObserverBubble(reaction = TodayOracle.dailyReaction(shown))
            }
        }

        if (shown != null) {
            Button(
                onClick = {
                    val draw = TodayOracle.randomDraw()
                    randomResult.value = draw
                    reaction.value = TodayOracle.manualReaction(draw)
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
                    TodayObserverBubble(reaction = reaction)
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
private fun TodayObserverBubble(reaction: TodayOracle.OracleReaction) {
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
            }
        }
    }
}
