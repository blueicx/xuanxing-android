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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.DivinationQuery
import com.xuanji.app.domain.divination.Rune
import com.xuanji.app.domain.divination.RuneReading
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun RuneScreen() {
    val today = LocalDate.now()
    val daily = remember(today) { Rune.daily(today) }
    val drawn = remember { mutableStateOf<Rune.DrawResult?>(null) }
    val stable = remember { mutableStateOf<RuneReading?>(null) }
    val spread = remember { mutableStateOf("single") }
    val question = remember { mutableStateOf("") }
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val shown = stable.value?.draws?.firstOrNull()?.result ?: drawn.value ?: daily

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("北欧符文", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = { spread.value = "single"; stable.value = null }, modifier = Modifier.weight(1f)) { Text("单符") }
            OutlinedButton(onClick = { spread.value = "three"; stable.value = null }, modifier = Modifier.weight(1f)) { Text("三符") }
        }
        OutlinedTextField(
            value = question.value,
            onValueChange = { question.value = it.take(120) },
            label = { Text("可选问题（用于稳定抽取）") },
            modifier = Modifier.fillMaxWidth()
        )

        FortuneCard {
            SectionTitle(if (drawn.value == null && stable.value == null) "今日符文 · $today" else "抽签结果")
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
        OutlinedButton(onClick = {
            val p = profile
            val key = p?.let { "${it.birthYear}-${it.birthMonth}-${it.birthDay}-${it.locationCode.orEmpty()}" } ?: "anonymous"
            stable.value = Rune.read(DivinationQuery("rune", key, today.toString(), question.value, spread.value))
            drawn.value = null
        }, modifier = Modifier.fillMaxWidth()) {
            Text("按问题稳定抽取")
        }
        stable.value?.let { reading ->
            Text("算法种子：${reading.seed.take(12)}…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(reading.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (drawn.value != null) {
            Button(onClick = { drawn.value = null }, modifier = Modifier.fillMaxWidth()) {
                Text("恢复今日符文")
            }
        }
        SystemExplanation("rune")
    }
}
