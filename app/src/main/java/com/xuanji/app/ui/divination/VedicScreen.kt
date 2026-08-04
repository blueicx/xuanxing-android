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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.di.AppModule
import com.xuanji.app.domain.divination.Vedic
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.InfoRow
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun VedicScreen() {
    val profile by AppModule.repository.userProfileFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("印度占星", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        val p = profile
        if (p == null) {
            Text("尚未设置出生信息，请先到「我的」填写。")
        } else {
            val res = Vedic.calculate(p)
            FortuneCard {
                SectionTitle("月亮星座（Rashi）")
                Spacer(Modifier.height(8.dp))
                InfoRow("Rashi", res.rashi.name)
                InfoRow("梵名", res.rashi.sanskrit)
                Spacer(Modifier.height(6.dp))
                Text(res.rashi.trait, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FortuneCard {
                SectionTitle("二十七宿（Nakshatra）")
                Spacer(Modifier.height(8.dp))
                InfoRow("宿序", "${res.nakshatraIndex + 1} / 27")
                InfoRow("宿名", "${res.nakshatraCn}（${res.nakshatra}）")
            }

            val vim = Vedic.vimshottari(p)
            FortuneCard {
                SectionTitle("Vimshottari 大运系统")
                Spacer(Modifier.height(8.dp))
                Text(
                    "黄道 360° 均分 27 宿（每宿 13°20′），每宿由九星之一固定主宰；首大限剩余年数 = 宿主宰星总年限 ×（1 − 宿内已过比例），其后按九星循环依次切换。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                InfoRow("月亮恒星黄经", "%.2f°".format(vim.moonLongitude))
                InfoRow("出生宿", "${vim.nakshatraCn}（${vim.nakshatraName}）")
                InfoRow("宿主星", vim.nakshatraLord.cn)
                InfoRow("宿内已过比例", "%.1f%%".format(vim.fracInNakshatra * 100))
                InfoRow("首大限剩余年数", "%.2f 年".format(vim.firstRemainingYears))
            }

            FortuneCard {
                SectionTitle("综合解读")
                Spacer(Modifier.height(8.dp))
                Text(
                    Vedic.interpretation(p),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            FortuneCard {
                SectionTitle("大限 · 子限序列")
                Spacer(Modifier.height(8.dp))
                if (vim.activeIndex >= 0) {
                    Text(
                        "当前处于第 ${vim.activeIndex + 1} 大限（${vim.mahadashas[vim.activeIndex].graha.cn}）。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                }
                vim.mahadashas.forEachIndexed { idx, md ->
                    DashaCard(md, idx == vim.activeIndex)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Text(
                "吠陀占星以月亮所在宿（Nakshatra）与星座（Rashi）为核心。月亮位置采用平黄经 + 中心差近似，并以 Lahiri 岁差转恒星系，宿边界误差约 1~2 宿，仅供娱乐参考。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SystemExplanation("vedic")
    }
}

@Composable
private fun DashaCard(md: Vedic.Mahadasha, active: Boolean) {
    val container = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val border = if (active) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    else null
    Card(
        Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = border
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${md.index} 大限 · ${md.graha.cn}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${"%.1f".format(md.startAge)}–${"%.1f".format(md.endAge)} 岁",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (md.isFirst) {
                Text(
                    "（首限，按出生宿内比例截断）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("子限：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                md.antardashas.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            a.graha.cn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(150.dp)
                        )
                        Text(
                            "${"%.2f".format(a.years)} 年",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${"%.1f".format(a.startAge)}–${"%.1f".format(a.endAge)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
