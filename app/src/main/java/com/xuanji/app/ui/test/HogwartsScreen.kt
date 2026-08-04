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
import com.xuanji.app.domain.test.Hogwarts
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

@Composable
fun HogwartsScreen() {
    var index by remember { mutableIntStateOf(0) }   // 0=介绍, 1..40=答题, 41=结果
    val answers = remember { mutableStateListOf<String>() }
    var currentPick by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            index == 0 -> IntroHogwarts(onStart = { index = 1; answers.clear() })
            index in 1..Hogwarts.QUESTIONS.size -> {
                val q = Hogwarts.QUESTIONS[index - 1]
                val selected = currentPick ?: answers.getOrNull(index - 1)
                LinearProgressIndicator(
                    progress = { index.toFloat() / Hogwarts.QUESTIONS.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("第 $index / ${Hogwarts.QUESTIONS.size} 题", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FortuneCard {
                    Text(q.text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    q.options.forEach { opt ->
                        val isSel = selected == opt.first
                        val border = if (isSel)
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null
                        OutlinedButton(
                            onClick = { currentPick = opt.first },
                            modifier = Modifier.fillMaxWidth(),
                            border = border
                        ) { Text("${opt.first}. ${opt.second}") }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (index > 1) {
                        OutlinedButton(
                            onClick = {
                                index--
                                currentPick = if (answers.isNotEmpty()) answers.removeAt(answers.size - 1) else null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("上一题") }
                    }
                    Button(
                        onClick = {
                            val pick = currentPick ?: return@Button
                            answers.add(pick)
                            currentPick = null
                            index++
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (index == Hogwarts.QUESTIONS.size) "查看结果" else "下一题")
                    }
                }
            }
            else -> HogwartsResultPage(answers, onRestart = { index = 0; answers.clear(); currentPick = null })
        }
    }
}

@Composable
private fun IntroHogwarts(onStart: () -> Unit) {
    FortuneCard {
        SectionTitle("关于霍格沃茨学院测试")
        Spacer(Modifier.height(8.dp))
        Text(
            "在魔法世界里，分院帽会根据每个人的价值取向与内心渴望，将其分入四大学院之一：格兰芬多（勇气与正义）、拉文克劳（智慧与博学）、赫奇帕奇（忠诚与公正）、斯莱特林（野心与谋略）。",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "本测试共 40 题，每题有 A/B/C/D 四个选项，请凭直觉选择最贴合你的那一个。全程约 5 分钟。结果仅供娱乐与自我探索参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("开始测试（40 题）") }
    }
}

@Composable
private fun HogwartsResultPage(answers: List<String>, onRestart: () -> Unit) {
    if (answers.size < Hogwarts.QUESTIONS.size) {
        Text("答题未完成，请返回继续。", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val result = Hogwarts.calculate(answers)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        TestRecordRecorder.save("霍格沃茨学院", "趣味", result.houseName, result.portrait.take(20), "Hogwarts|${result.houseCode}")
    }

    FortuneCard {
        SectionTitle("分院结果")
        Spacer(Modifier.height(8.dp))
        Text(result.houseName, style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("学院代码：${result.houseCode}", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary)
    }

    FortuneCard {
        SectionTitle("四学院得分")
        Spacer(Modifier.height(8.dp))
        Hogwarts.HOUSE_ORDER.forEach { code ->
            val score = result.scores[code] ?: 0
            Text(
                "${Hogwarts.HOUSE_NAMES[code]}（${code}）· ${score}/${Hogwarts.QUESTIONS.size}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            LinearProgressIndicator(
                progress = { score / Hogwarts.QUESTIONS.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
        }
    }

    FortuneCard {
        SectionTitle("${result.houseName}深度解读")
        Spacer(Modifier.height(6.dp))
        Text(result.interpretation, style = MaterialTheme.typography.bodyMedium)
    }

    FortuneCard {
        SectionTitle("综合画像")
        Spacer(Modifier.height(6.dp))
        Text(result.portrait, style = MaterialTheme.typography.bodyMedium)
    }
    FortuneCard {
        SectionTitle("📌 关于霍格沃茨")
        Spacer(Modifier.height(6.dp))
        Text(
            "本测试取材于广受欢迎的魔法世界观，四大学院各代表一组价值取向。结果仅供娱乐与自我探索参考，请以现实生活中的自我认知为准。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("重新测试") }
}
