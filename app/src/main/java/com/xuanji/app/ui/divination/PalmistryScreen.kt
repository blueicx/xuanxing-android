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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.DEFAULT_FEATURES
import com.xuanji.app.domain.divination.FEATURE_LABELS
import com.xuanji.app.domain.divination.FEATURE_OPTIONS
import com.xuanji.app.domain.divination.Palmistry
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.PalmSchematic
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

/** 特征 key → 示意图高亮标签（与 PalmSchematic 的标签一致） */
private val PALM_HIGHLIGHT = mapOf(
    "hand_shape" to "掌形",
    "life_line" to "生命线",
    "head_line" to "智慧线",
    "heart_line" to "感情线",
    "fate_line" to "命运线"
)

@Composable
fun PalmistryScreen() {
    val selected = remember {
        mutableStateMapOf<String, String>().apply { putAll(DEFAULT_FEATURES) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("欧洲手相学", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "根据手掌形状、手指比例、拇指大小与主要掌纹（生命线、智慧线、感情线、命运线等）推断性格与人生倾向。点击选项即显示该特征解读，掌图对应位置会高亮。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 掌图（高亮当前点击特征）
        FortuneCard {
            SectionTitle("掌纹参考图")
            Spacer(Modifier.height(8.dp))
            PalmSchematic(
                labelColor = MaterialTheme.colorScheme.primary,
                highlight = "掌形"
            )
        }

        // 逐项特征选择 + 内联解读
        FortuneCard {
            SectionTitle("逐项特征 · 点击查看解读")
            Spacer(Modifier.height(8.dp))
            FEATURE_LABELS.keys.forEach { key ->
                val label = FEATURE_LABELS[key] ?: key
                val chosen = selected[key] ?: DEFAULT_FEATURES[key] ?: ""
                Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
                val options = FEATURE_OPTIONS[key] ?: emptyList()
                options.forEach { option ->
                    FilterChip(
                        selected = chosen == option,
                        onClick = { selected[key] = option },
                        label = { Text(option) },
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                }
                val interp = Palmistry.interpretFeature(key, chosen)
                if (interp.isNotEmpty()) {
                    Text(
                        "· $interp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }
        }

        val report = Palmistry.generate(selected.toMap())
        FortuneCard {
            SectionTitle("综合解读")
            Spacer(Modifier.height(8.dp))
            Text(report.summary, style = MaterialTheme.typography.bodyMedium)
        }
        SystemExplanation("palm")
    }
}
