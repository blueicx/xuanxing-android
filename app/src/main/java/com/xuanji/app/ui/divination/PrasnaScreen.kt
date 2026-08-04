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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.Prasna
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDateTime

@Composable
fun PrasnaScreen() {
    var question by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("普拉萨那（卜卦占星）", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "吠陀占星中的即时卜卦术：输入你的问题，按提问时刻起盘解读行星影响。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SystemExplanation("prasna")
        FortuneCard {
            SectionTitle("提问")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("你的问题，如：我的事业会有进展吗？") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (question.trim().isNotEmpty()) {
            val res = Prasna.calculate(question.trim(), LocalDateTime.now())
            FortuneCard {
                SectionTitle("卜卦星盘 · ${res.askTime.toLocalDate()}")
                Spacer(Modifier.height(8.dp))
                Text("上升点：${res.ascHouse}（第${res.ascHouseIndex + 1}宫）", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("行星分布：", style = MaterialTheme.typography.bodyMedium)
                res.planets.forEach { pl ->
                    Text("　${pl.planet} 在 ${pl.house}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "强势行星：${if (res.strongPlanets.isEmpty()) "无明显强势行星" else res.strongPlanets.joinToString("、")}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            FortuneCard {
                SectionTitle("详细解读")
                Spacer(Modifier.height(8.dp))
                Text(res.detail, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Text("输入问题后按当前时刻起盘解读。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
