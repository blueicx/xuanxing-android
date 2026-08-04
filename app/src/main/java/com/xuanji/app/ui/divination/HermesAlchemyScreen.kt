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
import com.xuanji.app.domain.divination.ALCHEMY_OPERATIONS
import com.xuanji.app.domain.divination.ELEMENTS
import com.xuanji.app.domain.divination.HermesAlchemy
import com.xuanji.app.domain.divination.PATHS
import com.xuanji.app.domain.divination.HERMES_SEFIROT
import com.xuanji.app.domain.divination.TRIA_PRIMA
import com.xuanji.app.domain.divination.UNIVERSAL_LAWS
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun HermesAlchemyScreen() {
    val chart by AppModule.repository.natalChartFlow.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("赫尔墨斯 · 炼金术", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "融合赫尔墨斯七项宇宙法则、占星点（Lots）、炼金术符号（四元素/三原质/七步操作）与卡巴拉生命之树，是西方秘学传统的综合体系。本页基于确定性算法离线计算，仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FortuneCard {
            SectionTitle("七项宇宙法则")
            Spacer(Modifier.height(8.dp))
            UNIVERSAL_LAWS.forEach { law ->
                Text(
                    "✦ ${law.name} — ${law.core}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    law.interpretation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        val c = chart
        if (c != null) {
            val lon = c.planets.associate { it.name to it.longitude }
            val lots = HermesAlchemy.lots(
                asc = c.ascendant, sun = lon["太阳"] ?: 0.0, moon = lon["月亮"] ?: 0.0,
                isDiurnal = true, planets = lon
            )
            FortuneCard {
                SectionTitle("赫尔墨斯占星点（Lots）")
                Spacer(Modifier.height(8.dp))
                lots.forEach { lot ->
                    Text("• ${lot.name}: ${lot.degree}°", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        lot.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    HermesAlchemy.guidance(lots.firstOrNull()?.sign ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            FortuneCard { Text("正在计算命盘…", style = MaterialTheme.typography.bodyMedium) }
        }

        FortuneCard {
            SectionTitle("炼金术符号系统")
            Spacer(Modifier.height(8.dp))
            Text("四大元素：", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            ELEMENTS.forEach { (n, e) ->
                Text("${e.symbol} $n: ${e.trait}（对应${e.correspond}）", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("三大原质：", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            TRIA_PRIMA.forEach { (n, t) ->
                Text("${t.symbol} $n: ${t.trait}（对应${t.correspond}）", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("七步操作：", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            ALCHEMY_OPERATIONS.forEach { op ->
                Text("${op.symbol} ${op.name} — ${op.purpose}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
            }
        }

        FortuneCard {
            SectionTitle("生命之树（Sefirot）")
            Spacer(Modifier.height(8.dp))
            HERMES_SEFIROT.forEachIndexed { i, s ->
                Text(
                    "${i + 1}. ${s.name}（${s.symbol}）　${s.planet}　含义：${s.meaning}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("22 路径（塔罗）：", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            PATHS.forEach { p ->
                Text("${p.hebrewLetter} — ${p.tarot}（${p.meaning}）", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
        SystemExplanation("hermes")
    }
}
