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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.Physiognomy
import com.xuanji.app.ui.components.FaceSchematic
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

@Composable
fun PhysiognomyScreen() {
    val selections = remember {
        mutableStateMapOf<String, String>().apply {
            Physiognomy.FEATURES.forEach { put(it.label, it.options[0]) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("相术（面相）", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "为面部五个特征各选一项，点击选项即会显示该特征的解读（图上面部相应区域会高亮）。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SystemExplanation("physiognomy")

        // 特征选择 + 内联解读 + 联动示意图
        FortuneCard {
            SectionTitle("面部特征 · 逐项解读")
            Spacer(Modifier.height(8.dp))
            // 示意图（高亮当前点击的部位）
            FaceSchematic(
                labelColor = MaterialTheme.colorScheme.primary,
                highlight = selections.keys.firstOrNull { selections[it] != "" } ?: "额头"
            )
            Spacer(Modifier.height(12.dp))
            Physiognomy.FEATURES.forEach { feature ->
                val chosen = selections[feature.label] ?: feature.options[0]
                Text(feature.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                feature.options.forEach { opt ->
                    FilterChip(
                        selected = chosen == opt,
                        onClick = { selections[feature.label] = opt },
                        label = { Text(opt) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                    )
                }
                // 选中项的即时解读
                Text(
                    "· ${Physiognomy.interpretFeature(feature.label, chosen)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
            }
        }

        // 综合解读
        val res = Physiognomy.analyze(
            selections["额头"] ?: "饱满",
            selections["眼睛"] ?: "大而有神",
            selections["鼻子"] ?: "高挺",
            selections["嘴巴"] ?: "适中有棱",
            selections["下巴"] ?: "圆润"
        )
        FortuneCard {
            SectionTitle("综合性格总结")
            Spacer(Modifier.height(8.dp))
            Text(res.summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
