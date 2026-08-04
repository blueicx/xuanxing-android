package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.IChing
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun IChingScreen() {
    val res = IChing.shake(LocalDate.now())
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("《易经》六爻占", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "按当日日期确定性摇卦（当日结果固定），得六爻卦象与卦辞解读。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SystemExplanation("iching")
        FortuneCard {
            SectionTitle("今日卦象 · ${res.hexagramName}")
            Spacer(Modifier.height(8.dp))
            Text(
                res.hexagramStr,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text("卦辞：${res.judgement}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("象辞：${res.image}", style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("解读")
            Spacer(Modifier.height(8.dp))
            Text(res.interpretation, style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("六爻变化")
            Spacer(Modifier.height(8.dp))
            res.lines.forEach { line ->
                Text("第${line.position}爻：${line.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
