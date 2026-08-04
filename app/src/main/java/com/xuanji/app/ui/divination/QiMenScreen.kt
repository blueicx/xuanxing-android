package com.xuanji.app.ui.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xuanji.app.domain.divination.QiMen
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation
import com.xuanji.app.ui.viewmodel.QiMenViewModel
import com.xuanji.app.ui.xuanjiViewModel

@Composable
fun QiMenScreen() {
    val viewModel = xuanjiViewModel { QiMenViewModel() }
    val result by viewModel.result.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("奇门遁甲", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "依当前时间定阴阳遁局与九宫排盘（值符值使、八门九星八神、天盘地盘）。结果仅供文化娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = { viewModel.recast() }, modifier = Modifier.fillMaxWidth()) {
            Text("重新起局（随机抽时辰）")
        }

        result?.let { res ->
            FortuneCard {
                SectionTitle("局式")
                Spacer(Modifier.height(8.dp))
                Text(
                    "${res.yinYang} · ${res.jieqi} · ${res.sanYuan} · ${res.ju}局",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "值符（九星）：${res.zhiFuStar}　值使（八门）：${res.zhiShiDoor}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // 九宫格（洛书盘）：4 9 2 / 3 5 7 / 8 1 6
            FortuneCard {
                SectionTitle("九宫飞盘")
                Spacer(Modifier.height(8.dp))
                val layout = listOf(4, 9, 2, 3, 5, 7, 8, 1, 6)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    layout.chunked(3).forEach { rowGongs ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowGongs.forEach { g ->
                                val cell = res.cells.first { it.gong == g }
                                GongBox(cell)
                            }
                        }
                    }
                }
            }

            FortuneCard {
                SectionTitle("用神提要")
                Spacer(Modifier.height(8.dp))
                val jiMen = res.cells.filter { it.door in listOf("开", "休", "生") }
                Text(
                    "吉门（开/休/生）落宫：${if (jiMen.isEmpty()) "无" else jiMen.joinToString("、") { "${it.trigram}宫(${it.door})" }}。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "值符所在为 ${res.cells.first { it.isZhiFu }.trigram}宫，主事之枢机；值使门为 ${res.zhiShiDoor}。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "（奇门遁甲为传统数术，本盘为离线简化飞布，未做拆补置闰精校，仅供娱乐参考。）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FortuneCard {
                SectionTitle("六维解读")
                Spacer(Modifier.height(8.dp))
                Text(
                    res.verdict,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        SystemExplanation("qimen")
    }
}

@Composable
private fun GongBox(cell: QiMen.GongCell) {
    val highlight = cell.isZhiFu || cell.isZhiShi
    Box(
        Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (highlight) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                1.dp,
                if (cell.isZhiFu) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${cell.gong}宫", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(cell.trigram, style = MaterialTheme.typography.labelMedium)
            }
            Text("门 ${cell.door}", style = MaterialTheme.typography.bodySmall)
            Text("星 ${cell.star}", style = MaterialTheme.typography.bodySmall)
            cell.god?.let { Text("神 $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
            Text("天 ${cell.tianPan} / 地 ${cell.diPan}", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
            if (cell.isZhiFu) Text("值符", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            if (cell.isZhiShi) Text("值使", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
