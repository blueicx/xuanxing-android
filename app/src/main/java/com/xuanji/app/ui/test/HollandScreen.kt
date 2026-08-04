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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.test.Holland
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

@Composable
fun HollandScreen() {
    var index by remember { mutableIntStateOf(0) }   // 0=介绍, 1..60=答题, 61=结果
    val answers = remember { mutableStateListOf<Boolean>() }
    var pick by remember { mutableStateOf<Boolean?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            index == 0 -> IntroHolland(onStart = { index = 1; answers.clear() })
            index in 1..Holland.QUESTIONS.size -> {
                val q = Holland.QUESTIONS[index - 1]
                LinearProgressIndicator(
                    progress = { index.toFloat() / Holland.QUESTIONS.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("第 $index / ${Holland.QUESTIONS.size} 题", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FortuneCard {
                    Text(q.text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "类型：${Holland.TYPE_NAMES[q.type]}（${q.type}）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val yesSel = pick == true
                        val yesBorder = if (yesSel) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        OutlinedButton(onClick = { pick = true }, modifier = Modifier.weight(1f), border = yesBorder) {
                            Text("是")
                        }
                        val noSel = pick == false
                        val noBorder = if (noSel) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        OutlinedButton(onClick = { pick = false }, modifier = Modifier.weight(1f), border = noBorder) {
                            Text("否")
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (index > 1) {
                        OutlinedButton(onClick = {
                            index--
                            pick = if (answers.isNotEmpty()) answers.removeAt(answers.size - 1) else null
                        }, modifier = Modifier.weight(1f)) { Text("上一题") }
                    }
                    Button(onClick = {
                        val p = pick ?: return@Button
                        answers.add(p)
                        pick = null
                        index++
                    }, modifier = Modifier.weight(1f)) {
                        Text(if (index == Holland.QUESTIONS.size) "查看结果" else "下一题")
                    }
                }
            }
            else -> HollandResultPage(answers, onRestart = { index = 0; answers.clear(); pick = null })
        }
    }
}

@Composable
private fun IntroHolland(onStart: () -> Unit) {
    FortuneCard {
        SectionTitle("关于霍兰德职业兴趣测试")
        Spacer(Modifier.height(8.dp))
        Text(
            "霍兰德职业兴趣理论由美国心理学家约翰·霍兰德提出，将职业兴趣分为现实型（R）、研究型（I）、艺术型（A）、社会型（S）、企业型（E）、常规型（C）六种类型，并取分最高的三个类型组成「三码」，描绘你的职业兴趣结构。",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "本测试共 60 题，每题作答「是」或「否」。请凭第一反应作答，没有对错之分。全程约 5 分钟。结果仅供自我探索参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("开始测试（60 题）") }
    }
}

@Composable
private fun HollandResultPage(answers: List<Boolean>, onRestart: () -> Unit) {
    if (answers.size < Holland.QUESTIONS.size) {
        Text("答题未完成，请返回继续。", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val result = Holland.calculate(answers)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        TestRecordRecorder.save("霍兰德职业兴趣", "职业", "三码 ${result.code}", result.codeAdvice.take(20), "Holland|${result.code}")
    }

    FortuneCard {
        SectionTitle("六类型得分报告")
        Spacer(Modifier.height(8.dp))
        result.types.forEach { t ->
            Text(
                "${t.name}（${t.code}）· ${t.level} · ${t.score}/10",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            LinearProgressIndicator(progress = { t.score / 10f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
    }

    FortuneCard {
        SectionTitle("您的霍兰德三码")
        Spacer(Modifier.height(8.dp))
        Text(result.code, style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        val topNames = result.code.map { c -> "${Holland.TYPE_NAMES[c.toString()]!!}（${c}）" }.joinToString("、")
        Text("由得分最高的三个类型组成：$topNames", style = MaterialTheme.typography.bodyMedium)
    }

    FortuneCard {
        SectionTitle("三码综合建议")
        Spacer(Modifier.height(6.dp))
        Text(result.codeAdvice, style = MaterialTheme.typography.bodyMedium)
    }

    result.types.forEach { t ->
        FortuneCard {
            SectionTitle("${t.name}（${t.code}）· ${t.level} · ${t.score}/10")
            Spacer(Modifier.height(6.dp))
            Text(t.interpretation, style = MaterialTheme.typography.bodyMedium)
        }
    }

    FortuneCard {
        SectionTitle("综合职业画像")
        Spacer(Modifier.height(6.dp))
        Text(result.portrait, style = MaterialTheme.typography.bodyMedium)
    }
    FortuneCard {
        SectionTitle("📌 关于霍兰德")
        Spacer(Modifier.height(6.dp))
        Text(
            "霍兰德职业兴趣理论是职业规划中常用的工具，本简化版为通识改编。兴趣偏好不代表能力高低，职业选择还应结合能力、性格与现实条件综合考量。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("重新测试") }
}
