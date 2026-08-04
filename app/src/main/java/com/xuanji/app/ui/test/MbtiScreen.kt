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
import com.xuanji.app.domain.mbti.Mbti
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

@Composable
fun MbtiScreen() {
    // 当前题号（0=未开始，1..40=答题中，41=结果页）
    var index by remember { mutableIntStateOf(0) }
    // 已答选项（A/B）
    val answers = remember { mutableStateListOf<String>() }
    var currentPick by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("MBTI 职业性格测试", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        when {
            index == 0 -> Intro(
                onStart = { index = 1; answers.clear() }
            )
            index in 1..Mbti.QUESTIONS.size -> {
                val q = Mbti.QUESTIONS[index - 1]
                val selected = currentPick ?: answers.getOrNull(index - 1)
                // 进度
                LinearProgressIndicator(
                    progress = { index.toFloat() / Mbti.QUESTIONS.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "第 $index / ${Mbti.QUESTIONS.size} 题",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FortuneCard {
                    Text(q.q, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    q.options.forEach { opt ->
                        val isSel = selected == opt.key
                        val border = if (isSel)
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null
                        OutlinedButton(
                            onClick = { currentPick = opt.key },
                            modifier = Modifier.fillMaxWidth(),
                            border = border
                        ) { Text("${opt.key}. ${opt.text}") }
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
                        Text(if (index == Mbti.QUESTIONS.size) "查看结果" else "下一题")
                    }
                }
            }
            else -> ResultPage(
                answers = answers,
                onRestart = { index = 0; answers.clear(); currentPick = null }
            )
        }
    }
}

@Composable
private fun Intro(onStart: () -> Unit) {
    FortuneCard {
        SectionTitle("关于 MBTI")
        Spacer(Modifier.height(8.dp))
        Text(
            "MBTI（迈尔斯-布里格斯类型指标）通过 40 道题、四个维度（外向 E / 内向 I、感觉 S / 直觉 N、思考 T / 情感 F、判断 J / 感知 P）各 10 题，帮助你认识自己的性格偏好，并给出 16 种人格类型的详细解读与适合职业。",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "请凭第一反应选择最符合你的选项，没有对错之分。全程约 5 分钟。仅供自我探索参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text("开始测试（40 题）")
        }
    }
}

@Composable
private fun ResultPage(answers: List<String>, onRestart: () -> Unit) {
    if (answers.size < Mbti.QUESTIONS.size) {
        Text("答题未完成，请返回继续。", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val type = Mbti.calculate(answers)
    val dims = Mbti.scores(answers)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        TestRecordRecorder.save("MBTI 职业性格测试", "职业", type.code, type.name, "MBTI|${type.code}")
    }

    FortuneCard {
        SectionTitle("您的 MBTI 类型")
        Spacer(Modifier.height(8.dp))
        Text(
            type.code,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            type.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(8.dp))
        Text(type.description, style = MaterialTheme.typography.bodyMedium)
    }

    FortuneCard {
        SectionTitle("各维度得分")
        Spacer(Modifier.height(8.dp))
        dims.forEach { (label, pair) ->
            val left = pair.first
            val right = pair.second
            Text(
                "$label　　${if (left >= right) left else right} ${if (left >= right) "←" else "→"}（$left / $right）",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    FortuneCard {
        SectionTitle("✨ 核心优势")
        type.strengths.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
    FortuneCard {
        SectionTitle("⚠️ 潜在弱点")
        type.weaknesses.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
    FortuneCard {
        SectionTitle("💼 适合职业")
        type.careers.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
    FortuneCard {
        SectionTitle("❤️ 人际关系")
        Text(type.relationships, style = MaterialTheme.typography.bodyMedium)
    }
    FortuneCard {
        SectionTitle("🌱 个人成长建议")
        Text(type.growth, style = MaterialTheme.typography.bodyMedium)
    }
    FortuneCard {
        SectionTitle("👤 名人例子")
        Text(type.famous, style = MaterialTheme.typography.bodyMedium)
    }

    OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
        Text("重新测试")
    }
}
