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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.HellenisticAstrology
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HellenisticAstrologyScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("希腊占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "希腊占星是西方占星学的源头，以整宫制排盘，讲究区段（Sect）、幸运点与法达星盘。年主星推运（Annual Profections）每年将上升点推进一宫，揭示当年的核心议题。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val p = profile
        if (p == null) {
            FortuneCard { Text("尚未设置出生信息，请先到「我的」填写。", style = MaterialTheme.typography.bodyMedium) }
        } else {
            val birth = LocalDate.of(p.birthYear, p.birthMonth, p.birthDay)
            val hour = p.birthHour + p.birthMinute / 60.0
            val c = HellenisticAstrology.chart(birth, hour)
            val age = ChronoUnit.DAYS.between(birth, LocalDate.now()) / 365.2422

            FortuneCard {
                SectionTitle("本命盘（整宫制）")
                Spacer(Modifier.height(8.dp))
                InfoRow("区段 Sect", if (c.isDiurnal) "日生盘 (Diurnal)" else "夜生盘 (Nocturnal)")
                InfoRow("太阳", "${c.sunSign} ${"%.1f".format(c.sunDeg)}°")
                InfoRow("月亮", "${c.moonSign} ${"%.1f".format(c.moonDeg)}°")
                InfoRow("上升", "${c.ascSign} ${"%.1f".format(c.ascDeg)}°")
                InfoRow("幸运点", "${c.lotFortuneSign} ${"%.1f".format(c.lotFortuneDeg)}°")
                InfoRow("精神点", "${c.lotSpiritSign} ${"%.1f".format(c.lotSpiritDeg)}°")
                Spacer(Modifier.height(8.dp))
                Text(c.verdict, style = MaterialTheme.typography.bodyMedium)
            }

            val prof = HellenisticAstrology.profection(c, age.toInt())
            FortuneCard {
                SectionTitle("年主星推运 · ${age.toInt()} 岁")
                Spacer(Modifier.height(8.dp))
                InfoRow("推进上升", prof.sign)
                InfoRow("年主星", prof.lord)
                InfoRow("重点领域", prof.area)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${prof.age} 岁这一年的核心议题围绕「${prof.area}」展开，由 ${prof.lord} 主导。建议关注该领域的发展与挑战。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val current = HellenisticAstrology.currentFirdaria(c, birth, age)
            FortuneCard {
                SectionTitle("法达星盘（Firdaria）")
                Spacer(Modifier.height(8.dp))
                if (current != null) {
                    InfoRow("当前主星", "${current.planet}（${current.startAge}-${current.endAge} 岁）")
                    Text(current.meaning, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(8.dp))
                HellenisticAstrology.firdaria(c).forEach { f ->
                    Text(
                        "${f.planet}: ${f.startAge}-${f.endAge} 岁（${f.years} 年）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
        SystemExplanation("greek")
    }
}
