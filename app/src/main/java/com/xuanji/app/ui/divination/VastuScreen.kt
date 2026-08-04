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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.Vastu
import com.xuanji.app.domain.divination.VastuAnalysis
import com.xuanji.app.domain.divination.VastuRoom
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

/** 预置房间清单：名称 + 房间（40×60 地块，西南角为原点） */
private val PRESET_ROOMS = listOf(
    "厨房" to VastuRoom("厨房", 28f, 2f, 10f, 12f),
    "主卧室" to VastuRoom("主卧室", 2f, 40f, 14f, 16f),
    "客厅" to VastuRoom("客厅", 26f, 44f, 12f, 14f),
    "祈祷室" to VastuRoom("祈祷室", 30f, 48f, 8f, 8f),
    "浴室" to VastuRoom("浴室", 2f, 2f, 8f, 8f),
    "卧室" to VastuRoom("卧室", 2f, 12f, 12f, 14f)
)

@Composable
fun VastuScreen() {
    var plotW by rememberSaveable { mutableStateOf("40") }
    var plotD by rememberSaveable { mutableStateOf("60") }
    var facing by rememberSaveable { mutableStateOf("北") }
    val enabled = remember { mutableStateListOf<Boolean>().apply { repeat(PRESET_ROOMS.size) { add(true) } } }

    // —— 测量辅助：英尺↔米 换算 ——
    var ftInput by rememberSaveable { mutableStateOf("") }
    var mInput by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("瓦斯图", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "瓦斯图（Vastu Shastra）是印度古老建筑科学，以 Vastu Purusha Mandala 网格与五大元素协调空间能量。选择地块尺寸与朝向，勾选示例房间，即可获得合规评分与改进建议。仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // —— 测量辅助工具 ——
        FortuneCard {
            SectionTitle("测量辅助工具")
            Spacer(Modifier.height(4.dp))
            Text(
                "瓦斯图分析需要您先量出地块与房间的尺寸（英尺）与朝向，下方工具可辅助换算与判断。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            // 英尺↔米换算
            Text("英尺 ↔ 米 换算", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ftInput, onValueChange = { ftInput = it; mInput = it.toFloatOrNull()?.let { v -> "%.2f".format(v * 0.3048) } ?: "" },
                    label = { Text("英尺 ft") }, modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = mInput, onValueChange = { mInput = it; ftInput = it.toFloatOrNull()?.let { v -> "%.2f".format(v / 0.3048) } ?: "" },
                    label = { Text("米 m") }, modifier = Modifier.weight(1f), singleLine = true
                )
            }
            Spacer(Modifier.height(10.dp))
            // 推荐尺寸
            Text("常见吉利尺寸参考", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            listOf(
                "卧室" to "10–12 ft（约 3.0–3.7 m）",
                "客厅" to "12–14 ft（约 3.7–4.3 m）",
                "厨房" to "8–10 ft（约 2.4–3.0 m）",
                "主入口门" to "3.5–4 ft（约 1.1–1.2 m）"
            ).forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(k, Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(v, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            // 朝向判断
            Text("如何判断地块朝向", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(
                "面向地块主要入口站立：手机内置指南针指向 N 即为北向；也可在日出时面对太阳（东方）作为参照。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FortuneCard {
            SectionTitle("地块与朝向")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = plotW, onValueChange = { plotW = it },
                    label = { Text("宽 (ft)") }, modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = plotD, onValueChange = { plotD = it },
                    label = { Text("深 (ft)") }, modifier = Modifier.weight(1f), singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("东", "南", "西", "北").forEach { f ->
                    FilterChip(selected = facing == f, onClick = { facing = f }, label = { Text(f) })
                }
            }
        }

        FortuneCard {
            SectionTitle("房间布局（示例 40×60 宅，可增减）")
            Spacer(Modifier.height(8.dp))
            PRESET_ROOMS.forEachIndexed { i, (roomLabel, _) ->
                FilterChip(
                    selected = enabled[i],
                    onClick = { enabled[i] = !enabled[i] },
                    label = { Text(roomLabel) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { enabled.indices.forEach { enabled[it] = true } }) { Text("全选") }
                OutlinedButton(onClick = { enabled.indices.forEach { enabled[it] = false } }) { Text("清空") }
            }
        }

        val w = plotW.toFloatOrNull() ?: 40f
        val d = plotD.toFloatOrNull() ?: 60f
        val rooms = PRESET_ROOMS.map { it.second }.filterIndexed { i, _ -> enabled[i] }
        if (rooms.isNotEmpty()) {
            val analysis = Vastu.analyze(w, d, facing, rooms)
            AnalysisCard(analysis)
        } else {
            FortuneCard {
                Text("请至少选择一个房间进行分析。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        FortuneCard {
            SectionTitle("八方向 · 元素 · 理想功能")
            Spacer(Modifier.height(8.dp))
            Vastu.directionTable().forEach { (dName, elem, func) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.width(110.dp)) {
                        Text(dName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        Text(elem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(func, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SystemExplanation("vastu")
    }
}

@Composable
private fun AnalysisCard(a: VastuAnalysis) {
    FortuneCard {
        SectionTitle("分析结果 · ${a.score}/100")
        Spacer(Modifier.height(8.dp))
        Text("地块：${a.plotWidth}ft × ${a.plotDepth}ft　朝向：${a.facing}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(a.grade, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))

        a.roomResults.forEach { r ->
            val status = when (r.compliant) {
                true -> "✅"
                false -> "❌"
                null -> "⚪"
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text("$status ${r.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(90.dp))
                Text("方向 ${r.direction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(90.dp))
                Text(r.advice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("【详细解读】", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        Text(a.interpretation, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(10.dp))
        Text("【改进建议】", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(4.dp))
        a.recommendations.forEach { r ->
            Text(r, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
