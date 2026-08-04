package com.xuanji.app.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.test.BigFive
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

private val SCALE = listOf(1 to "非常不符合", 2 to "不符合", 3 to "中立", 4 to "符合", 5 to "非常符合")

@Composable
fun BigFiveScreen() {
    var index by remember { mutableIntStateOf(0) }   // 0=介绍, 1..50=答题, 51=结果
    val answers = remember { mutableStateListOf<Int>() }
    var pick by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            index == 0 -> IntroBigFive(onStart = { index = 1; answers.clear() })
            index in 1..BigFive.QUESTIONS.size -> {
                val q = BigFive.QUESTIONS[index - 1]
                LinearProgressIndicator(
                    progress = { index.toFloat() / BigFive.QUESTIONS.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("第 $index / ${BigFive.QUESTIONS.size} 题", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FortuneCard {
                    Text(q.text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "维度：${BigFive.DIMENSION_NAMES[q.dimension]}" + (if (q.reverse) "（反向计分）" else ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(12.dp))
                    SCALE.forEach { (v, label) ->
                        val selected = pick == v
                        val border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        OutlinedButton(onClick = { pick = v }, modifier = Modifier.fillMaxWidth(), border = border) {
                            Text("$v · $label")
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (index > 1) {
                        OutlinedButton(onClick = {
                            index--
                            pick = if (answers.isNotEmpty()) answers.removeAt(answers.size - 1) else 0
                        }, modifier = Modifier.weight(1f)) { Text("上一题") }
                    }
                    Button(onClick = {
                        if (pick == 0) return@Button
                        answers.add(pick)
                        pick = 0
                        index++
                    }, modifier = Modifier.weight(1f)) {
                        Text(if (index == BigFive.QUESTIONS.size) "查看结果" else "下一题")
                    }
                }
            }
            else -> BigFiveResultPage(answers, onRestart = { index = 0; answers.clear(); pick = 0 })
        }
    }
}

@Composable
private fun IntroBigFive(onStart: () -> Unit) {
    FortuneCard {
        SectionTitle("关于大五人格")
        Spacer(Modifier.height(8.dp))
        Text(
            "大五人格（Big Five / OCEAN）是心理学界公认的人格分类系统，从开放性、尽责性、外向性、宜人性、神经质五个维度全面描述人格。本测试共 50 题，每题按 1-5 自评，请凭第一反应诚实作答。",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "计分规则：1=非常不符合、2=不符合、3=中立、4=符合、5=非常符合。部分题目为反向计分。全程约 8 分钟。结果仅供自我探索参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("开始测试（50 题）") }
    }
}

@Composable
private fun BigFiveResultPage(answers: List<Int>, onRestart: () -> Unit) {
    if (answers.size < BigFive.QUESTIONS.size) {
        Text("答题未完成，请返回继续。", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val result = BigFive.calculate(answers)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val top = result.dimensions.first()
        TestRecordRecorder.save("大五人格测试", "性格", "大五·${top.name}(${top.level})", result.portrait.take(20), "BigFive|${result.dimensions.joinToString { "${it.code}${it.level}" }}")
    }

    FortuneCard {
        SectionTitle("大五人格结果报告")
        Spacer(Modifier.height(8.dp))
        // 各维度得分条
        result.dimensions.forEach { d ->
            Text("${d.name}（${d.code}）· ${d.level} · ${d.score}/50", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary)
            LinearProgressIndicator(progress = { d.score / 50f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
    }

    // 各维度超详细解读
    result.dimensions.forEach { d ->
        val i = d.interpretation
        FortuneCard {
            SectionTitle("${d.name} · ${i.title}")
            Spacer(Modifier.height(6.dp))
            Text(d.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("特质：${i.traits}", style = MaterialTheme.typography.bodyMedium)
            Text("优势：${i.strengths}", style = MaterialTheme.typography.bodyMedium)
            Text("劣势：${i.weaknesses}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("职场建议：${i.work}", style = MaterialTheme.typography.bodyMedium)
            Text("人际关系：${i.relationship}", style = MaterialTheme.typography.bodyMedium)
            Text("成长建议：${i.growth}", style = MaterialTheme.typography.bodyMedium)
            Text("名人例子：${i.famous}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    FortuneCard {
        SectionTitle("综合个性画像")
        Spacer(Modifier.height(8.dp))
        Text(result.portrait, style = MaterialTheme.typography.bodyMedium)
    }
    FortuneCard {
        SectionTitle("📌 关于大五人格")
        Spacer(Modifier.height(6.dp))
        Text(
            "大五人格模型（OCEAN）从五个核心维度描述人格特质，这些特质在不同文化中普遍存在。本测试仅供参考，不能替代专业心理评估。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("重新测试") }
}
