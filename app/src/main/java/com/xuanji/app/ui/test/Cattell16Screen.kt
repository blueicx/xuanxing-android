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
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.test.Cattell16PF
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

private val CATELL_OPTIONS = listOf(0 to "A · 是", 1 to "B · 介于", 2 to "C · 否")
private val FACTOR_NAMES = Cattell16PF.FACTORS.associate { it }

@Composable
fun Cattell16Screen() {
    var index by remember { mutableIntStateOf(0) }   // 0=介绍, 1..34=答题, 35=结果
    val answers = remember { mutableStateListOf<Int>() }
    var pick by remember { mutableIntStateOf(-1) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            index == 0 -> IntroCattell(onStart = { index = 1; answers.clear() })
            index in 1..Cattell16PF.QUESTIONS.size -> {
                val q = Cattell16PF.QUESTIONS[index - 1]
                LinearProgressIndicator(
                    progress = { index.toFloat() / Cattell16PF.QUESTIONS.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("第 $index / ${Cattell16PF.QUESTIONS.size} 题", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FortuneCard {
                    Text(q.text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    if (q.factor != null) {
                        Text(
                            "因素：${FACTOR_NAMES[q.factor]}（${q.factor}）" + (if (q.reverse) "（反向计分）" else ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    CATELL_OPTIONS.forEach { (v, label) ->
                        val selected = pick == v
                        val border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        OutlinedButton(onClick = { pick = v }, modifier = Modifier.fillMaxWidth(), border = border) {
                            Text(label)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (index > 1) {
                        OutlinedButton(onClick = {
                            index--
                            pick = if (answers.isNotEmpty()) answers.removeAt(answers.size - 1) else -1
                        }, modifier = Modifier.weight(1f)) { Text("上一题") }
                    }
                    Button(onClick = {
                        if (pick == -1) return@Button
                        answers.add(pick)
                        pick = -1
                        index++
                    }, modifier = Modifier.weight(1f)) {
                        Text(if (index == Cattell16PF.QUESTIONS.size) "查看结果" else "下一题")
                    }
                }
            }
            else -> CattellResultPage(answers, onRestart = { index = 0; answers.clear(); pick = -1 })
        }
    }
}

@Composable
private fun IntroCattell(onStart: () -> Unit) {
    FortuneCard {
        SectionTitle("关于卡特尔 16PF")
        Spacer(Modifier.height(8.dp))
        Text(
            "卡特尔 16PF 从 16 个独立的人格因素（乐群性、聪慧性、稳定性、恃强性、活泼性、有恒性、敢为性、敏感性、怀疑性、幻想性、世故性、忧虑性、实验性、独立性、自律性、紧张性）全面描述人格特质。",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "本测试共 34 题，每题三个选项：A=是、B=介于、C=否。请凭第一反应作答，部分题目为反向计分。全程约 6 分钟。结果仅供自我探索参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("开始测试（34 题）") }
    }
}

@Composable
private fun CattellResultPage(answers: List<Int>, onRestart: () -> Unit) {
    if (answers.size < Cattell16PF.QUESTIONS.size) {
        Text("答题未完成，请返回继续。", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val result = Cattell16PF.calculate(answers)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val f = result.factors.first()
        TestRecordRecorder.save("卡特尔 16PF", "职业", "16PF·${f.name}(${f.level})", result.portrait.take(20), "16PF|${result.factors.joinToString { "${it.code}${it.level}" }}")
    }

    FortuneCard {
        SectionTitle("16PF 因素得分报告")
        Spacer(Modifier.height(8.dp))
        result.factors.forEach { f ->
            Text("${f.name}（${f.code}）· ${f.level} · ${f.score}/10", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary)
            LinearProgressIndicator(progress = { f.score / 10f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
    }

    result.factors.forEach { f ->
        FortuneCard {
            SectionTitle("${f.name}（${f.code}）· ${f.level}")
            Spacer(Modifier.height(6.dp))
            Text(f.interpretation, style = MaterialTheme.typography.bodyMedium)
        }
    }

    FortuneCard {
        SectionTitle("综合人格画像")
        Spacer(Modifier.height(8.dp))
        Text(result.portrait, style = MaterialTheme.typography.bodyMedium)
    }
    FortuneCard {
        SectionTitle("📌 关于 16PF")
        Spacer(Modifier.height(6.dp))
        Text(
            "卡特尔 16PF 由美国心理学家雷蒙德·卡特尔提出，通过因素分析识别出 16 种根源特质。本简化版为通识改编，仅供参考，不能替代专业心理评估。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("重新测试") }
}
