package com.xuanji.app.ui.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.NagaRain
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun NagaRainScreen() {
    val currentYear = LocalDate.now().year
    var year by rememberSaveable { mutableIntStateOf(currentYear) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("那伽占雨", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "泰国传统占星以「那迦献水」古法预测年度降雨：佛历生肖年对应不同数量的那迦（Naga）献水，决定当年雨水多寡与农事吉凶。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FortuneCard {
            SectionTitle("选择公历年份")
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { year-- }) { Text("− 上一年") }
                Text("$year 年", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = { year++ }) { Text("下一年 +") }
            }
        }

        val result = NagaRain.predict(year)
        FortuneCard {
            SectionTitle("${result.zodiac}年 · 佛历 ${result.yearBe}")
            Spacer(Modifier.height(8.dp))
            Text(
                "公历 ${result.yearCe} 年 = 佛历 ${result.yearBe} 年",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "当年那迦数量：${result.nagaCount} 条",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "寓意：${result.nagaMeaning}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            RainDetailRow("平地降雨量", result.rainEarth)
            RainDetailRow("森林降雨量", result.rainForest)
            RainDetailRow("山区降雨量", result.rainMountain)
            RainDetailRow("空中降雨量", result.rainAir)
            Spacer(Modifier.height(10.dp))
            Text("【年度农业解读】", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(result.interpretation, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "农事建议：${result.farmingAdvice}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        FortuneCard {
            SectionTitle("十二生肖年 · 那迦数量一览")
            Spacer(Modifier.height(8.dp))
            NagaRain.countTable().forEach { (z, count, meaning) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(z, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(160.dp))
                    Text("$count 条", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(64.dp))
                    Text(meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SystemExplanation("naga")
    }
}

@Composable
private fun RainDetailRow(label: String, value: Int?) {
    if (value != null) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(120.dp))
            Text("$value Ha", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
