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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.divination.LawOfAttraction
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle
import com.xuanji.app.ui.components.SystemExplanation

private val DIMS = listOf("目标清晰度", "情绪状态", "信念模式", "行动频率", "感恩习惯")

@Composable
fun LawOfAttractionScreen() {
    var goal by remember { mutableIntStateOf(50) }
    var emotion by remember { mutableIntStateOf(50) }
    var belief by remember { mutableIntStateOf(50) }
    var action by remember { mutableIntStateOf(50) }
    var gratitude by remember { mutableIntStateOf(50) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("吸引力法则", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "思想—情绪—信念—行动—感恩，共同塑造你的现实。为五个维度各打一个 0-100 的分，即可得到综合振动频率与个性化行动指南。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SystemExplanation("loa")

        FortuneCard {
            SectionTitle("五维度自评")
            Spacer(Modifier.height(8.dp))
            DimensionSlider("目标清晰度", goal) { goal = it }
            DimensionSlider("情绪状态", emotion) { emotion = it }
            DimensionSlider("信念模式", belief) { belief = it }
            DimensionSlider("行动频率", action) { action = it }
            DimensionSlider("感恩习惯", gratitude) { gratitude = it }
        }

        val res = LawOfAttraction.evaluate(goal, emotion, belief, action, gratitude)
        FortuneCard {
            SectionTitle("综合振动频率 · ${res.total} / 100")
            Spacer(Modifier.height(8.dp))
            Text("等级 · ${res.level}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(res.interpretation, style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("六维解读")
            Spacer(Modifier.height(8.dp))
            Text(res.verdict, style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("各维度解读")
            Spacer(Modifier.height(8.dp))
            res.advice.forEach { a ->
                Text("• $a", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
            }
        }
        FortuneCard {
            SectionTitle("今日肯定语")
            Spacer(Modifier.height(8.dp))
            Text("「${res.affirmation}」", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        FortuneCard {
            SectionTitle("可视化练习")
            Spacer(Modifier.height(8.dp))
            Text(res.visualizationTip, style = MaterialTheme.typography.bodyMedium)
        }
        FortuneCard {
            SectionTitle("说明")
            Spacer(Modifier.height(8.dp))
            Text(
                "本评估为自我探索工具：肯定语按当天日期确定性选取（每日更换，人人当天结果一致），离线可复现。仅供灵性参考。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DimensionSlider(name: String, value: Int, onChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text("$value", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..100f
        )
        Spacer(Modifier.width(0.dp))
    }
}
