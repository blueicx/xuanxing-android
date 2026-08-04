package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.TodayOracle
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun TodayOracleScreen() {
    val today = LocalDate.now()
    val result = remember(today) { TodayOracle.generate(today) }
    val randomResult = remember { mutableStateOf<TodayOracle.OracleResult?>(null) }
    val shown = randomResult.value ?: result

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
        }

        Button(
            onClick = { randomResult.value = TodayOracle.randomDraw() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("随机重抽一签")
        }
        if (randomResult.value != null) {
            Button(
                onClick = { randomResult.value = null },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("恢复今日签")
            }
        }
        SystemExplanation("today")
    }
}
