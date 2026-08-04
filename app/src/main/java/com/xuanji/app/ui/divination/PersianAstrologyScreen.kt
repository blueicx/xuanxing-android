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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.PersianAstrology
import com.xuanji.app.domain.divination.PersianChartData
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate

@Composable
fun PersianAstrologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("波斯占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "波斯占星融合巴比伦、希腊传统，核心技法包括 Jarbakhtar 行星周期（129 年循环）、法达星盘 Firdaria（120 年时间主星）与 Tasyir 定向（1°≈1 年）。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard { Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            val chart = AppModule.repository.natalChartFlow.value
            if (chart == null) {
                FortuneCard { Text("正在计算命盘…", style = MaterialTheme.typography.bodyMedium) }
            } else {
                val lon = chart.planets.associate { it.name to it.longitude }
                val hour = p.birthHour + p.birthMinute / 60.0
                val c = PersianChartData(
                    ascendant = chart.ascendant,
                    sun = lon["太阳"] ?: 0.0, moon = lon["月亮"] ?: 0.0,
                    mercury = lon["水星"] ?: 0.0, venus = lon["金星"] ?: 0.0,
                    mars = lon["火星"] ?: 0.0, jupiter = lon["木星"] ?: 0.0,
                    saturn = lon["土星"] ?: 0.0,
                    isDiurnal = hour in 6.0..18.0
                )
                val birth = LocalDate.of(p.birthYear, p.birthMonth, p.birthDay)
                val fd = PersianAstrology.firdariaCurrent(c, birth)
                FortuneCard {
                    SectionTitle("法达星盘 · 当前周期")
                    Spacer(Modifier.height(8.dp))
                    InfoRow("当前年龄", "${"%.1f".format(fd.age)} 岁")
                    InfoRow("当前主星", "${fd.planet}（剩余 ${"%.1f".format(fd.remaining)} 年）")
                    InfoRow("吉凶", fd.omen)
                    Spacer(Modifier.height(6.dp))
                    Text(fd.text, style = MaterialTheme.typography.bodyMedium)
                }
                FortuneCard {
                    SectionTitle("法达完整 120 年周期")
                    Spacer(Modifier.height(8.dp))
                    PersianAstrology.firdariaFull().forEach { per ->
                        Text(
                            "${per.planet}: ${per.startAge}-${per.endAge} 岁（${per.years} 年 · ${per.omen}）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                FortuneCard {
                    SectionTitle("Jarbakhtar 周期（129 年）")
                    Spacer(Modifier.height(8.dp))
                    PersianAstrology.jarbakhtar(c).forEach { per ->
                        Text(
                            "${per.planet}（${per.startAge}-${per.endAge} 岁 · ${per.omen}）",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            per.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
                FortuneCard {
                    SectionTitle("Tasyir 定向 · 太阳 → 木星")
                    Spacer(Modifier.height(8.dp))
                    val (arc, _, text) = PersianAstrology.tasyir(c, "太阳", "木星")
                    Text("黄道度数差：${"%.2f".format(arc)}°", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
                FortuneCard {
                    SectionTitle("波斯解读")
                    Spacer(Modifier.height(8.dp))
                    Text(PersianAstrology.buildVerdict(c, birth), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        SystemExplanation("persia")
    }
}
