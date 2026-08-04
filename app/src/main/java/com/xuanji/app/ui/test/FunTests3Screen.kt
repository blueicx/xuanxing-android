package com.xuanji.app.ui.test

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xuanji.app.domain.test.Fun3Result
import com.xuanji.app.domain.test.FunTests3
import com.xuanji.app.ui.components.FortuneCard
import com.xuanji.app.ui.components.SectionTitle

/** 页面状态:菜单 / 某个子测试作答中 / 某个子测试结果 */
private sealed class Fun3Stage {
    object Menu : Fun3Stage()
    data class Taking(val subTest: String) : Fun3Stage()
    data class Result(val result: Fun3Result) : Fun3Stage()
}

/** 趣味测试大合集 3:选择页 → 逐题作答 → 结果页。支持 initialSubTest 直接进入指定子测试。 */
@Composable
fun FunTests3Screen(initialSubTest: String? = null) {
    var stage by remember { mutableStateOf<Fun3Stage>(if (initialSubTest != null) Fun3Stage.Taking(initialSubTest) else Fun3Stage.Menu) }
    when (val s = stage) {
        is Fun3Stage.Menu -> Fun3Menu(onPick = { stage = Fun3Stage.Taking(it) })
        is Fun3Stage.Taking -> key(s.subTest) {
            Fun3TakingScreen(
                subTest = s.subTest,
                onBackToMenu = { stage = Fun3Stage.Menu },
                onFinished = { stage = Fun3Stage.Result(it) }
            )
        }
        is Fun3Stage.Result -> Fun3ResultPage(
            result = s.result,
            onRestart = { stage = Fun3Stage.Taking(s.result.subTest) },
            onBackToMenu = { stage = Fun3Stage.Menu }
        )
    }
}

@Composable
private fun Fun3Menu(onPick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("趣味测试大合集 3", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "六个全新子测试:动物人格、美食水果人格、影视动漫角色、颜色心理、FBTI 美食 MBTI、瑞文智力挑战,测完图个乐。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FunTests3.SUBTESTS.forEach { info ->
            Fun3TestCard(info.title, info.badge, info.desc, onPick = { onPick(info.id) })
        }
    }
}

@Composable
private fun Fun3TestCard(title: String, badge: String, desc: String, onPick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(badge, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Fun3TakingScreen(subTest: String, onBackToMenu: () -> Unit, onFinished: (Fun3Result) -> Unit) {
    val questions = FunTests3.questionsOf(subTest)
    var index by remember { mutableIntStateOf(0) }   // 0=说明, 1..n=答题
    val answers = remember { mutableStateListOf<String>() }
    var pick by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            index == 0 -> Fun3Intro(
                subTest = subTest,
                questionCount = questions.size,
                onStart = { index = 1; answers.clear(); pick = null },
                onBackToMenu = onBackToMenu
            )
            index in 1..questions.size -> {
                val q = questions[index - 1]
                LinearProgressIndicator(
                    progress = { index.toFloat() / questions.size },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("第 $index / ${questions.size} 题", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FortuneCard {
                    Text(q.text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    q.options.forEach { (letter, label) ->
                        val selected = pick == letter
                        val border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        OutlinedButton(onClick = { pick = letter }, modifier = Modifier.fillMaxWidth(), border = border) {
                            Text("$letter · $label")
                        }
                        Spacer(Modifier.height(6.dp))
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
                        if (pick == null) return@Button
                        answers.add(pick!!)
                        pick = null
                        if (index == questions.size) {
                            onFinished(FunTests3.calculate(subTest, answers.toList()))
                        } else {
                            index++
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Text(if (index == questions.size) "查看结果" else "下一题")
                    }
                }
            }
        }
    }
}

@Composable
private fun Fun3Intro(subTest: String, questionCount: Int, onStart: () -> Unit, onBackToMenu: () -> Unit) {
    val info = FunTests3.SUBTESTS.first { it.id == subTest }
    val optionCount = FunTests3.questionsOf(subTest).first().options.size
    FortuneCard {
        SectionTitle(info.title)
        Spacer(Modifier.height(8.dp))
        Text(info.desc, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "共 $questionCount 题,每题 $optionCount 个选项,凭第一感觉作答,全程离线。结果仅供娱乐参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("开始测试（$questionCount 题）") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("返回测试列表") }
    }
}

@Composable
private fun Fun3ResultPage(result: Fun3Result, onRestart: () -> Unit, onBackToMenu: () -> Unit) {
    val info = FunTests3.SUBTESTS.first { it.id == result.subTest }
    val category = when (result.subTest) {
        "Raven" -> "职业"        // 瑞文推理 → 认知能力/职业类
        "ColorPsych" -> "性格"   // 颜色心理 → 性格心理类
        else -> "趣味"
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        TestRecordRecorder.save("趣味3·${result.subTest}", category, result.name, result.interpretation.take(20), "Fun3|${result.subTest}|${result.code}")
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FortuneCard {
            SectionTitle("${info.title} · 结果")
            Spacer(Modifier.height(8.dp))
            when (result.subTest) {
                "FBTI" -> Fun3FbtiBody(result)
                "Raven" -> Fun3RavenBody(result)
                else -> Fun3ScoresBody(result)
            }
        }
        FortuneCard {
            SectionTitle("解读")
            Spacer(Modifier.height(8.dp))
            Text(result.interpretation, style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("重新测试") }
        OutlinedButton(onClick = onBackToMenu, modifier = Modifier.fillMaxWidth()) { Text("返回测试列表") }
    }
}

/** 前四个子测试的结果:展示各类型得分条形 */
@Composable
private fun Fun3ScoresBody(result: Fun3Result) {
    val questions = FunTests3.questionsOf(result.subTest)
    val order = FunTests3.typeOrder(result.subTest)
    val names = FunTests3.typeNames(result.subTest)
    Text("你的类型:${result.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(12.dp))
    order.forEach { code ->
        val score = result.scores[code] ?: 0
        Text("${names[code] ?: code} · $score 分", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary)
        LinearProgressIndicator(progress = { score / questions.size.toFloat() }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
    }
}

/** FBTI 的结果:展示四维度倾向与四字母代码 */
@Composable
private fun Fun3FbtiBody(result: Fun3Result) {
    Text("你的 FBTI 美食人格:${result.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
    Text("人格代码:${result.code}", style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(12.dp))
    val labels = FunTests3.typeNames(result.subTest)
    val dims = listOf("E" to "I", "S" to "N", "T" to "F", "J" to "P")
    dims.forEach { (a, b) ->
        val sa = result.scores[a] ?: 0
        val sb = result.scores[b] ?: 0
        val chosen = if (sa >= sb) a else b
        Text(
            "${labels[a] ?: a} $sa / ${labels[b] ?: b} $sb · 倾向「${labels[chosen] ?: chosen}」",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        LinearProgressIndicator(progress = { (if (sa >= sb) sa else sb) / 2f }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
    }
}

/** 瑞文的结果:展示对题数与推理等级 */
@Composable
private fun Fun3RavenBody(result: Fun3Result) {
    val correct = result.scores["正确"] ?: 0
    val total = FunTests3.questionsOf(result.subTest).size
    Text("答对 $correct / $total 题", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
    Text("推理等级:${result.name}", style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.secondary)
    Spacer(Modifier.height(12.dp))
    LinearProgressIndicator(progress = { correct / total.toFloat() }, modifier = Modifier.fillMaxWidth())
}
