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
import com.xuanji.app.domain.test.Mmpi
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

@Composable
fun MmpiScreen() {
    var index by remember { mutableIntStateOf(0) }   // 0=介绍, 1..20=答题, 21=结果
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
            index == 0 -> IntroMmpi(onStart = { index = 1; answers.clear() })
            index in 1..Mmpi.QUESTIONS.size -> {
                val q = Mmpi.QUESTIONS[index - 1]
                LinearProgressIndicator(
                    progress = { index.toFloat() / Mmpi.QUESTIONS.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("第 $index / ${Mmpi.QUESTIONS.size} 题", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FortuneCard {
                    Text(q.text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    if (q.scale != null) {
                        Text(
                            "量表：${Mmpi.SCALE_NAMES[q.scale]}（${q.scale}）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
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
                        Text(if (index == Mmpi.QUESTIONS.size) "查看结果" else "下一题")
                    }
                }
            }
            else -> MmpiResultPage(answers, onRestart = { index = 0; answers.clear(); pick = null })
        }
    }
}

@Composable
private fun IntroMmpi(onStart: () -> Unit) {
    FortuneCard {
        SectionTitle("关于 MMPI")
        Spacer(Modifier.height(8.dp))
        Text(
            "本页是受 MMPI 量表概念启发的 20 题自编风格问卷，使用中性维度观察作答倾向；它不是 MMPI/MMPI-3，不能评估心理健康或替代专业测验。每题作答「是」或「否」。",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "请根据自己最近的真实感受作答，没有对错之分。部分题目可作反向计分。全程约 3 分钟。本结果仅供自我探索参考，不构成任何医学诊断。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("开始测试（20 题）") }
    }
}

@Composable
private fun MmpiResultPage(answers: List<Boolean>, onRestart: () -> Unit) {
    if (answers.size < Mmpi.QUESTIONS.size) {
        Text("答题未完成，请返回继续。", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val result = Mmpi.calculate(answers)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val top = result.scales.maxByOrNull { it.tScore }
        TestRecordRecorder.save("MMPI 风格自我探索", "自我探索", "演示维度·${top?.name ?: "N/A"}(${top?.tScore ?: 0})", if (result.valid) "自我探索完成" else "作答需复核", "MMPI_STYLE|${result.valid}")
    }

    FortuneCard {
        SectionTitle("MMPI 量表得分报告")
        Spacer(Modifier.height(8.dp))
        result.scales.forEach { s ->
            Text(
                "${s.name}（${s.code}）· T=${s.tScore}${if (s.tScore >= 60) "（偏高）" else "（正常）"}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            LinearProgressIndicator(
                progress = { (s.tScore.coerceIn(30, 80) - 30) / 50f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }
    }

    result.scales.forEach { s ->
        val corrected = if (s.kCorrected != s.raw.toDouble()) " · K 校正 ${"%.1f".format(s.kCorrected)}" else ""
        FortuneCard {
            SectionTitle("${s.name}（${s.code}）")
            Spacer(Modifier.height(6.dp))
            Text(
                "原始分 ${s.raw} · T=${s.tScore}${if (s.tScore >= 60) "（偏高）" else ""}$corrected",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(s.interpretation, style = MaterialTheme.typography.bodyMedium)
        }
    }

    FortuneCard {
        SectionTitle("效度与结论")
        Spacer(Modifier.height(6.dp))
        if (!result.valid) {
            result.invalidReasons.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
        Text(result.conclusion, style = MaterialTheme.typography.bodyMedium)
    }
    FortuneCard {
        SectionTitle("📌 关于 MMPI")
        Spacer(Modifier.height(6.dp))
        Text(
            "正式 MMPI/MMPI-3 需要授权题本、标准常模和受训专业人员解释。本页仅为离线通识改编，演示分数（≥60 仅表示本问卷内相对偏高）不能用于心理健康判断、医学诊断或治疗决策。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("重新测试") }
}
