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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.BabylonianAstrology
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun BabylonianAstrologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("巴比伦占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "人类历史上第一个有组织的占星体系。巴比伦人将黄道分为十二「lumaš」，以泥板文献的 System A（步进）与 System B（锯齿）算法推算行星位置，并视行星为神祇向人间传达的讯息。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard { Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            val date = LocalDate.of(p.birthYear, p.birthMonth, p.birthDay)
            val r = BabylonianAstrology.calculate(date)
            FortuneCard {
                SectionTitle("太阳 · 核心命宫")
                Spacer(Modifier.height(8.dp))
                InfoRow("位置", "${r.sun.signName} ${"%.1f".format(r.sun.degreeInSign)}°")
                InfoRow("现代对应", r.sun.modernSign)
                InfoRow("领域", r.sun.domain)
            }
            FortuneCard {
                SectionTitle("月亮 · 情感与直觉")
                Spacer(Modifier.height(8.dp))
                InfoRow("位置", "${r.moon.signName} ${"%.1f".format(r.moon.degreeInSign)}°")
                InfoRow("领域", r.moon.domain)
                InfoRow("黄纬", "${"%.2f".format(r.lunarLatitude)}°")
                Spacer(Modifier.height(4.dp))
                Text(r.lunarLatitudeMeaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FortuneCard {
                SectionTitle("五大行星（${if (r.useSystemA) "System A" else "System B"}）")
                Spacer(Modifier.height(8.dp))
                r.planets.forEach { pl ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.width(72.dp)) {
                            Text(pl.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text(pl.omen, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${pl.signName} ${"%.1f".format(pl.degreeInSign)}°", style = MaterialTheme.typography.bodyMedium)
                            Text(pl.symbol, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            FortuneCard {
                SectionTitle("巴比伦解读")
                Spacer(Modifier.height(8.dp))
                Text(r.verdict, style = MaterialTheme.typography.bodyMedium)
            }
        }
        SystemExplanation("babylon")
    }
}
