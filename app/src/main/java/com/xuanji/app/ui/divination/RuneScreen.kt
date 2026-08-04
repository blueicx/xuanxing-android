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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xuanji.app.domain.divination.Rune
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun RuneScreen() {
    val today = LocalDate.now()
    val daily = remember(today) { Rune.daily(today) }
    val drawn = remember { mutableStateOf<Rune.DrawResult?>(null) }
    val shown = drawn.value ?: daily

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("北欧符文", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        FortuneCard {
            SectionTitle(if (drawn.value == null) "今日符文 · $today" else "抽签结果")
            Spacer(Modifier.height(12.dp))
            Text(
                shown.rune.symbol,
                fontSize = 96.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                shown.rune.name + if (shown.reversed) "（逆位）" else "（正位）",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (shown.reversed) shown.rune.reversed else shown.rune.upright,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text(
                shown.verdict,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(onClick = { drawn.value = Rune.random() }, modifier = Modifier.fillMaxWidth()) {
            Text("随机抽签")
        }
        if (drawn.value != null) {
            Button(onClick = { drawn.value = null }, modifier = Modifier.fillMaxWidth()) {
                Text("恢复今日符文")
            }
        }
        SystemExplanation("rune")
    }
}
