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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.ArabicAstrology
import com.xuanji.app.domain.divination.ArabicChartData
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun ArabicAstrologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)
    val chart by AppModule.repository.natalChartFlow.collectAsStateWithLifecycle(initialValue = null)
    var name by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("阿拉伯占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "阿拉伯占星融合巴比伦、希腊与波斯传统，其最独特的贡献是「阿拉伯点」（Lots）——由三个星盘要素投影出的敏感点，另有 Jarbakhtar 周期、Tasyir 定向与 Abjad 字母数值。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard { Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            if (chart == null) {
                FortuneCard { Text("正在计算命盘…", style = MaterialTheme.typography.bodyMedium) }
            } else {
                val natalChart = chart!!
                val lon = natalChart.planets.associate { it.name to it.longitude }
                val hour = p.birthHour + p.birthMinute / 60.0
                val c = ArabicChartData(
                    ascendant = natalChart.ascendant,
                    sun = lon["太阳"] ?: 0.0, moon = lon["月亮"] ?: 0.0,
                    mercury = lon["水星"] ?: 0.0, venus = lon["金星"] ?: 0.0,
                    mars = lon["火星"] ?: 0.0, jupiter = lon["木星"] ?: 0.0,
                    saturn = lon["土星"] ?: 0.0,
                    isDiurnal = hour in 6.0..18.0
                )
                FortuneCard {
                    SectionTitle("阿拉伯点（Lots）")
                    Spacer(Modifier.height(8.dp))
                    ArabicAstrology.arabicParts(c).forEach { part ->
                        Text(
                            "${part.name}：${part.sign}座 ${"%.1f".format(part.degInSign)}°",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                FortuneCard {
                    SectionTitle("Jarbakhtar 周期（129 年循环）")
                    Spacer(Modifier.height(8.dp))
                    ArabicAstrology.jarbakhtar(c, p.birthYear).forEach { per ->
                        Text(
                            "${per.planet}（${per.startYear}-${per.endYear}，${per.years}年）：${per.text}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                FortuneCard {
                    SectionTitle("Tasyir 定向 · 太阳 → 木星")
                    Spacer(Modifier.height(8.dp))
                    val t = ArabicAstrology.tasyir(c, "太阳", "木星")
                    Text("黄道度数差：${"%.2f".format(t.arc)}° → ${"%.2f".format(t.years)} 年", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(t.text, style = MaterialTheme.typography.bodyMedium)
                }
                FortuneCard {
                    SectionTitle("阿拉伯解读")
                    Spacer(Modifier.height(8.dp))
                    Text(ArabicAstrology.buildVerdict(c), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        FortuneCard {
            SectionTitle("Abjad 字母数值（姓名）")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("输入姓名（拉丁或阿拉伯字母）") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            if (name.isNotBlank()) {
                val a = ArabicAstrology.abjad(name)
                Spacer(Modifier.height(8.dp))
                Text("阿拉伯字母：${a.arabic}", style = MaterialTheme.typography.bodyMedium)
                Text("数值总和：${a.total}　数根：${a.digitRoot}　模12：${a.mod12}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(a.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        SystemExplanation("arab")
    }
}
